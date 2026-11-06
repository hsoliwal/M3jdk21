// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Differential dump and reference model (gates 3 and 4 of recipe/utf16-stitch-20261106). Replays one seeded corpus
 * through the three relocated text atoms of a package named by args[0] (M3LiteralBatchCandidate, the sparse UTF-16
 * Aho-Corasick plan M3SparseUtf16LiteralPlan and the package-private stitched scanner M3SparseUtf16Stitched), reached
 * by reflection so that the donor classes and the relocated classes run the same driver, and prints one canonical
 * line per case: plan geometry (patterns, states, transition cells, primitive bytes), the ASCII Shift-And first
 * positions, the single-region and batched sparse first positions, the stitched first positions over the whole
 * logical region, the stitched window count, and the exception class of every refused call (including the explicit
 * native lanes, which refuse with UnsatisfiedLinkError when no JNI library is loaded). Every first position is also
 * checked against an independent brute-force UTF-16 scan (no String.indexOf); a mismatch aborts the run. Optional
 * args[1] names the package's stitch oracle, whose verify(1, false) report and refusal/frontier checks are printed.
 * The COMPUTED_LIST line probes a List whose get(i) answers differently on each call (the donor's 2026-10-27
 * snapshot correction). Two sides agree when their dumps are byte-identical; the TOTAL line counts cases, reference
 * comparisons and refusals.
 * Usage: {@code java -cp classes;test-classes jdk.m3.precompute.M3SparseUtf16StitchedDump PACKAGE [ORACLE] [CASES]}
 */
//FLLDCIM: TFG - dump driver from the template M3Utf16AtomsDump (recipe utf16-atoms-20261106): reflective, serves donor and relocated classes
public final class M3SparseUtf16StitchedDump {
    private static final long SEED = 0x5354_4954_4348_3236L;
    private static final int DEFAULT_CASES = 3_000;
    private static final int MAX_NEEDLES = 24;
    private static final int MAX_NEEDLE_UNITS = 63;
    private static final int MAX_REGION_UNITS = 4_096;
    private static final int MAX_BATCH = 64;
    private static final int SEAM = 4_034;
    private static final int SMALL_TEXT = 300;
    private static final int MEDIUM_TEXT = 9_000;
    private static final int LARGE_TEXT = 20_000;
    private static final int BEYOND_BATCH_TEXT = SEAM * 65 + 80;
    private static final int MEDIUM_EVERY = 3;
    private static final int LARGE_EVERY = 11;
    private static final int BEYOND_BATCH_EVERY = 500;
    private static final int DERIVED_NEEDLE_ONE_IN = 2;
    private static final int FULL_REGION_ONE_IN = 2;
    private static final int MAX_WINDOWS_UNITS = 16_523_264;
    private static final String ASCII_ALPHABET = "ab/*?[]{}\u0000caf";
    private static final String UTF16_ALPHABET = ASCII_ALPHABET + "éक世😀￿";

    private final Method prepare;
    private final Method shiftAndFirst;
    private final Method compile;
    private final Method sparseFirst;
    private final Method sparseBatch;
    private final Method sparseNativeBatch;
    private final Method patternCount;
    private final Method stateCount;
    private final Method transitionCells;
    private final Method primitiveBytes;
    private final Method stitchedJava;
    private final Method stitchedNative;
    private final Method windowCount;
    private final PrintStream out;
    private long references;
    private long refusals;

