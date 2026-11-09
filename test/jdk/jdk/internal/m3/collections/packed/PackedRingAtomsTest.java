// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class PackedRingAtomsTest {
  @Test void powerOfTwoIndexArithmeticWrapsExactly(){
    for(int capacity=2;capacity<=1024;capacity<<=1){
      for(int head=0;head<capacity;head++){
        for(int logical=0;logical<capacity;logical++){
          assertEquals((head+logical)&(capacity-1),PackedRingAtoms.powerOfTwoIndex(head,logical,capacity));
        }
        assertEquals((head-1)&(capacity-1),PackedRingAtoms.powerOfTwoPrevious(head,capacity));
        assertEquals((head+1)&(capacity-1),PackedRingAtoms.powerOfTwoNext(head,capacity));
      }
    }
  }
  @Test void arbitraryRingArithmeticMatchesModulo(){
    for(int capacity:new int[]{1,3,5,17,31}){
      for(int head=0;head<capacity;head++){
        for(int logical=0;logical<capacity;logical++){
          assertEquals((head+logical)%capacity,PackedRingAtoms.index(head,logical,capacity));
        }
        assertEquals((head+1)%capacity,PackedRingAtoms.next(head,capacity));
      }
    }
  }
  @Test void headNormalizationIsOnlyForEmptyState(){
    assertEquals(0,PackedRingAtoms.normalizeHeadAfterRemoval(7,0));
    assertEquals(7,PackedRingAtoms.normalizeHeadAfterRemoval(7,1));
  }
}
