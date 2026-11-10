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
 * IFactoryEventPublisher - Observable context factory lifecycle events.
 *
 * <h2>Architecture</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  FACTORY EVENT PUBLISHER                                                    │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │                                                                             │
 * │  IFactoryEventPublisher extends Flow.Publisher&lt;FactoryEvent&gt;               │
 * │                                                                             │
 * │  DUAL API PATTERN:                                                          │
 * │    Pattern 1: publisher.onEvent(e -&gt; audit(e))    // Simple callback       │
 * │    Pattern 2: publisher.subscribe(subscriber)     // JDK Flow API           │
 * │                                                                             │
 * │  Event Types:                                                               │
 * │    CREATED        - Context created by factory                              │
 * │    REGISTERED     - Context registered in registry                          │
 * │    CHILD_CREATED  - Child context created under parent                      │
 * │    LOOP_PREVENTED - Loop detection prevented cycle creation                 │
 * │    DESTROYED      - Context destroyed/unregistered                          │
 * │                                                                             │
 * │  Purpose:                                                                   │
 * │    - Audit trail of context creation                                        │
 * │    - Loop prevention tracking                                               │
 * │    - Registry synchronization                                               │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get or create factory event publisher
 * IFactoryEventPublisher publisher = IFactoryEventPublisher.create();
 *
 * // Simple callback - filter by type
 * publisher.onEvent(FactoryEventType.LOOP_PREVENTED, event -> {
 *     log.warn("Loop prevented: {} -> {}", event.contextId(), event.parentId());
 * });
 *
 * // Simple callback - all events
 * publisher.onEvent(event -> {
 *     auditLog.record(event.type(), event.contextId(), event.timestamp());
 * });
 *
 * // Reactive Streams - JDK Flow API
 * publisher.subscribe(new Flow.Subscriber<>() {
 *     public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
 *     public void onNext(FactoryEvent e) { persistAuditTrail(e); }
 *     public void onError(Throwable t) { alerting.notify(t); }
 *     public void onComplete() { }
 * });
 * }</pre>
 *
 * @since ContextOS 1.0 - Observable Factory System
 * @see java.util.concurrent.Flow
 */
