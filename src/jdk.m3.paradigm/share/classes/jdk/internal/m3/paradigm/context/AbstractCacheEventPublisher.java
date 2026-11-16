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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * AbstractCacheEventPublisher - Base implementation for cache event publishers.
 *
 * <p>Provides observable cache operation events:
 * <ul>
 *   <li>HIT - Cache lookup found entry</li>
 *   <li>MISS - Cache lookup did not find entry</li>
 *   <li>EVICTION - Entry evicted from cache</li>
 *   <li>THRESHOLD_EXCEEDED - Cache size threshold exceeded</li>
 *   <li>CLEAR - Cache cleared</li>
 * </ul>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses can override:
 * <ul>
 *   <li>{@link #shouldPublish(CacheEvent)} - Event filtering</li>
 *   <li>{@link #onEventPublished(CacheEvent)} - Post-publish callback</li>
 * </ul>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/AbstractCacheEventPublisher.java}
 * blob {@code 1ab921ff96c55bac51bbbf50a44570c90c586aa3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractCacheEventPublisher implements ICacheEventPublisher {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractCacheEventPublisher.class);


    private final List<Consumer<CacheEvent>> callbacks = new CopyOnWriteArrayList<>();
    private final List<Flow.Subscriber<? super CacheEvent>> subscribers = new CopyOnWriteArrayList<>();

    /** Create a cache event publisher. */
    protected AbstractCacheEventPublisher() {
    }

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
        if (!shouldPublish(event)) {
            return;
        }

        // Fire callbacks
        callbacks.forEach(cb -> cb.accept(event));

        // Fire Flow subscribers
        subscribers.forEach(sub -> sub.onNext(event));

        onEventPublished(event);
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

    // --- Template Methods (Override for Custom Behavior) ---

    /**
     * Determine if event should be published.
     *
     * @param event the cache event
     * @return true if should publish
     */
    protected boolean shouldPublish(CacheEvent event) {
        return true; // Default: always publish
    }

    /**
     * Called after event is published.
     *
     * @param event the published event
     */
    protected void onEventPublished(CacheEvent event) {
        // Default: no-op
    }

    /**
     * Get the callbacks list.
     *
     * @return the callbacks
     */
    protected List<Consumer<CacheEvent>> getCallbacks() {
        return callbacks;
    }

    /**
     * Get the Flow subscribers list.
     *
     * @return the Flow subscribers
     */
    protected List<Flow.Subscriber<? super CacheEvent>> getSubscribers() {
        return subscribers;
    }
}
