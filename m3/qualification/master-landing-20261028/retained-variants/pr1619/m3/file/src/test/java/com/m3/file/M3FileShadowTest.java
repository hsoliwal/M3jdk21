// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32C;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** S3 contract: progress monitors and shadow streams; one read pass feeds every layer. */
class M3FileShadowTest {
    private static final String TEXT = "package a;\r\nclass  B {\n\tint c = 42; // café € 😀\r}\n\nend";

    @TempDir
    Path root;

    @Test
    void onePassFeedsEveryLayer() throws Exception {
        byte[] data = TEXT.getBytes(StandardCharsets.UTF_8);
        Path file = Files.write(root.resolve("B.java"), data);
        M3FileDigestShadow sha = M3FileShadow.sha256();
        M3FileCrcShadow crc = M3FileShadow.crc32c();
        M3FileLineShadow lines = M3FileShadow.lines();
        M3FileTextShadow text = M3FileCharShadow.text();
        M3FileTokenShadow tokens = M3FileCharShadow.tokens();
        M3FileUtf8Shadow utf8 = M3FileShadow.utf8(text, tokens);
        AtomicLong worked = new AtomicLong();
        long[] total = new long[1];
        M3FileProgress progress = new M3FileProgress() {
            @Override
            public void worked(long units) {
                worked.addAndGet(units);
            }

            @Override
            public void begin(String task, long totalUnits) {
                total[0] = totalUnits;
            }
        };
        M3FileTree tree = M3FileTree.open(root);
        byte[] read;
        try (M3FileShadowInputStream in = tree.file(1).openInputStream(progress, sha, crc, lines, utf8)) {
            read = in.readAllBytes();
            assertTrue(in.ended());
            assertEquals(data.length, in.count());
        }
        assertArrayEquals(data, read);
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(data), sha.bytes());
        assertEquals(crc32c(data), crc.value());
        assertEquals(data.length, lines.length());
        assertEquals(new BufferedReader(new StringReader(TEXT)).lines().count(), lines.lineCount());
        assertEquals(TEXT, text.text());
        assertEquals(TEXT.length(), utf8.charCount());
        assertEquals(data.length, worked.get());
        assertEquals(data.length, total[0]);
        assertEquals(Files.size(file), tree.size(1));
        assertEquals(0L, lines.lineStart(0));
        assertEquals(lines.lineCount() - 1, lines.lineOf(data.length - 1));
    }

    @Test
    void layersAreIndependentOfBlockSplits() throws Exception {
        Random random = new Random(20261020);
        byte[] binary = new byte[40_000];
        random.nextBytes(binary);
        byte[] text = (TEXT + TEXT.repeat(500)).getBytes(StandardCharsets.UTF_8);
        for (byte[] data : List.of(binary, text, new byte[0], "\r\n\r\r\n\n".getBytes(StandardCharsets.US_ASCII))) {
            for (int block : new int[] {1, 2, 3, 7, 4096, 65536}) {
                M3FileDigestShadow sha = M3FileShadow.sha256();
                M3FileLineShadow lines = M3FileShadow.lines();
                M3FileTextShadow decoded = M3FileCharShadow.text();
                M3FileTokenShadow tokens = M3FileCharShadow.tokens();
                try (InputStream in = new M3FileShadowInputStream(new ByteArrayInputStream(data), Long.MAX_VALUE,
                        M3FileProgress.NONE, sha, lines, M3FileShadow.utf8(decoded, tokens))) {
                    byte[] buffer = new byte[block];
                    for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
                        // drain
                    }
                }
                String expected = new String(data, StandardCharsets.UTF_8);
                assertEquals(expected, decoded.text(), "block " + block);
                assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(data), sha.bytes());
                assertEquals(new BufferedReader(new StringReader(expected)).lines().count(), lines.lineCount());
                assertEquals(tokenHash(expected), tokens.hash(), "block " + block);
            }
        }
    }

    @Test
    void tokenHashIgnoresWhitespaceOnly() throws Exception {
        assertEquals(tokenHash("class B { int c = 42; }"), tokenHash("class   B\n{\n\tint c=42;\n}\n"));
        assertNotEquals(tokenHash("ab c"), tokenHash("a bc"));
        assertNotEquals(tokenHash("a+b"), tokenHash("a+ +b"));
        M3FileTokenShadow tokens = M3FileCharShadow.tokens();
        char[] chars = "int c = 42;".toCharArray();
        tokens.update(chars, 0, chars.length);
        assertThrows(IllegalStateException.class, tokens::hash);
        tokens.finish();
        assertEquals(5, tokens.tokens());
    }

    @Test
    void skipAndTransferToFeedShadows() throws Exception {
        byte[] data = TEXT.repeat(100).getBytes(StandardCharsets.UTF_8);
        M3FileDigestShadow sha = M3FileShadow.sha256();
        M3FileCrcShadow crc = M3FileShadow.crc32c();
        ByteArrayOutputStream rest = new ByteArrayOutputStream();
        try (M3FileShadowInputStream in = new M3FileShadowInputStream(new ByteArrayInputStream(data), data.length,
                M3FileProgress.NONE, sha, crc)) {
            assertEquals(100, in.skip(100));
            assertEquals(data[100] & 0xff, in.read());
            in.transferTo(rest);
            assertFalse(in.markSupported());
            assertThrows(IOException.class, in::reset);
        }
        assertEquals(data.length - 101, rest.size());
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(data), sha.bytes());
        assertEquals(crc32c(data), crc.value());
    }

    @Test
    void outputStreamFeedsShadowsAndProgress() throws Exception {
        byte[] data = TEXT.repeat(50).getBytes(StandardCharsets.UTF_8);
        Path file = Files.write(root.resolve("out.txt"), new byte[] {1, 2, 3});
        M3FileDigestShadow sha = M3FileShadow.sha256();
        M3FileLineShadow lines = M3FileShadow.lines();
        AtomicLong worked = new AtomicLong();
        M3FileTree tree = M3FileTree.open(root);
        M3File out = tree.file(1);
        try (OutputStream stream = out.openOutputStream(M3FileProgress.of(worked::addAndGet, () -> false), sha, lines)) {
            stream.write(data, 0, 10);
            stream.write(data[10]);
            stream.write(data, 11, data.length - 11);
            assertFalse(sha.finished());
        }
        assertTrue(sha.finished());
        assertArrayEquals(data, Files.readAllBytes(file));
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(data), sha.bytes());
        assertEquals(data.length, worked.get());
        assertEquals(data.length, lines.length());
        M3FileDigestShadow again = M3FileShadow.sha256();
        assertEquals(data.length, M3File.of(file).observe(data.length, M3FileProgress.NONE, again));
        assertEquals(sha.hex(), again.hex());
    }

    @Test
    void cancellationAndLimitsStopStreams() throws Exception {
        byte[] data = new byte[100_000];
        Path file = Files.write(root.resolve("big.bin"), data);
        AtomicLong worked = new AtomicLong();
        M3FileProgress cancelAfterFirstBlock = M3FileProgress.of(worked::addAndGet, () -> worked.get() > 0);
        try (InputStream in = M3File.of(file).openInputStream(cancelAfterFirstBlock)) {
            assertTrue(in.read(new byte[1000]) > 0);
            assertThrows(InterruptedIOException.class, () -> in.read(new byte[1000]));
        }
        try (OutputStream out = new M3FileShadowOutputStream(new ByteArrayOutputStream(), 1_000_000,
                M3FileProgress.of(units -> { }, () -> true))) {
            assertThrows(InterruptedIOException.class, () -> out.write(1));
        }
        assertThrows(IOException.class, () -> M3File.of(file).openInputStream(10, M3FileProgress.NONE));
        try (InputStream in = new M3FileShadowInputStream(new ByteArrayInputStream(data), 10, M3FileProgress.NONE)) {
            assertEquals(10, in.readNBytes(10).length);
            assertThrows(IOException.class, in::read);
        }
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (OutputStream out = new M3FileShadowOutputStream(sink, 10, M3FileProgress.NONE)) {
            out.write(new byte[10]);
            assertThrows(IOException.class, () -> out.write(0));
        }
        assertEquals(10, sink.size());
        assertThrows(IOException.class, () -> M3File.of(root).openInputStream(M3FileProgress.NONE));
    }

    @Test
    void splitScalesChildWorkOntoParentShare() {
        List<Long> parent = new ArrayList<>();
        M3FileProgress top = M3FileProgress.of(parent::add, () -> false);
        M3FileProgress child = top.split(100);
        child.begin("child", 1000);
        child.worked(500);
        assertEquals(List.of(50L), parent);
        child.worked(5);
        assertEquals(List.of(50L), parent);
        child.worked(5);
        assertEquals(List.of(50L, 1L), parent);
        child.done();
        assertEquals(100L, parent.stream().mapToLong(Long::longValue).sum());
        child.worked(1000);
        assertEquals(100L, parent.stream().mapToLong(Long::longValue).sum());
        List<Long> unknown = new ArrayList<>();
        M3FileProgress capped = M3FileProgress.of(unknown::add, () -> false).split(10);
        capped.worked(4);
        capped.worked(40);
        assertEquals(List.of(4L, 6L), unknown);
        assertThrows(IllegalArgumentException.class, () -> top.split(-1));
        M3FileProgress canceled = M3FileProgress.of(units -> { }, () -> true).split(5).split(1);
        assertTrue(canceled.isCanceled());
        assertThrows(InterruptedIOException.class, canceled::checkCanceled);
    }

    @Test
    void composedShadowsSeeTheSameBlocks() throws Exception {
        byte[] data = TEXT.getBytes(StandardCharsets.UTF_8);
        M3FileCrcShadow first = M3FileShadow.crc32c();
        M3FileCrcShadow second = M3FileShadow.crc32c();
        M3FileTextShadow a = M3FileCharShadow.text();
        M3FileTextShadow b = M3FileCharShadow.text();
        try (InputStream in = new M3FileShadowInputStream(new ByteArrayInputStream(data), Long.MAX_VALUE,
                M3FileProgress.NONE, first.andThen(second).andThen(M3FileShadow.utf8(a.andThen(b))))) {
            in.readAllBytes();
        }
        assertEquals(crc32c(data), first.value());
        assertEquals(first.value(), second.value());
        assertEquals(TEXT, a.text());
        assertEquals(TEXT, b.text());
    }

    private static long crc32c(byte[] data) {
        CRC32C crc = new CRC32C();
        crc.update(data);
        return crc.getValue();
    }

    private static long tokenHash(String text) {
        M3FileTokenShadow tokens = M3FileCharShadow.tokens();
        char[] chars = text.toCharArray();
        tokens.update(chars, 0, chars.length);
        tokens.finish();
        return tokens.hash();
    }
}
