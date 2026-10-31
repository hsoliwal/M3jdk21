// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contract oracle (Java) and existing M3FS SQLite/JNI storage round-trip. */
class M3BuildStateTest {
    private static final int MAX_BYTES = 1_000_000;
    @TempDir Path temp;

    @Test void deterministicJavaArrayGraphAndImageProof() throws Exception {
        assertTrue(M3BuildStateContract.verify() > 10_000);
    }

    @Test void sqliteHeadIsAtomicVersionedAndRestartPersistent() throws Exception {
        Path database = temp.resolve("build.sqlite");
        M3BuildState first = oneState("v1");
        M3BuildState second = oneState("v2");
        byte[] firstCommit;
        try (M3FS fs = M3FS.create(database)) {
            assertTrue(M3BuildStateStore.read(fs, MAX_BYTES, 3, 2).isEmpty());
            M3BuildStateStore.Version published = M3BuildStateStore.publish(
                    fs, first, null, MAX_BYTES, 3, 2);
            firstCommit = published.commitment();
            assertArrayEquals(firstCommit, M3BuildStateStore.read(fs, MAX_BYTES, 3, 2)
                    .orElseThrow().commitment());
            assertThrows(IOException.class, () -> M3BuildStateStore.publish(
                    fs, second, new byte[32], MAX_BYTES, 3, 2));
            assertArrayEquals(firstCommit, M3BuildStateStore.read(fs, MAX_BYTES, 3, 2)
                    .orElseThrow().commitment());
            try (M3FS pinned = M3FS.openSnapshot(database)) {
                M3BuildStateStore.Version next = M3BuildStateStore.publish(
                        fs, second, firstCommit, MAX_BYTES, 3, 2);
                assertFalse(Arrays.equals(firstCommit, next.commitment()));
                assertArrayEquals(firstCommit, M3BuildStateStore.read(pinned, MAX_BYTES, 3, 2)
                        .orElseThrow().commitment());
                assertArrayEquals(next.commitment(), M3BuildStateStore.read(fs, MAX_BYTES, 3, 2)
                        .orElseThrow().commitment());
            }
        }
        try (M3FS reopened = M3FS.openSnapshot(database)) {
            Optional<M3BuildStateStore.Version> loaded = M3BuildStateStore.read(reopened, MAX_BYTES, 3, 2);
            assertTrue(loaded.isPresent());
            assertEquals(1, loaded.orElseThrow().state().size());
            assertTrue(loaded.orElseThrow().state().changedFrom(first).affected(0));
        }
    }

    @Test void imageTamperingFailsInsteadOfAcceptingStaleBuildData() throws Exception {
        Path database = temp.resolve("tamper.sqlite");
        try (M3FS fs = M3FS.create(database)) {
            M3BuildStateStore.publish(fs, oneState("first"), null, MAX_BYTES, 3, 2);
            fs.write("/m3-build/state", 0, new byte[]{0});
            assertThrows(IOException.class, () -> M3BuildStateStore.read(fs, MAX_BYTES, 3, 2));
        }
    }

    private static M3BuildState oneState(String text) {
        byte[] known = sha(text); byte[] unknown = new byte[32];
        return new M3BuildState(new String[]{"src/A.java"},
                new byte[]{M3BuildState.SOURCE}, new long[]{text.length()},
                known, unknown, new long[]{0}, new int[]{0, 0}, new int[0], sha("build-context"));
    }
    private static byte[] sha(String data) {
        try { return MessageDigest.getInstance("SHA-256").digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
