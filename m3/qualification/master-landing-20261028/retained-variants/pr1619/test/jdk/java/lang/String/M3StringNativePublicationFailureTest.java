/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary Failed native String owner publication releases its address and reservation exactly once
 * @modules java.base/java.lang:open
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringNativePublicationFailureTest
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage -XX:-CompactStrings M3StringNativePublicationFailureTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.ref.Reference;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uses the real local factories, pool, weak-reference release and reservation counter.
 * Only one test-created bucket's ArrayList.add is replaced with a deterministic failure.
 * Automatic String admission stays disabled so unrelated test messages cannot change accounting.
 * This does not claim to reproduce arbitrary VM allocation exhaustion.
 */
public final class M3StringNativePublicationFailureTest {
    private static final OutOfMemoryError FAILURE =
            new OutOfMemoryError("deliberate test bucket publication failure");
    private static final ArrayList<Object> KEEP_ALIVE = new ArrayList<>();
    private static long checks;
    private static Class<?> poolClass;
    private static Constructor<?> bucketConstructor;
    private static Constructor<?> fingerprintConstructor;
    private static Field bucketValues;
    private static Field referenceReleased;
    private static Field localMap;
    private static Method nativeBytes;
    private static Method releaseNative;
    private static Method mix;
    private static Method charAt;
    private static Method valueOwner;
    private static boolean compact;

    private static final class FailingValues extends ArrayList<Object> {
        private static final long serialVersionUID = 1L;
        transient Object captured;
        transient Object capturedOwner;

        @Override
        public boolean add(Object value) {
            captured = value;
            capturedOwner = ((Reference<?>) value).get();
            throw FAILURE;
        }
    }

    private record Case(String label, Method operation, Object[] arguments, char[] expected) { }

    public static void main(String[] args) throws Exception {
        bind();
        char[] mixed = new char[19];
        for (int i = 0; i < mixed.length; i++) mixed[i] = (char) (0x100 + i * 37);
        mixed[7] = '\ud83d';
        mixed[8] = '\ude42';
        mixed[9] = 0;
        byte[] latin = new byte[23];
        char[] latinExpected = new char[latin.length];
        for (int i = 0; i < latin.length; i++) {
            latin[i] = (byte) (i * 43 + 11);
            latinExpected[i] = (char) (latin[i] & 255);
        }
        char[] compactExpected = mixed.clone();
        compactExpected[compactExpected.length - 1] ^= 0x20;
        byte[] compactBytes = new String(compactExpected).getBytes(
                ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN
                        ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE);
        int[] codePoints = {0x101, 0x102, 0x1f642, 0, 0xd800, 'x', 0xdc00, 0x103};
        char[] pointExpected = {'\u0101', '\u0102', '\ud83d', '\ude42', 0,
                '\ud800', 'x', '\udc00', '\u0103'};
        Case[] cases = {
            new Case("char-range", method("internChars", char[].class, int.class, int.class),
                    new Object[] {mixed.clone(), 0, mixed.length}, mixed),
            new Case("compact-range", method("internCompactBytes", byte[].class, int.class,
                    int.class, byte.class),
                    new Object[] {compactBytes, 0, compactExpected.length, (byte) 1}, compactExpected),
            new Case("latin1-range", method("internLatin1Bytes", byte[].class, int.class, int.class),
                    new Object[] {latin, 0, latin.length}, latinExpected),
            new Case("code-points", method("internCodePoints", int[].class, int.class, int.class),
                    new Object[] {codePoints, 0, codePoints.length}, pointExpected),
            new Case("single-unit", method("internUnit", char.class),
                    new Object[] {'\u7f31'}, new char[] {'\u7f31'})
        };
        for (Case scenario : cases) verify(scenario);
        System.out.println("M3_STRING_NATIVE_PUBLICATION_FAILURE_PASS checks=" + checks);
    }

