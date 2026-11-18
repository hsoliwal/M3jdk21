/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary FrozenList exact owner array, callback-neutral fallback and immutable views
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenListExactLanesTest
 */
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SplittableRandom;
import jdk.internal.m3.collections.FrozenList;

public final class FrozenListExactLanesTest {
    private static long checks;

    public static void main(String[] args) {
        for (int n : new int[] {0, 1, 2, 7, 8, 9, 10, 11, 13, 20, 21, 64, 256, 4096, 65536}) {
            ArrayList<Integer> array = new ArrayList<>();
            LinkedList<Integer> linked = new LinkedList<>();
            Set<Integer> hashed = new HashSet<>();
            Set<Integer> ordered = new LinkedHashSet<>();
            for (int i = 0; i < n; i++) {
                int value = Integer.rotateLeft(i * 31, 9);
                array.add(value);
                linked.add(value);
                hashed.add(value);
                ordered.add(value);
            }
            verify(array);
            verify(linked);
            verify(hashed);
            verify(ordered);
            if (n <= 4096) {
                verify(new UntrustedSizeCollection<>(array));
            }
        }
        SplittableRandom random = new SplittableRandom(0xF20A56D0BL);
        for (int round = 0; round < 180; round++) {
            int n = random.nextInt(900);
            ArrayList<Integer> input = new ArrayList<>();
            for (int i = 0; i < n; i++) input.add(random.nextInt(-100, 101));
            verify(input);
            verify(new UntrustedSizeCollection<>(input));
        }
        ArrayList<Integer> arraySubclass = new ArrayList<>() {
            @Override public int size() {
                throw new AssertionError("ArrayList subclass.size() queried");
            }
        };
        arraySubclass.add(1);
        verify(arraySubclass);
        LinkedList<Integer> linkedSubclass = new LinkedList<>() {
            @Override public int size() {
                throw new AssertionError("LinkedList subclass.size() queried");
            }
        };
        linkedSubclass.add(2);
        verify(linkedSubclass);
        HashSet<Integer> hashSubclass = new HashSet<>() {
            @Override public int size() {
                throw new AssertionError("HashSet subclass.size() queried");
            }
        };
        hashSubclass.add(3);
        verify(hashSubclass);
        LinkedHashSet<Integer> orderedSubclass = new LinkedHashSet<>() {
            @Override public int size() {
                throw new AssertionError("LinkedHashSet subclass.size() queried");
            }
        };
        orderedSubclass.add(4);
        verify(orderedSubclass);
        verifyNullOwnershipAndMutators();
        System.out.println("FrozenListExactLanesTest PASS: " + checks + " assertions");
    }

    private static <E> void verify(Collection<E> source) {
        ArrayList<E> expected = new ArrayList<>();
        for (E element : source) expected.add(element);
        FrozenList<E> frozen = FrozenList.copyOf(source);
        eq(frozen.size(), expected.size(), "size");
        eq(frozen.isEmpty(), expected.isEmpty(), "empty");
        check(frozen.asList().equals(expected), "list equality");
        check(expected.equals(frozen.asList()), "reverse list equality");
        eq(frozen.asList().hashCode(), expected.hashCode(), "hash code");
        eq(frozen.indexOf(null), -1, "null index");
        eq(frozen.contains(null), false, "null contains");
        Object[] copy = frozen.toArray();
        check(Arrays.equals(copy, expected.toArray()), "array copy contents");
        check(copy != frozen.toArray(), "array clone");
        Iterator<E> direct = frozen.iterator();
        Iterator<E> facade = frozen.asList().iterator();
        List<E> reversed = frozen.asList().reversed();
        for (int i = 0; i < expected.size(); i++) {
            E item = expected.get(i);
            check(item == frozen.get(i), "shallow element identity");
            check(item == facade.next(), "facade iterator identity");
            check(item == direct.next(), "direct iterator identity");
            check(item == reversed.get(expected.size() - i - 1), "reversed view identity");
            if (expected.size() <= 512 || i % 257 == 0) {
                check(frozen.contains(item), "contains item");
                eq(frozen.indexOf(item), expected.indexOf(item),
                        "first index including duplicates");
            }
            if (i % 257 == 0) {
                Object[] detached = frozen.toArray();
                detached[i] = null;
                check(frozen.get(i) == item, "export cannot mutate");
            }
        }
        check(!direct.hasNext(), "iterator exhausted");
        check(!facade.hasNext(), "facade exhausted");
        check(reversed.reversed().equals(expected), "reverse involution");
        int midpoint = expected.size() / 2;
        check(frozen.asList().subList(midpoint, expected.size())
                .equals(expected.subList(midpoint, expected.size())), "sublist");
        ListIterator<E> iterator = frozen.asList().listIterator(expected.size());
        for (int i = expected.size() - 1; i >= 0; i--) {
            check(iterator.previous() == expected.get(i), "reverse cursor");
        }
        if (expected.isEmpty()) {
            fails(NoSuchElementException.class, direct::next, "empty next");
        }
        fails(IndexOutOfBoundsException.class, () -> frozen.get(-1), "negative ordinal");
        fails(IndexOutOfBoundsException.class,
                () -> frozen.get(expected.size()), "high ordinal");
    }

    private static void verifyNullOwnershipAndMutators() {
        fails(NullPointerException.class, () -> FrozenList.copyOf(null), "null source");
        fails(NullPointerException.class,
                () -> FrozenList.copyOf(Arrays.asList("a", null)), "null element");
        ArrayList<String> original = new ArrayList<>();
        String representative = new String("v");
        original.add(representative);
        original.add(representative);
        FrozenList<String> frozen = FrozenList.copyOf(original);
        original.clear();
        eq(frozen.size(), 2, "source detach");
        check(frozen.get(0) == representative && frozen.get(1) == representative,
                "duplicate identity");
        for (FrozenList<String> item : List.of(FrozenList.<String>copyOf(List.of()), frozen)) {
            fails(UnsupportedOperationException.class, () -> item.add(null), "owner add");
            fails(UnsupportedOperationException.class, () -> item.set(0, null), "owner set");
            List<String> view = item.asList();
            fails(UnsupportedOperationException.class, () -> view.add(null), "view add");
            fails(UnsupportedOperationException.class, () -> view.clear(), "view clear");
            fails(UnsupportedOperationException.class,
                    () -> view.reversed().clear(), "reversed clear");
            fails(UnsupportedOperationException.class,
                    () -> view.replaceAll(x -> x), "view replaceAll");
            fails(UnsupportedOperationException.class,
                    () -> view.listIterator().remove(), "iterator remove");
        }
        check(FrozenList.scaffoldDescriptor().startsWith("M3FrozenList|"), "descriptor");
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
    private static void fails(Class<? extends Throwable> type, Runnable action, String context) {
        try {
            action.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), context + " wrong " + failure);
            return;
        }
        throw new AssertionError(context + ": expected exception");
    }
    private static final class UntrustedSizeCollection<E> extends AbstractCollection<E> {
        private final Collection<E> source;
        UntrustedSizeCollection(Collection<E> source) { this.source = source; }
        @Override public int size() {
            throw new AssertionError("custom Collection.size() called");
        }
        @Override public Iterator<E> iterator() { return source.iterator(); }
    }
}
