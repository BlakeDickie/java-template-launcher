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

/**
 *
 * @author Blake Dickie
 */
public class DockerPort {

    private Integer machinePort;
    private int containerPort;
    private boolean udp;

    public DockerPort() {
    }

    public Integer getMachinePort() {
        return machinePort;
    }

    public void setMachinePort( Integer machinePort ) {
        this.machinePort = machinePort;
    }

    public int getContainerPort() {
        return containerPort;
    }

    public void setContainerPort( int containerPort ) {
        this.containerPort = containerPort;
    }

    public boolean isUdp() {
        return udp;
    }

    public void setUdp( boolean udp ) {
        this.udp = udp;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString( this );
    }

}
