// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import jdk.m3.optane.M3PmemBackend;
import jdk.m3.optane.M3PmemSourceImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** File-backed end-to-end path oracle; the original verified source image is authoritative. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/junit/java/test/PmemSourcePathIndexJUnitTest.java blob 735849e28d29; recipe m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
public final class M3PmemSourcePathIndexJUnitTest {
    @TempDir Path temporary;

    @Test void sortedOrdinalMissNullAndUnicodePreserveOriginalContract() throws Exception {
        Path repository = Files.createDirectory(temporary.resolve("repo"));
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            String path = String.format(java.util.Locale.ROOT, "Class%03d.java", i);
            Files.writeString(repository.resolve(path), "final class C" + i + " {}\n");
            names.add(path);
        }
        Files.writeString(repository.resolve("雪.java"), "final class Snow {}\n");
        names.add("雪.java");
        Collections.reverse(names);
        Path image = temporary.resolve("image");
        var limits = new M3PmemSourceImage.Limits(256, 65536, 1024, 257);
        M3PmemSourceImage.pack(repository, names, image, limits);
        Collections.sort(names);
        try (M3PmemSourceImage source = M3PmemSourceImage.open(image, M3PmemBackend.FILE, limits)) {
            assertEquals(names.size(), source.fileCount());
            for (int i = 0; i < names.size(); i++) {
                assertEquals(i, source.fileIndexOf(names.get(i)), names.get(i));
                assertTrue(source.readSealed(i).length > 0);
            }
            assertEquals(-1, source.fileIndexOf("Class999.java"));
            assertEquals(-1, source.fileIndexOf("class000.java"));
            assertEquals(-1, source.fileIndexOf(""));
            assertThrows(NullPointerException.class, () -> source.fileIndexOf(null));
        }
    }

    @Test void emptyAndTinyImagesRetainBinarySearchBehavior() throws Exception {
        Path repository = Files.createDirectory(temporary.resolve("empty-repo"));
        var limits = new M3PmemSourceImage.Limits(8, 128, 32, 16);
        Path empty = temporary.resolve("empty");
        M3PmemSourceImage.pack(repository, List.of(), empty, limits);
        try (M3PmemSourceImage image = M3PmemSourceImage.open(empty, M3PmemBackend.FILE, limits)) {
            assertEquals(-1, image.fileIndexOf("missing"));
        }
        Files.writeString(repository.resolve("a.java"), "x");
        Path singleton = temporary.resolve("single");
        M3PmemSourceImage.pack(repository, List.of("a.java"), singleton, limits);
        try (M3PmemSourceImage image = M3PmemSourceImage.open(singleton, M3PmemBackend.FILE, limits)) {
            assertEquals(0, image.fileIndexOf("a.java"));
            assertEquals(-1, image.fileIndexOf("b.java"));
        }
    }
}
