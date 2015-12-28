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

package com.ritesh.idea.plugin.state;

/**
 * @author Ritesh
 */
public class Configuration {
    public String url;
    public String username;
    public String password;
    public Boolean useRbTools;

    public Configuration(String url, String username, String password, Boolean useRbTools) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.useRbTools = useRbTools;
    }

    public Configuration() {
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        super.clone();
        return new Configuration(url, username, password, useRbTools);
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "username='" + username + '\'' +
                ", url='" + url + '\'' +
                ", useRbTools='" + useRbTools + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Configuration that = (Configuration) o;

        return !(url != null ? !url.equals(that.url) : that.url != null)
                && !(username != null ? !username.equals(that.username) : that.username != null)
                && !(password != null ? !password.equals(that.password) : that.password != null)
                && !(useRbTools != null ? !useRbTools.equals(that.useRbTools) : that.useRbTools != null);

    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (useRbTools != null ? useRbTools.hashCode() : 0);
        return result;
    }
}
