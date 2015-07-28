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

        if ( options.getTemplate() == null && options.getCommand() == null ) {
            System.err.println( cli.getHelpMessage() );
            System.exit( 1 );
        }

        if ( options.getTemplate() != null ) {
            for ( String templatePair : options.getTemplate() ) {
                String[] pair = templatePair.split( ";", 2 );
                TemplateProcessor processor = new TemplateProcessor();
                processor.generateConfigs( new File( pair[0] ), new File( pair[1] ) );
            }
        }

        if ( options.getCommand() != null ) {
            ProcessBuilder pb = new ProcessBuilder( options.getCommand() );
            Process process = pb.inheritIO().start();
            System.exit( process.waitFor() );
        }

    }
}
