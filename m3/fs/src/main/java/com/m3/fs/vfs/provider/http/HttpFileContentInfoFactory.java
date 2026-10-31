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

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.lang3.ArrayUtils;
import com.m3.fs.vfs.FileContent;
import com.m3.fs.vfs.FileContentInfo;
import com.m3.fs.vfs.FileContentInfoFactory;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.impl.DefaultFileContentInfo;
import com.m3.fs.vfs.util.FileObjectUtils;

/**
 * Creates FileContentInfo instances for HTTP.
 *
 * @deprecated Use {@link com.m3.fs.vfs.provider.http5}.
 */
@Deprecated
public class HttpFileContentInfoFactory implements FileContentInfoFactory {

    /**
     * Constructs a new instance.
     */
    public HttpFileContentInfoFactory() {
        // empty
    }

    @Override
    public FileContentInfo create(final FileContent fileContent) throws FileSystemException {

        String contentType = null;

        final HeadMethod headMethod;
        try (HttpFileObject<HttpFileSystem> httpFile = (HttpFileObject<HttpFileSystem>) FileObjectUtils
                .getAbstractFileObject(fileContent.getFile())) {
            headMethod = httpFile.getHeadMethod();
        } catch (final IOException e) {
            throw new FileSystemException(e);
        }
        final Header header = headMethod.getResponseHeader("content-type");
        if (header != null) {
            final HeaderElement[] element = header.getElements();
            if (!ArrayUtils.isEmpty(element)) {
                contentType = element[0].getName();
            }
        }

        return new DefaultFileContentInfo(contentType, headMethod.getResponseCharSet());
    }
}
