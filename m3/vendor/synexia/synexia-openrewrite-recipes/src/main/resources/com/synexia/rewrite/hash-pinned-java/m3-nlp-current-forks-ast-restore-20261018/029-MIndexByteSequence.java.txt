// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable logical bytes backed by pooled or mapped canonical UTF-8 slices.
 *
 * <p>The sequence joins existing {@link MIndexString} or {@link MIndexLexicon} UTF-8 arenas
 * through read-only {@link ByteBuffer} descriptors. Indexed reads and slices do not materialize
 * a byte array; the explicit {@link #toByteArray()} method copies bytes. UTF-8 decoding traverses
 * the segments with bounded decoder buffering and materializes only its resulting text; other charsets retain the
 * String constructor copy path.
 * Values with unpaired UTF-16 surrogates are rejected on the MIndexString path because their
 * pooled compatibility bytes do not preserve the exact Java UTF-16 spelling. Lexicon entries are
 * valid canonical UTF-8 spellings.</p>
 */
public final class MIndexByteSequence {
    private final ByteBuffer[] segments;
    private final int[] ends;
    private final int length;
    private final int javaHash;

    private MIndexByteSequence(List<ByteBuffer> source) {
        segments = new ByteBuffer[source.size()];
        ends = new int[source.size()];
        long total = 0L;
        int hash = 1;
        for (int index = 0; index < source.size(); index++) {
            ByteBuffer segment = Objects.requireNonNull(source.get(index), "segment")
                    .asReadOnlyBuffer();
            segment.position(0);
            segment.limit(segment.capacity());
            segments[index] = segment;
            total = Math.addExact(total, segment.remaining());
            if (total > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("byte sequence is too large");
            }
            ends[index] = (int) total;
            for (int offset = 0; offset < segment.remaining(); offset++) {
                hash = 31 * hash + segment.get(offset);
            }
        }
        length = Math.toIntExact(total);
        javaHash = hash;
    }

    /** Join the exact pooled UTF-8 bytes of the supplied immutable values. */
    public static MIndexByteSequence of(MIndexString... values) {
        Objects.requireNonNull(values, "values");
        List<ByteBuffer> buffers = new ArrayList<>(values.length);
        for (MIndexString value : values) {
            MIndexString checked = Objects.requireNonNull(value, "value");
            if (checked.requiresUtf16Sidecar()) {
                throw new IllegalArgumentException("unpaired UTF-16 requires a sidecar");
            }
            buffers.add(checked.utf8View());
        }
        return new MIndexByteSequence(buffers);
    }

    /**
     * Join canonical UTF-8 spellings by lexical coordinate without decoding them.
     *
     * <p>The supplied lexicon remains the source of truth. Heap and mapped MIndex dictionaries
     * expose read-only slices of their existing arena; a third-party lexicon may use the
     * {@link MIndexLexicon#utf8View(LexicalRef)} compatibility copy path. Every coordinate is
     * checked against this lexicon before a slice is retained. Callers must keep the underlying
     * dictionary image immutable for the lifetime of a mapped view.</p>
     */
    public static MIndexByteSequence of(MIndexLexicon lexicon, LexicalRef... refs) {
        MIndexLexicon checked = Objects.requireNonNull(lexicon, "lexicon");
        Objects.requireNonNull(refs, "refs");
        List<ByteBuffer> buffers = new ArrayList<>(refs.length);
        for (LexicalRef ref : refs) {
            LexicalRef local = Objects.requireNonNull(ref, "ref");
            checked.requireLocal(local);
            buffers.add(Objects.requireNonNull(checked.utf8View(local), "utf8 view")
                    .slice().asReadOnlyBuffer());
        }
        return new MIndexByteSequence(buffers);
    }

    /** Join the canonical segments of an indexed composite without materializing its text. */
    public static MIndexByteSequence of(MIndexCompositeString value) {
        Objects.requireNonNull(value, "value");
        MIndexString[] segments = new MIndexString[value.segmentCount()];
        for (int index = 0; index < segments.length; index++) segments[index] = value.segment(index);
        return of(segments);
    }

    /** Join existing byte slices without decoding their possibly incomplete UTF-8 fragments. */
    public static MIndexByteSequence concat(MIndexByteSequence... values) {
        Objects.requireNonNull(values, "values");
        List<ByteBuffer> buffers = new ArrayList<>();
        for (MIndexByteSequence value : values) {
            MIndexByteSequence checked = Objects.requireNonNull(value, "value");
            for (ByteBuffer segment : checked.segments) buffers.add(segment);
        }
        return new MIndexByteSequence(buffers);
    }

    public int length() { return length; }
    public boolean isEmpty() { return length == 0; }
    public int segmentCount() { return segments.length; }

    /** Return a fresh read-only descriptor whose position can be changed by the caller. */
    public ByteBuffer segment(int index) {
        return segments[Objects.checkIndex(index, segments.length)].asReadOnlyBuffer();
    }

    public int segmentLength(int index) {
        int checked = Objects.checkIndex(index, segments.length);
        return ends[checked] - (checked == 0 ? 0 : ends[checked - 1]);
    }

    public byte byteAt(int index) {
        int checked = Objects.checkIndex(index, length);
        int segment = findSegment(checked);
        int start = segment == 0 ? 0 : ends[segment - 1];
        return segments[segment].get(checked - start);
    }

    /** Return a zero-copy byte-range view over this sequence. */
    public MIndexByteSequence slice(int start, int end) {
        Objects.checkFromToIndex(start, end, length);
        if (start == end) return new MIndexByteSequence(List.of());
        List<ByteBuffer> selected = new ArrayList<>();
        int previousEnd = 0;
        for (ByteBuffer segment : segments) {
            int segmentEnd = previousEnd + segment.remaining();
            int overlapStart = Math.max(start, previousEnd);
            int overlapEnd = Math.min(end, segmentEnd);
            if (overlapStart < overlapEnd) {
                ByteBuffer view = segment.asReadOnlyBuffer();
                view.position(overlapStart - previousEnd);
                view.limit(overlapEnd - previousEnd);
                selected.add(view.slice().asReadOnlyBuffer());
            }
            previousEnd = segmentEnd;
            if (previousEnd >= end) break;
        }
        return new MIndexByteSequence(selected);
    }

    /** Copy this sequence into a caller-owned destination buffer. */
    public void copyTo(ByteBuffer destination) {
        ByteBuffer checked = Objects.requireNonNull(destination, "destination");
        if (checked.remaining() < length) {
            throw new java.nio.BufferOverflowException();
        }
        for (ByteBuffer segment : segments) checked.put(segment.asReadOnlyBuffer());
    }

    /** Explicitly materialize one mutable-array-independent byte copy. */
    public byte[] toByteArray() {
        byte[] result = new byte[length];
        ByteBuffer destination = ByteBuffer.wrap(result);
        copyTo(destination);
        return result;
    }

    /**
     * Explicitly materialize one read-only direct buffer for JNI APIs that require contiguity.
     *
     * <p>The canonical sequence remains segmented and pooled. This method is the deliberate
     * native-pointer boundary: the returned buffer owns a copy, is direct, and is safe to pass to
     * {@code GetDirectBufferAddress} only for the duration of the caller's native operation.</p>
     */
    public ByteBuffer toDirectBuffer() {
        ByteBuffer destination = ByteBuffer.allocateDirect(length);
        copyTo(destination);
        destination.flip();
        return destination.asReadOnlyBuffer();
    }

    /**
     * Explicitly materialize one read-only direct buffer per logical segment for scatter/gather
     * JNI APIs. Empty source segments remain empty direct descriptors, preserving segment order.
     */
    public ByteBuffer[] toDirectSegments() {
        ByteBuffer[] result = new ByteBuffer[segments.length];
        for (int index = 0; index < segments.length; index++) {
            ByteBuffer destination = ByteBuffer.allocateDirect(segmentLength(index));
            copySegmentTo(index, destination);
            destination.flip();
            result[index] = destination.asReadOnlyBuffer();
        }
        return result;
    }

    /**
     * Build one reusable direct shadow for repeated JNI calls over this logical sequence.
     *
     * <p>The shadow is an explicit caller-owned admission boundary. It copies the immutable
     * pooled bytes once into one direct arena, then exposes read-only slices for the original
     * logical segments. Holding the returned object keeps the direct arena alive; the canonical
     * pooled sequence remains the content authority and is not changed into an off-heap store.</p>
     */
    public DirectShadow directShadow() {
        return new DirectShadow(this);
    }

    /**
     * Return an independent in-memory input cursor without materializing the joined bytes.
     * Closing the cursor has no effect, as with {@link java.io.ByteArrayInputStream}.
     */
    public InputStream openStream() {
        return new InputStream() {
            private int position;

            @Override public int read() {
                return position == length ? -1 : byteAt(position++) & 0xff;
            }

            @Override public int read(byte[] destination, int offset, int count) {
                Objects.checkFromIndexSize(offset, count,
                        Objects.requireNonNull(destination, "destination").length);
                if (count == 0) return 0;
                if (position == length) return -1;
                int requested = Math.min(count, length - position);
                int copied = 0;
                while (copied < requested) {
                    int segment = findSegment(position);
                    int start = segment == 0 ? 0 : ends[segment - 1];
                    int chunk = Math.min(requested - copied, ends[segment] - position);
                    segments[segment].get(position - start, destination, offset + copied, chunk);
                    position += chunk;
                    copied += chunk;
                }
                return copied;
            }

            @Override public long skip(long count) {
                int skipped = (int) Math.min(Math.max(count, 0L), length - position);
                position += skipped;
                return skipped;
            }

            @Override public int available() { return length - position; }
        };
    }

    /**
     * Decode UTF-8 with bounded JDK buffering; other charset instances use the String copy path.
     * Malformed UTF-8 uses the same replacement policy as {@code new String(bytes, UTF_8)}.
     */
    public String decode(Charset charset) {
        Charset checked = Objects.requireNonNull(charset, "charset");
        // Identity matters: a custom Charset named UTF-8 can have different decoder semantics.
        if (checked != StandardCharsets.UTF_8) return new String(toByteArray(), checked);
        try (Reader reader = new InputStreamReader(openStream(), checked)) {
            StringBuilder result = new StringBuilder(Math.min(length, 4096));
            char[] buffer = new char[2048];
            int count;
            while ((count = reader.read(buffer)) != -1) result.append(buffer, 0, count);
            // The String constructor preserves the previous fresh-result identity even when empty.
            return new String(result);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public String decodeUtf8() { return decode(StandardCharsets.UTF_8); }

    public boolean contentEquals(MIndexByteSequence other) {
        Objects.requireNonNull(other, "other");
        if (length != other.length) return false;
        for (int index = 0; index < length; index++) {
            if (byteAt(index) != other.byteAt(index)) return false;
        }
        return true;
    }

    @Override public int hashCode() { return javaHash; }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof MIndexByteSequence value
                && javaHash == value.javaHash
                && contentEquals(value);
    }

    @Override
    public String toString() {
        return "MIndexByteSequence[length=" + length + ", segments=" + segments.length + "]";
    }

    private int findSegment(int index) {
        int low = 0;
        int high = ends.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (ends[middle] <= index) low = middle + 1;
            else high = middle;
        }
        return low;
    }
    private void copySegmentTo(int index, ByteBuffer destination) {
        ByteBuffer source = segments[Objects.checkIndex(index, segments.length)].asReadOnlyBuffer();
        destination.put(source);
    }

    /** One-copy direct arena and read-only logical segment views for repeated native calls. */
    public static final class DirectShadow {
        private final ByteBuffer arena;
        private final int[] offsets;
        private final int[] lengths;
        private final int length;

        private DirectShadow(MIndexByteSequence source) {
            offsets = new int[source.segments.length];
            lengths = new int[source.segments.length];
            int cursor = 0;
            for (int index = 0; index < source.segments.length; index++) {
                offsets[index] = cursor;
                lengths[index] = source.segmentLength(index);
                cursor = Math.addExact(cursor, lengths[index]);
            }
            ByteBuffer writable = ByteBuffer.allocateDirect(cursor);
            source.copyTo(writable);
            writable.flip();
            arena = writable.asReadOnlyBuffer();
            length = cursor;
        }

        public int length() {
            return length;
        }

        public int segmentCount() {
            return offsets.length;
        }

        public int segmentLength(int index) {
            return lengths[Objects.checkIndex(index, lengths.length)];
        }

        /** Return a fresh read-only contiguous view over the one direct arena. */
        public ByteBuffer contiguous() {
            return view(0, length);
        }

        /** Return a fresh read-only direct view for one original logical segment. */
        public ByteBuffer segment(int index) {
            int checked = Objects.checkIndex(index, offsets.length);
            return view(offsets[checked], lengths[checked]);
        }

        /** Return read-only direct views while retaining the original segment order. */
        public ByteBuffer[] segments() {
            ByteBuffer[] result = new ByteBuffer[offsets.length];
            for (int index = 0; index < result.length; index++) result[index] = segment(index);
            return result;
        }

        private ByteBuffer view(int offset, int length) {
            ByteBuffer result = arena.duplicate();
            result.position(offset).limit(Math.addExact(offset, length));
            return result.slice().asReadOnlyBuffer();
        }
    }
}
