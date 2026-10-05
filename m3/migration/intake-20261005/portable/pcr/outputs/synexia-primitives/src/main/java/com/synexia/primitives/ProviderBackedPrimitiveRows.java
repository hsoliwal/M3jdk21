package com.synexia.primitives;

import java.util.Arrays;
import java.util.Objects;

/**
 * Sparse primitive SoA rows that materialize through one callback on first touch.
 *
 * <p>This class is intentionally single-thread/external-synchronization. The reusable
 * scratch row is collection-owned and therefore creates no per-load row object.
 *
 * <p>One callback guard is shared with an owning tree. Provider callbacks may read
 * metadata and materialized rows, but may not demand another callback or mutate
 * this collection. A callback may use a different collection.
 */
public final class ProviderBackedPrimitiveRows {
    private final LazyPrimitiveSoaTable rows;
    private final LazyPrimitiveAddressSpace flags;
    private final PrimitiveRowProvider provider;
    private final PrimitiveTouchObserver touchObserver;
    private final long[] scratch;

    // Lazily allocated collection-owned batch scratch; never one object per logical row.
    private long[] prefetchIds;
    private long[] prefetchValues;
    private short[] prefetchFlags;
    private boolean callbackActive;

    public ProviderBackedPrimitiveRows(
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        this(
                LazyPrimitiveAddressSpace.DEFAULT_ROOT_BITS,
                LazyPrimitiveAddressSpace.DEFAULT_DIRECTORY_BITS,
                LazyPrimitiveAddressSpace.DEFAULT_LEAF_BITS,
                provider,
                laneKinds);
    }

    public ProviderBackedPrimitiveRows(
            int rootBits,
            int directoryBits,
            int leafBits,
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.touchObserver = provider instanceof PrimitiveTouchObserver observer ? observer : null;
        Objects.requireNonNull(laneKinds, "laneKinds");
        if (laneKinds.length == 0) throw new IllegalArgumentException("at least one lane required");
        rows = new LazyPrimitiveSoaTable(rootBits, directoryBits, leafBits, laneKinds);
        flags = new LazyPrimitiveAddressSpace(
                PrimitiveKind.SHORT, rootBits, directoryBits, leafBits);
        scratch = new long[laneKinds.length];
    }

    public int laneCount() { return rows.laneCount(); }
    public PrimitiveKind laneKind(int lane) { return rows.laneKind(lane); }
    public long logicalCapacity() { return rows.logicalCapacity(); }
    public long materializedSize() { return rows.size(); }
    public long allocatedPayloadBytes() {
        return rows.allocatedPayloadBytes() + flags.allocatedPayloadBytes();
    }
    public int allocatedFlagLeafCount() { return flags.allocatedLeafCount(); }

    public short flags(long rowId) {
        return (short) flags.getBits(rowId);
    }

    public boolean isMaterialized(long rowId) {
        return rows.contains(rowId)
                && LazyElementFlags.has(flags(rowId), LazyElementFlags.MATERIALIZED);
    }

    /**
     * Materialize once. Returns true only when the provider was invoked successfully.
     */
    public boolean touch(long rowId) {
        short current = flags(rowId);
        if (callbackActive
                && LazyElementFlags.has(current, LazyElementFlags.MATERIALIZED)
                && !LazyElementFlags.has(current, LazyElementFlags.INVALIDATED)) {
            // A cached read inside a callback is not another external logical touch.
            return false;
        }
        try {
            notifyBeforeTouch(rowId, current);

            if (LazyElementFlags.has(current, LazyElementFlags.MATERIALIZED)
                    && !LazyElementFlags.has(current, LazyElementFlags.INVALIDATED)) {
                notifyAfterTouch(rowId, current, false);
                return false;
            }
            if (LazyElementFlags.has(current, LazyElementFlags.LOADING)) {
                throw new IllegalStateException("REENTRANT_PROVIDER_LOAD:" + rowId);
            }
            if (LazyElementFlags.has(current, LazyElementFlags.FAILED)
                    && !LazyElementFlags.has(current, LazyElementFlags.INVALIDATED)) {
                throw new IllegalStateException(
                        "PROVIDER_LOAD_PREVIOUSLY_FAILED:" + rowId + "; call retry or invalidate");
            }

            short preserved = (short) (current & LazyElementFlags.LOAD_PRESERVE_MASK);
            short loaded;
            beginCallback();
            try {
                short loading = (short) (preserved
                        | LazyElementFlags.DISCOVERED
                        | LazyElementFlags.LOADING);
                writeFlags(rowId, loading);
                Arrays.fill(scratch, 0L);
                provider.beforeLoad(rowId, current);
                provider.load(rowId, scratch);
                rows.put(rowId, scratch);
                loaded = (short) (preserved
                        | LazyElementFlags.DISCOVERED
                        | LazyElementFlags.MATERIALIZED);
                writeFlags(rowId, loaded);
                provider.afterLoad(rowId, loaded);
            } catch (Exception failure) {
                rollbackLoad(rowId, preserved, failure);
                try {
                    provider.onLoadFailure(rowId, failure);
                } catch (RuntimeException callbackFailure) {
                    suppressDistinct(failure, callbackFailure);
                }
                throw new ProviderLoadException(rowId, failure);
            } catch (Error failure) {
                rollbackLoad(rowId, preserved, failure);
                throw failure;
            } finally {
                Arrays.fill(scratch, 0L);
                endCallback();
            }

            notifyAfterTouch(rowId, loaded, true);
            return true;
        } catch (RuntimeException failure) {
            notifyTouchFailure(rowId, flags(rowId), failure);
            throw failure;
        }
    }

