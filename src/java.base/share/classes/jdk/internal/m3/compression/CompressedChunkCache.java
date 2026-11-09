// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Opt-in, format-neutral cache of deterministic compressed chunks.
 *
 * <p>All state is parent-owned, array-backed and bounded by a payload budget.
 * Hashes only select candidates: raw byte equality is checked before every hit.
 * The caller's configuration identity MUST include codec/format, implementation
 * version when relevant, level, framing, dictionary identity and all other
 * settings affecting the returned bytes. The encoder must be deterministic,
 * independent between invocations and must not mutate its input.
 *
 * <p>This is not a wire format, transcoder, streaming-context cache or
 * automatic replacement for any existing compression API. A single cache may
 * be shared by threads; a simultaneous miss may encode redundantly.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/main/java/com/synexia/algorithms/compression/M3CompressedChunkCache.java blob d5f34c713223; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class CompressedChunkCache {
    /** Produces a complete deterministic encoded chunk, not a streaming fragment. */
    @FunctionalInterface
    public interface DeterministicEncoder {
        byte[] encode(byte[] input);
    }

    private static final int MIN_TABLE_BITS = 1;
    private static final int MAX_TABLE_BITS = 12;
    private static final int MAX_PROFILE_LENGTH = 512;

    private final byte[][] originals;
    private final byte[][] compressed;
    private final byte[][] digests;
    private final String[] profiles;
    private final long[] access;
    private final int mask;
    private final long maxPayloadBytes;
    private long payloadBytes;
    private long clock;
    // A trim invalidates in-flight insertions; the encoder runs outside the cache lock.
    private long generation;
    private long hits;
    private long misses;
    private long evictions;
    private int entries;

    /**
     * @param tableBits direct-mapped slots, 2^tableBits, with 1..12 allowed
     * @param maxPayloadBytes bound on retained raw+compressed byte payloads
     */
    public CompressedChunkCache(int tableBits, long maxPayloadBytes) {
        if (tableBits < MIN_TABLE_BITS || tableBits > MAX_TABLE_BITS) {
            throw new IllegalArgumentException("table bits must be 1..12");
        }
        if (maxPayloadBytes < 1) {
            throw new IllegalArgumentException("positive byte budget required");
        }
        int slots = 1 << tableBits;
        originals = new byte[slots][];
        compressed = new byte[slots][];
        digests = new byte[slots][];
        profiles = new String[slots];
        access = new long[slots];
        mask = slots - 1;
        this.maxPayloadBytes = maxPayloadBytes;
    }

    /**
     * Return caller-owned encoded bytes. The original source, encoder input,
     * retained cache entry, and returned result never alias one another.
     * Cache hits require exact raw equality in addition to SHA-256 identity.
     */
    public byte[] encode(byte[] source, int offset, int length,
            String wireConfigurationId, DeterministicEncoder encoder) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(wireConfigurationId, "wireConfigurationId");
        Objects.requireNonNull(encoder, "encoder");
        if (offset < 0 || length < 0 || offset > source.length - length) {
            throw new IllegalArgumentException("invalid source window");
        }
        if (wireConfigurationId.isBlank() || wireConfigurationId.length() > MAX_PROFILE_LENGTH) {
            throw new IllegalArgumentException("invalid wire configuration identity");
        }
        byte[] snapshot = Arrays.copyOfRange(source, offset, offset + length);
        byte[] digest = sha256(snapshot);
        int slot = selectSlot(digest, wireConfigurationId);
        long observedGeneration;
        synchronized (this) {
            if (matches(slot, digest, wireConfigurationId, snapshot)) {
                hits++;
                touch(slot);
                return compressed[slot].clone();
            }
            misses++;
            observedGeneration = generation;
        }

        // Never expose retained snapshots to an encoder, even on exceptional exits.
        byte[] encoderInput = snapshot.clone();
        byte[] result = Objects.requireNonNull(encoder.encode(encoderInput),
                "encoder returned null");
        if (!Arrays.equals(encoderInput, snapshot)) {
            throw new IllegalStateException("deterministic encoder modified its input");
        }
        byte[] immutableResult = result.clone();
        long weight = (long) snapshot.length + immutableResult.length;
        synchronized (this) {
            // Another thread may already have installed exactly this content.
            if (matches(slot, digest, wireConfigurationId, snapshot)) {
                hits++;
                touch(slot);
                return compressed[slot].clone();
            }
            // A trim during encoding revokes this in-flight cache fill, but not
            // the caller's valid newly encoded return value.
            if (observedGeneration != generation) {
                return immutableResult.clone();
            }
            if (weight <= maxPayloadBytes) {
                evict(slot);
                while (payloadBytes > maxPayloadBytes - weight) {
                    int oldest = oldestSlot();
                    if (oldest < 0) throw new IllegalStateException("cache budget accounting");
                    evict(oldest);
                }
                originals[slot] = snapshot;
                compressed[slot] = immutableResult;
                digests[slot] = digest;
                profiles[slot] = wireConfigurationId;
                payloadBytes += weight;
                entries++;
                touch(slot);
            }
        }
        return immutableResult.clone();
    }

    /** Release retained content under memory pressure; counters remain cumulative. */
    public synchronized void trim() {
        generation++;
        Arrays.fill(originals, null);
        Arrays.fill(compressed, null);
        Arrays.fill(digests, null);
        Arrays.fill(profiles, null);
        Arrays.fill(access, 0L);
        payloadBytes = 0;
        entries = 0;
        clock = 0;
    }

    public synchronized long retainedPayloadBytes() { return payloadBytes; }
    public synchronized int retainedEntries() { return entries; }
    public synchronized long cacheHits() { return hits; }
    public synchronized long cacheMisses() { return misses; }
    public synchronized long evictions() { return evictions; }

    private boolean matches(int slot, byte[] digest, String profile, byte[] raw) {
        return originals[slot] != null
                && profile.equals(profiles[slot])
                && Arrays.equals(digests[slot], digest)
                && Arrays.equals(originals[slot], raw);
    }

    private void evict(int slot) {
        if (originals[slot] == null) return;
        payloadBytes -= (long) originals[slot].length + compressed[slot].length;
        originals[slot] = null;
        compressed[slot] = null;
        digests[slot] = null;
        profiles[slot] = null;
        access[slot] = 0;
        entries--;
        evictions++;
    }

    private int oldestSlot() {
        int oldest = -1;
        for (int i = 0; i < originals.length; i++) {
            if (originals[i] != null && (oldest < 0 || access[i] < access[oldest])) {
                oldest = i;
            }
        }
        return oldest;
    }

    private void touch(int slot) {
        // Rare rollover changes eviction priority, never cache correctness.
        if (clock == Long.MAX_VALUE) {
            Arrays.fill(access, 0L);
            clock = 0;
        }
        access[slot] = ++clock;
    }

    private int selectSlot(byte[] digest, String profile) {
        int hash = profile.hashCode();
        for (int i = 0; i < digest.length; i += 4) {
            int word = (digest[i] & 0xff)
                    | ((digest[i + 1] & 0xff) << 8)
                    | ((digest[i + 2] & 0xff) << 16)
                    | ((digest[i + 3] & 0xff) << 24);
            hash = (hash * 0x9e3779b1) ^ word;
        }
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        return (hash ^ (hash >>> 15)) & mask;
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
