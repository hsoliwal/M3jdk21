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
 * @summary lastIndexOf of an M3 needle over a flat String or StringBuilder (A43): the stock
 *          reverse search's shape with a hand-off to the reverse KMP agrees with the flat needle
 *          for needles of 1..4097 units (across the 16-unit lane threshold) shaped as atoms,
 *          tuples and slices, present at the start, the end and the middle, absent, absent with
 *          the text's commonest unit at their end and periodic over periodic texts (the hand-off
 *          cases), over texts of letters, the full Latin-1 range, CJK-mixed, periodic, single-
 *          unit and surrogate alphabets of 0..20000 units, with every from-index shape.
 * @modules java.base/java.lang:+open
 *          java.base/jdk.internal.misc
 * @run main M3ReverseNeedleTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3ReverseNeedleTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3ReverseNeedleTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 15, 16, 17, 100, 255, 256, 1000, 2048, 4095, 4096, 4097, 16384, 20000};
    private static final int[] NEEDLE_LENGTHS = {1, 2, 15, 16, 17, 31, 32, 64, 100, 257, 1024, 4097};
    private static long checks;
    private static final long EXPECTED_SEARCH_CHECKS = 428_544L;
    private static long fixtureChecks;
    private static ThreadLocal<Boolean> admitting;
    private static int contextLength;
    private static int contextKind;
    private static int contextNeedle;
    private static int contextShape;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        flatFixtureStates();
        Random random = new Random(0x5245564552534545L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 6; kind++) {
                String text = flatSpell(random, length, kind);
                StringBuilder builder = new StringBuilder(text);
                List<String> needles = flatNeedles(text, random, kind);
                for (int n = 0; n < needles.size(); n++) {
                    String needle = flat(needles.get(n).toCharArray());
                    Object needleValue = UNSAFE.getReference(needle, valueOffset);
                    Object textValue = UNSAFE.getReference(text, valueOffset);
                    Object[] shapes = flatShapes(needle, random);
                    Oracle oracle = new Oracle(text, builder, needle);
                    for (int s = 0; s < shapes.length; s++) {
                        contextLength = length;
                        contextKind = kind;
                        contextNeedle = n;
                        contextShape = s;
                        String canonical = shaped(needle, shapes[s]);
                        search(text, builder, needle, canonical, random, oracle);
                        fixture(UNSAFE.getReference(text, m3Offset) == null, "haystack must stay flat");
                        fixture(UNSAFE.getReference(needle, m3Offset) == null, "oracle needle must stay flat");
                        fixture(UNSAFE.getReference(canonical, m3Offset) == shapes[s], "canonical needle identity changed");
                        fixture(UNSAFE.getReference(text, valueOffset) == textValue, "haystack payload changed");
                        fixture(UNSAFE.getReference(needle, valueOffset) == needleValue, "oracle needle payload changed");
                        fixture(UNSAFE.getReference(canonical, valueOffset) == needleValue, "canonical shadow changed");
                    }
                }
            }
        }
        fixture(checks == EXPECTED_SEARCH_CHECKS, "search corpus count changed");
        System.out.println("M3ReverseNeedleTest checks=" + checks + " fixtureChecks=" + fixtureChecks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Field admission = m3.getDeclaredField("ADMITTING");
        admission.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Boolean> threadAdmission = (ThreadLocal<Boolean>) admission.get(null);
        admitting = threadAdmission;
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        valueOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
        coderOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("coder"));
        m3Offset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("m3"));
    }

    private static void search(String text, StringBuilder builder, String needle, String m3, Random random, Oracle oracle) {
        int length = text.length();
        int[] froms = {length, 0, -5, length + 5, length - needle.length(), length == 0 ? 0 : random.nextInt(length + 1), length / 2};
        int expected = oracle.fullString;
        int actual = text.lastIndexOf(m3);
        check(actual, expected, "lastIndexOf", length, needle.length());
        check(builder.lastIndexOf(m3), oracle.fullBuilder,
                "StringBuilder.lastIndexOf", length, needle.length());
        for (int from : froms) {
            int slot = oracle.slot(from);
            expected = oracle.stringResults[slot];
            actual = text.lastIndexOf(m3, from);
            check(actual, expected, "lastIndexOf from", from, needle.length());
            check(builder.lastIndexOf(m3, from), oracle.builderResults[slot],
                    "StringBuilder.lastIndexOf from", from, needle.length());
        }
    }

    /** Flat String and StringBuilder reference results are independent of the needle shape.
     * Preserve both no-arg calls and every distinct explicit from-index, including random ones.
     * The candidate calls still run once for every original comparison. */
    private static final class Oracle {
        final String text;
        final StringBuilder builder;
        final String needle;
        final int fullString;
        final int fullBuilder;
        final int[] froms = new int[21];
        final int[] stringResults = new int[21];
        final int[] builderResults = new int[21];
        int size;

        Oracle(String text, StringBuilder builder, String needle) {
            this.text = text;
            this.builder = builder;
            this.needle = needle;
            fullString = text.lastIndexOf(needle);
            fullBuilder = builder.lastIndexOf(needle);
        }

        int slot(int from) {
            for (int index = 0; index < size; index++) {
                if (froms[index] == from) return index;
            }
            int index = size++;
            froms[index] = from;
            stringResults[index] = text.lastIndexOf(needle, from);
            builderResults[index] = builder.lastIndexOf(needle, from);
            return index;
        }
    }

    // Admission is disabled only while constructing reference data. The explicit
    // pool/concat/slice calls still create the intended canonical descriptors.
    private static String flatSpell(Random random, int length, int kind) {
        Boolean previous = admitting.get();
        admitting.set(Boolean.TRUE);
        try {
            return flat(spell(random, length, kind).toCharArray());
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
    }

    private static List<String> flatNeedles(String text, Random random, int kind) {
        Boolean previous = admitting.get();
        admitting.set(Boolean.TRUE);
        try {
            return needles(text, random, kind);
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
    }

    private static Object[] flatShapes(String text, Random random) throws Exception {
        Boolean previous = admitting.get();
        admitting.set(Boolean.TRUE);
        try {
            return shapes(text, random);
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
    }

    private static List<String> needles(String text, Random random, int kind) {
        List<String> out = new ArrayList<>();
        int length = text.length();
        char common = commonest(text);
        for (int needleLength : NEEDLE_LENGTHS) {
            out.add(spell(random, needleLength, kind));
            if (needleLength > 1) out.add(spell(random, needleLength - 1, kind) + '');
            if (needleLength > 1) out.add("" + String.valueOf(common).repeat(needleLength - 1));
            if (needleLength > 1) out.add(String.valueOf(common).repeat(needleLength - 1) + '');
            if (needleLength <= length) {
                int at = random.nextInt(length - needleLength + 1);
                out.add(text.substring(at, at + needleLength));
                out.add(text.substring(0, needleLength));
                out.add(text.substring(length - needleLength));
                String present = text.substring(at, at + needleLength);
                out.add(mutate(present, 0));
                out.add(mutate(present, needleLength - 1));
                out.add(mutate(present, needleLength / 2));
            }
        }
        if (length >= 2) {
            out.add(text.substring(0, 2).repeat(9) + '');
            out.add("𐐀" + text.substring(1, Math.min(length, 20)));
            out.add(text.substring(length - Math.min(length, 20)) + "\ud801");
        }
        return out;
    }

    private static char commonest(String text) {
        int[] counts = new int[0x10000];
        char best = 'a';
        for (int i = 0; i < text.length(); i++) {
            char unit = text.charAt(i);
            if (++counts[unit] > counts[best]) best = unit;
        }
        return best;
    }

    private static String mutate(String needle, int at) {
        char unit = needle.charAt(at);
        return needle.substring(0, at) + (char) (unit ^ 1) + needle.substring(at + 1);
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
                case 3 -> out.append(random.nextInt(50) == 0 ? 'c' : (out.length() & 1) == 0 ? 'a' : 'b');
                case 4 -> out.append(random.nextInt(200) == 0 ? 'b' : 'a');
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

    /** Build a physically flat oracle and restore admission state even on failure. */
    private static String flat(char[] units) {
        Boolean previous = admitting.get();
        admitting.set(Boolean.TRUE);
        String value;
        try {
            value = new String(units);
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
        fixture(UNSAFE.getReference(value, m3Offset) == null, "flat fixture unexpectedly admitted");
        return value;
    }

    /** Do not consume any random draws while checking the flat fixture. */
    private static void flatFixtureStates() {
        Boolean previous = admitting.get();
        char[] units = {'A', 0, '\u00ff', '\u0100', '\ud800', '\udc00', '\uffff'};
        try {
            for (Boolean state : new Boolean[] {null, Boolean.FALSE, Boolean.TRUE}) {
                if (state == null) admitting.remove();
                else admitting.set(state);
                String value = flat(units);
                fixture(admitting.get() == state, "successful admission state restoration");
                for (int index = 0; index < units.length; index++) {
                    fixture(value.charAt(index) == units[index], "flat fixture UTF-16 unit");
                }
                try {
                    flat(null);
                    throw new AssertionError("null flat fixture must fail");
                } catch (NullPointerException expected) {
                    // The String constructor owns the validation.
                }
                fixture(admitting.get() == state, "exceptional admission state restoration");
            }
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
    }

    private static String shaped(String flat, Object storage) throws Exception {
        fixture(storage != null, "canonical pool declined needle fixture");
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

    private static void fixture(boolean ok, String message) {
        fixtureChecks++;
        if (!ok) throw new AssertionError(message);
    }

    /** Render diagnostics only on mismatch, never repeat an expensive search. */
    private static void check(int actual, int expected, String operation, int from, int needleLength) {
        checks++;
        if (actual != expected) {
            throw new AssertionError("length " + contextLength + " kind " + contextKind
                    + " needle " + contextNeedle + " (" + needleLength + " units) shape " + contextShape
                    + " " + operation + " from " + from + " " + actual + " vs " + expected);
        }
    }
}
