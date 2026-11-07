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

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongConsumer;
import jdk.m3.collections.M3LazyPrimitiveAddressSpace;
import jdk.m3.collections.M3PrimitiveKind;

/**
 * Pageable provider-backed tree with primitive columnar payload and relationship lanes.
 *
 * <p>Child-page discovery links stable IDs only. Child payload remains lazy until touch/get.
 * Relationship links are 32-bit encoded IDs over a sparse logical address space.
 *
 * <p>Row, child-hint and child-page callbacks share the row owner's synchronous
 * callback guard. Metadata and cached payload reads are permitted during a
 * callback; another callback demand or owner mutation is rejected before changes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ProviderBackedPrimitiveTree.java}
 * blob {@code 8d62d6b2ad333a8cc06ca7577477c309f0a839eb} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3ProviderBackedPrimitiveTree {
    private static final int NO_OFFSET_COMPLETE = -1;

    private final M3ProviderBackedPrimitiveRows nodes;
    private final M3PagedPrimitiveTreeProvider provider;
    private final int pageSize;
    private final long[] childScratch;
    private final long[] validationScratch;

    private final M3LazyPrimitiveAddressSpace parent;
    private final M3LazyPrimitiveAddressSpace firstChild;
    private final M3LazyPrimitiveAddressSpace lastChild;
    private final M3LazyPrimitiveAddressSpace nextSibling;
    private final M3LazyPrimitiveAddressSpace previousSibling;
    private final M3LazyPrimitiveAddressSpace nextChildOffset;
    private final M3LazyPrimitiveAddressSpace loadedChildCount;
    private final M3LazyBitAddressSpace rootMembership;
    private final M3LazyBitAddressSpace expanded;

    private int firstRoot;
    private int lastRoot;
    private int nextRootOffset;
    private int loadedRootCount;
    private short rootFlags;
    private long structureRevision;

    public M3ProviderBackedPrimitiveTree(
            M3PagedPrimitiveTreeProvider provider,
            int pageSize,
            M3PrimitiveKind... laneKinds) {
        this(
                M3LazyPrimitiveAddressSpace.DEFAULT_ROOT_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_DIRECTORY_BITS,
                M3LazyPrimitiveAddressSpace.DEFAULT_LEAF_BITS,
                provider,
                pageSize,
                laneKinds);
    }

    public M3ProviderBackedPrimitiveTree(
            int rootBits,
            int directoryBits,
            int leafBits,
            M3PagedPrimitiveTreeProvider provider,
            int pageSize,
            M3PrimitiveKind... laneKinds) {
        this.provider = Objects.requireNonNull(provider, "provider");
        if (pageSize <= 0) throw new IllegalArgumentException("pageSize <= 0");
        this.pageSize = pageSize;
        nodes = new M3ProviderBackedPrimitiveRows(
                rootBits, directoryBits, leafBits, provider, laneKinds);
        if (nodes.logicalCapacity() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "tree logical capacity must fit encoded signed-int links");
        }
        childScratch = new long[pageSize];
        validationScratch = new long[pageSize];
        parent = ints(rootBits, directoryBits, leafBits);
        firstChild = ints(rootBits, directoryBits, leafBits);
        lastChild = ints(rootBits, directoryBits, leafBits);
        nextSibling = ints(rootBits, directoryBits, leafBits);
        previousSibling = ints(rootBits, directoryBits, leafBits);
        nextChildOffset = ints(rootBits, directoryBits, leafBits);
        loadedChildCount = ints(rootBits, directoryBits, leafBits);
        rootMembership = new M3LazyBitAddressSpace(rootBits, directoryBits, leafBits);
        expanded = new M3LazyBitAddressSpace(rootBits, directoryBits, leafBits);
    }

    public int pageSize() { return pageSize; }
    public int laneCount() { return nodes.laneCount(); }
    public long logicalCapacity() { return nodes.logicalCapacity(); }
    public long materializedSize() { return nodes.materializedSize(); }
    public short flags(long nodeId) { return nodes.flags(nodeId); }
    public boolean isMaterialized(long nodeId) { return nodes.isMaterialized(nodeId); }

    public long allocatedPayloadBytes() {
        return nodes.allocatedPayloadBytes()
                + parent.allocatedPayloadBytes()
                + firstChild.allocatedPayloadBytes()
                + lastChild.allocatedPayloadBytes()
                + nextSibling.allocatedPayloadBytes()
                + previousSibling.allocatedPayloadBytes()
                + nextChildOffset.allocatedPayloadBytes()
                + loadedChildCount.allocatedPayloadBytes()
                + rootMembership.allocatedPayloadBytes()
                + expanded.allocatedPayloadBytes();
    }

    public boolean touch(long nodeId) {
        return nodes.touch(nodeId);
    }

    public long getBits(int lane, long nodeId) {
        return nodes.getBits(lane, nodeId);
    }

    public void readRow(long nodeId, long[] target) {
        nodes.readRow(nodeId, target);
    }

    public int parentOf(long nodeId) {
        return decodeLink(parent.getBits(nodeId));
    }

    public int firstLoadedChild(long parentId) {
        return decodeLink(firstChild.getBits(parentId));
    }

    public int nextLoadedSibling(long nodeId) {
        return decodeLink(nextSibling.getBits(nodeId));
    }

    public int previousLoadedSibling(long nodeId) {
        return decodeLink(previousSibling.getBits(nodeId));
    }

    public int lastLoadedChild(long parentId) {
        return decodeLink(lastChild.getBits(parentId));
    }

    public int loadedChildCount(long parentId) {
        return (int) loadedChildCount.getBits(parentId);
    }

    public int nextChildOffset(long parentId) {
        short state = flags(parentId);
        if (M3LazyElementFlags.has(state, M3LazyElementFlags.CHILDREN_COMPLETE)) return -1;
        return (int) nextChildOffset.getBits(parentId);
    }

    public short rootFlags() {
        return rootFlags;
    }

    public long structureRevision() {
        return structureRevision;
    }

    public boolean isLoadedRoot(long nodeId) {
        checkLinkableId(nodeId);
        return rootMembership.get(nodeId);
    }

    public int firstLoadedRoot() {
        return decodeLink(firstRoot);
    }

    public int lastLoadedRoot() {
        return decodeLink(lastRoot);
    }

    public int nextLoadedRoot(long rootId) {
        return isLoadedRoot(rootId) ? nextLoadedSibling(rootId) : -1;
    }

    public int previousLoadedRoot(long rootId) {
        return isLoadedRoot(rootId) ? previousLoadedSibling(rootId) : -1;
    }

    public int loadedRootCount() {
        return loadedRootCount;
    }

    public int nextRootOffset() {
        return M3LazyElementFlags.has(rootFlags, M3LazyElementFlags.CHILDREN_COMPLETE)
                ? -1
                : nextRootOffset;
    }

    public M3PagedPrimitiveTreeProvider.ChildHint rootHint() {
        if (M3LazyElementFlags.has(rootFlags, M3LazyElementFlags.CHILDREN_KNOWN)) {
            return M3LazyElementFlags.has(rootFlags, M3LazyElementFlags.HAS_CHILDREN)
                    ? M3PagedPrimitiveTreeProvider.ChildHint.SOME
                    : M3PagedPrimitiveTreeProvider.ChildHint.NONE;
        }
        nodes.beginCallback();
        try {
            M3PagedPrimitiveTreeProvider.ChildHint hint =
                    Objects.requireNonNull(provider.rootHint(), "rootHint");
            switch (hint) {
                case NONE -> {
                    rootFlags = M3LazyElementFlags.add(
                            rootFlags,
                            (short) (M3LazyElementFlags.CHILDREN_KNOWN
                                    | M3LazyElementFlags.CHILDREN_COMPLETE));
                    rootFlags = M3LazyElementFlags.remove(
                            rootFlags,
                            (short) (M3LazyElementFlags.HAS_CHILDREN
                                    | M3LazyElementFlags.CHILDREN_PARTIAL));
                }
                case SOME -> rootFlags = M3LazyElementFlags.add(
                        rootFlags,
                        (short) (M3LazyElementFlags.CHILDREN_KNOWN
                                | M3LazyElementFlags.HAS_CHILDREN));
                case UNKNOWN -> {
                    // Unknown is cached only by the provider itself; leave the tree state open.
                }
            }
            return hint;
        } catch (Exception failure) {
            throw new M3ProviderLoadException(M3PagedPrimitiveTreeProvider.ROOT_PARENT_ID, failure);
        } finally {
            nodes.endCallback();
        }
    }

    /** Loads the next root page. Root payload remains lazy. */
    public int loadNextRootPage() {
        if (M3LazyElementFlags.has(rootFlags, M3LazyElementFlags.CHILDREN_COMPLETE)) return 0;
        nodes.requireCallbackIdle();
        if (M3LazyElementFlags.has(rootFlags, M3LazyElementFlags.CHILDREN_LOADING)) {
            throw new IllegalStateException("REENTRANT_ROOT_PAGE_LOAD");
        }

        M3PagedPrimitiveTreeProvider.ChildHint hint =
                M3LazyElementFlags.has(rootFlags, M3LazyElementFlags.CHILDREN_KNOWN)
                        ? (M3LazyElementFlags.has(rootFlags, M3LazyElementFlags.HAS_CHILDREN)
                                ? M3PagedPrimitiveTreeProvider.ChildHint.SOME
                                : M3PagedPrimitiveTreeProvider.ChildHint.NONE)
                        : rootHint();
        if (hint == M3PagedPrimitiveTreeProvider.ChildHint.NONE) return 0;

        int offset = nextRootOffset;
        nodes.beginCallback();
        try {
            rootFlags = M3LazyElementFlags.add(rootFlags, M3LazyElementFlags.CHILDREN_LOADING);
            Arrays.fill(childScratch, 0L);
            long result = provider.loadRoots(offset, pageSize, childScratch);
            int count = M3PagedPrimitiveTreeProvider.pageCount(result);
            int nextOffset = M3PagedPrimitiveTreeProvider.nextOffset(result);
            validatePageResult(offset, count, nextOffset);
            validateRootPage(count);

            if (hint == M3PagedPrimitiveTreeProvider.ChildHint.SOME
                    && loadedRootCount == 0
                    && count == 0
                    && nextOffset == NO_OFFSET_COMPLETE) {
                throw new IllegalArgumentException(
                        "provider rootHint SOME contradicted by empty complete forest");
            }

            for (int index = 0; index < count; index++) {
                linkValidatedRoot(childScratch[index]);
            }

            loadedRootCount = Math.addExact(loadedRootCount, count);
            nextRootOffset = nextOffset;
            rootFlags = M3LazyElementFlags.remove(
                    rootFlags,
                    (short) (M3LazyElementFlags.CHILDREN_LOADING
                            | M3LazyElementFlags.CHILDREN_PARTIAL
                            | M3LazyElementFlags.CHILDREN_COMPLETE));
            rootFlags = M3LazyElementFlags.add(rootFlags, M3LazyElementFlags.CHILDREN_KNOWN);
            if (loadedRootCount > 0 || hint == M3PagedPrimitiveTreeProvider.ChildHint.SOME) {
                rootFlags = M3LazyElementFlags.add(rootFlags, M3LazyElementFlags.HAS_CHILDREN);
            } else {
                rootFlags = M3LazyElementFlags.remove(rootFlags, M3LazyElementFlags.HAS_CHILDREN);
            }
            rootFlags = M3LazyElementFlags.add(
                    rootFlags,
                    nextOffset == NO_OFFSET_COMPLETE
                            ? M3LazyElementFlags.CHILDREN_COMPLETE
                            : M3LazyElementFlags.CHILDREN_PARTIAL);
            if (count != 0) structureRevision++;
            return count;
        } catch (Exception failure) {
            rootFlags = M3LazyElementFlags.remove(rootFlags, M3LazyElementFlags.CHILDREN_LOADING);
            throw failure instanceof M3ProviderLoadException providerFailure
                    ? providerFailure
                    : new M3ProviderLoadException(M3PagedPrimitiveTreeProvider.ROOT_PARENT_ID, failure);
        } catch (Error failure) {
            rootFlags = M3LazyElementFlags.remove(rootFlags, M3LazyElementFlags.CHILDREN_LOADING);
            throw failure;
        } finally {
            Arrays.fill(childScratch, 0L);
            Arrays.fill(validationScratch, 0L);
            nodes.endCallback();
        }
    }

    public int ensureLoadedRoots(int minimumLoaded, int maxPageLoads) {
        if (minimumLoaded < 0) throw new IllegalArgumentException("minimumLoaded < 0");
        if (maxPageLoads < 0) throw new IllegalArgumentException("maxPageLoads < 0");
        int pages = 0;
        while (loadedRootCount < minimumLoaded
                && !M3LazyElementFlags.has(rootFlags, M3LazyElementFlags.CHILDREN_COMPLETE)
                && pages < maxPageLoads) {
            loadNextRootPage();
            pages++;
        }
        return loadedRootCount;
    }

    public int ensureLoadedChildren(long parentId, int minimumLoaded, int maxPageLoads) {
        if (minimumLoaded < 0) throw new IllegalArgumentException("minimumLoaded < 0");
        if (maxPageLoads < 0) throw new IllegalArgumentException("maxPageLoads < 0");
        int pages = 0;
        while (loadedChildCount(parentId) < minimumLoaded
                && !M3LazyElementFlags.has(flags(parentId), M3LazyElementFlags.CHILDREN_COMPLETE)
                && pages < maxPageLoads) {
            loadNextChildPage(parentId);
            pages++;
        }
        return loadedChildCount(parentId);
    }

    public void forEachLoadedRoot(LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int root = firstLoadedRoot();
        while (root >= 0) {
            consumer.accept(root);
            root = nextLoadedRoot(root);
        }
    }

    /**
     * Materializes an already-loaded root window without changing topology or emitting touch events.
     *
     * @return number of newly materialized root payload rows
     */
    public int prefetchLoadedRoots(int firstOrdinal, int count) {
        if (firstOrdinal < 0 || count < 0
                || firstOrdinal > loadedRootCount
                || count > loadedRootCount - firstOrdinal) {
            throw new IndexOutOfBoundsException(firstOrdinal + "+" + count);
        }
        nodes.requireCallbackIdle();
        int root = firstLoadedRoot();
        for (int skipped = 0; skipped < firstOrdinal; skipped++) {
            root = nextLoadedRoot(root);
        }

        int total = 0;
        int remaining = count;
        while (remaining > 0) {
            int batch = 0;
            while (batch < childScratch.length && batch < remaining && root >= 0) {
                childScratch[batch++] = root;
                root = nextLoadedRoot(root);
            }
            if (batch == 0) {
                throw new IllegalStateException("loaded root chain shorter than loadedRootCount");
            }
            if (batch == 0) {
                throw new IllegalStateException("loaded child chain shorter than loadedChildCount");
            }
            total += nodes.prefetchMaterialize(childScratch, 0, batch);
            Arrays.fill(childScratch, 0, batch, 0L);
            remaining -= batch;
        }
        return total;
    }

    /** Drops cached root relationship paging without evicting root/descendant payloads. */
    public void invalidateRoots() {
        nodes.requireCallbackIdle();
        int root = firstLoadedRoot();
        boolean changed = root >= 0 || rootFlags != 0 || nextRootOffset != 0;
        while (root >= 0) {
            int next = nextLoadedSibling(root);
            nextSibling.clearBits(root);
            previousSibling.clearBits(root);
            rootMembership.clear(root);
            root = next;
        }
        firstRoot = 0;
        lastRoot = 0;
        nextRootOffset = 0;
        loadedRootCount = 0;
        rootFlags = 0;
        if (changed) structureRevision++;
    }

    public boolean isExpanded(long nodeId) {
        checkLinkableId(nodeId);
        return expanded.get(nodeId);
    }

    public void setExpanded(long nodeId, boolean value) {
        nodes.requireCallbackIdle();
        checkLinkableId(nodeId);
        boolean changed = value ? expanded.set(nodeId) : expanded.clear(nodeId);
        if (changed) structureRevision++;
    }

    /** Loaded-only pre-order cursor. It never calls the provider. */
    public VisibleCursor visibleLoadedCursor() {
        nodes.requireCallbackIdle();
        return new VisibleCursor(this);
    }

    /**
     * Writes up to limit visible loaded node ids after skipping skip visible nodes.
     * Provider callbacks are never invoked.
     */
    public int fillVisibleLoadedWindow(
            int skip,
            int limit,
            long[] target) {
        if (skip < 0) throw new IllegalArgumentException("skip < 0");
        if (limit < 0) throw new IllegalArgumentException("limit < 0");
        Objects.requireNonNull(target, "target");
        if (target.length < limit) throw new IllegalArgumentException("target shorter than limit");

        VisibleCursor cursor = visibleLoadedCursor();
        int skipped = 0;
        while (skipped < skip && cursor.hasNext()) {
            cursor.nextLong();
            skipped++;
        }
        int written = 0;
        while (written < limit && cursor.hasNext()) {
            target[written++] = cursor.nextLong();
        }
        return written;
    }

    /**
     * Materializes a loaded visible viewport without topology callbacks and without touch events.
     *
     * @return number of newly materialized visible node payload rows
     */
    public int prefetchVisibleLoadedWindow(int skip, int limit) {
        if (skip < 0) throw new IllegalArgumentException("skip < 0");
        if (limit < 0) throw new IllegalArgumentException("limit < 0");
        nodes.requireCallbackIdle();

        VisibleCursor cursor = visibleLoadedCursor();
        for (int skipped = 0; skipped < skip && cursor.hasNext(); skipped++) {
            cursor.nextLong();
        }

        int total = 0;
        int remaining = limit;
        while (remaining > 0 && cursor.hasNext()) {
            int batch = 0;
            while (batch < childScratch.length && batch < remaining && cursor.hasNext()) {
                childScratch[batch++] = cursor.nextLong();
            }
            total += nodes.prefetchMaterialize(childScratch, 0, batch);
            Arrays.fill(childScratch, 0, batch, 0L);
            remaining -= batch;
        }
        return total;
    }

    public M3PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) {
        short state = flags(nodeId);
        if (M3LazyElementFlags.has(state, M3LazyElementFlags.CHILDREN_KNOWN)) {
            return M3LazyElementFlags.has(state, M3LazyElementFlags.HAS_CHILDREN)
                    ? M3PagedPrimitiveTreeProvider.ChildHint.SOME
                    : M3PagedPrimitiveTreeProvider.ChildHint.NONE;
        }
        nodes.beginCallback();
        try {
            M3PagedPrimitiveTreeProvider.ChildHint hint =
                    Objects.requireNonNull(provider.childHint(nodeId), "childHint");
            switch (hint) {
                case NONE -> {
                    nodes.addFlags(nodeId,
                            (short) (M3LazyElementFlags.DISCOVERED
                                    | M3LazyElementFlags.CHILDREN_KNOWN
                                    | M3LazyElementFlags.CHILDREN_COMPLETE));
                    nodes.clearFlags(nodeId,
                            (short) (M3LazyElementFlags.HAS_CHILDREN
                                    | M3LazyElementFlags.CHILDREN_PARTIAL));
                }
                case SOME -> nodes.addFlags(nodeId,
                        (short) (M3LazyElementFlags.DISCOVERED
                                | M3LazyElementFlags.CHILDREN_KNOWN
                                | M3LazyElementFlags.HAS_CHILDREN));
                case UNKNOWN -> nodes.addFlags(nodeId, M3LazyElementFlags.DISCOVERED);
            }
            return hint;
        } catch (Exception failure) {
            throw new M3ProviderLoadException(nodeId, failure);
        } finally {
            nodes.endCallback();
        }
    }

    /**
     * Loads the next direct-child page for one parent.
     *
     * @return number of newly linked child IDs
     */
    public int loadNextChildPage(long parentId) {
        // A resident completed branch is a read and must not acquire callback scratch.
        if (nodes.isMaterialized(parentId)
                && M3LazyElementFlags.has(flags(parentId), M3LazyElementFlags.CHILDREN_COMPLETE)) {
            return 0;
        }
        nodes.requireCallbackIdle();
        nodes.touch(parentId);
        short state = flags(parentId);
        if (M3LazyElementFlags.has(state, M3LazyElementFlags.CHILDREN_COMPLETE)) return 0;
        if (M3LazyElementFlags.has(state, M3LazyElementFlags.CHILDREN_LOADING)) {
            throw new IllegalStateException("REENTRANT_CHILD_PAGE_LOAD:" + parentId);
        }

        M3PagedPrimitiveTreeProvider.ChildHint hint =
                M3LazyElementFlags.has(state, M3LazyElementFlags.CHILDREN_KNOWN)
                        ? (M3LazyElementFlags.has(state, M3LazyElementFlags.HAS_CHILDREN)
                                ? M3PagedPrimitiveTreeProvider.ChildHint.SOME
                                : M3PagedPrimitiveTreeProvider.ChildHint.NONE)
                        : childHint(parentId);
        if (hint == M3PagedPrimitiveTreeProvider.ChildHint.NONE) return 0;

        int offset = (int) nextChildOffset.getBits(parentId);
        nodes.beginCallback();
        try {
            nodes.addFlags(parentId, M3LazyElementFlags.CHILDREN_LOADING);
            Arrays.fill(childScratch, 0L);
            long result = provider.loadChildren(parentId, offset, pageSize, childScratch);
            int count = M3PagedPrimitiveTreeProvider.pageCount(result);
            int nextOffset = M3PagedPrimitiveTreeProvider.nextOffset(result);
            validatePageResult(offset, count, nextOffset);
            validateChildPage(parentId, count);

            int existingTotal = (int) loadedChildCount.getBits(parentId);
            if (hint == M3PagedPrimitiveTreeProvider.ChildHint.SOME
                    && existingTotal == 0
                    && count == 0
                    && nextOffset == NO_OFFSET_COMPLETE) {
                throw new IllegalArgumentException(
                        "provider childHint SOME contradicted by empty complete branch");
            }

            for (int index = 0; index < count; index++) {
                linkValidatedChild(parentId, childScratch[index]);
            }

            int total = Math.addExact(existingTotal, count);
            loadedChildCount.setBits(parentId, total);
            nextChildOffset.setBits(parentId, nextOffset);

            short nextState = flags(parentId);
            nextState = M3LazyElementFlags.remove(nextState,
                    (short) (M3LazyElementFlags.CHILDREN_LOADING
                            | M3LazyElementFlags.CHILDREN_PARTIAL
                            | M3LazyElementFlags.CHILDREN_COMPLETE));
            nextState = M3LazyElementFlags.add(nextState,
                    (short) (M3LazyElementFlags.DISCOVERED
                            | M3LazyElementFlags.CHILDREN_KNOWN));
            if (total > 0 || hint == M3PagedPrimitiveTreeProvider.ChildHint.SOME) {
                nextState = M3LazyElementFlags.add(nextState, M3LazyElementFlags.HAS_CHILDREN);
            } else {
                nextState = M3LazyElementFlags.remove(nextState, M3LazyElementFlags.HAS_CHILDREN);
            }
            nextState = M3LazyElementFlags.add(
                    nextState,
                    nextOffset == NO_OFFSET_COMPLETE
                            ? M3LazyElementFlags.CHILDREN_COMPLETE
                            : M3LazyElementFlags.CHILDREN_PARTIAL);
            nodes.replaceFlags(parentId, nextState);
            if (count != 0) structureRevision++;
            return count;
        } catch (Exception failure) {
            clearLoadingAfterFailure(parentId, failure);
            throw failure instanceof M3ProviderLoadException providerFailure
                    ? providerFailure
                    : new M3ProviderLoadException(parentId, failure);
        } catch (Error failure) {
            clearLoadingAfterFailure(parentId, failure);
            throw failure;
        } finally {
            Arrays.fill(childScratch, 0L);
            Arrays.fill(validationScratch, 0L);
            nodes.endCallback();
        }
    }

    public void forEachLoadedChild(long parentId, LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        int child = firstLoadedChild(parentId);
        while (child >= 0) {
            consumer.accept(child);
            child = nextLoadedSibling(child);
        }
    }

    /**
     * Materializes an already-loaded direct-child window without invoking topology paging.
     *
     * @return number of newly materialized child payload rows
     */
    public int prefetchLoadedChildren(long parentId, int firstOrdinal, int count) {
        int loaded = loadedChildCount(parentId);
        if (firstOrdinal < 0 || count < 0
                || firstOrdinal > loaded
                || count > loaded - firstOrdinal) {
            throw new IndexOutOfBoundsException(firstOrdinal + "+" + count);
        }
        nodes.requireCallbackIdle();
        int child = firstLoadedChild(parentId);
        for (int skipped = 0; skipped < firstOrdinal; skipped++) {
            child = nextLoadedSibling(child);
        }

        int total = 0;
        int remaining = count;
        while (remaining > 0) {
            int batch = 0;
            while (batch < childScratch.length && batch < remaining && child >= 0) {
                childScratch[batch++] = child;
                child = nextLoadedSibling(child);
            }
            total += nodes.prefetchMaterialize(childScratch, 0, batch);
            Arrays.fill(childScratch, 0, batch, 0L);
            remaining -= batch;
        }
        return total;
    }

    /**
     * Drops the cached direct-child relationship page state without evicting child payloads.
     */
    public void invalidateChildren(long parentId) {
        nodes.requireCallbackIdle();
        int child = firstLoadedChild(parentId);
        while (child >= 0) {
            int next = nextLoadedSibling(child);
            parent.clearBits(child);
            nextSibling.clearBits(child);
            previousSibling.clearBits(child);
            child = next;
        }
        firstChild.clearBits(parentId);
        lastChild.clearBits(parentId);
        nextChildOffset.clearBits(parentId);
        loadedChildCount.clearBits(parentId);
        nodes.clearFlags(parentId, M3LazyElementFlags.CHILD_STATE_MASK);
        structureRevision++;
    }

    public boolean invalidatePayload(long nodeId) {
        return nodes.invalidate(nodeId);
    }

    /** Evicts only materialized payload; relationship/paging state is retained. */
    public boolean evictPayload(long nodeId) {
        return nodes.evict(nodeId);
    }

    public void pin(long nodeId, boolean value) {
        nodes.pin(nodeId, value);
    }

    private void clearLoadingAfterFailure(long parentId, Throwable failure) {
        try {
            nodes.clearFlags(parentId, M3LazyElementFlags.CHILDREN_LOADING);
        } catch (RuntimeException | Error cleanupFailure) {
            if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
        }
    }

    private void validateChildPage(long parentId, int count) {
        checkLinkableId(parentId);
        System.arraycopy(childScratch, 0, validationScratch, 0, count);
        Arrays.sort(validationScratch, 0, count);
        for (int index = 0; index < count; index++) {
            long childId = childScratch[index];
            checkLinkableId(childId);
            if (childId == parentId) {
                throw new IllegalArgumentException("node cannot be its own child: " + childId);
            }
            if (index > 0 && validationScratch[index - 1] == validationScratch[index]) {
                throw new IllegalArgumentException(
                        "duplicate child id in provider page: " + validationScratch[index]);
            }
            int existingParent = (int) parent.getBits(childId);
            if (existingParent != 0) {
                throw new IllegalArgumentException(
                        existingParent == encodeLink(parentId)
                                ? "child already linked to parent: " + childId
                                : "child already belongs to another parent: " + childId);
            }
            if (rootMembership.get(childId)) {
                throw new IllegalArgumentException("child is already linked as a root: " + childId);
            }
            if (nextSibling.getBits(childId) != 0L || previousSibling.getBits(childId) != 0L) {
                throw new IllegalArgumentException("child already linked as sibling: " + childId);
            }
            for (int ancestor = encodeLink(parentId); ancestor != 0;) {
                int ancestorId = ancestor - 1;
                if (ancestorId == childId) {
                    throw new IllegalArgumentException(
                            "provider introduced tree cycle at " + childId);
                }
                ancestor = (int) parent.getBits(ancestorId);
            }
        }
    }

    private void linkValidatedChild(long parentId, long childId) {
        int encodedParent = encodeLink(parentId);
        int encodedChild = encodeLink(childId);
        int last = (int) lastChild.getBits(parentId);
        if (last == 0) {
            firstChild.setBits(parentId, encodedChild);
        } else {
            nextSibling.setBits(last - 1L, encodedChild);
            previousSibling.setBits(childId, last);
        }
        lastChild.setBits(parentId, encodedChild);
        parent.setBits(childId, encodedParent);
        nodes.addFlags(childId, M3LazyElementFlags.DISCOVERED);
    }

    private void validateRootPage(int count) {
        System.arraycopy(childScratch, 0, validationScratch, 0, count);
        Arrays.sort(validationScratch, 0, count);
        for (int index = 0; index < count; index++) {
            long rootId = childScratch[index];
            checkLinkableId(rootId);
            if (index > 0 && validationScratch[index - 1] == validationScratch[index]) {
                throw new IllegalArgumentException(
                        "duplicate root id in provider page: " + validationScratch[index]);
            }
            if (parent.getBits(rootId) != 0L) {
                throw new IllegalArgumentException("root already belongs to a parent: " + rootId);
            }
            if (rootMembership.get(rootId)) {
                throw new IllegalArgumentException("root already linked: " + rootId);
            }
            if (nextSibling.getBits(rootId) != 0L || previousSibling.getBits(rootId) != 0L) {
                throw new IllegalArgumentException("root already linked as sibling: " + rootId);
            }
        }
    }

    private void linkValidatedRoot(long rootId) {
        int encodedRoot = encodeLink(rootId);
        if (lastRoot == 0) {
            firstRoot = encodedRoot;
        } else {
            nextSibling.setBits(lastRoot - 1L, encodedRoot);
            previousSibling.setBits(rootId, lastRoot);
        }
        lastRoot = encodedRoot;
        rootMembership.set(rootId);
        nodes.addFlags(rootId, M3LazyElementFlags.DISCOVERED);
    }

    public static final class VisibleCursor {
        private final M3ProviderBackedPrimitiveTree tree;
        private final long expectedRevision;
        private int[] resume = new int[16];
        private int resumeSize;
        private int next;

        private VisibleCursor(M3ProviderBackedPrimitiveTree tree) {
            this.tree = tree;
            expectedRevision = tree.structureRevision;
            next = tree.firstLoadedRoot();
        }

        public boolean hasNext() {
            checkRevision();
            return next >= 0;
        }

        public long nextLong() {
            checkRevision();
            if (next < 0) throw new NoSuchElementException();
            int current = next;
            int sibling = tree.nextLoadedSibling(current);
            int child = tree.isExpanded(current) ? tree.firstLoadedChild(current) : -1;

            if (child >= 0) {
                if (sibling >= 0) push(sibling);
                next = child;
            } else if (sibling >= 0) {
                next = sibling;
            } else if (resumeSize != 0) {
                next = resume[--resumeSize];
            } else {
                next = -1;
            }
            return current;
        }

        private void push(int nodeId) {
            if (resumeSize == resume.length) {
                resume = Arrays.copyOf(resume, resume.length << 1);
            }
            resume[resumeSize++] = nodeId;
        }

        private void checkRevision() {
            if (expectedRevision != tree.structureRevision) {
                throw new ConcurrentModificationException(
                        "tree relationships or expansion changed during visible traversal");
            }
        }
    }

    private void validatePageResult(int offset, int count, int nextOffset) {
        if (count < 0 || count > pageSize) {
            throw new IllegalArgumentException("provider returned invalid child count " + count);
        }
        if (nextOffset < -1) {
            throw new IllegalArgumentException("provider returned invalid nextOffset " + nextOffset);
        }
        if (nextOffset >= 0) {
            if (count == 0) {
                throw new IllegalArgumentException("partial page made no progress");
            }
            if (nextOffset != Math.addExact(offset, count)) {
                throw new IllegalArgumentException(
                        "offset contract requires nextOffset == offset + count");
            }
        }
    }

    private void checkLinkableId(long nodeId) {
        if (nodeId < 0L || nodeId >= logicalCapacity()) {
            throw new IndexOutOfBoundsException(Long.toString(nodeId));
        }
        if (nodeId >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("tree node id cannot be int-link encoded: " + nodeId);
        }
    }

    private static int encodeLink(long nodeId) {
        return Math.addExact(Math.toIntExact(nodeId), 1);
    }

    private static int decodeLink(long encoded) {
        return encoded == 0L ? -1 : Math.toIntExact(encoded) - 1;
    }

    private static M3LazyPrimitiveAddressSpace ints(
            int rootBits, int directoryBits, int leafBits) {
        return new M3LazyPrimitiveAddressSpace(
                M3PrimitiveKind.INT, rootBits, directoryBits, leafBits);
    }
}
