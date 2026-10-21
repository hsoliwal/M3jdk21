// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Read-only memory-mapped packed-lexeme exact index.
 *
 * <p>The file contains no process pointers. Identity is the immutable codec fingerprint plus the
 * two 64-bit coordinate lanes and caller-owned value id. Multiple JVMs may map the same sidecar at
 * unrelated virtual addresses while reading exactly the same offsets and primitive cells.</p>
 */
public final class MappedMIndexPackedLexemeIndex implements MIndexPackedLexemeKeySource {
    private static final int DIGEST_BUFFER_BYTES = 64 * 1024;

    private final ByteBuffer buffer;
    private final MIndexLexemeCodec codec;
    private final int entryCount;
    private final int bucketCount;
    private final int slotCount;
    private final long seed;
    private final int displacementsAt;
    private final int highKeysAt;
    private final int lowKeysAt;
    private final int valueIdsAt;
    private final int slotsAt;
    private final long mappedBytes;
    private final String checksumSha256;

    private MappedMIndexPackedLexemeIndex(
            ByteBuffer buffer,
            MIndexLexemeCodec codec,
            int entryCount,
            int bucketCount,
            int slotCount,
            long seed,
            MIndexPackedLexemeImageFormat.Layout layout,
            long mappedBytes,
            String checksumSha256) {
        this.buffer = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        this.codec = codec;
        this.entryCount = entryCount;
        this.bucketCount = bucketCount;
        this.slotCount = slotCount;
        this.seed = seed;
        this.displacementsAt = Math.toIntExact(layout.displacementsAt());
        this.highKeysAt = Math.toIntExact(layout.highKeysAt());
        this.lowKeysAt = Math.toIntExact(layout.lowKeysAt());
        this.valueIdsAt = Math.toIntExact(layout.valueIdsAt());
        this.slotsAt = Math.toIntExact(layout.slotsAt());
        this.mappedBytes = mappedBytes;
        this.checksumSha256 = checksumSha256;
    }

