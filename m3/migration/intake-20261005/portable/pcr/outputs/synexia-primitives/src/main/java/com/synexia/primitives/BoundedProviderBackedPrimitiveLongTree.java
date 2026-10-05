package com.synexia.primitives;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Cursor-paged long-id provider tree with bounded materialized-payload residency.
 *
 * <p>Relationship and cursor state remain resident independently of payload residency.
 */
public final class BoundedProviderBackedPrimitiveLongTree {
    private final ProviderBackedPrimitiveLongTree tree;
    private final ProviderResidencyClock residency;

    private long touches;
    private long cacheHits;
    private long loads;
    private long loadFailures;
    private long policyEvictions;
    private long explicitEvictions;
    private long invalidations;
    private long admissionFailures;

    public BoundedProviderBackedPrimitiveLongTree(
            int maximumResident,
            CursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        this(
                LazyPrimitiveAddressSpace.DEFAULT_ROOT_BITS,
                LazyPrimitiveAddressSpace.DEFAULT_DIRECTORY_BITS,
                LazyPrimitiveAddressSpace.DEFAULT_LEAF_BITS,
                maximumResident,
                provider,
                pageSize,
                laneKinds);
    }

    public BoundedProviderBackedPrimitiveLongTree(
            int rootBits,
            int directoryBits,
            int leafBits,
            int maximumResident,
            CursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        tree = new ProviderBackedPrimitiveLongTree(
                rootBits,
                directoryBits,
                leafBits,
                Objects.requireNonNull(provider, "provider"),
                pageSize,
                laneKinds);
        residency = new ProviderResidencyClock(
                rootBits, directoryBits, leafBits, maximumResident);
    }

    public int pageSize() { return tree.pageSize(); }
    public int laneCount() { return tree.laneCount(); }
    public long logicalCapacity() { return tree.logicalCapacity(); }
    public long materializedSize() { return tree.materializedSize(); }
    public int maximumResident() { return residency.maximumResident(); }
    public int residentCount() { return residency.residentCount(); }
    public short flags(long nodeId) { return tree.flags(nodeId); }
    public boolean isMaterialized(long nodeId) { return tree.isMaterialized(nodeId); }
    public boolean isResident(long nodeId) { return residency.isResident(nodeId); }

    public long allocatedPayloadBytes() {
        return tree.allocatedPayloadBytes() + residency.administrativePrimitiveBytes();
    }

    public long administrativePrimitiveBytes() {
        return residency.administrativePrimitiveBytes();
    }

    public ProviderCollectionStats stats() {
        return new ProviderCollectionStats(
                touches, cacheHits, loads, loadFailures, 0L,
                policyEvictions, explicitEvictions, invalidations, 0L, admissionFailures);
    }

    public boolean touch(long nodeId) {
        boolean hit = tree.isMaterialized(nodeId);
        touches++;
        boolean loaded;
        try {
            loaded = tree.touch(nodeId);
        } catch (RuntimeException | Error failure) {
            if (failure instanceof ProviderLoadException && !hit) loadFailures++;
            reconcileFailedDemand(nodeId, hit, failure);
            throw failure;
        }
        afterSuccessfulTouch(nodeId, hit, loaded);
        return loaded;
    }

    public long getBits(int lane, long nodeId) {
        boolean hit = tree.isMaterialized(nodeId);
        touches++;
        long value;
        try {
            value = tree.getBits(lane, nodeId);
        } catch (RuntimeException | Error failure) {
            if (failure instanceof ProviderLoadException && !hit) loadFailures++;
            reconcileFailedDemand(nodeId, hit, failure);
            throw failure;
        }
        afterSuccessfulTouch(nodeId, hit, !hit);
        return value;
    }

    public void readRow(long nodeId, long[] target) {
        boolean hit = tree.isMaterialized(nodeId);
        touches++;
        try {
            tree.readRow(nodeId, target);
        } catch (RuntimeException | Error failure) {
            if (failure instanceof ProviderLoadException && !hit) loadFailures++;
            reconcileFailedDemand(nodeId, hit, failure);
            throw failure;
        }
        afterSuccessfulTouch(nodeId, hit, !hit);
    }

