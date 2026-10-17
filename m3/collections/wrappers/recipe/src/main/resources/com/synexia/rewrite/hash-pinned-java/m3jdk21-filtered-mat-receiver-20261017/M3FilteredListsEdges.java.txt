// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Targeted failure/edge witnesses for mutable and read-only List filtering. */
public final class M3FilteredListsEdges {
    private static long checks;
    private M3FilteredListsEdges() { }
    private static void eq(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }
    private static void refuses(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable ex) { if (!expected.isInstance(ex)) throw new AssertionError(ex); checks++; return; }
        throw new AssertionError("expected " + expected.getSimpleName());
    }
    private static void readOnlyNoOps() {
        List<Integer> source = new ArrayList<>();
        List<Integer> view = M3FilteredLists.readOnlyFiltered(source, x -> true);
        refuses(UnsupportedOperationException.class, view::removeFirst);
        refuses(UnsupportedOperationException.class, view::removeLast);
        refuses(UnsupportedOperationException.class, () -> view.addFirst(1));
        refuses(UnsupportedOperationException.class, () -> view.addLast(1));
        refuses(UnsupportedOperationException.class, () -> view.addAll(List.of()));
        refuses(UnsupportedOperationException.class, () -> view.removeIf(x -> false));
        refuses(UnsupportedOperationException.class, () -> view.sort(Integer::compare));
        refuses(UnsupportedOperationException.class, () -> view.replaceAll(x -> x));
        refuses(UnsupportedOperationException.class, view::clear);
        refuses(UnsupportedOperationException.class, () -> view.reversed().add(2));
        refuses(UnsupportedOperationException.class, () -> view.reversed().removeFirst());
    }
    private static void iteratorSemantics() {
        List<Integer> source = new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7));
        AtomicInteger predicates = new AtomicInteger();
        Predicate<Integer> predicate = x -> { predicates.incrementAndGet(); return x%2==0; };
        List<Integer> view = M3FilteredLists.filtered(source,predicate);
        ListIterator<Integer> it=view.listIterator(1);
        predicates.set(0);
        eq(true,it.hasPrevious());
        int seen=predicates.get();
        eq(true,it.hasPrevious());eq(seen,predicates.get()); // cached, not reevaluated
        eq(2,it.previous());
        eq(true,it.hasNext());
        it.set(8);eq(List.of(1,8,3,4,5,6,7),source);
        eq(8,it.next());
        eq(true,it.hasNext());it.remove();eq(List.of(1,3,4,5,6,7),source);
        eq(4,it.next());
        it.add(10);eq(List.of(1,3,4,10,5,6,7),source);
        eq(10,it.previous());
        it.remove();eq(List.of(1,3,4,5,6,7),source);
        ListIterator<Integer> failed = M3FilteredLists.filtered(source, x -> {
            if (Objects.equals(x,7)) throw new IllegalArgumentException("predicate failure");
            return x%2==0;
        }).listIterator();
        eq(4, failed.next());
        eq(6, failed.next());
        refuses(IllegalArgumentException.class,failed::hasNext);
        refuses(IllegalStateException.class,failed::hasNext);
        ListIterator<Integer> external = view.listIterator();
        source.add(12);
        refuses(ConcurrentModificationException.class,external::next);
        eq(false,view.spliterator().hasCharacteristics(Spliterator.SIZED));
    }
    private static void boundaries() {
        List<Integer> source = new ArrayList<>(Arrays.asList(1,2,3,4,5));
        List<Integer> view = M3FilteredLists.filtered(source,x->x%2==0);
        eq(List.of(2,4),view);
        view.add(2,6);eq(List.of(1,2,3,4,5,6),source);
        view.add(1,8);eq(List.of(1,2,3,8,4,5,6),source);
        eq(List.of(2,8,4,6),view);
        eq(List.of(8,4),view.subList(1,3));
        List<Integer> reversed=view.reversed();
        reversed.set(0,12);eq(List.of(1,2,3,8,4,5,12),source);
        reversed.add(0,18);eq(List.of(1,2,3,8,4,5,12,18),source);
        eq(List.of(18,12,4,8,2),reversed);
        Collection<Integer> many = Arrays.asList(18,null);  // reject null for predicate requiring integer
        List<Integer> old = new ArrayList<>(source);
        refuses(NullPointerException.class,()->view.addAll(many));
        eq(old,source);
    }
    public static void main(String[] args) {
        readOnlyNoOps();iteratorSemantics();boundaries();
        System.out.println("PASS M3FilteredListsEdges checks="+checks);
    }
}
