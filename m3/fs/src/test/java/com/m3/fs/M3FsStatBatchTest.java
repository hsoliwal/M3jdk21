/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exact portable column/mask oracles and installed-M3FS JNI stat-batch checks. */
final class M3FsStatBatchTest {
    @TempDir Path temp;

    private static long[] row(long code, long inode, long size, long kind) {
        return new long[] {code, inode, size, 1, 100, kind, 0644, 1, 65536};
    }

    @Test void packedRowsFilterAndSortStablyWithoutObjects() {
        long[] values = new long[5 * 9];
        System.arraycopy(row(0, 1, 10, 1), 0, values, 0, 9);
        System.arraycopy(row(1, 0, 0, 0), 0, values, 9, 9);
        System.arraycopy(row(0, 2, 1, 2), 0, values, 18, 9);
        System.arraycopy(row(0, 3, 10, 1), 0, values, 27, 9);
        System.arraycopy(row(6, 0, 0, 0), 0, values, 36, 9);
        M3FsStatBatch result = new M3FsStatBatch(values);
        values[2] = -10L;
        assertEquals(10L, result.stat(0).size());
        assertArrayEquals(new int[] {0, 2, 3}, result.selectRows(M3FsStatBatch.EXISTS, 0));
        assertArrayEquals(new int[] {1}, result.selectRows(M3FsStatBatch.NOT_FOUND, 0));
        assertArrayEquals(new int[] {4}, result.selectRows(M3FsStatBatch.ERROR, 0));
        assertArrayEquals(new int[] {0, 3}, result.fileRowsBySize(false));
        assertArrayEquals(new int[] {0, 3}, result.fileRowsBySize(true));
        assertThrows(IllegalStateException.class, () -> result.stat(1));
        assertThrows(IndexOutOfBoundsException.class, () -> result.stateMask(5));
        assertThrows(IllegalArgumentException.class, () -> new M3FsStatBatch(new long[1]));
        assertThrows(IllegalArgumentException.class, () -> new M3FsStatBatch(row(0, 1, 3, 99)));
    }

    @Test void nativeBatchKeepsPerRowFailuresAndNativeMetadata() throws IOException {
        Assumptions.assumeTrue(Boolean.getBoolean("m3.fs.test") && M3FS.nativeAvailable());
        Path database = temp.resolve("bulk.sqlite");
        try (M3FS fs = M3FS.create(database)) {
            fs.mkdir("/d", 0755);
            fs.createFile("/d/a", 0644);
            fs.createFile("/d/b", 0644);
            fs.createFile("/d/c", 0644);
            fs.write("/d/a", 0, new byte[] {1, 2, 3});
            fs.write("/d/b", 0, new byte[] {1, 2, 3});
            fs.write("/d/c", 0, new byte[] {1});
            String[] paths = {"/d/a", "/absent", "/d/c", "/d", "/d/b"};
            M3FsStatBatch table = fs.statBatch(paths);
            paths[0] = "/missing-again";
            assertEquals(5, table.size());
            assertEquals(M3FsStatBatch.STATUS_NOT_FOUND, table.status(1));
            assertFalse(table.exists(1));
            assertTrue(table.exists(0));
            assertEquals(fs.stat("/d/a"), table.stat(0));
            assertEquals(fs.stat("/d"), table.stat(3));
            assertArrayEquals(new int[] {0, 2, 3, 4}, table.selectRows(M3FsStatBatch.EXISTS, M3FsStatBatch.ERROR));
            assertArrayEquals(new int[] {2, 0, 4}, table.fileRowsBySize(false));
            assertArrayEquals(new int[] {0, 4, 2}, table.fileRowsBySize(true));
            assertThrows(NullPointerException.class, () -> fs.statBatch("/d/a", null));
            assertThrows(IllegalArgumentException.class, () -> fs.statBatch(new String[4097]));
            assertEquals(0, fs.statBatch().size());
        }
    }

    @Test void nativeSnapshotKeepsMultiRowObservationsPinned() throws IOException {
        Assumptions.assumeTrue(Boolean.getBoolean("m3.fs.test") && M3FS.nativeAvailable()
                && M3FS.supportsReadSnapshots());
        Path database = temp.resolve("snapshot.sqlite");
        try (M3FS live = M3FS.create(database)) {
            live.createFile("/a", 0644);
            try (M3FS snapshot = M3FS.openSnapshot(database)) {
                M3FsStatBatch before = snapshot.statBatch("/a");
                live.write("/a", 0, new byte[] {1, 2});
                M3FsStatBatch after = snapshot.statBatch("/a");
                assertEquals(before.stat(0), after.stat(0));
                assertEquals(2L, live.stat("/a").size());
            }
        }
    }
}