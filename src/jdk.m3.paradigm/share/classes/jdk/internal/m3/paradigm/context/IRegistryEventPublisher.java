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
 * IRegistryEventPublisher - Observable context registry operation events.
 *
 * <h2>Architecture</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  REGISTRY EVENT PUBLISHER                                                   │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │                                                                             │
 * │  IRegistryEventPublisher extends Flow.Publisher&lt;RegistryEvent&gt;             │
 * │                                                                             │
 * │  DUAL API PATTERN:                                                          │
 * │    Pattern 1: publisher.onEvent(e -&gt; track(e))   // Simple callback        │
 * │    Pattern 2: publisher.subscribe(subscriber)    // JDK Flow API            │
 * │                                                                             │
 * │  Event Types:                                                               │
 * │    REGISTERED      - Context registered in registry                         │
 * │    UNREGISTERED    - Context unregistered from registry                     │
 * │    LOOKUP_HIT      - Lookup found context by ID                             │
 * │    LOOKUP_MISS     - Lookup did not find context                            │
 * │    ALIAS_RESOLVED  - Name alias resolved to context ID                      │
 * │    ALIAS_CREATED   - New alias created for context                          │
 * │                                                                             │
 * │  Purpose:                                                                   │
 * │    - Registry performance monitoring                                        │
 * │    - Lookup pattern analysis                                                │
 * │    - Alias resolution tracking                                              │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get or create registry event publisher
 * IRegistryEventPublisher publisher = IRegistryEventPublisher.create();
 *
 * // Track lookup efficiency
 * publisher.onLookupHit(event -> metrics.incrementRegistryHit(event.contextId()));
 * publisher.onLookupMiss(event -> {
 *     log.debug("Registry miss for: {}", event.contextId());
 *     metrics.incrementRegistryMiss(event.contextId());
 * });
 *
 * // Monitor alias resolution
 * publisher.onEvent(RegistryEventType.ALIAS_RESOLVED, event -> {
 *     log.trace("Alias '{}' resolved to {}", event.alias(), event.contextId());
 * });
 *
 * // Reactive Streams - JDK Flow API
 * publisher.subscribe(new Flow.Subscriber<>() {
 *     public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
 *     public void onNext(RegistryEvent e) { analytics.record(e); }
 *     public void onError(Throwable t) { alerting.notify(t); }
 *     public void onComplete() { }
 * });
 * }</pre>
 *
 * @since ContextOS 1.0 - Observable Factory System
 * @see java.util.concurrent.Flow
 */
