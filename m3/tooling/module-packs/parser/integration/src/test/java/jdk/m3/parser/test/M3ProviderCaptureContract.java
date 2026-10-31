// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import jdk.m3.parser.M3Utf8SourceMap;

/**
 * Executable integration of the ORIGINAL com.m3.file filesystem provider with
 * the ORIGINAL jdk.m3.parser.M3File immutable source owner. Compile as an
 * external classpath consumer: neither production module requires the other.
 */
public final class M3ProviderCaptureContract {
    private static int checks;
    private M3ProviderCaptureContract() { }

    private static void check(boolean yes, String description) {
        checks++;
        if (!yes) throw new AssertionError(description);
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void refuse(Class<? extends Throwable> expected, Checked code, String why) {
        checks++;
        try { code.run(); }
        catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError(why + " wrong exception", failure);
        }
        throw new AssertionError(why + " unexpectedly succeeded");
    }

    private static byte[] sha256(byte[] bytes) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    private static ByteBuffer paddedDirect(byte[] original) {
        ByteBuffer full = ByteBuffer.allocateDirect(original.length + 8);
        full.put(new byte[]{6, 5, 4, 3});
        full.put(original);
        full.put(new byte[]{2, 1, 0, 9});
        full.flip();
        full.position(4);
        full.limit(4 + original.length);
        return full.asReadOnlyBuffer();
    }

    private static void exercise(Path directory, String value, int ordinal) throws Exception {
        Path file = directory.resolve("observed-" + ordinal + ".java");
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        Files.write(file, encoded);

        // This is the only provider read for the initial source generation.
        com.m3.file.M3File provider = com.m3.file.M3File.of(file);
        byte[] captured = provider.readAllBytes(encoded.length);
        check(Arrays.equals(captured, encoded), "provider captured original bytes");
        URI uri = file.toUri();
        jdk.m3.parser.M3File immutable = jdk.m3.parser.M3File.fromCapturedBytes(
                uri, captured, StandardCharsets.UTF_8, captured.length);
        check(immutable.uri().equals(uri) && immutable.text().equals(value),
                "original provider source URI and strict decoded content");
        check(immutable.byteSha256().orElseThrow().equals(
                HexFormat.of().formatHex(sha256(encoded))), "exact original raw-byte SHA");
        M3Utf8SourceMap mapping = M3Utf8SourceMap.verified(immutable, captured, captured.length);
        check(mapping.source() == immutable && mapping.byteLength() == captured.length
                && mapping.utf16Length() == value.length(), "same captured generation");
        int chars = 0, bytes = 0;
        while (chars < value.length()) {
            check(mapping.byteOffset(chars) == bytes && mapping.utf16Offset(bytes) == chars,
                    "exact original Unicode scalar coordinates");
            int scalar = value.codePointAt(chars);
            int byteWidth = scalar < 128 ? 1 : scalar < 2048 ? 2 : scalar < 65536 ? 3 : 4;
            int charWidth = Character.charCount(scalar);
            if (charWidth == 2) {
                int interior = chars + 1;
                refuse(IllegalArgumentException.class, () -> mapping.byteOffset(interior),
                        "UTF-16 surrogate interior must refuse");
            }
            for (int interior = 1; interior < byteWidth; interior++) {
                int position = bytes + interior;
                refuse(IllegalArgumentException.class, () -> mapping.utf16Offset(position),
                        "UTF-8 byte interior must refuse");
            }
            chars += charWidth;
            bytes += byteWidth;
        }
        check(mapping.byteOffset(chars) == bytes && mapping.utf16Offset(bytes) == chars,
                "UTF-8 and UTF-16 EOF are exact");

        // Simulate mapped/direct/read-only NIO input without first re-reading provider.
        ByteBuffer active = paddedDirect(captured);
        int originalPosition = active.position(), originalLimit = active.limit();
        jdk.m3.parser.M3File direct = jdk.m3.parser.M3File.fromCaptured(
                uri, active, StandardCharsets.UTF_8, captured.length);
        M3Utf8SourceMap directMap = M3Utf8SourceMap.verified(direct, active, captured.length);
        check(active.isDirect() && active.isReadOnly(), "direct read-only source admitted");
        check(active.position() == originalPosition && active.limit() == originalLimit,
                "neither parser nor mapper moved borrowed NIO cursor");
        check(direct.text().equals(immutable.text())
                && direct.byteSha256().equals(immutable.byteSha256())
                && direct.utf16Sha256().equals(immutable.utf16Sha256())
                && directMap.byteOffset(direct.length()) == captured.length,
                "array and direct-buffer source identities and coordinates agree");

        // Change the physical file after the capture: old parser generation stays valid.
        Files.writeString(file, "later file generation #" + ordinal, StandardCharsets.UTF_8);
        check(immutable.text().equals(value) && direct.text().equals(value),
                "retained parser generations do not reread changed files");
        check(mapping.utf16Offset(bytes) == value.length(),
                "mapping independent of later filesystem changes");
        byte[] later = provider.readAllBytes(128);
        check(!Arrays.equals(later, encoded) || ordinal == -1,
                "explicit second provider read observes later generation");

        // Borrowed bytes are NOT retained: callers can release or repurpose them.
        Arrays.fill(captured, (byte) 0x55);
        check(immutable.text().equals(value) && direct.text().equals(value),
                "borrowed byte array is not part of retained M3File state");
        if (captured.length > 0) {
            refuse(IllegalArgumentException.class,
                    () -> M3Utf8SourceMap.verified(immutable, captured, captured.length),
                    "altered capture must not match retained SHA");
        }
    }

