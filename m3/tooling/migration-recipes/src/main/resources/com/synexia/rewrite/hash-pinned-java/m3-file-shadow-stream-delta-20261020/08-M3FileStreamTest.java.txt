// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FileStreamTest {
    @TempDir Path temp;

    @Test
    void childProgressScalesAndCompletesExactlyOnce() {
        AtomicLong worked = new AtomicLong();
        M3ProgressMonitor parent = new M3ProgressMonitor() {
            @Override public boolean isCanceled() { return false; }
            @Override public void worked(long units) { worked.addAndGet(units); }
        };
        M3ProgressMonitor child = parent.child(10, 100);
        child.worked(25);
        assertEquals(2L, worked.get());
        child.worked(25);
        assertEquals(5L, worked.get());
        child.done();
        child.done();
        assertEquals(10L, worked.get());
    }

    @Test
    void shadowInputMovesOnlyWithPrimaryReads() throws Exception {
        byte[] source = "abcdef".getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream shadow = new ByteArrayOutputStream();
        AtomicLong worked = new AtomicLong();
        M3ProgressMonitor progress =
                M3ProgressMonitor.none().andThen(new M3ProgressMonitor() {
                    @Override public boolean isCanceled() { return false; }
                    @Override public void worked(long units) { worked.addAndGet(units); }
                });

        try (InputStream input =
                M3ShadowInputStream.wrap(
                        new ByteArrayInputStream(source),
                        progress,
                        M3StreamObserver.to(shadow))) {
            byte[] first = input.readNBytes(2);
            assertArrayEquals("ab".getBytes(StandardCharsets.US_ASCII), first);
            assertEquals(2L, input.skip(2));
            assertArrayEquals("ef".getBytes(StandardCharsets.US_ASCII), input.readAllBytes());
        }

        assertArrayEquals("abef".getBytes(StandardCharsets.US_ASCII), shadow.toByteArray());
        assertEquals(4L, worked.get());
    }

    @Test
    void shadowOutputFollowsSuccessfulPrimaryWrites() throws Exception {
        ByteArrayOutputStream primary = new ByteArrayOutputStream();
        ByteArrayOutputStream shadow = new ByteArrayOutputStream();
        try (var output =
                M3ShadowOutputStream.wrap(
                        primary,
                        M3ProgressMonitor.none(),
                        M3StreamObserver.to(shadow))) {
            output.write("alpha".getBytes(StandardCharsets.US_ASCII));
            output.write('!');
        }
        assertArrayEquals(primary.toByteArray(), shadow.toByteArray());
        assertArrayEquals("alpha!".getBytes(StandardCharsets.US_ASCII), primary.toByteArray());
    }

    @Test
    void exactSnapshotsDetectSameLengthContentChanges() throws Exception {
        Path path = temp.resolve("same-length.txt");
        Files.writeString(path, "alpha", StandardCharsets.UTF_8);
        M3File file = M3File.of(path);

        M3FileSnapshot before = file.snapshotWithSha256(M3ProgressMonitor.none());
        Files.writeString(path, "bravo", StandardCharsets.UTF_8);
        M3FileSnapshot after = file.snapshotWithSha256(M3ProgressMonitor.none());
        M3FileDelta delta = before.deltaTo(after);

        assertTrue(delta.changed());
        assertTrue(delta.exactContentChanged());
        assertFalse(delta.requiresContentProbe());
        assertEquals(before.metadata().size(), after.metadata().size());
    }

    @Test
    void metadataOnlyDeltaMarksUnknownContentWhenMetadataChanges() throws Exception {
        Path path = temp.resolve("grow.txt");
        Files.writeString(path, "a", StandardCharsets.UTF_8);
        M3File file = M3File.of(path);
        M3FileSnapshot before = file.snapshot();

        Files.writeString(path, "alphabet", StandardCharsets.UTF_8);
        M3FileDelta delta = before.deltaTo(file.snapshot());

        assertTrue((delta.mask() & M3FileDelta.SIZE) != 0);
        assertTrue(delta.requiresContentProbe());
    }

    @Test
    void lazySuffixTraversalDoesNotRetainUnrelatedFiles() throws Exception {
        Path rootA = Files.createDirectory(temp.resolve("a"));
        Path rootB = Files.createDirectory(temp.resolve("b"));
        Files.writeString(rootA.resolve("one.txt"), "1");
        Files.writeString(rootA.resolve("two.bin"), "2");
        Files.writeString(rootB.resolve("three.txt"), "3");

        List<Path> paths;
        try (var stream =
                M3FileTraversal.roots(
                        List.of(rootA, rootB),
                        M3FileTraversal.Options.suffixes(".txt").withParallelRoots(true),
                        M3ProgressMonitor.none())) {
            paths = stream.map(M3File::path).sorted().toList();
        }

        assertEquals(List.of(rootA.resolve("one.txt"), rootB.resolve("three.txt")), paths);
    }
}
