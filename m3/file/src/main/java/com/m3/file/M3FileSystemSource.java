// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * The original M3File edge as a source: NIO over the caller's provider, links never followed. Rows
 * inside archives expanded by a scan ({@link M3FileScanSpec#expandArchives()}) read through
 * {@link M3FileArchiveContent}, report their central-directory metadata and refuse channels and output.
 */
final class M3FileSystemSource implements M3FileSource {
    static final M3FileSystemSource INSTANCE = new M3FileSystemSource();

    private M3FileSystemSource() {
    }

    @Override
    public InputStream openContent(M3File file) throws IOException {
        if (insideArchive(file)) return M3FileArchiveContent.open(file.tree(), file.id(), null);
        return Channels.newInputStream(openChannel(file));
    }

    /** Every invocation refreshes metadata with NOFOLLOW_LINKS (archive entries: scan-time lanes). */
    @Override
    public M3File.Metadata metadata(M3File file) throws IOException {
        if (insideArchive(file)) return M3FileSource.super.metadata(file);
        BasicFileAttributes attributes = Files.readAttributes(
                file.path(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return new M3File.Metadata(attributes.isRegularFile(), attributes.isDirectory(),
                attributes.isSymbolicLink(), attributes.size(), attributes.lastModifiedTime());
    }

    @Override
    public SeekableByteChannel openChannel(M3File file) throws IOException {
        if (insideArchive(file)) return M3FileSource.super.openChannel(file);
        return Files.newByteChannel(file.path(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public OutputStream openOutput(M3File file) throws IOException {
        if (insideArchive(file)) return M3FileSource.super.openOutput(file);
        return Files.newOutputStream(file.path(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
    }

    private static boolean insideArchive(M3File file) {
        return file.isTreeEntry() && M3FileArchiveContent.archiveOf(file.tree(), file.id()) >= 0;
    }
}
