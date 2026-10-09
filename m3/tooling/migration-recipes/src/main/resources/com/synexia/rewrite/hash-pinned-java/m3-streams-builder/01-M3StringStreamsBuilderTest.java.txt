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
 * @summary Builder comparison in place and streams over the bulk-read compact value (A17):
 *          contentEquals with StringBuilder and StringBuffer (capacity beyond the length, both
 *          coders on either side, differences at every position) and chars()/codePoints()
 *          (toArray, count, sum, parallel, spliterator characteristics and estimated size) on
 *          M3-backed Strings (atoms, nested tuples, slices) agree with the stock flat results.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3StringStreamsBuilderTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringStreamsBuilderTest
 */

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.Spliterator;

import jdk.internal.misc.Unsafe;

public class M3StringStreamsBuilderTest {

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
        builders();
        streams();
        System.out.println("M3StringStreamsBuilderTest checks=" + checks);
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

    /** contentEquals against builders of both coders with spare capacity, equal and differing. */
    private static void builders() throws Exception {
        Random random = new Random(0x4255494c44455253L);
        for (int round = 0; round < 4_000; round++) {
            int length = round % 9 == 0 ? 200 + random.nextInt(4_000) : random.nextInt(80);
            String text = spell(random, length, round % 4);
            for (Object storage : shapes(text, random)) {
                String m3 = shaped(text, storage);
                StringBuilder same = new StringBuilder(length + 1 + random.nextInt(64)).append(text);
                check(m3.contentEquals(same) == text.contentEquals(same), "equal builder " + round);
                check(m3.contentEquals(new StringBuffer(same)) == text.contentEquals(new StringBuffer(same)), "equal buffer " + round);
                StringBuilder widened = new StringBuilder().append('Ā').append(text);
                widened.deleteCharAt(0);
                check(m3.contentEquals(widened) == text.contentEquals(widened), "widened builder " + round);
                if (length > 0) {
                    int position = random.nextInt(length);
                    StringBuilder differing = new StringBuilder(text);
                    differing.setCharAt(position, (char) (text.charAt(position) ^ (1 + random.nextInt(0x1ff))));
                    check(m3.contentEquals(differing) == text.contentEquals(differing), "differing builder " + round + " at " + position);
                    StringBuilder last = new StringBuilder(text);
                    last.setCharAt(length - 1, (char) (text.charAt(length - 1) ^ 1));
                    check(m3.contentEquals(last) == text.contentEquals(last), "last unit builder " + round);
                }
                StringBuilder longer = new StringBuilder(text).append('x');
                check(m3.contentEquals(longer) == text.contentEquals(longer), "longer builder " + round);
                StringBuilder shorter = length == 0 ? new StringBuilder() : new StringBuilder(text.substring(0, length - 1));
                check(m3.contentEquals(shorter) == text.contentEquals(shorter), "shorter builder " + round);
                check(m3.contentEquals((CharSequence) text) == text.contentEquals((CharSequence) text), "contentEquals String " + round);
                check(m3.contentEquals(java.nio.CharBuffer.wrap(text)) == text.contentEquals(java.nio.CharBuffer.wrap(text)), "contentEquals CharBuffer " + round);
            }
        }
    }

    /** chars() and codePoints() agree with the flat streams and keep the stock characteristics. */
    private static void streams() throws Exception {
        Random random = new Random(0x53545245414d53L);
        for (int round = 0; round < 1_500; round++) {
            int length = round % 7 == 0 ? 200 + random.nextInt(4_000) : random.nextInt(80);
            String text = spell(random, length, round % 4);
            for (Object storage : shapes(text, random)) {
                String m3 = shaped(text, storage);
                check(Arrays.equals(m3.chars().toArray(), text.chars().toArray()), "chars toArray " + round);
                check(Arrays.equals(m3.codePoints().toArray(), text.codePoints().toArray()), "codePoints toArray " + round);
                check(m3.chars().count() == text.chars().count(), "chars count " + round);
                check(m3.codePoints().count() == text.codePoints().count(), "codePoints count " + round);
                check(m3.chars().sum() == text.chars().sum(), "chars sum " + round);
                check(m3.codePoints().parallel().sum() == text.codePoints().parallel().sum(), "codePoints parallel sum " + round);
                check(m3.chars().parallel().filter(unit -> unit > 0x7f).count() == text.chars().parallel().filter(unit -> unit > 0x7f).count(), "chars parallel filter " + round);
                Spliterator.OfInt a = m3.chars().spliterator();
                Spliterator.OfInt b = text.chars().spliterator();
                check(a.characteristics() == b.characteristics(), "chars characteristics " + round + " " + a.characteristics() + " vs " + b.characteristics());
                check(a.estimateSize() == b.estimateSize(), "chars estimateSize " + round);
                Spliterator.OfInt c = m3.codePoints().spliterator();
                Spliterator.OfInt d = text.codePoints().spliterator();
                check(c.characteristics() == d.characteristics(), "codePoints characteristics " + round);
                check(c.estimateSize() == d.estimateSize(), "codePoints estimateSize " + round);
                Spliterator.OfInt split = m3.chars().spliterator();
                Spliterator.OfInt half = split.trySplit();
                check((half == null) == (text.chars().spliterator().trySplit() == null), "chars trySplit " + round);
            }
        }
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        if (text.isEmpty()) return new Object[0];
        String padded = "épad" + text + "end\ud83d";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, 4, 4 + text.length())};
    }

    private static String spell(Random random, int length, int kind) {
        StringBuilder out = new StringBuilder(length);
        while (out.length() < length) {
            switch (kind) {
                case 0 -> out.append((char) ('a' + random.nextInt(26)));
                case 1 -> out.append((char) random.nextInt(0x100));
                case 2 -> out.append((char) (random.nextInt(3) == 0 ? 0x100 + random.nextInt(0xd700) : 'a' + random.nextInt(26)));
                default -> {
                    if (random.nextInt(4) == 0 && out.length() + 2 <= length) out.appendCodePoint(0x10000 + random.nextInt(0x10000));
                    else out.append(random.nextInt(5) == 0 ? (char) (0xd800 + random.nextInt(0x800)) : (char) ('a' + random.nextInt(26)));
                }
            }
        }
        return out.substring(0, length);
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
