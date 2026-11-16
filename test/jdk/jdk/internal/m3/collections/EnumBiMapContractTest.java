/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Differential and physical-lane proofs for the enum-to-enum one-to-one map. */
final class EnumBiMapContractTest {
    /** Constants with bodies: their runtime class is not the enum type. */
    private enum Shape {
        ROUND { @Override int corners() { return 0; } },
        SQUARE { @Override int corners() { return 4; } },
        TRIANGLE { @Override int corners() { return 3; } };

        abstract int corners();
    }

    /** Independent oracle: two hash maps kept in step by the one-to-one rules. */
    private static final class Model<K, V> {
        final Map<K, V> forward = new HashMap<>();
        final Map<V, K> backward = new HashMap<>();

        V put(K key, V value, boolean force) {
            if (Objects.equals(forward.get(key), value)) {
                return value;
            }
            if (backward.containsKey(value)) {
                if (!force) {
                    throw new IllegalArgumentException("value already present: " + value);
                }
                forward.remove(backward.remove(value));
            }
            V old = forward.put(key, value);
            if (old != null) {
                backward.remove(old);
            }
            backward.put(value, key);
            return old;
        }

        V remove(K key) {
            V old = forward.remove(key);
            if (old != null) {
                backward.remove(old);
            }
            return old;
        }

        void clear() {
            forward.clear();
            backward.clear();
        }
    }

    @Test
    void randomOperationsMatchTheModelAndTheLanesStayConsistent() throws Exception {
        runAgainstModel(Month.class, DayOfWeek.class, 0x51ED270BL, 60_000);
        runAgainstModel(TimeUnit.class, Month.class, 0x9E3779B9L, 60_000);
        runAgainstModel(Character.UnicodeScript.class, Character.UnicodeScript.class, 0x2545F491L, 120_000);
        runAgainstModel(Shape.class, Thread.State.class, 0x7F4A7C15L, 20_000);
    }

    private static <K extends Enum<K>, V extends Enum<V>> void runAgainstModel(
            Class<K> keys, Class<V> values, long seed, int steps) throws Exception {
        K[] keyUniverse = keys.getEnumConstants();
        V[] valueUniverse = values.getEnumConstants();
        EnumBiMap<K, V> map = new EnumBiMap<>(keys, values);
        Model<K, V> model = new Model<>();
        SplittableRandom random = new SplittableRandom(seed);
        for (int step = 0; step < steps; step++) {
            K key = keyUniverse[random.nextInt(keyUniverse.length)];
            V value = valueUniverse[random.nextInt(valueUniverse.length)];
            switch (random.nextInt(16)) {
                case 0, 1, 2, 3, 4 -> assertSamePut(map, model, key, value, false);
                case 5, 6, 7 -> assertSamePut(map, model, key, value, true);
                case 8, 9, 10 -> assertSame(model.remove(key), map.remove(key));
                case 11 -> {
                    if (random.nextInt(40) == 0) {
                        model.clear();
                        map.clear();
                    }
                }
                default -> {
                    assertSame(model.forward.get(key), map.get(key));
                    assertSame(model.backward.get(value), map.inverseGet(value));
                }
            }
            if (step % 97 == 0) {
                assertSameState(map, model, keyUniverse, valueUniverse);
            }
        }
        assertSameState(map, model, keyUniverse, valueUniverse);
    }

    private static <K extends Enum<K>, V extends Enum<V>> void assertSamePut(
            EnumBiMap<K, V> map, Model<K, V> model, K key, V value, boolean force) {
        V expected;
        try {
            expected = model.put(key, value, force);
        } catch (IllegalArgumentException refused) {
            IllegalArgumentException actual = assertThrows(IllegalArgumentException.class, () -> map.put(key, value));
            assertEquals(refused.getMessage(), actual.getMessage());
            return;
        }
        assertSame(expected, force ? map.forcePut(key, value) : map.put(key, value));
    }

