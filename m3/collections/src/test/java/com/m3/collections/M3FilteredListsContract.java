// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

/** A deterministic reference-model oracle. No test framework dependency. */
public final class M3FilteredListsContract {
    private static long checks;
    private static long operations;
    private static final Predicate<Integer> ACCEPT = n -> n == null || n % 3 == 0;
    private M3FilteredListsContract() { }

    private static void eq(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
    }
    private static void truth(boolean actual, String label) { eq(true, actual, label); }
    private static void throwsType(Class<? extends Throwable> type, Runnable action, String label) {
        try { action.run(); }
        catch (Throwable failure) {
            truth(type.isInstance(failure), label + ": " + failure);
            return;
        }
        throw new AssertionError(label + ": missing " + type.getSimpleName());
    }
    private static ArrayList<Integer> selected(List<Integer> list) {
        ArrayList<Integer> result = new ArrayList<>();
        for (Integer item : list) if (ACCEPT.test(item)) result.add(item);
        return result;
    }
    private static int physical(List<Integer> source, int visibleIndex, boolean allowEnd) {
        int seen = 0;
        for (int i = 0; i < source.size(); i++) {
            if (ACCEPT.test(source.get(i)) && seen++ == visibleIndex) return i;
        }
        if (allowEnd && seen == visibleIndex) return source.size();
        throw new IndexOutOfBoundsException();
    }
    private static Integer accepted(Random random) {
        int i = random.nextInt(16);
        return i == 0 ? null : (random.nextInt(21) - 10) * 3;
    }
    private static void verify(List<Integer> source, List<Integer> oracle, List<Integer> view, String label) {
        eq(oracle, source, label + " source mutation parity");
        ArrayList<Integer> expected = selected(oracle);
        eq(expected, new ArrayList<>(view), label + " visible sequence");
        eq(expected.size(), view.size(), label + " size");
        eq(expected.isEmpty(), view.isEmpty(), label + " empty");
        eq(expected, view, label + " list equality");
        eq(expected.hashCode(), view.hashCode(), label + " list hash");
        eq(expected.reversed(), new ArrayList<>(view.reversed()), label + " reversed order");
        for (int i = 0; i < expected.size(); i++) eq(expected.get(i), view.get(i), label + " indexed " + i);
        eq(expected.indexOf(null), view.indexOf(null), label + " indexOf(null)");
        eq(expected.lastIndexOf(null), view.lastIndexOf(null), label + " lastIndexOf(null)");
        eq(expected, view.stream().toList(), label + " stream order");
        truth(view.spliterator().hasCharacteristics(java.util.Spliterator.ORDERED), label + " ORDERED");
        truth(!view.spliterator().hasCharacteristics(java.util.Spliterator.SORTED), label + " !SORTED");
    }

