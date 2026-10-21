/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

final class M3ObjectCountsTest {
    private record Collision(int value) {
        @Override public int hashCode() { return value % 3; }
    }

    @Test void differentialCountsNullCollisionsAndSlotReuse() {
        M3ObjectCounts<Collision> actual = new M3ObjectCounts<>();
        Map<Collision, Integer> expected = new HashMap<>();
        Random random = new Random(0x4d33434f554e54L);
        for (int step = 0; step < 12000; step++) {
            Collision key = step % 29 == 0 ? null : new Collision(random.nextInt(79));
            int old = expected.getOrDefault(key, 0);
            int amount = random.nextInt(11);
            int next;
            switch (random.nextInt(5)) {
                case 0 -> { assertEquals(old, actual.add(key, amount)); next = old + amount; }
                case 1 -> { assertEquals(old, actual.remove(key, amount)); next = Math.max(0, old - amount); }
                case 2 -> { assertEquals(old, actual.setCount(key, amount)); next = amount; }
                case 3 -> {
                    assertEquals(old > 0, actual.elementSet().remove(key)); next = 0;
                }
                default -> { assertEquals(old, actual.count(key)); next = old; }
            }
            if (next == 0) expected.remove(key); else expected.put(key, next);
            actual.verifyInvariants();
            assertEquals(expected.keySet(), actual.elementSet());
            assertEquals(expected.size(), actual.distinctSize());
            assertEquals(expected.values().stream().mapToLong(Integer::longValue).sum(), actual.totalCount());
            expected.forEach((k, count) -> assertEquals(count.intValue(), actual.count(k)));
        }
        actual.clear(); actual.clear(); actual.verifyInvariants();
        assertEquals(0, actual.totalCount()); assertTrue(actual.elementSet().isEmpty());
    }

    @Test void countOverflowAndInvalidInputLeaveCountsFlagsAndOrderUnchanged() {
        M3ObjectCounts<String> counts = new M3ObjectCounts<>();
        counts.add("a", Integer.MAX_VALUE); counts.add("b", Integer.MAX_VALUE);
        counts.setFlags("a", 0x80000001);
        assertEquals(2L * Integer.MAX_VALUE, counts.totalCount());
        assertThrows(IllegalArgumentException.class, () -> counts.add("a", 1));
        assertThrows(IllegalArgumentException.class, () -> counts.add("a", -1));
        assertThrows(IllegalArgumentException.class, () -> counts.setCount("a", -1));
        assertThrows(IllegalArgumentException.class, () -> counts.remove("a", -1));
        assertThrows(IllegalArgumentException.class, () -> counts.setFlags("absent", 1));
        assertEquals(Integer.MAX_VALUE, counts.add("a", 0));
        assertEquals(Integer.MAX_VALUE, counts.remove("a", 0));
        assertEquals(0, counts.setCount("absent", 0));
        assertEquals(List.of("a", "b"), new ArrayList<>(counts.elementSet()));
        assertEquals(0x80000001, counts.flags("a"));
        assertEquals(2L * Integer.MAX_VALUE, counts.totalCount());
        counts.verifyInvariants();
    }

    @Test void distinctIteratorRemovalClearsFlagsAndRecycledSlots() {
        M3ObjectCounts<String> counts = new M3ObjectCounts<>();
        for (String key : List.of("a", "b", "c")) { counts.add(key, 7); counts.setFlags(key, -1); }
        Iterator<String> iterator = counts.elementSet().iterator();
        assertThrows(IllegalStateException.class, iterator::remove);
        assertEquals("a", iterator.next()); iterator.remove();
        assertEquals(14, counts.totalCount()); assertEquals(0, counts.flags("a"));
        assertThrows(IllegalStateException.class, iterator::remove);
        assertEquals("b", iterator.next());
        counts.add("x", 3);
        assertThrows(ConcurrentModificationException.class, iterator::hasNext);
        assertEquals(0, counts.flags("x")); counts.verifyInvariants();
        counts.elementSet().removeIf(k -> true);
        assertEquals(0, counts.totalCount()); counts.verifyInvariants();
        counts.add(null, 4); counts.setFlags(null, Integer.MIN_VALUE);
        assertEquals(Arrays.asList((String) null), counts.flaggedElements(Integer.MIN_VALUE, 0, M3IndexPathMasks.Backend.JAVA));
    }

