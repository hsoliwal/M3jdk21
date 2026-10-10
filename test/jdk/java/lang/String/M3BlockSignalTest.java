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
 * @summary The position precompute's 256-bit block signals (A40): indexOf(char), indexOf(char,
 *          from), indexOf(char, begin, end), lastIndexOf(char) and lastIndexOf(char, from) with
 *          twenty units per text (the first, the last, a middle, sixteen random present or absent
 *          units of either coder and one absent control unit), over atoms, tuples and slices of
 *          texts over a sparse alphabet, a dense ASCII alphabet, the full Latin-1 range, a
 *          CJK-mixed and a dense BMP alphabet, of 0..40000 units on both sides of the 256..32768
 *          block band, each search run three times (the blocks and their signals are built on the
 *          repeat use), agree with the flat String.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3BlockSignalTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3BlockSignalTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3BlockSignalTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 63, 64, 65, 255, 256, 257, 1000, 4095, 4096, 4097, 20000, 32768, 32769, 40000};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x5349474e414c32L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 5; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    compare(text, shaped(text, storage), random);
                }
            }
        }
        System.out.println("M3BlockSignalTest checks=" + checks);
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
        for (char unit : units(flat, random)) {
            String what = label + " unit " + (int) unit;
            for (int use = 0; use < 3; use++) {
                check(flat.indexOf(unit) == m3.indexOf(unit), what + " indexOf use " + use);
                check(flat.lastIndexOf(unit) == m3.lastIndexOf(unit), what + " lastIndexOf use " + use);
            }
            for (int round = 0; round < 3; round++) {
                int from = random.nextInt(length + 3) - 1;
                check(flat.indexOf(unit, from) == m3.indexOf(unit, from), what + " indexOf from " + from);
                check(flat.lastIndexOf(unit, from) == m3.lastIndexOf(unit, from), what + " lastIndexOf from " + from);
                if (length > 0) {
                    int begin = random.nextInt(length);
                    int end = begin + random.nextInt(length - begin + 1);
                    check(flat.indexOf(unit, begin, end) == m3.indexOf(unit, begin, end), what + " indexOf " + begin + ".." + end);
                }
            }
        }
    }

    private static List<Character> units(String flat, Random random) {
        int length = flat.length();
        List<Character> out = new ArrayList<>();
        if (length > 0) {
            out.add(flat.charAt(0));
            out.add(flat.charAt(length - 1));
            out.add(flat.charAt(length / 2));
        }
        for (int round = 0; round < 16; round++) {
            out.add(random.nextBoolean() && length > 0 ? flat.charAt(random.nextInt(length)) : spell(random, 1, random.nextInt(5)).charAt(0));
        }
        out.add((char) 1);
        return out;
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
                case 1 -> 0x20 + random.nextInt(0x5f);
                case 2 -> random.nextInt(0x100);
                case 3 -> random.nextInt(5) == 0 ? 0x4e00 + random.nextInt(0x1000) : (random.nextBoolean() ? 'A' : 'a') + random.nextInt(26);
                default -> 0x20 + random.nextInt(0xd700);
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