    private M3SparseUtf16StitchedDump(String pkg, PrintStream out) throws ReflectiveOperationException {
        Class<?> candidate = Class.forName(pkg + ".M3LiteralBatchCandidate");
        Class<?> plan = Class.forName(pkg + ".M3SparseUtf16LiteralPlan");
        Class<?> stitched = Class.forName(pkg + ".M3SparseUtf16Stitched");
        this.prepare = candidate.getMethod("prepare", List.class, int.class);
        this.shiftAndFirst = Class.forName(pkg + ".M3LiteralBatchCandidate$Plan")
                .getMethod("firstJava", char[].class, int.class, int.class);
        this.compile = plan.getMethod("compile", List.class, int.class);
        this.sparseFirst = plan.getMethod("firstJava", char[].class, int.class, int.class);
        this.sparseBatch = plan.getMethod("firstJavaBatch", char[].class, int[].class, int[].class);
        this.sparseNativeBatch = plan.getMethod("firstNativeBatch", char[].class, int[].class, int[].class);
        this.patternCount = plan.getMethod("patternCount");
        this.stateCount = plan.getMethod("stateCount");
        this.transitionCells = plan.getMethod("transitionCells");
        this.primitiveBytes = plan.getMethod("primitiveBytes");
        this.stitchedJava = accessible(stitched.getDeclaredMethod("firstJava", plan, char[].class, int.class, int.class));
        this.stitchedNative = accessible(stitched.getDeclaredMethod("firstNative", plan, char[].class, int.class, int.class));
        this.windowCount = accessible(stitched.getDeclaredMethod("windowCount", int.class));
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        String oracle = args.length > 1 && !args[1].isEmpty() ? args[0] + "." + args[1] : null;
        run(args[0], oracle, args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_CASES, out);
        out.flush();
    }

    /** Runs the corpus; returns {cases, reference comparisons, refusals}. A null oracle skips the oracle report. */
    static long[] run(String pkg, String oracle, int cases, PrintStream out) throws Exception {
        M3SparseUtf16StitchedDump dump = new M3SparseUtf16StitchedDump(pkg, out);
        dump.refusalCases();
        SplittableRandom random = new SplittableRandom(SEED);
        for (int id = 0; id < cases; id++) {
            dump.seededCase(id, random);
        }
        dump.computedList();
        if (oracle != null) {
            dump.oracleReport(Class.forName(oracle));
        }
        out.println("TOTAL cases=" + cases + " references=" + dump.references + " refusals=" + dump.refusals);
        return new long[] {cases, dump.references, dump.refusals};
    }

