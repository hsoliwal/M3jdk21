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
package com.m3.collections.apache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedLinkedHashMap;
import org.apache.commons.collections4.multimap.LinkedHashSetValuedLinkedHashMap;

/**
 * Differential run for the incremental counters of {@link M3AbstractMultiValuedMap} (leaf D1). Seeded random
 * operations run on {@link M3ListValuedMap} and {@link M3SetValuedMap} and on the Commons Collections 4.5.0
 * ArrayListValuedLinkedHashMap and LinkedHashSetValuedLinkedHashMap oracles. Every operation's return value or
 * exception type must match. After each batch of one to four operations (no reads in between, so incremental updates
 * chain) size(), isEmpty(), keySet().size(), keys().uniqueSet().size() and asMap().size() must equal the oracle's
 * logical values and a recount through get(key), which reads the groups and never the counters, and every key's group
 * must hold the same values in the same order.
 * <p>
 * Operations: map writes and bulk writes, wrapped-collection writes, removeIf with a predicate that reads size()
 * during the write, iterator removal on values(), entries(), mapIterator() and asMap(), list cursors, writes through
 * counted sub-list and reversed views, raw writes after getMap() (the epoch guard) and serialization round trips.
 * A sub-list write can empty a group that stays attached: an empty list in Commons, a private empty owner in M3.
 * Logical values are then computed from the oracle's non-empty groups, and the key-set and asMap() writes whose
 * Commons behaviour differs for such a group run only while none is attached.
 */
//FLLDCIM: ALG - LC904 distinct-count, LC992 logical key count: counters checked against an oracle and a recount
public final class M3MultiValuedMapCounterDifferentialTest {
    private static final String[] KEYS = {null, "a", "b", "c", "Aa", "BB", "last"};
    private static final String[] VALUES = {null, "x", "y", "z", "Aa", "BB", "dup"};
    private static final int SEEDS = 4;
    private static final int OPERATIONS_PER_RUN = 12_500;
    private static final int ROUND_TRIP_EVERY = 1_000;
    private static final int MAX_BATCH = 4;
    private static long operations, batches, checks, roundTrips, rawWrites, viewWrites, callbackReads;

    private M3MultiValuedMapCounterDifferentialTest() {
    }

    /** One owned map and its oracle; both are replaced by their copies on a serialization round trip. */
    private static final class Pair {
        MultiValuedMap<String, String> owned;
        MultiValuedMap<String, String> oracle;
        final boolean list;

        Pair(final MultiValuedMap<String, String> owned, final MultiValuedMap<String, String> oracle, final boolean list) {
            this.owned = owned;
            this.oracle = oracle;
            this.list = list;
        }
    }

    public static void main(final String[] args) throws Exception {
        for (int seed = 0; seed < SEEDS; seed++) {
            run(new Pair(new M3ListValuedMap<>(), new ArrayListValuedLinkedHashMap<>(), true), new Random(0x1D_0000L + seed));
            run(new Pair(new M3SetValuedMap<>(), new LinkedHashSetValuedLinkedHashMap<>(), false), new Random(0x5D_0000L + seed));
        }
        System.out.println("PASS M3_MULTIVALUE_COUNTERS operations=" + operations + " batches=" + batches + " checks=" + checks
                + " roundTrips=" + roundTrips + " rawWrites=" + rawWrites + " viewWrites=" + viewWrites
                + " callbackReads=" + callbackReads);
    }

    private static void run(final Pair p, final Random random) throws IOException, ClassNotFoundException {
        int done = 0;
        int sinceRoundTrip = 0;
        while (done < OPERATIONS_PER_RUN) {
            final int batch = Math.min(1 + random.nextInt(MAX_BATCH), OPERATIONS_PER_RUN - done);
            for (int i = 0; i < batch; i++) {
                step(p, random);
            }
            done += batch;
            operations += batch;
            batches++;
            verify(p, "seeded batch ending at operation " + done + (p.list ? " (list)" : " (set)"));
            sinceRoundTrip += batch;
            if (sinceRoundTrip >= ROUND_TRIP_EVERY) {
                sinceRoundTrip = 0;
                p.owned = copy(p.owned);
                p.oracle = copy(p.oracle);
                roundTrips++;
                verify(p, "round trip after operation " + done);
            }
        }
    }

