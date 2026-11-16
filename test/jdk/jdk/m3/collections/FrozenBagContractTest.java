/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import jdk.internal.m3.collections.FrozenBag;
import jdk.internal.m3.collections.FrozenLongBag;
import org.junit.jupiter.api.Test;

/** Independent count/order models for the two existing internal frozen bag APIs. */
final class FrozenBagContractTest {
    private static final class Key {
        final int value;
        final String label;
        Key(int value, String label) {
            this.value = value;
            this.label = label;
        }
        @Override public int hashCode() {
            return value & 3;
        }
        @Override public boolean equals(Object other) {
            return other instanceof Key key && value == key.value;
        }
        @Override public String toString() {
            return value + ":" + label;
        }
    }

    private static final class CallbackKey {
        final int value;
        final RuntimeException failure;
        boolean failHash;
        boolean failEquals;
        CallbackKey(int value, RuntimeException failure) {
            this.value = value;
            this.failure = failure;
        }
        @Override public int hashCode() {
            if (failHash) {
                throw failure;
            }
            return 0;
        }
        @Override public boolean equals(Object other) {
            if (failEquals) {
                throw failure;
            }
            return other instanceof CallbackKey key && value == key.value;
        }
    }

    private static <E> void verifyObject(List<E> input, FrozenBag<E> actual) {
        Map<E, Integer> expected = new LinkedHashMap<>();
        for (E element : input) {
            expected.merge(element, 1, Integer::sum);
        }
        assertEquals(input.size(), actual.size());
        assertEquals(expected.size(), actual.distinctSize());
        assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.elementSet()));
        int ordinal = 0;
        for (Map.Entry<E, Integer> entry : expected.entrySet()) {
            assertSame(entry.getKey(), actual.elementAt(ordinal));
            assertEquals(entry.getValue().intValue(), actual.countAt(ordinal));
            assertEquals(entry.getValue().intValue(), actual.count(entry.getKey()));
            assertTrue(actual.contains(entry.getKey()));
            ordinal++;
        }
        assertEquals(0, actual.count(null));
        assertFalse(actual.contains(null));
        assertFalse(actual.elementSet().contains(null));
        Object missing = new Object();
        assertEquals(0, actual.count(missing));
        assertFalse(actual.contains(missing));
        assertEquals(expected.keySet(), actual.elementSet());
        assertEquals(expected.keySet().hashCode(), actual.elementSet().hashCode());
        for (int index : new int[] {Integer.MIN_VALUE, -1, expected.size(), Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> actual.elementAt(index));
            assertThrows(IndexOutOfBoundsException.class, () -> actual.countAt(index));
        }
    }

    private static void verifyLong(long[] input, FrozenLongBag actual) {
        Map<Long, Integer> expected = new TreeMap<>();
        for (long element : input) {
            expected.merge(element, 1, Integer::sum);
        }
        assertEquals(input.length, actual.size());
        assertEquals(expected.size(), actual.distinctSize());
        long[] ordered = expected.keySet().stream().mapToLong(Long::longValue).toArray();
        assertArrayEquals(ordered, actual.distinctToArray());
        int ordinal = 0;
        for (Map.Entry<Long, Integer> entry : expected.entrySet()) {
            assertEquals(entry.getKey().longValue(), actual.elementAt(ordinal));
            assertEquals(entry.getValue().intValue(), actual.countAt(ordinal));
            assertEquals(entry.getValue().intValue(), actual.occurrencesOf(entry.getKey()));
            assertTrue(actual.contains(entry.getKey()));
            ordinal++;
        }
        List<Long> traversed = new ArrayList<>();
        actual.forEachDistinct(traversed::add);
        assertEquals(new ArrayList<>(expected.keySet()), traversed);
        for (long query : new long[] {Long.MIN_VALUE, Long.MIN_VALUE + 1, -1, 0, 1,
                1L << 32, Long.MAX_VALUE - 1, Long.MAX_VALUE}) {
            int count = expected.getOrDefault(query, 0);
            assertEquals(count, actual.occurrencesOf(query));
            assertEquals(count != 0, actual.contains(query));
        }
        for (int index : new int[] {Integer.MIN_VALUE, -1, expected.size(), Integer.MAX_VALUE}) {
            assertThrows(IndexOutOfBoundsException.class, () -> actual.elementAt(index));
            assertThrows(IndexOutOfBoundsException.class, () -> actual.countAt(index));
        }
    }

    @Test void emptyConstructorsAndFactoriesAreUsable() {
        verifyObject(List.of(), new FrozenBag<>());
        verifyObject(List.of(), FrozenBag.copyOf(List.of()));
        verifyLong(new long[0], new FrozenLongBag());
        verifyLong(new long[0], FrozenLongBag.copyOf(new long[0]));
        AtomicInteger calls = new AtomicInteger();
        new FrozenLongBag().forEachDistinct(value -> calls.incrementAndGet());
        assertEquals(0, calls.get());
        assertThrows(NullPointerException.class, () -> FrozenBag.copyOf(null));
        assertThrows(NullPointerException.class, () -> FrozenBag.copyOf(Arrays.asList("a", null)));
        assertThrows(NullPointerException.class, () -> FrozenLongBag.copyOf(null));
        assertThrows(NullPointerException.class, () -> new FrozenLongBag().forEachDistinct(null));
    }

    @Test void objectCopiesRetainFirstRepresentativesAndEncounterOrder() {
        Key firstSeven = new Key(7, "first");
        Key firstMinus = new Key(-3, "first");
        List<Key> source = new ArrayList<>(List.of(firstSeven, firstMinus,
                new Key(7, "second"), new Key(0, "zero"), new Key(-3, "second")));
        List<Key> frozenInput = new ArrayList<>(source);
        FrozenBag<Key> bag = FrozenBag.copyOf(source);
        verifyObject(frozenInput, bag);
        assertSame(firstSeven, bag.elementAt(0));
        assertSame(firstMinus, bag.elementAt(1));
        source.clear();
        source.add(new Key(99, "later"));
        verifyObject(frozenInput, bag);
        assertEquals(2, bag.count(new Key(7, "query")));
        assertEquals(0, bag.count(new Key(11, "same-hash-absent")));
    }

    @Test void objectCollisionCountsMatchIndependentLinkedMapModel() {
        Random random = new Random(0x46524f5a454e4241L);
        for (int trial = 0; trial < 240; trial++) {
            int size = random.nextInt(401);
            List<Key> source = new ArrayList<>();
            for (int index = 0; index < size; index++) {
                source.add(new Key(random.nextInt(131) - 65, trial + ":" + index));
            }
            FrozenBag<Key> bag = FrozenBag.copyOf(source);
            verifyObject(source, bag);
            for (int value = -70; value <= 70; value++) {
                int count = 0;
                for (Key key : source) {
                    if (key.value == value) {
                        count++;
                    }
                }
                assertEquals(count, bag.count(new Key(value, "query")));
            }
        }
    }

    @Test void objectDistinctBoundariesAndLargeDuplicateCounts() {
        for (int distinct : new int[] {0, 1, 7, 8, 9, 63, 64, 65, 127, 128, 129}) {
            List<Key> source = new ArrayList<>();
            for (int index = 0; index < distinct; index++) {
                source.add(new Key(index, "first"));
            }
            for (int index = distinct - 1; index >= 0; index--) {
                source.add(new Key(index, "duplicate"));
            }
            verifyObject(source, FrozenBag.copyOf(source));
        }
        Key repeated = new Key(8, "repeated");
        List<Key> source = Collections.nCopies(100_000, repeated);
        FrozenBag<Key> bag = FrozenBag.copyOf(source);
        assertEquals(100_000, bag.size());
        assertEquals(1, bag.distinctSize());
        assertEquals(100_000, bag.count(new Key(8, "query")));
        assertEquals(100_000, bag.countAt(0));
        assertSame(repeated, bag.elementAt(0));
    }

    @Test void factoryUsesEncounteredElementsDespiteHostileReportedSize() {
        List<String> expected = List.of("b", "a", "b", "c");
        for (int reported : new int[] {0, 1, Integer.MAX_VALUE}) {
            AtomicInteger iterators = new AtomicInteger();
            Collection<String> source = new AbstractCollection<>() {
                @Override public int size() {
                    return reported;
                }
                @Override public Iterator<String> iterator() {
                    iterators.incrementAndGet();
                    return expected.iterator();
                }
                @Override public Object[] toArray() {
                    throw new AssertionError("factory must stream the source");
                }
            };
            verifyObject(expected, FrozenBag.copyOf(source));
            assertEquals(1, iterators.get());
        }
    }

    @Test void constructionPropagatesTraversalAndKeyFailures() {
        RuntimeException failure = new IllegalStateException("frozen bag callback witness");
        Collection<String> traversalFailure = new AbstractCollection<>() {
            @Override public int size() {
                return 3;
            }
            @Override public Iterator<String> iterator() {
                return new Iterator<>() {
                    private int next;
                    @Override public boolean hasNext() {
                        return true;
                    }
                    @Override public String next() {
                        if (next++ == 2) {
                            throw failure;
                        }
                        return "counted-before-failure";
                    }
                };
            }
        };
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> FrozenBag.copyOf(traversalFailure)));
        CallbackKey first = new CallbackKey(1, failure);
        first.failHash = true;
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> FrozenBag.copyOf(List.of(first))));
        first.failHash = false;
        CallbackKey second = new CallbackKey(2, failure);
        first.failEquals = true;
        second.failEquals = true;
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> FrozenBag.copyOf(List.of(first, second))));
        first.failEquals = false;
        second.failEquals = false;
        verifyObject(List.of(first, second, first), FrozenBag.copyOf(List.of(first, second, first)));
    }

    private static <E> void assertImmutableSet(Set<E> set, E existing, E absent) {
        List<E> before = new ArrayList<>(set);
        assertThrows(UnsupportedOperationException.class, () -> set.add(absent));
        assertThrows(UnsupportedOperationException.class, () -> set.addAll(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> set.remove(absent));
        assertThrows(UnsupportedOperationException.class, () -> set.remove(existing));
        assertThrows(UnsupportedOperationException.class, () -> set.removeAll(List.of()));
        assertThrows(UnsupportedOperationException.class, () -> set.retainAll(before));
        assertThrows(UnsupportedOperationException.class, set::clear);
        AtomicInteger callbacks = new AtomicInteger();
        assertThrows(UnsupportedOperationException.class, () -> set.removeIf(value -> {
            callbacks.incrementAndGet();
            return false;
        }));
        assertEquals(0, callbacks.get());
        Iterator<E> iterator = set.iterator();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        while (iterator.hasNext()) {
            iterator.next();
            assertThrows(UnsupportedOperationException.class, iterator::remove);
        }
        assertThrows(NoSuchElementException.class, iterator::next);
        assertEquals(before, new ArrayList<>(set));
    }

    @Test void elementSetFacadesAreImmutableOrderedAndIndependent() {
        for (List<String> input : List.of(List.<String>of(), List.of("b", "a", "b", "c"))) {
            FrozenBag<String> bag = FrozenBag.copyOf(input);
            Set<String> view = bag.elementSet();
            assertImmutableSet(view, "b", "missing");
            List<String> expected = new ArrayList<>(new LinkedHashSet<>(input));
            assertEquals(expected, view.stream().toList());
            Spliterator<String> remaining = view.spliterator();
            assertTrue(remaining.hasCharacteristics(Spliterator.ORDERED | Spliterator.DISTINCT
                    | Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.SIZED));
            Spliterator<String> prefix = remaining.trySplit();
            List<String> traversed = new ArrayList<>();
            if (prefix != null) {
                prefix.forEachRemaining(traversed::add);
            }
            remaining.forEachRemaining(traversed::add);
            assertEquals(expected, traversed);
            Object[] exported = view.toArray();
            String[] typed = view.toArray(String[]::new);
            if (exported.length != 0) {
                exported[0] = "replacement";
                typed[0] = "replacement";
                assertEquals("b", bag.elementAt(0));
            }
            assertNotSame(exported, view.toArray());
            verifyObject(input, bag);
        }
    }

    @Test void allObjectMutatorsRefuseBeforeArgumentCallbacks() {
        RuntimeException failure = new IllegalStateException("must not hash during mutation refusal");
        CallbackKey hostile = new CallbackKey(9, failure);
        hostile.failHash = true;
        hostile.failEquals = true;
        for (FrozenBag<CallbackKey> bag : List.<FrozenBag<CallbackKey>>of(new FrozenBag<>(),
                FrozenBag.copyOf(List.of(new CallbackKey(1, failure))))) {
            for (CallbackKey key : Arrays.asList(null, hostile)) {
                for (int count : new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
                    assertThrows(UnsupportedOperationException.class, () -> bag.add(key, count));
                    assertThrows(UnsupportedOperationException.class, () -> bag.remove(key, count));
                    assertThrows(UnsupportedOperationException.class, () -> bag.setCount(key, count));
                }
            }
        }
    }

    @Test void primitiveOrderIsSignedSortedAndCopiesAreOwned() {
        long[] source = {Long.MAX_VALUE, 4, -7, 4, 0, Long.MIN_VALUE,
                Long.MAX_VALUE, Long.MIN_VALUE, 1L << 32, -1};
        long[] original = source.clone();
        FrozenLongBag bag = FrozenLongBag.copyOf(source);
        assertArrayEquals(original, source);
        verifyLong(original, bag);
        assertEquals(Long.MIN_VALUE, bag.elementAt(0));
        assertEquals(Long.MAX_VALUE, bag.elementAt(bag.distinctSize() - 1));
        Arrays.fill(source, 123);
        long[] exported = bag.distinctToArray();
        assertNotSame(exported, bag.distinctToArray());
        Arrays.fill(exported, 456);
        verifyLong(original, bag);
    }

    @Test void fullWidthPrimitiveCountsMatchIndependentTreeMap() {
        Random random = new Random(0x46524f5a454e4c42L);
        for (int trial = 0; trial < 240; trial++) {
            long[] pool = new long[129];
            for (int index = 0; index < pool.length; index++) {
                pool[index] = random.nextLong();
            }
            pool[0] = Long.MIN_VALUE;
            pool[1] = Long.MAX_VALUE;
            pool[2] = 0;
            pool[3] = -1;
            long[] input = new long[random.nextInt(513)];
            for (int index = 0; index < input.length; index++) {
                input[index] = pool[random.nextInt(pool.length)];
            }
            long[] original = input.clone();
            FrozenLongBag bag = FrozenLongBag.copyOf(input);
            assertArrayEquals(original, input);
            verifyLong(original, bag);
            for (int query = 0; query < 32; query++) {
                long value = random.nextLong();
                int count = 0;
                for (long element : original) {
                    if (element == value) {
                        count++;
                    }
                }
                assertEquals(count, bag.occurrencesOf(value));
            }
        }
    }

    @Test void primitiveDistinctBoundariesAndLargeRuns() {
        for (int distinct : new int[] {0, 1, 63, 64, 65, 127, 128, 129}) {
            long[] input = new long[distinct * 3];
            for (int index = 0; index < input.length; index++) {
                input[index] = ((long) (distinct - 1 - index % distinct) << 36) - (1L << 48);
            }
            verifyLong(input, FrozenLongBag.copyOf(input));
        }
        long[] repeated = new long[100_000];
        Arrays.fill(repeated, Long.MAX_VALUE);
        repeated[0] = Long.MIN_VALUE;
        repeated[1] = 0;
        FrozenLongBag bag = FrozenLongBag.copyOf(repeated);
        assertEquals(100_000, bag.size());
        assertEquals(3, bag.distinctSize());
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, bag.distinctToArray());
        assertEquals(99_998, bag.countAt(2));
        assertEquals(99_998, bag.occurrencesOf(Long.MAX_VALUE));
    }

    @Test void primitiveCallbacksAreDistinctOrderedAndStopAtFirstFailure() {
        long[] input = {9, -5, 4, 9, Long.MIN_VALUE, -5, Long.MAX_VALUE};
        FrozenLongBag bag = FrozenLongBag.copyOf(input);
        RuntimeException failure = new IllegalStateException("distinct callback witness");
        List<Long> visited = new ArrayList<>();
        assertSame(failure, assertThrows(IllegalStateException.class, () -> bag.forEachDistinct(value -> {
            assertTrue(bag.contains(value));
            visited.add(value);
            if (visited.size() == 3) {
                throw failure;
            }
        })));
        assertEquals(List.of(Long.MIN_VALUE, -5L, 4L), visited);
        assertThrows(NullPointerException.class, () -> bag.forEachDistinct(null));
        verifyLong(input, bag);
    }

    @Test void allPrimitiveMutationRequestsRefuse() {
        for (FrozenLongBag bag : List.of(new FrozenLongBag(), FrozenLongBag.copyOf(new long[] {7, 7}))) {
            for (long value : new long[] {Long.MIN_VALUE, 0, 7, Long.MAX_VALUE}) {
                for (int count : new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
                    assertThrows(UnsupportedOperationException.class, () -> bag.addOccurrences(value, count));
                    assertThrows(UnsupportedOperationException.class, () -> bag.removeOccurrences(value, count));
                }
            }
        }
    }

    private static Set<String> publicMethods(Class<?> owner) {
        Set<String> result = new HashSet<>();
        for (Method method : owner.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
                String parameters = Arrays.stream(method.getParameterTypes())
                        .map(Class::getTypeName).collect(Collectors.joining(","));
                result.add(method.getName() + "(" + parameters + ")->"
                        + method.getReturnType().getTypeName()
                        + (Modifier.isStatic(method.getModifiers()) ? ":static" : ""));
            }
        }
        return result;
    }

    @Test void originalPublicDescriptorsAndGenericSurfaceAreRetained() throws ReflectiveOperationException {
        assertEquals(Set.of(
                "scaffoldDescriptor()->java.lang.String:static",
                "copyOf(java.util.Collection)->jdk.internal.m3.collections.FrozenBag:static",
                "size()->int", "distinctSize()->int", "count(java.lang.Object)->int",
                "contains(java.lang.Object)->boolean", "elementAt(int)->java.lang.Object",
                "countAt(int)->int", "elementSet()->java.util.Set",
                "add(java.lang.Object,int)->int", "remove(java.lang.Object,int)->int",
                "setCount(java.lang.Object,int)->int"), publicMethods(FrozenBag.class));
        assertEquals(Set.of(
                "scaffoldDescriptor()->java.lang.String:static",
                "copyOf(long[])->jdk.internal.m3.collections.FrozenLongBag:static",
                "size()->int", "distinctSize()->int", "occurrencesOf(long)->int",
                "contains(long)->boolean", "elementAt(int)->long", "countAt(int)->int",
                "distinctToArray()->long[]", "forEachDistinct(java.util.function.LongConsumer)->void",
                "addOccurrences(long,int)->int", "removeOccurrences(long,int)->boolean"),
                publicMethods(FrozenLongBag.class));
        assertEquals(1, FrozenBag.class.getConstructors().length);
        assertEquals(0, FrozenBag.class.getConstructors()[0].getParameterCount());
        assertEquals(1, FrozenLongBag.class.getConstructors().length);
        assertEquals(0, FrozenLongBag.class.getConstructors()[0].getParameterCount());
        assertTrue(Modifier.isFinal(FrozenBag.class.getModifiers()));
        assertTrue(Modifier.isFinal(FrozenLongBag.class.getModifiers()));
        assertEquals("E", FrozenBag.class.getTypeParameters()[0].getName());
        assertEquals("jdk.internal.m3.collections.FrozenBag<E>",
                FrozenBag.class.getMethod("copyOf", Collection.class).getGenericReturnType().getTypeName());
        assertEquals("M3FrozenBag|FROZEN_BAG|OBJECT|FROZEN|Object[] elementLane;int[] countLane;int[] hashIndexLane",
                FrozenBag.scaffoldDescriptor());
        assertEquals("M3FrozenLongBag|FROZEN_LONG_BAG|LONG|FROZEN|long[] elementLane;int[] countLane",
                FrozenLongBag.scaffoldDescriptor());
    }
}
