// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wiring oracle of the bounded source-path index (census L026, docs/SOURCE_PATH_LOOKUP_CONTRACT.md) through the
 * relocated image: {@link M3PmemSourceImage} builds the {@link M3PmemSourcePathIndex} slots when it opens and
 * {@code fileIndexOf} consults them before any binary search. Once the image's own sorted name array is made unsorted
 * (first and last name swapped, their slot ordinals swapped with them) both names still resolve, which a binary search
 * over the same array cannot do; once the slots are cleared a present name is reported absent. A lookup that never
 * reads the slots fails both. Images below the index threshold keep empty slots and the exact fallback. The same
 * discriminators as the donor's module-patched oracle PmemSourcePathWiringContractTest (crate
 * m3-pmem-source-path-wiring-20261106); the oracles of this package run on the classpath, so the private fields are
 * read reflectively here as there.
 */
//FLLDCIM: PIN - recipe/pmem-source-image-20261106 BLOB row of PmemSourceImage (path-index wiring restored on top of verifySealed); this oracle fails on the unwired image (no pathSlots field)
final class M3PmemSourcePathWiringJUnitTest {
    private static final int FILES = 200;
    private static final int TINY = 63;
    private static final M3PmemSourceImage.Limits LIMITS = new M3PmemSourceImage.Limits(256, 65_536, 1_024, 257);

    @TempDir Path temporary;

    private static Object field(M3PmemSourceImage image, String name) throws ReflectiveOperationException {
        Field field = M3PmemSourceImage.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(image);
    }

    private Path pack(String label, int count) throws IOException {
        Path repository = Files.createDirectory(temporary.resolve(label + "-repo"));
        List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name = String.format(Locale.ROOT, "Class%03d.java", i);
            Files.writeString(repository.resolve(name), "final class C" + i + " {}\n");
            names.add(name);
        }
        Path image = temporary.resolve(label + "-image");
        M3PmemSourceImage.pack(repository, names, image, LIMITS);
        return image;
    }

    private static void swapOrdinals(int[] slots, int first, int second) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == first + 1) slots[i] = second + 1;
            else if (slots[i] == second + 1) slots[i] = first + 1;
        }
    }

    @Test
    void openBuildsTheSlotsAndFileIndexOfResolvesOnlyThroughThem() throws Exception {
        try (M3PmemSourceImage image = M3PmemSourceImage.open(pack("indexed", FILES), M3PmemBackend.FILE, LIMITS)) {
            String[] paths = (String[]) field(image, "paths");
            int[] slots = (int[]) field(image, "pathSlots");
            assertArrayEquals(M3PmemSourcePathIndex.build(paths), slots, "open must build the path index slots");
            assertTrue(slots.length >= 2 * FILES, "200 names must be indexed");
            int[] saved = slots.clone();
            String first = paths[0];
            String last = paths[FILES - 1];
            paths[0] = last;
            paths[FILES - 1] = first;
            swapOrdinals(slots, 0, FILES - 1);
            assertTrue(Arrays.binarySearch(paths, first) < 0 && Arrays.binarySearch(paths, last) < 0,
                    "binary search over the unsorted names must miss both moved names");
            assertEquals(FILES - 1, image.fileIndexOf(first), "the indexed lookup must resolve the moved first name");
            assertEquals(0, image.fileIndexOf(last), "the indexed lookup must resolve the moved last name");
            paths[0] = first;
            paths[FILES - 1] = last;
            String middle = paths[FILES / 2];
            Arrays.fill(slots, 0);
            assertEquals(FILES / 2, Arrays.binarySearch(paths, middle), "the sorted names hold the middle name");
            assertEquals(-1, image.fileIndexOf(middle), "an empty slot is the indexed lookup's definitive miss");
            System.arraycopy(saved, 0, slots, 0, slots.length);
            assertEquals(FILES / 2, image.fileIndexOf(middle), "restored slots resolve the middle name");
            assertEquals(0, image.fileIndexOf(first), "restored slots resolve the first name");
        }
    }

    @Test
    void imagesBelowTheThresholdKeepEmptySlotsAndTheExactFallback() throws Exception {
        try (M3PmemSourceImage image = M3PmemSourceImage.open(pack("tiny", TINY), M3PmemBackend.FILE, LIMITS)) {
            assertEquals(0, ((int[]) field(image, "pathSlots")).length, "below 64 names the index stays empty");
            assertEquals(TINY - 1, image.fileIndexOf("Class062.java"), "the binary-search fallback resolves");
            assertEquals(-1, image.fileIndexOf("Class063.java"), "the binary-search fallback misses");
        }
    }
}
