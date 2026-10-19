/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import org.junit.jupiter.api.Test;

/** Focused contract cases against the Guava provider, plus owned-only edge and invariant cases. */
final class M3EnumMultisetContractTest {
    private static final Code A = Code.at(1);
    private static final Code B = Code.at(2);
    private static final Code C = Code.at(3);
    private static final Code HIGH = Code.at(66);

    @FunctionalInterface
    interface Scenario {
        Object run(Multiset<Code> m) throws Exception;
    }

    private static Multiset<Code> guava() {
        Multiset<Code> m = EnumMultiset.create(Code.class);
        m.add(A, 3);
        m.add(B);
        m.add(C, 2);
        m.add(HIGH, 2);
        return m;
    }

    private static M3EnumMultiset<Code> owned() {
        M3EnumMultiset<Code> m = M3EnumMultiset.create(Code.class);
        m.add(A, 3);
        m.add(B);
        m.add(C, 2);
        m.add(HIGH, 2);
        return m;
    }

    /** Result, or exception class and message: both implementations must agree on both. */
    private static Object outcome(Scenario s, Multiset<Code> m) {
        try {
            return M3EnumMultisetDifferentialTest.outcome(() -> s.run(m));
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static void both(String name, Scenario s) {
        Multiset<Code> g = guava();
        M3EnumMultiset<Code> o = owned();
        assertEquals(messages(s, g), messages(s, o), name);
        assertEquals(g.toString(), o.toString(), name + " state");
        assertEquals(g, o, name + " equality");
        o.verifyInvariants();
        M3EnumMultisetDifferentialTest.CHECKS.addAndGet(3);
    }

    /** Like outcome, but keeps exception messages (the differential run compares classes only). */
    private static Object messages(Scenario s, Multiset<Code> m) {
        try {
            Object v = s.run(m);
            return v instanceof Multiset<?> ms ? new ArrayList<>(ms) : v;
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void factoriesAndArguments() {
        assertEquals(messages(m -> EnumMultiset.create(List.<Code>of()), null), messages(m -> M3EnumMultiset.create(List.<Code>of()), null));
        assertEquals(messages(m -> EnumMultiset.create(M3EnumMultisetContractTest.<Code>rawClass(String.class)), null), messages(m -> M3EnumMultiset.create(M3EnumMultisetContractTest.<Code>rawClass(String.class)), null));
        assertEquals(messages(m -> EnumMultiset.create((Class<Code>) null), null), messages(m -> M3EnumMultiset.create((Class<Code>) null), null));
        assertEquals(messages(m -> EnumMultiset.create((Iterable<Code>) null), null), messages(m -> M3EnumMultiset.create((Iterable<Code>) null), null));
        assertEquals(messages(m -> EnumMultiset.create(null, Code.class), null), messages(m -> M3EnumMultiset.create(null, Code.class), null));
        assertEquals(EnumMultiset.create(List.of(A, A, B)), M3EnumMultiset.create(List.of(A, A, B)));
        Iterable<Code> iterable = List.of(C, A, C)::iterator;
        assertEquals(EnumMultiset.create(iterable).toString(), M3EnumMultiset.create(iterable).toString());
        assertEquals(EnumMultiset.create(iterable, Code.class).toString(), M3EnumMultiset.create(iterable, Code.class).toString());
        assertEquals(EnumMultiset.create(List.of(B), Code.class).toString(), M3EnumMultiset.create(List.of(B), Code.class).toString());
        assertEquals(EnumMultiset.create(guava()).toString(), M3EnumMultiset.create(guava()).toString());
        assertEquals(EnumMultiset.create(List.of(Body.PLUS, Body.BLANK, Body.BLANK, Body.MINUS)).toString(),
                M3EnumMultiset.create(List.of(Body.PLUS, Body.BLANK, Body.BLANK, Body.MINUS)).toString());
        assertEquals(EnumMultiset.create(List.of(Body.BLANK, Body.MINUS)).toString(), M3EnumMultiset.create(List.of(Body.BLANK, Body.MINUS)).toString());
        assertEquals(messages(m -> EnumMultiset.create(List.of(Body.PLUS), rawClass(Body.PLUS.getClass())), null),
                messages(m -> M3EnumMultiset.create(List.of(Body.PLUS), rawClass(Body.PLUS.getClass())), null));
        both("add null", m -> m.add(null));
        both("add foreign", m -> raw(m).add(Other.X));
        both("add foreign counted", m -> raw(m).add("C01", 2));
        both("setCount foreign", m -> raw(m).setCount(Other.Y, 1));
        both("add negative", m -> m.add(A, -1));
        both("remove negative", m -> m.remove(A, -1));
        both("remove foreign negative", m -> List.of(m.remove(Other.X, -1), m.remove(null, -1), m.remove("x", -5)));
        both("setCount negative", m -> m.setCount(A, -1));
        both("setCount old negative", m -> m.setCount(A, -1, 0));
        both("setCount new negative", m -> m.setCount(A, 0, -1));
        both("setCount cas null", m -> List.of(m.setCount(null, 1, 2)));
        both("setCount cas null to set", m -> m.setCount(null, 0, 1));
        both("raw setCount cas foreign", m -> List.of(raw(m).setCount("x", 1, 2), raw(m).setCount(Other.X, 2, 0)));
        both("raw setCount cas foreign to set", m -> raw(m).setCount(Other.X, 0, 1));
        both("raw setCount cas non-enum to set", m -> raw(m).setCount("x", 0, 1));
        both("raw add non-enum", m -> raw(m).add("x"));
        Multiset<Other> gSmall = EnumMultiset.create(Other.class);
        M3EnumMultiset<Other> oSmall = M3EnumMultiset.create(Other.class);
        for (Multiset<Other> s : List.of(gSmall, oSmall)) { s.add(Other.Y, 2); }
        assertEquals(messages(m -> List.of(gSmall.count(Code.at(5)), gSmall.count(Code.at(1)), gSmall.remove(Code.at(5), 1), gSmall.contains(Body.MINUS)), null),
                messages(m -> List.of(oSmall.count(Code.at(5)), oSmall.count(Code.at(1)), oSmall.remove(Code.at(5), 1), oSmall.contains(Body.MINUS)), null));
        assertEquals(messages(m -> raw(gSmall).add(Code.at(5)), null), messages(m -> raw(oSmall).add(Code.at(5)), null));
        M3EnumMultiset<Empty> none = M3EnumMultiset.create(Empty.class);
        none.verifyInvariants();
        assertEquals(EnumMultiset.create(Empty.class).toString(), none.toString());
        assertFalse(none.iterator().hasNext());
        both("add zero", m -> List.of(m.add(A, 0), m.add(Code.at(9), 0)));
        both("remove zero", m -> List.of(m.remove(A, 0), m.remove(Code.at(9), 0)));
        both("too many", m -> m.add(A, Integer.MAX_VALUE));
        both("max new", m -> List.of(m.add(Code.at(9), Integer.MAX_VALUE), m.size()));
        both("saturated size", m -> {
            m.add(Code.at(8), Integer.MAX_VALUE);
            m.add(Code.at(9), Integer.MAX_VALUE);
            return List.of(m.size(), m.count(Code.at(8)), m.isEmpty());
        });
        both("setCount transitions", m -> List.of(m.setCount(A, 5), m.setCount(A, 0), m.setCount(Code.at(7), 0), m.setCount(Code.at(7), 2), m.count(Code.at(7))));
        both("setCount cas", m -> List.of(m.setCount(B, 1, 4), m.setCount(B, 1, 0), m.setCount(Code.at(9), 0, 3), m.setCount(Code.at(9), 3, 0), m.setCount(Code.at(5), 0, 0)));
        both("foreign queries", m -> List.of(m.count(null), m.count(Other.X), m.count("x"), m.contains(Other.Y), m.remove(Other.X), m.remove(null, 2)));
        both("addAll empty", m -> List.of(m.addAll(List.of()), m.addAll(EnumMultiset.create(Code.class)), m.addAll(M3EnumMultiset.create(Code.class))));
        both("addAll self", m -> m.addAll(m));
        both("addAll owned", m -> m.addAll(M3EnumMultiset.create(List.of(A, Code.at(9), Code.at(9)))));
        both("addAll immutable", m -> m.addAll(ImmutableMultiset.of(B, B, Code.at(6))));
        both("addAll foreign multiset", m -> raw(m).addAll(ImmutableMultiset.of(Other.X)));
        both("addAll null", m -> m.addAll(null));
        both("removeAll null", m -> m.removeAll(null));
        both("retainAll null", m -> m.retainAll(null));
        both("retainAll multiset", m -> m.retainAll(HashMultiset.create(List.of(A, C))));
        both("removeAll multiset", m -> m.removeAll(HashMultiset.create(List.of(A, Code.at(9)))));
        both("elementSet removeAll multiset", m -> m.elementSet().removeAll(HashMultiset.create(List.of(B, C))));
        both("elementSet removeAll null", m -> m.elementSet().removeAll(null));
        both("elementSet retainAll null", m -> m.elementSet().retainAll(null));
        both("elementSet removeAll big set", m -> m.elementSet().removeAll(new java.util.HashSet<>(List.of(A, Code.at(5), Code.at(6), Code.at(7), Code.at(8)))));
        both("entrySet removeAll big set", m -> m.entrySet().removeAll(new java.util.HashSet<>(List.of(Multisets.immutableEntry(A, 3), "x", 1, 2, 3, 4))));
        both("entrySet removeAll list", m -> m.entrySet().removeAll(List.of(Multisets.immutableEntry(B, 1), Multisets.immutableEntry(C, 1))));
        both("entrySet retainAll", m -> m.entrySet().retainAll(List.of(Multisets.immutableEntry(B, 1))));
    }

    @Test
    void viewsEntriesAndText() {
        both("elementSet ops", m -> List.of(m.elementSet().contains(A), m.elementSet().remove(A), m.elementSet().remove(A), m.elementSet().size(),
                m.elementSet().isEmpty(), m.elementSet().containsAll(List.of(B, C)), m.elementSet().remove(Other.X)));
        both("elementSet clear", m -> {
            m.elementSet().clear();
            return List.of(m.isEmpty(), m.elementSet().isEmpty());
        });
        both("entrySet clear", m -> {
            m.entrySet().clear();
            return List.of(m.size(), m.entrySet().isEmpty());
        });
        both("entrySet contains", m -> List.of(m.entrySet().contains(Multisets.immutableEntry(A, 3)), m.entrySet().contains(Multisets.immutableEntry(A, 0)),
                m.entrySet().contains(Multisets.immutableEntry(Code.at(9), 0)), m.entrySet().contains("x"), m.entrySet().contains(Multisets.immutableEntry(A, 2)),
                m.entrySet().contains(Multisets.immutableEntry(Other.X, 1))));
        both("entrySet remove", m -> List.of(m.entrySet().remove(Multisets.immutableEntry(A, 2)), m.entrySet().remove(Multisets.immutableEntry(A, 0)),
                m.entrySet().remove("x"), m.entrySet().remove(Multisets.immutableEntry(A, 3)), m.entrySet().remove(Multisets.immutableEntry(Other.X, 1))));
        both("entrySet remove negative custom entry", m -> m.entrySet().remove(new NegativeEntry()));
        both("live entry", m -> {
            Multiset.Entry<Code> e = m.entrySet().iterator().next();
            m.add(A, 2);
            int five = e.getCount();
            m.setCount(A, 0);
            int gone = e.getCount();
            return List.of(five, gone, e.toString(), e.equals(Multisets.immutableEntry(A, 0)), e.hashCode(), e.getElement());
        });
        both("entry text", m -> {
            List<String> out = new ArrayList<>();
            for (Multiset.Entry<Code> e : m.entrySet()) { out.add(e.toString() + "|" + e.hashCode() + "|" + e.equals(e) + "|" + e.equals("x") + "|" + e.equals(Multisets.immutableEntry(e.getElement(), e.getCount() + 1)) + "|" + e.equals(Multisets.immutableEntry(Code.at(9), e.getCount()))); }
            return out;
        });
        both("equality", m -> List.of(m.equals(m), m.equals("x"), m.equals(HashMultiset.create(List.of(A, B, C))),
                m.equals(HashMultiset.create(guava())), m.equals(EnumMultiset.create(List.of(A, A, A, B, C, C, HIGH, Code.at(9)))), m.equals(EnumMultiset.create(List.of(A, A, B, B, C, C, HIGH, HIGH)))));
        both("text", m -> List.of(m.toString(), m.entrySet().toString(), m.elementSet().toString(), m.hashCode(), m.elementSet().hashCode()));
        both("streams", m -> List.of(m.stream().count(), m.spliterator().estimateSize(), m.spliterator().characteristics(),
                m.elementSet().spliterator().estimateSize(), m.entrySet().spliterator().estimateSize(),
                m.elementSet().spliterator().hasCharacteristics(Spliterator.DISTINCT), m.parallelStream().toList(), m.stream().skip(2).toList()));
        both("spliterator walk", m -> {
            Spliterator<Code> s = m.spliterator();
            List<Object> out = new ArrayList<>();
            while (s.tryAdvance(out::add)) { out.add(s.estimateSize()); }
            out.add(s.tryAdvance(out::add));
            out.add(s.estimateSize());
            return out;
        });
        both("spliterator null action", m -> m.spliterator().tryAdvance(null));
        both("toArray", m -> List.of(List.of(m.toArray(new Code[0])), m.toArray(new Code[9]).length, List.of(m.toArray())));
        both("containsAll", m -> List.of(m.containsAll(List.of(A, B)), m.containsAll(List.of(A, Code.at(9))), m.elementSet().containsAll(List.of(C))));
        both("forEachEntry", m -> {
            List<String> seen = new ArrayList<>();
            m.forEachEntry((x, n) -> seen.add(x + ":" + n));
            return seen;
        });
        both("forEachEntry adds", m -> {
            List<String> seen = new ArrayList<>();
            m.forEachEntry((x, n) -> {
                seen.add(x + ":" + n);
                m.add(Code.at(x.ordinal() + 1));
            });
            return seen;
        });
        both("forEach adds occurrences", m -> {
            List<Code> seen = new ArrayList<>();
            m.forEach(x -> {
                seen.add(x);
                if (x == B) { m.add(Code.at(4)); }
                if (x == A) { m.setCount(C, 0); }
            });
            return seen;
        });
        both("empty", m -> {
            m.clear();
            return List.of(m.isEmpty(), m.size(), m.entrySet().isEmpty(), m.iterator().hasNext(), m.toString(), m.hashCode(), m.elementSet().iterator().hasNext());
        });
    }

    @Test
    void iterators() {
        both("occurrence iterator", m -> {
            List<Code> seen = new ArrayList<>();
            for (Code x : m) { seen.add(x); }
            return seen;
        });
        both("iterator remove twice", m -> {
            Iterator<Code> it = m.iterator();
            it.next();
            it.remove();
            it.remove();
            return null;
        });
        both("iterator remove before next", m -> {
            m.iterator().remove();
            return null;
        });
        both("iterator exhausted", m -> {
            Iterator<Code> it = m.iterator();
            while (it.hasNext()) { it.next(); }
            return it.next();
        });
        both("element iterator exhausted", m -> {
            Iterator<Code> it = m.elementSet().iterator();
            while (it.hasNext()) { it.next(); }
            return it.next();
        });
        both("element iterator remove twice", m -> {
            Iterator<Code> it = m.elementSet().iterator();
            it.next();
            it.remove();
            it.remove();
            return null;
        });
        both("element iterator remove after external zero", m -> {
            Iterator<Code> it = m.elementSet().iterator();
            Code x = it.next();
            m.setCount(x, 0);
            it.remove();
            return m.toString();
        });
        both("new element during iteration", m -> {
            Iterator<Code> it = m.elementSet().iterator();
            it.next();
            m.add(Code.at(0));
            m.add(Code.at(9));
            List<Code> rest = new ArrayList<>();
            while (it.hasNext()) { rest.add(it.next()); }
            return rest;
        });
        both("occurrence remove after external removal", m -> {
            Iterator<Code> it = m.iterator();
            Code x = it.next();
            m.setCount(x, 0);
            m.add(x);
            it.remove();
            return m.toString();
        });
        both("occurrence remove after count drop", m -> {
            Iterator<Code> it = m.iterator();
            Code x = it.next();
            m.remove(x, 2);
            it.next();
            it.remove();
            it.next();
            it.remove();
            return m.toString();
        });
        both("occurrence single remove after external add", m -> {
            Iterator<Code> it = m.iterator();
            for (int i = 0; i < 4; i++) { it.next(); }
            m.add(B, 3);
            it.remove();
            return m.toString();
        });
        both("forEachEntry null", m -> {
            m.forEachEntry((ObjIntConsumer<Code>) null);
            return null;
        });
        both("forEach null", m -> {
            m.forEach((Consumer<Code>) null);
            return null;
        });
        both("entry iterator remove twice", m -> {
            Iterator<Multiset.Entry<Code>> it = m.entrySet().iterator();
            it.next();
            it.remove();
            it.remove();
            return null;
        });
        both("removeIf all", m -> List.of(m.removeIf(x -> true), m.isEmpty()));
    }

    @Test
    void serializationRoundTrip() throws Exception {
        M3EnumMultiset<Code> o = owned();
        Object back = read(write(o));
        assertEquals(o, back);
        @SuppressWarnings("unchecked") M3EnumMultiset<Code> copy = (M3EnumMultiset<Code>) back;
        copy.verifyInvariants();
        copy.add(A);
        assertEquals(4, copy.count(A));
        assertEquals(o.toString(), read(write(EnumMultiset.create(o))).toString());
        assertEquals("[]", read(write(M3EnumMultiset.create(Other.class))).toString());
        assertThrows(InvalidObjectException.class, () -> read(corruptCounts(-1, 1)));
        assertThrows(InvalidObjectException.class, () -> read(corruptCounts(Other.values().length + 1, 1)));
        assertThrows(InvalidObjectException.class, () -> read(corruptCounts(1, 0)));
        assertEquals("[Y]", read(corruptCounts(1, 1)).toString());
        assertThrows(InvalidObjectException.class, () -> read(spliceType(objectBytes("not a class"))));
        assertThrows(InvalidObjectException.class, () -> read(spliceType(objectBytes(String.class))));
        assertEquals("[]", read(spliceType(objectBytes(Thread.State.class))).toString());
        assertThrows(InvalidObjectException.class, () -> read(replacing(Other.X, Body.MINUS)));
        assertThrows(InvalidObjectException.class, () -> read(replacing(Other.Y, Other.X)));
        assertThrows(InvalidObjectException.class, () -> read(replacing(Other.X, "X")));
    }

    /** The stream bytes of one object (after the stream header); class descriptors in them are self-contained. */
    private static byte[] objectBytes(Object o) throws Exception {
        byte[] all = write(o);
        return java.util.Arrays.copyOfRange(all, 4, all.length);
    }

    /**
     * Writes an empty owned multiset of {@code Other} and replaces its written type (TC_CLASS and the class
     * descriptor chain, up to the element-count block 77 04 00 00 00 00) with {@code replacement}. Nothing later
     * refers to the replaced handles, so the stream stays well formed.
     */
    private static byte[] spliceType(byte[] replacement) throws Exception {
        byte[] data = write(M3EnumMultiset.create(Other.class));
        int start = indexOf(data, new byte[] {0x76, 0x72}, 0);
        int end = indexOf(data, new byte[] {0x77, 4, 0, 0, 0, 0}, start);
        assertTrue(start > 0 && end > start, "owned stream layout");
        byte[] out = new byte[data.length - (end - start) + replacement.length];
        System.arraycopy(data, 0, out, 0, start);
        System.arraycopy(replacement, 0, out, start, replacement.length);
        System.arraycopy(data, end, out, start + replacement.length, data.length - end);
        return out;
    }

    /** Writes the owned multiset {X, Y} of {@code Other} with one element object replaced while streaming. */
    private static byte[] replacing(Object from, Object to) throws Exception {
        M3EnumMultiset<Other> m = M3EnumMultiset.create(Other.class);
        m.add(Other.X);
        m.add(Other.Y);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes) {
            {
                enableReplaceObject(true);
            }

            @Override protected Object replaceObject(Object obj) {
                return obj == from ? to : obj;
            }
        }) {
            out.writeObject(m);
        }
        return bytes.toByteArray();
    }

    private static byte[] write(Object o) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(o); }
        return bytes.toByteArray();
    }

