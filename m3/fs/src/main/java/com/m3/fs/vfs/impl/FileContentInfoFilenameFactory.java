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
package com.m3.fs.vfs.impl;

import java.net.URLConnection;

import com.m3.fs.vfs.FileContent;
import com.m3.fs.vfs.FileContentInfo;
import com.m3.fs.vfs.FileContentInfoFactory;

/**
 * The FileContentInfoFilenameFactory.
 * <p>
 * Uses the file name extension to determine the content-type. The content-encoding is not resolved.
 * </p>
 */
public class FileContentInfoFilenameFactory implements FileContentInfoFactory {

    private static final FileContentInfo NULL_INSTANCE = new DefaultFileContentInfo(null, null);

    /**
     * Constructs a new instance.
     */
    public FileContentInfoFilenameFactory() {
        // empty
    }

    @Override
    public FileContentInfo create(final FileContent fileContent) {
        String contentType = null;

        final String name = fileContent.getFile().getName().getBaseName();
        if (name != null) {
            contentType = URLConnection.getFileNameMap().getContentTypeFor(name);
        }

        // optimize object creation for common case
        if (contentType == null) {
            return NULL_INSTANCE;
        }
        return new DefaultFileContentInfo(contentType, null);
    }
}
