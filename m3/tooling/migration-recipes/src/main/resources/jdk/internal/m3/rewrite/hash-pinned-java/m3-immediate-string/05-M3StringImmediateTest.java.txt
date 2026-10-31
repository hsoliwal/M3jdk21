/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary M3StringImmediate packs short Latin-1 / identifier text into one long: round trip,
 *          canonical profile choice, java.lang.String hash parity, refusal outside the domain.
 * @modules java.base/java.lang:open
 * @run main/othervm M3StringImmediateTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;

public final class M3StringImmediateTest {
    private static final String IDENT =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_$";

    private static MethodHandle packChars;
    private static MethodHandle packCompactBytes;
    private static MethodHandle packCodePoints;
    private static MethodHandle of;
    private static MethodHandle length;
    private static MethodHandle unit;
    private static MethodHandle charAt;
    private static MethodHandle getChars;
    private static MethodHandle javaHash;
    private static MethodHandle sameText;
    private static long none;
    private static long tagMask;
    private static long tagLatin1;
    private static long tagIdent;
    private static long checks;

    public static void main(String[] args) throws Throwable {
        bind();
        exhaustiveLatin1();
        randomLatin1();
        randomIdent();
        refusals();
        injectivity();
        System.out.println("M3StringImmediateTest PASS checks=" + checks);
    }

    private static void bind() throws Throwable {
        Class<?> immediate = Class.forName("java.lang.M3StringImmediate");
        Class<?> owner = Class.forName("java.lang.M3StringOwner");
        MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(immediate, MethodHandles.lookup());
        MethodHandles.Lookup ownerLookup =
                MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
        packChars = lookup.findStatic(immediate, "packChars",
                MethodType.methodType(long.class, char[].class, int.class, int.class));
        packCompactBytes = lookup.findStatic(immediate, "packCompactBytes",
                MethodType.methodType(long.class, byte[].class, int.class, int.class, byte.class));
        packCodePoints = lookup.findStatic(immediate, "packCodePoints",
                MethodType.methodType(long.class, int[].class, int.class, int.class));
        of = lookup.findStatic(immediate, "of", MethodType.methodType(immediate, long.class));
        length = lookup.findStatic(immediate, "length", MethodType.methodType(int.class, long.class));
        unit = lookup.findStatic(immediate, "unit",
                MethodType.methodType(char.class, long.class, int.class));
        charAt = lookup.findVirtual(immediate, "charAt",
                MethodType.methodType(char.class, int.class));
        getChars = lookup.findVirtual(immediate, "getChars",
                MethodType.methodType(void.class, int.class, int.class, char[].class, int.class));
        javaHash = ownerLookup.findGetter(owner, "javaHash", int.class);
        sameText = lookup.findStatic(immediate, "sameText",
                MethodType.methodType(boolean.class, owner, owner));
        none = (long) lookup.findStaticGetter(immediate, "NONE", long.class).invoke();
        tagMask = (long) lookup.findStaticGetter(immediate, "TAG_MASK", long.class).invoke();
        tagLatin1 = (long) lookup.findStaticGetter(immediate, "TAG_LATIN1", long.class).invoke();
        tagIdent = (long) lookup.findStaticGetter(immediate, "TAG_IDENT", long.class).invoke();
    }

    /** Every Latin-1 text of length 1 and 2: 256 + 65,536 values. */
    private static void exhaustiveLatin1() throws Throwable {
        for (int a = 0; a < 256; a++) {
            roundTrip(String.valueOf((char) a), tagLatin1);
            for (int b = 0; b < 256; b++) {
                roundTrip(new String(new char[] {(char) a, (char) b}), tagLatin1);
            }
        }
    }

    private static void randomLatin1() throws Throwable {
        Random random = new Random(0x4d33L);
        for (int round = 0; round < 200_000; round++) {
            int size = 3 + random.nextInt(5);
            char[] text = new char[size];
            for (int i = 0; i < size; i++) text[i] = (char) random.nextInt(256);
            roundTrip(new String(text), tagLatin1);
        }
    }

    private static void randomIdent() throws Throwable {
        Random random = new Random(0x1d3eL);
        for (int round = 0; round < 200_000; round++) {
            int size = 8 + random.nextInt(3);
            char[] text = new char[size];
            for (int i = 0; i < size; i++) text[i] = IDENT.charAt(random.nextInt(IDENT.length()));
            roundTrip(new String(text), tagIdent);
        }
        roundTrip("$$$$$$$$$$", tagIdent);
        roundTrip("0000000000", tagIdent);
        roundTrip("computational".substring(0, 10), tagIdent);
        long max = pack("$$$$$$$$$$");
        expect((max & ~tagMask) < (1L << 61), "IDENT x10 maximum fits the 61-bit payload");
    }

