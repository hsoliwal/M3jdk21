// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.function.Predicate;

/** Run as a normal Java21 main; this class is also invoked from the JUnit bridge. */
public final class M3FilteredSortedMultisetContract {
    private static long checks;
    private M3FilteredSortedMultisetContract() { }
    public static final class Domain<E> implements MIndexSpace<E> {
        private final IdentityHashMap<E, Integer> ids = new IdentityHashMap<>();
        private final ArrayList<E> values = new ArrayList<>();
        @Override public int id(E value) {
            Integer id = ids.get(value);
            if (id != null) return id;
            int fresh = values.size(); ids.put(value, fresh); values.add(value); return fresh;
        }
        @Override public int findId(E value) { return ids.getOrDefault(value, -1); }
        @Override public E value(int id) { return values.get(id); }
        @Override public int size() { return values.size(); }
        @Override public boolean javaEqualityCompatible() { return true; }
    }
    private static void equal(Object expected, Object actual, String why) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(why + " expected=" + expected + " actual=" + actual);
    }
    private static void yes(boolean condition, String why) { equal(true, condition, why); }
    private static void refuses(Class<? extends Throwable> type, Runnable body, String why) {
        try { body.run(); } catch (Throwable e) { yes(type.isInstance(e), why+" got "+e); return; }
        throw new AssertionError(why + " expected " + type.getName());
    }
    private static <E> ArrayList<E> copies(Iterable<E> source) {
        ArrayList<E> values = new ArrayList<>(); for (E value : source) values.add(value); return values;
    }
    private static <E> List<E> expand(Map<E, Integer> oracle, Predicate<? super E> select) {
        ArrayList<E> values = new ArrayList<>();
        for (Map.Entry<E,Integer> e : oracle.entrySet()) if (select.test(e.getKey()))
            for (int i=0;i<e.getValue();i++) values.add(e.getKey());
        return values;
    }
    private static void assertOracle(M3FilteredSortedMultisets.View<Integer> view,
            TreeMap<Integer,Integer> oracle, Predicate<Integer> filter, String label) {
        List<Integer> expected=expand(oracle, filter);
        equal(expected, copies(view), label+" occurrence traversal");
        equal((long)expected.size(), view.longSize(), label+" long size");
        equal(expected.size(), view.size(), label+" Collection size");
        long count=oracle.keySet().stream().filter(filter).count();
        equal((int)count, view.distinctSize(), label+" distinct size");
        equal(expected.isEmpty(), view.isEmpty(), label+" empty");
        List<Integer> expectedDistinct=oracle.keySet().stream().filter(filter).toList();
        equal(expectedDistinct, copies(view.elementSet()), label+" elementSet");
        equal(expectedDistinct.size(), view.entrySet().size(), label+" entrySet size");
        ArrayList<M3IndexSortedMultiset.Entry<Integer>> records=new ArrayList<>();
        for(Integer key:expectedDistinct) records.add(new M3IndexSortedMultiset.Entry<>(key,oracle.get(key)));
        equal(records, copies(view.entrySet()), label+" entrySet snapshots");
        for(int q=-22;q<22;q++) {
            Integer key=Integer.valueOf(q);
            int expectedCount=oracle.containsKey(key)&&filter.test(key)?oracle.get(key):0;
            equal(expectedCount,view.count(key),label+" count "+q);
            equal(expectedCount>0,view.contains(key),label+" contains "+q);
            long lower=expected.stream().filter(e->Integer.compare(e,key)<0).count();
            long inclusive=expected.stream().filter(e->Integer.compare(e,key)<=0).count();
            equal(lower,view.rankOf(key,false),label+" rank strict "+q);
            equal(inclusive,view.rankOf(key,true),label+" rank incl "+q);
            NavigableSet<Integer> elementSet=view.elementSet();
            TreeMap<Integer,Integer> selected=new TreeMap<>(); for(Integer v:expectedDistinct) selected.put(v,1);
            equal(selected.lowerKey(key),elementSet.lower(key),label+" lower "+q);
            equal(selected.floorKey(key),elementSet.floor(key),label+" floor "+q);
            equal(selected.ceilingKey(key),elementSet.ceiling(key),label+" ceiling "+q);
            equal(selected.higherKey(key),elementSet.higher(key),label+" higher "+q);
        }
        for(int i=0;i<expected.size();i++) equal(expected.get(i),view.elementAtOccurrence(i),label+" select "+i);
        refuses(IndexOutOfBoundsException.class,()->view.elementAtOccurrence(-1),label+" negative ordinal");
        refuses(IndexOutOfBoundsException.class,()->view.elementAtOccurrence(expected.size()),label+" after end");
        Spliterator<Integer> spliterator=view.spliterator();
        yes(spliterator.hasCharacteristics(Spliterator.SORTED|Spliterator.ORDERED),label+" sorted spliterator");
        yes(!spliterator.hasCharacteristics(Spliterator.DISTINCT|Spliterator.SIZED),label+" neither distinct nor sized");
        equal(view.comparator(),spliterator.getComparator(),label+" comparator metadata");
        equal(null,spliterator.trySplit(),label+" unsplit no batches");
        equal(expected,view.stream().toList(),label+" stream order");
        equal(expected,view.parallelStream().toList(),label+" parallel stream safe fallback");
    }
    private static void randomDifferential() {
        Random random=new Random(271828);
        for(int seed=0;seed<16;seed++) {
            Domain<Integer> space=new Domain<>();
            M3IndexSortedMultiset<Integer> source=new M3IndexSortedMultiset<>(space);
            TreeMap<Integer,Integer> oracle=new TreeMap<>();
            int salt=seed;
            Predicate<Integer> filter=e->Math.floorMod(e+salt,4)<2;
            M3FilteredSortedMultisets.View<Integer> view=M3FilteredSortedMultisets.filtered(source,filter);
            for(int operation=0;operation<240;operation++) {
                Integer key=Integer.valueOf(random.nextInt(31)-15);
                int amount=random.nextInt(4);
                switch(random.nextInt(9)) {
                    case 0,1 -> {
                        if (filter.test(key)) {
                            int prev=oracle.getOrDefault(key,0);equal(prev,view.add(key,amount),"add previous");
                            if(amount!=0) oracle.put(key,prev+amount);
                        } else if (amount!=0) refuses(IllegalArgumentException.class,()->view.add(key,amount),"reject hidden add");
                        else equal(0,view.add(key,0),"hidden zero addition");
                    }
                    case 2 -> {
                        int previous=filter.test(key)?oracle.getOrDefault(key,0):0;
                        equal(previous,view.remove(key,amount),"remove previous");
                        if(previous!=0&&amount!=0) {int next=previous-amount;if(next<=0)oracle.remove(key);else oracle.put(key,next);}
                    }
                    case 3 -> {
                        if (filter.test(key)) {
                            int previous=oracle.getOrDefault(key,0);equal(previous,view.setCount(key,amount),"set count previous");
                            if(amount==0)oracle.remove(key);else oracle.put(key,amount);
                        } else if(amount!=0) refuses(IllegalArgumentException.class,()->view.setCount(key,amount),"hidden set positive");
                        else equal(0,view.setCount(key,0),"hidden zero set");
                    }
                    case 4 -> { // Write through to the source outside the filter; the view must remain live.
                        int previous=oracle.getOrDefault(key,0);source.add(key,1);oracle.put(key,previous+1);
                    }
                    case 5 -> {
                        if (filter.test(key)) {
                            int previous=oracle.getOrDefault(key,0);
                            equal(true,view.setCount(key,previous,amount),"conditional set");
                            if(amount==0)oracle.remove(key);else oracle.put(key,amount);
                        } else {
                            equal(true,view.setCount(key,0,0),"hidden conditional zero");
                        }
                    }
                    case 6 -> {
                        boolean removed=view.remove(key);
                        int previous=filter.test(key)?oracle.getOrDefault(key,0):0;
                        equal(previous!=0,removed,"single occurrence remove");
                        if(previous>0){if(previous==1)oracle.remove(key);else oracle.put(key,previous-1);}
                    }
                    case 7 -> {
                        if(operation%7==0) {
                            view.clear();
                            oracle.keySet().removeIf(filter);
                        }
                    }
                    default -> { // count-only read must not admit unseen IDs
                        int old=space.size();view.count(Integer.valueOf(123));equal(old,space.size(),"queries do not admit");
                    }
                }
                assertOracle(view,oracle,filter,"seed "+seed+" step "+operation);
            }
        }
    }
    private static void edgeCases() {
        Domain<Integer> space=new Domain<>();
        Comparator<Integer> comparator=Comparator.nullsFirst(Comparator.naturalOrder());
        M3IndexSortedMultiset<Integer> source=new M3IndexSortedMultiset<>(space,comparator);
        source.add(null,2);source.add(1,4);source.add(2,3);source.add(3,1);source.add(4,5);source.add(5,1);
        Predicate<Integer> visible=e->e==null||e%2==0;
        var view=M3FilteredSortedMultisets.filtered(source,visible);
        equal(Arrays.asList(null,null,2,2,2,4,4,4,4,4),copies(view),"null and duplicates");
        equal(2,view.count(null),"null count");equal(0,view.count(3),"hidden count");
        equal(10L,view.longSize(),"null-aware total");
        equal(Arrays.asList(4,4,4,4,4,2,2,2,null,null),copies(view.descendingMultiset()),"descending counts");
        equal(Arrays.asList(2,2,2,4,4,4,4,4),copies(view.tailMultiset(2,true)),"tail bounds");
        equal(Arrays.asList(null,null,2,2,2),copies(view.headMultiset(2,true)),"head bounds");
        equal(Arrays.asList(2,2,2),copies(view.subMultiset(2,true,4,false)),"sub bounds");
        refuses(IllegalArgumentException.class,()->view.subMultiset(4,true,2,true),"invalid range endpoints");
        equal(null,view.elementSet().pollFirst(),"poll null group");equal(0,source.count(null),"poll removes all nulls");
        equal(5,source.count(4),"hidden and retained group");
        equal(new M3IndexSortedMultiset.Entry<>(2,3),view.firstEntry(),"first entry");
        equal(new M3IndexSortedMultiset.Entry<>(4,5),view.lastEntry(),"last entry");
        equal(new M3IndexSortedMultiset.Entry<>(4,5),view.pollLastEntry(),"poll last group");
        equal(0,source.count(4),"group removed");equal(4,source.count(1),"hidden retained");
        view.setCount(2,3,1);equal(1,source.count(2),"conditional decreased count");
        equal(false,view.setCount(2,3,2),"conditional mismatch");
        equal(1,view.add(2,0),"zero-add previous value preserved");
        var nested=view.filtered(x->x!=null&&x>=2);
        equal(1,nested.count(2),"composed predicate");equal(0,nested.count(1),"hidden after composition");
        var readOnly=M3FilteredSortedMultisets.readOnlyFiltered(source,x->true);
        int before=source.count(3);
        refuses(UnsupportedOperationException.class,()->readOnly.add(3,0),"read-only zero add");
        refuses(UnsupportedOperationException.class,()->readOnly.remove(3,0),"read-only zero remove");
        refuses(UnsupportedOperationException.class,()->readOnly.setCount(3,3),"read-only equal set");
        refuses(UnsupportedOperationException.class,()->readOnly.setCount(3,3,3),"read-only conditional no-op");
        refuses(UnsupportedOperationException.class,()->readOnly.addAll(List.of()),"read-only empty addAll");
        refuses(UnsupportedOperationException.class,()->readOnly.removeAll(List.of()),"read-only empty removeAll");
        refuses(UnsupportedOperationException.class,()->readOnly.retainAll(List.of()),"read-only empty retainAll");
        refuses(UnsupportedOperationException.class,()->readOnly.removeIf(x->false),"read-only no-op removeIf");
        refuses(UnsupportedOperationException.class,readOnly::clear,"read-only clear");
        refuses(UnsupportedOperationException.class,readOnly::pollFirstEntry,"read-only poll first");
        refuses(UnsupportedOperationException.class,readOnly::pollLastEntry,"read-only poll last");
        refuses(UnsupportedOperationException.class,()->readOnly.elementSet().remove(3),"read-only element removal");
        refuses(UnsupportedOperationException.class,()->readOnly.elementSet().removeIf(x->false),"read-only element removeIf");
        refuses(UnsupportedOperationException.class,()->readOnly.entrySet().removeIf(e->false),"read-only entry removeIf");
        refuses(UnsupportedOperationException.class,()->readOnly.entrySet().clear(),"read-only entry clear");
        equal(before,source.count(3),"all mutations refused");
        source.add(6,7);equal(7,readOnly.count(6),"read-only live backing");
        var iter=view.iterator();yes(iter.hasNext(),"iterator hasNext");refuses(UnsupportedOperationException.class,iter::remove,"unsupported lookahead removal");
        Iterator<M3IndexSortedMultiset.Entry<Integer>> entries=view.entrySet().iterator();
        yes(entries.hasNext(),"entry iterator");refuses(UnsupportedOperationException.class,entries::remove,"entry iterator remove refused");
        Iterator<Integer> elements=view.elementSet().iterator();
        yes(elements.hasNext(),"element iterator");refuses(UnsupportedOperationException.class,elements::remove,"element iterator remove refused");
        var spliterator=view.descendingMultiset().spliterator();
        yes(spliterator.hasCharacteristics(Spliterator.SORTED|Spliterator.ORDERED),"descending sorted spliterator");
        equal(view.descendingMultiset().comparator(),spliterator.getComparator(),"descending comparator");
        view.removeIf(x->Objects.equals(x,2));
        equal(0,source.count(2),"removeIf removed occurrence");equal(7,source.count(6),"readOnly observes changes");
        // all counts removed via group operations, hidden groups preserved
        view.elementSet().clear();equal(0,source.count(6),"elementSet clear visible");
        equal(4,source.count(1),"hidden unaffected");
        equal(1,source.count(3),"hidden unaffected again");
        // Mutable callback during evaluation must not corrupt partial state or silently appear successful.
        source.add(8,1);var concurrent=M3FilteredSortedMultisets.filtered(source,x->{source.add(10,1);return true;});
        refuses(ConcurrentModificationException.class,concurrent::longSize,"reentrant mutation detected");
        // Every retained field belongs to a view owner/strategy, not a per-occurrence list or node cache.
        for(Field field:M3FilteredSortedMultisets.View.class.getDeclaredFields())
            yes(!field.getType().isArray() && (!Collection.class.isAssignableFrom(field.getType())
                    || field.getType()==M3IndexSortedMultiset.class), "only existing canonical backing collection retained");
    }
    private static void wideCounts() {
        Domain<Integer> space=new Domain<>();M3IndexSortedMultiset<Integer> source=new M3IndexSortedMultiset<>(space);
        source.add(2,Integer.MAX_VALUE);source.add(3,Integer.MAX_VALUE);
        var view=M3FilteredSortedMultisets.filtered(source,x->x==2);
        equal((long)Integer.MAX_VALUE,view.longSize(),"no expanded filtered total");
        equal(Integer.MAX_VALUE,view.size(),"saturated size");equal(1,view.distinctSize(),"one selected distinct");
        equal(Integer.valueOf(2),view.elementAtOccurrence(Integer.MAX_VALUE-1L),"wide select");
        refuses(IndexOutOfBoundsException.class,()->view.elementAtOccurrence(Integer.MAX_VALUE),"wide just after end");
        var ro=M3FilteredSortedMultisets.readOnlyFiltered(source,x->true);
        equal(4294967294L,ro.longSize(),"wide exact total");equal(Integer.MAX_VALUE,ro.size(),"saturated wide size");
        equal(4294967294L,ro.rankOf(3,true),"wide rank inclusive");
        equal(Integer.valueOf(3),ro.elementAtOccurrence(4294967293L),"wide ordinal");
        refuses(IndexOutOfBoundsException.class,()->ro.elementAtOccurrence(4294967294L),"wide bounds");
    }
    public static void main(String[] args) {
        randomDifferential();edgeCases();wideCounts();
        System.out.println("PASS M3FilteredSortedMultisetContract checks="+checks);
    }
}
