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
 * @summary Builder appends of M3-backed Strings across coders (A22): append (whole and ranged),
 *          insert and replace of atoms, nested tuples and slices (Latin-1 spellings inside
 *          UTF-16 owners included) into Latin-1 and UTF-16 builders produce the same contents as
 *          the flat Strings, for lengths across the bulk (64) and window (4096) thresholds and
 *          ranges that start and end mid-window; a builder's subsequent toString, length,
 *          charAt and compareTo agree.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3BuilderAppendTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3BuilderAppendTest
 */

import java.lang.reflect.Method;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3BuilderAppendTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 63, 64, 65, 100, 1000, 4095, 4096, 4097, 5000, 9000, 20000};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x415050454e44L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 3; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    String m3 = shaped(text, storage);
                    for (int builderKind = 0; builderKind < 3; builderKind++) {
                        compare(text, m3, builderKind, random);
                    }
                }
            }
        }
        System.out.println("M3BuilderAppendTest checks=" + checks);
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

    private static StringBuilder builder(int kind, int capacity) {
        StringBuilder out = new StringBuilder(capacity + 4);
        if (kind == 1) out.append('一');
        if (kind == 2) out.append("ab");
        return out;
    }

    private static void compare(String flat, String m3, int builderKind, Random random) {
        int length = flat.length();
        String label = length + " units, builder " + builderKind;
        check(builder(builderKind, length).append(m3).toString().equals(builder(builderKind, length).append(flat).toString()), "append whole " + label);
        check(builder(builderKind, length).append((CharSequence) m3).toString().equals(builder(builderKind, length).append((CharSequence) flat).toString()), "append CharSequence " + label);
        int start = random.nextInt(length + 1);
        int end = start + random.nextInt(length - start + 1);
        check(builder(builderKind, length).append(m3, start, end).toString().equals(builder(builderKind, length).append(flat, start, end).toString()), "append range " + start + ".." + end + " " + label);
        if (length > 4100) {
            int edge = 4096 + random.nextInt(4);
            check(builder(builderKind, length).append(m3, 1, edge).toString().equals(builder(builderKind, length).append(flat, 1, edge).toString()), "append window edge " + label);
            check(builder(builderKind, length).append(m3, edge, length).toString().equals(builder(builderKind, length).append(flat, edge, length).toString()), "append from window edge " + label);
        }
        StringBuilder a = builder(builderKind, length).append("tail");
        StringBuilder b = builder(builderKind, length).append("tail");
        int at = random.nextInt(a.length() + 1);
        check(a.insert(at, m3).toString().equals(b.insert(at, flat).toString()), "insert " + label);
        StringBuilder c = builder(builderKind, length).append("replace me");
        StringBuilder d = builder(builderKind, length).append("replace me");
        int from = random.nextInt(c.length() + 1);
        int to = from + random.nextInt(c.length() - from + 1);
        check(c.replace(from, to, m3).toString().equals(d.replace(from, to, flat).toString()), "replace " + label);
        check(c.length() == d.length() && c.compareTo(d) == 0, "length/compareTo " + label);
        if (c.length() > 0) {
            int index = random.nextInt(c.length());
            check(c.charAt(index) == d.charAt(index), "charAt " + label);
        }
        StringBuilder twice = builder(builderKind, length).append(m3).append(m3, 0, length / 2).append(m3);
        StringBuilder twiceFlat = builder(builderKind, length).append(flat).append(flat, 0, length / 2).append(flat);
        check(twice.toString().equals(twiceFlat.toString()), "append thrice " + label);
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        if (text.isEmpty()) return new Object[] {slice.invoke(atom("padend"), 3, 3)};
        String padded = (random.nextBoolean() ? "一pad" : "pad") + text + "end";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        int begin = padded.length() - 3 - text.length();
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, begin, begin + text.length())};
    }

    private static String spell(Random random, int length, int kind) {
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = (char) switch (kind) {
                case 0 -> 'a' + random.nextInt(26);
                case 1 -> random.nextInt(0x100);
                default -> random.nextInt(4) == 0 ? 0x100 + random.nextInt(0xd700) : 'a' + random.nextInt(26);
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
