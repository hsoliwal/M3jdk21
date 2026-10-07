// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

/**
 * Exact semantic transfer of one primitive coordinate between MIndex spaces.
 *
 * <p>The transfer owns the proof that a source ID and returned target ID denote
 * the same logical value. Collection transfer code therefore never assumes
 * that equal integer IDs have meaning across independent spaces.
 */
@FunctionalInterface
public interface MIndexDomainTransfer<E> {
    /**
     * Transfer one source coordinate into {@code target}.
     *
     * @return an ID owned by {@code target} that denotes the same logical value
     */
    int transferId(
            MIndexSpace<E> source,
            int sourceId,
            MIndexSpace<E> target);

    /**
     * Whether this policy is a proven identity mapping when source and target
     * are the same MIndexSpace instance.
     *
     * <p>The default is conservative. Identity-aware policies let compiled
     * plans avoid allocating a remap lane for globally shared spaces such as
     * MIndexString.
     */
    default boolean identityWhenSameSpace() {
        return false;
    }

    /**
     * Transfer a complete lane without modifying the caller-owned source.
     */
    default int[] transferIds(
            MIndexSpace<E> source,
            int[] sourceIds,
            MIndexSpace<E> target) {
        int[] ids = java.util.Objects.requireNonNull(sourceIds, "sourceIds");
        int[] result = new int[ids.length];
        for (int index = 0; index < ids.length; index++) {
            result[index] = transferId(source, ids[index], target);
        }
        return result;
    }
}
