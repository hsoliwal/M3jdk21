// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Existing identity, scan-coverage and authoritative observation contracts. */
class M3FileSnapshotContractTest {
    @TempDir
    Path root;

    @Test
    void detachedFacadesKeepOriginalObjectIdentity() {
        M3File first = M3File.of(root);
        M3File second = M3File.of(root);
        assertNotSame(first, second);
        assertEquals(first, first);
        assertNotEquals(first, second);
        assertNotEquals(second, first);
        assertEquals(System.identityHashCode(first), first.hashCode());
        assertEquals(2, new HashSet<>(List.of(first, second)).size());
    }

    @Test
    void unscannedBeforeCannotFabricateAdditions() throws Exception {
        rejectIncomplete(M3FileScanSpec.lazy(0), M3FileScanSpec.EAGER);
    }

    @Test
    void unscannedAfterCannotFabricateRemovals() throws Exception {
        rejectIncomplete(M3FileScanSpec.EAGER, M3FileScanSpec.lazy(0));
    }

    @Test
    void populatedRootDoesNotHideUnscannedDescendants() throws Exception {
        rejectIncomplete(M3FileScanSpec.lazy(1), M3FileScanSpec.EAGER);
    }

    @Test
    void failedRootScanCannotFabricateRemovals() throws Exception {
        rejectFailedScan(root);
    }

    @Test
    void failedDescendantScanCannotHideUnknownChildren() throws Exception {
        rejectFailedScan(root.resolve("dir"));
    }

