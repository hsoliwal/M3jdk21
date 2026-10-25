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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Arrays;
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
    private final int utf8ByteLength;
    private volatile Utf8Offsets utf8Offsets;
    private volatile LineIndex lineIndex;

    private M3File(URI uri, String text, String byteSha256, int utf8ByteLength) {
        this.uri = Objects.requireNonNull(uri, "uri");
        this.text = Objects.requireNonNull(text, "text");
        this.utf16Sha256 = hashUtf16(text);
        this.byteSha256 = byteSha256;
        this.utf8ByteLength = utf8ByteLength;
    }

    /** Retain an immutable String by identity, or snapshot another CharSequence once. */
    public static M3File of(URI uri, CharSequence text) {
        Objects.requireNonNull(text, "text");
        return new M3File(uri, text instanceof String value ? value : text.toString(), null, -1);
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
        return new M3File(path.toAbsolutePath().normalize().toUri(), text, byteHash,
                StandardCharsets.UTF_8.equals(charset) ? bytes.length : -1);
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
        return new M3File(uri, text, byteHash,
                StandardCharsets.UTF_8.equals(charset) ? bytes.length : -1);
    }

    public URI uri() { return uri; }
    public String text() { return text; }

    /** SHA-256 of exact UTF-16 big-endian code units, including unpaired surrogates. */
    public String utf16Sha256() { return utf16Sha256; }

    /** Raw byte SHA-256 when created by read; absent for character-only sources. */
    public Optional<String> byteSha256() { return Optional.ofNullable(byteSha256); }

    /** The two admitted raw-byte SHA-256 receipts are compared, never inferred from decoded text. */
    public enum RawDigestComparison { UNAVAILABLE, SHA256_SAME, SHA256_DIFFERENT }

    /**
     * Conservative, single-replacement UTF-16 envelope covering every changed
     * source unit between two immutable generations of the same URI. Unchanged
     * common prefix and suffix are excluded; independent edits inside this
     * envelope are intentionally not represented as separate hunks. Boundaries
     * never split a valid UTF-16 surrogate pair in either generation.
     *
     * <p>This is a source-text change hint, not an ABI/semantic change proof,
     * filesystem observation or authority to reuse a parser's prior tree.
     * Raw-byte identity is reported separately: equal decoded text can come
     * from different original encodings or byte sequences.</p>
     */
    public record ChangeEnvelope(int previousStart, int previousEnd,
                                 int currentStart, int currentEnd,
                                 RawDigestComparison rawDigestComparison) {
        public ChangeEnvelope {
            if (previousStart < 0 || currentStart < 0 || previousStart != currentStart
                    || previousEnd < previousStart || currentEnd < currentStart) {
                throw new IllegalArgumentException("invalid source change envelope");
            }
            Objects.requireNonNull(rawDigestComparison, "raw digest comparison");
        }

        public boolean textChanged() {
            return previousEnd != previousStart || currentEnd != currentStart;
        }
    }

    /**
     * Find one bounded edit envelope without new source storage or reparsing.
     * The byte budget is in UTF-16 units across both existing String owners;
     * callers must independently certify any compiler dependency/ABI reuse.
     */
    public ChangeEnvelope changeSince(M3File previous, long maxComparedUtf16Units) {
        Objects.requireNonNull(previous, "previous source generation");
        if (!uri.equals(previous.uri)) throw new IllegalArgumentException("different source URI");
        if (maxComparedUtf16Units < 0
                || (long) text.length() + previous.text.length() > maxComparedUtf16Units) {
            throw new IllegalArgumentException("source change comparison budget exceeded");
        }
        RawDigestComparison raw = byteSha256 == null || previous.byteSha256 == null
                ? RawDigestComparison.UNAVAILABLE
                : byteSha256.equals(previous.byteSha256)
                        ? RawDigestComparison.SHA256_SAME : RawDigestComparison.SHA256_DIFFERENT;
        if (text == previous.text || text.equals(previous.text)) {
            return new ChangeEnvelope(0, 0, 0, 0, raw);
        }
        int previousLength = previous.text.length();
        int currentLength = text.length();
        int prefix = 0;
        while (prefix < previousLength && prefix < currentLength
                && previous.text.charAt(prefix) == text.charAt(prefix)) prefix++;
        if (insideSurrogatePair(previous.text, prefix) || insideSurrogatePair(text, prefix)) prefix--;

        int previousEnd = previousLength, currentEnd = currentLength;
        while (previousEnd > prefix && currentEnd > prefix
                && previous.text.charAt(previousEnd - 1) == text.charAt(currentEnd - 1)) {
            previousEnd--;
            currentEnd--;
        }
        if (insideSurrogatePair(previous.text, previousEnd)
                || insideSurrogatePair(text, currentEnd)) {
            previousEnd++;
            currentEnd++;
        }
        return new ChangeEnvelope(prefix, previousEnd, prefix, currentEnd, raw);
    }

    private static boolean insideSurrogatePair(String value, int boundary) {
        return boundary > 0 && boundary < value.length()
                && Character.isHighSurrogate(value.charAt(boundary - 1))
                && Character.isLowSurrogate(value.charAt(boundary));
    }

    /**
     * Lazily index original UTF-16 line starts, without copying the source or
     * retaining a second line representation. CR, LF and CRLF each terminate
     * one line; a trailing terminator opens an empty last line. The first line
     * is one-based, and columns are zero-based UTF-16 offsets. The caller's
     * bound is enforced both when constructing and when reusing a cached index.
     */
    public LineIndex lineIndex(int maxLines) {
        if (maxLines < 1) throw new IllegalArgumentException("line budget must be positive");
        LineIndex found = lineIndex;
        if (found == null) {
            synchronized (this) {
                found = lineIndex;
                if (found == null) lineIndex = found = LineIndex.build(this, maxLines);
            }
        }
        if (found.lineCount() > maxLines) {
            throw new IllegalArgumentException("source line budget exceeded");
        }
        return found;
    }

    /** One-based line/zero-based UTF-16 column coordinate. */
    public record LinePosition(int line, int column) {
        public LinePosition {
            if (line < 1 || column < 0) throw new IllegalArgumentException("invalid line position");
        }
    }

    /** Source-generation-bound sparse structure: one primitive start offset per line. */
    public static final class LineIndex {
        private final M3File owner;
        private final int[] starts;

        private LineIndex(M3File owner, int[] starts) {
            this.owner = owner;
            this.starts = starts;
        }

        private static LineIndex build(M3File owner, int maxLines) {
            String source = owner.text;
            int[] starts = new int[16];
            starts[0] = 0;
            int lines = 1;
            for (int i = 0; i < source.length(); i++) {
                char unit = source.charAt(i);
                if (unit != '\r' && unit != '\n') continue;
                if (unit == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') i++;
                if (lines == maxLines) throw new IllegalArgumentException("source line budget exceeded");
                if (lines == starts.length) starts = Arrays.copyOf(starts, lines * 2);
                starts[lines++] = i + 1;
            }
            return new LineIndex(owner, Arrays.copyOf(starts, lines));
        }

        public int lineCount() { return starts.length; }
        public long primitiveBytes() { return (long) starts.length * Integer.BYTES; }
        public int lineStart(int oneBasedLine) {
            if (oneBasedLine < 1 || oneBasedLine > starts.length) {
                throw new IndexOutOfBoundsException("line outside source");
            }
            return starts[oneBasedLine - 1];
        }

        /** A CRLF interior offset belongs to the preceding line. EOF is legal. */
        public LinePosition positionAt(int utf16Offset) {
            if (utf16Offset < 0 || utf16Offset > owner.length()) {
                throw new IndexOutOfBoundsException("offset outside source");
            }
            int found = Arrays.binarySearch(starts, utf16Offset);
            int row = found >= 0 ? found : -found - 2;
            return new LinePosition(row + 1, utf16Offset - starts[row]);
        }

        /** Exact inverse for positions within the requested line, including final EOF. */
        public int offsetAt(int oneBasedLine, int utf16Column) {
            int start = lineStart(oneBasedLine);
            int end = oneBasedLine == starts.length ? owner.length() : starts[oneBasedLine] - 1;
            if (utf16Column < 0 || utf16Column > end - start) {
                throw new IndexOutOfBoundsException("column outside line");
            }
            return start + utf16Column;
        }
    }

    /**
     * Optional, source-generation-bound UTF-8 byte/UTF-16 coordinate bridge.
     * Only strict original UTF-8 reads qualify; character-only sources and
     * other encodings cannot assert the original byte offsets. The lazy index
     * retains two primitive arrays for non-ASCII code points, never the input
     * bytes, and leaves ASCII-only files with empty arrays.
     */
    public Optional<Utf8Offsets> utf8Offsets() {
        if (utf8ByteLength < 0) return Optional.empty();
        Utf8Offsets found = utf8Offsets;
        if (found == null) {
            synchronized (this) {
                found = utf8Offsets;
                if (found == null) utf8Offsets = found = Utf8Offsets.build(text, utf8ByteLength);
            }
        }
        return Optional.of(found);
    }

    /** Boundary-exact, half-open UTF-8 source coordinates; no mid-codepoint rounding. */
    public static final class Utf8Offsets {
        private final String text;
        private final int byteLength;
        private final int[] byteStarts;
        private final int[] utf16Starts;

        private Utf8Offsets(String text, int byteLength, int[] byteStarts, int[] utf16Starts) {
            this.text = text;
            this.byteLength = byteLength;
            this.byteStarts = byteStarts;
            this.utf16Starts = utf16Starts;
        }

        private static Utf8Offsets build(String text, int exactByteLength) {
            int[] byteStarts = new int[16], utf16Starts = new int[16];
            int count = 0, byteIndex = 0;
            for (int charIndex = 0; charIndex < text.length();) {
                int codepoint = text.codePointAt(charIndex);
                int units = Character.charCount(codepoint);
                if (Character.isSurrogate(text.charAt(charIndex))
                        && (units != 2 || !Character.isSurrogatePair(text.charAt(charIndex),
                                text.charAt(charIndex + 1)))) {
                    throw new IllegalStateException("UTF-8 snapshot contains an unpaired surrogate");
                }
                if (codepoint >= 0x80) {
                    if (count == byteStarts.length) {
                        byteStarts = Arrays.copyOf(byteStarts, count * 2);
                        utf16Starts = Arrays.copyOf(utf16Starts, count * 2);
                    }
                    byteStarts[count] = byteIndex;
                    utf16Starts[count++] = charIndex;
                }
                byteIndex = Math.addExact(byteIndex, byteWidth(codepoint));
                charIndex += units;
            }
            if (byteIndex != exactByteLength) {
                throw new IllegalStateException("UTF-8 coordinate identity differs from admitted bytes");
            }
            return new Utf8Offsets(text, byteIndex,
                    Arrays.copyOf(byteStarts, count), Arrays.copyOf(utf16Starts, count));
        }

        public int byteLength() { return byteLength; }
        public int utf16Length() { return text.length(); }
        public int nonAsciiCount() { return byteStarts.length; }
        public long primitiveBytes() { return 2L * Integer.BYTES * byteStarts.length; }

        /** Refuse a byte offset inside a multibyte UTF-8 sequence. */
        public int toUtf16(int byteOffset) {
            Objects.checkIndex(byteOffset, byteLength + 1);
            int found = Arrays.binarySearch(byteStarts, byteOffset);
            if (found >= 0) return utf16Starts[found];
            int previous = -found - 2;
            if (previous < 0) return byteOffset;
            int codepoint = text.codePointAt(utf16Starts[previous]);
            int byteEnd = byteStarts[previous] + byteWidth(codepoint);
            if (byteOffset < byteEnd) throw new IllegalArgumentException("offset inside UTF-8 code point");
            return utf16Starts[previous] + Character.charCount(codepoint) + byteOffset - byteEnd;
        }

        /** Refuse a UTF-16 offset between the two halves of a surrogate pair. */
        public int toByte(int utf16Offset) {
            Objects.checkIndex(utf16Offset, text.length() + 1);
            int found = Arrays.binarySearch(utf16Starts, utf16Offset);
            if (found >= 0) return byteStarts[found];
            int previous = -found - 2;
            if (previous < 0) return utf16Offset;
            int codepoint = text.codePointAt(utf16Starts[previous]);
            int end = utf16Starts[previous] + Character.charCount(codepoint);
            if (utf16Offset < end) throw new IllegalArgumentException("offset inside surrogate pair");
            return byteStarts[previous] + byteWidth(codepoint) + utf16Offset - end;
        }

        private static int byteWidth(int codepoint) {
            if (codepoint < 0x80) return 1;
            if (codepoint < 0x800) return 2;
            return codepoint < 0x10000 ? 3 : 4;
        }
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
