/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import com.m3.compiletime.M3GvasLiteralImageWriter;
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
=======
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
>>>>>>> ca4965cde9af1ab10d52cb2f27d38dcbbb34904c
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/** Actual mapped-owner oracle: prepared lookup, exact collisions and bounded fallback. */
public final class M3GvasLookupIndexContractTest {
    private static volatile long consumed;

    private M3GvasLookupIndexContractTest() { }

    private static List<String> fixtures(String kind) {
        TreeSet<String> values = new TreeSet<>();
        if (kind.startsWith("collision")) {
            int count = switch (kind) {
                case "collision64" -> 64;
                case "collision65" -> 65;
                case "collision" -> 256;
                default -> throw new IllegalArgumentException("unknown fixture: " + kind);
            };
            for (int bits = 0; bits < count; bits++) {
                StringBuilder value = new StringBuilder();
                for (int shift = 0; shift < 8; shift++) {
                    value.append((bits & (1 << shift)) == 0 ? "Aa" : "BB");
                }
                values.add(value.toString());
            }
        } else {
            Collections.addAll(values, "", "\0", "Aa", "BB", "a\0b", "\ud800", "\udc00",
                    "\ud800\udc00", "\uffff", "caf\u00e9", "e\u0301");
            if (kind.equals("normal")) {
                Random random = new Random(0x4d3347564153L);
                for (int row = 0; row < 8192; row++) {
                    StringBuilder value = new StringBuilder("common-prefix-");
                    value.append(row).append('-');
                    for (int i = 0; i < 12; i++) value.append((char) random.nextInt(65536));
                    values.add(value.toString());
                }
            } else if (!kind.equals("small")) {
                throw new IllegalArgumentException("unknown fixture: " + kind);
            }
        }
        return List.copyOf(values);
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    private static int metadataBytes(M3GvasLiteralImage image) throws Exception {
        try {
            Field field = M3GvasLiteralImage.class.getDeclaredField("lookupRows");
            field.setAccessible(true);
            int[] slots = (int[]) field.get(image);
            return slots == null ? 0 : Math.multiplyExact(slots.length, Integer.BYTES);
        } catch (NoSuchFieldException baseline) {
            return -1;
        }
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
=======
    private static void nativePathFailure(Method method, Class<? extends Throwable> expected,
            String path, long address, long length) throws Exception {
        try {
            method.invoke(null, path, address, length);
            throw new AssertionError("native filename was not refused");
        } catch (InvocationTargetException rejected) {
            require(expected.isInstance(rejected.getCause()),
                    "native filename refusal type: " + rejected.getCause());
        }
    }

    private static void pathnames(Path library, Path work) throws Exception {
        List<String> names = List.of("ascii", "caf\u00e9", "cafe\u0301", "\u4f60\u597d",
                "\u0939\u093f\u0928\u094d\u0926\u0940", "\ud800\udc00",
                "\ud83d\ude80", "\udbff\udfff");
        List<String> words = fixtures("small");
        var compiled = M3GvasLiteralImageWriter.build(words);
        Method nativeMap = M3FixedAddressMapping.class.getDeclaredMethod("mapExact0",
                String.class, long.class, long.class);
        nativeMap.setAccessible(true);
        long checks = 0;
        for (int index = 0; index < names.size(); index++) {
            String name = names.get(index);
            Path directory = Files.createDirectories(work.resolve("folder-" + name));
            Path file = directory.resolve("image-" + name + ".m3gvas");
            M3GvasLiteralImageWriter.writeNew(file, compiled);
            long address = M3FixedAddressMapping.DEFAULT_BASE + index * 0x20000L;
            M3FixedAddressMapping.Image mapping = M3FixedAddressMapping.open(
                    file, address, compiled.sha256(), library);
            M3GvasLiteralImage image = M3GvasLiteralImage.open(mapping);
            require(mapping.address() == address, "pathname retains exact fixed address");
            require(mapping.file().equals(file.toRealPath()), "exact canonical pathname");
            require(mapping == M3FixedAddressMapping.open(file, address, compiled.sha256(), library),
                    "repeated pathname admission preserves process-lifetime owner");
            checks += 3;
            for (int row = 0; row < words.size(); row++) {
                String spelling = words.get(row);
                require(image.findRow(spelling) == row, "pathname UTF-16 row identity");
                require(image.addressOf(spelling) == image.addressAt(row), "pathname reverse address");
                require(image.hashCodeAt(row) == spelling.hashCode(), "pathname mapped hash");
                checks += 3;
            }
            long retryAddress = address + 0x10000L;
            nativePathFailure(nativeMap, IllegalArgumentException.class, null,
                    retryAddress, compiled.bytes().length);
            nativePathFailure(nativeMap, IOException.class, file + "\0suffix",
                    retryAddress, compiled.bytes().length);
            nativePathFailure(nativeMap, IOException.class, file + "-absent",
                    retryAddress, compiled.bytes().length);
            try {
                M3FixedAddressMapping.open(file, retryAddress, "0".repeat(64), library);
                throw new AssertionError("wrong digest was accepted");
            } catch (IOException rejected) {
                require(rejected.getMessage().contains("SHA-256 mismatch"),
                        "digest rejection reached the mapped image");
            }
            M3FixedAddressMapping.Image retry = M3FixedAddressMapping.open(
                    file, retryAddress, compiled.sha256(), library);
            require(retry.address() == retryAddress, "failed admission released the address");
            M3GvasLiteralImage retryImage = M3GvasLiteralImage.open(retry);
            require(retryImage.addressOf("Aa") - retryAddress == image.addressOf("Aa") - address,
                    "retry retains the same immutable image coordinate");
            checks += 6;
        }
        System.out.println("M3_GVAS_NATIVE_PATH_PASS paths=" + names.size() + " checks=" + checks
                + " encoder=sun.nio.fs.Util.toBytes");
    }

>>>>>>> ca4965cde9af1ab10d52cb2f27d38dcbbb34904c
    public static void main(String[] args) throws Exception {
        if (args.length == 3 && args[0].equals("emit")) {
            var result = M3GvasLiteralImageWriter.build(fixtures(args[2]));
            M3GvasLiteralImageWriter.writeNew(Path.of(args[1]), result);
            System.out.println("M3_GVAS_INDEX_EMIT sha256=" + result.sha256());
            return;
        }
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        if (args.length != 5) throw new IllegalArgumentException("kind library image digest checks|bench");
        List<String> words = fixtures(args[0]);
        M3FixedAddressMapping.Image mapping = M3FixedAddressMapping.open(Path.of(args[2]),
                M3FixedAddressMapping.DEFAULT_BASE, args[3], Path.of(args[1]));
=======
        if (args.length == 3 && args[0].equals("paths")) {
            pathnames(Path.of(args[1]), Path.of(args[2]));
            return;
        }
        if (args.length != 5) throw new IllegalArgumentException("kind library image digest checks|bench");
        List<String> words = fixtures(args[0]);
        long mappingStart = System.nanoTime();
        M3FixedAddressMapping.Image mapping = M3FixedAddressMapping.open(Path.of(args[2]),
                M3FixedAddressMapping.DEFAULT_BASE, args[3], Path.of(args[1]));
        long mappingNanos = System.nanoTime() - mappingStart;
>>>>>>> ca4965cde9af1ab10d52cb2f27d38dcbbb34904c
        long prepareStart = System.nanoTime();
        M3GvasLiteralImage image = M3GvasLiteralImage.open(mapping);
        long prepareNanos = System.nanoTime() - prepareStart;
        require(image.size() == words.size(), "row count");
        long checks = 1;
        for (int row = 0; row < words.size(); row++) {
            String value = words.get(row);
            require(image.findRow(value) == row, "same lexicographic row identity");
            require(image.addressOf(value) == image.addressAt(row), "same canonical numeric pointer");
            require(image.hashCodeAt(row) == value.hashCode(), "mapped hash");
            require(image.utf16LengthAt(row) == value.length(), "mapped UTF-16 length");
            String absent = value + '\uffff';
            int expected = Collections.binarySearch(words, absent);
            require(image.findRow(absent) == (expected < 0 ? -1 : expected), "exact miss/hit oracle");
            checks += 5;
        }
        require(image.findRow("missing-exact-value") == -1, "ordinary miss");
        int metadata = metadataBytes(image);
        if (metadata >= 0) {
            boolean prepared = args[0].equals("normal") || args[0].equals("collision64");
            require((metadata != 0) == prepared, "small/collision/probe-boundary admission");
            require(metadata <= 16 * 1024 * 1024, "metadata budget");
        }
        if (args[4].equals("bench")) {
            List<String> randomized = new ArrayList<>(words);
            Collections.shuffle(randomized, new Random(0x3a11L));
            String[] needles = randomized.toArray(String[]::new);
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
            long sum = 0;
            for (int repetition = 0; repetition < 20; repetition++) {
                for (String value : needles) sum += image.findRow(value);
=======
            String[] misses = new String[needles.length];
            for (int index = 0; index < needles.length; index++) {
                String absent = needles[index] + '\uffff';
                while (image.findRow(absent) >= 0) absent += '\uffff';
                misses[index] = absent;
            }
            long sum = 0;
            for (int repetition = 0; repetition < 20; repetition++) {
                for (String value : needles) sum += image.findRow(value);
                for (String value : misses) sum += image.findRow(value);
                for (int row = 0; row < words.size(); row++) {
                    sum = Long.rotateLeft(sum, 7) ^ image.addressAt(row);
                }
>>>>>>> ca4965cde9af1ab10d52cb2f27d38dcbbb34904c
            }
            consumed = sum;
            com.sun.management.ThreadMXBean bean =
                    (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            boolean allocations = bean.isThreadAllocatedMemorySupported();
            if (allocations && !bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
            long thread = Thread.currentThread().threadId();
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
=======
            sum = 0;
>>>>>>> ca4965cde9af1ab10d52cb2f27d38dcbbb34904c
            long allocatedBefore = allocations ? bean.getThreadAllocatedBytes(thread) : -1;
            long start = System.nanoTime();
            for (int repetition = 0; repetition < 100; repetition++) {
                for (String value : needles) sum += image.findRow(value);
            }
            long lookupNanos = System.nanoTime() - start;
            long allocated = allocations ? bean.getThreadAllocatedBytes(thread) - allocatedBefore : -1;
            consumed = sum;
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
=======
            long hitChecksum = sum;
            sum = 0;
            allocatedBefore = allocations ? bean.getThreadAllocatedBytes(thread) : -1;
            start = System.nanoTime();
            for (int repetition = 0; repetition < 100; repetition++) {
                for (String value : misses) sum += image.findRow(value);
            }
            long missNanos = System.nanoTime() - start;
            long allocatedMiss = allocations ? bean.getThreadAllocatedBytes(thread) - allocatedBefore : -1;
            consumed = sum;
            long missChecksum = sum;
            sum = 0;
            allocatedBefore = allocations ? bean.getThreadAllocatedBytes(thread) : -1;
>>>>>>> ca4965cde9af1ab10d52cb2f27d38dcbbb34904c
            start = System.nanoTime();
            for (int repetition = 0; repetition < 100; repetition++) {
                for (int row = 0; row < words.size(); row++) {
                    sum = Long.rotateLeft(sum, 7) ^ image.addressAt(row);
                }
            }
            long reverseNanos = System.nanoTime() - start;
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
            consumed = sum;
=======
            long allocatedReverse = allocations ? bean.getThreadAllocatedBytes(thread) - allocatedBefore : -1;
            consumed = sum;
            require(hitChecksum == 100L * words.size() * (words.size() - 1) / 2,
                    "timed hit checksum");
            require(missChecksum == -100L * words.size(), "timed miss checksum");
>>>>>>> ca4965cde9af1ab10d52cb2f27d38dcbbb34904c
            System.out.println("M3_GVAS_INDEX_COST words=" + words.size() + " operations="
                    + (100L * words.size()) + " prepare_ns=" + prepareNanos
                    + " lookup_ns=" + lookupNanos + " reverse_ns=" + reverseNanos
                    + " allocated_lookup_bytes=" + allocated + " retained_index_payload_bytes=" + metadata
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
                    + " mapped_bytes=" + mapping.byteLength() + " consumed=" + consumed);
=======
                    + " mapped_bytes=" + mapping.byteLength() + " consumed=" + consumed
                    + " mapping_ns=" + mappingNanos + " miss_ns=" + missNanos
                    + " allocated_miss_bytes=" + allocatedMiss
                    + " allocated_reverse_bytes=" + allocatedReverse
                    + " hit_checksum=" + hitChecksum + " miss_checksum=" + missChecksum);
>>>>>>> ca4965cde9af1ab10d52cb2f27d38dcbbb34904c
        }
        System.out.println("M3_GVAS_INDEX_PASS kind=" + args[0] + " checks=" + (checks + 3)
                + " retained_index_payload_bytes=" + metadata);
    }
}
