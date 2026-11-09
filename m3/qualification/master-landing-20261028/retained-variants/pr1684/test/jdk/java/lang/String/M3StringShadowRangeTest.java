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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary Check actual M3String JNI shadows at empty, overflowing and clipped logical ranges
 * @run main/othervm -Xcheck:jni --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringShadowRangeTest
 * @run main/othervm -Xcheck:jni --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringShadowRangeTest
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.util.Arrays;

public class M3StringShadowRangeTest {
    private static final Class<?> TYPE;
    private static final Method ADMIT;
    private static final Method CONCAT;
    private static final Method SLICE;
    private static final Method CHARS;
    private static final Method BYTES;
    private static long checks;

    static {
        try {
            TYPE = Class.forName("java.lang.M3String");
            ADMIT = method(TYPE, "admit", char[].class, int.class, int.class);
            SLICE = method(TYPE, "slice", int.class, int.class);
            CONCAT = method(Class.forName("java.lang.M3StringPool"), "concat", TYPE, TYPE);
            CHARS = method(TYPE, "nativeCharShadow", TYPE, int.class, int.class);
            BYTES = method(TYPE, "nativeByteShadow", TYPE, int.class, int.class, byte.class);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        char[][] inputs = {
            {}, {'a'}, {'a', '\0', '\u00ff', 'z'},
            {'\u0100', '\uD800', 'x', '\uDC00', '\uD83D', '\uDE42', 'z'}
        };
        for (char[] expected : inputs) {
            Object value = admit(expected);
            verifyRanges(value, expected);
            verifyInvalid(value, expected.length);
        }
        char[] left = {'a', '\u0100', '\uD83D'};
        char[] right = {'\uDE42', '\0', 'z'};
        Object joined = CONCAT.invoke(null, admit(left), admit(right));
        char[] expected = new char[left.length + right.length];
        System.arraycopy(left, 0, expected, 0, left.length);
        System.arraycopy(right, 0, expected, left.length, right.length);
        verifyRanges(joined, expected);
        verifyRanges(SLICE.invoke(joined, 1, expected.length - 1),
                Arrays.copyOfRange(expected, 1, expected.length - 1));
        expect(NullPointerException.class, CHARS, null, -1, -1);
        expect(NullPointerException.class, BYTES, null, -1, -1, (byte) 3);
        System.out.println("M3_STRING_SHADOW_RANGE_PASS|checks=" + checks);
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        Method result = owner.getDeclaredMethod(name, parameters);
        result.setAccessible(true);
        return result;
    }

    private static Object admit(char[] input) throws ReflectiveOperationException {
        Object result = ADMIT.invoke(null, input, 0, input.length);
        check(result != null, "canonical admission unexpectedly refused");
        return result;
    }

    private static void verifyRanges(Object value, char[] expected) throws Exception {
        for (int start = 0; start <= expected.length; start++) {
            for (int length = 0; length <= expected.length - start; length++) {
                char[] wanted = Arrays.copyOfRange(expected, start, start + length);
                char[] first = (char[]) CHARS.invoke(null, value, start, length);
                char[] second = (char[]) CHARS.invoke(null, value, start, length);
                check(first != second, "char shadow identity");
                check(Arrays.equals(first, wanted), "char shadow range");
                if (first.length > 0) first[0] ^= 0x55;
                check(Arrays.equals(second, wanted), "char shadow isolation");
                check(Arrays.equals((char[]) CHARS.invoke(null, value, start, length), wanted),
                        "canonical text unchanged");
                verifyBytes(value, start, length, wanted, (byte) 1);
                boolean latin = true;
                for (char unit : wanted) latin &= unit <= 0xff;
                if (latin) verifyBytes(value, start, length, wanted, (byte) 0);
            }
        }
    }

    private static void verifyBytes(Object value, int start, int length,
                                    char[] expected, byte coder) throws Exception {
        byte[] first = (byte[]) BYTES.invoke(null, value, start, length, coder);
        byte[] second = (byte[]) BYTES.invoke(null, value, start, length, coder);
        check(first != second, "byte shadow identity");
        check(first.length == length << coder, "byte shadow length");
        int highShift = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ? 8 : 0;
        int lowShift = 8 - highShift;
        for (int i = 0; i < length; i++) {
            int actual = coder == 0 ? first[i] & 0xff
                    : ((first[i * 2] & 0xff) << highShift)
                    | ((first[i * 2 + 1] & 0xff) << lowShift);
            check(actual == expected[i], "byte shadow content");
        }
        check(Arrays.equals(first, second), "byte shadow determinism");
        if (first.length > 0) first[0] ^= 0x5a;
        check(Arrays.equals(second, (byte[]) BYTES.invoke(null, value, start, length, coder)),
                "byte shadow isolation");
    }

    private static void verifyInvalid(Object value, int size) throws Exception {
        expect(IllegalArgumentException.class, CHARS, value, -1, 0);
        expect(IllegalArgumentException.class, CHARS, value, 0, -1);
        expect(IllegalArgumentException.class, BYTES, value, -1, 0, (byte) 0);
        expect(IllegalArgumentException.class, BYTES, value, 0, -1, (byte) 0);
        expect(IllegalArgumentException.class, BYTES, value, 0, 0, (byte) 2);
        expect(IllegalArgumentException.class, BYTES, value, 0, 0, (byte) -1);
        expect(StringIndexOutOfBoundsException.class, CHARS, value, size + 1, 0);
        expect(StringIndexOutOfBoundsException.class, CHARS, value, size, 1);
        expect(StringIndexOutOfBoundsException.class, CHARS, value, Integer.MAX_VALUE, 1);
        expect(StringIndexOutOfBoundsException.class, CHARS, value, 1, Integer.MAX_VALUE);
        expect(IndexOutOfBoundsException.class, BYTES, value, size + 1, 0, (byte) 0);
        expect(IndexOutOfBoundsException.class, BYTES, value, size, 1, (byte) 0);
        expect(IndexOutOfBoundsException.class, BYTES, value, Integer.MAX_VALUE, 1, (byte) 0);
        expect(IndexOutOfBoundsException.class, BYTES, value, 1, Integer.MAX_VALUE, (byte) 0);
        expect(OutOfMemoryError.class, BYTES, value, 0, Integer.MAX_VALUE, (byte) 1);
    }

    private static void expect(Class<? extends Throwable> expected, Method method,
                               Object... arguments) throws Exception {
        try {
            method.invoke(null, arguments);
        } catch (InvocationTargetException failure) {
            check(failure.getCause().getClass() == expected,
                    "expected " + expected.getName() + " but got " + failure.getCause());
            return;
        }
        throw new AssertionError("expected " + expected.getName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
