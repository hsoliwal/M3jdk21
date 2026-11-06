// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

/**
 * Packed immutable-object identity.
 *
 * <p>High 16 bits are a pool/type id; low 48 bits are a local canonical-object id.
 * Id equality is therefore one primitive long comparison.</p>
 */
//FLLDCIM: RELOCATE - donor blob a6f11fc6e2c5 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public record M3ImmutableId(long packed) implements Comparable<M3ImmutableId> {
    public static final int MAX_TYPE_ID = 0xffff;
    public static final long MAX_LOCAL_ID = 0x0000_ffff_ffff_ffffL;
    public static final M3ImmutableId NONE = new M3ImmutableId(-1L);

    public static M3ImmutableId of(int typeId, long localId) {
        if (typeId <= 0 || typeId > MAX_TYPE_ID) {
            throw new IllegalArgumentException("type id must be in 1..65535");
        }
        if (localId < 0L || localId > MAX_LOCAL_ID) {
            throw new IllegalArgumentException("local id outside 48-bit range");
        }
        return new M3ImmutableId(((long) typeId << 48) | localId);
    }

    public int typeId() {
        return (int) ((packed >>> 48) & 0xffffL);
    }

    public long localId() {
        return packed & MAX_LOCAL_ID;
    }

    public boolean present() {
        return !equals(NONE);
    }

    public static boolean same(long leftPacked, long rightPacked) {
        return leftPacked == rightPacked;
    }

    @Override
    public int compareTo(M3ImmutableId other) {
        return Long.compareUnsigned(packed, other.packed);
    }
}
