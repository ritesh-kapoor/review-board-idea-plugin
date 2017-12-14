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

package com.ritesh.idea.plugin.ui.toolswindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.ChangeNodeDecorator;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode;
import com.intellij.openapi.vcs.changes.ui.ChangesTreeList;
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultTreeModel;
import java.util.Collection;
import java.util.List;

/**
 * @author Ritesh
 */
public class ReviewChangesTreeList extends ChangesTreeList<Change> {

    public ReviewChangesTreeList(Project project, Collection<Change> changes) {
        super(project, changes, false, true, null, null);
    }

    @Override
    protected DefaultTreeModel buildTreeModel(List<Change> list, ChangeNodeDecorator changeNodeDecorator) {
        return new TreeModelBuilder(myProject, false).buildModel(list, changeNodeDecorator);
    }

    @Override
    protected List<Change> getSelectedObjects(ChangesBrowserNode<?> node) {
        return node.getAllChangesUnder();
    }

    @Nullable
    @Override
    protected Change getLeadSelectedObject(ChangesBrowserNode node) {
        final Object o = node.getUserObject();
        if (o instanceof Change) {
            return (Change) o;
        }
        return null;
    }
}
