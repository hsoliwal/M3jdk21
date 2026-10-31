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
package com.m3.fs.vfs.provider.compressed;

import java.util.Collection;

import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystem;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.FileType;
import com.m3.fs.vfs.provider.AbstractLayeredFileProvider;
import com.m3.fs.vfs.provider.LayeredFileName;

/**
 * A file system provider for compressed files. Provides read-only file systems.
 */
public abstract class CompressedFileFileProvider extends AbstractLayeredFileProvider {

    /**
     * Constructs a new instance.
     */
    public CompressedFileFileProvider() {
    }

    /**
     * Create a FileSystem.
     *
     * @param name a file name.
     * @param file a file object.
     * @param fileSystemOptions the file system options.
     * @return a FileSystem.
     * @throws FileSystemException if a file system error occurs.
     */
    protected abstract FileSystem createFileSystem(FileName name, FileObject file, FileSystemOptions fileSystemOptions) throws FileSystemException;

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
        final FileName name = new LayeredFileName(scheme, file.getName(), FileName.ROOT_PATH, FileType.FOLDER);
        return createFileSystem(name, file, fileSystemOptions);
    }

    @Override
    public abstract Collection<Capability> getCapabilities();
}