    private static <K extends Enum<K>, V extends Enum<V>> void assertSameState(
            EnumBiMap<K, V> map, Model<K, V> model, K[] keyUniverse, V[] valueUniverse) throws Exception {
        assertEquals(model.forward.size(), map.size());
        for (K key : keyUniverse) {
            assertSame(model.forward.get(key), map.get(key));
            assertEquals(model.forward.containsKey(key), map.containsKey(key));
        }
        for (V value : valueUniverse) {
            assertSame(model.backward.get(value), map.inverseGet(value));
            assertEquals(model.backward.containsKey(value), map.containsValue(value));
        }
        assertLanes(map, keyUniverse.length, valueUniverse.length);
    }

    /** Physical proof: the two lanes mirror each other, the state words agree with them and with the size. */
    private static void assertLanes(EnumBiMap<?, ?> map, int keyCount, int valueCount) throws Exception {
        int[] keyToValue = (int[]) lane(map, "keyToValueOrdinalLane");
        int[] valueToKey = (int[]) lane(map, "valueToKeyOrdinalLane");
        long[] states = (long[]) lane(map, "stateMaskWords");
        assertEquals(keyCount, keyToValue.length);
        assertEquals(valueCount, valueToKey.length);
        assertEquals(keyCount, states.length);
        int present = 0;
        for (int key = 0; key < keyCount; key++) {
            boolean mapped = states[key] == EnumBiMap.STATE_PRESENT;
            assertTrue(mapped || states[key] == 0L, "a state word holds PRESENT or nothing");
            assertEquals(mapped, keyToValue[key] != -1, "state word and key lane agree");
            if (mapped) {
                present++;
                assertEquals(key, valueToKey[keyToValue[key]], "the value lane points back");
            }
        }
        int held = 0;
        for (int value = 0; value < valueCount; value++) {
            if (valueToKey[value] != -1) {
                held++;
                assertEquals(value, keyToValue[valueToKey[value]], "the key lane points back");
            }
        }
        assertEquals(map.size(), present);
        assertEquals(map.size(), held);
    }

    private static Object lane(EnumBiMap<?, ?> map, String name) throws Exception {
        Field field = EnumBiMap.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(map);
    }

    @Test
    void putKeepsTheMapOneToOne() {
        EnumBiMap<Month, DayOfWeek> map = new EnumBiMap<>(Month.class, DayOfWeek.class);
        assertNull(map.put(Month.MAY, DayOfWeek.MONDAY));
        assertSame(DayOfWeek.MONDAY, map.put(Month.MAY, DayOfWeek.MONDAY), "the same mapping returns the value");
        assertSame(DayOfWeek.MONDAY, map.put(Month.MAY, DayOfWeek.FRIDAY), "a new value returns the old one");
        assertFalse(map.containsValue(DayOfWeek.MONDAY), "the old value is free again");
        IllegalArgumentException refused =
                assertThrows(IllegalArgumentException.class, () -> map.put(Month.JUNE, DayOfWeek.FRIDAY));
        assertEquals("value already present: FRIDAY", refused.getMessage());
        assertEquals(1, map.size());
        assertFalse(map.containsKey(Month.JUNE), "a refused put changes nothing");
        assertSame(Month.MAY, map.inverseGet(DayOfWeek.FRIDAY));
    }

    @Test
    void forcePutTakesTheValueFromItsKey() {
        EnumBiMap<Month, DayOfWeek> map = new EnumBiMap<>(Month.class, DayOfWeek.class);
        map.put(Month.MAY, DayOfWeek.MONDAY);
        map.put(Month.JUNE, DayOfWeek.FRIDAY);
        assertSame(DayOfWeek.FRIDAY, map.forcePut(Month.JUNE, DayOfWeek.MONDAY), "returns the old value of the key");
        assertEquals(1, map.size());
        assertFalse(map.containsKey(Month.MAY), "the key that held the value is removed");
        assertFalse(map.containsValue(DayOfWeek.FRIDAY));
        assertSame(Month.JUNE, map.inverseGet(DayOfWeek.MONDAY));
        assertNull(map.forcePut(Month.JULY, DayOfWeek.MONDAY), "a new key has no old value");
        assertEquals(1, map.size());
        assertSame(DayOfWeek.MONDAY, map.forcePut(Month.JULY, DayOfWeek.MONDAY), "the same mapping returns the value");
    }