    public static MappedMIndexPackedLexemeIndex open(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (FileChannel channel =
                FileChannel.open(path.toAbsolutePath().normalize(), StandardOpenOption.READ)) {
            long size = channel.size();
            require(
                    size >= MIndexPackedLexemeImageFormat.HEADER_BYTES
                            + MIndexPackedLexemeImageFormat.CHECKSUM_BYTES,
                    "packed lexeme image too small");
            require(size <= Integer.MAX_VALUE, "packed lexeme image exceeds 2 GiB");

            ByteBuffer buffer =
                    channel.map(FileChannel.MapMode.READ_ONLY, 0, size)
                            .order(ByteOrder.LITTLE_ENDIAN);
            require(
                    buffer.getLong(MIndexPackedLexemeImageFormat.MAGIC_AT)
                            == MIndexPackedLexemeImageFormat.MAGIC,
                    "packed lexeme image magic");
            require(
                    buffer.getInt(MIndexPackedLexemeImageFormat.FORMAT_AT)
                            == MIndexPackedLexemeImageFormat.FORMAT,
                    "packed lexeme image format");
            require(
                    buffer.getInt(MIndexPackedLexemeImageFormat.HEADER_BYTES_AT)
                            == MIndexPackedLexemeImageFormat.HEADER_BYTES,
                    "packed lexeme image header");
            require(
                    buffer.getInt(MIndexPackedLexemeImageFormat.MAX_SYMBOLS_AT)
                            == MIndexLexemeKey128.MAX_SYMBOLS,
                    "packed lexeme max-symbol contract");

            int alphabetSize =
                    buffer.getInt(MIndexPackedLexemeImageFormat.ALPHABET_SIZE_AT);
            int entryCount = buffer.getInt(MIndexPackedLexemeImageFormat.ENTRY_COUNT_AT);
            int bucketCount = buffer.getInt(MIndexPackedLexemeImageFormat.BUCKET_COUNT_AT);
            int slotCount = buffer.getInt(MIndexPackedLexemeImageFormat.SLOT_COUNT_AT);
            long seed = buffer.getLong(MIndexPackedLexemeImageFormat.SEED_AT);
            MIndexPackedLexemeImageFormat.Layout layout;
            try {
                layout =
                        MIndexPackedLexemeImageFormat.layout(
                                alphabetSize, entryCount, bucketCount, slotCount);
            } catch (IllegalArgumentException malformed) {
                throw new IOException("malformed packed lexeme counts", malformed);
            }

            require(size == layout.finalBytes(), "packed lexeme image length");
            require(
                    buffer.getLong(MIndexPackedLexemeImageFormat.ALPHABET_AT)
                            == layout.alphabetAt(),
                    "packed alphabet offset");
            require(
                    buffer.getLong(MIndexPackedLexemeImageFormat.DISPLACEMENTS_AT)
                            == layout.displacementsAt(),
                    "packed displacement offset");
            require(
                    buffer.getLong(MIndexPackedLexemeImageFormat.HIGH_KEYS_AT)
                            == layout.highKeysAt(),
                    "packed high-key offset");
            require(
                    buffer.getLong(MIndexPackedLexemeImageFormat.LOW_KEYS_AT)
                            == layout.lowKeysAt(),
                    "packed low-key offset");
            require(
                    buffer.getLong(MIndexPackedLexemeImageFormat.VALUE_IDS_AT)
                            == layout.valueIdsAt(),
                    "packed value-id offset");
            require(
                    buffer.getLong(MIndexPackedLexemeImageFormat.SLOTS_AT)
                            == layout.slotsAt(),
                    "packed slot offset");
            require(
                    buffer.getLong(MIndexPackedLexemeImageFormat.DATA_END_AT)
                            == layout.dataEnd(),
                    "packed data-end offset");

            byte[] actual = sha256(channel, layout.dataEnd());
            byte[] expected = new byte[MIndexPackedLexemeImageFormat.CHECKSUM_BYTES];
            ByteBuffer checksumView = buffer.duplicate();
            checksumView.position(Math.toIntExact(layout.dataEnd()));
            checksumView.get(expected);
            require(MessageDigest.isEqual(actual, expected), "packed lexeme checksum");

            char[] alphabet = new char[alphabetSize];
            int alphabetAt = Math.toIntExact(layout.alphabetAt());
            for (int symbol = 0; symbol < alphabet.length; symbol++) {
                alphabet[symbol] =
                        buffer.getChar(alphabetAt + symbol * Character.BYTES);
            }
            MIndexLexemeCodec codec = MIndexLexemeCodec.of("mapped-packed-lexeme", alphabet);
            require(
                    codec.fingerprint64()
                            == buffer.getLong(
                                    MIndexPackedLexemeImageFormat.CODEC_FINGERPRINT_AT),
                    "packed lexeme codec fingerprint");

            validate(
                    buffer,
                    codec,
                    layout,
                    entryCount,
                    bucketCount,
                    slotCount,
                    seed);

            return new MappedMIndexPackedLexemeIndex(
                    buffer,
                    codec,
                    entryCount,
                    bucketCount,
                    slotCount,
                    seed,
                    layout,
                    size,
                    HexFormat.of().formatHex(actual));
        }
    }

    public MIndexLexemeCodec codec() {
        return codec;
    }

    @Override
    public int packedEntryCount() {
        return entryCount;
    }

    @Override
    public long highKeyAtEntry(int entry) {
        Objects.checkIndex(entry, entryCount);
        return buffer.getLong(highKeysAt + entry * Long.BYTES);
    }

    @Override
    public long lowKeyAtEntry(int entry) {
        Objects.checkIndex(entry, entryCount);
        return buffer.getLong(lowKeysAt + entry * Long.BYTES);
    }

    @Override
    public int valueIdAtEntry(int entry) {
        Objects.checkIndex(entry, entryCount);
        return buffer.getInt(valueIdsAt + entry * Integer.BYTES);
    }

    public int bucketCount() {
        return bucketCount;
    }

    public int slotCount() {
        return slotCount;
    }

    public int denseBucketCells() {
        return MIndexPackedLexemeIndex.DENSE_BUCKET_CELLS;
    }

    public int runtimeCandidateBound() {
        return MIndexPackedLexemeIndex.FIXED_RUNTIME_CANDIDATES;
    }

    /** Compile a heap CSR matrix whose canonical coordinate lanes remain in this mapped index. */
    public MIndexLexemeMatrixIndex compileMatrix() {
        return MIndexLexemeMatrixIndex.compile(this);
    }

    public long mappedBytes() {
        return mappedBytes;
    }

    public String checksumSha256() {
        return checksumSha256;
    }

    public int lookup(CharSequence value) {
        Objects.requireNonNull(value, "value");
        if (!codec.canEncode(value)) return MIndexPackedLexemeIndex.UNENCODABLE;
        return lookup(codec.packHigh(value), codec.packLow(value));
    }

    public int lookup(MIndexLexemeKey128 key) {
        Objects.requireNonNull(key, "key");
        return lookup(key.high(), key.low());
    }

