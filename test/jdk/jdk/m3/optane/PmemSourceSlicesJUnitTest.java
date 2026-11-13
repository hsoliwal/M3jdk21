// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import jdk.m3.optane.PmemAccess;
import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemRegion;
import jdk.m3.optane.PmemSdk;
import jdk.m3.optane.PmemSourceSlices;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Buffer and source-lifetime regressions against the real POSIX JNI mapping. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/junit/java/test/PmemSourceSlicesJUnitTest.java blob cce4288cc865; recipe make/m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
public final class PmemSourceSlicesJUnitTest {
    private static final int EMPTY_INDEX = 0;
    private static final int LONG_INDEX = 1;
    private static final int SHORT_INDEX = 2;
    private static final int LONG_LENGTH = 6;
    private static final int SHORT_LENGTH = 2;
    private static final byte SENTINEL = 90;
    private static final byte[] EMPTY_SOURCE = {};
    private static final byte[] LONG_SOURCE = {0, 1, 2, -1, 127, -128};
    private static final byte[] SHORT_SOURCE = {65, 66};
    private static final byte[] IMAGE = {0, 1, 2, -1, 127, -128, 65, 66};

    @TempDir
    Path directory;

    private PmemRegion openImage() throws Exception {
        Path path = directory.resolve("source-image.bin");
        Files.write(path, IMAGE);
        return PmemSdk.open(path, 0, IMAGE.length, PmemAccess.READ_ONLY, PmemBackend.POSIX);
    }

    private static PmemSourceSlices sources(PmemRegion mapping) throws Exception {
        byte[] hashes = new byte[3 * 32];
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        System.arraycopy(sha256.digest(EMPTY_SOURCE), 0, hashes, 0, 32);
        System.arraycopy(sha256.digest(LONG_SOURCE), 0, hashes, 32, 32);
        System.arraycopy(sha256.digest(SHORT_SOURCE), 0, hashes, 64, 32);
        return new PmemSourceSlices(mapping, new long[]{0, 0, LONG_LENGTH},
                new int[]{0, LONG_LENGTH, SHORT_LENGTH}, hashes);
    }

    @Test
    void emptySourceRequiresAnOpenMappingForBothReadApis() throws Exception {
        PmemSourceSlices slices;
        byte[] target = {SENTINEL};
        try (PmemRegion mapping = openImage()) {
            slices = sources(mapping);
            assertArrayEquals(EMPTY_SOURCE, slices.readSealed(EMPTY_INDEX));
            assertEquals(0, slices.readSealed(EMPTY_INDEX, target, target.length));
            assertArrayEquals(new byte[]{SENTINEL}, target);
        }
        assertThrows(IllegalStateException.class, () -> slices.readSealed(EMPTY_INDEX));
        assertThrows(IllegalStateException.class,
                () -> slices.readSealed(EMPTY_INDEX, target, target.length));
    }

    @Test
    void destinationOffsetPreservesBothSentinelMargins() throws Exception {
        try (PmemRegion mapping = openImage()) {
            PmemSourceSlices slices = sources(mapping);
            byte[] target = new byte[12];
            Arrays.fill(target, SENTINEL);
            assertEquals(LONG_LENGTH, slices.readSealed(LONG_INDEX, target, 3));
            assertArrayEquals(new byte[]{90, 90, 90, 0, 1, 2, -1, 127, -128, 90, 90, 90},
                    target);
        }
    }

    @Test
    void shorterReadVerifiesOnlyItsExtentInAReusedBuffer() throws Exception {
        try (PmemRegion mapping = openImage()) {
            PmemSourceSlices slices = sources(mapping);
            byte[] target = new byte[8];
            Arrays.fill(target, SENTINEL);
            assertEquals(LONG_LENGTH, slices.readSealed(LONG_INDEX, target, 1));
            assertEquals(SHORT_LENGTH, slices.readSealed(SHORT_INDEX, target, 1));
            assertArrayEquals(new byte[]{90, 65, 66, 2, -1, 127, -128, 90}, target);
        }
    }

