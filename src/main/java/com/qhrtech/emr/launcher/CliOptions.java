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

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;
import java.io.File;
import java.util.List;

/**
 * Command line options definition for JewelCLI parsing.
 *
 * @author Blake Dickie
 */
public interface CliOptions {

    @Option( longName = { "template" },
             shortName = { "t" },
             defaultToNull = true,
             pattern = "[^;]*;[^;]*",
             description = "Takes pairs of configuration template sources and outputs seperated by a semicolon (;)" )
    public List<String> getTemplate();

    @Option( longName = "docker",
             description = "Enable docker machine extensions for provided servers",
             pattern = "[^\\:]+(?:\\:\\d+)?",
             defaultToNull = true )
    public List<String> getDockerHosts();

    @Option( longName = "monitor",
             description = "Continuing monitoring for changes to docker" )
    public boolean isMonitor();

    @Option( longName = "proxyFile",
             description = "Generate a pre-configured Nginx proxy.",
             defaultToNull = true )
    public File getProxyFile();

    @Option( longName = "proxyCerts",
             description = "Folder containing HTTPS Certificate for use with proxyFile.",
             defaultToNull = true )
    public File getProxyCerts();

    @Option( longName = "dockerCerts",
             description = "Folder containing HTTPS Certificate to use when connecting to docker.",
             defaultToNull = true )
    public File getDockerCerts();

    @Option( longName = "proxyConfs",
             description = "Folder containing nginx include files for use with proxyFile.",
             defaultToNull = true )
    public File getProxyConfs();

    @Option( longName = "notify",
             shortName = "n",
             defaultToNull = true,
             description = "Command to exectute after the configuration is changed" )
    public String getNotifyCommand();

    @Unparsed( name = "command", defaultToNull = true )
    public List<String> getCommand();
}
