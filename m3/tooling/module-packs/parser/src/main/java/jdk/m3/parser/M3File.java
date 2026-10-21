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

    /** Disposition of two independently captured original-byte SHA-256 seals. */
    public enum ByteChange {
        /** At least one snapshot was created without original-byte identity. */
        UNKNOWN,
        /** Both captured SHA-256 seals match; this is not an ABI comparison. */
        MATCHING_SEALS,
        /** The captured original bytes have different SHA-256 seals. */
        DIFFERENT_SEALS
    }

    /**
     * Detached change evidence in exact UTF-16 code-unit coordinates.
     *
     * <p>The previous window is [start, previousEnd) and the current window is
     * [start, currentEnd). Results from changesFrom place unchanged text at zero.
     * The windows can contain unchanged text between separate edits. URI and
     * byte-seal changes are independent of character changes. This value does
     * not establish reusable syntax, attributed symbols, class ABI or outputs.</p>
     */
    public record SourceDelta(boolean sameUri, int start, int previousEnd,
            int currentEnd, ByteChange byteChange) {
        public SourceDelta {
            if (start < 0 || previousEnd < start || currentEnd < start) {
                throw new IllegalArgumentException("invalid source delta window");
            }
            Objects.requireNonNull(byteChange, "byteChange");
        }

        /** Whether the retained source characters differ, independent of URI/encoding. */
        public boolean charactersChanged() {
            return previousEnd != start || currentEnd != start;
        }
    }

    /**
     * Compare this immutable snapshot with an earlier snapshot without parsing or I/O.
     *
     * <p>A maximal common prefix and a non-overlapping common suffix enclose all
     * changed characters. Comparison is exact, including unpaired surrogates,
     * and never treats a hash match as character equality. Callers retain
     * responsibility for complete compilation-context and output validation.
     * A lexical boundary change can invalidate syntax beyond this window.</p>
     *
     * @param previous the earlier immutable source snapshot
     * @return immutable evidence; no source payload is copied into the result
     * @throws NullPointerException if previous is null
     */
    public SourceDelta changesFrom(M3File previous) {
        Objects.requireNonNull(previous, "previous");
        ByteChange bytes = compareByteSeals(previous);
        boolean sameUri = uri.equals(previous.uri);
        if (text == previous.text) {
            return new SourceDelta(sameUri, 0, 0, 0, bytes);
        }
        int previousEnd = previous.text.length();
        int currentEnd = text.length();
        int prefix = 0;
        int sharedLength = Math.min(previousEnd, currentEnd);
        while (prefix < sharedLength && previous.text.charAt(prefix) == text.charAt(prefix)) {
            prefix++;
        }
        if (prefix == previousEnd && prefix == currentEnd) {
            return new SourceDelta(sameUri, 0, 0, 0, bytes);
        }
        while (previousEnd > prefix && currentEnd > prefix
                && previous.text.charAt(previousEnd - 1) == text.charAt(currentEnd - 1)) {
            previousEnd--;
            currentEnd--;
        }
        return new SourceDelta(sameUri, prefix, previousEnd, currentEnd, bytes);
    }

    private ByteChange compareByteSeals(M3File previous) {
        if (byteSha256 == null || previous.byteSha256 == null) return ByteChange.UNKNOWN;
        return byteSha256.equals(previous.byteSha256)
                ? ByteChange.MATCHING_SEALS : ByteChange.DIFFERENT_SEALS;
    }

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
