// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.*;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class PackedHeapAtomParityTest {
  @Test void packedLongMinMaxMatchReference(){
    Random random=new Random(0x4845415041544f4dL);
    PackedLongHeap min=PackedLongHeap.minHeap(1);
    PackedLongHeap max=PackedLongHeap.maxHeap(1);
    PriorityQueue<Long> minRef=new PriorityQueue<>();
    PriorityQueue<Long> maxRef=new PriorityQueue<>(java.util.Comparator.reverseOrder());
    for(int i=0;i<20_000;i++){
      long v=switch(i&31){case 0->Long.MIN_VALUE;case 1->Long.MAX_VALUE;default->random.nextLong();};
      min.offer(v);max.offer(v);minRef.add(v);maxRef.add(v);
      if((i&7)==7){
        assertEquals(minRef.remove().longValue(),min.poll());
        assertEquals(maxRef.remove().longValue(),max.poll());
      }
    }
    while(!minRef.isEmpty())assertEquals(minRef.remove().longValue(),min.poll());
    while(!maxRef.isEmpty())assertEquals(maxRef.remove().longValue(),max.poll());
  }

  @Test void primitiveNumericHeapsPreserveOrdering(){
    var ints=new PrimitivePriorityQueues.IntMinHeap(1);
    for(int i=1000;i>=-1000;i--)ints.add(i);
    for(int i=-1000;i<=1000;i++)assertEquals(i,ints.removeMin());

    var longs=new PrimitivePriorityQueues.LongMinHeap(1);
    long[] lv={Long.MAX_VALUE,0,-1,1,Long.MIN_VALUE,17,-17};
    for(long v:lv)longs.add(v);
    java.util.Arrays.sort(lv);
    for(long v:lv)assertEquals(v,longs.removeMin());

    var floats=new PrimitivePriorityQueues.FloatMinHeap(1);
    float[] fv={Float.NaN,-0f,0f,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,-1f,1f};
    for(float v:fv)floats.add(v);
    java.util.Arrays.sort(fv);
    for(float v:fv)assertEquals(Float.floatToRawIntBits(v),Float.floatToRawIntBits(floats.removeMin()));

    var doubles=new PrimitivePriorityQueues.DoubleMinHeap(1);
    double[] dv={Double.NaN,-0d,0d,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,-1d,1d};
    for(double v:dv)doubles.add(v);
    java.util.Arrays.sort(dv);
    for(double v:dv)assertEquals(Double.doubleToRawLongBits(v),Double.doubleToRawLongBits(doubles.removeMin()));
  }

  @Test void indexedHeapPreservesTieBreakAndUpdates(){
    DenseIndexedLongHeap heap=new DenseIndexedLongHeap(16,true);
    heap.put(7,10);heap.put(2,10);heap.put(5,5);
    assertEquals(5,heap.pollId());
    assertEquals(2,heap.peekId());
    heap.put(7,1);
    assertEquals(7,heap.peekId());
    assertTrue(heap.remove(7));
    assertEquals(2,heap.peekId());
  }
}