    private static void edgeContracts() {
        ArrayList<Integer> source = new ArrayList<>(Arrays.asList(null, 1, 3, 4, 6, 6, 7, 9, 2));
        List<Integer> view = M3FilteredLists.filtered(source, ACCEPT);
        eq(Arrays.asList(null, 3, 6, 6, 9), view, "initial null/duplicate order");
        List<Integer> nested = M3FilteredLists.filtered(view, x -> x != null && x >= 6);
        eq(List.of(6, 6, 9), nested, "nested select");
        nested.remove(0);eq(Arrays.asList(null,1,3,4,6,7,9,2),source,"nested indexed remove first duplicate");
        ListIterator<Integer> iter = view.listIterator();
        eq(null, iter.next(), "iterator null delivered");
        truth(iter.hasNext(), "lookahead after next");
        iter.remove();eq(Arrays.asList(1,3,4,6,7,9,2), source, "iterator remove uses delivered, not prefetched value");
        eq(3, iter.next(), "after remove same logical next");
        truth(iter.hasPrevious(), "lookbehind after next");
        iter.set(12);eq(Arrays.asList(1,12,4,6,7,9,2), source, "iterator set after lookbehind");
        throwsType(IllegalArgumentException.class, () -> iter.set(13), "iterator set preflight");
        iter.add(15);eq(Arrays.asList(1,12,15,4,6,7,9,2),source,"iterator add after set");
        eq(2,iter.nextIndex(),"iterator add advances visible cursor");
        eq(15,iter.previous(),"previous after add");
        truth(iter.hasNext(), "lookahead after previous");
        iter.remove();eq(Arrays.asList(1,12,4,6,7,9,2),source,"previous/remove after lookahead");
        throwsType(IllegalStateException.class,iter::remove,"iterator double remove");
        throwsType(IllegalArgumentException.class,()->view.add(4),"add refuses hidden");
        var before = List.copyOf(source);
        throwsType(IllegalArgumentException.class,()->view.addAll(Arrays.asList(18,19,21)),"addAll preflight");
        eq(before,source,"rejected addAll no prefix mutation");
        var readOnly = M3FilteredLists.readOnlyFiltered(source, ACCEPT);
        throwsType(UnsupportedOperationException.class,()->readOnly.addAll(List.of()),"read-only empty addAll");
        throwsType(UnsupportedOperationException.class,()->readOnly.removeIf(x->false),"read-only no-op removeIf");
        throwsType(UnsupportedOperationException.class,readOnly::clear,"read-only clear");
        throwsType(UnsupportedOperationException.class,()->readOnly.reversed().add(99),"read-only reversed add");
        source.add(21);truth(readOnly.contains(21),"read-only is live");
        List<Integer> window = view.subList(1,3);
        eq(List.of(6,9),window,"physical bounded subList");
        window.add(0,24);truth(view.contains(24),"subList write through");
        eq(List.of(24,6,9),window,"subList order");
        window.clear();eq(Arrays.asList(1,12,4,7,2,21),source,"bounded clear hidden conserved");
        eq(List.of(12,21),view,"live backing changes after sublist mutation");
        var sorted = new ArrayList<>(Arrays.asList(8, 9, 6, 7, 3, 4, 12, 1));
        var selected = M3FilteredLists.filtered(sorted, ACCEPT);
        selected.sort(Integer::compare);
        eq(Arrays.asList(8,3,6,7,9,4,12,1),sorted,"sort only selected rows");
        selected.replaceAll(x->x+3);
        eq(Arrays.asList(8,6,9,7,12,4,15,1),sorted,"replaceAll only selected rows");
        throwsType(IllegalArgumentException.class,()->selected.replaceAll(x->x+1),"replaceAll predicate validation");
        List<Integer> linked = new LinkedList<>(Arrays.asList(1,3,4,6,9,2));
        List<Integer> lview = M3FilteredLists.filtered(linked, ACCEPT);
        eq(List.of(3,6,9),lview,"LinkedList selection");
        ListIterator<Integer> it = lview.listIterator(2);
        eq(6,it.previous(),"LinkedList backwards");
        eq(6,it.next(),"LinkedList direction turnaround");
        it.remove();eq(List.of(3,9),lview,"LinkedList iterator remove");
        throwsType(IndexOutOfBoundsException.class,()->view.get(-1),"get negative");
        throwsType(IndexOutOfBoundsException.class,()->view.get(300),"get past end");
        throwsType(IndexOutOfBoundsException.class,()->view.listIterator(-1),"iterator negative");
        throwsType(IndexOutOfBoundsException.class,()->view.subList(3,2),"bad range");
        operations += 52;
    }
    private static void randomized() {
        for (int run=0;run<18;run++) {
            Random random = new Random(713331L + run*9929L);
            ArrayList<Integer> source = new ArrayList<>();
            ArrayList<Integer> oracle = new ArrayList<>();
            List<Integer> view = M3FilteredLists.filtered(source, ACCEPT);
            for (int step=0;step<1800;step++) {
                String label = "run="+run+" step="+step;
                int mode = random.nextInt(13);
                int visible=selected(oracle).size();
                int index = visible==0?0:random.nextInt(visible);
                switch(mode) {
                    case 0, 1 -> {
                        Integer value = random.nextBoolean()?accepted(random):Integer.valueOf(random.nextInt(21)*3+1);
                        int position = random.nextInt(source.size()+1);
                        source.add(position,value);oracle.add(position,value);
                    }
                    case 2 -> {
                        Integer value=accepted(random); int pos=random.nextInt(visible+1);
                        view.add(pos,value);oracle.add(physical(oracle,pos,true),value);
                    }
                    case 3 -> {
                        if(visible!=0){Integer value=accepted(random);eq(oracle.set(physical(oracle,index,false),value),view.set(index,value),label+" set");}
                    }
                    case 4 -> {
                        if(visible!=0)eq(oracle.remove(physical(oracle,index,false)),view.remove(index),label+" remove");
                    }
                    case 5 -> {
                        int pos=random.nextInt(visible+1);
                        List<Integer> data=Arrays.asList(accepted(random),accepted(random));
                        eq(true,view.addAll(pos,data),label+" addAll");oracle.addAll(physical(oracle,pos,true),data);
                    }
                    case 6 -> {
                        int pos=random.nextInt(visible+1);ListIterator<Integer> it=view.listIterator(pos);
                        eq(pos,it.nextIndex(),label+" nextIndex");eq(pos-1,it.previousIndex(),label+" previousIndex");
                        if(pos<visible){truth(it.hasNext(),label+" hasNext");eq(selected(oracle).get(pos),it.next(),label+" iterator next");
                            truth(it.hasPrevious(),label+" hasPrevious");it.remove();oracle.remove(physical(oracle,pos,false));}
                    }
                    case 7 -> {
                        int pos=random.nextInt(visible+1);ListIterator<Integer> it=view.listIterator(pos);
                        if(pos>0){truth(it.hasPrevious(),label+" hasPrevious");eq(selected(oracle).get(pos-1),it.previous(),label+" iterator prev");
                            truth(it.hasNext(),label+" hasNext");it.remove();oracle.remove(physical(oracle,pos-1,false));}
                    }
                    case 8 -> {
                        if(visible>0){Integer old=selected(oracle).get(index);
                            eq(true,view.remove(old),label+" remove object");
                            oracle.remove(old);}
                    }
                    case 9 -> {
                        var expected=selected(oracle); int from=visible==0?0:random.nextInt(visible+1);int to=from+random.nextInt(visible-from+1);
                        eq(expected.subList(from,to),view.subList(from,to),label+" sublist");
                        eq(expected.subList(from,to).reversed(),view.subList(from,to).reversed(),label+" reverse sublist");
                    }
                    case 10 -> {
                        int candidate=random.nextInt(100)*3+1;
                        throwsType(IllegalArgumentException.class,()->view.add(candidate),label+" reject add");
                    }
                    case 11 -> {
                        if(source.size()>120){view.clear();oracle.removeIf(ACCEPT);}
                        else {int candidate=random.nextInt(24)*3+1;eq(false,view.remove(Integer.valueOf(candidate)),label+" hidden removal");}
                    }
                    case 12 -> {
                        eq(selected(oracle),view.parallelStream().toList(),label+" stream stable order");
                    }
                    default -> throw new AssertionError();
                }
                operations++;
                verify(source,oracle,view,label);
            }
        }
    }
    public static void main(String[] args) {
        edgeContracts();randomized();
        System.out.println("PASS M3FilteredListsContract operations="+operations+" checks="+checks);
    }
}