    @Test
    void nullsAreRefusedAndForeignObjectsAreNeverFound() {
        EnumBiMap<Month, DayOfWeek> map = new EnumBiMap<>(Month.class, DayOfWeek.class);
        map.put(Month.MAY, DayOfWeek.MONDAY);
        assertThrows(NullPointerException.class, () -> map.put(null, DayOfWeek.FRIDAY));
        assertThrows(NullPointerException.class, () -> map.put(Month.JUNE, null));
        assertThrows(NullPointerException.class, () -> map.forcePut(null, DayOfWeek.MONDAY));
        assertThrows(NullPointerException.class, () -> new EnumBiMap<Month, DayOfWeek>(null, DayOfWeek.class));
        assertEquals(1, map.size());
        for (Object foreign : new Object[] {null, "MAY", 4, DayOfWeek.MONDAY, TimeUnit.DAYS}) {
            assertNull(map.get(foreign));
            assertNull(map.remove(foreign));
            assertFalse(map.containsKey(foreign));
        }
        for (Object foreign : new Object[] {null, "MONDAY", Month.MAY, TimeUnit.DAYS}) {
            assertNull(map.inverseGet(foreign));
            assertFalse(map.containsValue(foreign));
        }
        assertEquals(1, map.size());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void aConstantOfAnotherEnumTypeIsRefusedBeforeAnythingChanges() throws Exception {
        EnumBiMap<Month, DayOfWeek> map = new EnumBiMap<>(Month.class, DayOfWeek.class);
        map.put(Month.MAY, DayOfWeek.MONDAY);
        EnumBiMap raw = map;
        assertThrows(ClassCastException.class, () -> raw.put(TimeUnit.DAYS, DayOfWeek.FRIDAY));
        assertThrows(ClassCastException.class, () -> raw.put(Month.JUNE, TimeUnit.DAYS));
        assertThrows(ClassCastException.class, () -> raw.forcePut(TimeUnit.DAYS, DayOfWeek.MONDAY));
        assertSame(DayOfWeek.MONDAY, map.get(Month.MAY), "a refused forcePut does not take the value away");
        assertEquals(1, map.size());
        assertLanes(map, 12, 7);
    }

    @Test
    void aMapMadeWithoutTypesLearnsThemFromItsFirstMapping() throws Exception {
        EnumBiMap<Shape, Month> map = new EnumBiMap<>();
        assertEquals(0, map.size());
        assertNull(map.get(Shape.ROUND));
        assertNull(map.inverseGet(Month.MAY));
        assertNull(map.remove(Shape.ROUND));
        assertFalse(map.containsKey(Shape.ROUND));
        assertFalse(map.containsValue(Month.MAY));
        map.clear();
        assertNull(map.put(Shape.SQUARE, Month.MAY));
        assertSame(Month.MAY, map.get(Shape.SQUARE), "a constant with a body is found through its declaring class");
        assertSame(Shape.SQUARE, map.inverseGet(Month.MAY));
        assertLanes(map, 3, 12);
        map.clear();
        assertEquals(0, map.size());
        assertNull(map.put(Shape.ROUND, Month.MAY), "the types stay after clear");
        assertLanes(map, 3, 12);
    }

    @Test
    void removeAndClearFreeBothDirections() throws Exception {
        EnumBiMap<Month, DayOfWeek> map = new EnumBiMap<>(Month.class, DayOfWeek.class);
        map.put(Month.MAY, DayOfWeek.MONDAY);
        map.put(Month.JUNE, DayOfWeek.FRIDAY);
        assertSame(DayOfWeek.MONDAY, map.remove(Month.MAY));
        assertNull(map.remove(Month.MAY), "a second removal finds nothing");
        assertNull(map.inverseGet(DayOfWeek.MONDAY));
        assertNull(map.put(Month.JULY, DayOfWeek.MONDAY), "the freed value can be mapped again");
        map.clear();
        assertEquals(0, map.size());
        assertNull(map.get(Month.JUNE));
        assertNull(map.inverseGet(DayOfWeek.FRIDAY));
        assertLanes(map, 12, 7);
    }

    @Test
    void theDescriptorNamesTheLanes() {
        assertEquals("EnumBiMap|ENUM_BIMAP|OBJECT|PRESENT|int[] keyToValueOrdinalLane;int[] valueToKeyOrdinalLane",
                EnumBiMap.scaffoldDescriptor());
    }
}
