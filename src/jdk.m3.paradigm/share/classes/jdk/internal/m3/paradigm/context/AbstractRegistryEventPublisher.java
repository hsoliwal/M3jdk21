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

import jdk.internal.m3.collections.TokenInterner;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * AbstractRegistryEventPublisher - Base implementation for IRegistryEventPublisher.
 *
 * <p>Provides registry event publishing with template method pattern:
 * <ul>
 *   <li>Thread-safe callback management via CopyOnWriteArrayList</li>
 *   <li>JDK Flow API integration</li>
 *   <li>Dual API pattern (callbacks + reactive)</li>
 * </ul>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses can override:
 * <ul>
 *   <li>{@link #doPublish(IRegistryEventPublisher.RegistryEvent)} - Custom publish logic</li>
 *   <li>{@link #onCallbackRegistered(Consumer)} - Callback when handler registered</li>
 *   <li>{@link #onSubscriberAdded(Flow.Subscriber)} - Callback when subscriber added</li>
 *   <li>{@link #onPublishError(IRegistryEventPublisher.RegistryEvent, Throwable)} - Error handling</li>
 * </ul>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/factory/AbstractRegistryEventPublisher.java}
 * blob {@code 106199b141ce4ffc99fef5d011abaf2c8d0d310e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractRegistryEventPublisher implements IRegistryEventPublisher {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractRegistryEventPublisher.class);


    // --- INTERNED STRING CONSTANTS ---
    private static final TokenInterner INTERNER = TokenInterner.getInstance();

    private static final String ERR_CALLBACK_NULL = INTERNER.intern("callback cannot be null");
    private static final String ERR_FILTER_CALLBACK_NULL = INTERNER.intern("filter and callback cannot be null");
    private static final String ERR_SUBSCRIBER_NULL = INTERNER.intern("subscriber cannot be null");
    private static final String ERR_PUBLISH_PREFIX = INTERNER.intern("Publish error for registry event ");
    private static final String COLON_SPACE = INTERNER.intern(": ");
    private static final String PREFIX_PUBLISHER = INTERNER.intern("RegistryEventPublisher[callbacks=");
    private static final String SUBSCRIBERS_EQ = INTERNER.intern(", subscribers=");
    private static final String BRACKET_CLOSE = INTERNER.intern("]");

    // --- STATE ---
    private final List<Consumer<RegistryEvent>> callbacks = new CopyOnWriteArrayList<>();
    private final List<Flow.Subscriber<? super RegistryEvent>> subscribers = new CopyOnWriteArrayList<>();

    /** Create a registry event publisher. */
    protected AbstractRegistryEventPublisher() {
        // Default constructor
    }

    @Override
    public IRegistryEventPublisher onEvent(Consumer<RegistryEvent> callback) {
        if (callback == null) {
            throw new NullPointerException(ERR_CALLBACK_NULL);
        }
        callbacks.add(callback);
        onCallbackRegistered(callback);
        return this;
    }

    @Override
    public IRegistryEventPublisher onEvent(RegistryEventType type, Consumer<RegistryEvent> callback) {
        if (callback == null) {
            throw new NullPointerException(ERR_CALLBACK_NULL);
        }
        callbacks.add(event -> {
            if (event.type() == type) {
                callback.accept(event);
            }
        });
        onCallbackRegistered(callback);
        return this;
    }

    @Override
    public IRegistryEventPublisher onEvent(Predicate<RegistryEvent> filter, Consumer<RegistryEvent> callback) {
        if (filter == null || callback == null) {
            throw new NullPointerException(ERR_FILTER_CALLBACK_NULL);
        }
        callbacks.add(event -> {
            if (filter.test(event)) {
                callback.accept(event);
            }
        });
        onCallbackRegistered(callback);
        return this;
    }

    @Override
    public void publish(RegistryEvent event) {
        if (event == null) {
            return;
        }

        // Allow subclass to handle publish
        if (doPublish(event)) {
            return;
        }

        // Fire callbacks
        callbacks.forEach(cb -> {
            try {
                cb.accept(event);
            } catch (Exception e) {
                onPublishError(event, e);
            }
        });

        // Fire Flow subscribers
        subscribers.forEach(sub -> {
            try {
                sub.onNext(event);
            } catch (Exception e) {
                onPublishError(event, e);
            }
        });
    }

    @Override
    public void subscribe(Flow.Subscriber<? super RegistryEvent> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException(ERR_SUBSCRIBER_NULL);
        }
        subscribers.add(subscriber);
        onSubscriberAdded(subscriber);

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
     * Custom publish logic.
     *
     * @param event the event to publish
     * @return true if handled (skip default), false to use default
     */
    protected boolean doPublish(RegistryEvent event) {
        return false; // Default: use standard publish
    }

    /**
     * Callback when a handler is registered.
     *
     * @param callback the registered callback
     */
    protected void onCallbackRegistered(Consumer<RegistryEvent> callback) {
        // Default: no-op
    }

    /**
     * Callback when a subscriber is added.
     *
     * @param subscriber the added subscriber
     */
    protected void onSubscriberAdded(Flow.Subscriber<? super RegistryEvent> subscriber) {
        // Default: no-op
    }

    /**
     * Callback on publish error.
     *
     * @param event the event that caused error
     * @param error the error
     */
    protected void onPublishError(RegistryEvent event, Throwable error) {
        // Default: log to stderr
        log.error("{}{}{}", ERR_PUBLISH_PREFIX, event.type(), COLON_SPACE, error);
    }

    // --- Accessors for Subclasses ---

    /**
     * Get the callback count.
     *
     * @return number of registered callbacks
     */
    protected int callbackCount() {
        return callbacks.size();
    }

    /**
     * Get the subscriber count.
     *
     * @return number of registered subscribers
     */
    protected int subscriberCount() {
        return subscribers.size();
    }

    /** Clear all callbacks and subscribers. */
    protected void clear() {
        callbacks.clear();
        subscribers.clear();
    }

    @Override
    public String toString() {
        return PREFIX_PUBLISHER + callbacks.size() +
                SUBSCRIBERS_EQ + subscribers.size() + BRACKET_CLOSE;
    }
}
