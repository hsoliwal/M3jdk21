/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary M3Object explicit JNI producers share asynchronous Java observation
 * @run main/othervm/native -Xcheck:jni -Djdk.m3.objectEvents.enabled=false M3ObjectNativeTest disabled
 * @run main/othervm/native -Xcheck:jni -Djdk.m3.objectEvents.enabled=true M3ObjectNativeTest enabled
 */

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class M3ObjectNativeTest {
    private static final class Holder {
        Object reference;
        int value;
    }

    private static final class Capture implements ObjectChangeListener {
        private final Thread producer = Thread.currentThread();
        private final BlockingQueue<ObjectEvent> events = new LinkedBlockingQueue<>();
        private volatile Throwable failure;

        @Override
        public void onChange(ObjectEvent event) {
            if (Thread.currentThread() == producer) {
                failure = new AssertionError("listener executed in native producer thread");
            }
            events.add(event);
        }

        ObjectEvent take() throws InterruptedException {
            ObjectEvent event = events.poll(30, TimeUnit.SECONDS);
            if (failure != null) {
                throw new AssertionError("asynchronous delivery failed", failure);
            }
            check(event != null, "timed out waiting for native publication");
            return event;
        }
    }

    private static native boolean writeReference(Holder source, Object value,
                                                  Class<?> declaringType,
                                                  String fieldName, Class<?> fieldType);
    private static native boolean writeValue(Holder source, int value,
                                              Class<?> declaringType,
                                              String fieldName, Class<?> fieldType);
    private static native void storeReferenceOnly(Holder source, Object value);
    private static native boolean writeByte(byte[] source, int index, byte value);
    private static native boolean notifyContent(Object source);
    private static native boolean notifyField(Object source, Class<?> declaringType,
                                               String fieldName, Class<?> fieldType,
                                               int mask);
    private static native boolean pendingIsPreserved(Object source, Throwable expected);
    private static native int mutateMany(byte[] source, int count);

    public static void main(String[] args) throws Exception {
        System.loadLibrary("M3ObjectNativeTest");
        boolean enabled = args.length == 1 && args[0].equals("enabled");
        check(M3Object.observationEnabled() == enabled, "startup observation setting");
        validateNativeArguments();
        if (enabled) {
            fieldsAndDownstreamContent();
            lateReferencePublication();
            rootOnlyNativeField();
            repeatedNativeFrame();
        } else {
            disabledAndUnobserved();
        }
        System.out.println("M3ObjectNativeTest " + (enabled ? "enabled" : "disabled") + " passed");
    }

    private static M3Object.Field field(M3Object owner, String name, Class<?> type) {
        return owner.field(MethodHandles.lookup(), Holder.class, name, type);
    }

    private static boolean setReference(Holder source, Object value) {
        return writeReference(source, value, Holder.class, "reference", Object.class);
    }

    private static boolean setValue(Holder source, int value) {
        return writeValue(source, value, Holder.class, "value", int.class);
    }

    private static void validateNativeArguments() {
        Holder holder = new Holder();
        expect(NullPointerException.class, () -> notifyContent(null));
        expect(NullPointerException.class,
               () -> notifyField(null, Holder.class, "reference", Object.class,
                                 ObjectEvent.REFERENCE));
        expect(NullPointerException.class,
               () -> notifyField(holder, null, "reference", Object.class,
                                 ObjectEvent.REFERENCE));
        expect(NullPointerException.class,
               () -> notifyField(holder, Holder.class, null, Object.class,
                                 ObjectEvent.REFERENCE));
        expect(NullPointerException.class,
               () -> notifyField(holder, Holder.class, "reference", null,
                                 ObjectEvent.REFERENCE));
        expect(IllegalArgumentException.class,
               () -> notifyField(holder, Holder.class, "reference", Object.class,
                                 ObjectEvent.CONTENT));
        expect(NullPointerException.class, () -> setReference(null, holder));
        expect(NullPointerException.class, () -> setValue(null, 1));
        expect(NullPointerException.class, () -> writeByte(null, 0, (byte) 1));
    }

    private static void disabledAndUnobserved() {
        Holder holder = new Holder();
        M3Object owner = M3Object.of(holder);
        M3Object.Field reference = field(owner, "reference", Object.class);
        AtomicInteger callbacks = new AtomicInteger();
        ObjectSubscription rootSubscription = owner.listen(ObjectEvent.ALL,
                                                            e -> callbacks.incrementAndGet());
        ObjectSubscription fieldSubscription = reference.listen(ObjectEvent.ALL,
                                                                  e -> callbacks.incrementAndGet());
        check(rootSubscription.isClosed() && fieldSubscription.isClosed(),
              "disabled subscriptions must be inactive");
        byte[] bytes = new byte[1];
        check(setReference(holder, bytes), "disabled native reference mutation");
        check(setValue(holder, 9), "disabled native value mutation");
        check(writeByte(bytes, 0, (byte) 3), "disabled native content mutation");
        check(holder.reference == bytes && holder.value == 9 && bytes[0] == 3,
              "disabled observation must retain native mutation behavior");
        check(!setReference(holder, bytes) && !setValue(holder, 9) &&
              !writeByte(bytes, 0, (byte) 3), "producer suppresses unchanged values");
        check(pendingIsPreserved(bytes, new IllegalStateException("native sentinel")),
              "disabled bridge preserves pending exception identity");
        System.setProperty("jdk.m3.objectEvents.enabled", "true");
        check(!M3Object.observationEnabled(), "startup property must not become a live toggle");
        check(writeByte(bytes, 0, (byte) 4), "mutation after property edit");
        check(callbacks.get() == 0, "disabled native mutation delivered a callback");
        rootSubscription.close();
        fieldSubscription.close();
    }

    private static void fieldsAndDownstreamContent() throws Exception {
        Holder holder = new Holder();
        M3Object owner = M3Object.of(holder);
        M3Object.Field reference = field(owner, "reference", Object.class);
        M3Object.Field value = field(owner, "value", int.class);
        Capture rootEvents = new Capture();
        Capture referenceEvents = new Capture();
        Capture valueEvents = new Capture();
        ObjectSubscription rootSubscription = owner.listen(ObjectEvent.ALL, rootEvents);
        ObjectSubscription referenceSubscription = reference.listen(ObjectEvent.ALL, referenceEvents);
        ObjectSubscription valueSubscription = value.listen(ObjectEvent.VALUE, valueEvents);
        try {
            byte[] firstChild = new byte[1];
            check(setReference(holder, firstChild), "native reference transition");
            ObjectEvent firstReference = event(referenceEvents, holder, reference,
                                               ObjectEvent.REFERENCE, 1, firstChild);
            event(rootEvents, holder, null, ObjectEvent.REFERENCE, 1, holder);

            check(!setReference(holder, firstChild), "same-reference write must not publish");
            expect(IllegalArgumentException.class,
                   () -> notifyField(holder, Holder.class, "reference", Object.class,
                                     ObjectEvent.VALUE));
            expect(IllegalArgumentException.class,
                   () -> notifyField(holder, Holder.class, "value", void.class,
                                     ObjectEvent.VALUE));
            expect(IllegalArgumentException.class,
                   () -> notifyField(new Object(), Holder.class, "reference", Object.class,
                                     ObjectEvent.REFERENCE));
            check(setReference(new Holder(), new Object()), "unobserved owner can mutate");

            check(setValue(holder, 7), "native primitive transition");
            event(valueEvents, holder, value, ObjectEvent.VALUE, 2, 7);
            event(rootEvents, holder, null, ObjectEvent.VALUE, 2, holder);
            check(!setValue(holder, 7), "same-value write must not publish");

            check(writeByte(firstChild, 0, (byte) 1), "native downstream content mutation");
            ObjectEvent content = event(referenceEvents, holder, reference,
                                        ObjectEvent.CONTENT, 3, firstChild);
            event(rootEvents, holder, null, ObjectEvent.CONTENT, 3, holder);
            check(content.generation() == firstReference.generation(), "live edge generation");
            check(firstChild[0] == 1, "native content committed before delivery");

            check(!writeByte(firstChild, 0, (byte) 1), "same-byte write must not publish");
            expect(IndexOutOfBoundsException.class,
                   () -> writeByte(firstChild, firstChild.length, (byte) 2));
            check(firstChild[0] == 1, "failed native operation changed content");
            check(pendingIsPreserved(firstChild, new IllegalStateException("native sentinel")),
                  "bridge replaced or cleared the pending native exception");
            check(writeByte(firstChild, 0, (byte) 2), "marker after no-op/failure/pending exception");
            event(referenceEvents, holder, reference, ObjectEvent.CONTENT, 4, firstChild);
            event(rootEvents, holder, null, ObjectEvent.CONTENT, 4, holder);

            byte[] secondChild = new byte[1];
            check(setReference(holder, secondChild), "native reference rebinding");
            ObjectEvent replacement = event(referenceEvents, holder, reference,
                                            ObjectEvent.REFERENCE, 5, secondChild);
            event(rootEvents, holder, null, ObjectEvent.REFERENCE, 5, holder);
            check(replacement.generation() > firstReference.generation(), "rebind generation");
            check(writeByte(firstChild, 0, (byte) 3), "detached child can still mutate");
            check(writeByte(secondChild, 0, (byte) 1), "replacement child content mutation");
            ObjectEvent replacementContent = event(referenceEvents, holder, reference,
                                                   ObjectEvent.CONTENT, 6, secondChild);
            event(rootEvents, holder, null, ObjectEvent.CONTENT, 6, holder);
            check(replacementContent.generation() == replacement.generation(),
                  "detached generation was attributed to replacement");
        } finally {
            valueSubscription.close();
            referenceSubscription.close();
            rootSubscription.close();
        }
    }

    private static void rootOnlyNativeField() throws Exception {
        Holder holder = new Holder();
        M3Object owner = M3Object.of(holder);
        Capture capture = new Capture();
        ObjectSubscription subscription = owner.listen(ObjectEvent.ALL, capture);
        try {
            // No Field capability or slot has been requested for this owner.
            byte[] child = new byte[1];
            check(setReference(holder, child), "root-only native reference mutation");
            event(capture, holder, null, ObjectEvent.REFERENCE, 1, holder);
            check(setValue(holder, 1), "root-only native value mutation");
            event(capture, holder, null, ObjectEvent.VALUE, 2, holder);
            check(writeByte(child, 0, (byte) 1), "unfollowed child can mutate");
            check(setValue(holder, 2), "marker after unfollowed child mutation");
            event(capture, holder, null, ObjectEvent.VALUE, 3, holder);
        } finally {
            subscription.close();
        }
    }

    private static void lateReferencePublication() throws Exception {
        Holder holder = new Holder();
        byte[] oldChild = new byte[1];
        byte[] newChild = new byte[1];
        holder.reference = oldChild;
        M3Object owner = M3Object.of(holder);
        M3Object.Field reference = field(owner, "reference", Object.class);
        M3Object oldOwner = M3Object.of(oldChild);
        M3Object newOwner = M3Object.of(newChild);
        Capture selectedEvents = new Capture();
        Capture rootEvents = new Capture();
        Capture oldEvents = new Capture();
        Capture newEvents = new Capture();
        ObjectSubscription selected = reference.listen(ObjectEvent.CONTENT, selectedEvents);
        ObjectSubscription root = owner.listen(ObjectEvent.ALL, rootEvents);
        ObjectSubscription oldRoot = oldOwner.listen(ObjectEvent.CONTENT, oldEvents);
        ObjectSubscription newRoot = newOwner.listen(ObjectEvent.CONTENT, newEvents);
        M3Object gate = M3Object.of(new Object());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ObjectSubscription blocker = gate.listen(ObjectEvent.CONTENT, ignored -> {
            entered.countDown();
            try {
                check(release.await(30, TimeUnit.SECONDS), "dispatch blocker timed out");
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new AssertionError(failure);
            }
        });
        try {
            check(notifyContent(oldChild), "initial old-child publication");
            ObjectEvent initial = event(selectedEvents, holder, reference,
                                        ObjectEvent.CONTENT, 1, oldChild);
            event(rootEvents, holder, null, ObjectEvent.CONTENT, 1, holder);
            ObjectEvent oldOrigin = event(oldEvents, oldChild, null,
                                          ObjectEvent.CONTENT, 1, oldChild);
            gate.contentChanged();
            check(entered.await(30, TimeUnit.SECONDS), "dispatcher did not enter blocker");

            // Commit the real JNI store, deliberately delaying its producer hook.
            storeReferenceOnly(holder, newChild);
            check(holder.reference == newChild, "split JNI store did not commit");
            check(notifyContent(oldChild), "old child triggers current-edge reconciliation");
            check(writeByte(newChild, 0, (byte) 1), "new child publishes before late reference hook");
            check(notifyField(holder, Holder.class, "reference", Object.class,
                              ObjectEvent.REFERENCE), "late reference producer hook");
            release.countDown();

            ObjectEvent retained = event(selectedEvents, holder, reference,
                                         ObjectEvent.CONTENT, 2, newChild);
            ObjectEvent currentOrigin = event(newEvents, newChild, null,
                                              ObjectEvent.CONTENT, 1, newChild);
            event(oldEvents, oldChild, null, ObjectEvent.CONTENT, 2, oldChild);
            ObjectEvent combined = rootEvents.take();
            check(combined.mask() == (ObjectEvent.REFERENCE | ObjectEvent.CONTENT)
                  && combined.firstSequence() == 2 && combined.sequence() == 3,
                  "late reference root invalidation range");
            check(retained.originId() == currentOrigin.originId()
                  && retained.originSequence() == currentOrigin.originSequence()
                  && retained.originId() != oldOrigin.originId(),
                  "retained content came from detached child");
            check(retained.generation() > initial.generation(),
                  "retained content kept a detached generation");

            check(writeByte(newChild, 0, (byte) 2), "current child after late hook");
            ObjectEvent next = event(selectedEvents, holder, reference,
                                     ObjectEvent.CONTENT, 4, newChild);
            check(next.generation() == retained.generation(),
                  "preserved pending content did not adopt current edge generation");
            event(rootEvents, holder, null, ObjectEvent.CONTENT, 4, holder);
            event(newEvents, newChild, null, ObjectEvent.CONTENT, 2, newChild);
        } finally {
            release.countDown();
            blocker.close();
            newRoot.close();
            oldRoot.close();
            root.close();
            selected.close();
        }
    }

    private static void repeatedNativeFrame() throws Exception {
        byte[] source = new byte[1];
        M3Object owner = M3Object.of(source);
        Capture capture = new Capture();
        ObjectSubscription subscription = owner.listen(ObjectEvent.CONTENT, capture);
        try {
            int count = 4096;
            check(mutateMany(source, count) == count, "native loop completed");
            long last = 0;
            while (last < count) {
                ObjectEvent event = capture.take();
                check(event.source() == source && event.field() == null,
                      "native loop event authority");
                check(event.mask() == ObjectEvent.CONTENT, "native loop event mask");
                check(event.firstSequence() == last + 1 && event.sequence() >= event.firstSequence(),
                      "native loop lost or repeated an accepted publication");
                check(event.sequence() <= count, "native loop invented publications");
                check(event.coalesced() == (event.firstSequence() != event.sequence()),
                      "native loop coalescing metadata");
                last = event.sequence();
            }
            check(source[0] == 0, "native loop final content");
        } finally {
            subscription.close();
        }
    }

    private static ObjectEvent event(Capture capture, Object source,
                                     M3Object.Field field, int mask, long sequence,
                                     Object currentValue) throws InterruptedException {
        ObjectEvent event = capture.take();
        check(event.source() == source && event.field() == field, "event source/authority");
        check(event.mask() == mask, "event mask: " + event.mask() + " expected " + mask);
        check(event.firstSequence() == sequence && event.sequence() == sequence,
              "event sequence: " + event.firstSequence() + ".." + event.sequence() +
              " expected " + sequence);
        check(!event.coalesced(), "serialized single publication was coalesced");
        Object actual = event.currentValue();
        check(field != null && field.type().isPrimitive()
              ? Objects.equals(actual, currentValue) : actual == currentValue,
              "event lazy current value");
        check(event.originId() != 0 && event.originSequence() > 0, "event origin");
        return event;
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) {
                return;
            }
            throw new AssertionError("expected " + type.getName(), failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
