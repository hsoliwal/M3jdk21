// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import jdk.m3.parser.M3File;

/** Byte-exact streaming M3File admission oracle; no eager second input byte[]. */
public final class M3FileStreamingContract {
    private static int checks;
    private M3FileStreamingContract() { }

    private static void ok(boolean yes, String reason) {
        checks++;
        if (!yes) throw new AssertionError(reason);
    }

    private static void refuses(Checked work, String reason) {
        checks++;
        try { work.execute(); }
        catch (IOException expected) { return; }
        throw new AssertionError("missing input decoding/budget refusal: " + reason);
    }
    @FunctionalInterface private interface Checked { void execute() throws IOException; }

    private static String utf16Sha(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < text.length(); i++) {
            char unit = text.charAt(i);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("one absent directory required");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(root)) throw new IOException("directory exists");
        Files.createDirectories(root);

        String payload = "a\r\n\u03bb\uD83D\uDE00\n" + "0123456789".repeat(4097);
        byte[] encoded = payload.getBytes(StandardCharsets.UTF_8);
        Path file = root.resolve("source.json");
        Files.write(file, encoded);
        M3File source = M3File.read(file, StandardCharsets.UTF_8, encoded.length);
        ok(payload.equals(source.text()), "complete strict UTF-8 payload");
        ok(source.length() == payload.length(), "original UTF-16 geometry");
        ok(source.utf16Sha256().equals(utf16Sha(payload)), "big-endian UTF-16 SHA exact");
        ok(source.byteSha256().orElseThrow().equals(HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(encoded))), "exact original byte SHA");
        ok(source.uri().equals(file.toUri()), "source URI identity");
        refuses(() -> M3File.read(file, StandardCharsets.UTF_8, encoded.length - 1),
                "one byte beyond exact budget");
        refuses(() -> M3File.read(file, StandardCharsets.UTF_8, 0), "nonempty zero budget");
        Path empty = root.resolve("empty");
        Files.write(empty, new byte[0]);
        ok(M3File.read(empty, StandardCharsets.UTF_8, 0).length() == 0,
                "zero byte file admitted under exact zero limit");
        Path corrupt = root.resolve("corrupt");
        Files.write(corrupt, new byte[] {(byte) 0xC3, 0x28});
        refuses(() -> M3File.read(corrupt, StandardCharsets.UTF_8, 2),
                "malformed UTF-8 is refused without replacement");
        String original = new String(payload);
        M3File owned = M3File.of(URI.create("memory:/immutable"), original);
        ok(owned.text() == original, "immutable String reference reused exactly");
        StringBuilder mutable = new StringBuilder("hello");
        M3File copied = M3File.of(URI.create("memory:/mutable"), mutable);
        mutable.setLength(0);
        ok(Objects.equals("hello", copied.text()), "mutable input snapshotted once");
        ok(copied.byteSha256().isEmpty(), "no invented byte hash for character input");
        System.out.println("M3_FILE_STREAMING_PASS checks=" + checks);
    }
}
