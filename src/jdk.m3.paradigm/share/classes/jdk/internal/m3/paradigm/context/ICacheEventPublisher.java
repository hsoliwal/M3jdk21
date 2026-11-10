/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import jdk.internal.m3.collections.PlaceholderLogger;

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * ICacheEventPublisher - Observable cache operation events.
 *
 * <h2>Architecture</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  CACHE EVENT PUBLISHER                                                      │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │                                                                             │
 * │  ICacheEventPublisher extends Flow.Publisher&lt;CacheEvent&gt;                   │
 * │                                                                             │
 * │  DUAL API PATTERN:                                                          │
 * │    Pattern 1: publisher.onEvent(e -&gt; metrics(e))  // Simple callback       │
 * │    Pattern 2: publisher.subscribe(subscriber)     // JDK Flow API           │
 * │                                                                             │
 * │  Event Types:                                                               │
 * │    HIT                - Cache lookup found entry                            │
 * │    MISS               - Cache lookup did not find entry                     │
 * │    EVICTION           - Entry evicted from cache                            │
 * │    THRESHOLD_EXCEEDED - Cache size threshold exceeded                       │
 * │    CLEAR              - Cache cleared                                       │
 * │                                                                             │
 * │  Purpose:                                                                   │
 * │    - Cache performance monitoring                                           │
 * │    - Hit/miss ratio tracking                                                │
 * │    - Eviction pressure analysis                                             │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get or create cache event publisher
 * ICacheEventPublisher publisher = ICacheEventPublisher.create();
 *
 * // Simple callback - track hit/miss ratio
 * publisher.onHit(event -> metrics.incrementCacheHit(event.key()));
 * publisher.onMiss(event -> metrics.incrementCacheMiss(event.key()));
 *
 * // Monitor eviction pressure
 * publisher.onEvent(CacheEventType.THRESHOLD_EXCEEDED, event -> {
 *     log.warn("Cache threshold exceeded: size={}, threshold={}",
 *              event.currentSize(), event.threshold());
 * });
 *
 * // Reactive Streams - JDK Flow API
 * publisher.subscribe(new Flow.Subscriber<>() {
 *     public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
 *     public void onNext(CacheEvent e) { dashboard.update(e); }
 *     public void onError(Throwable t) { alerting.notify(t); }
 *     public void onComplete() { }
 * });
 * }</pre>
 *
 * @since ContextOS 1.0 - Observable Memory System
 * @see java.util.concurrent.Flow
 */
