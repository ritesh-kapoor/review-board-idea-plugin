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

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.spellchecker.ui.SpellCheckingEditorCustomization;
import com.intellij.ui.EditorCustomization;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;
import com.intellij.ui.SoftWrapsEditorCustomization;
import com.intellij.util.TextFieldCompletionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author ritesh on 24/7/15.
 */
public class MultiValueAutoComplete {
    private static final char[] SEPARATORS = {','};

    public static EditorTextField create(Project project, DataProvider dataProvider) {
        List<EditorCustomization> customizations =
                Arrays.<EditorCustomization>asList(SoftWrapsEditorCustomization.ENABLED, SpellCheckingEditorCustomization.DISABLED);
        EditorTextField editorField = ServiceManager.getService(project, EditorTextFieldProvider.class)
                .getEditorField(FileTypes.PLAIN_TEXT.getLanguage(), project, customizations);
        new CommaSeparatedTextFieldCompletion(dataProvider).apply(editorField);
        return editorField;

    }

    public interface DataProvider {
        List<String> getValues(String prefix);
    }

    private static class CommaSeparatedTextFieldCompletion extends TextFieldCompletionProvider {
        private DataProvider dataProvider;

        public CommaSeparatedTextFieldCompletion(DataProvider dataProvider) {
            this.dataProvider = dataProvider;
        }

        @NotNull
        @Override
        protected String getPrefix(@NotNull String currentTextPrefix) {
            final int separatorPosition = lastSeparatorPosition(currentTextPrefix);
            return separatorPosition == -1 ? currentTextPrefix : currentTextPrefix.substring(separatorPosition + 1).trim();
        }

        private static int lastSeparatorPosition(@NotNull String text) {
            int lastPosition = -1;
            for (char separator : SEPARATORS) {
                int lio = text.lastIndexOf(separator);
                if (lio > lastPosition) {
                    lastPosition = lio;
                }
            }
            return lastPosition;
        }

        @Override
        protected void addCompletionVariants(@NotNull String text, int offset, @NotNull String prefix,
                                             @NotNull CompletionResultSet result) {

            result.addLookupAdvertisement("Select one or more users separated with comma, | or new lines");
            List<String> values = dataProvider.getValues(prefix);
            for (String completionVariant : values) {
                final LookupElementBuilder element = LookupElementBuilder.create(completionVariant);
                result.addElement(element.withLookupString(completionVariant.toLowerCase()));
            }

        }
    }
}