    private static void refusals() throws Throwable {
        expect(pack("") == none, "empty text is not immediate");
        expect(pack("abcdefghijk") == none, "11 units exceed IDENT x10");
        expect(pack("Ā") == none, "a unit above 0xFF refuses LATIN1");
        expect(pack("ab€") == none, "a UTF-16-only unit refuses both profiles");
        expect(pack("abcd-fgh") == none, "'-' is outside the IDENT alphabet at length 8");
        expect(pack("abc def gh") == none, "space is outside the IDENT alphabet at length 10");
        int[] supplementary = {0x1F600};
        expect((long) packCodePoints.invoke(supplementary, 0, 1) == none,
                "a supplementary code point refuses both profiles");
    }

    /** Distinct texts never share a packed value (canonical, injective packing). */
    private static void injectivity() throws Throwable {
        HashMap<Long, String> seen = new HashMap<>();
        Random random = new Random(7L);
        for (int round = 0; round < 300_000; round++) {
            String text;
            if (random.nextBoolean()) {
                char[] value = new char[1 + random.nextInt(7)];
                for (int i = 0; i < value.length; i++) value[i] = (char) random.nextInt(256);
                text = new String(value);
            } else {
                char[] value = new char[8 + random.nextInt(3)];
                for (int i = 0; i < value.length; i++) {
                    value[i] = IDENT.charAt(random.nextInt(IDENT.length()));
                }
                text = new String(value);
            }
            long packed = pack(text);
            String previous = seen.putIfAbsent(packed, text);
            expect(previous == null || previous.equals(text), "packed collision " + previous + " / " + text);
        }
    }

    private static void roundTrip(String text, long expectedTag) throws Throwable {
        long packed = pack(text);
        expect(packed != none, "admitted: " + escape(text));
        expect((packed & tagMask) == expectedTag, "profile of " + escape(text));
        expect((int) length.invoke(packed) == text.length(), "length of " + escape(text));
        Object owner = of.invoke(packed);
        char[] copy = new char[text.length()];
        getChars.invoke(owner, 0, text.length(), copy, 0);
        for (int i = 0; i < text.length(); i++) {
            expect((char) unit.invoke(packed, i) == text.charAt(i), "unit " + i + " of " + escape(text));
            expect((char) charAt.invoke(owner, i) == text.charAt(i), "charAt " + i + " of " + escape(text));
            expect(copy[i] == text.charAt(i), "getChars " + i + " of " + escape(text));
        }
        expect((int) javaHash.invoke(owner) == text.hashCode(), "String.hashCode parity " + escape(text));
        if (text.length() > 1) {
            char[] tail = new char[text.length() - 1];
            getChars.invoke(owner, 1, text.length(), tail, 0);
            expect(new String(tail).equals(text.substring(1)), "getChars from 1 of " + escape(text));
        }
        byte[] latin1 = text.getBytes(StandardCharsets.ISO_8859_1);
        expect((long) packCompactBytes.invoke(latin1, 0, latin1.length, (byte) 0) == packed,
                "LATIN1 bytes pack the same value");
        byte[] utf16 = text.getBytes(StandardCharsets.UTF_16LE);
        boolean little = java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN;
        if (little) {
            expect((long) packCompactBytes.invoke(utf16, 0, text.length(), (byte) 1) == packed,
                    "UTF16 bytes pack the same value");
        }
        expect((long) packCodePoints.invoke(text.codePoints().toArray(), 0, text.length()) == packed,
                "code points pack the same value");
        expect((boolean) sameText.invoke(owner, of.invoke(packed)), "sameText on equal packed");
    }

    private static long pack(String text) throws Throwable {
        char[] value = text.toCharArray();
        return (long) packChars.invoke(value, 0, value.length);
    }

    private static void expect(boolean condition, String what) {
        checks++;
        if (!condition) throw new AssertionError("FAILED: " + what);
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder();
        text.chars().forEach(c -> out.append(c < 0x20 || c > 0x7e
                ? String.format("\\u%04x", c) : String.valueOf((char) c)));
        return out.toString();
    }
}
