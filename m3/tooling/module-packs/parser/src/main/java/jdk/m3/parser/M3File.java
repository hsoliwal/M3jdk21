/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
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
    private volatile CodePointOffsets codePointOffsets;
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

    /**
     * Receive the bytes of one externally owned, checked file observation as
     * UTF-8, without reopening the source or retaining an original byte array.
     * Intended to be passed to the existing physical M3File's synchronous
     * ByteObserver during its one-pass SHA-256 observation. This class is not
     * coupled to a particular filesystem provider or its module descriptor.
     *
     * <p>The observer's owner must finish its read and metadata checks before
     * passing the resulting four big-endian SHA-256 words to seal(). M3File
     * verifies the accepted byte count, but the supplied digest is an external
     * provenance assertion, not a hash re-computed from retained bytes. A
     * failed read, decode or source-generation check must never be sealed.
     * The capture is deliberately single-threaded and one-shot.</p>
     */
    public static Utf8Capture captureObservedUtf8(URI uri, int maxBytes) {
        return new Utf8Capture(uri, maxBytes);
    }

    /** Bounded, transient UTF-8 decoding state, never a second source owner. */
    public static final class Utf8Capture {
        private final URI uri;
        private final int maxBytes;
        private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        private final CharBuffer characters = CharBuffer.allocate(4096);
        private final byte[] unfinishedScalar = new byte[4];
        private StringBuilder decoded = new StringBuilder();
        private int unfinishedLength;
        private int acceptedBytes;
        private boolean terminal;

        private Utf8Capture(URI uri, int maxBytes) {
            this.uri = Objects.requireNonNull(uri, "uri");
            if (maxBytes < 0 || maxBytes > Integer.MAX_VALUE - 8) {
                throw new IllegalArgumentException("maxBytes outside supported array bounds");
            }
            this.maxBytes = maxBytes;
        }

        /**
         * Borrow a caller's bytes only during this invocation. Arbitrary
         * fragmentation, including splitting all four bytes of one scalar,
         * is supported. Neither the scratch array nor a source-byte copy is
         * retained. Every failed call permanently invalidates this capture.
         */
        public void onBytes(byte[] bytes, int offset, int length) throws IOException {
            requireOpen();
            try {
                Objects.requireNonNull(bytes, "observed byte chunk");
                Objects.checkFromIndexSize(offset, length, bytes.length);
                if (length > maxBytes - acceptedBytes) {
                    throw new IOException("observed UTF-8 source exceeds byte budget");
                }
                int at = offset, end = offset + length;
                while (unfinishedLength != 0 && at < end) {
                    if (unfinishedLength == unfinishedScalar.length) {
                        throw new IOException("invalid incomplete UTF-8 scalar");
                    }
                    unfinishedScalar[unfinishedLength++] = bytes[at++];
                    ByteBuffer fragment = ByteBuffer.wrap(unfinishedScalar, 0, unfinishedLength);
                    decode(fragment, false);
                    int remainder = fragment.remaining();
                    if (remainder != 0 && fragment.position() != 0) {
                        System.arraycopy(unfinishedScalar, fragment.position(),
                                unfinishedScalar, 0, remainder);
                    }
                    unfinishedLength = remainder;
                }
                if (at < end) {
                    ByteBuffer fragment = ByteBuffer.wrap(bytes, at, end - at);
                    decode(fragment, false);
                    unfinishedLength = fragment.remaining();
                    if (unfinishedLength > unfinishedScalar.length) {
                        throw new IOException("invalid UTF-8 decoder remainder");
                    }
                    fragment.get(unfinishedScalar, 0, unfinishedLength);
                }
                acceptedBytes += length;
            } catch (IOException | RuntimeException | Error failure) {
                terminal = true;
                decoded = null;
                throw failure;
            }
        }

        /**
         * Finish only after the original physical observation succeeds. The
         * digest words are those of its already-checked SHA-256 Fingerprint,
         * ordered exactly as ByteBuffer.getLong() reads the digest bytes.
         */
        public M3File seal(long observedSize, long digest0, long digest1,
                long digest2, long digest3) throws IOException {
            requireOpen();
            terminal = true;
            try {
                if (observedSize != acceptedBytes) {
                    throw new IOException("physical observation byte count does not match UTF-8 capture");
                }
                ByteBuffer last = ByteBuffer.wrap(unfinishedScalar, 0, unfinishedLength);
                decode(last, true);
                if (last.hasRemaining()) throw new IOException("incomplete UTF-8 scalar at EOF");
                while (true) {
                    CoderResult result = decoder.flush(characters);
                    drainCharacters();
                    if (result.isOverflow()) continue;
                    if (result.isError()) result.throwException();
                    break;
                }
                byte[] digest = ByteBuffer.allocate(32).putLong(digest0).putLong(digest1)
                        .putLong(digest2).putLong(digest3).array();
                return new M3File(uri, decoded.toString(), HexFormat.of().formatHex(digest), acceptedBytes);
            } finally {
                decoded = null;
                unfinishedLength = 0;
            }
        }

        private void decode(ByteBuffer source, boolean last) throws IOException {
            while (true) {
                CoderResult result = decoder.decode(source, characters, last);
                drainCharacters();
                if (result.isOverflow()) continue;
                if (result.isError()) result.throwException();
                return;
            }
        }

        private void drainCharacters() {
            characters.flip();
            decoded.append(characters);
            characters.clear();
        }

        private void requireOpen() {
            if (terminal) throw new IllegalStateException("UTF-8 capture is already terminal");
        }
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

    /** One losslessly corresponding half-open range in two immutable source generations. */
    public record ChangeHunk(int previousStart, int previousEnd,
                             int currentStart, int currentEnd) {
        public ChangeHunk {
            if (previousStart < 0 || currentStart < 0
                    || previousEnd < previousStart || currentEnd < currentStart
                    || (previousStart == previousEnd && currentStart == currentEnd)) {
                throw new IllegalArgumentException("invalid source change hunk");
            }
        }
    }

    /**
     * Parent-owned four-integer lanes rather than one retained object per
     * hunk. Each projection is a temporary value. Intervening unchanged
     * regions are byte-for-byte identical as UTF-16 units; minimality of the
     * edit script and semantic/ABI equivalence are deliberately not claimed.
     */
    public static final class SourceHunks {
        private final int[] coordinates;
        private final RawDigestComparison raw;
        private final boolean coarse;

        private SourceHunks(int[] coordinates, RawDigestComparison raw, boolean coarse) {
            this.coordinates = coordinates;
            this.raw = raw;
            this.coarse = coarse;
        }

        public int hunkCount() { return coordinates.length / 4; }
        public long primitiveBytes() { return (long) coordinates.length * Integer.BYTES; }
        public RawDigestComparison rawDigestComparison() { return raw; }
        /** True if resource limits forced the original conservative envelope. */
        public boolean coarse() { return coarse; }
        public ChangeHunk hunk(int index) {
            Objects.checkIndex(index, hunkCount());
            int offset = index * 4;
            return new ChangeHunk(coordinates[offset], coordinates[offset + 1],
                    coordinates[offset + 2], coordinates[offset + 3]);
        }
    }

    /** Generation-absolute original UTF-8 byte correspondence, not sequential parser edits. */
    public record Utf8Hunk(int previousStartByte, int previousEndByte,
                           int currentStartByte, int currentEndByte) {
        public Utf8Hunk {
            if (previousStartByte < 0 || currentStartByte < 0
                    || previousEndByte < previousStartByte
                    || currentEndByte < currentStartByte
                    || (previousStartByte == previousEndByte
                        && currentStartByte == currentEndByte)) {
                throw new IllegalArgumentException("invalid UTF-8 change hunk");
            }
        }
    }

    /** One packed immutable byte-coordinate lane per original source generation. */
    public static final class Utf8Hunks {
        private final int[] coordinates;
        private final RawDigestComparison raw;
        private final boolean coarse;

        private Utf8Hunks(int[] coordinates, RawDigestComparison raw, boolean coarse) {
            this.coordinates = coordinates;
            this.raw = raw;
            this.coarse = coarse;
        }

        public int hunkCount() { return coordinates.length / 4; }
        public long primitiveBytes() { return (long) coordinates.length * Integer.BYTES; }
        public RawDigestComparison rawDigestComparison() { return raw; }
        public boolean coarse() { return coarse; }
        public Utf8Hunk hunk(int index) {
            Objects.checkIndex(index, hunkCount());
            int offset = index * 4;
            return new Utf8Hunk(coordinates[offset], coordinates[offset + 1],
                    coordinates[offset + 2], coordinates[offset + 3]);
        }
    }

    /**
     * Find disjoint, deterministic source spans through exact unchanged line
     * anchors. Anchors must be unique in both generations and strictly
     * increasing; a collision in the noncryptographic discovery hash can
     * discard an anchor but can never establish correspondence without an
     * exact String comparison. Local gap trimming preserves surrogate pairs.
     *
     * <p>Bounds apply to both source lengths, the per-source line index, and
     * retained hunk count. Exhausting anchor search capacity or maxHunks
     * returns one explicitly coarse envelope from changeSince(), never a
     * partial optimistic diff. This is a source hint, not permission to
     * reuse an old parser tree or downstream ABI facts.</p>
     */
    public SourceHunks changeHunksSince(M3File previous, long maxComparedUtf16Units,
            int maxLines, int maxHunks) {
        ChangeEnvelope entire = changeSince(previous, maxComparedUtf16Units);
        if (maxLines < 1 || maxHunks < 1 || maxHunks > Integer.MAX_VALUE / 4) {
            throw new IllegalArgumentException("invalid source hunk resource budget");
        }
        if (!entire.textChanged()) {
            return new SourceHunks(new int[0], entire.rawDigestComparison(), false);
        }
        LineIndex oldLines = previous.lineIndex(maxLines);
        LineIndex newLines = lineIndex(maxLines);
        UniqueLines old = UniqueLines.build(previous.text, oldLines);
        UniqueLines current = UniqueLines.build(text, newLines);
        if (old == null || current == null) return coarseHunks(entire);

        int[] oldCandidates = new int[current.lineCount];
        int[] newCandidates = new int[current.lineCount];
        int candidates = 0;
        for (int line = 0; line < current.lineCount; line++) {
            if (current.hashes[line] == 0L) continue;
            int uniquelyCurrent = current.unique(current.hashes[line]);
            int uniquelyOld = old.unique(current.hashes[line]);
            if (uniquelyCurrent == -2 || uniquelyOld == -2) return coarseHunks(entire);
            if (uniquelyCurrent != line || uniquelyOld < 0) continue;
            int oldStart = old.start(uniquelyOld), oldEnd = old.end(uniquelyOld);
            int newStart = current.start(line), newEnd = current.end(line);
            if (oldEnd - oldStart != newEnd - newStart
                    || !previous.text.regionMatches(oldStart, text, newStart, oldEnd - oldStart)) {
                continue;
            }
            oldCandidates[candidates] = uniquelyOld;
            newCandidates[candidates] = line;
            candidates++;
        }

        // A longest increasing sequence keeps the same exact anchors in
        // original order without treating moved/reordered lines as unchanged.
        int[] tails = new int[candidates];
        int[] predecessors = new int[candidates];
        int selected = 0;
        for (int i = 0; i < candidates; i++) {
            int low = 0, high = selected;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (oldCandidates[tails[middle]] < oldCandidates[i]) low = middle + 1;
                else high = middle;
            }
            predecessors[i] = low == 0 ? -1 : tails[low - 1];
            tails[low] = i;
            if (low == selected) selected++;
        }
        int[] ordered = new int[selected];
        for (int i = selected - 1, at = selected == 0 ? -1 : tails[selected - 1];
                i >= 0; i--) {
            ordered[i] = at;
            at = predecessors[at];
        }
        HunkCollector collected = new HunkCollector(maxHunks);
        int priorAt = 0, currentAt = 0;
        for (int index : ordered) {
            int p = oldCandidates[index], c = newCandidates[index];
            if (!collected.gap(previous.text, text, priorAt, old.start(p),
                    currentAt, current.start(c))) return coarseHunks(entire);
            priorAt = old.end(p);
            currentAt = current.end(c);
        }
        if (!collected.gap(previous.text, text, priorAt, previous.text.length(),
                currentAt, text.length())) return coarseHunks(entire);
        return new SourceHunks(collected.result(), entire.rawDigestComparison(), false);
    }

    /**
     * Project exact multi-hunk correspondence into independent original UTF-8
     * byte coordinate spaces. These are NOT TSInputEdit instances: applying
     * multiple edits to one mutable tree requires explicit coordinate/point
     * rebasing and proof from the original parser owner.
     */
    public Utf8Hunks utf8ChangeHunksSince(M3File previous,
            long maxComparedUtf16Units, int maxLines, int maxHunks) {
        SourceHunks changes = changeHunksSince(previous, maxComparedUtf16Units, maxLines, maxHunks);
        Utf8Offsets oldBytes = previous.utf8Offsets().orElseThrow(() ->
                new IllegalArgumentException("previous source lacks original UTF-8 byte geometry"));
        Utf8Offsets newBytes = utf8Offsets().orElseThrow(() ->
                new IllegalArgumentException("current source lacks original UTF-8 byte geometry"));
        int[] positions = new int[changes.hunkCount() * 4];
        for (int i = 0; i < changes.hunkCount(); i++) {
            ChangeHunk unit = changes.hunk(i);
            int at = i * 4;
            positions[at] = oldBytes.toByte(unit.previousStart());
            positions[at + 1] = oldBytes.toByte(unit.previousEnd());
            positions[at + 2] = newBytes.toByte(unit.currentStart());
            positions[at + 3] = newBytes.toByte(unit.currentEnd());
        }
        return new Utf8Hunks(positions, changes.rawDigestComparison(), changes.coarse());
    }

    private static SourceHunks coarseHunks(ChangeEnvelope entire) {
        return new SourceHunks(new int[]{entire.previousStart(), entire.previousEnd(),
                entire.currentStart(), entire.currentEnd()},
                entire.rawDigestComparison(), true);
    }

    /** Temporary primitive discovery table. Hashes nominate; characters certify. */
    private static final class UniqueLines {
        private static final int MAX_PROBES = 64;
        private final String text;
        private final LineIndex lines;
        private final long[] hashes;
        private final long[] keys;
        private final int[] slots;
        private final int mask;
        private final int lineCount;

        private UniqueLines(String text, LineIndex lines, int capacity) {
            this.text = text;
            this.lines = lines;
            this.lineCount = lines.lineCount();
            this.hashes = new long[lineCount];
            this.keys = new long[capacity];
            this.slots = new int[capacity];
            this.mask = capacity - 1;
        }

        private static UniqueLines build(String text, LineIndex lines) {
            int count = lines.lineCount();
            long requested = 4L * count;
            if (requested > 1L << 23) return null; // explicit bounded fallback
            int capacity = 4;
            while (capacity < requested) capacity <<= 1;
            UniqueLines result = new UniqueLines(text, lines, capacity);
            for (int i = 0; i < count; i++) {
                int start = result.start(i), end = result.end(i);
                if (start == end) continue; // no zero-width anchors
                long hash = 0xcbf29ce484222325L;
                for (int j = start; j < end; j++) {
                    hash = (hash ^ text.charAt(j)) * 0x100000001b3L;
                }
                hash = (hash ^ (end - start)) * 0x100000001b3L;
                if (hash == 0L) hash = 1L; // sentinel only; exact equality still required
                result.hashes[i] = hash;
                if (!result.add(hash, i)) return null;
            }
            return result;
        }

        private int start(int line) { return lines.lineStart(line + 1); }
        private int end(int line) {
            return line + 1 < lineCount ? lines.lineStart(line + 2) : text.length();
        }

        private int slot(long hash) {
            long mixed = hash ^ (hash >>> 33);
            mixed *= 0xff51afd7ed558ccdL;
            mixed ^= mixed >>> 33;
            return ((int) mixed) & mask;
        }

        private boolean add(long hash, int line) {
            int at = slot(hash);
            for (int probe = 0; probe < MAX_PROBES; probe++, at = (at + 1) & mask) {
                if (slots[at] == 0) {
                    keys[at] = hash;
                    slots[at] = line + 1;
                    return true;
                }
                if (keys[at] == hash) {
                    slots[at] = -1; // same or colliding line: ambiguous, never an anchor
                    return true;
                }
            }
            return false;
        }

        /** Nonnegative unique zero-based line; -1 absent/ambiguous; -2 probe refusal. */
        private int unique(long hash) {
            int at = slot(hash);
            for (int probe = 0; probe < MAX_PROBES; probe++, at = (at + 1) & mask) {
                if (slots[at] == 0) return -1;
                if (keys[at] == hash) return slots[at] - 1 < 0 ? -1 : slots[at] - 1;
            }
            return -2;
        }
    }

    /** File-local, primitive-backed hunk emission with a fail-closed count limit. */
    private static final class HunkCollector {
        private final int max;
        private int[] lanes;
        private int size;

        private HunkCollector(int max) {
            this.max = max;
            this.lanes = new int[Math.min(max, 8) * 4];
        }

        private boolean gap(String before, String after, int oldStart, int oldEnd,
                int newStart, int newEnd) {
            while (oldStart < oldEnd && newStart < newEnd
                    && before.charAt(oldStart) == after.charAt(newStart)) {
                oldStart++;
                newStart++;
            }
            if (insideSurrogatePair(before, oldStart) || insideSurrogatePair(after, newStart)) {
                oldStart--;
                newStart--;
            }
            while (oldEnd > oldStart && newEnd > newStart
                    && before.charAt(oldEnd - 1) == after.charAt(newEnd - 1)) {
                oldEnd--;
                newEnd--;
            }
            if (insideSurrogatePair(before, oldEnd) || insideSurrogatePair(after, newEnd)) {
                oldEnd++;
                newEnd++;
            }
            if (oldStart == oldEnd && newStart == newEnd) return true;
            if (size != 0) {
                int last = (size - 1) * 4;
                if (lanes[last + 1] == oldStart && lanes[last + 3] == newStart) {
                    lanes[last + 1] = oldEnd;
                    lanes[last + 3] = newEnd;
                    return true;
                }
            }
            if (size == max) return false;
            if (size * 4 == lanes.length) {
                lanes = Arrays.copyOf(lanes, Math.min(max, size * 2) * 4);
            }
            int at = size++ * 4;
            lanes[at] = oldStart;
            lanes[at + 1] = oldEnd;
            lanes[at + 2] = newStart;
            lanes[at + 3] = newEnd;
            return true;
        }

        private int[] result() { return Arrays.copyOf(lanes, size * 4); }
    }

    /**
     * Exact original-UTF-8 byte geometry for the same conservative source edit.
     * No original byte buffer is retained or regenerated for diffing; both
     * generations must have been independently admitted from strict UTF-8.
     * Consumers such as Tree-sitter must still compute their own provider-
     * specific row/column points and certify incremental parser reuse.
     */
    public Utf8ChangeEnvelope utf8ChangeSince(M3File previous, long maxComparedUtf16Units) {
        ChangeEnvelope edit = changeSince(previous, maxComparedUtf16Units);
        Utf8Offsets earlier = previous.utf8Offsets().orElseThrow(() ->
                new IllegalArgumentException("previous source lacks original UTF-8 byte geometry"));
        Utf8Offsets current = utf8Offsets().orElseThrow(() ->
                new IllegalArgumentException("current source lacks original UTF-8 byte geometry"));
        int previousStart = earlier.toByte(edit.previousStart());
        int currentStart = current.toByte(edit.currentStart());
        if (previousStart != currentStart) {
            throw new IllegalStateException("shared source prefix has different UTF-8 coordinates");
        }
        return new Utf8ChangeEnvelope(previousStart, earlier.toByte(edit.previousEnd()),
                currentStart, current.toByte(edit.currentEnd()), edit.rawDigestComparison());
    }

    /**
     * An original UTF-8 edit with the three LF-based, zero-origin byte points
     * required by incremental CST parsers such as Tree-sitter. CR is a column
     * byte; only LF advances the row. This is geometry, not permission to
     * mutate/reuse the provider's authoritative old tree.
     */
    public Utf8InputEdit utf8InputEditSince(M3File previous,
            long maxComparedUtf16Units, int maxLines) {
        Utf8ChangeEnvelope envelope = utf8ChangeSince(previous, maxComparedUtf16Units);
        Utf8Offsets earlier = previous.utf8Offsets().orElseThrow();
        Utf8Offsets current = utf8Offsets().orElseThrow();
        Utf8Point startPoint = earlier.pointAt(envelope.previousStartByte(), maxLines);
        if (!startPoint.equals(current.pointAt(envelope.currentStartByte(), maxLines))) {
            throw new IllegalStateException("shared UTF-8 prefix has different LF coordinates");
        }
        return new Utf8InputEdit(envelope.previousStartByte(), envelope.previousEndByte(),
                envelope.currentEndByte(), startPoint,
                earlier.pointAt(envelope.previousEndByte(), maxLines),
                current.pointAt(envelope.currentEndByte(), maxLines),
                envelope.rawDigestComparison());
    }

    /** Zero-based line, zero-based original UTF-8 byte column, with LF-only rows. */
    public record Utf8Point(int row, int column) {
        public Utf8Point {
            if (row < 0 || column < 0) throw new IllegalArgumentException("negative UTF-8 point");
        }
    }

    /** Tree-sitter-compatible numerical edit fields; no parser tree is edited here. */
    public record Utf8InputEdit(int startByte, int oldEndByte, int newEndByte,
                                Utf8Point startPoint, Utf8Point oldEndPoint,
                                Utf8Point newEndPoint, RawDigestComparison rawDigestComparison) {
        public Utf8InputEdit {
            if (startByte < 0 || oldEndByte < startByte || newEndByte < startByte) {
                throw new IllegalArgumentException("invalid original UTF-8 input edit");
            }
            Objects.requireNonNull(startPoint, "start point");
            Objects.requireNonNull(oldEndPoint, "old end point");
            Objects.requireNonNull(newEndPoint, "new end point");
            Objects.requireNonNull(rawDigestComparison, "raw digest comparison");
        }

        public boolean textChanged() { return startByte != oldEndByte || startByte != newEndByte; }
    }

    /** Half-open byte-change envelope, *not* a multi-hunk patch or Tree-sitter point tuple. */
    public record Utf8ChangeEnvelope(int previousStartByte, int previousEndByte,
                                     int currentStartByte, int currentEndByte,
                                     RawDigestComparison rawDigestComparison) {
        public Utf8ChangeEnvelope {
            if (previousStartByte < 0 || currentStartByte != previousStartByte
                    || previousEndByte < previousStartByte
                    || currentEndByte < currentStartByte) {
                throw new IllegalArgumentException("invalid original UTF-8 change envelope");
            }
            Objects.requireNonNull(rawDigestComparison, "raw digest comparison");
        }

        public boolean textChanged() {
            return previousEndByte != previousStartByte || currentEndByte != currentStartByte;
        }
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

    /**
     * Native Unicode-codepoint to Java UTF-16 boundary conversion for parsers
     * whose original input symbols are scalar values (ANTLR CodePointCharStream,
     * for example). This is available for text-only sources too; unlike the
     * original-byte map it makes no claim about the source encoding.
     *
     * <p>A supplementary character adds precisely one UTF-16 unit beyond its
     * codepoint index. The optional map retains only one int[] containing the
     * scalar positions of surrogate pairs; BMP-only text uses an empty array.
     * Unpaired surrogates are refused rather than fabricated as valid scalars.
     */
    public CodePointOffsets codePointOffsets() {
        CodePointOffsets found = codePointOffsets;
        if (found == null) {
            synchronized (this) {
                found = codePointOffsets;
                if (found == null) codePointOffsets = found = CodePointOffsets.build(text);
            }
        }
        return found;
    }

    /** One source-owned sparse primitive lane, with exact reversible boundaries. */
    public static final class CodePointOffsets {
        private final int utf16Length;
        private final int scalarLength;
        private final int[] supplementaryAt;

        private CodePointOffsets(int utf16Length, int scalarLength, int[] supplementaryAt) {
            this.utf16Length = utf16Length;
            this.scalarLength = scalarLength;
            this.supplementaryAt = supplementaryAt;
        }

        private static CodePointOffsets build(String text) {
            int[] indexes = new int[16];
            int pairs = 0, scalars = 0;
            for (int i = 0; i < text.length();) {
                char unit = text.charAt(i);
                if (Character.isHighSurrogate(unit)) {
                    if (i + 1 == text.length()
                            || !Character.isLowSurrogate(text.charAt(i + 1))) {
                        throw new IllegalArgumentException("unpaired high surrogate in scalar source");
                    }
                    if (pairs == indexes.length) indexes = Arrays.copyOf(indexes, pairs * 2);
                    indexes[pairs++] = scalars;
                    i += 2;
                } else if (Character.isLowSurrogate(unit)) {
                    throw new IllegalArgumentException("unpaired low surrogate in scalar source");
                } else i++;
                scalars++;
            }
            return new CodePointOffsets(text.length(), scalars, Arrays.copyOf(indexes, pairs));
        }

        public int codePointLength() { return scalarLength; }
        public int utf16Length() { return utf16Length; }
        public int supplementaryCount() { return supplementaryAt.length; }
        public long primitiveBytes() { return (long) supplementaryAt.length * Integer.BYTES; }

        /** Scalar offset to half-open Java source position; EOF is legal. */
        public int toUtf16(int codePointOffset) {
            Objects.checkIndex(codePointOffset, scalarLength + 1);
            int found = Arrays.binarySearch(supplementaryAt, codePointOffset);
            int completedPairs = found < 0 ? -found - 1 : found;
            return codePointOffset + completedPairs;
        }

        /** Reject a UTF-16 boundary between the high and low surrogate halves. */
        public int toCodePoint(int utf16Offset) {
            Objects.checkIndex(utf16Offset, utf16Length + 1);
            int low = 0, high = supplementaryAt.length;
            while (low < high) {
                int mid = (low + high) >>> 1;
                int start = supplementaryAt[mid] + mid;
                if (start + 2 <= utf16Offset) low = mid + 1;
                else high = mid;
            }
            if (low < supplementaryAt.length
                    && utf16Offset == supplementaryAt[low] + low + 1) {
                throw new IllegalArgumentException("offset inside UTF-16 surrogate pair");
            }
            return utf16Offset - low;
        }
    }

    /** Boundary-exact, half-open UTF-8 source coordinates; no mid-codepoint rounding. */
    public static final class Utf8Offsets {
        private final String text;
        private final int byteLength;
        private final int[] byteStarts;
        private final int[] utf16Starts;
        private volatile int[] lfLineByteStarts;

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

        /**
         * Strict source-byte point using LF, unlike the general Java CR/LF/CRLF
         * UTF-16 line table. An offset inside a UTF-8 code point is refused.
         */
        public Utf8Point pointAt(int byteOffset, int maxLines) {
            toUtf16(byteOffset);
            int[] lines = lfLines(maxLines);
            int found = Arrays.binarySearch(lines, byteOffset);
            int row = found >= 0 ? found : -found - 2;
            return new Utf8Point(row, byteOffset - lines[row]);
        }

        /** Inverse of pointAt for LF-based rows at valid UTF-8 codepoint boundaries. */
        public int byteAt(Utf8Point point, int maxLines) {
            Objects.requireNonNull(point, "UTF-8 point");
            int[] lines = lfLines(maxLines);
            int row = Objects.checkIndex(point.row(), lines.length);
            int last = row + 1 == lines.length ? byteLength : lines[row + 1] - 1;
            if (point.column() > last - lines[row]) {
                throw new IndexOutOfBoundsException("byte column outside LF-based line");
            }
            int byteOffset = lines[row] + point.column();
            toUtf16(byteOffset);
            return byteOffset;
        }

        /** Lazily retain one int[] of UTF-8 starts after each original LF byte. */
        public int lfLineCount(int maxLines) { return lfLines(maxLines).length; }

        private int[] lfLines(int maxLines) {
            if (maxLines < 1) throw new IllegalArgumentException("LF line budget must be positive");
            int[] found = lfLineByteStarts;
            if (found == null) {
                synchronized (this) {
                    found = lfLineByteStarts;
                    if (found == null) lfLineByteStarts = found = buildLfLines(maxLines);
                }
            }
            if (found.length > maxLines) throw new IllegalArgumentException("LF line budget exceeded");
            return found;
        }

        private int[] buildLfLines(int maxLines) {
            int[] starts = new int[16];
            int rows = 1, byteOffset = 0;
            for (int i = 0; i < text.length();) {
                int codepoint = text.codePointAt(i);
                byteOffset = Math.addExact(byteOffset, byteWidth(codepoint));
                i += Character.charCount(codepoint);
                if (codepoint == '\n') {
                    if (rows == maxLines) throw new IllegalArgumentException("LF line budget exceeded");
                    if (rows == starts.length) starts = Arrays.copyOf(starts, rows * 2);
                    starts[rows++] = byteOffset;
                }
            }
            if (byteOffset != byteLength) {
                throw new IllegalStateException("original UTF-8 LF map differs from byte length");
            }
            return Arrays.copyOf(starts, rows);
        }

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
