// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded, owner-scoped canonical store for immutable classfile-byte candidates.
 * This is NOT a class loader, HotSpot Klass table, CDS replacement or class-equivalence oracle.
 * Loader-dependent resolution, identity, initialization and GC ownership remain outside the store.
 *
 * <p>Stores the bytes once in a primitive arena with primitive offset, length, digest and
 * collision-chain lanes. Hashes select candidate entries; full bytes determine equality.
 * Insertion is serialized; independent hash precomputation can fan out, but deterministic
 * identifier assignment requires a deterministic insertion order.
 *
 * <p>No strong {@code Class}, {@code ClassLoader}, reflection object or native JNI pointer is
 * retained in this store. Owners must bound its lifetime; this prototype does not implement
 * HotSpot class unloading or cross-process sharing.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-class-space-canonical-images-20261030/src/main/java/com/synexia/m3/classspace/M3ClassSpace.java blob 9ca0f183b6f4; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
public final class ClassSpace {
    static final int SHA256_BYTES = 32;
    private static final int DEFAULT_LIMIT_BYTES = 64 * 1024 * 1024;
    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final DigestFunction DEFAULT_DIGEST = ClassSpace::sha256;

    @FunctionalInterface
    interface DigestFunction {
        byte[] digest(byte[] input);
    }

    private final int maxBytes;
    private final DigestFunction digestFunction;
    private byte[] arena;
    private int arenaLength;
    private int[] offsets = new int[16];
    private int[] lengths = new int[16];
    private int[] next = new int[16];
    private byte[] digests = new byte[16 * SHA256_BYTES];
    // Entry index + 1, with zero as the end-of-chain sentinel.
    private int[] heads = new int[16];
    private int entries;
    // Excludes source-mutating reentry from user-provided digest callbacks during a batch.
    private boolean preparingBatch;

    public ClassSpace() {
        this(DEFAULT_LIMIT_BYTES);
    }

    public ClassSpace(int maxBytes) {
        this(maxBytes, DEFAULT_DIGEST);
    }

