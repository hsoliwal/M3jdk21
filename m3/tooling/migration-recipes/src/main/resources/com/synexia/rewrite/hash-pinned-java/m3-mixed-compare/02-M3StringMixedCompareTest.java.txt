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
 * @summary Mixed and M3-on-M3 comparisons over bulk-read windows agree exactly with the stock
 *          flat results on the same spellings (equals, compareTo, CASE_INSENSITIVE_ORDER,
 *          compareToIgnoreCase, equalsIgnoreCase, regionMatches), across window boundaries,
 *          surrogate pairs, both coders and the short-length cutoff; prepared range facts are
 *          found without allocating a range (A10 MIXED-COMPARE-WINDOWS).
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3StringMixedCompareTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringMixedCompareTest
 */

import java.lang.reflect.Method;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3StringMixedCompareTest {

    private static final String[] ATOMS = {
        "a", "bc", "XYZ", "hello", "HELLO", "Hello", " ", "\t", "é", "É", "ÿ", "Ÿ", "µ", "Μ", "Ā",
        "🙂", "\ud83d", "\ude42", "\u0000", "abcabc", "ABCABC", "needle", "k", "K", "K", "i", "İ",
        "ı", "s", "ſ", "ß", "ẞ", "The quick brown fox", "0123456789", "ab", "AB", "𐐀", "𐐨",
        "𐐀", "𐐨", "x\ud801", "\udc00y", "ǅ", "ǆ", "Ǆ", "σ", "ς", "Σ"
    };
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static Method facts;
    private static Method factsIfPrepared;
    private static Method factsIfPreparedRange;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        parity();
        windowBoundaries();
        cutoffBoundary();
        rangeFacts();
        System.out.println("M3StringMixedCompareTest checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        facts = accessible(m3.getDeclaredMethod("facts"));
        factsIfPrepared = accessible(m3.getDeclaredMethod("factsIfPrepared"));
        factsIfPreparedRange = accessible(m3.getDeclaredMethod("factsIfPrepared", int.class, int.class));
        valueOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
        coderOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("coder"));
        m3Offset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("m3"));
    }

    /** Random atoms, tuples and case variants: every consumer agrees with the stock flat result. */
    private static void parity() throws Exception {
        Random random = new Random(0x4d3343504d505231L);
        for (int round = 0; round < 6_000; round++) {
            String left = spell(random, 1 + random.nextInt(6));
            String right = switch (round % 4) {
                case 0 -> new String(left.toCharArray());
                case 1 -> mixCase(left, random);
                case 2 -> spell(random, 1 + random.nextInt(6));
                default -> left.isEmpty() ? "z" : left.substring(0, random.nextInt(left.length()))
                        + (char) (left.charAt(random.nextInt(left.length())) ^ random.nextInt(3));
            };
            String leftM3 = round % 5 == 0 ? tuple(left, random) : m3(left);
            String rightM3 = round % 7 == 0 ? tuple(right, random) : m3(right);
            if (round % 3 == 0) right.hashCode();
            compareAll(left, right, leftM3, right, "m3 left");
            compareAll(left, right, left, rightM3, "m3 right");
            compareAll(left, right, leftM3, rightM3, "m3 both");
            if (!left.isEmpty() && !right.isEmpty()) {
                int len = 1 + random.nextInt(Math.min(left.length(), right.length()));
                int toffset = random.nextInt(left.length() - len + 1);
                int ooffset = random.nextInt(right.length() - len + 1);
                boolean expected = left.regionMatches(true, toffset, right, ooffset, len);
                boolean expectedExact = left.regionMatches(toffset, right, ooffset, len);
                check(leftM3.regionMatches(true, toffset, right, ooffset, len) == expected, "regionMatches(true) m3 left " + show(left) + " " + show(right));
                check(left.regionMatches(true, toffset, rightM3, ooffset, len) == expected, "regionMatches(true) m3 right " + show(left) + " " + show(right));
                check(leftM3.regionMatches(true, toffset, rightM3, ooffset, len) == expected, "regionMatches(true) m3 both " + show(left) + " " + show(right));
                check(leftM3.regionMatches(toffset, right, ooffset, len) == expectedExact, "regionMatches m3 left");
            }
        }
    }

    private static void compareAll(String left, String right, String a, String b, String label) {
        String where = label + " " + show(left) + " / " + show(right);
        check(a.equals(b) == left.equals(right), "equals " + where);
        check(b.equals(a) == right.equals(left), "equals reversed " + where);
        check(a.compareTo(b) == left.compareTo(right), "compareTo " + where);
        check(b.compareTo(a) == right.compareTo(left), "compareTo reversed " + where);
        check(a.compareToIgnoreCase(b) == left.compareToIgnoreCase(right), "compareToIgnoreCase " + where);
        check(b.compareToIgnoreCase(a) == right.compareToIgnoreCase(left), "compareToIgnoreCase reversed " + where);
        check(String.CASE_INSENSITIVE_ORDER.compare(a, b) == String.CASE_INSENSITIVE_ORDER.compare(left, right), "CASE_INSENSITIVE_ORDER " + where);
        check(a.equalsIgnoreCase(b) == left.equalsIgnoreCase(right), "equalsIgnoreCase " + where);
        check(b.equalsIgnoreCase(a) == right.equalsIgnoreCase(left), "equalsIgnoreCase reversed " + where);
        check(a.contentEquals(b) == left.contentEquals(right), "contentEquals " + where);
    }

    /** Lengths around the window size with surrogate pairs and lone surrogates on the boundaries. */
    private static void windowBoundaries() throws Exception {
        Random random = new Random(0x57494e444f575f42L);
        int[] lengths = {255, 256, 257, 300, 511, 512, 513, 1023, 1025};
        for (int length : lengths) {
            for (int variant = 0; variant < 6; variant++) {
                char[] base = new char[length];
                for (int index = 0; index < length; index++) {
                    base[index] = (char) (variant < 3 ? 'a' + random.nextInt(26) : 0x100 + random.nextInt(0x300));
                }
                for (int boundary : new int[] {254, 255, 256, 511, 512}) {
                    if (boundary + 1 < length && variant % 3 != 2) {
                        base[boundary] = '\ud801';
                        base[boundary + 1] = variant % 3 == 0 ? '\udc00' : 'q';
                    }
                }
                String left = new String(base);
                int cut = Math.min(256, length - 2);
                String[] rights = {
                    new String(base),
                    mixCase(left, random),
                    left.substring(0, length - 1) + (char) (base[length - 1] ^ 1),
                    flipAt(base, 255), flipAt(base, 256), flipAt(base, 257), flipAt(base, 512),
                    left.substring(0, cut), left + "z", left.substring(0, cut) + "\udc00" + left.substring(cut + 1)
                };
                for (String right : rights) {
                    compareAll(left, right, m3(left), right, "boundary m3 left " + length);
                    compareAll(left, right, left, m3(right), "boundary m3 right " + length);
                    compareAll(left, right, tuple(left, random), m3(right), "boundary tuple " + length);
                    int len = Math.min(left.length(), right.length()) - 1;
                    if (len > 0) {
                        check(m3(left).regionMatches(true, 1, right, 1, len) == left.regionMatches(true, 1, right, 1, len), "boundary regionMatches(true) " + length);
                    }
                }
            }
        }
    }

    /** Every length through the short cutoff, differing at every position, both orders. */
    private static void cutoffBoundary() throws Exception {
        for (int length = 0; length <= 40; length++) {
            char[] base = new char[length];
            for (int index = 0; index < length; index++) base[index] = (char) ('A' + (index % 26));
            String left = new String(base);
            String leftM3 = m3(left);
            compareAll(left, new String(base), leftM3, new String(base), "cutoff equal " + length);
            for (int position = 0; position < length; position++) {
                char[] other = base.clone();
                other[position] = (char) (other[position] + 1 + (position % 2) * 31);
                String right = new String(other);
                compareAll(left, right, leftM3, right, "cutoff diff " + length + "@" + position);
                compareAll(left, right, left, m3(right), "cutoff diff right " + length + "@" + position);
                String lower = right.toLowerCase(java.util.Locale.ROOT);
                compareAll(left, lower, leftM3, lower, "cutoff lower " + length + "@" + position);
            }
        }
    }

    /** factsIfPrepared(begin, end) is the recorded range fact or the owner's facts, never prepares. */
    private static void rangeFacts() throws Exception {
        Random random = new Random(0x52414e47455f4654L);
        for (int round = 0; round < 400; round++) {
            String spelling = spell(random, 2 + random.nextInt(5));
            if (spelling.length() < 4) continue;
            Object storage = round % 2 == 0 ? atom(spelling) : tupleStorage(spelling, random);
            int begin = random.nextInt(spelling.length() - 1);
            int end = begin + 1 + random.nextInt(spelling.length() - begin);
            boolean whole = begin == 0 && end == spelling.length();
            Object before = factsIfPreparedRange.invoke(storage, begin, end);
            Object sliced = slice.invoke(storage, begin, end);
            Object preparedSlice = factsIfPrepared.invoke(sliced);
            check(before == preparedSlice, "unprepared lookup agrees with the slice " + show(spelling));
            Object prepared = facts.invoke(sliced);
            Object after = factsIfPreparedRange.invoke(storage, begin, end);
            check(after == prepared || (!whole && after == null && factsIfPrepared.invoke(sliced) == null),
                    "prepared lookup returns the recorded fact " + show(spelling) + " [" + begin + "," + end + ")");
            check(factsIfPreparedRange.invoke(storage, begin, begin) == null, "empty range has no facts");
            check(factsIfPreparedRange.invoke(storage, 0, spelling.length()) == factsIfPrepared.invoke(storage), "whole range is the owner's facts");
        }
    }

    private static String spell(Random random, int parts) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < parts; index++) out.append(ATOMS[random.nextInt(ATOMS.length)]);
        return out.toString();
    }

    private static String mixCase(String value, Random random) {
        StringBuilder out = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            out.append(random.nextBoolean() ? Character.toUpperCase(unit) : Character.toLowerCase(unit));
        }
        return out.toString();
    }

    private static String flipAt(char[] base, int position) {
        char[] other = base.clone();
        if (position < other.length) other[position] = (char) (other[position] ^ 0x20);
        return new String(other);
    }

    private static Object atom(String spelling) throws Exception {
        char[] chars = spelling.toCharArray();
        return internChars.invoke(null, chars, 0, chars.length);
    }

    private static Object tupleStorage(String spelling, Random random) throws Exception {
        if (spelling.length() < 2) return atom(spelling);
        int cut = 1 + random.nextInt(spelling.length() - 1);
        return concat.invoke(atom(spelling.substring(0, cut)), atom(spelling.substring(cut)));
    }

    /** A String carrying M3 storage in the JNI compatibility-shadow shape, flag independent. */
    private static String m3(String spelling) throws Exception {
        return shaped(spelling, atom(spelling));
    }

    private static String tuple(String spelling, Random random) throws Exception {
        return shaped(spelling, tupleStorage(spelling, random));
    }

    private static String shaped(String spelling, Object storage) throws Exception {
        String flat = new String(spelling.toCharArray());
        String out = (String) UNSAFE.allocateInstance(String.class);
        UNSAFE.putReference(out, valueOffset, UNSAFE.getReference(flat, valueOffset));
        UNSAFE.putByte(out, coderOffset, UNSAFE.getByte(flat, coderOffset));
        UNSAFE.putReference(out, m3Offset, storage);
        check(out.length() == spelling.length(), "shaped length " + show(spelling));
        return out;
    }

    private static String show(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            out.append(unit >= 0x20 && unit <= 0x7e ? String.valueOf(unit) : String.format("\\u%04x", (int) unit));
        }
        return out.append('"').toString();
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
