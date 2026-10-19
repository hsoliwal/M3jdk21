/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Focused contract cases, each run against the Guava provider and the owned implementation. */
final class M3ArrayListMultimapContractTest {
    @FunctionalInterface
    interface Scenario {
        Object run(ListMultimap<Integer, Integer> m) throws Exception;
    }

    private static ListMultimap<Integer, Integer> guava() {
        ListMultimap<Integer, Integer> m = ArrayListMultimap.create();
        m.putAll(1, List.of(10, 11, 12));
        m.putAll(2, List.of(20));
        m.putAll(3, List.of(30, 31));
        return m;
    }

    private static ListMultimap<Integer, Integer> owned() {
        return M3ArrayListMultimap.create(guava());
    }

    private static Object outcome(Scenario s, ListMultimap<Integer, Integer> m) {
        try {
            return M3ArrayListMultimapDifferentialTest.normalize(s.run(m));
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getSimpleName();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /** Runs one scenario on both owners and compares the result and final state. */
    private static void both(String name, Scenario s) {
        ListMultimap<Integer, Integer> g = guava();
        ListMultimap<Integer, Integer> o = owned();
        assertEquals(outcome(s, g), outcome(s, o), name);
        assertEquals(g, o, name + " state");
        ((M3ArrayListMultimap<Integer, Integer>) o).verifyInvariants();
        M3ArrayListMultimapDifferentialTest.CHECKS.addAndGet(2);
    }

    @Test
    void factoriesValidateLikeGuava() {
        IllegalArgumentException g = assertThrows(IllegalArgumentException.class, () -> ArrayListMultimap.create(-1, 2));
        IllegalArgumentException o = assertThrows(IllegalArgumentException.class, () -> M3ArrayListMultimap.create(-1, 2));
        assertEquals(g.getMessage(), o.getMessage());
        g = assertThrows(IllegalArgumentException.class, () -> ArrayListMultimap.create(2, -1));
        o = assertThrows(IllegalArgumentException.class, () -> M3ArrayListMultimap.create(2, -1));
        assertEquals(g.getMessage(), o.getMessage());
        assertThrows(NullPointerException.class, () -> M3ArrayListMultimap.create(null));
        M3ArrayListMultimap<Integer, Integer> zero = M3ArrayListMultimap.create(0, 0);
        zero.put(1, 1);
        zero.put(1, 2);
        assertEquals(List.of(1, 2), zero.get(1));
        M3ArrayListMultimap<Integer, Integer> sized = M3ArrayListMultimap.create(40, 5);
        for (int i = 0; i < 200; i++) { sized.put(i % 40, i); }
        sized.verifyInvariants();
        assertEquals(200, sized.size());
        assertEquals(guava(), M3ArrayListMultimap.create(guava()));
        assertEquals(guava(), M3ArrayListMultimap.create(M3ArrayListMultimap.create(guava())));
    }

    @Test
    void nullKeysAndValuesAndTextForms() {
        both("nulls", m -> {
            m.put(null, null);
            m.put(null, 5);
            m.put(7, null);
            return List.of(m.containsKey(null), m.containsValue(null), m.containsEntry(null, null), m.get(null).toString(), m.size());
        });
        M3ArrayListMultimap<String, String> o = M3ArrayListMultimap.create();
        ListMultimap<String, String> g = ArrayListMultimap.create();
        for (Multimap<String, String> m : List.of(o, g)) {
            m.put("a", "x");
            m.put("a", "y");
        }
        assertEquals(g.toString(), o.toString());
        assertEquals(g.asMap().toString(), o.asMap().toString());
        assertEquals(g.keys().toString(), o.keys().toString());
        assertEquals(g.entries().toString(), o.entries().toString());
        assertEquals(g.get("a").toString(), o.get("a").toString());
        o.put("b", "z");
        g.put("b", "z");
        assertEquals(g.keys().entrySet().stream().map(Object::toString).sorted().toList(),
                o.keys().entrySet().stream().map(Object::toString).sorted().toList());
        assertEquals("{}", M3ArrayListMultimap.create().toString());
        assertEquals("[]", M3ArrayListMultimap.create().keys().toString());
    }

    @Test
    void returnedCollectionsAreDetachedOrLive() {
        both("removeAll detached", m -> {
            List<Integer> removed = m.removeAll(1);
            m.put(1, 99);
            return List.of(removed, removed.getClass().getSimpleName().contains("Unmodifiable"));
        });
        both("removeAll unmodifiable", m -> m.removeAll(1).add(5));
        both("removeAll absent", m -> m.removeAll(77).isEmpty());
        both("replaceValues old", m -> m.replaceValues(1, List.of(7, 8)));
        both("replaceValues absent", m -> m.replaceValues(9, List.of(7)));
        both("replaceValues empty", m -> m.replaceValues(1, List.of()));
        both("replaceValues null", m -> m.replaceValues(1, null));
        both("asMap remove copy", m -> {
            Collection<Integer> c = m.asMap().remove(1);
            c.add(4);
            return List.of(c, m.containsKey(1));
        });
        both("live view adds key", m -> {
            List<Integer> view = m.get(5);
            view.add(1);
            view.add(0, 2);
            return List.of(m.get(5), m.containsKey(5), view.size());
        });
        both("view survives removal", m -> {
            List<Integer> view = m.get(2);
            view.remove(Integer.valueOf(20));
            boolean gone = m.containsKey(2);
            view.add(21);
            return List.of(gone, m.get(2));
        });
        both("asMap get live", m -> {
            Collection<Integer> c = m.asMap().get(3);
            c.add(32);
            return List.of(m.get(3), m.asMap().get(99) == null);
        });
        both("asMap put", m -> m.asMap().put(4, List.of(1)));
        both("asMap entry setValue", m -> m.asMap().entrySet().iterator().next().setValue(List.of()));
        both("entries setValue", m -> m.entries().iterator().next().setValue(1));
        both("keys add", m -> m.keys().add(1));
        both("keys add n", m -> m.keys().add(1, 2));
        both("keys addAll empty", m -> m.keys().addAll(List.of()));
        both("keys addAll", m -> m.keys().addAll(List.of(1)));
        both("keys setCount up", m -> m.keys().setCount(1, 5));
        both("keys setCount down", m -> m.keys().setCount(1, 1));
        both("keys setCount cas", m -> List.of(m.keys().setCount(1, 3, 1), m.keys().setCount(1, 3, 0), m.keys().setCount(3, 2, 0)));
        both("keys negative", m -> m.keys().remove(1, -1));
        both("keys setCount negative", m -> m.keys().setCount(1, -1));
        both("keys remove zero", m -> m.keys().remove(1, 0));
        both("keys remove one", m -> m.keys().remove((Object) 1));
        both("keys entry contains", m -> List.of(m.keys().entrySet().contains(Multisets.immutableEntry(1, 3)),
                m.keys().entrySet().contains(Multisets.immutableEntry(1, 2)), m.keys().entrySet().contains("x")));
        both("keys entry remove", m -> List.of(m.keys().entrySet().remove(Multisets.immutableEntry(1, 2)),
                m.keys().entrySet().remove(Multisets.immutableEntry(1, 3)), m.keys().entrySet().remove("x")));
        both("keys entries live", m -> {
            Multiset.Entry<Integer> e = m.keys().entrySet().stream().filter(x -> x.getElement() == 1).findFirst().orElseThrow();
            m.put(1, 13);
            return List.of(e.getCount(), e.toString(), e.equals(Multisets.immutableEntry(1, 4)), e.hashCode() == Multisets.immutableEntry(1, 4).hashCode());
        });
        both("keys equality", m -> List.of(m.keys().equals(HashMultiset.create(List.of(1, 1, 1, 2, 3, 3))),
                m.keys().equals(HashMultiset.create(List.of(1, 2, 3))), m.keys().equals(List.of(1)), m.keys().equals(m.keys()),
                m.keys().equals(HashMultiset.create(List.of(1, 1, 2, 2, 3, 3)))));
        both("keys elementSet", m -> List.of(m.keys().elementSet().size(), m.keys().isEmpty(), m.keys().entrySet().isEmpty()));
        both("keys forEachEntry", m -> {
            List<String> seen = new ArrayList<>();
            m.keys().forEachEntry((k, n) -> seen.add(k + ":" + n));
            return HashMultiset.create(seen);
        });
        both("keys forEach", m -> {
            List<Integer> seen = new ArrayList<>();
            m.keys().forEach(seen::add);
            return HashMultiset.create(seen);
        });
        both("keys stream", m -> m.keys().stream().collect(Collectors.toList()));
        both("keys clear", m -> {
            m.keys().clear();
            return m.isEmpty();
        });
        both("keys entrySet clear", m -> {
            m.keys().entrySet().clear();
            return m.isEmpty();
        });
    }

    @Test
    void listViewContract() {
        both("random access", m -> m.get(1) instanceof RandomAccess);
        both("get by index", m -> List.of(m.get(1).get(0), m.get(1).get(2), m.get(1).subList(1, 3).get(1)));
        both("get out of range", m -> m.get(1).get(3));
        both("get negative", m -> m.get(1).get(-1));
        both("set out of range", m -> m.get(1).set(3, 0));
        both("remove index out of range", m -> m.get(2).remove(1));
        both("add index out of range", m -> {
            m.get(2).add(3, 0);
            return null;
        });
        both("addAll index", m -> List.of(m.get(1).addAll(1, List.of(5, 6)), m.get(1).addAll(List.of()), m.get(9).addAll(List.of(1, 2))));
        both("addAll self", m -> List.of(m.get(1).addAll(m.get(1)), m.get(1)));
        both("addAll bad index", m -> m.get(1).addAll(9, List.of(1)));
        both("listIterator bad index", m -> m.get(1).listIterator(9));
        both("listIterator negative", m -> m.get(1).listIterator(-1));
        both("sublist bounds", m -> m.get(1).subList(-1, 1));
        both("sublist to", m -> m.get(1).subList(0, 4));
        both("sublist order", m -> m.get(1).subList(2, 1));
        both("nested sublist", m -> {
            List<Integer> outer = m.get(1).subList(0, 3);
            List<Integer> inner = outer.subList(1, 3);
            inner.remove(0);
            inner.add(99);
            inner.add(0, 98);
            return List.of(outer.size(), inner.size(), m.get(1), outer.indexOf(99), outer.lastIndexOf(99), outer.contains(98));
        });
        both("sublist clears key then reuses", m -> {
            List<Integer> sub = m.get(2).subList(0, 1);
            sub.clear();
            boolean gone = !m.containsKey(2);
            sub.add(5);
            return List.of(gone, m.get(2), sub);
        });
        both("sublist stale", m -> {
            List<Integer> sub = m.get(1).subList(0, 2);
            m.put(1, 13);
            return sub.size();
        });
        both("sublist sibling stale", m -> {
            List<Integer> view = m.get(1);
            List<Integer> a = view.subList(0, 1);
            List<Integer> b = view.subList(1, 2);
            a.add(5);
            return b.get(0);
        });
        both("sublist equals hash", m -> {
            List<Integer> sub = m.get(1).subList(1, 3);
            return List.of(sub.equals(List.of(11, 12)), sub.hashCode(), sub.equals(List.of()), sub.equals("x"), sub.isEmpty());
        });
        both("empty view equality", m -> List.of(m.get(8).equals(List.of()), m.get(8).equals(List.of(1)), m.get(8).hashCode(),
                m.get(8).equals("x"), m.get(8).indexOf(1), m.get(8).lastIndexOf(1), m.get(8).remove((Object) 1)));
        both("view equals itself", m -> {
            List<Integer> v = m.get(1);
            return List.of(v.equals(v), v.equals(new ArrayList<>(v)), v.equals(List.of(10, 11)), v.equals(List.of(10, 11, 13)));
        });
        both("removeIf none", m -> List.of(m.get(1).removeIf(x -> x > 100), m.get(8).removeIf(x -> true)));
        both("bulk remove all", m -> List.of(m.get(2).removeAll(List.of(20)), m.containsKey(2)));
        both("bulk retain none", m -> List.of(m.get(3).retainAll(List.of(30, 31)), m.get(3).retainAll(List.of())));
        both("bulk null arg", m -> m.get(1).removeAll(null));
        both("retain null arg", m -> m.get(1).retainAll(null));
        both("removeIf null", m -> m.get(1).removeIf(null));
        both("replaceAll null", m -> {
            m.get(1).replaceAll(null);
            return null;
        });
        both("replaceAll empty", m -> {
            m.get(8).replaceAll(x -> x);
            return null;
        });
        both("sort empty and natural", m -> {
            m.get(8).sort(null);
            m.put(4, 3);
            m.put(4, 1);
            m.put(4, 2);
            m.get(4).sort(null);
            m.get(4).subList(0, 2).sort((x, y) -> y - x);
            return m.get(4);
        });
        both("clear empty", m -> {
            m.get(8).clear();
            return m.containsKey(8);
        });
        both("toArray", m -> List.of(Arrays.asList(m.get(1).toArray()), Arrays.asList(m.get(1).toArray(new Integer[0])),
                Arrays.asList(m.get(1).toArray(new Integer[5]))));
        both("spliterator", m -> List.of(m.get(1).spliterator().estimateSize(),
                m.get(1).spliterator().hasCharacteristics(Spliterator.ORDERED), m.get(1).stream().map(x -> x * 2).toList()));
        both("iterator exhausted", m -> {
            Iterator<Integer> it = m.get(2).iterator();
            it.next();
            return it.next();
        });
        both("iterator remove twice", m -> {
            Iterator<Integer> it = m.get(1).iterator();
            it.next();
            it.remove();
            it.remove();
            return null;
        });
        both("listIterator previous at start", m -> m.get(1).listIterator().previous());
        both("listIterator set without next", m -> {
            m.get(1).listIterator().set(5);
            return null;
        });
        both("listIterator set after add", m -> {
            ListIterator<Integer> it = m.get(1).listIterator();
            it.next();
            it.add(5);
            it.set(6);
            return null;
        });
        both("listIterator full", m -> {
            ListIterator<Integer> it = m.get(1).listIterator(1);
            List<Object> trace = new ArrayList<>();
            trace.add(it.previous());
            trace.add(it.nextIndex());
            it.set(100);
            it.add(101);
            trace.add(it.previousIndex());
            trace.add(it.next());
            it.remove();
            trace.add(it.hasNext());
            trace.add(it.hasPrevious());
            trace.add(m.get(1));
            return trace;
        });
    }

    @Test
    void failFastMatchesGuava() {
        both("same key iterator", m -> {
            Iterator<Integer> it = m.get(1).iterator();
            it.next();
            m.put(1, 13);
            return it.next();
        });
        both("other key iterator", m -> {
            Iterator<Integer> it = m.get(1).iterator();
            it.next();
            m.put(2, 21);
            return it.next();
        });
        both("absent key iterator then put", m -> {
            Iterator<Integer> it = m.get(5).iterator();
            m.put(5, 50);
            return it.hasNext();
        });
        both("recreated key iterator", m -> {
            Iterator<Integer> it = m.get(2).iterator();
            m.removeAll(2);
            m.put(2, 22);
            return it.hasNext();
        });
        both("removed key iterator", m -> {
            Iterator<Integer> it = m.get(1).iterator();
            it.next();
            m.removeAll(1);
            return List.of(it.hasNext());
        });
        both("removed key iterator next", m -> {
            Iterator<Integer> it = m.get(1).iterator();
            it.next();
            m.removeAll(1);
            return it.next();
        });
        both("set does not invalidate", m -> {
            Iterator<Integer> it = m.get(1).iterator();
            it.next();
            m.get(1).set(0, 5);
            return it.next();
        });
        both("values new key", m -> {
            Iterator<Integer> it = m.values().iterator();
            it.next();
            m.put(9, 90);
            List<Integer> rest = new ArrayList<>();
            while (it.hasNext()) { rest.add(it.next()); }
            return rest.size();
        });
        both("keySet removal", m -> {
            Iterator<Integer> it = m.keySet().iterator();
            it.next();
            m.removeAll(m.keySet().stream().skip(2).findFirst().orElseThrow());
            return it.next();
        });
        both("asMap entries removal", m -> {
            Iterator<Map.Entry<Integer, Collection<Integer>>> it = m.asMap().entrySet().iterator();
            it.next();
            m.put(9, 9);
            return it.next().getKey();
        });
        both("keySet iterator remove without next", m -> {
            m.keySet().iterator().remove();
            return null;
        });
        both("entries iterator remove without next", m -> {
            m.entries().iterator().remove();
            return null;
        });
        both("keySet iterator stale remove", m -> {
            Iterator<Integer> it = m.keySet().iterator();
            it.next();
            m.put(9, 9);
            it.remove();
            return null;
        });
        both("entries iterator stale remove", m -> {
            Iterator<Map.Entry<Integer, Integer>> it = m.entries().iterator();
            Map.Entry<Integer, Integer> e = it.next();
            m.get(e.getKey()).add(0, 7);
            it.remove();
            return null;
        });
        both("keySet exhausted", m -> {
            Iterator<Integer> it = m.keySet().iterator();
            while (it.hasNext()) { it.next(); }
            return it.next();
        });
        both("values exhausted", m -> {
            Iterator<Integer> it = m.values().iterator();
            while (it.hasNext()) { it.next(); }
            return it.next();
        });
        both("forEach structural", m -> {
            m.forEach((k, v) -> {
                if (v == 10) { m.put(4, 40); }
            });
            return null;
        });
        both("forEach same key", m -> {
            m.forEach((k, v) -> {
                if (v == 10) { m.put(1, 14); }
            });
            return null;
        });
        both("values forEach", m -> {
            List<Integer> seen = new ArrayList<>();
            m.values().forEach(seen::add);
            return HashMultiset.create(seen);
        });
        both("forEach null", m -> {
            m.forEach((BiConsumer<Integer, Integer>) null);
            return null;
        });
        both("values forEach null", m -> {
            m.values().forEach((Consumer<Integer>) null);
            return null;
        });
        both("keys forEach null", m -> {
            m.keys().forEach((Consumer<Integer>) null);
            return null;
        });
        both("keys forEachEntry null", m -> {
            m.keys().forEachEntry(null);
            return null;
        });
        both("keys forEachEntry structural", m -> {
            m.keys().forEachEntry((k, n) -> m.put(9, 9));
            return null;
        });
        both("replaceAll structural", m -> {
            m.get(1).replaceAll(x -> {
                m.put(1, 1);
                return x;
            });
            return null;
        });
        both("removeIf structural", m -> m.get(3).removeIf(x -> {
            m.put(3, 3);
            return false;
        }));
        both("sort structural", m -> {
            m.get(1).sort((x, y) -> {
                m.put(1, 5);
                return Integer.compare(x, y);
            });
            return null;
        });
        both("putAll self", m -> m.putAll(m));
        both("putAll empty other", m -> m.putAll(ArrayListMultimap.create()));
        both("putAll null", m -> m.putAll(null));
        both("putAll key null iterable", m -> m.putAll(1, null));
        both("putAll empty iterable", m -> m.putAll(1, (Iterable<Integer>) List.<Integer>of()::iterator));
        both("putAll empty collection", m -> m.putAll(6, List.of()));
    }

    @Test
    void viewsAndEquality() {
        both("keySet ops", m -> List.of(m.keySet().contains(1), m.keySet().remove(1), m.keySet().remove(1), m.keySet().size()));
        both("keySet clear", m -> {
            m.keySet().clear();
            return m.size();
        });
        both("values clear", m -> {
            m.values().clear();
            return m.size();
        });
        both("entries clear", m -> {
            m.entries().clear();
            return m.size();
        });
        both("asMap clear", m -> {
            m.asMap().clear();
            return m.size();
        });
        both("asMap entries clear", m -> {
            m.asMap().entrySet().clear();
            return m.size();
        });
        both("entries contains wrong type", m -> List.of(m.entries().contains("x"), m.entries().remove("x")));
        both("asMap entries", m -> List.of(m.asMap().entrySet().contains(new AbstractMap.SimpleImmutableEntry<>(1, List.of(10, 11, 12))),
                m.asMap().entrySet().contains("x"), m.asMap().entrySet().remove("x"), m.asMap().entrySet().size(),
                m.asMap().keySet().size(), m.asMap().values().size(), m.asMap().containsValue(List.of(20)), m.asMap().isEmpty()));
        both("asMap hash and string", m -> List.of(m.asMap().hashCode(), m.asMap().equals(guava().asMap())));
        both("streams", m -> List.of(HashMultiset.create(m.values().stream().toList()), HashMultiset.create(m.entries().stream().toList()),
                HashMultiset.create(m.keySet().stream().toList()), HashMultiset.create(m.asMap().entrySet().stream().map(Map.Entry::getKey).toList()),
                m.values().spliterator().estimateSize(), m.entries().spliterator().estimateSize(), m.keySet().spliterator().estimateSize(),
                m.asMap().entrySet().spliterator().estimateSize(), m.keys().spliterator().estimateSize()));
        both("trimToSize", m -> {
            trim(m);
            m.put(1, 1);
            return m.get(1);
        });
        ListMultimap<Integer, Integer> o = owned();
        assertTrue(o.equals(o));
        assertFalse(o.equals("x"));
        assertFalse(o.equals(HashMultimap.create(guava())));
        assertNotEquals(o, M3ArrayListMultimap.create());
        assertFalse(o.equals(new BrokenMultimap()));
        ListMultimap<Integer, Integer> bigger = guava();
        bigger.put(1, 1);
        assertNotEquals(o, bigger);
        ListMultimap<Integer, Integer> other = guava();
        other.removeAll(3);
        other.put(4, 30);
        other.put(4, 31);
        assertNotEquals(o, other);
        assertSame(o.keySet(), o.keySet());
        assertSame(o.values(), o.values());
        assertSame(o.entries(), o.entries());
        assertSame(o.keys(), o.keys());
        assertSame(o.asMap(), o.asMap());
        assertSame(o.asMap().entrySet(), o.asMap().entrySet());
        assertSame(o.keys().entrySet(), o.keys().entrySet());
        assertNull(o.asMap().remove(42));
        assertEquals(o.toString(), o.asMap().toString());
        assertThrows(NoSuchElementException.class, () -> M3ArrayListMultimap.create().keys().entrySet().iterator().next());
    }

    @SuppressWarnings("deprecation")
    private static void trim(ListMultimap<Integer, Integer> m) {
        if (m instanceof M3ArrayListMultimap<Integer, Integer> o) { o.trimToSize(); } else { ((ArrayListMultimap<Integer, Integer>) m).trimToSize(); }
    }

    @Test
    void serializationRoundTrip() throws Exception {
        M3ArrayListMultimap<Integer, Integer> o = M3ArrayListMultimap.create(guava());
        o.put(null, null);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(o); }
        Object back;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) { back = in.readObject(); }
        assertEquals(o, back);
        assertEquals(o.toString(), back.toString());
        @SuppressWarnings("unchecked") M3ArrayListMultimap<Integer, Integer> copy = (M3ArrayListMultimap<Integer, Integer>) back;
        copy.verifyInvariants();
        copy.put(1, 2);
        assertEquals(4, copy.get(1).size());
        byte[] corrupt = serializedCounts(-1, 0);
        assertThrows(InvalidObjectException.class, () -> read(corrupt));
        assertEquals(one(), read(serializedCounts(1, 1)));
        byte[] corruptValues = serializedCounts(1, 0);
        assertThrows(InvalidObjectException.class, () -> read(corruptValues));
    }

