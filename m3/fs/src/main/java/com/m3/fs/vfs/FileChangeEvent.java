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
package com.m3.fs.vfs;

/**
 * An event fired when a file is changed.
 */
public class FileChangeEvent {

    /**
     * The file object
     */
    private final FileObject fileObject;

    /**
     * Constructs a new instance.
     *
     * @param fileObject the file object.
     */
    public FileChangeEvent(final FileObject fileObject) {
        this.fileObject = fileObject;
    }

    /**
     * Returns the file that changed.
     *
     * @return The FileObject that was changed.
     * @deprecated Use {@link #getFileObject()}.
     */
    @Deprecated
    public FileObject getFile() {
        return fileObject;
    }

    /**
     * Returns the file that changed.
     *
     * @return The FileObject that was changed.
     * @since 2.5.0
     */
    public FileObject getFileObject() {
        return fileObject;
    }
}
