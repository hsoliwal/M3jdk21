/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Read-only M3 receiver of Synexia's format-3 dense word-row image.
 *
 * <p>Stable local word IDs select existing 128-byte records by arithmetic;
 * the first 64 bytes contain the hot scalar facts. This reader owns no spelling,
 * copied word rows, per-word object, or second canonical lexicon. A publisher
 * must preserve the immutability and lifetime of the mapped source file.
 *
 * <p>Format 1/2 images remain with the existing Synexia readers until their
 * historical layouts and compatibility contracts are independently qualified.
 */
public final class M3WordRows {
    public static final long MAGIC = 0x4d4957524f575331L;
    public static final int FORMAT = 3;
    public static final int HEADER_BYTES = 104;
    public static final int ROW_BYTES = 128;
    public static final int HOT_CORE_BYTES = 64;
    public static final int MAX_LANGUAGE_ID = 0xffff;
    public static final long OVERLAY_BIT = 1L << 47;
    private static final long LOCAL_MASK = OVERLAY_BIT - 1;

    private final ByteBuffer data;
    private final int languageId;
    private final int wordCount;
    private final long lexiconFingerprint;
    private final String languageTag;
    private final String dictionaryVersion;
    private final long corpusTokens;
    private final long documents;
    private final int rowsBase;
    private final int membershipsBase;
    private final int conceptsBase;
    private final int subjectsBase;
    private final int expansionsBase;

    private M3WordRows(ByteBuffer data, int languageId, int wordCount,
                       long lexiconFingerprint, String languageTag, String dictionaryVersion,
                       long corpusTokens, long documents, int rowsBase, int membershipsBase,
                       int conceptsBase, int subjectsBase, int expansionsBase) {
        this.data = data.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
        this.languageId = languageId;
        this.wordCount = wordCount;
        this.lexiconFingerprint = lexiconFingerprint;
        this.languageTag = languageTag;
        this.dictionaryVersion = dictionaryVersion;
        this.corpusTokens = corpusTokens;
        this.documents = documents;
        this.rowsBase = rowsBase;
        this.membershipsBase = membershipsBase;
        this.conceptsBase = conceptsBase;
        this.subjectsBase = subjectsBase;
        this.expansionsBase = expansionsBase;
    }

    /** Opens one immutable published image; the mapping keeps its backing alive. */
    public static M3WordRows open(Path source) throws IOException {
        Objects.requireNonNull(source, "source");
        try (FileChannel channel = FileChannel.open(source, StandardOpenOption.READ)) {
            long size = channel.size();
            if (size < HEADER_BYTES || size > Integer.MAX_VALUE) {
                throw new IOException("invalid M3 word-row image length");
            }
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
            return decode(map);
        } catch (ArithmeticException | IndexOutOfBoundsException invalid) {
            throw new IOException("overflow or bounds in M3 word-row image", invalid);
        }
    }

    private static M3WordRows decode(ByteBuffer buffer) throws IOException {
        ByteBuffer image = buffer.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
        if (image.getLong(0) != MAGIC || image.getInt(8) != FORMAT
                || image.getInt(20) != ROW_BYTES) {
            throw new IOException("unsupported M3 word-row magic, format, or stride");
        }
        int language = image.getInt(12);
        int words = image.getInt(16);
        int tagLength = image.getInt(56);
        int versionLength = image.getInt(60);
        int members = image.getInt(72);
        int concepts = image.getInt(80);
        int subjects = image.getInt(88);
        int expansions = image.getInt(96);
        long tokens = image.getLong(40);
        long documents = image.getLong(48);
        if (language < 0 || language > MAX_LANGUAGE_ID || words < 0
                || tagLength < 0 || versionLength < 0 || members < 0 || concepts < 0
                || subjects < 0 || expansions < 0 || tokens < 0 || documents < 0) {
            throw new IOException("negative or unqualified M3 word-row header field");
        }
        try {
            int rows = align(Math.addExact(HEADER_BYTES,
                    Math.addExact(tagLength, versionLength)));
            int membershipBase = align(Math.addExact(rows, Math.multiplyExact(words, ROW_BYTES)));
            int conceptBase = align(Math.addExact(membershipBase,
                    Math.multiplyExact(members, Integer.BYTES)));
            int subjectBase = align(Math.addExact(conceptBase,
                    Math.multiplyExact(concepts, Long.BYTES)));
            int expansionBase = align(Math.addExact(subjectBase,
                    Math.multiplyExact(subjects, Long.BYTES)));
            int end = Math.addExact(expansionBase, Math.multiplyExact(expansions, Long.BYTES));
            if (image.getInt(64) != rows || image.getInt(68) != membershipBase
                    || image.getInt(76) != conceptBase || image.getInt(84) != subjectBase
                    || image.getInt(92) != expansionBase || image.getInt(100) != end
                    || end != image.capacity()) {
                throw new IOException("corrupt M3 word-row section geometry");
            }
            String tag = metadata(image, HEADER_BYTES, tagLength);
            String version = metadata(image, HEADER_BYTES + tagLength, versionLength);
            M3WordRows result = new M3WordRows(image, language, words, image.getLong(24),
                    tag, version, tokens, documents, rows, membershipBase,
                    conceptBase, subjectBase, expansionBase);
            result.validateRows(conceptBase, subjectBase, expansionBase, end);
            return result;
        } catch (ArithmeticException | IndexOutOfBoundsException invalid) {
            throw new IOException("invalid M3 word-row arithmetic", invalid);
        }
    }

