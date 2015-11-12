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

package com.ritesh.idea.plugin.messages;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

/**
 * @author ritesh
 */
public class PluginBundle {

    public static final String CONNECTION_ERROR_MSG = "reviewboard.message.connection.error";
    public static final String LOGIN_SUCCESS_MESSAGE = "reviewboard.message.login.success";
    public static final String CONNECTION_STATUS_TITLE = "reviewboard.message.connection.status.title";
    public static final String CONNECTION_TEST_TITLE = "reviewboard.message.connection.title";
    public static final String NOTIFICATION_TITLE = "reviewboard.notification.title";
    public static final String UNSUPPORTED_VCS_TITLE = "reviewboard.unsupported.vcs.title";
    public static final String UNSUPPORTED_VCS_MESSAGE = "reviewboard.unsupported.vcs.message";

    public static String message(@NotNull String key, @NotNull Object... params) {
        return getBundle().getString(key);
    }

    private static Reference<ResourceBundle> bundle;
    @NonNls
    public static final String BUNDLE = "com.ritesh.idea.plugin.messages.reviewboard";

    private PluginBundle() {
    }

    private static ResourceBundle getBundle() {
        if (bundle == null || bundle.get() == null) {
            bundle = new SoftReference<>(ResourceBundle.getBundle(BUNDLE));
        }
        return bundle.get();
    }
}
