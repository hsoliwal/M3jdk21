// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import com.m3.file.M3File;
import com.m3.file.M3StreamProgress;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Original host M3File stream -> completed V2 M3BuildState -> native SQLite observation. */
class M3HostFileInventoryTest {
    @TempDir Path temporary;
    private static final M3HostFileInventory.Limits LIMITS =
            new M3HostFileInventory.Limits(100, 1_000_000, 100_000, 1_000_000);

    private static byte[] hash(String value) throws Exception {
        return MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
    }

    private static M3HostFileInventory.Result capture(Path root, String selection,
            Predicate<String> filter, byte[] context, M3BuildState prior,
            M3HostFileInventory.ByteLayer layer, M3StreamProgress progress) throws IOException {
        // Caller owns enumeration; the SDK consumes the original M3File stream.
        try (Stream<Path> paths = Files.walk(root)) {
            return M3HostFileInventory.capture(root, paths.map(M3File::of), selection,
                    filter, context, prior, LIMITS, progress, layer);
        }
    }

    @Test
    void completeHostRowsAndDeltaCoverOfflineMutations() throws Exception {
        Path root = temporary.resolve("reactor");
        Files.createDirectories(root.resolve("module/src"));
        Files.createDirectories(root.resolve(".git"));
        Files.createDirectories(root.resolve(".m3"));
        Path java = root.resolve("module/src/App.java");
        Files.writeString(java, "class App { int x=1; }", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("pom.xml"), "root", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("module/pom.xml"), "child", StandardCharsets.UTF_8);
        Files.writeString(root.resolve(".git/HEAD"), "transient", StandardCharsets.UTF_8);
        Files.writeString(root.resolve(".m3/tmp.txt"), "index", StandardCharsets.UTF_8);
        byte[] context = hash("JDK21_Maven_context");
        AtomicLong seen = new AtomicLong(), reported = new AtomicLong();
        var observed = capture(root, "project-all-v1", name -> true, context,
                null, (source, bytes, offset, count) -> {
                    seen.addAndGet(count);
                }, reported::addAndGet);
        M3BuildState first = observed.inventory();
        assertTrue(first.completeInventory());
        assertEquals(3, first.size(), "control folders are excluded");
        assertEquals(first.size(), observed.delta().changedCount());
        assertTrue(observed.delta().initial());
        assertEquals(first.size(), observed.addedRows().count());
        assertEquals(observed.observedBytes(), seen.get(), "one shared byte observation");
        assertEquals(observed.observedBytes(), reported.get(), "byte progress matches shadows");
        assertEquals(M3BuildState.BUILD_INPUT,
                first.role(first.find("module/pom.xml", M3BuildState.BUILD_INPUT)));
        assertEquals(M3BuildState.SOURCE,
                first.role(first.find("module/src/App.java", M3BuildState.SOURCE)));
        assertArrayEquals(hash("class App { int x=1; }"),
                first.contentSha256(first.find("module/src/App.java", M3BuildState.SOURCE)));
        assertEquals(0, first.dependencyCount());
        for (int row = 0; row < first.size(); row++) assertFalse(first.hasAbi(row));
        byte[] encoded = first.encode(LIMITS.maxStateBytes());
        var decoded = M3BuildState.decode(encoded, LIMITS.maxStateBytes(), 100, 0);
        assertTrue(decoded.completeInventory());
        assertArrayEquals(encoded, decoded.encode(LIMITS.maxStateBytes()));

        var warm = capture(root, "project-all-v1", name -> true, context,
                decoded, (file, bytes, offset, count) -> { }, M3StreamProgress.none());
        assertEquals(0, warm.delta().changedCount());
        assertFalse(warm.needsImpactAnalysis());

        FileTime stamp = Files.getLastModifiedTime(java);
        Files.writeString(java, "class App { int x=2; }", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(java, stamp); // stat metadata deliberately unchanged
        var changed = capture(root, "project-all-v1", name -> true, context,
                warm.inventory(), (file, bytes, offset, count) -> { },
                M3StreamProgress.none());
        assertEquals(1, changed.delta().changedCount());
        assertEquals(1, changed.modifiedRows().count());
        assertTrue(changed.delta().modified(
                changed.inventory().find("module/src/App.java", M3BuildState.SOURCE)));

        Files.delete(java);
        Files.writeString(root.resolve("module/src/New.java"), "class New {}", StandardCharsets.UTF_8);
        var renamed = capture(root, "project-all-v1", name -> true, context,
                changed.inventory(), (file, bytes, offset, count) -> { },
                M3StreamProgress.none());
        assertTrue(renamed.delta().comparable());
        assertEquals(2, renamed.delta().changedCount());
        assertArrayEquals(new String[]{"module/src/App.java"}, renamed.removedPaths());
        assertEquals(1, renamed.addedRows().count());
        var newScope = capture(root, "source-only-v1", key -> key.endsWith(".java"),
                context, renamed.inventory(), (file, bytes, offset, count) -> { },
                M3StreamProgress.none());
        assertFalse(newScope.delta().comparable(), "policy change invalidates prior scope");
        assertEquals(0, newScope.removedPaths().length, "not comparable does not invent deletions");
        assertEquals(1, newScope.inventory().size());
    }

    @Test
    void failuresNeverIssueACompletedInventory() throws Exception {
        Path root = temporary.resolve("validation");
        Files.createDirectories(root);
        Path source = root.resolve("A.java");
        Files.writeString(source, "class A {}", StandardCharsets.UTF_8);
        byte[] context = hash("flags");
        M3HostFileInventory.ByteLayer none = (file, bytes, offset, count) -> { };
        assertThrows(IOException.class, () -> M3HostFileInventory.capture(
                root, Stream.of(M3File.of(source), M3File.of(source)),
                "duplicates", name -> true, context, null, LIMITS,
                M3StreamProgress.none(), none));
        assertThrows(IOException.class, () -> M3HostFileInventory.capture(
                root, Stream.of(M3File.of(temporary.resolve("outside.java"))),
                "escape", name -> true, context, null, LIMITS,
                M3StreamProgress.none(), none));
        assertThrows(IOException.class, () -> M3HostFileInventory.capture(
                root, Stream.of(M3File.of(source)), "shadow-failure",
                name -> true, context, null, LIMITS, M3StreamProgress.none(),
                (file, bytes, offset, count) -> { throw new IOException("failed parser"); }));
        assertThrows(IOException.class, () -> M3HostFileInventory.capture(
                root, Stream.of(M3File.of(source)), "budget", name -> true,
                context, null, new M3HostFileInventory.Limits(1, 1_000_000, 2, 2),
                M3StreamProgress.none(), none));
        assertThrows(IllegalArgumentException.class, () -> M3HostFileInventory.capture(
                root, Stream.of(M3File.of(source)), "bad-profile", name -> true,
                new byte[31], null, LIMITS, M3StreamProgress.none(), none));
    }

    @Test
    void sqliteStagesObservationButDoesNotClaimSuccessfulBuild() throws Exception {
        Path root = temporary.resolve("native-project");
        Files.createDirectories(root.resolve(".m3"));
        Path source = root.resolve("input.java");
        Files.writeString(source, "class Native {}", StandardCharsets.UTF_8);
        var completed = capture(root, "maven-native-v1", key -> true, hash("maven"),
                null, (file, bytes, offset, count) -> { }, M3StreamProgress.none());
        Path db = root.resolve(".m3/project.sqlite");
        try (M3FS fs = M3FS.create(db)) {
            var staged = M3HostFileInventory.stage(fs, completed, null, null,
                    LIMITS.maxStateBytes(), LIMITS.maxRows());
            assertTrue(M3BuildStateStore.read(fs, LIMITS.maxStateBytes(),
                    LIMITS.maxRows(), 0).isEmpty(), "observation is NOT a successful build");
            try (M3FS pinned = M3FS.openSnapshot(db)) {
                var original = M3BuildStateStore.readObserved(pinned, null,
                        completed.inventory().contextSha256(),
                        LIMITS.maxStateBytes(), LIMITS.maxRows(), 0).orElseThrow();
                assertArrayEquals(staged.commitment(), original.commitment());
            }
            // The caller explicitly attests successful work before promotion.
            var promoted = M3BuildStateStore.promoteObserved(fs, null,
                    staged.commitment(), completed.inventory().contextSha256(),
                    LIMITS.maxStateBytes(), LIMITS.maxRows(), 0);
            assertArrayEquals(staged.commitment(), promoted.commitment());
            assertEquals(1, promoted.state().size());
        }
    }
}
