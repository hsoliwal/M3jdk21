// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

/**
 * Differential dump driver of recipe/corpus-search-20261108 (test scope, not a test): replays seeded byte pattern
 * sets, probes and haystacks through every kept public member of the relocated kernels, and the Rabin-Karp NEC pair,
 * and prints one line per case (scalars as is, arrays as length and Arrays.hashCode, refusals as exception class and
 * message) plus one TOTAL line. One source, two compiles: differential.sh compiles it here (relocated) and, with the
 * TYPE and MEMBER rows of RELOCATION_MAP.tsv reversed, against the donor classes; the two dumps must be
 * byte-identical.
 * usage: java jdk.m3.algorithm.CorpusSearchDump --seed N --cases N
 */
//FLLDCIM: TOP - differential driver of recipe/corpus-search-20261108 (M3JDK-authored, test scope)
public final class CorpusSearchDump {
    private static final byte[] SMALL = {0, 'a', 'b', (byte) 0xff};
    private static final char[] UNITS = {'a', 'b', '$', (char) 0, (char) 0xd800, (char) 0xdfff};
    private static long lines;

    private CorpusSearchDump() {
    }

    public static void main(String[] args) {
        long seed = Long.parseLong(args[1]);
        int cases = Integer.parseInt(args[3]);
        Random random = new Random(seed);
        StringBuilder out = new StringBuilder(1 << 20);
        for (int index = 0; index < cases; index++) {
            List<byte[]> patterns = patterns(random);
            byte[] probe = probe(random, patterns);
            byte[] haystack = bytes(random, random.nextInt(40));
            automatonCases(out, index, patterns, probe, haystack, random);
            facadeCases(out, index, patterns, probe);
            rabinKarpCases(out, index, random);
        }
        refusalCases(out);
        out.append("TOTAL lines=").append(lines).append(" seed=").append(seed).append(" cases=").append(cases)
                .append('\n');
        System.out.print(out);
    }

    private static void automatonCases(StringBuilder out, int index, List<byte[]> patterns, byte[] probe,
            byte[] haystack, Random random) {
        MultiPatternSearch.Prepared prepared = MultiPatternSearch.prepare(patterns);
        line(out, "prepare.patternCount", index, prepared::patternCount);
        line(out, "prepare#monitor.totalPatternBytes", index,
                () -> MultiPatternSearch.prepare(patterns, ProgressMonitor.none()).totalPatternBytes());
        int pick = prepared.patternCount() == 0 ? -1 : random.nextInt(prepared.patternCount());
        line(out, "Prepared.pattern", index, () -> pick < 0 ? "none" : prepared.pattern(pick));
        line(out, "Prepared.prefixCount", index, () -> prepared.prefixCount(probe));
        line(out, "Prepared.startsWith", index, () -> prepared.startsWith(probe));
        line(out, "Prepared.containsExact", index, () -> prepared.containsExact(probe));
        line(out, "Prepared.exactCount", index, () -> prepared.exactCount(probe));
        searchCases(out, index, prepared, probe, haystack);
    }

    private static void searchCases(StringBuilder out, int index, MultiPatternSearch.Prepared prepared,
            byte[] probe, byte[] haystack) {
        line(out, "prefixCount#monitor", index,
                () -> MultiPatternSearch.prefixCount(prepared, probe, ProgressMonitor.none()));
        line(out, "exactCount#monitor", index,
                () -> MultiPatternSearch.exactCount(prepared, probe, ProgressMonitor.none()));
        line(out, "startsWith", index, () -> MultiPatternSearch.startsWith(prepared, probe));
        line(out, "containsExact", index, () -> MultiPatternSearch.containsExact(prepared, probe));
        line(out, "firstOffsets", index, () -> MultiPatternSearch.firstOffsets(prepared, haystack));
        line(out, "firstOffsets#monitor", index,
                () -> MultiPatternSearch.firstOffsets(prepared, haystack, ProgressMonitor.none()));
        line(out, "firstOffsetsJava", index, () -> MultiPatternSearch.firstOffsetsJava(prepared, haystack));
    }

    private static void facadeCases(StringBuilder out, int index, List<byte[]> patterns, byte[] probe) {
        PrefixIndex facade = PrefixIndex.prepare(patterns);
        line(out, "PrefixIndex.size", index, facade::size);
        line(out, "PrefixIndex.prefixCount", index, () -> facade.prefixCount(probe));
        line(out, "PrefixIndex.prefixCount#monitor", index,
                () -> facade.prefixCount(probe, ProgressMonitor.none()));
        line(out, "PrefixIndex.exactCount", index, () -> facade.exactCount(probe));
        line(out, "PrefixIndex.exactCount#monitor", index,
                () -> facade.exactCount(probe, ProgressMonitor.none()));
        line(out, "PrefixIndex.contains", index, () -> facade.contains(probe));
        line(out, "PrefixIndex.startsWith", index, () -> facade.startsWith(probe));
        line(out, "PrefixIndex.prepare#monitor.size", index,
                () -> PrefixIndex.prepare(patterns, ProgressMonitor.none()).size());
    }

    private static void rabinKarpCases(StringBuilder out, int index, Random random) {
        String text = text(random, random.nextInt(48));
        String pattern = pattern(random, text);
        line(out, "rabinKarp.indexOf", index, () -> PrimitiveStringAlgorithms.rabinKarpIndexOf(text, pattern));
        line(out, "rabinKarp.indexOf#empty", index, () -> PrimitiveStringAlgorithms.rabinKarpIndexOf(text, ""));
    }

