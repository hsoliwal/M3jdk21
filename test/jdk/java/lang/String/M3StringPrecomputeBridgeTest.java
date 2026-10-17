/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3 String precompute bridge centralizes exact String operations without adding bridge state
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringPrecomputeBridgeTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Random;

public class M3StringPrecomputeBridgeTest {
    private static final Field STRING_M3;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        bridgeShape();
        deterministic();
        randomized();
        System.out.println("M3_STRING_PRECOMPUTE_BRIDGE_PASS|checks=" + checks);
    }

    private static void bridgeShape() throws Exception {
        Class<?> bridge = Class.forName("java.lang.M3StringPrecomputeBridge");
        check(!Modifier.isPublic(bridge.getModifiers()), "bridge must stay package-private");
        check(bridge.getDeclaredFields().length == 0, "bridge owns no cache/state fields");
        for (Method method : bridge.getDeclaredMethods()) {
            check(Modifier.isStatic(method.getModifiers()), "bridge method must be static: " + method);
            check(!Modifier.isPublic(method.getModifiers()), "bridge method must stay internal: " + method);
        }
    }

    private static void deterministic() throws Exception {
        String source = fresh("  bananana\uD83D\uDE00\u2003  ");
        String ana = fresh("ana");
        String prefix = fresh("  ban");
        String suffix = fresh("\u2003  ");
        requireM3(source);
        requireM3(ana);
        requireM3(prefix);
        requireM3(suffix);

        check(source.startsWith(prefix), "prefix");
        check(source.endsWith(suffix), "suffix");
        check(source.indexOf(ana) == 3, "indexOf canonical");
        check(source.indexOf(ana, 4) == 5, "indexOf from canonical");
        check(source.lastIndexOf(ana) == 7, "lastIndexOf canonical");
        check(source.lastIndexOf(ana, 6) == 5, "lastIndexOf from canonical");
        check(source.contains(ana), "contains inherits bridged indexOf");

        String equal = fresh(source);
        requireM3(equal);
        check(source.equals(equal), "M3 equality");
        check(source.compareTo(equal) == 0, "M3 compare equality");
        check(source.hashCode() == manualHash(source), "M3 hash");

        String smaller = fresh("  bananana\uD83D\uDE00\u2002  ");
        requireM3(smaller);
        check(Integer.signum(source.compareTo(smaller))
                        == Integer.signum(manualCompare(source, smaller)),
                "UTF-16 compare");

        String sub = source.substring(2, source.length() - 2);
        requireM3(sub);
        check(equalsChars(sub, chars(source, 2, source.length() - 2)), "substring bridge");

        String asciiTrim = fresh("  Alpha  ");
        requireM3(asciiTrim);
        check(equalsChars(asciiTrim.trim(), "Alpha".toCharArray()), "trim");
        check(equalsChars(asciiTrim.strip(), "Alpha".toCharArray()), "strip");
        check(equalsChars(asciiTrim.stripLeading(), "Alpha  ".toCharArray()), "stripLeading");
        check(equalsChars(asciiTrim.stripTrailing(), "  Alpha".toCharArray()), "stripTrailing");

        String split = fresh("A\uD83D\uDE00Z");
        requireM3(split);
        String high = split.substring(1, 2);
        String low = split.substring(2, 3);
        check(high.length() == 1 && high.charAt(0) == '\uD83D', "high surrogate slice");
        check(low.length() == 1 && low.charAt(0) == '\uDE00', "low surrogate slice");
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d33425249444745L);
        char[] alphabet = {
                0, 'a', 'b', 'A', 'Z', ' ', '\t', '\n',
                '\u00ff', '\u0100', '\u2003', '\ud83d', '\ude00',
                '\ud800', '\udc00', '\uffff'
        };

        for (int trial = 0; trial < 4_000; trial++) {
            char[] sourceChars = randomChars(random, alphabet, random.nextInt(48));
            char[] needleChars = randomChars(random, alphabet, random.nextInt(12));
            String source = fresh(sourceChars);
            String needle = fresh(needleChars);
            requireM3(source);
            requireM3(needle);

            check(source.hashCode() == manualHash(sourceChars), "hash " + trial);
            check(source.equals(needle) == Arrays.equals(sourceChars, needleChars),
                    "equals " + trial);
            check(Integer.signum(source.compareTo(needle))
                            == Integer.signum(manualCompare(sourceChars, needleChars)),
                    "compare " + trial);
            check(source.startsWith(needle) == manualStartsWith(sourceChars, needleChars, 0),
                    "startsWith " + trial);
            check(source.endsWith(needle)
                            == manualStartsWith(
                                    sourceChars, needleChars, sourceChars.length - needleChars.length),
                    "endsWith " + trial);

            for (int sample = 0; sample < 4; sample++) {
                int from = random.nextInt(sourceChars.length + 9) - 4;
                check(source.indexOf(needle, from) == manualIndexOf(sourceChars, needleChars, from),
                        "indexOf " + trial + ":" + sample);
                check(source.lastIndexOf(needle, from)
                                == manualLastIndexOf(sourceChars, needleChars, from),
                        "lastIndexOf " + trial + ":" + sample);
            }

            if (sourceChars.length != 0) {
                int a = random.nextInt(sourceChars.length + 1);
                int b = a + random.nextInt(sourceChars.length - a + 1);
                check(equalsChars(source.substring(a, b), Arrays.copyOfRange(sourceChars, a, b)),
                        "substring " + trial);
            }
        }
    }

    private static void requireM3(String value) throws Exception {
        value.length();
        check(STRING_M3.get(value) != null, "String must be M3-backed");
    }

    private static String fresh(String value) {
        return new String(value.toCharArray());
    }

    private static String fresh(char[] value) {
        return new String(value.clone());
    }

    private static char[] randomChars(Random random, char[] alphabet, int length) {
        char[] result = new char[length];
        for (int index = 0; index < length; index++) {
            result[index] = alphabet[random.nextInt(alphabet.length)];
        }
        return result;
    }

    private static int manualHash(CharSequence value) {
        int hash = 0;
        for (int index = 0; index < value.length(); index++) {
            hash = 31 * hash + value.charAt(index);
        }
        return hash;
    }

    private static int manualHash(char[] value) {
        int hash = 0;
        for (char unit : value) hash = 31 * hash + unit;
        return hash;
    }

    private static int manualCompare(CharSequence left, CharSequence right) {
        int limit = Math.min(left.length(), right.length());
        for (int index = 0; index < limit; index++) {
            int difference = left.charAt(index) - right.charAt(index);
            if (difference != 0) return difference;
        }
        return left.length() - right.length();
    }

    private static int manualCompare(char[] left, char[] right) {
        int limit = Math.min(left.length, right.length);
        for (int index = 0; index < limit; index++) {
            int difference = left[index] - right[index];
            if (difference != 0) return difference;
        }
        return left.length - right.length;
    }

    private static boolean manualStartsWith(char[] source, char[] prefix, int offset) {
        if (offset < 0 || offset > source.length - prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) {
            if (source[offset + index] != prefix[index]) return false;
        }
        return true;
    }

    private static int manualIndexOf(char[] source, char[] needle, int fromIndex) {
        int from = Math.max(0, fromIndex);
        if (needle.length == 0) return Math.min(from, source.length);
        if (from > source.length - needle.length) return -1;
        for (int start = from; start <= source.length - needle.length; start++) {
            if (manualStartsWith(source, needle, start)) return start;
        }
        return -1;
    }

    private static int manualLastIndexOf(char[] source, char[] needle, int fromIndex) {
        int start = Math.min(fromIndex, source.length - needle.length);
        if (start < 0) return -1;
        if (needle.length == 0) return start;
        for (int candidate = start; candidate >= 0; candidate--) {
            if (manualStartsWith(source, needle, candidate)) return candidate;
        }
        return -1;
    }

    private static char[] chars(CharSequence value, int from, int to) {
        char[] result = new char[to - from];
        for (int index = from; index < to; index++) result[index - from] = value.charAt(index);
        return result;
    }

    private static boolean equalsChars(CharSequence value, char[] expected) {
        if (value.length() != expected.length) return false;
        for (int index = 0; index < expected.length; index++) {
            if (value.charAt(index) != expected[index]) return false;
        }
        return true;
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
