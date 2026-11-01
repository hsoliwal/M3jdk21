/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable source identity and its single String payload.
 *
 * <p>On M3JDK ordinary String uses the existing internal M3String owner. This
 * class never creates an alternative string store. UTF-16 source coordinates
 * and file-byte identity are separate. Reading a file snapshots the bytes
 * actually read; it does not claim an atomic filesystem version or watch the
 * file for later changes. A retained snapshot remains valid after an edit.
 */
public final class M3File implements CharSequence {
    private final URI uri;
    private final String text;
    private final String utf16Sha256;
    private final String byteSha256;

    private M3File(URI uri, String text, String byteSha256) {
        this.uri = Objects.requireNonNull(uri, "uri");
        this.text = Objects.requireNonNull(text, "text");
        this.utf16Sha256 = hashUtf16(text);
        this.byteSha256 = byteSha256;
    }

    /** Retain an immutable String by identity, or snapshot another CharSequence once. */
    public static M3File of(URI uri, CharSequence text) {
        Objects.requireNonNull(text, "text");
        return new M3File(uri, text instanceof String value ? value : text.toString(), null);
    }

    /**
     * Read at most maxBytes bytes, refusing malformed/unmappable input and oversized files.
     * The selected charset defines BOM handling. No replacement characters are introduced
     * by error recovery. The stream is closed on success and failure.
     */
    public static M3File read(Path path, Charset charset, int maxBytes) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(charset, "charset");
        if (maxBytes < 0 || maxBytes > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("maxBytes outside supported array bounds");
        }
        byte[] bytes;
        try (InputStream input = Files.newInputStream(path)) {
            bytes = input.readNBytes(maxBytes + 1);
        }
        if (bytes.length > maxBytes) {
            throw new IOException("source exceeds maxBytes=" + maxBytes + ": " + path);
        }
        String text = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
        String byteHash = HexFormat.of().formatHex(sha256().digest(bytes));
        return new M3File(path.toAbsolutePath().normalize().toUri(), text, byteHash);
    }

    public URI uri() { return uri; }
    public String text() { return text; }

    /** SHA-256 of exact UTF-16 big-endian code units, including unpaired surrogates. */
    public String utf16Sha256() { return utf16Sha256; }

    /** Raw byte SHA-256 when created by read; absent for character-only sources. */
    public Optional<String> byteSha256() { return Optional.ofNullable(byteSha256); }

    @Override public int length() { return text.length(); }
    @Override public char charAt(int index) { return text.charAt(index); }

    /** An explicit String range projection; M3JDK may share its canonical owner. */
    @Override public String subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, text.length());
        return text.substring(start, end);
    }

    @Override public String toString() { return text; }

    private static String hashUtf16(String value) {
        MessageDigest digest = sha256();
        byte[] pair = new byte[2];
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            pair[0] = (byte) (unit >>> 8);
            pair[1] = (byte) unit;
            digest.update(pair);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("required SHA-256 provider unavailable", impossible);
        }
    }
}
