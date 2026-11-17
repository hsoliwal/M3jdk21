/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import sun.nio.cs.ArrayEncoder;

/**
 * M3JDK canonical String value.
 *
 * <p>This is the M3JDK counterpart of Synexia MIndexString. The representation invariant is
 * deliberately the same shape while the target name is different:</p>
 *
 * <pre>
 * M3String = canonical owner + packed coordinate
 * </pre>
 *
 * <p>Text payload belongs to canonical native/mapped atom owners. Composition belongs to
 * persistent tuple owner nodes. This value retains no byte[], char[], segment[], offset[] or
 * end[] payload. Contiguous arrays exist only as explicit compatibility shadows, which
 * {@link #materialize()} and {@link #charShadow()} fill in Java.</p>
 */
final class M3String implements CharSequence {
    private static final int SPAN_SHIFT = Integer.SIZE;
    private static final long SPAN_MASK = 0xffff_ffffL;

    private static final M3StringAtom EMPTY_OWNER =
            new M3StringAtom(
                    null,
                    0L,
                    (byte) 1,
                    true,
                    0,
                    String.COMPACT_STRINGS ? String.LATIN1 : String.UTF16,
                    0,
                    0L,
                    0x9e3779b97f4a7c15L);
    private static final M3String EMPTY = new M3String(EMPTY_OWNER, span(0, 0));

    private static volatile boolean ready;
    private static final ThreadLocal<Boolean> ADMITTING = new ThreadLocal<>();

    /*
     * Absolute M3 representation invariant: these are the only instance fields.
     * HotSpot/recipes/tests must fail if payload fields are added here.
     */
    private final M3StringOwner owner;
    private final long value;

    private M3String(M3StringOwner owner, long value) {
        this.owner = Objects.requireNonNull(owner, "owner");
        int start = start(value);
        int length = count(value);
        Objects.checkFromIndexSize(start, length, owner.length);
        this.value = value;
    }

    static M3String whole(M3StringOwner owner) {
        return owner.length == 0 ? EMPTY : new M3String(owner, span(0, owner.length));
    }

    static M3String range(M3StringOwner owner, int start, int length) {
        if (length == 0) return EMPTY;
        if (start == 0 && length == owner.length) return whole(owner);
        return new M3String(owner, span(start, length));
    }

    static M3String empty() {
        return EMPTY;
    }

    static void activate(String lexiconFile) {
        if (ready) return;
        synchronized (M3String.class) {
            if (ready) return;
            ADMITTING.set(Boolean.TRUE);
            try {
                M3StringPool.initializeLexicon(lexiconFile);
            } finally {
                ADMITTING.remove();
            }
            ready = true;
        }
    }

    static boolean ready() {
        return ready;
    }

    static boolean admissionEnabled() {
        return ready && ADMITTING.get() != Boolean.TRUE;
    }

    static M3String admitCodePoints(int[] source, int offset, int count) {
        Objects.requireNonNull(source, "source");
        ADMITTING.set(Boolean.TRUE);
        try {
            return M3StringPool.internCodePoints(source, offset, count);
        } finally {
            ADMITTING.remove();
        }
    }

    static M3String admitCompactBytes(
            byte[] source, int sourceOffset, int length, byte sourceCoder) {
        Objects.requireNonNull(source, "source");
        ADMITTING.set(Boolean.TRUE);
        try {
            return M3StringPool.internCompactBytes(source, sourceOffset, length, sourceCoder);
        } finally {
            ADMITTING.remove();
        }
    }

