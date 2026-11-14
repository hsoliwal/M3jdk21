/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Exact hierarchical M3 String facts for standard UTF-8 versus JNI modified UTF-8
 * @requires (vm.bits == "64")
 * @run main/native/othervm -Xmx128m --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringModifiedUtf8DpTest
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/** Separate exact JNI/Java contract oracle and the M3 owner-range DP admission witness. */
public final class M3StringModifiedUtf8DpTest {
    private static final char[] ALPHABET = {'\0', 'A', '\u007f', '\u0080', '\u07ff',
            '\u0800', '\ud800', '\udc00', '\ud83d', '\ude03'};
    private static final int UTF8_REPLACEMENT = StandardCharsets.UTF_8.newEncoder()
            .replacement().length;
    private static final boolean ORACLE_ONLY = Boolean.getBoolean("m3.oracle.only");
    private static final Field M3_FIELD;
    private static final Method M3_FACTS;
    private static final Field MUTF8_FACT;
    private static long assertions;
    private static long cases;

    static {
        System.loadLibrary("M3StringModifiedUtf8DpTest");
        Field m3 = null, mutf8 = null;
        Method facts = null;
        try {
            m3 = String.class.getDeclaredField("m3");
            m3.setAccessible(true);
            Class<?> m3Type = Class.forName("java.lang.M3String");
            facts = m3Type.getDeclaredMethod("facts");
            facts.setAccessible(true);
            Class<?> factsType = Class.forName("java.lang.M3StringFacts");
            mutf8 = factsType.getDeclaredField("modifiedUtf8Length");
            mutf8.setAccessible(true);
        } catch (ReflectiveOperationException noM3OnStockJdk) {
            if (!ORACLE_ONLY) throw new ExceptionInInitializerError(noM3OnStockJdk);
        }
        M3_FIELD = m3;
        M3_FACTS = facts;
        MUTF8_FACT = mutf8;
    }

    private M3StringModifiedUtf8DpTest() { }

