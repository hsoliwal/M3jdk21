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
 * @test id=serial
 * @summary Weak object and access-plan caches release discarded class loaders;
 *          retained fields remain authorized and usable after cache eviction
 * @requires vm.gc.Serial
 * @modules java.base/java.lang:open
 * @run main/othervm -Xmx64m -XX:+UseSerialGC -XX:+ClassUnloading M3ObjectLoaderLifetimeTest
 */

/*
 * @test id=g1
 * @summary Weak object and access-plan caches release discarded class loaders;
 *          retained fields remain authorized and usable after cache eviction
 * @requires vm.gc.G1
 * @modules java.base/java.lang:open
 * @run main/othervm -Xmx64m -XX:+UseG1GC -XX:+ClassUnloading M3ObjectLoaderLifetimeTest
 */

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class M3ObjectLoaderLifetimeTest {
    private static final String PAYLOAD_NAME =
            "M3ObjectLoaderLifetimeTest$Payload";
    private static final int INITIAL_VALUE = 41;
    private static final int UPDATED_VALUE = 42;
    private static final int FINAL_VALUE = 43;
    private static final int MAXIMUM_PLAN_CAPACITY = 256;
    private static final int CHURN_AUTHORITIES = MAXIMUM_PLAN_CAPACITY * 2;
    private static final int GC_ATTEMPTS = 120;
    private static final int GC_PAUSE_MILLIS = 25;
    private static final int PRESSURE_BLOCKS = 8;
    private static final int PRESSURE_BLOCK_BYTES = 256 * 1024;
    private static final Field FIELD_PLAN = openedField(M3Object.Field.class, "plan");
    private static final Class<?> RUNTIME = runtimeClass();
    private static final Field RUNTIME_LOCK = openedField(RUNTIME, "LOCK");
    private static final Field RUNTIME_PLANS = openedField(RUNTIME, "plans");
    private static volatile Object allocationSink;
    private static int checks;

    /** Its bytes, rather than its Class object, are defined by each child loader. */
    public static final class Payload {
        private volatile int value = INITIAL_VALUE;

        public Payload() { }

        public static MethodHandles.Lookup authority() {
            return MethodHandles.lookup();
        }
    }

    private static final class Filler {
        private volatile int value;
    }

    private static final class PayloadLoader extends ClassLoader {
        PayloadLoader() {
            super(M3ObjectLoaderLifetimeTest.class.getClassLoader());
        }

        Class<?> definePayload(byte[] bytes) {
            // Direct definition deliberately bypasses parent delegation for this
            // one name. The parent has no reference back to the child loader.
            return defineClass(PAYLOAD_NAME, bytes, 0, bytes.length);
        }
    }

    private record Tracked(String name, WeakReference<Object> reference) { }

    private record Witness(String scenario, List<Tracked> tracked) {
        String remaining() {
            StringBuilder result = new StringBuilder();
            for (Tracked value : tracked) {
                // refersTo(null) does not transiently strengthen the referent.
                if (!value.reference().refersTo(null)) {
                    if (!result.isEmpty()) result.append(", ");
                    result.append(value.name());
                }
            }
            return result.toString();
        }

        void assertAllLive() {
            for (Tracked value : tracked) {
                check(!value.reference().refersTo(null),
                        scenario + " lost live " + value.name());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        byte[] bytes;
        try (InputStream input = M3ObjectLoaderLifetimeTest.class.getResourceAsStream(
                "/" + PAYLOAD_NAME + ".class")) {
            check(input != null, "compiled nested payload resource is missing");
            bytes = input.readAllBytes();
        }

        // The helper frame ends before collection. No cache churn can hide a
        // strong retention bug by removing the discarded entry first.
        awaitCollected(discardSharedFields(bytes));
        awaitCollected(retainFieldAcrossEviction(bytes));
        System.out.println("M3ObjectLoaderLifetimeTest passed: " + checks + " assertions");
    }

    private static Witness discardSharedFields(byte[] bytes) throws Exception {
        PayloadLoader loader = new PayloadLoader();
        Class<?> type = loader.definePayload(bytes);
        check(type.getClassLoader() == loader, "payload was delegated to its parent");
        Object source = type.getConstructor().newInstance();
        MethodHandles.Lookup authority =
                (MethodHandles.Lookup) type.getMethod("authority").invoke(null);
        check(authority.lookupClass() == type, "authority belongs to another class");
        M3Object facade = M3Object.of(source);
        M3Object.Field first = facade.field(authority, type, "value", int.class);
        M3Object.Field second = facade.field(authority, type, "value", int.class);
        Object plan = FIELD_PLAN.get(first);
        check(plan == FIELD_PLAN.get(second), "exact Lookup did not share its plan");
        check((int) first.get() == INITIAL_VALUE, "child private field read failed");
        second.set(UPDATED_VALUE);
        check((int) first.get() == UPDATED_VALUE, "shared child field write failed");
        check(cacheContains(first), "fixture did not leave its plan in the cache");
        Witness witness = witness("discarded shared fields", loader, type, authority,
                source, facade, plan, first, second);
        witness.assertAllLive();
        return witness;
    }

    private static Witness retainFieldAcrossEviction(byte[] bytes) throws Exception {
        // createRetained returns only one Field as a strong child-loader root.
        M3Object.Field retained = createRetained(bytes);
        Witness witness = witness("released evicted field", retained);
        evict(retained);
        for (int attempt = 0; attempt < 3; attempt++) gcRound();
        witness.assertAllLive();
        check((int) retained.get() == INITIAL_VALUE, "evicted field lost read authority");
        retained.set(UPDATED_VALUE);
        check(retained.compareAndSet(UPDATED_VALUE, FINAL_VALUE),
                "evicted field lost compare-and-set authority");
        check((int) retained.get() == FINAL_VALUE, "evicted field lost write authority");
        Reference.reachabilityFence(retained);
        return witness;
    }

    private static M3Object.Field createRetained(byte[] bytes) throws Exception {
        PayloadLoader loader = new PayloadLoader();
        Class<?> type = loader.definePayload(bytes);
        check(type.getClassLoader() == loader, "retained payload was delegated");
        Object source = type.getConstructor().newInstance();
        MethodHandles.Lookup authority =
                (MethodHandles.Lookup) type.getMethod("authority").invoke(null);
        M3Object facade = M3Object.of(source);
        M3Object.Field field = facade.field(authority, type, "value", int.class);
        M3Object.Field shared = facade.field(authority, type, "value", int.class);
        check(FIELD_PLAN.get(field) == FIELD_PLAN.get(shared),
                "retained exact Lookup did not share its plan");
        check((int) field.get() == INITIAL_VALUE, "retained child field did not resolve");
        return field;
    }

    private static Witness witness(String scenario, M3Object.Field field) throws Exception {
        Object plan = FIELD_PLAN.get(field);
        Object authority = openedField(plan.getClass(), "lookup").get(plan);
        Object facade = openedField(M3Object.Field.class, "owner").get(field);
        Class<?> type = field.declaringType();
        return witness(scenario, type.getClassLoader(), type, authority, field.source(),
                facade, plan, field);
    }

    private static Witness witness(String scenario, Object... values) {
        String[] names = {"loader", "class", "Lookup", "source", "facade", "plan",
                "first Field", "second Field"};
        List<Tracked> tracked = new ArrayList<>(values.length);
        for (int index = 0; index < values.length; index++) {
            tracked.add(new Tracked(names[index], new WeakReference<>(values[index])));
        }
        return new Witness(scenario, List.copyOf(tracked));
    }

    private static void evict(M3Object.Field retained) throws Exception {
        M3Object filler = M3Object.of(new Filler());
        List<M3Object.Field> live = new ArrayList<>(CHURN_AUTHORITIES);
        for (int index = 0; index < CHURN_AUTHORITIES; index++) {
            MethodHandles.Lookup authority = MethodHandles.privateLookupIn(
                    Filler.class, MethodHandles.lookup());
            M3Object.Field field = filler.field(authority, Filler.class, "value", int.class);
            // Three uses outrank the retained plan's two uses. Holding every
            // Field keeps weak entries live, so capacity must evict a real plan.
            filler.field(authority, Filler.class, "value", int.class);
            filler.field(authority, Filler.class, "value", int.class);
            live.add(field);
        }
        check(!cacheContains(retained), "fixture failed to evict the retained plan");
        synchronized (RUNTIME_LOCK.get(null)) {
            Object[] plans = (Object[]) RUNTIME_PLANS.get(null);
            check(plans.length <= MAXIMUM_PLAN_CAPACITY, "plan capacity exceeded its bound");
        }
        Reference.reachabilityFence(live);
    }

    private static boolean cacheContains(M3Object.Field field) throws Exception {
        Object selected = FIELD_PLAN.get(field);
        synchronized (RUNTIME_LOCK.get(null)) {
            Object[] plans = (Object[]) RUNTIME_PLANS.get(null);
            for (Object slot : plans) {
                if (slot instanceof WeakReference<?> reference && reference.get() == selected) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void awaitCollected(Witness witness) throws Exception {
        for (int attempt = 0; attempt < GC_ATTEMPTS; attempt++) {
            if (witness.remaining().isEmpty()) {
                checks++;
                return;
            }
            gcRound();
        }
        throw new AssertionError(witness.scenario() + " retained " + witness.remaining()
                + " after " + GC_ATTEMPTS + " GC attempts");
    }

    private static void gcRound() throws InterruptedException {
        byte[][] pressure = new byte[PRESSURE_BLOCKS][];
        for (int index = 0; index < pressure.length; index++) {
            pressure[index] = new byte[PRESSURE_BLOCK_BYTES];
        }
        allocationSink = pressure;
        allocationSink = null;
        System.gc();
        Thread.sleep(GC_PAUSE_MILLIS);
    }

    private static Field openedField(Class<?> owner, String name) {
        try {
            Field result = owner.getDeclaredField(name);
            result.setAccessible(true);
            return result;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Class<?> runtimeClass() {
        try {
            return Class.forName("java.lang.M3ObjectRuntime");
        } catch (ClassNotFoundException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
