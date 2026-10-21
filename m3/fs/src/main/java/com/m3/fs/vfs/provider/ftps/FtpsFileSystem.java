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
package com.m3.fs.vfs.provider.ftps;

import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.provider.GenericFileName;
import com.m3.fs.vfs.provider.ftp.FtpClient;
import com.m3.fs.vfs.provider.ftp.FtpFileSystem;

/**
 * A FTPS file system.
 *
 * @since 2.1
 */
public class FtpsFileSystem extends FtpFileSystem {

    /**
     * Creates a new FtpsFileSystem.
     *
     * @param rootName The root of the file system.
     * @param ftpClient The FtpClient.
     * @param fileSystemOptions The FileSystemOptions.
     * @since 2.1
     */
    public FtpsFileSystem(final GenericFileName rootName, final FtpClient ftpClient,
            final FileSystemOptions fileSystemOptions) {
        super(rootName, ftpClient, fileSystemOptions);
    }

    @Override
    protected FtpsClientWrapper createWrapper() throws FileSystemException {
        return new FtpsClientWrapper((GenericFileName) getRoot().getName(), getFileSystemOptions());
    }
}
