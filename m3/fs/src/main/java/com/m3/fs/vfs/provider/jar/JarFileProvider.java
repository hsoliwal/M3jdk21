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
package com.m3.fs.vfs.provider.jar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystem;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.FileType;
import com.m3.fs.vfs.provider.AbstractFileName;
import com.m3.fs.vfs.provider.LayeredFileName;
import com.m3.fs.vfs.provider.zip.ZipFileProvider;

/**
 * A file system provider for Jar files. Provides read-only file systems. This provides access to Jar specific features
 * like Signing and Manifest Attributes.
 */
public class JarFileProvider extends ZipFileProvider {

    static final Collection<Capability> CAPABILITIES;

    static {
        final Collection<Capability> combined = new ArrayList<>(capabilities);
        combined.addAll(Arrays.asList(Capability.ATTRIBUTES, Capability.FS_ATTRIBUTES, Capability.SIGNING,
                Capability.MANIFEST_ATTRIBUTES, Capability.VIRTUAL));
        CAPABILITIES = Collections.unmodifiableCollection(combined);
    }

    /**
     * Constructs a new instance.
     */
    public JarFileProvider() {
    }

    /**
     * Creates a layered file system. This method is called if the file system is not cached.
     *
     * @param scheme The URI scheme.
     * @param file The file to create the file system on top of.
     * @return The file system.
     */
    @Override
    protected FileSystem doCreateFileSystem(final String scheme, final FileObject file,
            final FileSystemOptions fileSystemOptions) throws FileSystemException {
        final AbstractFileName name = new LayeredFileName(scheme, file.getName(), FileName.ROOT_PATH, FileType.FOLDER);
        return new JarFileSystem(name, file, fileSystemOptions);
    }

    @Override
    public Collection<Capability> getCapabilities() {
        return CAPABILITIES;
    }
}
