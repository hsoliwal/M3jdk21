// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import com.m3.file.M3File;
import com.m3.file.M3FileProgress;
import com.m3.file.M3FileShadow;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Actual M3File traversal + original project-local native SQLite M3FS custody. */
class M3OfflineProjectScanTest {
    private static final M3OfflineProjectScan.Limits LIMITS =
            new M3OfflineProjectScan.Limits(128, 1_000_000, 100_000);
    @TempDir Path temporary;

    @Test
    void offlineColdWarmModificationDeletionAndPersistentDatabase() throws Exception {
        Path project = temporary.resolve("offline");
        Files.createDirectories(project.resolve("src"));
        Files.createDirectories(project.resolve(".m3"));
        Files.createDirectories(project.resolve(".git"));
        Path java = project.resolve("src/App.java");
        Files.writeString(java, "abc", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("pom.xml"), "<project/>", StandardCharsets.UTF_8);
        Files.writeString(project.resolve(".git/HEAD"), "changed", StandardCharsets.UTF_8);
        Path database = project.resolve(".m3/project.sqlite");
        AtomicLong shadowBytes = new AtomicLong();
        AtomicLong progressBytes = new AtomicLong();
        M3OfflineProjectScan.ShadowLayers shadows = file ->
                new M3FileShadow[]{(buffer, offset, length) -> shadowBytes.addAndGet(length)};
        byte[] context = sha("maven:21:compiler:flags");
        try (M3FS fs = M3FS.create(database)) {
            var first = M3OfflineProjectScan.scan(project, fs, context, "all-inputs-v1",
                    path -> true, shadows, LIMITS,
                    M3FileProgress.of(progressBytes::addAndGet, () -> false));
            assertEquals(2, first.state().size());
            assertEquals(2, first.changedRows().count());
            assertTrue(first.delta().fullRebuild());
            assertTrue(first.needsCompilerImpactAnalysis());
            assertEquals(13, first.observedBytes());
            assertEquals(first.observedBytes(), shadowBytes.get());
            assertEquals(first.observedBytes(), progressBytes.get());
            assertTrue(M3BuildStateStore.read(fs, LIMITS.maxStateBytes(), 128, 0).isEmpty(),
                    "original attributed compiler state stays independent");
            var published = M3OfflineProjectScan.publish(fs, first, LIMITS);
            assertArrayEquals(published.commitment(), M3BuildStateStore.readOffline(
                    fs, LIMITS.maxStateBytes(), LIMITS.maxRows()).orElseThrow().commitment());

            var warm = M3OfflineProjectScan.scan(project, fs, context, "all-inputs-v1",
                    path -> true, file -> new M3FileShadow[0], LIMITS, M3FileProgress.NONE);
            assertFalse(warm.needsCompilerImpactAnalysis());
            assertEquals(0, warm.changedRows().count());
            assertEquals(0, warm.removedPaths().length);

            FileTime timestamp = Files.getLastModifiedTime(java);
            Files.writeString(java, "xyz", StandardCharsets.UTF_8);
            Files.setLastModifiedTime(java, timestamp); // same length + same timestamp
            var modified = M3OfflineProjectScan.scan(project, fs, context, "all-inputs-v1",
                    path -> true, file -> new M3FileShadow[0], LIMITS, M3FileProgress.NONE);
            assertEquals(1, modified.changedRows().count(),
                    "full content SHA identifies no-watch edits with unchanged stat hints");
            assertTrue(modified.delta().changed(
                    modified.state().find("src/App.java", M3BuildState.SOURCE)));
            M3OfflineProjectScan.publish(fs, modified, LIMITS);

            Files.delete(java);
            Files.writeString(project.resolve("src/New.java"), "new", StandardCharsets.UTF_8);
            var removed = M3OfflineProjectScan.scan(project, fs, context, "all-inputs-v1",
                    path -> true, file -> new M3FileShadow[0], LIMITS, M3FileProgress.NONE);
            assertTrue(removed.delta().fullRebuild(),
                    "unknown compiler ABI and removed source conservatively invalidate the graph");
            assertArrayEquals(new String[]{"src/App.java"}, removed.removedPaths());
            // A removal invalidates the entire previous resolver inventory;
            // the original M3BuildState intentionally marks all surviving rows dirty.
            assertEquals(2, removed.changedRows().count());
            M3OfflineProjectScan.publish(fs, removed, LIMITS);

            var newContext = M3OfflineProjectScan.scan(project, fs,
                    sha("maven:21:changed-flags"), "all-inputs-v1",
                    path -> true, file -> new M3FileShadow[0], LIMITS, M3FileProgress.NONE);
            assertTrue(newContext.delta().fullRebuild());
            var changedSelection = M3OfflineProjectScan.scan(project, fs,
                    context, "source-only-v1", path -> path.endsWith(".java"),
                    file -> new M3FileShadow[0], LIMITS, M3FileProgress.NONE);
            assertTrue(changedSelection.delta().fullRebuild());
            assertEquals(1, changedSelection.state().size());
        }
        try (M3FS reopened = M3FS.openSnapshot(database)) {
            var generation = M3BuildStateStore.readOffline(
                    reopened, LIMITS.maxStateBytes(), LIMITS.maxRows()).orElseThrow();
            assertTrue(generation.state().find("src/New.java", M3BuildState.SOURCE) >= 0);
            assertEquals(-1, generation.state().find("src/App.java", M3BuildState.SOURCE));
            assertTrue(M3BuildStateStore.read(reopened, LIMITS.maxStateBytes(), 128, 0).isEmpty());
        }
    }

