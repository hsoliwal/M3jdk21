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
package com.m3.fs.vfs.provider.local;

import java.io.File;
import java.io.FilePermission;
import java.util.Collection;

import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSelector;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.provider.AbstractFileName;
import com.m3.fs.vfs.provider.AbstractFileSystem;
import com.m3.fs.vfs.util.FileObjectUtils;

/**
 * A local file system.
 */
public class LocalFileSystem extends AbstractFileSystem {

    private final String rootFile;

    /**
     * Constructs a new instance.
     *
     * @param rootFileName The root file name of this file system.
     * @param rootFile The root of this file system.
     * @param fileSystemOptions Options to build this file system.
     */
    public LocalFileSystem(final FileName rootFileName, final String rootFile, final FileSystemOptions fileSystemOptions) {
        super(rootFileName, null, fileSystemOptions);
        this.rootFile = rootFile;
    }

    /**
     * Returns the capabilities of this file system.
     */
    @Override
    protected void addCapabilities(final Collection<Capability> caps) {
        caps.addAll(DefaultLocalFileProvider.capabilities);
    }

    /**
     * Creates a file object.
     */
    @Override
    protected FileObject createFile(final AbstractFileName name) throws FileSystemException {
        // Create the file
        return new LocalFile(this, rootFile, name);
    }

    /**
     * Creates a temporary local copy of a file and its descendants.
     */
    @Override
    protected File doReplicateFile(final FileObject fileObject, final FileSelector selector) throws Exception {
        final LocalFile localFile = (LocalFile) FileObjectUtils.getAbstractFileObject(fileObject);
        final File file = localFile.getLocalFile();
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new FilePermission(file.getAbsolutePath(), "read"));
        }
        return file;
    }

}
