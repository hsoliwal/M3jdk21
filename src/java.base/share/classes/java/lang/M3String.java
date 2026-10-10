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
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import jdk.internal.util.ArraysSupport;
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
 * end[] payload. Contiguous arrays exist only as explicit JNI compatibility shadows.</p>
 */
final class M3String implements CharSequence {
    private static final int SPAN_SHIFT = Integer.SIZE;
    private static final long SPAN_MASK = 0xffff_ffffL;
    /** Shared zero-length VM compatibility shadow, created by JNI after M3 activation. */
    private static volatile byte[] emptyCompatibilityShadow;

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

    /**
     * Whether every unit of this range fits Latin-1 (A22): a narrow owner answers at once; a
     * wide owner's range is read in bulk windows and the scan stops at the first wide unit.
     * Far cheaper than the facts when only this bit is needed.
     */
    boolean contentIsLatin1() {
        if (coder() == String.LATIN1) return true;
        int length = length();
        char[] window = null;
        for (int from = 0; from < length; ) {
            int count = windowUnits(from, length - from);
            if (window == null || window.length < count) window = new char[count];
            getChars(from, from + count, window, 0);
            for (int index = 0; index < count; index++) {
                if (window[index] > 0xff) return false;
            }
            from += count;
        }
        return true;
    }

    private static final int LATIN1_SCAN_WINDOW = 1024;

    /** The first index that is not whitespace in the stock code-point sense (A24). */
    int stripStart() {
        M3StringFacts prepared = factsIfPrepared();
        return prepared != null ? prepared.stripStart : leadingNonWhitespace(true);
    }

    /** One past the last index that is not whitespace in the stock code-point sense (A24). */
    int stripEnd() {
        M3StringFacts prepared = factsIfPrepared();
        return prepared != null ? prepared.stripEnd : trailingNonWhitespace(true);
    }

    /** The first index whose unit is above space (A24). */
    int trimStart() {
        M3StringFacts prepared = factsIfPrepared();
        return prepared != null ? prepared.trimStart : leadingNonWhitespace(false);
    }

    /** One past the last index whose unit is above space (A24). */
    int trimEnd() {
        M3StringFacts prepared = factsIfPrepared();
        return prepared != null ? prepared.trimEnd : trailingNonWhitespace(false);
    }

    /**
     * Leading whitespace scanned in bulk windows with early exit (A24): strip uses
     * {@code Character.isWhitespace}, trim units at or below space. A surrogate is never
     * whitespace, paired or not, so the unit scan stops where the stock code-point scan stops.
     */
    private int leadingNonWhitespace(boolean strip) {
        int length = length();
        char[] window = new char[Math.min(length, FIRST_WINDOW)];
        for (int from = 0; from < length; ) {
            if (from > 0 && window.length < LATIN1_SCAN_WINDOW) {
                window = new char[Math.min(length - from, LATIN1_SCAN_WINDOW)];
            }
            int count = Math.min(window.length, length - from);
            getChars(from, from + count, window, 0);
            for (int at = 0; at < count; at++) {
                if (!isWhitespaceUnit(window[at], strip)) return from + at;
            }
            from += count;
        }
        return length;
    }

    private int trailingNonWhitespace(boolean strip) {
        int length = length();
        char[] window = new char[Math.min(length, FIRST_WINDOW)];
        for (int end = length; end > 0; ) {
            if (end < length && window.length < LATIN1_SCAN_WINDOW) {
                window = new char[Math.min(end, LATIN1_SCAN_WINDOW)];
            }
            int count = Math.min(window.length, end);
            getChars(end - count, end, window, 0);
            for (int at = count - 1; at >= 0; at--) {
                if (!isWhitespaceUnit(window[at], strip)) return end - count + at + 1;
            }
            end -= count;
        }
        return 0;
    }

    /** Units in the first window of a whitespace scan: most texts decide within it. */
    private static final int FIRST_WINDOW = 32;

    private static boolean isWhitespaceUnit(char unit, boolean strip) {
        return strip ? Character.isWhitespace(unit) : unit <= ' ';
    }