    @Test
    void streamReusedWithoutWatcherAndFailuresCannotPromoteStaleState() throws Exception {
        Path project = temporary.resolve("stream");
        Files.createDirectories(project);
        Path file = project.resolve("input.txt");
        Files.writeString(file, "stream", StandardCharsets.UTF_8);
        Path database = temporary.resolve("stream.sqlite");
        byte[] context = sha("tool-context");
        try (M3FS fs = M3FS.create(database)) {
            var input = Stream.of(M3File.of(file));
            var prepared = M3OfflineProjectScan.capture(project, fs, input, context, "manual-file-stream-v1",
                    name -> true, original -> new M3FileShadow[0], LIMITS, M3FileProgress.NONE);
            assertEquals(1, prepared.state().size());
            assertEquals(6, prepared.observedBytes());
            var first = M3OfflineProjectScan.publish(fs, prepared, LIMITS);
            var stale = M3OfflineProjectScan.capture(project, fs, Stream.of(M3File.of(file)),
                    context, "manual-file-stream-v1", name -> true,
                    original -> new M3FileShadow[0], LIMITS, M3FileProgress.NONE);
            Files.writeString(file, "changed", StandardCharsets.UTF_8);
            var newest = M3OfflineProjectScan.capture(project, fs, Stream.of(M3File.of(file)),
                    context, "manual-file-stream-v1", name -> true,
                    original -> new M3FileShadow[0], LIMITS, M3FileProgress.NONE);
            M3OfflineProjectScan.publish(fs, newest, LIMITS);
            assertThrows(IOException.class, () -> M3OfflineProjectScan.publish(fs, stale, LIMITS));
            assertFalse(Arrays.equals(new byte[32], first.commitment()));

            assertThrows(IOException.class, () -> M3OfflineProjectScan.capture(project, fs,
                    Stream.of(M3File.of(file), M3File.of(file)), context, "duplicates-v1",
                    name -> true, original -> new M3FileShadow[0],
                    LIMITS, M3FileProgress.NONE));
            assertThrows(IOException.class, () -> M3OfflineProjectScan.capture(project, fs,
                    Stream.of(M3File.of(temporary.resolve("outsider.txt"))),
                    context, "escape-v1", name -> true,
                    original -> new M3FileShadow[0], LIMITS, M3FileProgress.NONE));
            try (M3FS anotherProject = M3FS.create(temporary.resolve("different.sqlite"))) {
                assertThrows(IOException.class, () -> M3OfflineProjectScan.publish(
                        anotherProject, newest, LIMITS));
            }
        }
    }

    private static byte[] sha(String value) throws Exception {
        return MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
    }
}
