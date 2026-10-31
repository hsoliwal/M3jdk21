/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// M3FS adaptation, Copyright 2026 Hitesh Soliwal and contributors.
// Original Apache license and attribution retained.
// Relocated Apache Commons VFS 2.10.0 to the M3FS namespace.
// Uses the existing donor GenericURLFileName API to remove the optional HttpClient 3 dependency.
package com.m3.fs.vfs.provider.url;

import org.apache.commons.lang3.StringUtils;
import com.m3.fs.vfs.FileType;
import com.m3.fs.vfs.provider.GenericURLFileName;

/**
 * A URL FileName.
 */
public class UrlFileName extends GenericURLFileName {

    /**
     * The constructor.
     *
     * @param scheme The scheme to use.
     * @param hostName The host name.
     * @param port The port.
     * @param defaultPort The default port.
     * @param userName The user's login id.
     * @param password The user's credentials.
     * @param path The file path.
     * @param type The file type.
     * @param queryString Parameters to use when locating or creating the file name.
     */
    public UrlFileName(final String scheme, final String hostName, final int port, final int defaultPort,
        final String userName, final String password, final String path, final FileType type,
        final String queryString) {
        super(scheme, hostName, port, defaultPort, userName, password, path, type, queryString);
    }

    @Override
    protected void appendRootUri(final StringBuilder buffer, final boolean addPassword) {
        if (!StringUtils.isEmpty(getHostName())) {
            super.appendRootUri(buffer, addPassword);
            return;
        }

        buffer.append(getScheme());
        buffer.append(":");
    }
}
