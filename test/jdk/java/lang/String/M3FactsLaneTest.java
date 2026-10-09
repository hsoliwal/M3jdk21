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
 * @summary The single-answer lanes beside the facts (A24): stripStart, stripEnd, trimStart,
 *          trimEnd, codePointCountValue and contentIsAscii of M3 ranges equal the flat String's
 *          strip/trim offsets, Character.codePointCount and an ASCII scan, before and after the
 *          facts are prepared, and equal the facts' own fields; String.isBlank, codePointCount
 *          over random sub-ranges, the identity results of trim/strip/stripLeading/stripTrailing/
 *          toLowerCase/toUpperCase and the ASCII case mapping agree with the flat results; atoms,
 *          nested tuples and slices of padded owners with whitespace of every kind (space, tab,
 *          line separators, U+2003, U+00A0 which is not whitespace), surrogate pairs and lone
 *          surrogates, all-whitespace and empty texts, 0..20000 units across the window sizes.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3FactsLaneTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3FactsLaneTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3FactsLaneTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 3, 31, 32, 33, 100, 1023, 1024, 1025, 2049, 5000, 20000};
    private static final char[] WHITESPACE = {' ', '\t', '\n', '\r', '\f', '\u000b', '\u001c', ' ', ' ', '　'};
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static Method facts;
    private static Method stripStart;
    private static Method stripEnd;
    private static Method trimStart;
    private static Method trimEnd;
    private static Method codePointCountValue;
    private static Method contentIsAscii;
    private static Method asciiCase;
    private static Method m3CharAt;
    private static Field factsStripStart;
    private static Field factsStripEnd;
    private static Field factsTrimStart;
    private static Field factsTrimEnd;
    private static Field factsCodePointCount;
    private static Field factsAscii;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(0x4c414e4553L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 8; kind++) {
                String text = spell(random, length, kind);
                for (Object storage : shapes(text, random)) {
                    compare(text, storage, random, "cold");
                    facts.invoke(storage);
                    compare(text, storage, random, "warm");
                }
            }
        }
        System.out.println("M3FactsLaneTest checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> factsClass = Class.forName("java.lang.M3StringFacts");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        facts = accessible(m3.getDeclaredMethod("facts"));
        stripStart = accessible(m3.getDeclaredMethod("stripStart"));
        stripEnd = accessible(m3.getDeclaredMethod("stripEnd"));
        trimStart = accessible(m3.getDeclaredMethod("trimStart"));
        trimEnd = accessible(m3.getDeclaredMethod("trimEnd"));
        codePointCountValue = accessible(m3.getDeclaredMethod("codePointCountValue"));
        contentIsAscii = accessible(m3.getDeclaredMethod("contentIsAscii"));
        asciiCase = accessible(m3.getDeclaredMethod("asciiCase", boolean.class));
        m3CharAt = accessible(m3.getDeclaredMethod("charAt", int.class));
        factsStripStart = field(factsClass, "stripStart");
        factsStripEnd = field(factsClass, "stripEnd");
        factsTrimStart = field(factsClass, "trimStart");
        factsTrimEnd = field(factsClass, "trimEnd");
        factsCodePointCount = field(factsClass, "codePointCount");
        factsAscii = field(factsClass, "ascii");
        valueOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
        coderOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("coder"));
        m3Offset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("m3"));
    }

    private static void compare(String text, Object storage, Random random, String phase) throws Exception {
        int length = text.length();
        String label = phase + " " + length + " units";
        int expectedStripStart = length - text.stripLeading().length();
        int expectedStripEnd = text.stripTrailing().length();
        int expectedTrimStart = leadingTrim(text);
        int expectedTrimEnd = trailingTrim(text);
        int expectedCodePoints = Character.codePointCount(text, 0, length);
        boolean expectedAscii = text.chars().allMatch(c -> c <= 0x7f);
        check((int) stripStart.invoke(storage) == expectedStripStart, "stripStart " + label);
        check((int) stripEnd.invoke(storage) == expectedStripEnd, "stripEnd " + label);
        check((int) trimStart.invoke(storage) == expectedTrimStart, "trimStart " + label);
        check((int) trimEnd.invoke(storage) == expectedTrimEnd, "trimEnd " + label);
        check((int) codePointCountValue.invoke(storage) == expectedCodePoints, "codePointCountValue " + label);
        check((boolean) contentIsAscii.invoke(storage) == expectedAscii, "contentIsAscii " + label);
        if (phase.equals("warm")) {
            Object prepared = facts.invoke(storage);
            check(factsStripStart.getInt(prepared) == expectedStripStart && factsStripEnd.getInt(prepared) == expectedStripEnd, "facts strip " + label);
            check(factsTrimStart.getInt(prepared) == expectedTrimStart && factsTrimEnd.getInt(prepared) == expectedTrimEnd, "facts trim " + label);
            check(factsCodePointCount.getInt(prepared) == expectedCodePoints, "facts codePointCount " + label);
            check(factsAscii.getBoolean(prepared) == expectedAscii, "facts ascii " + label);
        }
        String m3 = shaped(text, storage);
        check(m3.isBlank() == text.isBlank(), "isBlank " + label);
        check(m3.codePointCount(0, length) == expectedCodePoints, "codePointCount " + label);
        if (length > 1) {
            int begin = random.nextInt(length);
            int end = begin + random.nextInt(length - begin + 1);
            check(m3.codePointCount(begin, end) == text.codePointCount(begin, end), "codePointCount range " + begin + ".." + end + " " + label);
        }
        if (text.trim() == text) check(m3.trim() == m3, "trim identity " + label);
        if (text.strip() == text) check(m3.strip() == m3, "strip identity " + label);
        if (text.stripLeading() == text) check(m3.stripLeading() == m3, "stripLeading identity " + label);
        if (text.stripTrailing() == text) check(m3.stripTrailing() == m3, "stripTrailing identity " + label);
        if (text.isBlank() && length > 0) {
            check(m3.strip().isEmpty(), "blank strip " + label);
            if (text.trim().isEmpty()) check(m3.trim().isEmpty(), "blank trim " + label);
        }
        if (expectedAscii) {
            if (text.toLowerCase(Locale.ROOT) == text) check(m3.toLowerCase(Locale.ROOT) == m3, "toLowerCase identity " + label);
            if (text.toUpperCase(Locale.ROOT) == text) check(m3.toUpperCase(Locale.ROOT) == m3, "toUpperCase identity " + label);
            Object lower = asciiCase.invoke(storage, false);
            Object upper = asciiCase.invoke(storage, true);
            check(spelled(lower, length).equals(text.toLowerCase(Locale.ROOT)) && spelled(upper, length).equals(text.toUpperCase(Locale.ROOT)), "asciiCase " + label);
        }
    }

    private static String spelled(Object storage, int length) throws Exception {
        StringBuilder out = new StringBuilder(length);
        for (int index = 0; index < length; index++) out.append((char) m3CharAt.invoke(storage, index));
        return out.toString();
    }

    private static int leadingTrim(String text) {
        int index = 0;
        while (index < text.length() && text.charAt(index) <= ' ') index++;
        return index;
    }

    private static int trailingTrim(String text) {
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) <= ' ') end--;
        return end;
    }

    private static Object[] shapes(String text, Random random) throws Exception {
        if (text.isEmpty()) return new Object[] {slice.invoke(atom("padend"), 3, 3), slice.invoke(atom("一pad end"), 4, 4)};
        String padded = (random.nextBoolean() ? "一 pad" : " pad") + text + "end é";
        Object whole = random.nextBoolean() ? atom(padded) : tupleOf(padded, random, 2);
        int begin = padded.length() - 5 - text.length();
        return new Object[] {atom(text), tupleOf(text, random, 1 + random.nextInt(3)), slice.invoke(whole, begin, begin + text.length())};
    }

    private static String spell(Random random, int length, int kind) {
        StringBuilder out = new StringBuilder(length);
        int lead = kind >= 4 && length > 0 ? random.nextInt(Math.min(length, 40) + 1) : 0;
        int trail = kind >= 4 && length > 0 ? random.nextInt(Math.min(length - lead, 40) + 1) : 0;
        for (int i = 0; i < lead; i++) out.append(WHITESPACE[random.nextInt(WHITESPACE.length)]);
        while (out.length() < length - trail) {
            out.append((char) switch (kind & 3) {
                case 0 -> random.nextInt(20) == 0 ? ' ' : 'a' + random.nextInt(26);
                case 1 -> random.nextInt(25) == 0 ? ' ' : random.nextInt(0x100);
                case 2 -> random.nextInt(6) == 0 ? 0xd800 + random.nextInt(0x800) : random.nextInt(10) == 0 ? 0x2003 : 'A' + random.nextInt(26);
                default -> random.nextInt(8) == 0 ? WHITESPACE[random.nextInt(WHITESPACE.length)] : random.nextInt(0x10000);
            });
        }
        for (int i = 0; i < trail && out.length() < length; i++) out.append(WHITESPACE[random.nextInt(WHITESPACE.length)]);
        while (out.length() < length) out.append(' ');
        if (kind == 7 && length > 0) {
            out.setLength(0);
            while (out.length() < length) out.append(WHITESPACE[random.nextInt(WHITESPACE.length)]);
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

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
