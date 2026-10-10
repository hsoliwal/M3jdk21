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
 * @summary M3 needles searched over M3 haystacks through the flat needle's window lane with the
 *          needle's compact value (A35): indexOf, indexOf(from), indexOf(begin, end), lastIndexOf,
 *          lastIndexOf(from) and contains with M3 needles of every length (2..8193 units, over and
 *          under the 8192-unit plan bound; present at the start, the end, the middle and across
 *          the 64/256/1024/4096-unit window edges; absent by one unit of either coder; empty;
 *          longer than the text; the whole text) shaped as atoms, nested tuples and slices of
 *          padded owners of either coder, over atoms, nested tuples and slices of ASCII, Latin-1,
 *          mixed UTF-16 and periodic texts of 0..20000 units, each search run twice (the first
 *          use registers the source, the second builds its facts), agree with the flat String;
 *          the same M3 needles over the flat text agree too.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3NeedleLaneTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3NeedleLaneTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3NeedleLaneTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 3, 7, 8, 63, 64, 65, 127, 128, 255, 256, 257, 1000, 1023, 1024, 1025, 4095, 4096, 4097, 8191, 8192, 8193, 20000};
    private static final int[] NEEDLE_LENGTHS = {2, 3, 7, 8, 16, 33, 100, 1000, 5000, 8192, 8193};
    private static final int[] EDGES = {64, 256, 1024, 4096};
    private static long checks;
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
            for (int kind = 0; kind < 4; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    compare(text, shaped(text, storage), random);
                }
            }
        }
        System.out.println("M3NeedleLaneTest checks=" + checks);
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
            for (Object needleStorage : shapes(needle, random)) {
                String m3Needle = shaped(needle, needleStorage);
                String what = label + " needle " + needle.length() + " units";
                search(flat, m3, needle, m3Needle, what);
                search(flat, m3, needle, m3Needle, what + " again");
                for (int round = 0; round < 3; round++) {
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
        String empty = shaped("", shapes("", random)[0]);
        check(flat.indexOf("") == m3.indexOf(empty), label + " empty needle");
        check(flat.indexOf("", length + 5) == m3.indexOf(empty, length + 5), label + " empty needle beyond");
        check(flat.lastIndexOf("") == m3.lastIndexOf(empty), label + " empty needle last");
        check(flat.lastIndexOf("", -3) == m3.lastIndexOf(empty, -3), label + " empty needle last before");
        String longer = flat + "é!";
        String m3Longer = shaped(longer, atom(longer));
        check(flat.indexOf(longer) == m3.indexOf(m3Longer), label + " longer needle");
        check(flat.lastIndexOf(longer) == m3.lastIndexOf(m3Longer), label + " longer needle last");
        if (length > 0) {
            String whole = shaped(flat, atom(flat));
            check(flat.indexOf(flat) == m3.indexOf(whole), label + " whole text as needle");
            check(flat.lastIndexOf(flat) == m3.lastIndexOf(whole), label + " whole text as needle last");
        }
    }

    private static void search(String flat, String m3, String needle, String m3Needle, String what) {
        check(flat.indexOf(needle) == m3.indexOf(m3Needle), what + " indexOf");
        check(flat.lastIndexOf(needle) == m3.lastIndexOf(m3Needle), what + " lastIndexOf");
        check(flat.contains(needle) == m3.contains(m3Needle), what + " contains");
        check(flat.indexOf(needle, 0, flat.length()) == m3.indexOf(m3Needle, 0, flat.length()), what + " indexOf whole range");
        check(flat.indexOf(needle) == flat.indexOf(m3Needle), what + " flat text, M3 needle");
        check(flat.lastIndexOf(needle) == flat.lastIndexOf(m3Needle), what + " flat text, M3 needle last");
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
            for (int edge : EDGES) {
                if (edge + n / 2 + 1 <= length && edge - n / 2 >= 0) {
                    out.add(flat.substring(edge - n / 2, edge + n / 2 + 1));
                }
            }
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
                case 2 -> random.nextInt(4) == 0 ? 0x100 + random.nextInt(0xff00) : 'a' + random.nextInt(26);
                default -> "abcabd".charAt(i % 6);
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