    private int lookup(long high, long low) {
        int bucket =
                MIndexPackedLexemeIndex.bucketIndex(
                        high, low, seed, bucketCount - 1);
        int displacement =
                buffer.getInt(displacementsAt + bucket * Integer.BYTES);
        int slot =
                MIndexPackedLexemeIndex.slotIndex(
                        high, low, seed, displacement, slotCount - 1);
        int stored = buffer.getInt(slotsAt + slot * Integer.BYTES);
        if (stored == 0) return MIndexPackedLexemeIndex.NOT_FOUND;
        int entry = stored - 1;
        return buffer.getLong(highKeysAt + entry * Long.BYTES) == high
                        && buffer.getLong(lowKeysAt + entry * Long.BYTES) == low
                ? buffer.getInt(valueIdsAt + entry * Integer.BYTES)
                : MIndexPackedLexemeIndex.NOT_FOUND;
    }

    private static void validate(
            ByteBuffer buffer,
            MIndexLexemeCodec codec,
            MIndexPackedLexemeImageFormat.Layout layout,
            int entries,
            int buckets,
            int slots,
            long seed)
            throws IOException {
        int displacementAt = Math.toIntExact(layout.displacementsAt());
        int highAt = Math.toIntExact(layout.highKeysAt());
        int lowAt = Math.toIntExact(layout.lowKeysAt());
        int valueAt = Math.toIntExact(layout.valueIdsAt());
        int slotsAt = Math.toIntExact(layout.slotsAt());

        for (int offset = 100; offset < 104; offset++) {
            require(buffer.get(offset) == 0, "nonzero packed reserved header byte");
        }
        for (int offset = 112; offset < MIndexPackedLexemeImageFormat.HEADER_BYTES; offset++) {
            require(buffer.get(offset) == 0, "nonzero packed reserved header byte");
        }
        for (int bucket = 0; bucket < buckets; bucket++) {
            require(
                    buffer.getInt(displacementAt + bucket * Integer.BYTES) >= 0,
                    "negative packed displacement");
        }

        for (int entry = 0; entry < entries; entry++) {
            long high = buffer.getLong(highAt + entry * Long.BYTES);
            long low = buffer.getLong(lowAt + entry * Long.BYTES);
            try {
                MIndexLexemeKey128 key = new MIndexLexemeKey128(high, low);
                for (int index = 0; index < key.length(); index++) {
                    require(
                            key.symbolAt(index) <= codec.alphabetSize(),
                            "packed key symbol outside codec");
                }
            } catch (IllegalArgumentException malformed) {
                throw new IOException("non-canonical packed key at entry " + entry, malformed);
            }
            require(
                    buffer.getInt(valueAt + entry * Integer.BYTES) >= 0,
                    "negative packed value id");
        }

        BitSet seen = new BitSet(entries);
        for (int slot = 0; slot < slots; slot++) {
            int stored = buffer.getInt(slotsAt + slot * Integer.BYTES);
            require(stored >= 0 && stored <= entries, "packed slot entry range");
            if (stored == 0) continue;
            int entry = stored - 1;
            require(!seen.get(entry), "duplicate packed entry slot");
            seen.set(entry);
            long high = buffer.getLong(highAt + entry * Long.BYTES);
            long low = buffer.getLong(lowAt + entry * Long.BYTES);
            int bucket =
                    MIndexPackedLexemeIndex.bucketIndex(
                            high, low, seed, buckets - 1);
            int displacement =
                    buffer.getInt(displacementAt + bucket * Integer.BYTES);
            int expected =
                    MIndexPackedLexemeIndex.slotIndex(
                            high, low, seed, displacement, slots - 1);
            require(expected == slot, "unreachable packed lexeme slot");
        }
        require(seen.cardinality() == entries, "missing packed lexeme entry");
    }

    private static byte[] sha256(FileChannel channel, long bytes) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
        ByteBuffer scratch = ByteBuffer.allocate(DIGEST_BUFFER_BYTES);
        long position = 0;
        while (position < bytes) {
            scratch.clear();
            scratch.limit((int) Math.min(scratch.capacity(), bytes - position));
            int read = channel.read(scratch, position);
            if (read < 0) throw new IOException("truncated packed lexeme image");
            if (read == 0) continue;
            position += read;
            scratch.flip();
            digest.update(scratch);
        }
        return digest.digest();
    }

    private static void require(boolean condition, String message) throws IOException {
        if (!condition) throw new IOException(message);
    }
}
