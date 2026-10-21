// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/** Binary contract for the optional read-only packed-lexeme mmap sidecar. */
final class MIndexPackedLexemeImageFormat {
    static final long MAGIC = 0x4d495041434b3136L; // MIPACK16
    static final int FORMAT = 1;
    static final int HEADER_BYTES = 128;
    static final int CHECKSUM_BYTES = 32;

    static final int MAGIC_AT = 0;
    static final int FORMAT_AT = 8;
    static final int HEADER_BYTES_AT = 12;
    static final int MAX_SYMBOLS_AT = 16;
    static final int ALPHABET_SIZE_AT = 20;
    static final int ENTRY_COUNT_AT = 24;
    static final int SLOT_COUNT_AT = 28;
    static final int CODEC_FINGERPRINT_AT = 32;
    static final int ALPHABET_AT = 40;
    static final int DISPLACEMENTS_AT = 48;
    static final int HIGH_KEYS_AT = 56;
    static final int LOW_KEYS_AT = 64;
    static final int VALUE_IDS_AT = 72;
    static final int SLOTS_AT = 80;
    static final int DATA_END_AT = 88;
    static final int BUCKET_COUNT_AT = 96;
    static final int SEED_AT = 104;

    private MIndexPackedLexemeImageFormat() {}

    static Layout layout(int alphabetSize, int entries, int buckets, int slots) {
        if (alphabetSize < 0 || alphabetSize > MIndexLexemeCodec.MAX_ALPHABET_SIZE) {
            throw new IllegalArgumentException("invalid packed alphabet size");
        }
        if (entries < 0) throw new IllegalArgumentException("negative packed entry count");
        if (buckets < 2 || Integer.bitCount(buckets) != 1) {
            throw new IllegalArgumentException("packed bucket count must be a power of two");
        }
        if (slots < 2 || Integer.bitCount(slots) != 1) {
            throw new IllegalArgumentException("packed slot count must be a power of two");
        }

        long alphabetAt = HEADER_BYTES;
        long displacementsAt = align8(add(alphabetAt, Character.BYTES * (long) alphabetSize));
        long highKeysAt =
                align8(add(displacementsAt, Integer.BYTES * (long) buckets));
        long lowKeysAt = align8(add(highKeysAt, Long.BYTES * (long) entries));
        long valueIdsAt = align8(add(lowKeysAt, Long.BYTES * (long) entries));
        long slotsAt = align8(add(valueIdsAt, Integer.BYTES * (long) entries));
        long dataEnd = align8(add(slotsAt, Integer.BYTES * (long) slots));
        long finalBytes = add(dataEnd, CHECKSUM_BYTES);
        if (finalBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("packed lexeme image exceeds 2 GiB; shard it");
        }
        return new Layout(
                alphabetAt,
                displacementsAt,
                highKeysAt,
                lowKeysAt,
                valueIdsAt,
                slotsAt,
                dataEnd,
                finalBytes);
    }

    static long align8(long value) {
        return (value + 7L) & ~7L;
    }

    static long add(long at, long bytes) {
        long result = Math.addExact(at, bytes);
        if (result < 0) throw new IllegalArgumentException("packed lexeme image overflow");
        return result;
    }

    record Layout(
            long alphabetAt,
            long displacementsAt,
            long highKeysAt,
            long lowKeysAt,
            long valueIdsAt,
            long slotsAt,
            long dataEnd,
            long finalBytes) {}
}
