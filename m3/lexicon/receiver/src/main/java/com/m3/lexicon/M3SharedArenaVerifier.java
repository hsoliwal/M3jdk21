// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32C;

/**
 * Java verifier for a committed SYNARR01 arena snapshot (writer: Synexia
 * {@code com.synexia.indexstring.shared.SharedArrayPool}; reader:
 * {@code java.lang.M3StringPool.Lexicon.openSharedArena}). It applies the reader's admission rules
 * (M3StringPool.java L1258-1320): header CRC32C, per-record magic/geometry/CRC32C, the COMMIT01 trailer,
 * tuple parts that reference prior scalar records in bounds, and the Java hash of every language-0 UTF-16
 * scalar row (the rows that enter the String plane and receive word ids).
 */
public final class M3SharedArenaVerifier {
    private static final int RECORD_MAGIC = 0x41525231;
    private static final long COMMIT = 0x434f4d4d49543031L;
    private static final int RECORD_HEADER = 80;
    private static final int MAX_PAYLOAD = 64 * 1024 * 1024;

    /** Verified arena facts; {@code stringRows} = records admitted to the String plane. */
    public record Facts(int records, int stringRows, long namespaceHigh, long namespaceLow,
            long generation64) {
    }

    private M3SharedArenaVerifier() {
    }

    /** Verifies the whole snapshot; refuses on the first violation. */
    public static Facts verify(ByteBuffer image) throws M3LexiconFormatException {
        ByteBuffer map = image.duplicate().order(ByteOrder.BIG_ENDIAN);
        verifyHeader(map);
        Map<Integer, int[]> prior = new HashMap<>();
        int position = 64;
        int records = 0;
        int stringRows = 0;
        while (position < map.limit()) {
            M3LexiconFormatException.require(++records <= map.getInt(40), "record count exceeds header");
            int total = verifyRecord(map, position, prior);
            stringRows += isStringRow(map, position) ? 1 : 0;
            position += total;
        }
        long high = map.getLong(16);
        long low = map.getLong(24);
        return new Facts(records, stringRows, high, low, M3LexiconWordIds.generationOfArena(high, low));
    }

    private static void verifyHeader(ByteBuffer map) throws M3LexiconFormatException {
        M3LexiconFormatException.require(map.limit() >= 64, "truncated header");
        CRC32C crc = new CRC32C();
        crc.update(map.duplicate().position(0).limit(56));
        M3LexiconFormatException.require(map.getLong(0) == M3LexiconFormat.SYNARR01.magic()
                && map.getInt(8) == 1 && map.getLong(56) == crc.getValue()
                && map.getLong(32) >= map.limit() && map.getInt(40) >= 0, "invalid SYNARR01 header");
    }

    private static int verifyRecord(ByteBuffer map, int position, Map<Integer, int[]> prior)
            throws M3LexiconFormatException {
        M3LexiconFormatException.require(map.limit() - position >= RECORD_HEADER + 8, "incomplete record");
        int total = map.getInt(position + 4);
        int width = map.getInt(position + 8);
        int language = map.getInt(position + 12);
        int units = map.getInt(position + 16);
        int parts = map.getInt(position + 20);
        int payload = map.getInt(position + 28);
        long expected = parts < 0 ? (long) width * units : (long) parts * 16;
        M3LexiconFormatException.require(map.getInt(position) == RECORD_MAGIC && (width == 1 || width == 2)
                && units >= 0 && parts >= -1 && payload >= 0 && payload <= MAX_PAYLOAD && expected == payload
                && RECORD_HEADER + payload + 8L == total && total <= map.limit() - position
                && map.getLong(position + 72) == 0 && (width == 2 || language == 0)
                && map.getLong(position + total - 8) == (COMMIT ^ position ^ total), "invalid record");
        verifyCrc(map, position, payload);
        verifyBody(map, position, width, units, parts, prior);
        prior.put(position, new int[] {width, units, parts});
        return total;
    }

    private static void verifyCrc(ByteBuffer map, int position, int payload) throws M3LexiconFormatException {
        CRC32C crc = new CRC32C();
        crc.update(map.duplicate().position(position).limit(position + 64));
        crc.update(map.duplicate().position(position + RECORD_HEADER).limit(position + RECORD_HEADER + payload));
        M3LexiconFormatException.require(crc.getValue() == map.getLong(position + 64), "record CRC mismatch");
    }

    private static void verifyBody(ByteBuffer map, int position, int width, int units, int parts,
            Map<Integer, int[]> prior) throws M3LexiconFormatException {
        if (parts >= 0) {
            long length = 0;
            for (int part = 0; part < parts; part++) {
                int at = position + RECORD_HEADER + part * 16;
                long reference = map.getLong(at);
                int[] atom = reference == (int) reference ? prior.get((int) reference) : null;
                int start = map.getInt(at + 8);
                int size = map.getInt(at + 12);
                M3LexiconFormatException.require(atom != null && atom[0] == width && atom[2] == -1
                        && start >= 0 && size > 0 && (long) start + size <= atom[1], "invalid tuple");
                length += size;
            }
            M3LexiconFormatException.require(length == units, "invalid tuple length");
        } else if (isStringRow(map, position)) {
            int hash = 0;
            for (int index = 0; index < units; index++) {
                hash = 31 * hash + map.getChar(position + RECORD_HEADER + index * 2);
            }
            M3LexiconFormatException.require(hash == map.getInt(position + 24), "hash mismatch");
        }
    }

    private static boolean isStringRow(ByteBuffer map, int position) {
        return map.getInt(position + 8) == 2 && map.getInt(position + 12) == 0
                && map.getInt(position + 16) > 0 && map.getInt(position + 20) < 0;
    }
}