    public long parentOf(long nodeId) { return tree.parentOf(nodeId); }
    public long firstLoadedChild(long parentId) { return tree.firstLoadedChild(parentId); }
    public long nextLoadedSibling(long nodeId) { return tree.nextLoadedSibling(nodeId); }
    public long loadedChildCount(long parentId) { return tree.loadedChildCount(parentId); }
    public boolean childrenComplete(long parentId) { return tree.childrenComplete(parentId); }
    public long nextChildCursor(long parentId) { return tree.nextChildCursor(parentId); }
    public PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) { return tree.childHint(nodeId); }
    public int loadNextChildPage(long parentId) {
        boolean wasMaterialized = materializedForSettlement(parentId);
        int count;
        try {
            count = tree.loadNextChildPage(parentId);
        } catch (RuntimeException | Error failure) {
            reconcileFailedDemand(parentId, wasMaterialized, failure);
            throw failure;
        }
        reconcileNewPayload(parentId, wasMaterialized);
        return count;
    }
    public long ensureLoadedChildren(long parentId, long minimumLoaded, int maxPages) {
        boolean wasMaterialized = materializedForSettlement(parentId);
        long count;
        try {
            count = tree.ensureLoadedChildren(parentId, minimumLoaded, maxPages);
        } catch (RuntimeException | Error failure) {
            reconcileFailedDemand(parentId, wasMaterialized, failure);
            throw failure;
        }
        reconcileNewPayload(parentId, wasMaterialized);
        return count;
    }
    public long loadedChildAt(long parentId, long ordinal) {
        return tree.loadedChildAt(parentId, ordinal);
    }
    public void forEachLoadedChild(long parentId, LongConsumer consumer) {
        tree.forEachLoadedChild(parentId, consumer);
    }

    public int prefetchLoadedChildren(long parentId, long firstOrdinal, int count) {
        if (firstOrdinal < 0L || count < 0) throw new IllegalArgumentException("negative window");
        long child = tree.firstLoadedChild(parentId);
        long skipped = 0L;
        while (skipped < firstOrdinal && child >= 0L) {
            child = tree.nextLoadedSibling(child);
            skipped++;
        }
        int loaded = 0;
        for (int accepted = 0; accepted < count && child >= 0L; accepted++) {
            if (touch(child)) loaded++;
            child = tree.nextLoadedSibling(child);
        }
        return loaded;
    }

    public void invalidateChildren(long parentId) {
        tree.invalidateChildren(parentId);
    }

    public boolean invalidatePayload(long nodeId) {
        boolean resident = residency.isResident(nodeId);
        boolean removed = tree.invalidatePayload(nodeId);
        if (resident) residency.remove(nodeId);
        if (removed || resident) invalidations++;
        return removed;
    }

    public boolean evictPayload(long nodeId) {
        boolean removed = tree.evictPayload(nodeId);
        if (removed) {
            residency.remove(nodeId);
            explicitEvictions++;
        }
        return removed;
    }

    public void pin(long nodeId, boolean value) {
        tree.pin(nodeId, value);
    }

    /** Capture state without moving the delegated operation's validation boundary. */
    private boolean materializedForSettlement(long nodeId) {
        return nodeId >= 0L && nodeId < tree.logicalCapacity() && tree.isMaterialized(nodeId);
    }

    /** Settle only payload published by this demand; paging does not count as a facade touch. */
    private void reconcileNewPayload(long nodeId, boolean wasMaterialized) {
        if (wasMaterialized || !materializedForSettlement(nodeId) || residency.isResident(nodeId)) {
            return;
        }
        admitPayload(nodeId);
    }

    /** Keep the delegated failure primary if settling its published payload also fails. */
    private void reconcileFailedDemand(long nodeId, boolean wasMaterialized, Throwable failure) {
        try {
            reconcileNewPayload(nodeId, wasMaterialized);
        } catch (RuntimeException | Error admissionFailure) {
            if (admissionFailure != failure) failure.addSuppressed(admissionFailure);
        }
    }

    private void afterSuccessfulTouch(long nodeId, boolean hit, boolean loaded) {
        if (hit) {
            cacheHits++;
            residency.reference(nodeId);
            return;
        }
        if (!loaded && !tree.isMaterialized(nodeId)) return;

        loads++;
        admitPayload(nodeId);
    }

    private void admitPayload(long nodeId) {
        try {
            ProviderResidencyClock.Admission admission =
                    residency.admit(nodeId, tree::flags, tree::evictPayload);
            if (admission == ProviderResidencyClock.Admission.ADMITTED_WITH_EVICTION) {
                policyEvictions++;
            }
        } catch (IllegalStateException failure) {
            tree.invalidatePayload(nodeId);
            admissionFailures++;
            throw failure;
        }
    }
}
