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
package jdk.m3.collections.apache.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * The random lockstep script: list iterator steps (opened from the view, at an index or on a
 * sub-list, in or out of range), map changes that relink, shrink below a live cursor or leave
 * the links alone, and list queries that iterate, with reentrant keys armed when asked.
 */
final class LockstepOps {

    /** Live iterator slots per side. */
    private static final int SLOTS = 4;

    /** Most keys one forEachRemaining takes: a subclass's get(int) may never run out. */
    private static final int DRAIN_CAP = 500;

    /** Keys that change the map from equals, sharing buckets with Integer keys. */
    private static final ReentrantKey[] REENTRANT = {
        new ReentrantKey(3), new ReentrantKey(7), new ReentrantKey(11), new ReentrantKey(20),
    };

    /** The Integer keys 0 to 15, then null, then the reentrant keys. */
    private static final int POOL = 16 + 1 + REENTRANT.length;

    private LockstepOps() {
    }

    /**
     * Draws a key from the pool.
     *
     * @param rnd The random source
     * @return An Integer, null or a reentrant key
     */
    static Object key(final Random rnd) {
        final int r = rnd.nextInt(POOL);
        if (r < 16) {
            return r;
        }
        return r == 16 ? null : REENTRANT[r - 17];
    }

    /**
     * Runs one random step.
     *
     * @param d The driver
     * @param rnd The random source
     * @param reentrant Whether list queries arm the reentrant keys
     */
    static void step(final LockstepDriver d, final Random rnd, final boolean reentrant) {
        final int kind = rnd.nextInt(10);
        if (kind < 5) {
            iteratorStep(d, rnd);
        } else if (kind < 8) {
            mapStep(d, rnd);
        } else {
            listStep(d, rnd, reentrant && !d.shared() ? rnd.nextInt(4) : 0);
        }
    }

    private static void iteratorStep(final LockstepDriver d, final Random rnd) {
        final int slot = rnd.nextInt(SLOTS);
        if (!d.has(slot) || rnd.nextInt(12) == 0) {
            open(d, rnd, slot);
            return;
        }
        final int op = rnd.nextInt(16);
        final int count = rnd.nextInt(7);
        final int change = rnd.nextInt(5);
        final Object x = key(rnd);
        final boolean mutate = !d.shared() && rnd.nextBoolean();
        d.both("it" + slot + " op" + op + " " + count + "/" + change, 0,
                side -> move(side, slot, op, count, change, x, mutate));
    }

    private static void open(final LockstepDriver d, final Random rnd, final int slot) {
        final int n = d.size();
        final int k = rnd.nextInt(n + 5) - 2;
        final int a = rnd.nextInt(n + 3) - 1;
        final int b = rnd.nextInt(n + 3) - 1;
        final int j = rnd.nextInt(Math.max(b - a, 0) + 4) - 1;
        switch (rnd.nextInt(4)) {
            case 0 -> d.open(slot, "listIterator()", List::listIterator);
            case 1 -> d.open(slot, "listIterator(" + k + ")", v -> v.listIterator(k));
            case 2 -> d.open(slot, "listIterator(n)", v -> v.listIterator(v.size()));
            default -> d.open(slot, "subList(" + a + ", " + b + ").listIterator(" + j + ")",
                    v -> v.subList(a, b).listIterator(j));
        }
    }

    private static Object move(final LockstepSide side, final int slot, final int op,
            final int count, final int change, final Object x, final boolean mutate) {
        final ListIterator<Object> it = side.iterator(slot);
        return switch (op) {
            case 0, 1, 2 -> it.next();
            case 3, 4 -> it.previous();
            case 5 -> IntStream.range(0, count).mapToObj(i -> it.next()).toList();
            case 6 -> IntStream.range(0, count).mapToObj(i -> it.previous()).toList();
            case 7, 8 -> drain(side, it, count, change, mutate);
            default -> query(it, op, x);
        };
    }

    private static Object query(final ListIterator<Object> it, final int op, final Object x) {
        return switch (op) {
            case 9 -> it.hasNext();
            case 10 -> it.hasPrevious();
            case 11 -> it.nextIndex();
            case 12 -> it.previousIndex();
            case 13 -> unsupported(() -> it.set(x));
            case 14 -> unsupported(() -> it.add(x));
            default -> unsupported(it::remove);
        };
    }

    private static Object unsupported(final Runnable mutator) {
        mutator.run();
        return "mutated";
    }

    /**
     * forEachRemaining with a consumer that stops after {@code stop} keys (or the cap when 0)
     * and, when asked, changes the map after {@code change} keys.
     */
    private static Object drain(final LockstepSide side, final ListIterator<Object> it,
            final int stop, final int change, final boolean mutate) {
        final List<Object> seen = new ArrayList<>();
        try {
            it.forEachRemaining(key -> consume(side, seen, key, stop, change, mutate));
            seen.add("done");
        } catch (final IllegalStateException e) {
            seen.add(e.getMessage());
        }
        seen.add(it.nextIndex());
        seen.add(it.hasNext());
        return seen;
    }

