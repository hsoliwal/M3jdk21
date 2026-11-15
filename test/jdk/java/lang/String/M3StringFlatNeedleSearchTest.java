/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

/*
 * @test
 * @summary Flat-needle adaptive search preserves exact UTF-16 String contracts over canonical
 *          atoms, tuples and slices without admitting the needle or materializing the source
 * @modules java.base/java.lang:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringFlatNeedleSearchTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringFlatNeedleSearchTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

public class M3StringFlatNeedleSearchTest {
    private static final char[] ALPHABET = {
            0, 'a', 'b', 'c', '\u007f', '\u00ff', '\u0100', '\u4e00',
            '\ud800', '\udc00', '\ud83d', '\ude00', '\uffff'
    };
    private static final Class<?> STORAGE;
    private static final Field STRING_STORAGE;
    private static final Field STRING_VALUE;
    private static final Field OWNER;
    private static final Method INTERN_CHARS;
    private static final Method CONCAT;
    private static final Method SLICE;
    private static final Constructor<String> STRING_CONSTRUCTOR;
    private static final ThreadLocal<Boolean> ADMITTING;
    private static long checks;

    static {
        try {
            STORAGE = Class.forName("java.lang.M3String");
            Class<?> pool = Class.forName("java.lang.M3StringPool");
            STRING_STORAGE = accessible(String.class.getDeclaredField("m3"));
            STRING_VALUE = accessible(String.class.getDeclaredField("value"));
            OWNER = accessible(STORAGE.getDeclaredField("owner"));
            INTERN_CHARS = accessible(
                    pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
            CONCAT = accessible(STORAGE.getDeclaredMethod("concat", STORAGE));
            SLICE = accessible(STORAGE.getDeclaredMethod("slice", int.class, int.class));
            STRING_CONSTRUCTOR = String.class.getDeclaredConstructor(STORAGE);
            STRING_CONSTRUCTOR.setAccessible(true);
            @SuppressWarnings("unchecked")
            ThreadLocal<Boolean> admitting =
                    (ThreadLocal<Boolean>) accessible(STORAGE.getDeclaredField("ADMITTING")).get(null);
            ADMITTING = admitting;
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        boundaryCases();
        denseCandidates();
        tupleSeamsAndRanges();
        randomized();
        exceptions();
        System.out.println("M3_STRING_FLAT_NEEDLE_SEARCH_PASS checks=" + checks);
    }

    private static void boundaryCases() throws Exception {
        char[][] texts = {
                {}, {'a'}, {'a', 'a', 'a'}, {'a', 'b', 'a', 'b', 'a'},
                {'\ud800', 'a', '\udc00', '\ud83d', '\ude00', 0, '\uffff'},
                {'\u0001', '\u0101', '\u0201', '\u0001', '\u0101'}
        };
        char[][] needles = {
                {}, {'a'}, {'a', 'a'}, {'a', 'b', 'a'}, {'b', 'a', 'b', 'a'},
                {'\ud800'}, {'\udc00'}, {'\ud83d', '\ude00'}, {'\ude00', 0},
                {'\u0001', '\u0101'}, {'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x'}
        };
        for (char[] text : texts) {
            for (int shape = 0; shape < 3; shape++) {
                String source = canonical(text, shape);
                for (char[] needle : needles) {
                    exercise(text, source, needle, true);
                }
            }
        }
    }

    private static void denseCandidates() throws Exception {
        // The ninth candidate is the first match and overlaps the eighth failed candidate's
        // compared range. This directly exercises the exact forward/reverse KMP handoff.
        char[] overlapText = filled(16, 'a');
        overlapText[15] = 'b';
        char[] overlapNeedle = filled(8, 'a');
        overlapNeedle[7] = 'b';
        exercise(overlapText, canonical(overlapText, 1), overlapNeedle, true);
        reverse(overlapText);
        reverse(overlapNeedle);
        exercise(overlapText, canonical(overlapText, 2), overlapNeedle, true);

        // Both directions repeatedly verify long prefixes/suffixes before the mismatch.
        // These sources exceed the trigram gate's maximum, so the exact search must run.
        int sourceLength = 40_001;
        for (int needleLength : new int[] {8, 64, 257, 1_024}) {
            char[] forwardText = filled(sourceLength, 'a');
            forwardText[sourceLength - 1] = 'b';
            char[] forwardNeedle = filled(needleLength, 'a');
            forwardNeedle[needleLength - 1] = 'b';
            exercise(forwardText, canonical(forwardText, 1), forwardNeedle, false);

            char[] reverseText = filled(sourceLength, 'a');
            reverseText[0] = 'b';
            char[] reverseNeedle = filled(needleLength, 'a');
            reverseNeedle[0] = 'b';
            exercise(reverseText, canonical(reverseText, 2), reverseNeedle, false);

            // Full alphabet membership is not an exact match proof. The two b units exist
            // separately, but the needle's adjacent b units never occur in the source.
            char[] absentText = filled(sourceLength, 'a');
            absentText[sourceLength / 2] = 'b';
            char[] absentNeedle = filled(needleLength, 'a');
            absentNeedle[needleLength / 2] = 'b';
            absentNeedle[needleLength / 2 + 1] = 'b';
            exercise(absentText, canonical(absentText, 1), absentNeedle, false);
        }

        // Exactly at and beyond the metadata cap: the oversized lane stays an exact scan.
        for (int needleLength : new int[] {8_191, 8_192, 8_193}) {
            char[] text = filled(needleLength + 71, '\u0100');
            text[text.length - 1] = '\ud800';
            char[] needle = filled(needleLength, '\u0100');
            needle[needle.length - 1] = '\ud800';
            exercise(text, canonical(text, 1), needle, false);
            reverse(text);
            reverse(needle);
            exercise(text, canonical(text, 2), needle, false);
        }
    }

    private static void tupleSeamsAndRanges() throws Exception {
        Random random = new Random(0x5345414d534cL);
        char[] text = randomChars(random, 4_097);
        String source = canonical(text, 1);
        check(OWNER.get(STRING_STORAGE.get(source)).getClass().getSimpleName()
                        .equals("M3StringTuple"),
                "tuple fixture must use the tuple owner");
        for (int start : new int[] {0, 1, 16, 17, 30, 31, 62, 63, 64, 126, 127, 128, 2_047}) {
            char[] needle = Arrays.copyOfRange(text, start, start + 129);
            exercise(text, source, needle, false);
            String flatNeedle = flat(needle);
            check(source.indexOf(flatNeedle, start, start + needle.length) == start,
                    "exact seam-spanning range");
            check(source.indexOf(flatNeedle, start, start + needle.length - 1) == -1,
                    "range excludes last unit");
            check(source.indexOf(flatNeedle, start + 1, start + needle.length) == -1,
                    "range excludes first unit");
            check(STRING_STORAGE.get(flatNeedle) == null, "range search must retain flat needle");
        }

        // The source view starts/ends within surrogate pairs. Search sees UTF-16 units,
        // regardless of whether the same units form valid pairs in its underlying owner.
        char[] splitSurrogates = {'\ude00', 'a', '\ud800', 'b', '\udc00', '\ud83d'};
        exercise(splitSurrogates, canonical(splitSurrogates, 2),
                new char[] {'\ud800', 'b', '\udc00'}, true);
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d33464c41544b4dL);
        for (int trial = 0; trial < 1_200; trial++) {
            char[] text = randomChars(random, random.nextInt(257));
            char[] needle;
            if (text.length > 0 && random.nextBoolean()) {
                int start = random.nextInt(text.length);
                int end = start + random.nextInt(text.length - start + 1);
                needle = Arrays.copyOfRange(text, start, end);
                if (needle.length > 0 && random.nextBoolean()) {
                    needle[random.nextInt(needle.length)] ^= 1;
                }
            } else {
                needle = randomChars(random, random.nextInt(49));
            }
            String source = canonical(text, trial % 3);
            exercise(text, source, needle, trial % 19 == 0);
            int begin = random.nextInt(text.length + 1);
            int end = begin + random.nextInt(text.length - begin + 1);
            String flatNeedle = flat(needle);
            int expected = forward(text, needle, begin, end);
            check(source.indexOf(flatNeedle, begin, end) == expected,
                    "random bounded range trial " + trial);
            check(STRING_STORAGE.get(flatNeedle) == null, "random range retains flat needle");
        }
    }

    private static void exercise(
            char[] text, String source, char[] units, boolean canonicalPeer) throws Exception {
        Object sourceStorage = STRING_STORAGE.get(source);
        Object sourceValue = STRING_VALUE.get(source);
        check(sourceStorage != null, "source must have canonical storage");
        String needle = flat(units);
        int length = text.length;
        int[] froms = {Integer.MIN_VALUE, -1, 0, 1, length / 2,
                Math.max(0, length - units.length), length, length + 1, Integer.MAX_VALUE};
        for (int from : froms) {
            int expected = forward(text, units, from, length);
            check(source.indexOf(needle, from) == expected, "forward search from " + from);
            expected = backward(text, units, from);
            check(source.lastIndexOf(needle, from) == expected, "reverse search from " + from);
        }
        int first = forward(text, units, 0, length);
        int last = backward(text, units, length);
        for (int repeat = 0; repeat < 3; repeat++) {
            check(source.indexOf(needle) == first, "repeat forward " + repeat);
            check(source.lastIndexOf(needle) == last, "repeat reverse " + repeat);
            check(source.contains(needle) == (first >= 0), "contains " + repeat);
        }
        check(source.indexOf(needle, 0, length) == first, "full bounded range");
        check(STRING_STORAGE.get(needle) == null, "search must not admit a flat needle");
        check(STRING_STORAGE.get(source) == sourceStorage, "source owner must remain unchanged");
        check(STRING_VALUE.get(source) == sourceValue, "search must not replace source shadow");
        check(((byte[]) sourceValue).length == 0, "canonical source must retain only its empty sentinel");
        if (canonicalPeer) {
            String peer = canonical(units, 1);
            for (int from : froms) {
                check(source.indexOf(peer, from) == forward(text, units, from, length),
                        "canonical peer forward");
                check(source.lastIndexOf(peer, from) == backward(text, units, from),
                        "canonical peer reverse");
            }
        }
    }

    private static void exceptions() throws Exception {
        String source = canonical(new char[] {'a', 'b', 'c'}, 1);
        String needle = flat(new char[] {'a', 'b'});
        expect(NullPointerException.class, () -> source.indexOf((String) null));
        expect(NullPointerException.class, () -> source.lastIndexOf((String) null));
        expect(NullPointerException.class, () -> source.indexOf(null, 0, 3));
        expect(StringIndexOutOfBoundsException.class, () -> source.indexOf(needle, -1, 3));
        expect(StringIndexOutOfBoundsException.class, () -> source.indexOf(needle, 0, 4));
        expect(StringIndexOutOfBoundsException.class, () -> source.indexOf(needle, 2, 1));
        expect(StringIndexOutOfBoundsException.class,
                () -> source.indexOf(needle, Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    /** Independent oracle over test-owned code units; it never calls a String search method. */
    private static int forward(char[] text, char[] needle, int from, int end) {
        int start = Math.min(Math.max(from, 0), text.length);
        if (needle.length == 0) return Math.min(start, end);
        for (int candidate = start; candidate <= end - needle.length; candidate++) {
            int matched = 0;
            while (matched < needle.length && text[candidate + matched] == needle[matched]) matched++;
            if (matched == needle.length) return candidate;
        }
        return -1;
    }

    private static int backward(char[] text, char[] needle, int from) {
        int start = Math.min(from, text.length - needle.length);
        if (start < 0) return -1;
        if (needle.length == 0) return start;
        for (int candidate = start; candidate >= 0; candidate--) {
            int matched = 0;
            while (matched < needle.length && text[candidate + matched] == needle[matched]) matched++;
            if (matched == needle.length) return candidate;
        }
        return -1;
    }

    /** Disable only this test thread's optional ingress while constructing the flat oracle/needle. */
    private static String flat(char[] units) throws Exception {
        Boolean previous = ADMITTING.get();
        ADMITTING.set(Boolean.TRUE);
        String value;
        try {
            value = new String(units);
        } finally {
            if (previous == null) ADMITTING.remove();
            else ADMITTING.set(previous);
        }
        check(STRING_STORAGE.get(value) == null, "flat fixture unexpectedly admitted");
        return value;
    }

    private static String canonical(char[] units, int shape) throws Exception {
        Object storage;
        if (shape == 0) {
            storage = atom(units, 0, units.length);
        } else if (shape == 1) {
            storage = tuple(units, 0, units.length);
        } else {
            char[] padded = new char[units.length + 8];
            Arrays.fill(padded, '\ud800');
            System.arraycopy(units, 0, padded, 4, units.length);
            storage = SLICE.invoke(tuple(padded, 0, padded.length), 4, 4 + units.length);
        }
        check(storage != null, "canonical pool declined test fixture");
        return STRING_CONSTRUCTOR.newInstance(storage);
    }

    private static Object tuple(char[] units, int begin, int end) throws Exception {
        if (end - begin <= 31) return atom(units, begin, end - begin);
        int middle = begin + (end - begin) / 2;
        return CONCAT.invoke(tuple(units, begin, middle), tuple(units, middle, end));
    }

    private static Object atom(char[] units, int begin, int length) throws Exception {
        Object storage = INTERN_CHARS.invoke(null, units, begin, length);
        check(storage != null, "canonical atom missing");
        return storage;
    }

    private static char[] filled(int length, char unit) {
        char[] result = new char[length];
        Arrays.fill(result, unit);
        return result;
    }

    private static char[] randomChars(Random random, int length) {
        char[] result = new char[length];
        for (int index = 0; index < length; index++) {
            result[index] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return result;
    }

    private static void reverse(char[] units) {
        for (int left = 0, right = units.length - 1; left < right; left++, right--) {
            char swap = units[left];
            units[left] = units[right];
            units[right] = swap;
        }
    }

    private static Field accessible(Field field) {
        field.setAccessible(true);
        return field;
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    private static void expect(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getName() + ", got " + failure);
            return;
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
