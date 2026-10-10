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
 * @summary Trigram facts built on the repeat use of a source (A34): indexOf and lastIndexOf with
 *          M3 needles (present, absent, sharing trigrams with the text but absent), regex
 *          literal find and matches, and contains, over fresh sources of 256..32768 units
 *          searched once, searched again (the facts built on the repeat use) and in rotation
 *          with hundreds of other sources (the facts cache thrashes), atoms, tuples and slices,
 *          both coders, agree with the flat String on every call.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3TrigramRepeatTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3TrigramRepeatTest
 */

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import jdk.internal.misc.Unsafe;

public class M3TrigramRepeatTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {255, 256, 257, 1000, 4096, 20000, 32768, 32769};
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
        Random random = new Random(0x5452494752414dL);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 3; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    repeated(text, shaped(text, storage), random);
                }
            }
        }
        rotation(random);
        System.out.println("M3TrigramRepeatTest checks=" + checks);
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

    /** The same source searched four times: the first use runs exact, the later ones gate. */
    private static void repeated(String flat, String m3, Random random) throws Exception {
        int length = flat.length();
        List<String> needles = new ArrayList<>();
        needles.add(flat.substring(0, 8));
        needles.add(flat.substring(length - 8));
        int at = random.nextInt(length - 8);
        needles.add(flat.substring(at, at + 8));
        needles.add(flat.substring(at, at + 3) + "\u0001" + flat.substring(at + 3, at + 6));
        needles.add("zq\u0001xj\u0002kv");
        needles.add(flat.substring(at + 1, at + 4) + flat.substring(at, at + 3));
        for (int round = 0; round < 4; round++) {
            for (String needle : needles) {
                String m3Needle = shaped(needle, atom(needle));
                String what = length + " units round " + round + " needle " + needle.length();
                check(m3.indexOf(m3Needle) == flat.indexOf(needle), what + " indexOf m3 needle");
                check(m3.indexOf(needle) == flat.indexOf(needle), what + " indexOf flat needle");
                check(m3.lastIndexOf(m3Needle) == flat.lastIndexOf(needle), what + " lastIndexOf m3 needle");
                check(m3.contains(m3Needle) == flat.contains(needle), what + " contains");
                Pattern literal = Pattern.compile(Pattern.quote(needle));
                check(literal.matcher(m3).find() == literal.matcher(flat).find(), what + " regex literal find");
                check(m3.matches(".*" + Pattern.quote(needle) + ".*") == flat.matches(".*" + Pattern.quote(needle) + ".*"), what + " regex matches");
            }
        }
    }

    /** Hundreds of distinct 1000-unit sources searched in rotation, three passes: the facts cache thrashes. */
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
                int at = (i * 7 + pass * 13) % (flat.length() - 8);
                String needle = pass == 0 ? "zq\u0001xj\u0002kv" : flat.substring(at, at + 8);
                String m3Needle = shaped(needle, atom(needle));
                String what = "rotation " + i + " pass " + pass;
                check(m3.indexOf(m3Needle) == flat.indexOf(needle), what + " indexOf m3 needle");
                check(m3.lastIndexOf(m3Needle) == flat.lastIndexOf(needle), what + " lastIndexOf m3 needle");
                Pattern literal = Pattern.compile(Pattern.quote(needle));
                check(literal.matcher(m3).find() == literal.matcher(flat).find(), what + " regex literal find");
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
