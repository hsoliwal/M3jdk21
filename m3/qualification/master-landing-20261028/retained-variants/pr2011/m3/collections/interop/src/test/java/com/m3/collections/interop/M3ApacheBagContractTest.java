/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import static org.junit.jupiter.api.Assertions.*;

import com.m3.collections.M3CountedBag;
import com.m3.collections.M3IndexMaskCompactor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.junit.jupiter.api.Test;

/** Real Commons 4.5.0 donor-API differential over M3 arrays/counts. */
final class M3ApacheBagContractTest {
    @Test void randomizedOfficialHashBagOracle() {
        Bag<Integer> expected=new HashBag<>();
        Bag<Integer> actual=new M3ApacheBag<>();
        Random random=new Random(20261018L);
        for(int step=0;step<6000;step++) {
            Integer key=random.nextInt(17)==0?null:random.nextInt(43)-21;
            int count=random.nextInt(6);
            switch(random.nextInt(6)) {
                case 0 -> assertEquals(expected.add(key,count),actual.add(key,count));
                case 1 -> assertEquals(expected.remove(key,count),actual.remove(key,count));
                case 2 -> assertEquals(expected.remove(key),actual.remove(key));
                case 3 -> {
                    List<Integer> source=Arrays.asList(key,key,random.nextInt(11));
                    assertEquals(expected.addAll(source),actual.addAll(source));
                }
                case 4 -> {
                    List<Integer> source=Arrays.asList(key,key,random.nextInt(11));
                    assertEquals(expected.removeAll(source),actual.removeAll(source));
                }
                default -> {
                    List<Integer> source=Arrays.asList(key,key,random.nextInt(11));
                    assertEquals(expected.retainAll(source),actual.retainAll(source));
                }
            }
            assertEquals(expected.size(),actual.size(),"size step="+step);
            assertEquals(expected.uniqueSet(),actual.uniqueSet(),"distinct step="+step);
            for(Object item:expected.uniqueSet())assertEquals(expected.getCount(item),actual.getCount(item));
            assertEquals(expected,actual,"equals step="+step);
            assertEquals(expected.hashCode(),actual.hashCode(),"hash step="+step);
        }
    }

    @Test void overDemandRetainAllMatchesOfficialApacheDonor() {
        Bag<String> expected = new HashBag<>();
        Bag<String> actual = new M3ApacheBag<>();
        expected.add("x", 2); actual.add("x", 2);
        expected.add("y", 1); actual.add("y", 1);
        List<String> requested = List.of("x", "x", "x", "y");
        assertEquals(expected.retainAll(requested), actual.retainAll(requested));
        assertEquals(expected, actual);
        assertEquals(expected.size(), actual.size());
        // Apache's AbstractMapBag retains zero copies if otherCount > ownCount.
        assertEquals(0, actual.getCount("x"));
        assertEquals(1, actual.getCount("y"));
    }

    @Test void iteratorLiveUniqueAndRealPrimitiveMaskSelection() {
        M3ApacheBag<String> actual=new M3ApacheBag<>();
        M3CountedBag<String> store=actual.m3Backend();
        actual.add("a",3);
        actual.add("b",2);
        var unique=actual.uniqueSet();
        assertThrows(UnsupportedOperationException.class,()->unique.remove("a"));
        assertEquals(2,unique.size());
        actual.remove("b");
        assertEquals(1,unique.size());
        byte[] accepted={1};
        M3CountedBag<String> selected=store.selectedDistinct(accepted,1,M3IndexMaskCompactor.Backend.JAVA);
        assertEquals(3,selected.count("a"));
        if(M3IndexMaskCompactor.nativeAvailable()) {
            assertEquals(3,store.selectedDistinct(accepted,1,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).count("a"));
        } else {
            assertThrows(UnsatisfiedLinkError.class,()->store.selectedDistinct(accepted,1,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        actual.clear();
        assertTrue(unique.isEmpty());
    }

    @Test void allFiveProviderContractsUseM3Owners() {
        var jdk=M3LibraryCollections.<String>jdkList();
        var guava=M3LibraryCollections.<String,String>guavaListMultimap();
        var eclipse=new M3EclipseBag<String>();
        var apache=new M3ApacheBag<String>();
        var spring=M3LibraryCollections.<String,String>springMultiValueMap();
        for(String value:List.of("a","a","b")) {
            jdk.add(value);
            guava.put("group",value);
            eclipse.add(value);
            apache.add(value);
            spring.add("group",value);
        }
        assertEquals(List.of("a","a","b"),jdk);
        assertEquals(List.of("a","a","b"),new ArrayList<>(guava.get("group")));
        assertEquals(3,guava.size());
        assertEquals(3,eclipse.size());
        assertEquals(2,eclipse.occurrencesOf("a"));
        assertEquals(3,apache.size());
        assertEquals(2,apache.getCount("a"));
        assertEquals(1,spring.size()); // Spring MultiValueMap.size counts keys.
        assertEquals(List.of("a","a","b"),spring.get("group"));
    }
}
