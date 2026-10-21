// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/**
 * Packed coordinate into the hierarchical lexicon space.
 *
 * <p>Layout: 8-bit snapshot version, 20-bit namespace id, 36-bit entry id.
 * This supports 1,048,576 namespaces and 68,719,476,736 entries per namespace.</p>
 */
public record MIndexAtomRef(long bits)
        implements Comparable<MIndexAtomRef>, MIndexSymbolReference {
    public static final int VERSION_BITS = 8;
    public static final int NAMESPACE_BITS = 20;
    public static final int ENTRY_BITS = 36;
    public static final int MAX_VERSION = (1 << VERSION_BITS) - 1;
    public static final int MAX_NAMESPACE_ID = (1 << NAMESPACE_BITS) - 1;
    public static final long MAX_ENTRY_ID = (1L << ENTRY_BITS) - 1L;

    private static final long ENTRY_MASK = MAX_ENTRY_ID;
    private static final long NAMESPACE_MASK = (1L << NAMESPACE_BITS) - 1L;

    public static MIndexAtomRef of(int version, int namespaceId, long entryId) {
        if (version < 0 || version > MAX_VERSION) {
            throw new IllegalArgumentException("version out of range: " + version);
        }
        if (namespaceId < 0 || namespaceId > MAX_NAMESPACE_ID) {
            throw new IllegalArgumentException("namespaceId out of range: " + namespaceId);
        }
        if (entryId < 0 || entryId > MAX_ENTRY_ID) {
            throw new IllegalArgumentException("entryId out of range: " + entryId);
        }
        long packed = ((long) version << (NAMESPACE_BITS + ENTRY_BITS))
                | ((long) namespaceId << ENTRY_BITS)
                | entryId;
        return new MIndexAtomRef(packed);
    }

    public int version() {
        return (int) (bits >>> (NAMESPACE_BITS + ENTRY_BITS));
    }

    public int namespaceId() {
        return (int) ((bits >>> ENTRY_BITS) & NAMESPACE_MASK);
    }

    public long entryId() {
        return bits & ENTRY_MASK;
    }

    @Override
    public MIndexSymbolRef symbolRef() {
        long scope = ((long) version() << NAMESPACE_BITS) | Integer.toUnsignedLong(namespaceId());
        return new MIndexSymbolRef(MIndexReferenceDomain.ATOM, scope, entryId());
    }

    public boolean sameNamespace(MIndexAtomRef other) {
        return other != null && version() == other.version() && namespaceId() == other.namespaceId();
    }

    @Override
    public int compareTo(MIndexAtomRef other) {
        return Long.compareUnsigned(bits, other.bits);
    }

    @Override
    public String toString() {
        return "MIndexAtomRef[v=" + version() + ", ns=" + namespaceId() + ", entry=" + entryId() + "]";
    }
}
