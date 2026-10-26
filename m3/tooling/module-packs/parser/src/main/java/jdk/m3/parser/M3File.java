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
import java.util.Arrays;
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
    private volatile int[] lineStarts;

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

    /**
     * Read an existing input once and retain its supplied source URI exactly.
     * This method takes ownership of a nonnull input and closes it exactly once,
     * including when URI, charset or byte-budget validation fails. The caller
     * must not separately close the transferred input.
     *
     * <p>At most {@code maxBytes + 1} bytes are consumed; the extra byte detects
     * an oversized source. Successful input reaches EOF within the budget.
     * As with the Path reader, input close precedes size validation and strict
     * decoding. The selected charset owns BOM handling. Malformed or unmappable
     * data is refused without replacement-character recovery. A close failure
     * prevents a source result; read/setup failures retain distinct close
     * failures as suppressed evidence without suppressing themselves.</p>
     *
     * <p>The raw byte and exact UTF-16 identities describe the admitted byte
     * sequence and its decoded String. No source is reopened, and this method
     * does not establish an atomic filesystem snapshot.</p>
     */
    public static M3File read(URI uri, InputStream input, Charset charset, int maxBytes) throws IOException {
        Objects.requireNonNull(input, "input");
        byte[] bytes;
        Throwable primary = null;
        try {
            Objects.requireNonNull(uri, "uri");
            Objects.requireNonNull(charset, "charset");
            if (maxBytes < 0 || maxBytes > Integer.MAX_VALUE - 8) {
                throw new IllegalArgumentException("maxBytes outside supported array bounds");
            }
            bytes = input.readNBytes(maxBytes + 1);
        } catch (IOException | RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                input.close();
            } catch (IOException | RuntimeException | Error cleanup) {
                if (primary == null) {
                    throw cleanup;
                }
                if (primary != cleanup) {
                    primary.addSuppressed(cleanup);
                }
            }
        }
        if (bytes.length > maxBytes) {
            throw new IOException("source exceeds maxBytes=" + maxBytes + ": " + uri);
        }
        String text = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
        String byteHash = HexFormat.of().formatHex(sha256().digest(bytes));
        return new M3File(uri, text, byteHash);
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

    /** One-based logical line and zero-based UTF-16 code-unit column. */
    public record LinePosition(int line, int column) { }

    /** Immutable source, lazily cached primitive offsets; CRLF is one break. */
    public int lineCount() { return lineStarts().length; }

    /** EOF is addressable; the LF part of CRLF belongs to the old line. */
    public LinePosition linePosition(int utf16Offset) {
        if (utf16Offset < 0 || utf16Offset > text.length()) {
            throw new IndexOutOfBoundsException("UTF-16 source offset");
        }
        int[] starts = lineStarts();
        int at = Arrays.binarySearch(starts, utf16Offset);
        int lineIndex = at >= 0 ? at : -at - 2;
        return new LinePosition(lineIndex + 1, utf16Offset - starts[lineIndex]);
    }

    /** Strict inverse for coordinates in the current immutable source generation. */
    public int offsetAt(int oneBasedLine, int utf16Column) {
        int[] starts = lineStarts();
        int row = Objects.checkIndex(oneBasedLine - 1, starts.length);
        int begin = starts[row];
        int max = row + 1 == starts.length
                ? text.length() - begin : starts[row + 1] - begin - 1;
        if (utf16Column < 0 || utf16Column > max) {
            throw new IndexOutOfBoundsException("UTF-16 source column");
        }
        return begin + utf16Column;
    }

    private int[] lineStarts() {
        int[] result = lineStarts;
        if (result != null) return result;
        synchronized (this) {
            result = lineStarts;
            if (result != null) return result;
            int[] starts = new int[16];
            int used = 1;
            for (int offset = 0; offset < text.length(); offset++) {
                char c = text.charAt(offset);
                if (c != '\r' && c != '\n') continue;
                if (c == '\r' && offset + 1 < text.length() && text.charAt(offset + 1) == '\n') offset++;
                if (used == starts.length) starts = Arrays.copyOf(starts, starts.length * 2);
                starts[used++] = offset + 1;
            }
            result = Arrays.copyOf(starts, used);
            lineStarts = result;
            return result;
        }
    }

    private static String hashUtf16(String value) {
        MessageDigest digest = sha256();
        byte[] block = new byte[2 * Math.min(4096, value.length())];
        int used = 0;
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            block[used++] = (byte) (unit >>> 8);
            block[used++] = (byte) unit;
            if (used == block.length) {
                digest.update(block);
                used = 0;
            }
        }
        if (used != 0) digest.update(block, 0, used);
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
