// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Archive source: top-level entries read through {@link ZipFile#getInputStream}, nested entries by
 * streaming their enclosing archive entry to the named entry (no temporary files). Metadata is the
 * scan-time central-directory observation (the default). Closing releases the {@link ZipFile}.
 */
final class M3FileZipSource implements M3FileSource {
    private final ZipFile zip;

    M3FileZipSource(ZipFile zip) {
        this.zip = zip;
    }

    @Override
    public InputStream openContent(M3File file) throws IOException {
        M3FileTree tree = file.tree();
        int id = file.id();
        if (id == 0) return Files.newInputStream(tree.root(), LinkOption.NOFOLLOW_LINKS);
        int archive = tree.parent(id);
        while (!tree.flag(M3FileFlag.ARCHIVE, archive)) archive = tree.parent(archive);
        StringBuilder name = new StringBuilder();
        tree.appendRelative(archive, id, name);
        String entryName = name.toString();
        if (archive == 0) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null || entry.isDirectory()) throw new IOException("no zip entry " + entryName + " in " + tree.root());
            return zip.getInputStream(entry);
        }
        ZipInputStream nested = new ZipInputStream(openContent(tree.file(archive)));
        try {
            for (ZipEntry entry = nested.getNextEntry(); entry != null; entry = nested.getNextEntry()) {
                if (!entry.isDirectory() && M3FileZipScanner.normalize(entry.getName()).equals(entryName)) return nested;
            }
        } catch (IOException | RuntimeException failure) {
            nested.close();
            throw failure;
        }
        nested.close();
        throw new IOException("no nested entry " + entryName + " in " + tree.path(archive));
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
