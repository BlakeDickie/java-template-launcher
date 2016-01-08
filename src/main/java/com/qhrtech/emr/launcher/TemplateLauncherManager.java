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
import com.qhrtech.emr.launcher.docker.DockerState;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Blake Dickie
 */
public class TemplateLauncherManager {

    private static final TemplateLauncherManager INSTANCE = new TemplateLauncherManager();

    public static TemplateLauncherManager getInstance() {
        return INSTANCE;
    }

    private final List<Generator> generators;
    private final List<DockerInstance> instances;

    private List<String> launchCommand;
    private String notifyCommand;

    private TemplateLauncherManager() {
        generators = new ArrayList<>();
        instances = new ArrayList<>();
    }

    public void addGenerator( Generator g ) {
        generators.add( g );
    }

    public void addDockerInstance( DockerInstance i ) {
        instances.add( i );
    }

    private Map<File, byte[]> currentFiles = Collections.EMPTY_MAP;

    public synchronized void doGeneration() throws Exception {
        Map<File, byte[]> newFiles = new HashMap<>();

        DockerState state = new DockerState();
        for ( DockerInstance instance : instances ) {
            instance.appendToState( state );
        }

        for ( Generator g : generators ) {
            newFiles.putAll( g.generate( state ) );
        }

        if ( mapsEqual( newFiles, currentFiles ) ) {
            // No Changes
            return;
        }

        currentFiles = newFiles;
        for ( Map.Entry<File, byte[]> entry : newFiles.entrySet() ) {
            File key = entry.getKey();
            byte[] value = entry.getValue();
            FileUtils.writeByteArrayToFile( key, value );
        }

    }

    private boolean mapsEqual( Map<File, byte[]> m1, Map<File, byte[]> m2 ) {
        if ( m1.size() != m2.size() ) {
            return false;
        }

        for ( Map.Entry<File, byte[]> entry : m1.entrySet() ) {
            File key = entry.getKey();
            byte[] m1Value = entry.getValue();

            byte[] m2Value = m2.get( key );
            if ( m2Value == null || !Arrays.equals( m1Value, m2Value ) ) {
                return false;
            }
        }

        return true;
    }

    public List<String> getLaunchCommand() {
        return launchCommand;
    }

    public void setLaunchCommand( List<String> launchCommand ) {
        this.launchCommand = launchCommand;
    }

    public String getNotifyCommand() {
        return notifyCommand;
    }

    public void setNotifyCommand( String notifyCommand ) {
        this.notifyCommand = notifyCommand;
    }

    public void startMonitoring() {
        RefreshMonitorThread monitorThread = new RefreshMonitorThread();
        monitorThread.start();
        for ( DockerInstance docker : instances ) {
            docker.startMonitoring();
        }
    }

    private final Object eventLock = new Object();
    private boolean refreshQueued = false;
    private boolean commandStarted = false;

    public void startUp() throws Exception {
        Process p = null;
        synchronized ( eventLock ) {
            doGeneration();
            if ( launchCommand != null ) {
                ProcessBuilder pb = new ProcessBuilder( launchCommand );
                p = pb.inheritIO().start();
            }
            commandStarted = true;
            eventLock.notifyAll();
        }
        if ( p != null ) {
            System.exit( p.waitFor() );
        }
    }

    public void reportDockerEvent() {
        synchronized ( eventLock ) {
            refreshQueued = true;
            eventLock.notifyAll();
        }
    }

    private void doRefresh() {
        synchronized ( eventLock ) {
            try {
                doGeneration();
                if ( notifyCommand != null ) {
                    ProcessBuilder pb = new ProcessBuilder( notifyCommand );
                    pb.redirectError( ProcessBuilder.Redirect.INHERIT );
                    pb.redirectOutput( ProcessBuilder.Redirect.INHERIT );
                    pb.start().waitFor();
                }
            } catch ( Exception ex ) {
                LoggerFactory.getLogger( getClass() ).error( "Error reloading templates.", ex );
            }
        }
    }

    private class RefreshMonitorThread extends Thread {

        public RefreshMonitorThread() {
        }

        @Override
        public void run() {
            while ( true ) {
                synchronized ( eventLock ) {
                    if ( refreshQueued && commandStarted ) {
                        refreshQueued = false;
                        doRefresh();
                    }

                    try {
                        eventLock.wait();
                    } catch ( InterruptedException ignore ) {
                    }
                }
            }
        }

    }

}
