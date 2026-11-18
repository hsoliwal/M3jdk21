/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary FrozenBag array/DOOP owner preserves encounter-ordered counted semantics
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenBagExactLanesTest
 */
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.SplittableRandom;
import jdk.internal.m3.collections.FrozenBag;

public final class FrozenBagExactLanesTest {
    private static long checks;

    public static void main(String[] args) {
        for (int size : new int[] {0, 1, 2, 7, 8, 9, 13, 14, 20, 21, 64, 256, 4096, 65536}) {
            verify(populated(new LinkedHashSet<>(), size));
            verify(populated(new HashSet<>(), size));
            if (size <= 4096) {
                ArrayList<Integer> list = new ArrayList<>();
                for (int i = 0; i < size; i++) list.add(i % 33);
                verify(list);
                verify(new UntrustedSizeCollection<>(list));
            }
        }
        SplittableRandom random = new SplittableRandom(0xF20A56D0AL);
        for (int round = 0; round < 180; round++) {
            int n = random.nextInt(800);
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = 0; i < n; i++) list.add(random.nextInt(-60, 61));
            verify(list);
            verify(new UntrustedSizeCollection<>(list));
        }
        Set<Integer> hashSubclass = new HashSet<>() {
            @Override public int size() {
                throw new AssertionError("HashSet subclass size() queried");
            }
        };
        verify(populated(hashSubclass, 33));
        Set<Integer> linkedSubclass = new LinkedHashSet<>() {
            @Override public int size() {
                throw new AssertionError("LinkedHashSet subclass size() queried");
            }
        };
        verify(populated(linkedSubclass, 33));
        verifyCollisionsAndIdentity();
        checkNullAndMutatorContracts();
        System.out.println("FrozenBagExactLanesTest PASS: " + checks + " assertions");
    }

    private static Set<Integer> populated(Set<Integer> set, int count) {
        for (int i = 0; i < count; i++) {
            set.add(Integer.rotateLeft(31 * i, 11));
        }
        return set;
    }

    private static <E> void verify(Collection<E> source) {
        LinkedHashMap<E, Integer> reference = new LinkedHashMap<>();
        int total = 0;
        for (E item : source) {
            reference.merge(item, 1, Integer::sum);
            total++;
        }
        FrozenBag<E> frozen = FrozenBag.copyOf(source);
        eq(frozen.size(), total, "total size");
        eq(frozen.distinctSize(), reference.size(), "distinct size");
        eq(frozen.elementSet().size(), reference.size(), "elementSet size");
        eq(frozen.count(null), 0, "null absent count");
        eq(frozen.contains(null), false, "null absent containment");
        Iterator<Map.Entry<E, Integer>> iter = reference.entrySet().iterator();
        Iterator<E> elements = frozen.elementSet().iterator();
        for (int ordinal = 0; ordinal < reference.size(); ordinal++) {
            Map.Entry<E, Integer> expected = iter.next();
            check(frozen.elementAt(ordinal) == expected.getKey(),
                    "first representative and encounter ordinal");
            eq(frozen.countAt(ordinal), expected.getValue(), "ordinal count");
            check(elements.next() == expected.getKey(), "facade identity");
            eq(frozen.count(expected.getKey()), expected.getValue(), "key count");
            check(frozen.contains(expected.getKey()), "known key containment");
            check(frozen.elementSet().contains(expected.getKey()), "known element facade");
        }
        check(!elements.hasNext(), "elementSet exhausted");
        check(!iter.hasNext(), "expected exhausted");
        check(frozen.elementSet().spliterator().hasCharacteristics(
                        Spliterator.ORDERED | Spliterator.DISTINCT
                                | Spliterator.IMMUTABLE | Spliterator.NONNULL),
                "frozen spliterator traits");
        if (reference.isEmpty()) {
            fails(NoSuchElementException.class, elements::next, "empty iterator");
        }
        fails(IndexOutOfBoundsException.class, () -> frozen.elementAt(-1), "negative ordinal");
        fails(IndexOutOfBoundsException.class,
                () -> frozen.countAt(reference.size()), "high ordinal");
    }

    private record Collision(int id) {
        @Override public int hashCode() { return id & 15; }
    }

    private static void verifyCollisionsAndIdentity() {
        ArrayList<Collision> values = new ArrayList<>();
        Collision representative = new Collision(1);
        values.add(representative);
        for (int id = 0; id < 1400; id++) {
            values.add(new Collision(id));
            values.add(new Collision(id));
        }
        FrozenBag<Collision> bag = FrozenBag.copyOf(values);
        eq(bag.distinctSize(), 1400, "collision distinct");
        eq(bag.size(), 2801, "collision total");
        check(bag.elementAt(0) == representative, "first colliding representative");
        eq(bag.count(new Collision(1)), 3, "colliding repeated key count");
        for (int id = 0; id < 1400; id++) {
            check(bag.contains(new Collision(id)), "colliding membership");
        }
        values.clear();
        eq(bag.size(), 2801, "detached source");
    }

    private static void checkNullAndMutatorContracts() {
        fails(NullPointerException.class, () -> FrozenBag.copyOf(null), "null source");
        fails(NullPointerException.class, () -> FrozenBag.copyOf(Arrays.asList("x", null)),
                "null element");
        FrozenBag<String> empty = FrozenBag.copyOf(List.of());
        FrozenBag<String> one = FrozenBag.copyOf(List.of("a", "a", "b"));
        for (FrozenBag<String> bag : List.of(empty, one)) {
            fails(UnsupportedOperationException.class, () -> bag.add(null, 0), "add no-op");
            fails(UnsupportedOperationException.class, () -> bag.remove("missing", 0),
                    "remove no-op");
            fails(UnsupportedOperationException.class, () -> bag.setCount("missing", 0),
                    "set count no-op");
            fails(UnsupportedOperationException.class,
                    () -> bag.elementSet().remove("missing"), "set remove");
            fails(UnsupportedOperationException.class,
                    () -> bag.elementSet().clear(), "set clear");
        }
        check(FrozenBag.scaffoldDescriptor().startsWith("M3FrozenBag|"), "descriptor");
    }

    private static void check(boolean condition, String context) {
        checks++;
        if (!condition) throw new AssertionError(context);
    }
    private static void eq(Object actual, Object expected, String context) {
        checks++;
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError(context + " actual=" + actual + " expected=" + expected);
        }
    }
    private static void fails(Class<? extends Throwable> kind, Runnable action, String context) {
        try {
            action.run();
        } catch (Throwable ex) {
            check(kind.isInstance(ex), context + " wrong: " + ex);
            return;
        }
        throw new AssertionError(context + ": exception not thrown");
    }
    private static final class UntrustedSizeCollection<E> extends AbstractCollection<E> {
        private final Collection<E> source;
        UntrustedSizeCollection(Collection<E> source) { this.source = source; }
        @Override public int size() {
            throw new AssertionError("untrusted size() called");
        }
        @Override public Iterator<E> iterator() { return source.iterator(); }
    }
}
