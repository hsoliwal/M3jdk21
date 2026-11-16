/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Differential and physical-lane proofs for the write-hooked map. */
final class TransformedMapViewContractTest {
    /** A key whose hash keeps only a few bits, so probe runs collide, wrap around and close their gaps. */
    private record Crowded(int id, int bits) {
        @Override public int hashCode() { return id & bits; }
    }

    private static final Function<Crowded, Crowded> FOLD_KEY = key -> key == null ? null : new Crowded(key.id() % 97, key.bits());
    private static final Function<String, String> UPPER = value -> value == null ? null : value.toUpperCase();
    private static final Function<String, String> NO_EMPTY = value -> {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("refused value");
        }
        return value;
    };

    private static Object lane(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    /** Lanes are parallel, a power of two long, at most half full; OCCUPIED slots are exactly the model's mappings. */
    private static <K, V> void checkLanes(TransformedMapView<K, V> view, Map<K, V> model) {
        Object[] keys = (Object[]) lane(view, "keyLane");
        Object[] values = (Object[]) lane(view, "valueLane");
        long[] states = (long[]) lane(view, "stateMaskWords");
        assertEquals(model.size(), view.size());
        assertEquals(keys.length, values.length);
        assertEquals(0, keys.length & keys.length - 1, "power of two");
        assertTrue(2 * model.size() <= keys.length, "at most half full");
        Map<Object, Object> stored = new HashMap<>();
        for (int slot = 0; slot < keys.length; slot++) {
            if ((states[slot >>> 6] >>> slot & TransformedMapView.STATE_OCCUPIED) != 0) {
                assertFalse(stored.containsKey(keys[slot]), "a key in two slots");
                stored.put(keys[slot], values[slot]);
            } else {
                assertNull(keys[slot], "stale key in slot " + slot);
                assertNull(values[slot], "stale value in slot " + slot);
            }
        }
        assertEquals(model, stored);
        for (Map.Entry<K, V> entry : model.entrySet()) {
            assertEquals(entry.getValue(), view.get(entry.getKey()), "every stored key is reachable by probing");
        }
    }

    @Test
    void plainViewBehavesLikeAMap() {
        TransformedMapView<String, Integer> view = new TransformedMapView<>();
        assertEquals(0, view.size());
        assertNull(view.get("a"));
        assertNull(view.remove("a"));
        assertNull(view.put("a", 1));
        assertEquals(1, view.put("a", 2));
        assertNull(view.put(null, 3));
        assertNull(view.put("n", null));
        assertEquals(3, view.get(null));
        assertNull(view.get("n"));
        assertEquals(3, view.size());
        assertEquals(2, view.remove("a"));
        assertNull(view.remove("a"));
        assertEquals(3, view.remove(null));
        assertEquals(1, view.size());
        assertEquals("TransformedMapView|TRANSFORMED_MAP|OBJECT|OCCUPIED|Object[] keyLane;Object[] valueLane",
                TransformedMapView.scaffoldDescriptor());
        assertThrows(NullPointerException.class, () -> view.putAll(null));
    }

    @Test
    void hooksTransformWritesOnly() {
        TransformedMapView<String, String> view = new TransformedMapView<>(String::trim, UPPER);
        assertNull(view.put(" k ", "v"));
        assertEquals("V", view.get("k"));
        assertNull(view.get(" k "), "lookups use the key as given");
        assertEquals("V", view.put("k", "w"));
        Map<String, String> batch = new LinkedHashMap<>();
        batch.put(" a", "x");
        batch.put("a ", "y");
        view.putAll(batch);
        assertEquals("Y", view.get("a"), "the later entry wins when two keys transform to the same key");
        assertEquals(2, view.size());
        assertNull(view.remove(" a"));
        assertEquals("Y", view.remove("a"));
    }

    @Test
    void refusingHookLeavesTheMapUnchanged() {
        TransformedMapView<String, String> view = new TransformedMapView<>(null, NO_EMPTY);
        view.put("a", "1");
        assertThrows(IllegalArgumentException.class, () -> view.put("b", ""));
        Map<String, String> batch = new LinkedHashMap<>();
        batch.put("c", "3");
        batch.put("a", "changed");
        batch.put("d", null);
        assertThrows(IllegalArgumentException.class, () -> view.putAll(batch));
        Map<String, String> model = new HashMap<>();
        model.put("a", "1");
        checkLanes(view, model);
    }

    @Test
    void randomOperationsMatchAHashMapModel() {
        for (int seed = 0; seed < 8; seed++) {
            SplittableRandom random = new SplittableRandom(seed * 104729L + 7);
            int bits = seed % 2 == 0 ? 0x7 : -1;
            TransformedMapView<Crowded, String> view = new TransformedMapView<>(FOLD_KEY, UPPER);
            Map<Crowded, String> model = new HashMap<>();
            for (int step = 0; step < 6000; step++) {
                Crowded key = random.nextInt(30) == 0 ? null : new Crowded(random.nextInt(300), bits);
                String value = random.nextInt(12) == 0 ? null : "v" + random.nextInt(20);
                switch (random.nextInt(9)) {
                    case 0, 1, 2, 3 -> assertEquals(model.put(FOLD_KEY.apply(key), UPPER.apply(value)), view.put(key, value));
                    case 4 -> putBatch(random, bits, view, model);
                    case 5, 6 -> assertEquals(model.remove(key), view.remove(key));
                    default -> assertTrue(Objects.equals(model.get(key), view.get(key)));
                }
                assertEquals(model.size(), view.size());
                if (step % 61 == 0) {
                    checkLanes(view, model);
                }
            }
            checkLanes(view, model);
        }
    }

    private static void putBatch(SplittableRandom random, int bits, TransformedMapView<Crowded, String> view,
            Map<Crowded, String> model) {
        Map<Crowded, String> batch = new LinkedHashMap<>();
        for (int i = random.nextInt(60); i > 0; i--) {
            batch.put(new Crowded(random.nextInt(300), bits), "b" + i);
        }
        view.putAll(batch);
        batch.forEach((key, value) -> model.put(FOLD_KEY.apply(key), UPPER.apply(value)));
    }
}
