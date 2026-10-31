// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.util.function.IntConsumer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An opt-in CPU execution backend for proven primitive operators only.
 * The larger attached catalogue is candidate metadata, not a list of callable APIs.
 * No shell, filesystem, arbitrary Java callback or JNI symbol is accepted from a plan.
 */
public final class M3CpuToolRuntime {
    private static final int MAX_OUTPUT = 64 * 1024 * 1024;
    private static final int MIN_BUDGET = 8 * 1024;
    private static final double INV_LOG_2 = 1.0 / StrictMath.log(2.0);
    private static final Set<String> EXECUTABLES = Set.of(
            "bytes.identity", "bytes.concat", "bytes.length", "bytes.xor", "hash.sha256",
            "search.literal", "ordering.bytes", "set.unique.bytes", "statistics.byte-histogram",
            "compression.rle.encode", "compression.rle.decode", "information.entropy",
            "text.code-signals");

    private M3CpuToolRuntime() {}

    public static Set<String> operations() { return EXECUTABLES; }

    /** One immutable result with an exact plan/input/output identity receipt. */
    public static final class Execution {
        private final byte[] value;
        private final String planSha256;
        private final String inputSha256;
        private final String outputSha256;
        private final int steps;

        private Execution(byte[] value, String planSha256, String inputSha256, int steps) {
            this.value = value.clone();
            this.planSha256 = planSha256;
            this.inputSha256 = inputSha256;
            this.outputSha256 = hex(M3CpuToolPlan.sha256().digest(this.value));
            this.steps = steps;
        }

        public byte[] bytes() { return value.clone(); }
        public String planSha256() { return planSha256; }
        public String inputSha256() { return inputSha256; }
        public String outputSha256() { return outputSha256; }
        public int steps() { return steps; }
        public int byteCount() { return value.length; }
    }

