/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Deterministic oracle for wraparound linear-probe back-shift of stable slots. */
@Timeout(20)
final class M3LinkedHashBackshiftTest {
    // Colliding home bucket 15 deliberately crosses both ends of a power-of-two index.
    private record HotKey(int id) {
        @Override public int hashCode() { return 15; }
    }

    @Test void deletionCompactsOnlyHashCoordinatesAcrossWraparound() {
        for (boolean accessOrder : new boolean[] {false, true}) {
            M3LinkedHashMap<HotKey, Integer> actual = new M3LinkedHashMap<>(accessOrder);
            LinkedHashMap<HotKey, Integer> expected = new LinkedHashMap<>(16, .75f, accessOrder);
            HotKey[] keys = new HotKey[149];
            for (int i = 0; i < keys.length; i++) keys[i] = new HotKey(i);
            Random random = new Random(0x4d334241434b5348L);
            for (int step = 0; step < 24_000; step++) {
                HotKey key = step % 101 == 0 ? null : keys[random.nextInt(keys.length)];
                Integer value = step % 13 == 0 ? null : random.nextInt(500);
                int operation = random.nextInt(10);
                Object oracle = apply(expected, key, value, operation);
                Object result = apply(actual, key, value, operation);
                assertEquals(oracle, result, "step " + step + " operation " + operation);
                if ((step & 127) == 0) {
                    assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()));
                    assertEquals(new ArrayList<>(expected.reversed().keySet()),
                            new ArrayList<>(actual.reversed().keySet()));
                    assertEquals(expected, actual);
                    assertEquals(expected.hashCode(), actual.hashCode());
                    actual.verifyInvariants();
                }
            }
            Iterator<Map.Entry<HotKey, Integer>> a = actual.entrySet().iterator();
            Iterator<Map.Entry<HotKey, Integer>> e = expected.entrySet().iterator();
            int ordinal = 0;
            while (e.hasNext()) {
                assertTrue(a.hasNext());
                assertEquals(e.next(), a.next());
                if ((ordinal++ & 3) == 0) { e.remove(); a.remove(); }
            }
            assertFalse(a.hasNext());
            assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()));
            actual.verifyInvariants();
            Iterator<HotKey> failFast = actual.keySet().iterator();
            actual.put(new HotKey(9999), 12);
            assertThrows(ConcurrentModificationException.class, failFast::next);
            actual.verifyInvariants();
        }
    }

    private static Object apply(Map<HotKey, Integer> map, HotKey key,
            Integer value, int operation) {
        return switch (operation) {
            case 0 -> map.put(key, value);
            case 1 -> map.remove(key);
            case 2 -> map.get(key);
            case 3 -> map.getOrDefault(key, -1);
            case 4 -> map.putIfAbsent(key, value);
            case 5 -> map.remove(key, value);
            case 6 -> map.compute(key, (k, old) -> value);
            case 7 -> map.containsKey(key);
            case 8 -> ((java.util.SequencedMap<HotKey, Integer>) map).putFirst(key, value);
            default -> ((java.util.SequencedMap<HotKey, Integer>) map).putLast(key, value);
        };
    }
}
