// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3Utf8SourceMap;

/**
 * Real original-source Java21 contract: a single caller-owned captured payload
 * supplies both immutable M3File identity and its exact UTF8/UTF16 coordinates.
 */
public final class M3CapturedSourceContract {
    private static final URI URI_ID = URI.create("memory:/captured/project/%E2%82%AC.java");
    private static int checks;
    private M3CapturedSourceContract() { }
    private static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }
    private interface Action { void run() throws Exception; }
    private static void refuse(Class<? extends Throwable> expected, Action action) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (expected.isInstance(error)) return;
            throw new AssertionError("wrong captured-source failure", error);
        }
        throw new AssertionError("expected captured-source refusal: " + expected.getName());
    }
    private static ByteBuffer window(byte[] payload, boolean direct, boolean readonly) {
        ByteBuffer raw = direct ? ByteBuffer.allocateDirect(payload.length + 11)
                : ByteBuffer.allocate(payload.length + 11);
        raw.put(new byte[] {9,8,7,6,5});
        raw.put(payload);
        raw.put(new byte[] {4,3,2,1,0,0});
        raw.position(5);
        raw.limit(5 + payload.length);
        return readonly ? raw.asReadOnlyBuffer() : raw;
    }
    private static void roundtrip(String original) throws Exception {
        byte[] captured = original.getBytes(StandardCharsets.UTF_8);
        M3File fromArray = M3File.fromCapturedBytes(
                URI_ID, captured, StandardCharsets.UTF_8, captured.length);
        M3File fromStream = M3File.read(URI_ID, new ByteArrayInputStream(captured),
                StandardCharsets.UTF_8, captured.length);
        check(fromArray.uri().equals(URI_ID), "original URI retained");
        check(fromArray.text().equals(original), "decoded immutable String");
        check(fromArray.utf16Sha256().equals(fromStream.utf16Sha256())
                && fromArray.byteSha256().equals(fromStream.byteSha256()),
                "stream/array ingress exact two-hash parity");
        for (boolean direct : new boolean[]{false,true}) {
            for (boolean readonly : new boolean[]{false,true}) {
                ByteBuffer payload = window(captured, direct, readonly);
                int initialPosition = payload.position(), initialLimit = payload.limit();
                M3File fromWindow = M3File.fromCaptured(
                        URI_ID, payload, StandardCharsets.UTF_8, captured.length);
                check(payload.position() == initialPosition && payload.limit() == initialLimit,
                        "source buffer cursor and limit unchanged");
                check(fromWindow.text().equals(original)
                        && fromWindow.byteSha256().equals(fromArray.byteSha256())
                        && fromWindow.utf16Sha256().equals(fromArray.utf16Sha256()),
                        "direct, read-only and heap window source identity parity");
                var map = M3Utf8SourceMap.verified(fromWindow, payload, captured.length);
                var arrayMap = M3Utf8SourceMap.verified(fromArray, captured, captured.length);
                check(payload.position() == initialPosition && payload.limit() == initialLimit,
                        "UTF-8 mapping also preserves original buffer cursor");
                check(map.byteLength() == captured.length && map.utf16Length() == original.length(),
                        "mapped byte and UTF16 dimensions");
                int utf16 = 0, bytes = 0;
                while (utf16 < original.length()) {
                    check(map.byteOffset(utf16) == bytes && map.utf16Offset(bytes) == utf16
                            && arrayMap.byteOffset(utf16) == bytes, "scalar boundary parity");
                    int scalar = original.codePointAt(utf16);
                    int byteWidth = scalar < 128 ? 1 : scalar < 2048 ? 2 : scalar < 65536 ? 3 : 4;
                    int charWidth = Character.charCount(scalar);
                    for (int interior = 1; interior < byteWidth; interior++) {
                        int bytePosition = bytes + interior;
                        refuse(IllegalArgumentException.class, () -> map.utf16Offset(bytePosition));
                    }
                    if (charWidth == 2) {
                        int charPosition = utf16 + 1;
                        refuse(IllegalArgumentException.class, () -> map.byteOffset(charPosition));
                    }
                    bytes += byteWidth;
                    utf16 += charWidth;
                }
                check(map.byteOffset(utf16) == bytes && map.utf16Offset(bytes) == utf16,
                        "EOF roundtrip for current window only");
            }
        }
        byte[] expected = captured.clone();
        Arrays.fill(captured, (byte) 'x');
        check(fromArray.text().equals(original)
                && fromArray.byteSha256().equals(fromStream.byteSha256())
                && fromArray.utf16Sha256().equals(fromStream.utf16Sha256()),
                "mutating released captured array cannot change snapshot");
        if (expected.length > 0) {
            refuse(IllegalArgumentException.class, () ->
                    M3Utf8SourceMap.verified(fromArray, captured, captured.length));
        }
    }
    private static void boundsAndCharset() throws Exception {
        byte[] utf8 = HexFormat.of().parseHex("41c3a9f09f98800d0a");
        ByteBuffer padded = window(utf8, true, true);
        check(M3File.fromCaptured(URI_ID, padded, StandardCharsets.UTF_8, 9)
                        .text().equals("Aé😀\r\n"), "exact UTF8 byte budget");
        refuse(IOException.class, () -> M3File.fromCaptured(
                URI_ID, padded, StandardCharsets.UTF_8, 8));
        refuse(IllegalArgumentException.class, () -> M3File.fromCaptured(
                URI_ID, padded, StandardCharsets.UTF_8, -1));
        refuse(IllegalArgumentException.class, () -> M3File.fromCaptured(
                URI_ID, padded, StandardCharsets.UTF_8, Integer.MAX_VALUE));
        refuse(IllegalArgumentException.class, () -> M3Utf8SourceMap.verified(
                M3File.fromCaptured(URI_ID, padded, StandardCharsets.UTF_8, 9), padded, 8));
        refuse(NullPointerException.class, () -> M3File.fromCaptured(
                null, padded, StandardCharsets.UTF_8, 9));
        refuse(NullPointerException.class, () -> M3File.fromCaptured(
                URI_ID, (ByteBuffer) null, StandardCharsets.UTF_8, 9));
        refuse(NullPointerException.class, () -> M3File.fromCaptured(
                URI_ID, padded, null, 9));
        refuse(NullPointerException.class, () -> M3File.fromCapturedBytes(
                URI_ID, null, StandardCharsets.UTF_8, 9));
        for (byte[] invalid : new byte[][]{
                {(byte)0xc3, (byte)0x28}, {(byte)0xff},
                {(byte)0xf0,(byte)0x9f,(byte)0x98}
        }) {
            ByteBuffer buffer = window(invalid, true, true);
            refuse(CharacterCodingException.class, () ->
                    M3File.fromCaptured(URI_ID, buffer, StandardCharsets.UTF_8, 16));
            check(buffer.position() == 5 && buffer.limit() == 5 + invalid.length,
                    "decoder refusal still preserves borrowed buffer cursor");
        }
        byte[] utf16 = HexFormat.of().parseHex("feff0041");
        M3File autodetect = M3File.fromCaptured(
                URI_ID, ByteBuffer.wrap(utf16), StandardCharsets.UTF_16, 4);
        M3File explicit = M3File.fromCaptured(
                URI_ID, ByteBuffer.wrap(utf16), StandardCharsets.UTF_16BE, 4);
        check(autodetect.text().equals("A") && explicit.text().equals("\ufeffA"),
                "selected decoder owns BOM policy");
        check(autodetect.byteSha256().equals(explicit.byteSha256())
                && !autodetect.utf16Sha256().equals(explicit.utf16Sha256()),
                "raw identity separated from decoded identity");
        refuse(CharacterCodingException.class, () ->
                M3Utf8SourceMap.verified(autodetect, ByteBuffer.wrap(utf16), 4));
        refuse(CharacterCodingException.class, () ->
                M3File.fromCaptured(URI_ID, ByteBuffer.wrap(new byte[]{(byte)0xe9}),
                        StandardCharsets.US_ASCII, 1));
        ByteBuffer empty = ByteBuffer.allocateDirect(0).asReadOnlyBuffer();
        M3File blank = M3File.fromCaptured(URI_ID, empty, StandardCharsets.UTF_8, 0);
        var map = M3Utf8SourceMap.verified(blank, empty, 0);
        check(blank.length() == 0 && map.byteLength() == 0 && map.byteOffset(0) == 0,
                "empty source admitted only at exactly zero bytes");
    }
    public static void main(String[] args) throws Exception {
        for (String value : new String[]{
                "", "A", "é", "€", "😀", "\ufeffA",
                "line\r\nnext", "é😀\r\n".repeat(260), "ascii".repeat(400)
        }) roundtrip(value);
        Random random = new Random(0x4d3353);
        String[] scalars = {"a","é","€","😀","\ufeff","\n","\r\n","\ud801\udc37"};
        for (int trial = 0; trial < 130; trial++) {
            StringBuilder text = new StringBuilder();
            int count = random.nextInt(180);
            for (int i = 0; i < count; i++) text.append(scalars[random.nextInt(scalars.length)]);
            roundtrip(text.toString());
        }
        boundsAndCharset();
        System.out.println("M3_CAPTURED_SOURCE_PASS checks=" + checks);
    }
}
