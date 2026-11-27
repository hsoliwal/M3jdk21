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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jdk.internal.misc.Unsafe;

public class M3FlatNeedleGateTest {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int[] LENGTHS = {0, 1, 2, 3, 8, 255, 256, 257, 1000, 1024, 2047, 2048, 2049, 4096, 20000, 32768, 32769, 40000};
    private static final int[] NEEDLE_LENGTHS = {3, 4, 5, 8, 17, 40};
    private static final long EXPECTED_SEARCH_CHECKS = 172_125L;
    private static long checks;
    private static long fixtureChecks;
    private static ThreadLocal<Boolean> admitting;
    private static int contextLength;
    private static int contextKind;
    private static int contextShape;
    private static int contextNeedle;
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static long valueOffset;
    private static long coderOffset;
    private static long m3Offset;

    public static void main(String[] args) throws Exception {
        bind();
        flatFixtureStates();
        Random random = new Random(0x4e4545444c45L);
        for (int length : LENGTHS) {
            for (int kind = 0; kind < 5; kind++) {
                String text = flat(spell(random, length, kind).toCharArray());
                List<String> needles = needles(text, random, kind);
                // Fixture representation must not depend on optional VM admission.
                for (int n = 0; n < needles.size(); n++) {
                    needles.set(n, flat(needles.get(n).toCharArray()));
                }
                Object oracleValue = UNSAFE.getReference(text, valueOffset);
                Object[] shapes = shapes(text, random);
                for (int s = 0; s < shapes.length; s++) {
                    String m3 = shaped(text, shapes[s]);
                    Object sourceValue = UNSAFE.getReference(m3, valueOffset);
                    for (int n = 0; n < needles.size(); n++) {
                        contextLength = length;
                        contextKind = kind;
                        contextShape = s;
                        contextNeedle = n;
                        String needle = needles.get(n);
                        Object needleValue = UNSAFE.getReference(needle, valueOffset);
                        int from = length == 0 ? 0 : random.nextInt(length + 1);
                        for (int repeat = 0; repeat < 3; repeat++) {
                            search(text, m3, needle, from, repeat);
                        }
                        fixture(UNSAFE.getReference(text, m3Offset) == null,
                                "oracle must remain flat");
                        fixture(UNSAFE.getReference(needle, m3Offset) == null,
                                "needle must remain flat");
                        fixture(UNSAFE.getReference(m3, m3Offset) == shapes[s],
                                "canonical source storage must remain unchanged");
                        fixture(UNSAFE.getReference(m3, valueOffset) == sourceValue,
                                "canonical source shadow must remain unchanged");
                        fixture(UNSAFE.getReference(text, valueOffset) == oracleValue,
                                "flat oracle payload must remain unchanged");
                        fixture(UNSAFE.getReference(needle, valueOffset) == needleValue,
                                "flat needle payload must remain unchanged");
                    }
                }
            }
        }
        fixture(checks == EXPECTED_SEARCH_CHECKS, "search corpus count changed");
        System.out.println("M3FlatNeedleGateTest checks=" + checks);
        System.out.println("M3FlatNeedleGateTest fixtureChecks=" + fixtureChecks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Field admission = m3.getDeclaredField("ADMITTING");
        admission.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Boolean> threadAdmission =
                (ThreadLocal<Boolean>) admission.get(null);
        admitting = threadAdmission;
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        slice = accessible(m3.getDeclaredMethod("slice", int.class, int.class));
        valueOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("value"));
        coderOffset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("coder"));
        m3Offset = UNSAFE.objectFieldOffset(String.class.getDeclaredField("m3"));
    }

    private static void search(String flat, String m3, String needle, int from, int repeat) {
        int needleLength = needle.length();
        int expected = flat.indexOf(needle);
        int actual = m3.indexOf(needle);
        check(actual, expected, "indexOf", from, repeat, needleLength);
        expected = flat.indexOf(needle, from);
        actual = m3.indexOf(needle, from);
        check(actual, expected, "indexOf from", from, repeat, needleLength);
        expected = flat.lastIndexOf(needle);
        actual = m3.lastIndexOf(needle);
        check(actual, expected, "lastIndexOf", from, repeat, needleLength);
        expected = flat.lastIndexOf(needle, from);
        actual = m3.lastIndexOf(needle, from);
        check(actual, expected, "lastIndexOf from", from, repeat, needleLength);
        // Preserve the original contains call order: canonical source before flat oracle.
        actual = m3.contains(needle) ? 1 : 0;
        expected = flat.contains(needle) ? 1 : 0;
        check(actual, expected, "contains", from, repeat, needleLength);
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

    /**
     * Construct a physical flat fixture using the existing test-thread admission guard.
     * The flat String lane may still share lower-level matchers with canonical search.
     */
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
        fixture(UNSAFE.getReference(value, m3Offset) == null,
                "flat fixture unexpectedly admitted");
        return value;
    }

    /** Exercise restoration without consuming any of the deterministic corpus's random draws. */
    private static void flatFixtureStates() {
        Boolean previous = admitting.get();
        char[] units = {'A', 0, '\u00ff', '\u0100', '\ud800', '\udc00', '\uffff'};
        try {
            admitting.remove();
            String unset = flat(units);
            fixture(admitting.get() == null, "unset admission state must be restored");
            exceptionalFlatState();
            admitting.set(Boolean.FALSE);
            String falseState = flat(units);
            fixture(admitting.get() == Boolean.FALSE, "false admission state must be restored");
            exceptionalFlatState();
            admitting.set(Boolean.TRUE);
            String trueState = flat(units);
            fixture(admitting.get() == Boolean.TRUE, "true admission state must be restored");
            exceptionalFlatState();
            for (int index = 0; index < units.length; index++) {
                fixture(unset.charAt(index) == units[index], "unset fixture UTF-16 unit");
                fixture(falseState.charAt(index) == units[index], "false fixture UTF-16 unit");
                fixture(trueState.charAt(index) == units[index], "true fixture UTF-16 unit");
            }
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
    }

    /** The constructor's failure must restore the same thread-local state as success. */
    private static void exceptionalFlatState() {
        Boolean previous = admitting.get();
        try {
            flat(null);
            throw new AssertionError("null flat fixture must fail");
        } catch (NullPointerException expected) {
            // The String(char[]) constructor owns the validation.
        }
        fixture(admitting.get() == previous, "exceptional admission state must be restored");
    }

    /** Fixture guards are separate from the historical search-comparison count. */
    private static void fixture(boolean ok, String message) {
        fixtureChecks++;
        if (!ok) throw new AssertionError(message);
    }

    private static String shaped(String flat, Object storage) throws Exception {
        fixture(storage != null, "canonical pool declined source fixture");
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

    /** Format diagnostics only on failure, outside successful search/admission work. */
    private static void check(int actual, int expected, String operation,
            int from, int repeat, int needleLength) {
        checks++;
        if (actual != expected) {
            throw new AssertionError("length " + contextLength + " kind " + contextKind
                    + " shape " + contextShape + " needle " + contextNeedle
                    + " repeat " + repeat + " needle " + needleLength + " units "
                    + operation + " from " + from + " " + actual + " vs " + expected);
        }
    }
}