    /**
     * Automatic budget derived from -Xmx and current residency. No caller-sized array or
     * manual heap-tuning obligation. This is a bounded admission check, not a GC guarantee.
     */
    public static int automaticByteBudget() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long remaining = Math.max(0L, runtime.maxMemory() - used);
        long quarter = remaining / 4L;
        return (int) Math.min(MAX_OUTPUT, Math.max(MIN_BUDGET, quarter));
    }

    /** Execution is serial and a stage's last consumed buffer is dropped immediately. */
    public static Execution execute(M3CpuToolPlan.Compiled plan, byte[] input) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(input, "input");
        int budget = automaticByteBudget();
        if (input.length > budget) throw new IllegalArgumentException("input exceeds current Xmx budget");
        byte[] stableInput = input.clone();
        Map<String, byte[]> outputs = new HashMap<>();
        Map<String, Integer> remaining = new HashMap<>();
        for (M3CpuToolPlan.Step declaration : plan.definitions()) {
            for (String dependency : declaration.dependencies()) {
                remaining.merge(dependency, 1, Integer::sum);
            }
        }
        for (M3CpuToolPlan.PreparedStep prepared : plan.orderedExecution()) {
            M3CpuToolPlan.Step step = prepared.declaration;
            List<String> inputs = step.dependencies();
            byte[] data = inputs.isEmpty() ? stableInput :
                    inputs.size() == 1 ? outputs.get(inputs.get(0)) :
                    concatenate(inputs, outputs, budget);
            if (data == null) throw new IllegalStateException("unresolved prepared dependency");
            byte[] value = apply(prepared, data, budget);
            if (value.length > budget) throw new IllegalArgumentException("stage exceeds Xmx budget");
            outputs.put(step.id(), value);
            for (String id : inputs) {
                if (remaining.merge(id, -1, Integer::sum) == 0) outputs.remove(id);
            }
            long resident = stableInput.length;
            for (byte[] live : outputs.values()) resident += live.length;
            if (resident > (long) budget * 2L) {
                throw new IllegalArgumentException("DAG residency budget exceeded");
            }
        }
        return new Execution(outputs.get(plan.outputId()), plan.sha256(),
                hex(M3CpuToolPlan.sha256().digest(stableInput)), plan.stages());
    }

    private static byte[] concatenate(List<String> inputIds, Map<String, byte[]> outputs, int budget) {
        int size = 0;
        for (String inputId : inputIds) {
            byte[] bytes = outputs.get(inputId);
            if (bytes == null) throw new IllegalStateException("missing fan-in input");
            size = checkedTotal(size, bytes.length, budget);
        }
        byte[] merged = new byte[size];
        int at = 0;
        for (String inputId : inputIds) {
            byte[] bytes = outputs.get(inputId);
            System.arraycopy(bytes, 0, merged, at, bytes.length);
            at += bytes.length;
        }
        return merged;
    }

    private static byte[] apply(M3CpuToolPlan.PreparedStep prepared, byte[] data, int budget) {
        return switch (prepared.declaration.operation()) {
            case "bytes.identity" -> data;
            case "bytes.concat" -> data;
            case "bytes.length" -> ByteBuffer.allocate(Long.BYTES).putLong(data.length).array();
            case "bytes.xor" -> xor(data, prepared.xor);
            case "hash.sha256" -> M3CpuToolPlan.sha256().digest(data);
            case "search.literal" -> positions(data, prepared.literal, prepared.prefix, budget);
            case "ordering.bytes" -> sorted(data);
            case "set.unique.bytes" -> unique(data);
            case "statistics.byte-histogram" -> histogram(data);
            case "compression.rle.encode" -> rleEncode(data, budget);
            case "compression.rle.decode" -> rleDecode(data, budget);
            case "information.entropy" -> entropy(data);
            case "text.code-signals" -> M3CpuToolNativeSignals.pack(data);
            default -> throw new IllegalStateException("unknown admitted tool");
        };
    }

    private static byte[] xor(byte[] data, byte mask) {
        byte[] result = data.clone();
        for (int i = 0; i < result.length; i++) result[i] ^= mask;
        return result;
    }

    private static byte[] positions(byte[] input, byte[] pattern, int[] prefix, int budget) {
        int[] hits = {0};
        walkMatches(input, pattern, prefix, ignored -> hits[0]++);
        if (4L * hits[0] > budget) {
            throw new IllegalArgumentException("position output budget exceeded");
        }
        ByteBuffer result = ByteBuffer.allocate(hits[0] * Integer.BYTES);
        walkMatches(input, pattern, prefix, result::putInt);
        return result.array();
    }

    private static void walkMatches(byte[] input, byte[] pattern, int[] prefix,
            IntConsumer onMatch) {
        int matched = 0;
        for (int i = 0; i < input.length; i++) {
            while (matched > 0 && input[i] != pattern[matched]) matched = prefix[matched - 1];
            if (input[i] == pattern[matched]) matched++;
            if (matched == pattern.length) {
                onMatch.accept(i - pattern.length + 1);
                matched = prefix[matched - 1];
            }
        }
    }

    private static int[] frequencies(byte[] input) {
        int[] result = new int[256];
        for (byte value : input) result[Byte.toUnsignedInt(value)]++;
        return result;
    }

    private static byte[] sorted(byte[] input) {
        byte[] result = new byte[input.length];
        int offset = 0;
        int[] count = frequencies(input);
        for (int v = 0; v < count.length; v++) {
            Arrays.fill(result, offset, offset + count[v], (byte) v);
            offset += count[v];
        }
        return result;
    }

    private static byte[] unique(byte[] input) {
        int[] count = frequencies(input);
        int size = 0;
        for (int value : count) if (value > 0) size++;
        byte[] result = new byte[size];
        int at = 0;
        for (int v = 0; v < count.length; v++) if (count[v] > 0) result[at++] = (byte) v;
        return result;
    }

    private static byte[] histogram(byte[] input) {
        int[] count = frequencies(input);
        ByteBuffer output = ByteBuffer.allocate(Integer.BYTES * count.length);
        for (int value : count) output.putInt(value);
        return output.array();
    }

    private static byte[] rleEncode(byte[] input, int budget) {
        int runs = 0;
        for (int i = 0; i < input.length;) {
            int count = runLength(input, i);
            if (2L * (runs + 1L) > budget) {
                throw new IllegalArgumentException("RLE encoding budget exceeded");
            }
            runs++;
            i += count;
        }
        byte[] coded = new byte[runs * 2];
        int cursor = 0;
        for (int i = 0; i < input.length;) {
            int count = runLength(input, i);
            coded[cursor++] = (byte) count;
            coded[cursor++] = input[i];
            i += count;
        }
        return coded;
    }

    private static int runLength(byte[] source, int start) {
        int length = 1;
        while (start + length < source.length && length < 255
                && source[start + length] == source[start]) length++;
        return length;
    }

    private static byte[] rleDecode(byte[] input, int budget) {
        if ((input.length & 1) != 0) throw new IllegalArgumentException("truncated RLE pair");
        int expanded = 0;
        for (int i = 0; i < input.length; i += 2) {
            int count = Byte.toUnsignedInt(input[i]);
            if (count == 0) throw new IllegalArgumentException("zero-length RLE run");
            expanded = checkedTotal(expanded, count, budget);
        }
        byte[] result = new byte[expanded];
        int cursor = 0;
        for (int i = 0; i < input.length; i += 2) {
            int count = Byte.toUnsignedInt(input[i]);
            Arrays.fill(result, cursor, cursor + count, input[i + 1]);
            cursor += count;
        }
        return result;
    }

    private static byte[] entropy(byte[] input) {
        if (input.length == 0) return ByteBuffer.allocate(Long.BYTES).putDouble(0.0).array();
        double result = 0.0;
        for (int count : frequencies(input)) {
            if (count == 0) continue;
            double probability = (double) count / input.length;
            result -= probability * StrictMath.log(probability) * INV_LOG_2;
        }
        return ByteBuffer.allocate(Long.BYTES).putDouble(result).array();
    }

    private static int checkedTotal(int current, int added, int budget) {
        long sum = (long) current + added;
        if (sum > budget) throw new IllegalArgumentException("output exceeds current Xmx budget");
        return (int) sum;
    }

    private static String hex(byte[] bytes) { return java.util.HexFormat.of().formatHex(bytes); }
}
