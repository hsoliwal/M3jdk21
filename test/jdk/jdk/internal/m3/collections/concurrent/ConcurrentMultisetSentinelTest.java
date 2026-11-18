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
package jdk.internal.m3.collections.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * ConcurrentMultiset marks emptied slots with a REMOVED sentinel instead of per-slot state words:
 * against the verbatim pre-change copy ConcurrentMultisetReference, the same calls with counting
 * keys crowded into two stripes (forcing tombstone reuse, repacking and region growth) give equal
 * results, exceptions, hashCode and equals calls, element order and private lanes.
 */
//FLLDCIM: LEETCODIFY - differential proof for the ConcurrentMultiset REMOVED slots
class ConcurrentMultisetSentinelTest {
    private static final Key[] POOL = new Key[260];
    private static StringBuilder log = new StringBuilder();

    static {
        int found = 0;
        for (int h = 1; found < POOL.length; h++) {
            int spread = h * 0x9E3779B9;
            int stripe = (spread ^ spread >>> 16) >>> 27;
            if (stripe == 0 || stripe == 1 && found % 3 == 0) {
                POOL[found] = new Key(found, h);
                found++;
            }
        }
    }

    /** Key with a chosen hash that logs every hashCode and equals call. */
    private static final class Key {
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
            log.append('e').append(id).append(':').append(other instanceof Key key ? "k" + key.id
                    : String.valueOf(other == null ? null : "FOREIGN")).append(' ');
            return other instanceof Key key && key.id == id;
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

    private static Object removedOf(Object owner) {
        try {
            Field field = owner.getClass().getDeclaredField("REMOVED");
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** Slot kinds: "-" never handed out or wiped, "x" emptied, else the key id. */
    private static String lanes(Object multiset, boolean patched) {
        Object[] elements = (Object[]) read(multiset, "elementLane");
        Object sentinel = patched ? removedOf(multiset) : null;
        long[] states = patched ? null : (long[]) read(multiset, "stateMaskWords");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            boolean removed = patched ? elements[i] == sentinel : states[i] == 2L;
            text.append(removed ? "x" : elements[i] == null ? "-" : "k" + ((Key) elements[i]).id)
                    .append(' ');
        }
        return text + Arrays.toString((int[]) read(multiset, "countLane"))
                + Arrays.toString((int[]) read(multiset, "hashIndexLane"))
                + Arrays.toString((int[]) read(multiset, "used"))
                + Arrays.toString((int[]) read(multiset, "live"))
                + Arrays.toString((long[]) read(multiset, "total")) + read(multiset, "region");
    }

    private static String outcome(StringBuilder side, Supplier<Object> action) {
        log = side;
        try {
            return "=" + action.get();
        } catch (RuntimeException e) {
            return "!" + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private static String walk(Set<Object> elements, int removeEvery) {
        StringBuilder text = new StringBuilder();
        Iterator<Object> it = elements.iterator();
        for (int i = 0; it.hasNext(); i++) {
            text.append('k').append(((Key) it.next()).id).append(' ');
            if (removeEvery > 0 && i % removeEvery == 0) {
                it.remove();
            }
        }
        return text.toString();
    }

    private static Object stepReference(ConcurrentMultisetReference<Object> m, int op, Key key,
            int n, int arg) {
        return switch (op) {
            case 0, 1, 2, 3 -> m.add(key, n);
            case 4, 5 -> m.remove(key, n);
            case 6 -> m.setCount(key, n);
            case 7 -> m.setCount(key, arg % 4, n);
            case 8 -> m.count(key);
            case 9 -> m.size() + "/" + m.isEmpty() + "/" + m.elementSet().size();
            case 10 -> m.elementSet().contains(key);
            case 11 -> m.elementSet().remove(key);
            default -> walk(m.elementSet(), arg % 5);
        };
    }

    private static Object stepPatched(ConcurrentMultiset<Object> m, int op, Key key, int n,
            int arg) {
        return switch (op) {
            case 0, 1, 2, 3 -> m.add(key, n);
            case 4, 5 -> m.remove(key, n);
            case 6 -> m.setCount(key, n);
            case 7 -> m.setCount(key, arg % 4, n);
            case 8 -> m.count(key);
            case 9 -> m.size() + "/" + m.isEmpty() + "/" + m.elementSet().size();
            case 10 -> m.elementSet().contains(key);
            case 11 -> m.elementSet().remove(key);
            default -> walk(m.elementSet(), arg % 5);
        };
    }

    @Test
    @Timeout(value = 300, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void randomOperationsMatchMaster() {
        StringBuilder refLog = new StringBuilder();
        StringBuilder implLog = new StringBuilder();
        for (int seed = 0; seed < 60; seed++) {
            Random random = new Random(0x5E7L * 1_000_003L + seed);
            ConcurrentMultisetReference<Object> ref = new ConcurrentMultisetReference<>();
            ConcurrentMultiset<Object> impl = new ConcurrentMultiset<>();
            int keys = 20 + random.nextInt(POOL.length - 20);
            for (int step = 0; step < 1500; step++) {
                Key key = POOL[random.nextInt(keys)];
                int op = random.nextInt(14);
                int n = random.nextInt(12) == 0 ? -1 : random.nextInt(4);
                int arg = random.nextInt(1 << 20);
                if (op == 13 && random.nextInt(20) == 0) {
                    log = refLog;
                    ref.clear();
                    log = implLog;
                    impl.clear();
                } else {
                    assertEquals(outcome(refLog, () -> stepReference(ref, op, key, n, arg)),
                            outcome(implLog, () -> stepPatched(impl, op, key, n, arg)), "op " + op);
                }
                assertEquals(refLog.toString(), implLog.toString(), "calls at " + step);
                refLog.setLength(0);
                implLog.setLength(0);
                assertEquals(lanes(ref, false), lanes(impl, true), "lanes at " + step);
            }
        }
    }
}
