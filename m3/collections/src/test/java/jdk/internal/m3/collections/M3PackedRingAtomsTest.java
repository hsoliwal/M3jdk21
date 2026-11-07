// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class M3PackedRingAtomsTest {
  @Test void powerOfTwoIndexArithmeticWrapsExactly(){
    for(int capacity=2;capacity<=1024;capacity<<=1){
      for(int head=0;head<capacity;head++){
        for(int logical=0;logical<capacity;logical++){
          assertEquals((head+logical)&(capacity-1),M3PackedRingAtoms.powerOfTwoIndex(head,logical,capacity));
        }
        assertEquals((head-1)&(capacity-1),M3PackedRingAtoms.powerOfTwoPrevious(head,capacity));
        assertEquals((head+1)&(capacity-1),M3PackedRingAtoms.powerOfTwoNext(head,capacity));
      }
    }
  }
  @Test void arbitraryRingArithmeticMatchesModulo(){
    for(int capacity:new int[]{1,3,5,17,31}){
      for(int head=0;head<capacity;head++){
        for(int logical=0;logical<capacity;logical++){
          assertEquals((head+logical)%capacity,M3PackedRingAtoms.index(head,logical,capacity));
        }
        assertEquals((head+1)%capacity,M3PackedRingAtoms.next(head,capacity));
      }
    }
  }
  @Test void headNormalizationIsOnlyForEmptyState(){
    assertEquals(0,M3PackedRingAtoms.normalizeHeadAfterRemoval(7,0));
    assertEquals(7,M3PackedRingAtoms.normalizeHeadAfterRemoval(7,1));
  }
}
