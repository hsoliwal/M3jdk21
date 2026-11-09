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

/** Actual PMEMSDK owner tests; optional Linux POSIX JNI parity and 20k files. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign/PmemCorpusSelectedJUnitTest.java blob 7c58d0d2004c; recipe m3/optane/recipe/pmem-gram-index-20261106/relocate.py v3
final class PmemCorpusSelectedJUnitTest {
    @TempDir Path temp;
    private static final PmemSourceImage.Limits SMALL =
            new PmemSourceImage.Limits(8, 400_000, 150_000, 4096);

    private static byte[] utf8(String text) { return text.getBytes(StandardCharsets.UTF_8); }

    private Path sampleRepository() throws Exception {
        Path repo = Files.createDirectory(temp.resolve("repository"));
        Files.write(repo.resolve("A.java"), utf8("ababa"));
        Files.write(repo.resolve("B.java"), utf8("aba"));
        Files.write(repo.resolve("C.java"), utf8("baba"));
        Files.write(repo.resolve("D.java"), new byte[0]);
        Files.write(repo.resolve("E.java"), utf8("🦊aba"));
        return repo;
    }

    private PmemSourceImage image(Path root, List<String> selected, PmemBackend backend,
                                  PmemSourceImage.Limits limits) throws Exception {
        Path snapshot = temp.resolve("snapshot");
        PmemSourceImage.pack(root, selected, snapshot, limits);
        return PmemSourceImage.open(snapshot, backend, limits);
    }

    @Test void selectedResultsAreSourceBoundAndIncludeOnlyRequestedFileOffsets() throws Exception {
        Path repo = sampleRepository();
        try (PmemSourceImage image = image(repo,
                List.of("E.java", "C.java", "A.java", "D.java", "B.java"), PmemBackend.FILE, SMALL)) {
            byte[] pattern = utf8("aba");
            var chosen = PmemCorpusSelectedSearch.firstHits(image, new int[]{1, 3, 4},
                    pattern, 20, () -> false);
            assertTrue(chosen.complete());
            assertEquals(3, chosen.selectedFiles());
            assertEquals(3, chosen.scannedFiles());
            assertEquals(2, chosen.hitCount());
            assertEquals(1, chosen.fileIndexAt(0));
            assertEquals(0L, chosen.fileOffsetAt(0));
            assertEquals(4, chosen.fileIndexAt(1));
            assertEquals(4L, chosen.fileOffsetAt(1));
            assertEquals(image.sourceRoot(), chosen.sourceRoot());
            assertEquals(64, chosen.patternSha256().length());
            assertEquals(64, chosen.selectionSha256().length());
            var other = PmemCorpusSelectedSearch.firstHits(image, new int[]{0, 3, 4},
                    pattern, 20, () -> false);
            assertNotEquals(chosen.selectionSha256(), other.selectionSha256());
            var again = PmemCorpusSelectedSearch.firstHits(image, new int[]{1,3,4},
                    pattern, 1, () -> false);
            assertEquals(chosen.selectionSha256(), again.selectionSha256(),
                    "selection identity is independent of result cap");
            assertFalse(again.complete());
            assertEquals(1, again.scannedFiles());
            assertEquals(3, again.selectedFiles());
        }
    }

    @Test void validatesSelectionBeforeNativeAndLegacyWholeCorpusIsUnchanged() throws Exception {
        Path repo = sampleRepository();
        PmemSourceImage source = image(repo, List.of("A.java", "B.java", "C.java", "D.java", "E.java"),
                PmemBackend.FILE, SMALL);
        try (source) {
            byte[] pattern = utf8("aba");
            for (int[] bad : List.of(new int[]{-1}, new int[]{5}, new int[]{3,3},
                    new int[]{4,3}, new int[]{0,4,4})) {
                assertThrows(IllegalArgumentException.class,
                        () -> PmemCorpusSelectedSearch.firstHits(source, bad, pattern, 5, () -> false));
            }
            assertThrows(IllegalArgumentException.class, () -> PmemCorpusSelectedSearch.firstHits(
                    source, new int[]{0,1}, pattern, 0, () -> false));
            assertThrows(CancellationException.class, () -> PmemCorpusSelectedSearch.firstHits(
                    source, new int[]{0,1}, pattern, 5, () -> true));
            var none = PmemCorpusSelectedSearch.firstHits(source, new int[0], pattern, 5,
                    () -> false);
            assertTrue(none.complete()); assertEquals(0, none.hitCount());
            var empty = PmemCorpusSelectedSearch.firstHits(source, new int[]{0,3,4},
                    new byte[0], 5, () -> false);
            assertTrue(empty.complete()); assertEquals(3, empty.hitCount());
            for (int i=0; i<empty.hitCount(); i++) assertEquals(0L, empty.fileOffsetAt(i));
            var legacy = PmemCorpusSearch.firstHits(source, pattern, 10, () -> false);
            var selectedAll = PmemCorpusSelectedSearch.firstHits(source, new int[]{0,1,2,3,4},
                    pattern, 10, () -> false);
            assertEquals(legacy.hitCount(), selectedAll.hitCount());
            for (int i=0; i<legacy.hitCount(); i++) {
                assertEquals(legacy.fileIndexAt(i), selectedAll.fileIndexAt(i));
                assertEquals(legacy.fileOffsetAt(i), selectedAll.fileOffsetAt(i));
            }
        }
        assertThrows(IllegalStateException.class, () -> PmemCorpusSelectedSearch.firstHits(
                source, new int[]{0}, utf8("aba"), 5, () -> false));
    }

    @Test void originalFileKmpWindowAndBinaryByteOffsetsArePreserved() throws Exception {
        Path repo = Files.createDirectory(temp.resolve("repository"));
        byte[] longFile = new byte[130_000]; Arrays.fill(longFile, (byte)'x');
        System.arraycopy(utf8("aaab"), 0, longFile, 65_534, 4);
        Files.write(repo.resolve("Large.java"), longFile);
        Files.write(repo.resolve("Small.java"), new byte[]{0,1,0,1,2,0});
        try (PmemSourceImage image = image(repo, List.of("Small.java","Large.java"),
                PmemBackend.FILE, SMALL)) {
            var first = PmemCorpusSelectedSearch.firstHits(image, new int[]{0},
                    utf8("aaab"), 5, () -> false);
            assertEquals(1, first.hitCount());
            assertEquals(65_534L, first.fileOffsetAt(0));
            var second = PmemCorpusSelectedSearch.firstHits(image, new int[]{1},
                    new byte[]{0,1,2}, 5, () -> false);
            assertEquals(1, second.hitCount());
            assertEquals(2L, second.fileOffsetAt(0));
        }
    }

    @Test void originalFileAndActualPosixJniMatchWhenExplicitlyRequired() throws Exception {
        if (!Boolean.getBoolean("pmem.corpus.selected.native")) return;
        assertTrue(PmemSdk.capabilities().nativeAvailable(), "actual JNI is mandatory when enabled");
        Path repo = Files.createDirectory(temp.resolve("repository"));
        byte[] content = new byte[130_000]; Arrays.fill(content, (byte)'a');
        content[65_534] = 'b';
        Files.write(repo.resolve("Large.java"), content);
        Files.write(repo.resolve("Small.java"), new byte[]{0,1,0,1,2,0});
        PmemSourceImage.pack(repo, List.of("Large.java", "Small.java"), temp.resolve("snapshot"), SMALL);
        try (PmemSourceImage file = PmemSourceImage.open(temp.resolve("snapshot"), PmemBackend.FILE, SMALL);
             PmemSourceImage nativeRegion = PmemSourceImage.open(temp.resolve("snapshot"), PmemBackend.POSIX, SMALL)) {
            for (byte[] needle : List.of(utf8("aaaaab"), utf8("missing"),
                    new byte[]{0,1,2}, new byte[0])) {
                for (int[] selected : List.of(new int[]{0}, new int[]{1}, new int[]{0,1})) {
                    var a = PmemCorpusSelectedSearch.firstHits(file, selected, needle, 10, () -> false);
                    var b = PmemCorpusSelectedSearch.firstHits(nativeRegion, selected, needle, 10, () -> false);
                    assertEquals(a.selectionSha256(), b.selectionSha256());
                    assertEquals(a.complete(), b.complete());
                    assertEquals(a.hitCount(), b.hitCount());
                    for (int i=0;i<a.hitCount();i++) {
                        assertEquals(a.fileIndexAt(i), b.fileIndexAt(i));
                        assertEquals(a.fileOffsetAt(i), b.fileOffsetAt(i));
                    }
                }
            }
        }
    }

    @Test @EnabledIfSystemProperty(named="pmem.corpus.selected.scale", matches="true")
    void twentyThousandPhysicalFilesAndSparseSelection() throws Exception {
        final int count=20_000;
        Path repo=Files.createDirectory(temp.resolve("repository"));
        List<String> files=new ArrayList<>(count);
        for(int i=0;i<count;i++) {
            String path=String.format(Locale.ROOT,"File%05d.java",i);
            Files.writeString(repo.resolve(path), "final class X" + i + " { String tag = \"" +
                    (i%50==0 ? "M3_MATCH" : "cold") + "\"; }\n");
            files.add(path);
        }
        PmemSourceImage.Limits limits=new PmemSourceImage.Limits(count,6_000_000L,1024,8192);
        int[] selected=new int[400];
        for(int i=0;i<selected.length;i++)selected[i]=50*i;
        try(PmemSourceImage image=image(repo, files,PmemBackend.FILE,limits)) {
            var hits=PmemCorpusSelectedSearch.firstHits(image,selected,utf8("M3_MATCH"),20_000,()->false);
            assertEquals(400,hits.hitCount());
            assertTrue(hits.complete());
            assertEquals(400,hits.scannedFiles());
            for(int i=0;i<400;i++)assertEquals(i*50,hits.fileIndexAt(i));
        }
    }
}
