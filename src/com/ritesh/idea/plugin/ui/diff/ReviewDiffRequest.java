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

package com.ritesh.idea.plugin.ui.diff;

import com.intellij.openapi.diff.DiffContent;
import com.intellij.openapi.diff.DiffRequest;
import com.intellij.openapi.diff.SimpleContent;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import org.jetbrains.annotations.NotNull;

import static org.apache.commons.lang.StringUtils.defaultString;

/**
 * @author ritesh
 */
public class ReviewDiffRequest extends DiffRequest {

    private final Change selectedChange;

    public ReviewDiffRequest(Project project, Change selectedChange) {
        super(project);
        this.selectedChange = selectedChange;
    }

    @NotNull
    @Override
    public DiffContent[] getContents() {
        try {
            ContentRevision beforeRevision = selectedChange.getBeforeRevision();
            ContentRevision afterRevision = selectedChange.getAfterRevision();
            FileType srcFileType = FileTypeRegistry.getInstance()
                    .getFileTypeByFileName(beforeRevision.getFile().getName());
            FileType dstFileType = FileTypeRegistry.getInstance()
                    .getFileTypeByFileName(afterRevision.getFile().getName());

            return new DiffContent[]{
                    new SimpleContent(defaultString(beforeRevision.getContent()), srcFileType),
                    new SimpleContent(defaultString(afterRevision.getContent()), dstFileType)
            };
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
    }

    @Override
    public String[] getContentTitles() {
        return new String[]{
                selectedChange.getBeforeRevision().getRevisionNumber().asString(),
                selectedChange.getAfterRevision().getRevisionNumber().asString()
        };
    }

    @Override
    public String getWindowTitle() {
        return selectedChange.getAfterRevision().getFile().getName();
    }
}