    private static void step(final Pair p, final Random r) {
        final String k = KEYS[r.nextInt(KEYS.length)];
        final String v = VALUES[r.nextInt(VALUES.length)];
        final List<String> vs = values(r);
        final int n = r.nextInt(6);
        final boolean flip = r.nextBoolean();
        final boolean rare = r.nextInt(40) == 0;
        final int op = r.nextInt(p.list ? 30 : 22);
        if (op >= 15 && op <= 17 && hasEmptyOwner(p.oracle)) {
            same(p, m -> m.put(k, v));
            return;
        }
        switch (op) {
            case 0, 1, 2 -> same(p, m -> m.put(k, v));
            case 3 -> same(p, m -> m.putAll(k, vs));
            case 4 -> {
                final Map<String, String> source = new LinkedHashMap<>();
                vs.forEach(value -> source.put(KEYS[r.nextInt(KEYS.length)], value));
                same(p, m -> m.putAll(source));
            }
            case 5 -> {
                final MultiValuedMap<String, String> source = new ArrayListValuedLinkedHashMap<>();
                vs.forEach(value -> source.put(KEYS[r.nextInt(KEYS.length)], value));
                same(p, m -> m.putAll(source));
            }
            case 6 -> same(p, m -> m.remove(k));
            case 7, 8 -> same(p, m -> m.removeMapping(k, v));
            case 9 -> same(p, m -> rare ? clear(m) : m.get(k).add(v));
            case 10 -> same(p, m -> m.get(k).addAll(vs));
            case 11 -> same(p, m -> m.get(k).remove(v));
            case 12 -> same(p, m -> flip ? m.get(k).removeAll(vs) : m.get(k).retainAll(vs));
            case 13 -> same(p, m -> removeIfReadingSize(m, k, v));
            case 14 -> same(p, m -> {
                m.get(k).clear();
                return "void";
            });
            case 15 -> same(p, m -> m.keySet().remove(k));
            case 16 -> same(p, m -> m.asMap().remove(k));
            case 17 -> same(p, m -> removeNth(m.asMap().entrySet().iterator(), n));
            case 18 -> same(p, m -> removeNth(m.values().iterator(), n));
            case 19 -> same(p, m -> removeNth(m.entries().iterator(), n));
            case 20 -> same(p, m -> removeNthMapping(m.mapIterator(), n));
            case 21 -> raw(p, k, v, r);
            default -> listStep(p, k, v, vs, n, op, r);
        }
    }

    private static void listStep(final Pair p, final String k, final String v, final List<String> vs, final int n,
            final int op, final Random r) {
        final int size = p.oracle.get(k).size();
        final int index = r.nextInt(size + 2);
        final int from = r.nextInt(size + 1);
        final int to = from + r.nextInt(size - from + 1);
        final int write = r.nextInt(9);
        final int cursorWrite = r.nextInt(3);
        switch (op) {
            case 22 -> sameList(p, m -> {
                m.get(k).add(index, v);
                return "void";
            });
            case 23 -> sameList(p, m -> m.get(k).addAll(index, vs));
            case 24 -> sameList(p, m -> m.get(k).remove(index));
            case 25 -> sameList(p, m -> cursor(m.get(k).listIterator(), n, cursorWrite, v));
            case 26 -> sameList(p, m -> m.get(k).reversed().remove(v));
            default -> {
                viewWrites++;
                sameList(p, m -> subListWrite(m, m.get(k).subList(from, to), write, v, vs));
            }
        }
    }

    /**
     * One write through a sub-list (directly, nested, reversed or through its cursor). The removeIf predicate reads the
     * map while the owned write is open; the read must recount and agree with the groups.
     */
    private static Object subListWrite(final MultiValuedMap<String, String> m, final List<String> view, final int write,
            final String v, final List<String> vs) {
        switch (write) {
            case 0 -> view.clear();
            case 1 -> {
                return view.add(v);
            }
            case 2 -> {
                return view.isEmpty() ? "empty" : view.remove(0);
            }
            case 3 -> {
                final List<Boolean> consistent = new ArrayList<>();
                final boolean changed = view.removeIf(x -> {
                    consistent.add(consistent(m));
                    return Objects.equals(x, v);
                });
                return Arrays.asList(changed, !consistent.contains(Boolean.FALSE));
            }
            case 4 -> {
                return view.addAll(vs);
            }
            case 5 -> {
                return view.retainAll(vs);
            }
            case 6 -> view.subList(0, view.size()).clear();
            case 7 -> {
                return view.isEmpty() ? "empty" : view.reversed().removeFirst();
            }
            default -> {
                return cursor(view.listIterator(), 0, 1, v);
            }
        }
        return "void";
    }

    /** Writes the directory handed out by getMap(); the oracle gets the same logical change. */
    //FLLDCIM: ALG - epoch guard: raw writes after getMap() must be seen by the next size query
    private static void raw(final Pair p, final String k, final String v, final Random r) {
        rawWrites++;
        final Map<String, ? extends Collection<String>> directory = ((M3AbstractMultiValuedMap<String, String>) p.owned).getMap();
        final Collection<String> group = directory.get(k);
        if (group == null || r.nextBoolean()) {
            directory.remove(k);
            p.oracle.remove(k);
        } else if (p.list && r.nextBoolean()) {
            group.clear();
            final List<String> oracleGroup = ((ListValuedMap<String, String>) p.oracle).get(k);
            oracleGroup.subList(0, oracleGroup.size()).clear();
        } else {
            equal(p.oracle.get(k).add(v), group.add(v), "raw add on " + k);
        }
    }

