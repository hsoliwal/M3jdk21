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
 * @summary The reverse linear lane of the position precompute tests each window with the forward
 *          single-unit intrinsic and walks back only inside a window holding the unit, and the
 *          reverse block walk hands off like the forward one (A38): lastIndexOf(char) and
 *          lastIndexOf(char, from) with units at every k-th position, only at the start, only at
 *          the end, only in the middle, absent, the first, the last and random units, from indexes
 *          at and around the 64/256/1024/4096-unit window edges from the end, over atoms, tuples
 *          and slices of texts over a sparse alphabet, a dense ASCII alphabet, the full Latin-1
 *          range, a CJK-mixed and a dense BMP alphabet, of 0..40000 units on both sides of the
 *          256..32768 block band, each search run three times, agree with the flat String;
 *          indexOf(char) over the same cases agrees too.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3ReverseLaneTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3ReverseLaneTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3ReverseLaneTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 63, 64, 65, 255, 256, 257, 1000, 1024, 1025, 4095, 4096, 4097, 5000, 20000, 32768, 32769, 40000};
    private static final int[] EDGES = {64, 256, 1024, 4096};
    private static final int[] PERIODS = {1, 2, 7, 64, 65, 1000};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x5245564552534553L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 5; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    compare(text, shaped(text, storage), random);
                }
                for (int period : PERIODS) {
                    if (length == 0) break;
                    String periodic = plant(text, period, kind >= 3 ? '中' : '|');
                    Object[] periodicShapes = shapes(periodic, random);
                    compare(periodic, shaped(periodic, periodicShapes[random.nextInt(periodicShapes.length)]), random);
                }
            }
        }
        System.out.println("M3ReverseLaneTest checks=" + checks);
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
                check(flat.lastIndexOf(unit) == m3.lastIndexOf(unit), what + " lastIndexOf use " + use);
                check(flat.indexOf(unit) == m3.indexOf(unit), what + " indexOf use " + use);
            }
            for (int edge : EDGES) {
                for (int delta = -1; delta <= 1; delta++) {
                    int from = length - edge + delta;
                    check(flat.lastIndexOf(unit, from) == m3.lastIndexOf(unit, from), what + " lastIndexOf from " + from);
                    int begin = edge + delta;
                    check(flat.indexOf(unit, begin) == m3.indexOf(unit, begin), what + " indexOf from " + begin);
                }
            }
            for (int round = 0; round < 4; round++) {
                int from = random.nextInt(length + 3) - 1;
                check(flat.lastIndexOf(unit, from) == m3.lastIndexOf(unit, from), what + " lastIndexOf from " + from);
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
            out.add(flat.charAt(random.nextInt(length)));
        }
        out.add('|');
        out.add('中');
        out.add((char) 1);
        out.add('À');
        out.add('￿');
        for (int round = 0; round < 3; round++) {
            out.add(spell(random, 1, random.nextInt(5)).charAt(0));
        }
        return out;
    }

    /** The text with {@code mark} planted at every {@code period}-th position, counted from the end. */
    private static String plant(String text, int period, char mark) {
        char[] out = text.toCharArray();
        for (int at = out.length - 1; at >= 0; at -= period) out[at] = mark;
        return new String(out);
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
