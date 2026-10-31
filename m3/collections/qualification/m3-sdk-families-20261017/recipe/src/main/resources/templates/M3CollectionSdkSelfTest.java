/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/** Standalone Java 21 SDK naming and existing-wrapper differential oracle. */
public final class M3CollectionSdkSelfTest {
    private static int checks;
    private M3CollectionSdkSelfTest() { }
    private static void check(boolean value, String issue) {
        checks++;
        if (!value) throw new AssertionError(issue);
    }
    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (kind.isInstance(problem)) return;
            throw new AssertionError("expected " + kind + ", got " + problem, problem);
        }
        throw new AssertionError("expected " + kind);
    }
    private static void wrappers() {
        ArrayList<Integer> backing = new ArrayList<>(List.of(5, 2, 3, 4, 1, 6));
        List<Integer> live = M3CollectionSdk.filteredList(backing, x -> (x & 1) == 0);
        check(live.equals(List.of(2,4,6)), "live predicate/list bridge");
        backing.add(8);
        check(live.equals(List.of(2,4,6,8)), "live backing visibility");
        var readonly = M3CollectionSdk.readOnlyFilteredList(backing, x -> x < 5);
        expect(UnsupportedOperationException.class, () -> readonly.add(0));
        TreeSet<Integer> ints = new TreeSet<>(List.of(1,2,3,4,5,6));
        NavigableSet<Integer> filtered = M3CollectionSdk.readOnlyFilteredNavigableSet(ints, x -> (x & 1)==1);
        check(filtered.equals(new TreeSet<>(List.of(1,3,5))), "sorted set values");
        check(filtered.descendingSet().first() == 5, "descending navigation");
        ints.add(7);
        check(filtered.last() == 7, "live navigable set");
        TreeMap<Integer, Integer> map = new TreeMap<>(Map.of(1, 10, 2, 20, 3, 30));
        NavigableMap<Integer,Integer> view = M3CollectionSdk.readOnlyFilteredNavigableMap(map, (k,v)->v>=20);
        check(view.firstKey() == 2 && view.lastKey() == 3, "sorted map predicate");
        Random r = new Random(0x3efaac71L);
        for (int trial=0;trial<240;trial++) {
            int n=r.nextInt(90); long[] source=new long[n];
            for (int i=0;i<n;i++) source[i]=r.nextInt(15)-7;
            AtomicInteger evaluations = new AtomicInteger();
            var snapshot = M3CollectionSdk.filteredLongSnapshot(source, x->{
                evaluations.incrementAndGet(); return x >= 0;
            });
            check(evaluations.get()==n, "one predicate call per occurrence");
            long[] expected=Arrays.stream(source).filter(x->x>=0).toArray();
            Arrays.fill(source, -77);
            check(Arrays.equals(snapshot.toArray(),expected), "snapshot isolation");
            snapshot.longStream().sum(); snapshot.size(); snapshot.sortedDescending();
            check(evaluations.get()==n, "no filter reevaluation");
            var sort=M3CollectionSdk.sortedLongSnapshot(expected);
            long[] sorted=expected.clone(); Arrays.sort(sorted);
            check(Arrays.equals(sorted,sort.toArray()), "sorted index semantics");
            if (M3IndexMaskCompactor.nativeAvailable()) {
                check(Arrays.equals(snapshot.toArray(), M3CollectionSdk.filteredLongSnapshot(
                        expected,x->true,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).toArray()),
                        "native parity on admitted values");
            } else {
                expect(UnsatisfiedLinkError.class, () -> M3CollectionSdk.filteredLongSnapshot(
                        expected,x->true,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
            }
        }
    }
    public static void main(String[] args) throws Exception {
        checks += M3CollectionSdkCatalogSelfTest.verify();
        wrappers();
        System.out.println("PASS M3CollectionSdkSelfTest checks="+checks
                +" catalog="+M3CollectionSdkCatalog.size()
                +" native="+M3IndexMaskCompactor.nativeAvailable());
    }
}
