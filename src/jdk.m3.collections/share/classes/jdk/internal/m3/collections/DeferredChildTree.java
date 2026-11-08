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
import java.util.function.Consumer;
import jdk.m3.collections.ParentOwned;
import jdk.m3.collections.RuntimeHeap;
import jdk.m3.collections.TransientFacadeStore;

/**
 * DOOP parent-owned logical children: no per-child Java Entry or Node objects.
 *
 * <p>Each logical child has a stable, monotonically assigned long ID, a primitive payload,
 * and one 16-bit lifecycle/application flag word. The parent owns the indexed sibling
 * topology in parallel primitive arrays. Appending children is O(log sibling count)
 * worst-case through an array-backed AVL order-statistic index. Selecting a child by
 * ordinal is O(log sibling count); reading a consecutive viewport is O(log n + window).
 * The original child order is never changed by scheduling or facade materialization.
 *
 * <p>The facade factory belongs to the existing {@link TransientFacadeStore}. Behavior
 * operations change parent-owned bits without calling it. Publicly exposed facades are
 * pinned and keep ordinary Java identity. This class does not attempt to transparently
 * swap live Java references or persist mutable arbitrary object graphs. The existing
 * M3 paged/native stores remain the separate canonical owners of cold primitive data.
 *
 * <p>Thread safety: each public method is synchronized. A caller-supplied facade factory
 * or disposer must respect UI-thread/constructor contracts; a compiler must prove
 * non-escape before using {@link #withInternalFacade(long, Consumer)}. There is no
 * source-to-bytecode rewrite or JVM-level escape proof in this component.
 */
@ParentOwned(owner = "DeferredChildTree", flags = TransientFacadeStore.DEFAULT_MODE
        | TransientFacadeStore.AUTO_HEAP_PRESSURE | (1 << 16))
