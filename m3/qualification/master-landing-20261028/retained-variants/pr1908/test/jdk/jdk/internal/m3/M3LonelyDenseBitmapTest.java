/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Dense singleton eligibility preserves word boundaries, encounter order and ownership
 * @modules java.base/jdk.internal.m3 jdk.management/com.sun.management
 * @run main/othervm M3LonelyDenseBitmapTest functional
 * @run main/othervm -Xint M3LonelyDenseBitmapTest functional
 */

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import jdk.internal.m3.M3IntAlgorithms;

public class M3LonelyDenseBitmapTest {
    private static volatile int[] escaped;
    private static int cases;

    public static void main(String[] args) {
        String mode = args.length == 0 ? "functional" : args[0];
        switch (mode) {
            case "functional" -> functional();
            case "allocation" -> allocation();
            case "resource" -> resource();
            default -> throw new IllegalArgumentException(mode);
        }
    }

    private static void functional() {
        for (int length = 0; length <= 6; length++) {
            int count = 1;
            for (int i = 0; i < length; i++) count *= 5;
            for (int encoding = 0; encoding < count; encoding++) {
                int[] values = new int[length + 2];
                values[0] = Integer.MIN_VALUE; values[values.length - 1] = Integer.MAX_VALUE;
                int code = encoding;
                for (int i = 0; i < length; i++, code /= 5) values[i + 1] = code % 5 - 2;
                check(values, 1, length);
            }
        }
        for (int base : new int[]{Integer.MIN_VALUE, -129, 0, Integer.MAX_VALUE - 255}) {
            for (int inputLength : new int[]{130, 4096, 65536}) {
            for (int boundary : new int[]{1, 63, 64, 65, 127, 128, 129, 254}) {
                for (int pattern = 0; pattern < 8; pattern++) {
                    int[] values = new int[inputLength];
                    Arrays.fill(values, base);
                    values[0] = base + 255;
                    values[1] = base + boundary;
                    values[2] = (pattern & 1) == 0 ? base : base + boundary - 1;
                    values[3] = (pattern & 2) == 0 ? base : base + boundary + 1;
                    values[4] = (pattern & 4) == 0 ? base : base + boundary;
                    check(values, 0, values.length);
                }
            }
        }
        }
        Random random = new Random(0x4d334249544cL);
        for (int trial = 0; trial < 2500; trial++) {
            int length = random.nextInt(513);
            int[] values = new int[length + 4];
            int base = trial % 3 == 0 ? Integer.MIN_VALUE : trial % 3 == 1 ? -4096 : Integer.MAX_VALUE - 8192;
            for (int i = 0; i < values.length; i++) values[i] = base + random.nextInt(8193);
            check(values, 2, length);
        }
        Random dense = new Random(0x4d3344454e5345L);
        for (int trial = 0; trial < 250; trial++) {
            int length = 4095 + trial % 3;
            int[] values = new int[length + 4];
            int bound = 1 + dense.nextInt(length * 3);
            int base = trial % 3 == 0 ? Integer.MIN_VALUE : trial % 3 == 1 ? -8192 : Integer.MAX_VALUE - bound;
            for (int i = 0; i < values.length; i++) values[i] = base + dense.nextInt(bound);
            check(values, 2, length);
        }
        for (int width : new int[]{63, 64, 65, 127, 128, 129, 1 << 20, (1 << 20) + 1}) {
            int[] values = new int[Math.max(130, (width + 7) / 8)];
            Arrays.fill(values, -10);
            values[0] = -11; values[1] = width - 12; values[2] = 40; values[3] = 63;
            check(values, 0, values.length);
        }
        Random largeDense = new Random(0x4d334c41524745L);
        for (int trial = 0; trial < 12; trial++) {
            int length = 65535 + trial % 3;
            int[] values = new int[length + 4];
            int bound = 1 + largeDense.nextInt(length * 3);
            int base = trial % 2 == 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE - bound;
            for (int i = 0; i < values.length; i++) values[i] = base + largeDense.nextInt(bound);
            check(values, 2, length);
        }
        // Exact short bitmap widths on the large-input reducer, including tail words.
        for (int width : new int[]{63, 64, 65, 127, 128, 129}) {
            int[] values = new int[65536];
            Arrays.fill(values, -10);
            values[0] = -11; values[1] = width - 12;
            values[2] = Math.min(40, width - 12); values[3] = Math.min(63, width - 12);
            check(values, 0, values.length);
        }
        // Every selected value is unique and separated; shuffle and slice the input
        // to force exact-size arraycopy to preserve encounter order and source offset.
        for (int n : new int[]{4095, 4096, 4097, 65535, 65536, 65537}) {
            checkAllIsolated(n, 0);
        }
        checkAllIsolated(65536, Integer.MIN_VALUE);
        checkAllIsolated(65536, Integer.MAX_VALUE - 131072);
        check(new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0, 63, 64, 128}, 0, 7);
        System.out.println("DENSE_BITMAP_CONTRACT_PASS cases=" + cases);
    }

    private static void checkAllIsolated(int length, int base) {
        int[] values = new int[length + 5];
        Arrays.fill(values, 123456789);
        for (int i = 0; i < length; i++) values[i + 3] = base + i * 2;
        Random random = new Random(length);
        for (int i = length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1), value = values[i + 3];
            values[i + 3] = values[j + 3]; values[j + 3] = value;
        }
        check(values, 3, length);
    }

    private static void check(int[] values, int offset, int length) {
        int[] before = values.clone();
        HashMap<Integer, Integer> counts = new HashMap<>();
        for (int i = offset; i < offset + length; i++) counts.merge(values[i], 1, Integer::sum);
        ArrayList<Integer> expected = new ArrayList<>();
        for (int i = offset; i < offset + length; i++) {
            int value = values[i];
            if (counts.get(value) == 1
                    && (value == Integer.MIN_VALUE || !counts.containsKey(value - 1))
                    && (value == Integer.MAX_VALUE || !counts.containsKey(value + 1))) expected.add(value);
        }
        int[] result = M3IntAlgorithms.lonelyNumbers(values, offset, length);
        if (result.length != expected.size()) throw new AssertionError("cardinality case=" + cases);
        for (int i = 0; i < result.length; i++) if (result[i] != expected.get(i)) {
            throw new AssertionError("encounter order/value case=" + cases);
        }
        if (!Arrays.equals(values, before)) throw new AssertionError("input changed");
        if (result.length != 0) {
            result[0] ^= -1;
            int[] again = M3IntAlgorithms.lonelyNumbers(values, offset, length);
            if (again[0] != expected.getFirst() || !Arrays.equals(values, before)) {
                throw new AssertionError("result aliases source or another result");
            }
        }
        cases++;
    }

    private static ThreadMXBean bean() {
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        if (!bean.isThreadCpuTimeEnabled()) bean.setThreadCpuTimeEnabled(true);
        return bean;
    }

    private static void allocation() {
        int[] values = shape(65536, "adjacent");
        ThreadMXBean bean = bean();
        for (int i = 0; i < 300; i++) escaped = M3IntAlgorithms.lonelyNumbers(values);
        long before = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
        for (int i = 0; i < 32; i++) escaped = M3IntAlgorithms.lonelyNumbers(values);
        long bytes = bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - before;
        if (escaped.length != 0) throw new AssertionError("all adjacent");
        System.out.println("DENSE_EMPTY_BYTES_PER_CALL=" + bytes / 32.0);
        // Diagnostic only. Candidate-specific budgets belong to the canonical recipe tests.
    }

    private static int[] shape(int n, String shape) {
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = switch (shape) {
            case "adjacent" -> i;
            case "isolated" -> i * 2;
            case "duplicate" -> i / 2;
            case "mixed" -> i % 3 == 0 ? i * 2 : i;
            case "sparse" -> i * 8191;
            default -> throw new AssertionError(shape);
        };
        return values;
    }

    private static void resource() {
        ThreadMXBean bean = bean();
        long thread = Thread.currentThread().threadId();
        System.out.println("n,shape,phase,round,calls,allocated_bytes,thread_cpu_ns");
        for (int n : new int[]{1, 8, 64, 4095, 4096, 4097, 65535, 65536, 65537}) {
            for (String shape : new String[]{"adjacent", "isolated", "duplicate", "mixed", "sparse"}) {
                int[] values = shape(n, shape);
                int calls = Math.max(16, Math.min(20000, 524288 / n));
                long memory = bean.getThreadAllocatedBytes(thread), cpu = bean.getCurrentThreadCpuTime();
                escaped = M3IntAlgorithms.lonelyNumbers(values);
                long elapsed = bean.getCurrentThreadCpuTime() - cpu;
                long allocation = bean.getThreadAllocatedBytes(thread) - memory;
                System.out.println(n + "," + shape + ",first,0,1," + allocation + "," + elapsed);
                for (int i = 0; i < Math.max(calls, 400); i++) escaped = M3IntAlgorithms.lonelyNumbers(values);
                for (int round = 0; round < 4; round++) {
                    memory = bean.getThreadAllocatedBytes(thread); cpu = bean.getCurrentThreadCpuTime();
                    for (int i = 0; i < calls; i++) escaped = M3IntAlgorithms.lonelyNumbers(values);
                    elapsed = bean.getCurrentThreadCpuTime() - cpu;
                    allocation = bean.getThreadAllocatedBytes(thread) - memory;
                    System.out.println(n + "," + shape + ",warm," + round + "," + calls + "," + allocation + "," + elapsed);
                }
            }
        }
    }
}
