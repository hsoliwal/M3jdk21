/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3 String concat/join overflow must throw OutOfMemoryError, not ArithmeticException
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -Xmx512m M3StringConcatLengthOverflowTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings -Xmx512m M3StringConcatLengthOverflowTest
 */
import java.lang.reflect.Field;
import java.util.StringJoiner;

public class M3StringConcatLengthOverflowTest {
    private static final Field M3;
    private static int checks;

    static {
        try {
            M3 = String.class.getDeclaredField("m3");
            M3.setAccessible(true);
        } catch (ReflectiveOperationException problem) {
            throw new ExceptionInInitializerError(problem);
        }
    }

    public static void main(String[] args) throws Exception {
        // Deliberately create a large *logical* M3 range, not a 512 MiB byte[].
        // The bounded memory budget makes unintended flattening observable.
        String seed = new String(new char[] {'Q'});
        check(M3.get(seed) != null, "seed must use canonical M3 storage");
        String halfBillion = seed.repeat(1 << 29);
        check(halfBillion.length() == 536_870_912, "large logical repetition length");
        check(M3.get(halfBillion) != null, "repetition must retain no-copy M3 geometry");

        String billion = halfBillion.concat(halfBillion);
        check(billion.length() == (1 << 30), "legal concatenated logical length");
        check(billion.charAt(0) == 'Q' && billion.charAt(billion.length() - 1) == 'Q',
                "legal large concat still observes exact UTF-16 units");

        String withinLimit = String.join("", billion, halfBillion);
        check(withinLimit.length() == 1_610_612_736, "legal join may exceed flat heap");
        check(withinLimit.charAt(withinLimit.length() - 1) == 'Q',
                "legal large join has correct trailing UTF-16 unit");

        expectLogicalLengthLimit(() -> billion.concat(billion), "String.concat");
        expectLogicalLengthLimit(() -> String.join("", billion, billion),
                "String.join no delimiter");
        expectLogicalLengthLimit(() -> String.join(",", billion, billion),
                "String.join with delimiter");

        StringJoiner joiner = new StringJoiner(":", "[", "]");
        joiner.add(billion);
        check(joiner.length() == billion.length() + 2,
                "legal StringJoiner length includes prefix and suffix");
        // StringJoiner rejects the second element at add(), before toString() is reachable.
        // Its existing size-limit diagnostic is distinct from the M3 pool boundary.
        expectOutOfMemory(() -> joiner.add(billion), "StringJoiner.add",
                "Requested array size exceeds VM limit");
        verifyUtf8FactRepresentability();
        System.out.println("M3_STRING_CONCAT_LENGTH_OVERFLOW_PASS|checks=" + checks);
    }

    private static void expectLogicalLengthLimit(Runnable operation, String label) {
        expectOutOfMemory(operation, label, "Required length exceeds implementation limit");
    }

    private static void expectOutOfMemory(
            Runnable operation, String label, String expectedMessage) {
        checks++;
        try {
            operation.run();
        } catch (OutOfMemoryError expected) {
            if (!expectedMessage.equals(expected.getMessage())) {
                throw new AssertionError("unrelated OOME instead of logical length guard: " + label,
                        expected);
            }
            return;
        }
        throw new AssertionError("missing OutOfMemoryError for " + label);
    }

    private static void check(boolean okay, String explanation) {
        checks++;
        if (!okay) throw new AssertionError(explanation);
    }