    static M3String admitLatin1Bytes(byte[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        ADMITTING.set(Boolean.TRUE);
        try {
            return M3StringPool.internLatin1Bytes(source, offset, length);
        } finally {
            ADMITTING.remove();
        }
    }

    static M3String admit(char[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        ADMITTING.set(Boolean.TRUE);
        try {
            return M3StringPool.internChars(source, offset, length);
        } finally {
            ADMITTING.remove();
        }
    }

    /**
     * Canonically composes independent mutable UTF-16 arrays without flattening them.
     *
     * <p>The outer reference lane is snapshotted before validation. Every non-empty array is
     * admitted as one canonical scalar M3 atom and the atoms are combined through the existing
     * persistent tuple owner. The caller must exclude concurrent mutation while admission is in
     * progress; no source array is retained after admission.</p>
     *
     * @return canonical composition, {@link #empty()} for zero logical length, or {@code null}
     *         when the canonical pool refuses any scalar admission
     */
    static M3String admitArrays(char[]... arrays) {
        char[][] snapshot = Objects.requireNonNull(arrays, "arrays").clone();
        int[] offsets = new int[snapshot.length];
        int[] lengths = new int[snapshot.length];
        for (int index = 0; index < snapshot.length; index++) {
            char[] source = Objects.requireNonNull(snapshot[index], "array");
            lengths[index] = source.length;
        }
        return admitArrayRanges(snapshot, offsets, lengths);
    }

    /**
     * Canonically composes selected ranges from independent UTF-16 arrays.
     *
     * <p>All lane cardinality, null, range and aggregate-length checks complete before the first
     * scalar is admitted. Zero-length ranges contribute no physical owner. Temporary Java arrays
     * contain only caller references and integer metadata; the resulting M3String retains only its
     * canonical owner and packed coordinate.</p>
     */
    static M3String admitArrayRanges(
            char[][] arrays, int[] offsets, int[] lengths) {
        char[][] sources = Objects.requireNonNull(arrays, "arrays").clone();
        int[] starts = Objects.requireNonNull(offsets, "offsets").clone();
        int[] counts = Objects.requireNonNull(lengths, "lengths").clone();
        if (sources.length != starts.length || sources.length != counts.length) {
            throw new IllegalArgumentException("char-array range lane cardinality mismatch");
        }

        long totalLength = 0L;
        int nonEmpty = 0;
        for (int index = 0; index < sources.length; index++) {
            char[] source = Objects.requireNonNull(sources[index], "array");
            Objects.checkFromIndexSize(starts[index], counts[index], source.length);
            totalLength += counts[index];
            if (totalLength > Integer.MAX_VALUE) {
                throw new OutOfMemoryError("Required length exceeds implementation limit");
            }
            if (counts[index] != 0) nonEmpty++;
        }
        if (totalLength == 0L) return EMPTY;

        ArrayList<M3String> admitted = new ArrayList<>(nonEmpty);
        ADMITTING.set(Boolean.TRUE);
        try {
            for (int index = 0; index < sources.length; index++) {
                int count = counts[index];
                if (count == 0) continue;
                M3String value =
                        M3StringPool.internChars(sources[index], starts[index], count);
                if (value == null) return null;
                admitted.add(value);
            }
        } finally {
            ADMITTING.remove();
        }
        return joinValues(admitted);
    }

    static M3String admit(byte[] compactValue, byte coder) {
        Objects.requireNonNull(compactValue, "compactValue");
        ADMITTING.set(Boolean.TRUE);
        try {
            return M3StringPool.internScalar(compactValue, coder);
        } finally {
            ADMITTING.remove();
        }
    }

    /**
     * The binary String-concat hot path composes canonical coordinates directly.
     * Keep the array-based join as the independent compatibility oracle for N-ary joins.
     * {@code null} when the pool refuses either piece (budget); the caller keeps its flat path.
     */
    static M3String join(String first, String second) {
        M3String left = canonicalize(first);
        if (left == null) return null;
        M3String right = canonicalize(second);
        if (right == null) return null;
        return M3StringPool.concat(left, right);
    }

    /** The composition, or {@code null} when the pool refuses a flat part (the caller stays flat). */
    static M3String join(String[] parts) {
        Objects.requireNonNull(parts, "parts");
        ArrayList<M3String> level = new ArrayList<>(parts.length);
        for (String part : parts) {
            String checked = Objects.requireNonNull(part, "part");
            M3String storage = checked.m3();
            if (storage == null && checked.length() != 0) {
                // Legacy/bootstrap wrapper stays flat. Admit its immutable spelling into the
                // canonical M3 owner only for this composition; do not attach duplicate storage
                // back to the old String object.
                storage = M3String.admit(checked.value(), checked.coder());
                if (storage == null) return null;
            }
            if (storage != null && storage.length() != 0) level.add(storage);
        }
        if (level.isEmpty()) return EMPTY;
        while (level.size() > 1) {
            ArrayList<M3String> next = new ArrayList<>((level.size() + 1) >>> 1);
            for (int index = 0; index < level.size(); index += 2) {
                if (index + 1 == level.size()) next.add(level.get(index));
                else next.add(M3StringPool.concat(level.get(index), level.get(index + 1)));
            }
            level = next;
        }
        return level.getFirst();
    }

    /**
     * Canonicalizes selected UTF-16 ranges and composes them as one balanced M3 owner.
     *
     * <p>All descriptor lanes are snapshotted and every range and the total logical length are
     * validated before the first pool admission. The result retains only canonical atom/tuple
     * owners; caller arrays are never retained and no flat concatenated character payload is allocated.
     * A {@code null} result means the local pool declined a scalar admission.</p>
     */
    static M3String joinCharRanges(char[][] sources, int[] starts, int[] lengths) {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(starts, "starts");
        Objects.requireNonNull(lengths, "lengths");
        if (sources.length != starts.length || sources.length != lengths.length) {
            throw new IllegalArgumentException("range descriptor lane lengths differ");
        }

        char[][] checkedSources = sources.clone();
        int[] checkedStarts = starts.clone();
        int[] checkedLengths = lengths.clone();
        long totalLength = 0L;
        for (int index = 0; index < checkedSources.length; index++) {
            char[] source = Objects.requireNonNull(checkedSources[index], "source");
            int start = checkedStarts[index];
            int length = checkedLengths[index];
            Objects.checkFromIndexSize(start, length, source.length);
            totalLength += length;
        }
        if (totalLength > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        if (totalLength == 0L) return EMPTY;

        M3String[] levels =
                new M3String[Integer.SIZE - Integer.numberOfLeadingZeros(checkedSources.length)];
        boolean wasAdmitting = Boolean.TRUE.equals(ADMITTING.get());
        ADMITTING.set(Boolean.TRUE);
        try {
            for (int index = 0; index < checkedSources.length; index++) {
                if (checkedLengths[index] == 0) continue;
                M3String carry =
                        M3StringPool.internChars(
                                checkedSources[index], checkedStarts[index], checkedLengths[index]);
                if (carry == null) return null;

                boolean placed = false;
                for (int level = 0; level < levels.length; level++) {
                    M3String previous = levels[level];
                    if (previous == null) {
                        levels[level] = carry;
                        placed = true;
                        break;
                    }
                    levels[level] = null;
                    carry = M3StringPool.concat(previous, carry);
                }
                if (!placed) throw new InternalError("M3 String range carry level exhausted");
            }

            M3String result = EMPTY;
            for (int level = levels.length - 1; level >= 0; level--) {
                M3String part = levels[level];
                if (part != null) {
                    result = result.length() == 0 ? part : M3StringPool.concat(result, part);
                }
            }
            return result;
        } finally {
            if (wasAdmitting) ADMITTING.set(Boolean.TRUE);
            else ADMITTING.remove();
        }
    }

    /**
     * Designated String.join adapter with O(log N) temporary M3 references.
     *
     * <p>The caller has already converted its CharSequence elements to String, so no
     * user toString() action is moved across canonical admission. Empty pieces are
     * excluded exactly as in join(String[]). Adjacent equal-size groups are folded
     * pairwise, then remaining high groups fold over low suffix groups. The resulting
     * canonical text always belongs to an M3 owner/range, not a temporary array.</p>
     *
     * @return the composition, or {@code null} when the pool refuses a flat piece (the caller
     *         takes its flat path)
     */
    static M3String joinDesignated(
            String prefix, String suffix, String delimiter, String[] elements, int size) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(suffix, "suffix");
        Objects.requireNonNull(delimiter, "delimiter");
        String[] checked = Objects.requireNonNull(elements, "elements");
        Objects.checkFromIndexSize(0, size, checked.length);

        long maximumPieces = size == 0 ? 2L : 2L * size + 1L;
        M3String[] levels =
                new M3String[Long.SIZE - Long.numberOfLeadingZeros(maximumPieces)];
        if (!joinAddDesignated(levels, prefix)) return null;
        if (size != 0) {
            if (!joinAddDesignated(levels, checked[0])) return null;
            for (int index = 1; index < size; index++) {
                if (!joinAddDesignated(levels, delimiter)) return null;
                if (!joinAddDesignated(levels, checked[index])) return null;
            }
        }
        if (!joinAddDesignated(levels, suffix)) return null;

        M3String result = EMPTY;
        for (int level = 0; level < levels.length; level++) {
            M3String value = levels[level];
            if (value != null) {
                result = result.length() == 0 ? value : M3StringPool.concat(value, result);
            }
        }
        return result;
    }

    /** False when the pool refuses the flat piece; an empty piece is skipped. */
    private static boolean joinAddDesignated(M3String[] levels, String source) {
        String checked = Objects.requireNonNull(source, "join piece");
        M3String carry = checked.m3();
        if (carry == null && checked.length() != 0) {
            carry = M3String.admit(checked.value(), checked.coder());
            if (carry == null) return false;
        }
        if (carry == null || carry.length() == 0) return true;
        for (int level = 0; level < levels.length; level++) {
            M3String previous = levels[level];
            if (previous == null) {
                levels[level] = carry;
                return true;
            }
            levels[level] = null;
            carry = M3StringPool.concat(previous, carry);
        }
        throw new InternalError("M3 String.join carry level exhausted");
    }

    /**
     * The String's own storage, or its spelling admitted for this composition; {@code null} when
     * the pool refuses the admission (budget), in which case the caller keeps its flat path.
     */
    static M3String canonicalize(String source) {
        String checked = Objects.requireNonNull(source, "source");
        M3String storage = checked.m3();
        if (storage != null) return storage;
        if (checked.length() == 0) return EMPTY;
        return admit(checked.value(), checked.coder());
    }

    private static M3String joinValues(ArrayList<M3String> values) {
        if (values.isEmpty()) return EMPTY;
        ArrayList<M3String> level = values;
        while (level.size() > 1) {
            ArrayList<M3String> next = new ArrayList<>((level.size() + 1) >>> 1);
            for (int index = 0; index < level.size(); index += 2) {
                if (index + 1 == level.size()) next.add(level.get(index));
                else next.add(M3StringPool.concat(level.get(index), level.get(index + 1)));
            }
            level = next;
        }
        return level.getFirst();
    }

    static M3String sliceOf(String source, int beginIndex, int endIndex) {
        Objects.requireNonNull(source, "source");
        M3String storage = source.m3();
        return storage == null ? null : storage.slice(beginIndex, endIndex);
    }

    M3String slice(int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, length());
        if (beginIndex == 0 && endIndex == length()) return this;
        int length = endIndex - beginIndex;
        return range(owner, Math.addExact(start(), beginIndex), length);
    }

    M3String concat(M3String other) {
        return M3StringPool.concat(this, Objects.requireNonNull(other, "other"));
    }

    /** Python-style tab expansion using only canonical owner/range compositions. */
    M3String expandTabs(int tabSize) {
        int sourceLength = length();
        int width = Math.max(0, tabSize);
        ArrayList<M3String> pieces = new ArrayList<>();
        long outputLength = sourceLength;
        long column = 0;
        int start = 0;
        boolean changed = false;
        for (int index = 0; index < sourceLength;) {
            int codePoint = Character.codePointAt(this, index);
            if (codePoint == '\t') {
                changed = true;
                if (start != index) {
                    pieces.add(slice(start, index));
                }
                long spaces = width == 0 ? 0 : (long) width - column % width;
                outputLength += spaces - 1;
                if (outputLength > Integer.MAX_VALUE) {
                    throw new OutOfMemoryError("Expanded String exceeds implementation limit");
                }
                if (spaces != 0) {
                    pieces.add(M3StringPool.internUnit(' ').repeat((int) spaces));
                }
                column += spaces;
                index++;
                start = index;
            } else {
                column = codePoint == '\r' || codePoint == '\n' ? 0 : column + 1;
                index += Character.charCount(codePoint);
            }
        }
        if (!changed) {
            return this;
        }
        if (start != sourceLength) {
            pieces.add(slice(start, sourceLength));
        }
        return joinValues(pieces);
    }

    M3String indent(int amount) {
        if (length() == 0) return EMPTY;

        int lineCount = 0;
        long contentUnits = 0L;
        for (int index = 0; index < length();) {
            int end = index;
            while (end < length()) {
                char unit = charAt(end);
                if (unit == '\n' || unit == '\r') break;
                end++;
            }
            lineCount++;
            contentUnits += end - index;
            if (end == length()) {
                index = end;
            } else if (charAt(end) == '\r'
                    && end + 1 < length()
                    && charAt(end + 1) == '\n') {
                index = end + 2;
            } else {
                index = end + 1;
            }
        }

        if (amount > 0) {
            long output = contentUnits + lineCount;
            output = Math.addExact(output, Math.multiplyExact((long) lineCount, amount));
            if (output > Integer.MAX_VALUE) {
                throw new OutOfMemoryError("Required length exceeds implementation limit");
            }
        }

        M3String newline = M3StringPool.internUnit('\n');
        M3String spaces =
                amount > 0 ? M3StringPool.internUnit(' ').repeat(amount) : EMPTY;
        ArrayList<M3String> pieces = new ArrayList<>(Math.max(2, lineCount * 3));
        for (int index = 0; index < length();) {
            int end = index;
            while (end < length()) {
                char unit = charAt(end);
                if (unit == '\n' || unit == '\r') break;
                end++;
            }

            M3String line = slice(index, end);
            if (amount > 0) {
                pieces.add(spaces);
                if (line.length() != 0) pieces.add(line);
            } else if (amount == Integer.MIN_VALUE) {
                int start = line.facts().stripStart;
                if (start < line.length()) pieces.add(line.slice(start, line.length()));
            } else if (amount < 0) {
                int start = Math.min(-amount, line.facts().stripStart);
                if (start < line.length()) pieces.add(line.slice(start, line.length()));
            } else if (line.length() != 0) {
                pieces.add(line);
            }
            pieces.add(newline);

            if (end == length()) {
                index = end;
            } else if (charAt(end) == '\r'
                    && end + 1 < length()
                    && charAt(end + 1) == '\n') {
                index = end + 2;
            } else {
                index = end + 1;
            }
        }
        return joinValues(pieces);
    }

    M3String stripIndent() {
        int sourceLength = length();
        if (sourceLength == 0) return EMPTY;

        char last = charAt(sourceLength - 1);
        boolean optOut = last == '\n' || last == '\r';
        int outdent = optOut ? 0 : Integer.MAX_VALUE;
        int lastLineStart = 0;
        int lastLineEnd = 0;

        for (int index = 0; index < sourceLength;) {
            int end = index;
            while (end < sourceLength) {
                char unit = charAt(end);
                if (unit == '\n' || unit == '\r') break;
                end++;
            }
            M3String line = slice(index, end);
            if (!optOut) {
                int leading = line.facts().stripStart;
                if (leading != line.length()) outdent = Math.min(outdent, leading);
            }
            lastLineStart = index;
            lastLineEnd = end;

            if (end == sourceLength) {
                index = end;
            } else if (charAt(end) == '\r'
                    && end + 1 < sourceLength
                    && charAt(end + 1) == '\n') {
                index = end + 2;
            } else {
                index = end + 1;
            }
        }

        if (!optOut) {
            M3String lastLine = slice(lastLineStart, lastLineEnd);
            M3StringFacts lastFacts = lastLine.facts();
            if (lastFacts.stripStart == lastLine.length()) {
                outdent = Math.min(outdent, lastLine.length());
            }
            if (outdent == Integer.MAX_VALUE) outdent = 0;
        }

        M3String newline = M3StringPool.internUnit('\n');
        ArrayList<M3String> pieces = new ArrayList<>();
        boolean firstLine = true;
        for (int index = 0; index < sourceLength;) {
            int end = index;
            while (end < sourceLength) {
                char unit = charAt(end);
                if (unit == '\n' || unit == '\r') break;
                end++;
            }

            if (!firstLine) pieces.add(newline);
            firstLine = false;

            M3String line = slice(index, end);
            M3StringFacts facts = line.facts();
            int firstNonWhitespace = facts.stripStart;
            int lastNonWhitespace = facts.stripEnd;
            int incidentalWhitespace = Math.min(outdent, firstNonWhitespace);
            if (firstNonWhitespace <= lastNonWhitespace
                    && incidentalWhitespace < lastNonWhitespace) {
                pieces.add(line.slice(incidentalWhitespace, lastNonWhitespace));
            }

            if (end == sourceLength) {
                index = end;
            } else if (charAt(end) == '\r'
                    && end + 1 < sourceLength
                    && charAt(end + 1) == '\n') {
                index = end + 2;
            } else {
                index = end + 1;
            }
        }
        if (optOut) pieces.add(newline);
        return joinValues(pieces);
    }

    M3String repeat(int repetitions) {
        if (repetitions < 0) throw new IllegalArgumentException("count is negative: " + repetitions);
        if (repetitions == 0 || length() == 0) return EMPTY;
        if (repetitions == 1) return this;
        Math.multiplyExact(length(), repetitions);

        M3String result = EMPTY;
        M3String base = this;
        int remaining = repetitions;
        while (remaining != 0) {
            if ((remaining & 1) != 0) result = M3StringPool.concat(result, base);
            remaining >>>= 1;
            if (remaining != 0) base = M3StringPool.concat(base, base);
        }
        return result;
    }

    M3StringOwner owner() {
        return owner;
    }

    long coordinate() {
        return value;
    }

    int start() {
        return start(value);
    }

    int end() {
        return start() + length();
    }

    @Override
    public int length() {
        return count(value);
    }

    byte coder() {
        return owner.coder;
    }

    long structuralHash64() {
        return owner.structuralHash64;
    }

    boolean sameCoordinate(M3String other) {
        return other != null && owner == other.owner && value == other.value;
    }

    long identityHash64() {
        long identity = Integer.toUnsignedLong(System.identityHashCode(owner));
        long mixed = identity ^ Long.rotateLeft(value, 17) ^ owner.structuralHash64;
        mixed ^= mixed >>> 30;
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }

    @Override
    public char charAt(int index) {
        Objects.checkIndex(index, length());
        return owner.charAt(start() + index);
    }

    @Override
    public M3String subSequence(int beginIndex, int endIndex) {
        return slice(beginIndex, endIndex);
    }

    IntStream charsStream() {
        return StreamSupport.intStream(new CharsSpliterator(this, 0, length()), false);
    }

    IntStream codePointsStream() {
        return StreamSupport.intStream(new CodePointsSpliterator(this, 0, length()), false);
    }

    int hashCodeValue() {
        return isWholeOwner() ? owner.javaHash : facts().javaHash;
    }

    /**
     * True when {@link #hashCodeValue()} answers in O(1): a whole owner, or prepared range facts.
     * Never prepares facts.
     */
    boolean hashKnown() {
        return isWholeOwner() || factsIfPrepared() != null;
    }

    /**
     * True when this String's canonical Java hash is already known (whole owner, or prepared range
     * facts) and differs from {@code hash}. Never prepares facts; absence proves nothing.
     */
    boolean hashKnownToDiffer(int hash) {
        if (isWholeOwner()) return owner.javaHash != hash;
        M3StringFacts prepared = factsIfPrepared();
        return prepared != null && prepared.javaHash != hash;
    }

    M3StringFacts facts() {
        return start() == 0 && length() == owner.length
                ? owner.facts()
                : owner.rangeFacts(value, this);
    }

    M3StringFacts factsIfPrepared() {
        return start() == 0 && length() == owner.length
                ? owner.factsIfPrepared()
                : owner.rangeFactsIfPrepared(value);
    }

    static final class TokenSnapshot {
        final byte[] packedAscii;
        final int[] offsets;
        final int[] lengths;
        final int[] sequence;

        TokenSnapshot(byte[] packedAscii, int[] offsets, int[] lengths, int[] sequence) {
            this.packedAscii = packedAscii;
            this.offsets = offsets;
            this.lengths = lengths;
            this.sequence = sequence;
        }
    }

    TokenSnapshot asciiTokenSnapshot(
            int maxSequence, int maxVocabulary, int maxTokenLength, int maxPackedBytes) {
        if (maxSequence < 0 || maxVocabulary < 0 || maxTokenLength < 1 || maxPackedBytes < 0) {
            throw new IllegalArgumentException("invalid M3 token snapshot budget");
        }
        if (length() == 0) {
            return new TokenSnapshot(new byte[0], new int[0], new int[0], new int[0]);
        }
        if (maxSequence == 0 || maxVocabulary == 0 || maxPackedBytes == 0) return null;

        int uniqueCapacity = Math.min(8, maxVocabulary);
        M3StringOwner[] owners = new M3StringOwner[uniqueCapacity];
        int[] starts = new int[uniqueCapacity];
        int[] lengths = new int[uniqueCapacity];
        int[] offsets = new int[uniqueCapacity];
        int[] slots = new int[16];

        int sequenceCapacity = Math.min(8, maxSequence);
        int[] sequence = new int[sequenceCapacity];
        byte[] packed = new byte[Math.min(64, maxPackedBytes)];

        int unique = 0;
        int sequenceSize = 0;
        int packedSize = 0;
        M3StringLeafCursor cursor = new M3StringLeafCursor(this, false);
        while (cursor.next()) {
            M3StringOwner scalar = cursor.owner();
            int scalarStart = cursor.start();
            int remaining = cursor.length();
            while (remaining != 0) {
                if (sequenceSize == maxSequence) return null;
                int tokenLength = Math.min(remaining, maxTokenLength);
                int tokenStart = scalarStart;

                if ((long) (unique + 1) * 10L >= (long) slots.length * 6L
                        && unique < maxVocabulary) {
                    int[] grown = new int[Math.multiplyExact(slots.length, 2)];
                    for (int index = 0; index < unique; index++) {
                        int slot = tokenSlot(
                                owners[index], starts[index], lengths[index], grown.length - 1);
                        while (grown[slot] != 0) slot = (slot + 1) & (grown.length - 1);
                        grown[slot] = index + 1;
                    }
                    slots = grown;
                }

                int slot = tokenSlot(scalar, tokenStart, tokenLength, slots.length - 1);
                int vocabulary = -1;
                while (slots[slot] != 0) {
                    int candidate = slots[slot] - 1;
                    if (owners[candidate] == scalar
                            && starts[candidate] == tokenStart
                            && lengths[candidate] == tokenLength) {
                        vocabulary = candidate;
                        break;
                    }
                    slot = (slot + 1) & (slots.length - 1);
                }

                if (vocabulary < 0) {
                    if (unique == maxVocabulary || tokenLength > maxPackedBytes - packedSize) {
                        return null;
                    }
                    if (unique == owners.length) {
                        int capacity = Math.min(
                                maxVocabulary,
                                Math.max(unique + 1, Math.multiplyExact(owners.length, 2)));
                        owners = Arrays.copyOf(owners, capacity);
                        starts = Arrays.copyOf(starts, capacity);
                        lengths = Arrays.copyOf(lengths, capacity);
                        offsets = Arrays.copyOf(offsets, capacity);
                    }
                    if (packedSize + tokenLength > packed.length) {
                        int capacity = Math.min(
                                maxPackedBytes,
                                Math.max(
                                        packedSize + tokenLength,
                                        Math.multiplyExact(packed.length, 2)));
                        packed = Arrays.copyOf(packed, capacity);
                    }
                    int offset = packedSize;
                    for (int index = 0; index < tokenLength; index++) {
                        char unit = scalar.charAt(tokenStart + index);
                        if (unit >= 128) return null;
                        packed[packedSize++] = (byte) unit;
                    }
                    vocabulary = unique++;
                    owners[vocabulary] = scalar;
                    starts[vocabulary] = tokenStart;
                    lengths[vocabulary] = tokenLength;
                    offsets[vocabulary] = offset;
                    slots[slot] = vocabulary + 1;
                }

                if (sequenceSize == sequence.length) {
                    int capacity = Math.min(
                            maxSequence,
                            Math.max(
                                    sequenceSize + 1,
                                    Math.multiplyExact(sequence.length, 2)));
                    sequence = Arrays.copyOf(sequence, capacity);
                }
                sequence[sequenceSize++] = vocabulary;
                scalarStart += tokenLength;
                remaining -= tokenLength;
            }
        }

        return new TokenSnapshot(
                Arrays.copyOf(packed, packedSize),
                Arrays.copyOf(offsets, unique),
                Arrays.copyOf(lengths, unique),
                Arrays.copyOf(sequence, sequenceSize));
    }

    private static int tokenSlot(M3StringOwner owner, int start, int length, int mask) {
        int hash = System.identityHashCode(owner);
        hash = 31 * hash + start;
        hash = 31 * hash + length;
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        hash *= 0x846c_a68b;
        hash ^= hash >>> 16;
        return hash & mask;
    }

    /**
     * Thread-confined logical UTF-16 reader over persistent canonical tuple coordinates.
     * It resolves the complete visible scalar run once, for reads in either direction.
     * Only canonical owners and primitive bounds are retained; no text or native pointer copy.
     */
    static final class CodeUnitCursor implements IntUnaryOperator {
        private final M3String source;
        private M3StringOwner scalar;
        private int logicalBegin;
        private int logicalEnd;
        private int scalarOffset;

        CodeUnitCursor(M3String source) {
            this.source = source;
        }

        @Override
        public int applyAsInt(int index) {
            return at(index);
        }

        char at(int index) {
            Objects.checkIndex(index, source.length());
            prepare(index);
            return scalar.charAt(scalarOffset + index - logicalBegin);
        }

        private void prepare(int index) {
            if (scalar == null || index < logicalBegin || index >= logicalEnd) {
                seek(index);
            }
        }

        private void seek(int index) {
            int position = source.start() + index;
            M3StringOwner current = source.owner();
            int before = index;
            int after = source.length() - index;
            while (current instanceof M3StringTuple tuple) {
                int leftLength = tuple.left.length();
                if (position < leftLength) {
                    // The selected left child clips only the visible interval after the read.
                    after = Math.min(after, leftLength - position);
                    position += tuple.left.start();
                    current = tuple.left.owner();
                } else {
                    position -= leftLength;
                    // Clip before translating through a possibly nonzero child start.
                    before = Math.min(before, position);
                    position += tuple.right.start();
                    current = tuple.right.owner();
                }
            }
            scalar = current;
            logicalBegin = index - before;
            logicalEnd = index + after;
            scalarOffset = position - before;
        }
    }

    private static final class CharsSpliterator implements Spliterator.OfInt {
        private final M3String source;
        private final CodeUnitCursor units;
        private int index;
        private final int fence;

        CharsSpliterator(M3String source, int index, int fence) {
            this.source = Objects.requireNonNull(source, "source");
            this.units = new CodeUnitCursor(this.source);
            this.index = index;
            this.fence = fence;
        }

        @Override
        public OfInt trySplit() {
            int lo = index;
            int mid = (lo + fence) >>> 1;
            if (mid <= lo) return null;
            index = mid;
            return new CharsSpliterator(source, lo, mid);
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            if (index >= fence) return false;
            action.accept(units.at(index++));
            return true;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            for (int at = index; at < fence; at++) action.accept(units.at(at));
            index = fence;
        }

        @Override
        public long estimateSize() {
            return fence - index;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED
                    | Spliterator.IMMUTABLE
                    | Spliterator.SIZED
                    | Spliterator.SUBSIZED;
        }
    }

    private static final class CodePointsSpliterator implements Spliterator.OfInt {
        private final M3String source;
        private final CodeUnitCursor units;
        private int index;
        private final int fence;

        CodePointsSpliterator(M3String source, int index, int fence) {
            this.source = Objects.requireNonNull(source, "source");
            this.units = new CodeUnitCursor(this.source);
            this.index = index;
            this.fence = fence;
        }

        @Override
        public OfInt trySplit() {
            int lo = index;
            int mid = (lo + fence) >>> 1;
            if (mid <= lo) return null;
            if (mid < fence
                    && mid > lo
                    && Character.isLowSurrogate(units.at(mid))
                    && Character.isHighSurrogate(units.at(mid - 1))) {
                mid--;
            }
            if (mid <= lo) return null;
            index = mid;
            return new CodePointsSpliterator(source, lo, mid);
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            if (index >= fence) return false;
            char first = units.at(index++);
            if (Character.isHighSurrogate(first) && index < fence) {
                char second = units.at(index);
                if (Character.isLowSurrogate(second)) {
                    index++;
                    action.accept(Character.toCodePoint(first, second));
                    return true;
                }
            }
            action.accept(first);
            return true;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            Objects.requireNonNull(action, "action");
            while (tryAdvance(action)) {
                // exact UTF-16 traversal; tryAdvance owns surrogate-pair boundaries.
            }
        }

        @Override
        public long estimateSize() {
            return fence - index;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.IMMUTABLE;
        }
    }

    private boolean isWholeOwner() {
        return start() == 0 && length() == owner.length;
    }

    /** {@link #equalityGate}: the gates prove neither equal nor unequal text. */
    static final int GATE_UNKNOWN = -1;
    /** {@link #equalityGate}: the gates prove unequal text. */
    static final int GATE_UNEQUAL = 0;
    /** {@link #equalityGate}: the gates prove equal text. */
    static final int GATE_EQUAL = 1;

    /**
     * The O(1) equality gates of two Strings of which at least one is M3-backed, in front of
     * the stock comparison of their values (decision 50): equal for the same coordinate or the
     * same immediate text; unequal for different lengths, different whole-owner Java hashes,
     * different prepared range Java hashes, or a cached flat hash that differs from a known M3
     * hash. Never prepares facts and never reads the text.
     */
    static int equalityGate(
            String left, M3String leftStorage, String right, M3String rightStorage) {
        if (leftStorage == null || rightStorage == null) {
            M3String storage = leftStorage != null ? leftStorage : rightStorage;
            String flat = leftStorage != null ? right : left;
            return flat.length() != storage.length() || flat.cachedHashDiffers(storage)
                    ? GATE_UNEQUAL : GATE_UNKNOWN;
        }
        if (leftStorage.sameCoordinate(rightStorage)) return GATE_EQUAL;
        boolean whole = leftStorage.isWholeOwner() && rightStorage.isWholeOwner();
        if (whole && M3StringImmediate.sameText(leftStorage.owner, rightStorage.owner)) {
            return GATE_EQUAL;
        }
        if (leftStorage.length() != rightStorage.length()) return GATE_UNEQUAL;
        if (whole) {
            return leftStorage.owner.javaHash != rightStorage.owner.javaHash
                    ? GATE_UNEQUAL : GATE_UNKNOWN;
        }
        M3StringFacts leftFacts = leftStorage.factsIfPrepared();
        M3StringFacts rightFacts = rightStorage.factsIfPrepared();
        return leftFacts != null && rightFacts != null && leftFacts.javaHash != rightFacts.javaHash
                ? GATE_UNEQUAL : GATE_UNKNOWN;
    }

    boolean contentEquals(String other) {
        Objects.requireNonNull(other, "other");
        M3String that = other.m3();
        if (that != null) {
            if (sameCoordinate(that)) return true;
            if (isWholeOwner() && that.isWholeOwner()
                    && owner instanceof M3StringImmediate && that.owner instanceof M3StringImmediate) {
                return M3StringImmediate.sameText(owner, that.owner);
            }
            if (that.length() != length()) return false;
            if (isWholeOwner() && that.isWholeOwner()) {
                if (owner.javaHash != that.owner.javaHash) return false;
            } else {
                M3StringFacts leftFacts = factsIfPrepared();
                M3StringFacts rightFacts = that.factsIfPrepared();
                if (leftFacts != null && rightFacts != null
                        && leftFacts.javaHash != rightFacts.javaHash) {
                    return false;
                }
            }
        } else if (other.length() != length()) {
            return false;
        }
        return that != null
                ? comparePrefix(that, length()) == 0
                : compareFlatPrefix(other, length()) == 0;
    }

    /**
     * Compare a logical prefix using existing scalar runs. Owner identity proves equality only for
     * the same offset and run length; different owners or tuple segmentation still compare text.
     */
    int comparePrefix(M3String other, int count) {
        Objects.requireNonNull(other, "other");
        Objects.checkFromIndexSize(0, count, length());
        Objects.checkFromIndexSize(0, count, other.length());
        if (count == 0 || sameCoordinate(other)) return 0;
        if (!(owner instanceof M3StringTuple) && !(other.owner instanceof M3StringTuple)) {
            return compareScalarRange(owner, start(), other.owner, other.start(), count);
        }
        CodeUnitCursor left = new CodeUnitCursor(this);
        CodeUnitCursor right = new CodeUnitCursor(other);
        for (int index = 0; index < count; ) {
            left.prepare(index);
            right.prepare(index);
            int runEnd = Math.min(count, Math.min(left.logicalEnd, right.logicalEnd));
            int difference = compareScalarRange(
                    left.scalar, left.scalarOffset + index - left.logicalBegin,
                    right.scalar, right.scalarOffset + index - right.logicalBegin,
                    runEnd - index);
            if (difference != 0) return difference;
            index = runEnd;
        }
        return 0;
    }

    private static int compareScalarRange(
            M3StringOwner left, int leftStart, M3StringOwner right, int rightStart, int count) {
        if (left == right && leftStart == rightStart) return 0;
        if (left instanceof M3StringAtom leftAtom && right instanceof M3StringAtom rightAtom) {
            return leftAtom.compareRange(leftStart, rightAtom, rightStart, count);
        }
        for (int index = 0; index < count; index++) {
            int difference = left.charAt(leftStart + index) - right.charAt(rightStart + index);
            if (difference != 0) return difference;
        }
        return 0;
    }

    /** Mixed flat/M3 comparison never admits, flattens, or prepares facts for either side. */
    int compareFlatPrefix(String other, int count) {
        Objects.requireNonNull(other, "other");
        Objects.checkFromIndexSize(0, count, length());
        Objects.checkFromIndexSize(0, count, other.length());
        CodeUnitCursor units = owner instanceof M3StringTuple ? new CodeUnitCursor(this) : null;
        for (int index = 0; index < count; index++) {
            char unit = units != null ? units.at(index) : owner.charAt(start() + index);
            int difference = unit - other.charAt(index);
            if (difference != 0) return difference;
        }
        return 0;
    }

    void getChars(int sourceBegin, int sourceEnd, char[] destination, int destinationBegin) {
        String.checkBoundsBeginEnd(sourceBegin, sourceEnd, length());
        Objects.checkFromIndexSize(destinationBegin, sourceEnd - sourceBegin, destination.length);
        owner.getChars(
                Math.addExact(start(), sourceBegin),
                Math.addExact(start(), sourceEnd),
                destination,
                destinationBegin);
    }

    void getBytes(byte[] destination, int sourceBegin, int destinationBegin, byte destinationCoder,
            int count) {
        Objects.requireNonNull(destination, "destination");
        if (destinationCoder != String.LATIN1 && destinationCoder != String.UTF16) {
            throw new IllegalArgumentException("invalid destination String coder");
        }
        Objects.checkFromIndexSize(sourceBegin, count, length());
        Objects.checkFromIndexSize(
                (long) destinationBegin << destinationCoder,
                (long) count << destinationCoder,
                destination.length);
        owner.getBytes(
                Math.addExact(start(), sourceBegin),
                Math.addExact(start(), sourceBegin + count),
                destination,
                destinationBegin,
                destinationCoder);
    }

    byte[] encode(Charset charset) {
        Charset checked = Objects.requireNonNull(charset, "charset");
        // Charset.equals compares names; custom encoders must retain their behavior.
        if (checked == StandardCharsets.UTF_8) {
            return encodeUtf8();
        }
        if (checked == StandardCharsets.ISO_8859_1) {
            return encodeSingleByte(false);
        }
        if (checked == StandardCharsets.US_ASCII) {
            return encodeSingleByte(true);
        }
        return encodeWithEncoder(checked);
    }

    private byte[] encodeUtf8() {
        M3StringFacts prepared = facts();
        if (prepared.utf8Length < 0) {
            throw new OutOfMemoryError("Required UTF-8 length exceeds implementation limit");
        }
        byte[] output = new byte[prepared.utf8Length];
        if (prepared.ascii) {
            getBytes(output, 0, 0, String.LATIN1, length());
            return output;
        }
        // Resolve each visible scalar run once; the ASCII return stays cursor-free.
        CodeUnitCursor units = new CodeUnitCursor(this);
        int target = 0;
        for (int index = 0; index < length(); index++) {
            char unit = units.at(index);
            if (unit < 0x80) {
                output[target++] = (byte) unit;
            } else if (unit < 0x800) {
                output[target++] = (byte) (0xc0 | (unit >>> 6));
                output[target++] = (byte) (0x80 | (unit & 0x3f));
            } else if (Character.isHighSurrogate(unit)
                    && index + 1 < length()
                    && Character.isLowSurrogate(units.at(index + 1))) {
                int codePoint = Character.toCodePoint(unit, units.at(++index));
                output[target++] = (byte) (0xf0 | (codePoint >>> 18));
                output[target++] = (byte) (0x80 | ((codePoint >>> 12) & 0x3f));
                output[target++] = (byte) (0x80 | ((codePoint >>> 6) & 0x3f));
                output[target++] = (byte) (0x80 | (codePoint & 0x3f));
            } else if (Character.isSurrogate(unit)) {
                // JDK21 String UTF-8 replacement is the single byte '?'.
                output[target++] = '?';
            } else {
                output[target++] = (byte) (0xe0 | (unit >>> 12));
                output[target++] = (byte) (0x80 | ((unit >>> 6) & 0x3f));
                output[target++] = (byte) (0x80 | (unit & 0x3f));
            }
        }
        if (target != output.length) {
            throw new InternalError("M3String UTF-8 precompute length mismatch");
        }
        return output;
    }

    private byte[] encodeSingleByte(boolean asciiOnly) {
        if (!asciiOnly && coder() == String.LATIN1) {
            // A Latin-1 coded owner holds no wider unit: copy without preparing facts.
            byte[] output = new byte[length()];
            getBytes(output, 0, 0, String.LATIN1, length());
            return output;
        }
        M3StringFacts prepared = facts();
        if ((asciiOnly && prepared.ascii) || (!asciiOnly && prepared.latin1)) {
            byte[] output = new byte[length()];
            getBytes(output, 0, 0, String.LATIN1, length());
            return output;
        }
        byte[] output = new byte[prepared.codePointCount];
        int target = 0;
        for (int index = 0; index < length(); index++) {
            char unit = charAt(index);
            int limit = asciiOnly ? 0x7f : 0xff;
            if (unit <= limit) {
                output[target++] = (byte) unit;
                continue;
            }
            if (Character.isHighSurrogate(unit)
                    && index + 1 < length()
                    && Character.isLowSurrogate(charAt(index + 1))) {
                index++;
            }
            output[target++] = '?';
        }
        if (target != output.length) {
            throw new InternalError("M3String single-byte precompute length mismatch");
        }
        return output;
    }

    byte[] encodeNoRepl(Charset charset) {
        Charset checked = Objects.requireNonNull(charset, "charset");
        // Charset.equals compares names; custom encoders must retain their behavior.
        if (checked == StandardCharsets.UTF_8) {
            return encodeUtf8NoRepl();
        }
        if (checked == StandardCharsets.ISO_8859_1) {
            // The coder answers for a Latin-1 coded owner; facts are read only for UTF-16.
            if (coder() == String.LATIN1 || facts().latin1) {
                byte[] output = new byte[length()];
                getBytes(output, 0, 0, String.LATIN1, length());
                return output;
            }
            for (int index = 0; index < length(); index++) {
                if (charAt(index) > 0xff) throw unmappable(index, 1);
            }
            throw new InternalError("M3String Latin-1 fact mismatch");
        }
        if (checked == StandardCharsets.US_ASCII && facts().ascii) {
            byte[] output = new byte[length()];
            getBytes(output, 0, 0, String.LATIN1, length());
            return output;
        }
        return encodeWithEncoderNoRepl(checked);
    }

    byte[] encodeUtf8NoRepl() {
        M3StringFacts prepared = facts();
        if (prepared.unpairedSurrogateCount == 0) {
            return encodeUtf8();
        }
        if (prepared.utf8Length < 0) {
            throw new OutOfMemoryError("Required UTF-8 length exceeds implementation limit");
        }
        byte[] output = new byte[prepared.utf8Length];
        if (prepared.ascii) {
            getBytes(output, 0, 0, String.LATIN1, length());
            return output;
        }
        int target = 0;
        for (int index = 0; index < length(); index++) {
            char unit = charAt(index);
            if (unit < 0x80) {
                output[target++] = (byte) unit;
            } else if (unit < 0x800) {
                output[target++] = (byte) (0xc0 | (unit >>> 6));
                output[target++] = (byte) (0x80 | (unit & 0x3f));
            } else if (Character.isHighSurrogate(unit)
                    && index + 1 < length()
                    && Character.isLowSurrogate(charAt(index + 1))) {
                int codePoint = Character.toCodePoint(unit, charAt(++index));
                output[target++] = (byte) (0xf0 | (codePoint >>> 18));
                output[target++] = (byte) (0x80 | ((codePoint >>> 12) & 0x3f));
                output[target++] = (byte) (0x80 | ((codePoint >>> 6) & 0x3f));
                output[target++] = (byte) (0x80 | (codePoint & 0x3f));
            } else if (Character.isSurrogate(unit)) {
                throw unmappable(index, 1);
            } else {
                output[target++] = (byte) (0xe0 | (unit >>> 12));
                output[target++] = (byte) (0x80 | ((unit >>> 6) & 0x3f));
                output[target++] = (byte) (0x80 | (unit & 0x3f));
            }
        }
        if (target != output.length) {
            throw new InternalError("M3String UTF-8 no-replacement length mismatch");
        }
        return output;
    }

    private byte[] encodeWithEncoderNoRepl(Charset charset) {
        CharsetEncoder encoder = charset.newEncoder();
        int length = length();
        int capacity = (int) (length * (double) encoder.maxBytesPerChar());
        byte[] output = new byte[capacity];
        if (length == 0) return output;

        ByteBuffer bytes = ByteBuffer.wrap(output);
        CharBuffer input = CharBuffer.wrap(this);
        try {
            CoderResult result = encoder.encode(input, bytes, true);
            if (!result.isUnderflow()) result.throwException();
            result = encoder.flush(bytes);
            if (!result.isUnderflow()) result.throwException();
        } catch (CharacterCodingException failure) {
            throw new IllegalArgumentException(failure);
        }
        return bytes.position() == output.length
                ? output
                : Arrays.copyOf(output, bytes.position());
    }

    private static IllegalArgumentException unmappable(int offset, int length) {
        String message = "malformed input off : " + offset + ", length : " + length;
        return new IllegalArgumentException(message, new UnmappableCharacterException(length));
    }

    private byte[] encodeWithEncoder(Charset charset) {
        CharsetEncoder encoder = charset.newEncoder();
        int length = length();
        if (encoder instanceof ArrayEncoder arrayEncoder
                && arrayEncoder.isASCIICompatible()) {
            M3StringFacts prepared = facts();
            if (prepared.ascii) {
                byte[] output = new byte[length];
                getBytes(output, 0, 0, String.LATIN1, length);
                return output;
            }
        }
        int capacity = (int) (length * (double) encoder.maxBytesPerChar());
        byte[] output = new byte[capacity];
        if (length == 0) return output;
        // Match String's callback order, including no configuration for empty input.
        encoder.onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);

        ByteBuffer bytes = ByteBuffer.wrap(output);
        CharBuffer input = CharBuffer.wrap(this);
        try {
            CoderResult result = encoder.encode(input, bytes, true);
            if (!result.isUnderflow()) result.throwException();
            result = encoder.flush(bytes);
            if (!result.isUnderflow()) result.throwException();
        } catch (CharacterCodingException impossibleWithReplacement) {
            throw new Error(impossibleWithReplacement);
        }
        return bytes.position() == output.length
                ? output
                : Arrays.copyOf(output, bytes.position());
    }

