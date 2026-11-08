// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.range;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PackedRangeAtomsTest {
    @Test
    void containmentAndIntersectionSearchCoverBoundaries() {
        long[] starts={Long.MIN_VALUE,-10,5,100};
        long[] ends={-20,-5,20,Long.MAX_VALUE};
        assertEquals(0,PackedRangeAtoms.containing(starts,ends,4,Long.MIN_VALUE));
        assertEquals(0,PackedRangeAtoms.containing(starts,ends,4,-20));
        assertEquals(-1,PackedRangeAtoms.containing(starts,ends,4,-19));
        assertEquals(1,PackedRangeAtoms.containing(starts,ends,4,-7));
        assertEquals(2,PackedRangeAtoms.containing(starts,ends,4,20));
        assertEquals(-1,PackedRangeAtoms.containing(starts,ends,4,21));
        assertEquals(3,PackedRangeAtoms.containing(starts,ends,4,Long.MAX_VALUE));

        assertEquals(0,PackedRangeAtoms.firstIntersection(ends,4,Long.MIN_VALUE));
        assertEquals(1,PackedRangeAtoms.firstIntersection(ends,4,-19));
        assertEquals(2,PackedRangeAtoms.firstIntersection(ends,4,0));
        assertEquals(3,PackedRangeAtoms.firstIntersection(ends,4,99));
    }

    @Test
    void adjacencyNeverOverflows() {
        assertTrue(PackedRangeAtoms.adjacent(4,5));
        assertFalse(PackedRangeAtoms.adjacent(4,6));
        assertFalse(PackedRangeAtoms.adjacent(Long.MAX_VALUE,Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE,PackedRangeAtoms.leftRemainderEnd(Long.MIN_VALUE+1));
        assertEquals(Long.MAX_VALUE,PackedRangeAtoms.rightRemainderStart(Long.MAX_VALUE-1));
        assertThrows(ArithmeticException.class,
                () -> PackedRangeAtoms.leftRemainderEnd(Long.MIN_VALUE));
        assertThrows(ArithmeticException.class,
                () -> PackedRangeAtoms.rightRemainderStart(Long.MAX_VALUE));
    }

    @Test
    void exactSpanPreservesOverflowContract() {
        assertEquals(1,PackedRangeAtoms.inclusiveSpanExact(7,7));
        assertEquals(3,PackedRangeAtoms.inclusiveSpanExact(-1,1));
        assertThrows(IllegalArgumentException.class,
                () -> PackedRangeAtoms.inclusiveSpanExact(1,0));
        assertThrows(ArithmeticException.class,
                () -> PackedRangeAtoms.inclusiveSpanExact(Long.MIN_VALUE,Long.MAX_VALUE));
    }

    @Test
    void rangeMapStillSplitsOverwritesAndCoalesces() {
        PackedLongRangeMap map=new PackedLongRangeMap();
        map.put(Long.MIN_VALUE,-1,1);
        map.put(0,10,2);
        map.put(11,Long.MAX_VALUE,3);
        assertEquals(3,map.rangeCount());

        map.put(-5,15,9);
        assertEquals(3,map.rangeCount());
        assertEquals(Long.MIN_VALUE,map.startAt(0));
        assertEquals(-6,map.endAt(0));
        assertEquals(1,map.valueAt(0));
        assertEquals(-5,map.startAt(1));
        assertEquals(15,map.endAt(1));
        assertEquals(9,map.valueAt(1));
        assertEquals(16,map.startAt(2));
        assertEquals(Long.MAX_VALUE,map.endAt(2));
        assertEquals(3,map.valueAt(2));

        map.put(16,20,9);
        assertEquals(3,map.rangeCount());
        assertEquals(20,map.endAt(1));

        map.remove(-2,18);
        assertEquals(4,map.rangeCount());
        assertTrue(map.contains(-3));
        assertFalse(map.contains(0));
        assertTrue(map.contains(19));
    }

    @Test
    void rangeSetRemainsTwoLaneCoalescingFacade() {
        PackedLongRangeSet set=new PackedLongRangeSet();
        set.add(1,3);
        set.add(4,6);
        assertEquals(1,set.rangeCount());
        assertEquals(1,set.startAt(0));
        assertEquals(6,set.endAt(0));
        assertEquals(6,set.cardinalityExact());

        set.remove(3,4);
        assertEquals(2,set.rangeCount());
        assertEquals(4,set.cardinalityExact());
        assertArrayEquals(new long[]{1,2,5,6}, points(set));
    }

    @Test
    void intersectionsStaySortedAndClipped() {
        PackedLongRangeMap map=new PackedLongRangeMap();
        map.put(0,5,10);
        map.put(10,15,20);
        map.put(20,25,30);
        List<Long> rows=new ArrayList<>();
        map.intersections(3,22,(a,b,v)->{
            rows.add(a); rows.add(b); rows.add(v);
        });
        assertEquals(List.of(3L,5L,10L,10L,15L,20L,20L,22L,30L),rows);
    }

    private static long[] points(PackedLongRangeSet set) {
        LongArray out=new LongArray();
        set.forEach((a,b)->{
            for(long value=a;;value++){
                out.add(value);
                if(value==b)break;
            }
        });
        return out.toArray();
    }

    private static final class LongArray {
        private long[] values=new long[8];
        private int size;
        void add(long value){
            if(size==values.length)values=java.util.Arrays.copyOf(values,size*2);
            values[size++]=value;
        }
        long[] toArray(){return java.util.Arrays.copyOf(values,size);}
    }
}