    private static void refusalCases(StringBuilder out) {
        MultiPatternSearch.Prepared one = MultiPatternSearch.prepare(List.of(new byte[] {1}));
        line(out, "refusal.prepare#null", 0, () -> MultiPatternSearch.prepare(null));
        line(out, "refusal.prepare#nullElement", 0,
                () -> MultiPatternSearch.prepare(Arrays.asList(new byte[1], null)));
        line(out, "refusal.prepare#count", 0, () -> MultiPatternSearch.prepare(
                Collections.nCopies(MultiPatternSearch.MAX_PATTERNS + 1, new byte[] {1})));
        line(out, "refusal.prepare#bytes", 0, () -> MultiPatternSearch.prepare(
                List.of(new byte[MultiPatternSearch.MAX_TOTAL_PATTERN_BYTES + 1])));
        line(out, "refusal.prefixCount#nullPrepared", 0, () -> MultiPatternSearch.prefixCount(null, new byte[0]));
        line(out, "refusal.prefixCount#nullPrefix", 0, () -> MultiPatternSearch.prefixCount(one, null));
        line(out, "refusal.exactCount#nullPattern", 0, () -> MultiPatternSearch.exactCount(one, null));
        line(out, "refusal.firstOffsets#nullHaystack", 0,
                () -> MultiPatternSearch.firstOffsets(one, (byte[]) null));
        line(out, "refusal.facade#nullValues", 0, () -> PrefixIndex.prepare(null));
        line(out, "refusal.rabinKarp#nullText", 0, () -> PrimitiveStringAlgorithms.rabinKarpIndexOf(null, "a"));
        interruptCases(out, one);
    }

    private static void interruptCases(StringBuilder out, MultiPatternSearch.Prepared one) {
        line(out, "interrupt.prepare", 0,
                () -> interrupted(() -> MultiPatternSearch.prepare(List.of(new byte[] {1}))));
        line(out, "interrupt.firstOffsets#monitor", 0, () -> interrupted(
                () -> MultiPatternSearch.firstOffsets(one, new byte[] {1, 2}, ProgressMonitor.none())));
        line(out, "interrupt.firstOffsets", 0,
                () -> interrupted(() -> MultiPatternSearch.firstOffsets(one, new byte[] {1, 2})));
        line(out, "interrupt.prefixCount#monitor", 0, () -> interrupted(
                () -> MultiPatternSearch.prefixCount(one, new byte[] {1}, ProgressMonitor.none())));
        line(out, "interrupt.facade.prepare", 0,
                () -> interrupted(() -> PrefixIndex.prepare(List.of(new byte[] {1}))));
    }

    private static Object interrupted(Supplier<Object> call) {
        Thread.currentThread().interrupt();
        try {
            return call.get();
        } catch (CancellationException canceled) {
            return "CANCELED " + canceled.getMessage();
        } finally {
            Thread.interrupted();
        }
    }

    private static void line(StringBuilder out, String member, int index, Supplier<Object> call) {
        Object result;
        try {
            result = call.get();
        } catch (RuntimeException refused) {
            result = "EXC " + refused.getClass().getSimpleName() + ": " + refused.getMessage();
        }
        out.append(member).append('#').append(index).append(' ').append(render(result)).append('\n');
        lines++;
    }

    private static String render(Object result) {
        if (result instanceof int[] values) {
            return "int[" + values.length + "] " + Arrays.hashCode(values);
        }
        if (result instanceof byte[] values) {
            return "byte[" + values.length + "] " + Arrays.hashCode(values);
        }
        return String.valueOf(result);
    }

    private static List<byte[]> patterns(Random random) {
        int count = random.nextInt(9);
        List<byte[]> patterns = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            patterns.add(bytes(random, random.nextInt(7)));
        }
        return patterns;
    }

    private static byte[] bytes(Random random, int length) {
        byte[] values = new byte[length];
        boolean small = random.nextInt(5) != 0;
        for (int index = 0; index < length; index++) {
            values[index] = small ? SMALL[random.nextInt(random.nextBoolean() ? 2 : SMALL.length)]
                    : (byte) random.nextInt(256);
        }
        return values;
    }

    private static byte[] probe(Random random, List<byte[]> patterns) {
        if (!patterns.isEmpty() && random.nextInt(3) != 0) {
            byte[] pattern = patterns.get(random.nextInt(patterns.size()));
            return Arrays.copyOf(pattern, random.nextInt(pattern.length + 1));
        }
        return bytes(random, random.nextInt(4));
    }

    private static String text(Random random, int length) {
        char[] units = new char[length];
        boolean small = random.nextInt(4) != 0;
        for (int index = 0; index < length; index++) {
            units[index] = small ? UNITS[random.nextInt(random.nextBoolean() ? 2 : UNITS.length)]
                    : (char) random.nextInt(Character.MAX_VALUE + 1);
        }
        return new String(units);
    }

    private static String pattern(Random random, String text) {
        if (!text.isEmpty() && random.nextInt(3) != 0) {
            int from = random.nextInt(text.length());
            int to = from + random.nextInt(Math.min(12, text.length() - from) + 1);
            char[] units = text.substring(from, to).toCharArray();
            if (units.length > 0 && random.nextBoolean()) {
                units[random.nextInt(units.length)] = UNITS[random.nextInt(UNITS.length)];
            }
            return new String(units);
        }
        return text(random, random.nextInt(6));
    }
}
