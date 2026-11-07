// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.eclipse.framework;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Merge-loss leaf 74 differential for M3EclipseIndexedSnapshot.selected. Seeded snapshots (0 to 9,000 payloads,
 * random byte states), every one of the 65,536 byte-mask pairs on small snapshots and random pairs on large ones,
 * plus invalid masks (negative, above 0xff, the int extremes). TRACE hashes, per call, the selected payload ordinals in
 * order (by identity), the result class, or the exception class and message; it must be identical for the master and
 * restored class. ALLOC (the restored behaviour: no copied handle array) reports allocated bytes per selection and may
 * differ.
 *
 * usage: java -cp framework-classes;deps;oracle jdk.m3.eclipse.framework.MergeLossSelectionDifferential snapshots seed
 */
public final class MergeLossSelectionDifferential {
    private static final int[] INVALID = {-1, 256, 257, 0x1ff, Integer.MIN_VALUE, Integer.MAX_VALUE};

    public static void main(String[] args) throws Exception {
        int snapshots = Integer.parseInt(args[0]);
        SplittableRandom random = new SplittableRandom(Long.decode(args[1]));
        MessageDigest trace = MessageDigest.getInstance("SHA-256");
        long calls = 0, refusals = 0, selected = 0;
        for (int s = 0; s < snapshots; s++) {
            int size = s % 10 == 0 ? random.nextInt(9000) : random.nextInt(300);
            M3EclipseIndexedSnapshot<Object> snapshot = new M3EclipseIndexedSnapshot<>();
            Map<Object, Integer> ordinal = new IdentityHashMap<>();
            int stateBias = random.nextInt(256);
            for (int i = 0; i < size; i++) {
                Object payload = new Object();
                ordinal.put(payload, i);
                snapshot.add(payload, random.nextInt(4) == 0 ? stateBias : random.nextInt(256));
            }
            boolean exhaustive = size <= 64 && s % 50 == 1;
            int pairs = exhaustive ? 65536 : 64;
            for (int p = 0; p < pairs; p++) {
                int required = exhaustive ? p >>> 8 : random.nextInt(256) & random.nextInt(256);
                int excluded = exhaustive ? p & 0xff : random.nextInt(256) & random.nextInt(256) & ~required;
                if (!exhaustive && p % 16 == 0) required = 0;
                if (!exhaustive && p % 16 == 1) excluded = 0;
                selected += call(snapshot, ordinal, required, excluded, trace);
                calls++;
            }
            for (int invalid : INVALID) {
                for (int[] masks : new int[][] {{invalid, 0}, {0, invalid}, {invalid, invalid}}) {
                    calls++;
                    try {
                        call(snapshot, ordinal, masks[0], masks[1], trace);
                        feed(trace, "accepted");
                    } catch (RuntimeException refused) {
                        refusals++;
                        feed(trace, refused.getClass().getName() + ":" + refused.getMessage());
                    }
                }
            }
        }
        System.out.printf("TRACE snapshots=%d calls=%d selected=%d refusals=%d sha256=%s%n", snapshots, calls,
                selected, refusals, HexFormat.of().formatHex(trace.digest()));
        alloc();
    }

    private static int call(M3EclipseIndexedSnapshot<Object> snapshot, Map<Object, Integer> ordinal, int required,
                            int excluded, MessageDigest trace) {
        List<Object> result = snapshot.selected(required, excluded);
        StringBuilder line = new StringBuilder().append(required).append('/').append(excluded).append(':')
                .append(result.getClass().getName()).append(':');
        for (Object payload : result) line.append(ordinal.get(payload)).append(',');
        feed(trace, line.toString());
        return result.size();
    }

    private static void feed(MessageDigest trace, String value) {
        trace.update(value.getBytes(StandardCharsets.UTF_8));
        trace.update((byte) '\n');
    }

    /** Allocated bytes per selected() on 65,536 payloads (states i & 255) for five mask workloads. */
    private static void alloc() {
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().threadId();
        M3EclipseIndexedSnapshot<Object> snapshot = new M3EclipseIndexedSnapshot<>();
        for (int i = 0; i < 65536; i++) snapshot.add(new Object(), i & 255);
        int[][] masks = {{0, 0}, {1, 0}, {255, 0}, {0, 254}, {1, 1}};
        String[] names = {"all", "half", "sparse", "excluded", "overlap"};
        StringBuilder out = new StringBuilder("ALLOC bytes_per_call");
        for (int w = 0; w < masks.length; w++) {
            for (int i = 0; i < 64; i++) snapshot.selected(masks[w][0], masks[w][1]);
            long before = bean.getThreadAllocatedBytes(thread);
            for (int i = 0; i < 64; i++) snapshot.selected(masks[w][0], masks[w][1]);
            out.append(' ').append(names[w]).append('=').append((bean.getThreadAllocatedBytes(thread) - before) / 64);
        }
        System.out.println(out);
    }
}