    private static Object removeIfReadingSize(final MultiValuedMap<String, String> m, final String k, final String v) {
        final List<Object> seen = new ArrayList<>();
        final boolean changed = m.get(k).removeIf(x -> {
            seen.add(Arrays.asList(m.size(), m.get(k).size(), consistent(m)));
            return Objects.equals(x, v);
        });
        seen.add(changed);
        return seen;
    }

    private static Object cursor(final ListIterator<String> it, final int steps, final int write, final String v) {
        for (int i = 0; i < steps && it.hasNext(); i++) {
            it.next();
        }
        switch (write) {
            case 0 -> it.add(v);
            case 1 -> {
                if (!it.hasNext()) {
                    return "end";
                }
                it.next();
                it.remove();
            }
            default -> {
                if (!it.hasNext()) {
                    return "end";
                }
                it.next();
                it.set(v);
            }
        }
        return it.nextIndex();
    }

    private static Object removeNth(final Iterator<?> it, final int n) {
        for (int i = 0; i <= n; i++) {
            if (!it.hasNext()) {
                return "end";
            }
            it.next();
        }
        it.remove();
        return "removed";
    }

    private static Object removeNthMapping(final MapIterator<String, String> it, final int n) {
        for (int i = 0; i <= n; i++) {
            if (!it.hasNext()) {
                return "end";
            }
            it.next();
        }
        it.remove();
        return "removed";
    }

    private static Object clear(final MultiValuedMap<String, String> m) {
        m.clear();
        return "cleared";
    }

    private static List<String> values(final Random r) {
        final List<String> values = new ArrayList<>();
        final int count = r.nextInt(4);
        for (int i = 0; i < count; i++) {
            values.add(VALUES[r.nextInt(VALUES.length)]);
        }
        return values;
    }

    private static void same(final Pair p, final Function<MultiValuedMap<String, String>, Object> op) {
        final Object expected = outcome(op, p.oracle);
        equal(expected, outcome(op, p.owned), "operation outcome");
    }

    private static void sameList(final Pair p, final Function<ListValuedMap<String, String>, Object> op) {
        final Object expected = outcome(op, (ListValuedMap<String, String>) p.oracle);
        equal(expected, outcome(op, (ListValuedMap<String, String>) p.owned), "list operation outcome");
    }

    private static <M> Object outcome(final Function<M, Object> op, final M map) {
        try {
            return normalize(op.apply(map));
        } catch (RuntimeException failure) {
            return failure.getClass().getName();
        }
    }

    private static Object normalize(final Object value) {
        return value instanceof Collection<?> c ? new ArrayList<>(c) : value;
    }

    /** Counters against the oracle's logical values and against a recount through get(key); groups value by value. */
    private static void verify(final Pair p, final String where) {
        int pairs = 0;
        int groups = 0;
        int logicalOracleGroups = 0;
        for (final String key : KEYS) {
            final Collection<String> owned = p.owned.get(key);
            final Collection<String> oracle = p.oracle.get(key);
            equal(new ArrayList<>(oracle), new ArrayList<>(owned), where + " group " + key);
            pairs += owned.size();
            groups += owned.isEmpty() ? 0 : 1;
            logicalOracleGroups += oracle.isEmpty() ? 0 : 1;
        }
        final int size = p.owned.size();
        equal(p.oracle.size(), size, where + " size against the oracle");
        equal(pairs, size, where + " size against the recount");
        equal(pairs == 0, p.owned.isEmpty(), where + " isEmpty against the recount");
        equal(p.oracle.size() == 0, p.owned.isEmpty(), where + " isEmpty against the oracle");
        equal(groups, p.owned.keySet().size(), where + " keySet size against the recount");
        equal(logicalOracleGroups, p.owned.keySet().size(), where + " keySet size against the oracle");
        equal(groups, p.owned.keys().uniqueSet().size(), where + " unique keys against the recount");
        equal(groups, p.owned.asMap().size(), where + " asMap size against the recount");
    }

    /** A read made from a callback: size() must equal the sum of the group sizes at that moment. */
    private static boolean consistent(final MultiValuedMap<String, String> m) {
        callbackReads++;
        return m.size() == Arrays.stream(KEYS).mapToInt(key -> m.get(key).size()).sum();
    }

    private static boolean hasEmptyOwner(final MultiValuedMap<String, String> oracle) {
        return oracle.keySet().stream().anyMatch(key -> oracle.get(key).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static <T> T copy(final T value) throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    private static void equal(final Object expected, final Object actual, final String where) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(where + " expected=" + expected + " actual=" + actual);
        }
    }
}
