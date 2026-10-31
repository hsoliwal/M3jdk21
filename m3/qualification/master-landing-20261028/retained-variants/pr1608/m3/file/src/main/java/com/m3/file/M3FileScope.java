// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Caller-owned, bounded interning of M3File facades by original provider Path.
 * One growing reference array holds the facades; no map entries or path copies
 * are retained. Equality requires the same FileSystem instance and Path.equals.
 * No path normalization, file IO or global pool is involved. Closing releases
 * the table without invalidating already returned facades or closing providers.
 *
 * <p>Operations are synchronized. Capacity exhaustion refuses new identities;
 * it never evicts an existing identity. Expected lookup cost is constant with
 * a suitable provider hash; worst-case collision cost is linear in occupancy.
 */
public final class M3FileScope implements AutoCloseable {
    private static final int MAX_FILES = 1 << 27;
    private final int maximumFiles;
    private M3File[] table = new M3File[0];
    private int size;
    private boolean closed;

    /** @param maximumFiles maximum unique provider paths, from zero to 2^27 */
    public M3FileScope(int maximumFiles) {
        if (maximumFiles < 0 || maximumFiles > MAX_FILES) {
            throw new IllegalArgumentException("invalid maximumFiles");
        }
        this.maximumFiles = maximumFiles;
    }

    /** Return the first facade admitted for this exact provider/path identity. */
    public synchronized M3File file(Path path) {
        requireOpen();
        Objects.requireNonNull(path, "path");
        int slot = table.length == 0 ? -1 : slot(path, table);
        if (slot >= 0 && table[slot] != null) return table[slot];
        if (size == maximumFiles) throw new IllegalStateException("file scope capacity exceeded");
        if (size >= table.length / 2) {
            M3File[] grown = new M3File[table.length == 0 ? 2 : table.length * 2];
            for (M3File file : table) {
                if (file != null) grown[slot(file.path(), grown)] = file;
            }
            table = grown;
            slot = slot(path, table);
        }
        M3File file = M3File.of(path);
        table[slot] = file;
        size++;
        return file;
    }

    /** Current number of identities; refuses access after close. */
    public synchronized int size() {
        requireOpen();
        return size;
    }

    @Override public synchronized void close() {
        table = new M3File[0];
        size = 0;
        closed = true;
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("file scope is closed");
    }

    private static int slot(Path path, M3File[] values) {
        int hash = path.hashCode() ^ System.identityHashCode(path.getFileSystem());
        int at = (hash ^ (hash >>> 16)) & (values.length - 1);
        while (values[at] != null) {
            Path previous = values[at].path();
            if (path.getFileSystem() == previous.getFileSystem() && path.equals(previous)) return at;
            at = (at + 1) & (values.length - 1);
        }
        return at;
    }
}
