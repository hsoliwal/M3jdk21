/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3-backed String ranges append/insert into builders without materializing String.value
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringBuilderInteropTest
 */

public class M3StringBuilderInteropTest {
    private static long checks;

    public static void main(String[] args) {
        String latin = String.join("", "alpha", "-", "beta");
        String wide = String.join("", "ab", "\u0100", "cd", "\ud83d", "\ude42", "ef");

        StringBuilder builder = new StringBuilder();
        builder.append(latin);
        check(builder.toString().equals("alpha-beta"), "append Latin1");

        builder.setLength(0);
        builder.append(wide);
        check(builder.toString().equals("ab\u0100cd\ud83d\ude42ef"), "append UTF16");

        builder.setLength(0);
        builder.append((CharSequence) wide, 1, wide.length() - 1);
        check(builder.toString().equals("b\u0100cd\ud83d\ude42e"), "append ranged UTF16");

        builder.setLength(0);
        builder.append("prefix:");
        builder.insert(3, wide, 1, wide.length() - 1);
        check(builder.toString().equals("preb\u0100cd\ud83d\ude42efix:"), "insert ranged UTF16");

        builder.setLength(0);
        builder.append("latin:");
        builder.append(wide, 0, 2);
        check(builder.toString().equals("latin:ab"), "late inflation avoided for Latin1 range");

        builder.setLength(0);
        builder.append("latin:");
        builder.append(wide, 0, 3);
        check(builder.toString().equals("latin:ab\u0100"), "late inflation at first wide unit");

        String searchTarget = String.join("", "\u0100", "cd", "\ud83d\ude42");
        StringBuilder searchable = new StringBuilder("xxab\u0100cd\ud83d\ude42efyyab\u0100cd\ud83d\ude42ef");
        check(searchable.indexOf(searchTarget) == 4, "builder indexOf M3 target");
        check(searchable.indexOf(searchTarget, 5) == 15, "builder indexOf M3 target from");
        check(searchable.lastIndexOf(searchTarget) == 15, "builder lastIndexOf M3 target");
        check(searchable.lastIndexOf(searchTarget, 14) == 4,
                "builder lastIndexOf M3 target from");

        String longTarget = String.join("", "needle-", "XYZ");
        String longText = "aaaaaaaaab".repeat(120)
                + "needle-XYZ"
                + "aaaaaaaaab".repeat(120);
        StringBuilder preparedSearch = new StringBuilder(longText);
        char[] longOracle = longText.toCharArray();
        char[] longNeedle = longTarget.toCharArray();
        for (int from : new int[] {-5, 0, 1, 200, 800, preparedSearch.length() - 20}) {
            check(preparedSearch.indexOf(longTarget, from)
                            == naiveIndexOf(longOracle, longNeedle, from),
                    "builder prepared target index from=" + from);
            check(preparedSearch.lastIndexOf(longTarget, from)
                            == naiveLastIndexOf(longOracle, longNeedle, from),
                    "builder prepared target last from=" + from);
        }

        preparedSearch.replace(
                preparedSearch.indexOf(longTarget),
                preparedSearch.indexOf(longTarget) + longTarget.length(),
                "xxxxxxxxxx");
        check(preparedSearch.indexOf(longTarget) == -1,
                "builder mutable source is never cached");

        String collisionTarget = String.join(
                "",
                "\u0001\u0201\u0301\u0401",
                "\u0501\u0601\u0701\u0801");
        String collisionText = "\u0101".repeat(180)
                + "\u0001\u0201\u0301\u0401\u0501\u0601\u0701\u0801"
                + "\u0101".repeat(180);
        StringBuilder collisionBuilder = new StringBuilder(collisionText);
        check(collisionBuilder.indexOf(collisionTarget)
                        == naiveIndexOf(
                                collisionText.toCharArray(), collisionTarget.toCharArray(), 0),
                "builder prepared target UTF16 low-byte collision");

        StringBuffer buffer = new StringBuffer();
        buffer.append(latin);
        buffer.append('|');
        buffer.append(wide, 1, wide.length() - 1);
        check(buffer.toString().equals("alpha-beta|b\u0100cd\ud83d\ude42e"), "StringBuffer ranged append");

        StringBuilder mixed = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            mixed.append((i & 1) == 0 ? latin : wide, 1, ((i & 1) == 0 ? latin : wide).length() - 1);
        }
        String actual = mixed.toString();
        int expectedLength = 0;
        for (int i = 0; i < 256; i++) {
            String source = (i & 1) == 0 ? latin : wide;
            expectedLength += source.length() - 2;
        }
        check(actual.length() == expectedLength, "mixed append length");

        System.out.println("M3_STRING_BUILDER_INTEROP_PASS|checks=" + checks);
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
