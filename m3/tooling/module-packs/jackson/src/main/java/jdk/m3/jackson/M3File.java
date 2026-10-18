// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
// Donor: hsoliwal/com.synexia at 50f48cbbe11f17fb8dc209bac3812f8940a1e267
// Path: synexia-indexstring/src/main/java/com/synexia/indexstring/file/MIndexFile.java
// Git blob: 0c30aaa8b9f88a829c87e46ef82a0ea96c0d68a3
// Adaptation: bounded indexed-input port of occurrence handles and UTF-16 prefix offsets.
// Existing String references replace resolver-specific text descriptors at the public JDK boundary.
// Search, postings, file persistence, native admission and the complete MIndexFile API are not ported here.
package jdk.m3.jackson;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable indexed content for Jackson input, composed from existing String atoms.
 *
 * <p>The source index retains one reference to the first supplied String with each distinct
 * spelling, an adaptive primitive occurrence lane, and UTF-16 prefix offsets. The temporary
 * admission map is discarded. It creates no independent character/byte spelling storage and
 * does not call {@link String#intern()}. On the M3JDK runtime, String is the existing public
 * boundary to the canonical internal M3String owner; this class does not activate that runtime
 * or establish its compatibility qualification.</p>
 *
 * <p>Every supplied piece is an occurrence, including empty pieces. Null arrays, lists and pieces
 * are rejected. Construction snapshots the sequence of references. Slices share the immutable
 * source index and may split surrogate pairs, exactly like UTF-16 String ranges. Methods whose
 * names begin with {@code source} describe that shared original index, including for a slice.
 * {@link #length()}, {@link #charAt(int)}, {@link #reader()} and {@link #toString()} describe the
 * current view. This is indexed content, not a filesystem path or a filesystem resource owner.</p>
 */
public final class M3File implements CharSequence {
    private final Source source;
    private final int start;
    private final int length;

    private M3File(Source source, int start, int length) {
        this.source = source;
        this.start = start;
        this.length = length;
    }

    /**
     * Snapshots the supplied sequence and deduplicates equal String references locally.
     * Empty pieces remain indexed occurrences; no pieces produce an empty source index.
     *
     * @throws NullPointerException if the array or any piece is null
     * @throws IllegalArgumentException if the total UTF-16 length exceeds Integer.MAX_VALUE
     */
    public static M3File of(String... pieces) {
        return admit(Objects.requireNonNull(pieces, "pieces").clone());
    }

    /**
     * Snapshots a list of pieces under the same rules as {@link #of(String...)}.
     * The caller must not concurrently mutate the list while it is being snapshotted.
     */
    public static M3File of(List<String> pieces) {
        return admit(Objects.requireNonNull(pieces, "pieces").toArray(String[]::new));
    }

    private static M3File admit(String[] pieces) {
        if (pieces.length == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("too many indexed pieces");
        }
        // Validate before building the index; large repeated atoms cannot overflow an offset.
        long total = 0;
        for (int i = 0; i < pieces.length; i++) {
            total += Objects.requireNonNull(pieces[i], "pieces[" + i + "]").length();
            if (total > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("indexed UTF-16 length exceeds Integer.MAX_VALUE");
            }
        }

        Map<String, Integer> admission = new HashMap<>();
        List<String> atoms = new ArrayList<>();
        int[] handles = new int[pieces.length];
        int[] offsets = new int[pieces.length + 1];
        int position = 0;
        for (int i = 0; i < pieces.length; i++) {
            String piece = pieces[i];
            Integer handle = admission.get(piece);
            if (handle == null) {
                handle = atoms.size();
                admission.put(piece, handle);
                atoms.add(piece);
            }
            handles[i] = handle;
            position += piece.length();
            offsets[i + 1] = position;
        }
        M3FileTokenStore tokens = M3FileTokenStore.freeze(handles, handles.length, atoms.size());
        Source source = new Source(atoms.toArray(String[]::new), tokens, offsets);
        return new M3File(source, 0, (int) total);
    }

    /** UTF-16 code units in this view. */
    @Override
    public int length() {
        return length;
    }

    /** Locates an occurrence in O(log(sourceTokenCount)); no text materialization occurs. */
    @Override
    public char charAt(int index) {
        Objects.checkIndex(index, length);
        int absolute = start + index;
        int token = source.tokenAtOffset(absolute);
        return source.atom(token).charAt(absolute - source.offsets[token]);
    }

    /** Shares the original source index, including for ranges that split surrogate pairs. */
    @Override
    public M3File subSequence(int from, int to) {
        Objects.checkFromToIndex(from, to, length);
        if (from == 0 && to == length) return this;
        return new M3File(source, start + from, to - from);
    }

    /** Equivalent to {@link #subSequence(int, int)}. */
    public M3File slice(int from, int to) {
        return subSequence(from, to);
    }

    /** Start of this view in the original source's UTF-16 coordinates. */
    public int sourceStart() {
        return start;
    }

    /** Exclusive end of this view in the original source's UTF-16 coordinates. */
    public int sourceEnd() {
        return start + length;
    }

    /** Number of original pieces, including empty pieces, in the shared source index. */
    public int sourceTokenCount() {
        return source.tokens.size();
    }

    /** Number of distinct String spellings referenced by the shared source index. */
    public int sourceUniqueTokenCount() {
        return source.atoms.length;
    }

    /** Bytes per source occurrence handle: 1, 2 or 4; the empty index uses width 1. */
    public int sourceTokenWidthBytes() {
        return source.tokens.widthBytes();
    }

    /** Zero-based local atom handle for an original source occurrence. */
    public int sourceLocalHandleAt(int tokenIndex) {
        return source.tokens.get(tokenIndex);
    }

    /** The first supplied String reference with this occurrence's exact spelling. */
    public String sourceTokenAt(int tokenIndex) {
        return source.atom(tokenIndex);
    }

    /**
     * UTF-16 source prefix offset at a token boundary, from zero through sourceTokenCount().
     * Empty occurrences yield repeated offsets. The final boundary is the source length.
     */
    public int sourceUtf16Offset(int tokenBoundary) {
        return source.offsets[Objects.checkIndex(tokenBoundary, source.offsets.length)];
    }

    /** Exact primitive index payload size, excluding String/reference/object overhead. */
    public long sourceRetainedPrimitiveBytes() {
        return source.tokens.retainedPayloadBytes() + (long) Integer.BYTES * source.offsets.length;
    }

    /**
     * Opens an independent forward cursor over this view without flattening it.
     *
     * <p>Bulk reads copy directly from existing String atoms to the caller's array. A forward
     * traversal costs O(characters + crossed occurrences), after one binary search. Mark/reset
     * retain only an offset; the immutable source allows marks to remain valid regardless of
     * read-ahead distance. Skip is forward-only, as in Reader's base contract. Closing the Reader
     * releases its source reference and does not affect this view or other readers.</p>
     */
    public Reader reader() {
        return new CursorReader(this);
    }

    /**
     * Explicit, uncached String materialization. A complete single-atom view returns that atom.
     * No constructed output is retained by this source index or view.
     */
    @Override
    public String toString() {
        if (length == 0) return "";
        int token = source.tokenAtOffset(start);
        int offset = start - source.offsets[token];
        String atom = source.atom(token);
        if (offset == 0 && length == atom.length()) return atom;
        StringBuilder output = new StringBuilder(length);
        int remaining = length;
        while (remaining > 0) {
            atom = source.atom(token++);
            int amount = Math.min(remaining, atom.length() - offset);
            output.append(atom, offset, offset + amount);
            remaining -= amount;
            offset = 0;
        }
        return output.toString();
    }

    /** Owns references and indexing metadata only. It never owns another text spelling. */
    private static final class Source {
        private final String[] atoms;
        private final M3FileTokenStore tokens;
        private final int[] offsets;

        private Source(String[] atoms, M3FileTokenStore tokens, int[] offsets) {
            this.atoms = atoms;
            this.tokens = tokens;
            this.offsets = offsets;
        }

        private String atom(int token) {
            return atoms[tokens.get(token)];
        }

        /** Upper bound skips empty occurrences at the requested position, including at EOF. */
        private int tokenAtOffset(int absolute) {
            int low = 0;
            int high = offsets.length;
            while (low < high) {
                int middle = low + ((high - low) >>> 1);
                if (offsets[middle] <= absolute) low = middle + 1;
                else high = middle;
            }
            return low - 1;
        }
    }

    private static final class CursorReader extends Reader {
        private M3File file;
        private int position;
        private int marked;
        private int token;
        private int atomOffset;

        private CursorReader(M3File file) {
            this.file = file;
            locate(0);
        }

        private void ensureOpen() throws IOException {
            if (file == null) throw new IOException("Reader closed");
        }

        private void locate(int next) {
            position = next;
            int absolute = file.start + next;
            token = file.source.tokenAtOffset(absolute);
            atomOffset = token == file.source.tokens.size()
                    ? 0 : absolute - file.source.offsets[token];
        }

        /** Caller has already checked that this view still contains an unread code unit. */
        private String nextAtom() {
            String atom = file.source.atom(token);
            while (atomOffset == atom.length()) {
                token++;
                atomOffset = 0;
                atom = file.source.atom(token);
            }
            return atom;
        }

        @Override
        public int read() throws IOException {
            synchronized (lock) {
                ensureOpen();
                if (position == file.length) return -1;
                char value = nextAtom().charAt(atomOffset++);
                position++;
                return value;
            }
        }

        @Override
        public int read(char[] target, int offset, int count) throws IOException {
            synchronized (lock) {
                ensureOpen();
                Objects.requireNonNull(target, "target");
                Objects.checkFromIndexSize(offset, count, target.length);
                if (count == 0) return 0;
                if (position == file.length) return -1;
                int amount = Math.min(count, file.length - position);
                int copied = 0;
                while (copied < amount) {
                    String atom = nextAtom();
                    int chunk = Math.min(amount - copied, atom.length() - atomOffset);
                    atom.getChars(atomOffset, atomOffset + chunk, target, offset + copied);
                    atomOffset += chunk;
                    position += chunk;
                    copied += chunk;
                }
                return amount;
            }
        }

        @Override
        public long skip(long count) throws IOException {
            synchronized (lock) {
                ensureOpen();
                if (count < 0) throw new IllegalArgumentException("negative skip");
                int amount = (int) Math.min(count, (long) file.length - position);
                if (amount != 0) locate(position + amount);
                return amount;
            }
        }

        /** Reading an open in-memory view never blocks, including when it is at EOF. */
        @Override
        public boolean ready() throws IOException {
            synchronized (lock) {
                ensureOpen();
                return true;
            }
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public void mark(int readAheadLimit) throws IOException {
            synchronized (lock) {
                ensureOpen();
                if (readAheadLimit < 0) throw new IllegalArgumentException("negative read-ahead limit");
                marked = position;
            }
        }

        /** Before the first mark, reset restores the beginning of this immutable view. */
        @Override
        public void reset() throws IOException {
            synchronized (lock) {
                ensureOpen();
                locate(marked);
            }
        }

        @Override
        public void close() {
            synchronized (lock) {
                file = null;
            }
        }
    }
}
