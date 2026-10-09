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
 * @summary Single-unit search over M3 Strings outside the block range read in bulk windows
 *          (A31): indexOf(int), indexOf(int, from), lastIndexOf(int) and lastIndexOf(int, from)
 *          with units present (first, last, random), absent, above 0xff in Latin-1 text,
 *          supplementary code points and random from values (negative and beyond too), over
 *          atoms, nested tuples and slices of padded owners, ASCII, Latin-1 and mixed UTF-16
 *          texts of every length 0..70, then up to 4097, and 32768..40000 (past the block range),
 *          agree with the flat String.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3CharSearchWindowsTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3CharSearchWindowsTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3CharSearchWindowsTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] MORE_LENGTHS = {100, 127, 128, 200, 255, 256, 257, 1000, 4095, 4096, 4097, 32767, 32768, 32769, 40000};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x43484152L);
        List<Integer> lengths = new ArrayList<>();
        for (int length = 0; length <= 70; length++) lengths.add(length);
        for (int length : MORE_LENGTHS) lengths.add(length);
        for (int length : lengths) {
            for (int kind = 0; kind < 3; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    compare(text, shaped(text, storage), random);
                }
            }
        }
        System.out.println("M3CharSearchWindowsTest checks=" + checks);
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
        for (int unit : units(flat, random)) {
            String what = label + " unit " + Integer.toHexString(unit);
            check(m3.indexOf(unit) == flat.indexOf(unit), what + " indexOf");
            check(m3.lastIndexOf(unit) == flat.lastIndexOf(unit), what + " lastIndexOf");
            for (int round = 0; round < 3; round++) {
                int from = random.nextInt(length + 3) - 1;
                check(m3.indexOf(unit, from) == flat.indexOf(unit, from), what + " indexOf from " + from);
                check(m3.lastIndexOf(unit, from) == flat.lastIndexOf(unit, from), what + " lastIndexOf from " + from);
            }
            if (length > 0) {
                check(m3.indexOf(unit, length - 1) == flat.indexOf(unit, length - 1), what + " indexOf from last");
                check(m3.lastIndexOf(unit, 0) == flat.lastIndexOf(unit, 0), what + " lastIndexOf from first");
            }
        }
    }

    private static List<Integer> units(String flat, Random random) {
        int length = flat.length();
        List<Integer> out = new ArrayList<>();
        out.add(1);
        out.add(0x4e2d);
        out.add(0xe9);
        out.add(0x10400);
        out.add(0xd801);
        if (length > 0) {
            out.add((int) flat.charAt(0));
            out.add((int) flat.charAt(length - 1));
            out.add((int) flat.charAt(random.nextInt(length)));
            int at = random.nextInt(length);
            if (Character.isHighSurrogate(flat.charAt(at)) && at + 1 < length) out.add(flat.codePointAt(at));
        }
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
                case 0 -> 'a' + random.nextInt(26);
                case 1 -> random.nextInt(0x100);
                default -> switch (random.nextInt(8)) {
                    case 0 -> 0x100 + random.nextInt(0xd700);
                    case 1 -> 0xd801;
                    case 2 -> 0xdc00 + random.nextInt(0x50);
                    default -> 'a' + random.nextInt(26);
                };
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
