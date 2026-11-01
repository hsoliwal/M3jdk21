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
 * @summary lastIndexOf of an M3 needle over a flat String or StringBuilder (A43): the stock
 *          reverse search's shape with a hand-off to the reverse KMP agrees with the flat needle
 *          for needles of 1..4097 units (across the 16-unit lane threshold) shaped as atoms,
 *          tuples and slices, present at the start, the end and the middle, absent, absent with
 *          the text's commonest unit at their end and periodic over periodic texts (the hand-off
 *          cases), over texts of letters, the full Latin-1 range, CJK-mixed, periodic, single-
 *          unit and surrogate alphabets of 0..20000 units, with every from-index shape.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3ReverseNeedleTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3ReverseNeedleTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3ReverseNeedleTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 15, 16, 17, 100, 255, 256, 1000, 2048, 4095, 4096, 4097, 16384, 20000};
    private static final int[] NEEDLE_LENGTHS = {1, 2, 15, 16, 17, 31, 32, 64, 100, 257, 1024, 4097};
    private static long checks;
    private static String context = "";
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x5245564552534545L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 6; kind++) {
                String text = spell(random, length, kind);
                StringBuilder builder = new StringBuilder(text);
                List<String> needles = needles(text, random, kind);
                for (int n = 0; n < needles.size(); n++) {
                    String needle = needles.get(n);
                    Object[] shapes = shapes(needle, random);
                    for (int s = 0; s < shapes.length; s++) {
                        context = "length " + length + " kind " + kind + " needle " + n + " (" + needle.length() + " units) shape " + s + " ";
                        search(text, builder, needle, shaped(needle, shapes[s]), random);
                    }
                }
            }
        }
        System.out.println("M3ReverseNeedleTest checks=" + checks);
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

    private static void search(String text, StringBuilder builder, String needle, String m3, Random random) {
        int length = text.length();
        int[] froms = {length, 0, -5, length + 5, length - needle.length(), length == 0 ? 0 : random.nextInt(length + 1), length / 2};
        check(text.lastIndexOf(m3) == text.lastIndexOf(needle), context + "lastIndexOf " + text.lastIndexOf(m3) + " vs " + text.lastIndexOf(needle));
        check(builder.lastIndexOf(m3) == builder.lastIndexOf(needle), context + "StringBuilder.lastIndexOf");
        for (int from : froms) {
            int expected = text.lastIndexOf(needle, from);
            int actual = text.lastIndexOf(m3, from);
            check(actual == expected, context + "lastIndexOf from " + from + " " + actual + " vs " + expected);
            check(builder.lastIndexOf(m3, from) == builder.lastIndexOf(needle, from), context + "StringBuilder.lastIndexOf from " + from);
        }
    }

    private static List<String> needles(String text, Random random, int kind) {
        List<String> out = new ArrayList<>();
        int length = text.length();
        char common = commonest(text);
        for (int needleLength : NEEDLE_LENGTHS) {
            out.add(spell(random, needleLength, kind));
            if (needleLength > 1) out.add(spell(random, needleLength - 1, kind) + '');
            if (needleLength > 1) out.add("" + String.valueOf(common).repeat(needleLength - 1));
            if (needleLength > 1) out.add(String.valueOf(common).repeat(needleLength - 1) + '');
            if (needleLength <= length) {
                int at = random.nextInt(length - needleLength + 1);
                out.add(text.substring(at, at + needleLength));
                out.add(text.substring(0, needleLength));
                out.add(text.substring(length - needleLength));
                String present = text.substring(at, at + needleLength);
                out.add(mutate(present, 0));
                out.add(mutate(present, needleLength - 1));
                out.add(mutate(present, needleLength / 2));
            }
        }
        if (length >= 2) {
            out.add(text.substring(0, 2).repeat(9) + '');
            out.add("𐐀" + text.substring(1, Math.min(length, 20)));
            out.add(text.substring(length - Math.min(length, 20)) + "\ud801");
        }
        return out;
    }

    private static char commonest(String text) {
        int[] counts = new int[0x10000];
        char best = 'a';
        for (int i = 0; i < text.length(); i++) {
            char unit = text.charAt(i);
            if (++counts[unit] > counts[best]) best = unit;
        }
        return best;
    }

    private static String mutate(String needle, int at) {
        char unit = needle.charAt(at);
        return needle.substring(0, at) + (char) (unit ^ 1) + needle.substring(at + 1);
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        if (text.isEmpty()) return new Object[] {slice.invoke(atom("padend"), 3, 3)};
        String padded = (random.nextBoolean() ? "一pad" : "pad") + text + "endé";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        int begin = padded.length() - 4 - text.length();
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, begin, begin + text.length())};
    }

    private static String spell(Random random, int length, int kind) {
        StringBuilder out = new StringBuilder(length + 1);
        while (out.length() < length) {
            switch (kind) {
                case 0 -> out.append((char) ((random.nextBoolean() ? 'A' : 'a') + random.nextInt(26)));
                case 1 -> out.append((char) random.nextInt(0x100));
                case 2 -> out.append((char) (random.nextInt(4) == 0 ? 0x4e00 + random.nextInt(0x1000) : 'a' + random.nextInt(26)));
                case 3 -> out.append(random.nextInt(50) == 0 ? 'c' : (out.length() & 1) == 0 ? 'a' : 'b');
                case 4 -> out.append(random.nextInt(200) == 0 ? 'b' : 'a');
                default -> {
                    if (random.nextInt(6) == 0) {
                        out.append(random.nextBoolean() ? "𐐀" : "\ud801");
                    } else {
                        out.append((char) ('a' + random.nextInt(4)));
                    }
                }
            }
        }
        out.setLength(length);
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
