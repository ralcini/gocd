/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.config.parts;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.WildcardScanner;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import org.jdom.input.JDOMParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class XmlPartialConfigProvider implements PartialConfigProvider {

    private final String defaultPatter = "**/*.gocd.xml";

    private MagicalGoConfigXmlLoader loader;

    public  XmlPartialConfigProvider(MagicalGoConfigXmlLoader loader)
    {
        this.loader = loader;
    }

    @Override
    public PartialConfig load(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
        File[] allFiles = getFiles(configRepoCheckoutDirectory, context);

        // if context had changed files list then we could parse only new content

        PartialConfig[] allFragments = parseFiles(allFiles);

        PartialConfig partialConfig = new PartialConfig();

        collectFragments(allFragments, partialConfig);

        partialConfig.validatePart();

        return partialConfig;
    }

    public File[] getFiles(File configRepoCheckoutDirectory, PartialConfigLoadContext context) {
        String pattern = defaultPatter;

        Configuration configuration = context.configuration();
        if(configuration != null)
        {
            ConfigurationProperty explicitPattern = configuration.getProperty("pattern");
            if(explicitPattern != null)
            {
                pattern = explicitPattern.getValue();
            }
        }

        return getFiles(configRepoCheckoutDirectory,pattern);
    }

    private File[] getFiles(File configRepoCheckoutDirectory,String pattern) {
        WildcardScanner scanner = new WildcardScanner(configRepoCheckoutDirectory,pattern);

        return scanner.getFiles();
    }

    private void collectFragments(PartialConfig[] allFragments, PartialConfig partialConfig) {
        for(PartialConfig frag : allFragments)
        {
            for(PipelineConfigs pipesInGroup : frag.getGroups())
            {
                for(PipelineConfig pipe : pipesInGroup)
                {
                    partialConfig.getGroups().addPipeline(pipesInGroup.getGroup(),pipe);
                }
            }
            for(EnvironmentConfig env : frag.getEnvironments())
            {
                partialConfig.getEnvironments().add(env);
            }
        }
    }

    public PartialConfig[] parseFiles(File[] allFiles) {
        PartialConfig[] parts = new PartialConfig[allFiles.length];
        for(int i = 0; i < allFiles.length; i++){
            parts[i] = parseFile(allFiles[i]);
        }

        return parts;
    }

    public PartialConfig parseFile(File file) {
        try {
            final FileInputStream inputStream = new FileInputStream(file);
            return loader.fromXmlPartial(inputStream, PartialConfig.class);
        }
        catch (JDOMParseException jdomex)
        {
            throw new RuntimeException("Syntax error in xml file in configuration repository",jdomex);
        }
        catch (IOException ioex)
        {
            throw new RuntimeException("IO error when trying to parse xml file in configuration repository",ioex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Failed to parse xml file in configuration repository",ex);
        }
    }
}