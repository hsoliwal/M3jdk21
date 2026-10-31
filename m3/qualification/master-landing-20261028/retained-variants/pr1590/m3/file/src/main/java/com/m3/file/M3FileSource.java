// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * Where the rows of an {@link M3FileTree} come from and how their bytes are read: the file system,
 * a zip/jar central directory, an archive nested in an archive.
 *
 * <p>One abstract method, {@link #openContent(M3File)}; metadata defaults to the scan-time lanes,
 * channels and output default to refusal, the separator to the root's file system. {@link M3File}
 * routes its reads through the source of its tree, so byte limits, progress and shadows apply to
 * every source alike.</p>
 */
@FunctionalInterface
public interface M3FileSource extends Closeable {
    /** NIO with {@code NOFOLLOW_LINKS}: fresh metadata, seekable channels, output. */
    M3FileSource FILESYSTEM = M3FileSystemSource.INSTANCE;

    /** Raw bytes of a regular entry; the caller closes the stream. */
    InputStream openContent(M3File file) throws IOException;

    /** Scan-time metadata from the lanes (unknown size as 0, unknown time as the epoch). */
    default M3File.Metadata metadata(M3File file) throws IOException {
        M3FileTree tree = file.tree();
        int id = file.id();
        long mtime = tree.mtime(id);
        return new M3File.Metadata(tree.flag(M3FileFlag.REGULAR, id), tree.flag(M3FileFlag.DIR, id),
                tree.flag(M3FileFlag.LINK, id), Math.max(0, tree.size(id)),
                FileTime.fromMillis(mtime == Long.MIN_VALUE ? 0 : mtime));
    }

    default SeekableByteChannel openChannel(M3File file) throws IOException {
        throw new IOException("source has no seekable channel: " + file);
    }

    default OutputStream openOutput(M3File file) throws IOException {
        throw new IOException("read-only source: " + file);
    }

    /** Separator of relative path strings of trees from this source. */
    default String separator(Path root) {
        return root.getFileSystem().getSeparator();
    }

    @Override
    default void close() throws IOException {
    }
}
