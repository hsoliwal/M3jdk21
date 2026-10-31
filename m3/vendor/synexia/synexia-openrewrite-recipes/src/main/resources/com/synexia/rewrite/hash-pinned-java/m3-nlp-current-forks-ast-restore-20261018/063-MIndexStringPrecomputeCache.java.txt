// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Primitive weighted LRU for heavy MIndexString precompute images.
 *
 * <p>Content IDs index lazily allocated pages directly; there is no boxed-key HashMap on the hot
 * lookup path.</p>
 */
public final class MIndexStringPrecomputeCache {
    public static final long DEFAULT_BUDGET_BYTES = 64L << 20;
    private static final int PAGE_SHIFT = 10;
    private static final int PAGE_SIZE = 1 << PAGE_SHIFT;
    private static final int PAGE_MASK = PAGE_SIZE - 1;

    private MIndexStringPrecomputeImage[][] images = new MIndexStringPrecomputeImage[4][];
    private int[][] previous = new int[4][];
    private int[][] next = new int[4][];
    private long[][] weights = new long[4][];
    private long budgetBytes;
    private long retainedBytes;
    private int retainedEntries;
    private int head = -1;
    private int tail = -1;

    public MIndexStringPrecomputeCache() { this(DEFAULT_BUDGET_BYTES); }

    public MIndexStringPrecomputeCache(long budgetBytes) {
        setBudgetBytes(budgetBytes);
    }

    public long budgetBytes() { synchronized (this) { return budgetBytes; } }
    public long retainedBytes() { synchronized (this) { return retainedBytes; } }
    public int retainedEntries() { synchronized (this) { return retainedEntries; } }

    public void setBudgetBytes(long bytes) {
        if (bytes < 0) throw new IllegalArgumentException("negative cache budget");
        synchronized (this) {
            budgetBytes = bytes;
            evictToBudget();
        }
    }

    public MIndexStringPrecomputeImage get(
            MIndexString value, MIndexStringPrecomputeProfile profile) {
        MIndexString canonical = Objects.requireNonNull(value, "value").intern();
        MIndexStringPrecomputeProfile wanted = Objects.requireNonNull(profile, "profile");
        int id = canonical.contentIndex();

        MIndexStringPrecomputeImage current;
        synchronized (this) {
            current = image(id);
            if (current != null && current.profile().includes(wanted)) {
                touch(id);
                return current;
            }
        }

        MIndexStringPrecomputeImage compiled = current == null
                ? MIndexStringPrecomputeImage.compile(canonical, wanted) : current.extend(wanted);
        // Cold work already happened: sample the *full* image peak outside the cache lock.
        MIndexHeapAdmission.Snapshot heap =
                MIndexHeapAdmission.current(compiled.estimatedBytes());

        // A racing upgrade can add at most seven profile bits. Never construct an image while
        // holding the cache monitor: unrelated hits, eviction and budget changes must stay cheap.
        for (;;) {
            MIndexStringPrecomputeProfile missing;
            synchronized (this) {
                MIndexStringPrecomputeImage raced = image(id);
                if (raced != null && raced.profile().includes(wanted)) {
                    touch(id);
                    return raced;
                }
                if (raced == null || compiled.profile().includes(raced.profile())) {
                    replace(id, compiled, heap);
                    return compiled;
                }
                missing = raced.profile();
            }
            compiled = compiled.extend(missing);
            heap = MIndexHeapAdmission.current(compiled.estimatedBytes());
        }
    }

    public synchronized MIndexStringPrecomputeImage peek(int contentIndex) {
        if (contentIndex < 0) return null;
        return image(contentIndex);
    }

    public synchronized void clear() {
        images = new MIndexStringPrecomputeImage[4][];
        previous = new int[4][];
        next = new int[4][];
        weights = new long[4][];
        retainedBytes = 0L;
        retainedEntries = 0;
        head = -1;
        tail = -1;
    }

