/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * @test
 * @summary M3 JNI byte/char shadows copy canonical leaf paths into independent ordinary arrays
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringJniCompositeLeafPathTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringJniCompositeLeafPathTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/** Original java.base owners and JNI exports; test data is assembled independently. */
public class M3StringJniCompositeLeafPathTest {
    private static final Class<?> M3;
    private static final Method ADMIT;
    private static final Method SLICE;
    private static final Method LENGTH;
    private static final Method CODER;
    private static final Method CHAR_EXPORT;
    private static final Method BYTE_EXPORT;
    private static final Method UTF16_UNIT;
    private static final Field OWNER;
    private static long checks;

    static {
        try {
            M3 = Class.forName("java.lang.M3String");
            ADMIT = method(M3, "admitArrayRanges", char[][].class, int[].class, int[].class);
            SLICE = method(M3, "slice", int.class, int.class);
            LENGTH = method(M3, "length");
            CODER = method(M3, "coder");
            CHAR_EXPORT = method(M3, "charShadow");
            BYTE_EXPORT = method(M3, "materialize");
            UTF16_UNIT = method(Class.forName("java.lang.StringUTF16"),
                    "getChar", byte[].class, int.class);
            OWNER = M3.getDeclaredField("owner");
            OWNER.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... args)
            throws ReflectiveOperationException {
        Method value = type.getDeclaredMethod(name, args);
        value.setAccessible(true);
        return value;
    }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail + " check=" + checks);
    }

    private static void verify(Object value, char[] expected, String label) throws Exception {
        check(value != null, label + " canonical value");
        check((int) LENGTH.invoke(value) == expected.length, label + " length");
        Object owner = OWNER.get(value);

        char[] first = (char[]) CHAR_EXPORT.invoke(value);
        char[] second = (char[]) CHAR_EXPORT.invoke(value);
        check(first != second && Arrays.equals(first, expected)
                && Arrays.equals(second, expected), label + " fresh UTF16 arrays");

        byte coder = (byte) CODER.invoke(value);
        byte[] bytes = (byte[]) BYTE_EXPORT.invoke(value);
        byte[] secondBytes = (byte[]) BYTE_EXPORT.invoke(value);
        check(bytes != secondBytes && Arrays.equals(bytes, secondBytes),
                label + " fresh compact arrays");
        check(bytes.length == (expected.length << coder), label + " byte length");
        for (int i = 0; i < expected.length; i++) {
            char actual = coder == 0 ? (char) (bytes[i] & 255)
                    : (char) UTF16_UNIT.invoke(null, bytes, i);
            check(actual == expected[i], label + " byte coder at " + i);
        }

        if (first.length != 0) first[0] ^= 0x55;
        if (bytes.length != 0) bytes[0] ^= 0x5a;
        check(Arrays.equals((char[]) CHAR_EXPORT.invoke(value), expected),
                label + " mutable export cannot change canonical text");
        check(OWNER.get(value) == owner, label + " same canonical parent owner");
    }

    private static void slices(char[] alphabet, int count, long seed) throws Exception {
        char[][] backing = new char[count][];
        int[] offsets = new int[count];
        int[] lengths = new int[count];
        char[] expected = new char[count];
        for (int i = 0; i < count; i++) {
            char unit = alphabet[i % alphabet.length];
            backing[i] = new char[] {'#', unit, '!'};
            offsets[i] = 1;
            lengths[i] = 1;
            expected[i] = unit;
        }
        Object value = ADMIT.invoke(null, (Object) backing, offsets, lengths);
        verify(value, expected, "all slices " + count);
        Random random = new Random(seed);
        for (int trial = 0; trial < 40; trial++) {
            int from = random.nextInt(count + 1);
            int end = from + random.nextInt(count - from + 1);
            Object slice = SLICE.invoke(value, from, end);
            verify(slice, Arrays.copyOfRange(expected, from, end),
                    "subrange " + count + "/" + from + ":" + end);
        }
        for (char[] array : backing) array[1] = '?';
        verify(value, expected, "caller array mutated after admission " + count);
    }

    public static void main(String[] args) throws Exception {
        check(args.length == 0, "argument count");
        slices("0123456789abcdef".toCharArray(), 1, 7L);
        slices("0123456789abcdef".toCharArray(), 65, 17L);
        slices(new char[] {'x', '\ud83d', '\ude42', '\u0100', '\0', '\ud800',
                'z', '\udc00'}, 193, 27L);
        slices(new char[] {'\u00ff', '\u0100', 'A', '\ud83d', '\ude42'},
                513, 37L);
        System.out.println("M3_JNI_COMPOSITE_LEAF_PATH_PASS checks=" + checks);
    }
}