    /**
     * Compact-String compatibility shadow in the canonical coder of this text: a fresh array
     * filled in Java by the owners' bulk reads, Latin-1 exactly when Compact Strings are on and
     * every unit fits, else UTF-16. The owner coder only bounds it: a UTF-16 owner (a tuple ORs
     * its children's coders) can hold an all-Latin-1 range. Never cached by M3String or its
     * owners.
     */
    byte[] materialize() {
        //FLLDCIM: LAYOUT - decision 50: shadows are filled in Java; the JNI round trip is gone
        //FLLDCIM: LAYOUT - decision 50: the shadow is laid out in the canonical coder
        int length = length();
        boolean latin1Owner = coder() == String.LATIN1;
        M3StringFacts prepared = latin1Owner ? null : factsIfPrepared();
        if (String.COMPACT_STRINGS && (latin1Owner || prepared != null && prepared.latin1)) {
            byte[] latin1 = new byte[length];
            if (length != 0) getBytes(latin1, 0, 0, String.LATIN1, length);
            return latin1;
        }
        byte[] utf16 = StringUTF16.newBytesFor(length);
        if (length == 0) return utf16;
        getBytes(utf16, 0, 0, String.UTF16, length);
        if (!String.COMPACT_STRINGS || prepared != null) return utf16;
        byte[] latin1 = new byte[length];
        return StringUTF16.compress(utf16, 0, latin1, 0, length) == length ? latin1 : utf16;
    }

