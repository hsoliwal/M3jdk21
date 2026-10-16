/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

/*
 * @test
 * @bug 8316879
 * @summary Preserve both regionMatches overloads for non-positive lengths and M3 ranges
 * @modules java.base/java.lang:open
 * @run main/othervm M3RegionMatchesNonPositive
 * @run main/othervm -XX:-CompactStrings M3RegionMatchesNonPositive
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3RegionMatchesNonPositive requireM3
 * @run main/othervm -XX:-CompactStrings -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3RegionMatchesNonPositive requireM3
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class M3RegionMatchesNonPositive {
    private static long checks;

    public static void main(String[] args) throws Exception {
        List<String> values = new ArrayList<>();
        for (String value : List.of("", "abc", "ABC", "\u0100bc", "\uD801\uDC00x", "\uD800x", "\uDC00x")) {
            String scalar = new String(value.toCharArray());
            String tuple = scalar.concat(new String("end".toCharArray()));
            values.add(scalar);
            values.add(tuple);
            values.add(tuple.substring(1));
        }
        if (args.length != 0) {
            if (args.length != 1 || !args[0].equals("requireM3")) throw new IllegalArgumentException("requireM3");
            Field field = String.class.getDeclaredField("m3");
            field.setAccessible(true);
            for (String value : values) {
                if (!value.isEmpty() && field.get(value) == null) {
                    throw new AssertionError("test value did not enter M3 storage");
                }
            }
        }
        int[] lengths = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1073741825,
                -1073741824, -2, -1, 0};
        for (String left : values) {
            for (String right : values) {
                for (int length : lengths) {
                    for (int to : offsets(left.length())) {
                        for (int from : offsets(right.length())) {
                            boolean expected = to >= 0 && from >= 0
                                    && (long) to + length <= left.length()
                                    && (long) from + length <= right.length();
                            check(left.regionMatches(to, right, from, length) == expected);
                            check(left.regionMatches(false, to, right, from, length) == expected);
                            check(left.regionMatches(true, to, right, from, length) == expected);
                        }
                    }
                }
            }
            for (int length : lengths) {
                for (int to : offsets(left.length())) {
                    for (int from : new int[] {-1, 0, Integer.MAX_VALUE}) {
                        boolean shortCircuit = from < 0 || to < 0 || (long) to + length > left.length();
                        nullCase(() -> left.regionMatches(to, null, from, length), shortCircuit);
                        nullCase(() -> left.regionMatches(false, to, null, from, length), shortCircuit);
                        nullCase(() -> left.regionMatches(true, to, null, from, length), shortCircuit);
                    }
                }
            }
        }
        for (String left : values) {
            for (String right : values) {
                for (int length = 1; length <= 3; length++) {
                    boolean valid = length <= left.length() && length <= right.length();
                    boolean exact = valid && left.substring(0, length).equals(right.substring(0, length));
                    boolean folded = valid && left.substring(0, length).equalsIgnoreCase(right.substring(0, length));
                    check(left.regionMatches(0, right, 0, length) == exact);
                    check(left.regionMatches(false, 0, right, 0, length) == exact);
                    check(left.regionMatches(true, 0, right, 0, length) == folded);
                }
            }
        }
        System.out.println("M3_REGION_NONPOSITIVE_PASS checks=" + checks);
    }

    private static int[] offsets(int length) {
        return new int[] {Integer.MIN_VALUE, -1, 0, 1, length, length + 1, Integer.MAX_VALUE};
    }

    private static void nullCase(java.util.function.BooleanSupplier action, boolean shortCircuit) {
        try {
            boolean result = action.getAsBoolean();
            check(shortCircuit && !result);
        } catch (NullPointerException expected) {
            check(!shortCircuit);
        }
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("check=" + checks);
        checks++;
    }
}
