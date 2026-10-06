/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Prepared M3 String targets search mutable StringBuilder/StringBuffer sources exactly
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringBuilderSearchTest
 */

public class M3StringBuilderSearchTest {
    private static long checks;

    public static void main(String[] args) {
        latin1();
        utf16Collisions();
        mutation();
        buffer();
        System.out.println("M3_STRING_BUILDER_SEARCH_PASS|checks=" + checks);
    }

    private static void latin1() {
        String sourceText =
                "aaaaaaaaab".repeat(120) + "needle-XYZ" + "aaaaaaaaab".repeat(120);
        StringBuilder source = new StringBuilder(sourceText);
        String target = String.join("", "needle-", "XYZ");
        char[] oracle = sourceText.toCharArray();
        char[] needle = target.toCharArray();

        for (int from : new int[] {-5, 0, 1, 200, 800, source.length() - 20, source.length()}) {
            check(source.indexOf(target, from) == naiveIndexOf(oracle, needle, from),
                    "latin1 index from=" + from);
            check(source.lastIndexOf(target, from) == naiveLastIndexOf(oracle, needle, from),
                    "latin1 last from=" + from);
        }

        String absent = String.join("", "aaaaaaaaa", "c");
        check(source.indexOf(absent) == -1, "latin1 adversarial absent");
    }

    private static void utf16Collisions() {
        String sourceText =
                "\u0101".repeat(180)
                        + "\u0001\u0201\u0301\u0401\u0501\u0601\u0701\u0801"
                        + "\u0101".repeat(180);
        StringBuilder source = new StringBuilder(sourceText);
        char[] oracle = sourceText.toCharArray();

        for (String target : new String[] {
                String.join("", "\u0001\u0201\u0301\u0401", "\u0501\u0601\u0701\u0801"),
                String.join("", "\u0101\u0101\u0101\u0101", "\u0101\u0101\u0101\u0101"),
                String.join("", "\u0001\u0201\u0301\u0401", "\u0501\u0601\u0701\u0901")
        }) {
            char[] needle = target.toCharArray();
            check(source.indexOf(target) == naiveIndexOf(oracle, needle, 0),
                    "utf16 collision forward");
            check(source.lastIndexOf(target) == naiveLastIndexOf(oracle, needle, oracle.length),
                    "utf16 collision reverse");
        }
    }

    private static void mutation() {
        String target = String.join("", "mutable-", "target");
        StringBuilder source = new StringBuilder("prefix mutable-target suffix");

        check(source.indexOf(target) == 7, "mutation initial");
        source.replace(7, 21, "xxxxxxxxxxxxxx");
        check(source.indexOf(target) == -1, "mutation removes match");
        source.append(" mutable-target");
        check(source.lastIndexOf(target) == source.length() - target.length(),
                "mutation adds later match");
    }

    private static void buffer() {
        String target = String.join("", "buffer-", "needle");
        String text = "x".repeat(300) + "buffer-needle" + "x".repeat(300);
        StringBuffer source = new StringBuffer(text);
        char[] oracle = text.toCharArray();
        char[] needle = target.toCharArray();

        check(source.indexOf(target) == naiveIndexOf(oracle, needle, 0), "buffer forward");
        check(source.lastIndexOf(target) == naiveLastIndexOf(oracle, needle, oracle.length),
                "buffer reverse");
    }

    private static int naiveIndexOf(char[] source, char[] target, int fromIndex) {
        int from = Math.max(0, fromIndex);
        if (target.length == 0) return Math.min(from, source.length);
        for (int at = from; at <= source.length - target.length; at++) {
            int index = 0;
            while (index < target.length && source[at + index] == target[index]) index++;
            if (index == target.length) return at;
        }
        return -1;
    }

    private static int naiveLastIndexOf(char[] source, char[] target, int fromIndex) {
        int at = Math.min(fromIndex, source.length - target.length);
        if (target.length == 0) return Math.max(-1, Math.min(fromIndex, source.length));
        for (; at >= 0; at--) {
            int index = 0;
            while (index < target.length && source[at + index] == target[index]) index++;
            if (index == target.length) return at;
        }
        return -1;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