    /** Explicit retry after a failed provider call. */
    public boolean retry(long rowId) {
        requireCallbackIdle();
        short current = flags(rowId);
        writeFlags(rowId, LazyElementFlags.remove(
                current, (short) (LazyElementFlags.FAILED | LazyElementFlags.INVALIDATED)));
        return touch(rowId);
    }

    public long getBits(int lane, long rowId) {
        touch(rowId);
        return rows.getBits(lane, rowId, 0L);
    }

    public void readRow(long rowId, long[] target) {
        touch(rowId);
        rows.readRow(rowId, target);
    }

    public void setBits(int lane, long rowId, long bits) {
        requireCallbackIdle();
        touch(rowId);
        rows.setBits(lane, rowId, bits);
        addFlags(rowId, LazyElementFlags.DIRTY);
    }

    /** Install a row without invoking the provider. */
    public boolean putMaterialized(long rowId, long... rowBits) {
        requireCallbackIdle();
        boolean added = rows.put(rowId, rowBits);
        short current = flags(rowId);
        short next = (short) ((current & LazyElementFlags.LOAD_PRESERVE_MASK)
                | LazyElementFlags.DISCOVERED
                | LazyElementFlags.MATERIALIZED);
        writeFlags(rowId, next);
        return added;
    }

    /**
     * Drop payload and mark stale. Relationship/state users may retain other flags.
     */
    public boolean invalidate(long rowId) {
        requireCallbackIdle();
        boolean removed = rows.remove(rowId);
        short current = flags(rowId);
        short next = LazyElementFlags.remove(
                current,
                (short) (LazyElementFlags.MATERIALIZED
                        | LazyElementFlags.LOADING
                        | LazyElementFlags.FAILED));
        next = LazyElementFlags.add(next,
                (short) (LazyElementFlags.DISCOVERED | LazyElementFlags.INVALIDATED));
        writeFlags(rowId, next);
        return removed;
    }

    /** Evicts materialized payload unless pinned. */
    public boolean evict(long rowId) {
        requireCallbackIdle();
        short current = flags(rowId);
        if (LazyElementFlags.has(current, LazyElementFlags.PINNED)) return false;
        return invalidate(rowId);
    }

    public void pin(long rowId, boolean value) {
        updateFlag(rowId, LazyElementFlags.PINNED, value);
    }

    public void markDirty(long rowId, boolean value) {
        updateFlag(rowId, LazyElementFlags.DIRTY, value);
    }

    public short applicationFlags(long rowId) {
        return (short) (flags(rowId) & LazyElementFlags.APPLICATION_MASK);
    }

    public void applicationFlags(long rowId, short applicationBits) {
        requireCallbackIdle();
        if ((applicationBits & ~LazyElementFlags.APPLICATION_MASK) != 0) {
            throw new IllegalArgumentException("application flag outside reserved high nibble");
        }
        short current = flags(rowId);
        writeFlags(rowId, (short) ((current & ~LazyElementFlags.APPLICATION_MASK)
                | applicationBits));
    }

    public void prefetch(long... rowIds) {
        Objects.requireNonNull(rowIds, "rowIds");
        for (long rowId : rowIds) touch(rowId);
    }

    /**
     * Materializes unresolved rows ahead of use without emitting logical touch-observer events.
     *
     * <p>The full request is validated and deduplicated before any LOADING state is published.
     * Successful publication is all-or-none for the requested unresolved rows.
     *
     * @return number of newly materialized rows
     */
    public int prefetchMaterialize(long[] rowIds, int from, int to) {
        Objects.requireNonNull(rowIds, "rowIds");
        if (from < 0 || from > to || to > rowIds.length) {
            throw new IndexOutOfBoundsException(from + ".." + to);
        }
        requireCallbackIdle();
        int requestCount = to - from;
        if (requestCount == 0) return 0;
        ensurePrefetchIdCapacity(requestCount);
        System.arraycopy(rowIds, from, prefetchIds, 0, requestCount);
        return prefetchPrepared(requestCount);
    }

