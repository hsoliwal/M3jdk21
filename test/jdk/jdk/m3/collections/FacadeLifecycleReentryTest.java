// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Reentrant callback closure must not retain GC-eligible idle roots. */
//FLLDCIM: RELOCATE - donor blob 0cded62297c5 at fd9a233680b4; recipe make/m3/collections/recipe/transient-facade-20261106/relocate.py v4
class FacadeLifecycleReentryTest {
    private static final class Facade {
        long owner;
        Facade(long owner) { this.owner = owner; }
    }

    @Test
    void scrubClosingOwnerCannotRepublishIdleRoot() throws ReflectiveOperationException {
        AtomicReference<TransientFacadeStore<Facade>> ref = new AtomicReference<>();
        AtomicInteger disposals = new AtomicInteger();
        TransientFacadeStore.Recycler<Facade> recycler = new TransientFacadeStore.Recycler<>() {
            @Override public void scrub(Facade facade) { facade.owner = -1; ref.get().close(); }
            @Override public void rebind(Facade facade, long id) { facade.owner = id; }
        };
        TransientFacadeStore<Facade> owner = new TransientFacadeStore<>(
                Facade::new, (id, facade) -> disposals.incrementAndGet(),
                TransientFacadeStore.DEFAULT_MODE, recycler, 4);
        ref.set(owner);
        owner.withInternal(1L, value -> assertEquals(1L, value.owner));
        assertEquals(1, disposals.get());
        assertThrows(IllegalStateException.class, owner::reusableIdleCount);
        assertEquals(0, idleArrayLength(owner));
    }

    @Test
    void rebindClosingOwnerCannotInvokeDetachedConsumer() {
        AtomicReference<TransientFacadeStore<Facade>> ref = new AtomicReference<>();
        AtomicInteger disposals = new AtomicInteger();
        AtomicInteger actions = new AtomicInteger();
        TransientFacadeStore.Recycler<Facade> recycler = new TransientFacadeStore.Recycler<>() {
            @Override public void scrub(Facade facade) { facade.owner = -1; }
            @Override public void rebind(Facade facade, long id) {
                facade.owner = id;
                if (id == 2L) ref.get().close();
            }
        };
        TransientFacadeStore<Facade> owner = new TransientFacadeStore<>(
                Facade::new, (id, facade) -> disposals.incrementAndGet(),
                TransientFacadeStore.DEFAULT_MODE, recycler, 4);
        ref.set(owner);
        owner.withInternal(1L, value -> actions.incrementAndGet());
        assertEquals(1, owner.reusableIdleCount());
        assertThrows(IllegalStateException.class,
                () -> owner.withInternal(2L, value -> actions.incrementAndGet()));
        assertEquals(1, actions.get());
        assertEquals(1, disposals.get());
    }

    @Test
    void factoryClosingOwnerCannotExposeFacade() {
        AtomicReference<TransientFacadeStore<Facade>> ref = new AtomicReference<>();
        AtomicInteger disposals = new AtomicInteger();
        TransientFacadeStore<Facade> owner = new TransientFacadeStore<>(
                id -> { ref.get().close(); return new Facade(id); },
                (id, facade) -> disposals.incrementAndGet(),
                TransientFacadeStore.DEFAULT_MODE);
        ref.set(owner);
        assertThrows(IllegalStateException.class, () -> owner.expose(7L));
        assertEquals(1, disposals.get());
    }

    @Test
    void sampledHighPressureDropsIdleFacadesAndEmptyBacking()
            throws ReflectiveOperationException {
        AtomicReference<RuntimeHeap.Snapshot> heap = new AtomicReference<>(
                new RuntimeHeap.Snapshot(100_000L, 100_000L, 1_000L));
        AtomicInteger disposals = new AtomicInteger();
        TransientFacadeStore.Recycler<Facade> recycler = new TransientFacadeStore.Recycler<>() {
            @Override public void scrub(Facade facade) { facade.owner = -1; }
            @Override public void rebind(Facade facade, long id) { facade.owner = id; }
        };
        int mode = TransientFacadeStore.DEFAULT_MODE
                | TransientFacadeStore.AUTO_HEAP_PRESSURE;
        TransientFacadeStore<Facade> owner = new TransientFacadeStore<>(
                Facade::new, (id, facade) -> disposals.incrementAndGet(),
                mode, heap::get, recycler, 4);
        owner.withInternal(1L, value -> assertEquals(1L, value.owner));
        assertEquals(1, owner.reusableIdleCount());
        heap.set(new RuntimeHeap.Snapshot(100_000L, 100_000L, 95_000L));
        // The incumbent pressure sampler samples once per 64 admissions.
        for (int i = 2; i <= 65; i++) {
            final long id = i;
            owner.withInternal(id, value -> assertEquals(id, value.owner));
        }
        assertEquals(0, owner.reusableIdleCount());
        assertEquals(0, idleArrayLength(owner));
        assertTrue(disposals.get() >= 2);
        owner.close();
    }

    private static int idleArrayLength(TransientFacadeStore<?> owner)
            throws ReflectiveOperationException {
        Field field = TransientFacadeStore.class.getDeclaredField("freeFacades");
        field.setAccessible(true);
        return ((Object[]) field.get(owner)).length;
    }
}
