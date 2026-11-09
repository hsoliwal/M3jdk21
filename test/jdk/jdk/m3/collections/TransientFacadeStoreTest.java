// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor blob fd143d57abd0 at fd9a233680b4; recipe m3/collections/recipe/transient-facade-20261106/relocate.py v4
class TransientFacadeStoreTest {

    @Test
    void defaultInternalUseRecreatesWithoutResidency() {
        int[] creates = {0};
        int[] disposes = {0};
        TransientFacadeStore<String> store = new TransientFacadeStore<>(
                id -> id + ":" + ++creates[0],
                (id, value) -> disposes[0]++,
                TransientFacadeStore.DEFAULT_MODE);

        List<String> seen = new ArrayList<>();
        store.withInternal(7, seen::add);
        store.withInternal(7, seen::add);

        assertEquals(List.of("7:1", "7:2"), seen);
        assertEquals(0, store.residentCount());
        assertEquals(2, disposes[0]);
    }

    @Test
    void exposedFacadeKeepsIdentityAcrossPressureTrim() {
        int[] disposes = {0};
        TransientFacadeStore<Object> store = new TransientFacadeStore<>(
                id -> new Object(),
                (id, value) -> disposes[0]++,
                TransientFacadeStore.DEFAULT_MODE);

        Object exposed = store.expose(42);
        assertSame(exposed, store.expose(42));
        assertTrue(store.isPinned(42));
        assertEquals(0, store.releaseOptionalResidency(0));
        assertSame(exposed, store.peekResident(42));
        assertEquals(0, disposes[0]);
    }

    @Test
    void retainedInternalFacadesUseLruAndNeverEvictPinnedIdentity() {
        int mode = TransientFacadeStore.RETAIN_UNEXPOSED
                | TransientFacadeStore.PIN_EXPOSED
                | TransientFacadeStore.PRESSURE_TRIMMABLE;
        int[] creates = {0};
        TransientFacadeStore<String> store = new TransientFacadeStore<>(
                id -> id + ":" + ++creates[0],
                mode);

        store.withInternal(1, ignored -> { });
        store.withInternal(2, ignored -> { });
        store.withInternal(1, ignored -> { });
        String pinned = store.expose(3);

        assertEquals(3, store.residentCount());
        assertEquals(2, store.releaseOptionalResidency(1));
        assertEquals(1, store.residentCount());
        assertSame(pinned, store.peekResident(3));
        assertFalse(store.isResident(1));
        assertFalse(store.isResident(2));
    }

    @Test
    void semanticRemovalCanReleasePinnedFacade() {
        int[] disposes = {0};
        TransientFacadeStore<Object> store = new TransientFacadeStore<>(
                id -> new Object(),
                (id, value) -> disposes[0]++,
                TransientFacadeStore.DEFAULT_MODE);
        Object exposed = store.expose(9);

        assertTrue(store.remove(9));
        assertEquals(1, disposes[0]);
        assertFalse(store.isResident(9));
        assertNotSame(exposed, store.expose(9),
                "a new logical generation may manufacture a new facade after removal");
    }

    @Test
    void rejectsModesThatCouldBreakExposedIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new TransientFacadeStore<>(
                id -> new Object(),
                TransientFacadeStore.RECREATE_UNEXPOSED));
        assertThrows(IllegalArgumentException.class, () -> new TransientFacadeStore<>(
                id -> new Object(),
                TransientFacadeStore.RECREATE_UNEXPOSED
                        | TransientFacadeStore.RETAIN_UNEXPOSED
                        | TransientFacadeStore.PIN_EXPOSED));
    }
}
