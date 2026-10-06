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

    static M3String admit(byte[] compactValue, byte coder) {
        Objects.requireNonNull(compactValue, "compactValue");
        ADMITTING.set(Boolean.TRUE);
        try {
            return M3StringPool.internScalar(compactValue, coder);
        } finally {
            ADMITTING.remove();
        }
    }

    static M3String join(String first, String second) {
        return join(new String[] {first, second});
    }

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

    int hashCodeValue() {
        return isWholeOwner() ? owner.javaHash : facts().javaHash;
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
        for (int index = 0; index < length(); index++) {
            if (charAt(index) != other.charAt(index)) return false;
        }
        return true;
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

    private byte[] encodeUtf8() {
        M3StringFacts prepared = facts();
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
            return M3StringSearchPrecompute.lastIndexOf(this, checked, plan, maximumStart);
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

    M3String replace(M3String target, M3String replacement) {
        M3String checkedTarget = Objects.requireNonNull(target, "target");
        M3String checkedReplacement = Objects.requireNonNull(replacement, "replacement");
        if (checkedTarget.length() == 0) {
            throw new IllegalArgumentException("empty literal target handled by String compatibility path");
        }

        int found = indexOf(checkedTarget, 0);
        if (found < 0) return this;

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
            cursor = found + checkedTarget.length();
            found = cursor <= length() - checkedTarget.length()
                    ? indexOf(checkedTarget, cursor)
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
