/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;
import org.apache.commons.collections4.*;
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.collections4.multiset.HashMultiSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Actual Apache 4.4 behavioral oracles; reflective discovery also executes against the immutable absent baseline. */
final class M3ApacheContractTest {
    static Object call(String type, String method, Class<?>[] types, Object... args) {
        try { return Class.forName("com.m3.collections.interop."+type).getMethod(method, types).invoke(null,args); }
        catch (InvocationTargetException e) { Throwable t=e.getCause(); if(t instanceof RuntimeException r)throw r; if(t instanceof Error r)throw r; throw new AssertionError(t); }
        catch (ReflectiveOperationException e) { throw new AssertionError("Missing admitted adapter: "+type+"."+method,e); }
    }
    @SuppressWarnings("unchecked") static <T>T factory(String name) { return (T)call("M3ApacheCollections",name,new Class<?>[0]); }
    @SuppressWarnings("unchecked") static <T>T wrap(String name,Class<?> type,Object source) { return (T)call("M3ApacheCollections",name,new Class<?>[]{type},source); }
    @SuppressWarnings("unchecked") static <T>T collector(String name,Function<String,String> key,Function<String,Integer> value) {
        return (T)call("M3ApacheCollectors",name,new Class<?>[]{Function.class,Function.class},key,value);
    }
    @SuppressWarnings("unchecked") static MultiSet<String> counts() {
        try { return (MultiSet<String>)Class.forName("com.m3.collections.interop.M3ApacheMultiSet").getConstructor().newInstance(); }
        catch (ReflectiveOperationException e) { throw new AssertionError("Missing admitted Apache MultiSet",e); }
    }
    static Map<String,Integer> frequencies(MultiSet<String> m) { Map<String,Integer> result=new HashMap<>(); for(var e:m.entrySet())result.put(e.getElement(),e.getCount()); return result; }
    static Map<Integer,List<Integer>> lists(MultiValuedMap<Integer,Integer> m) { Map<Integer,List<Integer>> r=new HashMap<>();m.asMap().forEach((k,v)->r.put(k,new ArrayList<>(v)));return r; }
    static Map<Integer,Set<Integer>> sets(MultiValuedMap<Integer,Integer> m) { Map<Integer,Set<Integer>> r=new HashMap<>();m.asMap().forEach((k,v)->r.put(k,new HashSet<>(v)));return r; }

