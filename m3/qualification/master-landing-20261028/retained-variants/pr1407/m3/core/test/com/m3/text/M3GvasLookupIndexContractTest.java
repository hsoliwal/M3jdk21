/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import com.m3.compiletime.M3GvasLiteralImageWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
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

    public static void main(String[] args) throws Exception {
        if (args.length == 3 && args[0].equals("emit")) {
            var result = M3GvasLiteralImageWriter.build(fixtures(args[2]));
            M3GvasLiteralImageWriter.writeNew(Path.of(args[1]), result);
            System.out.println("M3_GVAS_INDEX_EMIT sha256=" + result.sha256());
            return;
        }
        if (args.length != 5) throw new IllegalArgumentException("kind library image digest checks|bench");
        List<String> words = fixtures(args[0]);
        M3FixedAddressMapping.Image mapping = M3FixedAddressMapping.open(Path.of(args[2]),
                M3FixedAddressMapping.DEFAULT_BASE, args[3], Path.of(args[1]));
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
            long sum = 0;
            for (int repetition = 0; repetition < 20; repetition++) {
                for (String value : needles) sum += image.findRow(value);
            }
            consumed = sum;
            com.sun.management.ThreadMXBean bean =
                    (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            boolean allocations = bean.isThreadAllocatedMemorySupported();
            if (allocations && !bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
            long thread = Thread.currentThread().threadId();
            long allocatedBefore = allocations ? bean.getThreadAllocatedBytes(thread) : -1;
            long start = System.nanoTime();
            for (int repetition = 0; repetition < 100; repetition++) {
                for (String value : needles) sum += image.findRow(value);
            }
            long lookupNanos = System.nanoTime() - start;
            long allocated = allocations ? bean.getThreadAllocatedBytes(thread) - allocatedBefore : -1;
            consumed = sum;
            start = System.nanoTime();
            for (int repetition = 0; repetition < 100; repetition++) {
                for (int row = 0; row < words.size(); row++) {
                    sum = Long.rotateLeft(sum, 7) ^ image.addressAt(row);
                }
            }
            long reverseNanos = System.nanoTime() - start;
            consumed = sum;
            System.out.println("M3_GVAS_INDEX_COST words=" + words.size() + " operations="
                    + (100L * words.size()) + " prepare_ns=" + prepareNanos
                    + " lookup_ns=" + lookupNanos + " reverse_ns=" + reverseNanos
                    + " allocated_lookup_bytes=" + allocated + " retained_index_payload_bytes=" + metadata
                    + " mapped_bytes=" + mapping.byteLength() + " consumed=" + consumed);
        }
        System.out.println("M3_GVAS_INDEX_PASS kind=" + args[0] + " checks=" + (checks + 3)
                + " retained_index_payload_bytes=" + metadata);
    }
}
