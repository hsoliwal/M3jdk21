// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemSourceImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Exact-byte snapshot contract; uses real SDK backing rather than a fake mapping. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/junit/java/test/PmemSourceImageJUnitTest.java blob 045712cc23ae; recipe make/m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
public final class PmemSourceImageJUnitTest {
    @TempDir Path temp;
    private static final PmemSourceImage.Limits LIMITS = new PmemSourceImage.Limits(32, 1 << 20, 65536, 127);
    private Path repository() throws IOException { return Files.createDirectory(temp.resolve("repo")); }
    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static PmemBackend backend() { return PmemBackend.valueOf("FILE"); }

    @Test void roundTripExactNamesBytesSearchAndSealedCopies() throws Exception {
        Path repo = repository();
        byte[] bytes = {0, 13, 10, (byte) 0xff, 42, 0};
        Files.write(repo.resolve("z.java"), bytes);
        Files.writeString(repo.resolve("α 雪.java"), "\uFEFFnaïve\r\n", StandardCharsets.UTF_8);
        Files.write(repo.resolve("empty"), new byte[0]);
        Path image = temp.resolve("image");
        PmemSourceImage.pack(repo, List.of("α 雪.java", "z.java", "empty"), image, LIMITS);
        try (PmemSourceImage opened = PmemSourceImage.open(image, backend(), LIMITS)) {
            assertEquals(3, opened.fileCount());
            assertEquals("empty", opened.relativePathAt(0));
            assertEquals("z.java", opened.relativePathAt(1));
            assertEquals("α 雪.java", opened.relativePathAt(2));
            assertEquals(1, opened.fileIndexOf("z.java"));
            assertEquals(-1, opened.fileIndexOf("absent"));
            assertEquals(bytes.length, opened.lengthAt(1));
            assertEquals(hash(bytes), opened.sha256At(1));
            assertArrayEquals(bytes, opened.readSealed(1));
            byte[] copy = opened.readSealed(1); copy[0] = 99;
            assertArrayEquals(bytes, opened.readSealed(1));
            assertArrayEquals(new byte[0], opened.readSealed(0));
            assertEquals(3L, opened.indexOf(1, new byte[]{(byte) 0xff, 42}));
            assertEquals(-1L, opened.indexOf(1, new byte[]{42, 42}));
            assertEquals(0L, opened.indexOf(0, new byte[0]));
            assertThrows(IndexOutOfBoundsException.class, () -> opened.readSealed(3));
            assertThrows(IndexOutOfBoundsException.class, () -> opened.indexOf(-1, new byte[0]));
            assertThrows(NullPointerException.class, () -> opened.indexOf(1, null));
            assertEquals(5, opened.warm(5));
            assertEquals(0, opened.warm(0));
            assertThrows(IllegalArgumentException.class, () -> opened.warm(-1));
        }
    }

    @Test void deterministicManifestRootAcrossInputOrderAndRepositoryLocations() throws Exception {
        Path first = repository(), second = Files.createDirectory(temp.resolve("other"));
        for (Path root : List.of(first, second)) {
            Files.writeString(root.resolve("a"), "abc"); Files.writeString(root.resolve("b"), "λ");
        }
        Path one = temp.resolve("one"), two = temp.resolve("two");
        PmemSourceImage.pack(first, List.of("b", "a"), one, LIMITS);
        PmemSourceImage.pack(second, List.of("a", "b"), two, LIMITS);
        try (PmemSourceImage a = PmemSourceImage.open(one, backend(), LIMITS);
             PmemSourceImage b = PmemSourceImage.open(two, backend(), LIMITS)) {
            assertEquals(a.sourceRoot(), b.sourceRoot());
            assertEquals(64, a.sourceRoot().length());
            assertEquals(5, a.warm(Long.MAX_VALUE));
        }
        assertArrayEquals(Files.readAllBytes(one.resolve("manifest.bin")), Files.readAllBytes(two.resolve("manifest.bin")));
    }

    @Test void emptyImageAndClosedOperations() throws Exception {
        Path repo = repository(), image = temp.resolve("empty-image");
        PmemSourceImage.pack(repo, List.of(), image, LIMITS);
        PmemSourceImage opened = PmemSourceImage.open(image, backend(), LIMITS);
        assertEquals(0, opened.fileCount()); assertEquals(0, opened.warm(Long.MAX_VALUE));
        opened.close(); opened.close();
        assertThrows(IllegalStateException.class, opened::fileCount);
        assertThrows(IllegalStateException.class, opened::sourceRoot);
        assertThrows(IllegalStateException.class, () -> opened.fileIndexOf("x"));
        assertThrows(IllegalStateException.class, () -> opened.readSealed(0));
        assertThrows(IllegalStateException.class, () -> opened.warm(0));
        Path emptyFileImage = temp.resolve("empty-file-image"); Files.write(repo.resolve("empty"), new byte[0]);
        PmemSourceImage.pack(repo, List.of("empty"), emptyFileImage, LIMITS);
        PmemSourceImage file = PmemSourceImage.open(emptyFileImage, backend(), LIMITS); file.close();
        assertThrows(IllegalStateException.class, () -> file.readSealed(0));
        assertThrows(IllegalStateException.class, () -> file.relativePathAt(0));
        assertThrows(IllegalStateException.class, () -> file.lengthAt(0));
        assertThrows(IllegalStateException.class, () -> file.sha256At(0));
        assertThrows(IllegalStateException.class, () -> file.indexOf(0, new byte[0]));
    }

