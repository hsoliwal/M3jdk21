// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class PackedHeapAtomsTest {
  @Test void geometryMatchesBinaryHeapLayout(){
    for(int size=0;size<10_000;size++){
      assertEquals(size>>>1,PackedHeapAtoms.firstLeaf(size));
      assertEquals((size>>>1)-1,PackedHeapAtoms.lastParent(size));
      for(int index=0;index<size;index++){
        assertEquals(index<(size>>>1),PackedHeapAtoms.hasChildren(index,size));
        int left=PackedHeapAtoms.left(index);
        int right=PackedHeapAtoms.right(index);
        assertEquals((index<<1)+1,left);
        assertEquals((index<<1)+2,right);
        if(index>0){
          int parent=PackedHeapAtoms.parent(index);
          assertEquals((index-1)>>>1,parent);
          assertTrue(leftOf(parent)==index||rightOf(parent)==index);
        }
      }
    }
  }
  private static int leftOf(int i){return (i<<1)+1;}
  private static int rightOf(int i){return (i<<1)+2;}
}
