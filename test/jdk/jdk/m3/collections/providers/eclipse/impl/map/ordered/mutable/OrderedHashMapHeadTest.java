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
package jdk.m3.collections.eclipse.impl.map.ordered.mutable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import jdk.m3.collections.eclipse.api.map.MutableOrderedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * OrderedHashMap starts every forward scan at a head index before which all pairs are
 * REMOVED_KEY: against the verbatim pre-change copy OrderedHashMapReference, the same calls with
 * logging keys (null included), queue-like removal of the first key, iterators started before a
 * rehash, index-range visits, take/drop/removeIf and serialization give equal results,
 * exceptions, hashCode and equals calls, and equal private lanes; the head is always the first
 * live pair.
 */
//FLLDCIM: LEETCODIFY - differential proof for the OrderedHashMap OFFSET_ARRAY head
class OrderedHashMapHeadTest {
    private static final Key[] POOL = new Key[40];
    private static StringBuilder log = new StringBuilder();

    static {
        for (int i = 0; i < POOL.length; i++) {
            POOL[i] = new Key(i, i % 7 == 0 ? 0 : i * 31);
        }
    }

    /** Key with a chosen hash that logs every hashCode and equals call. */
    private static final class Key implements Serializable {
        private static final long serialVersionUID = 1L;
        final int id;
        final int hash;

        Key(int id, int hash) {
            this.id = id;
            this.hash = hash;
        }

        @Override public int hashCode() {
            log.append('h').append(id).append(' ');
            return hash;
        }

        @Override public boolean equals(Object other) {
            log.append('e').append(id).append(':')
                    .append(other instanceof Key key ? "k" + key.id : String.valueOf(other))
                    .append(' ');
            return other instanceof Key key && key.id == id;
        }

        @Override public String toString() {
            return "k" + id;
        }
    }