    @Test
    void fullyExpandedLazySnapshotsRemainComparable() throws Exception {
        fixture();
        try (M3FileTree before = M3FileTree.open(root, M3FileScanSpec.lazy(1));
             M3FileTree after = M3FileTree.open(root, M3FileScanSpec.lazy(1))) {
            expandAll(before);
            expandAll(after);
            before.hash(before.walk(), 1024, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
            int beforeGeneration = before.generation();
            int afterGeneration = after.generation();
            assertTrue(M3FileTreeDelta.between(before, after, 1024, M3FileProgress.NONE,
                    M3FileChangeProvider.ATTRIBUTES).isEmpty());
            assertEquals(beforeGeneration, before.generation());
            assertEquals(afterGeneration, after.generation());
        }
    }

    @Test
    void freshHashWinsOverMatchingAttributeCache() throws Exception {
        Path file = root.resolve("value.txt");
        Files.writeString(file, "old");
        try (M3FileTree before = M3FileTree.open(root)) {
            before.hash(before.walk(), 1024, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
            FileTime time = Files.getLastModifiedTime(file);
            Files.writeString(file, "new");
            Files.setLastModifiedTime(file, time);
            try (M3FileTree after = M3FileTree.open(root)) {
                after.hash(after.walk(), 1024, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
                int beforeId = id(before, file);
                int afterId = id(after, file);
                long[] fresh = sha(after, afterId);
                assertEquals(0, M3FileChangeProvider.ATTRIBUTES.attributeChange(before, beforeId, after, afterId));
                assertFalse(java.util.Arrays.equals(sha(before, beforeId), fresh));
                M3FileTreeDelta delta = M3FileTreeDelta.between(before, after, 1024, M3FileProgress.NONE,
                        M3FileChangeProvider.ATTRIBUTES);
                assertArrayEquals(fresh, sha(after, afterId));
                assertEquals(List.of(file), delta.paths(M3FileChange.CONTENT_CHANGED).toList());
                assertEquals(List.of(file), delta.paths(M3FileChange.TOKENS).toList());
            }
        }
    }

    @Test
    void freshStructureHashIsComparedWhenAttributesMatch() throws Exception {
        Path file = root.resolve("shape.txt");
        Files.writeString(file, "{}[]");
        M3FileChangeProvider provider = new M3FileChangeProvider() {
            @Override
            public Optional<? extends M3FileStructHash> structure(M3File entry) {
                return Optional.of(new BraceCount());
            }
        };
        try (M3FileTree before = M3FileTree.open(root)) {
            before.hash(before.walk(), 1024, M3FileProgress.NONE, provider);
            FileTime time = Files.getLastModifiedTime(file);
            Files.writeString(file, "{{}}");
            Files.setLastModifiedTime(file, time);
            try (M3FileTree after = M3FileTree.open(root)) {
                after.hash(after.walk(), 1024, M3FileProgress.NONE, provider);
                int beforeId = id(before, file);
                int afterId = id(after, file);
                assertEquals(0, provider.attributeChange(before, beforeId, after, afterId));
                assertEquals(1, before.structHash(beforeId));
                assertEquals(2, after.structHash(afterId));
                M3FileTreeDelta delta = M3FileTreeDelta.between(before, after, 1024, M3FileProgress.NONE, provider);
                assertEquals(2, after.structHash(afterId));
                assertTrue(after.flag(M3FileFlag.STRUCT_HASHED, afterId));
                assertEquals(List.of(file), delta.paths(M3FileChange.STRUCTURE).toList());
                assertEquals(List.of(file), delta.paths(M3FileChange.CONTENT_CHANGED).toList());
            }
        }
    }

    @Test
    void failedObservationCannotBeReplacedWithCachedSuccess() throws Exception {
        Path file = root.resolve("value.txt");
        Files.writeString(file, "unchanged");
        try (M3FileTree before = M3FileTree.open(root); M3FileTree after = M3FileTree.open(root)) {
            before.hash(before.walk(), 1024, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
            after.hash(after.walk(), 0, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
            int afterId = id(after, file);
            assertTrue(after.flag(M3FileFlag.FAILED, afterId));
            assertFalse(after.flag(M3FileFlag.HASHED, afterId));
            M3FileTreeDelta delta = M3FileTreeDelta.between(before, after, 0, M3FileProgress.NONE,
                    M3FileChangeProvider.ATTRIBUTES);
            assertTrue(after.flag(M3FileFlag.FAILED, afterId));
            assertFalse(after.flag(M3FileFlag.HASHED, afterId));
            assertFalse(after.flag(M3FileFlag.TOKEN_HASHED, afterId));
            assertFalse(after.flag(M3FileFlag.STRUCT_HASHED, afterId));
            assertEquals(M3FileError.LIMIT, after.error(afterId));
            assertEquals(List.of(file), delta.paths(M3FileChange.UNREADABLE).toList());
        }
    }

    @Test
    void unobservedMatchingAttributesStillReuseTheCacheWithoutIo() throws Exception {
        Path file = root.resolve("value.txt");
        Files.writeString(file, "unchanged");
        try (M3FileTree before = M3FileTree.open(root); M3FileTree after = M3FileTree.open(root)) {
            before.hash(before.walk(), 1024, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
            int beforeId = id(before, file);
            int afterId = id(after, file);
            assertFalse(after.flag(M3FileFlag.HASHED, afterId));
            Files.delete(file);
            M3FileTreeDelta delta = M3FileTreeDelta.between(before, after, 1024, M3FileProgress.NONE,
                    M3FileChangeProvider.ATTRIBUTES);
            assertTrue(delta.isEmpty(), "the existing attribute-only cache policy remains unchanged");
            assertTrue(after.flag(M3FileFlag.HASHED, afterId));
            assertFalse(after.flag(M3FileFlag.FAILED, afterId));
            assertArrayEquals(sha(before, beforeId), sha(after, afterId));
        }
    }

    private void rejectIncomplete(M3FileScanSpec beforeSpec, M3FileScanSpec afterSpec) throws Exception {
        fixture();
        try (M3FileTree before = M3FileTree.open(root, beforeSpec);
             M3FileTree after = M3FileTree.open(root, afterSpec)) {
            assertIncomplete(before, after);
        }
    }

    /** Requires a POSIX provider where empty permissions deny directory listing to this process. */
    private void rejectFailedScan(Path denied) throws Exception {
        assumeTrue(Files.getFileAttributeView(root, PosixFileAttributeView.class) != null,
                "POSIX permissions are required for this real access-denied fixture");
        fixture();
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(denied);
        try (M3FileTree before = M3FileTree.open(root)) {
            M3FileTree after;
            Files.setPosixFilePermissions(denied, Set.of());
            try {
                after = M3FileTree.open(root);
            } finally {
                Files.setPosixFilePermissions(denied, permissions);
            }
            try (after) {
                int deniedId = id(after, denied);
                assumeTrue(after.flag(M3FileFlag.FAILED, deniedId)
                                && after.error(deniedId) == M3FileError.ACCESS_DENIED,
                        "this process/provider bypasses denied POSIX directory permissions");
                assertEquals(M3FileKind.OTHER, after.kind(deniedId));
                assertFalse(after.flag(M3FileFlag.SCANNED, deniedId));
                assertEquals("also unchanged", Files.readString(root.resolve("dir/nested.txt")));
                assertIncomplete(before, after);
            }
        }
    }

    private static void assertIncomplete(M3FileTree before, M3FileTree after) {
        int beforeRows = before.rows();
        int afterRows = after.rows();
        int beforeGeneration = before.generation();
        int afterGeneration = after.generation();
        int[] providerCalls = {0};
        M3FileChangeProvider provider = new M3FileChangeProvider() {
            @Override
            public int attributeChange(M3FileTree b, int bi, M3FileTree a, int ai) {
                providerCalls[0]++;
                return M3FileChangeProvider.super.attributeChange(b, bi, a, ai);
            }
        };
        CountingProgress progress = new CountingProgress();
        assertThrows(IllegalArgumentException.class,
                () -> M3FileTreeDelta.between(before, after, 1024, progress, provider));
        assertEquals(0, providerCalls[0], "refuse before provider callbacks");
        assertEquals(0, progress.calls, "refuse before progress callbacks");
        assertEquals(beforeRows, before.rows());
        assertEquals(afterRows, after.rows());
        assertEquals(beforeGeneration, before.generation());
        assertEquals(afterGeneration, after.generation());
        assertEquals(0, after.ids().filter(i -> after.flag(M3FileFlag.HASHED, i)).count());
    }

    private void fixture() throws Exception {
        Files.writeString(root.resolve("keep.txt"), "unchanged");
        Files.createDirectory(root.resolve("dir"));
        Files.writeString(root.resolve("dir/nested.txt"), "also unchanged");
    }

    private static void expandAll(M3FileTree tree) throws Exception {
        for (int id = 0; id < tree.rows(); id++) {
            if (tree.flag(M3FileFlag.DIR, id) && !tree.flag(M3FileFlag.SCANNED, id)) tree.scanChildren(id);
        }
    }

    private static int id(M3FileTree tree, Path file) {
        return tree.ids().filter(row -> tree.path(row).equals(file)).findFirst().orElseThrow();
    }

    private static long[] sha(M3FileTree tree, int id) {
        return new long[] {tree.shaWord(id, 0), tree.shaWord(id, 1), tree.shaWord(id, 2), tree.shaWord(id, 3)};
    }

    private static final class BraceCount implements M3FileStructHash {
        private long count;

        @Override
        public void update(char[] chars, int offset, int length) {
            for (int i = offset; i < offset + length; i++) if (chars[i] == '{') count++;
        }

        @Override
        public long structHash() {
            return count;
        }
    }

    private static final class CountingProgress implements M3FileProgress {
        private int calls;

        @Override
        public void worked(long units) {
            calls++;
        }

        @Override
        public void begin(String task, long totalUnits) {
            calls++;
        }

        @Override
        public void done() {
            calls++;
        }

        @Override
        public M3FileProgress split(long parentUnits) {
            calls++;
            return M3FileProgress.super.split(parentUnits);
        }
    }
}
