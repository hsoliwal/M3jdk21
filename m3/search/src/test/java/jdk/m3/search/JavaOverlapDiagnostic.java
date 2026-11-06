// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// Relocated from com.synexia m3-fast-search/src/test/java/com/synexia/m3/search/JavaOverlapDiagnostic.java (blob 6fe7841c485f) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

/** Bounded contract observations and non-gating, setup-separated allocation/time diagnostics. */
public final class JavaOverlapDiagnostic {
    private static volatile long sink;
    private static final IntConsumer DISCARD = position -> {};

    private JavaOverlapDiagnostic() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--behavior")) behavior();
        else if (args.length == 1 && args[0].equals("--cost")) cost();
        else throw new IllegalArgumentException("expected --behavior or --cost");
    }

    private static void behavior() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Random random = new Random(0x0b5e7eL);
        int cases = 0;
        for (int sample = 0; sample < 300; sample++) {
            int period = 1 + random.nextInt(12);
            byte[] needle = periodic(1 + random.nextInt(96), period);
            byte[] haystack = periodic(random.nextInt(400), period);
            if (haystack.length > 0 && (sample & 1) == 0) haystack[random.nextInt(haystack.length)] = -1;
            for (M3CompiledSearch compiled : new M3CompiledSearch[] {
                    new M3KmpSearch(needle), new M3BoyerMooreHorspoolSearch(needle),
                    new M3JavaAdaptiveSearchEngine().compile(needle)}) {
                try (compiled) {
                    for (int from : new int[] {-5, 0, 1, haystack.length / 2, Integer.MAX_VALUE}) {
                        List<Integer> expected = oracle(haystack, needle, from);
                        List<Integer> observed = new ArrayList<>();
                        int delivered = M3ByteOccurrences.visit(compiled, haystack, from, Integer.MAX_VALUE, observed::add, null);
                        int count = M3ByteOccurrences.count(compiled, haystack, from, null);
                        if (!expected.equals(observed) || count != expected.size() || delivered != count) {
                            throw new AssertionError("oracle mismatch " + sample + " " + compiled.strategy());
                        }
                        String observation = sample + "|" + compiled.strategy() + "|" + from + "|"
                                + count + "|" + observed + "|" + compiled.indexOf(haystack, from) + "|"
                                + compiled.lastIndexOf(haystack, from) + "|"
                                + compiled.indexOf(ByteBuffer.wrap(haystack), from) + "|"
                                + compiled.lastIndexOf(ByteBuffer.wrap(haystack), from) + "\n";
                        digest.update(observation.getBytes(StandardCharsets.UTF_8));
                        cases++;
                    }
                }
            }
        }
        System.out.println("{\"cases\":" + cases + ",\"behavior_sha256\":\""
                + HexFormat.of().formatHex(digest.digest()) + "\"}");
    }

    private static void cost() {
        var bean = ManagementFactory.getThreadMXBean();
        com.sun.management.ThreadMXBean allocation = bean instanceof com.sun.management.ThreadMXBean supported
                && supported.isThreadAllocatedMemorySupported() ? supported : null;
        if (allocation != null && !allocation.isThreadAllocatedMemoryEnabled()) allocation.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        byte[] periodicNeedle = periodic(1536, 3);
        byte[] periodicHaystack = periodic(65_536, 3);
        byte[] sparseNeedle = new byte[1536];
        byte[] sparseHaystack = new byte[65_536];
        Random random = new Random(0x0c057L);
        random.nextBytes(sparseNeedle);
        random.nextBytes(sparseHaystack);
        for (int scenario = 0; scenario < 5; scenario++) {
            byte[] needle = scenario == 1 ? sparseNeedle : periodicNeedle;
            byte[] haystack = scenario == 0 ? periodicHaystack : sparseHaystack;
            String label = switch (scenario) {
                case 0 -> "periodic-overlaps";
                case 1 -> "diverse-no-match";
                case 2 -> "one-early-hit";
                case 3 -> "three-sparse-hits";
                default -> "dense-prefix-empty-tail";
            };
            if (scenario >= 2) {
                haystack = new byte[65_536];
                Arrays.fill(haystack, (byte) 99);
                if (scenario == 4) System.arraycopy(periodicHaystack, 0, haystack, 0, needle.length + 9);
                else for (int position : scenario == 2 ? new int[] {0} : new int[] {0, 20_000, 50_000}) {
                    System.arraycopy(needle, 0, haystack, position, needle.length);
                }
            }
            for (int iteration = 0; iteration < 100; iteration++) {
                try (M3CompiledSearch pattern = new M3JavaAdaptiveSearchEngine().compile(needle)) {
                    sink += pattern.needleLength();
                    if (iteration < 12) sink += M3ByteOccurrences.count(pattern, haystack, 0, null);
                }
            }
            int coldIterations = 200;
            long allocated = allocated(allocation, thread);
            long start = System.nanoTime();
            for (int iteration = 0; iteration < coldIterations; iteration++) {
                try (M3CompiledSearch pattern = new M3JavaAdaptiveSearchEngine().compile(needle)) { sink += pattern.needleLength(); }
            }
            emit(label, "cold-compile", coldIterations, System.nanoTime() - start, delta(allocation, thread, allocated), needle.length, haystack.length);
            M3CompiledSearch[] cold = new M3CompiledSearch[12];
            for (int index = 0; index < cold.length; index++) cold[index] = new M3JavaAdaptiveSearchEngine().compile(needle);
            allocated = allocated(allocation, thread);
            start = System.nanoTime();
            for (M3CompiledSearch pattern : cold) sink += M3ByteOccurrences.count(pattern, haystack, 0, null);
            emit(label, "first-overlap", cold.length, System.nanoTime() - start, delta(allocation, thread, allocated), needle.length, haystack.length);
            for (M3CompiledSearch pattern : cold) pattern.close();
            try (M3CompiledSearch pattern = new M3JavaAdaptiveSearchEngine().compile(needle)) {
                for (int iteration = 0; iteration < 12; iteration++) sink += M3ByteOccurrences.count(pattern, haystack, 0, null);
                int repetitions = 30;
                allocated = allocated(allocation, thread);
                start = System.nanoTime();
                for (int iteration = 0; iteration < repetitions; iteration++) sink += M3ByteOccurrences.count(pattern, haystack, 0, null);
                emit(label, "warm-overlap", repetitions, System.nanoTime() - start, delta(allocation, thread, allocated), needle.length, haystack.length);
            }
            try (M3CompiledSearch pattern = new M3JavaAdaptiveSearchEngine().compile(needle)) {
                for (int iteration = 0; iteration < 200; iteration++) sink += M3ByteOccurrences.visit(pattern, haystack, 0, 1, DISCARD, null);
                int repetitions = 200;
                allocated = allocated(allocation, thread);
                start = System.nanoTime();
                for (int iteration = 0; iteration < repetitions; iteration++) sink += M3ByteOccurrences.visit(pattern, haystack, 0, 1, DISCARD, null);
                emit(label, "one-match-limit", repetitions, System.nanoTime() - start, delta(allocation, thread, allocated), needle.length, haystack.length);
            }
        }
    }

    private static long allocated(com.sun.management.ThreadMXBean bean, long thread) {
        return bean == null ? -1L : bean.getThreadAllocatedBytes(thread);
    }

    private static long delta(com.sun.management.ThreadMXBean bean, long thread, long before) {
        return before < 0 ? -1 : allocated(bean, thread) - before;
    }

    private static void emit(String scenario, String phase, int repetitions, long nanos, long bytes, int needle, int haystack) {
        System.out.println("{\"scenario\":\"" + scenario + "\",\"phase\":\"" + phase + "\",\"repetitions\":"
                + repetitions + ",\"nanoseconds\":" + nanos + ",\"allocated_bytes\":" + bytes
                + ",\"needle_bytes\":" + needle + ",\"haystack_bytes\":" + haystack + "}");
    }

    private static byte[] periodic(int length, int period) {
        byte[] result = new byte[length];
        for (int index = 0; index < length; index++) result[index] = (byte) (index % period);
        return result;
    }

    private static List<Integer> oracle(byte[] haystack, byte[] needle, int from) {
        List<Integer> positions = new ArrayList<>();
        for (int start = Math.max(0, from); start <= haystack.length - needle.length; start++) {
            if (Arrays.equals(haystack, start, start + needle.length, needle, 0, needle.length)) positions.add(start);
        }
        return positions;
    }
}