    /** Fresh UTF-16 array shadow, filled in Java by the owners' bulk reads. */
    char[] charShadow() {
        int length = length();
        char[] chars = new char[length];
        if (length != 0) getChars(0, length, chars, 0);
        return chars;
    }

    boolean mayContain(M3String needle) {
        M3String checked = Objects.requireNonNull(needle, "needle");
        M3StringFacts sourceFacts = factsIfPrepared();
        M3StringFacts needleFacts = checked.factsIfPrepared();
        return sourceFacts == null
                || needleFacts == null
                || sourceFacts.mayContain(needleFacts);
    }

    boolean startsWith(M3String prefix, int offset) {
        M3String checked = Objects.requireNonNull(prefix, "prefix");
        if (offset < 0 || offset > length() - checked.length()) return false;
        if (offset == 0 && sameCoordinate(checked)) return true;
        M3StringFacts sourceFacts = factsIfPrepared();
        M3StringFacts prefixFacts = checked.factsIfPrepared();
        if (sourceFacts != null && prefixFacts != null) {
            if (!sourceFacts.mayContain(prefixFacts)) return false;
            if (offset == 0 && !sourceFacts.prefixMayMatch(prefixFacts)) return false;
            if (offset == length() - checked.length() && !sourceFacts.suffixMayMatch(prefixFacts)) {
                return false;
            }
        }
        // A long prefix of a composite would otherwise descend the balanced
        // tuple DAG once per UTF-16 unit. Reuse the existing scalar-run cursor:
        // at most two request-local cursors, no leaf directory or copied text.
        // Short/flat comparisons retain their allocation-free direct path.
        if (checked.length() >= 64
                && (owner instanceof M3StringTuple
                        || checked.owner() instanceof M3StringTuple)) {
            CodeUnitCursor sourceUnits = new CodeUnitCursor(this);
            CodeUnitCursor prefixUnits = new CodeUnitCursor(checked);
            for (int index = 0; index < checked.length(); index++) {
                if (sourceUnits.at(offset + index) != prefixUnits.at(index)) {
                    return false;
                }
            }
            return true;
        }
        for (int index = 0; index < checked.length(); index++) {
            if (charAt(offset + index) != checked.charAt(index)) return false;
        }
        return true;
    }