    /**
     * Contiguous logical-id bulk prefetch. The count is intentionally int-bounded so reusable
     * batch scratch cannot silently become a multi-gigabyte allocation.
     */
    public int prefetchMaterialize(long firstRowId, int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        requireCallbackIdle();
        if (count == 0) return 0;
        long last = Math.addExact(firstRowId, (long) count - 1L);
        // Validate both ends before populating scratch; per-row validation still occurs below.
        flags(firstRowId);
        flags(last);
        ensurePrefetchIdCapacity(count);
        for (int index = 0; index < count; index++) {
            prefetchIds[index] = firstRowId + index;
        }
        return prefetchPrepared(count);
    }

    /** Reject callback reentry before a public operation changes collection state. */
    void requireCallbackIdle() {
        if (callbackActive) throw new IllegalStateException("REENTRANT_PROVIDER_CALLBACK");
    }

    /** Acquires the single synchronous callback scope used by rows and tree pages. */
    void beginCallback() {
        requireCallbackIdle();
        callbackActive = true;
    }

    void endCallback() {
        callbackActive = false;
    }

    void addFlags(long rowId, short bits) {
        writeFlags(rowId, LazyElementFlags.add(flags(rowId), bits));
    }

    void clearFlags(long rowId, short bits) {
        writeFlags(rowId, LazyElementFlags.remove(flags(rowId), bits));
    }

    void replaceFlags(long rowId, short next) {
        writeFlags(rowId, next);
    }

    private void updateFlag(long rowId, short bit, boolean value) {
        requireCallbackIdle();
        short current = flags(rowId);
        writeFlags(rowId, value
                ? LazyElementFlags.add(current, bit)
                : LazyElementFlags.remove(current, bit));
    }

    private void notifyBeforeTouch(long rowId, short currentFlags) {
        if (touchObserver == null) return;
        beginCallback();
        try {
            touchObserver.beforeTouch(rowId, currentFlags);
        } catch (Exception failure) {
            throw new ProviderLoadException(rowId, failure);
        } finally {
            endCallback();
        }
    }

    private void notifyAfterTouch(long rowId, short resultingFlags, boolean materializedNow) {
        if (touchObserver == null) return;
        beginCallback();
        try {
            touchObserver.afterTouch(rowId, resultingFlags, materializedNow);
        } catch (Exception failure) {
            throw new ProviderLoadException(rowId, failure);
        } finally {
            endCallback();
        }
    }

    private void notifyTouchFailure(long rowId, short state, RuntimeException failure) {
        if (touchObserver == null) return;
        Exception observed = failure;
        if (failure instanceof ProviderLoadException providerFailure
                && providerFailure.getCause() instanceof Exception cause) {
            observed = cause;
        }
        beginCallback();
        try {
            try {
                touchObserver.onTouchFailure(rowId, state, observed);
            } catch (RuntimeException observerFailure) {
                suppressDistinct(failure, observerFailure);
            }
        } finally {
            endCallback();
        }
    }

