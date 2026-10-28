/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */

/*
 * @test
 * @summary Lazy authorized object locations, async invalidation and graph lifetime
 * @modules java.base/java.lang:open
 * @build access.M3ObjectAccessBase
 * @run main/othervm -Xmx64m -Djdk.m3.objectEvents.enabled=false M3ObjectTest disabled
 * @run main/othervm -Xmx64m -Djdk.m3.objectEvents.enabled=true M3ObjectTest enabled
 */

import access.M3ObjectAccessBase;

import java.lang.invoke.MethodHandles;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("try") // Subscriptions are intentionally scoped for close side effects.
public class M3ObjectTest {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final long TIMEOUT = 15;

    public static class Holder {
        private Object secret = new Object();
        public volatile Object reference;
        public volatile boolean bool;
        public volatile byte octet;
        public volatile short small;
        public volatile char character;
        public volatile int number;
        public volatile long wide;
        public volatile float single;
        public volatile double real;
        public final int fixed = 7;
        public static int staticField;
    }

    public static class Base {
        public Object value;
    }

    public static class Inherited extends Base {
    }

    public static class Hidden extends Base {
        public Object value;
    }

    public static class ProtectedChild extends M3ObjectAccessBase {
        static MethodHandles.Lookup authority() {
            return MethodHandles.lookup();
        }
    }

    public static class Node {
        public Node left;
        public Node right;
    }

    private static final class HostileIdentity {
        @Override
        public int hashCode() {
            throw new AssertionError("identity registry called hashCode");
        }

        @Override
        public boolean equals(Object other) {
            throw new AssertionError("identity registry called equals");
        }
    }

