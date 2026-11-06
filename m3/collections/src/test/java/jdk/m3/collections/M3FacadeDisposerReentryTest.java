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

/** A Disposer may close or shrink its owner reentrantly (remove, close, trim); every facade is disposed exactly once. */
//FLLDCIM: MRL - reentrant Disposer during remove/close disposed a facade twice, then AIOOBE(-1) in removeSlot
//FLLDCIM: MRL - reentrant Disposer during releaseOptionalResidency disposed the in-flight victim twice, then (0, null)
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

    /** Pinned facade 0 (lowest slot, never a victim) and unpinned retained facades 1, 2, 3: a full trim's victims. */
    private static M3TransientFacadeStore<Facade> retainingOwner(List<Facade> disposed, long trigger,
            Consumer<M3TransientFacadeStore<Facade>> reentry) {
        AtomicReference<M3TransientFacadeStore<Facade>> ref = new AtomicReference<>();
        boolean[] fired = {false};
        int mode = M3TransientFacadeStore.RETAIN_UNEXPOSED | M3TransientFacadeStore.PIN_EXPOSED
                | M3TransientFacadeStore.PRESSURE_TRIMMABLE;
        M3TransientFacadeStore<Facade> owner = new M3TransientFacadeStore<>(Facade::new, (id, facade) -> {
            disposed.add(facade);
            if (id == trigger && !fired[0]) {
                fired[0] = true;
                reentry.accept(ref.get());
            }
        }, mode);
        ref.set(owner);
        owner.expose(0);
        for (long id = 1; id <= 3; id++) owner.withInternal(id, facade -> { });
        return owner;
    }

    @Test
    void disposerClosingOwnerDuringTrimDisposesEachFacadeOnce() {
        List<Facade> disposed = new ArrayList<>();
        M3TransientFacadeStore<Facade> owner = retainingOwner(disposed, 1, M3TransientFacadeStore::close);
        assertEquals(3, assertDoesNotThrow(() -> owner.releaseOptionalResidency(0)));
        assertDisposedOnce(disposed, 0, 1, 2, 3);
        assertThrows(IllegalStateException.class, owner::residentCount);
    }

    @Test
    void disposerRemovingLowerSlotDuringTrimKeepsVictimsAligned() {
        List<Facade> disposed = new ArrayList<>();
        M3TransientFacadeStore<Facade> owner = retainingOwner(disposed, 1, store -> assertTrue(store.remove(0)));
        assertEquals(3, assertDoesNotThrow(() -> owner.releaseOptionalResidency(0)));
        assertDisposedOnce(disposed, 0, 1, 2, 3);
        assertEquals(0, owner.residentCount());
        owner.close();
        assertEquals(4, disposed.size());
    }

    @Test
    void disposerRemovingPendingVictimDuringTrimDisposesItOnce() {
        List<Facade> disposed = new ArrayList<>();
        M3TransientFacadeStore<Facade> owner = retainingOwner(disposed, 1, store -> assertFalse(store.remove(3)));
        Facade pinned = owner.peekResident(0);
        assertEquals(3, assertDoesNotThrow(() -> owner.releaseOptionalResidency(0)));
        assertDisposedOnce(disposed, 1, 2, 3);
        assertEquals(1, owner.residentCount());
        assertSame(pinned, owner.peekResident(0));
        owner.close();
        assertDisposedOnce(disposed, 0, 1, 2, 3);
    }

    @Test
    void disposerExposingPendingVictimDuringTrimGetsLivePinnedFacade() {
        List<Facade> disposed = new ArrayList<>();
        Facade[] exposed = new Facade[1];
        M3TransientFacadeStore<Facade> owner = retainingOwner(disposed, 1, store -> exposed[0] = store.expose(3));
        assertEquals(3, assertDoesNotThrow(() -> owner.releaseOptionalResidency(0)));
        assertDisposedOnce(disposed, 1, 2, 3);
        assertTrue(disposed.stream().noneMatch(f -> f == exposed[0]), "exposed facade was disposed");
        assertSame(exposed[0], owner.peekResident(3));
        assertTrue(owner.isPinned(3));
        owner.close();
        assertEquals(5, disposed.size());
        assertEquals(1, disposed.stream().filter(f -> f == exposed[0]).count());
    }

    /** Non-reentrant contract kept: a throwing Disposer stops the trim; unreleased victims stay resident, unchanged. */
    @Test
    void failedDisposalDuringTrimKeepsUnreleasedVictimsResident() {
        List<Facade> disposed = new ArrayList<>();
        IllegalStateException fault = new IllegalStateException("dispose fault");
        boolean[] failing = {true};
        int mode = M3TransientFacadeStore.RETAIN_UNEXPOSED | M3TransientFacadeStore.PIN_EXPOSED
                | M3TransientFacadeStore.PRESSURE_TRIMMABLE;
        M3TransientFacadeStore<Facade> owner = new M3TransientFacadeStore<>(Facade::new, (id, facade) -> {
            if (id == 2 && failing[0]) throw fault;
            disposed.add(facade);
        }, mode);
        owner.expose(0);
        for (long id = 1; id <= 3; id++) owner.withInternal(id, facade -> { });
        Facade second = owner.peekResident(2);
        Facade third = owner.peekResident(3);
        short secondFlags = owner.residencyFlags(2);
        short thirdFlags = owner.residencyFlags(3);
        assertSame(fault, assertThrows(IllegalStateException.class, () -> owner.releaseOptionalResidency(0)));
        assertDisposedOnce(disposed, 1);
        assertEquals(3, owner.residentCount());
        assertSame(second, owner.peekResident(2));
        assertSame(third, owner.peekResident(3));
        assertEquals(secondFlags, owner.residencyFlags(2));
        assertEquals(thirdFlags, owner.residencyFlags(3));
        assertEquals(1L, owner.stats().trims());
        failing[0] = false;
        assertEquals(2, owner.releaseOptionalResidency(1));
        assertSame(second, disposed.get(1));
        assertSame(third, disposed.get(2));
        owner.close();
        assertDisposedOnce(disposed, 0, 1, 2, 3);
    }
}
