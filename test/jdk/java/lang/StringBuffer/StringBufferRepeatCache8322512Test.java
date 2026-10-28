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
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, see https://www.gnu.org/licenses/.
 */
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: GPL-2.0-only

/*
 * @test
 * @bug 8322512
 * @summary Invalidate StringBuffer.toString cache for both inherited repeat overloads
 * @run main/othervm StringBufferRepeatCache8322512Test
 * @run main/othervm -Xint StringBufferRepeatCache8322512Test
 * @run main/othervm -XX:+CompactStrings StringBufferRepeatCache8322512Test
 * @run main/othervm -XX:-CompactStrings StringBufferRepeatCache8322512Test
 */

public class StringBufferRepeatCache8322512Test {
    private static long checks;

    private static void equal(StringBuffer actual, StringBuilder expected, String context) {
        String observed = actual.toString();
        if (!observed.equals(expected.toString())) {
            throw new AssertionError(context + " expected=[" + expected + "] actual=[" + observed + "]");
        }
        if (!observed.equals(actual.toString())) {
            throw new AssertionError(context + " repeated toString unstable");
        }
        checks++;
    }

    public static void main(String[] args) {
        String[] sources = {"", "a", "ab", "\u00e9", "\u4e2d", "\ud83d\ude80"};
        for (String initial : sources) {
            for (String toRepeat : sources) {
                for (int count : new int[]{0, 1, 2, 5}) {
                    StringBuffer actual = new StringBuffer(initial);
                    StringBuilder expected = new StringBuilder(initial);
                    actual.toString();
                    actual.toString();
                    actual.repeat((CharSequence) toRepeat, count);
                    expected.repeat((CharSequence) toRepeat, count);
                    equal(actual, expected, "CharSequence repeat");
                    actual.repeat('x', 2);
                    expected.repeat('x', 2);
                    equal(actual, expected, "sequential code point repeat");
                }
            }
            for (int cp : new int[]{'A', 0x00e9, 0x4e2d, 0x1f680}) {
                for (int count : new int[]{0, 1, 2, 5}) {
                    StringBuffer actual = new StringBuffer(initial);
                    StringBuilder expected = new StringBuilder(initial);
                    actual.toString();
                    actual.toString();
                    actual.repeat(cp, count);
                    expected.repeat(cp, count);
                    equal(actual, expected, "code point repeat");
                    actual.repeat("tail", 1);
                    expected.repeat("tail", 1);
                    equal(actual, expected, "sequential CharSequence repeat");
                }
            }
        }
        for (boolean charSequence : new boolean[]{true, false}) {
            StringBuffer actual = new StringBuffer("seed");
            actual.toString();
            try {
                if (charSequence) actual.repeat("bad", -1);
                else actual.repeat('x', -1);
                throw new AssertionError("negative repeat count accepted");
            } catch (IllegalArgumentException expected) {
                if (!"seed".equals(actual.toString())) {
                    throw new AssertionError("exception mutated source");
                }
                checks++;
            }
        }
        System.out.println("M3_STRINGBUFFER_REPEAT_CACHE_PASS checks=" + checks);
    }
}
