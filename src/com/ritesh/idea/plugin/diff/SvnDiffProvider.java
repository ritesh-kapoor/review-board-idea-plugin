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
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.idea.svn.SvnVcs;
import org.jetbrains.idea.svn.api.BaseSvnClient;
import org.jetbrains.idea.svn.api.Revision;
import org.jetbrains.idea.svn.api.Target;
import org.jetbrains.idea.svn.commandLine.Command;
import org.jetbrains.idea.svn.commandLine.CommandExecutor;
import org.jetbrains.idea.svn.commandLine.SvnCommandName;
import org.jetbrains.idea.svn.history.LogEntry;
import org.jetbrains.idea.svn.history.LogEntryConsumer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ritesh
 */
public class SvnDiffProvider extends BaseSvnClient implements IVcsDiffProvider {

    private static final Logger LOG = Logger.getInstance(SvnDiffProvider.class);

    @Override
    public boolean isFromRevision(Project project, AnActionEvent action) throws VcsException {
        ChangeList[] data = action.getData(VcsDataKeys.CHANGE_LISTS);
        if (data != null && data.length > 0 && data[0] instanceof CommittedChangeList) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String generateDiff(Project project, AnActionEvent action) throws VcsException {
        String diffContent;
        if (isFromRevision(project, action)) {
            ChangeList[] data = action.getData(VcsDataKeys.CHANGE_LISTS);
            diffContent = fromRevisions(project, project.getBaseDir(), ((CommittedChangeList) data[data.length - 1]).getNumber(),
                    ((CommittedChangeList) data[0]).getNumber());
        } else {
            final Change[] changes = action.getData(VcsDataKeys.CHANGES);
            diffContent = fromHead(project, project.getBaseDir(), changes);
        }
        return diffContent;
    }

    private String fromRevisions(Project project, VirtualFile root, long beforeRevisionNumber,
                                 long afterRevisionNumber) throws VcsException {
        SvnVcs svnVcs = SvnVcs.getInstance(project);
        Target svnTarget = Target.on(new File(root.getPath()));

        final long[] lastRevisionNumber = new long[1];
        svnVcs.getFactory().createHistoryClient().doLog(svnTarget, Revision.of(beforeRevisionNumber)
                , Revision.of(0), false, true, false, 2, null, new LogEntryConsumer() {
            @Override
            public void consume(LogEntry logEntry) {
                lastRevisionNumber[0] = logEntry.getRevision();
            }
        });

        List<String> parameters = Arrays.asList("-r", lastRevisionNumber[0] + ":" + afterRevisionNumber, "--patch-compatible");
        Command command = new Command(SvnCommandName.diff);
        command.setWorkingDirectory(new File(root.getPath()));
        command.setTarget(svnTarget);
        command.put(parameters);
        LOG.info("Executing svn command : Parameters : " + parameters + " ,target :" + svnTarget);
        CommandExecutor commandExecutor = newRuntime(svnVcs).runWithAuthenticationAttempt(command);
        String output = commandExecutor.getBinaryOutput().toString();
        return output;
    }


    private String fromHead(Project project, VirtualFile root, Change[] changes) throws VcsException {
        //TODO: publish only selected changes attribute (need to handle deleted files)
        SvnVcs svnVcs = SvnVcs.getInstance(project);

        Target svnTarget = Target.on(new File(root.getPath()));
        List<String> parameters = new ArrayList<>();
        /*for (Change change : changes) {
            if (change.getVirtualFile() != null) {
                String path = new File(root.getPath()).toURI()
                        .relativize(new File(change.getVirtualFile().getPath()).toURI()).getPath();
                parameters.add(path);
            }
        }*/
        parameters.add("-r");
        parameters.add("HEAD");
        parameters.add("--patch-compatible");
        Command command = new Command(SvnCommandName.diff);
        command.setWorkingDirectory(new File(root.getPath()));
        command.setTarget(svnTarget);
        command.put(parameters);
        LOG.info("Executing svn command : Parameters : " + parameters + " ,target :" + svnTarget);
        CommandExecutor commandExecutor = newRuntime(svnVcs).runWithAuthenticationAttempt(command);
        String output = commandExecutor.getBinaryOutput().toString();
        return output;
    }
}
