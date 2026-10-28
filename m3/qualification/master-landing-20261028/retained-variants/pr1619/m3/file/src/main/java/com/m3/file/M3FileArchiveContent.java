// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Bytes of rows inside archives, for every source: an entry of a file-backed archive comes from
 * {@link ZipFile#getInputStream} (the tree's open archive for an archive root, otherwise a
 * {@link ZipFile} opened for the read and closed with the stream); an entry of a nested archive comes from
 * streaming its enclosing entry to the named entry. Entry names are {@code /}-joined row names.
 */
final class M3FileArchiveContent {
    private M3FileArchiveContent() {
    }

    /** Nearest proper ancestor flagged {@link M3FileFlag#ARCHIVE}, or {@code -1}. */
    static int archiveOf(M3FileTree tree, int id) {
        for (int ancestor = tree.parent(id); ancestor >= 0; ancestor = tree.parent(ancestor)) {
            if (tree.flag(M3FileFlag.ARCHIVE, ancestor)) return ancestor;
        }
        return -1;
    }

    /** Content of row {@code id}, which lies inside an archive; {@code rootZip} backs row 0 when not null. */
    static InputStream open(M3FileTree tree, int id, ZipFile rootZip) throws IOException {
        int archive = archiveOf(tree, id);
        String name = M3FileZipWriter.name(tree, archive, id);
        if (archiveOf(tree, archive) < 0) {
            if (archive == 0 && rootZip != null) return entry(rootZip, name, false);
            return entry(new ZipFile(tree.path(archive).toFile()), name, true);
        }
        ZipInputStream nested = new ZipInputStream(open(tree, archive, rootZip));
        try {
            for (ZipEntry entry = nested.getNextEntry(); entry != null; entry = nested.getNextEntry()) {
                if (!entry.isDirectory() && M3FileZipScanner.normalize(entry.getName()).equals(name)) return nested;
            }
        } catch (IOException | RuntimeException failure) {
            nested.close();
            throw failure;
        }
        nested.close();
        throw new IOException("no nested entry " + name + " in " + tree.path(archive));
    }

    private static InputStream entry(ZipFile zip, String name, boolean owned) throws IOException {
        try {
            ZipEntry entry = zip.getEntry(name);
            if (entry == null || entry.isDirectory()) throw new IOException("no zip entry " + name + " in " + zip.getName());
            InputStream in = zip.getInputStream(entry);
            if (!owned) return in;
            return new FilterInputStream(in) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        zip.close();
                    }
                }
            };
        } catch (IOException | RuntimeException failure) {
            if (owned) zip.close();
            throw failure;
        }
    }
}
