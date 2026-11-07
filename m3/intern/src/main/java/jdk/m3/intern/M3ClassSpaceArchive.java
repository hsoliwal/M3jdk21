// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, versioned M3ClassSpace byte-image archive candidate.
 *
 * <p>This is not a HotSpot Klass, CDS metadata image, resolved constant pool, JVM type
 * identity, verifier, or trust boundary. IDs describe exact immutable byte payloads only.
 * A memory-mapped caller must keep the underlying file immutable for the lifetime of a view;
 * read-only ByteBuffer itself does not prevent modifications by another file descriptor.
 * Never infer equivalence of runtime classes from this image.
 *
 * <p>V1: 8 magic bytes, 4-byte version, count, total payload bytes and reserved flags;
 * count fixed 40-byte index cells (relative offset, length, SHA-256 of entry bytes),
 * concatenated payload bytes, and SHA-256 of all preceding archive bytes. Big endian.
 * Indexes are ordinal-preserving, contiguous, non-overlapping and bounded.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-class-space-canonical-images-20261030/src/main/java/com/synexia/m3/classspace/M3ClassSpaceArchive.java blob bb3eb9e9869a; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
final class M3ClassSpaceArchive {
    private static final byte[] MAGIC = {'M','3','C','S','I','M','G','1'};
    private static final int VERSION = 1;
    private static final int HEADER_BYTES = 24;
    private static final int CELL_BYTES = 40;
    private static final int SHA_BYTES = 32;
    private static final int MAX_ENTRIES = 1 << 20;
    private static final int MAX_IMAGE_BYTES = 128 * 1024 * 1024;

    private M3ClassSpaceArchive() {
    }

