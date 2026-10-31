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
package com.m3.fs.vfs.filter;

import java.io.Serializable;

import com.m3.fs.vfs.FileContent;
import com.m3.fs.vfs.FileFilter;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSelectInfo;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileType;

/**
 * This filter accepts files or directories that are empty.
 * <p>
 * If the {@code File} is a directory it checks that it contains no files.
 * </p>
 * <p>
 * Example, showing how to print out a list of the current directory's empty files/directories:
 * </p>
 *
 * <pre>
 * FileSystemManager fsManager = M3VFS.getManager();
 * FileObject dir = fsManager.toFileObject(new File(&quot;.&quot;));
 * FileObject[] files = dir.findFiles(new FileFilterSelector(EmptyFileFilter.EMPTY));
 * for (int i = 0; i &lt; files.length; i++) {
 *     System.out.println(files[i]);
 * }
 * </pre>
 *
 * <p>
 * Example, showing how to print out a list of the current directory's non-empty files/directories:
 * </p>
 *
 * <pre>
 * FileSystemManager fsManager = M3VFS.getManager();
 * FileObject dir = fsManager.toFileObject(new File(&quot;.&quot;));
 * FileObject[] files = dir.findFiles(new FileFilterSelector(EmptyFileFilter.NOT_EMPTY));
 * for (int i = 0; i &lt; files.length; i++) {
 *     System.out.println(files[i]);
 * }
 * </pre>
 *
 * @author This code was originally ported from Apache Commons IO File Filter
 * @see "https://commons.apache.org/proper/commons-io/"
 * @since 2.4
 */
public class EmptyFileFilter implements FileFilter, Serializable {

    /** Singleton instance of <em>empty</em> filter. */
    public static final FileFilter EMPTY = new EmptyFileFilter();

    /** Singleton instance of <em>not-empty</em> filter. */
    public static final FileFilter NOT_EMPTY = new NotFileFilter(EMPTY);

    private static final long serialVersionUID = 1L;

    /**
     * Restrictive constructor.
     */
    protected EmptyFileFilter() {
    }

    /**
     * Checks to see if the file is empty. A non-existing file is also considered empty.
     *
     * @param fileSelectInfo the file or directory to check
     * @return {@code true} if the file or directory is <em>empty</em>, otherwise {@code false}.
     * @throws FileSystemException Thrown for file system errors.
     */
    @Override
    public boolean accept(final FileSelectInfo fileSelectInfo) throws FileSystemException {
        try (FileObject file = fileSelectInfo.getFile()) {
            if (!file.exists()) {
                return true;
            }
            if (file.getType() == FileType.FOLDER) {
                final FileObject[] files = file.getChildren();
                return files == null || files.length == 0;
            }
            try (FileContent content = file.getContent()) {
                return content.isEmpty();
            }
        }
    }

}
