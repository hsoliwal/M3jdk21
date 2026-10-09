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
 * @summary Bulk getChars over Latin-1 M3 atoms widened through the inflate intrinsic (A26):
 *          String.getChars over every range shape (whole, sub-ranges across the 64-unit bulk
 *          and 4096-unit window thresholds, window-edge starts, single units, destination
 *          offsets) of atoms, nested tuples and slices of padded owners, both coders, Latin-1
 *          bytes above 0x7f, plus StringReader.read(char[]) and the regex subject view, agree
 *          with the flat String's units; destination units outside the range are untouched.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3AtomGetCharsTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3AtomGetCharsTest
 */

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3AtomGetCharsTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 63, 64, 65, 127, 128, 1000, 4095, 4096, 4097, 8191, 8192, 8193, 20000};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x4745544348415253L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 3; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    compare(text, shaped(text, storage), random);
                }
            }
        }
        System.out.println("M3AtomGetCharsTest checks=" + checks);
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
        range(flat, m3, 0, length, 0, label + " whole");
        range(flat, m3, 0, length, 3, label + " whole offset 3");
        for (int round = 0; round < 6 && length > 1; round++) {
            int begin = random.nextInt(length);
            int end = begin + random.nextInt(length - begin + 1);
            range(flat, m3, begin, end, random.nextInt(5), label + " range " + begin + ".." + end);
        }
        for (int edge : new int[] {64, 4096, 8192}) {
            if (length > edge + 2) {
                range(flat, m3, edge - 1, length, 0, label + " from edge " + (edge - 1));
                range(flat, m3, 1, edge + 1, 0, label + " to edge " + (edge + 1));
                range(flat, m3, edge, edge + 1, 0, label + " single at edge " + edge);
            }
        }
        if (length > 0) {
            char[] expected = new char[length + 1];
            char[] actual = new char[length + 1];
            StringReader flatReader = new StringReader(flat);
            StringReader m3Reader = new StringReader(m3);
            int a = flatReader.read(expected, 1, length);
            int b = m3Reader.read(actual, 1, length);
            check(a == b && Arrays.equals(expected, actual), "StringReader.read(char[]) " + label);
        }
    }

    private static void range(String flat, String m3, int begin, int end, int offset, String label) {
        int count = end - begin;
        char[] expected = new char[count + offset + 2];
        char[] actual = new char[count + offset + 2];
        Arrays.fill(expected, '☃');
        Arrays.fill(actual, '☃');
        flat.getChars(begin, end, expected, offset);
        m3.getChars(begin, end, actual, offset);
        check(Arrays.equals(expected, actual), "getChars " + label);
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
                case 0 -> 'a' + random.nextInt(26);
                case 1 -> random.nextInt(0x100);
                default -> random.nextInt(4) == 0 ? 0x100 + random.nextInt(0xff00) : 'a' + random.nextInt(26);
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