    /** The code point count of this range (A24): a narrow owner's length, prepared facts, or a bulk count of surrogate pairs. */
    int codePointCountValue() {
        int length = length();
        if (coder() == String.LATIN1) return length;
        M3StringFacts prepared = factsIfPrepared();
        if (prepared != null) return prepared.codePointCount;
        char[] window = new char[Math.min(length, LATIN1_SCAN_WINDOW)];
        int count = length;
        char previous = 0;
        for (int from = 0; from < length; from += window.length) {
            int chunk = Math.min(window.length, length - from);
            getChars(from, from + chunk, window, 0);
            for (int at = 0; at < chunk; at++) {
                char unit = window[at];
                if (Character.isLowSurrogate(unit) && Character.isHighSurrogate(previous)) {
                    count--;
                    previous = 0;
                } else {
                    previous = unit;
                }
            }
        }
        return count;
    }

    /** Whether every unit is ASCII (A24): prepared facts, or a bulk scan with early exit. */
    boolean contentIsAscii() {
        M3StringFacts prepared = factsIfPrepared();
        if (prepared != null) return prepared.ascii;
        int length = length();
        if (coder() == String.LATIN1) {
            byte[] window = new byte[Math.min(length, LATIN1_SCAN_WINDOW)];
            for (int from = 0; from < length; from += window.length) {
                int count = Math.min(window.length, length - from);
                getBytes(window, from, 0, String.LATIN1, count);
                if (StringCoding.countPositives(window, 0, count) != count) return false;
            }
            return true;
        }
        char[] window = new char[Math.min(length, LATIN1_SCAN_WINDOW)];
        for (int from = 0; from < length; from += window.length) {
            int count = Math.min(window.length, length - from);
            getChars(from, from + count, window, 0);
            for (int at = 0; at < count; at++) {
                if (window[at] > 0x7f) return false;
            }
        }
        return true;
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

    /**
     * The compact value of this range read once in bulk (A17), narrowed to Latin-1 when every
     * unit allows it (as a flat String of the same spelling would be stored): the stock
     * spliterators fold the array with the characteristics a flat String has. Transient scratch
     * of the range's size.
     */
    byte[] compactValue() {
        byte coder = coder();
        int length = length();
        byte[] out = new byte[length << coder];
        getBytes(out, 0, 0, coder, length);
        if (coder == String.UTF16) {
            byte[] narrow = new byte[length];
            if (StringUTF16.compress(out, 0, narrow, 0, length) == length) return narrow;
        }
        return out;
    }

    IntStream charsStream() {
        byte[] value = compactValue();
        return StreamSupport.intStream(
                value.length == length()
                        ? new StringLatin1.CharsSpliterator(value, Spliterator.IMMUTABLE)
                        : new StringUTF16.CharsSpliterator(value, Spliterator.IMMUTABLE),
                false);
    }

    IntStream codePointsStream() {
        byte[] value = compactValue();
        return StreamSupport.intStream(
                value.length == length()
                        ? new StringLatin1.CharsSpliterator(value, Spliterator.IMMUTABLE)
                        : new StringUTF16.CodePointsSpliterator(value, Spliterator.IMMUTABLE),
                false);
    }

    int hashCodeValue() {
        if (isWholeOwner()) return owner.javaHash;
        M3StringFacts prepared = factsIfPrepared();
        return prepared != null ? prepared.javaHash : bulkJavaHash();
    }

    /** Units per window when the Java hash is folded from a bulk read. */
    private static final int HASH_WINDOW = 4096;

    /**
     * The String.hashCode polynomial over this range's units read in bulk windows (A23): the
     * facts are not computed for the hash alone, and the consumers that need them prepare them
     * later. The value equals the facts' javaHash and a flat String's hashCode of the same
     * spelling (the stock StringLatin1/StringUTF16 folds).
     */
    private int bulkJavaHash() {
        int length = length();
        int hash = 0;
        if (coder() == String.LATIN1) {
            byte[] window = new byte[Math.min(length, HASH_WINDOW)];
            for (int from = 0; from < length; from += window.length) {
                int count = Math.min(window.length, length - from);
                getBytes(window, from, 0, String.LATIN1, count);
                hash = ArraysSupport.vectorizedHashCode(window, 0, count, hash, ArraysSupport.T_BOOLEAN);
            }
            return hash;
        }
        char[] window = new char[Math.min(length, HASH_WINDOW)];
        for (int from = 0; from < length; from += window.length) {
            int count = Math.min(window.length, length - from);
            getChars(from, from + count, window, 0);
            hash = ArraysSupport.vectorizedHashCode(window, 0, count, hash, ArraysSupport.T_CHAR);
        }
        return hash;
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

    /**
     * Prepared facts of {@code [beginIndex, endIndex)} without allocating the range: the owner's
     * facts for the whole owner, a recorded range fact otherwise, {@code null} when unprepared or
     * empty. Never prepares anything.
     */
    M3StringFacts factsIfPrepared(int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, length());
        int count = endIndex - beginIndex;
        if (count == 0) return null;
        int begin = start() + beginIndex;
        if (begin == 0 && count == owner.length) return owner.factsIfPrepared();
        return owner.rangeFactsIfPrepared(span(begin, count));
    }

    private boolean isWholeOwner() {
        return start() == 0 && length() == owner.length;
    }

    boolean contentEquals(String other) {
        Objects.requireNonNull(other, "other");
        M3String that = other.m3();
        if (that != null) {
            if (sameCoordinate(that)) return true;
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
        return M3StringMixedCompare.unitsEqual(this, other);
    }

    /**
     * First index in {@code [from, from + count)} whose unit differs from the flat compact value
     * at {@code flatOffset}, {@code -1} when none; the owner compares in place where it can.
     */
    int mismatchUnits(int from, byte[] flat, int flatOffset, byte flatCoder, int count) {
        Objects.checkFromIndexSize(from, count, length());
        return owner.mismatchUnits(Math.addExact(start(), from), flat, flatOffset, flatCoder, count);
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
        if (checked.equals(StandardCharsets.UTF_8)) {
            return encodeUtf8();
        }
        if (checked.equals(StandardCharsets.ISO_8859_1)) {
            return encodeSingleByte(false);
        }
        if (checked.equals(StandardCharsets.US_ASCII)) {
            return encodeSingleByte(true);
        }
        return encodeWithEncoder(checked);
    }

    /**
     * Every unit of this String read once in bulk (A13): the encoders fold this array with their
     * unchanged byte rules instead of dispatching to the owner per unit. Transient scratch of the
     * same order as the output they allocate.
     */
    char[] units() {
        char[] out = new char[length()];
        getChars(0, out.length, out, 0);
        return out;
    }

    private byte[] encodeUtf8() {
        M3StringFacts prepared = facts();
        byte[] output = new byte[prepared.utf8Length];
        if (prepared.ascii) {
            getBytes(output, 0, 0, String.LATIN1, length());
            return output;
        }
        char[] units = units();
        int target = 0;
        for (int index = 0; index < units.length; index++) {
            char unit = units[index];
            if (unit < 0x80) {
                output[target++] = (byte) unit;
            } else if (unit < 0x800) {
                output[target++] = (byte) (0xc0 | (unit >>> 6));
                output[target++] = (byte) (0x80 | (unit & 0x3f));
            } else if (Character.isHighSurrogate(unit)
                    && index + 1 < units.length
                    && Character.isLowSurrogate(units[index + 1])) {
                int codePoint = Character.toCodePoint(unit, units[++index]);
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
        M3StringFacts prepared = facts();
        if ((asciiOnly && prepared.ascii) || (!asciiOnly && prepared.latin1)) {
            byte[] output = new byte[length()];
            getBytes(output, 0, 0, String.LATIN1, length());
            return output;
        }
        byte[] output = new byte[prepared.codePointCount];
        char[] units = units();
        int target = 0;
        for (int index = 0; index < units.length; index++) {
            char unit = units[index];
            int limit = asciiOnly ? 0x7f : 0xff;
            if (unit <= limit) {
                output[target++] = (byte) unit;
                continue;
            }
            if (Character.isHighSurrogate(unit)
                    && index + 1 < units.length
                    && Character.isLowSurrogate(units[index + 1])) {
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
        if (checked.equals(StandardCharsets.UTF_8)) {
            return encodeUtf8NoRepl();
        }
        if (checked.equals(StandardCharsets.ISO_8859_1)) {
            M3StringFacts prepared = facts();
            if (prepared.latin1) {
                byte[] output = new byte[length()];
                getBytes(output, 0, 0, String.LATIN1, length());
                return output;
            }
            for (int index = 0; index < length(); index++) {
                if (charAt(index) > 0xff) throw unmappable(index, 1);
            }
            throw new InternalError("M3String Latin-1 fact mismatch");
        }
        if (checked.equals(StandardCharsets.US_ASCII) && facts().ascii) {
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
        byte[] output = new byte[prepared.utf8Length];
        if (prepared.ascii) {
            getBytes(output, 0, 0, String.LATIN1, length());
            return output;
        }
        char[] units = units();
        int target = 0;
        for (int index = 0; index < units.length; index++) {
            char unit = units[index];
            if (unit < 0x80) {
                output[target++] = (byte) unit;
            } else if (unit < 0x800) {
                output[target++] = (byte) (0xc0 | (unit >>> 6));
                output[target++] = (byte) (0x80 | (unit & 0x3f));
            } else if (Character.isHighSurrogate(unit)
                    && index + 1 < units.length
                    && Character.isLowSurrogate(units[index + 1])) {
                int codePoint = Character.toCodePoint(unit, units[++index]);
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
        CharBuffer input = CharBuffer.wrap(units());
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
        CharsetEncoder encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
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

        ByteBuffer bytes = ByteBuffer.wrap(output);
        CharBuffer input = CharBuffer.wrap(units());
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
     * JNI-created Compact-String compatibility shadow. Never cached by M3String or its owners.
     */
    byte[] materialize() {
        return nativeByteShadow(this, 0, length(), coder());
    }

    byte[] compatibilityValue() {
        // VM layout sentinel only. Canonical text lives behind owner+coordinate.
        // Even the empty sentinel is created by the explicit JNI shadow boundary.
        byte[] shadow = emptyCompatibilityShadow;
        if (shadow != null) return shadow;
        synchronized (M3String.class) {
            shadow = emptyCompatibilityShadow;
            if (shadow == null) {
                shadow = nativeByteShadow(EMPTY, 0, 0, String.LATIN1);
                emptyCompatibilityShadow = shadow;
            }
            return shadow;
        }
    }

    /** JNI-created final Java UTF-16 array shadow. */
    char[] charShadow() {
        return nativeCharShadow(this, 0, length());
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
        return M3StringMixedCompare.mismatchStorages(this, offset, checked, 0, checked.length()) < 0;
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
            // The needle comes out once in its compact value and the haystack takes the flat
            // needle's window lane (A35): the stock vectorized search over bulk windows beats the
            // per-unit skip search over the owner at every size.
            byte[] units = checked.compactValue();
            byte unitsCoder = units.length == checked.length() ? String.LATIN1 : String.UTF16;
            if (coder() == String.LATIN1 && unitsCoder == String.UTF16) return -1;
            return indexOfUnits(units, unitsCoder, checked.length(), from, end);
        }

        int limit = end - checked.length();
        char first = checked.charAt(0);
        for (int start = from; start <= limit; start++) {
            if (charAt(start) != first) continue;
            int index = 1;
            while (index < checked.length()
                    && charAt(start + index) == checked.charAt(index)) index++;
            if (index == checked.length()) return start;
        }
        return -1;
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
            // The needle's compact value and the reverse window lane (A35), as for indexOf.
            byte[] units = checked.compactValue();
            byte unitsCoder = units.length == checked.length() ? String.LATIN1 : String.UTF16;
            if (coder() == String.LATIN1 && unitsCoder == String.UTF16) return -1;
            return lastIndexOfUnits(units, unitsCoder, checked.length(), maximumStart);
        }

        char first = checked.charAt(0);
        for (int candidate = maximumStart; candidate >= 0; candidate--) {
            if (charAt(candidate) != first) continue;
            int index = 1;
            while (index < checked.length()
                    && charAt(candidate + index) == checked.charAt(index)) index++;
            if (index == checked.length()) return candidate;
        }
        return -1;
    }

    /** The first bulk window of a scan (A33): an early exit pays a small read. */
    static final int FIRST_WINDOW_UNITS = 64;

    /** The second bulk window of a scan (A33). */
    static final int SECOND_WINDOW_UNITS = 256;

    /** The third bulk window of a scan (A33). */
    static final int THIRD_WINDOW_UNITS = 1024;

    /** The steady-state bulk window of a scan (A33). */
    static final int MAX_WINDOW_UNITS = 4096;

    /**
     * Units of the next bulk window of a scan that has consumed {@code done} units and has
     * {@code remaining} to go (A33): 64 first, then 256, 1024 and 4096, so a comparison that
     * differs early or a search that hits early reads little and a long scan keeps its wide
     * windows.
     */
    static int windowUnits(int done, int remaining) {
        int units = done == 0 ? FIRST_WINDOW_UNITS
                : done < SECOND_WINDOW_UNITS ? SECOND_WINDOW_UNITS
                : done < THIRD_WINDOW_UNITS ? THIRD_WINDOW_UNITS : MAX_WINDOW_UNITS;
        return Math.min(units, remaining);
    }

    /** Units per bulk window when a flat needle is searched (A27). */
    private static final int SEARCH_WINDOW = 4096;

    /**
     * Index of a flat needle (for example a literal) in {@code [fromIndex, endIndex)}, or -1
     * (A27): the haystack range comes out in bulk windows in its own coder (graded through
     * {@link #windowUnits}, never shorter than twice the needle) and the stock vectorized search
     * runs over each window; consecutive windows overlap by the needle's length minus one, so a
     * match straddling a window edge is seen by the next window.
     * Prepared facts prove absence first; a UTF-16 needle is never in a Latin-1 haystack. An
     * M3 needle takes its own lane (A35), which shares the window loop.
     */
    int indexOf(String needle, int fromIndex, int endIndex) {
        int end = Math.min(length(), endIndex);
        int from = Math.clamp(fromIndex, 0, length());
        int needleLength = needle.length();
        if (needleLength == 0) return Math.min(from, end);
        if (needleLength == 1) return indexOf(needle.charAt(0), from, end);
        if (from > end - needleLength) return -1;
        M3String target = needle.m3();
        if (target != null) return indexOf(target, from, end);
        M3StringFacts prepared = factsIfPrepared();
        if (prepared != null && !prepared.mayContain(needle)) return -1;
        byte coder = coder();
        if (coder == String.LATIN1 && !needle.isLatin1()) return -1;
        if (searchWindow(end - from, needleLength, coder) < 0) return indexOfPerUnit(needle, from, end);
        return indexOfUnits(needle.value(), needle.coder(), needleLength, from, end);
    }

    /**
     * Index in {@code [from, end)} of a needle given as its compact value (a flat String's value,
     * or an M3 needle's {@link #compactValue}) with the coder that value has, or -1: the window
     * loop of the flat needle's lane (A27, graded by A33), shared with the M3 needle's lane (A35).
     */
    private int indexOfUnits(byte[] units, byte unitsCoder, int needleLength, int from, int end) {
        byte coder = coder();
        int floor = (int) Math.min(end - from, 2L * needleLength);
        byte[] window = null;
        for (int start = from, done = 0; ; ) {
            int remaining = end - start;
            int count = Math.min(remaining, Math.max(windowUnits(done, remaining), floor));
            if (window == null || window.length < count << coder) window = new byte[count << coder];
            getBytes(window, start, 0, coder, count);
            int found = windowIndexOf(window, coder, count, units, unitsCoder, needleLength);
            if (found >= 0) return start + found;
            if (count == remaining) return -1;
            int step = count - needleLength + 1;
            start += step;
            done += step;
        }
    }

    /**
     * Last index of a flat needle starting at or before {@code maximumStart}, or -1 (A27): bulk
     * windows walked from the end, each overlapping the previous by the needle's length minus
     * one, searched by the stock reverse search. An M3 needle takes its own lane (A35), which
     * shares the window loop.
     */
    int lastIndexOf(String needle, int maximumStart) {
        int needleLength = needle.length();
        int start = Math.min(maximumStart, length() - needleLength);
        if (start < 0) return -1;
        if (needleLength == 0) return start;
        if (needleLength == 1) return lastIndexOf(needle.charAt(0), start);
        M3String target = needle.m3();
        if (target != null) return lastIndexOf(target, start);
        M3StringFacts prepared = factsIfPrepared();
        if (prepared != null && !prepared.mayContain(needle)) return -1;
        byte coder = coder();
        if (coder == String.LATIN1 && !needle.isLatin1()) return -1;
        int end = start + needleLength;
        if (searchWindow(end, needleLength, coder) < 0) return lastIndexOfPerUnit(needle, start);
        return lastIndexOfUnits(needle.value(), needle.coder(), needleLength, start);
    }

    /**
     * Last index at or before {@code maximumStart} of a needle given as its compact value with
     * the coder that value has, or -1: the reverse window loop of the flat needle's lane (A27,
     * graded by A33), shared with the M3 needle's lane (A35).
     */
    private int lastIndexOfUnits(byte[] units, byte unitsCoder, int needleLength, int maximumStart) {
        byte coder = coder();
        int end = maximumStart + needleLength;
        int floor = (int) Math.min(end, 2L * needleLength);
        byte[] window = null;
        for (int stop = end, done = 0; ; ) {
            int count = Math.min(stop, Math.max(windowUnits(done, stop), floor));
            int begin = stop - count;
            if (window == null || window.length < count << coder) window = new byte[count << coder];
            getBytes(window, begin, 0, coder, count);
            int found = windowLastIndexOf(window, coder, count, units, unitsCoder, needleLength);
            if (found >= 0) return begin + found;
            if (begin == 0) return -1;
            int step = count - needleLength + 1;
            stop -= step;
            done += step;
        }
    }

    /**
     * Units per window for a bulk search over {@code span} units: the window, at least twice the
     * needle, is bounded by {@link #SEARCH_WINDOW}; -1 when a window could not fit a byte array.
     */
    private static int searchWindow(int span, int needleLength, byte coder) {
        long units = Math.min(span, Math.max(SEARCH_WINDOW, 2L * needleLength));
        return (units << coder) > Integer.MAX_VALUE - 8 ? -1 : (int) units;
    }

    /** The stock forward search of a needle's compact value over one window, as String dispatches it. */
    private static int windowIndexOf(byte[] window, byte coder, int count, byte[] units,
            byte unitsCoder, int needleLength) {
        if (coder == unitsCoder) {
            return coder == String.LATIN1
                    ? StringLatin1.indexOf(window, count, units, needleLength, 0)
                    : StringUTF16.indexOf(window, count, units, needleLength, 0);
        }
        if (coder == String.LATIN1) return -1;
        return StringUTF16.indexOfLatin1(window, count, units, needleLength, 0);
    }

    /** The stock reverse search of a needle's compact value over one window, as String dispatches it. */
    private static int windowLastIndexOf(byte[] window, byte coder, int count, byte[] units,
            byte unitsCoder, int needleLength) {
        int from = count - needleLength;
        if (coder == unitsCoder) {
            return coder == String.LATIN1
                    ? StringLatin1.lastIndexOf(window, count, units, needleLength, from)
                    : StringUTF16.lastIndexOf(window, count, units, needleLength, from);
        }
        if (coder == String.LATIN1) return -1;
        return StringUTF16.lastIndexOfLatin1(window, count, units, needleLength, from);
    }

    private int indexOfPerUnit(String needle, int from, int end) {
        int needleLength = needle.length();
        char first = needle.charAt(0);
        for (int start = from; start <= end - needleLength; start++) {
            if (charAt(start) != first) continue;
            int index = 1;
            while (index < needleLength && charAt(start + index) == needle.charAt(index)) index++;
            if (index == needleLength) return start;
        }
        return -1;
    }

    private int lastIndexOfPerUnit(String needle, int maximumStart) {
        int needleLength = needle.length();
        for (int candidate = maximumStart; candidate >= 0; candidate--) {
            int index = 0;
            while (index < needleLength && charAt(candidate + index) == needle.charAt(index)) index++;
            if (index == needleLength) return candidate;
        }
        return -1;
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

    M3String asciiCase(boolean upper) {
        if (!contentIsAscii()) {
            throw new IllegalStateException("ASCII case mapping requires ASCII M3 String");
        }

        ArrayList<M3String> pieces = null;
        int cursor = 0;
        // The units are read once in bulk (A24): the mapping scan reads an array.
        char[] units = units();
        for (int index = 0; index < units.length; index++) {
            char unit = units[index];
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

    /**
     * Conservative BMP literal subset of regex syntax: non-empty, no metacharacter, no escape
     * and no surrogate, so {@code Pattern.compile(regex)} can only ever match the literal itself.
     */
    static boolean isLiteralRegex(String regex) {
        if (regex == null || regex.isEmpty()) return false;
        for (int index = 0; index < regex.length(); index++) {
            char unit = regex.charAt(index);
            // Regex observes code points: UTF-16 substring search can match half a pair.
            if (Character.isSurrogate(unit) || "\\.^$|?*+()[]{}".indexOf(unit) >= 0) return false;
        }
        return true;
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

    private static native byte[] nativeByteShadow(
            M3String value, int start, int length, byte coder);

    private static native char[] nativeCharShadow(
            M3String value, int start, int length);
}
