// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** A Disposer may close or shrink its owner reentrantly; every facade is still disposed exactly once. */
//FLLDCIM: MRL - reentrant Disposer during remove/close disposed a facade twice, then AIOOBE(-1) in removeSlot
class M3FacadeDisposerReentryTest {
    private static final class Facade {
        final long id;
        Facade(long id) { this.id = id; }
        @Override public String toString() { return "F" + id; }
    }

    /** Owner with pinned facades 1, 2 and 3; {@code reentry} runs once, inside the disposal of {@code trigger}. */
    private static M3TransientFacadeStore<Facade> owner(List<Facade> disposed, long trigger,
            Consumer<M3TransientFacadeStore<Facade>> reentry) {
        AtomicReference<M3TransientFacadeStore<Facade>> ref = new AtomicReference<>();
        boolean[] fired = {false};
        M3TransientFacadeStore<Facade> owner = new M3TransientFacadeStore<>(Facade::new, (id, facade) -> {
            disposed.add(facade);
            if (id == trigger && !fired[0]) {
                fired[0] = true;
                reentry.accept(ref.get());
            }
        }, M3TransientFacadeStore.DEFAULT_MODE);
        ref.set(owner);
        for (long id = 1; id <= 3; id++) owner.expose(id);
        return owner;
    }

    /** Exactly the named facades, each once: no repeat and no detached (null) slot handed to the Disposer. */
    private static void assertDisposedOnce(List<Facade> disposed, long... ids) {
        assertEquals(ids.length, disposed.size(), "disposals " + disposed);
        for (long id : ids) {
            assertEquals(1, disposed.stream().filter(f -> f != null && f.id == id).count(),
                    "disposals of F" + id + " in " + disposed);
        }
    }

    @Test
    void disposerClosingOwnerDuringRemoveDisposesEachFacadeOnce() {
        List<Facade> disposed = new ArrayList<>();
        M3TransientFacadeStore<Facade> owner = owner(disposed, 2, M3TransientFacadeStore::close);
        assertTrue(assertDoesNotThrow(() -> owner.remove(2)));
        assertDisposedOnce(disposed, 1, 2, 3);
        assertThrows(IllegalStateException.class, owner::residentCount);
        assertDoesNotThrow(owner::close);
        assertEquals(3, disposed.size());
    }

    @Test
    void disposerRemovingOtherDuringRemoveKeepsSlotsAligned() {
        List<Facade> disposed = new ArrayList<>();
        M3TransientFacadeStore<Facade> owner = owner(disposed, 2, store -> assertTrue(store.remove(1)));
        Facade third = owner.peekResident(3);
        assertTrue(assertDoesNotThrow(() -> owner.remove(2)));
        assertDisposedOnce(disposed, 1, 2);
        assertEquals(1, owner.residentCount());
        assertFalse(owner.isResident(1));
        assertFalse(owner.isResident(2));
        assertSame(third, owner.peekResident(3));
        assertTrue(owner.isPinned(3));
        owner.close();
        assertDisposedOnce(disposed, 1, 2, 3);
    }

    @Test
    void disposerRemovingItsOwnIdDuringRemoveDisposesOnce() {
        List<Facade> disposed = new ArrayList<>();
        M3TransientFacadeStore<Facade> owner = owner(disposed, 2, store -> assertFalse(store.remove(2)));
        Facade third = owner.peekResident(3);
        assertTrue(assertDoesNotThrow(() -> owner.remove(2)));
        assertDisposedOnce(disposed, 2);
        assertEquals(2, owner.residentCount());
        assertSame(third, owner.peekResident(3));
        owner.close();
        assertDisposedOnce(disposed, 1, 2, 3);
    }

    @Test
    void disposerClosingOwnerDuringCloseDisposesEachFacadeOnce() {
        List<Facade> disposed = new ArrayList<>();
        M3TransientFacadeStore<Facade> owner = owner(disposed, 2, M3TransientFacadeStore::close);
        assertDoesNotThrow(owner::close);
        assertDisposedOnce(disposed, 1, 2, 3);
        assertThrows(IllegalStateException.class, owner::residentCount);
    }

    @Test
    void disposerRemovingOtherDuringCloseDisposesEachFacadeOnce() {
        List<Facade> disposed = new ArrayList<>();
        M3TransientFacadeStore<Facade> owner = owner(disposed, 2, store -> assertTrue(store.remove(1)));
        assertDoesNotThrow(owner::close);
        assertDisposedOnce(disposed, 1, 2, 3);
        assertThrows(IllegalStateException.class, owner::residentCount);
    }

    /** Non-reentrant contract kept: a throwing Disposer leaves the facade resident, pinned and identical. */
    @Test
    void failedDisposalKeepsFacadeResidentWithIdentityAndFlags() {
        List<Facade> disposed = new ArrayList<>();
        IllegalStateException fault = new IllegalStateException("dispose fault");
        boolean[] failing = {true};
        M3TransientFacadeStore<Facade> owner = new M3TransientFacadeStore<>(Facade::new, (id, facade) -> {
            if (id == 2 && failing[0]) throw fault;
            disposed.add(facade);
        }, M3TransientFacadeStore.DEFAULT_MODE);
        for (long id = 1; id <= 3; id++) owner.expose(id);
        Facade second = owner.peekResident(2);
        short flags = owner.residencyFlags(2);
        assertSame(fault, assertThrows(IllegalStateException.class, () -> owner.remove(2)));
        assertSame(second, owner.peekResident(2));
        assertEquals(flags, owner.residencyFlags(2));
        assertEquals(3, owner.residentCount());
        assertEquals(0L, owner.stats().disposals());
        assertSame(fault, assertThrows(IllegalStateException.class, owner::close));
        assertDisposedOnce(disposed, 3);
        assertEquals(2, owner.residentCount());
        assertSame(second, owner.expose(2));
        failing[0] = false;
        owner.close();
        assertDisposedOnce(disposed, 1, 2, 3);
    }
}
