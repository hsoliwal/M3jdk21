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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

/**
 * Demand-driven Java 21 sources. No listener registry or shared event backlog.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/stream/ContextFlows.java}
 * blob {@code 912aa13684199e84e78c1179476393d1f0e7681e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class Flows {
    private Flows() { }

    /** One operation per publisher, started on positive demand; all subscribers share its result. */
    public static <T> Flow.Publisher<T> once(Callable<? extends T> operation) {
        Objects.requireNonNull(operation, "operation");
        FutureTask<T> task = new FutureTask<>(() -> Objects.requireNonNull(operation.call(), "result"));
        return page(() -> {
            task.run();
            try { return List.of(task.get()); }
            catch (ExecutionException failure) {
                if (failure.getCause() instanceof Exception exception) throw exception;
                if (failure.getCause() instanceof Error error) throw error;
                throw new IllegalStateException(failure.getCause());
            }
        });
    }

    /** Bounded finite page, loaded independently per subscription only after demand. */
    public static <T> Flow.Publisher<T> page(Callable<? extends List<T>> load) {
        Objects.requireNonNull(load, "load");
        return source(() -> new Cursor<>() {
            private Iterator<T> iterator;
            @Override public T next() throws Exception {
                if (iterator == null) iterator = List.copyOf(load.call()).iterator();
                return iterator.hasNext() ? iterator.next() : null;
            }
            @Override public boolean done() { return iterator != null && !iterator.hasNext(); }
        });
    }

    /** Poll one durable event at a time only with demand. Cancellation wakes an idle poll. */
    public static <T> Flow.Publisher<T> follow(long after, LongFunction<Optional<T>> read,
                                               ToLongFunction<T> sequence) {
        if (after < 0) throw new IllegalArgumentException("Negative cursor");
        Objects.requireNonNull(read); Objects.requireNonNull(sequence);
        return source(() -> new Cursor<>() {
            private long position = after;
            @Override public T next() {
                Optional<T> result = Objects.requireNonNull(read.apply(position));
                if (result.isEmpty()) return null;
                T value = result.orElseThrow();
                long next = sequence.applyAsLong(value);
                if (next <= position) throw new IllegalStateException("Non-increasing durable sequence");
                position = next; return value;
            }
            @Override public boolean done() { return false; }
        });
    }

    /** Lambda sink with one-at-a-time demand. Cancelling the future cancels observation, not a commit. */
    public static <T> CompletableFuture<Void> consume(Flow.Publisher<T> source, Consumer<? super T> sink) {
        Objects.requireNonNull(source); Objects.requireNonNull(sink);
        CompletableFuture<Void> result = new CompletableFuture<>();
        AtomicReference<Flow.Subscription> active = new AtomicReference<>();
        result.whenComplete((value, error) -> {
            Flow.Subscription subscription = active.get();
            if (subscription != null && result.isCancelled()) subscription.cancel();
        });
        source.subscribe(new Flow.Subscriber<>() {
            @Override public void onSubscribe(Flow.Subscription subscription) {
                if (!active.compareAndSet(null, subscription) || result.isCancelled()) subscription.cancel();
                else subscription.request(1);
            }
            @Override public void onNext(T item) {
                if (result.isDone()) return;
                try { sink.accept(item); active.get().request(1); }
                catch (Throwable failure) { active.get().cancel(); result.completeExceptionally(failure); }
            }
            @Override public void onError(Throwable error) { result.completeExceptionally(error); }
            @Override public void onComplete() { result.complete(null); }
        });
        return result;
    }

    private interface Cursor<T> { T next() throws Exception; boolean done(); }
    private static <T> Flow.Publisher<T> source(Callable<Cursor<T>> factory) {
        return subscriber -> {
            Objects.requireNonNull(subscriber, "subscriber");
            Demand<T> subscription = new Demand<>(subscriber, factory);
            try { subscriber.onSubscribe(subscription); }
            catch (Throwable invalidSubscriber) { subscription.cancel(); return; }
            subscription.worker.start();
        };
    }
    private static final class Demand<T> implements Flow.Subscription {
        private static final VarHandle DEMAND, FAILURE;
        static {
            try {
                var lookup = MethodHandles.lookup();
                DEMAND = lookup.findVarHandle(Demand.class, "demand", long.class);
                FAILURE = lookup.findVarHandle(Demand.class, "failure", Throwable.class);
            } catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
        }
        private final Flow.Subscriber<? super T> subscriber;
        private final Callable<Cursor<T>> factory;
        private final Thread worker;
        private volatile long demand;
        private volatile Throwable failure;
        private volatile boolean cancelled, terminal;
        Demand(Flow.Subscriber<? super T> subscriber, Callable<Cursor<T>> factory) {
            this.subscriber = subscriber; this.factory = factory;
            worker = Thread.ofVirtual().name("cop-stream").unstarted(this::drain);
        }
        @Override public void request(long n) {
            if (cancelled || terminal) return;
            if (n <= 0) FAILURE.compareAndSet(this, null, new IllegalArgumentException("Demand must be positive"));
            else {
                // CAS protocol loop, not a collection transformation.
                long current, next;
                do {
                    current = (long) DEMAND.getVolatile(this);
                    if (current == Long.MAX_VALUE) break;
                    next = current + n;
                    if (next < 0) next = Long.MAX_VALUE;
                } while (!DEMAND.compareAndSet(this, current, next));
            }
            LockSupport.unpark(worker);
        }
        @Override public void cancel() { cancelled = true; LockSupport.unpark(worker); }
        private void consumeDemand() {
            long current;
            do {
                current = (long) DEMAND.getVolatile(this);
                if (current == Long.MAX_VALUE) return;
            } while (!DEMAND.compareAndSet(this, current, current - 1));
        }
        private void drain() {
            Cursor<T> cursor = null;
            try {
                while (!cancelled && !terminal) {
                    if (failure != null) { finish(); return; }
                    if ((long) DEMAND.getVolatile(this) == 0) { LockSupport.park(this); continue; }
                    if (cursor == null) cursor = factory.call();
                    T next = cursor.next();
                    if (cancelled) return;
                    if (failure != null) { finish(); return; }
                    if (next == null) {
                        if (cursor.done()) { finish(); return; }
                        LockSupport.parkNanos(this, 100_000_000L); continue;
                    }
                    consumeDemand();
                    try { subscriber.onNext(next); }
                    catch (Throwable invalidSubscriber) { cancel(); return; }
                    if (cursor.done()) { finish(); return; }
                }
            } catch (Throwable error) { FAILURE.compareAndSet(this, null, error); finish(); }
        }
        private void finish() {
            if (cancelled || terminal) return;
            terminal = true;
            try {
                if (failure == null) subscriber.onComplete(); else subscriber.onError(failure);
            } catch (Throwable invalidSubscriber) { cancel(); }
        }
    }
}
