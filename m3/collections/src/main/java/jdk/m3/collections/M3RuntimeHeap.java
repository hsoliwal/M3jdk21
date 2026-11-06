/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
// Modified 2026: M3JDK optional collection projection; source contracts preserved.
package jdk.m3.collections;

/**
 * JVM heap pressure observations for M3-owned optional storage and growth.
 * This is advisory: the JVM remains the sole allocator/GC authority, and live
 * Java objects are never swapped solely because this utility reports pressure.
 * Call at growth and bounded residency checkpoints, not on every add/get.
 */
public final class M3RuntimeHeap {
    public enum Pressure { NORMAL, WARM, HIGH }

    public record Snapshot(long maximumBytes, long committedBytes, long usedBytes) {
        public Snapshot {
            if (maximumBytes <= 0 || committedBytes < 0 || usedBytes < 0
                    || usedBytes > committedBytes) {
                throw new IllegalArgumentException("invalid heap snapshot");
            }
        }

        public long freeToMaximum() {
            return Math.max(0L, maximumBytes - usedBytes);
        }

        public long safetyReserve() {
            // Reserve 1/32 of Xmx, bounded to 1..32 MiB when affordable.
            return Math.min(maximumBytes / 32L, 32L << 20);
        }

        /** Pending bytes are peak *new* backing size while the old array remains live. */
        public Pressure pressure(long pendingPeakBytes) {
            if (pendingPeakBytes < 0) throw new IllegalArgumentException("negative peak");
            if (maximumBytes == Long.MAX_VALUE) return Pressure.NORMAL; // JVM-reported unknown.
            long projected = saturatingPlus(saturatingPlus(usedBytes, pendingPeakBytes),
                    safetyReserve());
            if (projected >= percent(maximumBytes, 85)) return Pressure.HIGH;
            if (projected >= percent(maximumBytes, 70)) return Pressure.WARM;
            return Pressure.NORMAL;
        }
    }

    private M3RuntimeHeap() { }

    public static Snapshot current() {
        Runtime runtime = Runtime.getRuntime();
        long committed = runtime.totalMemory();
        long free = runtime.freeMemory();
        return new Snapshot(runtime.maxMemory(), committed,
                Math.max(0L, committed - Math.max(0L, free)));
    }

    /** Pure, deterministic growth policy. Never under-allocates the required capacity. */
    public static long chooseCapacity(Snapshot heap, long required, long preferred,
                                      int bytesPerElement) {
        if (heap == null) throw new NullPointerException("heap");
        if (required < 0 || preferred < required || bytesPerElement < 1) {
            throw new IllegalArgumentException("invalid growth request");
        }
        if (heap.pressure(saturatingMultiply(preferred, bytesPerElement)) == Pressure.NORMAL) {
            return preferred;
        }
        return required;
    }

    /** Called only when the backing array must grow. */
    public static long chooseCapacity(long required, long preferred, int bytesPerElement) {
        return chooseCapacity(current(), required, preferred, bytesPerElement);
    }

    /**
     * Bounded amortization for append-heavy M3 collections under pressure.
     *
     * <p>Exact-size growth on every append makes copying a partially filled
     * segment (or four facade metadata lanes) quadratic. At a real growth
     * boundary we may retain at most one small 4 KiB quantum of slack, but
     * only when the projected <em>full new allocation</em> plus reserve fits
     * below Xmx. If no space is indicated, return the exact required length;
     * this is advisory, not an allocation guarantee. Normal pressure retains
     * the original geometric policy. A single wide element never triggers
     * a gratuitous oversized allocation.</p>
     */
    public static long chooseAmortizedCapacity(Snapshot heap, long required,
            long preferred, int bytesPerElement) {
        long baseline = chooseCapacity(heap, required, preferred, bytesPerElement);
        if (baseline != required || required == preferred
                || heap.maximumBytes() == Long.MAX_VALUE) {
            return baseline;
        }
        long quantum = 4096L / bytesPerElement;
        if (quantum == 0L) return required;
        long freeAfterReserve = Math.max(0L,
                heap.freeToMaximum() - heap.safetyReserve());
        long affordableElements = freeAfterReserve / bytesPerElement;
        if (affordableElements <= required) return required;
        long surplus = Math.min(quantum,
                Math.min(preferred - required, affordableElements - required));
        return required + surplus;
    }

    /** Sample JVM state only at backing growth, never for an ordinary append. */
    public static long chooseAmortizedCapacity(long required, long preferred,
            int bytesPerElement) {
        return chooseAmortizedCapacity(current(), required, preferred, bytesPerElement);
    }


