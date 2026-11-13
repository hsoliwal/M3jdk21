/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
import java.lang.reflect.Field;
import java.util.Arrays;

/** Bounded diagnosis of independently retained legacy fixture failures. */
public final class M3StringLegacyFixtureProbe {
    public static void main(String[] args) throws Exception {
        String whole = new String("0123456789-\u0100-\uD83D\uDE42-tail".toCharArray());
        String middle = whole.substring(3, whole.length() - 4);
        String nested = middle.substring(2, middle.length() - 2);
        String inheritedExpectation = "56789-\u0100-\uD83D\uDE42";
        String utf16Slice = "56789-\u0100-\uD83D";
        if (!nested.equals(utf16Slice) || nested.equals(inheritedExpectation)) {
            throw new AssertionError("unexpected independent UTF16 slice geometry");
        }
        System.out.println("LEGACY_NESTED_EXPECTATION_DIAGNOSED nestedUnits=" + nested.length()
                + " inheritedExpectedUnits=" + inheritedExpectation.length()
                + " lastUnit=" + Integer.toHexString(nested.charAt(nested.length() - 1)));
        if (args.length == 1 && args[0].equals("m3")) {
            Field backing = String.class.getDeclaredField("m3");
            backing.setAccessible(true);
            String firstRow = new String(new char[0]);
            if (backing.get(firstRow) != null) throw new AssertionError("unexpected empty M3 backing");
            System.out.println("LEGACY_ROW_PRECOMPUTE_FIRST_EMPTY_INPUT_NO_OWNER_CONFIRMED");
            var fields = Arrays.stream(Class.forName("java.lang.M3StringRegexDp")
                    .getDeclaredFields()).map(Field::getName).toList();
            if (fields.contains("caches") || fields.contains("RELATION_ACCESS")) {
                throw new AssertionError("unexpected legacy DP fields");
            }
            System.out.println("LEGACY_REFLECTION_FIELDS_ABSENT caches=true RELATION_ACCESS=true");
        }
    }
}
