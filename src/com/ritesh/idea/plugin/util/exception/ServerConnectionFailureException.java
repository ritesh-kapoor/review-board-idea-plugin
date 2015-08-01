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

package com.ritesh.idea.plugin.util.exception;

/**
 * @author ritesh
 */
public class ServerConnectionFailureException extends RuntimeException {
    public ServerConnectionFailureException(String message) {
        super(message);
    }

    public ServerConnectionFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