@ISpec(
    value = "Observable Cache Events - hit/miss/eviction events with Flow.Publisher",
    constraints = {
        "Dual API: simple callback and JDK Flow API",
        "Event types: HIT, MISS, EVICTION, THRESHOLD_EXCEEDED, CLEAR",
        "Purpose: cache performance monitoring and hit/miss ratio tracking"
    },
    invariants = {
        "create() returns new publisher instance",
        "onHit/onMiss register typed callbacks",
        "publish(event) notifies all subscribers"
    },
    examples = {
        "ICacheEventPublisher.create()",
        "publisher.onHit(e -> metrics.incrementHit())",
        "publisher.onMiss(e -> metrics.incrementMiss())"
    },
    model = Model.ModelType.HAIKU,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/ICacheEventPublisher.java}
 * blob {@code 5497a305edec30a3945116474a15f53a94169d00} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ICacheEventPublisher extends Flow.Publisher<ICacheEventPublisher.CacheEvent> {
    PlaceholderLogger log = PlaceholderLogger.of(ICacheEventPublisher.class);

    /** Cache event types. */
    enum CacheEventType {
        /** Cache lookup found entry. */
        HIT,
        /** Cache lookup did not find entry. */
        MISS,
        /** Entry evicted from cache. */
        EVICTION,
        /** Cache size threshold exceeded. */
        THRESHOLD_EXCEEDED,
        /** Cache cleared. */
        CLEAR
    }

    /**
     * Cache event record - immutable snapshot of a cache operation.
     *
     * @param type        the event type
     * @param key         the cache key (nullable for CLEAR and THRESHOLD_EXCEEDED)
     * @param currentSize the current cache size
     * @param threshold   the cache size threshold (for THRESHOLD_EXCEEDED)
     * @param timestamp   when the event occurred
     */
    record CacheEvent(
            CacheEventType type,
            String key,
            int currentSize,
            int threshold,
            long timestamp
    ) {
        /**
         * Create a HIT event.
         *
         * @param key         the cache key
         * @param currentSize the current cache size
         * @return new event
         */
        public static CacheEvent hit(String key, int currentSize) {
            return new CacheEvent(
                    CacheEventType.HIT,
                    key,
                    currentSize,
                    0,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create a MISS event.
         *
         * @param key         the cache key
         * @param currentSize the current cache size
         * @return new event
         */
        public static CacheEvent miss(String key, int currentSize) {
            return new CacheEvent(
                    CacheEventType.MISS,
                    key,
                    currentSize,
                    0,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create an EVICTION event.
         *
         * @param key         the evicted key
         * @param currentSize the current cache size after eviction
         * @return new event
         */
        public static CacheEvent eviction(String key, int currentSize) {
            return new CacheEvent(
                    CacheEventType.EVICTION,
                    key,
                    currentSize,
                    0,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create a THRESHOLD_EXCEEDED event.
         *
         * @param currentSize the current cache size
         * @param threshold   the threshold that was exceeded
         * @return new event
         */
        public static CacheEvent thresholdExceeded(int currentSize, int threshold) {
            return new CacheEvent(
                    CacheEventType.THRESHOLD_EXCEEDED,
                    null,
                    currentSize,
                    threshold,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create a CLEAR event.
         *
         * @param previousSize the size before clearing
         * @return new event
         */
        public static CacheEvent clear(int previousSize) {
            return new CacheEvent(
                    CacheEventType.CLEAR,
                    null,
                    0,
                    previousSize,
                    System.currentTimeMillis()
            );
        }

        /**
         * Check if this is a successful lookup (HIT).
         *
         * @return true if HIT
         */
        public boolean isHit() {
            return type == CacheEventType.HIT;
        }

        /**
         * Check if this is a failed lookup (MISS).
         *
         * @return true if MISS
         */
        public boolean isMiss() {
            return type == CacheEventType.MISS;
        }

        /**
         * Check if this indicates memory pressure (EVICTION or THRESHOLD_EXCEEDED).
         *
         * @return true if memory pressure
         */
        public boolean isMemoryPressure() {
            return type == CacheEventType.EVICTION || type == CacheEventType.THRESHOLD_EXCEEDED;
        }
    }

    // --- SIMPLE CALLBACK API ---

    /**
     * Register callback for all cache events.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    ICacheEventPublisher onEvent(Consumer<CacheEvent> callback);

    /**
     * Register callback for specific event type.
     *
     * @param type     the event type to filter
     * @param callback the callback
     * @return this for fluent chaining
     */
    ICacheEventPublisher onEvent(CacheEventType type, Consumer<CacheEvent> callback);

    /**
     * Register callback with custom filter.
     *
     * @param filter   the event filter
     * @param callback the callback
     * @return this for fluent chaining
     */
    ICacheEventPublisher onEvent(Predicate<CacheEvent> filter, Consumer<CacheEvent> callback);

    /**
     * Register callback for cache hits only.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    default ICacheEventPublisher onHit(Consumer<CacheEvent> callback) {
        return onEvent(CacheEventType.HIT, callback);
    }

    /**
     * Register callback for cache misses only.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    default ICacheEventPublisher onMiss(Consumer<CacheEvent> callback) {
        return onEvent(CacheEventType.MISS, callback);
    }

    /**
     * Register callback for cache evictions only.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    default ICacheEventPublisher onEviction(Consumer<CacheEvent> callback) {
        return onEvent(CacheEventType.EVICTION, callback);
    }

    // --- PUBLISHING ---

    /**
     * Publish a cache event to all subscribers and callbacks.
     *
     * @param event the event to publish
     */
    void publish(CacheEvent event);

    /**
     * Publish HIT event.
     *
     * @param key         the cache key
     * @param currentSize the current cache size
     */
    default void publishHit(String key, int currentSize) {
        publish(CacheEvent.hit(key, currentSize));
    }

    /**
     * Publish MISS event.
     *
     * @param key         the cache key
     * @param currentSize the current cache size
     */
    default void publishMiss(String key, int currentSize) {
        publish(CacheEvent.miss(key, currentSize));
    }

    /**
     * Publish EVICTION event.
     *
     * @param key         the evicted key
     * @param currentSize the current cache size after eviction
     */
    default void publishEviction(String key, int currentSize) {
        publish(CacheEvent.eviction(key, currentSize));
    }

    /**
     * Publish THRESHOLD_EXCEEDED event.
     *
     * @param currentSize the current cache size
     * @param threshold   the threshold that was exceeded
     */
    default void publishThresholdExceeded(int currentSize, int threshold) {
        publish(CacheEvent.thresholdExceeded(currentSize, threshold));
    }

    // --- FACTORY ---

    /**
     * Create a new cache event publisher.
     *
     * @return new publisher
     */
    static ICacheEventPublisher create() {
        return new DefaultCacheEventPublisher();
    }

    // --- IMPLEMENTATION ---

    /** Default implementation with thread-safe callback and subscriber management. */
    class DefaultCacheEventPublisher implements ICacheEventPublisher {

        private final List<Consumer<CacheEvent>> callbacks = new CopyOnWriteArrayList<>();
        private final List<Flow.Subscriber<? super CacheEvent>> subscribers = new CopyOnWriteArrayList<>();

        @Override
        public ICacheEventPublisher onEvent(Consumer<CacheEvent> callback) {
            callbacks.add(callback);
            return this;
        }

        @Override
        public ICacheEventPublisher onEvent(CacheEventType type, Consumer<CacheEvent> callback) {
            callbacks.add(event -> {
                if (event.type() == type) {
                    callback.accept(event);
                }
            });
            return this;
        }

        @Override
        public ICacheEventPublisher onEvent(Predicate<CacheEvent> filter, Consumer<CacheEvent> callback) {
            callbacks.add(event -> {
                if (filter.test(event)) {
                    callback.accept(event);
                }
            });
            return this;
        }

        @Override
        public void publish(CacheEvent event) {
            // Fire callbacks
            callbacks.forEach(cb -> cb.accept(event));

            // Fire Flow subscribers
            subscribers.forEach(sub -> sub.onNext(event));
        }

        @Override
        public void subscribe(Flow.Subscriber<? super CacheEvent> subscriber) {
            subscribers.add(subscriber);
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    // Unbounded - events are pushed as they occur
                }

                @Override
                public void cancel() {
                    subscribers.remove(subscriber);
                }
            });
        }
    }
}
