// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3Utf8SourceMap;

/** Scalar-boundary roundtrip and failure cases over the original M3File API. */
public final class M3Utf8SourceMapContract {
    private static int checks;
    private static void check(boolean valid, String label) {
        checks++;
        if (!valid) throw new AssertionError(label);
    }
    private interface Throwing { void run() throws Exception; }
    private static void reject(Class<? extends Throwable> kind, Throwing operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (kind.isInstance(failure)) return;
            throw new AssertionError("wrong refusal", failure);
        }
        throw new AssertionError("expected refusal: " + kind.getName());
    }
    private static void trial(String original) throws Exception {
        byte[] captured = original.getBytes(StandardCharsets.UTF_8);
        M3File source = M3File.read(URI.create("memory:/captured"),
                new ByteArrayInputStream(captured), StandardCharsets.UTF_8, captured.length);
        M3Utf8SourceMap map = M3Utf8SourceMap.verified(source, captured, captured.length);
        check(map.source() == source && map.utf16Length() == original.length()
                && map.byteLength() == captured.length, "same original source generation");
        check(map.checkpointCount() >= 1, "nonempty checkpoint lane");
        int chars = 0, bytes = 0;
        while (chars < original.length()) {
            check(map.byteOffset(chars) == bytes, "scalar start: char to byte");
            check(map.utf16Offset(bytes) == chars, "scalar start: byte to char");
            int scalar = original.codePointAt(chars);
            int byteWidth = scalar <= 0x7f ? 1 : scalar <= 0x7ff ? 2 : scalar <= 0xffff ? 3 : 4;
            int charWidth = Character.charCount(scalar);
            for (int b = 1; b < byteWidth; b++) {
                int interior = bytes + b;
                reject(IllegalArgumentException.class, () -> map.utf16Offset(interior));
            }
            if (charWidth == 2) {
                int interior = chars + 1;
                reject(IllegalArgumentException.class, () -> map.byteOffset(interior));
            }
            chars += charWidth;
            bytes += byteWidth;
        }
        check(map.byteOffset(chars) == bytes && map.utf16Offset(bytes) == chars,
                "exact source EOF");
        reject(IndexOutOfBoundsException.class, () -> map.byteOffset(-1));
        reject(IndexOutOfBoundsException.class, () -> map.byteOffset(original.length() + 1));
        reject(IndexOutOfBoundsException.class, () -> map.utf16Offset(-1));
        reject(IndexOutOfBoundsException.class, () -> map.utf16Offset(map.byteLength() + 1));
        reject(IllegalArgumentException.class, () ->
                M3Utf8SourceMap.verified(source, captured, captured.length - 1));
        if (captured.length > 0) {
            byte[] modified = captured.clone();
            modified[0] ^= 1;
            reject(IllegalArgumentException.class, () ->
                    M3Utf8SourceMap.verified(source, modified, modified.length));
        }
        reject(IllegalArgumentException.class, () ->
                M3Utf8SourceMap.verified(M3File.of(source.uri(), source.text()),
                        captured, captured.length));
        java.util.Arrays.fill(captured, (byte) 42);
        check(map.byteOffset(0) == 0 && map.utf16Offset(map.byteLength()) == chars,
                "borrowed raw array not retained");
    }
    public static void main(String[] args) throws Exception {
        for (String item : new String[] { "", "a", "é", "€", "\ud83d\ude00",
                "\ufeffa", "\r\n", "a".repeat(130), "\ud83d\ude00".repeat(600),
                "aé€\ud83d\ude00\r\n".repeat(280) }) trial(item);
        Random random = new Random(14335);
        String[] units = {"a", "e", "é", "€", "\ud83d\ude00", "\ufeff", "\n", "\r\n",
                "\ud801\udc37"};
        for (int n = 0; n < 120; n++) {
            StringBuilder text = new StringBuilder();
            for (int i = 0, count = random.nextInt(1000); i < count; i++) {
                text.append(units[random.nextInt(units.length)]);
            }
            trial(text.toString());
        }
        M3File known = M3File.read(URI.create("memory:/bad"),
                new ByteArrayInputStream(new byte[] {111, 107}), StandardCharsets.UTF_8, 2);
        reject(IllegalArgumentException.class, () ->
                M3Utf8SourceMap.verified(known, new byte[] {(byte) 0xc0, (byte) 0xaf}, 2));
        System.out.println("M3_UTF8_SOURCE_MAP_PASS checks=" + checks);
    }
}
