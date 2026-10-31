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
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.provider.AbstractFileName;
import com.m3.fs.vfs.provider.AbstractFileSystem;

/**
 * A read-only file system for compressed files.
 */
public abstract class CompressedFileFileSystem extends AbstractFileSystem {

    /**
     * Constructs a new instance.
     *
     * @param rootName The root file name of this file system.
     * @param parentLayer The parent layer of this file system.
     * @param fileSystemOptions Options to build this file system.
     */
    protected CompressedFileFileSystem(final FileName rootName, final FileObject parentLayer, final FileSystemOptions fileSystemOptions) {
        super(rootName, parentLayer, fileSystemOptions);
    }

    /**
     * Returns the capabilities of this file system.
     */
    @Override
    protected abstract void addCapabilities(Collection<Capability> caps);

    /**
     * Creates a file object.
     */
    @Override
    protected abstract FileObject createFile(AbstractFileName name) throws FileSystemException;
}
