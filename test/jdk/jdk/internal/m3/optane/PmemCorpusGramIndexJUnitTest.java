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
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.UncheckedIOException;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Original SDK FILE and explicitly required POSIX JNI semantic parity. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign/PmemCorpusGramIndexJUnitTest.java blob 31160b305bc5; recipe m3/optane/recipe/pmem-gram-index-20261106/relocate.py v3
final class PmemCorpusGramIndexJUnitTest {
    @TempDir Path temp;
    private static final PmemSourceImage.Limits SMALL =
            new PmemSourceImage.Limits(8, 500_000, 150_000, 8192);

    private static byte[] utf8(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private Path repository() throws Exception {
        Path source = Files.createDirectory(temp.resolve("repository"));
        Files.write(source.resolve("A.java"), utf8("abcabab"));
        Files.write(source.resolve("B.java"), utf8("abXbc")); // both grams, no whole 'abc'
        Files.write(source.resolve("C.java"), new byte[0]);
        Files.write(source.resolve("D.java"), utf8("🦊aba"));
        Files.write(source.resolve("E.java"), new byte[]{0, 1, 2, (byte)255, 0});
        Files.write(source.resolve("F.java"), utf8("no match"));
        return source;
    }
    private Path pack(Path source, List<String> selected, PmemSourceImage.Limits limits)
            throws Exception {
        Path snapshot = temp.resolve("snapshot");
        PmemSourceImage.pack(source, selected, snapshot, limits);
        return snapshot;
    }
    private static void sameHits(PmemCorpusGramIndex.Result indexed,
                                 PmemCorpusSelectedSearch.Result original) {
        assertEquals(original.hitCount(), indexed.hitCount());
        for (int i = 0; i < original.hitCount(); i++) {
            assertEquals(original.fileIndexAt(i), indexed.fileIndexAt(i));
            assertEquals(original.fileOffsetAt(i), indexed.fileOffsetAt(i));
        }
    }

    @Test void actualFileBackendIsSourceBoundAndPreservesEveryFirstHit() throws Exception {
        Path root = repository();
        Path packed = pack(root, List.of("F.java", "C.java", "E.java", "A.java", "D.java", "B.java"), SMALL);
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, SMALL)) {
            var index = PmemCorpusGramIndex.build(image, 4_000_000, () -> false);
            assertEquals(6, index.fileCount());
            assertTrue(index.retainedPrimitiveBytes() < 4_000_000);
            assertEquals(image.sourceRoot(), index.sourceRoot());
            assertEquals(64, index.indexSha256().length());
            assertEquals(3, index.documentFrequency(('a' << 8) | 'b'));
            int[] selected = {0,1,2,3,4,5};
            for (byte[] needle : List.of(utf8("abc"), utf8("aba"), utf8("🦊"), utf8("zzzz"),
                    new byte[0], new byte[]{0,1,2}, new byte[]{(byte)255,0}, utf8("a"))) {
                for (int limit : new int[]{1, 2, 6}) {
                    var got = index.firstHits(image, selected, needle, limit, () -> false);
                    var original = PmemCorpusSelectedSearch.firstHits(image, selected, needle,
                            limit, () -> false);
                    sameHits(got, original);
                    assertEquals(selected.length, got.selectedFiles());
                    assertTrue(got.searchedCandidates() <= got.candidateFiles());
                    if (got.complete()) assertTrue(original.complete() || original.hitCount() == limit);
                }
            }
            var missing = index.firstHits(image, selected, utf8("QZ"), 6, () -> false);
            assertTrue(missing.complete());
            assertEquals(0, missing.candidateFiles());
            assertEquals(6, missing.prunedFiles());
            var falsePositive = index.firstHits(image, new int[]{1}, utf8("abc"), 6, () -> false);
            assertTrue(falsePositive.complete());
            assertEquals(0, falsePositive.hitCount());
            assertNotEquals(index.indexSha256(), falsePositive.originalSelectionSha256());
        }
    }

    @Test void distinctRequiredGramsReduceFalsePositiveKmpCallsWithoutLosingHits() throws Exception {
        Path root = Files.createDirectory(temp.resolve("repository"));
        final int files = 300;
        List<String> paths = new ArrayList<>(files);
        for (int i = 0; i < files; i++) {
            String name = String.format(Locale.ROOT, "F%03d.java", i);
            // Only file 299 contains the complete pattern. Both halves are
            // common in independent files, so either pair alone is a weak filter.
            String data = i == files - 1 ? "abc" : i % 2 == 0 ? "ab--xx" : "yy--bc";
            Files.write(root.resolve(name), utf8(data));
            paths.add(name);
        }
        var limits = new PmemSourceImage.Limits(files, 64_000, 1024, 8192);
        Path packed = pack(root, paths, limits);
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, limits)) {
            var index = PmemCorpusGramIndex.build(image, 4_000_000, () -> false);
            int[] chosen = new int[files];
            for (int i = 0; i < chosen.length; i++) chosen[i] = i;
            var got = index.firstHits(image, chosen, utf8("abc"), files, () -> false);
            var direct = PmemCorpusSelectedSearch.firstHits(image, chosen, utf8("abc"), files, () -> false);
            sameHits(got, direct);
            assertTrue(got.complete());
            assertEquals(1, got.hitCount());
            assertEquals(files - 1, got.fileIndexAt(0));
            // Both individually necessary grams occur, but never contiguously.
            assertTrue(got.candidateFiles() < files / 10);
            assertEquals(index.sourceRoot(), got.sourceRoot());
        }
    }

    @Test void threeDistinctPairsPruneTwoPairCooccurrenceWithoutLosingHits() throws Exception {
        Path root = Files.createDirectory(temp.resolve("repository"));
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String name = String.format(Locale.ROOT, "S%02d.java", i);
            String source = i < 5 ? "abXbcY" : i < 10 ? "cdXcdY" : "abcd";
            Files.write(root.resolve(name), utf8(source));
            names.add(name);
        }
        var limits = new PmemSourceImage.Limits(12, 512, 32, 32);
        Path packed = pack(root, names, limits);
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, limits)) {
            var index = PmemCorpusGramIndex.build(image, 2_000_000, () -> false);
            int[] selected = new int[12];
            for (int i=0; i<selected.length; i++) selected[i] = i;
            var filtered = index.firstHits(image, selected, utf8("abcd"), 12, () -> false);
            var exact = PmemCorpusSelectedSearch.firstHits(image, selected, utf8("abcd"), 12, () -> false);
            assertTrue(filtered.complete());
            assertEquals(12, filtered.selectedFiles());
            assertEquals(2, filtered.candidateFiles(), "third pair must reject 5 false-positive files");
            assertEquals(10, filtered.prunedFiles());
            sameHits(filtered, exact);
            assertEquals(2, filtered.hitCount());
            assertEquals(10, filtered.fileIndexAt(0));
            assertEquals(11, filtered.fileIndexAt(1));
        }
    }

    @Test void budgetSourceIdentityCancellationAndDefensiveIndexAdmission() throws Exception {
        Path root = repository();
        Path packed = pack(root, List.of("A.java", "B.java", "C.java", "D.java", "E.java", "F.java"), SMALL);
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, SMALL)) {
            var first = PmemCorpusGramIndex.build(image, 4_000_000, () -> false);
            assertEquals(first.indexSha256(),
                    PmemCorpusGramIndex.build(image, first.retainedPrimitiveBytes(), () -> false).indexSha256());
            assertThrows(IllegalArgumentException.class,
                    () -> PmemCorpusGramIndex.build(image, first.retainedPrimitiveBytes() - 1, () -> false));
            assertThrows(CancellationException.class,
                    () -> PmemCorpusGramIndex.build(image, 4_000_000, () -> true));
            assertThrows(CancellationException.class,
                    () -> first.firstHits(image, new int[]{0,1}, utf8("ab"), 2, () -> true));
            for (int[] bad : List.of(new int[]{-1}, new int[]{6}, new int[]{1,1},
                    new int[]{2,1}, new int[]{0,4,4}))
                assertThrows(IllegalArgumentException.class,
                        () -> first.firstHits(image, bad, utf8("ab"), 6, () -> false));
            assertThrows(IllegalArgumentException.class,
                    () -> first.firstHits(image, new int[]{0,1}, new byte[1_048_577], 6, () -> false));
            assertThrows(IllegalArgumentException.class,
                    () -> first.firstHits(image, new int[]{0,1}, utf8("ab"), 0, () -> false));
            assertThrows(IndexOutOfBoundsException.class, () -> first.documentFrequency(65_536));
            var empty = first.firstHits(image, new int[0], utf8("ab"), 1, () -> false);
            assertTrue(empty.complete()); assertEquals(0, empty.hitCount());
        }
    }

    @Test void fileKmpWindowBoundaryByteOffsetsRemainExact() throws Exception {
        Path root = Files.createDirectory(temp.resolve("repository"));
        byte[] source = new byte[130_000]; Arrays.fill(source, (byte)'x');
        System.arraycopy(utf8("aaab"), 0, source, 65_534, 4);
        Files.write(root.resolve("Large.java"), source);
        Files.write(root.resolve("Small.java"), new byte[]{0,1,0,1,2,0});
        Path packed = pack(root, List.of("Small.java", "Large.java"), SMALL);
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, SMALL)) {
            var index = PmemCorpusGramIndex.build(image, 4_000_000, () -> false);
            var first = index.firstHits(image, new int[]{0,1}, utf8("aaab"), 5, () -> false);
            assertEquals(1, first.hitCount()); assertEquals(0, first.fileIndexAt(0));
            assertEquals(65_534L, first.fileOffsetAt(0));
            var bytes = index.firstHits(image, new int[]{0,1}, new byte[]{0,1,2}, 5, () -> false);
            assertEquals(1, bytes.hitCount()); assertEquals(1, bytes.fileIndexAt(0));
            assertEquals(2L, bytes.fileOffsetAt(0));
        }
    }

    @Test void realPosixJniMatchesFileKmpWhenExplicitlyRequired() throws Exception {
        if (!Boolean.getBoolean("pmem.gram.native")) return;
        assertTrue(PmemSdk.capabilities().nativeAvailable(), "native proof requested; JNI unavailable");
        Path root = repository();
        Path packed = pack(root, List.of("A.java", "B.java", "C.java", "D.java", "E.java", "F.java"), SMALL);
        try (PmemSourceImage file = PmemSourceImage.open(packed, PmemBackend.FILE, SMALL);
             PmemSourceImage nativeImage = PmemSourceImage.open(packed, PmemBackend.POSIX, SMALL)) {
            var a = PmemCorpusGramIndex.build(file, 4_000_000, () -> false);
            var b = PmemCorpusGramIndex.build(nativeImage, 4_000_000, () -> false);
            assertEquals(a.indexSha256(), b.indexSha256());
            for (int[] selected : List.of(new int[]{0,1}, new int[]{1,3,4}, new int[]{0,1,2,3,4,5})) {
                for (byte[] needle : List.of(utf8("abc"), utf8("aba"), utf8("abXbc"),
                        utf8("🦊"), new byte[0], new byte[]{0,1,2}, utf8("absent"))) {
                    var x = a.firstHits(file, selected, needle, 6, () -> false);
                    var y = b.firstHits(nativeImage, selected, needle, 6, () -> false);
                    assertEquals(x.complete(), y.complete());
                    assertEquals(x.originalSelectionSha256(), y.originalSelectionSha256());
                    assertEquals(x.candidateFiles(), y.candidateFiles());
                    assertEquals(x.hitCount(), y.hitCount());
                    for (int i = 0; i < x.hitCount(); i++) {
                        assertEquals(x.fileIndexAt(i), y.fileIndexAt(i));
                        assertEquals(x.fileOffsetAt(i), y.fileOffsetAt(i));
                    }
                }
            }
        }
    }

    @Test void denseFiftyKilobyteBinarySourcesUseExactEightKiBBitmaps() throws Exception {
        final int count = 32;
        Path root = Files.createDirectory(temp.resolve("repository"));
        Random random = new Random(980123);
        List<String> names = new ArrayList<>(count);
        byte[] token = utf8("M3_DENSE!");
        for (int i = 0; i < count; i++) {
            String name = String.format(Locale.ROOT, "Binary%03d.java", i);
            byte[] content = new byte[50_000];
            random.nextBytes(content);
            if (i % 7 == 0) System.arraycopy(token, 0, content, 30_000, token.length);
            Files.write(root.resolve(name), content);
            names.add(name);
        }
        PmemSourceImage.Limits limits = new PmemSourceImage.Limits(count, 2_000_000L, 50_000, 8192);
        Path packed = pack(root, names, limits);
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, limits)) {
            var index = PmemCorpusGramIndex.build(image, 1_000_000L, () -> false);
            assertEquals(count, index.denseFileCount());
            assertTrue(index.retainedPrimitiveBytes() < 1_000_000L);
            int[] selected = new int[count]; for (int i = 0; i < count; i++) selected[i] = i;
            var gram = index.firstHits(image, selected, token, count, () -> false);
            var direct = PmemCorpusSelectedSearch.firstHits(image, selected, token, count, () -> false);
            sameHits(gram, direct);
            assertTrue(gram.complete());
            assertEquals((count + 6) / 7, gram.hitCount());
        }
    }

    @Test @EnabledIfSystemProperty(named="pmem.gram.scale", matches="true")
    void twentyThousandPhysicalFilesFilterBeforeNativeSearch() throws Exception {
        final int count = 20_000;
        Path root = Files.createDirectory(temp.resolve("repository"));
        List<String> paths = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = String.format(Locale.ROOT, "File%05d.java", i);
            Files.writeString(root.resolve(name), "final class X" + i +
                    " { String tag = \"" + (i%73 == 0 ? "M3QZ!" : "cold") + "\"; }\n");
            paths.add(name);
        }
        PmemSourceImage.Limits limits = new PmemSourceImage.Limits(count, 6_000_000L, 1024, 8192);
        Path packed = pack(root, paths, limits);
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, limits)) {
            var index = PmemCorpusGramIndex.build(image, 32_000_000, () -> false);
            int[] selected = new int[count]; for (int i = 0; i < count; i++) selected[i] = i;
            var got = index.firstHits(image, selected, utf8("M3QZ!"), count, () -> false);
            assertTrue(got.complete()); assertEquals(count, got.selectedFiles());
            int expected = (count + 72) / 73;
            assertEquals(expected, got.hitCount());
            assertTrue(got.candidateFiles() < count / 10, "rarest gram should prune common source text");
            for (int i = 0; i < expected; i++) assertEquals(i * 73, got.fileIndexAt(i));
        }
    }

    @Test void reusedSourceBufferNeverCountsStaleSuffixAfterShortFile() throws Exception {
        Path root = Files.createDirectory(temp.resolve("repository"));
        Files.write(root.resolve("A.java"), utf8("abXY"));
        Files.write(root.resolve("B.java"), utf8("c"));
        Files.write(root.resolve("C.java"), new byte[0]);
        Path packed = pack(root, List.of("C.java", "A.java", "B.java"), SMALL);
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, SMALL)) {
            var index = PmemCorpusGramIndex.build(image, 2_000_000, () -> false);
            assertEquals(index.indexSha256(),
                    PmemCorpusGramIndex.build(image, 2_000_000, () -> false).indexSha256());
            int[] files = {0, 1, 2};
            var exact = index.firstHits(image, files, utf8("abXY"), 3, () -> false);
            assertTrue(exact.complete());
            assertEquals(1, exact.hitCount());
            assertEquals(0, exact.fileIndexAt(0));
            assertEquals(0L, exact.fileOffsetAt(0));
            var absent = index.firstHits(image, files, utf8("XYc"), 3, () -> false);
            assertTrue(absent.complete());
            assertEquals(0, absent.hitCount(), "file boundaries and stale suffixes are opaque");
            var empty = index.firstHits(image, files, new byte[0], 3, () -> false);
            assertTrue(empty.complete());
            assertEquals(3, empty.hitCount());
        }
    }

    @Test void sourceMutationBetweenTwoGramPassesRefusesBeforePublishingIndex() throws Exception {
        Path root = Files.createDirectory(temp.resolve("repository"));
        Files.write(root.resolve("A.java"), utf8("abcd"));
        Files.write(root.resolve("B.java"), utf8("abXbcY"));
        Files.write(root.resolve("C.java"), utf8("cccc"));
        Path packed = pack(root, List.of("B.java", "A.java", "C.java"), SMALL);
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, SMALL)) {
            AtomicInteger callbacks = new AtomicInteger();
            assertThrows(SecurityException.class, () -> PmemCorpusGramIndex.build(image,
                    2_000_000, () -> {
                        // The original two search passes call this once per file.
                        if (callbacks.incrementAndGet() == image.fileCount() + 1) {
                            try {
                                Files.write(packed.resolve("source.bin"), utf8("Z"),
                                        StandardOpenOption.WRITE);
                            } catch (java.io.IOException problem) {
                                throw new UncheckedIOException(problem);
                            }
                        }
                        return false;
                    }));
        }
    }
}
