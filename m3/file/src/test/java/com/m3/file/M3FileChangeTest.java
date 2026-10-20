// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32C;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** S5 contract: hash lanes, attribute gate, change providers. */
class M3FileChangeTest {
    @TempDir
    Path root;

    @BeforeEach
    void fixture() throws IOException {
        Files.writeString(root.resolve("A.java"), "class A { int x = 1; }");
        Files.writeString(root.resolve("touch.txt"), "same bytes");
        Files.writeString(root.resolve("grow.txt"), "abc");
        Files.writeString(root.resolve("space.txt"), "a b");
        Files.writeString(root.resolve("keep.txt"), "untouched");
        Files.write(root.resolve("blob.bin"), new byte[] {1, 0, 2, 0, 3});
        Files.createDirectories(root.resolve("dir"));
    }

    @Test
    void hashLanesEqualJdkDigests() throws Exception {
        M3FileTree tree = M3FileTree.open(root);
        AtomicLong worked = new AtomicLong();
        assertEquals(6, tree.hash(tree.walk(), 1 << 20, M3FileProgress.of(worked::addAndGet, () -> false),
                M3FileChangeProvider.ATTRIBUTES));
        long total = 0;
        for (int id = 0; id < tree.rows(); id++) {
            if (!tree.flag(M3FileFlag.REGULAR, id)) {
                assertFalse(tree.flag(M3FileFlag.HASHED, id));
                continue;
            }
            byte[] data = Files.readAllBytes(tree.path(id));
            total += data.length;
            ByteBuffer sha = ByteBuffer.wrap(MessageDigest.getInstance("SHA-256").digest(data));
            for (int word = 0; word < 4; word++) assertEquals(sha.getLong(), tree.shaWord(id, word));
            CRC32C crc = new CRC32C();
            crc.update(data);
            assertEquals(crc.getValue(), tree.crc(id));
            assertTrue(tree.flag(M3FileFlag.HASHED, id));
            assertEquals(!tree.name(id).equals("blob.bin"), tree.flag(M3FileFlag.TOKEN_HASHED, id), tree.name(id));
            assertFalse(tree.flag(M3FileFlag.STRUCT_HASHED, id));
        }
        assertEquals(total, worked.get());
    }

