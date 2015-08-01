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

package com.ritesh.idea.plugin.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.ritesh.idea.plugin.util.exception.InvalidConfigurationException;
import com.ritesh.idea.plugin.util.exception.InvalidCredentialException;
import com.ritesh.idea.plugin.util.exception.ServerConnectionFailureException;
import org.apache.http.client.ClientProtocolException;

import java.net.SocketException;
import java.net.UnknownHostException;

import static org.apache.commons.lang.StringUtils.defaultString;

/**
 * @author ritesh
 */
public class ErrorManager {
    private static final Logger LOG = Logger.getInstance(ErrorManager.class);

    public static class Message {
        public String message;
        public Type type;

        public Message(String message, Type type) {
            this.message = message;
            this.type = type;
        }

        public static enum Type {
            ERROR, WARNING
        }
    }

    public static Message getMessage(Exception exception) {
        if (exception instanceof InvalidCredentialException || exception instanceof InvalidConfigurationException) {
            LOG.warn(exception.getMessage());
            return new Message(defaultString(exception.getMessage()), Message.Type.WARNING);
        } else if (exception instanceof ServerConnectionFailureException || exception instanceof SocketException
                || exception instanceof UnknownHostException || exception instanceof ClientProtocolException) {
            LOG.warn(exception.getMessage());
            return new Message("Unable to connect to server", Message.Type.WARNING);
        } else {
            LOG.error(exception);
            return new Message(defaultString(exception.getMessage()), Message.Type.ERROR);
        }
    }

    public static void showMessage(Exception exception) {
        Message message = getMessage(exception);
        Notifications.Bus.notify(new Notification("ReviewBoard", "Message from Reviewboard server", message.message,
                message.type == Message.Type.ERROR ? NotificationType.ERROR : NotificationType.WARNING));

    }
}
