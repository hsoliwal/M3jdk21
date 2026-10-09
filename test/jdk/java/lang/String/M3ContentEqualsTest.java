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
 * @summary String.contentEquals over M3 receivers read in bulk windows (A29): against a
 *          CharBuffer, a custom CharSequence, a StringBuilder, a StringBuffer and a String that
 *          are equal, differ at a random unit, at the first or last unit, at the 4096/8192-unit
 *          window edges, or differ in length, over atoms, nested tuples and slices of padded
 *          owners, both coders, 0..20000 units, agree with the flat String; a sequence shorter or
 *          longer than the receiver is never read past its end.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3ContentEqualsTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3ContentEqualsTest
 */

import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3ContentEqualsTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 63, 64, 65, 1000, 4095, 4096, 4097, 8191, 8192, 8193, 20000};
    private static final int[] EDGES = {64, 4096, 8192};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    /** A CharSequence that is neither a String nor a builder and counts how far it is read. */
    private static final class Counted implements CharSequence {
        private final char[] units;
        int farthest = -1;

        Counted(char[] units) {
            this.units = units;
        }

        public int length() {
            return units.length;
        }

        public char charAt(int index) {
            farthest = Math.max(farthest, index);
            return units[index];
        }

        public CharSequence subSequence(int start, int end) {
            return new Counted(Arrays.copyOfRange(units, start, end));
        }

        public String toString() {
            return new String(units);
        }
    }

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x434f4e54454e54L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 3; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    compare(text, shaped(text, storage), random);
                }
            }
        }
        System.out.println("M3ContentEqualsTest checks=" + checks);
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
        for (char[] other : others(flat, random)) {
            String what = label + " vs " + other.length + " units";
            boolean expected = flat.contentEquals(CharBuffer.wrap(other));
            check(m3.contentEquals(CharBuffer.wrap(other)) == expected, what + " CharBuffer");
            Counted counted = new Counted(other);
            check(m3.contentEquals(counted) == expected, what + " custom CharSequence");
            check(counted.farthest < other.length, what + " read past the end");
            check(m3.contentEquals(new StringBuilder().append(other)) == expected, what + " StringBuilder");
            check(m3.contentEquals(new StringBuffer().append(other)) == expected, what + " StringBuffer");
            check(m3.contentEquals(new String(other)) == expected, what + " String");
            check(expected == (length == other.length && Arrays.equals(flat.toCharArray(), other)), what + " oracle");
        }
    }

    private static List<char[]> others(String flat, Random random) {
        char[] text = flat.toCharArray();
        int length = text.length;
        List<char[]> out = new ArrayList<>();
        out.add(text.clone());
        out.add(Arrays.copyOf(text, length + 1));
        if (length > 0) {
            out.add(Arrays.copyOf(text, length - 1));
            out.add(mutate(text, 0));
            out.add(mutate(text, length - 1));
            out.add(mutate(text, random.nextInt(length)));
            for (int edge : EDGES) {
                for (int at = edge - 1; at <= edge + 1; at++) {
                    if (at < length) out.add(mutate(text, at));
                }
            }
        }
        return out;
    }

    private static char[] mutate(char[] text, int at) {
        char[] out = text.clone();
        out[at] = out[at] == 'x' ? 'y' : 'x';
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