    private int prefetchPrepared(int requestCount) {
        Arrays.sort(prefetchIds, 0, requestCount);
        ensurePrefetchFlagCapacity(requestCount);

        int unresolved = 0;
        long previousId = Long.MIN_VALUE;
        boolean havePrevious = false;
        for (int index = 0; index < requestCount; index++) {
            long rowId = prefetchIds[index];
            if (havePrevious && rowId == previousId) continue;
            havePrevious = true;
            previousId = rowId;

            short current = flags(rowId); // validates the logical id
            if (LazyElementFlags.has(current, LazyElementFlags.MATERIALIZED)
                    && !LazyElementFlags.has(current, LazyElementFlags.INVALIDATED)) {
                continue;
            }
            if (LazyElementFlags.has(current, LazyElementFlags.LOADING)) {
                throw new IllegalStateException("PROVIDER_LOAD_IN_PROGRESS:" + rowId);
            }
            if (LazyElementFlags.has(current, LazyElementFlags.FAILED)
                    && !LazyElementFlags.has(current, LazyElementFlags.INVALIDATED)) {
                throw new IllegalStateException(
                        "PROVIDER_LOAD_PREVIOUSLY_FAILED:" + rowId + "; call retry or invalidate");
            }
            prefetchIds[unresolved] = rowId;
            prefetchFlags[unresolved] = current;
            unresolved++;
        }
        if (unresolved == 0) return 0;

        int laneCount = laneCount();
        int valueCount = Math.multiplyExact(unresolved, laneCount);
        ensurePrefetchValueCapacity(valueCount);
        Arrays.fill(prefetchValues, 0, valueCount, 0L);

        for (int index = 0; index < unresolved; index++) {
            short preserved = (short) (prefetchFlags[index] & LazyElementFlags.LOAD_PRESERVE_MASK);
            writeFlags(
                    prefetchIds[index],
                    (short) (preserved | LazyElementFlags.DISCOVERED | LazyElementFlags.LOADING));
        }

        beginCallback();
        try {
            for (int index = 0; index < unresolved; index++) {
                provider.beforeLoad(prefetchIds[index], prefetchFlags[index]);
            }

            if (provider instanceof BulkPrimitiveRowProvider bulk) {
                bulk.loadRows(
                        prefetchIds,
                        0,
                        unresolved,
                        laneCount,
                        scratch,
                        prefetchValues);
            } else {
                int targetOffset = 0;
                for (int index = 0; index < unresolved; index++) {
                    Arrays.fill(scratch, 0L);
                    provider.load(prefetchIds[index], scratch);
                    System.arraycopy(scratch, 0, prefetchValues, targetOffset, laneCount);
                    targetOffset += laneCount;
                }
            }

            for (int index = 0; index < unresolved; index++) {
                rows.putFrom(prefetchIds[index], prefetchValues, index * laneCount);
            }
            for (int index = 0; index < unresolved; index++) {
                short loaded = (short) ((prefetchFlags[index] & LazyElementFlags.LOAD_PRESERVE_MASK)
                        | LazyElementFlags.DISCOVERED
                        | LazyElementFlags.MATERIALIZED);
                writeFlags(prefetchIds[index], loaded);
            }
            for (int index = 0; index < unresolved; index++) {
                provider.afterLoad(prefetchIds[index], flags(prefetchIds[index]));
            }
            return unresolved;
        } catch (Exception failure) {
            rollbackPrefetch(unresolved, failure);
            for (int index = 0; index < unresolved; index++) {
                try {
                    provider.onLoadFailure(prefetchIds[index], failure);
                } catch (RuntimeException callbackFailure) {
                    suppressDistinct(failure, callbackFailure);
                }
            }
            throw new ProviderLoadException(prefetchIds[0], failure);
        } catch (Error failure) {
            rollbackPrefetch(unresolved, failure);
            throw failure;
        } finally {
            Arrays.fill(scratch, 0L);
            Arrays.fill(prefetchValues, 0, valueCount, 0L);
            Arrays.fill(prefetchIds, 0, unresolved, 0L);
            Arrays.fill(prefetchFlags, 0, unresolved, (short) 0);
            endCallback();
        }
    }

    private void rollbackPrefetch(int count, Throwable failure) {
        for (int index = 0; index < count; index++) {
            long rowId = prefetchIds[index];
            try {
                rows.remove(rowId);
            } catch (RuntimeException | Error cleanupFailure) {
                suppressDistinct(failure, cleanupFailure);
            }
            short failed = (short) ((prefetchFlags[index] & LazyElementFlags.LOAD_PRESERVE_MASK)
                    | LazyElementFlags.DISCOVERED
                    | LazyElementFlags.FAILED);
            try {
                writeFlags(rowId, failed);
            } catch (RuntimeException | Error cleanupFailure) {
                suppressDistinct(failure, cleanupFailure);
            }
        }
    }

    private void ensurePrefetchIdCapacity(int required) {
        if (prefetchIds != null && prefetchIds.length >= required) return;
        prefetchIds = new long[grow(prefetchIds == null ? 0 : prefetchIds.length, required)];
    }

    private void ensurePrefetchFlagCapacity(int required) {
        if (prefetchFlags != null && prefetchFlags.length >= required) return;
        prefetchFlags = new short[grow(prefetchFlags == null ? 0 : prefetchFlags.length, required)];
    }

    private void ensurePrefetchValueCapacity(int required) {
        if (prefetchValues != null && prefetchValues.length >= required) return;
        prefetchValues = new long[grow(prefetchValues == null ? 0 : prefetchValues.length, required)];
    }

    private static int grow(int current, int required) {
        if (required < 0) throw new IllegalArgumentException("required < 0");
        int next = Math.max(8, current);
        while (next < required) {
            int grown = next + (next >>> 1) + 1;
            if (grown < 0 || grown < next) return required;
            next = grown;
        }
        return next;
    }

    private void rollbackLoad(long rowId, short preserved, Throwable failure) {
        try {
            rows.remove(rowId);
        } catch (RuntimeException | Error cleanupFailure) {
            suppressDistinct(failure, cleanupFailure);
        }
        short failed = (short) (preserved
                | LazyElementFlags.DISCOVERED
                | LazyElementFlags.FAILED);
        try {
            writeFlags(rowId, failed);
        } catch (RuntimeException | Error cleanupFailure) {
            suppressDistinct(failure, cleanupFailure);
        }
    }

    private static void suppressDistinct(Throwable failure, Throwable secondary) {
        if (secondary != failure) failure.addSuppressed(secondary);
    }

    private void writeFlags(long rowId, short state) {
        flags.setBits(rowId, state);
    }
}
