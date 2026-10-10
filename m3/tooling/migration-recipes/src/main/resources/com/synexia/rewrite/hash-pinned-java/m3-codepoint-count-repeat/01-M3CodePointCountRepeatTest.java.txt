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
 * @summary The code point geometry of an M3 range is built on its repeat use and the bulk count
 *          runs the stock UTF-16 count over byte windows (A39): codePointCount(begin, end),
 *          codePointCount over the whole range and offsetByCodePoints with paired surrogates,
 *          lone high and lone low surrogates, pairs straddling the 64/256/1024/4096-unit window
 *          edges and the range ends, over atoms, tuples and slices of UTF-16 texts of 1..40000
 *          units on both sides of the 32768-unit band, each range counted three times (the first
 *          count in bulk, the second building the geometry, the third served by it) and three
 *          hundred distinct ranges in two rotating passes (the 64-slot cache thrashes), agree with
 *          the flat String.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3CodePointCountRepeatTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3CodePointCountRepeatTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3CodePointCountRepeatTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {1, 2, 3, 63, 64, 65, 255, 256, 257, 1000, 1023, 1024, 1025, 4095, 4096, 4097, 5000, 20000, 32768, 32769, 40000};
    private static final int[] EDGES = {64, 256, 320, 1024, 1344, 4096, 5440, 9536};
    private static final int THRASH = 300;
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x434f4445504f494eL);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 4; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    compare(text, shaped(text, storage), random);
                }
            }
        }
        thrash(random);
        System.out.println("M3CodePointCountRepeatTest checks=" + checks);
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

    private static void compare(String flat, String m3, Random random) {
        int length = flat.length();
        String label = length + " units";
        for (int use = 0; use < 3; use++) {
            check(flat.codePointCount(0, length) == m3.codePointCount(0, length), label + " whole use " + use);
        }
        for (int edge : EDGES) {
            for (int delta = -2; delta <= 2; delta++) {
                int at = edge + delta;
                if (at < 0 || at > length) continue;
                check(flat.codePointCount(0, at) == m3.codePointCount(0, at), label + " 0.." + at);
                check(flat.codePointCount(at, length) == m3.codePointCount(at, length), label + " " + at + "..end");
            }
        }
        for (int round = 0; round < 6; round++) {
            int begin = random.nextInt(length + 1);
            int end = begin + random.nextInt(length - begin + 1);
            check(flat.codePointCount(begin, end) == m3.codePointCount(begin, end), label + " " + begin + ".." + end);
            int index = random.nextInt(length + 1);
            int count = flat.codePointCount(0, length);
            int offset = random.nextInt(2 * count + 1) - count;
            Object expected = offsetOrFailure(flat, index, offset);
            Object actual = offsetOrFailure(m3, index, offset);
            check(expected.equals(actual), label + " offsetByCodePoints " + index + " by " + offset + ": " + expected + " vs " + actual);
        }
    }

    private static Object offsetOrFailure(String text, int index, int offset) {
        try {
            return text.offsetByCodePoints(index, offset);
        } catch (IndexOutOfBoundsException failure) {
            return "IOOBE";
        }
    }

    private static void thrash(Random random) throws Exception {
        List<String> flats = new ArrayList<>();
        List<String> m3s = new ArrayList<>();
        for (int i = 0; i < THRASH; i++) {
            String text = spell(random, 300 + random.nextInt(3000), random.nextInt(4));
            flats.add(text);
            m3s.add(shaped(text, shapes(text, random)[random.nextInt(3)]));
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < THRASH; i++) {
                String flat = flats.get(i);
                String m3 = m3s.get(i);
                int length = flat.length();
                int begin = random.nextInt(length + 1);
                int end = begin + random.nextInt(length - begin + 1);
                check(flat.codePointCount(0, length) == m3.codePointCount(0, length), "thrash " + i + " pass " + pass);
                check(flat.codePointCount(begin, end) == m3.codePointCount(begin, end), "thrash " + i + " pass " + pass + " " + begin + ".." + end);
            }
        }
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        String padded = (random.nextBoolean() ? "😀pad" : "pad\ud800") + text + "end\udc00é";
        int lead = padded.length() - 5 - text.length();
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, lead, lead + text.length())};
    }

    /** UTF-16 text: pairs, lone highs, lone lows and BMP units in the mix the kind sets. */
    private static String spell(Random random, int length, int kind) {
        StringBuilder out = new StringBuilder(length + 1);
        while (out.length() < length) {
            int roll = random.nextInt(16);
            if (kind == 0 || roll < 8) {
                out.append((char) (kind == 3 ? 0x4e00 + random.nextInt(0x1000) : 'a' + random.nextInt(26)));
            } else if (roll < 13) {
                out.appendCodePoint(0x10000 + random.nextInt(0x10000));
            } else if (roll < 15) {
                out.append((char) (0xd800 + random.nextInt(0x400)));
            } else {
                out.append((char) (0xdc00 + random.nextInt(0x400)));
            }
        }
        out.setLength(length);
        if (kind == 0 && length > 0) out.setCharAt(length - 1, '中');
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
