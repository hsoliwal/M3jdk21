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
 * ({@code -Djdk.mindex.lexicon=<file>}). The verifier applies exactly the reader's admission rules
 * (M3StringPool.java blob 0905414556ad L991-1062), so an image it accepts is one the VM admits:
 * 64-byte big-endian header, v1 8-byte or v2 12-byte directory rows, UTF-16LE payload, strictly
 * ascending UTF-16 order, SHA-256 over {@code bytes[0,32) + bytes[64,end)} stored at 32..63, size at most
 * 1 GiB. Writing is a cold path; verification reads the mapping in place.
 */
public final class M3Lex001Image {
    /** Header bytes. */
    public static final int HEADER = 64;
    /** Upper size bound shared with M3StringPool. */
    public static final long MAX_BYTES = 1L << 30;

    /** Verified header facts. */
    public record Facts(int version, int count, long units, long generation64, byte[] sha256) {
    }

    private M3Lex001Image() {
    }

    /** Writes a version-2 image for a strictly ascending, duplicate-free word list. */
    public static byte[] write(List<? extends CharSequence> sortedWords) {
        long units = requireSortedUnits(sortedWords);
        int count = sortedWords.size();
        long payload = HEADER + 12L * count;
        long size = payload + 2L * units;
        if (size > MAX_BYTES) {
            throw new IllegalArgumentException("M3LEX001 image exceeds 1 GiB");
        }
        ByteBuffer out = ByteBuffer.allocate((int) size).order(ByteOrder.BIG_ENDIAN);
        out.putLong(0, M3LexiconFormat.M3LEX001.magic()).putInt(8, 2).putInt(12, count)
                .putLong(16, payload).putLong(24, units);
        writeRows(out, sortedWords, (int) payload);
        out.put(32, digest(out));
        return out.array();
    }

    /** Applies the M3StringPool admission rules; refuses on the first violation. */
    public static Facts verify(ByteBuffer image) throws M3LexiconFormatException {
        ByteBuffer map = image.duplicate().order(ByteOrder.BIG_ENDIAN);
        long size = map.limit();
        M3LexiconFormatException.require(size >= HEADER && size <= MAX_BYTES, "image size out of bounds");
        M3LexiconFormatException.require(map.getLong(0) == M3LexiconFormat.M3LEX001.magic(), "magic");
        int version = map.getInt(8);
        M3LexiconFormatException.require(version == 1 || version == 2, "unsupported version");
        int count = map.getInt(12);
        int directory = version == 1 ? 8 : 12;
        long payload = map.getLong(16);
        long units = map.getLong(24);
        M3LexiconFormatException.require(count >= 0 && payload == HEADER + (long) directory * count
                && units >= 0 && payload + 2L * units == size, "invalid dimensions");
        byte[] actual = digest(map);
        byte[] expected = new byte[32];
        map.get(32, expected);
        M3LexiconFormatException.require(MessageDigest.isEqual(expected, actual), "checksum mismatch");
        verifyRows(map, version, count, (int) payload, units);
        return new Facts(version, count, units, M3LexiconWordIds.generationOfDigest(actual), actual);
    }

    /**
     * One {@code synexia.shards.tsv} row as read by {@code m3/runtime-integration/verify-synexia-lexicon.py}
     * (columns shard_id, file, first_lexeme, last_lexeme, image_records, utf16_units, sha256); lexemes use
     * that tool's UTF-16-preserving sidecar escaping ({@code \\} and {@code \\uXXXX}).
     */
    public static String shardRow(int shardId, String file, List<? extends CharSequence> sortedWords,
            byte[] imageBytes) throws M3LexiconFormatException {
        Facts facts = verify(ByteBuffer.wrap(imageBytes));
        M3LexiconFormatException.require(facts.count() == sortedWords.size() && facts.count() > 0,
                "shard row needs the non-empty word list of this image");
        byte[] fileDigest = sha256(imageBytes);
        return shardId + "\t" + file + "\t" + escape(sortedWords.get(0)) + "\t"
                + escape(sortedWords.get(sortedWords.size() - 1)) + "\t" + facts.count() + "\t"
                + facts.units() + "\t" + java.util.HexFormat.of().formatHex(fileDigest);
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
        long units = 0;
        for (int index = 0; index < words.size(); index++) {
            if (index > 0 && CharSequence.compare(words.get(index - 1), words.get(index)) >= 0) {
                throw new IllegalArgumentException("words must be strictly ascending in UTF-16 order");
            }
            units += words.get(index).length();
        }
        return units;
    }

    private static void writeRows(ByteBuffer out, List<? extends CharSequence> words, int payload) {
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
            out.putInt(HEADER + row * 12, offset).putInt(HEADER + row * 12 + 4, word.length())
                    .putInt(HEADER + row * 12 + 8, hash);
            offset += word.length();
        }
    }

    private static void verifyRows(ByteBuffer map, int version, int count, int payload, long units)
            throws M3LexiconFormatException {
        int directory = version == 1 ? 8 : 12;
        int previousOffset = 0;
        int previousLength = -1;
        for (int row = 0; row < count; row++) {
            int entry = HEADER + row * directory;
            int offset = map.getInt(entry);
            int length = map.getInt(entry + 4);
            M3LexiconFormatException.require(offset >= 0 && length >= 0 && (long) offset + length <= units,
                    "invalid record");
            int hash = javaHash(map, payload, offset, length);
            M3LexiconFormatException.require(version == 1 || map.getInt(entry + 8) == hash, "Java hash mismatch");
            M3LexiconFormatException.require(previousLength < 0
                    || compare(map, payload, previousOffset, previousLength, offset, length) < 0,
                    "records must be UTF-16 sorted");
            previousOffset = offset;
            previousLength = length;
        }
    }

    private static int javaHash(ByteBuffer map, int payload, int offset, int length) {
        int hash = 0;
        for (int index = 0; index < length; index++) {
            hash = 31 * hash + unit(map, payload, offset + index);
        }
        return hash;
    }

    private static int compare(ByteBuffer map, int payload, int leftOffset, int leftLength,
            int rightOffset, int rightLength) {
        int common = Math.min(leftLength, rightLength);
        for (int index = 0; index < common; index++) {
            int order = unit(map, payload, leftOffset + index) - unit(map, payload, rightOffset + index);
            if (order != 0) {
                return order;
            }
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
