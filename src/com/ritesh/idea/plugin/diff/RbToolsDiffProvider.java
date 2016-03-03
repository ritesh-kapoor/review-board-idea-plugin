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

import com.google.common.io.CharStreams;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import org.jetbrains.idea.svn.SvnVcs;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ritesh on 18/12/15.
 */
public class RbToolsDiffProvider implements IVcsDiffProvider {
    private static final Logger LOG = Logger.getInstance(RbToolsDiffProvider.class);

    private String userName;
    private String password;
    private String url;
    private String rbtPath;
    private AbstractVcs vcs;

    public RbToolsDiffProvider(String url, String userName, String password, String rbtPath, AbstractVcs vcs) {
        this.userName = userName;
        this.password = password;
        this.url = url;
        this.rbtPath = rbtPath;
        this.vcs = vcs;
    }

    @Override
    public boolean isFromRevision(Project project, AnActionEvent action) throws VcsException {

        return revision(project, action) != null;
    }

    public VcsRevision revision(Project project, AnActionEvent action) {
        VcsRevisionNumber[] data = action.getData(VcsDataKeys.VCS_REVISION_NUMBERS);
        if (data != null) {
            return new VcsRevision(data[data.length - 1].asString(), data[0].asString());
        }

        ChangeList[] changeLists = action.getData(VcsDataKeys.CHANGE_LISTS);
        if (changeLists != null && changeLists.length > 0 && changeLists[0] instanceof CommittedChangeList) {
            String from = String.valueOf(((CommittedChangeList) changeLists[changeLists.length - 1]).getNumber());
            String to = String.valueOf(((CommittedChangeList) changeLists[changeLists.length - 1]).getNumber());
            return new VcsRevision(from, to);
        }
        return null;
    }

    @Override
    public String generateDiff(Project project, AnActionEvent action) throws VcsException {
        VcsRevision revision = revision(project, action);
        List<String> options = new ArrayList<>();
        try {
            if (vcs instanceof SvnVcs) {
                options.addAll(Arrays.asList("--svn-show-copies-as-adds", "y"));
            }
            return generateDiff(revision, project.getBaseDir().getPath(), options);
        } catch (IOException e) {
            throw new VcsException(e);
        }
    }

    private String generateDiff(VcsRevision revision, String rootPath, List<String> additionalOptions) throws IOException {
        List<String> commands = new ArrayList<>();
        String processPath = (rbtPath == null ? "rbt" : rbtPath);
        commands.addAll(Arrays.asList(processPath, "diff", "--server", url, "--username", userName, "--password", password));
        commands.addAll(additionalOptions);

        if (revision != null) {
            if (revision.fromRevision() != null) commands.add(revision.fromRevision());
            if (revision.toRevision() != null && !revision.toRevision().equals(revision.fromRevision()))
                commands.add(revision.toRevision());
        }

        LOG.info("Running command : " + commands);

        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(new File(rootPath));
        builder.redirectErrorStream(true);
        Process process = builder.start();

        String stdInput = CharStreams.toString(new InputStreamReader(process.getInputStream()));
        String stdErrorInput = CharStreams.toString(new InputStreamReader(process.getErrorStream()));
        if (stdErrorInput.trim().isEmpty()) {
            return stdInput;
        } else {
            throw new RuntimeException("Error : " + stdErrorInput);
        }
    }

}
