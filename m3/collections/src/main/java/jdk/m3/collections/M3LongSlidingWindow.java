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
package jdk.m3.collections;

import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

/**
 * Fixed-capacity rolling long window with O(1) append/eviction and running sum.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongSlidingWindow.java}
 * blob {@code e7cd6dcb5ecdf4010df9fd676502fb14cc230bb0} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3LongSlidingWindow {
    private final long[] values;
    private int head;
    private int size;
    private long sum;
    // Optional extrema state is activated only by min()/max(). No extra
    // per-element lane or JNI resource is retained; add() remains O(1).
    private long cachedMin;
    private long cachedMax;
    private boolean minKnown;
    private boolean maxKnown;

    public M3LongSlidingWindow(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity <= 0");
        values = new long[capacity];
    }

    public int capacity() { return values.length; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public boolean isFull() { return size == values.length; }
    public long sum() { return sum; }
    public double average() { return size == 0 ? Double.NaN : sum / (double) size; }
    public long payloadBytes() { return (long) values.length * Long.BYTES; }

    /**
     * Appends a value and returns the evicted oldest value, or defaultValue while
     * the window is still filling.
     */
    public long add(long value, long defaultValue) {
        if (size < values.length) {
            int toEnd = values.length - head;
            int tail = size >= toEnd ? size - toEnd : head + size;
            values[tail] = value;
            size++;
            sum += value;
            if (minKnown) cachedMin = Math.min(cachedMin, value);
            if (maxKnown) cachedMax = Math.max(cachedMax, value);
            return defaultValue;
        }
        long evicted = values[head];
        values[head] = value;
        head = head + 1 == values.length ? 0 : head + 1;
        sum += value - evicted;
        // A removed extremum might have been the unique copy. Invalidate
        // only when the incoming value does not itself certify the bound.
        if (minKnown) {
            if (evicted == cachedMin && value > cachedMin) minKnown = false;
            else cachedMin = Math.min(cachedMin, value);
        }
        if (maxKnown) {
            if (evicted == cachedMax && value < cachedMax) maxKnown = false;
            else cachedMax = Math.max(cachedMax, value);
        }
        return evicted;
    }

    public long oldest() {
        if (size == 0) throw new NoSuchElementException();
        return values[head];
    }

    public long newest() {
        if (size == 0) throw new NoSuchElementException();
        int index = size - 1;
        int toEnd = values.length - head;
        return values[index >= toEnd ? index - toEnd : head + index];
    }

    public long getFromOldest(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        int toEnd = values.length - head;
        return values[index >= toEnd ? index - toEnd : head + index];
    }

    public long min() {
        if (size == 0) throw new NoSuchElementException();
        if (!minKnown) refreshExtrema();
        return cachedMin;
    }

    public long max() {
        if (size == 0) throw new NoSuchElementException();
        if (!maxKnown) refreshExtrema();
        return cachedMax;
    }

    /** One ring pass repairs both extrema, sharing work after expiration. */
    private void refreshExtrema() {
        long minimum = values[head];
        long maximum = minimum;
        int position = head;
        for (int remaining = size - 1; remaining > 0; remaining--) {
            if (++position == values.length) position = 0;
            long value = values[position];
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        cachedMin = minimum;
        cachedMax = maximum;
        minKnown = true;
        maxKnown = true;
    }

    public void forEach(LongConsumer consumer) {
        for (int i = 0; i < size; i++) consumer.accept(getFromOldest(i));
    }

    public void clear() {
        M3RingLiveClear.clear(values, head, size);
        head = 0;
        size = 0;
        sum = 0L;
        cachedMin = 0L;
        cachedMax = 0L;
        minKnown = false;
        maxKnown = false;
    }
}
