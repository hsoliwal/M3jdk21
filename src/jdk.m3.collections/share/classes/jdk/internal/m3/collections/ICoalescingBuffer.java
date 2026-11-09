/* *****************************************************************************
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

import java.util.Collection;

/**
 * A bounded buffer that coalesces values with the same key.
 *
 * <p>
 * When a value is offered with a key that already exists in the buffer, the
 * existing value is replaced (coalesced) rather than creating a new entry. This
 * is ideal for scenarios where only the latest value matters.
 * </p>
 *
 * <h2>Use Cases</h2>
 * <ul>
 * <li>Market data streaming (stock prices: only latest price matters)</li>
 * <li>Sensor data aggregation (many readings, latest value wins)</li>
 * <li>Event deduplication (collapse duplicate events)</li>
 * <li>High-frequency updates with slow consumers</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 * <li>Compression ratio: 100-1000:1 typical in streaming scenarios</li>
 * <li>Lock-free concurrent access</li>
 * <li>Bounded capacity (no OOM)</li>
 * <li>GC-friendly (objects updated in-place)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * ICoalescingBuffer<String, StockPrice> buffer = new CoalescingRingBuffer<>(1024);
 *
 * // Producer: offer updates (same key coalesces)
 * buffer.offer("AAPL", new StockPrice(150.00)); // New entry
 * buffer.offer("AAPL", new StockPrice(150.50)); // Coalesces: replaces previous
 * buffer.offer("GOOG", new StockPrice(2800.00)); // New entry
 *
 * // Consumer: poll all available values
 * List<StockPrice> batch = new ArrayList<>();
 * int count = buffer.poll(batch); // Returns 2: AAPL (latest) and GOOG
 * }</pre>
 *
 * <h2>Inspired by</h2>
 *
 * @param <K>
 *            the key type (used for coalescing)
 * @param <V>
 *            the value type
 * @author Hitesh Soliwal
 * @version 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/ICoalescingBuffer.java}
 * blob {@code 104166861fee84e03489aaac7466bf2f82943fef} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.lazy batch 1 (eligible core contracts) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ICoalescingBuffer<K, V> {

    // --- CAPACITY OPERATIONS ---

    /**
     * Returns the current number of elements in the buffer.
     *
     * @return number of elements
     */
    int size();

    /**
     * Returns the maximum capacity of the buffer.
     *
     * @return maximum capacity
     */
    int capacity();

    /**
     * Checks if the buffer is empty.
     *
     * @return true if no elements
     */
    boolean isEmpty();

    /**
     * Checks if the buffer is full.
     *
     * @return true if at capacity
     */
    boolean isFull();

    // --- PRODUCER API ---

    /**
     * Offers a key-value pair to the buffer.
     *
     * <p>
     * If a value with the same key exists, it is replaced (coalesced). If buffer is
     * full and no coalescing possible, returns false.
     * </p>
     *
     * @param key
     *            the key for coalescing
     * @param value
     *            the value to store
     * @return true if accepted, false if buffer full
     */
    boolean offer(K key, V value);

    /**
     * Offers a value without coalescing (always creates new entry).
     *
     * <p>
     * Use for values that should never be coalesced.
     * </p>
     *
     * @param value
     *            the value to store
     * @return true if accepted, false if buffer full
     */
    boolean offer(V value);

    // --- CONSUMER API ---

    /**
     * Polls all available values into the collection.
     *
     * @param bucket
     *            collection to receive values
     * @return number of values polled
     */
    int poll(Collection<? super V> bucket);

    /**
     * Polls up to maxItems values into the collection.
     *
     * @param bucket
     *            collection to receive values
     * @param maxItems
     *            maximum items to poll
     * @return number of values polled
     */
    int poll(Collection<? super V> bucket, int maxItems);

    // --- METRICS ---

    /**
     * Returns the number of rejected offers (buffer was full).
     *
     * @return rejection count
     */
    long rejectionCount();
}
