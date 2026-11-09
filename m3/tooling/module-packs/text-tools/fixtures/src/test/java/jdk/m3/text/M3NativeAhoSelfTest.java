// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Independent UTF-16 String.contains oracle for actual checked JNI Aho trie. */
public final class M3NativeAhoSelfTest {
    private static long checks;
    private static final long SEED = 0x5143a0a11c2026L;
    private static final String[] FRAGMENTS = {
        "", "a", "aa", "aba", "π", "漢", "\u0000", "🙂", "e\u0301",
        "\uD800", "\uDC00", "public class R {}", "(?<name>x)",
        "/* fake Java(); */", "\\Q.*\\E", "\n", "\r", "\r\n", "🚀", "{{ }}"
    };

    private M3NativeAhoSelfTest() { }

    private static void check(boolean condition, String cause) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks + ": " + cause);
    }

    private static void expect(Class<? extends Throwable> exception, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable ex) {
            if (exception.isInstance(ex)) return;
            throw new AssertionError("expected " + exception + ", got " + ex, ex);
        }
        throw new AssertionError("missing " + exception);
    }

    private static String randomString(Random rng, int maxFragments) {
        StringBuilder result = new StringBuilder();
        for (int i = rng.nextInt(maxFragments + 1); i > 0; i--) {
            result.append(FRAGMENTS[rng.nextInt(FRAGMENTS.length)]);
        }
        return result.toString();
    }

    private static int[] selected(List<String> rows, M3NativeAho plan) {
        long[] bits = plan.selectNative(rows);
        int[] result = new int[rows.size()];
        int used = 0;
        for (int i = 0; i < rows.size(); i++) {
            if ((bits[i >>> 6] & (1L << (i & 63))) != 0) result[used++] = i;
        }
        return Arrays.copyOf(result, used);
    }

    private static void compare(List<String> needles, List<String> rows) {
        M3NativeAho plan = M3NativeAho.compile(needles);
        check(plan.fits(rows), "bounded test geometry");
        long[] actual = plan.selectNative(rows);
        check(actual.length == ((rows.size() + 63) >>> 6), "mask geometry");
        for (int i = 0; i < rows.size(); i++) {
            final String row = rows.get(i);
            boolean expected = needles.stream().allMatch(row::contains);
            boolean got = (actual[i >>> 6] & (1L << (i & 63))) != 0;
            check(got == expected, "mismatch needles=" + needles + " row=" + row);
        }
    }

    private static void invalidNativeArrays() throws Exception {
        Method method = M3NativeAho.class.getDeclaredMethod("select0", char[].class,
                int[].class, int[].class, char[].class, int[].class, int[].class,
                int[].class, long[].class, long.class, long.class);
        method.setAccessible(true);
        // Invalid source row boundary must refuse without indexing outside any array.
        Object[] badOffsets = {"hi".toCharArray(), new int[]{0, 3},
                new int[]{0, -1}, new char[]{'h'}, new int[]{1}, new int[]{-1},
                new int[]{0, 0}, new long[]{0, 1}, 1L, 0L};
        expect(IllegalArgumentException.class, () -> invokeBad(method, badOffsets));
        Object[] badEdge = {"hi".toCharArray(), new int[]{0, 2},
                new int[]{0, -1}, new char[]{'h'}, new int[]{1}, new int[]{0},
                new int[]{0, 0}, new long[]{0, 1}, 1L, 0L};
        expect(IllegalArgumentException.class, () -> invokeBad(method, badEdge));
        // A malicious failure cycle is rejected after a bounded number of steps.
        Object[] badFailure = {"az".toCharArray(), new int[]{0, 2},
                new int[]{0, -1}, new char[]{'a'}, new int[]{1}, new int[]{-1},
                new int[]{0, 1}, new long[]{0, 0}, 1L, 0L};
        expect(IllegalArgumentException.class, () -> invokeBad(method, badFailure));
    }

    private static void invokeBad(Method method, Object[] args) {
        try { method.invoke(null, args); }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException ex) throw ex;
            if (e.getCause() instanceof Error ex) throw ex;
            throw new AssertionError(e.getCause());
        } catch (IllegalAccessException e) { throw new AssertionError(e); }
    }

    public static void main(String[] args) throws Exception {
        check(M3NativeAho.available(), "real JNI is required");
        compare(List.of(""), List.of("", "a", "\u0000", "漢"));
        compare(List.of("a", "a", ""), List.of("", "a", "ab", "aa", "b"));
        compare(List.of("🙂", "a", "e\u0301"),
                List.of("🙂ae\u0301", "a🙂e\u0301", "🙂a", "a", ""));
        compare(List.of("\uD800", "\uDC00"), List.of("", "\uD800", "\uDC00", "\uD800\uDC00"));
        List<String> sixtyFour = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        for (int n = 0; n < 64; n++) {
            String term = String.valueOf((char)(0x500 + n));
            sixtyFour.add(term); row.append(term);
        }
        compare(sixtyFour, List.of(row.toString(), row.substring(1), "", row.reverse().toString()));
        Random rng = new Random(SEED);
        for (int trial = 0; trial < 1200; trial++) {
            int termCount = 1 + rng.nextInt(16);
            List<String> terms = new ArrayList<>();
            for (int j = 0; j < termCount; j++) terms.add(randomString(rng, 3));
            int rowCount = rng.nextInt(100);
            List<String> rows = new ArrayList<>();
            for (int j = 0; j < rowCount; j++) rows.add(randomString(rng, 8));
            compare(terms, rows);
        }
        M3NativeAho shared = M3NativeAho.compile(List.of("public", "class", "{}", "\\Q.*\\E"));
        List<String> rows = List.of("public class R {} // \\Q.*\\E", "public class R {}", "", "漢");
        int[] expected = selected(rows, shared);
        try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<int[]>> work = new ArrayList<>();
            for (int i = 0; i < 64; i++) work.add(threads.submit(() -> selected(rows, shared)));
            for (Future<int[]> result : work) {
                check(Arrays.equals(result.get(), expected), "parallel output drift");
            }
        }
        expect(IllegalArgumentException.class, () -> M3NativeAho.compile(List.of()));
        expect(IllegalArgumentException.class, () -> M3NativeAho.compile(List.of("a".repeat(16384))));
        expect(IllegalArgumentException.class, () -> M3NativeAho.compile(java.util.Collections.nCopies(65, "a")));
        check(!shared.fits(List.of("a".repeat((1 << 20) + 1))), "oversize fallback obligation");
        invalidNativeArrays();
        System.out.println("M3_AHO_NATIVE_PARITY_PASS checks=" + checks + " seed=" + SEED);
    }
}
