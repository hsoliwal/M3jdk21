// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;
//FLLDCIM: MRL - ADAPT M3File->M3NioFile - source m3/qualification/master-landing-20261028/retained-variants/pr1608/m3/file/src/main/java/jdk/m3/file/M3FileScope.java blob 29d14a539edc3d471f7671dc92f2da6424498163 (retained PR #1608 head; parent 4c51b1e6cadb blob 75a0e44fd34a4c8e6d76cb0ce6b50fdae2ac2127 https://github.com/hsoliwal/M3jdk21/blob/4c51b1e6cadb/m3/file/src/main/java/com/m3/file/M3FileScope.java); recipe m3/file/qualification/retained-port-20261107 ADAPT:M3File->M3NioFile

import java.nio.file.Path;
import java.util.Objects;

/**
 * Caller-owned, bounded interning of M3NioFile facades by original provider Path.
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
    private M3NioFile[] table = new M3NioFile[0];
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
    public synchronized M3NioFile file(Path path) {
        requireOpen();
        Objects.requireNonNull(path, "path");
        int slot = table.length == 0 ? -1 : slot(path, table);
        if (slot >= 0 && table[slot] != null) return table[slot];
        if (size == maximumFiles) throw new IllegalStateException("file scope capacity exceeded");
        if (size >= table.length / 2) {
            M3NioFile[] grown = new M3NioFile[table.length == 0 ? 2 : table.length * 2];
            for (M3NioFile file : table) {
                if (file != null) grown[slot(file.path(), grown)] = file;
            }
            table = grown;
            slot = slot(path, table);
        }
        M3NioFile file = M3NioFile.of(path);
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
        table = new M3NioFile[0];
        size = 0;
        closed = true;
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("file scope is closed");
    }

    private static int slot(Path path, M3NioFile[] values) {
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
