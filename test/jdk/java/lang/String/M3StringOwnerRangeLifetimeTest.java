/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 *
 * @test
 * @summary M3 String ranges retain owners without a second Java spelling payload and local native atoms are weakly reclaimable
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringOwnerRangeLifetimeTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringOwnerRangeLifetimeTest
 */

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class M3StringOwnerRangeLifetimeTest {
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static final Field M3_VALUE;
    private static final Field TUPLE_LEFT;
    private static final Field TUPLE_RIGHT;
    private static final Method LOCAL_NATIVE_BYTES;
    private static final Method EXPUNGE_LOCALS;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_OWNER = m3.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
            M3_VALUE = m3.getDeclaredField("value");
            M3_VALUE.setAccessible(true);
            Class<?> tuple = Class.forName("java.lang.M3StringTuple");
            TUPLE_LEFT = tuple.getDeclaredField("left");
            TUPLE_LEFT.setAccessible(true);
            TUPLE_RIGHT = tuple.getDeclaredField("right");
            TUPLE_RIGHT.setAccessible(true);
            Class<?> pool = Class.forName("java.lang.M3StringPool");
            LOCAL_NATIVE_BYTES = pool.getDeclaredMethod("localNativeBytes");
            LOCAL_NATIVE_BYTES.setAccessible(true);
            EXPUNGE_LOCALS = pool.getDeclaredMethod("expungeLocals");
            EXPUNGE_LOCALS.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        structureHasNoSecondSpellingPayload();
        slicesRetainOneOwner();
        tupleRetainsChildCoordinates();
        localNativeAtomsAreWeaklyReclaimable();
        System.out.println("M3_STRING_OWNER_RANGE_LIFETIME_PASS|checks=" + checks);
    }

    private static void structureHasNoSecondSpellingPayload() throws Exception {
        Class<?> m3 = Class.forName("java.lang.M3String");
        List<Field> m3Fields = instanceFields(m3);
        check(m3Fields.size() == 2, "M3String must have exactly owner+coordinate instance fields");
        check(m3Fields.stream().anyMatch(field -> field.getName().equals("owner")),
                "M3String owner field");
        check(m3Fields.stream().anyMatch(field -> field.getName().equals("value")
                        && field.getType() == long.class),
                "M3String packed coordinate field");
        for (String className : List.of(
                "java.lang.M3String",
                "java.lang.M3StringOwner",
                "java.lang.M3StringAtom",
                "java.lang.M3StringTuple",
                "java.lang.M3StringFacts")) {
            for (Field field : instanceFields(Class.forName(className))) {
                check(!field.getType().isArray(),
                        className + " must not retain array payload field " + field.getName());
            }
        }
    }

    private static void slicesRetainOneOwner() throws Exception {
        String whole = new String("0123456789-\u0100-\uD83D\uDE42-tail".toCharArray());
        String middle = whole.substring(3, whole.length() - 4);
        String nested = middle.substring(2, middle.length() - 2);
        Object wholeM3 = requireM3(whole);
        Object middleM3 = requireM3(middle);
        Object nestedM3 = requireM3(nested);
        Object owner = M3_OWNER.get(wholeM3);
        same(owner, M3_OWNER.get(middleM3), "substring owner");
        same(owner, M3_OWNER.get(nestedM3), "nested substring owner");
        check(M3_VALUE.getLong(wholeM3) != M3_VALUE.getLong(middleM3),
                "substring changes only packed coordinate");
        check(M3_VALUE.getLong(middleM3) != M3_VALUE.getLong(nestedM3),
                "nested substring changes only packed coordinate");
        check(middle.equals("3456789-\u0100-\uD83D\uDE42-"), "middle content");
        check(nested.equals("56789-\u0100-\uD83D\uDE42"), "nested content");
    }

    private static void tupleRetainsChildCoordinates() throws Exception {
        String left = new String("left-\u0100".toCharArray());
        String right = new String("\uD83D\uDE42-right".toCharArray());
        Object leftM3 = requireM3(left);
        Object rightM3 = requireM3(right);
        Object leftOwner = M3_OWNER.get(leftM3);
        Object rightOwner = M3_OWNER.get(rightM3);
        String joined = left.concat(right);
        Object joinedM3 = requireM3(joined);
        Object tupleOwner = M3_OWNER.get(joinedM3);
        check(tupleOwner.getClass().getName().equals("java.lang.M3StringTuple"),
                "concat must retain tuple owner");
        Object tupleLeft = TUPLE_LEFT.get(tupleOwner);
        Object tupleRight = TUPLE_RIGHT.get(tupleOwner);
        same(leftOwner, M3_OWNER.get(tupleLeft), "tuple left retains canonical owner");
        same(rightOwner, M3_OWNER.get(tupleRight), "tuple right retains canonical owner");
        WeakReference<Object> leftOwnerRef = new WeakReference<>(leftOwner);
        WeakReference<Object> rightOwnerRef = new WeakReference<>(rightOwner);
        left = null;
        right = null;
        leftM3 = null;
        rightM3 = null;
        leftOwner = null;
        rightOwner = null;
        forceGcCycles(3);
        check(leftOwnerRef.get() != null, "live tuple must strongly retain left owner");
        check(rightOwnerRef.get() != null, "live tuple must strongly retain right owner");
        check(joined.equals("left-\u0100\uD83D\uDE42-right"),
                "tuple content survives source-wrapper collection pressure");
    }

    private static void localNativeAtomsAreWeaklyReclaimable() throws Exception {
        long before = localNativeBytes();
        List<WeakReference<Object>> owners = allocateEphemeralOwners();
        long peak = localNativeBytes();
        check(peak > before, "ephemeral local atoms must increase native retained-byte accounting");
        int cleared = 0;
        long after = peak;
        for (int attempt = 0; attempt < 40 && (cleared == 0 || after >= peak); attempt++) {
            forceGcCycles(1);
            EXPUNGE_LOCALS.invoke(null);
            cleared = 0;
            for (WeakReference<Object> owner : owners) {
                if (owner.get() == null) cleared++;
            }
            after = localNativeBytes();
        }
        check(cleared > 0, "pool weak references must allow local atom owner collection");
        check(after < peak, "expunge must release at least some local native payload");
    }

    private static List<WeakReference<Object>> allocateEphemeralOwners() throws Exception {
        ArrayList<WeakReference<Object>> owners = new ArrayList<>();
        for (int item = 0; item < 96; item++) {
            char[] units = new char[257 + (item % 17)];
            for (int index = 0; index < units.length; index++) {
                units[index] = (char) (0x0100 + ((item * 131 + index * 17) & 0x3fff));
            }
            units[0] = (char) (0x4000 + item);
            String value = new String(units);
            Object m3 = requireM3(value);
            Object owner = M3_OWNER.get(m3);
            check(owner.getClass().getName().equals("java.lang.M3StringAtom"),
                    "ephemeral scalar owner");
            owners.add(new WeakReference<>(owner));
        }
        return owners;
    }

    private static long localNativeBytes() throws Exception {
        return (long) LOCAL_NATIVE_BYTES.invoke(null);
    }

    private static void forceGcCycles(int cycles) throws InterruptedException {
        for (int cycle = 0; cycle < cycles; cycle++) {
            System.gc();
            byte[][] pressure = new byte[4][];
            for (int i = 0; i < pressure.length; i++) {
                pressure[i] = new byte[1 << 20];
            }
            Thread.sleep(20L);
        }
    }

    private static Object requireM3(String value) throws IllegalAccessException {
        value.length();
        Object m3 = STRING_M3.get(value);
        check(m3 != null, "String must be M3-backed");
        return m3;
    }

    private static List<Field> instanceFields(Class<?> type) {
        ArrayList<Field> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) fields.add(field);
        }
        return fields;
    }

    private static void same(Object expected, Object actual, String label) {
        check(expected == actual, label);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
