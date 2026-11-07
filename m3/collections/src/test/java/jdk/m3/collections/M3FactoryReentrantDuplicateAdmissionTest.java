// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Source-sealed regressions for factory reentry before a facade is resident. */
//FLLDCIM: RELOCATE - donor blob 40ef508a7ba6 at 6a534f23a85f; recipe m3/collections/qualification/doop-facade-fixes-20261107/apply.py v1
final class M3FactoryReentrantDuplicateAdmissionTest {
    private record Facade(long id, int invocation) { }

    @Test
    void publicReentryAdmitsNestedIdentityAndDisposesOnlyOuterFacade() {
        AtomicReference<M3TransientFacadeStore<Facade>> ref = new AtomicReference<>();
        List<Facade> disposed = new ArrayList<>();
        Facade[] nested = {null};
        int[] calls = {0};
        M3TransientFacadeStore<Facade> owner = new M3TransientFacadeStore<>(id -> {
            int invocation = ++calls[0];
            if (invocation == 1) nested[0] = ref.get().expose(id);
            return new Facade(id, invocation);
        }, (id, facade) -> disposed.add(facade), M3TransientFacadeStore.DEFAULT_MODE);
        ref.set(owner);

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> owner.expose(7));
        assertEquals("duplicate facade id: 7", failure.getMessage());
        assertEquals(2, calls[0]);
        assertSame(nested[0], owner.expose(7));
        assertTrue(owner.isPinned(7));
        assertEquals(1, disposed.size());
        assertEquals(1, disposed.get(0).invocation());
        assertEquals(1L, owner.stats().disposals());

        owner.close();
        assertEquals(2, disposed.size());
        assertSame(nested[0], disposed.get(1));
    }

    @Test
    void retainedInternalReentryDoesNotInvokeRejectedOuterAction() {
        AtomicReference<M3TransientFacadeStore<Facade>> ref = new AtomicReference<>();
        List<Facade> disposed = new ArrayList<>();
        int[] calls = {0};
        int[] nestedActions = {0};
        int[] outerActions = {0};
        int mode = M3TransientFacadeStore.RETAIN_UNEXPOSED
                | M3TransientFacadeStore.PIN_EXPOSED | M3TransientFacadeStore.PRESSURE_TRIMMABLE;
        M3TransientFacadeStore<Facade> owner = new M3TransientFacadeStore<>(id -> {
            int invocation = ++calls[0];
            if (invocation == 1) {
                ref.get().withInternal(id, live -> {
                    assertEquals(2, live.invocation());
                    nestedActions[0]++;
                });
            }
            return new Facade(id, invocation);
        }, (id, facade) -> disposed.add(facade), mode);
        ref.set(owner);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> owner.withInternal(12, facade -> outerActions[0]++));
        assertEquals("duplicate facade id: 12", failure.getMessage());
        assertEquals(1, nestedActions[0]);
        assertEquals(0, outerActions[0]);
        assertEquals(2, owner.peekResident(12).invocation());
        assertFalse(owner.isPinned(12));
        assertEquals(1, disposed.size());
        assertEquals(1, disposed.get(0).invocation());

        owner.close();
        assertEquals(2, disposed.size());
        assertEquals(2, disposed.get(1).invocation());
    }

    @Test
    void failedUnadmittedCleanupIsSuppressedOnOriginalAdmissionFailure() {
        AtomicReference<M3TransientFacadeStore<Facade>> ref = new AtomicReference<>();
        IllegalArgumentException cleanupFailure = new IllegalArgumentException("cleanup");
        int[] calls = {0};
        List<Facade> disposed = new ArrayList<>();
        M3TransientFacadeStore<Facade> owner = new M3TransientFacadeStore<>(id -> {
            int invocation = ++calls[0];
            if (invocation == 1) ref.get().expose(id);
            return new Facade(id, invocation);
        }, (id, facade) -> {
            if (facade.invocation() == 1) throw cleanupFailure;
            disposed.add(facade);
        }, M3TransientFacadeStore.DEFAULT_MODE);
        ref.set(owner);

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> owner.expose(21));
        assertEquals("duplicate facade id: 21", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertSame(cleanupFailure, failure.getSuppressed()[0]);
        assertEquals(2, owner.peekResident(21).invocation());
        assertEquals(0L, owner.stats().disposals());
        owner.close();
        assertEquals(1, disposed.size());
        assertEquals(2, disposed.get(0).invocation());
    }

    @Test
    void sharedFactoryInstanceAlreadyResidentIsNeverDisposedOnOuterFailure() {
        AtomicReference<M3TransientFacadeStore<Facade>> ref = new AtomicReference<>();
        Facade shared = new Facade(33, 1);
        List<Facade> disposed = new ArrayList<>();
        int[] calls = {0};
        M3TransientFacadeStore<Facade> owner = new M3TransientFacadeStore<>(id -> {
            if (++calls[0] == 1) assertSame(shared, ref.get().expose(id));
            return shared;
        }, (id, facade) -> disposed.add(facade), M3TransientFacadeStore.DEFAULT_MODE);
        ref.set(owner);

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> owner.expose(33));
        assertEquals("duplicate facade id: 33", failure.getMessage());
        assertSame(shared, owner.peekResident(33));
        assertTrue(disposed.isEmpty());
        assertEquals(0L, owner.stats().disposals());
        owner.close();
        assertEquals(1, disposed.size());
        assertSame(shared, disposed.get(0));
    }

    @Test
    void unadmittedDisposerMayCloseReentrantlyWithoutDoubleDisposal() {
        AtomicReference<M3TransientFacadeStore<Facade>> ref = new AtomicReference<>();
        List<Facade> disposed = new ArrayList<>();
        int[] calls = {0};
        M3TransientFacadeStore<Facade> owner = new M3TransientFacadeStore<>(id -> {
            int invocation = ++calls[0];
            if (invocation == 1) ref.get().expose(id);
            return new Facade(id, invocation);
        }, (id, facade) -> {
            disposed.add(facade);
            if (facade.invocation() == 1) ref.get().close();
        }, M3TransientFacadeStore.DEFAULT_MODE);
        ref.set(owner);

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> owner.expose(44));
        assertEquals("duplicate facade id: 44", failure.getMessage());
        assertEquals(2, disposed.size());
        assertEquals(1, disposed.get(0).invocation());
        assertEquals(2, disposed.get(1).invocation());
        assertThrows(IllegalStateException.class, owner::residentCount);
    }

    @Test
    void nonReentrantExposureAndRetainedInternalUseKeepTheirContracts() {
        List<Facade> disposed = new ArrayList<>();
        int mode = M3TransientFacadeStore.RETAIN_UNEXPOSED
                | M3TransientFacadeStore.PIN_EXPOSED | M3TransientFacadeStore.PRESSURE_TRIMMABLE;
        M3TransientFacadeStore<Facade> owner = new M3TransientFacadeStore<>(
                id -> new Facade(id, 1), (id, facade) -> disposed.add(facade), mode);
        Facade pinned = owner.expose(1);
        assertSame(pinned, owner.expose(1));
        owner.withInternal(2, live -> assertSame(live, owner.peekResident(2)));
        assertTrue(owner.isPinned(1));
        assertFalse(owner.isPinned(2));
        assertEquals(2, owner.residentCount());
        assertTrue(disposed.isEmpty());
        owner.close();
        assertEquals(2, disposed.size());
    }
}
