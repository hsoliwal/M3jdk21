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
 * @summary Needle searches over an M3 haystack on bulk-copied windows (A14): indexOf, indexOf
 *          with fromIndex and with begin/end, lastIndexOf, contains with flat and M3 needles over
 *          atoms, nested tuples and slices agree with the stock flat results; matches placed on
 *          and across the window boundaries, overlapping repeats, needles longer than the
 *          haystack, empty needles, out-of-range indexes, Latin-1 and UTF-16 haystacks with
 *          needles of either coder, surrogate pairs split by a window edge, and needles on both
 *          sides of the long-needle threshold.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3StringWindowSearchTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringWindowSearchTest
 */

import java.lang.reflect.Method;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3StringWindowSearchTest {

    private static final int WINDOW = 4096;
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        randomSearches();
        windowBoundaries();
        edges();
        System.out.println("M3StringWindowSearchTest checks=" + checks);
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

    /** Random haystacks over small alphabets (many overlapping repeats) and needles of 0..40 units. */
    private static void randomSearches() throws Exception {
        Random random = new Random(0x57494e444f575f53L);
        for (int round = 0; round < 3_000; round++) {
            int length = round % 11 == 0 ? 1000 + random.nextInt(6_000) : 1 + random.nextInt(120);
            int kind = round % 4;
            String haystack = spell(random, length, kind);
            int needleLength = random.nextInt(Math.min(41, length + 2));
            int start = needleLength <= length ? random.nextInt(length - needleLength + 1) : 0;
            String needle = round % 3 == 0 && needleLength > 0 && needleLength <= length
                    ? haystack.substring(start, start + needleLength)
                    : spell(random, needleLength, random.nextInt(4));
            for (Object storage : shapes(haystack, random)) {
                String m3 = shaped(haystack, storage);
                compareAll(haystack, needle, m3, needle, "flat needle round " + round);
                String m3Needle = needle.isEmpty() ? needle : shaped(needle, round % 2 == 0 ? atom(needle) : tupleOf(needle, random, 2));
                compareAll(haystack, needle, m3, m3Needle, "M3 needle round " + round);
            }
        }
    }

    /** Matches exactly on, before and across the window edges of long haystacks. */
    private static void windowBoundaries() throws Exception {
        Random random = new Random(0x424f554e44415259L);
        int[] lengths = {WINDOW - 1, WINDOW, WINDOW + 1, 2 * WINDOW - 3, 2 * WINDOW, 2 * WINDOW + 5, 3 * WINDOW + 17};
        for (int length : lengths) {
            for (int wide = 0; wide < 2; wide++) {
                char[] units = new char[length];
                for (int index = 0; index < length; index++) {
                    units[index] = (char) (wide == 0 ? 'a' + random.nextInt(3) : 0x100 + random.nextInt(3));
                }
                String base = new String(units);
                for (int needleLength : new int[] {2, 5, 15, 16, 17, 40, 100}) {
                    if (needleLength > length) continue;
                    for (int at : new int[] {0, 1, WINDOW - needleLength - 1, WINDOW - needleLength, WINDOW - needleLength + 1, WINDOW - 1, WINDOW, WINDOW + 1, 2 * WINDOW - needleLength, 2 * WINDOW - 1, length - needleLength}) {
                        if (at < 0 || at + needleLength > length) continue;
                        char[] marked = units.clone();
                        char[] needleUnits = new char[needleLength];
                        for (int index = 0; index < needleLength; index++) {
                            needleUnits[index] = (char) (wide == 0 ? 'x' + random.nextInt(2) : 0x200 + random.nextInt(2));
                            marked[at + index] = needleUnits[index];
                        }
                        if (wide == 1 && needleLength >= 2) {
                            needleUnits[0] = '\ud801';
                            needleUnits[1] = '\udc00';
                            marked[at] = '\ud801';
                            marked[at + 1] = '\udc00';
                        }
                        String haystack = new String(marked);
                        String needle = new String(needleUnits);
                        String m3 = shaped(haystack, atom(haystack));
                        String m3Tuple = shaped(haystack, tupleOf(haystack, random, 2));
                        compareAll(haystack, needle, m3, needle, "boundary atom length " + length + " at " + at + " needle " + needleLength);
                        compareAll(haystack, needle, m3Tuple, needle, "boundary tuple length " + length + " at " + at + " needle " + needleLength);
                        compareAll(haystack, needle, m3, shaped(needle, atom(needle)), "boundary M3 needle length " + length + " at " + at + " needle " + needleLength);
                    }
                }
                compareAll(base, "zzz", shaped(base, atom(base)), "zzz", "boundary miss " + length);
            }
        }
    }

    /** Empty needles, needles longer than the haystack, out-of-range indexes, single units. */
    private static void edges() throws Exception {
        Random random = new Random(0x45444745535f4134L);
        for (int round = 0; round < 400; round++) {
            String haystack = spell(random, 1 + random.nextInt(50), round % 4);
            String m3 = shaped(haystack, round % 2 == 0 ? atom(haystack) : tupleOf(haystack, random, 2));
            String longer = haystack + spell(random, 1 + random.nextInt(5), round % 4);
            compareAll(haystack, "", m3, "", "empty needle " + round);
            compareAll(haystack, longer, m3, longer, "longer needle " + round);
            compareAll(haystack, longer, m3, shaped(longer, atom(longer)), "longer M3 needle " + round);
            int at = random.nextInt(haystack.length());
            String unit = haystack.substring(at, at + 1);
            compareAll(haystack, unit, m3, unit, "single unit " + round);
            String needle = haystack.substring(0, Math.min(3, haystack.length()));
            for (int from : new int[] {-5, -1, 0, 1, haystack.length() - 1, haystack.length(), haystack.length() + 3, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
                check(m3.indexOf(needle, from) == haystack.indexOf(needle, from), "indexOf from " + from + " round " + round);
                check(m3.lastIndexOf(needle, from) == haystack.lastIndexOf(needle, from), "lastIndexOf from " + from + " round " + round);
            }
        }
    }

    private static void compareAll(String haystack, String needle, String m3, String m3Needle, String where) {
        check(m3.indexOf(m3Needle) == haystack.indexOf(needle), "indexOf " + where);
        check(m3.lastIndexOf(m3Needle) == haystack.lastIndexOf(needle), "lastIndexOf " + where);
        check(m3.contains(m3Needle) == haystack.contains(needle), "contains " + where);
        int length = haystack.length();
        int from = length == 0 ? 0 : length / 3;
        check(m3.indexOf(m3Needle, from) == haystack.indexOf(needle, from), "indexOf from " + where);
        check(m3.lastIndexOf(m3Needle, from) == haystack.lastIndexOf(needle, from), "lastIndexOf from " + where);
        int end = length - length / 4;
        check(m3.indexOf(m3Needle, from, end) == haystack.indexOf(needle, from, end), "indexOf begin/end " + where);
        if (!needle.isEmpty()) {
            int expected = haystack.indexOf(needle);
            if (expected > 0) {
                check(m3.indexOf(m3Needle, expected + 1) == haystack.indexOf(needle, expected + 1), "indexOf after first " + where);
            }
        }
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        String padded = "pad" + text + "end";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, 3, 3 + text.length())};
    }

    private static String spell(Random random, int length, int kind) {
        StringBuilder out = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            out.append(switch (kind) {
                case 0 -> (char) ('a' + random.nextInt(3));
                case 1 -> (char) (random.nextInt(4) == 0 ? 0x80 + random.nextInt(0x80) : 'a' + random.nextInt(4));
                case 2 -> (char) (random.nextInt(3) == 0 ? 0x100 + random.nextInt(3) : 'a' + random.nextInt(3));
                default -> random.nextInt(6) == 0 ? (char) (0xd800 + random.nextInt(0x800)) : (char) ('a' + random.nextInt(3));
            });
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
        if (flat.isEmpty()) return flat;
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
