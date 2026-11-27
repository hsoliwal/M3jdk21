/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only
 */

/*
 * @test
 * @summary Verify the physically flat needle read lane of the shared two-way matcher
 * @modules java.base/java.lang:+open
 * @run main M3TwoWayRawNeedleTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3TwoWayRawNeedleTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3TwoWayRawNeedleTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.Random;

/** Independent UTF-16 oracle for the raw flat and canonical needle paths. */
public class M3TwoWayRawNeedleTest {
    private static final long SEED = 0x5241574e4545444cL;
    private static final int RANDOM_CASES = 200;
    private static long checks;
    private static ThreadLocal<Boolean> admitting;
    private static Field valueField;
    private static Field coderField;
    private static Field m3Field;
    private static Method internChars;
    private static Method twoWay;
    private static Method canonicalTwoWay;
    private static Constructor<String> canonicalString;

    public static void main(String[] args) throws Exception {
        bind();
        Random random = new Random(SEED);
        char[] alphabet = {'a', 'b', 'c', 0, 255, 256, '\ud800', '\udc00', '\uffff'};
        for (int trial = 0; trial < RANDOM_CASES; trial++) {
            char[] hay = new char[1 + random.nextInt(30)];
            for (int i = 0; i < hay.length; i++) {
                hay[i] = alphabet[random.nextInt(trial % 2 == 0 ? 5 : alphabet.length)];
            }
            char[] needle = new char[1 + random.nextInt(hay.length)];
            if (trial % 3 == 0) {
                System.arraycopy(hay, random.nextInt(hay.length - needle.length + 1),
                        needle, 0, needle.length);
            } else {
                for (int i = 0; i < needle.length; i++) {
                    needle[i] = alphabet[random.nextInt(alphabet.length)];
                }
            }
            verify(hay, needle);
        }
        verify("abababababababababab".toCharArray(), "ababab".toCharArray());
        verify("aaaaaaaaaaaaaaaaaaaa".toCharArray(), "baaaaa".toCharArray());
        verify("aaaaaaaaaaaaaaaaaaaa".toCharArray(), "aaaaab".toCharArray());
        verify("\ud800\u0000\udc00\ud800\u0000\udc00".toCharArray(), "\u0000\udc00".toCharArray());
        System.out.println("M3_TWO_WAY_RAW_NEEDLE_PASS checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> storage = Class.forName("java.lang.M3String");
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> search = Class.forName("java.lang.M3StringSearchPrecompute");
        Class<?> lane = Class.forName("java.lang.M3StringBuilderLane");
        Class<?> cursor = Class.forName("java.lang.M3String$CodeUnitCursor");
        Field admission = storage.getDeclaredField("ADMITTING");
        admission.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Boolean> state = (ThreadLocal<Boolean>) admission.get(null);
        admitting = state;
        valueField = String.class.getDeclaredField("value");
        coderField = String.class.getDeclaredField("coder");
        m3Field = String.class.getDeclaredField("m3");
        valueField.setAccessible(true);
        coderField.setAccessible(true);
        m3Field.setAccessible(true);
        internChars = pool.getDeclaredMethod("internChars", char[].class, int.class, int.class);
        internChars.setAccessible(true);
        canonicalString = String.class.getDeclaredConstructor(storage);
        canonicalString.setAccessible(true);
        twoWay = search.getDeclaredMethod("twoWay", byte[].class, byte.class, int.class,
                lane, CharSequence.class, int.class, boolean.class);
        twoWay.setAccessible(true);
        canonicalTwoWay = search.getDeclaredMethod("twoWay", byte[].class, byte.class,
                int.class, lane, storage, cursor, CharSequence.class, int.class, boolean.class);
        canonicalTwoWay.setAccessible(true);
    }

    private static String flat(char[] units) throws Exception {
        Boolean previous = admitting.get();
        admitting.set(Boolean.TRUE);
        String result;
        try {
            result = new String(units);
        } finally {
            if (previous == null) admitting.remove();
            else admitting.set(previous);
        }
        if (m3Field.get(result) != null) throw new AssertionError("flat fixture unexpectedly admitted");
        return result;
    }

    /** Reversed results are offsets in the reversed haystack, as in the production kernel. */
    private static int reference(char[] hay, char[] needle, int first, boolean reverse) {
        for (int position = first; position <= hay.length - needle.length; position++) {
            boolean same = true;
            for (int i = 0; i < needle.length; i++) {
                if (hay[reverse ? hay.length - 1 - position - i : position + i]
                        != needle[reverse ? needle.length - 1 - i : i]) {
                    same = false;
                    break;
                }
            }
            if (same) return position;
        }
        return -1;
    }

    private static void verify(char[] hay, char[] needle) throws Exception {
        String text = flat(hay);
        String flatNeedle = flat(needle);
        Object textBytes = valueField.get(text);
        Object needleBytes = valueField.get(flatNeedle);
        byte coder = coderField.getByte(text);
        Object canonicalText = internChars.invoke(null, hay, 0, hay.length);
        Object canonicalNeedle = internChars.invoke(null, needle, 0, needle.length);
        if (canonicalText == null || canonicalNeedle == null) throw new AssertionError("pool refusal");
        String backed = canonicalString.newInstance(canonicalNeedle);
        if (m3Field.get(backed) != canonicalNeedle) throw new AssertionError("canonical fixture");
        CharSequence[] representations = {flatNeedle, CharBuffer.wrap(needle),
                (CharSequence) canonicalNeedle, backed};
        for (boolean reverse : new boolean[] {false, true}) {
            for (int first = 0; first <= hay.length - needle.length; first++) {
                int expected = reference(hay, needle, first, reverse);
                for (CharSequence representation : representations) {
                    check((int) twoWay.invoke(null, textBytes, coder, hay.length, null,
                            representation, first, reverse), expected);
                    check((int) canonicalTwoWay.invoke(null, null, (byte) 0, hay.length,
                            null, canonicalText, null, representation, first, reverse), expected);
                }
            }
        }
        if (m3Field.get(text) != null || m3Field.get(flatNeedle) != null
                || valueField.get(text) != textBytes || valueField.get(flatNeedle) != needleBytes
                || m3Field.get(backed) != canonicalNeedle) {
            throw new AssertionError("search changed the fixture representation");
        }
    }

    private static void check(int actual, int expected) {
        checks++;
        if (actual != expected) {
            throw new AssertionError("check " + checks + ": " + actual + " != " + expected);
        }
    }
}
