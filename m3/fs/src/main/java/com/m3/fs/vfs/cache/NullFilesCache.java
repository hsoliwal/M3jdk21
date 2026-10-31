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
package com.m3.fs.vfs.cache;

import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystem;

/**
 * A {@link com.m3.fs.vfs.FilesCache} implementation.
 * <p>
 * This implementation never ever caches a single file.
 * </p>
 * <p>
 * <strong>Notice</strong>: if you use resolveFile(uri) multiple times with the same path, the system will always create a new
 * instance. Changes on one instance of this file are not seen by the others.
 * </p>
 */
public class NullFilesCache extends AbstractFilesCache {

    /**
     * Constructs a new instance.
     */
    public NullFilesCache() {
        // empty
    }

    @Override
    public void clear(final FileSystem filesystem) {
        // empty
    }

    @Override
    public FileObject getFile(final FileSystem filesystem, final FileName name) {
        return null;
    }

    @Override
    public void putFile(final FileObject file) {
        // empty
    }

    @Override
    public boolean putFileIfAbsent(final FileObject file) {
        return false;
    }

    @Override
    public void removeFile(final FileSystem filesystem, final FileName name) {
        // empty
    }
}
