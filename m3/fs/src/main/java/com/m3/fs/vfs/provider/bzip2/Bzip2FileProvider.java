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
package com.m3.fs.vfs.provider.bzip2;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystem;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.provider.compressed.CompressedFileFileProvider;

/**
 * Provides access to the content of bzip2 compressed files.
 */
public class Bzip2FileProvider extends CompressedFileFileProvider {

    /** The provider's capabilities */
    protected static final Collection<Capability> capabilities = Collections.unmodifiableCollection(Arrays.asList(Capability.GET_LAST_MODIFIED,
        Capability.GET_TYPE, Capability.LIST_CHILDREN, Capability.READ_CONTENT, Capability.WRITE_CONTENT, Capability.URI, Capability.COMPRESS));

    /**
     * Constructs a new instance.
     */
    public Bzip2FileProvider() {
    }

    @Override
    protected FileSystem createFileSystem(final FileName name, final FileObject file,
            final FileSystemOptions fileSystemOptions) throws FileSystemException {
        return new Bzip2FileSystem(name, file, fileSystemOptions);
    }

    @Override
    public Collection<Capability> getCapabilities() {
        return capabilities;
    }
}
