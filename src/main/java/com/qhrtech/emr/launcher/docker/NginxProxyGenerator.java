/*
 * Copyright 2015 QHR Technologies.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qhrtech.emr.launcher.docker;

import com.qhrtech.emr.launcher.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Blake Dickie
 */
public class NginxProxyGenerator implements Generator {

    private final Logger log = LoggerFactory.getLogger( getClass() );

    private final File outputFile;
    private final File certsFile;
    private final File configsFile;

    public NginxProxyGenerator( File outputFile, File certsFile, File configsFile ) {
        this.outputFile = outputFile;
        this.certsFile = certsFile;
        this.configsFile = configsFile;
    }

    private Set<String> detectCurrentCertificates() {
        if ( certsFile == null ) {
            return Collections.EMPTY_SET;
        }
        Set<String> certs = new HashSet<>();
        for ( File file : certsFile.listFiles() ) {
            String filename = file.getName();
            if ( filename.toLowerCase().endsWith( ".crt" ) ) {
                certs.add( filename.substring( 0, filename.length() - 4 ) );
            }
        }
        return certs;
    }

    private void writeHeader( PrintWriter out, String httpsCert ) {
        out.println( "# If we receive X-Forwarded-Proto, pass it through; otherwise, pass along the" );
        out.println( "# scheme used to connect to this server" );
        out.println( "map $http_x_forwarded_proto $proxy_x_forwarded_proto {" );
        out.println( "  default $http_x_forwarded_proto;" );
        out.println( "  ''      $scheme;" );
        out.println( "}" );
        out.println( "# If we receive Upgrade, set Connection to \"upgrade\"; otherwise, delete any" );
        out.println( "# Connection header that may have been passed to this server" );
        out.println( "map $http_upgrade $proxy_connection {" );
        out.println( "  default upgrade;" );
        out.println( "  '' close;" );
        out.println( "}" );

        out.println( "gzip_types text/plain text/css application/javascript application/json application/x-javascript text/xml application/xml application/xml+rss text/javascript;" );

        out.println( "# HTTP 1.1 support" );
        out.println( "proxy_http_version 1.1;" );
        out.println( "proxy_buffering off;" );
        out.println( "proxy_set_header Host $http_host;" );
        out.println( "proxy_set_header Upgrade $http_upgrade;" );
        out.println( "proxy_set_header Connection $proxy_connection;" );
        out.println( "proxy_set_header X-Real-IP $remote_addr;" );
        out.println( "proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;" );
        out.println( "proxy_set_header X-Forwarded-Proto $proxy_x_forwarded_proto;" );
        out.println( "server {" );
        out.println( "  listen 80;" );
        out.println( "  server_name _; # This is just an invalid value which will never trigger on a real hostname." );
        out.println( "  return 404;" );
        out.println( "}" );

        out.println( "server {" );
        out.println( "  listen 443 ssl spdy;" );
        out.println( "  server_name _; # This is just an invalid value which will never trigger on a real hostname." );
        out.println( "  return 404;" );
        out.println( "  ssl_protocols TLSv1 TLSv1.1 TLSv1.2;" );
        out.println( "  ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-DSS-AES128-GCM-SHA256:kEDH+AESGCM:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA:ECDHE-ECDSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-DSS-AES128-SHA256:DHE-RSA-AES256-SHA256:DHE-DSS-AES256-SHA:DHE-RSA-AES256-SHA:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA256:AES256-SHA256:AES128-SHA:AES256-SHA:AES:CAMELLIA:DES-CBC3-SHA:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!MD5:!PSK:!aECDH:!EDH-DSS-DES-CBC3-SHA:!EDH-RSA-DES-CBC3-SHA:!KRB5-DES-CBC3-SHA;" );
        out.println( "  ssl_prefer_server_ciphers on;" );
        out.println( "  ssl_session_timeout 5m;" );
        out.println( "  ssl_session_cache shared:SSL:50m;" );
        out.println( "  ssl_certificate " + new File( certsFile, httpsCert + ".crt" ).getAbsolutePath() + ";" );
        out.println( "  ssl_certificate_key " + new File( certsFile, httpsCert + ".key" ).getAbsolutePath() + ";" );
        out.println( "}" );
    }

    private void writeSegment( PrintWriter out, String hostname, String sslCert, boolean redirect, boolean errorResponse, String exposedIp, int exposedPort, String proxyType ) throws IOException {
        out.println( "server {" );
        if ( sslCert == null ) {
            out.println( "  listen 80;" );
        } else {
            out.println( "  listen 443 ssl spdy;" );

            out.println( "  ssl_protocols TLSv1 TLSv1.1 TLSv1.2;" );
            out.println( "  ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-DSS-AES128-GCM-SHA256:kEDH+AESGCM:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA:ECDHE-ECDSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-DSS-AES128-SHA256:DHE-RSA-AES256-SHA256:DHE-DSS-AES256-SHA:DHE-RSA-AES256-SHA:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA256:AES256-SHA256:AES128-SHA:AES256-SHA:AES:CAMELLIA:DES-CBC3-SHA:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!MD5:!PSK:!aECDH:!EDH-DSS-DES-CBC3-SHA:!EDH-RSA-DES-CBC3-SHA:!KRB5-DES-CBC3-SHA;" );
            out.println( "  ssl_prefer_server_ciphers on;" );
            out.println( "  ssl_session_timeout 5m;" );
            out.println( "  ssl_session_cache shared:SSL:50m;" );

            out.println( "  ssl_certificate " + new File( certsFile, sslCert + ".crt" ).getAbsolutePath() + ";" );
            out.println( "  ssl_certificate_key " + new File( certsFile, sslCert + ".key" ).getAbsolutePath() + ";" );

        }
        out.println( "  server_name " + hostname + ";" );

        if ( redirect ) {
            out.println( "  return 301 https://$host$request_uri;" );
        } else if ( errorResponse ) {
            out.println( "  return 404;" );
        } else {

            File configExtras = new File( configsFile, hostname + ".conf" );
            if ( configExtras.isFile() ) {
                out.println( "  include " + configExtras.getAbsolutePath() + ";" );
            }

            out.println( "  location / {" );
            out.println( "    proxy_pass " + proxyType + "://" + exposedIp + ":" + exposedPort + ";" );
            out.println( "  }" );
        }
        out.println( "}" );
    }