    private void replace(int id, MIndexStringPrecomputeImage image,
            MIndexHeapAdmission.Snapshot heap) {
        long activeBudget = MIndexPrecomputePressureBudget.activeBudget(budgetBytes, heap);
        evictToBudget(activeBudget);
        long weight = image.estimatedBytes();
        // Preserve an affordable older precompute profile when an upgrade cannot remain hot.
        if (activeBudget == 0 || weight > activeBudget) return;
        MIndexStringPrecomputeImage old = image(id);
        if (old != null) remove(id);

        ensurePage(id);
        int page = id >>> PAGE_SHIFT;
        int offset = id & PAGE_MASK;
        images[page][offset] = image;
        weights[page][offset] = weight;
        previous[page][offset] = tail;
        next[page][offset] = -1;
        if (tail >= 0) setNext(tail, id);
        else head = id;
        tail = id;
        retainedBytes += weight;
        retainedEntries++;
        evictToBudget(activeBudget);
    }

    private void evictToBudget() {
        evictToBudget(budgetBytes);
    }

    private void evictToBudget(long ceiling) {
        while (retainedBytes > ceiling && head >= 0) remove(head);
    }

    private void touch(int id) {
        if (tail == id) return;
        int prev = previous(id);
        int nxt = next(id);
        if (prev >= 0) setNext(prev, nxt);
        else head = nxt;
        if (nxt >= 0) setPrevious(nxt, prev);
        previous(id, tail);
        next(id, -1);
        if (tail >= 0) setNext(tail, id);
        tail = id;
        if (head < 0) head = id;
    }

    private void remove(int id) {
        MIndexStringPrecomputeImage existing = image(id);
        if (existing == null) return;
        int prev = previous(id);
        int nxt = next(id);
        if (prev >= 0) setNext(prev, nxt);
        else head = nxt;
        if (nxt >= 0) setPrevious(nxt, prev);
        else tail = prev;

        int page = id >>> PAGE_SHIFT;
        int offset = id & PAGE_MASK;
        retainedBytes -= weights[page][offset];
        retainedEntries--;
        images[page][offset] = null;
        weights[page][offset] = 0L;
        previous[page][offset] = -1;
        next[page][offset] = -1;
    }

    private MIndexStringPrecomputeImage image(int id) {
        int page = id >>> PAGE_SHIFT;
        return page < images.length && images[page] != null
                ? images[page][id & PAGE_MASK] : null;
    }

    private int previous(int id) {
        int page = id >>> PAGE_SHIFT;
        return previous[page][id & PAGE_MASK];
    }

    private int next(int id) {
        int page = id >>> PAGE_SHIFT;
        return next[page][id & PAGE_MASK];
    }

    private void previous(int id, int value) {
        previous[id >>> PAGE_SHIFT][id & PAGE_MASK] = value;
    }

    private void next(int id, int value) {
        next[id >>> PAGE_SHIFT][id & PAGE_MASK] = value;
    }

    private void setPrevious(int id, int value) { previous(id, value); }
    private void setNext(int id, int value) { next(id, value); }

    private void ensurePage(int id) {
        int page = id >>> PAGE_SHIFT;
        if (page >= images.length) {
            int length = images.length;
            while (length <= page) length = Math.multiplyExact(length, 2);
            images = Arrays.copyOf(images, length);
            previous = Arrays.copyOf(previous, length);
            next = Arrays.copyOf(next, length);
            weights = Arrays.copyOf(weights, length);
        }
        if (images[page] == null) {
            images[page] = new MIndexStringPrecomputeImage[PAGE_SIZE];
            previous[page] = new int[PAGE_SIZE];
            next[page] = new int[PAGE_SIZE];
            weights[page] = new long[PAGE_SIZE];
            Arrays.fill(previous[page], -1);
            Arrays.fill(next[page], -1);
        }
    }
}
