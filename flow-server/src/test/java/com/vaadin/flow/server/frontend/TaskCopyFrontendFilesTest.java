/*
 * Copyright 2000-2019 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;

public class TaskCopyFrontendFilesTest extends NodeUpdateTestUtil {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void should_collectJsAndCssFilesFromJars() throws IOException {
        // creating non-existing folder to make sure the execute() creates
        // the folder if missing
        File npmFolder = new File(temporaryFolder.newFolder(), "child/");
        File targetFolder = new File(npmFolder, NODE_MODULES + FLOW_NPM_PACKAGE_NAME);

        // contains:
        // - ExampleConnector.js
        // - inline.css
        File jar = TestUtils.getTestJar("jar-with-frontend-resources.jar");

        TaskCopyFrontendFiles task = new TaskCopyFrontendFiles(npmFolder,
                jars(jar));

        task.execute();

        List<String> files = TestUtils.listFilesRecursively(targetFolder);
        Assert.assertEquals(2, files.size());

        Assert.assertTrue("Js resource should have been copied",
                files.contains("ExampleConnector.js"));

        Assert.assertTrue("Css resource should have been copied",
                files.contains("inline.css"));
    }

    private static Set<File> jars(File... files) {
        return Stream.of(files).collect(Collectors.toSet());
    }
}
