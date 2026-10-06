/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary M3-backed String.intern preserves JDK identity and collision semantics
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringInternTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringInternTest
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class M3StringInternTest {
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static final Constructor<String> RAW_STRING;
    private static final Method UTF16_TO_BYTES;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_OWNER = m3.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
            RAW_STRING = String.class.getDeclaredConstructor(byte[].class, byte.class);
            RAW_STRING.setAccessible(true);
            Class<?> utf16 = Class.forName("java.lang.StringUTF16");
            UTF16_TO_BYTES =
                    utf16.getDeclaredMethod("toBytes", char[].class, int.class, int.class);
            UTF16_TO_BYTES.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        String composed = String.join("", "m3", "-", "intern");
        String scalar = new String("m3-intern".toCharArray());
        check(composed != scalar, "distinct wrappers before intern");
        check(composed.intern() == scalar.intern(), "composed/scalar intern identity");
        check(composed.intern() == composed.intern(), "stable repeated intern");

        String source = String.join("", "xx", "range", "yy");
        String range = source.substring(2, 7);
        String rangeScalar = new String("range".toCharArray());
        check(range.intern() == rangeScalar.intern(), "range/scalar intern identity");

        String unicode = String.join("", "\u03b2", "\ud83d", "\ude42", "\u00ff");
        String unicodeScalar = new String("\u03b2\ud83d\ude42\u00ff".toCharArray());
        check(unicode.intern() == unicodeScalar.intern(), "Unicode seam intern identity");

        // Java-hash collisions must remain distinct interned strings.
        String collisionLeft = String.join("", "A", "a");
        String collisionRight = String.join("", "B", "B");
        check(collisionLeft.hashCode() == collisionRight.hashCode(), "known collision");
        check(collisionLeft.intern() != collisionRight.intern(), "collision intern distinction");
        check(collisionLeft.intern().equals("Aa"), "collision left content");
        check(collisionRight.intern().equals("BB"), "collision right content");

        String unitValue = String.valueOf('x');
        String unitScalar = new String(new char[] {'x'});
        unitValue.length();
        unitScalar.length();
        Object unitValueM3 = STRING_M3.get(unitValue);
        Object unitScalarM3 = STRING_M3.get(unitScalar);
        check(unitValueM3 != null && unitScalarM3 != null, "unit Strings admitted to M3");
        check(M3_OWNER.get(unitValueM3) == M3_OWNER.get(unitScalarM3),
                "valueOf/scalar canonical unit owner identity");

        String wideValue = String.valueOf('\u0100');
        String wideScalar = new String(new char[] {'\u0100'});
        wideValue.length();
        wideScalar.length();
        check(M3_OWNER.get(STRING_M3.get(wideValue)) == M3_OWNER.get(STRING_M3.get(wideScalar)),
                "wide valueOf/scalar canonical unit owner identity");

        String deletedToEmpty = String.join("", "x").replace("x", "");
        check(deletedToEmpty.isEmpty(), "canonical delete-to-empty content");
        check(deletedToEmpty.intern() == "".intern(), "canonical delete-to-empty intern identity");

        String continuedToEmpty = String.join("", "\\\n").translateEscapes();
        check(continuedToEmpty.isEmpty(), "canonical continuation-to-empty content");
        check(continuedToEmpty.intern() == "".intern(),
                "canonical continuation-to-empty intern identity");

        char[] mutableChars = new char[] {'a', 'b', '\u0100', 'c'};
        String charSnapshot = new String(mutableChars);
        String charSnapshotPeer = new String(new char[] {'a', 'b', '\u0100', 'c'});
        charSnapshot.length();
        charSnapshotPeer.length();
        check(M3_OWNER.get(STRING_M3.get(charSnapshot))
                        == M3_OWNER.get(STRING_M3.get(charSnapshotPeer)),
                "equal char[] constructors share canonical owner");
        mutableChars[0] = 'z';
        mutableChars[2] = 'Q';
        check(charSnapshot.equals("ab\u0100c"), "char[] constructor snapshots mutable input");

        char[] rangedChars = new char[] {'x', 'a', 'b', '\u0100', 'c', 'y'};
        String ranged = new String(rangedChars, 1, 4);
        check(ranged.equals("ab\u0100c"), "char[] range constructor content");
        check(M3_OWNER.get(STRING_M3.get(ranged))
                        == M3_OWNER.get(STRING_M3.get(charSnapshot)),
                "char[] range shares canonical owner");
        rangedChars[1] = 'q';
        check(ranged.equals("ab\u0100c"), "char[] range snapshots mutable input");

        byte[] latinBytes = new byte[] {'a', (byte) 0xff, 'b'};
        String latinByteString = new String(latinBytes, StandardCharsets.ISO_8859_1);
        String latinBytePeer = new String(new char[] {'a', '\u00ff', 'b'});
        check(M3_OWNER.get(STRING_M3.get(latinByteString))
                        == M3_OWNER.get(STRING_M3.get(latinBytePeer)),
                "ISO-8859-1 byte constructor shares canonical owner");
        latinBytes[0] = 'z';
        check(latinByteString.equals("a\u00ffb"), "ISO-8859-1 byte constructor snapshots input");

        byte[] utf8Ascii = new byte[] {'A', 'S', 'C', 'I', 'I'};
        String utf8AsciiString = new String(utf8Ascii, StandardCharsets.UTF_8);
        String utf8AsciiPeer = new String(new char[] {'A', 'S', 'C', 'I', 'I'});
        check(M3_OWNER.get(STRING_M3.get(utf8AsciiString))
                        == M3_OWNER.get(STRING_M3.get(utf8AsciiPeer)),
                "ASCII UTF-8 byte constructor shares canonical owner");
        utf8Ascii[0] = 'x';
        check(utf8AsciiString.equals("ASCII"), "ASCII UTF-8 byte constructor snapshots input");

        byte[] asciiBytes = new byte[] {'J', 'D', 'K'};
        String asciiString = new String(asciiBytes, StandardCharsets.US_ASCII);
        String asciiPeer = new String(new char[] {'J', 'D', 'K'});
        check(M3_OWNER.get(STRING_M3.get(asciiString))
                        == M3_OWNER.get(STRING_M3.get(asciiPeer)),
                "US-ASCII byte constructor shares canonical owner");

        String malformedAscii =
                new String(new byte[] {(byte) 0x80}, StandardCharsets.US_ASCII);
        check(malformedAscii.equals("\ufffd"), "malformed ASCII remains decoder-owned");

        StringBuilder builder = new StringBuilder("ab\u0100c");
        String builderSnapshot = new String(builder);
        String builderPeer = new String(new char[] {'a', 'b', '\u0100', 'c'});
        check(M3_OWNER.get(STRING_M3.get(builderSnapshot))
                        == M3_OWNER.get(STRING_M3.get(builderPeer)),
                "StringBuilder constructor shares canonical owner");
        builder.setCharAt(0, 'z');
        builder.setCharAt(2, 'Q');
        check(builderSnapshot.equals("ab\u0100c"),
                "StringBuilder constructor snapshots mutable backing");

        StringBuilder latinBuilder = new StringBuilder("builder-ascii");
        String latinBuilderSnapshot = new String(latinBuilder);
        String latinBuilderPeer = new String("builder-ascii".toCharArray());
        check(M3_OWNER.get(STRING_M3.get(latinBuilderSnapshot))
                        == M3_OWNER.get(STRING_M3.get(latinBuilderPeer)),
                "Latin1 StringBuilder constructor shares canonical owner");

        int[] codePoints = new int[] {'a', 0x1f642, 0xff, 0x100, 'b'};
        String codePointString = new String(codePoints, 0, codePoints.length);
        String codePointPeer =
                new String(new char[] {'a', '\ud83d', '\ude42', '\u00ff', '\u0100', 'b'});
        check(M3_OWNER.get(STRING_M3.get(codePointString))
                        == M3_OWNER.get(STRING_M3.get(codePointPeer)),
                "code-point constructor shares canonical owner");
        codePoints[0] = 'z';
        codePoints[1] = 'q';
        check(codePointString.equals("a\ud83d\ude42\u00ff\u0100b"),
                "code-point constructor snapshots mutable input");

        int[] rangedCodePoints = new int[] {'x', 'a', 0x1f642, 0xff, 'y'};
        String rangedCodePointString = new String(rangedCodePoints, 1, 3);
        String rangedCodePointPeer = new String(new char[] {'a', '\ud83d', '\ude42', '\u00ff'});
        check(M3_OWNER.get(STRING_M3.get(rangedCodePointString))
                        == M3_OWNER.get(STRING_M3.get(rangedCodePointPeer)),
                "code-point range shares canonical owner");
        rangedCodePoints[1] = 'q';
        check(rangedCodePointString.equals("a\ud83d\ude42\u00ff"),
                "code-point range snapshots mutable input");

        try {
            new String(new int[] {Character.MAX_CODE_POINT + 1}, 0, 1);
            throw new AssertionError("invalid code point did not throw");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().equals(Integer.toString(Character.MAX_CODE_POINT + 1)),
                    "invalid code-point message parity");
        }

        byte[] forcedUtf16Ascii =
                (byte[]) UTF16_TO_BYTES.invoke(null, new char[] {'A', 'B'}, 0, 2);
        String forcedUtf16 = RAW_STRING.newInstance(forcedUtf16Ascii, (byte) 1);
        String normalizedAsciiPeer = new String(new char[] {'A', 'B'});
        check(M3_OWNER.get(STRING_M3.get(forcedUtf16))
                        == M3_OWNER.get(STRING_M3.get(normalizedAsciiPeer)),
                "incoming UTF16 ASCII normalizes to canonical owner");
        forcedUtf16Ascii[0] ^= 1;
        check(forcedUtf16.equals("AB"), "raw compact input snapshots into canonical owner");

        String longComposed = String.join(
                "",
                "left-".repeat(300),
                "\ud83d",
                "\ude42",
                "-middle-".repeat(220),
                "\u0100",
                "-right".repeat(280));
        String longScalar = new String(longComposed.toCharArray());
        check(longComposed.hashCode() == longScalar.hashCode(),
                "long composed VM hash parity");
        check(longComposed.intern() == longScalar.intern(),
                "long composed VM intern identity");

        int longBegin = 257;
        int longEnd = longComposed.length() - 193;
        String longRange = longComposed.substring(longBegin, longEnd);
        String longRangeScalar = new String(
                longComposed.substring(longBegin, longEnd).toCharArray());
        check(longRange.hashCode() == longRangeScalar.hashCode(),
                "long M3 range VM hash parity");
        check(longRange.intern() == longRangeScalar.intern(),
                "long M3 range VM intern identity");

        String empty = new String(new char[0]);
        check(empty.intern() == "".intern(), "empty intern identity");

        System.out.println("M3_STRING_INTERN_PASS|checks=" + checks);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
