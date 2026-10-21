/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.m3.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Actual SQLite/JNI inventory proof, independent of Maven watch or compiler scheduling. */
class M3FSInventoryTest {
    private static final int MAX_BYTES = 4_000_000;
    private static final byte[] CONTEXT = sha("inventory-test-context");
    private static final M3FS.InventoryLimits LIMITS = new M3FS.InventoryLimits(1, 32, 100, MAX_BYTES);
    @TempDir Path temp;

    @Test void pinnedNativeViewHashesEachAdmittedByteOnceWithExactNanos() throws Exception {
        Path database = temp.resolve("snapshot.sqlite");
        byte[] large = new byte[150_003];
        for (int i = 0; i < large.length; i++) large[i] = (byte) (i * 37);
        try (M3FS live = M3FS.create(database)) {
            live.mkdir("/project", 0700); live.mkdir("/project/nested", 0700);
            put(live, "/project/a", large, 1_000_000_007L);
            put(live, "/project/nested/b", new byte[0], 2_000_000_009L);
            try (M3FS snapshot = M3FS.openSnapshot(database)) {
                live.write("/project/a", 0, new byte[]{99});
                live.unlink("/project/nested/b");
                AtomicLong consumed = new AtomicLong();
                List<String> paths = new ArrayList<>();
                M3FS.InventoryReceipt complete = snapshot.scanInventory("/project", LIMITS,
                        M3FsProgress.of(() -> false, consumed::addAndGet), (path, stat, capture) -> {
                            paths.add(path);
                            assertEquals(stat.size(), capture.manifest().raw().size());
                            if (path.equals("a")) {
                                assertEquals(1_000_000_007L, stat.modifiedTimeNanos());
                                assertArrayEquals(sha(large), capture.manifest().raw().id().bytes());
                            }
                        });
                assertEquals(List.of("a", "nested/b"), paths);
                assertEquals(3, complete.entries()); assertEquals(2, complete.files());
                assertEquals(large.length, complete.bytes()); assertEquals(large.length, consumed.get());
                M3BuildState state = M3BuildState.captureInventory(snapshot, "/project", CONTEXT, LIMITS,
                        MAX_BYTES, M3FsProgress.noop());
                assertEquals(2, state.size()); assertEquals(1_000_000_007L, state.modifiedTimeNanos(0).orElseThrow());
                assertFalse(state.hasAbi(0)); assertEquals(M3BuildState.BUILD_INPUT, state.role(0));
                assertEquals(0, state.dependencyCount());
                M3BuildState decoded = M3BuildState.decode(state.encode(MAX_BYTES), MAX_BYTES, 100, 0);
                assertTrue(decoded.completeInventory());
                assertArrayEquals(state.encode(MAX_BYTES), decoded.encode(MAX_BYTES));
                assertEquals(0, decoded.deltaFrom(state, true).changedCount());
                assertFalse(decoded.deltaFrom(state, true).needsPlanning());
            }
            assertEquals(1, capture(database).size());
        }
    }

    @Test void sameSizeRestoredTimeMutationTouchAdditionAndDeletionHaveExactDeltas() throws Exception {
        Path database = temp.resolve("delta.sqlite");
        try (M3FS live = M3FS.create(database)) {
            live.mkdir("/project", 0700); put(live, "/project/a", bytes("abc"), 123_456_789L);
            M3BuildState first = capture(database);
            live.write("/project/a", 0, bytes("xyz")); live.setMetadata("/project/a", 0, 123_456_789L, false, true);
            M3BuildState second = capture(database);
            assertTrue(second.deltaFrom(first, false).modified(0));
            assertTrue(second.deltaFrom(first, false).needsPlanning());
            assertTrue(second.changedFrom(first).fullRebuild());
            live.setMetadata("/project/a", 0, 123_456_790L, false, true);
            M3BuildState touch = capture(database);
            assertEquals(0, touch.deltaFrom(second, false).changedCount());
            assertTrue(touch.deltaFrom(second, true).modified(0));
            put(live, "/project/b", bytes("new"), 9);
            live.unlink("/project/a");
            M3BuildState changed = capture(database);
            M3BuildState.InventoryDelta delta = changed.deltaFrom(touch, true);
            assertTrue(delta.comparable()); assertTrue(delta.added(0)); assertTrue(delta.removed(0));
            assertEquals(2, delta.changedCount());
            long[] detached = delta.removedMask(); detached[0] = 0;
            assertTrue(delta.removed(0));
            live.unlink("/project/b");
            M3BuildState empty = capture(database);
            assertEquals(0, empty.size()); assertTrue(empty.deltaFrom(changed, true).removed(0));
        }
    }