    private static void verify(Case scenario) throws Exception {
        Object map = localMap.get(null);
        Method putIfAbsent = ConcurrentHashMap.class.getMethod("putIfAbsent", Object.class, Object.class);
        Method remove = ConcurrentHashMap.class.getMethod("remove", Object.class, Object.class);
        byte coder = (byte) (compact && allLatin1(scenario.expected()) ? 0 : 1);
        int bytes = scenario.expected().length << coder;
        long hash = (long) mix.invoke(null,
                0x9e3779b97f4a7c15L ^ coder ^ Integer.toUnsignedLong(bytes));
        for (char unit : scenario.expected()) hash = (long) mix.invoke(null, hash ^ unit);
        Object fingerprint = fingerprintConstructor.newInstance(coder, bytes, hash);
        Object bucket = bucketConstructor.newInstance();
        FailingValues failing = new FailingValues();
        bucketValues.set(bucket, failing);
        Object previous = putIfAbsent.invoke(map, fingerprint, bucket);
        check(previous == null, scenario.label() + ": isolated fingerprint was not previously admitted");
        long before = (long) nativeBytes.invoke(null);
        Throwable observed = null;
        try {
            scenario.operation().invoke(null, scenario.arguments());
        } catch (InvocationTargetException expected) {
            observed = expected.getCause();
        }
        long afterFailure = (long) nativeBytes.invoke(null);
        Object reference = failing.captured;
        boolean wasReleased = reference != null && referenceReleased.getBoolean(reference);
        long afterFirstCleanup;
        long afterSecondCleanup;
        try {
            if (reference != null) {
                synchronized (bucket) {
                    releaseNative.invoke(reference);
                    afterFirstCleanup = (long) nativeBytes.invoke(null);
                    releaseNative.invoke(reference);
                    afterSecondCleanup = (long) nativeBytes.invoke(null);
                }
            } else {
                afterFirstCleanup = afterSecondCleanup = afterFailure;
            }
        } finally {
            remove.invoke(map, fingerprint, bucket);
        }
        check(observed == FAILURE, scenario.label() + ": exact failure preserved");
        check(reference != null, scenario.label() + ": real LocalRef reached publication");
        check(afterFailure == before, scenario.label() + ": reservation released on failed publication");
        check(wasReleased, scenario.label() + ": native cleanup record marked released");
        check(afterFirstCleanup == before && afterSecondCleanup == before,
                scenario.label() + ": delayed release is idempotent");

        Object result = scenario.operation().invoke(null, scenario.arguments());
        Object repeated = scenario.operation().invoke(null, scenario.arguments());
        KEEP_ALIVE.add(result);
        KEEP_ALIVE.add(repeated);
        check(valueOwner.invoke(result) == valueOwner.invoke(repeated),
                scenario.label() + ": successful retry reuses canonical owner");
        char[] actual = new char[scenario.expected().length];
        for (int i = 0; i < actual.length; i++) actual[i] = (char) charAt.invoke(result, i);
        check(Arrays.equals(actual, scenario.expected()), scenario.label() + ": exact UTF-16 retry");
    }

    private static boolean allLatin1(char[] units) {
        for (char unit : units) if (unit > 255) return false;
        return true;
    }

    private static Method method(String name, Class<?>... arguments) throws Exception {
        Method method = poolClass.getDeclaredMethod(name, arguments);
        method.setAccessible(true);
        return method;
    }

    private static void bind() throws Exception {
        poolClass = Class.forName("java.lang.M3StringPool");
        Class<?> bucketClass = Class.forName("java.lang.M3StringPool$LocalBucket");
        Class<?> fingerprintClass = Class.forName("java.lang.M3StringPool$Fingerprint");
        Class<?> referenceClass = Class.forName("java.lang.M3StringPool$LocalRef");
        Class<?> valueClass = Class.forName("java.lang.M3String");
        bucketConstructor = bucketClass.getDeclaredConstructor();
        bucketConstructor.setAccessible(true);
        fingerprintConstructor = fingerprintClass.getDeclaredConstructor(byte.class, int.class, long.class);
        fingerprintConstructor.setAccessible(true);
        bucketValues = bucketClass.getDeclaredField("values");
        bucketValues.setAccessible(true);
        referenceReleased = referenceClass.getDeclaredField("released");
        referenceReleased.setAccessible(true);
        localMap = poolClass.getDeclaredField("LOCAL");
        localMap.setAccessible(true);
        nativeBytes = method("localNativeBytes");
        mix = method("mix64", long.class);
        releaseNative = referenceClass.getDeclaredMethod("releaseNative");
        releaseNative.setAccessible(true);
        charAt = valueClass.getDeclaredMethod("charAt", int.class);
        charAt.setAccessible(true);
        valueOwner = valueClass.getDeclaredMethod("owner");
        valueOwner.setAccessible(true);
        Field compactStrings = String.class.getDeclaredField("COMPACT_STRINGS");
        compactStrings.setAccessible(true);
        compact = compactStrings.getBoolean(null);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
