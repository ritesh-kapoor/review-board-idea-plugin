/*
 * Copyright 2015 Ritesh Kapoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritesh.idea.plugin.diff;

import com.intellij.history.integration.patches.PatchCreator;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author ritesh
 */
public class IdeaDiffProvider implements IVcsDiffProvider {
    @Override
    public boolean isFromRevision(Project project, AnActionEvent action) throws VcsException {
        return false;
    }

    @Override
    public String generateDiff(Project project, AnActionEvent action) throws VcsException {
        try {
            Change[] data = action.getData(VcsDataKeys.CHANGES);
            File tempFile = File.createTempFile("diff", "patch");
            PatchCreator.create(project, Arrays.asList(data), tempFile.getPath(), false, null);
            byte[] encoded = Files.readAllBytes(Paths.get(tempFile.getPath()));
            String contents = new String(encoded, StandardCharsets.UTF_8);
            return contents;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