    @Test void utf8PageCursorAndUtf16StateOrderingStayDistinctAndMembershipIsUnique() throws Exception {
        Path database = temp.resolve("unicode.sqlite");
        try (M3FS live = M3FS.create(database)) {
            live.mkdir("/project", 0700);
            put(live, "/project/\ud800\udc00", bytes("supplementary"), 1);
            put(live, "/project/\ue000", bytes("bmp"), 2);
            assertThrows(IOException.class, () -> live.createFile("/project/\ue000", 0600));
            try (M3FS snapshot = M3FS.openSnapshot(database)) {
                List<String> encountered = new ArrayList<>();
                M3FS.InventoryReceipt complete = snapshot.scanInventory("/project", LIMITS, M3FsProgress.noop(),
                        (path, stat, capture) -> encountered.add(path));
                assertEquals(List.of("\ue000", "\ud800\udc00"), encountered);
                assertEquals(2, complete.files());
                M3BuildState state = M3BuildState.captureInventory(snapshot, "/project", CONTEXT, LIMITS,
                        MAX_BYTES, M3FsProgress.noop());
                assertEquals("\ud800\udc00", state.path(0)); assertEquals("\ue000", state.path(1));
            }
        }
    }

    @Test void incompleteScansHaveNoReceiptAndCannotPublishDeletion() throws Exception {
        Path database = temp.resolve("failures.sqlite");
        try (M3FS live = M3FS.create(database)) {
            live.mkdir("/project", 0700); put(live, "/project/a", new byte[100_000], 1);
            M3BuildState first = capture(database);
            byte[] successful = M3BuildStateStore.publish(live, first, null, MAX_BYTES, 100, 0).commitment();
            assertThrows(IOException.class, () -> live.scanInventory("/project", LIMITS,
                    M3FsProgress.noop(), (p, s, c) -> {}));
            try (M3FS snapshot = M3FS.openSnapshot(database)) {
                assertThrows(IOException.class, () -> snapshot.scanInventory("/project", LIMITS,
                        M3FsProgress.noop(), (p, s, c) -> { throw new IOException("sink refused"); }));
                for (M3FS.InventoryLimits limit : List.of(new M3FS.InventoryLimits(1, 0, 10, MAX_BYTES),
                        new M3FS.InventoryLimits(1, 10, 0, MAX_BYTES), new M3FS.InventoryLimits(1, 10, 10, 99_999))) {
                    assertThrows(IOException.class, () -> snapshot.scanInventory("/project", limit,
                            M3FsProgress.noop(), (p, s, c) -> {}));
                }
                assertThrows(IOException.class, () -> M3BuildState.captureInventory(snapshot, "/project", CONTEXT,
                        LIMITS, 116, M3FsProgress.noop()));
                AtomicLong consumed = new AtomicLong();
                assertThrows(CancellationException.class, () -> snapshot.scanInventory("/project", LIMITS,
                        M3FsProgress.of(() -> consumed.get() > 0, consumed::addAndGet), (p, s, c) -> fail("partial capture")));
                assertEquals(65_536L, consumed.get());
            }
            M3FS interrupted = M3FS.openSnapshot(database);
            try {
                assertThrows(IOException.class, () -> interrupted.scanInventory("/project", LIMITS,
                        M3FsProgress.noop(), (p, s, c) -> interrupted.close()));
            } finally {
                interrupted.close();
            }
            assertArrayEquals(successful, M3BuildStateStore.read(live, MAX_BYTES, 100, 0).orElseThrow().commitment());
            try (M3FS snapshot = M3FS.openSnapshot(database)) {
                assertTrue(M3BuildStateStore.readObserved(snapshot, successful, MAX_BYTES, 100, 0).isEmpty());
            }
        }
    }

