// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class M3PackedSlotLinkAtomsTest {
    @Test
    void insertionOrderedPrimitiveMapMatchesLinkedHashMap() {
        M3PackedLongLongOrderedMap actual = new M3PackedLongLongOrderedMap(2);
        LinkedHashMap<Long,Long> expected = new LinkedHashMap<>();
        Random random = new Random(0x4f52444552L);
        for (int round=0; round<10_000; round++) {
            long key=random.nextInt(257)-128L, value=random.nextLong();
            switch (round & 7) {
                case 0,1,2 -> { boolean inserted=!expected.containsKey(key); expected.put(key,value); assertEquals(inserted,actual.put(key,value)); }
                case 3 -> assertEquals(expected.remove(key)!=null,actual.remove(key));
                case 4 -> { if(expected.containsKey(key)){ expected.remove(key); expected.put(key,value); assertTrue(actual.put(key,value)==false); assertTrue(actual.moveToLast(key)); } }
                case 5 -> assertEquals(expected.containsKey(key),actual.containsKey(key));
                case 6 -> assertEquals(expected.getOrDefault(key,Long.MIN_VALUE),actual.getOrDefault(key,Long.MIN_VALUE));
                default -> { if(!expected.isEmpty()){ long first=expected.keySet().iterator().next(); assertEquals(first,actual.firstKey()); } }
            }
            if ((round & 127)==0) {
                assertArrayEquals(expected.keySet().stream().mapToLong(Long::longValue).toArray(),actual.keysToArray());
                assertArrayEquals(expected.values().stream().mapToLong(Long::longValue).toArray(),actual.valuesToArray());
            }
        }
    }

    @Test
    void accessOrderedMapMatchesLinkedHashMap() {
        M3PackedLongLongOrderedMap actual = new M3PackedLongLongOrderedMap(4,true);
        LinkedHashMap<Long,Long> expected = new LinkedHashMap<>(16,.75f,true);
        for(long i=0;i<32;i++){actual.put(i,i*10);expected.put(i,i*10);}
        for(long i=0;i<1_000;i++){
            long key=(i*17)&31;
            assertEquals(expected.get(key).longValue(),actual.getOrThrow(key));
            if((i&15)==0) assertArrayEquals(expected.keySet().stream().mapToLong(Long::longValue).toArray(),actual.keysToArray());
        }
    }

    @Test
    void denseOrderedSetMatchesLinkedHashSetAcrossSlotReuse() {
        M3DenseOrderedSet<Long> actual = new M3DenseOrderedSet<>(2);
        LinkedHashSet<Long> expected = new LinkedHashSet<>();
        Random random = new Random(0x534c4f54L);
        for(int round=0;round<10_000;round++){
            long value=random.nextInt(257)-128L;
            if((round&1)==0) assertEquals(expected.add(value),actual.add(value));
            else assertEquals(expected.remove(value),actual.remove(value));
            if((round&127)==0){
                List<Long> got=new ArrayList<>(); actual.forEach(got::add);
                assertEquals(new ArrayList<>(expected),got);
            }
        }
    }
}
