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
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * AbstractObservableContext - Base implementation for observable contexts.
 *
 * <p>Provides context change notifications for:
 * <ul>
 *   <li>State changes (intent, emotion, direction, focus)</li>
 *   <li>Event appends</li>
 *   <li>Snapshot/collapse</li>
 *   <li>Component load/evict</li>
 *   <li>Parent/child linking</li>
 * </ul>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses can override:
 * <ul>
 *   <li>{@link #shouldEmit(ContextChange)} - Emission filtering</li>
 *   <li>{@link #onContextChanged(ContextChange)} - Change callback</li>
 * </ul>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/AbstractObservableContext.java}
 * blob {@code 14ecbf05d9597b2534cc903a82d873b783761efe} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractObservableContext implements IObservableContext {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractObservableContext.class);


    private final List<Consumer<ContextChange>> listeners = new CopyOnWriteArrayList<>();
    private final List<Flow.Subscriber<? super ContextChange>> flowSubscribers = new CopyOnWriteArrayList<>();

    /** Create an observable context. */
    protected AbstractObservableContext() {
    }

    @Override
    public void onChange(Consumer<ContextChange> listener) {
        listeners.add(listener);
    }

    @Override
    public void onChange(ChangeType type, Consumer<ContextChange> listener) {
        listeners.add(change -> {
            if (change.type() == type) {
                listener.accept(change);
            }
        });
    }

    @Override
    public void removeListener(Consumer<ContextChange> listener) {
        listeners.remove(listener);
    }

    @Override
    public void clearListeners() {
        listeners.clear();
        flowSubscribers.clear();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ContextChange> subscriber) {
        flowSubscribers.add(subscriber);
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                // Unbounded
            }

            @Override
            public void cancel() {
                flowSubscribers.remove(subscriber);
            }
        });
    }

    /**
     * Emit a context change to all listeners.
     *
     * @param type the change type
     * @param contextId the context UUID
     * @param oldValue the old value
     * @param newValue the new value
     */
    protected void emit(ChangeType type, UUID contextId, Object oldValue, Object newValue) {
        ContextChange change = ContextChange.of(contextId, type, oldValue, newValue);
        emit(change);
    }

    /**
     * Emit a context change to all listeners.
     *
     * @param change the context change
     */
    protected void emit(ContextChange change) {
        if (!shouldEmit(change)) {
            return;
        }
        onContextChanged(change);
        listeners.forEach(l -> l.accept(change));
        flowSubscribers.forEach(s -> s.onNext(change));
    }

    // --- Template Methods (Override for Custom Behavior) ---

    /**
     * Determine if change should be emitted.
     *
     * @param change the context change
     * @return true if should emit
     */
    protected boolean shouldEmit(ContextChange change) {
        return true; // Default: always emit
    }

    /**
     * Called when context changes.
     *
     * @param change the context change
     */
    protected void onContextChanged(ContextChange change) {
        // Default: no-op
    }

    /**
     * Get the listeners list.
     *
     * @return the listeners
     */
    protected List<Consumer<ContextChange>> getListeners() {
        return listeners;
    }

    /**
     * Get the Flow subscribers list.
     *
     * @return the Flow subscribers
     */
    protected List<Flow.Subscriber<? super ContextChange>> getFlowSubscribers() {
        return flowSubscribers;
    }
}
