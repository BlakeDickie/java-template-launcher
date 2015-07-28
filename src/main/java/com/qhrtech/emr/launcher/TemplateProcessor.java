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

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Blake Dickie
 */
public class TemplateProcessor {

    public TemplateProcessor() {
    }

    public Map<String, Object> buildDataModel() {
        Map<String, Object> result = new HashMap<>();
        result.put( "env", System.getenv() );
        return result;
    }

    public void generateConfigs( File source, File destination ) throws IOException, TemplateException {
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

        generateConfigsImpl( source, destination, relativePath, cfg );
    }

    private void generateConfigsImpl( File source, File destination, String relativePath, Configuration cfg ) throws IOException, TemplateException {

        if ( source.isFile() ) {
            Template template = cfg.getTemplate( relativePath );
            try ( Writer out = new BufferedWriter( new FileWriter( destination ) ) ) {
                template.process( buildDataModel(), out );
            }

        } else {
            if ( !relativePath.isEmpty() ) {
                relativePath += "/";
            }
        }

    }

}
