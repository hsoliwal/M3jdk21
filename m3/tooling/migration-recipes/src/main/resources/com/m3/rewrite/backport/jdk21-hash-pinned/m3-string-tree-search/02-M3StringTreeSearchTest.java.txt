/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Differential prepared-fact tuple pruning for M3 String first/reverse literal search
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringTreeSearchTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

public class M3StringTreeSearchTest {
    private static final Field STRING_M3;
    private static final Method M3_FACTS;
    private static final Method PREPARE;
    private static final Method TREE_INDEX;
    private static final Method TREE_LAST;
    private static final int NOT_APPLICABLE;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_FACTS = m3.getDeclaredMethod("facts");
            M3_FACTS.setAccessible(true);
            Class<?> plan = Class.forName("java.lang.M3StringSearchPrecompute$Plan");
            Class<?> precompute = Class.forName("java.lang.M3StringSearchPrecompute");
            PREPARE = precompute.getDeclaredMethod("prepare", m3);
            PREPARE.setAccessible(true);
            Class<?> tree = Class.forName("java.lang.M3StringTreeSearch");
            TREE_INDEX = tree.getDeclaredMethod("indexOf", m3, m3, plan, int.class, int.class);
            TREE_LAST = tree.getDeclaredMethod("lastIndexOf", m3, m3, plan, int.class);
            TREE_INDEX.setAccessible(true);
            TREE_LAST.setAccessible(true);
            Field sentinel = tree.getDeclaredField("NOT_APPLICABLE");
            sentinel.setAccessible(true);
            NOT_APPLICABLE = sentinel.getInt(null);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        deterministicPruning();
        seamCrossing();
        slicedTuple();
        randomized();
        System.out.println("M3_STRING_TREE_SEARCH_PASS|checks=" + checks);
    }

    private static void deterministicPruning() throws Exception {
        String source = String.join(
                "",
                fresh("a".repeat(512)),
                fresh("b".repeat(512)),
                fresh("needle"),
                fresh("c".repeat(512)),
                fresh("d".repeat(512)));
        String needle = fresh("needle");
        prepare(source);
        prepare(needle);

        int expected = flat(source).indexOf(needle);
        check(source.indexOf(needle) == expected, "prepared first search");
        check(source.lastIndexOf(needle) == expected, "prepared reverse search");

        int direct = treeIndex(source, needle, 0, source.length());
        check(direct != NOT_APPLICABLE, "tree path applicable after facts");
        check(direct == expected, "direct tree first result");
        check(treeLast(source, needle, source.length()) == expected, "direct tree reverse result");

        String absent = fresh("not-present-anywhere");
        prepare(absent);
        check(source.indexOf(absent) == -1, "prepared absent search");
        check(treeIndex(source, absent, 0, source.length()) == -1, "tree absent proof");
    }

    private static void seamCrossing() throws Exception {
        String source = String.join(
                "",
                fresh("x".repeat(384) + "ABCD"),
                fresh("EFGH" + "y".repeat(384)));
        String needle = fresh("ABCDEFGH");
        prepare(source);
        prepare(needle);
        int expected = 384;
        check(source.indexOf(needle) == expected, "seam first");
        check(source.lastIndexOf(needle) == expected, "seam reverse");
        check(treeIndex(source, needle, 0, source.length()) == expected, "direct seam first");
        check(treeLast(source, needle, source.length()) == expected, "direct seam reverse");

        String overlap = String.join("", fresh("q".repeat(256) + "aaa"), fresh("aa" + "q".repeat(256)));
        String aa = fresh("aaa");
        prepare(overlap);
        prepare(aa);
        int first = naiveIndexOf(chars(overlap), chars(aa), 0, overlap.length());
        int last = naiveLastIndexOf(chars(overlap), chars(aa), overlap.length());
        check(overlap.indexOf(aa) == first, "overlap seam first");
        check(overlap.lastIndexOf(aa) == last, "overlap seam last");
    }

    private static void slicedTuple() throws Exception {
        String whole = String.join(
                "",
                fresh("L".repeat(300)),
                fresh("alpha"),
                fresh("M".repeat(300)),
                fresh("beta"),
                fresh("R".repeat(300)));
        String slice = whole.substring(113, whole.length() - 97);
        String needle = fresh("beta");
        prepare(slice);
        prepare(needle);
        char[] oracle = chars(slice);
        int expected = naiveIndexOf(oracle, chars(needle), 0, oracle.length);
        check(slice.indexOf(needle) == expected, "tuple-range first");
        check(slice.lastIndexOf(needle) == naiveLastIndexOf(oracle, chars(needle), oracle.length),
                "tuple-range reverse");
        check(treeIndex(slice, needle, 0, slice.length()) == expected, "direct tuple-range first");
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d33545245455345L);
        String[] atoms = {
                "a", "bc", "XYZ", "needle", "\u03b2eta", "\ud83d", "\ude42", "1234", "--", "mnop"
        };
        for (int round = 0; round < 350; round++) {
            int parts = 12 + random.nextInt(28);
            String[] pieces = new String[parts];
            StringBuilder flat = new StringBuilder();
            for (int part = 0; part < parts; part++) {
                String atom = atoms[random.nextInt(atoms.length)];
                String piece = atom.repeat(4 + random.nextInt(12));
                pieces[part] = fresh(piece);
                flat.append(piece);
            }
            String source = String.join("", pieces);
            char[] oracle = flat.toString().toCharArray();
            prepare(source);

            String needleText;
            if (round % 3 != 0 && oracle.length != 0) {
                int start = random.nextInt(oracle.length);
                int width = 1 + random.nextInt(Math.min(16, oracle.length - start));
                needleText = new String(oracle, start, width);
            } else {
                needleText = "Z" + round + "Q";
            }
            if (needleText.length() < 2) needleText += "x";
            String needle = fresh(needleText);
            prepare(needle);
            char[] needleChars = needleText.toCharArray();

            int from = random.nextInt(oracle.length + 7) - 3;
            int end = random.nextInt(oracle.length + 1);
            int expected = naiveIndexOf(oracle, needleChars, from, end);
            check(source.indexOf(needle, from, end) == expected,
                    "random bounded first round=" + round);

            int reverseFrom = random.nextInt(oracle.length + 9) - 4;
            int expectedLast = naiveLastIndexOf(oracle, needleChars, reverseFrom);
            check(source.lastIndexOf(needle, reverseFrom) == expectedLast,
                    "random reverse round=" + round);
        }
    }

    private static String fresh(String value) {
        return new String(value.toCharArray());
    }

    private static void prepare(String value) throws Exception {
        value.length();
        Object m3 = STRING_M3.get(value);
        check(m3 != null, "String admitted to M3");
        M3_FACTS.invoke(m3);
    }

    private static int treeIndex(String source, String needle, int from, int end) throws Exception {
        Object sourceM3 = STRING_M3.get(source);
        Object needleM3 = STRING_M3.get(needle);
        Object plan = PREPARE.invoke(null, needleM3);
        check(plan != null, "prepared search plan");
        return (int) TREE_INDEX.invoke(null, sourceM3, needleM3, plan, from, end);
    }

    private static int treeLast(String source, String needle, int maximumStart) throws Exception {
        Object sourceM3 = STRING_M3.get(source);
        Object needleM3 = STRING_M3.get(needle);
        Object plan = PREPARE.invoke(null, needleM3);
        check(plan != null, "prepared reverse plan");
        return (int) TREE_LAST.invoke(null, sourceM3, needleM3, plan, maximumStart);
    }

    private static char[] chars(String value) {
        char[] out = new char[value.length()];
        value.getChars(0, value.length(), out, 0);
        return out;
    }

    private static String flat(String value) {
        return new String(chars(value));
    }

    private static int naiveIndexOf(char[] source, char[] needle, int fromIndex, int endIndex) {
        int from = Math.max(0, Math.min(fromIndex, source.length));
        int end = Math.min(source.length, endIndex);
        if (needle.length == 0) return Math.min(from, end);
        int limit = end - needle.length;
        for (int start = from; start <= limit; start++) {
            int index = 0;
            while (index < needle.length && source[start + index] == needle[index]) index++;
            if (index == needle.length) return start;
        }
        return -1;
    }

    private static int naiveLastIndexOf(char[] source, char[] needle, int fromIndex) {
        int maximum = Math.min(fromIndex, source.length - needle.length);
        if (maximum < 0) return -1;
        if (needle.length == 0) return maximum;
        for (int start = maximum; start >= 0; start--) {
            int index = 0;
            while (index < needle.length && source[start + index] == needle[index]) index++;
            if (index == needle.length) return start;
        }
        return -1;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
