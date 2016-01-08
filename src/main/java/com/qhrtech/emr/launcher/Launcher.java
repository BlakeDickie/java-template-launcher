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
package com.qhrtech.emr.launcher;

import com.qhrtech.emr.launcher.docker.DockerInstance;
import com.qhrtech.emr.launcher.docker.NginxProxyGenerator;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import java.io.File;

/**
 *
 * @author Blake Dickie
 */
public class Launcher {

    public static void main( String[] args ) throws Exception {

        Cli<CliOptions> cli = CliFactory.createCli( CliOptions.class );
        CliOptions options = cli.parseArguments( args );

        if ( options.getDockerHosts() == null && options.isMonitor() ) {
            System.err.println( "-monitor can only be used when combined with -docker." );
            System.exit( 1 );
        }
        if ( options.getDockerHosts() == null && options.getProxyFile() != null ) {
            System.err.println( "-proxyFile can only be used when combined with -docker." );
            System.exit( 1 );
        }

        TemplateLauncherManager manager = TemplateLauncherManager.getInstance();

        if ( options.getDockerHosts() != null ) {
            for ( String hostArg : options.getDockerHosts() ) {
                for ( String host : hostArg.split( "," ) ) {
                    host = host.trim();
                    if ( host.isEmpty() ) {
                        continue;
                    }
                    if ( host.contains( ":" ) ) {
                        String[] parts = host.split( "\\:", 2 );
                        manager.addDockerInstance( new DockerInstance( parts[0], Integer.parseInt( parts[1] ), options.getDockerCerts() ) );
                    } else {
                        manager.addDockerInstance( new DockerInstance( host, options.getDockerCerts() ) );
                    }
                }
            }
        }

        if ( options.getTemplate() != null ) {
            for ( String templatePair : options.getTemplate() ) {
                String[] pair = templatePair.split( ";", 2 );
                manager.addGenerator( new TemplateProcessor( new File( pair[0] ), new File( pair[1] ) ) );
//                TemplateProcessor processor = new TemplateProcessor();
//                processor.generateConfigs( new File( pair[0] ), new File( pair[1] ) );
            }
        }

        if ( options.getProxyFile() != null ) {
            NginxProxyGenerator gen = new NginxProxyGenerator( options.getProxyFile(), options.getProxyCerts(), options.getProxyConfs() );
            manager.addGenerator( gen );
        }

        manager.setLaunchCommand( options.getCommand() );
        manager.setNotifyCommand( options.getNotifyCommand() );

        if ( options.isMonitor() ) {
            manager.startMonitoring();
        }

        manager.startUp();

    }
}
