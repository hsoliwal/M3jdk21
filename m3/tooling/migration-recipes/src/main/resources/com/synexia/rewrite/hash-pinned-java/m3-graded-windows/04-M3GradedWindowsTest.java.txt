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
 * @summary Graded bulk windows (A33: 64, 256, 1024, then 4096 units) across the window lanes:
 *          equalsIgnoreCase, compareToIgnoreCase, regionMatches(ignoreCase), contentEquals
 *          against a CharBuffer, indexOf and lastIndexOf with flat needles, equals and compareTo
 *          across coders, indexOf(char) and lastIndexOf(char) outside the block range and on a
 *          first search inside it, with the difference, the hit or the needle at every window
 *          edge (63..65, 1087..1089, 5183..5185 and their mirrors from the end) and at random
 *          places, over atoms, nested tuples and slices of padded owners, both coders, agree with
 *          the flat String (windows graded 64, 256, 1024, 4096 units).
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3GradedWindowsTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3GradedWindowsTest
 */

import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3GradedWindowsTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 63, 64, 65, 100, 1087, 1088, 1089, 2000, 5183, 5184, 5185, 10000, 40000};
    private static final int[] EDGES = {63, 64, 65, 1087, 1088, 1089, 5183, 5184, 5185};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x4752414445L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 3; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    compare(text, shaped(text, storage), random);
                }
            }
        }
        System.out.println("M3GradedWindowsTest checks=" + checks);
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

    private static void compare(String flat, String m3, Random random) throws Exception {
        int length = flat.length();
        String label = length + " units";
        List<Integer> places = new ArrayList<>();
        for (int edge : EDGES) {
            if (edge < length) places.add(edge);
            if (length - 1 - edge >= 0) places.add(length - 1 - edge);
        }
        if (length > 0) {
            places.add(0);
            places.add(length - 1);
            places.add(random.nextInt(length));
        }
        for (int at : places) {
            String other = replace(flat, at, flat.charAt(at) == 'x' ? 'y' : 'x');
            String swapped = replace(flat, at, swapCase(flat.charAt(at)));
            String otherWide = replace(flat, at, '中');
            String what = label + " at " + at;
            caseLanes(flat, m3, other, what + " differs");
            caseLanes(flat, m3, swapped, what + " case-swapped");
            caseLanes(flat, m3, otherWide, what + " wide");
            check(m3.contentEquals(CharBuffer.wrap(other.toCharArray())) == flat.contentEquals(CharBuffer.wrap(other.toCharArray())), what + " contentEquals differs");
            check(m3.equals(other) == flat.equals(other), what + " equals differs");
            check(m3.equals(otherWide) == flat.equals(otherWide), what + " equals wide");
            check(m3.compareTo(other) == flat.compareTo(other), what + " compareTo differs");
            check(m3.compareTo(otherWide) == flat.compareTo(otherWide), what + " compareTo wide");
            for (int n : new int[] {2, 8, 40}) {
                if (at + n <= length) {
                    String needle = flat.substring(at, at + n);
                    check(m3.indexOf(needle) == flat.indexOf(needle), what + " indexOf needle " + n);
                    check(m3.lastIndexOf(needle) == flat.lastIndexOf(needle), what + " lastIndexOf needle " + n);
                    check(m3.indexOf(needle, at) == flat.indexOf(needle, at), what + " indexOf needle " + n + " from at");
                    check(m3.lastIndexOf(needle, at) == flat.lastIndexOf(needle, at), what + " lastIndexOf needle " + n + " to at");
                }
            }
            int unit = flat.charAt(at);
            check(m3.indexOf(unit) == flat.indexOf(unit), what + " indexOf(char)");
            check(m3.lastIndexOf(unit) == flat.lastIndexOf(unit), what + " lastIndexOf(char)");
            check(m3.indexOf(unit, at) == flat.indexOf(unit, at), what + " indexOf(char) from at");
            check(m3.lastIndexOf(unit, at) == flat.lastIndexOf(unit, at), what + " lastIndexOf(char) to at");
        }
        check(m3.equalsIgnoreCase(new String(flat.toCharArray())) == flat.equalsIgnoreCase(flat), label + " equalsIgnoreCase equal");
        check(m3.contentEquals(CharBuffer.wrap(flat.toCharArray())), label + " contentEquals equal");
        check(m3.indexOf('\u0001') == flat.indexOf('\u0001') && m3.lastIndexOf('\u0001') == flat.lastIndexOf('\u0001'), label + " rare unit");
        check(m3.indexOf("\u0001\u0002") == flat.indexOf("\u0001\u0002") && m3.lastIndexOf("\u0001\u0002") == flat.lastIndexOf("\u0001\u0002"), label + " rare needle");
    }

    private static void caseLanes(String flat, String m3, String other, String what) throws Exception {
        String otherM3 = shaped(other, atom(other));
        check(m3.equalsIgnoreCase(other) == flat.equalsIgnoreCase(other), what + " equalsIgnoreCase");
        check(m3.equalsIgnoreCase(otherM3) == flat.equalsIgnoreCase(other), what + " equalsIgnoreCase m3 both");
        check(m3.compareToIgnoreCase(other) == flat.compareToIgnoreCase(other), what + " compareToIgnoreCase");
        check(other.compareToIgnoreCase(m3) == other.compareToIgnoreCase(flat), what + " compareToIgnoreCase reversed");
        int len = Math.min(flat.length(), other.length());
        check(m3.regionMatches(true, 0, other, 0, len) == flat.regionMatches(true, 0, other, 0, len), what + " regionMatches(true)");
        if (len > 2) {
            check(m3.regionMatches(true, 1, otherM3, 1, len - 2) == flat.regionMatches(true, 1, other, 1, len - 2), what + " regionMatches(true) inner m3");
        }
    }

    private static String replace(String text, int at, char unit) {
        return text.substring(0, at) + unit + text.substring(at + 1);
    }

    private static char swapCase(char unit) {
        return Character.isUpperCase(unit) ? Character.toLowerCase(unit) : Character.toUpperCase(unit);
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        if (text.isEmpty()) return new Object[] {slice.invoke(atom("padend"), 3, 3)};
        String padded = (random.nextBoolean() ? "一pad" : "pad") + text + "endé";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        int begin = padded.length() - 4 - text.length();
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, begin, begin + text.length())};
    }

    private static String spell(Random random, int length, int kind) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = (char) switch (kind) {
                case 0 -> (random.nextBoolean() ? 'A' : 'a') + random.nextInt(26);
                case 1 -> random.nextInt(0x100);
                default -> random.nextInt(5) == 0 ? 0x4e00 + random.nextInt(0x1000) : 'a' + random.nextInt(26);
            };
        }
        return new String(out);
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