    /**
     * Rewrites the two counts of the owned stream format (key count, then the first key's value count),
     * each written as a 4-byte TC_BLOCKDATA payload {@code 77 04 00 00 00 01}.
     */
    private static byte[] serializedCounts(int keys, int values) throws Exception {
        M3ArrayListMultimap<Integer, Integer> one = M3ArrayListMultimap.create();
        one.put(1, 2);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(one); }
        byte[] data = bytes.toByteArray();
        List<Integer> at = new ArrayList<>();
        for (int i = 0; i + 6 <= data.length; i++) {
            if (data[i] == 0x77 && data[i + 1] == 4 && java.nio.ByteBuffer.wrap(data, i + 2, 4).getInt() == 1) { at.add(i + 2); }
        }
        assertEquals(2, at.size(), "owned stream layout");
        java.nio.ByteBuffer.wrap(data, at.get(0), 4).putInt(keys);
        java.nio.ByteBuffer.wrap(data, at.get(1), 4).putInt(values);
        return data;
    }

    private static Object read(byte[] data) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) { return in.readObject(); }
    }

    @Test
    void steadyStateHotPathsAllocateNothing() {
        java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
        assumeTrue(base instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) base;
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        M3ArrayListMultimap<Integer, Integer> m = M3ArrayListMultimap.create();
        Integer[] boxes = new Integer[64];
        for (int i = 0; i < boxes.length; i++) { boxes[i] = i; }
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 4; j++) { m.put(boxes[i], boxes[j]); }
        }
        Multiset<Integer> keys = m.keys();
        Integer k = boxes[7];
        Integer v = boxes[9];
        long[] sink = new long[1];
        BiConsumer<Integer, Integer> visit = (a, b) -> sink[0] += a + b;
        Runnable op = () -> {
            m.put(k, v);
            sink[0] += m.containsEntry(k, v) ? 1 : 0;
            sink[0] += m.containsKey(k) ? 1 : 0;
            sink[0] += keys.count(k);
            m.remove(k, v);
            m.forEach(visit);
        };
        for (int i = 0; i < 20_000; i++) { op.run(); }
        long best = Long.MAX_VALUE;
        long id = Thread.currentThread().threadId();
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 20_000; i++) { op.run(); }
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state allocation in put/containsEntry/containsKey/count/remove/forEach");
        assertTrue(sink[0] != 0);
    }

    /** A multimap whose asMap lookups throw, to exercise the defensive equality path. */
    private static final class BrokenMultimap extends com.google.common.collect.ForwardingListMultimap<Integer, Integer> {
        private final ListMultimap<Integer, Integer> delegate = guava();

        @Override protected ListMultimap<Integer, Integer> delegate() {
            return delegate;
        }

        @Override public Map<Integer, Collection<Integer>> asMap() {
            return new java.util.AbstractMap<>() {
                @Override public int size() {
                    return 3;
                }

                @Override public java.util.Set<Map.Entry<Integer, Collection<Integer>>> entrySet() {
                    return java.util.Set.of();
                }

                @Override public Collection<Integer> get(Object key) {
                    throw new ClassCastException();
                }
            };
        }
    }

    private static ListMultimap<Integer, Integer> one() {
        ListMultimap<Integer, Integer> m = ArrayListMultimap.create();
        m.put(1, 2);
        return m;
    }
}
