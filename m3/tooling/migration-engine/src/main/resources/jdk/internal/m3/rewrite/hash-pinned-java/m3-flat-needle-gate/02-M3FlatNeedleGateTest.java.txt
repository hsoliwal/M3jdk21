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
 * @summary Flat needles over M3 sources with the exact-trigram gate (A42): indexOf(String),
 *          indexOf(String, from), lastIndexOf(String), lastIndexOf(String, from) and contains
 *          over atoms, tuples and slices of texts over five alphabets of 0..40000 units on both
 *          sides of the 2048..32768 gate band, with present needles (at random places, at the
 *          first and the last unit, of 3..40 units), absent needles, needles whose end trigrams
 *          are present and whose middle one is not (and the other way round), needles of 0..2
 *          units, needles with surrogates; every search three times on the same source (the
 *          first use registers it, the second builds its trigram facts, the third probes them);
 *          against the flat String on every call.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3FlatNeedleGateTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3FlatNeedleGateTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3FlatNeedleGateTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 3, 8, 255, 256, 257, 1000, 1024, 2047, 2048, 2049, 4096, 20000, 32768, 32769, 40000};
    private static final int[] NEEDLE_LENGTHS = {3, 4, 5, 8, 17, 40};
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
        Random random = new Random(0x4e4545444c45L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 5; kind++) {
                String text = spell(random, length, kind);
                List<String> needles = needles(text, random, kind);
                Object[] shapes = shapes(text, random);
                for (int s = 0; s < shapes.length; s++) {
                    String m3 = shaped(text, shapes[s]);
                    for (int n = 0; n < needles.size(); n++) {
                        context = "length " + length + " kind " + kind + " shape " + s + " needle " + n + " ";
                        String needle = needles.get(n);
                        int from = length == 0 ? 0 : random.nextInt(length + 1);
                        for (int repeat = 0; repeat < 3; repeat++) {
                            search(text, m3, needle, from, repeat);
                        }
                    }
                }
            }
        }
        System.out.println("M3FlatNeedleGateTest checks=" + checks);
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

    private static void search(String flat, String m3, String needle, int from, int repeat) {
        String label = context + "repeat " + repeat + " needle " + needle.length() + " units ";
        int expected = flat.indexOf(needle);
        int actual = m3.indexOf(needle);
        check(actual == expected, label + "indexOf " + actual + " vs " + expected);
        expected = flat.indexOf(needle, from);
        actual = m3.indexOf(needle, from);
        check(actual == expected, label + "indexOf from " + from + " " + actual + " vs " + expected);
        expected = flat.lastIndexOf(needle);
        actual = m3.lastIndexOf(needle);
        check(actual == expected, label + "lastIndexOf " + actual + " vs " + expected);
        expected = flat.lastIndexOf(needle, from);
        actual = m3.lastIndexOf(needle, from);
        check(actual == expected, label + "lastIndexOf from " + from + " " + actual + " vs " + expected);
        check(m3.contains(needle) == flat.contains(needle), label + "contains");
    }

    private static List<String> needles(String text, Random random, int kind) {
        List<String> out = new ArrayList<>();
        int length = text.length();
        out.add("");
        out.add(spell(random, 1, kind));
        out.add(spell(random, 2, kind));
        for (int needleLength : NEEDLE_LENGTHS) {
            out.add(spell(random, needleLength, kind));
            if (needleLength <= length) {
                int at = random.nextInt(length - needleLength + 1);
                String present = text.substring(at, at + needleLength);
                out.add(present);
                out.add(text.substring(0, needleLength));
                out.add(text.substring(length - needleLength));
                out.add(mutate(present, needleLength / 2, random));
                out.add(mutate(present, 0, random));
                out.add(mutate(present, needleLength - 1, random));
                if (needleLength >= 4 && length >= 2 * needleLength) {
                    int other = random.nextInt(length - needleLength + 1);
                    out.add(present.substring(0, needleLength / 2) + text.substring(other, other + needleLength - needleLength / 2));
                }
            }
        }
        if (length >= 3) {
            out.add(text.substring(0, 3) + "𐐀");
            out.add("\ud801" + text.substring(length - 3));
            out.add(text.substring(length / 2, Math.min(length, length / 2 + 3)) + text.substring(0, Math.min(length, 2)));
        }
        return out;
    }

    private static String mutate(String needle, int at, Random random) {
        char unit = needle.charAt(at);
        char replacement = switch (random.nextInt(4)) {
            case 0 -> (char) (unit ^ 1);
            case 1 -> (char) ('a' + random.nextInt(26));
            case 2 -> (char) (0x4e00 + random.nextInt(0x1000));
            default -> '\udc00';
        };
        return needle.substring(0, at) + replacement + needle.substring(at + 1);
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
                case 3 -> out.append((char) (0x100 + random.nextInt(0xd700)));
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
