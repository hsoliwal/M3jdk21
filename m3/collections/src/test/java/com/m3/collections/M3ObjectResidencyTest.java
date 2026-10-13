// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class M3ObjectResidencyTest {
    private static final class Value {
        int canonicalId;
        String text;
        Value(int id, String text) { this.canonicalId = id; this.text = text; }
    }
    private static final class MemoryStore implements M3ObjectResidency.SpillStore {
        final Map<Integer, byte[]> bytes = new HashMap<>();
        @Override public void write(int slot, byte[] value) { bytes.put(slot, value.clone()); }
        @Override public byte[] read(int slot) throws IOException {
            byte[] value = bytes.get(slot);
            if (value == null) throw new IOException("missing slot " + slot);
            return value.clone();
        }
        @Override public void delete(int slot) { bytes.remove(slot); }
    }
    private static final M3ObjectResidency.Codec<Value> CODEC = new M3ObjectResidency.Codec<>() {
        @Override public byte[] encode(Value value) {
            return (value.canonicalId + "\\n" + value.text).getBytes(StandardCharsets.UTF_8);
        }
        @Override public Value decode(int slot, byte[] bytes) {
            String[] fields = new String(bytes, StandardCharsets.UTF_8).split("\\\\n", 2);
            return new Value(Integer.parseInt(fields[0]), fields[1]);
        }
    };

    @Test void lazyCreateAndStableIntegerSlot() {
        java.util.concurrent.atomic.AtomicInteger creates = new java.util.concurrent.atomic.AtomicInteger();
        var arena = new M3ObjectResidency<Value>(2,
                M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.LRU | M3ObjectResidency.PRESSURE_AWARE,
                1024, 0, slot -> { creates.incrementAndGet(); return new Value(slot, "v" + slot); },
                null, null);
        assertEquals(M3ObjectResidency.State.ABSENT, arena.state(0));
        assertEquals("v0", arena.use(0, 64, value -> value.text));
        assertEquals("v0", arena.use(0, 64, value -> value.text));
        assertEquals(1, creates.get());
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(0));
    }

    @Test void swapsToSecondaryAndRestoresWithoutChangingLogicalValue() {
        var store = new MemoryStore();
        var arena = new M3ObjectResidency<Value>(2,
                M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.LRU | M3ObjectResidency.SWAP_SECONDARY,
                64, 0, slot -> new Value(slot, "parent-state-" + slot), CODEC, store);
        arena.use(0, 48, value -> { value.text = "mutated"; return null; });
        arena.use(1, 48, value -> value.text);
        assertEquals(M3ObjectResidency.State.SWAPPED, arena.state(0));
        assertEquals("mutated", arena.use(0, 48, value -> value.text));
        assertEquals(M3ObjectResidency.State.RESIDENT, arena.state(0));
        assertFalse(store.bytes.containsKey(0));
    }

    @Test void neverEvictsActiveSlotAndReportsUnrecoverableBudget() {
        var arena = new M3ObjectResidency<String>(1,
                M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.LRU,
                8, 0, slot -> "v", null, null);
        assertThrows(M3ObjectResidency.BudgetExceededException.class, () -> arena.use(0, 16, value -> value));
    }

    @Test void strictScopeRejectsReturningTheProjection() {
        var arena = new M3ObjectResidency<Object>(1,
                M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.STRICT_NO_ESCAPE,
                128, 0, slot -> new Object(), null, null);
        assertThrows(IllegalStateException.class, () -> arena.use(0, 8, value -> value));
    }

    @Test void rejectsUnknownFlagsAndIncompleteSwapConfiguration() {
        assertThrows(IllegalArgumentException.class, () ->
                new M3ObjectResidency<Object>(1, 1 << 20, 128, 0, slot -> new Object(), null, null));
        assertThrows(IllegalArgumentException.class, () ->
                new M3ObjectResidency<Object>(1, M3ObjectResidency.SWAP_SECONDARY,
                        128, 0, slot -> new Object(), null, null));
    }

    @Test void lruDoesNotSpinWhenNoEvictionStrategyIsConfigured() {
        var arena = new M3ObjectResidency<String>(2,
                M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.LRU,
                16, 0, slot -> "slot-" + slot, null, null);
        arena.use(0, 8, value -> value.length());
        assertThrows(M3ObjectResidency.BudgetExceededException.class,
                () -> arena.use(1, 16, value -> value.length()));
    }

    @Test void residencyAnnotationIsClassRetainedAndExplicit() throws Exception {
        var annotation = AnnotatedProjection.class.getAnnotation(M3Residency.class);
        assertNull(annotation, "CLASS retention must not add reflective runtime lookup");
        assertEquals(M3Residency.Strategy.SWAP, AnnotatedProjection.class
                .getDeclaredField("marker").getAnnotation(M3Residency.class).strategy());
    }

    static final class AnnotatedProjection {
        @M3Residency(flags = M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.SWAP_SECONDARY,
                estimatedBytes = 128, parent = "owner", strategy = M3Residency.Strategy.SWAP)
        Object marker;
    }
}