    /** Serializes an owner under its own monitor, preventing interleaved ID publication. */
    static byte[] freeze(M3ClassSpace owner) {
        Objects.requireNonNull(owner, "owner");
        synchronized (owner) {
            int count = owner.imageCount();
            int payload = owner.retainedByteCount();
            int total = checkedTotal(count, payload);
            ByteBuffer archive = ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN);
            archive.put(MAGIC).putInt(VERSION).putInt(count).putInt(payload).putInt(0);
            int payloadBase = HEADER_BYTES + count * CELL_BYTES;
            byte[] output = archive.array();
            int cursor = 0;
            MessageDigest sha = newSha256();
            for (int id = 0; id < count; id++) {
                // The shared owner writes directly into the final private archive
                // buffer; no temporary byte[] is allocated for each atom.
                int length = owner.copyInto(id, output, payloadBase + cursor,
                        payload - cursor);
                int cell = HEADER_BYTES + id * CELL_BYTES;
                archive.putInt(cell, cursor);
                archive.putInt(cell + Integer.BYTES, length);
                sha.update(output, payloadBase + cursor, length);
                archive.position(cell + 2 * Integer.BYTES);
                archive.put(sha.digest());
                cursor += length;
            }
            if (cursor != payload) {
                throw new IllegalStateException("Owner byte count and ordinal data disagree");
            }
            sha.update(output, 0, total - SHA_BYTES);
            System.arraycopy(sha.digest(), 0, output, total - SHA_BYTES, SHA_BYTES);
            return output;
        }
    }

    /** Defensive-copy intake; caller mutations cannot change the opened archive. */
    static View openCopy(byte[] archive) {
        Objects.requireNonNull(archive, "archive");
        if (archive.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Archive byte budget exceeded");
        }
        return inspect(ByteBuffer.wrap(archive.clone()).asReadOnlyBuffer());
    }

    /**
     * Strictly admits a canonical owner-local image without rebuilding the M3ClassSpace.
     * Unlike the established openCopy method, rejects repeated exact byte atoms even
     * when the index, per-entry SHA-256 and archive root are all self-consistent.
     * The caller-owned image is still defensively copied before any inspection.
     */
    static View openCanonicalCopy(byte[] archive) {
        View view = openCopy(archive);
        view.verifyCanonicalIds();
        return view;
    }

    /**
     * Zero-copy strict admission for a caller-controlled immutable mapping.
     * Checking at open time does not make the backing file immutable: the caller
     * must keep the mapping and underlying file sealed for the entire view lifetime.
     */
    static View openCanonicalTrustedMapped(ByteBuffer mapped) {
        View view = openTrustedMapped(mapped);
        view.verifyCanonicalIds();
        return view;
    }

    /**
     * Zero-copy intake only for owner-controlled, immutable read-only direct buffers.
     * Intended for FileChannel.map(READ_ONLY), with a separately enforced immutable-file
     * custody policy. The caller MUST NOT alter the mapped file while views are in use.
     */
    static View openTrustedMapped(ByteBuffer mapped) {
        Objects.requireNonNull(mapped, "mapped");
        if (!mapped.isDirect() || !mapped.isReadOnly()) {
            throw new IllegalArgumentException("Requires direct read-only immutable mapping");
        }
        return inspect(mapped.slice());
    }

    /**
     * Opt-in monitored read path for an externally owned direct mapping. Normal
     * zero-copy reads and the immutable-file custody precondition are unchanged.
     * PinnedView copies index coordinates and SHA-256 digests into private primitive
     * arrays, so subsequent header edits cannot redirect verified payload reads.
     * A verification is a point-in-time check, not a lock on another file descriptor.
     */
    static PinnedView openPinnedCanonicalTrustedMapped(ByteBuffer mapped) {
        return PinnedView.capture(openCanonicalTrustedMapped(mapped));
    }

    private static View inspect(ByteBuffer bytes) {
        int total = bytes.remaining();
        if (total < HEADER_BYTES + SHA_BYTES || total > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Archive envelope size is invalid");
        }
        ByteBuffer image = bytes.slice().asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < MAGIC.length; i++) {
            if (image.get(i) != MAGIC[i]) {
                throw new IllegalArgumentException("Archive magic differs");
            }
        }
        if (image.getInt(8) != VERSION || image.getInt(20) != 0) {
            throw new IllegalArgumentException("Unsupported archive version or flags");
        }
        int count = image.getInt(12);
        int payload = image.getInt(16);
        int expected = checkedTotal(count, payload);
        if (expected != total) {
            throw new IllegalArgumentException("Archive frame length differs");
        }
        MessageDigest sha = newSha256();
        int payloadBase = HEADER_BYTES + count * CELL_BYTES;
        int cursor = 0;
        for (int id = 0; id < count; id++) {
            int cell = HEADER_BYTES + id * CELL_BYTES;
            int offset = image.getInt(cell);
            int length = image.getInt(cell + Integer.BYTES);
            if (offset != cursor || length < 0 || length > payload - cursor) {
                throw new IllegalArgumentException("Non-contiguous or invalid entry span");
            }
            ByteBuffer segment = boundedSlice(image, payloadBase + cursor, length);
            sha.update(segment);
            byte[] computed = sha.digest();
            for (int j = 0; j < SHA_BYTES; j++) {
                if (image.get(cell + 2 * Integer.BYTES + j) != computed[j]) {
                    throw new IllegalArgumentException("Entry SHA-256 differs");
                }
            }
            cursor += length;
        }
        if (cursor != payload) {
            throw new IllegalArgumentException("Archive entry payload is incomplete");
        }
        sha.update(boundedSlice(image, 0, total - SHA_BYTES));
        byte[] root = sha.digest();
        for (int i = 0; i < SHA_BYTES; i++) {
            if (image.get(total - SHA_BYTES + i) != root[i]) {
                throw new IllegalArgumentException("Archive SHA-256 root differs");
            }
        }
        return new View(image, count, payload, payloadBase);
    }

    private static int checkedTotal(int count, int payload) {
        long total = (long) HEADER_BYTES + (long) count * CELL_BYTES + payload + SHA_BYTES;
        if (count < 0 || count > MAX_ENTRIES || payload < 0 || total > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Archive capacity or counter is invalid");
        }
        return (int) total;
    }

    private static ByteBuffer boundedSlice(ByteBuffer image, int offset, int length) {
        ByteBuffer view = image.duplicate();
        view.position(offset);
        view.limit(offset + length);
        return view.slice().asReadOnlyBuffer();
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 provider is unavailable", ex);
        }
    }

    /** Immutable index snapshots for integrity-checked reads from a mutable mapping. */
    static final class PinnedView {
        private final View source;
        private final int[] offsets;
        private final int[] lengths;
        private final byte[] digests;
        private final byte[] root;

        private PinnedView(View source, int[] offsets, int[] lengths,
                byte[] digests, byte[] root) {
            this.source = source;
            this.offsets = offsets;
            this.lengths = lengths;
            this.digests = digests;
            this.root = root;
        }

        private static PinnedView capture(View source) {
            int count = source.count;
            int[] offsets = new int[count];
            int[] lengths = new int[count];
            byte[] digests = new byte[Math.multiplyExact(count, SHA_BYTES)];
            for (int id = 0; id < count; id++) {
                int cell = HEADER_BYTES + id * CELL_BYTES;
                offsets[id] = source.image.getInt(cell);
                lengths[id] = source.image.getInt(cell + Integer.BYTES);
                // inspect() previously validated geometry; still refuse a racy edit.
                if (offsets[id] < 0 || lengths[id] < 0
                        || offsets[id] > source.payload
                        || lengths[id] > source.payload - offsets[id]) {
                    throw new IllegalArgumentException("Mapped index changed during pinning");
                }
                for (int k = 0; k < SHA_BYTES; k++) {
                    digests[id * SHA_BYTES + k] =
                            source.image.get(cell + 2 * Integer.BYTES + k);
                }
            }
            byte[] root = new byte[SHA_BYTES];
            int trailer = source.image.limit() - SHA_BYTES;
            for (int i = 0; i < SHA_BYTES; i++) {
                root[i] = source.image.get(trailer + i);
            }
            PinnedView pinned = new PinnedView(source, offsets, lengths, digests, root);
            // This verifies the freshly captured root again. It cannot guarantee
            // against concurrent mutation/reversion or malicious SHA-256 collisions.
            pinned.verifyRoot();
            return pinned;
        }

        int count() { return offsets.length; }

        /**
         * Takes a privately owned, canonical, byte-identical snapshot of the pinned frame.
         * A mapped file may be changed by another descriptor while this copy is running:
         * validate the *captured bytes* against the pinned root, not the live mapping
         * before or after copying. A stable, self-consistent archive is returned, or the
         * entire attempt is rejected. This is an opt-in allocating operation and does
         * not change the established zero-copy API, JNI symbols, or archive format.
         *
         * <p>Cross-check the captured immutable index against the original pinned
         * primitive lanes. This also refuses a torn header/payload read even when the
         * mutable source happens to pass a separately timed root check. A caller cannot
         * mutate the returned View's private ByteBuffer backing.
         */
        View snapshotCanonicalCopy() {
            byte[] snapshotBytes = new byte[source.image.limit()];
            ByteBuffer reader = source.image.duplicate();
            reader.position(0);
            reader.get(snapshotBytes);

            int trailer = snapshotBytes.length - SHA_BYTES;
            MessageDigest sha = newSha256();
            sha.update(snapshotBytes, 0, trailer);
            if (!MessageDigest.isEqual(sha.digest(), root)) {
                throw new IllegalStateException("Pinned archive snapshot differs from original root");
            }
            for (int i = 0; i < SHA_BYTES; i++) {
                if (snapshotBytes[trailer + i] != root[i]) {
                    throw new IllegalStateException("Pinned archive snapshot trailer differs");
                }
            }

            // Inspect this private, read-only snapshot, never the live mapped source.
            View verified = inspect(ByteBuffer.wrap(snapshotBytes).asReadOnlyBuffer());
            if (verified.count != offsets.length) {
                throw new IllegalStateException("Pinned archive snapshot count differs");
            }
            for (int id = 0; id < offsets.length; id++) {
                int cell = HEADER_BYTES + id * CELL_BYTES;
                if (verified.image.getInt(cell) != offsets[id]
                        || verified.image.getInt(cell + Integer.BYTES) != lengths[id]) {
                    throw new IllegalStateException("Pinned archive snapshot index differs: " + id);
                }
                int digestBase = id * SHA_BYTES;
                for (int j = 0; j < SHA_BYTES; j++) {
                    if (verified.image.get(cell + 2 * Integer.BYTES + j)
                            != digests[digestBase + j]) {
                        throw new IllegalStateException("Pinned archive snapshot digest differs: " + id);
                    }
                }
            }
            verified.verifyCanonicalIds();
            return verified;
        }

        /** Detects changes to the full archived frame since the original pin. */
        void verifyRoot() {
            int trailer = source.image.limit() - SHA_BYTES;
            MessageDigest sha = newSha256();
            sha.update(boundedSlice(source.image, 0, trailer));
            byte[] current = sha.digest();
            if (!MessageDigest.isEqual(current, root)) {
                throw new IllegalStateException("Pinned archive content has changed");
            }
            for (int i = 0; i < SHA_BYTES; i++) {
                if (source.image.get(trailer + i) != root[i]) {
                    throw new IllegalStateException("Pinned archive trailer has changed");
                }
            }
        }

        /**
         * Returns an independent array only after verifying its snapshot payload.
         * Never exposes mutable mapped bytes or uses mutable mapped index geometry.
         * Unrelated changes to the image header require an explicit verifyRoot().
         */
        byte[] copyVerifiedAt(int id) {
            if (id < 0 || id >= offsets.length) {
                throw new IndexOutOfBoundsException("Pinned archive entry: " + id);
            }
            byte[] data = new byte[lengths[id]];
            boundedSlice(source.image, source.payloadBase + offsets[id], lengths[id])
                    .get(data);
            byte[] actual = newSha256().digest(data);
            int digestBase = id * SHA_BYTES;
            for (int i = 0; i < SHA_BYTES; i++) {
                if (actual[i] != digests[digestBase + i]) {
                    throw new IllegalStateException("Pinned archive entry has changed: " + id);
                }
            }
            return data;
        }
    }

    /** One immutable image's stable ordinal and original data; no class identity. */
    static final class View {
        private final ByteBuffer image;
        private final int count;
        private final int payload;
        private final int payloadBase;

        private View(ByteBuffer image, int count, int payload, int payloadBase) {
            this.image = image;
            this.count = count;
            this.payload = payload;
            this.payloadBase = payloadBase;
        }

        int count() { return count; }

        int payloadBytes() { return payload; }

        ByteBuffer readOnlyImage() { return image.asReadOnlyBuffer(); }

        ByteBuffer viewAt(int id) {
            checkId(id);
            int cell = HEADER_BYTES + id * CELL_BYTES;
            return boundedSlice(image, payloadBase + image.getInt(cell),
                    image.getInt(cell + Integer.BYTES));
        }

        byte[] copyAt(int id) {
            ByteBuffer value = viewAt(id);
            byte[] result = new byte[value.remaining()];
            value.get(result);
            return result;
        }

        /**
         * Verifies that each content ID is unique without rebuilding the original store.
         * Radix-sort primitive ordinal lanes by all 32 SHA-256 bytes, then compare exact
         * payload bytes in each equal-digest group. This is O(32*n + collision checks)
         * with bounded temporary int[] lanes, including for adversarial digest prefixes.
         * The root and each per-entry digest were verified during inspect(). No Class,
         * ClassLoader, String, native pointer or per-entry wrapper is retained.
         *
         * A mapped caller must still enforce immutable-file custody after admission.
         */
        void verifyCanonicalIds() {
            if (count < 2) return;
            int[] order = new int[count];
            int[] scratch = new int[count];
            for (int i = 0; i < count; i++) order[i] = i;
            int[] positions = new int[256];
            // Stable LSD radix ordering: SHA-256 bytes, not a short hash prefix.
            for (int digit = SHA_BYTES - 1; digit >= 0; digit--) {
                Arrays.fill(positions, 0);
                for (int ordinal : order) {
                    positions[image.get(digestOffset(ordinal) + digit) & 0xff]++;
                }
                int accumulated = 0;
                for (int i = 0; i < positions.length; i++) {
                    int frequency = positions[i];
                    positions[i] = accumulated;
                    accumulated += frequency;
                }
                for (int ordinal : order) {
                    int symbol = image.get(digestOffset(ordinal) + digit) & 0xff;
                    scratch[positions[symbol]++] = ordinal;
                }
                int[] prior = order;
                order = scratch;
                scratch = prior;
            }
            // SHA-256 is a candidate grouping key, never a proof of byte identity.
            for (int begin = 0; begin < count;) {
                int end = begin + 1;
                while (end < count && sameDigest(order[begin], order[end])) end++;
                for (int current = begin + 1; current < end; current++) {
                    for (int previous = begin; previous < current; previous++) {
                        if (samePayload(order[current], order[previous])) {
                            throw new IllegalArgumentException(
                                    "Archive contains duplicate canonical atom IDs: "
                                            + order[previous] + " and " + order[current]);
                        }
                    }
                }
                begin = end;
            }
        }

        private static int digestOffset(int ordinal) {
            return HEADER_BYTES + ordinal * CELL_BYTES + 2 * Integer.BYTES;
        }

        private boolean sameDigest(int left, int right) {
            int a = digestOffset(left);
            int b = digestOffset(right);
            for (int i = 0; i < SHA_BYTES; i += Long.BYTES) {
                if (image.getLong(a + i) != image.getLong(b + i)) return false;
            }
            return true;
        }

        private boolean samePayload(int left, int right) {
            int leftCell = HEADER_BYTES + left * CELL_BYTES;
            int rightCell = HEADER_BYTES + right * CELL_BYTES;
            int length = image.getInt(leftCell + Integer.BYTES);
            if (length != image.getInt(rightCell + Integer.BYTES)) return false;
            int a = payloadBase + image.getInt(leftCell);
            int b = payloadBase + image.getInt(rightCell);
            for (int i = 0; i < length; i++) {
                if (image.get(a + i) != image.get(b + i)) return false;
            }
            return true;
        }

        /** Rebuilds an independent owner, rejecting duplicate IDs or noncanonical entries. */
        M3ClassSpace restore() {
            M3ClassSpace owner = new M3ClassSpace(Math.max(1, payload));
            for (int id = 0; id < count; id++) {
                if (owner.intern(copyAt(id)) != id) {
                    throw new IllegalArgumentException("Archive duplicates a canonical atom");
                }
            }
            if (owner.retainedByteCount() != payload) {
                throw new IllegalArgumentException("Restored payload size differs");
            }
            return owner;
        }

        private void checkId(int id) {
            if (id < 0 || id >= count) {
                throw new IndexOutOfBoundsException("Archive entry: " + id);
            }
        }
    }
}
