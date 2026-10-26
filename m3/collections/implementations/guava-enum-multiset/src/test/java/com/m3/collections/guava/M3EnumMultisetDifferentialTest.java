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
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Seeded differential run against the real Guava 33.4.8-jre EnumMultiset (test-scope oracle): return values,
 * exception classes and the full resulting state are compared after every operation. Both iterate in ordinal
 * order and neither is fail-fast, so traversals are compared exactly, including traversals interleaved with
 * external modification.
 */
final class M3EnumMultisetDifferentialTest {
    static final AtomicLong CHECKS = new AtomicLong();
    static final AtomicLong OPERATIONS = new AtomicLong();
    private static final Logger LOG = Logger.getLogger(M3EnumMultisetDifferentialTest.class.getName());
    private static final int OPS_PER_SEED = 12_000;

    private final Multiset<Code> oracle = EnumMultiset.create(Code.class);
    private final M3EnumMultiset<Code> m3 = M3EnumMultiset.create(Code.class);

    @AfterAll
    static void report() {
        LOG.info(() -> "M3EnumMultiset differential: operations=" + OPERATIONS.get() + " checks=" + CHECKS.get());
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 7L, 42L, 1_001L, 20_261_019L, 0xBADCAFEL, -3L, 987_654_321L})
    void seededOperationsMatchGuava(long seed) {
        Random random = new Random(seed);
        for (int step = 0; step < OPS_PER_SEED; step++) {
            int op = random.nextInt(38);
            long opSeed = random.nextLong();
            same("op" + op + "@" + step, () -> apply(oracle, op, opSeed), () -> apply(m3, op, opSeed));
            OPERATIONS.incrementAndGet();
            compareState(step);
            if (oracle.size() > 400) { same("trim", () -> oracle.setCount(Code.at(3), 0), () -> m3.setCount(Code.at(3), 0)); }
        }
    }

    /** Mostly the first 12 constants, sometimes the second mask word (ordinals 60..69). */
    private static Code code(Random r) {
        return Code.at(r.nextInt(6) == 0 ? 60 + r.nextInt(10) : r.nextInt(12));
    }

    /** An argument for Object-typed queries: a constant, null, a foreign enum constant or a non-enum. */
    private static Object any(Random r) {
        return switch (r.nextInt(14)) {
            case 0 -> null;
            case 1 -> Other.X;
            case 2 -> "C01";
            default -> code(r);
        };
    }

    private static Object apply(Multiset<Code> m, int op, long opSeed) throws Exception {
        Random r = new Random(opSeed);
        Code e = r.nextInt(25) == 0 ? null : code(r);
        Object o = any(r);
        int n = r.nextInt(8) - 1;
        int n2 = r.nextInt(8) - 1;
        List<Code> list = new ArrayList<>();
        for (int i = r.nextInt(5); i > 0; i--) { list.add(r.nextInt(20) == 0 ? null : code(r)); }
        List<Code> clean = new ArrayList<>(list);
        clean.removeIf(x -> x == null);
        Code pick = code(r);
        Predicate<Code> odd = x -> (x.ordinal() & 1) == 1;
        switch (op) {
            case 0, 1, 2: return m.add(e);
            case 3: return m.add(e, n);
            case 4: return m.remove(o);
            case 5: return m.remove(o, n);
            case 6: return m.setCount(e, n);
            case 7: return m.setCount(e, n, n2);
            case 8: return List.of(m.count(o), m.contains(o), m.size(), m.isEmpty());
            case 9: return m.addAll(list);
            case 10: return clean.isEmpty() ? m.addAll(clean) : m.addAll(EnumMultiset.create(clean));
            case 11: return m.removeAll(list) | m.removeAll(HashMultiset.create(Arrays.asList(pick, Code.at(Math.min(69, pick.ordinal() + 1)))));
            case 12: return m.retainAll(new HashSet<>(Arrays.asList(Code.at(0), Code.at(2), Code.at(3), Code.at(5), Code.at(7), Code.at(61), Code.at(64), null)));
            case 13: return m.elementSet().remove(o);
            case 14: return m.elementSet().removeAll(list.size() > 2 ? new HashSet<>(Arrays.asList(Code.values()).subList(0, 20)) : new HashSet<>(list));
            case 15: return m.elementSet().retainAll(Arrays.asList(Code.at(1), Code.at(2), Code.at(4), Code.at(6), Code.at(8), Code.at(9), Code.at(63), Code.at(65)));
            case 16: return m.entrySet().remove(Multisets.immutableEntry(o, Math.max(0, n)));
            case 17: return m.entrySet().contains(Multisets.immutableEntry(o, Math.max(0, n)));
            case 18: return removeThrough(m.iterator(), x -> x == pick);
            case 19: return removeThrough(m.elementSet().iterator(), x -> x.ordinal() == pick.ordinal() + 1);
            case 20: return removeThrough(m.entrySet().iterator(), x -> x.getCount() > 4);
            case 21: return m.removeIf(odd.and(x -> x == pick || x.ordinal() == pick.ordinal() + 2));
            case 22: if (r.nextInt(30) == 0) { m.clear(); } return null;
            case 23: {
                List<String> seen = new ArrayList<>();
                m.forEachEntry((x, c) -> seen.add(x + ":" + c));
                return seen;
            }
            case 24: {
                List<Code> seen = new ArrayList<>();
                m.forEach(seen::add);
                return seen;
            }
            case 25: return List.of(m.stream().count(), m.stream().collect(Collectors.toList()), m.parallelStream().map(Code::ordinal).reduce(0, Integer::sum));
            case 26: return List.of(Arrays.asList(m.toArray()), Arrays.asList(m.toArray(new Code[0])), m.toArray(new Code[m.size() + 2]).length);
            case 27: return List.of(m.containsAll(clean), m.elementSet().containsAll(clean));
            case 28: return List.of(m.elementSet().contains(o), m.elementSet().size(), m.elementSet().isEmpty(), m.entrySet().size(), m.entrySet().isEmpty());
            case 29: return List.of(m.entrySet().hashCode(), m.elementSet().hashCode(), m.hashCode(), m.entrySet().toString(), m.elementSet().toString());
            case 30: return m.equals(HashMultiset.create(m)) && HashMultiset.create(m).equals(m) && !m.equals(clean);
            case 31: return m.count(pick) > 0 ? m.add(pick, Integer.MAX_VALUE) : m.setCount(pick, 2);
            case 32: return walkWithExternalChanges(m, r);
            case 33: return liveEntries(m, pick);
            case 34: {
                // Split points are implementation-defined (Guava splits its entry spliterator, the owned one does
                // not), so only the unsplit characteristics, the size estimate and the concatenated walk are compared.
                Spliterator<Code> s = m.spliterator();
                List<Object> out = new ArrayList<>(List.of(s.characteristics(), s.estimateSize()));
                if (r.nextBoolean()) {
                    s.tryAdvance(out::add);
                    out.add(s.estimateSize());
                }
                Spliterator<Code> prefix = s.trySplit();
                if (prefix != null) { prefix.forEachRemaining(out::add); }
                s.forEachRemaining(out::add);
                return out;
            }
            case 35: return m.remove(o, Integer.MAX_VALUE);
            case 36: return m.setCount(e, n, n2) + "/" + m.entrySet().remove(Multisets.immutableEntry(e, m.count(e)));
            case 37: return removeThrough(m.iterator(), odd.and(x -> x.ordinal() > 8));
            default: throw new AssertionError(op);
        }
    }

    /** Neither implementation is fail-fast: a traversal that sees external adds and removals is compared exactly. */
    private static Object walkWithExternalChanges(Multiset<Code> m, Random r) {
        List<Object> out = new ArrayList<>();
        Iterator<?> it = switch (r.nextInt(3)) {
            case 0 -> m.iterator();
            case 1 -> m.elementSet().iterator();
            default -> m.entrySet().iterator();
        };
        for (int step = 0; step < 6; step++) {
            try {
                if (!it.hasNext()) { out.add("end"); }
                Object x = it.next();
                out.add(String.valueOf(x));
                switch (r.nextInt(5)) {
                    case 0 -> m.add(code(r), 1 + r.nextInt(2));
                    case 1 -> m.setCount(code(r), 0);
                    case 2 -> {
                        it.remove();
                        out.add("removed");
                    }
                    case 3 -> m.remove(code(r));
                    default -> out.add(m.size());
                }
            } catch (RuntimeException ex) {
                out.add(ex.getClass().getSimpleName());
            }
        }
        return out;
    }

    /** Entries read their counts live and keep identity semantics after removal. */
    private static Object liveEntries(Multiset<Code> m, Code pick) {
        List<Object> out = new ArrayList<>();
        for (Multiset.Entry<Code> entry : new ArrayList<>(m.entrySet())) {
            if (entry.getElement() == pick) { m.add(pick, 2); }
            out.add(entry + "|" + entry.getCount() + "|" + entry.hashCode() + "|" + entry.equals(Multisets.immutableEntry(entry.getElement(), entry.getCount())));
        }
        return out;
    }

    private static <T> boolean removeThrough(Iterator<T> it, Predicate<T> remove) {
        boolean changed = false;
        while (it.hasNext()) {
            if (remove.test(it.next())) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    static Object outcome(Callable<?> action) {
        try {
            Object value = action.call();
            return value instanceof Multiset<?> ms ? new ArrayList<>(ms) : value;
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getName();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void same(String what, Callable<?> guava, Callable<?> owned) {
        assertEquals(outcome(guava), outcome(owned), what);
        CHECKS.incrementAndGet();
    }

    private void compareState(int step) {
        m3.verifyInvariants();
        assertEquals(oracle.size(), m3.size(), "size@" + step);
        assertTrue(m3.equals(oracle), "m3.equals(guava)@" + step);
        assertTrue(oracle.equals(m3), "guava.equals(m3)@" + step);
        assertEquals(oracle.hashCode(), m3.hashCode(), "hashCode@" + step);
        assertEquals(oracle.toString(), m3.toString(), "toString@" + step);
        assertEquals(new ArrayList<>(oracle), new ArrayList<>(m3), "iteration@" + step);
        assertEquals(new ArrayList<>(oracle.elementSet()), new ArrayList<>(m3.elementSet()), "elementSet@" + step);
        assertEquals(oracle.entrySet(), m3.entrySet(), "entrySet@" + step);
        assertEquals(m3.entrySet(), oracle.entrySet(), "entrySet reverse@" + step);
        CHECKS.addAndGet(9);
    }
}
