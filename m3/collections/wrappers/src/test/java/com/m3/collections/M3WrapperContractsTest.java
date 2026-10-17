/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.*;
import org.junit.jupiter.api.Test;
import org.eclipse.collections.impl.set.sorted.mutable.SortedSetAdapter;
import org.eclipse.collections.impl.map.sorted.mutable.SortedMapAdapter;
import org.springframework.util.MultiValueMap;
import com.google.common.collect.Sets;
import com.m3.collections.interop.M3BiMap;

/** Actual M3 owners plus independent JDK/provider oracles, never substitute storage stubs. */
final class M3WrapperContractsTest {
    @Test void erasedCollectionPreservesSortedAndNavigableContracts() {
        NavigableSet<Integer> owner = new M3TreeSet<>(); owner.addAll(List.of(1,2,3,4));
        Collection<Integer> erased = owner;
        var view = M3CollectionViews.filtered(erased, x -> x % 2 == 0);
        assertInstanceOf(NavigableSet.class, view);
        assertEquals(Set.of(2,4), view); assertEquals(Set.of(2,4).hashCode(), view.hashCode());
        @SuppressWarnings("unchecked") var nav = (NavigableSet<Integer>)view;
        assertEquals(4, nav.floor(100)); assertEquals(2, nav.ceiling(-100));
        Set<Integer> set = owner; assertInstanceOf(NavigableSet.class, M3CollectionViews.filtered(set, x -> true));
        SortedSet<Integer> sorted = owner; assertInstanceOf(NavigableSet.class, M3CollectionViews.filtered(sorted, x -> true));
        Collection<Integer> plainSet = new M3LinkedHashSet<>(); plainSet.add(1);
        assertInstanceOf(Set.class, M3CollectionViews.filtered(plainSet, x -> true));
    }
    @Test void sortedOnlyOwnerKeepsComparatorThroughErasure() {
        SortedSet<String> owner = Collections.unmodifiableSortedSet(new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        assertFalse(owner instanceof NavigableSet<?>);
        Collection<String> view = M3CollectionViews.filtered((Collection<String>)owner, x -> true);
        assertInstanceOf(SortedSet.class, view);
        assertSame(String.CASE_INSENSITIVE_ORDER, ((SortedSet<?>)view).comparator());
    }
    @Test void directOwnerMembershipRespectsIdentity() {
        Set<String> owner = Collections.newSetFromMap(new IdentityHashMap<>());
        String a = new String("same"), b = new String("same"); owner.add(a);
        var view = M3CollectionViews.filtered(owner, x -> true);
        assertTrue(view.contains(a)); assertFalse(view.contains(b)); assertFalse(view.remove(b));
        assertTrue(view.remove(a)); assertTrue(owner.isEmpty());
        // Only membership/removal is qualified here; identity-set equals/hash is not universalized.
    }
    @Test void explicitOwnerRemovalDoesNotRequireRemovableIterator() {
        var data = new ArrayList<>(List.of(1,2,3));
        Collection<Integer> owner = new AbstractCollection<>() {
            @Override public int size() { return data.size(); }
            @Override public Iterator<Integer> iterator() { return Collections.unmodifiableList(data).iterator(); }
            @Override public boolean remove(Object key) { return data.remove(key); }
        };
        var view = M3CollectionViews.filtered(owner, x -> x % 2 == 0);
        assertFalse(view.remove(1)); assertTrue(view.remove(2)); assertEquals(List.of(1,3), data);
    }
    @Test void listDuplicatesAndHiddenSurvivors() {
        List<Integer> owner = new M3LinkedList<>(); owner.addAll(Arrays.asList(1,2,2,3,null,4));
        var view = M3CollectionViews.filtered((Collection<Integer>)owner, x -> x != null && x % 2 == 0);
        assertEquals(List.of(2,2,4), new ArrayList<>(view)); assertTrue(view.remove(2));
        assertEquals(List.of(2,4), new ArrayList<>(view)); view.retainAll(Set.of(4));
        assertEquals(Arrays.asList(1,3,null,4), owner); view.clear(); assertEquals(Arrays.asList(1,3,null), owner);
    }
    @Test void nestedPredicatesShortCircuitInOrder() {
        Collection<Integer> owner = new M3LinkedList<>(); owner.addAll(List.of(1,2,3,4));
        var calls = new ArrayList<String>();
        var inner = M3CollectionViews.filtered(owner, x -> { calls.add("i"+x); return x%2==0; });
        var outer = M3CollectionViews.filtered(inner, x -> { calls.add("o"+x); return x>2; });
        assertEquals(List.of(4), new ArrayList<>(outer.stream().toList()));
        assertEquals(List.of("i1","i2","o2","i3","i4","o4"), calls);
        owner.add(6); assertEquals(List.of(4,6), outer.stream().toList());
    }
    @Test void sortedSnapshotFiltersOnceAndPreservesDuplicates() {
        List<Integer> owner = new M3LinkedList<>(); owner.addAll(Arrays.asList(4,2,2,null,1));
        AtomicInteger seen = new AtomicInteger();
        var filtered = M3CollectionViews.filtered((Collection<Integer>)owner, x -> {seen.incrementAndGet();return true;});
        var snapshot = M3CollectionViews.sortedSnapshot(filtered, Comparator.nullsFirst(Comparator.naturalOrder()));
        assertEquals(5, seen.get()); assertEquals(Arrays.asList(null,1,2,2,4), snapshot);
        owner.clear(); assertEquals(5, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(8));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.reversed().removeFirst());
    }
    @Test void streamMetadataDoesNotCountPredicateAndShortCircuitIsLazy() {
        Collection<Integer> owner = new M3LinkedList<>(); for(int i=0;i<1000;i++) owner.add(i);
        AtomicInteger count = new AtomicInteger();
        var view = M3CollectionViews.filtered(owner, x -> {count.incrementAndGet();return x%2==0;});
        var spliterator = view.spliterator(); assertFalse(spliterator.hasCharacteristics(Spliterator.SIZED));
        assertFalse(spliterator.hasCharacteristics(Spliterator.SUBSIZED)); spliterator.estimateSize(); assertEquals(0,count.get());
        assertEquals(0, view.stream().findFirst().orElseThrow()); assertEquals(1,count.get());
        assertEquals(500, view.parallelStream().count());
    }
    @Test void sortedTraversalMetadataAndSplitMatchComparator() {
        Comparator<Integer> comparator = Comparator.reverseOrder(); var owner = new M3TreeSet<Integer>(comparator);
        for(int i=0;i<128;i++) owner.add(i);
        var view = M3CollectionViews.filtered(owner, x -> x%3==0);
        var sp = view.spliterator(); assertTrue(sp.hasCharacteristics(Spliterator.ORDERED|Spliterator.SORTED|Spliterator.DISTINCT));
        assertSame(comparator, sp.getComparator()); assertFalse(sp.hasCharacteristics(Spliterator.SIZED));
        var values = new ArrayList<Integer>(); var prefix=sp.trySplit(); if(prefix!=null) prefix.forEachRemaining(values::add); sp.forEachRemaining(values::add);
        assertEquals(view.stream().toList(), values);
    }
    @Test void boundedNavigationNullsAndBothDirectionsAgainstJdk() {
        long queries=0;
        for(Comparator<Integer> comparator: List.<Comparator<Integer>>of(Comparator.naturalOrder(), Comparator.reverseOrder(), Comparator.nullsFirst(Comparator.naturalOrder()), Comparator.nullsLast(Comparator.naturalOrder()))) {
            for(int seed=0;seed<32;seed++) {
                var source=new M3TreeSet<Integer>(comparator); var all=new TreeSet<Integer>(comparator);
                Random random=new Random(seed); for(int i=-20;i<=20;i++) if(random.nextBoolean()){source.add(i);all.add(i);}
                if(seed%2==0 && (comparator.compare(1,2)<0)) {try {source.add(null);all.add(null);}catch(NullPointerException expected){}}
                Predicate<Integer> admit=x->x==null||x%3==0;
                for(boolean reverse:new boolean[]{false,true}) {
                    NavigableSet<Integer> owner=reverse?source.descendingSet():source;
                    var visible=new TreeSet<Integer>(owner.comparator()); for(Integer x:owner)if(admit.test(x))visible.add(x);
                    var view=M3CollectionViews.filtered(owner,admit);
                    for(int q=-23;q<=23;q++) {assertEquals(visible.lower(q),view.lower(q));assertEquals(visible.floor(q),view.floor(q));assertEquals(visible.ceiling(q),view.ceiling(q));assertEquals(visible.higher(q),view.higher(q));queries++;}
                    if(owner.size()>4) {
                        var keys=new ArrayList<>(owner); Integer low=keys.get(1),high=keys.get(keys.size()-2);
                        var range=owner.subSet(low,true,high,false); var sub=M3CollectionViews.filtered(range,admit);
                        var expected=new TreeSet<Integer>(owner.comparator()); for(Integer x:range)if(admit.test(x))expected.add(x);
                        for(int q=-25;q<=25;q++){assertEquals(expected.floor(q),sub.floor(q));assertEquals(expected.ceiling(q),sub.ceiling(q));queries++;}
                        assertEquals(new ArrayList<>(expected.descendingSet()),new ArrayList<>(sub.descendingSet()));
                    }
                }
            }
        }
        System.out.println("M3_WRAPPER_NAVIGATION_QUERIES="+queries);
    }
    @Test void comparatorStoredRepresentativeLookupRemoval() {
        var owner = new M3TreeSet<String>(String.CASE_INSENSITIVE_ORDER); owner.add("Alpha");owner.add("Beta");
        var view=M3CollectionViews.filtered(owner, x -> x.equals("Alpha"));
        assertTrue(view.contains("ALPHA")); assertFalse(view.contains("BETA")); assertTrue(view.remove("alpha")); assertEquals(Set.of("Beta"),owner);
    }
    @Test void sortedReadOnlyCheckedAndSequencedComposition() {
        var owner=new M3TreeSet<Integer>();owner.addAll(List.of(1,2,3,4));
        var view=M3CollectionViews.filtered(owner,x->x%2==0);
        var readOnly=Collections.unmodifiableNavigableSet(view);
        assertThrows(UnsupportedOperationException.class,()->readOnly.descendingSet().pollFirst());
        assertThrows(UnsupportedOperationException.class,()->readOnly.headSet(3,true).clear());
        assertEquals(4,view.reversed().getFirst());assertEquals(2,view.removeFirst());
        assertEquals(Set.of(1,3,4),owner);assertThrows(IllegalArgumentException.class,()->view.add(7));
        assertThrows(UnsupportedOperationException.class,()->view.addFirst(8));
        var checked=Collections.checkedNavigableSet(view,Integer.class);assertEquals(4,checked.first());
    }
    @Test void collectionAddAllChecksAllPredicatesBeforeWriting() {
        Collection<Integer> owner=new M3LinkedList<>();owner.add(2);var view=M3CollectionViews.filtered(owner,x->x%2==0);
        assertThrows(IllegalArgumentException.class,()->view.addAll(List.of(4,5,6)));assertEquals(List.of(2),new ArrayList<>(owner));
        assertThrows(UnsupportedOperationException.class,()->view.iterator().remove());
    }
    @Test void mapViewsUseActualM3OwnerAndKeepHiddenEntries() {
        Map<Integer,Integer> owner=new M3LinkedHashMap<>();owner.put(1,-1);owner.put(2,20);owner.put(3,null);
        var view=M3FilteredMaps.filtered(owner,(k,v)->v==null||v>=0);
        assertNull(view.compute(1,(k,v)->null));assertEquals(-1,owner.get(1));assertNull(view.put(1,10));
        assertEquals(3,view.size());view.values().removeIf(Objects::isNull);assertFalse(owner.containsKey(3));
        owner.put(4,-4);view.clear();assertEquals(Map.of(4,-4),owner);
    }
    @Test void filteredSortedMapRangesAndImmutableNavigationEntries() {
        var owner=new M3TreeMap<Integer,Integer>();for(int i=0;i<30;i++)owner.put(i,i%2==0?i:-i);
        Map<Integer,Integer> erased=owner;var result=M3FilteredMaps.filtered(erased,(k,v)->v>=0);assertInstanceOf(NavigableMap.class,result);
        @SuppressWarnings("unchecked") var view=(NavigableMap<Integer,Integer>)result;
        var range=view.subMap(5,true,20,false);assertEquals(18,range.floorKey(999));assertEquals(6,range.ceilingKey(-999));
        assertThrows(UnsupportedOperationException.class,()->range.firstEntry().setValue(8));
        assertEquals(18,range.descendingMap().firstKey());range.clear();
        for(int i=5;i<20;i++)assertEquals(i%2!=0,owner.containsKey(i));
        assertThrows(IllegalArgumentException.class,()->range.put(100,100));
    }
    @Test void entryBoundariesNeverLeakUnfilteredWriteAuthority() {
        var owner=new M3TreeMap<Integer,Integer>();owner.put(1,10);owner.put(2,-2);
        var view=M3FilteredMaps.filtered(owner,(k,v)->v>0);
        var fromArray=view.entrySet().toArray(new Map.Entry<?,?>[0])[0];
        @SuppressWarnings("unchecked") var entry=(Map.Entry<Integer,Integer>)fromArray;
        assertThrows(IllegalArgumentException.class,()->entry.setValue(-1));
        assertThrows(IllegalArgumentException.class,()->view.entrySet().stream().findFirst().orElseThrow().setValue(-1));
        assertThrows(IllegalArgumentException.class,()->view.sequencedEntrySet().getFirst().setValue(-1));
        assertThrows(IllegalArgumentException.class,()->view.entrySet().removeIf(e->{e.setValue(-1);return true;}));
        entry.setValue(12);assertEquals(12,owner.get(1));assertEquals(-2,owner.get(2));
    }
    @Test void nullKeysAndComparatorMapLookup() {
        var owner=new M3TreeMap<String,Integer>(Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));
        owner.put(null,0);owner.put("Alpha",1);owner.put("Beta",-1);
        var view=M3FilteredMaps.filtered(owner,(k,v)->v>=0);
        assertEquals(1,view.get("ALPHA"));assertTrue(view.containsKey(null));assertNotNull(view.firstEntry());
        assertNull(view.firstEntry().getKey());view.pollFirstEntry();assertFalse(owner.containsKey(null));
        assertTrue(view.keySet().remove("alpha"));assertEquals(Map.of("Beta",-1),owner);
    }
    @Test void readOnlySortedMapsProtectAllNestedEntries() {
        var owner=new M3TreeMap<Integer,Integer>();owner.put(1,1);owner.put(2,-1);
        var view=M3FilteredMaps.filtered(owner,(k,v)->v>0);
        var ro=Collections.unmodifiableNavigableMap(view);
        assertThrows(UnsupportedOperationException.class,()->ro.descendingMap().pollFirstEntry());
        assertThrows(UnsupportedOperationException.class,()->ro.entrySet().iterator().next().setValue(7));
        assertThrows(UnsupportedOperationException.class,()->ro.navigableKeySet().clear());assertEquals(2,owner.size());
    }
    @Test void bimapEntryUpdatesRetainBothPredicateAndInverseGuards() {
        var owner=M3BiMap.<String,Integer>create();owner.put("a",1);owner.put("b",2);owner.put("c",-3);
        var view=M3FilteredMaps.filtered((Map<String,Integer>)owner,(k,v)->v>0);
        var first=view.entrySet().iterator().next();assertThrows(IllegalArgumentException.class,()->first.setValue(-1));
        assertThrows(IllegalArgumentException.class,()->first.setValue(2));first.setValue(5);assertEquals("a",owner.inverse().get(5));
        view.clear();assertEquals(Map.of("c",-3),owner);assertEquals(Map.of(-3,"c"),owner.inverse());
    }
    @Test void actualGuavaSetFilterSharedProfile() {
        Set<Integer> a=new M3LinkedHashSet<>(),b=new LinkedHashSet<>();for(int i=0;i<100;i++){a.add(i);b.add(i);}
        Set<Integer> actual=M3CollectionViews.filtered(a,x->x%2==0),expected=Sets.filter(b,x->x%2==0);
        assertEquals(expected,actual);actual.removeAll(Set.of(2,4,7));expected.removeAll(Set.of(2,4,7));assertEquals(b,a);
        actual.retainAll(Set.of(8,10));expected.retainAll(Set.of(8,10));assertEquals(b,a);assertEquals(expected,actual);
    }
    @Test void actualEclipseSortedWrappersKeepM3BackedMutations() {
        var owner=new M3TreeSet<Integer>();owner.addAll(List.of(1,2,3,4));
        var view=M3CollectionViews.filtered(owner,x->x%2==0);var eclipse=SortedSetAdapter.adapt(view);
        assertEquals(Set.of(2,4),eclipse);eclipse.remove(2);assertFalse(owner.contains(2));assertTrue(owner.contains(1));
        var mapOwner=new M3TreeMap<Integer,Integer>();mapOwner.put(1,1);mapOwner.put(2,-2);
        var map=SortedMapAdapter.adapt(M3FilteredMaps.filtered(mapOwner,(k,v)->v>0));
        map.updateValue(1,()->0,x->x+10);assertEquals(11,mapOwner.get(1));map.clear();assertEquals(Map.of(2,-2),mapOwner);
    }
    @Test void actualSpringWrapperRespectsFilteredVisibility() {
        var owner=new M3TreeMap<String,Integer>();owner.put("hidden",-1);owner.put("shown",1);
        var spring=MultiValueMap.fromSingleValue(M3FilteredMaps.filtered(owner,(k,v)->v>=0));
        assertNull(spring.get("hidden"));spring.set("new",2);assertEquals(2,owner.get("new"));
        assertThrows(IllegalArgumentException.class,()->spring.set("bad",-2));spring.clear();assertEquals(Map.of("hidden",-1),owner);
    }
    @Test void externallyLockedFilteredPollingKeepsHiddenElements() throws Exception {
        var owner=new M3TreeSet<Integer>();for(int i=0;i<800;i++)owner.add(i);
        var view=Collections.synchronizedNavigableSet(M3CollectionViews.filtered(owner,x->x%2==0));
        Set<Integer> removed=ConcurrentHashMap.newKeySet();
        try(var pool=Executors.newFixedThreadPool(4)) {
            var futures=new ArrayList<Future<?>>();for(int i=0;i<4;i++)futures.add(pool.submit(()->{
                for(;;){Integer value;synchronized(view){value=view.pollFirst();}if(value==null)break;if(!removed.add(value))throw new AssertionError("duplicate");}
            }));for(var f:futures)f.get(10,TimeUnit.SECONDS);
        }
        assertEquals(400,removed.size());assertEquals(400,owner.size());assertTrue(owner.stream().allMatch(x->x%2!=0));
    }
    @Test void predicateFailureIdentityAndEmptyValidation() {
        RuntimeException marker=new RuntimeException("marker");var owner=new M3TreeSet<Integer>();owner.add(1);
        var view=M3CollectionViews.filtered(owner,x->{throw marker;});
        assertSame(marker,assertThrows(RuntimeException.class,view::first));assertEquals(Set.of(1),owner);
        assertThrows(NullPointerException.class,()->M3CollectionViews.filtered((Collection<Integer>)null,x->true));
        assertThrows(NullPointerException.class,()->M3CollectionViews.filtered(owner,null));
        assertThrows(NullPointerException.class,()->M3FilteredMaps.filtered((Map<Integer,Integer>)null,(k,v)->true));
    }
}