    private static void consume(final LockstepSide side, final List<Object> seen,
            final Object key, final int stop, final int change, final boolean mutate) {
        seen.add(key);
        if (mutate && seen.size() == change) {
            if (change % 2 == 0) {
                side.map.remove(side.map.firstKey());
            } else {
                side.map.put(-seen.size(), "drain");
            }
        }
        if (seen.size() == stop || seen.size() == DRAIN_CAP) {
            throw new IllegalStateException("stop " + seen.size());
        }
    }

    private static void mapStep(final LockstepDriver d, final Random rnd) {
        final int op = rnd.nextInt(11);
        final Object key = key(rnd);
        final int value = rnd.nextInt(1000);
        final int r = rnd.nextInt(d.size() + 3) - 1;
        d.map("map op" + op + " " + key + " " + value + " " + r,
                m -> change(m, op, key, value, r));
    }

    private static Object change(final LinkedMap<Object, Object> m, final int op,
            final Object key, final int value, final int r) {
        return switch (op) {
            case 0, 1, 2 -> m.put(key, value);
            case 3, 4 -> m.remove(key);
            case 5 -> m.remove(r);
            case 6 -> value % 4 == 0 ? clear(m) : m.put(key, value);
            case 7 -> putAll(m, key, value);
            default -> walkThenChange(m, op, value, r);
        };
    }

    private static Object clear(final LinkedMap<Object, Object> m) {
        m.clear();
        return "clear";
    }

    private static Object putAll(final LinkedMap<Object, Object> m, final Object key,
            final int value) {
        final LinkedMap<Object, Object> more = new LinkedMap<>();
        more.put(key, value);
        more.put(value % 16, value);
        more.put(-1 - value % 3, value);
        m.putAll(more);
        return "putAll";
    }

    /** Steps a map, key-set or entry-set iterator r + 1 times, then removes or sets a value. */
    private static Object walkThenChange(final LinkedMap<Object, Object> m, final int op,
            final int value, final int r) {
        final int steps = Math.max(r, 0) + 1;
        if (op == 8) {
            return removeAfter(m.mapIterator(), steps, "mapIterator.remove");
        }
        if (op == 9) {
            return removeAfter(m.keySet().iterator(), steps, "keySet.remove");
        }
        final Iterator<Map.Entry<Object, Object>> it = m.entrySet().iterator();
        IntStream.range(0, steps - 1).forEach(i -> it.next());
        return it.next().setValue(value);
    }

    private static Object removeAfter(final Iterator<?> it, final int steps, final String what) {
        IntStream.range(0, steps).forEach(i -> it.next());
        it.remove();
        return what;
    }

    private static void listStep(final LockstepDriver d, final Random rnd, final int changes) {
        final int op = rnd.nextInt(16);
        final Object key = key(rnd);
        final int i = rnd.nextInt(d.size() + 3) - 1;
        final int j = rnd.nextInt(d.size() + 3) - 1;
        d.both("list op" + op + " " + key + " " + i + " " + j + " armed " + changes, changes,
                side -> query(side, op, key, i, j));
    }

    private static Object query(final LockstepSide side, final int op, final Object key,
            final int i, final int j) {
        final List<Object> v = side.view();
        return switch (op) {
            case 0, 1 -> v.equals(side.view());
            case 2 -> v.equals(snapshot(side.map, 0, null));
            case 3 -> v.equals(snapshot(side.map, 0, "changed"));
            case 4 -> v.equals(snapshot(side.map, 1, null));
            case 5 -> v.equals("not a list");
            default -> read(side, v, op, key, i, j);
        };
    }

    private static Object read(final LockstepSide side, final List<Object> v, final int op,
            final Object key, final int i, final int j) {
        return switch (op) {
            case 6 -> v.hashCode();
            case 7 -> v.toString();
            case 8 -> v.get(i);
            case 9 -> v.indexOf(key);
            case 10 -> v.lastIndexOf(key);
            case 11 -> v.containsAll(Arrays.asList(key, i));
            case 12 -> new ArrayList<>(v.subList(i, j));
            case 13 -> v.subList(i, j).equals(side.view().subList(i, j));
            default -> v.subList(i, j).hashCode();
        };
    }

    /**
     * Copies the keys, dropping the last {@code drop} of them, then replacing the last with
     * {@code last} when it is not null.
     */
    private static List<Object> snapshot(final LinkedMap<Object, Object> map, final int drop,
            final Object last) {
        final List<Object> keys = new ArrayList<>(map.keySet());
        keys.subList(Math.max(keys.size() - drop, 0), keys.size()).clear();
        if (last != null && !keys.isEmpty()) {
            keys.set(keys.size() - 1, last);
        }
        return keys;
    }
}
