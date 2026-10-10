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
 * @summary Single-unit search over M3 sources inside the block range when a source is searched
 *          once, searched again (the block index built on the repeat search, A32), or searched
 *          in rotation with hundreds of other sources (the block cache thrashes): indexOf(int),
 *          indexOf(int, from), lastIndexOf(int) and lastIndexOf(int, from) with units present,
 *          absent and above 0xff over atoms, tuples and slices of 256..32768 units, both coders,
 *          agree with the flat String on every call, and the answers of the first and the
 *          repeat searches agree with each other.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3CharSearchRepeatTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3CharSearchRepeatTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3CharSearchRepeatTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {256, 257, 1000, 4096, 4097, 20000, 32768};
    private static final int ROTATION = 300;
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x5245504541L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 3; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    repeated(text, shaped(text, storage), random);
                }
            }
        }
        rotation(random);
        System.out.println("M3CharSearchRepeatTest checks=" + checks);
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

    /** The same source searched five times: the first search scans, the later ones index. */
    private static void repeated(String flat, String m3, Random random) {
        int length = flat.length();
        int[] units = {1, 0x4e2d, 0xe9, flat.charAt(0), flat.charAt(length - 1), flat.charAt(random.nextInt(length))};
        int[] froms = {-1, 0, length / 2, length - 1, length + 5};
        for (int round = 0; round < 5; round++) {
            for (int unit : units) {
                String what = length + " units unit " + Integer.toHexString(unit) + " round " + round;
                check(m3.indexOf(unit) == flat.indexOf(unit), what + " indexOf");
                check(m3.lastIndexOf(unit) == flat.lastIndexOf(unit), what + " lastIndexOf");
                for (int from : froms) {
                    check(m3.indexOf(unit, from) == flat.indexOf(unit, from), what + " indexOf from " + from);
                    check(m3.lastIndexOf(unit, from) == flat.lastIndexOf(unit, from), what + " lastIndexOf from " + from);
                }
            }
        }
    }

    /** Hundreds of distinct 1000-unit sources searched in rotation, three passes: the cache thrashes. */
    private static void rotation(Random random) throws Exception {
        String[] flats = new String[ROTATION];
        String[] m3s = new String[ROTATION];
        for (int i = 0; i < ROTATION; i++) {
            flats[i] = spell(random, 1000 + (i & 7), i % 3);
            m3s[i] = shaped(flats[i], i % 2 == 0 ? atom(flats[i]) : tupleOf(flats[i], random, 2));
        }
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < ROTATION; i++) {
                String flat = flats[i];
                String m3 = m3s[i];
                int unit = pass == 0 ? 1 : flat.charAt((i * 7 + pass) % flat.length());
                String what = "rotation " + i + " pass " + pass;
                check(m3.indexOf(unit) == flat.indexOf(unit), what + " indexOf");
                check(m3.lastIndexOf(unit) == flat.lastIndexOf(unit), what + " lastIndexOf");
                check(m3.indexOf(unit, 500) == flat.indexOf(unit, 500), what + " indexOf from 500");
            }
        }
    }

    private static Object[] shapes(String text, Random random) throws Exception {
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
