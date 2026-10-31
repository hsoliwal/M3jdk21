/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileType;
import com.m3.fs.vfs.provider.LayeredFileName;
import com.m3.fs.vfs.provider.UriParser;

/** Keeps a literal inner exclamation mark distinct from the existing layer separator. */
final class DatabaseFileName extends LayeredFileName {
    private static final char[] PATH_RESERVED = {'!', '#', '?', ' '};

    DatabaseFileName(String scheme, FileName outer, String path, FileType type) {
        super(scheme, outer, path, type);
    }

    @Override
    public FileName createName(String path, FileType type) {
        return new DatabaseFileName(getScheme(), getOuterName(), path, type);
    }

    @Override
    protected String createURI() {
        StringBuilder uri = new StringBuilder();
        appendRootUri(uri, true);
        try {
            uri.append(UriParser.encode(getPathDecoded(), PATH_RESERVED));
        } catch (FileSystemException malformed) {
            throw new IllegalStateException("Invalid canonical M3FS database path", malformed);
        }
        return uri.toString();
    }

    @Override
    public String getFriendlyURI() {
        return getURI(); // Local backing URIs contain no authentication fields.
    }
}