    @Test @Tag("counts") void countReturnsNullsAndZero() {
        MultiSet<String>a=counts(),b=new HashMultiSet<>();
        for(String k:Arrays.asList("x",null,"x","y",null)) {
            assertEquals(b.add(k,3),a.add(k,3));assertEquals(b.add(k,0),a.add(k,0));
            assertEquals(b.remove(k,1),a.remove(k,1));assertEquals(b.setCount(k,5),a.setCount(k,5));
            assertEquals(b,a);assertEquals(a,b);assertEquals(b.hashCode(),a.hashCode());
        }
        assertTrue(a.add("x"));assertEquals(6,a.getCount("x"));assertTrue(a.remove("x"));assertEquals(5,a.getCount("x"));
        assertThrows(IllegalArgumentException.class,()->a.add("x",-1));assertThrows(IllegalArgumentException.class,()->a.remove("x",-1));
        assertThrows(IllegalArgumentException.class,()->a.setCount("x",-1));assertEquals(5,a.getCount("x"));
    }
    @Test @Tag("counts") void collectionOperationsIgnoreSourceMultiplicity() {
        MultiSet<String>a=counts();a.add("a",3);a.add("b",2);
        assertTrue(a.containsAll(Collections.nCopies(20,"a")));assertTrue(a.retainAll(List.of("a")));assertEquals(3,a.getCount("a"));
        assertTrue(a.removeAll(List.of("a")));assertTrue(a.isEmpty());
        a.add("x",3);assertFalse(a.retainAll(a));assertTrue(a.addAll(a));assertEquals(6,a.getCount("x"));assertTrue(a.removeAll(a));
        assertFalse(a.addAll(List.of()));assertThrows(NullPointerException.class,()->a.retainAll(null));
    }
    @Test @Tag("counts") void uniqueAndEntryViewsRemoveThroughAndStayLive() {
        MultiSet<String>a=counts();Set<String> unique=a.uniqueSet();Set<MultiSet.Entry<String>> entries=a.entrySet();
        a.add(null,4);assertTrue(unique.contains(null));var held=entries.iterator().next();assertEquals(4,held.getCount());
        a.add(null,2);assertEquals(4,held.getCount());assertFalse(entries.contains(held));assertFalse(entries.remove(held));
        var iterator=entries.iterator();assertEquals(6,iterator.next().getCount());iterator.remove();assertTrue(a.isEmpty());
        a.add("new",8);assertTrue(unique.remove("new"));assertEquals(0,a.size());
        a.add("x",2);var now=entries.iterator().next();assertTrue(entries.remove(now));assertTrue(a.isEmpty());
        assertThrows(UnsupportedOperationException.class,()->unique.add("forbidden"));
    }
    @Test @Tag("counts") void occurrenceIteratorRemovesOneAndDetectsInterference() {
        MultiSet<String>a=counts();a.add("x",3);Iterator<String>i=a.iterator();assertThrows(IllegalStateException.class,i::remove);
        assertEquals("x",i.next());i.remove();assertEquals(2,a.getCount("x"));assertThrows(IllegalStateException.class,i::remove);
        assertEquals("x",i.next());assertEquals("x",i.next());assertFalse(i.hasNext());assertThrows(NoSuchElementException.class,i::next);
        i=a.iterator();a.add("x",1);Iterator<String> stale=i;assertThrows(ConcurrentModificationException.class,stale::next);
    }
    @Test @Tag("counts") void overflowRefusesWithoutChangingCountsAndLongSizeSaturates() throws Exception {
        MultiSet<String>a=counts();a.add("x",Integer.MAX_VALUE);a.add("y",3);
        assertEquals(Integer.MAX_VALUE,a.size());assertEquals(2147483650L,a.getClass().getMethod("longSize").invoke(a));
        assertEquals(2147483650L,a.spliterator().estimateSize());
        assertThrows(IllegalArgumentException.class,()->a.add("x",1));assertEquals(Integer.MAX_VALUE,a.getCount("x"));
        assertThrows(IllegalArgumentException.class,()->a.addAll(a));assertEquals(3,a.getCount("y"));
        assertEquals(Integer.MAX_VALUE,a.remove("x",Integer.MAX_VALUE));assertEquals(3,a.size());
    }
    @Test @Tag("counts") void multiSetEqualityIsNotGuavaOrLegacyBagEquality() {
        MultiSet<String>a=counts(),b=new HashMultiSet<>();a.add("x",2);b.add("x",2);
        assertEquals(b,a);assertEquals(a,b);assertEquals(b.entrySet(),a.entrySet());assertEquals(a.entrySet(),b.entrySet());
        assertNotEquals(a,List.of("x","x"));assertNotEquals(a,com.google.common.collect.HashMultiset.create(List.of("x","x")));
        assertNotEquals(a,new org.apache.commons.collections4.bag.HashBag<>(List.of("x","x")));
    }
    @Test @Tag("counts") void seededMultiSetDifferential() {
        for(int seed=0;seed<8;seed++) {
            Random r=new Random(seed);MultiSet<String>a=counts(),b=new HashMultiSet<>();
            for(int step=0;step<2000;step++) {
                String k=r.nextInt(9)==0?null:"k"+r.nextInt(24);int n=r.nextInt(7);
                switch(r.nextInt(8)) {
                    case 0 -> assertEquals(b.add(k,n),a.add(k,n));case 1->assertEquals(b.remove(k,n),a.remove(k,n));
                    case 2->assertEquals(b.setCount(k,n),a.setCount(k,n));case 3->assertEquals(b.remove(k),a.remove(k));
                    case 4->assertEquals(b.add(k),a.add(k));case 5->assertEquals(b.removeAll(Arrays.asList(k,k)),a.removeAll(Arrays.asList(k,k)));
                    case 6->assertEquals(b.containsAll(Arrays.asList(k,k)),a.containsAll(Arrays.asList(k,k)));default->{if(step%71==0){a.clear();b.clear();}}
                }
                assertEquals(frequencies(b),frequencies(a));assertEquals(b.size(),a.size());assertEquals(b.hashCode(),a.hashCode());assertEquals(b,a);assertEquals(a,b);
            }
        }
    }
    @Test @Tag("maps") void retainedListViewAbsentRemoveReinsertAndDetachedRemove() {
        ListValuedMap<Integer,Integer>a=factory("listValuedMap");List<Integer>held=a.get(1);assertTrue(held.isEmpty());assertFalse(a.containsKey(1));
        assertNull(a.asMap().get(1));held.addAll(Arrays.asList(1,null,2,1));assertEquals(4,a.size());
        List<Integer> removed=a.remove(1);assertEquals(Arrays.asList(1,null,2,1),removed);assertTrue(held.isEmpty());
        held.add(7);assertEquals(List.of(7),a.get(1));removed.add(8);assertEquals(List.of(7),a.get(1));
        held.clear();assertFalse(a.containsKey(1));assertTrue(a.isEmpty());
    }
    @Test @Tag("maps") void listPositionsSubListAndListIteratorMirrorDonor() {
        ListValuedMap<Integer,Integer>a=factory("listValuedMap"),b=new ArrayListValuedHashMap<>();
        a.putAll(1,Arrays.asList(1,null,2,3));b.putAll(1,Arrays.asList(1,null,2,3));
        ListIterator<Integer> ai=a.get(1).listIterator(1),bi=b.get(1).listIterator(1);
        assertEquals(bi.next(),ai.next());ai.set(8);bi.set(8);ai.add(9);bi.add(9);assertEquals(bi.previous(),ai.previous());ai.remove();bi.remove();
        a.get(1).subList(1,3).clear();b.get(1).subList(1,3).clear();assertEquals(lists(b),lists(a));
        assertThrows(IndexOutOfBoundsException.class,()->a.get(2).add(1,4));
        assertThrows(IndexOutOfBoundsException.class,()->b.get(2).add(1,4));
        assertTrue(b.containsKey(2));assertEquals(lists(b),lists(a));
        assertFalse(a.putAll(3,List.of()));assertFalse(a.containsKey(3));
    }
    @Test @Tag("maps") void setValuedDuplicateRulesAndHeldViews() {
        SetValuedMap<Integer,Integer>a=factory("setValuedMap");Set<Integer> held=a.get(null);
        assertTrue(held.add(null));assertFalse(held.add(null));assertTrue(a.put(null,1));assertEquals(2,a.size());
        assertEquals(new HashSet<>(Arrays.asList(null,1)),a.remove(null));assertTrue(held.isEmpty());held.add(8);assertEquals(Set.of(8),a.get(null));
        a.asMap().get(null).clear();assertFalse(a.containsKey(null));assertTrue(a.isEmpty());
    }
    @Test @Tag("maps") void keyEntryValueAndMapIteratorsRemoveMappings() {
        for(String name:List.of("listValuedMap","setValuedMap")) {
            MultiValuedMap<Integer,Integer>a=factory(name);a.putAll(1,List.of(4,5));a.put(2,6);
            assertEquals(2,a.keys().getCount(1));var entries=a.entries().iterator();entries.next();entries.remove();assertEquals(2,a.size());
            var values=a.values().iterator();values.next();values.remove();assertEquals(1,a.size());
            var map=a.mapIterator();map.next();assertThrows(UnsupportedOperationException.class,()->map.setValue(42));map.remove();assertTrue(a.isEmpty());
            a.putAll(1,List.of(1,2));a.put(2,3);a.keySet().remove(1);assertEquals(1,a.size());
            a.asMap().entrySet().iterator().next().getValue().clear();assertTrue(a.isEmpty());
        }
    }
    @Test @Tag("maps") void seededMultivalueDifferential() {
        for(boolean list:List.of(true,false))for(int seed=0;seed<6;seed++) {
            Random r=new Random(31L*seed);MultiValuedMap<Integer,Integer>a=factory(list?"listValuedMap":"setValuedMap");
            MultiValuedMap<Integer,Integer>b=list?new ArrayListValuedHashMap<>():new HashSetValuedHashMap<>();
            for(int step=0;step<1500;step++) {
                Integer k=r.nextInt(9)==0?null:r.nextInt(12),v=r.nextInt(7)==0?null:r.nextInt(10);
                switch(r.nextInt(8)) {
                    case 0->assertEquals(b.put(k,v),a.put(k,v));case 1->assertEquals(b.removeMapping(k,v),a.removeMapping(k,v));
                    case 2->assertEquals(b.get(k).add(v),a.get(k).add(v));case 3->assertEquals(b.get(k).remove(v),a.get(k).remove(v));
                    case 4->assertEquals(b.putAll(k,Arrays.asList(v,v,3)),a.putAll(k,Arrays.asList(v,v,3)));
                    case 5->{a.get(k).clear();b.get(k).clear();}case 6->{a.keySet().remove(k);b.keySet().remove(k);}
                    default->{assertEquals(b.containsMapping(k,v),a.containsMapping(k,v));}
                }
                assertEquals(list?lists(b):sets(b),list?lists(a):sets(a));assertEquals(b.size(),a.size());assertEquals(b,a);assertEquals(a,b);assertEquals(b.hashCode(),a.hashCode());
            }
        }
    }
    @Test @Tag("maps") void bidiEvictionInverseIdentityAndNulls() {
        BidiMap<Integer,Integer>a=factory("bidiMap"),b=new DualLinkedHashBidiMap<>();
        assertSame(a,a.inverseBidiMap().inverseBidiMap());assertSame(a.inverseBidiMap(),a.inverseBidiMap());
        for(Integer k:Arrays.asList(1,2,null,4,1)){Integer v=k==null?null:k%2;assertEquals(b.put(k,v),a.put(k,v));assertEquals(b,a);assertEquals(b.inverseBidiMap(),a.inverseBidiMap());}
        a.inverseBidiMap().put(99,6);b.inverseBidiMap().put(99,6);assertEquals(b,a);
        assertEquals(b.removeValue(99),a.removeValue(99));assertEquals(b,a);assertEquals(new ArrayList<>(b.keySet()),new ArrayList<>(a.keySet()));
    }
    @Test @Tag("maps") void bidiEntryMutationAndInverseViewsStayConsistent() {
        BidiMap<Integer,Integer>a=factory("bidiMap");a.put(1,10);a.put(2,20);var first=a.entrySet().iterator().next();
        assertThrows(IllegalArgumentException.class,()->first.setValue(20));BidiMap<Integer,Integer> oracle=new DualLinkedHashBidiMap<>();oracle.put(1,10);oracle.put(2,20);
        // Apache 4.4 delegates after updating its backing entry, returning the NEW value.
        // Preserve the selected provider profile; strict Map.Entry old-value compatibility remains OPEN.
        assertEquals(oracle.entrySet().iterator().next().setValue(30),first.setValue(30));assertEquals(1,a.getKey(30));assertFalse(a.containsValue(10));
        var i=a.mapIterator();i.next();i.setValue(40);assertEquals(1,a.getKey(40));i.remove();assertFalse(a.containsKey(1));
        a.inverseBidiMap().values().remove(2);assertTrue(a.isEmpty());
    }
    @Test @Tag("maps") void seededBidiDifferentialIncludingBothDirections() {
        for(int seed=0;seed<8;seed++){Random r=new Random(seed);BidiMap<Integer,Integer>a=factory("bidiMap"),b=new DualLinkedHashBidiMap<>();
            for(int step=0;step<1500;step++){Integer k=r.nextInt(8)==0?null:r.nextInt(30),v=r.nextInt(8)==0?null:r.nextInt(30);
                switch(r.nextInt(7)){
                    case 0->assertEquals(b.put(k,v),a.put(k,v));case 1->assertEquals(b.remove(k),a.remove(k));case 2->assertEquals(b.removeValue(v),a.removeValue(v));
                    case 3->assertEquals(b.inverseBidiMap().put(v,k),a.inverseBidiMap().put(v,k));case 4->assertEquals(b.values().remove(v),a.values().remove(v));
                    case 5->assertEquals(b.keySet().remove(k),a.keySet().remove(k));default->{if(step%93==0){a.clear();b.clear();}}
                }
                assertEquals(b,a);assertEquals(a,b);assertEquals(b.inverseBidiMap(),a.inverseBidiMap());assertEquals(b.hashCode(),a.hashCode());
                assertEquals(new ArrayList<>(b.keySet()),new ArrayList<>(a.keySet()));assertEquals(a.size(),a.inverseBidiMap().size());
            }
        }
    }
    @Test @Tag("maps") void readOnlyViewsRemainLiveAndRejectNestedMutation() {
        MultiValuedMap<Integer,Integer> m=factory("listValuedMap");MultiValuedMap<Integer,Integer> u=wrap("unmodifiableMultiValuedMap",MultiValuedMap.class,m);
        m.put(1,2);assertEquals(List.of(2),new ArrayList<>(u.get(1)));assertThrows(UnsupportedOperationException.class,()->u.get(1).add(3));
        assertThrows(UnsupportedOperationException.class,()->u.asMap().get(1).clear());assertThrows(UnsupportedOperationException.class,()->u.clear());
        m.put(1,3);assertEquals(List.of(2,3),new ArrayList<>(u.get(1)));BidiMap<Integer,Integer>b=factory("bidiMap");BidiMap<Integer,Integer> ub=wrap("unmodifiableBidiMap",BidiMap.class,b);
        b.put(1,2);assertEquals(1,ub.getKey(2));assertThrows(UnsupportedOperationException.class,()->ub.inverseBidiMap().put(2,4));
        MultiSet<String>s=counts(),us=wrap("unmodifiableMultiSet",MultiSet.class,s);s.add("x",3);assertEquals(3,us.getCount("x"));assertThrows(UnsupportedOperationException.class,()->us.uniqueSet().clear());
        assertThrows(NullPointerException.class,()->wrap("unmodifiableMultiSet",MultiSet.class,null));
    }
    @Test @Tag("maps") void deepReadOnlyAsMapHasNoEntryArrayStreamOrDefaultMethodEscape() {
        for (String kind : List.of("listValuedMap", "setValuedMap")) {
            MultiValuedMap<Integer,Integer> source=factory(kind);
            source.putAll(1,List.of(2,3));
            MultiValuedMap<Integer,Integer> readOnly=wrap("unmodifiableMultiValuedMap",MultiValuedMap.class,source);
            Map<Integer,Collection<Integer>> map=readOnly.asMap();
            assertEquals(source,readOnly);assertEquals(readOnly,source);
            assertThrows(UnsupportedOperationException.class,()->map.values().iterator().next().clear());
            assertThrows(UnsupportedOperationException.class,()->map.entrySet().iterator().next().getValue().remove(2));
            assertThrows(UnsupportedOperationException.class,()->map.entrySet().iterator().next().setValue(List.of(9)));
            @SuppressWarnings("unchecked") Collection<Integer> fromArray=(Collection<Integer>)map.values().toArray()[0];
            assertThrows(UnsupportedOperationException.class,()->fromArray.add(9));
            assertThrows(UnsupportedOperationException.class,()->map.values().stream().findFirst().orElseThrow().clear());
            assertThrows(UnsupportedOperationException.class,()->map.compute(1,(k,v)->{v.clear();return v;}));
            assertThrows(UnsupportedOperationException.class,()->map.replaceAll((k,v)->List.of(9)));
            assertThrows(UnsupportedOperationException.class,()->map.get(1).removeIf(v->true));
            Collection<Integer> held=map.get(1);source.remove(1);source.put(1,7);
            assertEquals(List.of(7),new ArrayList<>(held));assertEquals(1,source.size());
            // Independently witness the pinned donor leak rather than accidentally copy it.
            var donor=new ArrayListValuedHashMap<Integer,Integer>();donor.put(1,2);
            var donorReadOnly=org.apache.commons.collections4.multimap.UnmodifiableMultiValuedMap.unmodifiableMultiValuedMap(donor);
            donorReadOnly.asMap().get(1).clear();assertTrue(donor.isEmpty());
        }
    }
    @Test @Tag("maps") void arbitraryProviderMarkersAndMutableEntriesDoNotEscapeReadOnly() {
        MultiValuedMap<Integer,Integer> source=factory("listValuedMap");source.put(1,2);
        class OtherProvider extends org.apache.commons.collections4.multimap.AbstractMultiValuedMapDecorator<Integer,Integer>
                implements org.apache.commons.collections4.Unmodifiable {
            private static final long serialVersionUID=1L;
            OtherProvider(){super(source);}
            @Override public Collection<Map.Entry<Integer,Integer>> entries() {
                return List.of(new AbstractMap.SimpleEntry<Integer,Integer>(1,2) {
                    private static final long serialVersionUID=1L;
                    @Override public Integer setValue(Integer value){source.remove(1);source.put(1,value);return super.setValue(value);}
                });
            }
        }
        MultiValuedMap<Integer,Integer> readOnly=wrap("unmodifiableMultiValuedMap",MultiValuedMap.class,new OtherProvider());
        assertSame(readOnly,wrap("unmodifiableMultiValuedMap",MultiValuedMap.class,readOnly));
        assertThrows(UnsupportedOperationException.class,()->readOnly.entries().iterator().next().setValue(9));
        assertEquals(List.of(2),new ArrayList<>(source.get(1)));
        assertThrows(UnsupportedOperationException.class,()->readOnly.mapIterator().setValue(9));
        assertThrows(UnsupportedOperationException.class,()->readOnly.keys().remove(1,1));
    }
    @Test @Tag("maps") void synchronizedCountsAndViewsUseSharedMutex() throws Exception {
        MultiSet<String> base=counts(),s=wrap("synchronizedMultiSet",MultiSet.class,base);int workers=4,steps=4000;
        ExecutorService pool=Executors.newFixedThreadPool(workers);CountDownLatch start=new CountDownLatch(1);
        try {List<Future<?>> futures=new ArrayList<>();for(int w=0;w<workers;w++)futures.add(pool.submit(()->{start.await();for(int n=0;n<steps;n++)s.add("x",1);return null;}));
            start.countDown();for(Future<?>f:futures)f.get(20,TimeUnit.SECONDS);assertEquals(workers*steps,s.getCount("x"));
            CountDownLatch entered=new CountDownLatch(1);Future<?> blocked;
            synchronized(s){blocked=pool.submit(()->{entered.countDown();s.uniqueSet().remove("x");});assertTrue(entered.await(5,TimeUnit.SECONDS));assertThrows(TimeoutException.class,()->blocked.get(30,TimeUnit.MILLISECONDS));assertEquals(workers*steps,s.getCount("x"));}
            blocked.get(5,TimeUnit.SECONDS);assertTrue(s.isEmpty());
        }finally{start.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test @Tag("collectors") void orderedParallelListAndSetCollection() {
        List<String> input=IntStream.range(0,3000).mapToObj(i->"k"+(i%31)).toList();Function<String,String>key=Function.identity();Function<String,Integer>value=String::length;
        Collector<String,?,ListValuedMap<String,Integer>>lc=collector("toListValuedMap",key,value);
        var sequential=input.stream().collect(lc);var parallel=input.parallelStream().collect(lc);assertEquals(sequential,parallel);assertEquals(input.size(),parallel.size());
        assertEquals(new ArrayList<>(sequential.keySet()),new ArrayList<>(parallel.keySet()));assertFalse(lc.characteristics().contains(Collector.Characteristics.CONCURRENT));assertFalse(lc.characteristics().contains(Collector.Characteristics.UNORDERED));
        Collector<String,?,SetValuedMap<String,Integer>>sc=collector("toSetValuedMap",key,value);assertEquals(input.stream().collect(sc),input.parallelStream().collect(sc));
        assertNotSame(Stream.<String>empty().collect(lc),Stream.<String>empty().collect(lc));
    }
    @Test @Tag("collectors") void multisetCollectorCombinesDistinctCountsAndAllowsNulls() {
        @SuppressWarnings("unchecked") Collector<String,?,MultiSet<String>>c=(Collector<String,?,MultiSet<String>>)call("M3ApacheCollectors","toMultiSet",new Class<?>[0]);
        var input=IntStream.range(0,10000).mapToObj(i->i%11==0?null:"k"+(i%17)).toList();
        MultiSet<String>a=input.stream().collect(c),b=input.parallelStream().collect(c);assertEquals(a,b);assertEquals(10000,b.size());assertFalse(c.characteristics().contains(Collector.Characteristics.CONCURRENT));
    }
    @Test @Tag("collectors") void collectorCallbackFailureAndNullFunctionRefusal() {
        assertThrows(NullPointerException.class,()->collector("toListValuedMap",null,String::length));
        Collector<String,?,ListValuedMap<String,Integer>> c=collector("toListValuedMap",Function.identity(),s->{throw new IllegalStateException("callback");});
        assertEquals("callback",assertThrows(IllegalStateException.class,()->Stream.of("x").collect(c)).getMessage());
    }
    @Test @Tag("collectors") void lossyBidiCombinerIsExplicitlyNotOffered() {
        BidiMap<String,Integer> whole=factory("bidiMap"),left=factory("bidiMap"),right=factory("bidiMap");
        whole.put("a",1);whole.put("b",1);whole.put("b",2);left.put("a",1);right.put("b",1);right.put("b",2);left.putAll(right);
        assertNotEquals(whole,left);assertEquals(Map.of("b",2),whole);
        assertTrue(Arrays.stream(loadCollectors().getMethods()).noneMatch(m->m.getName().equals("toBidiMap")));
    }
    static Class<?> loadCollectors(){try{return Class.forName("com.m3.collections.interop.M3ApacheCollectors");}catch(ClassNotFoundException e){throw new AssertionError(e);}}
}
