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
 * @summary Needle plans asked only for sources inside the trigram band and built without the
 *          forward tables (A36): indexOf, indexOf(from), indexOf(begin, end), lastIndexOf,
 *          lastIndexOf(from) and contains with M3 needles of 2..8193 units (present, absent by
 *          one unit of either coder, random) shaped as atoms, tuples and slices, over atoms,
 *          tuples and slices of 0..40000 units on both sides of the 256..32768-unit band and of
 *          both coders, each needle used three times (the first use builds its plan where the
 *          band allows one, the later uses serve it) and three hundred distinct needles used in two
 *          rotating passes (the plan cache thrashes), agree with the flat String; the same M3
 *          needles over the flat text (the reverse search of a needle of 16 units and up keeps
 *          its plan) agree too.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3NeedlePlanBandTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3NeedlePlanBandTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3NeedlePlanBandTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 100, 255, 256, 257, 1000, 5000, 32768, 32769, 40000};
    private static final int[] NEEDLE_LENGTHS = {2, 3, 8, 15, 16, 17, 33, 100, 8192, 8193};
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
        Random random = new Random(0x504c414e42414e44L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 3; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    compare(text, shaped(text, storage), random);
                }
            }
        }
        thrash(random);
        System.out.println("M3NeedlePlanBandTest checks=" + checks);
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
        for (String needle : needles(flat, random)) {
            Object[] needleShapes = shapes(needle, random);
            Object needleStorage = needleShapes[random.nextInt(needleShapes.length)];
            String m3Needle = shaped(needle, needleStorage);
            String what = label + " needle " + needle.length() + " units";
            for (int use = 0; use < 3; use++) {
                search(flat, m3, needle, m3Needle, what + " use " + use);
            }
            for (int round = 0; round < 2; round++) {
                int from = random.nextInt(length + 3) - 1;
                check(flat.indexOf(needle, from) == m3.indexOf(m3Needle, from), what + " indexOf from " + from);
                check(flat.lastIndexOf(needle, from) == m3.lastIndexOf(m3Needle, from), what + " lastIndexOf from " + from);
                if (length > 0) {
                    int begin = random.nextInt(length);
                    int end = begin + random.nextInt(length - begin + 1);
                    check(flat.indexOf(needle, begin, end) == m3.indexOf(m3Needle, begin, end), what + " indexOf " + begin + ".." + end);
                }
            }
        }
    }

    private static void search(String flat, String m3, String needle, String m3Needle, String what) {
        check(flat.indexOf(needle) == m3.indexOf(m3Needle), what + " indexOf");
        check(flat.lastIndexOf(needle) == m3.lastIndexOf(m3Needle), what + " lastIndexOf");
        check(flat.contains(needle) == m3.contains(m3Needle), what + " contains");
        check(flat.indexOf(needle) == flat.indexOf(m3Needle), what + " flat text, M3 needle");
        check(flat.lastIndexOf(needle) == flat.lastIndexOf(m3Needle), what + " flat text, M3 needle last");
    }

    private static void thrash(Random random) throws Exception {
        for (int length : new int[] {200, 1000, 20000}) {
            for (int kind = 0; kind < 3; kind++) {
                String flat = spell(random, length, kind);
                String m3 = shaped(flat, shapes(flat, random)[random.nextInt(3)]);
                List<String> flats = new ArrayList<>();
                List<String> m3s = new ArrayList<>();
                for (int i = 0; i < THRASH; i++) {
                    int n = 2 + random.nextInt(30);
                    int at = random.nextInt(length - n + 1);
                    String present = flat.substring(at, at + n);
                    String needle = random.nextBoolean() ? present : present.substring(0, n - 1) + (char) ('!' + random.nextInt(14));
                    flats.add(needle);
                    m3s.add(shaped(needle, shapes(needle, random)[random.nextInt(3)]));
                }
                for (int pass = 0; pass < 2; pass++) {
                    for (int i = 0; i < THRASH; i++) {
                        search(flat, m3, flats.get(i), m3s.get(i), length + " units thrash needle " + i + " pass " + pass);
                    }
                }
            }
        }
    }

    private static List<String> needles(String flat, Random random) {
        int length = flat.length();
        List<String> out = new ArrayList<>();
        for (int n : NEEDLE_LENGTHS) {
            if (n > length) break;
            out.add(flat.substring(0, n));
            out.add(flat.substring(length - n));
            int at = random.nextInt(length - n + 1);
            String present = flat.substring(at, at + n);
            out.add(present);
            out.add(present.substring(0, n - 1) + (char) 1);
            out.add(present.substring(0, n - 1) + 'Ā');
            out.add(present.substring(0, n - 1) + '中');
        }
        for (int round = 0; round < 3 && length > 0; round++) {
            out.add(spell(random, 2 + random.nextInt(4), random.nextInt(3)));
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