    @Test void emptyScopeIsCompleteButAliasesSymlinksAndLedgerScopeAreRefused() throws Exception {
        Path database = temp.resolve("scope.sqlite");
        try (M3FS live = M3FS.create(database)) {
            live.mkdir("/project", 0700); live.mkdir("/other", 0700);
            M3BuildState empty = capture(database); assertTrue(empty.completeInventory()); assertEquals(0, empty.size());
            assertTrue(empty.deltaFrom(null, true).needsPlanning());
            try (M3FS snapshot = M3FS.openSnapshot(database)) {
                for (String root : List.of("project", "/project/", "/project/.", "/project//x", "/project/../other")) {
                    assertThrows(IOException.class, () -> snapshot.scanInventory(root, LIMITS,
                            M3FsProgress.noop(), (p, s, c) -> fail()));
                }
                assertThrows(IOException.class, () -> M3BuildState.captureInventory(snapshot, "/", CONTEXT,
                        LIMITS, MAX_BYTES, M3FsProgress.noop()));
                assertThrows(IOException.class, () -> M3BuildState.captureInventory(snapshot, "/m3-build", CONTEXT,
                        LIMITS, MAX_BYTES, M3FsProgress.noop()));
                M3BuildState other = M3BuildState.captureInventory(snapshot, "/other", CONTEXT,
                        LIMITS, MAX_BYTES, M3FsProgress.noop());
                assertFalse(other.deltaFrom(empty, true).comparable());
                assertEquals(0, other.deltaFrom(empty, true).changedCount());
                M3BuildState contextChanged = M3BuildState.captureInventory(snapshot, "/project", sha("different"),
                        LIMITS, MAX_BYTES, M3FsProgress.noop());
                assertFalse(contextChanged.deltaFrom(empty, true).comparable());
            }
            live.symlink("/other", "/project/link");
            assertThrows(IOException.class, () -> capture(database));
            live.unlink("/project/link"); put(live, "/project/back\\slash", bytes("a"), 1);
            assertThrows(IOException.class, () -> capture(database));
        }
    }

    @Test void legacyTimestampIsUnknownAndKnowledgeAloneDoesNotMakeATouch() throws Exception {
        Path database = temp.resolve("knowledge.sqlite");
        try (M3FS live = M3FS.create(database)) {
            live.mkdir("/project", 0700); put(live, "/project/a", bytes("abc"), 17);
            M3BuildState state = capture(database);
            byte[] image = state.encode(MAX_BYTES);
            assertEquals(2, ByteBuffer.wrap(image).getInt(4));
            // V2 tail is scope[32], nanos[rows], known-mask[words], checksum[32].
            ByteBuffer.wrap(image).putLong(image.length - 48, 0).putLong(image.length - 40, 0);
            reseal(image);
            M3BuildState unknown = M3BuildState.decode(image, MAX_BYTES, 100, 0);
            assertFalse(unknown.hasModifiedTime(0)); assertTrue(unknown.modifiedTimeNanos(0).isEmpty());
            assertEquals(0, state.deltaFrom(unknown, true).changedCount());
            M3BuildState legacy = new M3BuildState(new String[]{"a"}, new byte[]{M3BuildState.BUILD_INPUT},
                    new long[]{3}, sha("abc"), new byte[32], new long[1], new int[2], new int[0], CONTEXT);
            assertEquals(1, ByteBuffer.wrap(legacy.encode(MAX_BYTES)).getInt(4));
            M3BuildState v1 = M3BuildState.decode(legacy.encode(MAX_BYTES), MAX_BYTES, 100, 0);
            assertFalse(v1.completeInventory()); assertTrue(v1.modifiedTimeNanos(0).isEmpty());
            assertFalse(state.deltaFrom(v1, true).comparable());
            assertThrows(IllegalStateException.class, () -> v1.deltaFrom(state, true));
            assertThrows(IOException.class, () -> M3BuildStateStore.observe(live, v1, null, null, MAX_BYTES, 100, 0));
        }
    }

