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
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary Flat receivers with an M3-backed needle (A21): String.indexOf/lastIndexOf/contains
 *          and StringBuilder.indexOf/lastIndexOf, with and without a from-index, plus
 *          startsWith, endsWith and equals, agree with the flat-needle results for
 *          needles of 0..40 units (across the 16-unit reverse skip-search threshold) of either
 *          coder, present or absent, shaped as atoms, nested tuples and slices of padded owners
 *          (a Latin-1 spelling inside a UTF-16 owner narrows), over Latin-1 and UTF-16 haystacks
 *          of 0..5000 units.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3NeedleArgTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3NeedleArgTest
 */

import java.lang.reflect.Method;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3NeedleArgTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 5, 17, 100, 255, 256, 1000, 5000};
    private static final int[] NEEDLES = {0, 1, 2, 3, 7, 8, 15, 16, 17, 31, 32, 40};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x4e4545444c45L);
        for (int length : LENGTHS) {
            for (int haystackKind = 0; haystackKind < 3; haystackKind++) {
                String haystack = spell(random, length, haystackKind);
                StringBuilder builder = new StringBuilder(haystack);
                for (int needleLength : NEEDLES) {
                    for (int round = 0; round < 6; round++) {
                        String needle = needle(random, haystack, needleLength, round);
                        for (Object storage : shapes(needle, random)) {
                            compare(haystack, builder, needle, shaped(needle, storage), random);
                        }
                    }
                }
            }
        }
        System.out.println("M3NeedleArgTest checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        valueOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
        coderOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("coder"));
        m3Offset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("m3"));
    }

    private static void compare(String haystack, StringBuilder builder, String flat, String m3, Random random) {
        String label = flat.length() + " units in " + haystack.length();
        check(haystack.indexOf(m3) == haystack.indexOf(flat), "indexOf " + label);
        check(haystack.lastIndexOf(m3) == haystack.lastIndexOf(flat), "lastIndexOf " + label);
        check(haystack.contains(m3) == haystack.contains(flat), "contains " + label);
        check(builder.indexOf(m3) == builder.indexOf(flat), "builder indexOf " + label);
        check(builder.lastIndexOf(m3) == builder.lastIndexOf(flat), "builder lastIndexOf " + label);
        int from = random.nextInt(haystack.length() + 40) - 20;
        check(haystack.indexOf(m3, from) == haystack.indexOf(flat, from), "indexOf from " + from + " " + label);
        check(haystack.lastIndexOf(m3, from) == haystack.lastIndexOf(flat, from), "lastIndexOf from " + from + " " + label);
        check(builder.indexOf(m3, from) == builder.indexOf(flat, from), "builder indexOf from " + from + " " + label);
        check(builder.lastIndexOf(m3, from) == builder.lastIndexOf(flat, from), "builder lastIndexOf from " + from + " " + label);
        check(haystack.equals(m3) == haystack.equals(flat), "equals " + label);
        check(haystack.startsWith(m3) == haystack.startsWith(flat), "startsWith " + label);
        check(haystack.endsWith(m3) == haystack.endsWith(flat), "endsWith " + label);
    }

    private static String needle(Random random, String haystack, int length, int round) {
        if (round < 3 && haystack.length() >= length && length > 0) {
            int at = random.nextInt(haystack.length() - length + 1);
            String present = haystack.substring(at, at + length);
            if (round == 2) {
                StringBuilder altered = new StringBuilder(present);
                altered.setCharAt(length - 1, (char) (present.charAt(length - 1) ^ (round + 1)));
                return altered.toString();
            }
            return present;
        }
        return spell(random, length, round % 3);
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        if (text.isEmpty()) return emptyShapes();
        String padded = (random.nextBoolean() ? "一pad" : "pad") + text + "end";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        int begin = padded.length() - 3 - text.length();
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, begin, begin + text.length())};
    }

    private static Object[] emptyShapes() throws Exception {
        Object owner = atom("padend");
        return new Object[] {slice.invoke(owner, 3, 3)};
    }

    private static String spell(Random random, int length, int kind) {
        StringBuilder out = new StringBuilder(length);
        while (out.length() < length) {
            int pick = random.nextInt(24);
            if (pick < 18) out.append((char) ('a' + random.nextInt(3)));
            else if (pick < 21 || kind == 0) out.append((char) ('a' + random.nextInt(26)));
            else if (kind == 1) out.append((char) (0x80 + random.nextInt(0x80)));
            else out.append((char) (0x4e00 + random.nextInt(0x100)));
        }
        return out.toString();
    }

    private static Object atom(String text) throws Exception {
        char[] chars = text.toCharArray();
        return internChars.invoke(null, chars, 0, chars.length);
    }

    private static Object tupleOf(String text, Random random, int depth) throws Exception {
        if (depth == 0 || text.length() < 2) return atom(text);
        int cut = 1 + random.nextInt(text.length() - 1);
        return concat.invoke(tupleOf(text.substring(0, cut), random, depth - 1), tupleOf(text.substring(cut), random, depth - 1));
    }

    private static String shaped(String flat, Object storage) throws Exception {
        String out = (String) UNSAFE.allocateInstance(String.class);
        UNSAFE.putReference(out, valueOffset, UNSAFE.getReference(flat, valueOffset));
        UNSAFE.putByte(out, coderOffset, UNSAFE.getByte(flat, coderOffset));
        UNSAFE.putReference(out, m3Offset, storage);
        return out;
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