    private static void verifyUtf8FactRepresentability() throws Exception {
        String seed = new String(new char[] {'\u0800'});
        check(M3.get(seed) != null, "UTF-8 seed must use canonical M3 storage");
        String huge = seed.repeat(1 << 30);
        check(M3.get(huge) != null, "huge UTF-8 repetition must retain canonical storage");
        check(huge.length() == (1 << 30), "UTF-8 overflow keeps legal UTF-16 length");
        check(huge.charAt(0) == '\u0800'
                        && huge.charAt(huge.length() / 2) == '\u0800'
                        && huge.charAt(huge.length() - 1) == '\u0800',
                "huge UTF-8 repetition keeps exact units");
        check(huge.codePointCount(0, huge.length()) == huge.length(),
                "codePointCount does not require representable UTF-8 bytes");
        check(huge.trim().length() == huge.length(),
                "trim does not require representable UTF-8 bytes");
        check(huge.strip().length() == huge.length(),
                "strip does not require representable UTF-8 bytes");
        checkUtf8Facts(huge, -1, -1, 0, "huge BMP repetition");
        expectOutOfMemory(() -> huge.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "String.getBytes UTF-8 length",
                "Required UTF-8 length exceeds implementation limit");
        expectOutOfMemory(() -> strictUtf8(huge), "strict well-formed UTF-8 length",
                "Required UTF-8 length exceeds implementation limit");

        String high = new String(new char[] {'\ud800'});
        String low = new String(new char[] {'\udc00'});
        String malformedHuge = huge.concat(high);
        check(malformedHuge.length() == huge.length() + 1
                        && malformedHuge.charAt(malformedHuge.length() - 1) == '\ud800',
                "huge unpaired suffix preserves logical units");
        checkUtf8Facts(malformedHuge, -1, -1, 1, "huge unpaired suffix");
        // The unpaired suffix selects encodeUtf8NoRepl's own allocation guard.
        expectOutOfMemory(() -> strictUtf8(malformedHuge), "strict malformed UTF-8 length",
                "Required UTF-8 length exceeds implementation limit");

        String boundary = seed.repeat(715_827_882);
        check(boundary.length() == 715_827_882, "UTF-8 representable boundary length");
        checkUtf8Facts(boundary, 2_147_483_646, 2_147_483_646, 0,
                "largest three-byte BMP repetition that fits");
        checkUtf8Facts(boundary.concat(seed), -1, -1, 0,
                "one more three-byte BMP unit exceeds int");

        String nearBoundary = seed.repeat(715_827_881);
        String seam = nearBoundary.concat(high).concat(low);
        check(seam.length() == 715_827_883
                        && seam.charAt(seam.length() - 2) == '\ud800'
                        && seam.charAt(seam.length() - 1) == '\udc00',
                "boundary surrogate seam preserves its UTF-16 pair");
        check(seam.codePointCount(0, seam.length()) == 715_827_882,
                "boundary seam folds the pair into one code point");
        checkUtf8Facts(seam, Integer.MAX_VALUE, -1, 0,
                "standard UTF-8 seam fits while modified UTF-8 does not");
        checkUtf8Facts(seam.concat(new String(new char[] {'A'})), -1, -1, 0,
                "one ASCII byte exceeds the exact UTF-8 boundary");

        // Expected bytes are literals, independent of the candidate encoding implementation.
        checkSmallUtf8("ASCII", new char[] {'A', 'z'},
                new byte[] {0x41, 0x7a}, 2, -1);
        checkSmallUtf8("NUL", new char[] {'\0'}, new byte[] {0}, 2, -1);
        checkSmallUtf8("BMP", new char[] {'\u07ff', '\u0800'},
                new byte[] {(byte) 0xdf, (byte) 0xbf, (byte) 0xe0, (byte) 0xa0, (byte) 0x80},
                5, -1);
        checkSmallUtf8("surrogate pair", new char[] {'\ud83d', '\ude00'},
                new byte[] {(byte) 0xf0, (byte) 0x9f, (byte) 0x98, (byte) 0x80}, 6, -1);
        checkSmallUtf8("unpaired high", new char[] {'\ud800'}, new byte[] {0x3f}, 3, 0);
        checkSmallUtf8("unpaired low", new char[] {'\udc00'}, new byte[] {0x3f}, 3, 0);
        checkSmallUtf8("embedded unpaired high", new char[] {'A', '\ud800', 'B'},
                new byte[] {0x41, 0x3f, 0x42}, 5, 1);
    }

    private static void checkUtf8Facts(
            String value, int expectedUtf8, int expectedModifiedUtf8, int expectedUnpaired,
            String label) throws ReflectiveOperationException {
        Object storage = M3.get(value);
        check(storage != null, label + " must use canonical M3 storage");
        java.lang.reflect.Method prepare = storage.getClass().getDeclaredMethod("facts");
        prepare.setAccessible(true);
        Object facts = prepare.invoke(storage);
        Field utf8 = facts.getClass().getDeclaredField("utf8Length");
        Field modifiedUtf8 = facts.getClass().getDeclaredField("modifiedUtf8Length");
        Field unpaired = facts.getClass().getDeclaredField("unpairedSurrogateCount");
        check(utf8.getType() == int.class && modifiedUtf8.getType() == int.class,
                label + " UTF-8 facts retain the int ABI");
        utf8.setAccessible(true);
        modifiedUtf8.setAccessible(true);
        unpaired.setAccessible(true);
        check(utf8.getInt(facts) == expectedUtf8, label + " standard UTF-8 fact");
        check(modifiedUtf8.getInt(facts) == expectedModifiedUtf8,
                label + " modified UTF-8 fact");
        check(unpaired.getInt(facts) == expectedUnpaired, label + " unpaired surrogate fact");
    }

    private static void checkSmallUtf8(
            String label, char[] units, byte[] expected, int expectedModifiedUtf8,
            int malformedOffset) throws Exception {
        String value = new String(units);
        checkUtf8Facts(value, expected.length, expectedModifiedUtf8,
                malformedOffset < 0 ? 0 : 1, label);
        check(java.util.Arrays.equals(
                        value.getBytes(java.nio.charset.StandardCharsets.UTF_8), expected),
                label + " replacement UTF-8 bytes");
        if (malformedOffset < 0) {
            check(java.util.Arrays.equals(strictUtf8(value), expected),
                    label + " strict UTF-8 bytes");
        } else {
            expectStrictMalformed(value, malformedOffset, label);
        }
    }

    private static byte[] strictUtf8(String value) {
        try {
            java.lang.reflect.Method encode =
                    String.class.getDeclaredMethod("getBytesUTF8NoRepl", String.class);
            encode.setAccessible(true);
            return (byte[]) encode.invoke(null, value);
        } catch (java.lang.reflect.InvocationTargetException wrapper) {
            Throwable cause = wrapper.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new AssertionError("unexpected checked strict UTF-8 failure", cause);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("cannot invoke String.getBytesUTF8NoRepl", failure);
        }
    }

    private static void expectStrictMalformed(String value, int offset, String label) {
        try {
            strictUtf8(value);
        } catch (IllegalArgumentException expected) {
            check(("malformed input off : " + offset + ", length : 1")
                            .equals(expected.getMessage()),
                    label + " strict malformed offset");
            check(expected.getCause() instanceof java.nio.charset.UnmappableCharacterException,
                    label + " strict malformed cause");
            check(((java.nio.charset.UnmappableCharacterException) expected.getCause())
                            .getInputLength() == 1,
                    label + " strict malformed input length");
            return;
        }
        throw new AssertionError("missing strict malformed failure for " + label);
    }
}
