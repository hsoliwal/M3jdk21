/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * @test
 * @summary FrozenSet exact-cardinality owner array and untrusted Collection fallback
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main/othervm FrozenSetExactLanesTest
 */
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.SplittableRandom;
import jdk.internal.m3.collections.FrozenSet;

public final class FrozenSetExactLanesTest {
    private static long checks;

    public static void main(String[] args) {
        for (int size : new int[] {0, 1, 2, 7, 8, 9, 13, 14, 20, 21, 64, 256, 4096, 65536}) {
            checkSet(populated(new LinkedHashSet<>(), size));
            checkSet(populated(new HashSet<>(), size));
        }
        SplittableRandom random = new SplittableRandom(0xF20A56D09L);
        for (int round = 0; round < 180; round++) {
            int n = random.nextInt(900);
            ArrayList<Integer> duplicates = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                duplicates.add(random.nextInt(-100, 101));
            }
            checkSet(new UntrustedSizeCollection<>(duplicates));
        }
        Set<Integer> hashSubclass = new HashSet<>() {
            @Override public int size() {
                throw new AssertionError("HashSet subclass size() queried");
            }
        };
        checkSet(populated(hashSubclass, 33));
        Set<Integer> linkedSubclass = new LinkedHashSet<>() {
            @Override public int size() {
                throw new AssertionError("LinkedHashSet subclass size() queried");
            }
        };
        checkSet(populated(linkedSubclass, 33));
        checkIdentityAndCollisions();
        checkNullsAndMutators();
        System.out.println("FrozenSetExactLanesTest PASS: " + checks + " assertions");
    }

    private static Set<Integer> populated(Set<Integer> set, int size) {
        for (int i = 0; i < size; i++) {
            set.add(Integer.rotateLeft(i * 31, 7));
        }
        return set;
    }

    private static <E> void checkSet(Collection<E> source) {
        LinkedHashSet<E> reference = new LinkedHashSet<>();
        for (E value : source) {
            reference.add(value);
        }
        FrozenSet<E> actual = FrozenSet.copyOf(source);
        eq(actual.size(), reference.size(), "size");
        eq(actual.isEmpty(), reference.isEmpty(), "empty");
        check(actual.asSet().equals(reference), "set equals");
        check(reference.equals(actual.asSet()), "reverse set equals");
        eq(actual.asSet().hashCode(), reference.hashCode(), "hash code");
        eq(actual.contains(null), false, "null contains");
        eq(actual.asSet().contains(null), false, "null facade contains");
        eq(actual.asSet().size(), reference.size(), "facade size");
        Object[] expected = reference.toArray();
        Object[] exported = actual.toArray();
        check(Arrays.equals(exported, expected), "detached array equality");
        check(exported != actual.toArray(), "fresh array export");
        eq(actual.asSet().spliterator().characteristics()
                        & (Spliterator.ORDERED | Spliterator.DISTINCT
                        | Spliterator.IMMUTABLE | Spliterator.NONNULL),
                Spliterator.ORDERED | Spliterator.DISTINCT
                        | Spliterator.IMMUTABLE | Spliterator.NONNULL, "spliterator traits");
        Iterator<E> iter = actual.iterator();
        Iterator<E> facade = actual.asSet().iterator();
        for (int i = 0; i < expected.length; i++) {
            E representative = actual.get(i);
            check(representative == expected[i], "representative identity");
            check(representative == iter.next(), "direct iterator identity");
            check(representative == facade.next(), "facade iterator identity");
            check(actual.contains(representative), "known element");
            check(actual.asSet().contains(representative), "known facade element");
            if (i < expected.length && i % 257 == 0) {
                Object[] modified = actual.toArray();
                modified[i] = null;
                check(actual.get(i) == representative, "detached toArray");
            }
        }
        check(!iter.hasNext(), "iterator exhausted");
        check(!facade.hasNext(), "facade exhausted");
        if (reference.isEmpty()) {
            fails(NoSuchElementException.class, iter::next, "empty iterator");
        }
        fails(IndexOutOfBoundsException.class, () -> actual.get(-1), "negative ordinal");
        fails(IndexOutOfBoundsException.class,
                () -> actual.get(actual.size()), "high ordinal");
    }

    private record Collision(int id) {
        @Override public int hashCode() { return id & 15; }
    }

    private static void checkIdentityAndCollisions() {
        ArrayList<Collision> input = new ArrayList<>();
        Collision first = new Collision(1);
        input.add(first);
        input.add(new Collision(1));
        for (int i = 0; i < 1500; i++) {
            input.add(new Collision(i));
        }
        FrozenSet<Collision> frozen = FrozenSet.copyOf(input);
        eq(frozen.size(), 1500, "colliding distinct size");
        check(frozen.get(0) == first, "first representative retained");
        for (int i = 0; i < 1500; i++) {
            check(frozen.contains(new Collision(i)), "colliding key query");
        }
        input.clear();
        eq(frozen.size(), 1500, "detached collection");
        check(frozen.get(0) == first, "detached representative");
    }

    private static void checkNullsAndMutators() {
        fails(NullPointerException.class, () -> FrozenSet.copyOf(null), "null collection");
        fails(NullPointerException.class, () -> FrozenSet.copyOf(Arrays.asList("a", null)),
                "null element");
        FrozenSet<String> empty = FrozenSet.copyOf(List.of());
        FrozenSet<String> singleton = FrozenSet.copyOf(List.of("a"));
        for (FrozenSet<String> set : List.of(empty, singleton)) {
            fails(UnsupportedOperationException.class, () -> set.add(null), "owner add");
            fails(UnsupportedOperationException.class, () -> set.remove("missing"), "owner remove");
            fails(UnsupportedOperationException.class, () -> set.asSet().add("x"), "facade add");
            fails(UnsupportedOperationException.class,
                    () -> set.asSet().remove("missing"), "facade remove");
            fails(UnsupportedOperationException.class, () -> set.asSet().clear(), "facade clear");
        }
        check(FrozenSet.scaffoldDescriptor().startsWith("M3FrozenSet|"), "descriptor");
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
    private static void fails(Class<? extends Throwable> type, Runnable operation, String context) {
        try {
            operation.run();
        } catch (Throwable ex) {
            check(type.isInstance(ex), context + " wrong: " + ex);
            return;
        }
        throw new AssertionError(context + ": exception not thrown");
    }

    private static final class UntrustedSizeCollection<E> extends AbstractCollection<E> {
        private final Collection<E> source;
        UntrustedSizeCollection(Collection<E> source) { this.source = source; }
        @Override public int size() {
            throw new AssertionError("untrusted Collection.size() queried");
        }
        @Override public Iterator<E> iterator() { return source.iterator(); }
    }
}
