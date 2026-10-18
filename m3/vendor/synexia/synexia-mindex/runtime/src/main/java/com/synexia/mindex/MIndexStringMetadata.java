// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Objects;

/**
 * Immutable side-table entry for one canonical {@link MIndexString}.
 *
 * <p>The spelling remains owned by the MIndex string pool. This object freezes the derived facts
 * that are repeatedly needed by lexers, converters, prefix/suffix matchers, and AST adapters:
 * lengths, hashes, Unicode/lexical flags, and successful primitive conversions. Failed conversions
 * are represented by absent bits rather than exceptions on every hot-path read.</p>
 */
public final class MIndexStringMetadata {
    public static final int FLAG_EMPTY = 1;
    public static final int FLAG_ASCII = 1 << 1;
    public static final int FLAG_UNICODE = 1 << 2;
    public static final int FLAG_LETTER = 1 << 3;
    public static final int FLAG_DIGIT = 1 << 4;
    public static final int FLAG_WHITESPACE = 1 << 5;
    public static final int FLAG_IDENTIFIER = 1 << 6;
    public static final int FLAG_ACRONYM = 1 << 7;
    public static final int FLAG_HEX = 1 << 8;
    public static final int FLAG_SUPPLEMENTARY = 1 << 9;
    public static final int FLAG_PUNCTUATION = 1 << 10;

    public static final int NUMERIC_INT32 = 1;
    public static final int NUMERIC_INT64 = 1 << 1;
    public static final int NUMERIC_FLOAT32 = 1 << 2;
    public static final int NUMERIC_FLOAT64 = 1 << 3;

    // A composite segment retains these lanes without creating a metadata object per segment.
    public static final int FLAG_LEXICAL = 1;
    public static final int FLAG_SOURCE_SPAN = 1 << 1;
    public static final int FLAG_STRUCTURAL = 1 << 2;
    public static final int FLAG_NOVEL = 1 << 3;
    public static final int FLAG_ESCAPED = 1 << 4;
    public static final int FLAG_SOURCE_BYTES = 1 << 5;
    public static final int FLAG_KNOWN_ATOM = 1 << 6;

    public static final MIndexStringMetadata NONE =
            new MIndexStringMetadata(0, 0L, 0L, 0L, -1, -1);

    private final MIndexString value;
    private final int contentIndex;
    private final int utf16Length;
    private final int codePointLength;
    private final int utf8Length;
    private final int javaHashCode;
    private final long contentHash64;
    private final long rollingHash64;
    private final int flags;
    private final int numericMask;
    private final int int32Value;
    private final long int64Value;
    private final int float32RawBits;
    private final long float64RawBits;
    private final long lexicalBits;
    private final long semanticBits;
    private final long relationBits;
    private final int sourceStart;
    private final int sourceEnd;

    private MIndexStringMetadata(
            MIndexString value,
            String spelling,
            int flags,
            int numericMask,
            int int32Value,
            long int64Value,
            int float32RawBits,
            long float64RawBits) {
        this.value = value;
        contentIndex = value.contentIndex();
        utf16Length = value.length();
        codePointLength = spelling.codePointCount(0, spelling.length());
        utf8Length = value.getBytes().length;
        javaHashCode = value.hashCode();
        contentHash64 = value.contentHash64();
        rollingHash64 = rollingHash(spelling);
        this.flags = flags;
        this.numericMask = numericMask;
        this.int32Value = int32Value;
        this.int64Value = int64Value;
        this.float32RawBits = float32RawBits;
        this.float64RawBits = float64RawBits;
        lexicalBits = 0L;
        semanticBits = 0L;
        relationBits = 0L;
        sourceStart = -1;
        sourceEnd = -1;
    }

    /** Segment metadata; the canonical spelling remains in the owning composite. */
    public MIndexStringMetadata(
            int flags,
            long lexicalBits,
            long semanticBits,
            long relationBits,
            int sourceStart,
            int sourceEnd) {
        boolean hasSource = (flags & FLAG_SOURCE_SPAN) != 0;
        if (hasSource) {
            if (sourceStart < 0 || sourceEnd < sourceStart) {
                throw new IllegalArgumentException("invalid source span");
            }
        } else if (sourceStart != -1 || sourceEnd != -1) {
            throw new IllegalArgumentException("source span requires FLAG_SOURCE_SPAN");
        }
        value = null;
        contentIndex = -1;
        utf16Length = 0;
        codePointLength = 0;
        utf8Length = 0;
        javaHashCode = 0;
        contentHash64 = 0L;
        rollingHash64 = 0L;
        this.flags = flags;
        numericMask = 0;
        int32Value = 0;
        int64Value = 0L;
        float32RawBits = 0;
        float64RawBits = 0L;
        this.lexicalBits = lexicalBits;
        this.semanticBits = semanticBits;
        this.relationBits = relationBits;
        this.sourceStart = sourceStart;
        this.sourceEnd = sourceEnd;
    }

