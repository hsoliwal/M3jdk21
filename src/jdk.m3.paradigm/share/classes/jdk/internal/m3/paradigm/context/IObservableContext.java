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

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;
import jdk.internal.m3.paradigm.context.IContextMemento.ContextId;
import jdk.internal.m3.paradigm.context.IContextMemento.ContextSummary;


import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * Observable Context - Context with Change Notifications.
 *
 * <h2>What's Observable</h2>
 * <ul>
 *   <li>State changes (intent, emotion, direction, focus)</li>
 *   <li>Event appends</li>
 *   <li>Snapshot/collapse</li>
 *   <li>Component load/evict</li>
 *   <li>Parent/child linking</li>
 * </ul>
 *
 * <h2>Integration Options</h2>
 * <pre>
 * // Callback style
 * context.onChange(delta -&gt; updateUI(delta));
 *
 * // RxJava
 * Observable.create(e -&gt; context.onChange(e::onNext));
 *
 * // Reactor
 * Flux.create(s -&gt; context.onChange(s::next));
 *
 * // Java Flow
 * context.subscribe(subscriber);
 * </pre>
 *
 * @since 1.1.0 - ContextOS Integration
 */
@ISpec(
    value = "Observable Context - context with change notifications for state/events/components",
    constraints = {
        "Observes: state changes, event appends, snapshots, component load/evict",
        "Integrates with RxJava, Reactor, and Flow APIs",
        "ChangeTypes: STATE_CHANGED, EVENT_APPENDED, SNAPSHOT_CREATED, etc.",
        "Thread-safe notification via CopyOnWriteArrayList"
    },
    invariants = {
        "onChange(listener) registers change callback",
        "subscribe(subscriber) registers Flow.Subscriber",
        "Each change has type, contextId, and timestamp"
    },
    examples = {
        "context.onChange(delta -> updateUI(delta))",
        "Observable.create(e -> context.onChange(e::onNext))",
        "context.subscribe(flowSubscriber)"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/IObservableContext.java}
 * blob {@code 29f7f0106dfb46123e5fbf10a325ab1a824573f9} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IObservableContext extends Flow.Publisher<IObservableContext.ContextChange> {

    // --- CHANGE EVENTS ---

    /** Change types. */
    enum ChangeType {
        STATE_CHANGED,      // intent/emotion/direction/focus changed
        EVENT_APPENDED,     // new event added to history
        SNAPSHOT_CREATED,   // collapse happened
        COMPONENT_LOADED,   // lazy component materialized
        COMPONENT_EVICTED,  // component flushed back to memento
        LINKED,             // parent changed
        BLOOMED,            // selective materialization
        COLLAPSED           // summarize + forget
    }

    /** Context change event. */
    record ContextChange(
            UUID contextId,
            ChangeType type,
            Object oldValue,
            Object newValue,
            long timestamp
    ) {
        public static ContextChange of(UUID id, ChangeType type, Object oldVal, Object newVal) {
            return new ContextChange(id, type, oldVal, newVal, System.currentTimeMillis());
        }
    }

    // --- SUBSCRIPTION API ---

    /** Subscribe to all changes (callback style). */
    void onChange(Consumer<ContextChange> listener);

    /** Subscribe to specific change type. */
    void onChange(ChangeType type, Consumer<ContextChange> listener);

    /** Unsubscribe. */
    void removeListener(Consumer<ContextChange> listener);

    /** Clear all listeners. */
    void clearListeners();

    // --- JAVA FLOW API ---

    /** Subscribe (Flow.Publisher). */
    @Override
    void subscribe(Flow.Subscriber<? super ContextChange> subscriber);

    // --- FACTORY ---

    /** Make a context observable. */
    static IObservableContext wrap(IContextKernel.IContextNode context) {
        return new ObservableContextWrapper(context);
    }

    // --- DEFAULT IMPLEMENTATION ---

    /** Observable context wrapper. */
    final class ObservableContextWrapper implements IObservableContext, IContextKernel.IContextNode {

        private final IContextKernel.IContextNode delegate;
        private final List<Consumer<ContextChange>> listeners = new CopyOnWriteArrayList<>();
        private final List<Flow.Subscriber<? super ContextChange>> flowSubscribers = new CopyOnWriteArrayList<>();

        ObservableContextWrapper(IContextKernel.IContextNode delegate) {
            this.delegate = delegate;
        }

        // === Observable API ===

        @Override
        public void onChange(Consumer<ContextChange> listener) {
            listeners.add(listener);
        }

        @Override
        public void onChange(ChangeType type, Consumer<ContextChange> listener) {
            listeners.add(change -> {
                if (change.type() == type) listener.accept(change);
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
                public void request(long n) { /* unbounded */ }
                @Override
                public void cancel() { flowSubscribers.remove(subscriber); }
            });
        }

        private void emit(ChangeType type, Object oldVal, Object newVal) {
            ContextChange change = ContextChange.of(delegate.id().id(), type, oldVal, newVal);
            listeners.forEach(l -> l.accept(change));
            flowSubscribers.forEach(s -> s.onNext(change));
        }

        // === Delegated IContextNode with emissions ===

        @Override
        public ContextId id() { return delegate.id(); }

        @Override
        public IContextKernel.ContextMeta meta() { return delegate.meta(); }

        @Override
        public void accept(String eventType, String payload) {
            delegate.accept(eventType, payload);
            emit(ChangeType.EVENT_APPENDED, null, payload);
        }

        @Override
        public List<String> history() { return delegate.history(); }

        @Override
        public List<String> history(int limit) { return delegate.history(limit); }

        @Override
        public void snapshot(ContextSummary summary) {
            delegate.snapshot(summary);
            emit(ChangeType.SNAPSHOT_CREATED, null, summary);
        }

        @Override
        public java.util.Optional<ContextSummary> loadSnapshot() {
            return delegate.loadSnapshot();
        }

        @Override
        public void flush() {
            delegate.flush();
            emit(ChangeType.COMPONENT_EVICTED, null, null);
        }

        @Override
        public boolean isLoaded() { return delegate.isLoaded(); }
    }
}
