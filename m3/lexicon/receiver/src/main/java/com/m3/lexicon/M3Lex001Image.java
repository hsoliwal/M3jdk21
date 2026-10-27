// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Java writer and verifier for the M3LEX001 image read by {@code java.lang.M3StringPool.Lexicon.open}
 * ({@code -Djdk.mindex.lexicon=<file>}).
 *
 * <p>Versions 1 and 2 retain the historical directory + UTF-16LE payload layouts. Version 3 keeps
 * the same 12-byte word directory and appends the authenticated direct-address geometry used by
 * M3StringPool. The lookup trailer is therefore OS-shareable immutable data rather than per-VM
 * reconstruction.</p>
 */
public final class M3Lex001Image {
    /** Header bytes. */
    public static final int HEADER = 64;
    /** Version-3 direct-index trailer header bytes. */
    public static final int DIRECT_INDEX_HEADER = M3Lex001DirectIndex.HEADER;
    /** Version-3 direct-index trailer magic. */
    public static final long DIRECT_INDEX_MAGIC = M3Lex001DirectIndex.MAGIC;
    /** Upper size bound shared with M3StringPool. */
    public static final long MAX_BYTES = 1L << 30;

    /** Verified header facts. */
    public record Facts(int version, int count, long units, long generation64, byte[] sha256) {
    }

    private M3Lex001Image() {
    }

    /** Writes the current version-3 image for a strictly ascending, duplicate-free word list. */
    public static byte[] write(List<? extends CharSequence> sortedWords) {
        return writeVersion(sortedWords, 3);
    }

    /** Qualification helper retaining a version-2 writer for compatibility tests. */
    static byte[] writeVersion2(List<? extends CharSequence> sortedWords) {
        return writeVersion(sortedWords, 2);
    }

    private static byte[] writeVersion(List<? extends CharSequence> sortedWords, int version) {
        long units = requireSortedUnits(sortedWords);
        int count = sortedWords.size();
        long payload = HEADER + 12L * count;
        long textEnd = Math.addExact(payload, Math.multiplyExact(2L, units));

        M3Lex001DirectIndex.Geometry direct =
                version == 3 ? M3Lex001DirectIndex.build(sortedWords) : null;
        long indexBytes = direct == null
                ? 0L
                : DIRECT_INDEX_HEADER
                        + Math.multiplyExact((long) direct.displacements().length, Integer.BYTES)
                        + Math.multiplyExact((long) direct.rows().length, Integer.BYTES);
        long size = Math.addExact(textEnd, indexBytes);
        if (size > MAX_BYTES) {
            throw new IllegalArgumentException("M3LEX001 image exceeds 1 GiB");
        }

        ByteBuffer out = ByteBuffer.allocate(Math.toIntExact(size)).order(ByteOrder.BIG_ENDIAN);
        out.putLong(0, M3LexiconFormat.M3LEX001.magic())
                .putInt(8, version)
                .putInt(12, count)
                .putLong(16, payload)
                .putLong(24, units);
        writeRows(out, sortedWords, Math.toIntExact(payload));
        if (direct != null) {
            writeDirectIndex(out, Math.toIntExact(textEnd), count, direct);
        }
        out.put(32, digest(out));
        return out.array();
    }

    /** Applies the M3StringPool admission rules; refuses on the first violation. */
    public static Facts verify(ByteBuffer image) throws M3LexiconFormatException {
        ByteBuffer map = image.duplicate().order(ByteOrder.BIG_ENDIAN);
        long size = map.limit();
        M3LexiconFormatException.require(
                size >= HEADER && size <= MAX_BYTES, "image size out of bounds");
        M3LexiconFormatException.require(
                map.getLong(0) == M3LexiconFormat.M3LEX001.magic(), "magic");
        int version = map.getInt(8);
        M3LexiconFormatException.require(
                version == 1 || version == 2 || version == 3, "unsupported version");
        int count = map.getInt(12);
        int directory = version == 1 ? 8 : 12;
        long payload = map.getLong(16);
        long units = map.getLong(24);
        long textEnd;
        try {
            textEnd = Math.addExact(payload, Math.multiplyExact(2L, units));
        } catch (ArithmeticException malformed) {
            throw new M3LexiconFormatException("invalid dimensions", malformed);
        }
        M3LexiconFormatException.require(
                count >= 0
                        && payload == HEADER + (long) directory * count
                        && units >= 0
                        && (version < 3 ? textEnd == size : textEnd + DIRECT_INDEX_HEADER <= size),
                "invalid dimensions");

        byte[] actual = digest(map);
        byte[] expected = new byte[32];
        map.get(32, expected);
        M3LexiconFormatException.require(
                MessageDigest.isEqual(expected, actual), "checksum mismatch");

        verifyRows(map, version, count, Math.toIntExact(payload), units);
        if (version == 3) {
            verifyDirectIndex(map, count, Math.toIntExact(payload), Math.toIntExact(textEnd));
        }
        return new Facts(
                version,
                count,
                units,
                M3LexiconWordIds.generationOfDigest(actual),
                actual);
    }