    public static MIndexStringMetadata analyze(MIndexString value) {
        Objects.requireNonNull(value, "value");
        String spelling = value.toString();
        int flags = lexicalFlags(spelling);
        int numericMask = 0;
        int int32 = 0;
        long int64 = 0L;
        int float32 = 0;
        long float64 = 0L;

        try {
            int32 = Integer.parseInt(spelling);
            numericMask |= NUMERIC_INT32;
        } catch (NumberFormatException ignored) {
            // The absent lane is the precomputed result for this spelling.
        }
        try {
            int64 = Long.parseLong(spelling);
            numericMask |= NUMERIC_INT64;
        } catch (NumberFormatException ignored) {
            // The absent lane is the precomputed result for this spelling.
        }
        try {
            float32 = Float.floatToRawIntBits(Float.parseFloat(spelling));
            numericMask |= NUMERIC_FLOAT32;
        } catch (NumberFormatException ignored) {
            // The absent lane is the precomputed result for this spelling.
        }
        try {
            float64 = Double.doubleToRawLongBits(Double.parseDouble(spelling));
            numericMask |= NUMERIC_FLOAT64;
        } catch (NumberFormatException ignored) {
            // The absent lane is the precomputed result for this spelling.
        }
        return new MIndexStringMetadata(value, spelling, flags, numericMask, int32, int64, float32, float64);
    }

    public static MIndexStringMetadata analyze(CharSequence value) {
        Objects.requireNonNull(value, "value");
        return analyze(MIndexString.from(value));
    }

    public MIndexString value() { return value; }
    public int contentIndex() { return contentIndex; }
    public int utf16Length() { return utf16Length; }
    public int codePointLength() { return codePointLength; }
    public int utf8Length() { return utf8Length; }
    public int javaHashCode() { return javaHashCode; }
    public long contentHash64() { return contentHash64; }
    public long rollingHash64() { return rollingHash64; }
    public int flags() { return flags; }
    public boolean hasFlag(int flag) { return flag != 0 && (flags & flag) == flag; }
    public boolean isEmpty() { return value != null && hasFlag(FLAG_EMPTY); }
    public boolean isAscii() { return value != null && hasFlag(FLAG_ASCII); }
    public boolean isUnicode() { return value != null && hasFlag(FLAG_UNICODE); }
    public boolean isIdentifier() { return value != null && hasFlag(FLAG_IDENTIFIER); }
    public boolean isAcronym() { return value != null && hasFlag(FLAG_ACRONYM); }
    public int numericMask() { return numericMask; }
    public boolean canConvertToInt32() { return (numericMask & NUMERIC_INT32) != 0; }
    public boolean canConvertToInt64() { return (numericMask & NUMERIC_INT64) != 0; }
    public boolean canConvertToFloat32() { return (numericMask & NUMERIC_FLOAT32) != 0; }
    public boolean canConvertToFloat64() { return (numericMask & NUMERIC_FLOAT64) != 0; }

    public int int32Value() {
        requireNumeric(NUMERIC_INT32, "int32");
        return int32Value;
    }

    public long int64Value() {
        requireNumeric(NUMERIC_INT64, "int64");
        return int64Value;
    }

    public int float32RawBits() {
        requireNumeric(NUMERIC_FLOAT32, "float32");
        return float32RawBits;
    }

    public float float32Value() { return Float.intBitsToFloat(float32RawBits()); }

    public long float64RawBits() {
        requireNumeric(NUMERIC_FLOAT64, "float64");
        return float64RawBits;
    }

    public double float64Value() { return Double.longBitsToDouble(float64RawBits()); }

    private void requireNumeric(int lane, String name) {
        if ((numericMask & lane) == 0) {
            throw new IllegalStateException("spelling is not convertible to " + name);
        }
    }