    private static final class EqualValue {
        @Override
        public boolean equals(Object other) {
            return other instanceof EqualValue;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    private static final class Recorder implements ObjectChangeListener {
        final LinkedBlockingQueue<ObjectEvent> events = new LinkedBlockingQueue<>();
        volatile Thread callbackThread;

        @Override
        public void onChange(ObjectEvent event) {
            callbackThread = Thread.currentThread();
            events.add(event);
        }

        ObjectEvent take() throws InterruptedException {
            ObjectEvent event = events.poll(TIMEOUT, TimeUnit.SECONDS);
            check(event != null, "timed out waiting for event");
            check(callbackThread != Thread.currentThread(), "callback ran inline");
            return event;
        }
    }

    private static final class Blocker implements AutoCloseable {
        final M3Object owner = M3Object.of(new Object());
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final ObjectSubscription subscription = owner.listen(ObjectEvent.CONTENT, event -> {
            entered.countDown();
            awaitUnchecked(release);
        });

        void start() throws InterruptedException {
            owner.contentChanged();
            await(entered);
        }

        @Override
        public void close() {
            release.countDown();
            subscription.close();
        }
    }

    public static void main(String[] args) throws Exception {
        boolean enabled = args.length == 1 && args[0].equals("enabled");
        // The policy is a VM startup value, even before its first lazy read.
        System.setProperty("jdk.m3.objectEvents.enabled", Boolean.toString(!enabled));
        check(M3Object.observationEnabled() == enabled, "policy followed mutable properties");
        laziness(enabled);
        if (enabled) {
            authority();
            referencesAndFailures();
            primitives();
            coalescingAndReentrancy();
            rebindingAndClose();
            aliasesAndCycles();
            concurrentCas();
            ownership();
        }
        System.out.println("M3ObjectTest " + (enabled ? "enabled" : "disabled") + " passed");
    }

    private static void laziness(boolean enabled) throws Exception {
        Object hostile = new HostileIdentity();
        M3Object owner = M3Object.of(hostile);
        check(owner == M3Object.of(hostile), "live facade identity changed");
        check(owner.source() == hostile, "source identity changed");
        expect(NullPointerException.class, () -> M3Object.of(null));
        expect(IllegalArgumentException.class, () -> owner.listen(0, event -> { }));
        expect(IllegalArgumentException.class, () -> owner.listen(8, event -> { }));
        expect(NullPointerException.class, () -> owner.listen(ObjectEvent.ALL, null));
        Holder holder = new Holder();
        M3Object capability = M3Object.of(holder);
        M3Object.Field missing = capability.field(MethodHandles.publicLookup(),
                Holder.class, "doesNotExist", Object.class);
        check(unresolved(missing), "field construction resolved access");
        check(unobserved(capability), "adaptation allocated observation state");
        check(!hasWorker(), "structural operations started dispatcher");
        if (!enabled) {
            ObjectSubscription first = capability.listen(ObjectEvent.ALL, event -> {
                throw new AssertionError("disabled root callback");
            });
            ObjectSubscription second = missing.listen(ObjectEvent.CONTENT, event -> {
                throw new AssertionError("disabled field callback");
            });
            check(first == second && first.isClosed(), "disabled subscription is not shared/inactive");
            first.close();
            first.close();
            check(unresolved(missing), "disabled listen resolved missing field");
            M3Object.Field number = field(capability, Holder.class, "number", int.class);
            number.set(3);
            check(number.compareAndSet(3, 4), "disabled CAS failed");
            check((int) number.get() == 4, "disabled structural mutation unavailable");
            capability.contentChanged();
            check(unobserved(capability), "disabled operation allocated observer state");
            check(!hasWorker(), "disabled operation started dispatcher");
        }
        expect(IllegalArgumentException.class, missing::get);
        expect(IllegalArgumentException.class, () -> capability.field(LOOKUP,
                String.class, "value", byte[].class));
    }

    private static void authority() throws Exception {
        Holder holder = new Holder();
        M3Object owner = M3Object.of(holder);
        M3Object.Field privateField = field(owner, Holder.class, "secret", Object.class);
        Recorder selected = new Recorder();
        Recorder root = new Recorder();
        try (ObjectSubscription selectedSub = privateField.listen(ObjectEvent.REFERENCE, selected);
             ObjectSubscription rootSub = owner.listen(ObjectEvent.ALL, root)) {
            M3Object.Field denied = owner.field(MethodHandles.publicLookup(),
                    Holder.class, "secret", Object.class);
            expect(IllegalArgumentException.class,
                    () -> denied.listen(ObjectEvent.CONTENT, event -> { }));
            expect(IllegalArgumentException.class, denied::get);
            Object replacement = new Object();
            privateField.set(replacement);
            ObjectEvent detail = selected.take();
            ObjectEvent envelope = root.take();
            check(detail.field() == privateField && detail.currentValue() == replacement,
                    "subscriber authority lost");
            check(envelope.field() == null && envelope.source() == holder
                    && envelope.currentValue() == holder, "root leaked selected capability");
        }

        MethodHandles.Lookup protectedLookup = ProtectedChild.authority();
        M3Object.Field wrongReceiver = M3Object.of(new M3ObjectAccessBase()).field(
                protectedLookup, M3ObjectAccessBase.class, "restricted", Object.class);
        expect(ClassCastException.class, wrongReceiver::get);
        expect(ClassCastException.class,
                () -> wrongReceiver.listen(ObjectEvent.ALL, event -> { }));
        M3Object.Field rightReceiver = M3Object.of(new ProtectedChild()).field(
                protectedLookup, M3ObjectAccessBase.class, "restricted", Object.class);
        rightReceiver.set(holder);
        check(rightReceiver.get() == holder, "authorized protected receiver rejected");

        Inherited inherited = new Inherited();
        M3Object inheritedOwner = M3Object.of(inherited);
        M3Object.Field alias = field(inheritedOwner, Inherited.class, "value", Object.class);
        expect(IllegalArgumentException.class, alias::get);
        expect(IllegalArgumentException.class,
                () -> alias.listen(ObjectEvent.ALL, event -> { }));
        M3Object.Field actual = field(inheritedOwner, Base.class, "value", Object.class);
        actual.set(holder);
        check(inherited.value == holder, "actual inherited declaration failed");

        Hidden hidden = new Hidden();
        M3Object hiddenOwner = M3Object.of(hidden);
        M3Object.Field baseField = field(hiddenOwner, Base.class, "value", Object.class);
        M3Object.Field hiddenField = field(hiddenOwner, Hidden.class, "value", Object.class);
        Recorder baseEvents = new Recorder();
        Recorder hiddenEvents = new Recorder();
        try (ObjectSubscription a = baseField.listen(ObjectEvent.REFERENCE, baseEvents);
             ObjectSubscription b = hiddenField.listen(ObjectEvent.REFERENCE, hiddenEvents)) {
            baseField.set(holder);
            hiddenField.set(inherited);
            check(baseEvents.take().field() == baseField, "base slot lost to hiding");
            check(hiddenEvents.take().field() == hiddenField, "hidden slot lost to base");
            check(((Base) hidden).value == holder && hidden.value == inherited,
                    "distinct physical fields conflated");
        }
    }

    private static void referencesAndFailures() throws Exception {
        Holder holder = new Holder();
        M3Object owner = M3Object.of(holder);
        M3Object.Field reference = field(owner, Holder.class, "reference", Object.class);
        Recorder fields = new Recorder();
        Recorder roots = new Recorder();
        try (ObjectSubscription selected = reference.listen(ObjectEvent.REFERENCE, fields);
             ObjectSubscription root = owner.listen(ObjectEvent.ALL, roots)) {
            Object first = new EqualValue();
            Object equal = new EqualValue();
            reference.set(first);
            ObjectEvent initial = fields.take();
            check(initial.mask() == ObjectEvent.REFERENCE && initial.currentValue() == first,
                    "reference publication mismatch");
            check(roots.take().sequence() == 1, "initial sequence mismatch");
            reference.set(equal);
            check(fields.take().currentValue() == equal, "equals suppressed identity transition");
            check(roots.take().sequence() == 2, "second sequence mismatch");
            reference.set(equal);
            check(reference.compareAndSet(equal, equal), "same identity CAS should succeed");
            check(!reference.compareAndSet(first, null), "failed CAS succeeded");
            M3Object.Field fixed = field(owner, Holder.class, "fixed", int.class);
            expect(UnsupportedOperationException.class, () -> fixed.set(8));
            expect(UnsupportedOperationException.class, () -> fixed.compareAndSet(7, 8));
            M3Object.Field number = field(owner, Holder.class, "number", int.class);
            expect(ClassCastException.class, () -> number.set(1L));
            expect(NullPointerException.class, () -> number.set(null));
            expect(ClassCastException.class, () -> number.compareAndSet(0, "bad"));
            M3Object.Field staticField = field(owner, Holder.class, "staticField", int.class);
            expect(IllegalArgumentException.class, () -> staticField.set(1));
            owner.contentChanged();
            ObjectEvent marker = roots.take();
            check(marker.mask() == ObjectEvent.CONTENT && marker.sequence() == 3,
                    "no-op/failure published or mutated sequence");
            check(fields.events.isEmpty(), "field received root-only marker");
            reference.set(null);
            ObjectEvent cleared = fields.take();
            check(cleared.currentValue() == null, "resolved null is not a value");
            check(roots.take().sequence() == 4, "null transition missing");
            reference.set(first);
            fields.take();
            roots.take();
            check(cleared.currentValue() == first, "event did not read lazy latest value");
        }
    }

    private static void primitives() throws Exception {
        Holder holder = new Holder();
        M3Object owner = M3Object.of(holder);
        String[] names = {"bool", "octet", "small", "character", "number", "wide", "single", "real"};
        Class<?>[] types = {boolean.class, byte.class, short.class, char.class,
                int.class, long.class, float.class, double.class};
        Object[] values = {true, (byte) -128, (short) -32768, '\uffff',
                Integer.MIN_VALUE, Long.MIN_VALUE, -0.0f, -0.0d};
        Recorder roots = new Recorder();
        try (ObjectSubscription root = owner.listen(ObjectEvent.ALL, roots)) {
            long sequence = 0;
            for (int i = 0; i < names.length; i++) {
                M3Object.Field selected = field(owner, Holder.class, names[i], types[i]);
                Recorder changes = new Recorder();
                try (ObjectSubscription subscription = selected.listen(ObjectEvent.VALUE, changes)) {
                    selected.set(values[i]);
                    check(changes.take().mask() == ObjectEvent.VALUE, "primitive mask mismatch");
                    check(roots.take().sequence() == ++sequence, "primitive sequence mismatch");
                    selected.set(values[i]);
                    check(selected.compareAndSet(values[i], values[i]), "same-bit CAS failed");
                    owner.contentChanged();
                    ObjectEvent marker = roots.take();
                    check(marker.mask() == ObjectEvent.CONTENT && marker.sequence() == ++sequence,
                            "same primitive bits published");
                }
            }
        }
        M3Object.Field single = field(owner, Holder.class, "single", float.class);
        M3Object.Field real = field(owner, Holder.class, "real", double.class);
        Recorder floating = new Recorder();
        try (ObjectSubscription a = single.listen(ObjectEvent.VALUE, floating);
             ObjectSubscription b = real.listen(ObjectEvent.VALUE, floating)) {
            float nanA = Float.intBitsToFloat(0x7fc0_0001);
            float nanB = Float.intBitsToFloat(0x7fc0_0002);
            single.set(nanA);
            floating.take();
            check(single.compareAndSet(nanA, nanB), "raw NaN CAS failed");
            check(Float.floatToRawIntBits((float) floating.take().currentValue()) == 0x7fc0_0002,
                    "float NaN payload erased");
            double nanC = Double.longBitsToDouble(0x7ff8_0000_0000_0001L);
            double nanD = Double.longBitsToDouble(0x7ff8_0000_0000_0002L);
            real.set(nanC);
            floating.take();
            check(real.compareAndSet(nanC, nanD), "raw double NaN CAS failed");
            check(Double.doubleToRawLongBits((double) floating.take().currentValue())
                    == 0x7ff8_0000_0000_0002L, "double NaN payload erased");
        }
    }

    private static void coalescingAndReentrancy() throws Exception {
        M3Object owner = M3Object.of(new Object());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        Recorder events = new Recorder();
        try (ObjectSubscription sub = owner.listen(ObjectEvent.CONTENT, event -> {
            if (first.getAndSet(false)) {
                entered.countDown();
                awaitUnchecked(release);
            }
            events.onChange(event);
        })) {
            owner.contentChanged();
            await(entered);
            for (int i = 0; i < 4096; i++) {
                owner.contentChanged();
            }
            release.countDown();
            check(events.take().sequence() == 1, "first envelope changed after capture");
            ObjectEvent combined = events.take();
            check(combined.firstSequence() == 2 && combined.sequence() == 4097
                    && combined.coalesced(), "dirty range was dropped or not coalesced");
            check(combined.originId() != 0 && combined.originSequence() == 4097,
                    "same-origin coalescing lost origin");
        } finally {
            release.countDown();
        }

        M3Object reentrant = M3Object.of(new Object());
        AtomicInteger depth = new AtomicInteger();
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        try (ObjectSubscription bad = reentrant.listen(ObjectEvent.CONTENT, event -> {
                 throw new AssertionError("intentional callback failure");
             });
             ObjectSubscription good = reentrant.listen(ObjectEvent.CONTENT, event -> {
                 if (depth.incrementAndGet() != 1) {
                     failure.set(new AssertionError("recursive callback"));
                 }
                 if (calls.incrementAndGet() == 1) reentrant.contentChanged();
                 else done.countDown();
                 depth.decrementAndGet();
             })) {
            reentrant.contentChanged();
            await(done);
            check(failure.get() == null && calls.get() == 2,
                    "reentrancy or failure containment broken");
        }
    }

    private static void rebindingAndClose() throws Exception {
        Node parent = new Node();
        Node oldChild = new Node();
        Node newChild = new Node();
        parent.left = oldChild;
        M3Object owner = M3Object.of(parent);
        M3Object oldOwner = M3Object.of(oldChild);
        M3Object newOwner = M3Object.of(newChild);
        M3Object.Field left = field(owner, Node.class, "left", Node.class);
        Recorder fieldEvents = new Recorder();
        Recorder rootEvents = new Recorder();
        try (ObjectSubscription selected = left.listen(ObjectEvent.ALL, fieldEvents);
             ObjectSubscription root = owner.listen(ObjectEvent.ALL, rootEvents)) {
            oldOwner.contentChanged();
            long generation = fieldEvents.take().generation();
            rootEvents.take();
            try (Blocker blocker = new Blocker()) {
                blocker.start();
                oldOwner.contentChanged(); // queued content for generation one
                left.set(newChild);          // invalidates that queued content
                oldOwner.contentChanged();  // detached origin must not reach parent
                newOwner.contentChanged();
            }
            ObjectEvent event = fieldEvents.take();
            rootEvents.take();
            check(event.firstSequence() == 3 && event.sequence() == 4,
                    "stale generation remained in selected pending range");
            check(event.mask() == (ObjectEvent.REFERENCE | ObjectEvent.CONTENT)
                    && event.generation() > generation && event.currentValue() == newChild,
                    "rebind did not follow new child");
            check(event.originId() == 0 && event.originSequence() == 0,
                    "mixed origins exposed an invented exact origin");
        }

        Recorder content = new Recorder();
        Recorder root = new Recorder();
        try (ObjectSubscription selected = left.listen(ObjectEvent.CONTENT, content);
             ObjectSubscription rootSub = owner.listen(ObjectEvent.ALL, root)) {
            try (Blocker blocker = new Blocker()) {
                blocker.start();
                newOwner.contentChanged();
                left.set(oldChild);
            }
            root.take();
            check(content.events.isEmpty(), "detached pending child event was delivered");
            try (Blocker blocker = new Blocker()) {
                blocker.start();
                oldOwner.contentChanged();
                selected.close();
                selected.close();
                check(selected.isClosed(), "close is not visible");
            }
            root.take();
            check(content.events.isEmpty(), "closed subscription admitted queued callback");
        }
    }

    private static void aliasesAndCycles() throws Exception {
        Node a = new Node();
        Node b = new Node();
        a.left = b;
        a.right = b;
        b.left = a;
        M3Object ao = M3Object.of(a);
        M3Object bo = M3Object.of(b);
        M3Object.Field al = field(ao, Node.class, "left", Node.class);
        M3Object.Field ar = field(ao, Node.class, "right", Node.class);
        M3Object.Field bl = field(bo, Node.class, "left", Node.class);
        Recorder left = new Recorder();
        Recorder right = new Recorder();
        Recorder back = new Recorder();
        Recorder root = new Recorder();
        try (ObjectSubscription x = al.listen(ObjectEvent.CONTENT, left);
             ObjectSubscription y = ar.listen(ObjectEvent.CONTENT, right);
             ObjectSubscription z = bl.listen(ObjectEvent.CONTENT, back);
             ObjectSubscription r = ao.listen(ObjectEvent.CONTENT, root)) {
            bo.contentChanged();
            ObjectEvent l = left.take();
            ObjectEvent rr = right.take();
            ObjectEvent bb = back.take();
            ObjectEvent envelope = root.take();
            check(l.field() == al && rr.field() == ar && bb.field() == bl,
                    "alias or cycle selected field lost");
            check(l.originId() != 0 && l.originId() == rr.originId()
                    && l.originId() == bb.originId(), "origin changed during propagation");
            check(l.sequence() == 1 && rr.sequence() == 1 && bb.sequence() == 1,
                    "cycle expanded an owner more than once");
            check(!envelope.coalesced() && envelope.field() == null,
                    "two aliases were treated as two originating publications");
        }
    }

    private static void concurrentCas() throws Exception {
        Holder holder = new Holder();
        M3Object owner = M3Object.of(holder);
        M3Object.Field value = field(owner, Holder.class, "number", int.class);
        Recorder changes = new Recorder();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> writers = new ArrayList<>();
        try (ObjectSubscription subscription = owner.listen(ObjectEvent.ALL, changes)) {
            for (int w = 0; w < 4; w++) {
                Thread thread = new Thread(() -> {
                    try {
                        await(start);
                        for (int i = 0; i < 500; i++) {
                            for (;;) {
                                int previous = (int) value.get();
                                if (value.compareAndSet(previous, previous + 1)) break;
                            }
                        }
                    } catch (Throwable ex) {
                        failure.compareAndSet(null, ex);
                    }
                }, "M3Object-writer-" + w);
                writers.add(thread);
                thread.start();
            }
            start.countDown();
            for (Thread writer : writers) {
                writer.join(TimeUnit.SECONDS.toMillis(TIMEOUT));
                check(!writer.isAlive(), "mutator blocked on user callback");
            }
            check(failure.get() == null, "concurrent writer failed: " + failure.get());
            check(holder.number == 2000, "atomic updates lost");
            owner.contentChanged();
            long last = 0;
            while (last < 2001) {
                ObjectEvent event = changes.take();
                check(event.firstSequence() == last + 1, "publication range gap or duplicate");
                check(!writers.contains(changes.callbackThread), "callback on mutator thread");
                last = event.sequence();
            }
            check(last == 2001, "failed CAS published an extra sequence");
        }
    }

    private static void ownership() throws Exception {
        Object payload = new Object();
        WeakReference<Object> retained = new WeakReference<>(payload);
        ObjectSubscription subscription = M3Object.of(payload).listen(ObjectEvent.CONTENT,
                event -> { });
        payload = null;
        System.gc();
        check(retained.get() != null, "live subscription lost its owner");
        Reference.reachabilityFence(subscription);
        subscription.close();
        awaitCollected(retained);
        List<WeakReference<Object>> graph = abandonedCycle();
        for (WeakReference<Object> reference : graph) {
            awaitCollected(reference);
        }
    }

    private static List<WeakReference<Object>> abandonedCycle() {
        Node a = new Node();
        Node b = new Node();
        a.left = b;
        b.left = a;
        M3Object ao = M3Object.of(a);
        M3Object bo = M3Object.of(b);
        field(ao, Node.class, "left", Node.class).listen(ObjectEvent.CONTENT, event -> { });
        field(bo, Node.class, "left", Node.class).listen(ObjectEvent.CONTENT, event -> { });
        return List.of(new WeakReference<>(a), new WeakReference<>(b));
    }

    private static void awaitCollected(WeakReference<?> reference) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT);
        while (reference.get() != null && System.nanoTime() < deadline) {
            System.gc();
            Thread.sleep(20);
        }
        check(reference.get() == null, "registry or idle dispatch retained abandoned payload");
    }

    private static M3Object.Field field(M3Object owner, Class<?> declaringType,
                                        String name, Class<?> type) {
        return owner.field(LOOKUP, declaringType, name, type);
    }

    private static boolean unobserved(M3Object owner) throws Exception {
        java.lang.reflect.Field state = M3Object.class.getDeclaredField("state");
        state.setAccessible(true);
        return state.get(owner) == null;
    }

    private static boolean unresolved(M3Object.Field field) throws Exception {
        java.lang.reflect.Field planField = M3Object.Field.class.getDeclaredField("plan");
        planField.setAccessible(true);
        Object plan = planField.get(field);
        java.lang.reflect.Field handle = plan.getClass().getDeclaredField("handle");
        handle.setAccessible(true);
        return handle.get(plan) == null;
    }

    private static boolean hasWorker() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(thread -> thread.getName().equals("M3Object-dispatch"));
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        check(latch.await(TIMEOUT, TimeUnit.SECONDS), "latch timed out");
    }

    private static void awaitUnchecked(CountDownLatch latch) {
        try {
            await(latch);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }

    private static void expect(Class<? extends Throwable> type, CheckedAction action) throws Exception {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