@ISpec(
    value = "Observable Registry Events - context registration/lookup events with Flow.Publisher",
    constraints = {
        "Dual API: simple callback and JDK Flow API",
        "Event types: REGISTERED, UNREGISTERED, LOOKUP_HIT, LOOKUP_MISS, ALIAS_RESOLVED",
        "Purpose: registry performance monitoring and lookup pattern analysis"
    },
    invariants = {
        "create() returns new publisher instance",
        "onEvent(handler) registers callback for all events",
        "onLookupHit/onLookupMiss register typed callbacks"
    },
    examples = {
        "IRegistryEventPublisher.create()",
        "publisher.onLookupHit(e -> metrics.incrementHit())"
    },
    model = Model.ModelType.HAIKU,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/factory/IRegistryEventPublisher.java}
 * blob {@code 860c4717c2a91599959e3a93405a6a1dd10a9a9e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IRegistryEventPublisher extends Flow.Publisher<IRegistryEventPublisher.RegistryEvent> {
    PlaceholderLogger log = PlaceholderLogger.of(IRegistryEventPublisher.class);

    /** Registry event types. */
    enum RegistryEventType {
        /** Context registered in registry. */
        REGISTERED,
        /** Context unregistered from registry. */
        UNREGISTERED,
        /** Lookup found context by ID. */
        LOOKUP_HIT,
        /** Lookup did not find context. */
        LOOKUP_MISS,
        /** Name alias resolved to context ID. */
        ALIAS_RESOLVED,
        /** New alias created for context. */
        ALIAS_CREATED
    }

    /**
     * Registry event record - immutable snapshot of a registry operation.
     *
     * @param type       the event type
     * @param contextId  the context ID
     * @param alias      the alias name (nullable, for ALIAS_RESOLVED and ALIAS_CREATED)
     * @param lookupTime the lookup time in nanoseconds (for LOOKUP_HIT and LOOKUP_MISS)
     * @param timestamp  when the event occurred
     */
    record RegistryEvent(
            RegistryEventType type,
            String contextId,
            String alias,
            long lookupTime,
            long timestamp
    ) {
        /**
         * Create a REGISTERED event.
         *
         * @param contextId the context ID
         * @return new event
         */
        public static RegistryEvent registered(String contextId) {
            return new RegistryEvent(
                    RegistryEventType.REGISTERED,
                    contextId,
                    null,
                    0,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create an UNREGISTERED event.
         *
         * @param contextId the context ID
         * @return new event
         */
        public static RegistryEvent unregistered(String contextId) {
            return new RegistryEvent(
                    RegistryEventType.UNREGISTERED,
                    contextId,
                    null,
                    0,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create a LOOKUP_HIT event.
         *
         * @param contextId  the context ID found
         * @param lookupTime the lookup time in nanoseconds
         * @return new event
         */
        public static RegistryEvent lookupHit(String contextId, long lookupTime) {
            return new RegistryEvent(
                    RegistryEventType.LOOKUP_HIT,
                    contextId,
                    null,
                    lookupTime,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create a LOOKUP_MISS event.
         *
         * @param contextId  the context ID that was not found
         * @param lookupTime the lookup time in nanoseconds
         * @return new event
         */
        public static RegistryEvent lookupMiss(String contextId, long lookupTime) {
            return new RegistryEvent(
                    RegistryEventType.LOOKUP_MISS,
                    contextId,
                    null,
                    lookupTime,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create an ALIAS_RESOLVED event.
         *
         * @param alias     the alias name
         * @param contextId the resolved context ID
         * @return new event
         */
        public static RegistryEvent aliasResolved(String alias, String contextId) {
            return new RegistryEvent(
                    RegistryEventType.ALIAS_RESOLVED,
                    contextId,
                    alias,
                    0,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create an ALIAS_CREATED event.
         *
         * @param alias     the new alias name
         * @param contextId the context ID
         * @return new event
         */
        public static RegistryEvent aliasCreated(String alias, String contextId) {
            return new RegistryEvent(
                    RegistryEventType.ALIAS_CREATED,
                    contextId,
                    alias,
                    0,
                    System.currentTimeMillis()
            );
        }

        /**
         * Check if this is a successful lookup (LOOKUP_HIT).
         *
         * @return true if LOOKUP_HIT
         */
        public boolean isHit() {
            return type == RegistryEventType.LOOKUP_HIT;
        }

        /**
         * Check if this is a failed lookup (LOOKUP_MISS).
         *
         * @return true if LOOKUP_MISS
         */
        public boolean isMiss() {
            return type == RegistryEventType.LOOKUP_MISS;
        }

        /**
         * Check if this is a lookup event (HIT or MISS).
         *
         * @return true if lookup event
         */
        public boolean isLookup() {
            return type == RegistryEventType.LOOKUP_HIT || type == RegistryEventType.LOOKUP_MISS;
        }

        /**
         * Check if this is an alias event.
         *
         * @return true if alias event
         */
        public boolean isAliasEvent() {
            return type == RegistryEventType.ALIAS_RESOLVED || type == RegistryEventType.ALIAS_CREATED;
        }
    }

    // --- SIMPLE CALLBACK API ---

    /**
     * Register callback for all registry events.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    IRegistryEventPublisher onEvent(Consumer<RegistryEvent> callback);

    /**
     * Register callback for specific event type.
     *
     * @param type     the event type to filter
     * @param callback the callback
     * @return this for fluent chaining
     */
    IRegistryEventPublisher onEvent(RegistryEventType type, Consumer<RegistryEvent> callback);

    /**
     * Register callback with custom filter.
     *
     * @param filter   the event filter
     * @param callback the callback
     * @return this for fluent chaining
     */
    IRegistryEventPublisher onEvent(Predicate<RegistryEvent> filter, Consumer<RegistryEvent> callback);

    /**
     * Register callback for lookup hits only.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    default IRegistryEventPublisher onLookupHit(Consumer<RegistryEvent> callback) {
        return onEvent(RegistryEventType.LOOKUP_HIT, callback);
    }

    /**
     * Register callback for lookup misses only.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    default IRegistryEventPublisher onLookupMiss(Consumer<RegistryEvent> callback) {
        return onEvent(RegistryEventType.LOOKUP_MISS, callback);
    }

    /**
     * Register callback for registration events only.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    default IRegistryEventPublisher onRegistered(Consumer<RegistryEvent> callback) {
        return onEvent(RegistryEventType.REGISTERED, callback);
    }

    // --- PUBLISHING ---

    /**
     * Publish a registry event to all subscribers and callbacks.
     *
     * @param event the event to publish
     */
    void publish(RegistryEvent event);

    /**
     * Publish REGISTERED event.
     *
     * @param contextId the context ID
     */
    default void publishRegistered(String contextId) {
        publish(RegistryEvent.registered(contextId));
    }

    /**
     * Publish LOOKUP_HIT event.
     *
     * @param contextId  the context ID found
     * @param lookupTime the lookup time in nanoseconds
     */
    default void publishLookupHit(String contextId, long lookupTime) {
        publish(RegistryEvent.lookupHit(contextId, lookupTime));
    }

    /**
     * Publish LOOKUP_MISS event.
     *
     * @param contextId  the context ID that was not found
     * @param lookupTime the lookup time in nanoseconds
     */
    default void publishLookupMiss(String contextId, long lookupTime) {
        publish(RegistryEvent.lookupMiss(contextId, lookupTime));
    }

    /**
     * Publish ALIAS_RESOLVED event.
     *
     * @param alias     the alias name
     * @param contextId the resolved context ID
     */
    default void publishAliasResolved(String alias, String contextId) {
        publish(RegistryEvent.aliasResolved(alias, contextId));
    }

    // --- FACTORY ---

    /**
     * Create a new registry event publisher.
     *
     * @return new publisher
     */
    static IRegistryEventPublisher create() {
        return new DefaultRegistryEventPublisher();
    }

    // --- IMPLEMENTATION ---

    /** Default implementation with thread-safe callback and subscriber management. */
    class DefaultRegistryEventPublisher implements IRegistryEventPublisher {

        private final List<Consumer<RegistryEvent>> callbacks = new CopyOnWriteArrayList<>();
        private final List<Flow.Subscriber<? super RegistryEvent>> subscribers = new CopyOnWriteArrayList<>();

        @Override
        public IRegistryEventPublisher onEvent(Consumer<RegistryEvent> callback) {
            callbacks.add(callback);
            return this;
        }

        @Override
        public IRegistryEventPublisher onEvent(RegistryEventType type, Consumer<RegistryEvent> callback) {
            callbacks.add(event -> {
                if (event.type() == type) {
                    callback.accept(event);
                }
            });
            return this;
        }

        @Override
        public IRegistryEventPublisher onEvent(Predicate<RegistryEvent> filter, Consumer<RegistryEvent> callback) {
            callbacks.add(event -> {
                if (filter.test(event)) {
                    callback.accept(event);
                }
            });
            return this;
        }

        @Override
        public void publish(RegistryEvent event) {
            // Fire callbacks
            callbacks.forEach(cb -> cb.accept(event));

            // Fire Flow subscribers
            subscribers.forEach(sub -> sub.onNext(event));
        }

        @Override
        public void subscribe(Flow.Subscriber<? super RegistryEvent> subscriber) {
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
