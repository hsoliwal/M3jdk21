/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Verify bounded weak-owner repeated-source M3String postings/hash precompute
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringSourcePrecomputeTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

public class M3StringSourcePrecomputeTest {
    private static final Field STRING_M3;
    private static final Method IS_PREPARED;
    private static final Method MAX_BYTES;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> type = Class.forName("java.lang.M3StringSourcePrecompute");
            Class<?> m3 = Class.forName("java.lang.M3String");
            IS_PREPARED = type.getDeclaredMethod("isPrepared", m3);
            IS_PREPARED.setAccessible(true);
            MAX_BYTES = type.getDeclaredMethod("maximumRetainedPrimitiveBytes");
            MAX_BYTES.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        deterministicHotReuse();
        randomizedRepeatedSearch();
        long maximum = (long) MAX_BYTES.invoke(null);
        check(maximum > 0L && maximum < 8L * 1024 * 1024,
                "source precompute primitive ceiling=" + maximum);
        System.out.println(
                "M3_STRING_SOURCE_PRECOMPUTE_PASS checks=" + checks
                        + " maxPrimitiveBytes=" + maximum);
    }

    private static void deterministicHotReuse() throws Exception {
        String source = fresh(
                "a".repeat(700)
                        + "needle-one|"
                        + "b".repeat(700)
                        + "needle-two|"
                        + "c".repeat(700)
                        + "needle-one|"
                        + "d".repeat(700));
        Object body = body(source);
        check(!(boolean) IS_PREPARED.invoke(null, body), "source index initially cold");

        String needleOne = fresh("needle-one");
        String needleTwo = fresh("needle-two");
        String needleThree = fresh("needle-three");
        check(source.indexOf(needleOne) == 700, "first search exact");
        check(!(boolean) IS_PREPARED.invoke(null, body), "first search only admits source");

        int second = source.lastIndexOf(needleOne);
        check(second > 1400, "second search exact reverse");
        check((boolean) IS_PREPARED.invoke(null, body), "second search prepares source index");

        check(source.indexOf(needleTwo) == 1411, "prepared forward search");
        check(source.indexOf(needleTwo, 1000) == 1411, "prepared from-index search");
        check(source.indexOf(needleTwo, 0, 1411) == -1, "prepared bounded end exclusion");
        check(source.indexOf(needleTwo, 0, 1421) == 1411, "prepared bounded end inclusion");
        check(source.lastIndexOf(needleTwo, source.length()) == 1411,
                "prepared reverse search");
        check(source.indexOf(needleThree) == -1, "prepared absent search");

        String dense = fresh("abc".repeat(600));
        String abc = fresh("abc");
        check(dense.indexOf(abc) == 0, "dense source first exact");
        check(dense.lastIndexOf(abc) == dense.length() - 3, "dense source reverse exact");
        check((boolean) IS_PREPARED.invoke(null, body(dense)),
                "dense source may prepare even when execution falls back");
    }

    private static void randomizedRepeatedSearch() throws Exception {
        Random random = new Random(0x4d33535243505245L);
        char[] oracle = new char[4096];
        String alphabet = "abcdeXYZ0123|:-_";
        for (int index = 0; index < oracle.length; index++) {
            oracle[index] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        String source = fresh(new String(oracle));

        // Admit and build the source index before randomized comparisons.
        source.indexOf(fresh("abc"));
        source.indexOf(fresh("XYZ"));
        check((boolean) IS_PREPARED.invoke(null, body(source)), "random source index prepared");

        for (int trial = 0; trial < 3000; trial++) {
            int length = 3 + random.nextInt(10);
            char[] needleChars = new char[length];
            if ((trial & 3) == 0) {
                int start = random.nextInt(oracle.length - length + 1);
                System.arraycopy(oracle, start, needleChars, 0, length);
            } else {
                for (int index = 0; index < length; index++) {
                    needleChars[index] = alphabet.charAt(random.nextInt(alphabet.length()));
                }
            }
            String needle = fresh(new String(needleChars));
            int from = random.nextInt(oracle.length + 11) - 5;
            int end = random.nextInt(oracle.length + 1);
            int begin = random.nextInt(end + 1);

            check(
                    source.indexOf(needle, from)
                            == naiveIndexOf(oracle, needleChars, from, oracle.length),
                    "random indexOf trial=" + trial);
            check(
                    source.lastIndexOf(needle, from)
                            == naiveLastIndexOf(oracle, needleChars, from),
                    "random lastIndexOf trial=" + trial);
            check(
                    source.indexOf(needle, begin, end)
                            == naiveIndexOf(oracle, needleChars, begin, end),
                    "random bounded indexOf trial=" + trial);
        }
    }

    private static int naiveIndexOf(char[] source, char[] target, int fromIndex, int endIndex) {
        int end = Math.min(source.length, endIndex);
        int from = Math.max(0, fromIndex);
        if (target.length == 0) return Math.min(from, end);
        for (int start = from; start <= end - target.length; start++) {
            if (matchesAt(source, target, start)) return start;
        }
        return -1;
    }

    private static int naiveLastIndexOf(char[] source, char[] target, int fromIndex) {
        int start = Math.min(fromIndex, source.length - target.length);
        if (target.length == 0) return Math.max(-1, start);
        for (; start >= 0; start--) {
            if (matchesAt(source, target, start)) return start;
        }
        return -1;
    }

    private static boolean matchesAt(char[] source, char[] target, int start) {
        if (start < 0 || start > source.length - target.length) return false;
        for (int index = 0; index < target.length; index++) {
            if (source[start + index] != target[index]) return false;
        }
        return true;
    }

    private static String fresh(String value) {
        return new String(value.toCharArray());
    }

    private static Object body(String value) throws Exception {
        value.length();
        Object body = STRING_M3.get(value);
        check(body != null, "String admitted to M3");
        return body;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