    private static native int nativeModifiedLength(String value);
    private static native byte[] nativeModifiedBytes(String value);

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        verify("");
        for (char value : ALPHABET) verify(new String(new char[] {value}));
        for (int width = 0; width <= 4; width++) {
            int options = 1;
            for (int i = 0; i < width; i++) options *= ALPHABET.length;
            for (int packed = 0; packed < options; packed++) {
                char[] chars = new char[width];
                int bits = packed;
                for (int i = 0; i < width; i++, bits /= ALPHABET.length) {
                    chars[i] = ALPHABET[bits % ALPHABET.length];
                }
                String word = fresh(new String(chars));
                verify(word);
                for (int seam = 0; seam <= width; seam++) {
                    LengthFacts left = scan(word.substring(0, seam));
                    LengthFacts right = scan(word.substring(seam));
                    LengthFacts combined = compose(left, right);
                    check(combined.standard == word.getBytes(StandardCharsets.UTF_8).length,
                            "standard UTF-8 seam DP");
                    check(combined.modified == nativeModifiedLength(word),
                            "modified UTF-8 seam DP");
                }
            }
        }
        Random random = new Random(20261014);
        for (int trial = 0; trial < 5_000; trial++) {
            int width = random.nextInt(80);
            char[] units = new char[width];
            for (int i = 0; i < width; i++) units[i] = (char) random.nextInt(1 << 16);
            String joined = fresh(new String(units));
            int a = random.nextInt(width + 1);
            int b = a + random.nextInt(width - a + 1);
            String result = fresh(joined.substring(0, a))
                    .concat(fresh(joined.substring(a, b)))
                    .concat(fresh(joined.substring(b)));
            verify(result);
            LengthFacts three = compose(
                    compose(scan(result.substring(0, a)), scan(result.substring(a, b))),
                    scan(result.substring(b)));
            LengthFacts other = compose(scan(result.substring(0, a)),
                    compose(scan(result.substring(a, b)), scan(result.substring(b))));
            check(three.equals(other), "associative hierarchical DP");
        }
        overflowProof();
        System.out.println("M3_MUTF8_DP_JNI_PASS cases=" + cases + " assertions=" + assertions
                + " maxHeap=" + Runtime.getRuntime().maxMemory());
    }

    private static void overflowProof() {
        LengthFacts current = scan("\ud800");
        for (int i = 0; i < 30; i++) {
            current = compose(current, current);
            if (current.modified > Integer.MAX_VALUE) {
                check(current.cachedModifiedLength() == -1,
                        "overflow must refuse positive JNI cached length");
            }
        }
        check(current.modified > Integer.MAX_VALUE,
                "hierarchical DP can exceed jint without allocating huge text");
    }

    private static void verify(String input) throws Exception {
        String word = fresh(input);
        int length = nativeModifiedLength(word);
        byte[] actual = nativeModifiedBytes(word);
        check(length == actual.length, "JNI length exactly matches returned native bytes");
        check(Arrays.equals(actual, independentDataOutputModifiedUtf8(word)),
                "JNI byte-for-byte modified UTF-8 oracle");
        LengthFacts facts = scan(word);
        check(facts.modified == length, "atom-level modified UTF-8 scan");
        check(facts.standard == word.getBytes(StandardCharsets.UTF_8).length,
                "standard UTF-8 fact stays independent");
        if (!ORACLE_ONLY && word.length() != 0) {
            Object storage = Objects.requireNonNull(M3_FIELD.get(word),
                    "nonempty String must have M3 backing with requested flag");
            Object exactFacts = M3_FACTS.invoke(storage);
            int cached = MUTF8_FACT.getInt(exactFacts);
            check(cached == facts.cachedModifiedLength(), "native-facing cached fact parity");
            check(length == nativeModifiedLength(word), "warm precompute JNI length");
            check(Arrays.equals(actual, nativeModifiedBytes(word)),
                    "warm precompute JNI byte-for-byte parity");
        }
        cases++;
    }

    private static String fresh(String text) { return new String(text.toCharArray()); }

    private static byte[] independentDataOutputModifiedUtf8(String text) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) { output.writeUTF(text); }
        byte[] withPrefix = bytes.toByteArray();
        int declared = ((withPrefix[0] & 0xff) << 8) | (withPrefix[1] & 0xff);
        check(declared == withPrefix.length - 2, "DataOutputStream oracle prefix");
        return Arrays.copyOfRange(withPrefix, 2, withPrefix.length);
    }

    private static LengthFacts scan(String text) {
        long standard = text.getBytes(StandardCharsets.UTF_8).length;
        long modified = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            modified += c >= 1 && c <= 0x7f ? 1 : c <= 0x07ff ? 2 : 3;
        }
        return new LengthFacts(text.length(), standard, modified,
                text.isEmpty() ? (char) 0 : text.charAt(0),
                text.isEmpty() ? (char) 0 : text.charAt(text.length() - 1));
    }

    private static LengthFacts compose(LengthFacts left, LengthFacts right) {
        if (left.length == 0) return right;
        if (right.length == 0) return left;
        boolean seam = Character.isHighSurrogate(left.last)
                && Character.isLowSurrogate(right.first);
        return new LengthFacts(Math.addExact(left.length, right.length),
                Math.addExact(left.standard, right.standard)
                        + (seam ? 4 - 2L * UTF8_REPLACEMENT : 0L),
                Math.addExact(left.modified, right.modified), left.first, right.last);
    }

    private record LengthFacts(long length, long standard, long modified,
                               char first, char last) {
        int cachedModifiedLength() {
            return modified <= Integer.MAX_VALUE ? (int) modified : -1;
        }
    }

    private static void check(boolean yes, String label) {
        assertions++;
        if (!yes) throw new AssertionError(label);
    }
}