    private static Object read(byte[] data) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) { return in.readObject(); }
    }

    /** Rewrites the element count and the first count (TC_BLOCKDATA int payloads 77 04 00 00 00 01). */
    private static byte[] corruptCounts(int elements, int count) throws Exception {
        M3EnumMultiset<Other> one = M3EnumMultiset.create(Other.class);
        one.add(Other.Y);
        byte[] data = write(one);
        List<Integer> at = new ArrayList<>();
        for (int i = 0; i + 6 <= data.length; i++) {
            if (data[i] == 0x77 && data[i + 1] == 4 && java.nio.ByteBuffer.wrap(data, i + 2, 4).getInt() == 1) { at.add(i + 2); }
        }
        assertEquals(2, at.size(), "owned stream layout");
        java.nio.ByteBuffer.wrap(data, at.get(0), 4).putInt(elements);
        java.nio.ByteBuffer.wrap(data, at.get(1), 4).putInt(count);
        return data;
    }

    private static int indexOf(byte[] data, byte[] needle, int from) {
        outer:
        for (int i = from; i + needle.length <= data.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) { continue outer; }
            }
            return i;
        }
        return -1;
    }

    @Test
    void ownedLanesAndSharing() throws Exception {
        M3EnumMultiset<Code> a = M3EnumMultiset.create(Code.class);
        M3EnumMultiset<Code> b = M3EnumMultiset.create(Code.class);
        assertSame(get(a, "universe"), get(b, "universe"), "one universe per enum class");
        assertNotSame(get(a, "counts"), get(b, "counts"));
        assertEquals(2, ((long[]) get(a, "present")).length);
        assertEquals(70, a.nextPresent(0));
        assertEquals(70, a.nextPresent(70));
        assertEquals(70, a.nextPresent(128));
        a.add(Code.at(63));
        a.add(Code.at(64));
        assertEquals(63, a.nextPresent(0));
        assertEquals(64, a.nextPresent(64));
        assertEquals(70, a.nextPresent(65));
        M3EnumMultiset<Other> small = M3EnumMultiset.create(Other.class);
        assertEquals(2, small.nextPresent(0));
        assertEquals(2, small.nextPresent(64));
        assertThrows(NoSuchElementException.class, () -> small.iterator().next());
        a.verifyInvariants();
    }

    @Test
    void invariantCheckerDetectsCorruption() {
        expectBroken("lanes", m -> set(m, "universe", new Object[70]));
        expectBroken("lanes", m -> set(m, "counts", new int[3]));
        expectBroken("lanes", m -> set(m, "present", new long[1]));
        expectBroken("mask", m -> ints(m, "counts")[5] = 1);
        expectBroken("mask", m -> ints(m, "counts")[1] = -1);
        expectBroken("mask", m -> longs(m, "present")[0] ^= 1L << 2);
        expectBroken("mask tail", m -> longs(m, "present")[1] |= 1L << 63);
        expectBroken("totals", m -> setLong(m, "total", 99));
        expectBroken("totals", m -> setInt(m, "distinct", 9));
    }

    @Test
    void steadyStateHotPathsAllocateNothing() {
        java.lang.management.ThreadMXBean base = ManagementFactory.getThreadMXBean();
        assumeTrue(base instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) base;
        assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        M3EnumMultiset<Code> m = M3EnumMultiset.create(Code.class);
        for (Code c : Code.values()) { m.add(c, 3); }
        long[] sink = new long[1];
        ObjIntConsumer<Code> visit = (x, n) -> sink[0] += x.ordinal() + n;
        Code k = Code.at(7);
        Runnable op = () -> {
            m.add(k, 2);
            sink[0] += m.count(k);
            sink[0] += m.contains(k) ? 1 : 0;
            m.remove(k, 2);
            m.setCount(k, 3);
            m.forEachEntry(visit);
            sink[0] += m.size() + m.hashCode();
        };
        for (int i = 0; i < 20_000; i++) { op.run(); }
        long best = Long.MAX_VALUE;
        long id = Thread.currentThread().threadId();
        for (int trial = 0; trial < 3; trial++) {
            long before = bean.getThreadAllocatedBytes(id);
            for (int i = 0; i < 20_000; i++) { op.run(); }
            best = Math.min(best, bean.getThreadAllocatedBytes(id) - before);
        }
        assertEquals(0L, best, "steady-state allocation in add/count/contains/remove/setCount/forEachEntry/size/hashCode");
        assertTrue(sink[0] != 0);
        assertSame(m.elementSet(), m.elementSet());
        assertSame(m.entrySet(), m.entrySet());
        assertFalse(m.isEmpty());
    }

    // ------------------------------------------------------------------ helpers

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Multiset<Object> raw(Multiset<?> m) {
        return (Multiset) m;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Class<T> rawClass(Class<?> c) {
        return (Class) c;
    }

    /** A custom entry with a negative count: Guava's entry-set removal reaches setCount's argument check. */
    private static final class NegativeEntry implements Multiset.Entry<Code> {
        @Override public Code getElement() {
            return A;
        }

        @Override public int getCount() {
            return -1;
        }
    }

    private interface Corruption {
        void apply(M3EnumMultiset<Code> m) throws Exception;
    }

    private static void expectBroken(String message, Corruption corrupt) {
        M3EnumMultiset<Code> m = owned();
        m.verifyInvariants();
        try {
            corrupt.apply(m);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        AssertionError e = assertThrows(AssertionError.class, m::verifyInvariants);
        assertEquals(message, e.getMessage());
    }

    private static Field field(String name) {
        try {
            Field f = M3EnumMultiset.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    private static void set(Object m, String name, Object value) throws IllegalAccessException {
        field(name).set(m, value);
    }

    private static void setInt(Object m, String name, int value) throws IllegalAccessException {
        field(name).setInt(m, value);
    }

    private static void setLong(Object m, String name, long value) throws IllegalAccessException {
        field(name).setLong(m, value);
    }

    private static Object get(Object m, String name) throws IllegalAccessException {
        return field(name).get(m);
    }

    private static int[] ints(Object m, String name) throws IllegalAccessException {
        return (int[]) get(m, name);
    }

    private static long[] longs(Object m, String name) throws IllegalAccessException {
        return (long[]) get(m, name);
    }
}
