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
package com.m3.fs.vfs.provider.http4;

import java.io.IOException;

import com.m3.fs.vfs.FileContent;
import com.m3.fs.vfs.FileContentInfo;
import com.m3.fs.vfs.FileContentInfoFactory;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.impl.DefaultFileContentInfo;
import com.m3.fs.vfs.util.FileObjectUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;

/**
 * Creates {@code FileContentInfoFactory} instances for http4 provider.
 *
 * @since 2.3
 * @deprecated Use {@link com.m3.fs.vfs.provider.http5}.
 */
@Deprecated
public class Http4FileContentInfoFactory implements FileContentInfoFactory {

    /**
     * Constructs a new instance.
     */
    public Http4FileContentInfoFactory() {
        // empty
    }

    @SuppressWarnings("unchecked")
    @Override
    public FileContentInfo create(final FileContent fileContent) throws FileSystemException {
        String contentMimeType = null;
        String contentCharset = null;

        try (Http4FileObject<Http4FileSystem> http4File = (Http4FileObject<Http4FileSystem>) FileObjectUtils
                .getAbstractFileObject(fileContent.getFile())) {
            final HttpResponse lastHeadResponse = http4File.getLastHeadResponse();

            final Header header = lastHeadResponse.getFirstHeader(HTTP.CONTENT_TYPE);

            if (header != null) {
                final ContentType contentType = ContentType.parse(header.getValue());
                contentMimeType = contentType.getMimeType();

                if (contentType.getCharset() != null) {
                    contentCharset = contentType.getCharset().name();
                }
            }

            return new DefaultFileContentInfo(contentMimeType, contentCharset);
        } catch (final IOException e) {
            throw new FileSystemException(e);
        }
    }
}
