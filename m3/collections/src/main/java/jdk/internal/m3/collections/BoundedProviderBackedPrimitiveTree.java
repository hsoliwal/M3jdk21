/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.Objects;
import java.util.function.LongConsumer;
import jdk.m3.collections.M3LazyPrimitiveAddressSpace;
import jdk.m3.collections.PrimitiveKind;

/**
 * Offset-paged provider tree with bounded materialized-payload residency.
 *
 * <p>Tree relationships, root/child page state and expansion bits remain owned by
 * {@link ProviderBackedPrimitiveTree} and are never evicted by the payload CLOCK.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/BoundedProviderBackedPrimitiveTree.java}
 * blob {@code 479eb3b5dbefc5fd126d2c675d96ed9cd7d4bd76} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
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
                M3LazyPrimitiveAddressSpace.DEFAULT_ROOT_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_DIRECTORY_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_LEAF_BITS,
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
        try {
            boolean loaded = tree.touch(nodeId);
            afterSuccessfulTouch(nodeId, hit, loaded);
            return loaded;
        } catch (ProviderLoadException failure) {
            if (!hit) loadFailures++;
            throw failure;
        }
    }

    public long getBits(int lane, long nodeId) {
        boolean hit = tree.isMaterialized(nodeId);
        touches++;
        try {
            long value = tree.getBits(lane, nodeId);
            afterSuccessfulTouch(nodeId, hit, !hit);
            return value;
        } catch (ProviderLoadException failure) {
            if (!hit) loadFailures++;
            throw failure;
        }
    }

    public void readRow(long nodeId, long[] target) {
        boolean hit = tree.isMaterialized(nodeId);
        touches++;
        try {
            tree.readRow(nodeId, target);
            afterSuccessfulTouch(nodeId, hit, !hit);
        } catch (ProviderLoadException failure) {
            if (!hit) loadFailures++;
            throw failure;
        }
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
        if (minimumLoaded < 0) throw new IllegalArgumentException("minimumLoaded < 0");
        if (maxPageLoads < 0) throw new IllegalArgumentException("maxPageLoads < 0");
        int pages = 0;
        while (loadedChildCount(parentId) < minimumLoaded
                && !LazyElementFlags.has(flags(parentId), LazyElementFlags.CHILDREN_COMPLETE)
                && pages < maxPageLoads) {
            loadNextChildPage(parentId);
            pages++;
        }
        return loadedChildCount(parentId);
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
        boolean materialized = tree.isMaterialized(parentId);
        Throwable primaryFailure = null;
        try {
            return tree.loadNextChildPage(parentId);
        } catch (RuntimeException | Error failure) {
            primaryFailure = failure;
            throw failure;
        } finally {
            // Child paging touches its parent through the delegate. Account that payload even
            // when the child callback fails; never hide the provider's primary exception.
            if (!materialized && tree.isMaterialized(parentId)) {
                try {
                    afterSuccessfulTouch(parentId, false, true);
                } catch (RuntimeException | Error admissionFailure) {
                    if (primaryFailure == null) throw admissionFailure;
                    primaryFailure.addSuppressed(admissionFailure);
                }
            } else if (materialized) {
                residency.reference(parentId);
            }
        }
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

    private void afterSuccessfulTouch(long nodeId, boolean hit, boolean loaded) {
        if (hit) {
            cacheHits++;
            residency.reference(nodeId);
            return;
        }

        if (!loaded && !tree.isMaterialized(nodeId)) return;
        loads++;
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
