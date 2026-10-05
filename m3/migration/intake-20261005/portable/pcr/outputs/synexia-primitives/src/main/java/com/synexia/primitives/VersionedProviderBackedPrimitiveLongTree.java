package com.synexia.primitives;

import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Version-consistent facade over the existing provider-backed long-id tree.
 *
 * <p>The existing tree remains the relationship/page-validation owner. A small adapter pins one
 * provider version token per cached parent branch in lazy primitive state and reuses it for every
 * cursor page until {@link #invalidateChildren(long)}.
 */
public final class VersionedProviderBackedPrimitiveLongTree {
    private final VersionPinningAdapter adapter;
    private final ProviderBackedPrimitiveLongTree tree;

    public VersionedProviderBackedPrimitiveLongTree(
            VersionedCursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        this(
                LazyPrimitiveAddressSpace.DEFAULT_ROOT_BITS,
                LazyPrimitiveAddressSpace.DEFAULT_DIRECTORY_BITS,
                LazyPrimitiveAddressSpace.DEFAULT_LEAF_BITS,
                provider,
                pageSize,
                laneKinds);
    }

    public VersionedProviderBackedPrimitiveLongTree(
            int rootBits,
            int directoryBits,
            int leafBits,
            VersionedCursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        adapter = new VersionPinningAdapter(
                rootBits,
                directoryBits,
                leafBits,
                Objects.requireNonNull(provider, "provider"));
        tree = new ProviderBackedPrimitiveLongTree(
                rootBits, directoryBits, leafBits,
                adapter, pageSize, laneKinds);
    }

    public int pageSize() { return tree.pageSize(); }
    public int laneCount() { return tree.laneCount(); }
    public long logicalCapacity() { return tree.logicalCapacity(); }
    public long materializedSize() { return tree.materializedSize(); }
    public short flags(long nodeId) { return tree.flags(nodeId); }
    public boolean isMaterialized(long nodeId) { return tree.isMaterialized(nodeId); }

    public long allocatedPayloadBytes() {
        return tree.allocatedPayloadBytes() + adapter.allocatedPayloadBytes();
    }

    public boolean touch(long nodeId) { return tree.touch(nodeId); }
    public long getBits(int lane, long nodeId) { return tree.getBits(lane, nodeId); }
    public void readRow(long nodeId, long[] target) { tree.readRow(nodeId, target); }
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

    /** Clears both cached relationships and the pinned branch version. */
    public void invalidateChildren(long parentId) {
        tree.invalidateChildren(parentId);
        adapter.invalidateVersion(parentId);
    }

    /** Invalidate and immediately load the first page from the provider's current version. */
    public int refreshChildren(long parentId) {
        invalidateChildren(parentId);
        return loadNextChildPage(parentId);
    }

    public boolean invalidatePayload(long nodeId) { return tree.invalidatePayload(nodeId); }
    public boolean evictPayload(long nodeId) { return tree.evictPayload(nodeId); }
    public void pin(long nodeId, boolean value) { tree.pin(nodeId, value); }

    public boolean hasPinnedChildVersion(long parentId) {
        return adapter.hasVersion(parentId);
    }

    public long pinnedChildVersionOrDefault(long parentId, long defaultValue) {
        return adapter.versionOrDefault(parentId, defaultValue);
    }

    public VersionedTreeProviderStats versionStats() {
        return adapter.stats();
    }

    public record VersionedTreeProviderStats(
            long versionLookups,
            long childPageLoads,
            long pinnedBranches) {
        public VersionedTreeProviderStats {
            if (versionLookups < 0L || childPageLoads < 0L || pinnedBranches < 0L) {
                throw new IllegalArgumentException("negative versioned-tree statistic");
            }
        }
    }

    private static final class VersionPinningAdapter implements CursorPagedPrimitiveTreeProvider {
        private final VersionedCursorPagedPrimitiveTreeProvider provider;
        private final LazyPrimitiveAddressSpace versions;
        private final LazyBitAddressSpace known;
        private long versionLookups;
        private long childPageLoads;

        private VersionPinningAdapter(
                int rootBits,
                int directoryBits,
                int leafBits,
                VersionedCursorPagedPrimitiveTreeProvider provider) {
            this.provider = provider;
            versions = new LazyPrimitiveAddressSpace(
                    PrimitiveKind.LONG, rootBits, directoryBits, leafBits);
            known = new LazyBitAddressSpace(rootBits, directoryBits, leafBits);
        }

        @Override
        public void load(long rowId, long[] target) throws Exception {
            provider.load(rowId, target);
        }

        @Override
        public void beforeLoad(long rowId, short currentFlags) {
            provider.beforeLoad(rowId, currentFlags);
        }

        @Override
        public void afterLoad(long rowId, short loadedFlags) {
            provider.afterLoad(rowId, loadedFlags);
        }

        @Override
        public void onLoadFailure(long rowId, Exception failure) {
            provider.onLoadFailure(rowId, failure);
        }

        @Override
        public PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) throws Exception {
            return provider.childHint(nodeId);
        }

        @Override
        public int loadChildren(
                long parentId,
                long cursor,
                int limit,
                long[] childIds,
                long[] nextCursorOut) throws Exception {
            long version = ensureVersion(parentId);
            childPageLoads++;
            return provider.loadChildren(
                    parentId, version, cursor, limit, childIds, nextCursorOut);
        }

        private long ensureVersion(long parentId) throws Exception {
            if (known.get(parentId)) return versions.getBits(parentId);
            long version = provider.childVersion(parentId);
            versions.setBits(parentId, version);
            known.set(parentId);
            versionLookups++;
            return version;
        }

        private boolean hasVersion(long parentId) {
            return known.get(parentId);
        }

        private long versionOrDefault(long parentId, long defaultValue) {
            return known.get(parentId) ? versions.getBits(parentId) : defaultValue;
        }

        private void invalidateVersion(long parentId) {
            versions.clearBits(parentId);
            known.clear(parentId);
        }

        private long allocatedPayloadBytes() {
            return versions.allocatedPayloadBytes() + known.allocatedPayloadBytes();
        }

        private VersionedTreeProviderStats stats() {
            return new VersionedTreeProviderStats(
                    versionLookups,
                    childPageLoads,
                    known.cardinality());
        }
    }
}
