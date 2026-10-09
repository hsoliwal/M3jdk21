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
 * @summary Plain-unit comparisons across coders (A30): a UTF-16-coded M3 range holding Latin-1
 *          text (a slice of a wide owner, a tuple with a wide piece) against Latin-1 flat Strings,
 *          and Latin-1 atoms, tuples and slices against UTF-16 flat Strings (prefix-equal, a wide
 *          unit at a random place, at the first or last unit or at the 64/2048/4096/8192-unit
 *          window edges), through equals, compareTo, startsWith, endsWith, regionMatches and
 *          contentEquals, agree with the flat Strings; same-coder shapes and differences at every
 *          window edge included; 0..20000 units.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3MixedCoderCompareTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3MixedCoderCompareTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3MixedCoderCompareTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 63, 64, 65, 1000, 2047, 2048, 2049, 4095, 4096, 4097, 8191, 8192, 8193, 20000};
    private static final int[] EDGES = {64, 2048, 4096, 8192};
    private static final char WIDE = '中';
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x4d49584544L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 2; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    String m3 = shaped(text, storage);
                    for (String other : others(text, random)) {
                        compare(text, m3, other);
                    }
                }
            }
        }
        System.out.println("M3MixedCoderCompareTest checks=" + checks);
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

    private static void compare(String flat, String m3, String other) {
        String label = flat.length() + "/" + other.length() + " units";
        check(m3.equals(other) == flat.equals(other), label + " equals");
        check(other.equals(m3) == other.equals(flat), label + " equals reversed");
        check(m3.compareTo(other) == flat.compareTo(other), label + " compareTo");
        check(other.compareTo(m3) == other.compareTo(flat), label + " compareTo reversed");
        check(m3.startsWith(other) == flat.startsWith(other), label + " startsWith");
        check(other.startsWith(m3) == other.startsWith(flat), label + " startsWith reversed");
        check(m3.endsWith(other) == flat.endsWith(other), label + " endsWith");
        check(other.endsWith(m3) == other.endsWith(flat), label + " endsWith reversed");
        check(m3.contentEquals(other) == flat.contentEquals(other), label + " contentEquals");
        int len = Math.min(flat.length(), other.length());
        check(m3.regionMatches(0, other, 0, len) == flat.regionMatches(0, other, 0, len), label + " regionMatches");
        check(other.regionMatches(0, m3, 0, len) == other.regionMatches(0, flat, 0, len), label + " regionMatches reversed");
        if (len > 2) {
            check(m3.regionMatches(1, other, 1, len - 2) == flat.regionMatches(1, other, 1, len - 2), label + " regionMatches inner");
        }
    }

    private static List<String> others(String text, Random random) {
        int length = text.length();
        List<String> out = new ArrayList<>();
        out.add(new String(text.toCharArray()));
        out.add(text + WIDE);
        out.add(text + 'z');
        if (length > 0) {
            out.add(text.substring(0, length - 1));
            out.add(text.substring(0, length - 1) + WIDE);
            out.add(replace(text, 0, WIDE));
            out.add(replace(text, length - 1, WIDE));
            out.add(replace(text, random.nextInt(length), WIDE));
            out.add(replace(text, random.nextInt(length), 'q'));
            out.add(replace(text, random.nextInt(length), 'é'));
            for (int edge : EDGES) {
                for (int at = edge - 1; at <= edge + 1; at++) {
                    if (at < length) {
                        out.add(replace(text, at, WIDE));
                        out.add(replace(text, at, 'q'));
                    }
                }
            }
        } else {
            out.add("" + WIDE);
        }
        return out;
    }

    private static String replace(String text, int at, char unit) {
        return text.substring(0, at) + unit + text.substring(at + 1);
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        if (text.isEmpty()) return new Object[] {slice.invoke(atom("padend"), 3, 3)};
        String wide = WIDE + "pad" + text + "end" + WIDE;
        Object wideWhole = random.nextBoolean() ? atom(wide) : tupleOf(wide, random, 2);
        int begin = 4;
        List<Object> out = new ArrayList<>();
        out.add(atom(text));
        out.add(tupleOf(text, random, 1 + random.nextInt(3)));
        out.add(slice.invoke(wideWhole, begin, begin + text.length()));
        if (text.length() >= 2) {
            int cut = 1 + random.nextInt(text.length() - 1);
            Object wideLeft = slice.invoke(atom(WIDE + text.substring(0, cut)), 1, 1 + cut);
            out.add(concat.invoke(wideLeft, atom(text.substring(cut))));
        }
        return out.toArray();
    }

    private static String spell(Random random, int length, int kind) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = (char) (kind == 0 ? 'a' + random.nextInt(26) : random.nextInt(0x100));
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
