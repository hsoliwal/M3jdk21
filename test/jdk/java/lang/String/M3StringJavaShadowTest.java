/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */

/*
 * @test
 * @summary M3JDK decision 50: the M3String byte and char shadows are filled in Java. Over atoms,
 *          immediates, tuples and slices of Latin-1 and UTF-16 text, materialize() and
 *          charShadow() hold the text of the flat String of the same spelling, are fresh arrays
 *          on every call, and new String(M3String) holds exactly that text
 * @modules java.base/java.lang:open
 * @run main M3StringJavaShadowTest
 * @run main/othervm -XX:-CompactStrings M3StringJavaShadowTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      M3StringJavaShadowTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class M3StringJavaShadowTest {
    private static final int[] SIZES = {1, 2, 7, 8, 11, 16, 255, 256, 1023, 1024, 1025, 4099};
    private static final Map<String, Integer> OWNERS = new TreeMap<>();
    private static Method internChars;
    private static Method concat;
    private static Method slice;
    private static Method materialize;
    private static Method charShadow;
    private static Method coder;
    private static Method owner;
    private static Constructor<String> fromM3;
    private static Field stringValue;
    private static Field stringM3;
    private static long checks;
    private static long refused;

    public static void main(String[] args) throws Throwable {
        bind();
        for (String text : texts()) {
            shapes(text);
        }
        System.out.println("owners " + OWNERS);
        System.out.println("M3StringJavaShadowTest checks=" + checks + " refused=" + refused);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        internChars = method(pool, "internChars", char[].class, int.class, int.class);
        concat = method(pool, "concat", m3, m3);
        slice = method(m3, "slice", int.class, int.class);
        materialize = method(m3, "materialize");
        charShadow = method(m3, "charShadow");
        coder = method(m3, "coder");
        owner = method(m3, "owner");
        fromM3 = String.class.getDeclaredConstructor(m3);
        fromM3.setAccessible(true);
        stringValue = String.class.getDeclaredField("value");
        stringValue.setAccessible(true);
        stringM3 = String.class.getDeclaredField("m3");
        stringM3.setAccessible(true);
    }

    /** Latin-1, UTF-16 (BMP, pairs, lone surrogates) and mixed spellings at the sizes. */
    private static List<String> texts() {
        List<String> texts = new ArrayList<>();
        texts.add("");
        texts.add("abcdefg");
        texts.add("identifier");
        for (int size : SIZES) {
            texts.add(spell(size, 0));
            texts.add(spell(size, 1));
            texts.add(spell(size, 2));
        }
        return texts;
    }

    /** kind 0: Latin-1 incl. NUL and 0xff; 1: UTF-16 with pairs and lone surrogates; 2: one
     *  UTF-16 unit at the end of Latin-1 text, so slices of a UTF-16 owner hold Latin-1 text. */
    private static String spell(int size, int kind) {
        char[] units = new char[size];
        for (int index = 0; index < size; index++) {
            int step = index * 7 + size;
            units[index] = kind == 1 ? (char) (0x100 + step % 0x2f00) : (char) (step % 0x100);
        }
        if (kind == 1 && size >= 4) {
            units[1] = '\ud83d';
            units[2] = '\ude42';
            units[size - 1] = '\ud800';
            units[size / 2] = '\udc00';
        }
        if (kind == 2) {
            units[size - 1] = '\u0101';
        }
        return new String(units);
    }

    private static void shapes(String text) throws Throwable {
        Object atom = intern(text);
        if (atom == null) {
            return;
        }
        verify(atom, text, "atom");
        slices(atom, text, "atom");
        if (text.length() < 2) {
            return;
        }
        Object tuple = null;
        int pieces = Math.min(8, text.length());
        for (int piece = 0; piece < pieces; piece++) {
            int begin = text.length() * piece / pieces;
            int end = text.length() * (piece + 1) / pieces;
            Object part = intern(text.substring(begin, end));
            if (part == null) {
                return;
            }
            tuple = tuple == null ? part : invoke(concat, null, tuple, part);
        }
        verify(tuple, text, "tuple");
        slices(tuple, text, "tuple");
        Object doubled = invoke(concat, null, tuple, atom);
        verify(doubled, text + text, "tuple+atom");
    }

    private static void slices(Object storage, String text, String shape) throws Throwable {
        int n = text.length();
        int[][] ranges = {{1, n - 1}, {n / 3, 2 * n / 3}, {0, n / 2}, {n / 2, n}, {n - 1, n}};
        for (int[] range : ranges) {
            if (range[0] >= 0 && range[0] < range[1] && range[1] <= n) {
                Object part = invoke(slice, storage, range[0], range[1]);
                verify(part, text.substring(range[0], range[1]), shape + " slice");
            }
        }
    }

    private static Object intern(String text) throws Throwable {
        char[] units = text.toCharArray();
        Object storage = invoke(internChars, null, units, 0, units.length);
        if (storage == null) {
            refused++;
        }
        return storage;
    }

    private static void verify(Object storage, String text, String shape) throws Throwable {
        String kind = invoke(owner, storage).getClass().getSimpleName();
        OWNERS.merge(kind, 1, Integer::sum);
        String label = shape + " (" + kind + ") of " + text.length() + " units";
        char[] chars = (char[]) invoke(charShadow, storage);
        check(Arrays.equals(chars, text.toCharArray()), label + ": charShadow holds the text");
        check(chars != invoke(charShadow, storage), label + ": charShadow is a fresh array");
        byte[] bytes = (byte[]) invoke(materialize, storage);
        check(text.equals(decode(bytes, text.length())), label + ": materialize holds the text");
        check(bytes.length == text.length() << (byte) invoke(coder, storage),
                label + ": materialize lays the bytes out in the value's coder");
        check(bytes != invoke(materialize, storage), label + ": materialize is a fresh array");
        String string = fromM3.newInstance(storage);
        check(stringM3.get(string) == storage, label + ": new String(M3String) keeps the storage");
        check(Arrays.equals((byte[]) stringValue.get(string), bytes),
                label + ": new String(M3String) holds the shadow bytes");
        check(string.length() == text.length() && string.equals(text) && text.equals(string),
                label + ": new String(M3String) equals the flat String");
        check(Arrays.equals(string.toCharArray(), chars), label + ": toCharArray");
        check(string.hashCode() == text.hashCode(), label + ": hashCode");
    }

    /** The text of String bytes: Latin-1 when one byte per unit, else StringUTF16 order. */
    private static String decode(byte[] bytes, int units) {
        if (bytes.length == units) {
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
        check(bytes.length == 2 * units, "byte length " + bytes.length + " for " + units);
        char[] chars = new char[units];
        ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asCharBuffer().get(chars);
        return new String(chars);
    }

    private static Object invoke(Method method, Object target, Object... arguments)
            throws Throwable {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters)
            throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
