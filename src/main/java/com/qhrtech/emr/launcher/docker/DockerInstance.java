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

import com.qhrtech.emr.launcher.TemplateLauncherManager;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.EventsCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.slf4j.LoggerFactory;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Blake Dickie
 */
public class DockerInstance {

    private final String hostname;
    private final int portNumber;
    private DockerClient client;

    public DockerInstance( String hostname, int portNumber, File dockerCerts ) {
        this.hostname = hostname;
        this.portNumber = portNumber;

        DockerClientConfig.DockerClientConfigBuilder confBuilder =
                                                     DockerClientConfig.createDefaultConfigBuilder()
                                                     .withUri( String.format( "https://%s:%d", hostname, portNumber ) );
        if ( dockerCerts != null ) {
            confBuilder.withDockerCertPath( dockerCerts.getPath() );
        }
        DockerClientConfig config = confBuilder.build();
        client = DockerClientBuilder.getInstance( config ).build();
    }

    public DockerInstance( String hostname, File dockerCerts ) {
        this( hostname, 2376, dockerCerts );
    }

    public void startMonitoring() {
        EventsCmd eventsCmd = client.eventsCmd();
        EventMonitor exec = eventsCmd.exec( new EventMonitor() );
        TemplateLauncherManager.getInstance().reportDockerEvent();
    }

    public void appendToState( DockerState state ) {
        List<Container> containers = client.listContainersCmd().exec();
        for ( Container c : containers ) {
            InspectContainerResponse response = client.inspectContainerCmd( c.getId() ).exec();

            DockerContainer container = new DockerContainer();
            container.setContainerId( response.getId() );
            container.setMachineHostname( hostname );

            Map<String, String> environment = new HashMap<String, String>();
            for ( String envString : response.getConfig().getEnv() ) {
                String[] parts = envString.split( "=", 2 );
                environment.put( parts[0], parts[1] );
            }
            container.setEnvironment( environment );

            InspectContainerResponse.NetworkSettings networkSettings = response.getNetworkSettings();
            container.setContainerIpAddress( networkSettings.getIpAddress() );
            for ( Map.Entry<ExposedPort, Ports.Binding[]> entry : networkSettings.getPorts().getBindings().entrySet() ) {
                ExposedPort key = entry.getKey();
                Ports.Binding[] value = entry.getValue();

                if ( value == null || value.length == 0 ) {
                    DockerPort p = new DockerPort();
                    p.setContainerPort( key.getPort() );
                    p.setUdp( key.getProtocol() == InternetProtocol.UDP );
                    container.addPort( p );
                } else {
                    for ( Ports.Binding binding : value ) {
                        DockerPort p = new DockerPort();
                        p.setContainerPort( key.getPort() );
                        p.setUdp( key.getProtocol() == InternetProtocol.UDP );
                        p.setMachinePort( binding.getHostPort() );
                        container.addPort( p );
                    }
                }

            }

            state.addContainer( container );
        }
    }

    private class EventMonitor implements ResultCallback<Event> {

        @Override
        public void onStart( Closeable closeable ) {

        }

        @Override
        public void onNext( Event object ) {
            TemplateLauncherManager.getInstance().reportDockerEvent();
        }

        @Override
        public void onError( Throwable throwable ) {
            LoggerFactory.getLogger( getClass() ).warn( "Error monitoring for docker events.", throwable );

            startMonitoring();
        }

        @Override
        public void onComplete() {

        }

        @Override
        public void close() throws IOException {
        }

    }
}
