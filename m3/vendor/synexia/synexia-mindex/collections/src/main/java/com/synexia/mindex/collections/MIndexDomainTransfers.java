// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import com.synexia.mindex.MIndexString;

/** Built-in exact transfer policies for canonical MIndex domains. */
public final class MIndexDomainTransfers {
    private static final MIndexDomainTransfer<MIndexString> STRINGS =
            new MIndexDomainTransfer<>() {
                @Override
                public int transferId(
                        MIndexSpace<MIndexString> source,
                        int sourceId,
                        MIndexSpace<MIndexString> target) {
                    MIndexSpace<MIndexString> strings =
                            MIndexSpaces.strings();
                    if (source != strings || target != strings) {
                        throw new IllegalArgumentException(
                                "MIndexString transfer requires MIndexSpaces.strings()");
                    }
                    strings.requireId(sourceId);
                    return sourceId;
                }

                @Override
                public boolean identityWhenSameSpace() {
                    return true;
                }
            };

    private static final MIndexDomainTransfer<Object> OBJECTS =
            new MIndexDomainTransfer<>() {
                @Override
                public int transferId(
                        MIndexSpace<Object> source,
                        int sourceId,
                        MIndexSpace<Object> target) {
                    if (!(source instanceof MIndexObjectSpace<?> sourceObjects)
                            || !(target instanceof MIndexObjectSpace<?> targetObjects)) {
                        throw new IllegalArgumentException(
                                "object transfer requires MIndexObjectSpace on both sides");
                    }
                    if (source == target) {
                        source.requireId(sourceId);
                        return sourceId;
                    }
                    @SuppressWarnings("unchecked")
                    MIndexObjectSpace<Object> typedSource =
                            (MIndexObjectSpace<Object>) sourceObjects;
                    @SuppressWarnings("unchecked")
                    MIndexObjectSpace<Object> typedTarget =
                            (MIndexObjectSpace<Object>) targetObjects;
                    return typedTarget.transferIdFrom(
                            typedSource, sourceId);
                }

                @Override
                public boolean identityWhenSameSpace() {
                    return true;
                }
            };

    private static final MIndexDomainTransfer<Object> SAME_SPACE =
            new MIndexDomainTransfer<>() {
                @Override
                public int transferId(
                        MIndexSpace<Object> source,
                        int sourceId,
                        MIndexSpace<Object> target) {
                    if (source != target) {
                        throw new IllegalArgumentException(
                                "same-space transfer requires identical MIndexSpace instances");
                    }
                    source.requireId(sourceId);
                    return sourceId;
                }

                @Override
                public boolean identityWhenSameSpace() {
                    return true;
                }
            };

    private MIndexDomainTransfers() {
    }

    /**
     * Global MIndexString content coordinates are already shared, so transfer
     * validates ownership and returns the same primitive ID.
     */
    public static MIndexDomainTransfer<MIndexString> strings() {
        return STRINGS;
    }

    /**
     * Exact value-semantic transfer between independent MIndexObject spaces.
     *
     * <p>The source indexed handle is admitted directly into the target. Hash
     * and signal equality are never accepted as proof; the target's ordinary
     * exact MIndexObject confirmation remains authoritative.
     */
    @SuppressWarnings("unchecked")
    public static <T> MIndexDomainTransfer<T> objects() {
        return (MIndexDomainTransfer<T>)
                (MIndexDomainTransfer<?>) OBJECTS;
    }

    /**
     * Identity transfer for a domain that is literally the same Java space.
     */
    @SuppressWarnings("unchecked")
    public static <T> MIndexDomainTransfer<T> sameSpace() {
        return (MIndexDomainTransfer<T>)
                (MIndexDomainTransfer<?>) SAME_SPACE;
    }

}