    @Test
    void attributeGateReadsOnlyWhatMoved() throws Exception {
        M3FileTree before = M3FileTree.open(root);
        before.hash(before.walk(), 1 << 20, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
        Path touch = root.resolve("touch.txt");
        Files.setLastModifiedTime(touch, FileTime.fromMillis(Files.getLastModifiedTime(touch).toMillis() + 5_000));
        append(root.resolve("grow.txt"), "def");
        rewrite(root.resolve("space.txt"), "a  b");
        rewrite(root.resolve("A.java"), "class A { int x = 2; }");
        M3FileTree after = M3FileTree.open(root);
        AtomicLong read = new AtomicLong();
        M3FileProgress counting = M3FileProgress.of(read::addAndGet, () -> false);
        M3FileChangeProvider provider = M3FileChangeProvider.ATTRIBUTES;

        assertEquals(0, change(provider, before, after, "keep.txt", counting));
        assertEquals(0, read.get(), "closed gate reads nothing");
        assertTrue(after.flag(M3FileFlag.HASHED, id(after, "keep.txt")), "closed gate carries the observation");
        assertEquals(M3FileChange.MTIME, change(provider, before, after, "touch.txt", counting));
        assertEquals(10, read.get());
        assertEquals(M3FileChange.SIZE | M3FileChange.MTIME | M3FileChange.CONTENT_CHANGED | M3FileChange.TOKENS,
                change(provider, before, after, "grow.txt", counting) & ~M3FileChange.FILE_KEY);
        assertEquals(M3FileChange.SIZE | M3FileChange.MTIME | M3FileChange.CONTENT_CHANGED,
                change(provider, before, after, "space.txt", counting) & ~M3FileChange.FILE_KEY, "whitespace keeps tokens");
        int edit = change(provider, before, after, "A.java", counting);
        assertTrue((edit & M3FileChange.CONTENT_CHANGED) != 0 && (edit & M3FileChange.TOKENS) != 0,
                M3FileChange.describe(edit));
        assertEquals(0, change(provider, before, after, "dir", counting) & ~M3FileChange.MTIME);
        assertEquals("CONTENT_CHANGED|SIZE|MTIME", M3FileChange.describe(M3FileChange.SIZE | M3FileChange.MTIME
                | M3FileChange.CONTENT_CHANGED));
        assertEquals("NONE", M3FileChange.describe(0));
    }

    @Test
    void sameSizeAndTimeIsTheRacyCaseProvidersClose() throws Exception {
        M3FileTree before = M3FileTree.open(root);
        before.hash(before.walk(), 1 << 20, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
        Path a = root.resolve("A.java");
        FileTime time = Files.getLastModifiedTime(a);
        rewrite(a, "class A { int x = 9; }");
        Files.setLastModifiedTime(a, time);
        M3FileTree after = M3FileTree.open(root);
        assertEquals(0, change(M3FileChangeProvider.ATTRIBUTES, before, after, "A.java", M3FileProgress.NONE));
        M3FileChangeProvider watched = new M3FileChangeProvider() {
            @Override
            public int attributeChange(M3FileTree b, int beforeId, M3FileTree n, int afterId) {
                int bits = M3FileChangeProvider.super.attributeChange(b, beforeId, n, afterId);
                return n.name(afterId).equals("A.java") ? bits | M3FileChange.MTIME : bits;
            }
        };
        M3FileTree again = M3FileTree.open(root);
        int bits = change(watched, before, again, "A.java", M3FileProgress.NONE);
        assertTrue((bits & M3FileChange.CONTENT_CHANGED) != 0, M3FileChange.describe(bits));
    }

    @Test
    void structureLayerComesFromTheProvider() throws Exception {
        M3FileChangeProvider braces = new M3FileChangeProvider() {
            @Override
            public Optional<? extends M3FileStructHash> structure(M3File file) {
                return file.type().equals("java") ? Optional.of(new Braces()) : Optional.empty();
            }
        };
        M3FileTree before = M3FileTree.open(root);
        before.hash(before.walk(), 1 << 20, M3FileProgress.NONE, braces);
        int a = id(before, "A.java");
        assertTrue(before.flag(M3FileFlag.STRUCT_HASHED, a));
        assertEquals(1, before.structHash(a));
        assertFalse(before.flag(M3FileFlag.STRUCT_HASHED, id(before, "keep.txt")));
        rewrite(root.resolve("A.java"), "class A { int x = 1; class B { } }");
        M3FileTree after = M3FileTree.open(root);
        int bits = change(braces, before, after, "A.java", M3FileProgress.NONE);
        assertTrue((bits & M3FileChange.STRUCTURE) != 0, M3FileChange.describe(bits));
        assertEquals(2, after.structHash(id(after, "A.java")));
    }

    @Test
    void failuresAndCancellation() throws Exception {
        M3FileTree before = M3FileTree.open(root);
        before.hash(before.walk(), 1 << 20, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES);
        rewrite(root.resolve("grow.txt"), "abcdefgh");
        M3FileTree after = M3FileTree.open(root);
        Files.delete(root.resolve("grow.txt"));
        int bits = change(M3FileChangeProvider.ATTRIBUTES, before, after, "grow.txt", M3FileProgress.NONE);
        assertTrue((bits & M3FileChange.UNREADABLE) != 0, M3FileChange.describe(bits));
        assertTrue(after.flag(M3FileFlag.FAILED, id(after, "grow.txt")));
        assertEquals(5, after.hash(after.walk().maxDepth(1).nameRegex("[^g].*"), 1 << 20, M3FileProgress.NONE,
                M3FileChangeProvider.ATTRIBUTES));
        assertEquals(0, after.hash(after.walk().nameRegex("A.java"), 2, M3FileProgress.NONE, M3FileChangeProvider.ATTRIBUTES));
        assertTrue(after.flag(M3FileFlag.FAILED, id(after, "A.java")), "over the byte limit");
        assertThrows(InterruptedIOException.class, () -> after.hash(after.walk(), 1 << 20,
                M3FileProgress.of(units -> { }, () -> true), M3FileChangeProvider.ATTRIBUTES));
        M3FileTree other = M3FileTree.open(root);
        assertThrows(IllegalArgumentException.class, () -> after.hash(other.walk(), 1, M3FileProgress.NONE,
                M3FileChangeProvider.ATTRIBUTES));
    }

    /** Fake structure hash: number of '{' (stands in for an M3AST layer). */
    private static final class Braces implements M3FileStructHash {
        private long braces;

        @Override
        public void update(char[] chars, int offset, int length) {
            for (int i = offset; i < offset + length; i++) if (chars[i] == '{') braces++;
        }

        @Override
        public long structHash() {
            return braces;
        }
    }

    private int change(M3FileChangeProvider provider, M3FileTree before, M3FileTree after, String name,
            M3FileProgress progress) throws InterruptedIOException {
        return provider.change(before, id(before, name), after, id(after, name), 1 << 20, progress);
    }

    private int id(M3FileTree tree, String relative) {
        Path target = root.resolve(relative);
        return tree.ids().filter(id -> tree.path(id).equals(target)).findFirst().orElseThrow();
    }

    private static void rewrite(Path file, String text) throws IOException {
        FileTime previous = Files.getLastModifiedTime(file);
        Files.writeString(file, text, StandardCharsets.UTF_8);
        Files.setLastModifiedTime(file, FileTime.fromMillis(previous.toMillis() + 7_000));
    }

    private static void append(Path file, String text) throws IOException {
        rewrite(file, Files.readString(file) + text);
    }
}
