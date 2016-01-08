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

import com.qhrtech.emr.launcher.docker.DockerState;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Blake Dickie
 */
public class TemplateProcessor implements Generator {

    private final File source;
    private final File destination;

    public TemplateProcessor( File source, File destination ) {
        this.source = source;
        this.destination = destination;
    }

    public Map<String, Object> buildDataModel() {
        Map<String, Object> result = new HashMap<>();
        result.put( "env", System.getenv() );
        return result;
    }

    @Override
    public Map<File, byte[]> generate( DockerState state ) throws Exception {
        Configuration cfg = new Configuration( Configuration.VERSION_2_3_22 );
        cfg.setDefaultEncoding( "UTF-8" );
        cfg.setTemplateExceptionHandler( TemplateExceptionHandler.RETHROW_HANDLER );

        String relativePath;
        if ( source.isDirectory() ) {
            cfg.setDirectoryForTemplateLoading( source );
            relativePath = "";
        } else if ( source.isFile() ) {
            cfg.setDirectoryForTemplateLoading( source.getParentFile() );
            relativePath = source.getName();
        } else {
            throw new IllegalArgumentException( "Unknown path: " + source.getPath() );
        }

        return generateConfigsImpl( source, destination, relativePath, cfg );
    }

    private Map<File, byte[]> generateConfigsImpl( File source, File destination, String relativePath, Configuration cfg ) throws IOException, TemplateException {

        Map<File, byte[]> result = new HashMap<>();

        if ( source.isFile() ) {
            Template template = cfg.getTemplate( relativePath );
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try ( Writer out = new BufferedWriter( new OutputStreamWriter( bout ) ) ) {
                template.process( buildDataModel(), out );
            }
            result.put( destination, bout.toByteArray() );

        } else {
            if ( !relativePath.isEmpty() ) {
                relativePath += "/";
            }
        }

        return result;

    }

}