    private static void refusal(Path directory) throws Exception {
        Path file = directory.resolve("malformed.raw");
        byte[] malformed = {(byte)0xc3, (byte)0x28};
        Files.write(file, malformed);
        com.m3.file.M3File provider = com.m3.file.M3File.of(file);
        byte[] captured = provider.readAllBytes(2);
        refuse(CharacterCodingException.class,
                () -> jdk.m3.parser.M3File.fromCapturedBytes(
                        file.toUri(), captured, StandardCharsets.UTF_8, 2),
                "strict malformed UTF-8 refusal");
        refuse(IOException.class, () -> provider.readAllBytes(1),
                "original provider enforces byte budget");
        refuse(IOException.class,
                () -> jdk.m3.parser.M3File.fromCapturedBytes(
                        file.toUri(), captured, StandardCharsets.UTF_8, 1),
                "parser also enforces byte budget without provider replay");
        Path link = directory.resolve("source-link.java");
        try {
            Files.createSymbolicLink(link, file.getFileName());
            refuse(IOException.class, () -> com.m3.file.M3File.of(link).readAllBytes(32),
                    "original provider must not follow symbolic links");
        } catch (UnsupportedOperationException | SecurityException unsupported) {
            System.out.println("M3_PROVIDER_SYMLINK_SKIPPED runtime does not support symlinks");
        }
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-original-provider-parser-");
        try {
            String[] fixed = {"", "a", "é", "€", "😀", "\ufeffA", "a\r\nb\n",
                    "é😀\r\n".repeat(150), "abc".repeat(200)};
            for (int i = 0; i < fixed.length; i++) exercise(directory, fixed[i], i);
            Random random = new Random(0x4d3350524f564944L);
            String[] scalars = {"a", "é", "€", "😀", "\r\n", "\ufeff", "\ud801\udc37"};
            for (int i = 0; i < 120; i++) {
                StringBuilder text = new StringBuilder();
                int count = random.nextInt(150);
                for (int j = 0; j < count; j++) text.append(scalars[random.nextInt(scalars.length)]);
                exercise(directory, text.toString(), fixed.length + i);
            }
            refusal(directory);
            System.out.println("M3_PROVIDER_CAPTURE_BRIDGE_PASS checks=" + checks);
        } finally {
            try (var listing = Files.list(directory)) {
                for (Path path : listing.toList()) Files.deleteIfExists(path);
            }
            Files.deleteIfExists(directory);
        }
    }
}
