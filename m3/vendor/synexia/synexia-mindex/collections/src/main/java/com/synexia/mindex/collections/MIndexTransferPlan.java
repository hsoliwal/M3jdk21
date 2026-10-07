// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Reusable primitive remap compiled from one exact domain-transfer policy.
 *
 * <p>The first visit to a source ID delegates to the semantic transfer policy
 * and validates the returned target coordinate. Later visits are one primitive
 * array lookup. This is particularly important for duplicate-heavy lists,
 * tables, graphs and repeated transfer of related canonical collections.
 *
 * <p>A plan is bound to exactly one source space and one target space. It is
 * intentionally mutable and thread-confined; callers that transfer in parallel
 * should use one plan per worker. Source-space growth is supported by growing
 * the primitive remap lane on demand.
 */
public final class MIndexTransferPlan<E>
        implements MIndexDomainTransfer<E> {
    private final MIndexSpace<E> source;
    private final MIndexSpace<E> target;
    private final MIndexDomainTransfer<E> delegate;
    private final boolean identity;
    private int[] targetIds;
    private int mappedCount;

    private MIndexTransferPlan(
            MIndexSpace<E> source,
            MIndexSpace<E> target,
            MIndexDomainTransfer<E> delegate) {
        this.source = Objects.requireNonNull(source, "source");
        this.target = Objects.requireNonNull(target, "target");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        identity = source == target
                && delegate.identityWhenSameSpace();
        targetIds = new int[0];
    }

    public static <E> MIndexTransferPlan<E> bind(
            MIndexSpace<E> source,
            MIndexSpace<E> target,
            MIndexDomainTransfer<E> delegate) {
        return new MIndexTransferPlan<>(source, target, delegate);
    }

    public MIndexSpace<E> source() {
        return source;
    }

    public MIndexSpace<E> target() {
        return target;
    }

    public int mappedCount() {
        return mappedCount;
    }

    public boolean identity() {
        return identity;
    }

    /**
     * Transfer one coordinate using the compiled remap.
     */
    public int transferId(int sourceId) {
        source.requireId(sourceId);
        if (identity) {
            target.requireId(sourceId);
            return sourceId;
        }
        ensureCapacity(sourceId + 1);
        int mapped = targetIds[sourceId];
        if (mapped >= 0) {
            return mapped;
        }

        int targetId = delegate.transferId(
                source, sourceId, target);
        target.requireId(targetId);
        targetIds[sourceId] = targetId;
        mappedCount++;
        return targetId;
    }

    /**
     * Transfer a complete source-ID lane using this plan.
     */
    public int[] transferIds(int[] sourceIds) {
        int[] ids = Objects.requireNonNull(
                sourceIds, "sourceIds");
        int[] result = new int[ids.length];
        for (int index = 0; index < ids.length; index++) {
            result[index] = transferId(ids[index]);
        }
        return result;
    }

    @Override
    public int transferId(
            MIndexSpace<E> actualSource,
            int sourceId,
            MIndexSpace<E> actualTarget) {
        requireBoundSpaces(actualSource, actualTarget);
        return transferId(sourceId);
    }

    @Override
    public int[] transferIds(
            MIndexSpace<E> actualSource,
            int[] sourceIds,
            MIndexSpace<E> actualTarget) {
        requireBoundSpaces(actualSource, actualTarget);
        return transferIds(sourceIds);
    }

    boolean boundTo(
            MIndexSpace<?> actualSource,
            MIndexSpace<?> actualTarget) {
        return source == actualSource && target == actualTarget;
    }

    private void requireBoundSpaces(
            MIndexSpace<?> actualSource,
            MIndexSpace<?> actualTarget) {
        if (!boundTo(actualSource, actualTarget)) {
            throw new IllegalArgumentException(
                    "MIndexTransferPlan is bound to different source/target spaces");
        }
    }

    private void ensureCapacity(int needed) {
        if (needed <= targetIds.length) {
            return;
        }
        int previous = targetIds.length;
        targetIds = Arrays.copyOf(
                targetIds,
                IdSupport.grown(previous, needed));
        Arrays.fill(targetIds, previous, targetIds.length, -1);
    }
}
