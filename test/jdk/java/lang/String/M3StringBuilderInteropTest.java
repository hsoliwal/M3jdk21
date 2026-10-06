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

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
