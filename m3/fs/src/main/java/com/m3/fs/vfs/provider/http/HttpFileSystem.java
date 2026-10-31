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

import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.provider.AbstractFileName;
import com.m3.fs.vfs.provider.AbstractFileSystem;
import com.m3.fs.vfs.provider.GenericFileName;

/**
 * An HTTP file system.
 *
 * @deprecated Use {@link com.m3.fs.vfs.provider.http5}.
 */
@Deprecated
public class HttpFileSystem extends AbstractFileSystem {

    private final HttpClient httpClient;

    /**
     * Constructs a new instance.
     *
     * @param rootName root base name
     * @param httpClient {@link HttpClient} instance
     * @param fileSystemOptions Options to build this file system.
     */
    protected HttpFileSystem(final GenericFileName rootName, final HttpClient httpClient,
            final FileSystemOptions fileSystemOptions) {
        super(rootName, null, fileSystemOptions);
        this.httpClient = httpClient;
    }

    /**
     * Adds the capabilities of this file system.
     */
    @Override
    protected void addCapabilities(final Collection<Capability> caps) {
        caps.addAll(HttpFileProvider.CAPABILITIES);
    }

    /** @since 2.0 */
    @Override
    public void closeCommunicationLink() {
        if (getClient() != null) {
            final HttpConnectionManager mgr = getClient().getHttpConnectionManager();
            if (mgr instanceof MultiThreadedHttpConnectionManager) {
                ((MultiThreadedHttpConnectionManager) mgr).shutdown();
            }
        }
    }

    /**
     * Creates a file object. This method is called only if the requested file is not cached.
     */
    @Override
    protected FileObject createFile(final AbstractFileName name) throws Exception {
        return new HttpFileObject<>(name, this);
    }

    /**
     * Gets the HTTP client.
     *
     * @return the HttpClient.
     */
    protected HttpClient getClient() {
        return httpClient;
    }
}