    int indexOf(char unit, int fromIndex, int endIndex) {
        M3StringFacts prepared = factsIfPrepared();
        if (prepared != null && !prepared.mayContainCodeUnit(unit)) return -1;
        return M3StringPositionPrecompute.indexOf(this, unit, fromIndex, endIndex);
    }

    int lastIndexOf(char unit, int fromIndex) {
        M3StringFacts prepared = factsIfPrepared();
        if (prepared != null && !prepared.mayContainCodeUnit(unit)) return -1;
        return M3StringPositionPrecompute.lastIndexOf(this, unit, fromIndex);
    }

    int indexOfCodePoint(int codePoint, int fromIndex, int endIndex) {
        if (!Character.isValidCodePoint(codePoint)) return -1;
        if (Character.isBmpCodePoint(codePoint)) {
            return indexOf((char) codePoint, fromIndex, endIndex);
        }

        char high = Character.highSurrogate(codePoint);
        char low = Character.lowSurrogate(codePoint);
        M3StringFacts prepared = factsIfPrepared();
        if (prepared != null
                && (!prepared.mayContainCodeUnit(high)
                        || !prepared.mayContainCodeUnit(low))) {
            return -1;
        }

        int end = Math.min(length(), endIndex);
        int candidate = indexOf(high, fromIndex, Math.max(0, end - 1));
        while (candidate >= 0) {
            if (candidate + 1 < end && charAt(candidate + 1) == low) return candidate;
            candidate = indexOf(high, candidate + 1, Math.max(0, end - 1));
        }
        return -1;
    }