    private static Object read(Object owner, String name) {
        try {
            Field field = (owner instanceof OrderedHashMap<?, ?> ? OrderedHashMap.class
                    : OrderedHashMapReference.class).getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static String pair(Object key) {
        if (key == null) {
            return "-";
        }
        String text = String.valueOf(key);
        return text.endsWith("REMOVED_KEY") ? "x" : text.endsWith("NULL_KEY") ? "NK" : text;
    }

    private static String lanes(Object map) {
        Object[] pairs = (Object[]) read(map, "orderedKeyValues");
        StringBuilder text = new StringBuilder();
        for (int i = 0; pairs != null && i < pairs.length; i += 2) {
            text.append(pair(pairs[i])).append('=').append(pairs[i + 1]).append(' ');
        }
        return text + Arrays.toString((int[]) read(map, "indicesHashTable"))
                + read(map, "orderedKeyValuesOccupied") + "/"
                + read(map, "indicesHashTableOccupied") + "/" + read(map, "size");
    }

    /** The head invariant: it is the first live pair, or the occupied end when none is live. */
    private static void checkHead(OrderedHashMap<?, ?> map) {
        int head = (int) read(map, "head");
        int occupied = (int) read(map, "orderedKeyValuesOccupied");
        Object[] pairs = (Object[]) read(map, "orderedKeyValues");
        assertTrue(head >= 0 && head <= occupied, "head " + head + " occupied " + occupied);
        int first = 0;
        while (first < occupied && "x".equals(pair(pairs[first << 1]))) {
            first++;
        }
        assertEquals(first, head, "head is the first live pair");
    }

    private static String outcome(StringBuilder side, Supplier<Object> action) {
        log = side;
        try {
            return "=" + action.get();
        } catch (RuntimeException e) {
            return "!" + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private static <T> String drain(Iterator<T> it, int removeEvery) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; it.hasNext(); i++) {
            text.append(it.next()).append(' ');
            if (removeEvery > 0 && i % removeEvery == 0) {
                it.remove();
            }
        }
        return text.toString();
    }

    private static Object roundTrip(Object map) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(map);
            }
            try (ObjectInputStream in = new ObjectInputStream(
                    new ByteArrayInputStream(bytes.toByteArray()))) {
                return lanes(in.readObject());
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("deprecation") // getFirst is the inherited entry point the head serves
    private static Object step(MutableOrderedMap<Key, String> m, int op, Key key, String value,
            int arg) {
        int size = m.size();
        return switch (op) {
            case 0, 1, 2, 3 -> m.put(key, value);
            case 4 -> m.get(key);
            case 5, 6 -> m.removeKey(key);
            case 7, 8, 9 -> size == 0 ? "empty" : m.removeKey(m.keysView().getFirst());
            case 10 -> m.containsKey(key) + "/" + m.containsValue(value);
            case 11 -> m.getFirst() + "/" + m.getLast();
            case 12 -> drain(m.iterator(), arg % 4);
            case 13 -> drain(m.keySet().iterator(), arg % 5);
            case 14 -> drain(m.entrySet().iterator(), arg % 3);
            case 15 -> {
                StringBuilder text = new StringBuilder();
                m.forEachKeyValue((k, v) -> text.append(k).append('=').append(v).append(' '));
                yield text;
            }
            case 16 -> {
                StringBuilder text = new StringBuilder();
                m.forEach(arg % (size + 1), (arg >> 8) % (size + 1),
                        v -> text.append(v).append(' '));
                yield text;
            }
            case 17 -> {
                StringBuilder text = new StringBuilder();
                m.forEachWithIndex(arg % (size + 1), (arg >> 8) % (size + 1),
                        (v, i) -> text.append(i).append(':').append(v).append(' '));
                yield text;
            }
            case 18 -> m.take(arg % (size + 2)).toString() + m.drop(arg % (size + 2));
            case 19 -> m.removeIf((k, v) -> k != null && (k.id + arg) % 5 == 0);
            case 20 -> m.toString() + m.hashCode() + m.equals(Map.copyOf(Map.of()));
            case 21 -> {
                Iterator<String> it = m.iterator();
                for (int i = 0; i < 6; i++) {
                    m.put(POOL[(arg + i * 7) % POOL.length], "s" + i);
                }
                yield drain(it, 0);
            }
            case 22 -> roundTrip(m);
            default -> {
                if (arg % 9 == 0) {
                    m.clear();
                }
                yield m.size();
            }
        };
    }

    @Test
    @Timeout(value = 300, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void randomOperationsMatchMaster() {
        StringBuilder refLog = new StringBuilder();
        StringBuilder implLog = new StringBuilder();
        for (int seed = 0; seed < 300; seed++) {
            Random random = new Random(0x0AEADL * 1_000_003L + seed);
            boolean sized = random.nextBoolean();
            int capacity = random.nextInt(20);
            MutableOrderedMap<Key, String> ref = sized ? new OrderedHashMapReference<>(capacity)
                    : new OrderedHashMapReference<>();
            OrderedHashMap<Key, String> impl = sized ? new OrderedHashMap<>(capacity)
                    : new OrderedHashMap<>();
            int keys = 4 + random.nextInt(POOL.length - 4);
            for (int step = 0; step < 400; step++) {
                Key key = random.nextInt(12) == 0 ? null : POOL[random.nextInt(keys)];
                int op = random.nextInt(24);
                String value = random.nextInt(9) == 0 ? null : "v" + random.nextInt(6);
                int arg = random.nextInt(1 << 16);
                assertEquals(outcome(refLog, () -> step(ref, op, key, value, arg)),
                        outcome(implLog, () -> step(impl, op, key, value, arg)), "op " + op);
                assertEquals(refLog.toString(), implLog.toString(), "calls at " + step);
                refLog.setLength(0);
                implLog.setLength(0);
                assertEquals(lanes(ref), lanes(impl), "lanes at " + step);
                checkHead(impl);
            }
        }
    }
}
