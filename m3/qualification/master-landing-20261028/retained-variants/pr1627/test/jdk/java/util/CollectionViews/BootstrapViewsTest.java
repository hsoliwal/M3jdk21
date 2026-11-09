/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary CollectionViews executes from java.base without a library or extra exports
 * @run main BootstrapViewsTest
 */
import java.util.*;
import java.util.function.Predicate;

public final class BootstrapViewsTest {
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("bootstrap check=" + checks);
    }
    public static void main(String[] args) throws Exception {
        check(CollectionViews.class.getModule() == Object.class.getModule());
        check(CollectionViews.class.getClassLoader() == null);
        check(CollectionViews.class.getName().equals("java.util.CollectionViews"));
        String first = new String("same"), second = new String("same");
        List<String> input = new ArrayList<>(List.of(first, second, "other"));
        List<String> copy = CollectionViews.filteredSortedSnapshot(input,
                x -> x.equals("same"), Comparator.naturalOrder());
        check(copy.size() == 2 && copy.get(0) == first && copy.get(1) == second);
        check(first != second); // Value equality never folds Java reference identity.
        input.add("same"); check(copy.size() == 2);
        try { copy.add("x"); throw new AssertionError(); }
        catch (UnsupportedOperationException expected) { checks++; }
        // Real dynamic array, one source traversal, no count/predicate prepass.
        int[] calls = {0};
        Collection<Integer> rows = new AbstractCollection<>() {
            public int size() { throw new AssertionError("eager size"); }
            public Iterator<Integer> iterator() {
                return new Iterator<>() {
                    private int next;
                    public boolean hasNext() { return next < 65537; }
                    public Integer next() {
                        if (!hasNext()) throw new NoSuchElementException();
                        return next++;
                    }
                };
            }
        };
        List<Integer> selected = CollectionViews.filteredSnapshot(rows,
                x -> { calls[0]++; return (x & 1023) == 0; });
        check(calls[0] == 65537 && selected.size() == 65);
        for (int i = 0; i < selected.size(); i++) check(selected.get(i) == i * 1024);
        NavigableSet<Integer> tree = new TreeSet<>(List.of(1, 2, 3, 4, 5));
        Collection<Integer> erased = CollectionViews.filtered((Collection<Integer>) tree, x -> (x & 1) == 0);
        check(erased instanceof NavigableSet<?>);
        check(erased.getClass().getModule() == Object.class.getModule());
        erased.clear(); check(tree.equals(new TreeSet<>(List.of(1,3,5))));
        Collection<Integer> deep = List.of(42);
        Predicate<Integer> yes = x -> true;
        for (int i = 0; i < 20000; i++) deep = CollectionViews.filtered(deep, yes);
        check(deep.iterator().next() == 42);
        System.out.println("BootstrapViewsTest checks=" + checks + " module=java.base loader=bootstrap PASS");
    }
}