    int lastIndexOfCodePoint(int codePoint, int fromIndex) {
        if (!Character.isValidCodePoint(codePoint)) return -1;
        if (Character.isBmpCodePoint(codePoint)) {
            return lastIndexOf((char) codePoint, fromIndex);
        }

        char high = Character.highSurrogate(codePoint);
        char low = Character.lowSurrogate(codePoint);
        M3StringFacts prepared = factsIfPrepared();
        if (prepared != null
                && (!prepared.mayContainCodeUnit(high)
                        || !prepared.mayContainCodeUnit(low))) {
            return -1;
        }

        int candidate = lastIndexOf(high, Math.min(fromIndex, length() - 2));
        while (candidate >= 0) {
            if (charAt(candidate + 1) == low) return candidate;
            candidate = lastIndexOf(high, candidate - 1);
        }
        return -1;
    }

    int indexOf(M3String needle, int fromIndex) {
        return indexOf(needle, fromIndex, length());
    }

    int indexOf(M3String needle, int fromIndex, int endIndex) {
        M3String checked = Objects.requireNonNull(needle, "needle");
        int end = Math.min(length(), endIndex);
        int from = Math.clamp(fromIndex, 0, length());
        if (checked.length() == 0) return Math.min(from, end);
        if (checked.length() == 1) return indexOf(checked.charAt(0), from, end);
        if (from > end - checked.length() || !mayContain(checked)) return -1;

        M3StringSearchPrecompute.Plan plan = M3StringSearchPrecompute.prepare(checked);
        if (plan != null) {
            if (!M3StringSearchPrecompute.mayContain(this, plan)) return -1;
            return M3StringSearchPrecompute.indexOf(this, checked, plan, from, end);
        }

        return M3StringSearchPrecompute.indexOfNoPlan(this, checked, from, end);
    }

