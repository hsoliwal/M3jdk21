// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemSdk;
import jdk.m3.optane.PmemSourceImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Real SDK FILE-backend and optional actual Linux POSIX JNI acceptance.
 * These are byte-level signals, NOT code-pattern semantic admission.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign/PmemCorpusSearchJUnitTest.java blob 7856b538070a; recipe make/m3/optane/recipe/pmem-gram-index-20261106/relocate.py v3
final class PmemCorpusSearchJUnitTest {
    @TempDir Path temporary;
    private static final PmemSourceImage.Limits LIMITS =
            new PmemSourceImage.Limits(6, 500_000L, 150_000, 4096);

    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }

    private Path smallRepository() throws Exception {
        Path repo = Files.createDirectory(temporary.resolve("repository"));
        Files.write(repo.resolve("A.java"), bytes("ababa"));
        Files.write(repo.resolve("B.java"), bytes("aba"));
        Files.write(repo.resolve("C.java"), bytes("baba"));
        Files.write(repo.resolve("D.java"), new byte[0]);
        Files.write(repo.resolve("E.java"), bytes("🦊aba"));
        return repo;
    }
    private PmemSourceImage snapshot(Path repo, PmemBackend backend, List<String> selected,
                                     PmemSourceImage.Limits limits) throws Exception {
        Path destination = temporary.resolve("snapshot");
        PmemSourceImage.pack(repo, selected, destination, limits);
        return PmemSourceImage.open(destination, backend, limits);
    }

    @Test void exactUtf8ByteOffsetsAndSortedFirstHitsUseOriginalFileBackend() throws Exception {
        Path repo = smallRepository();
        try (PmemSourceImage image = snapshot(repo, PmemBackend.FILE,
                List.of("E.java", "C.java", "B.java", "D.java", "A.java"), LIMITS)) {
            var result = PmemCorpusSearch.firstHits(image, bytes("aba"), 20, () -> false);
            assertTrue(result.complete());
            assertEquals(image.sourceRoot(), result.sourceRoot());
            assertEquals(5, result.scannedFiles());
            assertEquals(4, result.hitCount());
            assertEquals(List.of(0, 1, 2, 4), List.of(
                    result.fileIndexAt(0), result.fileIndexAt(1),
                    result.fileIndexAt(2), result.fileIndexAt(3)));
            assertEquals(List.of(0L, 0L, 1L, 4L), List.of(
                    result.fileOffsetAt(0), result.fileOffsetAt(1),
                    result.fileOffsetAt(2), result.fileOffsetAt(3)));
            assertEquals(64, result.patternSha256().length());
            assertEquals(0, PmemCorpusSearch.firstHits(image, bytes("nothing here"), 20, () -> false).hitCount());
            // No pattern may cross the boundary between adjacent packed files.
            assertEquals(0, PmemCorpusSearch.firstHits(image, bytes("aaba"), 20, () -> false).hitCount());
        }
    }

    @Test void emptyBinaryBoundedCancellationAndClosedImageSemantics() throws Exception {
        Path repo = smallRepository();
        PmemSourceImage image = snapshot(repo, PmemBackend.FILE,
                List.of("A.java", "B.java", "C.java", "D.java", "E.java"), LIMITS);
        try {
            var all = PmemCorpusSearch.firstHits(image, new byte[0], 5, () -> false);
            assertTrue(all.complete());
            assertEquals(5, all.hitCount());
            for (int i = 0; i < all.hitCount(); i++) {
                assertEquals(i, all.fileIndexAt(i));
                assertEquals(0, all.fileOffsetAt(i));
            }
            var partial = PmemCorpusSearch.firstHits(image, bytes("aba"), 2, () -> false);
            assertFalse(partial.complete());
            assertEquals(2, partial.hitCount());
            assertEquals(2, partial.scannedFiles());
            assertThrows(IllegalArgumentException.class,
                    () -> PmemCorpusSearch.firstHits(image, bytes("x"), 0, () -> false));
            assertThrows(IllegalArgumentException.class,
                    () -> PmemCorpusSearch.firstHits(image, new byte[1_048_577], 2, () -> false));
            assertThrows(CancellationException.class,
                    () -> PmemCorpusSearch.firstHits(image, bytes("aba"), 5, () -> true));
            assertThrows(IndexOutOfBoundsException.class, () -> all.fileIndexAt(5));
            assertThrows(IndexOutOfBoundsException.class, () -> all.fileOffsetAt(-1));
        } finally { image.close(); }
        assertThrows(IllegalStateException.class,
                () -> PmemCorpusSearch.firstHits(image, bytes("aba"), 5, () -> false));
    }

    @Test void fileKmpStreamingRetainsBinaryAndWindowBoundaryFirstHit() throws Exception {
        Path repo = Files.createDirectory(temporary.resolve("repository"));
        byte[] content = new byte[130_000];
        Arrays.fill(content, (byte) 'x');
        byte[] match = bytes("aaab");
        System.arraycopy(match, 0, content, 65_534, match.length);
        Files.write(repo.resolve("Large.java"), content);
        Files.write(repo.resolve("Small.java"), new byte[]{0, 1, 0, 1, 2, 0});
        try (PmemSourceImage file = snapshot(repo, PmemBackend.FILE,
                List.of("Small.java", "Large.java"), LIMITS)) {
            var located = PmemCorpusSearch.firstHits(file, match, 2, () -> false);
            assertTrue(located.complete());
            assertEquals(1, located.hitCount());
            assertEquals(0, located.fileIndexAt(0));
            assertEquals(65_534L, located.fileOffsetAt(0));
            var binary = PmemCorpusSearch.firstHits(file, new byte[]{0,1,2}, 2, () -> false);
            assertEquals(1, binary.hitCount());
            assertEquals(1, binary.fileIndexAt(0));
            assertEquals(2L, binary.fileOffsetAt(0));
        }
    }

    @Test void actualPosixJniAndFileKmpGiveTheSameOutputsWhenRequired() throws Exception {
        if (!Boolean.getBoolean("pmem.corpus.native")) return;
        assertTrue(PmemSdk.capabilities().nativeAvailable(), "native qualification requested; JNI must exist");
        Path repo = Files.createDirectory(temporary.resolve("repository"));
        byte[] content = new byte[130_000];
        Arrays.fill(content, (byte) 'a');
        content[65_534] = 'b';
        Files.write(repo.resolve("Large.java"), content);
        Files.write(repo.resolve("Small.java"), new byte[]{0, 1, 0, 1, 2});
        PmemSourceImage.pack(repo, List.of("Large.java", "Small.java"),
                temporary.resolve("snapshot"), LIMITS);
        List<byte[]> needles = List.of(bytes("aaaaab"), bytes("nope"), new byte[]{0, 1, 2}, new byte[0]);
        try (PmemSourceImage file = PmemSourceImage.open(temporary.resolve("snapshot"),
                    PmemBackend.FILE, LIMITS);
             PmemSourceImage nativeMap = PmemSourceImage.open(temporary.resolve("snapshot"),
                    PmemBackend.POSIX, LIMITS)) {
            for (byte[] needle : needles) {
                var javaResult = PmemCorpusSearch.firstHits(file, needle, 2, () -> false);
                var jniResult = PmemCorpusSearch.firstHits(nativeMap, needle, 2, () -> false);
                assertEquals(javaResult.hitCount(), jniResult.hitCount());
                assertEquals(javaResult.complete(), jniResult.complete());
                for (int i = 0; i < javaResult.hitCount(); i++) {
                    assertEquals(javaResult.fileIndexAt(i), jniResult.fileIndexAt(i));
                    assertEquals(javaResult.fileOffsetAt(i), jniResult.fileOffsetAt(i));
                }
            }
        }
    }

    @Test @EnabledIfSystemProperty(named = "pmem.corpus.scale", matches = "true")
    void twentyThousandPhysicalFileFirstHitSignalsAreBounded() throws Exception {
        final int count = 20_000;
        Path repo = Files.createDirectory(temporary.resolve("repository"));
        List<String> selected = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String path = String.format(Locale.ROOT, "File%05d.java", i);
            Files.writeString(repo.resolve(path),
                    "final class X" + i + " { String pattern = \"" + (i%7==0?"KMP":"none") + "\"; }\n");
            selected.add(path);
        }
        var limits = new PmemSourceImage.Limits(count, 6_000_000L, 1024, 8192);
        try (PmemSourceImage image = snapshot(repo, PmemBackend.FILE, selected, limits)) {
            var results = PmemCorpusSearch.firstHits(image, bytes("KMP"), 20_000, () -> false);
            assertTrue(results.complete());
            assertEquals(count, results.scannedFiles());
            int expected = (count + 6)/7;
            assertEquals(expected, results.hitCount());
            for (int i = 0; i < results.hitCount(); i++)
                assertEquals(i * 7, results.fileIndexAt(i));
        }
    }
}
