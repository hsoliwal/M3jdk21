/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections;
import java.util.Arrays;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatIntMultiMap.java}
 * blob {@code 5f03bc719e8f7a42fec6fbf653fc82acc558feb0} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatIntMultiMap{
  private final int[] keys,offsets,values;private final M3MatCollectionPlan plan;
  private M3MatIntMultiMap(int[] keys,int[] offsets,int[] values){this.keys=keys;this.offsets=offsets;this.values=values;long b=4L*(keys.length+offsets.length+values.length);this.plan=new M3MatCollectionPlan(M3MatStorageKind.MULTIMAP_CSR,b,values.length,keys.length,"sorted keys + offsets + flat values");}
  public static Builder builder(){return new Builder();} public int keyCount(){return keys.length;}public int pairCount(){return values.length;}public int[] keys(){return keys.clone();}public M3MatCollectionPlan plan(){return plan;}public long primitivePayloadBytes(){return plan.primitivePayloadBytes();}
  public M3MatIntList values(int key){int i=Arrays.binarySearch(keys,key);return i<0?new M3MatIntList(new int[0]):M3MatIntList.trustedView(values,offsets[i],offsets[i+1]-offsets[i]);}
  public static final class Builder{
      /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
      public Builder() {
      }

    private int[] k=new int[16],v=new int[16];private int size;
    public Builder put(int key,int value){if(size==k.length){k=Arrays.copyOf(k,k.length*2);v=Arrays.copyOf(v,v.length*2);}k[size]=key;v[size]=value;size++;return this;}
    /** Sort by signed key, then original insertion index, without boxing row ordinals. */
    public M3MatIntMultiMap build(){
      if(size==0)return new M3MatIntMultiMap(new int[0],new int[]{0},new int[0]);
      long[] ordered=new long[size];
      for(int i=0;i<size;i++)ordered[i]=((long)k[i]<<32)|(i&0xffff_ffffL);
      // Signed long order preserves signed int key order; the nonnegative index
      // in the lower lane preserves insertion order for every duplicate key.
      Arrays.sort(ordered);
      int unique=1;
      for(int i=1;i<size;i++)if((int)(ordered[i]>>32)!=(int)(ordered[i-1]>>32))unique++;
      int[] ok=new int[unique],off=new int[unique+1],ov=new int[size];
      int g=-1,prev=0;
      for(int i=0;i<size;i++){
        long indexed=ordered[i];int key=(int)(indexed>>32),src=(int)indexed;
        if(i==0||key!=prev){g++;ok[g]=key;off[g]=i;prev=key;}
        ov[i]=v[src];
      }
      off[unique]=size;
      return new M3MatIntMultiMap(ok,off,ov);
    }
  }
}
