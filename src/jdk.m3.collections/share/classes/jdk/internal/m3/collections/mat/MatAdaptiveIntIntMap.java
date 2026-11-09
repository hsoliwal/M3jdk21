/* *****************************************************************************
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
package jdk.internal.m3.collections.mat;
import java.util.Arrays;
import java.util.Objects;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatAdaptiveIntIntMap.java}
 * blob {@code 3c734139c82ee9c103c0a7965cdfcc42c1cb2f0c} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatAdaptiveIntIntMap {
  public enum Encoding { DIRECT, SORTED, HASH }
  private final Encoding encoding; private final int size; private final int minKey;
  private final int[] keys,values,states; private final long[] presence; private final MatCollectionPlan plan;
  private MatAdaptiveIntIntMap(Encoding e,int size,int minKey,int[] keys,int[] values,long[] presence,int[] states,MatCollectionPlan plan){
    this.encoding=e;this.size=size;this.minKey=minKey;this.keys=keys;this.values=values;this.presence=presence;this.states=states;this.plan=plan;
  }
  public static Builder builder(){return new Builder();} public static Builder builder(MatCollectionPolicy p){return new Builder().policy(p);}
  public int size(){return size;} public Encoding encoding(){return encoding;} public MatCollectionPlan plan(){return plan;}
  public long primitivePayloadBytes(){return plan.primitivePayloadBytes();}
  public boolean containsKey(int key){return switch(encoding){case DIRECT->directPresent(key);case SORTED->Arrays.binarySearch(keys,key)>=0;case HASH->hashSlot(key)>=0;};}
  public int get(int key){return get0(key,false,0);} public int getOrDefault(int key,int fallback){return get0(key,true,fallback);}
  public int[] keys(){return switch(encoding){case SORTED->keys.clone();case DIRECT->directKeys();case HASH->hashKeys();};}
  public int[] valuesBySortedKey(){int[] k=keys(),r=new int[k.length];for(int i=0;i<k.length;i++)r[i]=get(k[i]);return r;}
  private int get0(int key,boolean def,int fallback){
    if(encoding==Encoding.DIRECT){long o=(long)key-minKey;if(o>=0&&o<values.length&&bit(presence,(int)o))return values[(int)o];}
    else if(encoding==Encoding.SORTED){int i=Arrays.binarySearch(keys,key);if(i>=0)return values[i];}
    else{int i=hashSlot(key);if(i>=0)return values[i];}
    if(def)return fallback;throw new IllegalArgumentException("missing key: "+key);
  }
  private boolean directPresent(int key){long o=(long)key-minKey;return o>=0&&o<values.length&&bit(presence,(int)o);}
  private int[] directKeys(){int[] r=new int[size];int p=0;for(int i=0;i<values.length;i++)if(bit(presence,i))r[p++]=minKey+i;return r;}
  private int hashSlot(int key){int m=states.length-1,s=mix(key)&m,d=1;for(int p=0;p<states.length;p++,d++){int r=states[s];if(r==0||r<d)return-1;if(keys[s]==key)return s;s=(s+1)&m;}return-1;}
  private int[] hashKeys(){int[]r=new int[size];int p=0;for(int i=0;i<states.length;i++)if(states[i]!=0)r[p++]=keys[i];Arrays.sort(r);return r;}
  private static boolean bit(long[] w,int i){return(w[i>>>6]&(1L<<(i&63)))!=0;}
  private static int mix(int v){v^=v>>>16;v*=0x7feb352d;v^=v>>>15;v*=0x846ca68b;return v^(v>>>16);}
  private static int cap(int n){int r=2,need=Math.max(2,Math.multiplyExact(n,2));while(r<need)r<<=1;return r;}
  public static final class Builder{
      /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
      public Builder() {
      }

    private int[] k=new int[16],v=new int[16],lookup=new int[32];private int size;private MatCollectionPolicy policy=MatCollectionPolicy.BALANCED;private int maxDirect=16_777_216;
    public Builder policy(MatCollectionPolicy p){policy=Objects.requireNonNull(p);return this;} public Builder maxDirectSpan(int x){if(x<0)throw new IllegalArgumentException();maxDirect=x;return this;}
    public Builder put(int key,int value){int s=find(key),stored=lookup[s];if(stored!=0){v[stored-1]=value;return this;}insertAbsent(key,value,s);return this;}
    /** Shared primitive insertion atom, applied only after a failed lookup. */
    private void insertAbsent(int key,int value,int slot){
      if(size==k.length)ensureCapacity(size+1);
      k[size]=key;v[size]=value;lookup[slot]=++size;
      if(size*2>lookup.length)rehash();
    }
    public MatAdaptiveIntIntMap build(){
      if(size==0)return new MatAdaptiveIntIntMap(Encoding.SORTED,0,0,new int[0],new int[0],null,null,new MatCollectionPlan(MatStorageKind.MAP_SORTED,0,0,0,"empty"));
      int[] sk=Arrays.copyOf(k,size),sv=Arrays.copyOf(v,size);sort(sk,sv,0,size-1);int min=sk[0],max=sk[size-1];long span=(long)max-min+1,sortedBytes=8L*size,directBytes=span<=maxDirect&&span<=Integer.MAX_VALUE?4L*span+8L*((span+63)>>>6):Long.MAX_VALUE;int hc=cap(size);long hashBytes=12L*hc;
      Encoding e=switch(policy){case MIN_MEMORY->directBytes<sortedBytes&&directBytes<=hashBytes?Encoding.DIRECT:sortedBytes<=hashBytes?Encoding.SORTED:Encoding.HASH;case FAST_LOOKUP->directBytes<=hashBytes*2?Encoding.DIRECT:Encoding.HASH;case BALANCED->directBytes<=sortedBytes*2?Encoding.DIRECT:size>=128?Encoding.HASH:Encoding.SORTED;};
      if(e==Encoding.DIRECT){int[] vals=new int[(int)span];long[] pres=new long[(int)((span+63)>>>6)];for(int i=0;i<size;i++){int o=(int)((long)sk[i]-min);vals[o]=sv[i];pres[o>>>6]|=1L<<(o&63);}return new MatAdaptiveIntIntMap(e,size,min,null,vals,pres,null,new MatCollectionPlan(MatStorageKind.MAP_DIRECT,directBytes,size,span,"direct values+presence"));}
      if(e==Encoding.SORTED)return new MatAdaptiveIntIntMap(e,size,0,sk,sv,null,null,new MatCollectionPlan(MatStorageKind.MAP_SORTED,sortedBytes,size,span,"parallel sorted int[]"));
      int[] hk=new int[hc],hv=new int[hc],st=new int[hc];int m=hc-1;for(int i=0;i<size;i++){int key=sk[i],value=sv[i],s=mix(key)&m,d=1;while(st[s]!=0){if(st[s]<d){int swap=hk[s];hk[s]=key;key=swap;swap=hv[s];hv[s]=value;value=swap;int resident=st[s];st[s]=d;d=resident;}s=(s+1)&m;if(d==Integer.MAX_VALUE)throw new IllegalStateException("hash probe distance exhausted");d++;}st[s]=d;hk[s]=key;hv[s]=value;}return new MatAdaptiveIntIntMap(e,size,0,hk,hv,null,st,new MatCollectionPlan(MatStorageKind.MAP_HASH,hashBytes,size,span,"primitive Robin-Hood open addressing"));
    }
    /** Package-private deterministic copy-failure seam; public contract is unchanged. */
    @FunctionalInterface interface IntCopier { int[] copy(int[] source, int capacity); }
    private void ensureCapacity(int required) { ensureCapacity(required, Arrays::copyOf); }
    /** Stage and validate both copy results before publishing any backing reference. */
    void ensureCapacity(int required, IntCopier copier) {
      if(required<=k.length)return;
      Objects.requireNonNull(copier,"copier");
      int next=k.length*2;
      int[] grownKeys=Objects.requireNonNull(copier.copy(k,next),"keys copy");
      int[] grownValues=Objects.requireNonNull(copier.copy(v,next),"values copy");
      if(grownKeys.length!=next||grownValues.length!=next)
        throw new IllegalStateException("invalid M3MatAdaptiveIntIntMap backing capacity");
      k=grownKeys;
      v=grownValues;
    }
    /** Package-private no-boxing frequency increment, sharing the existing insertion atom. */
    void incrementCount(int key){
      int slot=find(key),entry=lookup[slot];
      if(entry!=0){v[entry-1]=Math.addExact(v[entry-1],1);return;}
      insertAbsent(key,1,slot);
    }
    private int find(int key){int m=lookup.length-1,s=mix(key)&m;while(true){int x=lookup[s];if(x==0||k[x-1]==key)return s;s=(s+1)&m;}}
    private void rehash(){lookup=new int[lookup.length*2];for(int i=0;i<size;i++)lookup[find(k[i])]=i+1;}
    private static void sort(int[] k,int[] v,int lo,int hi){while(lo<hi){int i=lo,j=hi,p=k[(lo+hi)>>>1];while(i<=j){while(k[i]<p)i++;while(k[j]>p)j--;if(i<=j){int a=k[i];k[i]=k[j];k[j]=a;a=v[i];v[i]=v[j];v[j]=a;i++;j--;}}if(j-lo<hi-i){if(lo<j)sort(k,v,lo,j);lo=i;}else{if(i<hi)sort(k,v,i,hi);hi=j;}}}
  }
}
