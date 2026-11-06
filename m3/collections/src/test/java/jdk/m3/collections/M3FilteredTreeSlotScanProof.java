/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.SortedMap;
import java.util.function.BiPredicate;

/** Product differential oracle for M3 slot scanning vs public Map.Entry traversal. */
public final class M3FilteredTreeSlotScanProof {
    private static long checks;
    private M3FilteredTreeSlotScanProof() { }
    private static void same(Object expected, Object actual, String message) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + ": expected " + expected + " got " + actual);
        }
    }
    private static <K,V> int baseline(SortedMap<K,V> source,
            BiPredicate<? super K,? super V> predicate, boolean firstOnly) {
        int matches = 0;
        for (Map.Entry<K,V> entry : source.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                matches++;
                if (firstOnly) break;
            }
        }
        return matches;
    }
    private static void differential(int seed, boolean navigable) {
        Random random = new Random(seed * 0x9e3779b97f4a7c15L);
        List<String> events = new ArrayList<>();
        Comparator<Integer> comparator = (a,b) -> {
            events.add("C" + a + ":" + b);
            return Integer.compare(a,b);
        };
        M3TreeMap<Integer,Integer> map = new M3TreeMap<>(comparator);
        int count = seed % 31;
        for (int j=0;j<count;j++) {
            map.put(random.nextInt(59)-29,
                    random.nextInt(6)==0 ? null : random.nextInt(100));
        }
        BiPredicate<Integer,Integer> predicate = (k,v) -> {
            events.add("P" + k + ":" + v);
            return (k & 1)==0 || (v != null && v % 7 == 0);
        };
        SortedMap<Integer,Integer> view = navigable
                ? M3FilteredViews.filterEntries((NavigableMap<Integer,Integer>)map,predicate)
                : M3FilteredViews.filterEntries((SortedMap<Integer,Integer>)map,predicate);
        events.clear();
        int expectedCount = baseline(map,predicate,false);
        List<String> expectedEvents = List.copyOf(events);
        events.clear();
        same(expectedCount,view.size(),"size");
        same(expectedEvents,List.copyOf(events),"size callback order");
        events.clear();
        boolean expectedEmpty = baseline(map,predicate,true)==0;
        expectedEvents=List.copyOf(events);
        events.clear();
        same(expectedEmpty,view.isEmpty(),"isEmpty");
        same(expectedEvents,List.copyOf(events),"isEmpty callback order");
        if(seed%7==0) {
            map.put(1000+seed,seed);
            events.clear();
            expectedCount=baseline(map,predicate,false);
            expectedEvents=List.copyOf(events);
            events.clear();
            same(expectedCount,view.size(),"live view size");
            same(expectedEvents,List.copyOf(events),"live view callback order");
        }
    }
    private static void mutationDuringSize(boolean accepted) {
        M3TreeMap<Integer,Integer> map = new M3TreeMap<>();
        for(int i=0;i<6;i++) map.put(i,i);
        BiPredicate<Integer,Integer> predicate = (k,v)->{
            if(k==1) map.put(90,90);
            return accepted;
        };
        Class<?> oldFailure = null, newFailure = null;
        try { baseline(map,predicate,false); }
        catch (RuntimeException thrown) { oldFailure=thrown.getClass(); }
        map.remove(90);
        try { M3FilteredViews.filterEntries((SortedMap<Integer,Integer>)map,predicate).size(); }
        catch (RuntimeException thrown) { newFailure=thrown.getClass(); }
        same(oldFailure,newFailure,"structural mutation fails consistently");
    }
    public static void main(String[] args) {
        for(int seed=0;seed<450;seed++) {
            differential(seed,false);
            differential(seed,true);
        }
        mutationDuringSize(false);
        mutationDuringSize(true);
        System.out.println("PASS M3FilteredTreeSlotScanProof assertions=" + checks);
    }
}