    private void writeEnvironment( PrintWriter out, Set<String> certs, HostEnvironment env ) throws IOException {
        int port = Integer.parseInt( env.getPort() );
        DockerPort portInfo = env.container.findPort( port );
        if ( portInfo == null ) {
            log.error( "Port Not Exposed: " + env.host + ":" + port );
            return;
        }

        String cert = env.detectCert( certs );

        if ( env.httpMode.equals( "auto" ) ) {
            if ( cert == null ) {
                writeSegment( out, env.host, null, false, false, env.container.getMachineHostname(), portInfo.getMachinePort(), env.getProxyType() );
            } else {
                writeSegment( out, env.host, null, true, false, env.container.getMachineHostname(), portInfo.getMachinePort(), env.getProxyType() );
            }
        } else if ( env.httpMode.equals( "enabled" ) ) {
            writeSegment( out, env.host, null, false, false, env.container.getMachineHostname(), portInfo.getMachinePort(), env.getProxyType() );
        }

        if ( cert != null ) {
            if ( env.httpsMode.equals( "auto" ) || env.httpsMode.equals( "enabled" ) ) {
                writeSegment( out, env.host, cert, false, false, env.container.getMachineHostname(), portInfo.getMachinePort(), env.getProxyType() );
            }
        }
    }

    private void doGenerate( PrintWriter out, DockerState state ) throws Exception {
        Set<String> certs = detectCurrentCertificates();

        String fallbackCert = null;
        if ( !certs.isEmpty() ) {
            fallbackCert = certs.iterator().next();
        }

        writeHeader( out, fallbackCert );

        Map<String, HostEnvironment> environments = new HashMap<>();

        for ( DockerContainer c : state.getContainers() ) {
            for ( Map.Entry<String, String> entry : c.getEnvironment().entrySet() ) {
                String envName = entry.getKey();
                String envValue = entry.getValue();

                if ( envName.startsWith( "VIRTUAL_HOST" ) ) {
                    for ( String hostname : envValue.split( "," ) ) {
                        HostEnvironment env = new HostEnvironment( hostname, envName.substring( 12 ), c );
                        environments.put( hostname, env );
                    }
                }
            }
        }

        for ( HostEnvironment env : environments.values() ) {
            writeEnvironment( out, certs, env );
        }

    }

    @Override
    public Map<File, byte[]> generate( DockerState state ) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try ( PrintWriter writer = new PrintWriter( bout ) ) {
            doGenerate( writer, state );
        }
        return Collections.singletonMap( outputFile, bout.toByteArray() );
    }

    private class HostEnvironment {

        private final String host;
        private final String port;
        private final String httpsMode;
        private final String httpMode;
        private final String sslCert;
        private final String proxyType;
        private final DockerContainer container;

        public HostEnvironment( String host, String suffix, DockerContainer container ) {
            this.host = host;
            port = container.getEnvWithDefault( "VIRTUAL_PORT" + suffix, "80" );
            httpsMode = container.getEnvWithDefault( "HTTPS_MODE" + suffix, "auto" );
            httpMode = container.getEnvWithDefault( "HTTP_MODE" + suffix, "auto" );
            sslCert = container.getEnvWithDefault( "SSL_CERT" + suffix, null );
            proxyType = container.getEnvWithDefault( "PROXY_TYPE" + suffix, "http" );
            this.container = container;
        }

        public String getHost() {
            return host;
        }

        public String getPort() {
            return port;
        }

        public String getHttpsMode() {
            return httpsMode;
        }

        public String getHttpMode() {
            return httpMode;
        }

        public String getSslCert() {
            return sslCert;
        }

        public String getProxyType() {
            return proxyType;
        }

        private String detectCert( Set<String> knownCerts ) {
            if ( sslCert != null ) {
                if ( sslCert.equals( "" ) ) {
                    return null;
                }
                if ( knownCerts.contains( sslCert ) ) {
                    return sslCert;
                } else {
                    log.warn( "Unable to find requested SSL certificate: " + sslCert );
                    return null;
                }
            }

            String bestMatch = null;
            for ( String cert : knownCerts ) {
                if ( host.endsWith( cert ) ) {
                    // Find the longest possible match.
                    if ( bestMatch == null || bestMatch.length() < cert.length() ) {
                        bestMatch = cert;
                    }
                }
            }

            return bestMatch;
        }

    }

}
