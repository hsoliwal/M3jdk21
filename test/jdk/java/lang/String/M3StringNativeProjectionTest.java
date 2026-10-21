/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only
 *
 * @test
 * @summary M3 JNI projections validate logical ranges, preserve shared owners, and bulk-copy composite String values
 * @run main/othervm/native --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xcheck:jni M3StringNativeProjectionTest
 * @run main/othervm/native --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -Xcheck:jni M3StringNativeProjectionTest
 * @run main/othervm/native --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xint -Xcheck:jni M3StringNativeProjectionTest
 * @run main/othervm/native --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:TieredStopAtLevel=1 -Xbatch -Xcheck:jni M3StringNativeProjectionTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class M3StringNativeProjectionTest {
    private static long checks;
    private static Field stringM3;
    private static Field stringValue;
    private static Field m3Owner;
    private static Field m3Coordinate;
    private static Method byteShadow;
    private static Method charShadow;
    private static native int projectionMask(String value, char[] expected);

    public static void main(String[] args) throws Exception {
        System.loadLibrary("M3StringNativeProjectionTest");
        stringM3 = field(String.class, "m3");
        stringValue = field(String.class, "value");
        Class<?> m3 = Class.forName("java.lang.M3String");
        m3Owner = field(m3, "owner");
        m3Coordinate = field(m3, "value");
        byteShadow = m3.getDeclaredMethod("nativeByteShadow", m3, int.class, int.class, byte.class);
        charShadow = m3.getDeclaredMethod("nativeCharShadow", m3, int.class, int.class);
        byteShadow.setAccessible(true);
        charShadow.setAccessible(true);
        long fields = Arrays.stream(m3.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers())).count();
        check(fields == 2, "M3String retains only owner and coordinate");

        char[] original = {'A', '\0', '\u00ff', '\u0100', '\ud83d', '\ude42', '\ud800', 'Z'};
        String scalar = new String(original);
        verify(scalar, original, "scalar");
        verify(scalar.substring(1, original.length - 1),
                Arrays.copyOfRange(original, 1, original.length - 1), "slice");
        String left = new String(new char[] {'L', '\ud83d'});
        String right = new String(new char[] {'\ude42', 'R', '\u0100'});
        char[] joinedExpected = {'L', '\ud83d', '\ude42', 'R', '\u0100'};
        String joined = left.concat(right);
        verify(joined, joinedExpected, "surrogate seam tuple");
        String dag = joined;
        char[] dagExpected = joinedExpected;
        for (int i = 0; i < 13; i++) {
            dag = dag.concat(dag);
            char[] doubled = Arrays.copyOf(dagExpected, dagExpected.length * 2);
            System.arraycopy(dagExpected, 0, doubled, dagExpected.length, dagExpected.length);
            dagExpected = doubled;
        }
        verify(dag, dagExpected, "shared tuple DAG");
        verify(dag.substring(3, dag.length() - 2),
                Arrays.copyOfRange(dagExpected, 3, dagExpected.length - 2), "DAG range");
        char[] ascii = {'a', 'b', 'c', 'd', 'e', 'f'};
        verify(new String(ascii), ascii, "compact ASCII");
        check(projectionMask("", new char[0]) == 31, "empty JNI output");
        invalidRanges(stringM3.get(scalar), scalar.length());
        System.out.println("M3_STRING_NATIVE_PROJECTION_PASS checks=" + checks);
    }

    private static void verify(String value, char[] expected, String label) throws Exception {
        Object storage = stringM3.get(value);
        check(storage != null, label + " is M3-backed");
        Object owner = m3Owner.get(storage);
        long coordinate = m3Coordinate.getLong(storage);
        byte[] sentinel = (byte[]) stringValue.get(value);
        check(sentinel.length == 0, label + " has no retained flat payload");
        for (int round = 0; round < 8; round++) {
            check(projectionMask(value, expected) == 31, label + " native projection mask");
            char[] chars = value.toCharArray();
            char[] charsAgain = value.toCharArray();
            check(chars != charsAgain && Arrays.equals(chars, expected), label + " independent char result");
            if (chars.length > 0) chars[0] ^= 0x1111;
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            byte[] bytesAgain = value.getBytes(StandardCharsets.UTF_8);
            check(bytes != bytesAgain && Arrays.equals(bytes, bytesAgain), label + " independent byte result");
            if (bytes.length > 0) bytes[0] ^= 0x5a;
            check(Arrays.equals(value.toCharArray(), expected), label + " mutations do not change backing");
            check(stringValue.get(value) == sentinel, label + " no retained replacement shadow");
            check(m3Owner.get(stringM3.get(value)) == owner, label + " owner unchanged");
            check(m3Coordinate.getLong(stringM3.get(value)) == coordinate, label + " coordinate unchanged");
        }
        String duplicate = new String(value);
        check(m3Owner.get(stringM3.get(duplicate)) == owner, label + " copy constructor shares owner");
        check(stringValue.get(duplicate) == sentinel, label + " copy constructor shares empty sentinel");
    }

    private static void invalidRanges(Object storage, int length) throws Exception {
        expect(IndexOutOfBoundsException.class, byteShadow, storage, length + 1, 0, (byte) 0);
        expect(StringIndexOutOfBoundsException.class, charShadow, storage, length + 1, 0);
        expect(IndexOutOfBoundsException.class, byteShadow, storage, 0, Integer.MAX_VALUE, (byte) 1);
        expect(StringIndexOutOfBoundsException.class, charShadow, storage, Integer.MAX_VALUE, 1);
        expect(IllegalArgumentException.class, byteShadow, storage, -1, 0, (byte) 0);
        expect(IllegalArgumentException.class, charShadow, storage, 0, -1);
        check(((byte[]) byteShadow.invoke(null, storage, length, 0, (byte) 1)).length == 0,
                "valid empty end byte range");
        check(((char[]) charShadow.invoke(null, storage, length, 0)).length == 0,
                "valid empty end char range");
    }

    private static void expect(Class<? extends Throwable> type, Method method, Object... arguments)
            throws Exception {
        checks++;
        try {
            method.invoke(null, arguments);
        } catch (InvocationTargetException failure) {
            if (failure.getCause().getClass() != type) throw new AssertionError(type.getName(), failure);
            return;
        }
        throw new AssertionError("missing " + type.getName());
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
