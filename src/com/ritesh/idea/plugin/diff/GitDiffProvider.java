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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ritesh
 */
public class GitDiffProvider implements IVcsDiffProvider {

    private static final Logger LOG = Logger.getInstance(GitDiffProvider.class);

    @Override
    public boolean isFromRevision(Project project, AnActionEvent action) throws VcsException {
        VcsRevisionNumber[] data = action.getData(VcsDataKeys.VCS_REVISION_NUMBERS);
        if (data != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String generateDiff(Project project, AnActionEvent action) throws VcsException {
        String diffContent;
        VcsRevisionNumber[] data = action.getData(VcsDataKeys.VCS_REVISION_NUMBERS);
        if (data != null) {
            diffContent = fromRevisions(project, project.getBaseDir(), data[data.length - 1], data[0]);
        } else {
            final Change[] changes = action.getData(VcsDataKeys.CHANGES);
            List<VirtualFile> virtualFiles = new ArrayList<>();
            for (Change change : changes) {
                if (change.getVirtualFile() != null) {
                    virtualFiles.add(change.getVirtualFile());
                }
            }
            diffContent = fromHead(project, project.getBaseDir(), virtualFiles);
        }
        return diffContent;
    }

    private String fromRevisions(Project project, VirtualFile root, VcsRevisionNumber beforeRevisionNumber,
                                 VcsRevisionNumber afterRevisionNumber) throws VcsException {
        //TODO: First commit results in error
        GitSimpleHandler handler = new GitSimpleHandler(project, root, GitCommand.DIFF);
        handler.addParameters(beforeRevisionNumber.asString() + "^");
        handler.addParameters(afterRevisionNumber.asString());
        handler.setSilent(true);
        handler.setStdoutSuppressed(true);
        LOG.info("Executing git command : " + handler.printableCommandLine());
        return handler.run();
    }

    private String fromHead(Project project, VirtualFile root, List<VirtualFile> virtualFiles) throws VcsException {
        //TODO: publish only selected changes attribute (need to handle deleted files)
        GitSimpleHandler handler = new GitSimpleHandler(project, root, GitCommand.DIFF);
        handler.setSilent(true);
        handler.setStdoutSuppressed(true);
        handler.addParameters("HEAD");
        //handler.addRelativeFiles(virtualFiles);
        LOG.info("Executing git command : " + handler.printableCommandLine());
        return handler.run();
    }
}
