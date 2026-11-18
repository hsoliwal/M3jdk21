/* *****************************************************************************
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
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * M3WeakHashMap marks never-used and erased slots with EMPTY and REMOVED sentinels in its
 * reference lane instead of a byte state lane: against the verbatim pre-change copy
 * M3WeakHashMapReference, the same calls with colliding counting keys, the null key, re-entrant
 * equals callbacks and forced reference clearing give equal results, exceptions, hashCode and
 * equals calls (with their arguments), iteration order and private lanes.
 */
//FLLDCIM: LEETCODIFY - differential proof for the M3WeakHashMap EMPTY/REMOVED slots
class M3WeakHashMapSentinelTest {
    private static final int[] HASHES = {0, 0, 1, 1, 16, 17, 32, 65536, 2, 3, 0, 48};
    private static final Key[] POOL = new Key[HASHES.length];
    private static Side current;

    static {
        for (int i = 0; i < POOL.length; i++) {
            POOL[i] = new Key(i, HASHES[i]);
        }
    }

    /** One map with its own call log and re-entry counter. */
    private static final class Side {
        final Map<Key, String> map;
        final StringBuilder log = new StringBuilder();
        int equalsCalls;
        boolean inside;
        int reentry;

        Side(Map<Key, String> map) {
            this.map = map;
        }
    }

    /** Key with a chosen hash that logs every hashCode and equals call on the current side. */
    private static final class Key {
        final int id;
        final int hash;

        Key(int id, int hash) {
            this.id = id;
            this.hash = hash;
        }

        @Override public int hashCode() {
            current.log.append('h').append(id).append(' ');
            return hash;
        }

        @Override public boolean equals(Object other) {
            Side side = current;
            side.log.append('e').append(id).append(':').append(name(other)).append(' ');
            if (side.reentry != 0 && ++side.equalsCalls % side.reentry == 0 && !side.inside) {
                side.inside = true;
                try {
                    side.log.append(side.equalsCalls % 2 == 0 ? "rs" + side.map.size()
                            : "rp" + side.map.put(POOL[11], "re")).append(' ');
                } finally {
                    side.inside = false;
                }
            }
            return other instanceof Key key && key.id == id;
        }
    }

    private static String name(Object other) {
        if (other == null) {
            return "null";
        }
        if (other instanceof Key key) {
            return "k" + key.id;
        }
        return other == staticField(current.map, "NULL_KEY") ? "NK" : "FOREIGN";
    }

    private static Object staticField(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static Object read(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** Slot kind: "-" never used, "x" erased, then the null box or the key reference. */
    private static String slot(Object map, Object reference, boolean patched, int index) {
        if (patched ? reference == staticField(map, "EMPTY")
                : ((byte[]) read(map, "states"))[index] == 0) {
            return "-";
        }
        if (patched ? reference == staticField(map, "REMOVED")
                : ((byte[]) read(map, "states"))[index] == 2) {
            return "x";
        }
        if (reference == staticField(map, "NULL_KEY")) {
            return "NK";
        }
        Object key = ((Reference<?>) reference).get();
        return (key == null ? "dead" : "k" + ((Key) key).id) + "@" + read(reference, "slot");
    }

    private static String lanes(Object map, boolean patched) {
        Object[] references = (Object[]) read(map, "references");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < references.length; i++) {
            text.append(slot(map, references[i], patched, i)).append(' ');
        }
        return text + Arrays.toString((Object[]) read(map, "values"))
                + Arrays.toString((int[]) read(map, "hashes")) + read(map, "size") + "/"
                + read(map, "occupied") + "/" + read(map, "modifications");
    }

    private static String outcome(Side side, Supplier<Object> action) {
        current = side;
        try {
            return "=" + action.get();
        } catch (RuntimeException e) {
            return "!" + e.getClass().getName();
        }
    }

    private static String entries(Map<Key, String> map, int removeEvery) {
        StringBuilder text = new StringBuilder();
        Iterator<Map.Entry<Key, String>> it = map.entrySet().iterator();
        for (int i = 0; it.hasNext(); i++) {
            Map.Entry<Key, String> entry = it.next();
            text.append(entry.getKey() == null ? "null" : "k" + entry.getKey().id).append('=')
                    .append(entry.getValue()).append(' ');
            if (removeEvery > 0 && i % removeEvery == 0) {
                it.remove();
            }
        }
        return text.toString();
    }

    /** Clears (and optionally enqueues) the key reference at the same slot in both maps. */
    private static void clearReference(Side ref, Side impl, int pick, boolean enqueue) {
        Object[] refLane = (Object[]) read(ref.map, "references");
        Object[] implLane = (Object[]) read(impl.map, "references");
        if (refLane.length == 0) {
            return;
        }
        int s = pick % refLane.length;
        if (refLane[s] instanceof Reference<?> a && implLane[s] instanceof Reference<?> b) {
            if (enqueue) {
                a.enqueue();
                b.enqueue();
            } else {
                a.clear();
                b.clear();
            }
        }
    }

    private static Object step(Side side, int op, Key key, String value, int arg) {
        Map<Key, String> map = side.map;
        return switch (op) {
            case 0, 1, 2 -> map.put(key, value);
            case 3, 4 -> map.get(key);
            case 5, 6 -> map.remove(key);
            case 7 -> map.containsKey(key);
            case 8 -> map.size();
            case 9 -> map.putIfAbsent(key, value);
            case 10 -> map.getOrDefault(key, "dflt");
            case 11 -> map.computeIfAbsent(key, k -> arg % 3 == 0 ? null : value);
            case 12 -> map.merge(key, value, (a, b) -> arg % 4 == 0 ? null : a + b);
            case 13 -> entries(map, arg % 4);
            case 14 -> map.containsValue(value);
            case 15 -> map.isEmpty();
            default -> map.hashCode();
        };
    }

    @Test
    @Timeout(value = 300, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void randomOperationsMatchMaster() {
        for (int seed = 0; seed < 300; seed++) {
            Random random = new Random(0x3EAEL * 1_000_003L + seed);
            Side ref = new Side(new M3WeakHashMapReference<>());
            Side impl = new Side(new M3WeakHashMap<>());
            int reentry = random.nextInt(3) == 0 ? 3 + random.nextInt(9) : 0;
            ref.reentry = reentry;
            impl.reentry = reentry;
            for (int step = 0; step < 400; step++) {
                Key key = random.nextInt(9) == 0 ? null : POOL[random.nextInt(POOL.length)];
                int op = random.nextInt(24);
                String value = random.nextInt(8) == 0 ? null : "v" + random.nextInt(5);
                int arg = random.nextInt(1 << 20);
                if (op == 18 || op == 19) {
                    clearReference(ref, impl, arg, op == 19);
                } else if (op == 20) {
                    current = ref;
                    ((M3WeakHashMapReference<Key, String>) ref.map).expungeStaleKeys();
                    current = impl;
                    ((M3WeakHashMap<Key, String>) impl.map).expungeStaleKeys();
                } else if (op == 21 && random.nextInt(6) == 0) {
                    current = ref;
                    ref.map.clear();
                    current = impl;
                    impl.map.clear();
                } else {
                    int o = op;
                    assertEquals(outcome(ref, () -> step(ref, o, key, value, arg)),
                            outcome(impl, () -> step(impl, o, key, value, arg)), "op " + op);
                }
                assertEquals(ref.log.toString(), impl.log.toString(), "calls at " + step);
                ref.log.setLength(0);
                impl.log.setLength(0);
                assertEquals(lanes(ref.map, false), lanes(impl.map, true), "lanes at " + step);
            }
        }
    }
}
