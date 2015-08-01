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

package com.ritesh.idea.plugin.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;

/**
 * @author Ritesh
 */
public class GitUtil {

    private static final Logger LOG = Logger.getInstance(GitUtil.class);

    public static String generateDiffFromRevision(Project project, VirtualFile root, VcsRevisionNumber beforeRevisionNumber,
                                                  VcsRevisionNumber afterRevisionNumber) throws VcsException {
        //TODO: First commit results in error
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        });
        GitSimpleHandler handler = new GitSimpleHandler(project, root, GitCommand.DIFF);
        handler.addParameters(beforeRevisionNumber.asString() + "^");
        handler.addParameters(afterRevisionNumber.asString());
        handler.setSilent(true);
        handler.setStdoutSuppressed(true);
        LOG.info("Executing git command : " + handler.printableCommandLine());
        return handler.run();
    }

    public static String generateHeadDiff(Project project, VirtualFile root) throws VcsException {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        });
        GitSimpleHandler handler = new GitSimpleHandler(project, root, GitCommand.DIFF);
        handler.setSilent(true);
        handler.setStdoutSuppressed(true);
        handler.addParameters("HEAD");
        LOG.info("Executing git command : " + handler.printableCommandLine());
        return handler.run();
    }

}