/**
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/DeferredChildTree.java}
 * blob {@code 50ed19dbb542d926c7e87c443da54dadf6d9ca13} at com.synexia develop {@code eaacff5931ef16a1a52425043bb71b821eda3515} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class DeferredChildTree<T> implements AutoCloseable {
    /** Optional viewport prefetch; main visible rows are always mandatory. */
    public static final int VIEWPORT_OVERSCAN = 1 << 16;
    /** 4k maximum rows per query (visible + optional overscan). */
    public static final int MAX_WINDOW = 4096;
    public static final int DEFAULT_MODE = TransientFacadeStore.DEFAULT_MODE
            | TransientFacadeStore.AUTO_HEAP_PRESSURE | VIEWPORT_OVERSCAN;
    private static final int SLOT_BYTES = 31;
    private static final long ESTIMATED_PREFETCH_BYTES = 128L;

    private final TransientFacadeStore<T> facades;
    private final int mode;

    // Slot zero is an invisible root whose children are the top-level logical rows.
    private long[] payload = new long[17];
    private short[] flags = new short[17];
    private int[] owner = new int[17];
    private int[] childRoot = new int[17];
    private int[] left = new int[17];
    private int[] right = new int[17];
    private int[] subtree = new int[17];
    private byte[] height = new byte[17];
    // Optional DOOP sorted projection. One stable child ID is one primitive AVL slot.
    // A key lane may be prepared before the first sorted query; no extra per-child objects.
    private long[] sortKeys;
    private int[] sortedRoot;
    private int[] sortedLeft;
    private int[] sortedRight;
    private int[] sortedSubtree;
    private byte[] sortedHeight;
    private byte[] sortedReady;
    private int size;
    private boolean closed;

    public DeferredChildTree(TransientFacadeStore.Factory<T> factory) {
        this(factory, DEFAULT_MODE);
    }

    /** Construction flags include the existing facade-store policies and VIEWPORT_OVERSCAN. */
    public DeferredChildTree(TransientFacadeStore.Factory<T> factory, int constructionFlags) {
        this(factory, (id, facade) -> { }, constructionFlags);
    }

    public DeferredChildTree(TransientFacadeStore.Factory<T> factory,
            TransientFacadeStore.Disposer<T> disposer, int constructionFlags) {
        int known = VIEWPORT_OVERSCAN | TransientFacadeStore.RECREATE_UNEXPOSED
                | TransientFacadeStore.RETAIN_UNEXPOSED | TransientFacadeStore.PIN_EXPOSED
                | TransientFacadeStore.PRESSURE_TRIMMABLE
                | TransientFacadeStore.AUTO_HEAP_PRESSURE;
        if ((constructionFlags & ~known) != 0) {
            throw new IllegalArgumentException("unknown DOOP construction flag");
        }
        mode = constructionFlags;
        facades = new TransientFacadeStore<>(Objects.requireNonNull(factory, "factory"),
                Objects.requireNonNull(disposer, "disposer"), constructionFlags & ~VIEWPORT_OVERSCAN);
    }

    public synchronized int mode() { return mode; }
    public synchronized int size() { return size; }
    public synchronized int capacity() { return payload.length - 1; }
    public synchronized long logicalPayloadBytes() { return (long) payload.length * SLOT_BYTES; }

    /**
     * Append under zero (invisible root) or a prior child. The caller supplies only
     * the logical parent, state bits and application flags; capacity decisions are internal.
     * Successful IDs are never reused, so already-exposed facades keep stable identity.
     */
    public synchronized long appendChild(long parentId, long initialBits, short applicationFlags) {
        requireOpen();
        int parent = slotOrRoot(parentId);
        requireApplicationBits(applicationFlags);
        if (size >= Integer.MAX_VALUE - 2) throw new IllegalStateException("logical child ID exhausted");
        int nextId = size + 1;
        ensureCapacity(nextId);
        payload[nextId] = initialBits;
        if (sortKeys != null) sortKeys[nextId] = initialBits;
        flags[nextId] = (short) (LazyElementFlags.DISCOVERED | applicationFlags);
        owner[nextId] = parent;
        left[nextId] = right[nextId] = childRoot[nextId] = 0;
        subtree[nextId] = 1;
        height[nextId] = 1;
        childRoot[parent] = appendRight(childRoot[parent], nextId);
        if (sortedReady != null && sortedReady[parent] != 0) {
            sortedLeaf(nextId);
            sortedRoot[parent] = sortedInsert(sortedRoot[parent], nextId);
        }
        size = nextId;
        return nextId;
    }

    public synchronized int childCount(long parentId) {
        requireOpen();
        return count(childRoot[slotOrRoot(parentId)]);
    }

    public synchronized long childAt(long parentId, int ordinal) {
        requireOpen();
        int p = slotOrRoot(parentId);
        int count = count(childRoot[p]);
        if (ordinal < 0 || ordinal >= count) throw new IndexOutOfBoundsException(ordinal);
        int node = childRoot[p];
        int k = ordinal;
        while (node != 0) {
            int before = count(left[node]);
            if (k < before) node = left[node];
            else if (k == before) return node;
            else { k -= before + 1; node = right[node]; }
        }
        throw new IllegalStateException("child topology corrupted");
    }

    public synchronized long parentOf(long childId) {
        requireOpen();
        return owner[slot(childId)];
    }

    public synchronized long valueBits(long childId) {
        requireOpen();
        return payload[slot(childId)];
    }

    /** Behavior-only update: no facade lookup, I/O or Runtime sampling.
     *  The creation-time sort signal is captured lazily before first divergence. */
    public synchronized void valueBits(long childId, long bits) {
        requireOpen();
        int id = slot(childId);
        if (payload[id] == bits) return;
        // Capture initial key signals exactly once before any payload diverges.
        // No facade, Entry or Node object is ever allocated.
        if (sortKeys == null) sortKeys = Arrays.copyOf(payload, payload.length);
        payload[id] = bits;
    }

    /** A stable ID's independent signed-long ordering signal, defaulting to its initial payload. */
    public synchronized long sortKey(long childId) {
        requireOpen();
        int id = slot(childId);
        return sortKeys == null ? payload[id] : sortKeys[id];
    }

    /**
     * Change only this ID's ordering signal. The payload, natural sibling order, flags,
     * existing public facades and every other sort signal remain unchanged. Tree work
     * starts only after the parent has had its first sorted query.
     */
    public synchronized void setSortKey(long childId, long key) {
        requireOpen();
        int id = slot(childId);
        long previous = sortKey(childId);
        if (previous == key) return;
        if (sortKeys == null) sortKeys = Arrays.copyOf(payload, payload.length);
        int parent = owner[id];
        if (sortedReady != null && sortedReady[parent] != 0) {
            sortedRoot[parent] = sortedDelete(sortedRoot[parent], id);
            sortedLeaf(id);
            sortKeys[id] = key;
            sortedRoot[parent] = sortedInsert(sortedRoot[parent], id);
        } else sortKeys[id] = key;
    }

    /** Sorted projection is lazy and independent of natural child/source order. */
    public synchronized boolean isSortedIndexActive(long parentId) {
        requireOpen();
        int parent = slotOrRoot(parentId);
        return sortedReady != null && sortedReady[parent] != 0;
    }

    /** Capacity-accounted optional primitive storage; excludes JVM array headers. */
    public synchronized long sortedIndexPrimitiveBytes() {
        requireOpen();
        if (sortKeys == null) return 0L;
        long total = (long) sortKeys.length * Long.BYTES;
        if (sortedRoot != null) total += (long) sortedRoot.length *
                (4L * Integer.BYTES + 2L * Byte.BYTES);
        return total;
    }

    /** Resolve one signed-sort-key ordered sibling in logarithmic time after first use. */
    public synchronized long sortedChildAt(long parentId, int ordinal) {
        requireOpen();
        int parent = slotOrRoot(parentId);
        int count = count(childRoot[parent]);
        if (ordinal < 0 || ordinal >= count) throw new IndexOutOfBoundsException(ordinal);
        activateSorted(parent);
        int node = sortedRoot[parent];
        int remainder = ordinal;
        while (node != 0) {
            int before = sortedCount(sortedLeft[node]);
            if (remainder < before) node = sortedLeft[node];
            else if (remainder == before) return node;
            else { remainder -= before + 1; node = sortedRight[node]; }
        }
        throw new IllegalStateException("corrupt DOOP sorted child rank");
    }

    /** Number of siblings whose cached sort key is strictly smaller. */
    public synchronized int sortedLowerBound(long parentId, long key) {
        requireOpen();
        int parent = slotOrRoot(parentId);
        activateSorted(parent);
        int total = 0, node = sortedRoot[parent];
        while (node != 0) {
            if (Long.compare(sortKeys[node], key) < 0) {
                total += sortedCount(sortedLeft[node]) + 1;
                node = sortedRight[node];
            } else node = sortedLeft[node];
        }
        return total;
    }

    /** Number of siblings whose cached sort key is smaller or equal. */
    public synchronized int sortedUpperBound(long parentId, long key) {
        requireOpen();
        int parent = slotOrRoot(parentId);
        activateSorted(parent);
        int total = 0, node = sortedRoot[parent];
        while (node != 0) {
            if (Long.compare(sortKeys[node], key) <= 0) {
                total += sortedCount(sortedLeft[node]) + 1;
                node = sortedRight[node];
            } else node = sortedLeft[node];
        }
        return total;
    }

    /** Sorted ordinal of this exact child ID; duplicate keys use ascending IDs. */
    public synchronized int sortedRankOf(long childId) {
        requireOpen();
        int id = slot(childId);
        int parent = owner[id];
        activateSorted(parent);
        int node = sortedRoot[parent], prefix = 0;
        while (node != 0) {
            int cmp = sortedCompare(id, node);
            if (cmp < 0) node = sortedLeft[node];
            else if (cmp == 0) return prefix + sortedCount(sortedLeft[node]);
            else { prefix += sortedCount(sortedLeft[node]) + 1; node = sortedRight[node]; }
        }
        throw new IllegalStateException("DOOP child absent from sorted projection");
    }

    /** Mandatory bounded sorted viewport; optional overscan uses the same Xmx policy. */
    public synchronized int fillSortedChildWindow(long parentId, int firstOrdinal,
            int visibleRows, int overscanRows, long[] output, int offset) {
        return fillSortedChildWindow(parentId, firstOrdinal, visibleRows, overscanRows,
                output, offset, null);
    }

    /** Deterministic injection for boundary tests; zero-visible requests do no work. */
    synchronized int fillSortedChildWindow(long parentId, int firstOrdinal,
            int visibleRows, int overscanRows, long[] output, int offset,
            RuntimeHeap.Snapshot observed) {
        requireOpen();
        int parent = slotOrRoot(parentId);
        Objects.requireNonNull(output, "output");
        if (firstOrdinal < 0 || visibleRows < 0 || overscanRows < 0
                || visibleRows > MAX_WINDOW || overscanRows > MAX_WINDOW - visibleRows)
            throw new IllegalArgumentException("viewport geometry");
        int total = count(childRoot[parent]);
        if (firstOrdinal > total) throw new IndexOutOfBoundsException(firstOrdinal);
        int mandatory = Math.min(visibleRows, total - firstOrdinal);
        Objects.checkFromIndexSize(offset, mandatory, output.length);
        if (visibleRows == 0 || mandatory == 0) return 0;
        int admitted = 0;
        if ((mode & VIEWPORT_OVERSCAN) != 0 && overscanRows > 0
                && mandatory == visibleRows) {
            RuntimeHeap.Snapshot heap = observed == null ? RuntimeHeap.current() : observed;
            long spare = Math.max(0L, heap.freeToMaximum() - heap.safetyReserve());
            admitted = (int) Math.min((long) overscanRows, spare / ESTIMATED_PREFETCH_BYTES);
        }
        admitted = Math.min(admitted, output.length - offset - mandatory);
        int wanted = Math.min(total - firstOrdinal, mandatory + admitted);
        activateSorted(parent);
        return visitSortedRanks(sortedRoot[parent], firstOrdinal,
                firstOrdinal + wanted, output, offset) - offset;
    }

    public synchronized short flags(long childId) {
        requireOpen();
        return flags[slot(childId)];
    }

    /** Only the four application bits may be changed without lifecycle authorization. */
    public synchronized void setApplicationFlags(long childId, short mask, boolean enabled) {
        requireOpen();
        requireApplicationBits(mask);
        int slot = slot(childId);
        flags[slot] = enabled ? LazyElementFlags.add(flags[slot], mask)
                : LazyElementFlags.remove(flags[slot], mask);
    }

    /** Materialize at the public/API boundary and retain stable public Java identity. */
    public synchronized T expose(long childId) {
        requireOpen();
        int slot = slot(childId);
        T exposed = facades.expose(childId);
        flags[slot] = LazyElementFlags.add(flags[slot],
                (short) (LazyElementFlags.MATERIALIZED | LazyElementFlags.PINNED));
        return exposed;
    }

    /** Eligible only for compiler-proven non-escaping references. */
    public synchronized void withInternalFacade(long childId, Consumer<? super T> callback) {
        requireOpen();
        slot(childId);
        facades.withInternal(childId, Objects.requireNonNull(callback, "callback"));
    }

    /** Drop optional facades through the existing LRU owner, never pinned public references. */
    public synchronized int trimOptionalFacades(int target) {
        requireOpen();
        return facades.releaseOptionalResidency(target);
    }

    public synchronized int residentFacadeCount() { requireOpen(); return facades.residentCount(); }

    /**
     * Read a caller-owned bounded viewport of stable child IDs. Overscan admission is sampled
     * once at the viewport boundary and is optional under pressure. No per-row objects.
     * Output is validated before writing, so invalid geometry changes no caller bytes.
     */
    public synchronized int fillChildWindow(long parentId, int firstOrdinal,
            int visibleRows, int overscanRows, long[] output, int offset) {
        return fillChildWindow(parentId, firstOrdinal, visibleRows, overscanRows,
                output, offset, null);
    }

    /** Deterministic package-private pressure seam for tests. */
    synchronized int fillChildWindow(long parentId, int firstOrdinal, int visibleRows,
            int overscanRows, long[] output, int offset, RuntimeHeap.Snapshot observed) {
        requireOpen();
        int parent = slotOrRoot(parentId);
        Objects.requireNonNull(output, "output");
        if (firstOrdinal < 0 || visibleRows < 0 || overscanRows < 0
                || visibleRows > MAX_WINDOW || overscanRows > MAX_WINDOW - visibleRows) {
            throw new IllegalArgumentException("viewport geometry");
        }
        int total = count(childRoot[parent]);
        if (firstOrdinal > total) throw new IndexOutOfBoundsException(firstOrdinal);
        int mandatory = Math.min(visibleRows, total - firstOrdinal);
        Objects.checkFromIndexSize(offset, mandatory, output.length);
        // No visible rows: skip optional overscan and JVM/JNI pressure sampling.
        if (visibleRows == 0) return 0;
        int admitted = 0;
        if ((mode & VIEWPORT_OVERSCAN) != 0 && overscanRows > 0
                && mandatory == visibleRows) {
            RuntimeHeap.Snapshot heap = observed == null ? RuntimeHeap.current() : observed;
            long spare = Math.max(0L, heap.freeToMaximum() - heap.safetyReserve());
            admitted = (int) Math.min((long) overscanRows, spare / ESTIMATED_PREFETCH_BYTES);
        }
        int outputHeadroom = output.length - offset - mandatory;
        admitted = Math.min(admitted, outputHeadroom);
        int wanted = Math.min(total - firstOrdinal, mandatory + admitted);
        int end = firstOrdinal + wanted;
        return visitRanks(childRoot[parent], firstOrdinal, end, output, offset) - offset;
    }


    /**
     * Project stable child IDs, payload bits and the complete parent-owned flag
     * words together. A GPU/SWT/Swing renderer may reuse three primitive
     * staging lanes instead of obtaining a facade or invoking three getters per
     * visible child. No per-row Java object, Entry or native call is created.
     *
     * <p>Logical parent state is immutable during this synchronized traversal.
     * Validate the mandatory visible span against all three destination arrays
     * before writing; optional overscan is reduced to their shared headroom.
     * Heap pressure is sampled at most once for the whole window, never per
     * child. Exposed Java facades retain their normal identity and lifetime.</p>
     *
     * @return number of logical children copied, including admitted overscan
     */
    public synchronized int fillChildWindowColumns(long parentId, int firstOrdinal,
            int visibleRows, int overscanRows, long[] ids, long[] bits,
            short[] flagWords, int offset) {
        return fillChildWindowColumns(parentId, firstOrdinal, visibleRows,
                overscanRows, ids, bits, flagWords, offset, null);
    }

    /** Package-private deterministic pressure source; ordinary callers need no knobs. */
    synchronized int fillChildWindowColumns(long parentId, int firstOrdinal,
            int visibleRows, int overscanRows, long[] ids, long[] bits,
            short[] flagWords, int offset, RuntimeHeap.Snapshot observed) {
        requireOpen();
        int parent = slotOrRoot(parentId);
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(bits, "bits");
        Objects.requireNonNull(flagWords, "flagWords");
        if (firstOrdinal < 0 || visibleRows < 0 || overscanRows < 0
                || visibleRows > MAX_WINDOW || overscanRows > MAX_WINDOW - visibleRows) {
            throw new IllegalArgumentException("viewport geometry");
        }
        int total = count(childRoot[parent]);
        if (firstOrdinal > total) throw new IndexOutOfBoundsException(firstOrdinal);
        int mandatory = Math.min(visibleRows, total - firstOrdinal);
        Objects.checkFromIndexSize(offset, mandatory, ids.length);
        Objects.checkFromIndexSize(offset, mandatory, bits.length);
        Objects.checkFromIndexSize(offset, mandatory, flagWords.length);
        // Refuse aliasing before optional JVM/JNI pressure sampling.
        if (mandatory != 0 && ids == bits) {
            throw new IllegalArgumentException("overlapping child ID/payload output lanes");
        }
        if (visibleRows == 0) return 0;

        int admitted = 0;
        if ((mode & VIEWPORT_OVERSCAN) != 0 && overscanRows > 0
                && mandatory == visibleRows) {
            RuntimeHeap.Snapshot heap = observed == null ? RuntimeHeap.current() : observed;
            long spare = Math.max(0L, heap.freeToMaximum() - heap.safetyReserve());
            admitted = (int) Math.min((long) overscanRows, spare / ESTIMATED_PREFETCH_BYTES);
        }
        int commonHeadroom = Math.min(ids.length - offset - mandatory,
                Math.min(bits.length - offset - mandatory,
                        flagWords.length - offset - mandatory));
        admitted = Math.min(admitted, commonHeadroom);
        int wanted = Math.min(total - firstOrdinal, mandatory + admitted);
        return visitRanksColumns(childRoot[parent], firstOrdinal, firstOrdinal + wanted,
                ids, bits, flagWords, offset) - offset;
    }

    @Override public synchronized void close() {
        if (closed) return;
        facades.close();
        closed = true;
    }

    private static void requireApplicationBits(short mask) {
        if ((mask & ~LazyElementFlags.APPLICATION_MASK) != 0) {
            throw new IllegalArgumentException("reserved lifecycle flags");
        }
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("closed DOOP tree");
    }

    private int slot(long id) {
        if (id <= 0 || id > size) throw new IllegalArgumentException("unknown child ID: " + id);
        return (int) id;
    }

    private int slotOrRoot(long id) {
        return id == 0 ? 0 : slot(id);
    }

    private int count(int node) { return node == 0 ? 0 : subtree[node]; }
    private int height(int node) { return node == 0 ? 0 : height[node] & 0xff; }

    /** Allocate the optional primitive lanes once, then build each parent's index on demand. */
    private void activateSorted(int parent) {
        if (sortedReady == null) {
            int capacity = payload.length;
            long[] keys = sortKeys == null ? Arrays.copyOf(payload, capacity) : sortKeys;
            int[] roots = new int[capacity];
            int[] l = new int[capacity];
            int[] r = new int[capacity];
            int[] sizes = new int[capacity];
            byte[] h = new byte[capacity];
            byte[] ready = new byte[capacity];
            // No partially allocated index is published if any lane allocation fails.
            sortKeys = keys;
            sortedRoot = roots;
            sortedLeft = l;
            sortedRight = r;
            sortedSubtree = sizes;
            sortedHeight = h;
            sortedReady = ready;
        }
        if (sortedReady[parent] != 0) return;
        sortedRoot[parent] = buildSorted(childRoot[parent], 0);
        sortedReady[parent] = 1;
    }

    private int buildSorted(int naturalNode, int root) {
        if (naturalNode == 0) return root;
        root = buildSorted(left[naturalNode], root);
        sortedLeaf(naturalNode);
        root = sortedInsert(root, naturalNode);
        return buildSorted(right[naturalNode], root);
    }

    private int sortedCount(int node) { return node == 0 ? 0 : sortedSubtree[node]; }
    private int sortedHeight(int node) { return node == 0 ? 0 : sortedHeight[node] & 0xff; }
    private void sortedLeaf(int id) {
        sortedLeft[id] = sortedRight[id] = 0;
        sortedSubtree[id] = 1;
        sortedHeight[id] = 1;
    }
    private int sortedCompare(int a, int b) {
        int key = Long.compare(sortKeys[a], sortKeys[b]);
        return key != 0 ? key : Integer.compare(a, b);
    }
    private void sortedRefresh(int node) {
        sortedSubtree[node] = 1 + sortedCount(sortedLeft[node]) + sortedCount(sortedRight[node]);
        sortedHeight[node] = (byte) (1 + Math.max(
                sortedHeight(sortedLeft[node]), sortedHeight(sortedRight[node])));
    }
    private int sortedRotateLeft(int node) {
        int pivot = sortedRight[node];
        sortedRight[node] = sortedLeft[pivot];
        sortedLeft[pivot] = node;
        sortedRefresh(node);
        sortedRefresh(pivot);
        return pivot;
    }
    private int sortedRotateRight(int node) {
        int pivot = sortedLeft[node];
        sortedLeft[node] = sortedRight[pivot];
        sortedRight[pivot] = node;
        sortedRefresh(node);
        sortedRefresh(pivot);
        return pivot;
    }
    private int sortedBalance(int node) {
        sortedRefresh(node);
        int diff = sortedHeight(sortedLeft[node]) - sortedHeight(sortedRight[node]);
        if (diff > 1) {
            if (sortedHeight(sortedLeft[sortedLeft[node]])
                    < sortedHeight(sortedRight[sortedLeft[node]]))
                sortedLeft[node] = sortedRotateLeft(sortedLeft[node]);
            return sortedRotateRight(node);
        }
        if (diff < -1) {
            if (sortedHeight(sortedRight[sortedRight[node]])
                    < sortedHeight(sortedLeft[sortedRight[node]]))
                sortedRight[node] = sortedRotateRight(sortedRight[node]);
            return sortedRotateLeft(node);
        }
        return node;
    }
    private int sortedInsert(int root, int id) {
        if (root == 0) return id;
        if (sortedCompare(id, root) < 0)
            sortedLeft[root] = sortedInsert(sortedLeft[root], id);
        else sortedRight[root] = sortedInsert(sortedRight[root], id);
        return sortedBalance(root);
    }
    private int sortedDetachMinimum(int node) {
        if (sortedLeft[node] == 0) return sortedRight[node];
        sortedLeft[node] = sortedDetachMinimum(sortedLeft[node]);
        return sortedBalance(node);
    }
    private int sortedDelete(int root, int id) {
        if (root == 0) throw new IllegalStateException("missing sorted DOOP child");
        int cmp = sortedCompare(id, root);
        if (cmp < 0) sortedLeft[root] = sortedDelete(sortedLeft[root], id);
        else if (cmp > 0) sortedRight[root] = sortedDelete(sortedRight[root], id);
        else {
            int formerLeft = sortedLeft[root], formerRight = sortedRight[root];
            if (formerLeft == 0) return formerRight;
            if (formerRight == 0) return formerLeft;
            int successor = formerRight;
            while (sortedLeft[successor] != 0) successor = sortedLeft[successor];
            int remaining = sortedDetachMinimum(formerRight);
            sortedLeft[successor] = formerLeft;
            sortedRight[successor] = remaining;
            return sortedBalance(successor);
        }
        return sortedBalance(root);
    }
    private int visitSortedRanks(int node, int from, int end, long[] out, int write) {
        if (node == 0 || from >= end) return write;
        int before = sortedCount(sortedLeft[node]);
        if (from < before) write = visitSortedRanks(sortedLeft[node],
                from, Math.min(end, before), out, write);
        if (from <= before && before < end) out[write++] = node;
        if (end > before + 1) write = visitSortedRanks(sortedRight[node],
                Math.max(0, from - before - 1), end - before - 1, out, write);
        return write;
    }

    private int appendRight(int root, int newNode) {
        if (root == 0) return newNode;
        right[root] = appendRight(right[root], newNode);
        refresh(root);
        int difference = height(left[root]) - height(right[root]);
        if (difference < -1) {
            if (height(right[right[root]]) > height(left[right[root]])) {
                return rotateLeft(root);
            }
            right[root] = rotateRight(right[root]);
            return rotateLeft(root);
        }
        if (difference > 1) {
            if (height(right[left[root]]) > height(left[left[root]])) {
                left[root] = rotateLeft(left[root]);
            }
            return rotateRight(root);
        }
        return root;
    }

    private int rotateLeft(int node) {
        int pivot = right[node];
        right[node] = left[pivot];
        left[pivot] = node;
        refresh(node);
        refresh(pivot);
        return pivot;
    }

    private int rotateRight(int node) {
        int pivot = left[node];
        left[node] = right[pivot];
        right[pivot] = node;
        refresh(node);
        refresh(pivot);
        return pivot;
    }

    private void refresh(int node) {
        subtree[node] = Math.addExact(1, Math.addExact(count(left[node]), count(right[node])));
        height[node] = (byte) (1 + Math.max(height(left[node]), height(right[node])));
    }

    private int visitRanks(int node, int from, int end, long[] out, int write) {
        if (node == 0 || from >= end) return write;
        int leftCount = count(left[node]);
        if (from < leftCount) {
            write = visitRanks(left[node], from, Math.min(end, leftCount), out, write);
        }
        if (from <= leftCount && leftCount < end) out[write++] = node;
        if (end > leftCount + 1) {
            write = visitRanks(right[node], Math.max(0, from - leftCount - 1),
                    end - leftCount - 1, out, write);
        }
        return write;
    }


    /**
     * One AVL rank walk writes all three CPU-owned lanes, with no extra lookup,
     * boxing, element facade, row wrapper or intermediate index array.
     */
    private int visitRanksColumns(int node, int from, int end, long[] ids,
            long[] bits, short[] flagWords, int write) {
        if (node == 0 || from >= end) return write;
        int leftCount = count(left[node]);
        if (from < leftCount) {
            write = visitRanksColumns(left[node], from, Math.min(end, leftCount),
                    ids, bits, flagWords, write);
        }
        if (from <= leftCount && leftCount < end) {
            ids[write] = node;
            bits[write] = payload[node];
            flagWords[write++] = flags[node];
        }
        if (end > leftCount + 1) {
            write = visitRanksColumns(right[node], Math.max(0, from - leftCount - 1),
                    end - leftCount - 1, ids, bits, flagWords, write);
        }
        return write;
    }

    /** Growth is failure-atomic: fully prepare every primitive lane before publication. */
    private void ensureCapacity(int wantedId) {
        if (wantedId < payload.length) return;
        int minimum = Math.addExact(wantedId, 1);
        long geometric = Math.max((long) minimum, (long) payload.length * 3L / 2L + 1L);
        int next = Math.toIntExact(RuntimeHeap.chooseAmortizedCapacity(
                minimum, Math.min((long) Integer.MAX_VALUE - 1L, geometric), SLOT_BYTES));
        long[] nextPayload = Arrays.copyOf(payload, next);
        short[] nextFlags = Arrays.copyOf(flags, next);
        int[] nextOwner = Arrays.copyOf(owner, next);
        int[] nextRoot = Arrays.copyOf(childRoot, next);
        int[] nextLeft = Arrays.copyOf(left, next);
        int[] nextRight = Arrays.copyOf(right, next);
        int[] nextSubtree = Arrays.copyOf(subtree, next);
        byte[] nextHeight = Arrays.copyOf(height, next);
        long[] nextSortKeys = sortKeys == null ? null : Arrays.copyOf(sortKeys, next);
        int[] nextSortedRoot = sortedRoot == null ? null : Arrays.copyOf(sortedRoot, next);
        int[] nextSortedLeft = sortedLeft == null ? null : Arrays.copyOf(sortedLeft, next);
        int[] nextSortedRight = sortedRight == null ? null : Arrays.copyOf(sortedRight, next);
        int[] nextSortedSubtree = sortedSubtree == null ? null : Arrays.copyOf(sortedSubtree, next);
        byte[] nextSortedHeight = sortedHeight == null ? null : Arrays.copyOf(sortedHeight, next);
        byte[] nextSortedReady = sortedReady == null ? null : Arrays.copyOf(sortedReady, next);
        payload = nextPayload;
        flags = nextFlags;
        owner = nextOwner;
        childRoot = nextRoot;
        left = nextLeft;
        right = nextRight;
        subtree = nextSubtree;
        height = nextHeight;
        sortKeys = nextSortKeys;
        sortedRoot = nextSortedRoot;
        sortedLeft = nextSortedLeft;
        sortedRight = nextSortedRight;
        sortedSubtree = nextSortedSubtree;
        sortedHeight = nextSortedHeight;
        sortedReady = nextSortedReady;
    }
}
