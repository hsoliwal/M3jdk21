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
 * Cursor-paged long-id provider tree with bounded materialized-payload residency.
 *
 * <p>Relationship and cursor state remain resident independently of payload residency.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/BoundedProviderBackedPrimitiveLongTree.java}
 * blob {@code 80fe4b963bc11fd596fa328cf4a2202f924626e3} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
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
                M3LazyPrimitiveAddressSpace.DEFAULT_ROOT_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_DIRECTORY_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_LEAF_BITS,
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

    public long parentOf(long nodeId) { return tree.parentOf(nodeId); }
    public long firstLoadedChild(long parentId) { return tree.firstLoadedChild(parentId); }
    public long nextLoadedSibling(long nodeId) { return tree.nextLoadedSibling(nodeId); }
    public long loadedChildCount(long parentId) { return tree.loadedChildCount(parentId); }
    public boolean childrenComplete(long parentId) { return tree.childrenComplete(parentId); }
    public long nextChildCursor(long parentId) { return tree.nextChildCursor(parentId); }
    public PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) { return tree.childHint(nodeId); }
    public int loadNextChildPage(long parentId) { return tree.loadNextChildPage(parentId); }
    public long ensureLoadedChildren(long parentId, long minimumLoaded, int maxPages) {
        return tree.ensureLoadedChildren(parentId, minimumLoaded, maxPages);
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
            tree.invalidatePayload(nodeId);
            admissionFailures++;
            throw failure;
        }
    }
}
