/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import com.m3.collections.M3IndexMaskCompactor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Real five-vendor API differential over a single M3 SoA array-index owner. */
final class M3FiveLibraryArrayificationTest {
    @Test void indexedGatherRotationOccurrenceStatesAndFiveProviders() {
        String[] keys={"x","x","y","x"};
        Integer[] vals={5,6,7,6};
        int[] order={0,1,1,2,3};
        byte[] physicalFlags={1,2,3,4};
        var view=M3FiveLibraryIndexViews.capture(keys,vals,order,physicalFlags);
        var taken=view.takeOrdinals(new int[]{4,1,1});
        assertEquals(List.of(6,6,6),taken.asJdkList());
        assertEquals(3,taken.asGuavaListMultimap().size());
        assertEquals(List.of(6,6,6),taken.asGuavaListMultimap().get("x"));
        assertEquals(List.of(6,6,6),taken.asEclipseListMultimap().get("x").toList());
        assertEquals(List.of(6,6,6),taken.asSpringMultiValueMap().get("x"));
        assertEquals(List.of(6,6,6),taken.asApacheListValuedMap().get("x"));
        assertEquals(1,taken.asSpringMultiValueMap().size()); // key count, not pair count
        assertEquals(3,taken.asApacheListValuedMap().size());
        assertEquals(List.of(6,5,6,6,7),view.rotated(1).asJdkList());
        assertEquals(List.of(6,5,6,6,7),view.rotated(-4).asJdkList());
        byte[] occurrenceFlags={1,2,0,2,0};
        assertEquals(List.of(6,7),view.selectedByOccurrenceStates(occurrenceFlags,2,0).asJdkList());
        assertEquals(List.of(),view.selectedByOccurrenceStates(occurrenceFlags,2,2).asJdkList());
        if (M3IndexMaskCompactor.nativeAvailable()) {
            assertEquals(List.of(6,7),view.selectedByOccurrenceStates(occurrenceFlags,2,0,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).asJdkList());
            assertEquals(List.of(),view.selectedByOccurrenceStates(occurrenceFlags,2,2,
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED).asJdkList());
        } else {
            assertThrows(UnsatisfiedLinkError.class, () -> view.selectedByOccurrenceStates(
                    occurrenceFlags,2,2,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED));
        }
        assertArrayEquals(order,new int[]{0,1,1,2,3});
        assertEquals(Integer.valueOf(5),view.valueAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> view.takeOrdinals(new int[]{5}));
        assertThrows(IllegalArgumentException.class, () -> view.selectedByOccurrenceStates(new byte[4],0,0));
    }

    @Test void stableSortedMergingDistinctAndRepeatedOccurrences() {
        String[] keys={"a","a","b","b","c"};
        Integer[] values={0,1,2,3,4};
        byte[] flags=new byte[5];
        var root=M3FiveLibraryIndexViews.capture(keys,values,new int[]{0,2,1,3,4},flags);
        var sorted=root.sortedByKey(Comparator.naturalOrder());
        var left=sorted.slice(0,3);
        var right=sorted.slice(3,5);
        var merged=left.mergeSortedByKey(right,Comparator.naturalOrder());
        assertEquals(sorted.asJdkList(),merged.asJdkList());
        assertArrayEquals(new int[]{0,1,2,3,4},java.util.stream.IntStream.range(0,merged.size())
                .map(merged::physicalIndexAt).toArray());
        assertThrows(IllegalArgumentException.class,()->root.mergeSortedByKey(right,Comparator.naturalOrder()));
        assertThrows(IllegalArgumentException.class,()->left.mergeSortedByKey(
                M3FiveLibraryIndexViews.capture(keys,values,new int[]{0},flags),Comparator.naturalOrder()));
        var duplicated=left.mergeSortedByKey(left,Comparator.naturalOrder());
        // Stable comparator ties retain each left run before the corresponding right run.
        assertEquals(List.of(0,1,0,1,2,2),duplicated.asJdkList());
    }

    @Test void randomizedArrayificationAgainstJdkReference() {
        Random random=new Random(0x5f17a4518L);
        for(int trial=0;trial<1700;trial++){
            int n=random.nextInt(100);
            String[] keys=new String[n]; Integer[] values=new Integer[n];byte[] flags=new byte[n];
            for(int i=0;i<n;i++){keys[i]=Integer.toString(random.nextInt(11)-5);values[i]=i; flags[i]=(byte)random.nextInt(256);}
            int m=n==0?0:random.nextInt(135);
            int[] order=new int[m];byte[] occurrenceStates=new byte[m];
            for(int i=0;i<m;i++){order[i]=random.nextInt(n);occurrenceStates[i]=(byte)random.nextInt(256);}
            var source=M3FiveLibraryIndexViews.capture(keys,values,order,flags);
            int[] take=new int[m==0?0:random.nextInt(m+1)];
            for(int i=0;i<take.length;i++)take[i]=random.nextInt(m);
            List<Integer> takenReference=new ArrayList<>();
            for(int j:take)takenReference.add(values[order[j]]);
            assertEquals(takenReference,source.takeOrdinals(take).asJdkList());
            int distance=random.nextInt();List<Integer> rotated=new ArrayList<>();
            int shift=m==0?0:Math.floorMod(distance,m);
            for(int i=0;i<m;i++)rotated.add(values[order[(i-shift+m)%m]]);
            assertEquals(rotated,source.rotated(distance).asJdkList());
            int required=random.nextInt(256),excluded=random.nextInt(256);
            List<Integer> selected=new ArrayList<>();
            for(int i=0;i<m;i++){
                int state=occurrenceStates[i]&255;
                if((state&required)==required && (state&excluded)==0)selected.add(values[order[i]]);
            }
            assertEquals(selected,source.selectedByOccurrenceStates(occurrenceStates,required,excluded).asJdkList());
            List<Integer> sortedRows=new ArrayList<>();for(int x:order)sortedRows.add(x);
            sortedRows.sort(Comparator.comparing(x->keys[x]));
            var sortedSource=source.sortedByKey(Comparator.naturalOrder());
            int cut=random.nextInt(m+1);
            var merged=sortedSource.slice(0,cut).mergeSortedByKey(sortedSource.slice(cut,m),Comparator.naturalOrder());
            assertEquals(sortedSource.asJdkList(),merged.asJdkList());
            for(int i=0;i<m;i++)assertEquals(sortedRows.get(i).intValue(),merged.physicalIndexAt(i));
            assertArrayEquals(order,Arrays.copyOf(order,order.length));
        }
    }
}