    static PrintStream discard() {
        return new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);
    }

    private static Method accessible(Method method) { method.setAccessible(true); return method; }

    private void seededCase(int id, SplittableRandom random) throws Exception {
        boolean ascii = (id & 1) == 0;
        char[] text = text(id, random, ascii ? ASCII_ALPHABET : UTF16_ALPHABET);
        List<String> needles = needles(random, text, ascii);
        int from = random.nextInt(FULL_REGION_ONE_IN) == 0 ? 0 : random.nextInt(text.length + 1);
        int to = random.nextInt(FULL_REGION_ONE_IN) == 0 ? text.length : from + random.nextInt(text.length - from + 1);
        Object plan = compile.invoke(null, needles, 0);
        StringBuilder line = new StringBuilder(256).append("CASE ").append(id).append(" n=").append(text.length)
                .append(" region=").append(from).append(',').append(to).append(" geometry=")
                .append(patternCount.invoke(plan)).append(',').append(stateCount.invoke(plan)).append(',')
                .append(transitionCells.invoke(plan)).append(',').append(primitiveBytes.invoke(plan));
        int boundedTo = Math.min(to, from + MAX_REGION_UNITS);
        if (ascii) {
            line.append(" shiftAnd=").append(checked(shiftAnd(needles, text, from, boundedTo), needles, text, from, boundedTo));
        }
        line.append(" sparse=").append(checked((int[]) sparseFirst.invoke(plan, text, from, boundedTo), needles, text, from, boundedTo));
        line.append(" batch=").append(batch(plan, needles, text, random));
        line.append(" windows=").append(windowCount.invoke(null, to - from));
        line.append(" stitched=").append(checked((int[]) stitchedJava.invoke(null, plan, text, from, to), needles, text, from, to));
        line.append(" native=").append(refusal(() -> stitchedNative.invoke(null, plan, text, from, to)));
        out.println(line);
    }

    private char[] text(int id, SplittableRandom random, String alphabet) {
        int size = id % BEYOND_BATCH_EVERY == BEYOND_BATCH_EVERY - 1 ? BEYOND_BATCH_TEXT
                : id % LARGE_EVERY == 0 ? random.nextInt(LARGE_TEXT + 1)
                : id % MEDIUM_EVERY == 0 ? random.nextInt(MEDIUM_TEXT + 1) : random.nextInt(SMALL_TEXT + 1);
        char[] text = new char[size];
        for (int i = 0; i < size; i++) {
            text[i] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        if (size > SEAM + MAX_NEEDLE_UNITS) {
            int start = SEAM - random.nextInt(MAX_NEEDLE_UNITS);
            Arrays.fill(text, start, start + MAX_NEEDLE_UNITS, 'a');
        }
        return text;
    }

    private List<String> needles(SplittableRandom random, char[] text, boolean ascii) {
        int count = 1 + random.nextInt(MAX_NEEDLES);
        List<String> needles = new ArrayList<>(count);
        for (int n = 0; n < count; n++) {
            int length = 1 + random.nextInt(random.nextBoolean() ? 4 : MAX_NEEDLE_UNITS);
            if (text.length >= length && random.nextInt(DERIVED_NEEDLE_ONE_IN) == 0) {
                int at = random.nextInt(text.length - length + 1);
                needles.add(new String(text, at, length));
            } else {
                String alphabet = ascii ? ASCII_ALPHABET : UTF16_ALPHABET;
                char[] word = new char[length];
                for (int i = 0; i < length; i++) {
                    word[i] = alphabet.charAt(random.nextInt(alphabet.length()));
                }
                needles.add(new String(word));
            }
        }
        return needles;
    }

    private int[] shiftAnd(List<String> needles, char[] text, int from, int to) throws Exception {
        return (int[]) shiftAndFirst.invoke(prepare.invoke(null, needles, 0), text, from, to);
    }

    private String batch(Object plan, List<String> needles, char[] text, SplittableRandom random) throws Exception {
        int regions = 1 + random.nextInt(MAX_BATCH);
        int[] from = new int[regions];
        int[] to = new int[regions];
        for (int r = 0; r < regions; r++) {
            from[r] = random.nextInt(text.length + 1);
            to[r] = from[r] + random.nextInt(Math.min(MAX_REGION_UNITS, text.length - from[r]) + 1);
        }
        int[] flat = (int[]) sparseBatch.invoke(plan, text, from, to);
        int patterns = needles.size();
        long digest = 1;
        for (int r = 0; r < regions; r++) {
            int[] row = Arrays.copyOfRange(flat, r * patterns, (r + 1) * patterns);
            checked(row, needles, text, from[r], to[r]);
            digest = digest * 31 + Arrays.hashCode(row);
        }
        String nativeLane = refusal(() -> sparseNativeBatch.invoke(plan, text, from, to));
        return regions + ":" + Long.toHexString(digest) + ":" + nativeLane;
    }

    private String checked(int[] actual, List<String> needles, char[] text, int from, int to) {
        if (actual.length != needles.size()) {
            throw new AssertionError("REFERENCE_SHAPE " + actual.length + " != " + needles.size());
        }
        for (int p = 0; p < actual.length; p++) {
            int expected = bruteForce(needles.get(p), text, from, to);
            if (actual[p] != expected) {
                throw new AssertionError("REFERENCE_DRIFT needle " + p + " expected " + expected + " got " + actual[p]);
            }
            references++;
        }
        return Arrays.toString(actual).replace(" ", "");
    }

    /** Independent first UTF-16 occurrence wholly within [from, to): a plain double loop, no String.indexOf. */
    static int bruteForce(String needle, char[] text, int from, int to) {
        int length = needle.length();
        for (int at = from; at + length <= to; at++) {
            int k = 0;
            while (k < length && text[at + k] == needle.charAt(k)) {
                k++;
            }
            if (k == length) {
                return at;
            }
        }
        return -1;
    }

    private void refusalCases() throws Exception {
        List<List<String>> lists = List.of(List.of(), Collections.nCopies(MAX_NEEDLES + 1, "a"), List.of(""),
                List.of("a".repeat(MAX_NEEDLE_UNITS + 1)), List.of("é"), List.of("ab"));
        StringBuilder line = new StringBuilder("REFUSALS");
        for (List<String> list : lists) {
            line.append(" prepare=").append(refusal(() -> prepare.invoke(null, list, 0)));
            line.append(" compile=").append(refusal(() -> compile.invoke(null, list, 0)));
        }
        line.append(" prepareFlags=").append(refusal(() -> prepare.invoke(null, List.of("a"), 2)));
        line.append(" compileFlags=").append(refusal(() -> compile.invoke(null, List.of("a"), 2)));
        line.append(" nullNeedle=").append(refusal(() -> compile.invoke(null, Arrays.asList("a", null), 0)));
        Object plan = compile.invoke(null, List.of("ab", "\uD83D", "\uDE00"), 0);
        char[] big = new char[MAX_REGION_UNITS + 1];
        line.append(" region=").append(refusal(() -> sparseFirst.invoke(plan, big, 0, big.length)));
        line.append(" batchEmpty=").append(refusal(() -> sparseBatch.invoke(plan, big, new int[0], new int[0])));
        line.append(" batchOver=").append(refusal(() -> sparseBatch.invoke(plan, big, new int[MAX_BATCH + 1], new int[MAX_BATCH + 1])));
        line.append(" batchShape=").append(refusal(() -> sparseBatch.invoke(plan, big, new int[1], new int[2])));
        line.append(" windowsNegative=").append(refusal(() -> windowCount.invoke(null, -1)));
        line.append(" windowsOver=").append(refusal(() -> windowCount.invoke(null, MAX_WINDOWS_UNITS + 1)));
        line.append(" windowsMax=").append(windowCount.invoke(null, MAX_WINDOWS_UNITS));
        line.append(" stitchedNull=").append(refusal(() -> stitchedJava.invoke(null, plan, null, 0, 0)));
        line.append(" stitchedBounds=").append(refusal(() -> stitchedJava.invoke(null, plan, big, 2, big.length + 1)));
        out.println(line);
    }

    private void computedList() throws Exception {
        List<String> flipping = new AbstractList<>() {
            private int reads;

            @Override
            public String get(int index) {
                return reads++ == 0 ? "ab" : "z";
            }

            @Override
            public int size() { return 1; }
        };
        char[] text = "xabz".toCharArray();
        Object plan = compile.invoke(null, flipping, 0);
        out.println("COMPUTED_LIST first=" + Arrays.toString((int[]) sparseFirst.invoke(plan, text, 0, text.length))
                + " geometry=" + patternCount.invoke(plan) + ',' + stateCount.invoke(plan));
    }

    private void oracleReport(Class<?> oracle) throws Exception {
        out.println("ORACLE verify=" + oracle.getMethod("verify", int.class, boolean.class).invoke(null, 1, false));
        out.println("ORACLE refusals=" + refusal(() -> oracle.getMethod("verifyRefusals", boolean.class).invoke(null, false))
                + " frontier=" + refusal(() -> oracle.getMethod("verifyFrontier", boolean.class).invoke(null, false)));
    }

    @FunctionalInterface
    private interface Call { Object call() throws ReflectiveOperationException; }

    /** The exception class of a refused call, or OK when the call returns. */
    private String refusal(Call call) {
        try {
            call.call();
            return "OK";
        } catch (InvocationTargetException thrown) {
            refusals++;
            return thrown.getCause().getClass().getName();
        } catch (ReflectiveOperationException broken) {
            throw new AssertionError("reflection failed", broken);
        }
    }
}
