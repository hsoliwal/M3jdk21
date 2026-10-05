package com.synexia.primitives;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Offset-paged provider tree with bounded materialized-payload residency.
 *
 * <p>Tree relationships, root/child page state and expansion bits remain owned by
 * {@link ProviderBackedPrimitiveTree} and are never evicted by the payload CLOCK.
 */
public final class BoundedProviderBackedPrimitiveTree {
    private final ProviderBackedPrimitiveTree tree;
    private final ProviderResidencyClock residency;

    private long touches;
    private long cacheHits;
    private long loads;
    private long loadFailures;
    private long policyEvictions;
    private long explicitEvictions;
    private long invalidations;
    private long admissionFailures;

    public BoundedProviderBackedPrimitiveTree(
            int maximumResident,
            PagedPrimitiveTreeProvider provider,
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

    public BoundedProviderBackedPrimitiveTree(
            int rootBits,
            int directoryBits,
            int leafBits,
            int maximumResident,
            PagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        tree = new ProviderBackedPrimitiveTree(
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
                touches,
                cacheHits,
                loads,
                loadFailures,
                0L,
                policyEvictions,
                explicitEvictions,
                invalidations,
                0L,
                admissionFailures);
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

    public int parentOf(long nodeId) { return tree.parentOf(nodeId); }
    public int firstLoadedChild(long parentId) { return tree.firstLoadedChild(parentId); }
    public int nextLoadedSibling(long nodeId) { return tree.nextLoadedSibling(nodeId); }
    public int previousLoadedSibling(long nodeId) { return tree.previousLoadedSibling(nodeId); }
    public int lastLoadedChild(long parentId) { return tree.lastLoadedChild(parentId); }
    public int loadedChildCount(long parentId) { return tree.loadedChildCount(parentId); }
    public int nextChildOffset(long parentId) { return tree.nextChildOffset(parentId); }

    public short rootFlags() { return tree.rootFlags(); }
    public long structureRevision() { return tree.structureRevision(); }
    public boolean isLoadedRoot(long nodeId) { return tree.isLoadedRoot(nodeId); }
    public int firstLoadedRoot() { return tree.firstLoadedRoot(); }
    public int lastLoadedRoot() { return tree.lastLoadedRoot(); }
    public int nextLoadedRoot(long rootId) { return tree.nextLoadedRoot(rootId); }
    public int previousLoadedRoot(long rootId) { return tree.previousLoadedRoot(rootId); }
    public int loadedRootCount() { return tree.loadedRootCount(); }
    public int nextRootOffset() { return tree.nextRootOffset(); }
    public PagedPrimitiveTreeProvider.ChildHint rootHint() { return tree.rootHint(); }

    public int loadNextRootPage() { return tree.loadNextRootPage(); }
    public int ensureLoadedRoots(int minimumLoaded, int maxPageLoads) {
        return tree.ensureLoadedRoots(minimumLoaded, maxPageLoads);
    }
    public int ensureLoadedChildren(long parentId, int minimumLoaded, int maxPageLoads) {
        boolean wasMaterialized = materializedForSettlement(parentId);
        int count;
        try {
            count = tree.ensureLoadedChildren(parentId, minimumLoaded, maxPageLoads);
        } catch (RuntimeException | Error failure) {
            reconcileFailedDemand(parentId, wasMaterialized, failure);
            throw failure;
        }
        reconcileNewPayload(parentId, wasMaterialized);
        return count;
    }

    public void forEachLoadedRoot(LongConsumer consumer) {
        tree.forEachLoadedRoot(consumer);
    }

    /**
     * Prefetches payload for an already-discovered root window through this bounded residency layer.
     *
     * @return newly materialized payload rows
     */
    public int prefetchLoadedRoots(int firstOrdinal, int count) {
        if (firstOrdinal < 0 || count < 0) throw new IllegalArgumentException("negative window");
        int root = tree.firstLoadedRoot();
        for (int skipped = 0; skipped < firstOrdinal && root >= 0; skipped++) {
            root = tree.nextLoadedRoot(root);
        }
        int loaded = 0;
        for (int accepted = 0; accepted < count && root >= 0; accepted++) {
            if (touch(root)) loaded++;
            root = tree.nextLoadedRoot(root);
        }
        return loaded;
    }

    public void invalidateRoots() {
        tree.invalidateRoots();
    }

    public boolean isExpanded(long nodeId) { return tree.isExpanded(nodeId); }
    public void setExpanded(long nodeId, boolean value) { tree.setExpanded(nodeId, value); }

    public ProviderBackedPrimitiveTree.VisibleCursor visibleLoadedCursor() {
        return tree.visibleLoadedCursor();
    }

    public int fillVisibleLoadedWindow(int skip, int limit, long[] target) {
        return tree.fillVisibleLoadedWindow(skip, limit, target);
    }

    /**
     * Materializes the visible loaded window while preserving the residency bound.
     */
    public int prefetchVisibleLoadedWindow(int skip, int limit) {
        if (skip < 0 || limit < 0) throw new IllegalArgumentException("negative window");
        ProviderBackedPrimitiveTree.VisibleCursor cursor = tree.visibleLoadedCursor();
        for (int skipped = 0; skipped < skip && cursor.hasNext(); skipped++) cursor.nextLong();
        int loaded = 0;
        for (int accepted = 0; accepted < limit && cursor.hasNext(); accepted++) {
            if (touch(cursor.nextLong())) loaded++;
        }
        return loaded;
    }

    public PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) {
        return tree.childHint(nodeId);
    }

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

    public void forEachLoadedChild(long parentId, LongConsumer consumer) {
        tree.forEachLoadedChild(parentId, consumer);
    }

    /**
     * Prefetches already-discovered child payload through bounded residency.
     *
     * @return newly materialized payload rows
     */
    public int prefetchLoadedChildren(long parentId, int firstOrdinal, int count) {
        if (firstOrdinal < 0 || count < 0) throw new IllegalArgumentException("negative window");
        int child = tree.firstLoadedChild(parentId);
        for (int skipped = 0; skipped < firstOrdinal && child >= 0; skipped++) {
            child = tree.nextLoadedSibling(child);
        }
        int loaded = 0;
        for (int accepted = 0; accepted < count && child >= 0; accepted++) {
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
            // Fail closed while preserving the bounded steady-state set even if the new row was
            // pre-pinned. Invalidation ignores PINNED for payload removal but preserves that flag.
            tree.invalidatePayload(nodeId);
            admissionFailures++;
            throw failure;
        }
    }
}