    int lastIndexOf(M3String needle, int fromIndex) {
        M3String checked = Objects.requireNonNull(needle, "needle");
        int maximumStart = Math.min(fromIndex, length() - checked.length());
        if (maximumStart < 0) return -1;
        if (checked.length() == 0) return maximumStart;
        if (checked.length() == 1) return lastIndexOf(checked.charAt(0), maximumStart);
        if (!mayContain(checked)) return -1;

        M3StringSearchPrecompute.Plan plan = M3StringSearchPrecompute.prepare(checked);
        if (plan != null) {
            if (!M3StringSearchPrecompute.mayContain(this, plan)) return -1;
            return M3StringSearchPrecompute.lastIndexOf(this, checked, plan, maximumStart);
        }

        return M3StringSearchPrecompute.lastIndexOfNoPlan(this, checked, maximumStart);
    }

    M3String translateEscapes() {
        ArrayList<M3String> pieces = null;
        int cursor = 0;
        int index = 0;
        while (index < length()) {
            if (charAt(index) != '\\') {
                index++;
                continue;
            }

            int slash = index++;
            char escaped = index < length() ? charAt(index++) : '\0';
            if (pieces == null) pieces = new ArrayList<>();
            if (cursor < slash) pieces.add(slice(cursor, slash));

            boolean emit = true;
            switch (escaped) {
                case 'b' -> escaped = '\b';
                case 'f' -> escaped = '\f';
                case 'n' -> escaped = '\n';
                case 'r' -> escaped = '\r';
                case 's' -> escaped = ' ';
                case 't' -> escaped = '\t';
                case '\'', '"', '\\' -> {
                    // as is
                }
                case '0', '1', '2', '3', '4', '5', '6', '7' -> {
                    int limit = Math.min(index + (escaped <= '3' ? 2 : 1), length());
                    int code = escaped - '0';
                    while (index < limit) {
                        char next = charAt(index);
                        if (next < '0' || next > '7') break;
                        index++;
                        code = (code << 3) | (next - '0');
                    }
                    escaped = (char) code;
                }
                case '\n' -> emit = false;
                case '\r' -> {
                    if (index < length() && charAt(index) == '\n') index++;
                    emit = false;
                }
                default -> {
                    String message = String.format(
                            "Invalid escape sequence: \\%c \\\\u%04X",
                            escaped,
                            (int) escaped);
                    throw new IllegalArgumentException(message);
                }
            }

            if (emit) pieces.add(M3StringPool.internUnit(escaped));
            cursor = index;
        }

        if (pieces == null) return this;
        if (cursor < length()) pieces.add(slice(cursor, length()));
        return joinValues(pieces);
    }

    /**
     * Unicode/locale case mapping over canonical UTF-16 coordinates. The original String is
     * used only by the JDK's context-sensitive rules; its character/code-point readers access
     * these same canonical owners. No whole-input compatibility shadow is requested.
     */
    M3String caseMap(String source, java.util.Locale locale, boolean upper) {
        Objects.requireNonNull(locale, "locale");
        CodeUnitCursor units = new CodeUnitCursor(this);
        int length = length();
        int first = 0;
        // Match StringUTF16's first-change scan before invoking conditional rules. In particular,
        // Lithuanian dot removal alone must not initiate an otherwise unchanged mapping pass.
        while (first < length) {
            char unit = units.at(first);
            if (Character.isSurrogate(unit)
                    || unit != (upper ? Character.toUpperCaseEx(unit)
                                      : Character.toLowerCase(unit))) break;
            first++;
        }
        if (first == length) return this;

        String language = locale.getLanguage();
        boolean localeDependent = language == "tr" || language == "az" || language == "lt";
        ArrayList<M3String> pieces = null;
        char[] output = null;
        int used = 0;
        int unchanged = 0;
        for (int index = first; index < length;) {
            int codePoint = units.at(index);
            int count = 1;
            if (Character.isHighSurrogate((char) codePoint) && index + 1 < length) {
                char low = units.at(index + 1);
                if (Character.isLowSurrogate(low)) {
                    codePoint = Character.toCodePoint((char) codePoint, low);
                    count = 2;
                }
            }
            boolean conditional = localeDependent
                    || (!upper && (codePoint == '\u03a3' || codePoint == '\u0130'));
            int mapped = upper
                    ? (conditional ? ConditionalSpecialCasing.toUpperCaseEx(source, index, locale)
                                   : Character.toUpperCaseEx(codePoint))
                    : (conditional ? ConditionalSpecialCasing.toLowerCaseEx(source, index, locale)
                                   : Character.toLowerCase(codePoint));
            if (mapped != codePoint) {
                if (pieces == null) {
                    pieces = new ArrayList<>();
                    output = new char[1024];
                }
                used = appendCaseRange(pieces, output, used, units, unchanged, index);
                if (mapped == Character.ERROR) {
                    char[] expansion = upper
                            ? (conditional
                                    ? ConditionalSpecialCasing.toUpperCaseCharArray(
                                            source, index, locale)
                                    : Character.toUpperCaseCharArray(codePoint))
                            : ConditionalSpecialCasing.toLowerCaseCharArray(source, index, locale);
                    for (char unit : expansion) {
                        used = appendCaseUnit(pieces, output, used, unit);
                    }
                } else if (Character.isBmpCodePoint(mapped)) {
                    used = appendCaseUnit(pieces, output, used, (char) mapped);
                } else {
                    used = appendCaseUnit(pieces, output, used, Character.highSurrogate(mapped));
                    used = appendCaseUnit(pieces, output, used, Character.lowSurrogate(mapped));
                }
                unchanged = index + count;
            }
            index += count;
        }
        // The only unchanged input reaching this point was stopped by a surrogate. The JDK
        // UTF-16 extended path returns a fresh String for it; share payload, not wrapper identity.
        if (pieces == null) return new M3String(owner, value);
        used = appendCaseRange(pieces, output, used, units, unchanged, length);
        if (used != 0) flushCaseChunk(pieces, output, used);
        return joinValues(pieces);
    }

    /** Preserve long unchanged runs; absorb short runs into the bounded output chunk. */
    private int appendCaseRange(ArrayList<M3String> pieces, char[] output, int used,
                                CodeUnitCursor units, int begin, int end) {
        if (end - begin >= output.length) {
            if (used != 0) flushCaseChunk(pieces, output, used);
            pieces.add(slice(begin, end));
            return 0;
        }
        for (int index = begin; index < end; index++) {
            used = appendCaseUnit(pieces, output, used, units.at(index));
        }
        return used;
    }

    private static int appendCaseUnit(ArrayList<M3String> pieces, char[] output,
                                      int used, char unit) {
        if (used == output.length) {
            flushCaseChunk(pieces, output, used);
            used = 0;
        }
        output[used++] = unit;
        return used;
    }

