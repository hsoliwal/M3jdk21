/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: GPL-2.0-only
 *
 * @test
 * @summary M3 JNI projections preserve shared owners and bulk-copy composite String values; the
 *          byte and char shadows are fresh arrays filled in Java (decisions 47, 50)
 * @run main/othervm/native --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xcheck:jni M3StringNativeProjectionTest
 * @run main/othervm/native --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -Xcheck:jni M3StringNativeProjectionTest
 * @run main/othervm/native --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xint -Xcheck:jni M3StringNativeProjectionTest
 * @run main/othervm/native --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:TieredStopAtLevel=1 -Xbatch -Xcheck:jni M3StringNativeProjectionTest
 */
import java.lang.reflect.Field;
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
        byteShadow = m3.getDeclaredMethod("materialize");
        charShadow = m3.getDeclaredMethod("charShadow");
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
        check(projectionMask("", new char[0]) == 23, "empty JNI output");
        freshShadows(stringM3.get(scalar), original);
        System.out.println("M3_STRING_NATIVE_PROJECTION_PASS checks=" + checks);
    }

    private static void verify(String value, char[] expected, String label) throws Exception {
        Object storage = stringM3.get(value);
        check(storage != null, label + " is M3-backed");
        Object owner = m3Owner.get(storage);
        long coordinate = m3Coordinate.getLong(storage);
        byte[] layout = (byte[]) stringValue.get(value);
        check(layout.length == expected.length || layout.length == 2 * expected.length,
                label + " holds its real bytes (layout invariant)");
        for (int round = 0; round < 8; round++) {
            // Stage J: GetStringCritical pins the const chars, so bit 8 (critical copy) is off.
            check(projectionMask(value, expected) == 23, label + " native projection mask");
            char[] chars = value.toCharArray();
            char[] charsAgain = value.toCharArray();
            check(chars != charsAgain && Arrays.equals(chars, expected), label + " independent char result");
            if (chars.length > 0) chars[0] ^= 0x1111;
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            byte[] bytesAgain = value.getBytes(StandardCharsets.UTF_8);
            check(bytes != bytesAgain && Arrays.equals(bytes, bytesAgain), label + " independent byte result");
            if (bytes.length > 0) bytes[0] ^= 0x5a;
            check(Arrays.equals(value.toCharArray(), expected), label + " mutations do not change backing");
            check(stringValue.get(value) == layout, label + " value is never replaced");
            check(m3Owner.get(stringM3.get(value)) == owner, label + " owner unchanged");
            check(m3Coordinate.getLong(stringM3.get(value)) == coordinate, label + " coordinate unchanged");
        }
        String duplicate = new String(value);
        check(m3Owner.get(stringM3.get(duplicate)) == owner, label + " copy constructor shares owner");
        check(stringValue.get(duplicate) == layout, label + " copy constructor shares value");
    }

    /** The Java shadows (decision 50): each call fills a fresh array with the whole text. */
    private static void freshShadows(Object storage, char[] expected) throws Exception {
        char[] chars = (char[]) charShadow.invoke(storage);
        check(chars != charShadow.invoke(storage) && Arrays.equals(chars, expected),
                "fresh char shadow");
        byte[] bytes = (byte[]) byteShadow.invoke(storage);
        check(bytes != byteShadow.invoke(storage) && bytes.length == 2 * expected.length,
                "fresh UTF-16 byte shadow");
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