    // Scoped to the package to enable deterministic adversarial digest-collision tests.
    ClassSpace(int maxBytes, DigestFunction digestFunction) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes must be positive");
        }
        this.maxBytes = maxBytes;
        this.digestFunction = Objects.requireNonNull(digestFunction, "digestFunction");
        this.arena = new byte[Math.min(maxBytes, 1024)];
    }

    /**
     * Interns exact bytes in this owner's bounded arena and returns a stable, owner-local ID.
     * The argument is never retained. Bytes are opaque: validation of the classfile format and
     * linking/verification must be performed independently by the JVM.
     */
    public synchronized int intern(byte[] classImage) {
        Objects.requireNonNull(classImage, "classImage");
        rejectBatchReentry();
        // Preserve the original digest callback's argument identity for this established API.
        byte[] digest = digestFunction.digest(classImage);
        return admit(classImage, 0, classImage.length, digest);
    }

    /**
     * Package-local, bounded region interning for a privately frozen classfile image.
     * The region is consumed synchronously and never retained or exposed. No temporary
     * UTF8 byte-array extraction is required with the built-in SHA-256 implementation.
     * Callers must not concurrently mutate the supplied array during the invocation,
     * exactly as with intern(byte[]). Invalid ranges fail before any store mutation.
     */
    synchronized int internRegion(byte[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        rejectBatchReentry();
        Objects.checkFromIndexSize(offset, length, source.length);
        // Keep test-provided digest functions semantically identical on the selected bytes.
        // The normal SHA-256 path hashes the immutable slice without first allocating it.
        byte[] digest = digestFunction == DEFAULT_DIGEST
                ? sha256Region(source, offset, length)
                : digestFunction.digest(Arrays.copyOfRange(source, offset, offset + length));
        return admit(source, offset, length, digest);
    }


    /**
     * Optional all-or-nothing constant-pool UTF8 batch admission.
     * The legacy per-symbol intern and Plan.internInto behavior are unchanged. This is a
     * primitive-array-only admission path for a privately frozen classfile source. All
     * byte ranges, digests, duplicate candidates, ordinal capacity and retained-byte budget
     * are validated before any new symbol is published. IDs follow the same serial CP order
     * as repeated internRegion calls; a quota failure leaves imageCount/retainedByteCount
     * unchanged. Class identity, resolution, unloading and native pointers are not handled.
     *
     * <p>Only tag 1 is interned; other CP ordinals return -1. Arrays are consumed within
     * the synchronized invocation, never retained or exposed. Digest callbacks are allowed
     * for differential tests, but cannot reenter an interning mutation during preflight.
     * A caller must not modify the source or metadata concurrently.
     */
    synchronized int[] internUtf8Batch(byte[] source, byte[] tags,
            int[] sourceOffsets, int[] sourceLengths) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(tags, "tags");
        Objects.requireNonNull(sourceOffsets, "sourceOffsets");
        Objects.requireNonNull(sourceLengths, "sourceLengths");
        rejectBatchReentry();
        int count = tags.length;
        if (count == 0 || count > 65535 || sourceOffsets.length != count
                || sourceLengths.length != count) {
            throw new IllegalArgumentException("Invalid constant-pool batch lanes");
        }
        // Require a fully valid candidate envelope before even invoking a digest function.
        for (int i = 1; i < count; i++) {
            if ((tags[i] & 0xff) == 1) {
                Objects.checkFromIndexSize(sourceOffsets[i], sourceLengths[i], source.length);
            }
        }
        int[] ids = new int[count];
        Arrays.fill(ids, -1);
        byte[] candidateDigests = new byte[count * SHA256_BYTES];
        int[] stagedCp = new int[count];
        int[] stagedNext = new int[count];
        int stageSize = 16;
        while (stageSize < count) stageSize <<= 1;
        int[] stagedHeads = new int[stageSize];
        int additional = 0;
        int additionalBytes = 0;
        // One provider instance per batch, not one provider lookup per symbol.
        // MessageDigest.digest() resets its state after each symbol; the provider
        // object is never stored in the owner or shared between invocations.
        MessageDigest batchSha = null;
        preparingBatch = true;
        try {
            for (int cp = 1; cp < count; cp++) {
                if ((tags[cp] & 0xff) != 1) continue;
                int start = sourceOffsets[cp];
                int length = sourceLengths[cp];
                byte[] digest;
                if (digestFunction == DEFAULT_DIGEST) {
                    // No provider lookup at all for batches without UTF8 entries.
                    if (batchSha == null) batchSha = newSha256();
                    batchSha.update(source, start, length);
                    digest = batchSha.digest();
                } else {
                    // Preserve custom DigestFunction's full-byte-array callback.
                    digest = digestFunction.digest(
                            Arrays.copyOfRange(source, start, start + length));
                }
                Objects.requireNonNull(digest, "digest");
                if (digest.length != SHA256_BYTES) {
                    throw new IllegalArgumentException("Expected a 32-byte content digest");
                }
                System.arraycopy(digest, 0, candidateDigests,
                        cp * SHA256_BYTES, SHA256_BYTES);
                int known = findExistingRegion(source, start, length, digest);
                if (known != -1) {
                    ids[cp] = known;
                    continue;
                }
                int stageBucket = bucket(digest, stageSize);
                int staged = -1;
                for (int cursor = stagedHeads[stageBucket]; cursor != 0;
                        cursor = stagedNext[cursor - 1]) {
                    int pending = cursor - 1;
                    int prior = stagedCp[pending];
                    if (sourceLengths[prior] == length
                            && equalsDigestRegions(candidateDigests, cp, prior)
                            && equalsRegions(source, start, sourceOffsets[prior], length)) {
                        staged = pending;
                        break;
                    }
                }
                if (staged != -1) {
                    ids[cp] = entries + staged;
                    continue;
                }
                if (entries > Integer.MAX_VALUE / SHA256_BYTES - 1 - additional) {
                    throw new IllegalStateException("Digest lane capacity exceeded");
                }
                if (length > maxBytes - arenaLength - additionalBytes) {
                    throw new IllegalStateException("ClassSpace owner byte budget exceeded");
                }
                ids[cp] = entries + additional;
                stagedCp[additional] = cp;
                stagedNext[additional] = stagedHeads[stageBucket];
                stagedHeads[stageBucket] = additional + 1;
                additionalBytes += length;
                additional++;
            }
            if (additional == 0) return ids;

            // All operations that can throw on ordinary invalid inputs or allocation
            // failure happen before changing the logical owner count or contents.
            final int targetEntries = entries + additional;
            final int targetBytes = arenaLength + additionalBytes;
            ensureArenaCapacity(targetBytes);
            ensureEntryCapacityForBatch(targetEntries);
            int bucketCount = heads.length;
            while (targetEntries > (bucketCount * 3) / 4 && bucketCount <= (1 << 27)) {
                bucketCount <<= 1;
            }
            int[] futureHeads = null;
            int[] futureNext = null;
            if (bucketCount != heads.length) {
                futureHeads = new int[bucketCount];
                futureNext = new int[next.length];
                for (int i = 0; i < entries; i++) {
                    int b = bucket(digests, i * SHA256_BYTES, bucketCount);
                    futureNext[i] = futureHeads[b];
                    futureHeads[b] = i + 1;
                }
                for (int i = 0; i < additional; i++) {
                    int cp = stagedCp[i];
                    int b = bucket(candidateDigests, cp * SHA256_BYTES, bucketCount);
                    futureNext[entries + i] = futureHeads[b];
                    futureHeads[b] = entries + i + 1;
                }
            }

            int newArenaLength = arenaLength;
            for (int i = 0; i < additional; i++) {
                int cp = stagedCp[i];
                int id = entries + i;
                int length = sourceLengths[cp];
                offsets[id] = newArenaLength;
                lengths[id] = length;
                System.arraycopy(source, sourceOffsets[cp], arena, newArenaLength, length);
                System.arraycopy(candidateDigests, cp * SHA256_BYTES,
                        digests, id * SHA256_BYTES, SHA256_BYTES);
                newArenaLength += length;
                if (futureHeads == null) {
                    int b = bucket(candidateDigests, cp * SHA256_BYTES, heads.length);
                    next[id] = heads[b];
                    heads[b] = id + 1;
                }
            }
            if (futureHeads != null) {
                heads = futureHeads;
                next = futureNext;
            }
            entries = targetEntries;
            arenaLength = newArenaLength;
            return ids;
        } finally {
            preparingBatch = false;
        }
    }

    private void rejectBatchReentry() {
        if (preparingBatch) {
            throw new IllegalStateException("Reentrant interning during atomic batch preflight");
        }
    }

    private int findExistingRegion(byte[] source, int offset, int length, byte[] digest) {
        int slot = bucket(digest, heads.length);
        for (int cursor = heads[slot]; cursor != 0; cursor = next[cursor - 1]) {
            int id = cursor - 1;
            if (lengths[id] == length && equalsDigest(id, digest)
                    && equalsBytes(id, source, offset, length)) return id;
        }
        return -1;
    }

    private static boolean equalsRegions(byte[] bytes, int a, int b, int length) {
        for (int i = 0; i < length; i++) {
            if (bytes[a + i] != bytes[b + i]) return false;
        }
        return true;
    }

    private static boolean equalsDigestRegions(byte[] packed, int aCp, int bCp) {
        int a = aCp * SHA256_BYTES;
        int b = bCp * SHA256_BYTES;
        for (int i = 0; i < SHA256_BYTES; i++) {
            if (packed[a + i] != packed[b + i]) return false;
        }
        return true;
    }

    /** Preserve logical preimage even if one of the capacity allocations fails. */
    private void ensureEntryCapacityForBatch(int requested) {
        if (requested <= offsets.length) return;
        int capacity = offsets.length;
        while (capacity < requested) {
            int nextCapacity = capacity + (capacity >>> 1);
            if (nextCapacity < capacity || nextCapacity > Integer.MAX_VALUE / SHA256_BYTES) {
                capacity = requested;
                break;
            }
            capacity = nextCapacity;
        }
        if (capacity > Integer.MAX_VALUE / SHA256_BYTES) {
            throw new IllegalStateException("Digest lane capacity exceeded");
        }
        int[] newOffsets = Arrays.copyOf(offsets, capacity);
        int[] newLengths = Arrays.copyOf(lengths, capacity);
        int[] newNext = Arrays.copyOf(next, capacity);
        byte[] newDigests = Arrays.copyOf(digests, capacity * SHA256_BYTES);
        offsets = newOffsets;
        lengths = newLengths;
        next = newNext;
        digests = newDigests;
    }

    /** Private, serial mutation shared by full-array and validated-region entrypoints. */
    private int admit(byte[] source, int offset, int length, byte[] digest) {
        Objects.requireNonNull(digest, "digest");
        if (digest.length != SHA256_BYTES) {
            throw new IllegalArgumentException("Expected a 32-byte content digest");
        }
        int bucket = bucket(digest, heads.length);
        for (int cursor = heads[bucket]; cursor != 0; cursor = next[cursor - 1]) {
            int candidate = cursor - 1;
            if (lengths[candidate] == length
                    && equalsDigest(candidate, digest)
                    && equalsBytes(candidate, source, offset, length)) {
                return candidate;
            }
        }
        // Duplicate lookup precedes the budget check, including for zero-byte regions.
        if (length > maxBytes - arenaLength) {
            throw new IllegalStateException("ClassSpace owner byte budget exceeded");
        }
        if (entries == Integer.MAX_VALUE) {
            throw new IllegalStateException("ClassSpace ordinal capacity exhausted");
        }
        ensureArenaCapacity(arenaLength + length);
        ensureEntryCapacity(entries + 1);
        // The previous post-insertion rebuild could throw OOME *after* the
        // symbol had been published. Reserve its only allocation before
        // changing entries, chains or retained bytes.
        int[] futureHeads = null;
        if (entries + 1 > (heads.length * 3) / 4 && heads.length <= (1 << 27)) {
            futureHeads = new int[heads.length << 1];
        }
        int id = entries++;
        offsets[id] = arenaLength;
        lengths[id] = length;
        System.arraycopy(source, offset, arena, arenaLength, length);
        System.arraycopy(digest, 0, digests, id * SHA256_BYTES, SHA256_BYTES);
        arenaLength += length;
        next[id] = heads[bucket];
        heads[bucket] = id + 1;
        if (futureHeads != null) {
            rebuildBuckets(futureHeads);
        }
        return id;
    }

    /** Returns a mutable compatibility copy; never exposes the shared arena. */
    public synchronized byte[] copy(int imageId) {
        checkImageId(imageId);
        return Arrays.copyOfRange(arena, offsets[imageId], offsets[imageId] + lengths[imageId]);
    }

    /**
     * Copies an owner-local atom directly into a caller-owned byte array. The entire
     * operation is bounds-checked before the first write: an invalid ID, destination
     * span or remaining archive payload budget leaves the destination untouched.
     * The owner retains its only copy of the immutable atom, and the destination
     * never aliases the primitive arena. Only the canonical archive writer uses
     * this package-private operation; public copy(int) remains unchanged.
     */
    synchronized int copyInto(int imageId, byte[] destination,
            int destinationOffset, int remainingPayloadBytes) {
        Objects.requireNonNull(destination, "destination");
        checkImageId(imageId);
        int length = lengths[imageId];
        if (remainingPayloadBytes < 0 || length > remainingPayloadBytes) {
            throw new IllegalStateException("Owner changed during snapshot");
        }
        Objects.checkFromIndexSize(destinationOffset, length, destination.length);
        System.arraycopy(arena, offsets[imageId], destination, destinationOffset, length);
        return length;
    }

    /** Hex-encoded candidate digest; never an equivalence or type-identity proof. */
    public synchronized String digestHex(int imageId) {
        checkImageId(imageId);
        char[] hex = new char[SHA256_BYTES * 2];
        for (int i = 0; i < SHA256_BYTES; i++) {
            int value = digests[imageId * SHA256_BYTES + i] & 0xff;
            hex[i * 2] = HEX[value >>> 4];
            hex[i * 2 + 1] = HEX[value & 0xf];
        }
        return new String(hex);
    }

    /**
     * Materializes a caller-owned binding. No binding is retained in the intern pool.
     * A binding does NOT assert that the image defines the supplied runtime class.
     */
    public synchronized Binding bind(Class<?> runtimeClass, int imageId) {
        Objects.requireNonNull(runtimeClass, "runtimeClass");
        checkImageId(imageId);
        return new Binding(runtimeClass, imageId);
    }

    public record Binding(Class<?> runtimeClass, int imageId) {
        public Binding {
            Objects.requireNonNull(runtimeClass, "runtimeClass");
            if (imageId < 0) {
                throw new IllegalArgumentException("imageId cannot be negative");
            }
        }
    }

    public synchronized int imageCount() {
        return entries;
    }

    public synchronized int retainedByteCount() {
        return arenaLength;
    }

    public int maxByteCount() {
        return maxBytes;
    }

    private void checkImageId(int id) {
        if (id < 0 || id >= entries) {
            throw new IndexOutOfBoundsException("Invalid owner-local image ID: " + id);
        }
    }

    private boolean equalsDigest(int id, byte[] digest) {
        int start = id * SHA256_BYTES;
        for (int i = 0; i < SHA256_BYTES; i++) {
            if (digests[start + i] != digest[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean equalsBytes(int id, byte[] bytes, int sourceOffset, int length) {
        int arenaOffset = offsets[id];
        for (int i = 0; i < length; i++) {
            if (arena[arenaOffset + i] != bytes[sourceOffset + i]) {
                return false;
            }
        }
        return true;
    }

    private static int bucket(byte[] digest, int size) {
        return bucket(digest, 0, size);
    }

    private static int bucket(byte[] digest, int offset, int size) {
        int h = 0x811c9dc5;
        for (int i = 0; i < SHA256_BYTES; i++) {
            h = (h ^ (digest[offset + i] & 0xff)) * 0x01000193;
        }
        return (h ^ (h >>> 16)) & (size - 1);
    }

    private void rebuildBuckets(int[] nextHeads) {
        // Caller preallocates this index before any logical insertion.
        int newSize = nextHeads.length;
        for (int id = 0; id < entries; id++) {
            // No allocation per stored atom while rebuilding the primitive index.
            int bucket = bucket(digests, id * SHA256_BYTES, newSize);
            next[id] = nextHeads[bucket];
            nextHeads[bucket] = id + 1;
        }
        heads = nextHeads;
    }

    private void ensureEntryCapacity(int requested) {
        if (requested <= offsets.length) {
            return;
        }
        int capacity = offsets.length + (offsets.length >> 1);
        if (capacity < requested) {
            capacity = requested;
        }
        if (capacity > Integer.MAX_VALUE / SHA256_BYTES) {
            throw new IllegalStateException("Digest lane capacity exceeded");
        }
        // Prepare every primitive column before publishing any new column.
        // If an intermediate allocation fails, the old coupled capacities
        // stay valid and retrying the same insertion remains safe.
        int[] futureOffsets = Arrays.copyOf(offsets, capacity);
        int[] futureLengths = Arrays.copyOf(lengths, capacity);
        int[] futureNext = Arrays.copyOf(next, capacity);
        byte[] futureDigests = Arrays.copyOf(digests, capacity * SHA256_BYTES);
        offsets = futureOffsets;
        lengths = futureLengths;
        next = futureNext;
        digests = futureDigests;
    }

    private void ensureArenaCapacity(int requested) {
        if (requested <= arena.length) {
            return;
        }
        int capacity = arena.length;
        while (capacity < requested) {
            // Saturating growth avoids wraparound and never exceeds the fixed owner budget.
            int growth = Math.max(1, capacity >>> 1);
            capacity += Math.min(growth, maxBytes - capacity);
        }
        arena = Arrays.copyOf(arena, capacity);
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JDK SHA-256 provider unavailable", ex);
        }
    }

    private static byte[] sha256(byte[] bytes) {
        return newSha256().digest(bytes);
    }

    private static byte[] sha256Region(byte[] bytes, int offset, int length) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(bytes, offset, length);
            return sha.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JDK SHA-256 provider unavailable", ex);
        }
    }
}