    private static void flushCaseChunk(ArrayList<M3String> pieces, char[] output, int used) {
        M3String chunk = M3StringPool.internChars(output, 0, used);
        if (chunk == null) {
            // Pool budgets may refuse a multi-unit atom. The existing single-unit lane never
            // refuses; compose at most one bounded chunk of those canonical units at a time.
            // This rare path retains more tuple metadata, but never a full spelling shadow.
            ArrayList<M3String> units = new ArrayList<>(used);
            for (int index = 0; index < used; index++) {
                units.add(M3StringPool.internUnit(output[index]));
            }
            chunk = joinValues(units);
        }
        pieces.add(chunk);
    }

    M3String asciiCase(boolean upper) {
        M3StringFacts prepared = facts();
        if (!prepared.ascii) {
            throw new IllegalStateException("ASCII case mapping requires ASCII M3 String");
        }

        ArrayList<M3String> pieces = null;
        int cursor = 0;
        for (int index = 0; index < length(); index++) {
            char unit = charAt(index);
            char mapped = upper
                    ? (unit >= 'a' && unit <= 'z' ? (char) (unit - ('a' - 'A')) : unit)
                    : (unit >= 'A' && unit <= 'Z' ? (char) (unit + ('a' - 'A')) : unit);
            if (mapped == unit) continue;

            if (pieces == null) pieces = new ArrayList<>();
            if (cursor < index) pieces.add(slice(cursor, index));
            pieces.add(M3StringPool.internUnit(mapped));
            cursor = index + 1;
        }
        if (pieces == null) return this;
        if (cursor < length()) pieces.add(slice(cursor, length()));
        return joinValues(pieces);
    }

    M3String replace(char oldChar, char newChar) {
        if (oldChar == newChar) return this;
        M3StringFacts prepared = factsIfPrepared();
        if (prepared != null && !prepared.mayContainCodeUnit(oldChar)) return this;

        int found = indexOf(oldChar, 0, length());
        if (found < 0) return this;

        M3String replacement = M3StringPool.internUnit(newChar);
        ArrayList<M3String> pieces = new ArrayList<>();
        int cursor = 0;
        while (found >= 0) {
            if (cursor < found) pieces.add(slice(cursor, found));
            pieces.add(replacement);
            cursor = found + 1;
            found = cursor < length() ? indexOf(oldChar, cursor, length()) : -1;
        }
        if (cursor < length()) pieces.add(slice(cursor, length()));
        return joinValues(pieces);
    }

    M3String replaceEmptyTarget(M3String replacement) {
        M3String checkedReplacement = Objects.requireNonNull(replacement, "replacement");
        if (checkedReplacement.length() == 0) {
            // String.replace("", "") returns a distinct but content-equal String; preserve
            // canonical storage while letting the String wrapper provide object freshness.
            return this;
        }
        ArrayList<M3String> pieces = new ArrayList<>();
        pieces.add(checkedReplacement);
        for (int index = 0; index < length(); index++) {
            pieces.add(slice(index, index + 1));
            pieces.add(checkedReplacement);
        }
        return joinValues(pieces);
    }

    /** {@link #literalRegex}: a whole-input match of the regex is equality with it. */
    static final int LITERAL_MATCHES = 1;
    /** {@link #literalRegex}: find, split and replace see exactly its UTF-16 occurrences. */
    static final int LITERAL_SEARCH = 2;
    /** The metacharacters of a java.util.regex pattern outside a class, escape included. */
    private static final String REGEX_METACHARACTERS = "\\.^$|?*+()[]{}";

    /**
     * The one owner of "this regex is a literal" (compile-time map G7): the literal rules that a
     * regex compiled without flags meets, as bits.
     * <ul>
     * <li>{@link #LITERAL_MATCHES}: no metacharacter and no escape. A whole-input match then
     *     compares the code points of the regex with those of the input, which are equal exactly
     *     when the strings are equal: {@code Pattern.matches(regex, s) == regex.equals(s)}, for
     *     the empty regex and for surrogates too.</li>
     * <li>{@link #LITERAL_SEARCH}: also non-empty and free of surrogates. Find, split and
     *     replace then see exactly the non-overlapping occurrences that {@code String.indexOf}
     *     finds. A surrogate refuses, because regex search walks code points and never starts or
     *     ends inside a pair; the empty regex refuses, because it matches between all units.</li>
     * </ul>
     * {@code null} meets neither rule.
     */
    static int literalRegex(String regex) {
        if (regex == null) return 0;
        int rules = regex.isEmpty() ? LITERAL_MATCHES : LITERAL_MATCHES | LITERAL_SEARCH;
        for (int index = 0; index < regex.length(); index++) {
            char unit = regex.charAt(index);
            if (REGEX_METACHARACTERS.indexOf(unit) >= 0) return 0;
            if (Character.isSurrogate(unit)) rules &= ~LITERAL_SEARCH;
        }
        return rules;
    }

    /** The regex meets {@link #LITERAL_SEARCH}: find, split and replace are its occurrences. */
    static boolean isLiteralRegex(String regex) {
        return (literalRegex(regex) & LITERAL_SEARCH) != 0;
    }

    /** The regex meets {@link #LITERAL_MATCHES}: a whole-input match is equality with it. */
    static boolean isLiteralMatchesRegex(String regex) {
        return (literalRegex(regex) & LITERAL_MATCHES) != 0;
    }

    /** Conservative BMP literal subset; all regex/replacement syntax stays in Matcher. */
    static boolean isLiteralRegexReplacement(String regex, String replacement) {
        if (!isLiteralRegex(regex) || replacement == null) return false;
        for (int index = 0; index < replacement.length(); index++) {
            char unit = replacement.charAt(index);
            if (unit == '$' || unit == '\\') return false;
        }
        return true;
    }

    /**
     * Literal regex lane: the regex is a flat literal located through the receiver's mixed-side
     * search and never admitted; only the replacement joins the result. {@code null} when the pool
     * refuses the replacement (the caller takes the Pattern path).
     */
    String replaceLiteralRegex(String original, String regex, String replacement, boolean firstOnly) {
        int found = original.indexOf(regex);
        if (found < 0) return original;
        M3String admitted = canonicalize(replacement);
        if (admitted == null) return null;
        // A match must produce a String even when composition aliases an input descriptor.
        return new String(replaceMatches(null, original, regex, admitted, found, firstOnly));
    }

    /** Replaces every occurrence of a non-empty M3 target from its first match {@code found}. */
    M3String replaceAt(M3String target, M3String replacement, int found) {
        M3String checkedTarget = Objects.requireNonNull(target, "target");
        if (checkedTarget.length() == 0) {
            throw new IllegalArgumentException("empty literal target handled by String compatibility path");
        }
        return replaceMatches(checkedTarget, null, null, replacement, found, false);
    }

    /**
     * Transient admission for {@code String.replace}: a flat target is searched through the
     * receiver's mixed-side {@code indexOf(String, int)} and never enters the pool; the result is
     * composed from slices of this storage and the replacement only.
     */
    M3String replaceFlatTarget(String receiver, String target, M3String replacement, int found) {
        if (target.isEmpty()) {
            throw new IllegalArgumentException("empty literal target handled by String compatibility path");
        }
        return replaceMatches(null, receiver, target, replacement, found, false);
    }

    /** Exactly one of {@code target} (M3 needle) and {@code receiver}+{@code flatTarget} drives the search. */
    private M3String replaceMatches(M3String target, String receiver, String flatTarget,
                                    M3String replacement, int found, boolean firstOnly) {
        M3String checkedReplacement = Objects.requireNonNull(replacement, "replacement");
        int targetLength = target != null ? target.length() : flatTarget.length();
        ArrayList<M3String> pieces = new ArrayList<>();
        long outputLength = 0L;
        int cursor = 0;
        while (found >= 0) {
            if (cursor < found) {
                M3String prefix = slice(cursor, found);
                pieces.add(prefix);
                outputLength += prefix.length();
            }
            if (checkedReplacement.length() != 0) {
                pieces.add(checkedReplacement);
                outputLength += checkedReplacement.length();
            }
            if (outputLength > Integer.MAX_VALUE) {
                throw new OutOfMemoryError("Required length exceeds implementation limit");
            }
            cursor = found + targetLength;
            found = !firstOnly && cursor <= length() - targetLength
                    ? (target != null ? indexOf(target, cursor) : receiver.indexOf(flatTarget, cursor))
                    : -1;
        }
        if (cursor < length()) {
            M3String suffix = slice(cursor, length());
            pieces.add(suffix);
            outputLength += suffix.length();
        }
        if (outputLength > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        return joinValues(pieces);
    }

    static int pow31(int length) {
        int result = 1;
        int base = 31;
        for (int remaining = length; remaining != 0; remaining >>>= 1) {
            if ((remaining & 1) != 0) result *= base;
            base *= base;
        }
        return result;
    }

    private static long span(int start, int length) {
        if (start < 0 || length < 0) throw new IllegalArgumentException("negative M3 range");
        return ((long) start << SPAN_SHIFT) | (length & SPAN_MASK);
    }

    private static int start(long coordinate) {
        return (int) (coordinate >>> SPAN_SHIFT);
    }

    private static int count(long coordinate) {
        return (int) (coordinate & SPAN_MASK);
    }
}
