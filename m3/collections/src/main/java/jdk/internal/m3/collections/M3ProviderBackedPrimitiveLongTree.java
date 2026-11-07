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
import java.util.Objects;
import java.util.function.LongConsumer;
import jdk.m3.collections.M3LazyPrimitiveAddressSpace;
import jdk.m3.collections.M3PrimitiveKind;

/**
 * Provider-backed pageable primitive tree with long logical ids and opaque long child cursors.
 *
 * <p>Payload, state and relationships are columnar lazy primitive address spaces. Child discovery
 * links ids only; child payload remains lazy until touched.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ProviderBackedPrimitiveLongTree.java}
 * blob {@code eeb297154bf50bda17534f5c6ec0ac8bb61a92db} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3ProviderBackedPrimitiveLongTree {
    public static final long NO_NODE = -1L;

    private final M3ProviderBackedPrimitiveRows nodes;
    private final M3CursorPagedPrimitiveTreeProvider provider;
    private final int pageSize;
    private final long[] childScratch;
    private final long[] validationScratch;
    private final long[] nextCursorScratch = new long[1];

    private final M3LazyPrimitiveAddressSpace parent;
    private final M3LazyPrimitiveAddressSpace firstChild;
    private final M3LazyPrimitiveAddressSpace lastChild;
    private final M3LazyPrimitiveAddressSpace nextSibling;
    private final M3LazyPrimitiveAddressSpace nextChildCursor;
    private final M3LazyPrimitiveAddressSpace loadedChildCount;

    public M3ProviderBackedPrimitiveLongTree(
            M3CursorPagedPrimitiveTreeProvider provider,
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

    public M3ProviderBackedPrimitiveLongTree(
            int rootBits,
            int directoryBits,
            int leafBits,
            M3CursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            M3PrimitiveKind... laneKinds) {
        this.provider = Objects.requireNonNull(provider, "provider");
        if (pageSize <= 0) throw new IllegalArgumentException("pageSize <= 0");
        this.pageSize = pageSize;
        nodes = new M3ProviderBackedPrimitiveRows(
                rootBits, directoryBits, leafBits, provider, laneKinds);
        childScratch = new long[pageSize];
        validationScratch = new long[pageSize];

        parent = longs(rootBits, directoryBits, leafBits);
        firstChild = longs(rootBits, directoryBits, leafBits);
        lastChild = longs(rootBits, directoryBits, leafBits);
        nextSibling = longs(rootBits, directoryBits, leafBits);
        nextChildCursor = longs(rootBits, directoryBits, leafBits);
        loadedChildCount = longs(rootBits, directoryBits, leafBits);
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
                + nextChildCursor.allocatedPayloadBytes()
                + loadedChildCount.allocatedPayloadBytes();
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

    public long parentOf(long nodeId) {
        checkLinkableId(nodeId);
        return decodeLink(parent.getBits(nodeId));
    }

    public long firstLoadedChild(long parentId) {
        checkLinkableId(parentId);
        return decodeLink(firstChild.getBits(parentId));
    }

    public long nextLoadedSibling(long nodeId) {
        checkLinkableId(nodeId);
        return decodeLink(nextSibling.getBits(nodeId));
    }

    public long loadedChildCount(long parentId) {
        checkLinkableId(parentId);
        return loadedChildCount.getBits(parentId);
    }

    public boolean childrenComplete(long parentId) {
        return M3LazyElementFlags.has(flags(parentId), M3LazyElementFlags.CHILDREN_COMPLETE);
    }

    /** Returns END_CURSOR when the branch is complete; otherwise the provider-owned cursor. */
    public long nextChildCursor(long parentId) {
        checkLinkableId(parentId);
        return childrenComplete(parentId)
                ? M3CursorPagedPrimitiveTreeProvider.END_CURSOR
                : nextChildCursor.getBits(parentId);
    }

    public M3PagedPrimitiveTreeProvider.ChildHint childHint(long nodeId) {
        checkLinkableId(nodeId);
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
     * @return number of newly linked child ids
     */
    public int loadNextChildPage(long parentId) {
        checkLinkableId(parentId);

        if (nodes.isMaterialized(parentId) && childrenComplete(parentId)) return 0;

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

        long cursor = nextChildCursor.getBits(parentId);
        nodes.beginCallback();
        try {
            nodes.addFlags(parentId, M3LazyElementFlags.CHILDREN_LOADING);
            Arrays.fill(childScratch, 0L);
            Arrays.fill(validationScratch, 0L);
            nextCursorScratch[0] = cursor;

            int count = provider.loadChildren(
                    parentId, cursor, pageSize, childScratch, nextCursorScratch);
            long nextCursor = nextCursorScratch[0];
            validatePageResult(cursor, count, nextCursor);
            validateChildPage(parentId, count);

            long existingTotal = loadedChildCount.getBits(parentId);
            if (hint == M3PagedPrimitiveTreeProvider.ChildHint.SOME
                    && existingTotal == 0L
                    && count == 0
                    && nextCursor == M3CursorPagedPrimitiveTreeProvider.END_CURSOR) {
                throw new IllegalArgumentException(
                        "provider childHint SOME contradicted by empty complete branch");
            }

            for (int index = 0; index < count; index++) {
                linkValidatedChild(parentId, childScratch[index]);
            }

            long total = Math.addExact(existingTotal, count);
            loadedChildCount.setBits(parentId, total);
            if (nextCursor == M3CursorPagedPrimitiveTreeProvider.END_CURSOR) {
                nextChildCursor.clearBits(parentId);
            } else {
                nextChildCursor.setBits(parentId, nextCursor);
            }

            short nextState = flags(parentId);
            nextState = M3LazyElementFlags.remove(nextState,
                    (short) (M3LazyElementFlags.CHILDREN_LOADING
                            | M3LazyElementFlags.CHILDREN_PARTIAL
                            | M3LazyElementFlags.CHILDREN_COMPLETE));
            nextState = M3LazyElementFlags.add(nextState,
                    (short) (M3LazyElementFlags.DISCOVERED
                            | M3LazyElementFlags.CHILDREN_KNOWN));
            if (total > 0L || hint == M3PagedPrimitiveTreeProvider.ChildHint.SOME) {
                nextState = M3LazyElementFlags.add(nextState, M3LazyElementFlags.HAS_CHILDREN);
            } else {
                nextState = M3LazyElementFlags.remove(nextState, M3LazyElementFlags.HAS_CHILDREN);
            }
            nextState = M3LazyElementFlags.add(
                    nextState,
                    nextCursor == M3CursorPagedPrimitiveTreeProvider.END_CURSOR
                            ? M3LazyElementFlags.CHILDREN_COMPLETE
                            : M3LazyElementFlags.CHILDREN_PARTIAL);
            nodes.replaceFlags(parentId, nextState);
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
            nextCursorScratch[0] = 0L;
            nodes.endCallback();
        }
    }

    /**
     * Page until at least {@code minimumLoaded} children are linked, the branch completes, or the
     * page budget is exhausted.
     *
     * @return current loaded child count
     */
    public long ensureLoadedChildren(long parentId, long minimumLoaded, int maxPages) {
        if (minimumLoaded < 0L) throw new IllegalArgumentException("minimumLoaded < 0");
        if (maxPages < 0) throw new IllegalArgumentException("maxPages < 0");
        checkLinkableId(parentId);

        int pages = 0;
        while (loadedChildCount(parentId) < minimumLoaded
                && !childrenComplete(parentId)
                && pages < maxPages) {
            loadNextChildPage(parentId);
            pages++;
        }
        return loadedChildCount(parentId);
    }

    /** O(ordinal) lookup over the already-linked sibling chain; never loads another page. */
    public long loadedChildAt(long parentId, long ordinal) {
        if (ordinal < 0L) throw new IndexOutOfBoundsException(Long.toString(ordinal));
        long child = firstLoadedChild(parentId);
        long index = 0L;
        while (child >= 0L && index < ordinal) {
            child = nextLoadedSibling(child);
            index++;
        }
        if (child < 0L) throw new IndexOutOfBoundsException(Long.toString(ordinal));
        return child;
    }

    public void forEachLoadedChild(long parentId, LongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        long child = firstLoadedChild(parentId);
        while (child >= 0L) {
            consumer.accept(child);
            child = nextLoadedSibling(child);
        }
    }

    /**
     * Materializes an already-linked long-id child window without invoking topology paging and
     * without emitting logical touch-observer events.
     *
     * @return number of newly materialized child payload rows
     */
    public int prefetchLoadedChildren(long parentId, long firstOrdinal, int count) {
        long loaded = loadedChildCount(parentId);
        if (firstOrdinal < 0L || count < 0
                || firstOrdinal > loaded
                || (long) count > loaded - firstOrdinal) {
            throw new IndexOutOfBoundsException(firstOrdinal + "+" + count);
        }
        nodes.requireCallbackIdle();

        long child = firstLoadedChild(parentId);
        for (long skipped = 0L; skipped < firstOrdinal; skipped++) {
            child = nextLoadedSibling(child);
        }

        int total = 0;
        int remaining = count;
        while (remaining > 0) {
            int batch = 0;
            while (batch < childScratch.length && batch < remaining && child >= 0L) {
                childScratch[batch++] = child;
                child = nextLoadedSibling(child);
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

    /** Drops direct-child relationship cache without evicting any materialized payload rows. */
    public void invalidateChildren(long parentId) {
        nodes.requireCallbackIdle();
        checkLinkableId(parentId);
        long child = firstLoadedChild(parentId);
        while (child >= 0L) {
            long next = nextLoadedSibling(child);
            parent.clearBits(child);
            nextSibling.clearBits(child);
            child = next;
        }
        firstChild.clearBits(parentId);
        lastChild.clearBits(parentId);
        nextChildCursor.clearBits(parentId);
        loadedChildCount.clearBits(parentId);
        nodes.clearFlags(parentId, M3LazyElementFlags.CHILD_STATE_MASK);
    }

    public boolean invalidatePayload(long nodeId) {
        return nodes.invalidate(nodeId);
    }

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

            long existingParent = parent.getBits(childId);
            if (existingParent != 0L) {
                throw new IllegalArgumentException(
                        existingParent == encodeLink(parentId)
                                ? "child already linked to parent: " + childId
                                : "child already belongs to another parent: " + childId);
            }
            if (nextSibling.getBits(childId) != 0L) {
                throw new IllegalArgumentException("child already linked as sibling: " + childId);
            }

            for (long ancestor = encodeLink(parentId); ancestor != 0L;) {
                long ancestorId = decodeLink(ancestor);
                if (ancestorId == childId) {
                    throw new IllegalArgumentException(
                            "provider introduced tree cycle at " + childId);
                }
                ancestor = parent.getBits(ancestorId);
            }
        }
    }

    private void linkValidatedChild(long parentId, long childId) {
        long encodedParent = encodeLink(parentId);
        long encodedChild = encodeLink(childId);
        long last = lastChild.getBits(parentId);
        if (last == 0L) {
            firstChild.setBits(parentId, encodedChild);
        } else {
            nextSibling.setBits(decodeLink(last), encodedChild);
        }
        lastChild.setBits(parentId, encodedChild);
        parent.setBits(childId, encodedParent);
        nodes.addFlags(childId, M3LazyElementFlags.DISCOVERED);
    }

    private void validatePageResult(long cursor, int count, long nextCursor) {
        if (count < 0 || count > pageSize) {
            throw new IllegalArgumentException("provider returned invalid child count " + count);
        }
        if (nextCursor != M3CursorPagedPrimitiveTreeProvider.END_CURSOR
                && nextCursor == cursor) {
            throw new IllegalArgumentException("partial page cursor made no progress");
        }
    }

    private void checkLinkableId(long nodeId) {
        if (nodeId < 0L || nodeId >= logicalCapacity()) {
            throw new IndexOutOfBoundsException(Long.toString(nodeId));
        }
    }

    private static long encodeLink(long nodeId) {
        return Math.addExact(nodeId, 1L);
    }

    private static long decodeLink(long encoded) {
        return encoded == 0L ? NO_NODE : encoded - 1L;
    }

    private static M3LazyPrimitiveAddressSpace longs(
            int rootBits, int directoryBits, int leafBits) {
        return new M3LazyPrimitiveAddressSpace(
                M3PrimitiveKind.LONG, rootBits, directoryBits, leafBits);
    }
}
