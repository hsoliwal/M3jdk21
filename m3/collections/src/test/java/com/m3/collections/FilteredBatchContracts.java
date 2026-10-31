/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/** Real JDK owners. A changing-input fixture is a counterexample, not a concurrency guarantee. */
public final class FilteredBatchContracts {
    private static int checks;
    private FilteredBatchContracts() { }
    private static void check(boolean ok, String name) { checks++; if (!ok) throw new AssertionError(name); }
    private static void eq(Object a, Object b, String name) { check(Objects.equals(a,b),name+" expected="+a+" actual="+b); }
    private static Throwable rejects(Class<? extends Throwable> type, Runnable task) {
        checks++;
        try { task.run(); } catch (RuntimeException | Error e) {
            if (type.isInstance(e)) return e;
            throw new AssertionError("wrong failure",e);
        }
        throw new AssertionError("missing failure "+type.getName());
    }
    /** A read-only iterator snapshot whose owner changes at end-of-traversal. */
    private static final class ChangingInput<E> extends AbstractCollection<E> {
        private final List<E> first;
        private final List<E> later;
        private boolean changed;
        private int opened;
        private int sizes;
        ChangingInput(List<E> first,List<E> later) { this.first=first;this.later=later; }
        @Override public int size() { sizes++;return (changed?later:first).size(); }
        @Override public Iterator<E> iterator() {
            opened++;Iterator<E> current=(changed?later:first).iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() { boolean more=current.hasNext();if(!more)changed=true;return more; }
                @Override public E next() { return current.next(); }
            };
        }
    }
    public static void changingSource(boolean candidate) {
        for(int kind=0;kind<5;kind++) {
            Collection<Integer> backing=switch(kind) {
                case 0 -> new ArrayList<>();
                case 1 -> new LinkedHashSet<>();
                case 2 -> new TreeSet<>();
                case 3 -> new TreeSet<Integer>().subSet(-10,true,10,true);
                default -> new TreeSet<Integer>().descendingSet();
            };
            var input=new ChangingInput<>(List.of(2,4),List.of(3,5));
            Collection<Integer> view=M3CollectionViews.filtered(backing,x->x%2==0);
            check(view.addAll(input),"changing input added");
            if(!candidate) {
                check(backing.contains(3)&&backing.contains(5),"BASELINE_UNVALIDATED_SECOND_READ");
                System.out.println("BASELINE_UNVALIDATED_SECOND_READ owner="+kind+" stored="+backing);
            } else {
                check(backing.contains(2)&&backing.contains(4)&&!backing.contains(3)&&!backing.contains(5),"VALIDATED_REFERENCES_ONLY");
                eq(1,input.opened,"one input cursor");eq(0,input.sizes,"no size prepass");
            }
        }
    }
    private static void failureAndAliasing() {
        var owner=new ArrayList<Integer>(List.of(1,2));
        var calls=new ArrayList<Integer>();
        var view=M3CollectionViews.filtered(owner,x->{calls.add(x);return x%2==0;});
        rejects(IllegalArgumentException.class,()->view.addAll(List.of(4,5,6)));
        eq(List.of(4,5),calls,"short-circuit admission");eq(List.of(1,2),owner,"rejection before owner mutation");
        var sentinel=new IllegalStateException("predicate-sentinel");
        var failed=M3CollectionViews.filtered(owner,x->{if(x==6)throw sentinel;return true;});
        check(rejects(IllegalStateException.class,()->failed.addAll(List.of(4,6)))==sentinel,"predicate failure identity");
        eq(List.of(1,2),owner,"predicate failure before write");
        var inputFailure=new AssertionError("source-sentinel");
        Collection<Integer> broken=new AbstractCollection<>() {
            @Override public int size(){throw new AssertionError("size not needed");}
            @Override public Iterator<Integer> iterator(){return new Iterator<>() {
                private boolean first=true;
                @Override public boolean hasNext(){if(!first)throw inputFailure;return true;}
                @Override public Integer next(){first=false;return 4;}
            };}
        };
        check(rejects(AssertionError.class,()->view.addAll(broken))==inputFailure,"input failure identity");
        eq(List.of(1,2),owner,"iterator failure before owner write");
        rejects(NullPointerException.class,()->view.addAll(null));
        eq(false,view.addAll(List.of()),"mutable empty batch");
        var readOnly=M3CollectionViews.filtered(Collections.unmodifiableCollection(owner),x->true);
        rejects(UnsupportedOperationException.class,()->readOnly.addAll(List.of()));
        rejects(UnsupportedOperationException.class,()->readOnly.addAll(List.of(2)));
        var self=new ArrayList<Integer>(List.of(2,4));var selfView=M3CollectionViews.filtered(self,x->true);
        check(selfView.addAll(selfView),"self-view admission");eq(List.of(2,4,2,4),self,"self snapshot order");
        var nulls=new ArrayList<Integer>();var nullable=M3CollectionViews.filtered(nulls,x->x==null||x%2==0);
        nullable.addAll(Arrays.asList(null,2,null,2));eq(Arrays.asList(null,2,null,2),nulls,"nulls duplicates retained");
        check(nulls.get(1)==nulls.get(3),"reference duplicate retained");
        var roFailure=new IllegalArgumentException("owner-sentinel");
        Collection<Integer> rejectingOwner=new ArrayList<>() {
            private static final long serialVersionUID=1L;
            @Override public boolean addAll(Collection<? extends Integer> e){throw roFailure;}
        };
        check(rejects(IllegalArgumentException.class,()->M3CollectionViews.filtered(rejectingOwner,x->true).addAll(List.of(2)))==roFailure,"backing error identity");
    }
    private static void nestedAndSorted() {
        List<String> order=new ArrayList<>();
        Collection<Integer> view=M3CollectionViews.filtered(new ArrayList<>(),x->{order.add("a"+x);return x>=0;});
        view=M3CollectionViews.filtered(view,x->{order.add("b"+x);return x%2==0;});
        Collection<Integer> nested=view;
        rejects(IllegalArgumentException.class,()->nested.addAll(List.of(2,-1,4)));
        eq(List.of("a2","b2","a-1"),order,"nested order and early refusal");check(nested.isEmpty(),"nested no mutation");
        Collection<Integer> deep=M3CollectionViews.filtered(new ArrayList<>(),x->true);
        for(int i=0;i<65536;i++)deep=M3CollectionViews.filtered(deep,x->true);
        deep.addAll(List.of(2,4));eq(List.of(2,4),new ArrayList<>(deep),"deep predicate reuse");
        NavigableSet<Integer> parent=new TreeSet<>(List.of(0,1,2,3,4,5,6,7,8,9));
        NavigableSet<Integer> selected=M3CollectionViews.filtered(parent,x->x%2==0);
        NavigableSet<Integer> range=selected.subSet(2,true,8,false);
        eq(null,range.lower(-100),"query below range");eq(2,range.ceiling(-100),"ceiling below range");
        eq(6,range.floor(100),"floor above range");eq(null,range.higher(100),"higher above range");
        check(selected.headSet(8) instanceof NavigableSet<?>,"legacy range runtime capability");
        range.descendingSet().clear();eq(List.of(0,1,3,5,7,8,9),new ArrayList<>(parent),"only visible range removed");
        var strings=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);strings.add("ADMIN");
        var lower=M3CollectionViews.filtered(strings,s->s.equals(s.toLowerCase(java.util.Locale.ROOT)));
        check(!lower.contains("admin")&&!lower.remove("admin"),"sorted stored representative retained");
        var nullable=new TreeSet<Integer>(Comparator.nullsFirst(Integer::compare));
        var nullableView=M3CollectionViews.filtered(nullable,x->x==null||x%2==0);
        nullableView.addAll(Arrays.asList(null,2,4));eq(null,nullableView.first(),"null first");
        eq(2,nullableView.higher(null),"null navigation");eq(4,nullableView.descendingSet().first(),"reverse comparator");
        rejects(IllegalArgumentException.class,()->range.addAll(List.of(10)));
    }
    private record Item(int key,int ordinal) { }
    private static void snapshotsAndRandomized() {
        Random random=new Random(20261017);
        for(int trial=0;trial<500;trial++) {
            ArrayList<Integer> input=new ArrayList<>();
            for(int i=0,n=random.nextInt(65);i<n;i++)input.add(random.nextInt(41)-20);
            for(int kind=0;kind<5;kind++) {
                Collection<Integer> owner=switch(kind) {
                    case 0 -> new ArrayList<>();
                    case 1 -> new HashSet<>();
                    case 2 -> new TreeSet<>();
                    case 3 -> new TreeSet<Integer>().descendingSet();
                    default -> new CopyOnWriteArrayList<>();
                };
                var all=new ArrayList<>(input);all.removeIf(x->x%2!=0);
                var view=M3CollectionViews.filtered(owner,x->x%2==0);
                eq(!all.isEmpty(),view.addAll(all),"addAll changed");
                List<Integer> expected=kind==0||kind==4?all:new ArrayList<>(new TreeSet<>(all));
                List<Integer> actual=new ArrayList<>(view);
                if(kind==1)actual.sort(null);if(kind==3)Collections.reverse(actual);
                eq(expected,actual,"owner order and duplicates");
                eq(all.stream().sorted().distinct().toList(),new ArrayList<>(new TreeSet<>(view)),"same unique members");
                var snap=M3CollectionViews.filteredSortedSnapshot(view,x->true,Comparator.reverseOrder());
                eq(view.stream().sorted(Comparator.reverseOrder()).toList(),snap,"stable sorted result");
                owner.add(100);check(!snap.contains(100),"snapshot not live");check(view.contains(100),"filtered live");
                Spliterator<Integer> split=view.spliterator();
                check(!split.hasCharacteristics(Spliterator.SIZED),"unknown filtered size");
                if(owner instanceof SortedSet<?>)check(split.hasCharacteristics(Spliterator.SORTED|Spliterator.ORDERED|Spliterator.DISTINCT),"retained sorted metadata");
                if(owner instanceof Set<?>)check(split.hasCharacteristics(Spliterator.DISTINCT),"retained distinct metadata");
                rejects(UnsupportedOperationException.class,()->snap.add(200));
            }
            List<Item> items=new ArrayList<>();for(int i=0;i<30;i++)items.add(new Item(random.nextInt(4),i));
            List<Item> sorted=M3CollectionViews.filteredSortedSnapshot(items,x->x.key()!=1,Comparator.comparingInt(Item::key));
            eq(items.stream().filter(x->x.key()!=1).sorted(Comparator.comparingInt(Item::key)).toList(),sorted,"stable equal-key identities");
            for(Item value:sorted)check(value==items.get(value.ordinal()),"same element reference");
        }
    }
    public static void main(String[] args) {
        checks=0;
        if(args.length>0&&args[0].equals("baseline")){changingSource(false);return;}
        if(args.length>0&&args[0].equals("stable")){failureAndAliasing();nestedAndSorted();snapshotsAndRandomized();}
        else {changingSource(true);failureAndAliasing();nestedAndSorted();snapshotsAndRandomized();}
        System.out.println("FILTERED_BATCH_CONTRACTS_PASS checks="+checks+" seed=20261017");
    }
}