@ISpec(
    value = "Observable Factory Events - context lifecycle events with Flow.Publisher",
    constraints = {
        "Dual API: simple callback and JDK Flow API",
        "Event types: CREATED, REGISTERED, CHILD_CREATED, LOOP_PREVENTED, DESTROYED",
        "Purpose: audit trail, loop prevention tracking, registry sync"
    },
    invariants = {
        "create() returns new publisher instance",
        "onEvent(type, handler) registers typed callback",
        "publish(event) notifies all subscribers"
    },
    examples = {
        "IFactoryEventPublisher.create()",
        "publisher.onEvent(LOOP_PREVENTED, e -> log.warn(e))"
    },
    model = Model.ModelType.HAIKU,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/factory/IFactoryEventPublisher.java}
 * blob {@code a000d68ddba4ce9895c170b29a8fc453075c2f30} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IFactoryEventPublisher extends Flow.Publisher<IFactoryEventPublisher.FactoryEvent> {
    PlaceholderLogger log = PlaceholderLogger.of(IFactoryEventPublisher.class);

    /** Factory event types. */
    enum FactoryEventType {
        /** Context created by factory. */
        CREATED,
        /** Context registered in registry. */
        REGISTERED,
        /** Child context created under parent. */
        CHILD_CREATED,
        /** Loop detection prevented cycle creation. */
        LOOP_PREVENTED,
        /** Context destroyed/unregistered. */
        DESTROYED
    }

    /**
     * Factory event record - immutable snapshot of a factory operation.
     *
     * @param type      the event type
     * @param contextId the context ID
     * @param parentId  the parent context ID (nullable, for CHILD_CREATED and LOOP_PREVENTED)
     * @param contextType the context type name (nullable)
     * @param timestamp when the event occurred
     */
    record FactoryEvent(
            FactoryEventType type,
            String contextId,
            String parentId,
            String contextType,
            long timestamp
    ) {
        /**
         * Create a CREATED event.
         *
         * @param contextId   the context ID
         * @param contextType the context type name
         * @return new event
         */
        public static FactoryEvent created(String contextId, String contextType) {
            return new FactoryEvent(
                    FactoryEventType.CREATED,
                    contextId,
                    null,
                    contextType,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create a REGISTERED event.
         *
         * @param contextId the context ID
         * @return new event
         */
        public static FactoryEvent registered(String contextId) {
            return new FactoryEvent(
                    FactoryEventType.REGISTERED,
                    contextId,
                    null,
                    null,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create a CHILD_CREATED event.
         *
         * @param childId  the child context ID
         * @param parentId the parent context ID
         * @return new event
         */
        public static FactoryEvent childCreated(String childId, String parentId) {
            return new FactoryEvent(
                    FactoryEventType.CHILD_CREATED,
                    childId,
                    parentId,
                    null,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create a LOOP_PREVENTED event.
         *
         * @param childId  the child context ID that would have created a loop
         * @param parentId the parent context ID
         * @return new event
         */
        public static FactoryEvent loopPrevented(String childId, String parentId) {
            return new FactoryEvent(
                    FactoryEventType.LOOP_PREVENTED,
                    childId,
                    parentId,
                    null,
                    System.currentTimeMillis()
            );
        }

        /**
         * Create a DESTROYED event.
         *
         * @param contextId the context ID
         * @return new event
         */
        public static FactoryEvent destroyed(String contextId) {
            return new FactoryEvent(
                    FactoryEventType.DESTROYED,
                    contextId,
                    null,
                    null,
                    System.currentTimeMillis()
            );
        }

        /**
         * Check if this is a lifecycle start event (CREATED or REGISTERED).
         *
         * @return true if lifecycle start
         */
        public boolean isLifecycleStart() {
            return type == FactoryEventType.CREATED || type == FactoryEventType.REGISTERED;
        }

        /**
         * Check if this is a warning event (LOOP_PREVENTED).
         *
         * @return true if warning
         */
        public boolean isWarning() {
            return type == FactoryEventType.LOOP_PREVENTED;
        }
    }

    // --- SIMPLE CALLBACK API ---

    /**
     * Register callback for all factory events.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    IFactoryEventPublisher onEvent(Consumer<FactoryEvent> callback);

    /**
     * Register callback for specific event type.
     *
     * @param type     the event type to filter
     * @param callback the callback
     * @return this for fluent chaining
     */
    IFactoryEventPublisher onEvent(FactoryEventType type, Consumer<FactoryEvent> callback);

    /**
     * Register callback with custom filter.
     *
     * @param filter   the event filter
     * @param callback the callback
     * @return this for fluent chaining
     */
    IFactoryEventPublisher onEvent(Predicate<FactoryEvent> filter, Consumer<FactoryEvent> callback);

    /**
     * Register callback for context creation only.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    default IFactoryEventPublisher onCreated(Consumer<FactoryEvent> callback) {
        return onEvent(FactoryEventType.CREATED, callback);
    }

    /**
     * Register callback for loop prevention only.
     *
     * @param callback the callback
     * @return this for fluent chaining
     */
    default IFactoryEventPublisher onLoopPrevented(Consumer<FactoryEvent> callback) {
        return onEvent(FactoryEventType.LOOP_PREVENTED, callback);
    }

    // --- PUBLISHING ---

    /**
     * Publish a factory event to all subscribers and callbacks.
     *
     * @param event the event to publish
     */
    void publish(FactoryEvent event);

    /**
     * Publish CREATED event.
     *
     * @param contextId   the context ID
     * @param contextType the context type name
     */
    default void publishCreated(String contextId, String contextType) {
        publish(FactoryEvent.created(contextId, contextType));
    }

    /**
     * Publish REGISTERED event.
     *
     * @param contextId the context ID
     */
    default void publishRegistered(String contextId) {
        publish(FactoryEvent.registered(contextId));
    }

    /**
     * Publish CHILD_CREATED event.
     *
     * @param childId  the child context ID
     * @param parentId the parent context ID
     */
    default void publishChildCreated(String childId, String parentId) {
        publish(FactoryEvent.childCreated(childId, parentId));
    }

    /**
     * Publish LOOP_PREVENTED event.
     *
     * @param childId  the child context ID
     * @param parentId the parent context ID
     */
    default void publishLoopPrevented(String childId, String parentId) {
        publish(FactoryEvent.loopPrevented(childId, parentId));
    }

    // --- FACTORY ---

    /**
     * Create a new factory event publisher.
     *
     * @return new publisher
     */
    static IFactoryEventPublisher create() {
        return new DefaultFactoryEventPublisher();
    }

    // --- IMPLEMENTATION ---

    /** Default implementation with thread-safe callback and subscriber management. */
    class DefaultFactoryEventPublisher implements IFactoryEventPublisher {

        private final List<Consumer<FactoryEvent>> callbacks = new CopyOnWriteArrayList<>();
        private final List<Flow.Subscriber<? super FactoryEvent>> subscribers = new CopyOnWriteArrayList<>();

        @Override
        public IFactoryEventPublisher onEvent(Consumer<FactoryEvent> callback) {
            callbacks.add(callback);
            return this;
        }

        @Override
        public IFactoryEventPublisher onEvent(FactoryEventType type, Consumer<FactoryEvent> callback) {
            callbacks.add(event -> {
                if (event.type() == type) {
                    callback.accept(event);
                }
            });
            return this;
        }

        @Override
        public IFactoryEventPublisher onEvent(Predicate<FactoryEvent> filter, Consumer<FactoryEvent> callback) {
            callbacks.add(event -> {
                if (filter.test(event)) {
                    callback.accept(event);
                }
            });
            return this;
        }

        @Override
        public void publish(FactoryEvent event) {
            // Fire callbacks
            callbacks.forEach(cb -> cb.accept(event));

            // Fire Flow subscribers
            subscribers.forEach(sub -> sub.onNext(event));
        }

        @Override
        public void subscribe(Flow.Subscriber<? super FactoryEvent> subscriber) {
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