    @Test void refusesUnsafePathsDuplicatesSymlinksAndExistingDestination() throws Exception {
        Path repo = repository(); Files.writeString(repo.resolve("a"), "abc");
        for (String path : List.of("../a", "/a", "a\\b", "./a", "a/../a", "", "a//b")) {
            assertThrows(IllegalArgumentException.class, () -> PmemSourceImage.pack(repo, List.of(path), temp.resolve("bad"), LIMITS), path);
        }
        assertThrows(IllegalArgumentException.class, () -> PmemSourceImage.pack(repo, List.of("a", "a"), temp.resolve("dup"), LIMITS));
        Files.createSymbolicLink(repo.resolve("link"), repo.resolve("a"));
        assertThrows(IOException.class, () -> PmemSourceImage.pack(repo, List.of("link"), temp.resolve("symlink"), LIMITS));
        Path existing = Files.createDirectory(temp.resolve("existing"));
        assertThrows(IOException.class, () -> PmemSourceImage.pack(repo, List.of("a"), existing, LIMITS));
        Path partial = Files.createDirectory(temp.resolve("partial")); Files.write(partial.resolve("source.bin"), new byte[]{0});
        assertThrows(IOException.class, () -> PmemSourceImage.open(partial, backend(), LIMITS));
    }

    @Test void rejectsBudgetsBeforeMaterializationAndOnReopen() throws Exception {
        Path repo = repository(); Files.writeString(repo.resolve("a"), "abcd");
        assertThrows(IllegalArgumentException.class, () -> new PmemSourceImage.Limits(-1, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new PmemSourceImage.Limits(1, -1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new PmemSourceImage.Limits(1, 4, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new PmemSourceImage.Limits(1, 4, 4, 0));
        assertThrows(IOException.class, () -> PmemSourceImage.pack(repo, List.of("a"), temp.resolve("entry"), new PmemSourceImage.Limits(0, 4, 4, 1)));
        assertThrows(IOException.class, () -> PmemSourceImage.pack(repo, List.of("a"), temp.resolve("file"), new PmemSourceImage.Limits(1, 4, 3, 1)));
        assertThrows(IOException.class, () -> PmemSourceImage.pack(repo, List.of("a"), temp.resolve("corpus"), new PmemSourceImage.Limits(1, 3, 4, 1)));
        Path image = temp.resolve("ok"); PmemSourceImage.pack(repo, List.of("a"), image, LIMITS);
        assertThrows(IOException.class, () -> PmemSourceImage.open(image, backend(), new PmemSourceImage.Limits(0, 4, 4, 1)));
        assertThrows(IOException.class, () -> PmemSourceImage.open(image, backend(), new PmemSourceImage.Limits(1, 3, 4, 1)));
        assertThrows(IOException.class, () -> PmemSourceImage.open(image, backend(), new PmemSourceImage.Limits(1, 4, 3, 1)));
    }

    @Test void refusesCorruptionTruncationTrailingDataAndHashDrift() throws Exception {
        Path repo = repository(); Files.writeString(repo.resolve("a"), "abc");
        Path image = temp.resolve("ok"); PmemSourceImage.pack(repo, List.of("a"), image, LIMITS);
        Path manifest = image.resolve("manifest.bin"); byte[] good = Files.readAllBytes(manifest);
        byte[] bad = good.clone(); bad[bad.length / 2] ^= 1; Files.write(manifest, bad);
        assertThrows(IOException.class, () -> PmemSourceImage.open(image, backend(), LIMITS));
        Files.write(manifest, Arrays.copyOf(good, good.length - 1));
        assertThrows(IOException.class, () -> PmemSourceImage.open(image, backend(), LIMITS));
        Files.write(manifest, Arrays.copyOf(good, good.length + 1));
        assertThrows(IOException.class, () -> PmemSourceImage.open(image, backend(), LIMITS));
        Files.write(manifest, good);
        try (PmemSourceImage opened = PmemSourceImage.open(image, backend(), LIMITS)) {
            Files.writeString(image.resolve("source.bin"), "abd");
            assertThrows(SecurityException.class, () -> opened.readSealed(0));
        }
        assertThrows(IOException.class, () -> PmemSourceImage.open(image, backend(), LIMITS));
        Files.writeString(image.resolve("source.bin"), "ab");
        assertThrows(IOException.class, () -> PmemSourceImage.open(image, backend(), LIMITS));
    }

    @Test void nestedPathsSearchIsolationAndInvalidLimitCeilings() throws Exception {
        Path repo = repository(); Files.createDirectories(repo.resolve("src/雪"));
        Files.writeString(repo.resolve("a"), "abc"); Files.writeString(repo.resolve("src/雪/file.java"), "def");
        Path image = temp.resolve("nested");
        PmemSourceImage.pack(repo, List.of("src/雪/file.java", "a"), image, LIMITS);
        try (PmemSourceImage opened = PmemSourceImage.open(image, backend(), LIMITS)) {
            assertEquals("src/雪/file.java", opened.relativePathAt(1));
            assertEquals(-1, opened.indexOf(0, new byte[]{'c', 'd'}));
            assertEquals(1, opened.indexOf(1, new byte[]{'e', 'f'}));
        }
        assertThrows(IllegalArgumentException.class, () -> new PmemSourceImage.Limits(1_000_001, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new PmemSourceImage.Limits(1, 0, 16 * 1024 * 1024 + 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new PmemSourceImage.Limits(1, 0, 0, 16 * 1024 * 1024 + 1));
        assertThrows(IllegalArgumentException.class, () -> PmemSourceImage.pack(repo, List.of("x".repeat(4097)), temp.resolve("long"), LIMITS));
        Files.createSymbolicLink(repo.resolve("linked-dir"), repo.resolve("src"));
        assertThrows(IOException.class, () -> PmemSourceImage.pack(repo, List.of("linked-dir/雪/file.java"), temp.resolve("linked"), LIMITS));
    }

    @Test void rejectsForgedChecksummedGeometryAndMalformedUtf8() throws Exception {
        Path repo = repository(); Files.writeString(repo.resolve("a"), "abc");
        Path image = temp.resolve("forged"); PmemSourceImage.pack(repo, List.of("a"), image, LIMITS);
        Path manifest = image.resolve("manifest.bin"); byte[] good = Files.readAllBytes(manifest);
        // Offset/length lanes follow header(24), path length(4), and one ASCII path byte.
        for (int variant = 0; variant < 4; variant++) {
            byte[] bad = good.clone(); java.nio.ByteBuffer lanes = java.nio.ByteBuffer.wrap(bad);
            if (variant == 0) lanes.putInt(12, Integer.MAX_VALUE);
            if (variant == 1) lanes.putLong(29, Long.MAX_VALUE);
            if (variant == 2) lanes.putInt(37, -1);
            if (variant == 3) bad[28] = (byte) 0xff;
            byte[] checksum = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(bad, bad.length - 32));
            System.arraycopy(checksum, 0, bad, bad.length - 32, 32); Files.write(manifest, bad);
            assertThrows(IOException.class, () -> PmemSourceImage.open(image, backend(), LIMITS));
        }
    }

    @Test void detectsSameSizeMutationDuringCaptureEvenWithRestoredTimestamp() throws Exception {
        Path repo = repository(); Path first = repo.resolve("a"); Files.writeString(first, "abcd");
        java.nio.file.attribute.FileTime timestamp = Files.getLastModifiedTime(first);
        Files.write(repo.resolve("z"), new byte[4 * 1024 * 1024]);
        Path image = temp.resolve("changing");
        PmemSourceImage.Limits limits = new PmemSourceImage.Limits(2, 5 * 1024 * 1024, 4 * 1024 * 1024, 17);
        try (java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            java.util.concurrent.Future<?> capture = executor.submit(() -> {
                PmemSourceImage.pack(repo, List.of("a", "z"), image, limits); return null;
            });
            Path backing = image.resolve("source.bin"); long deadline = System.nanoTime() + 10_000_000_000L;
            while ((!Files.exists(backing) || Files.size(backing) <= 4) && !capture.isDone() && System.nanoTime() < deadline)
                Thread.sleep(1);
            assertFalse(capture.isDone(), "capture must still be streaming the second source");
            Files.writeString(first, "wxyz"); Files.setLastModifiedTime(first, timestamp);
            java.util.concurrent.ExecutionException failure = assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> capture.get(15, java.util.concurrent.TimeUnit.SECONDS));
            assertInstanceOf(IOException.class, failure.getCause());
            assertFalse(Files.exists(image.resolve("manifest.bin")));
            assertThrows(IOException.class, () -> PmemSourceImage.open(image, backend(), limits));
        }
    }

}
