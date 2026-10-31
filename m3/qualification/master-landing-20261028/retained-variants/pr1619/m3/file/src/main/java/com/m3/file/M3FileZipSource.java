// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/**
 * Source of an archive-root tree: the archive file's own bytes for row 0, entries through the tree's
 * open {@link ZipFile} or by streaming nested archives ({@link M3FileArchiveContent}). Metadata is the
 * scan-time central-directory observation (the default). Closing releases the {@link ZipFile}.
 */
final class M3FileZipSource implements M3FileSource {
    private final ZipFile zip;

    M3FileZipSource(ZipFile zip) {
        this.zip = zip;
    }

    @Override
    public InputStream openContent(M3File file) throws IOException {
        if (file.id() == 0) return Files.newInputStream(file.tree().root(), LinkOption.NOFOLLOW_LINKS);
        return M3FileArchiveContent.open(file.tree(), file.id(), zip);
    }

    @Override
    public String separator(Path root) {
        return "/";
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }
}
