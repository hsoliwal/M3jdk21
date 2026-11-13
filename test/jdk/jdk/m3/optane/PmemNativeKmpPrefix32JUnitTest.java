// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import jdk.m3.optane.PmemAccess;
import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemRegion;
import jdk.m3.optane.PmemSdk;
import jdk.m3.optane.PmemSearch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Actual JNI POSIX mmap parity against the Java FILE kernel and naive first-hit oracle. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/junit/java/test/PmemNativeKmpPrefix32JUnitTest.java blob db6ce8fa696b; recipe make/m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
public final class PmemNativeKmpPrefix32JUnitTest {
    @TempDir Path temporary;

    private static long naive(byte[] source, int start, int count, byte[] needle) {
        if (needle.length == 0) return start;
        outer: for (int i = start; i <= start + count - needle.length; i++) {
            for (int j = 0; j < needle.length; j++)
                if (source[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }

    @Test void oneMiBRepeatedPrefixAtLastPossibleOffsetAndMiss() throws Exception {
        int m = PmemSearch.MAX_PATTERN_BYTES;
        byte[] pattern = new byte[m];
        byte[] data = new byte[m * 2 + 9];
        Arrays.fill(pattern, (byte) 'a');
        Arrays.fill(data, (byte) 'a');
        pattern[m - 1] = (byte) 'b';
        data[data.length - 1] = (byte) 'b';
        Path file = temporary.resolve("max-pattern.bin");
        Files.write(file, data);
        try (PmemRegion nativeRegion = PmemSdk.open(file, 0, data.length,
                    PmemAccess.READ_ONLY, PmemBackend.POSIX);
             PmemRegion fileRegion = PmemSdk.open(file, 0, data.length,
                    PmemAccess.READ_ONLY, PmemBackend.FILE)) {
            long expected = data.length - m;
            assertEquals(expected, PmemSearch.indexOf(nativeRegion, 0, data.length, pattern));
            assertEquals(expected, PmemSearch.indexOf(fileRegion, 0, data.length, pattern));
            // The truncated search window excludes the only trailing 'b'.
            assertEquals(-1, PmemSearch.indexOf(nativeRegion, 0, data.length - 1, pattern));
            assertEquals(-1, PmemSearch.indexOf(fileRegion, 0, data.length - 1, pattern));
            assertEquals(data.length, PmemSearch.indexOf(nativeRegion, data.length, 0, new byte[0]));
            assertEquals(-1, PmemSearch.indexOf(nativeRegion, data.length, 0, new byte[]{1}));
            assertThrows(IllegalArgumentException.class, () ->
                    PmemSearch.indexOf(nativeRegion, 0, data.length, new byte[m + 1]));
        }
    }

    @Test void binaryRandomizedSearchSlicesMatchIndependentFirstHitOracle() throws Exception {
        Random random = new Random(0x5A1E2026L);
        byte[] data = new byte[131_099];
        random.nextBytes(data);
        Path file = temporary.resolve("binary-search.bin");
        Files.write(file, data);
        try (PmemRegion nativeRegion = PmemSdk.open(file, 0, data.length,
                    PmemAccess.READ_ONLY, PmemBackend.POSIX);
             PmemRegion fileRegion = PmemSdk.open(file, 0, data.length,
                    PmemAccess.READ_ONLY, PmemBackend.FILE)) {
            for (int run = 0; run < 320; run++) {
                int start = random.nextInt(data.length + 1);
                int count = random.nextInt(data.length - start + 1);
                byte[] pattern = new byte[random.nextInt(90)];
                random.nextBytes(pattern);
                if (pattern.length <= count && (run & 1) == 0) {
                    int selected = start + random.nextInt(count - pattern.length + 1);
                    System.arraycopy(data, selected, pattern, 0, pattern.length);
                }
                long expected = naive(data, start, count, pattern);
                assertEquals(expected, PmemSearch.indexOf(nativeRegion, start, count, pattern),
                        "native run " + run);
                assertEquals(expected, PmemSearch.indexOf(fileRegion, start, count, pattern),
                        "FILE run " + run);
            }
            for (byte[] needle : List.of(new byte[]{0}, new byte[]{(byte) 0xff}, new byte[0])) {
                long expected = naive(data, 0, data.length, needle);
                assertEquals(expected, PmemSearch.indexOf(nativeRegion, 0, data.length, needle));
            }
        }
    }
}
