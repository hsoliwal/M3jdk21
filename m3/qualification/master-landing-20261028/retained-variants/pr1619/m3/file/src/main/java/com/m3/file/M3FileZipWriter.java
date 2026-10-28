// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes the entries of a walk into a zip: directories as {@code name/}, regular files streamed through
 * {@link M3File#openInputStream(long, M3FileProgress, M3FileShadow...)} (any source, so archive trees
 * repack), names relative to the walk root joined by {@code /}, modified times kept. The walk root itself,
 * links and other kinds are not written; a nested archive row is written as its bytes and its entries are
 * not repeated.
 */
final class M3FileZipWriter {
    private M3FileZipWriter() {
    }

    static long write(M3FileWalk walk, OutputStream target, long maxBytes, M3FileProgress progress) throws IOException {
        M3FileTree tree = walk.tree();
        int root = walk.rootId();
        progress.begin("zip " + tree.path(root), walk.bytes());
        ZipOutputStream zip = new ZipOutputStream(target);
        long entries = 0;
        M3FileCursor cursor = walk.cursor();
        for (int id = cursor.nextId(); id >= 0; id = cursor.nextId()) {
            if (id == root) continue;
            boolean directory = tree.flag(M3FileFlag.DIR, id);
            if (!directory && !tree.flag(M3FileFlag.REGULAR, id)) continue;
            ZipEntry entry = new ZipEntry(name(tree, root, id) + (directory ? "/" : ""));
            long mtime = tree.mtime(id);
            if (mtime != Long.MIN_VALUE) entry.setLastModifiedTime(FileTime.fromMillis(mtime));
            zip.putNextEntry(entry);
            if (!directory) {
                try (InputStream in = tree.file(id).openInputStream(maxBytes, progress.split(Math.max(0, tree.size(id))))) {
                    in.transferTo(zip);
                }
                if (tree.flag(M3FileFlag.ARCHIVE, id)) cursor.skipSubtree();
            }
            zip.closeEntry();
            entries++;
        }
        zip.finish();
        progress.done();
        return entries;
    }

    /** {@code /}-joined names from {@code root} (exclusive) to {@code id}. */
    static String name(M3FileTree tree, int root, int id) {
        StringBuilder out = new StringBuilder();
        append(tree, root, id, out);
        return out.toString();
    }

    private static void append(M3FileTree tree, int root, int id, StringBuilder out) {
        int parent = tree.parent(id);
        if (parent != root) {
            append(tree, root, parent, out);
            out.append('/');
        }
        tree.appendName(id, out);
    }
}
