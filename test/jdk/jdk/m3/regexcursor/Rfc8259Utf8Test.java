/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Direct, read-only and mapped UTF-8 JSON must agree with the UTF-16 grammar oracle
 * @modules java.base/jdk.m3.regexcursor
 * @run main/othervm Rfc8259Utf8Test
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import jdk.m3.regexcursor.Rfc8259JsonGrammar;

public final class Rfc8259Utf8Test {
    private static long assertions;
    private static final Random RANDOM = new Random(0x82592026L);
    private Rfc8259Utf8Test() { }

    private static void yes(boolean valid) {
        if (!valid) throw new AssertionError("failed after " + assertions + " assertions");
        assertions++;
    }

    private static void compare(String source, boolean accepted) {
        boolean authoritative = Rfc8259JsonGrammar.acceptsStreaming(source);
        yes(authoritative == accepted);
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        ByteBuffer heap = ByteBuffer.wrap(bytes);
        boolean actual = Rfc8259JsonGrammar.acceptsUtf8(heap);
        if (actual != authoritative) {
            throw new AssertionError("DIFF source=" + source + " actual=" + actual
                    + " oracle=" + authoritative);
        }
        yes(actual == authoritative);
        yes(heap.position() == 0 && heap.limit() == bytes.length);
        yes(Rfc8259JsonGrammar.acceptsUtf8(heap.asReadOnlyBuffer()) == authoritative);
        ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length + 9);
        direct.position(5);
        direct.put(bytes);
        direct.limit(5 + bytes.length);
        direct.position(5);
        direct.mark();
        yes(Rfc8259JsonGrammar.acceptsUtf8(direct) == authoritative);
        yes(direct.position() == 5 && direct.limit() == 5 + bytes.length);
        direct.reset();
        yes(direct.position() == 5);
        ByteBuffer sliced = direct.slice().asReadOnlyBuffer();
        yes(Rfc8259JsonGrammar.acceptsUtf8(sliced) == authoritative);
    }

    private static void invalidUtf8(int... units) {
        byte[] source = new byte[units.length];
        for (int i = 0; i < source.length; i++) source[i] = (byte) units[i];
        yes(!Rfc8259JsonGrammar.acceptsUtf8(ByteBuffer.wrap(source)));
        ByteBuffer direct = ByteBuffer.allocateDirect(source.length);
        direct.put(source).flip();
        yes(!Rfc8259JsonGrammar.acceptsUtf8(direct.asReadOnlyBuffer()));
    }

    private static String validString() {
        String[] atoms = { "A", "\u00e9", "\u4e2d", "\ud834\udd1e",
                "\ud83d\ude00", "\\n", "\\u0000",
                "\\uD800", "\\uDE00", "\\\"", "\\\\", "/", "\u2028", "z" };
        StringBuilder word = new StringBuilder("\"");
        for (int i = RANDOM.nextInt(12); i > 0; i--) {
            word.append(atoms[RANDOM.nextInt(atoms.length)]);
        }
        return word.append('"').toString();
    }

    private static String validNumber() {
        StringBuilder number = new StringBuilder();
        if (RANDOM.nextBoolean()) number.append('-');
        if (RANDOM.nextBoolean()) number.append('0');
        else {
            number.append((char) ('1' + RANDOM.nextInt(9)));
            for (int n = RANDOM.nextInt(7); n > 0; n--) {
                number.append((char) ('0' + RANDOM.nextInt(10)));
            }
        }
        if (RANDOM.nextInt(3) == 0) {
            number.append('.');
            for (int n = 1 + RANDOM.nextInt(8); n > 0; n--) {
                number.append((char) ('0' + RANDOM.nextInt(10)));
            }
        }
        if (RANDOM.nextInt(3) == 0) {
            number.append(RANDOM.nextBoolean() ? 'E' : 'e');
            if (RANDOM.nextBoolean()) number.append(RANDOM.nextBoolean() ? '-' : '+');
            for (int n = 1 + RANDOM.nextInt(6); n > 0; n--) {
                number.append((char) ('0' + RANDOM.nextInt(10)));
            }
        }
        return number.toString();
    }

    private static String validJson(int depth) {
        int choice = depth == 0 ? RANDOM.nextInt(5) : RANDOM.nextInt(7);
        return switch (choice) {
            case 0 -> validString();
            case 1 -> validNumber();
            case 2 -> "null";
            case 3 -> "true";
            case 4 -> "false";
            case 5 -> {
                StringBuilder array = new StringBuilder("[");
                for (int i = 0, n = RANDOM.nextInt(4); i < n; i++) {
                    if (i > 0) array.append(',');
                    array.append(validJson(depth - 1));
                }
                yield array.append(']').toString();
            }
            default -> {
                StringBuilder obj = new StringBuilder("{");
                for (int i = 0, n = RANDOM.nextInt(4); i < n; i++) {
                    if (i > 0) obj.append(',');
                    obj.append(validString()).append(':').append(validJson(depth - 1));
                }
                yield obj.append('}').toString();
            }
        };
    }

    /** Exhaustive two-byte inputs plus longer random malformed/valid scalar tests. */
    private static void utf8Differential() {
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        byte[] buffer = new byte[6];
        buffer[0] = '"';
        for (int first = 0; first < 256; first++) {
            for (int second = 0; second < 256; second++) {
                buffer[1] = (byte) first;
                buffer[2] = (byte) second;
                buffer[3] = '"';
                checkDecoded(buffer, 4, decoder);
            }
        }
        for (int trial = 0; trial < 40_000; trial++) {
            int count = 1 + RANDOM.nextInt(4);
            for (int index = 0; index < count; index++) {
                buffer[index + 1] = (byte) RANDOM.nextInt(256);
            }
            buffer[count + 1] = '"';
            checkDecoded(buffer, count + 2, decoder);
        }
    }

    private static void checkDecoded(byte[] source, int length,
            java.nio.charset.CharsetDecoder decoder) {
        ByteBuffer bytes = ByteBuffer.wrap(source, 0, length);
        boolean accepted = Rfc8259JsonGrammar.acceptsUtf8(bytes);
        decoder.reset();
        try {
            String decoded = decoder.decode(ByteBuffer.wrap(source, 0, length)).toString();
            boolean reference = Rfc8259JsonGrammar.acceptsStreaming(decoded);
            yes(accepted == reference);
        } catch (CharacterCodingException malformed) {
            yes(!accepted);
        }
    }

    private static void mappedJson() throws IOException {
        Path temporary = Files.createTempFile("m3-rfc8259-utf8-", ".json");
        temporary.toFile().deleteOnExit();
        try {
            byte[] payload = "[\"\u4e2d\",\"\ud83d\ude00\",123e-9,true,null]"
                    .getBytes(StandardCharsets.UTF_8);
            Files.write(temporary, payload);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.READ)) {
                MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY,
                        0, payload.length);
                mapped.position(0);
                mapped.mark();
                yes(Rfc8259JsonGrammar.acceptsUtf8(mapped));
                mapped.reset();
                yes(mapped.position() == 0 && mapped.limit() == payload.length);
            }
        } finally {
            // Windows retains mapped-file handles until the buffer is reclaimed.
            // Best-effort early cleanup; deleteOnExit covers that platform.
            try { Files.deleteIfExists(temporary); }
            catch (IOException mappedInUse) { /* cleanup scheduled at process exit */ }
        }
    }

    public static void main(String[] args) throws Exception {
        String[] good = {"null", " true ", "0", "-0", "3.14", "1E-22", "[]", "{}",
                "{\"\u00e9\":[true, false, null]}", "\"\ud83d\ude00\"",
                "\"\\uD800\"", "[ 0, 2 , 3 ]"};
        String[] bad = {"", " ", "+1", "01", "-01", "1.", "1e", "1e+", ".7",
                "[1,]", "[1 2]", "{foo:1}", "{\"a\":}", "truefalse", "Null",
                "\"bad\\x\"", "\"bad\\u123\"", "\"bad\n\"",
                "[1] []", "[\"unterminated]", "{\"key\":true,}"};
        for (String sample : good) compare(sample, true);
        for (String sample : bad) compare(sample, false);
        for (int i = 0; i < 12_000; i++) compare(validJson(4), true);
        for (int i = 0; i < 5_000; i++) {
            String base = validJson(4);
            String corrupted = base + (i % 2 == 0 ? "\n!" : "\u0000");
            compare(corrupted, false);
        }
        for (int[] x : new int[][] {
                {0x22,0xc0,0xaf,0x22}, {0x22,0xc1,0xbf,0x22},
                {0x22,0xe0,0x80,0x80,0x22}, {0x22,0xed,0xa0,0x80,0x22},
                {0x22,0xf0,0x80,0x80,0x80,0x22},
                {0x22,0xf4,0x90,0x80,0x80,0x22},
                {0x22,0xf5,0x80,0x80,0x80,0x22},
                {0x22,0xe2,0x82}, {0x22,0xf0,0x9f,0x92},
                {0xef,0xbb,0xbf,0x6e,0x75,0x6c,0x6c},
                {0x22,0x80,0x22}, {0x22,0xff,0x22}
        }) invalidUtf8(x);
        utf8Differential();
        mappedJson();
        try { Rfc8259JsonGrammar.acceptsUtf8(null); throw new AssertionError(); }
        catch (NullPointerException expected) { assertions++; }
        System.out.println("Rfc8259Utf8Test PASS assertions=" + assertions);
    }
}
