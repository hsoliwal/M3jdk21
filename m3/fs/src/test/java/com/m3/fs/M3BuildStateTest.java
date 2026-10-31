// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

// Modified 2026-10-21: real-compiler failure/retry and native observed/success checkpoint regressions.

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

    @Test void failedRealCompilationRetainsObservedDeltaAndSuccessfulHeadAcrossRestart() throws Exception {
        Path database = temp.resolve("checkpoints.sqlite");
        String firstSource = "class A { static int value() { return 1; } }";
        String failedSource = "class A { int broken = ; }";
        String fixedSource = "class A { static int value() { return 2; } }";
        byte[] successful, observed;
        try (M3FS fs = M3FS.create(database)) {
            fs.mkdir("/project", 0700); fs.createFile("/project/A.java", 0600);
            replaceSource(fs, firstSource);
            M3BuildState first = inventory(database);
            VersionPair initial = observe(fs, first, null, null);
            assertTrue(M3BuildStateStore.read(fs, MAX_BYTES, 10, 0).isEmpty());
            successful = M3BuildStateStore.promoteObserved(fs, null, initial.observed(), MAX_BYTES, 10, 0).commitment();
            assertArrayEquals(initial.observed(), successful);
            replaceSource(fs, failedSource);
            M3BuildState invalid = inventory(database);
            observed = observe(fs, invalid, successful, null).observed();
            assertFalse(Arrays.equals(successful, observed));
            assertTrue(compile(failedSource) != 0);
            assertArrayEquals(successful, M3BuildStateStore.read(fs, MAX_BYTES, 10, 0).orElseThrow().commitment());
        }
        try (M3FS snapshot = M3FS.openSnapshot(database)) {
            assertArrayEquals(successful, M3BuildStateStore.read(snapshot, MAX_BYTES, 10, 0).orElseThrow().commitment());
            assertArrayEquals(observed, M3BuildStateStore.readObserved(snapshot, successful, MAX_BYTES, 10, 0)
                    .orElseThrow().commitment());
        }
        try (M3FS fs = M3FS.open(database); M3FS old = M3FS.openSnapshot(database)) {
            replaceSource(fs, fixedSource);
            M3BuildState repaired = inventory(database);
            byte[] repairedObservation = observe(fs, repaired, successful, observed).observed();
            assertThrows(IOException.class, () -> M3BuildStateStore.promoteObserved(
                    fs, successful, observed, MAX_BYTES, 10, 0));
            assertArrayEquals(successful, M3BuildStateStore.read(fs, MAX_BYTES, 10, 0).orElseThrow().commitment());
            assertEquals(0, compile(fixedSource));
            byte[] next = M3BuildStateStore.promoteObserved(fs, successful, repairedObservation,
                    MAX_BYTES, 10, 0).commitment();
            assertArrayEquals(repairedObservation, next);
            assertFalse(fs.list("/m3-build").stream().anyMatch(entry -> entry.name().equals(
                    "observed-" + java.util.HexFormat.of().formatHex(successful))));
            assertArrayEquals(successful, M3BuildStateStore.read(old, MAX_BYTES, 10, 0).orElseThrow().commitment());
            assertArrayEquals(observed, M3BuildStateStore.readObserved(old, successful, MAX_BYTES, 10, 0)
                    .orElseThrow().commitment());
            assertThrows(IOException.class, () -> M3BuildStateStore.observe(fs, repaired,
                    successful, repairedObservation, MAX_BYTES, 10, 0));
            assertThrows(IOException.class, () -> M3BuildStateStore.promoteObserved(fs,
                    successful, repairedObservation, MAX_BYTES, 10, 0));
            try (M3FS current = M3FS.openSnapshot(database)) {
                assertTrue(M3BuildStateStore.readObserved(current, next, MAX_BYTES, 10, 0).isEmpty());
                assertThrows(IOException.class, () -> M3BuildStateStore.readObserved(
                        current, successful, MAX_BYTES, 10, 0));
            }
        }
    }

    @Test void interruptedBuildCanRetryTheExactSameObservedCommitment() throws Exception {
        Path database = temp.resolve("unchanged-retry.sqlite");
        String source = "class A { static int value() { return 7; } }";
        byte[] pending;
        try (M3FS fs = M3FS.create(database)) {
            fs.mkdir("/project", 0700); fs.createFile("/project/A.java", 0600); replaceSource(fs, source);
            pending = M3BuildStateStore.observe(fs, inventory(database), null, null,
                    MAX_BYTES, 10, 0).commitment();
            assertTrue(M3BuildStateStore.read(fs, MAX_BYTES, 10, 0).isEmpty());
            // Process interruption before successful execution: leave the observation pending.
        }
        try (M3FS snapshot = M3FS.openSnapshot(database)) {
            assertArrayEquals(pending, M3BuildStateStore.readObserved(snapshot, null, MAX_BYTES, 10, 0)
                    .orElseThrow().commitment());
        }
        assertEquals(0, compile(source));
        try (M3FS fs = M3FS.open(database)) {
            assertArrayEquals(pending, M3BuildStateStore.promoteObserved(fs, null, pending,
                    MAX_BYTES, 10, 0).commitment());
        }
        try (M3FS snapshot = M3FS.openSnapshot(database)) {
            assertArrayEquals(pending, M3BuildStateStore.read(snapshot, MAX_BYTES, 10, 0)
                    .orElseThrow().commitment());
            assertTrue(M3BuildStateStore.readObserved(snapshot, pending, MAX_BYTES, 10, 0).isEmpty());
        }
    }

    @Test void observedCasWriterLockAndTamperRefuseWithoutChangingSuccessfulHead() throws Exception {
        Path database = temp.resolve("observed-cas.sqlite");
        try (M3FS fs = M3FS.create(database)) {
            fs.mkdir("/project", 0700); fs.createFile("/project/A.java", 0600);
            replaceSource(fs, "class A {}");
            M3BuildState first = inventory(database);
            byte[] successful = M3BuildStateStore.publish(fs, first, null, MAX_BYTES, 10, 0).commitment();
            replaceSource(fs, "class A { int x; }");
            M3BuildState second = inventory(database);
            Path lock = database.resolveSibling(database.getFileName() + ".m3build.lock");
            try (var channel = java.nio.channels.FileChannel.open(lock, java.nio.file.StandardOpenOption.WRITE);
                    var held = channel.lock()) {
                assertTrue(held.isValid());
                assertThrows(IOException.class, () -> M3BuildStateStore.observe(
                        fs, second, successful, null, MAX_BYTES, 10, 0));
            }
            byte[] pending = M3BuildStateStore.observe(fs, second, successful, null, MAX_BYTES, 10, 0).commitment();
            assertThrows(IOException.class, () -> M3BuildStateStore.observe(
                    fs, second, successful, null, MAX_BYTES, 10, 0));
            assertThrows(IOException.class, () -> M3BuildStateStore.observe(
                    fs, second, new byte[32], pending, MAX_BYTES, 10, 0));
            assertThrows(IOException.class, () -> M3BuildStateStore.readObserved(fs, successful, MAX_BYTES, 10, 0));
            String observedPath = "/m3-build/observed-" + java.util.HexFormat.of().formatHex(successful);
            fs.write(observedPath, 0, new byte[]{0});
            assertThrows(IOException.class, () -> M3BuildStateStore.promoteObserved(
                    fs, successful, pending, MAX_BYTES, 10, 0));
            assertArrayEquals(successful, M3BuildStateStore.read(fs, MAX_BYTES, 10, 0).orElseThrow().commitment());
        }
    }

    @Test void independentObservedProfilesShareOneSuccessfulBuildHead() throws Exception {
        Path database = temp.resolve("profiled-observations.sqlite");
        byte[] maven = sha("maven-4-jdk21-context");
        byte[] gradle = sha("gradle-jdk21-context");
        try (M3FS fs = M3FS.create(database)) {
            fs.mkdir("/project", 0700);
            fs.createFile("/project/A.java", 0600);
            replaceSource(fs, "class A {}");
            M3BuildState mavenInventory = inventory(database, maven);
            M3BuildState gradleInventory = inventory(database, gradle);
            assertTrue(mavenInventory.completeInventory() && gradleInventory.completeInventory());
            byte[] mavenObserved = M3BuildStateStore.observe(
                    fs, mavenInventory, null, null, maven, MAX_BYTES, 10, 0).commitment();
            byte[] gradleObserved = M3BuildStateStore.observe(
                    fs, gradleInventory, null, null, gradle, MAX_BYTES, 10, 0).commitment();
            try (M3FS snapshot = M3FS.openSnapshot(database)) {
                assertArrayEquals(mavenObserved, M3BuildStateStore.readObserved(
                        snapshot, null, maven, MAX_BYTES, 10, 0).orElseThrow().commitment());
                assertArrayEquals(gradleObserved, M3BuildStateStore.readObserved(
                        snapshot, null, gradle, MAX_BYTES, 10, 0).orElseThrow().commitment());
                assertTrue(M3BuildStateStore.readObserved(
                        snapshot, null, MAX_BYTES, 10, 0).isEmpty(),
                        "unprofiled original checkpoint remains untouched");
            }
            assertThrows(IllegalArgumentException.class, () -> M3BuildStateStore.observe(
                    fs, mavenInventory, null, null, sha("foreign"),
                    MAX_BYTES, 10, 0));
            assertThrows(IllegalArgumentException.class, () -> M3BuildStateStore.readObserved(
                    fs, null, new byte[31], MAX_BYTES, 10, 0));
            byte[] successful = M3BuildStateStore.promoteObserved(fs, null,
                    mavenObserved, maven, MAX_BYTES, 10, 0).commitment();
            assertArrayEquals(mavenObserved, successful);
            assertArrayEquals(successful, M3BuildStateStore.read(
                    fs, MAX_BYTES, 10, 0).orElseThrow().commitment());
            // A different profile never authorizes another global success
            // without rebasing on the just-committed successful image.
            assertThrows(IOException.class, () -> M3BuildStateStore.promoteObserved(
                    fs, null, gradleObserved, gradle, MAX_BYTES, 10, 0));
        }
    }

    private record VersionPair(byte[] observed) { }
    private static VersionPair observe(M3FS fs, M3BuildState candidate, byte[] successful,
            byte[] observed) throws IOException {
        return new VersionPair(M3BuildStateStore.observe(fs, candidate, successful, observed,
                MAX_BYTES, 10, 0).commitment());
    }
    private static M3BuildState inventory(Path database) throws IOException {
        return inventory(database, sha("compiler-context"));
    }

    private static M3BuildState inventory(Path database, byte[] context) throws IOException {
        try (M3FS snapshot = M3FS.openSnapshot(database)) {
            return M3BuildState.captureInventory(snapshot, "/project", context,
                    new M3FS.InventoryLimits(1, 8, 10, MAX_BYTES), MAX_BYTES, M3FsProgress.noop());
        }
    }
    private static void replaceSource(M3FS fs, String source) throws IOException {
        fs.truncate("/project/A.java", 0); fs.write("/project/A.java", 0,
                source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        fs.setMetadata("/project/A.java", 0, 123_456_789L, false, true);
    }
    private int compile(String source) throws IOException {
        Path file = temp.resolve("A.java"), output = temp.resolve("classes");
        java.nio.file.Files.writeString(file, source); java.nio.file.Files.createDirectories(output);
        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        assertTrue(compiler != null, "the qualification requires a real JDK compiler");
        java.io.ByteArrayOutputStream diagnostic = new java.io.ByteArrayOutputStream();
        return compiler.run(null, diagnostic, diagnostic, "-proc:none", "-d", output.toString(), file.toString());
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