    private static int align(int value) {
        return Math.addExact(value, (-value) & (HOT_CORE_BYTES - 1));
    }

    private static String metadata(ByteBuffer image, int offset, int length) throws IOException {
        ByteBuffer view = image.asReadOnlyBuffer();
        view.position(offset).limit(Math.addExact(offset, length));
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(view).toString();
        } catch (CharacterCodingException invalid) {
            throw new IOException("invalid M3 word-row UTF-8 metadata", invalid);
        }
    }

    private void validateRows(int concepts, int subjects, int expansions, int end)
            throws IOException {
        for (int local = 0; local < wordCount; local++) {
            int row = rowsBase + (local << 7);
            if (data.getInt(row + 4) < 0 || data.getInt(row + 8) < 0
                    || data.getInt(row + 12) < 0 || data.getLong(row + 32) < 0
                    || data.getInt(row + 40) < 0) {
                throw new IOException("negative M3 word-row scalar at local ID " + local);
            }
            if (data.getInt(row + 12) > data.getInt(row + 4)) {
                throw new IOException("invalid UTF-16/code-point relationship");
            }
            int lexRank = data.getInt(row + 44);
            int reverseRank = data.getInt(row + 48);
            int freqRank = data.getInt(row + 52);
            int prefixEnd = data.getInt(row + 56);
            int suffixEnd = data.getInt(row + 60);
            if (lexRank < 0 || lexRank >= wordCount
                    || reverseRank < 0 || reverseRank >= wordCount
                    || freqRank < 0 || freqRank >= wordCount
                    || prefixEnd <= lexRank || prefixEnd > wordCount
                    || suffixEnd <= reverseRank || suffixEnd > wordCount) {
                throw new IOException("invalid rank/range geometry at local ID " + local);
            }
            validateList(row + 72, row + 76, membershipsBase, concepts, Integer.BYTES);
            validateList(row + 80, row + 84, concepts, subjects, Long.BYTES);
            validateList(row + 88, row + 92, subjects, expansions, Long.BYTES);
            validateList(row + 96, row + 100, expansions, end, Long.BYTES);
        }
    }

    private void validateList(int offsetField, int countField, int sectionBegin,
                              int sectionEnd, int elementWidth) throws IOException {
        int address = data.getInt(offsetField);
        int count = data.getInt(countField);
        if (count < 0 || address < sectionBegin || address > sectionEnd
                || (address - sectionBegin) % elementWidth != 0
                || (long) address + (long) count * elementWidth > sectionEnd) {
            throw new IOException("invalid M3 word-row relation offset/count");
        }
    }

    /** Same generation, not merely same spelling or equal local ID. */
    public void requireGeneration(int language, int words, long fingerprint,
                                  String tag, String version) {
        if (language != languageId || words != wordCount || fingerprint != lexiconFingerprint
                || !languageTag.equals(Objects.requireNonNull(tag, "tag"))
                || !dictionaryVersion.equals(Objects.requireNonNull(version, "version"))) {
            throw new IllegalArgumentException("M3 word rows belong to another lexicon generation");
        }
    }

    public int languageId() { return languageId; }
    public int wordCount() { return wordCount; }
    public long lexiconFingerprint() { return lexiconFingerprint; }
    public String languageTag() { return languageTag; }
    public String dictionaryVersion() { return dictionaryVersion; }
    public long totalCorpusTokens() { return corpusTokens; }
    public long totalDocuments() { return documents; }
    public long mappedImageBytes() { return data.capacity(); }
    public int hotCoreBytes() { return HOT_CORE_BYTES; }

    /** Project the cross-language packed IndexId only at the API boundary. */
    public long wordId(int localId) {
        return ((long) languageId << 48) | Integer.toUnsignedLong(checked(localId));
    }

    /** Refuse foreign language, overlay and out-of-range identifiers. */
    public int localId(long wordId) {
        if ((int) ((wordId >>> 48) & 0xffff) != languageId
                || (wordId & OVERLAY_BIT) != 0 || (wordId & LOCAL_MASK) >= wordCount) {
            throw new IllegalArgumentException("word ID is outside M3 lexicon generation");
        }
        return (int) (wordId & LOCAL_MASK);
    }

    /** Checked byte address; no per-word row object. */
    public int rowByteOffset(int localId) {
        return rowsBase + (checked(localId) << 7);
    }

    private int checked(int local) { return Objects.checkIndex(local, wordCount); }
    public int flags(int local) { return data.getInt(rowByteOffset(local)); }
    public int utf16Length(int local) { return data.getInt(rowByteOffset(local) + 4); }
    public int utf8Length(int local) { return data.getInt(rowByteOffset(local) + 8); }
    public int codePointCount(int local) { return data.getInt(rowByteOffset(local) + 12); }
    public int javaHash(int local) { return data.getInt(rowByteOffset(local) + 16); }
    public int characterFlags(int local) { return data.getInt(rowByteOffset(local) + 20); }
    public long bitSignal(int local) { return data.getLong(rowByteOffset(local) + 24); }
    public long corpusCount(int local) { return data.getLong(rowByteOffset(local) + 32); }
    public int documentFrequency(int local) { return data.getInt(rowByteOffset(local) + 40); }
    public int lexicalRank(int local) { return data.getInt(rowByteOffset(local) + 44); }
    public int reverseLexicalRank(int local) { return data.getInt(rowByteOffset(local) + 48); }
    public int frequencyRank(int local) { return data.getInt(rowByteOffset(local) + 52); }
    public int prefixEndExclusive(int local) { return data.getInt(rowByteOffset(local) + 56); }
    public int suffixEndExclusive(int local) { return data.getInt(rowByteOffset(local) + 60); }
    public int hash31Power(int local) { return data.getInt(rowByteOffset(local) + 116); }

    public boolean isPrefixOf(int prefix, int candidate) {
        int row = rowByteOffset(prefix);
        int rank = lexicalRank(candidate);
        return rank >= data.getInt(row + 44) && rank < data.getInt(row + 56);
    }

    public boolean isSuffixOf(int suffix, int candidate) {
        int row = rowByteOffset(suffix);
        int rank = reverseLexicalRank(candidate);
        return rank >= data.getInt(row + 48) && rank < data.getInt(row + 60);
    }

    public int membershipCount(int local) { return data.getInt(rowByteOffset(local) + 76); }
    public int conceptCount(int local) { return data.getInt(rowByteOffset(local) + 84); }
    public int subjectCount(int local) { return data.getInt(rowByteOffset(local) + 92); }
    public int expansionCount(int local) { return data.getInt(rowByteOffset(local) + 100); }
    public int membershipAt(int local, int index) {
        int row = rowByteOffset(local);
        int count = data.getInt(row + 76);
        return data.getInt(data.getInt(row + 72) + (Objects.checkIndex(index, count) << 2));
    }
    public long conceptAt(int local, int index) {
        int row = rowByteOffset(local);
        int count = data.getInt(row + 84);
        return data.getLong(data.getInt(row + 80) + (Objects.checkIndex(index, count) << 3));
    }
    public long subjectAt(int local, int index) {
        int row = rowByteOffset(local);
        int count = data.getInt(row + 92);
        return data.getLong(data.getInt(row + 88) + (Objects.checkIndex(index, count) << 3));
    }
    public long expansionWordIdAt(int local, int index) {
        int row = rowByteOffset(local);
        int count = data.getInt(row + 100);
        return data.getLong(data.getInt(row + 96) + (Objects.checkIndex(index, count) << 3));
    }

    /** Return a caller-owned worker cursor; logical source storage stays with the map. */
    public Cursor cursor() { return new Cursor(this); }

    /** Package-private native read view; memory remains strongly owned by this instance. */
    ByteBuffer nativeImage() { return data; }
    int nativeRowsBase() { return rowsBase; }

    /** Reusable scalar scratch; not thread-safe and never retained by the image. */
    public static final class Cursor {
        private final M3WordRows owner;
        private int local = -1;
        private int row;
        private int flags;
        private int lexicalRank;
        private int reverseRank;
        private int prefixEnd;
        private int suffixEnd;
        private long count;

        private Cursor(M3WordRows owner) { this.owner = owner; }
        public Cursor reset(int localId) {
            row = owner.rowByteOffset(localId);
            local = localId;
            flags = owner.data.getInt(row);
            count = owner.data.getLong(row + 32);
            lexicalRank = owner.data.getInt(row + 44);
            reverseRank = owner.data.getInt(row + 48);
            prefixEnd = owner.data.getInt(row + 56);
            suffixEnd = owner.data.getInt(row + 60);
            return this;
        }
        public int localId() { requireBound(); return local; }
        public int rowByteOffset() { requireBound(); return row; }
        public int flags() { requireBound(); return flags; }
        public long corpusCount() { requireBound(); return count; }
        public boolean sameWord(Cursor other) {
            requireSame(other);
            return local == other.local;
        }
        public boolean isPrefixOf(Cursor other) {
            requireSame(other);
            return other.lexicalRank >= lexicalRank && other.lexicalRank < prefixEnd;
        }
        public boolean isSuffixOf(Cursor other) {
            requireSame(other);
            return other.reverseRank >= reverseRank && other.reverseRank < suffixEnd;
        }
        private void requireBound() {
            if (local < 0) throw new IllegalStateException("M3 word cursor not bound");
        }
        private void requireSame(Cursor other) {
            Objects.requireNonNull(other, "other");
            requireBound();
            other.requireBound();
            if (owner != other.owner) throw new IllegalArgumentException("foreign word generation");
        }
    }
}