    /**
     * One {@code synexia.shards.tsv} row as read by
     * {@code m3/runtime-integration/verify-synexia-lexicon.py}.
     */
    public static String shardRow(
            int shardId,
            String file,
            List<? extends CharSequence> sortedWords,
            byte[] imageBytes)
            throws M3LexiconFormatException {
        Facts facts = verify(ByteBuffer.wrap(imageBytes));
        M3LexiconFormatException.require(
                facts.count() == sortedWords.size() && facts.count() > 0,
                "shard row needs the non-empty word list of this image");
        byte[] fileDigest = sha256(imageBytes);
        return shardId
                + "\t"
                + file
                + "\t"
                + escape(sortedWords.get(0))
                + "\t"
                + escape(sortedWords.get(sortedWords.size() - 1))
                + "\t"
                + facts.count()
                + "\t"
                + facts.units()
                + "\t"
                + java.util.HexFormat.of().formatHex(fileDigest);
    }

    static String escape(CharSequence value) {
        StringBuilder out = new StringBuilder(value.length());
        value.chars().forEach(unit -> {
            if (unit == '\\') {
                out.append("\\\\");
            } else if (unit < 0x20 || unit == 0x7f || Character.isSurrogate((char) unit)) {
                out.append(String.format("\\u%04x", unit));
            } else {
                out.append((char) unit);
            }
        });
        return out.toString();
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** UTF-16 unit of row data at a payload unit index (UTF-16LE). */
    static char unit(ByteBuffer map, int payload, int unitIndex) {
        int at = payload + (unitIndex << 1);
        return (char) ((map.get(at) & 0xff) | ((map.get(at + 1) & 0xff) << 8));
    }

    private static long requireSortedUnits(List<? extends CharSequence> words) {
        if (words == null) throw new NullPointerException("words");
        long units = 0;
        for (int index = 0; index < words.size(); index++) {
            CharSequence word = java.util.Objects.requireNonNull(words.get(index), "word");
            if (index > 0 && CharSequence.compare(words.get(index - 1), word) >= 0) {
                throw new IllegalArgumentException(
                        "words must be strictly ascending in UTF-16 order");
            }
            units = Math.addExact(units, word.length());
        }
        return units;
    }

    private static void writeRows(
            ByteBuffer out, List<? extends CharSequence> words, int payload) {
        int offset = 0;
        for (int row = 0; row < words.size(); row++) {
            CharSequence word = words.get(row);
            int hash = 0;
            for (int index = 0; index < word.length(); index++) {
                char c = word.charAt(index);
                hash = 31 * hash + c;
                int at = payload + ((offset + index) << 1);
                out.put(at, (byte) c).put(at + 1, (byte) (c >>> 8));
            }
            out.putInt(HEADER + row * 12, offset)
                    .putInt(HEADER + row * 12 + 4, word.length())
                    .putInt(HEADER + row * 12 + 8, hash);
            offset += word.length();
        }
    }

    private static void writeDirectIndex(
            ByteBuffer out,
            int indexStart,
            int count,
            M3Lex001DirectIndex.Geometry direct) {
        int[] displacements = direct.displacements();
        int[] rows = direct.rows();
        out.putLong(indexStart, DIRECT_INDEX_MAGIC)
                .putLong(indexStart + 8, direct.seed())
                .putInt(indexStart + 16, displacements.length)
                .putInt(indexStart + 20, rows.length)
                .putInt(indexStart + 24, count)
                .putInt(indexStart + 28, 0);
        int cursor = indexStart + DIRECT_INDEX_HEADER;
        for (int displacement : displacements) {
            out.putInt(cursor, displacement);
            cursor += Integer.BYTES;
        }
        for (int row : rows) {
            out.putInt(cursor, row);
            cursor += Integer.BYTES;
        }
    }

    private static void verifyRows(
            ByteBuffer map, int version, int count, int payload, long units)
            throws M3LexiconFormatException {
        int directory = version == 1 ? 8 : 12;
        int previousOffset = 0;
        int previousLength = -1;
        for (int row = 0; row < count; row++) {
            int entry = HEADER + row * directory;
            int offset = map.getInt(entry);
            int length = map.getInt(entry + 4);
            M3LexiconFormatException.require(
                    offset >= 0 && length >= 0 && (long) offset + length <= units,
                    "invalid record");
            int hash = javaHash(map, payload, offset, length);
            M3LexiconFormatException.require(
                    version == 1 || map.getInt(entry + 8) == hash,
                    "Java hash mismatch");
            M3LexiconFormatException.require(
                    previousLength < 0
                            || compare(
                                            map,
                                            payload,
                                            previousOffset,
                                            previousLength,
                                            offset,
                                            length)
                                    < 0,
                    "records must be UTF-16 sorted");
            previousOffset = offset;
            previousLength = length;
        }
    }

    private static void verifyDirectIndex(
            ByteBuffer map, int count, int payload, int indexStart)
            throws M3LexiconFormatException {
        M3LexiconFormatException.require(
                map.getLong(indexStart) == DIRECT_INDEX_MAGIC, "direct-index magic");
        long seed = map.getLong(indexStart + 8);
        int displacementCount = map.getInt(indexStart + 16);
        int rowSlotCount = map.getInt(indexStart + 20);
        int indexedRows = map.getInt(indexStart + 24);
        int flags = map.getInt(indexStart + 28);
        M3LexiconFormatException.require(
                displacementCount > 0
                        && Integer.bitCount(displacementCount) == 1
                        && rowSlotCount >= 2
                        && Integer.bitCount(rowSlotCount) == 1
                        && rowSlotCount <= M3Lex001DirectIndex.MAX_SLOTS
                        && indexedRows == count
                        && flags == 0,
                "direct-index header");

        long expected =
                (long) indexStart
                        + DIRECT_INDEX_HEADER
                        + (long) displacementCount * Integer.BYTES
                        + (long) rowSlotCount * Integer.BYTES;
        M3LexiconFormatException.require(expected == map.limit(), "direct-index dimensions");

        int displacementStart = indexStart + DIRECT_INDEX_HEADER;
        int rowStart = displacementStart + displacementCount * Integer.BYTES;
        boolean[] seen = new boolean[count];
        int occupied = 0;
        for (int slot = 0; slot < rowSlotCount; slot++) {
            int encoded = map.getInt(rowStart + slot * Integer.BYTES);
            M3LexiconFormatException.require(
                    encoded >= 0 && encoded <= count, "direct-index row");
            if (encoded != 0) {
                int row = encoded - 1;
                M3LexiconFormatException.require(!seen[row], "duplicate direct-index row");
                seen[row] = true;
                occupied++;
            }
        }
        M3LexiconFormatException.require(occupied == count, "missing direct-index row");

        int slotMask = rowSlotCount - 1;
        int bucketMask = displacementCount - 1;
        for (int row = 0; row < count; row++) {
            int entry = HEADER + row * 12;
            int offset = map.getInt(entry);
            int length = map.getInt(entry + 4);
            long key = M3Lex001DirectIndex.exactHash(map, payload, offset, length, seed);
            long mixed = M3Lex001DirectIndex.mix64(key);
            int bucket = ((int) mixed) & bucketMask;
            int displacement =
                    map.getInt(displacementStart + bucket * Integer.BYTES);
            int slot =
                    (((int) (mixed >>> Integer.SIZE)) + displacement) & slotMask;
            M3LexiconFormatException.require(
                    map.getInt(rowStart + slot * Integer.BYTES) == row + 1,
                    "direct-index candidate mismatch");
        }
    }

    private static int javaHash(ByteBuffer map, int payload, int offset, int length) {
        int hash = 0;
        for (int index = 0; index < length; index++) {
            hash = 31 * hash + unit(map, payload, offset + index);
        }
        return hash;
    }

    private static int compare(
            ByteBuffer map,
            int payload,
            int leftOffset,
            int leftLength,
            int rightOffset,
            int rightLength) {
        int common = Math.min(leftLength, rightLength);
        for (int index = 0; index < common; index++) {
            int order =
                    unit(map, payload, leftOffset + index)
                            - unit(map, payload, rightOffset + index);
            if (order != 0) return order;
        }
        return leftLength - rightLength;
    }

    static byte[] digest(ByteBuffer image) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(image.duplicate().position(0).limit(32));
            sha.update(image.duplicate().position(HEADER).limit(image.limit()));
            return sha.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
