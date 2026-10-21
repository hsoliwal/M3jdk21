/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary Independent UTF-16 array oracles for copied, critical and region JNI String access
 * @modules java.base/java.lang:open
 * @build M3StringRegexNativeBridgeTest
 * @run main/native/othervm -Xmx128m -Xcheck:jni -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringCriticalBulkTest
 * @run main/native/othervm -Xmx128m -Xcheck:jni -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCriticalBulkTest requireM3
 * @run main/native/othervm -Xmx128m -Xcheck:jni -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringCriticalBulkTest requireM3
 * @run main/native/othervm -Xint -Xmx128m -XX:+UseSerialGC -Xcheck:jni -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCriticalBulkTest requireM3
 */

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Reuses the existing native bridge without changing its implementation.
 * Expected digests and round-trip content come only from independent char[] data.
 */
public final class M3StringCriticalBulkTest {
    private static final long OFFSET = 0xcbf29ce484222325L;
    private static final long PRIME = 0x100000001b3L;
    private static final char[] MIXED = {
        0, 'a', '\u00ff', '\u0100', '\u07ff', '\u0800', '\uffff',
        '\ud800', 'x', '\udc00', '\ud83d', '\ude42'
    };
    private static long checks;
    private static int fixtures;
    private static int m3Fixtures;
    private static int nativeAtomFixtures;
    private static volatile Object pressure;
    private static Method digests;
    private static Method roundTrip;
    private static Field m3Storage;
    private static Method owner;
    private static Method nativePayloadBytes;

    public static void main(String[] args) throws Exception {
        boolean requireM3 = Arrays.asList(args).contains("requireM3");
        Class<?> bridge = Class.forName("M3StringRegexNativeBridgeTest");
        digests = bridge.getDeclaredMethod("digests", String.class);
        roundTrip = bridge.getDeclaredMethod("roundTrip", String.class);
        digests.setAccessible(true);
        roundTrip.setAccessible(true);
        if (requireM3) {
            m3Storage = String.class.getDeclaredField("m3");
            m3Storage.setAccessible(true);
            owner = Class.forName("java.lang.M3String").getDeclaredMethod("owner");
            owner.setAccessible(true);
            nativePayloadBytes = Class.forName("java.lang.M3StringAtom")
                    .getDeclaredMethod("nativePayloadBytes");
            nativePayloadBytes.setAccessible(true);
        }
        check(invoke(digests, null) == null, "null digest contract");
        check(invoke(roundTrip, null) == null, "null round-trip contract");
        for (int kind = 0; kind < 3; kind++) {
            for (int length : new int[] {
                    0, 1, 31, 32, 63, 64, 127, 128, 129, 255, 256, 257,
                    4095, 4096, 4097, 8191, 8192, 8193, 16385}) {
                char[] expected = units(length, kind);
                String label = "kind=" + kind + ",length=" + length;
                verify(label + ",flat", new String(expected), expected);
                verify(label + ",composite", compose(expected, 97), expected);
                char[] padded = new char[expected.length + 34];
                Arrays.fill(padded, '\u4321');
                System.arraycopy(expected, 0, padded, 13, expected.length);
                String clipped = compose(padded, 97).substring(13, 13 + expected.length);
                verify(label + ",clipped", clipped, expected);
            }
        }
        char[] expected = units(513, 2);
        char[] mutable = expected.clone();
        String snapshot = new String(mutable);
        Arrays.fill(mutable, '?');
        verify("source-array-ownership", snapshot, expected);

        Detached detached = detached();
        for (int round = 0; round < 8; round++) {
            pressure = new byte[262144];
            System.gc();
            verify("detached-after-gc-" + round, detached.text(), detached.expected());
        }
        if (requireM3) {
            check(m3Fixtures > 0, "enabled action exercised admitted M3 strings");
            check(nativeAtomFixtures > 0, "enabled action exercised canonical native atom payloads");
        }
        System.out.println("M3_STRING_CRITICAL_BULK_PASS checks=" + checks
                + " fixtures=" + fixtures + " m3Fixtures=" + m3Fixtures
                + " nativeAtomFixtures=" + nativeAtomFixtures);
    }

    private static char[] units(int length, int kind) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = switch (kind) {
                case 0 -> (char) ((i * 53 + 11) & 127);
                case 1 -> (char) ((i * 37 + 17) & 255);
                case 2 -> MIXED[i % MIXED.length];
                default -> throw new AssertionError("unknown data kind");
            };
        }
        if (kind == 2 && length > 97) {
            result[96] = '\ud83d';
            result[97] = '\ude42';
        }
        if (kind == 2 && length > 8192) {
            result[8191] = '\ud800';
            result[8192] = '\udc00';
        }
        return result;
    }

    private static String compose(char[] units, int chunkSize) {
        String text = "";
        for (int at = 0; at < units.length; at += chunkSize) {
            text = text.concat(new String(units, at, Math.min(chunkSize, units.length - at)));
        }
        return text;
    }

    private static void verify(String label, String text, char[] expected) throws Exception {
        fixtures++;
        if (m3Storage != null) {
            Object storage = m3Storage.get(text);
            if (storage != null) {
                m3Fixtures++;
                Object payloadOwner = owner.invoke(storage);
                if (nativePayloadBytes.getDeclaringClass().isInstance(payloadOwner)
                        && (long) nativePayloadBytes.invoke(payloadOwner) > 0) {
                    nativeAtomFixtures++;
                }
            }
        }
        long expectedHash = OFFSET;
        for (char unit : expected) expectedHash = (expectedHash ^ unit) * PRIME;
        long[] actual = (long[]) invoke(digests, text);
        check(actual != null && actual.length == 3, label + ": digest shape");
        for (int method = 0; method < actual.length; method++) {
            check(actual[method] == expectedHash, label + ": JNI digest path " + method);
        }
        String copied = (String) invoke(roundTrip, text);
        check(copied != null, label + ": native NewString is non-null");
        check(copied.length() == expected.length, label + ": native NewString length");
        char[] copiedUnits = new char[expected.length];
        copied.getChars(0, copiedUnits.length, copiedUnits, 0);
        check(Arrays.equals(copiedUnits, expected), label + ": native NewString UTF-16 content");
    }

    private record Detached(String text, char[] expected) { }

    private static Detached detached() {
        char[] original = units(16393, 2);
        String clipped = compose(original, 63).substring(17, original.length - 11);
        return new Detached(clipped, Arrays.copyOfRange(original, 17, original.length - 11));
    }

    private static Object invoke(Method target, String input) throws Exception {
        try {
            return target.invoke(null, (Object) input);
        } catch (InvocationTargetException failure) {
            throw new AssertionError("Native bridge failed: " + target.getName(), failure.getCause());
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
