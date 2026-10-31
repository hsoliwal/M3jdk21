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

import com.m3.fs.vfs.FileFilter;
import com.m3.fs.vfs.FileSelectInfo;
import com.m3.fs.vfs.FileSystemException;

/**
 * Filter that accepts files whose size is &gt;= minimum size and &lt;= maximum
 * size.
 *
 * @since 2.4
 */
public class SizeRangeFileFilter implements FileFilter, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * File filter for min/max.
     */
    private final FileFilter fileFilter;

    /**
     * Constructor with sizes.
     *
     * @param minSizeInclusive the minimum file size (inclusive)
     * @param maxSizeInclusive the maximum file size (inclusive)
     */
    public SizeRangeFileFilter(final long minSizeInclusive, final long maxSizeInclusive) {
        final FileFilter minimumFilter = new SizeFileFilter(minSizeInclusive, true);
        final FileFilter maximumFilter = new SizeFileFilter(maxSizeInclusive + 1L, false);
        fileFilter = new AndFileFilter(minimumFilter, maximumFilter);
    }

    @Override
    public boolean accept(final FileSelectInfo fileSelectInfo) throws FileSystemException {
        return fileFilter.accept(fileSelectInfo);
    }

}
