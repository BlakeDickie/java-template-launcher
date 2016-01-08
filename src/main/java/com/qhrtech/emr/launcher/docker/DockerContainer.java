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

import org.apache.commons.lang.builder.ToStringBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Blake Dickie
 */
public class DockerContainer {

    private String machineHostname;
    private String containerId;
    private Map<String, String> environment;
    private String containerIpAddress;
    private List<DockerPort> ports;

    public DockerContainer() {
        ports = new ArrayList<>();
    }

    public String getMachineHostname() {
        return machineHostname;
    }

    public void setMachineHostname( String machineHostname ) {
        this.machineHostname = machineHostname;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId( String containerId ) {
        this.containerId = containerId;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment( Map<String, String> environment ) {
        this.environment = environment;
    }

    public String getContainerIpAddress() {
        return containerIpAddress;
    }

    public void setContainerIpAddress( String containerIpAddress ) {
        this.containerIpAddress = containerIpAddress;
    }

    public List<DockerPort> getPorts() {
        return ports;
    }

    public void addPort( DockerPort port ) {
        ports.add( port );
    }

    public DockerPort findPort( int port ) {
        for ( DockerPort p : ports ) {
            if ( p.getContainerPort() == port && !p.isUdp() ) {
                return p;
            }
        }
        return null;
    }

    public DockerPort findUDPPort( int port ) {
        for ( DockerPort p : ports ) {
            if ( p.getContainerPort() == port && p.isUdp() ) {
                return p;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString( this );
    }

    public String getEnvWithDefault( String name, String defaultValue ) {
        String value = environment.get( name );
        if ( value == null ) {
            return defaultValue;
        } else {
            return value;
        }
    }

}