    private static int lexicalFlags(String spelling) {
        if (spelling.isEmpty()) return FLAG_EMPTY;
        int flags = 0;
        boolean ascii = true;
        boolean allIdentifier = true;
        boolean first = true;
        boolean allHex = true;
        boolean allAcronymChars = true;
        int letters = 0;
        int uppercaseLetters = 0;
        boolean hasLowercase = false;

        for (int offset = 0; offset < spelling.length();) {
            int codePoint = spelling.codePointAt(offset);
            int charCount = Character.charCount(codePoint);
            offset += charCount;
            if (codePoint > 0x7f) {
                ascii = false;
                flags |= FLAG_UNICODE;
            }
            if (codePoint > 0xffff) flags |= FLAG_SUPPLEMENTARY;
            if (Character.isLetter(codePoint)) {
                flags |= FLAG_LETTER;
                letters++;
                if (Character.isUpperCase(codePoint)) uppercaseLetters++;
                if (Character.isLowerCase(codePoint)) hasLowercase = true;
            }
            if (Character.isDigit(codePoint)) flags |= FLAG_DIGIT;
            if (Character.isWhitespace(codePoint)) flags |= FLAG_WHITESPACE;
            if (Character.getType(codePoint) == Character.CONNECTOR_PUNCTUATION
                    || Character.getType(codePoint) == Character.DASH_PUNCTUATION
                    || Character.getType(codePoint) == Character.START_PUNCTUATION
                    || Character.getType(codePoint) == Character.END_PUNCTUATION
                    || Character.getType(codePoint) == Character.OTHER_PUNCTUATION
                    || Character.getType(codePoint) == Character.INITIAL_QUOTE_PUNCTUATION
                    || Character.getType(codePoint) == Character.FINAL_QUOTE_PUNCTUATION) {
                flags |= FLAG_PUNCTUATION;
            }
            if (first ? !Character.isJavaIdentifierStart(codePoint)
                    : !Character.isJavaIdentifierPart(codePoint)) allIdentifier = false;
            first = false;
            if (!isHex(codePoint)) allHex = false;
            if (!(Character.isLetterOrDigit(codePoint) || codePoint == '_' || codePoint == '-')) {
                allAcronymChars = false;
            }
        }
        if (ascii) flags |= FLAG_ASCII;
        if (allIdentifier) flags |= FLAG_IDENTIFIER;
        if (allHex) flags |= FLAG_HEX;
        if (letters >= 2 && uppercaseLetters == letters && !hasLowercase && allAcronymChars) {
            flags |= FLAG_ACRONYM;
        }
        return flags;
    }

    private static boolean isHex(int codePoint) {
        return codePoint >= '0' && codePoint <= '9'
                || codePoint >= 'a' && codePoint <= 'f'
                || codePoint >= 'A' && codePoint <= 'F';
    }

    private static long rollingHash(String spelling) {
        long hash = 0xcbf29ce484222325L;
        for (int index = 0; index < spelling.length(); index++) {
            hash ^= spelling.charAt(index);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    public long lexicalBits() { return lexicalBits; }
    public long semanticBits() { return semanticBits; }
    public long relationBits() { return relationBits; }
    public int sourceStart() { return sourceStart; }
    public int sourceEnd() { return sourceEnd; }

    public boolean hasLexicalRef() {
        return hasFlag(FLAG_LEXICAL);
    }

    public LexicalRef lexicalRef() {
        if (!hasLexicalRef()) throw new IllegalStateException("metadata has no lexical reference");
        return new LexicalRef(lexicalBits);
    }

    public SemanticRef semanticRef() {
        return new SemanticRef(semanticBits);
    }

    public static MIndexStringMetadata lexical(
            LexicalRef lexical,
            SemanticRef semantic,
            long relationBits,
            int sourceStart,
            int sourceEnd,
            int additionalFlags) {
        if (lexical == null) throw new NullPointerException("lexical");
        SemanticRef actualSemantic = semantic == null ? SemanticRef.NONE : semantic;
        int flags = additionalFlags | FLAG_LEXICAL;
        if (sourceStart >= 0) flags |= FLAG_SOURCE_SPAN;
        return new MIndexStringMetadata(
                flags,
                lexical.bits(),
                actualSemantic.bits(),
                relationBits,
                sourceStart,
                sourceStart >= 0 ? sourceEnd : -1);
    }
}