    @Test void callbackMutationCannotChangeTheRequestedBuildContext() throws Exception {
        Path database = temp.resolve("context.sqlite");
        byte[] supplied = sha("original context"), expected = supplied.clone();
        try (M3FS fs = M3FS.create(database)) {
            fs.mkdir("/project", 0700); put(fs, "/project/a", bytes("payload"), 1);
            try (M3FS snapshot = M3FS.openSnapshot(database)) {
                M3BuildState state = M3BuildState.captureInventory(snapshot, "/project", supplied, LIMITS,
                        MAX_BYTES, M3FsProgress.of(() -> false, count -> Arrays.fill(supplied, (byte) 0)));
                assertArrayEquals(expected, state.contextSha256());
                assertFalse(Arrays.equals(expected, supplied));
            }
        }
    }

    @Test void containerAndRootBoundaryPunctuationCannotAliasScopes() throws Exception {
        Path firstDatabase = temp.resolve("db");
        Path secondDatabase = temp.resolve("db!/x");
        java.nio.file.Files.createDirectories(secondDatabase.getParent());
        try (M3FS first = M3FS.create(firstDatabase); M3FS second = M3FS.create(secondDatabase)) {
            first.mkdir("/x!", 0700); first.mkdir("/x!/project", 0700);
            second.mkdir("/project", 0700);
            put(first, "/x!/project/a", bytes("one"), 1);
            put(second, "/project/a", bytes("two"), 1);
            try (M3FS left = M3FS.openSnapshot(firstDatabase); M3FS right = M3FS.openSnapshot(secondDatabase)) {
                M3BuildState a = M3BuildState.captureInventory(left, "/x!/project", CONTEXT, LIMITS,
                        MAX_BYTES, M3FsProgress.noop());
                M3BuildState b = M3BuildState.captureInventory(right, "/project", CONTEXT, LIMITS,
                        MAX_BYTES, M3FsProgress.noop());
                assertFalse(b.deltaFrom(a, true).comparable());
                assertEquals(0, b.deltaFrom(a, true).changedCount());
            }
        }
    }

    @Test void actualNativeReadCorruptionCannotCompleteAnInventory() throws Exception {
        Path database = temp.resolve("corrupt.sqlite");
        try (M3FS live = M3FS.create(database)) {
            live.mkdir("/project", 0700); put(live, "/project/a", bytes("payload"), 1); live.checkpoint();
        }
        // Deliberately corrupt an isolated fixture through the real SQLite test driver.
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                var sql = connection.createStatement()) {
            sql.execute("PRAGMA ignore_check_constraints=ON");
            assertEquals(1, sql.executeUpdate("UPDATE blocks SET data=zeroblob(1)"));
        }
        assertThrows(IOException.class, () -> capture(database));
    }

    @Test void finiteCreationPermutationsProduceTheSameSortedStateImage() throws Exception {
        Path database = temp.resolve("permutation.sqlite");
        try (M3FS live = M3FS.create(database)) {
            live.mkdir("/project", 0700);
            byte[] previous = null;
            for (List<String> order : List.of(List.of("c", "a", "b"), List.of("b", "c", "a"), List.of("a", "b", "c"))) {
                for (String name : order) put(live, "/project/" + name, bytes(name), name.charAt(0));
                byte[] image = capture(database).encode(MAX_BYTES);
                if (previous != null) assertArrayEquals(previous, image);
                previous = image;
                for (String name : order) live.unlink("/project/" + name);
            }
        }
    }

    private M3BuildState capture(Path database) throws IOException {
        try (M3FS snapshot = M3FS.openSnapshot(database)) {
            return M3BuildState.captureInventory(snapshot, "/project", CONTEXT, LIMITS, MAX_BYTES, M3FsProgress.noop());
        }
    }
    private static void put(M3FS fs, String path, byte[] bytes, long time) throws IOException {
        fs.createFile(path, 0600); fs.write(path, 0, bytes); fs.setMetadata(path, 0, time, false, true);
    }
    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    private static byte[] sha(String text) { return sha(bytes(text)); }
    private static byte[] sha(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void reseal(byte[] image) {
        byte[] seal = sha(Arrays.copyOf(image, image.length - 32));
        System.arraycopy(seal, 0, image, image.length - 32, 32);
    }
}