    /**
     * Select the capacity of an already-owned segmented primitive array from
     * its incremental allocation cost. Whole existing pages are reused;
     * only the replacement tail, new pages and directory consume new heap.
     *
     * <p>Returns at least required, at most preferred, and retains a bounded
     * 4KiB..1MiB burst under moderate or high advisory heap pressure. This
     * changes no allocator, JNI ABI, public collection contract or GC behavior.
     * It is called only at a true growth boundary, never per inserted element.
     */
    static long chooseSegmentedAmortizedCapacity(Snapshot heap, long current,
            long required, long preferred, int bytesPerElement, int segmentShift) {
        if (heap == null) throw new NullPointerException("heap");
        checkSegmented(current, required, bytesPerElement, segmentShift);
        if (preferred < required) throw new IllegalArgumentException("preferred < required");
        long maximumLength = (long) Integer.MAX_VALUE << segmentShift;
        if (required > maximumLength) throw new IllegalArgumentException("array too large");
        preferred = Math.min(preferred, maximumLength);
        if (preferred == required) return required;
        if (heap.maximumBytes() == Long.MAX_VALUE) return preferred;

        long budget = Math.max(0L, heap.freeToMaximum() - heap.safetyReserve());
        long desiredBytes = additionalSegmentedBytes(current, preferred,
                bytesPerElement, segmentShift);
        if (desiredBytes <= budget && heap.pressure(desiredBytes) == Pressure.NORMAL) {
            return preferred;
        }
        // Never reject necessary growth because of an advisory heap estimate.
        if (additionalSegmentedBytes(current, required,
                bytesPerElement, segmentShift) > budget) return required;

        long existingBytes = saturatingMultiply(current, bytesPerElement);
        long burstBytes = Math.max(4096L, Math.min(1L << 20, existingBytes >>> 4));
        long burstElements = Math.max(1L, burstBytes / bytesPerElement);
        long upper = required + Math.min(preferred - required, burstElements);
        if (additionalSegmentedBytes(current, upper, bytesPerElement, segmentShift) <= budget) {
            return upper;
        }
        // Logarithmic cold-path budget selection; all hot array reads remain unchanged.
        long lower = required;
        while (lower < upper) {
            long middle = lower + ((upper - lower + 1L) >>> 1);
            if (additionalSegmentedBytes(current, middle, bytesPerElement, segmentShift)
                    <= budget) {
                lower = middle;
            } else {
                upper = middle - 1L;
            }
        }
        return lower;
    }

    /**
     * Conservative additional heap bytes while old pages remain live:
     * replacement tail + newly allocated pages + fresh directory.
     * A reference is budgeted at 8 bytes, plus approximate object headers;
     * neither estimate is a JVM object-layout or allocation guarantee.
     */
    static long additionalSegmentedBytes(long current, long candidate,
            int bytesPerElement, int segmentShift) {
        checkSegmented(current, candidate, bytesPerElement, segmentShift);
        long maximumLength = (long) Integer.MAX_VALUE << segmentShift;
        if (candidate > maximumLength) throw new IllegalArgumentException("array too large");
        if (candidate == current) return 0L;
        long oldPages = pageCount(current, segmentShift);
        long newPages = pageCount(candidate, segmentShift);
        long pageSize = 1L << segmentShift;
        long additionalElements;
        long newPayloadArrays;
        if (oldPages == newPages) {
            additionalElements = candidate - ((oldPages - 1L) << segmentShift);
            newPayloadArrays = 1L;
        } else {
            long oldRemainder = current & (pageSize - 1L);
            long replacement = oldPages != 0L && oldRemainder != 0L ? pageSize : 0L;
            additionalElements = saturatingPlus(replacement,
                    candidate - (oldPages << segmentShift));
            newPayloadArrays = newPages - oldPages + (replacement != 0L ? 1L : 0L);
        }
        long payload = saturatingMultiply(additionalElements, bytesPerElement);
        long directory = saturatingPlus(32L, saturatingMultiply(newPages, 8L));
        long headers = saturatingMultiply(newPayloadArrays, 24L);
        return saturatingPlus(payload, saturatingPlus(directory, headers));
    }

    private static void checkSegmented(long current, long desired,
            int bytesPerElement, int segmentShift) {
        if (current < 0L || desired < current || bytesPerElement < 1
                || segmentShift < 10 || segmentShift > 26) {
            throw new IllegalArgumentException("invalid segmented growth request");
        }
    }

    private static long pageCount(long elements, int shift) {
        return elements == 0L ? 0L : ((elements - 1L) >>> shift) + 1L;
    }

    private static long percent(long value, int p) {
        return (value / 100L) * p + ((value % 100L) * p) / 100L;
    }

    private static long saturatingMultiply(long left, long right) {
        if (left == 0L || right == 0L) return 0L;
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static long saturatingPlus(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
