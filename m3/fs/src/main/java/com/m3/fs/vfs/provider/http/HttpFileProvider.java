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
package com.m3.fs.vfs.provider.http;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.httpclient.HttpClient;
import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileSystem;
import com.m3.fs.vfs.FileSystemConfigBuilder;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.UserAuthenticationData;
import com.m3.fs.vfs.provider.AbstractOriginatingFileProvider;
import com.m3.fs.vfs.provider.GenericFileName;
import com.m3.fs.vfs.util.UserAuthenticatorUtils;

/**
 * An HTTP provider that uses commons-httpclient.
 *
 * @deprecated Use {@link com.m3.fs.vfs.provider.http5}.
 */
@Deprecated
public class HttpFileProvider extends AbstractOriginatingFileProvider {

    /** Authenticator information. */
    public static final UserAuthenticationData.Type[] AUTHENTICATOR_TYPES = {
            UserAuthenticationData.USERNAME, UserAuthenticationData.PASSWORD };

    static final Collection<Capability> CAPABILITIES = Collections
            .unmodifiableCollection(Arrays.asList(Capability.GET_TYPE, Capability.READ_CONTENT, Capability.URI, Capability.GET_LAST_MODIFIED,
                    Capability.ATTRIBUTES, Capability.RANDOM_ACCESS_READ, Capability.DIRECTORY_READ_CONTENT));

    /**
     * Constructs a new provider.
     */
    public HttpFileProvider() {
        setFileNameParser(HttpFileNameParser.getInstance());
    }

    /**
     * Creates a {@link FileSystem}.
     *
     * @return a new FileSystem, never null.
     */
    @Override
    protected FileSystem doCreateFileSystem(final FileName name, final FileSystemOptions fileSystemOptions)
            throws FileSystemException {
        // Create the file system
        final GenericFileName rootName = (GenericFileName) name;
        UserAuthenticationData authData = null;
        HttpClient httpClient;
        try {
            authData = UserAuthenticatorUtils.authenticate(fileSystemOptions, AUTHENTICATOR_TYPES);
            final String fileScheme = rootName.getScheme();
            final char lastChar = fileScheme.charAt(fileScheme.length() - 1);
            final String internalScheme = lastChar == 's' || lastChar == 'S' ? "https" : "http";
            httpClient = HttpClientFactory.createConnection(internalScheme, rootName.getHostName(),
                    rootName.getPort(),
                    UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
                            UserAuthenticationData.USERNAME, UserAuthenticatorUtils.toChar(rootName.getUserName()))),
                    UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
                            UserAuthenticationData.PASSWORD, UserAuthenticatorUtils.toChar(rootName.getPassword()))),
                    fileSystemOptions);
        } finally {
            UserAuthenticatorUtils.cleanup(authData);
        }
        return new HttpFileSystem(rootName, httpClient, fileSystemOptions);
    }

    @Override
    public Collection<Capability> getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public FileSystemConfigBuilder getConfigBuilder() {
        return HttpFileSystemConfigBuilder.getInstance();
    }
}