    @Test void primitiveColumnsShareKeySlotsWithoutCountBoxesOrRetainedEntries() throws Exception {
        M3ObjectCounts<String> counts = new M3ObjectCounts<>();
        counts.add("x", 10000); counts.add("y", 20000); counts.setFlags("x", 0x12345678);
        Field keys = M3ObjectCounts.class.getDeclaredField("elements"); keys.setAccessible(true);
        Object map = keys.get(counts);
        assertInstanceOf(M3LinkedHashMap.class, map);
        Field table = M3LinkedHashMap.class.getDeclaredField("table"); table.setAccessible(true);
        Object storage = table.get(map);
        Field values = storage.getClass().getDeclaredField("values"); values.setAccessible(true);
        assertEquals(0, ((Object[]) values.get(storage)).length, "key-only owner has no payload/boxed handles");
        for (String name : List.of("counts", "flags")) {
            Field column = M3ObjectCounts.class.getDeclaredField(name);
            assertEquals(int[].class, column.getType());
        }
        counts.remove("x", Integer.MAX_VALUE); counts.verifyInvariants();
        Field keyColumn = storage.getClass().getDeclaredField("keys"); keyColumn.setAccessible(true);
        assertFalse(Arrays.asList((Object[]) keyColumn.get(storage)).contains("x"));
    }

    @Test void flagWindowsAgreeWithScalarOracleForAllBitsOffsetsAndBothDirections() {
        checkFlags(M3IndexPathMasks.Backend.JAVA);
        if (Boolean.getBoolean("m3.interop.requireNative")) {
            assertTrue(M3IndexPathMasks.nativeAvailable(), "explicit JNI qualification must load real library");
            checkFlags(M3IndexPathMasks.Backend.NATIVE_REQUIRED);
        }
    }

    private static void checkFlags(M3IndexPathMasks.Backend backend) {
        Random random = new Random(0x534c4f54464c4147L);
        M3ObjectCounts<Integer> counts = new M3ObjectCounts<>();
        List<Integer> order = new ArrayList<>();
        Map<Integer, Integer> states = new HashMap<>();
        for (int i = 0; i < 180; i++) {
            Integer key = i == 17 ? null : i;
            counts.add(key, i + 1); int state = random.nextInt(); counts.setFlags(key, state);
            order.add(key); states.put(key, state);
        }
        // Recycle physical slots, making encounter ordinals differ from slot coordinates.
        for (int i = 0; i < 140; i += 3) { counts.remove(i, Integer.MAX_VALUE); order.remove(Integer.valueOf(i)); states.remove(i); }
        for (int i = 180; i < 220; i++) { counts.add(i, 2); counts.setFlags(i, i); order.add(i); states.put(i, i); }
        for (int trial = 0; trial < 900; trial++) {
            int stride = trial % 2 == 0 ? 1 + random.nextInt(4) : -1 - random.nextInt(4);
            int first = random.nextInt(order.size());
            int available = stride > 0 ? (order.size() - 1 - first) / stride + 1 : first / -stride + 1;
            int length = random.nextInt(available + 1);
            int required = trial % 4 == 0 ? 0 : 1 << random.nextInt(32);
            int forbidden = trial % 7 == 0 ? required : 1 << random.nextInt(32);
            List<Integer> expected = new ArrayList<>();
            for (int n = 0, at = first; n < length; n++, at += stride) {
                Integer key = order.get(at); int state = states.get(key);
                if ((state & required) == required && (state & forbidden) == 0) expected.add(key);
            }
            List<Integer> actual = counts.flaggedElements(required, forbidden, first, length, stride, backend, null);
            assertEquals(expected, actual, "trial=" + trial + " backend=" + backend);
            assertThrows(UnsupportedOperationException.class, () -> actual.add(777));
        }
        List<Integer> snapshot = counts.flaggedElements(0, 0, backend);
        counts.clear(); assertEquals(order, snapshot); counts.verifyInvariants();
    }

    @Test void callbacksAndCancellationRetainExceptionIdentityAndRefuseReentrancy() {
        M3ObjectCounts<String> counts = new M3ObjectCounts<>(); counts.add("a", 2); counts.add("b", 3);
        RuntimeException marker = new RuntimeException("marker");
        assertSame(marker, assertThrows(RuntimeException.class, () -> counts.forEachCount((k, v) -> { throw marker; })));
        assertEquals(5, counts.totalCount());
        assertThrows(ConcurrentModificationException.class, () -> counts.forEachCount((k, v) -> counts.setFlags(k, 1)));
        M3Progress canceled = M3Progress.of(2); canceled.cancel();
        assertThrows(CancellationException.class, () -> counts.flaggedElements(0, 0, 0, 2, 1, M3IndexPathMasks.Backend.JAVA, canceled));
        assertThrows(CancellationException.class, () -> counts.forEachCount((k, v) -> fail(), canceled));
        M3Progress reentrant = M3Progress.of(2, p -> counts.add("new", 1));
        assertThrows(ConcurrentModificationException.class, () -> counts.flaggedElements(0, 0, 0, 2, 1, M3IndexPathMasks.Backend.JAVA, reentrant));
        counts.verifyInvariants();
    }
}