    @Test
    void invalidDestinationIsRejectedBeforeAccessingAClosedMapping() throws Exception {
        PmemSourceSlices slices;
        try (PmemRegion mapping = openImage()) {
            slices = sources(mapping);
        }
        byte[] target = new byte[LONG_LENGTH];
        assertThrows(NullPointerException.class,
                () -> slices.readSealed(LONG_INDEX, null, 0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> slices.readSealed(LONG_INDEX, target, -1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> slices.readSealed(LONG_INDEX, target, 1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> slices.readSealed(LONG_INDEX, target, Integer.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class,
                () -> slices.readSealed(EMPTY_INDEX, target, target.length + 1));
        assertArrayEquals(new byte[LONG_LENGTH], target);
    }

    @Test
    void invalidSourceIndexIsRejectedBeforeAccessingAClosedMapping() throws Exception {
        PmemSourceSlices slices;
        try (PmemRegion mapping = openImage()) {
            slices = sources(mapping);
        }
        byte[] target = new byte[LONG_LENGTH];
        assertThrows(IndexOutOfBoundsException.class,
                () -> slices.readSealed(-1, target, 0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> slices.readSealed(3, target, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> slices.readSealed(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> slices.readSealed(3));
    }

    @Test
    void validNonemptyReadIsRejectedAfterMappingClose() throws Exception {
        PmemSourceSlices slices;
        try (PmemRegion mapping = openImage()) {
            slices = sources(mapping);
        }
        assertThrows(IllegalStateException.class,
                () -> slices.readSealed(LONG_INDEX, new byte[LONG_LENGTH], 0));
        assertThrows(IllegalStateException.class, () -> slices.readSealed(LONG_INDEX));
    }

    @Test
    void changedMappedBytesAreRefusedByBothReadApis() throws Exception {
        try (PmemRegion mapping = openImage()) {
            PmemSourceSlices slices = sources(mapping);
            try (RandomAccessFile file = new RandomAccessFile(mapping.path().toFile(), "rw")) {
                file.seek(LONG_LENGTH);
                file.writeByte(88);
            }
            assertThrows(SecurityException.class,
                    () -> slices.readSealed(SHORT_INDEX, new byte[SHORT_LENGTH + 4], 2));
            assertThrows(SecurityException.class, () -> slices.readSealed(SHORT_INDEX));
        }
    }

    @Test
    void owningArrayReadsRemainDetachedFromEachOtherAndTheMapping() throws Exception {
        try (PmemRegion mapping = openImage()) {
            PmemSourceSlices slices = sources(mapping);
            byte[] first = slices.readSealed(LONG_INDEX);
            byte[] second = slices.readSealed(LONG_INDEX);
            assertNotSame(first, second);
            first[0] = 99;
            assertArrayEquals(LONG_SOURCE, second);
            assertArrayEquals(LONG_SOURCE, slices.readSealed(LONG_INDEX));
        }
    }

    @Test
    @Timeout(30)
    void concurrentReadersUseIndependentBuffersWithoutDigestInterference() throws Exception {
        final int workers = 4;
        final int iterations = 500;
        try (PmemRegion mapping = openImage();
             ExecutorService executor = Executors.newFixedThreadPool(workers)) {
            PmemSourceSlices slices = sources(mapping);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Void>> tasks = new ArrayList<>();
            for (int worker = 0; worker < workers; worker++) {
                tasks.add(executor.submit(() -> {
                    byte[] target = new byte[LONG_LENGTH + 4];
                    Arrays.fill(target, SENTINEL);
                    start.await();
                    for (int iteration = 0; iteration < iterations; iteration++) {
                        int index = (iteration & 1) == 0 ? LONG_INDEX : SHORT_INDEX;
                        byte[] expected = index == LONG_INDEX ? LONG_SOURCE : SHORT_SOURCE;
                        assertEquals(expected.length, slices.readSealed(index, target, 2));
                        assertArrayEquals(expected, Arrays.copyOfRange(target, 2, 2 + expected.length));
                        assertEquals(SENTINEL, target[0]);
                        assertEquals(SENTINEL, target[target.length - 1]);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<Void> task : tasks) task.get(20, TimeUnit.SECONDS);
        }
    }

    @Test
    void sliceOffsetsRemainRelativeToAMappingBeyondTheTwoGiBFileBoundary() throws Exception {
        final long fileOffset = (1L << 31) + 123;
        Path path = directory.resolve("sparse-source-image.bin");
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) {
            file.setLength(fileOffset + IMAGE.length);
            file.seek(fileOffset);
            file.write(IMAGE);
        }
        try (PmemRegion mapping = PmemSdk.open(path, fileOffset, IMAGE.length,
                PmemAccess.READ_ONLY, PmemBackend.POSIX)) {
            PmemSourceSlices slices = sources(mapping);
            byte[] target = new byte[SHORT_LENGTH];
            assertEquals(SHORT_LENGTH, slices.readSealed(SHORT_INDEX, target, 0));
            assertArrayEquals(SHORT_SOURCE, target);
            assertArrayEquals(LONG_SOURCE, slices.readSealed(LONG_INDEX));
        }
    }
}
