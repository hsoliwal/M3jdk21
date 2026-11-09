/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary Verify GetStringCritical before other JNI content access on flat and composite strings
 * @modules java.base/java.lang:open
 * @run main/native/othervm -Xcheck:jni -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringCriticalFirstTest
 * @run main/native/othervm -Xmx128m -Xcheck:jni -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCriticalFirstTest requireM3
 * @run main/native/othervm -Xmx128m -Xcheck:jni -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringCriticalFirstTest requireM3
 * @run main/native/othervm -Xint -Xmx128m -Xcheck:jni -XX:+UseSerialGC -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCriticalFirstTest requireM3
 */

import java.lang.reflect.Field;
import java.util.Arrays;

public final class M3StringCriticalFirstTest {
    private static final char[] MIXED = {
        0, 'a', '\u00ff', '\u0100', '\u07ff', '\u0800', '\uffff',
        '\ud800', 'x', '\udc00', '\ud83d', '\ude42'
    };
    private static final long OFFSET = 0xcbf29ce484222325L;
    private static final long PRIME = 0x100000001b3L;
    private static long checks;
    private static int fixtures;
    private static int m3Fixtures;
    private static volatile Object pressure;
    private static Field storageField;

    static {
        System.loadLibrary("M3StringCriticalFirstTest");
    }

    private static native long[] criticalFirst(String value);

    public static void main(String[] args) throws Exception {
        boolean requireM3 = Arrays.asList(args).contains("requireM3");
        if (requireM3) {
            storageField = String.class.getDeclaredField("m3");
            storageField.setAccessible(true);
        }
        check(criticalFirst(null) == null, "test bridge null input");
        for (int kind = 0; kind < 3; kind++) {
            for (int length : new int[] {0, 1, 7, 8, 63, 64, 127, 128, 255, 256,
                    257, 4095, 4096, 4097, 8191, 8192, 8193, 16385}) {
                char[] expected = units(kind, length);
                verify(new String(expected), expected);
                verify(compose(expected, 97), expected);
                char[] padded = new char[length + 32];
                Arrays.fill(padded, '\u4321');
                System.arraycopy(expected, 0, padded, 13, length);
                verify(compose(padded, 63).substring(13, 13 + length), expected);
            }
        }
        char[] expected = units(2, 8193);
        String retainedSlice = compose(expected, 31).substring(17, expected.length - 19);
        char[] wanted = Arrays.copyOfRange(expected, 17, expected.length - 19);
        for (int round = 0; round < 8; round++) {
            pressure = new byte[262144];
            System.gc();
            verify(retainedSlice, wanted);
        }
        if (requireM3) check(m3Fixtures > 0, "M3 storage was exercised");
        System.out.println("M3_STRING_CRITICAL_FIRST_PASS|checks=" + checks
                + "|fixtures=" + fixtures + "|m3Fixtures=" + m3Fixtures);
    }

    private static char[] units(int kind, int length) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = switch (kind) {
                case 0 -> (char) ((i * 53 + 11) & 127);
                case 1 -> (char) ((i * 37 + 17) & 255);
                default -> MIXED[i % MIXED.length];
            };
        }
        if (kind == 2 && length > 97) {
            result[96] = '\ud83d';
            result[97] = '\ude42';
        }
        return result;
    }

    private static String compose(char[] units, int width) {
        String result = "";
        for (int at = 0; at < units.length; at += width) {
            result = result.concat(new String(units, at, Math.min(width, units.length - at)));
        }
        return result;
    }

    private static void verify(String value, char[] expected) throws Exception {
        fixtures++;
        long wanted = OFFSET;
        for (char unit : expected) wanted = (wanted ^ unit) * PRIME;
        // The native bridge's first content read is GetStringCritical, with no
        // preceding GetStringChars, GetStringRegion or Java materialization.
        long[] first = criticalFirst(value);
        check(first != null && first.length == 3, "native result shape");
        check(first[0] == wanted, "independent UTF-16 digest");
        check(first[1] == expected.length, "native logical length");
        check(first[2] == 0 || first[2] == 1, "JNI copy indicator");
        if (storageField != null && storageField.get(value) != null) {
            m3Fixtures++;
            check(first[2] == 1, "M3 critical access uses the existing copied lifetime");
        }
        for (int repeat = 0; repeat < 3; repeat++) {
            check(Arrays.equals(first, criticalFirst(value)), "release and reacquire");
        }
        check(Arrays.equals(value.toCharArray(), expected), "canonical source unchanged");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
