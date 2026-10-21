// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/**
 * Digest-indexed move candidates over one already-scanned M3File delta generation.
 * This is part of the M3JDK21 file representation, not a parallel filesystem.
 * Removed slots retain their prior SHA-256 digest; created slots own the newly
 * observed digest. Results are only rename/copy hypotheses: byte-identical
 * independent files and digest collisions cannot certify a filesystem move.
 * Consumers must preserve the original create/delete events and revalidate
 * required file/provider identity before interpreting a pair as a move.
 *
 * <p>Primitive physical-slot arrays and hash-bucket chains replace per-file
 * map entries. Exact 32-byte digest comparison follows the bucket probe.
 * Ambiguous many-to-one and one-to-many content joins never become a unique
 * move candidate. No file I/O, JNI invocation, or source payload copy occurs.
 */
public final class M3FileMoveCandidates {
    private static final int NONE = 0;
    private static final int SINGLE = 1;
    private static final int MULTIPLE = 2;
    private final int[] removedSlots;
    private final int[] createdSlots;
    private final int[] ambiguousCreatedSlots;
    private final int[] unmatchedCreatedSlots;

    private M3FileMoveCandidates(int[] removedSlots, int[] createdSlots,
            int[] ambiguousCreatedSlots, int[] unmatchedCreatedSlots) {
        this.removedSlots = removedSlots;
        this.createdSlots = createdSlots;
        this.ambiguousCreatedSlots = ambiguousCreatedSlots;
        this.unmatchedCreatedSlots = unmatchedCreatedSlots;
    }

    /**
     * Identify unique content-join candidates without rescanning the file tree.
     * Indexes are the stable physical slots from the supplied delta generation.
     * The result is deterministic regardless of original tracked-input order.
     * Expected time is O(slots + bucket probes), with worst-case quadratic
     * behavior if every removed digest maps to one bucket.
     */
    public static M3FileMoveCandidates index(M3FileDeltaLedger.Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        int removals = 0;
        int creations = 0;
        for (int slot = 0; slot < snapshot.slots(); slot++) {
            long bits = snapshot.stateAt(slot);
            if ((bits & M3FileDeltaLedger.REMOVED) != 0) removals++;
            if ((bits & M3FileDeltaLedger.CREATED) != 0) creations++;
        }
        int[] removed = new int[removals];
        int[] created = new int[creations];
        for (int slot = 0, r = 0, c = 0; slot < snapshot.slots(); slot++) {
            long bits = snapshot.stateAt(slot);
            if ((bits & M3FileDeltaLedger.REMOVED) != 0) removed[r++] = slot;
            if ((bits & M3FileDeltaLedger.CREATED) != 0) created[c++] = slot;
        }
        if (removals == 0 || creations == 0) {
            return new M3FileMoveCandidates(new int[0], new int[0], new int[0], created);
        }

        // An open-addressed bucket directory with primitive linked-row offsets.
        int buckets = 16;
        while (buckets < (long) removals * 2L) buckets <<= 1;
        int[] head = new int[buckets];
        Arrays.fill(head, -1);
        int[] next = new int[removals];
        long[] fingerprints = new long[removals];
        for (int r = 0; r < removals; r++) {
            int slot = removed[r];
            long fingerprint = fingerprint(snapshot, slot, snapshot.digestAt(slot));
            fingerprints[r] = fingerprint;
            int bucket = spread(fingerprint) & (buckets - 1);
            next[r] = head[bucket];
            head[bucket] = r;
        }

        int[] matchedOld = new int[creations];
        byte[] status = new byte[creations];
        int[] matchedNewCount = new int[removals];
        for (int c = 0; c < creations; c++) {
            int slot = created[c];
            byte[] digest = snapshot.digestAt(slot);
            long fingerprint = fingerprint(snapshot, slot, digest);
            int bucket = spread(fingerprint) & (buckets - 1);
            int uniqueOld = -1;
            for (int r = head[bucket]; r >= 0; r = next[r]) {
                int oldSlot = removed[r];
                if (fingerprints[r] == fingerprint
                        && snapshot.sizeAt(oldSlot) == snapshot.sizeAt(slot)
                        && snapshot.kindAt(oldSlot) == snapshot.kindAt(slot)
                        && Arrays.equals(snapshot.digestAt(oldSlot), digest)) {
                    if (uniqueOld != -1) {
                        status[c] = MULTIPLE;
                        break;
                    }
                    uniqueOld = r;
                }
            }
            if (status[c] != MULTIPLE && uniqueOld >= 0) {
                status[c] = SINGLE;
                matchedOld[c] = uniqueOld;
                matchedNewCount[uniqueOld]++;
            }
        }
        int accepted = 0;
        int ambiguous = 0;
        int unmatched = 0;
        for (int c = 0; c < creations; c++) {
            if (status[c] == NONE) unmatched++;
            else if (status[c] == SINGLE && matchedNewCount[matchedOld[c]] == 1) accepted++;
            else ambiguous++;
        }
        int[] from = new int[accepted];
        int[] to = new int[accepted];
        int[] uncertain = new int[ambiguous];
        int[] without = new int[unmatched];
        for (int c = 0, a = 0, u = 0, n = 0; c < creations; c++) {
            if (status[c] == NONE) without[n++] = created[c];
            else if (status[c] == SINGLE && matchedNewCount[matchedOld[c]] == 1) {
                from[a] = removed[matchedOld[c]];
                to[a++] = created[c];
            } else uncertain[u++] = created[c];
        }
        return new M3FileMoveCandidates(from, to, uncertain, without);
    }

    public int candidateCount() { return removedSlots.length; }
    public int ambiguityCount() { return ambiguousCreatedSlots.length; }
    /** A pair's offsets correspond across these two arrays. */
    public int[] removedCandidateSlots() { return removedSlots.clone(); }
    public int[] createdCandidateSlots() { return createdSlots.clone(); }
    /** Newly created slots sharing content with multiple removed/created paths. */
    public int[] ambiguousCreatedSlots() { return ambiguousCreatedSlots.clone(); }
    /** Newly created slots with no same-kind, same-size, same-digest removed path. */
    public int[] unmatchedCreatedSlots() { return unmatchedCreatedSlots.clone(); }

    private static long fingerprint(M3FileDeltaLedger.Snapshot snapshot, int slot, byte[] digest) {
        long value = 0xcbf29ce484222325L;
        for (byte element : digest) value = (value ^ (element & 0xffL)) * 0x100000001b3L;
        value ^= Long.rotateLeft(snapshot.sizeAt(slot), 19);
        value ^= (long) snapshot.kindAt(slot).ordinal() * 0x9e3779b97f4a7c15L;
        return value;
    }

    private static int spread(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return (int) value;
    }
}
