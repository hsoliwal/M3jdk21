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
 * Immutable long->long map with direct, sorted and primitive open-addressed hash layouts.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatAdaptiveLongLongMap.java}
 * blob {@code 23c1729dae892dfc70041df8741cf1e75687d7b1} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatAdaptiveLongLongMap {
    public enum Encoding { DIRECT, SORTED, HASH }
    private static final long MAX_DIRECT_SPAN = 1L << 20;
    private final Encoding encoding;
    private final MatCollectionPolicy policy;
    private final int size;
    private final long minKey;
    private final long[] keys;
    private final long[] values;
    private final int[] states;
    private final long[] presence;

    private MatAdaptiveLongLongMap(
            Encoding encoding, MatCollectionPolicy policy, int size, long minKey,
            long[] keys, long[] values, int[] states, long[] presence) {
        this.encoding=encoding; this.policy=policy; this.size=size; this.minKey=minKey;
        this.keys=keys; this.values=values; this.states=states; this.presence=presence;
    }

    public static Builder builder(){return new Builder();}
    public static Builder builder(MatCollectionPolicy policy){return new Builder().policy(policy);}
    public int size(){return size;}
    public Encoding encoding(){return encoding;}
    public MatCollectionPolicy policy(){return policy;}
    public boolean containsKey(long key){return locate(key)>=0;}
    public long get(long key){
        int slot=locate(key);
        if(slot<0)throw new IllegalArgumentException("missing key: "+key);
        return values[slot];
    }
    public long getOrDefault(long key,long fallback){int slot=locate(key);return slot<0?fallback:values[slot];}

    public long[] keys(){
        if(encoding==Encoding.SORTED)return keys.clone();
        long[] out=new long[size];int p=0;
        if(encoding==Encoding.DIRECT){
            for(int i=0;i<values.length;i++)if(bit(presence,i))out[p++]=minKey+i;
        }else{
            for(int i=0;i<states.length;i++)if(states[i]!=0)out[p++]=keys[i];
        }
        Arrays.sort(out);
        return out;
    }

    public long[] valuesBySortedKey(){
        long[] ks=keys(),out=new long[ks.length];
        for(int i=0;i<ks.length;i++)out[i]=get(ks[i]);
        return out;
    }

    public long primitivePayloadBytes(){
        return 8L*(keys.length+values.length+presence.length)+4L*states.length;
    }

    private int locate(long key){
        if(encoding==Encoding.SORTED)return Arrays.binarySearch(keys,key);
        if(encoding==Encoding.DIRECT){
            long delta;
            try{delta=Math.subtractExact(key,minKey);}catch(ArithmeticException ex){return -1;}
            if(delta<0||delta>=values.length)return -1;
            int i=(int)delta;
            return bit(presence,i)?i:-1;
        }
        int mask=states.length-1,slot=mix(key)&mask,distance=1;
        for(int probe=0;probe<states.length;probe++,distance++){
            int residentDistance=states[slot];
            if(residentDistance==0||residentDistance<distance)return -1;
            if(keys[slot]==key)return slot;
            slot=(slot+1)&mask;
        }
        return -1;
    }

    private static boolean bit(long[] words,int index){
        return (words[index>>>6]&(1L<<(index&63)))!=0;
    }

    private static int mix(long value){
        value^=value>>>33;value*=0xff51afd7ed558ccdl;value^=value>>>33;
        value*=0xc4ceb9fe1a85ec53l;value^=value>>>33;
        return (int)(value^(value>>>32));
    }

    private static int capacity(int n){
        long need=Math.max(2L,2L*n);
        if(need>(1L<<30))throw new IllegalArgumentException("map too large");
        int c=2;while(c<need)c<<=1;return c;
    }

    private static void sort(long[] keys,long[] values,int lo,int hi){
        while(lo<hi){
            int i=lo,j=hi;long pivot=keys[(lo+hi)>>>1];
            while(i<=j){
                while(keys[i]<pivot)i++;
                while(keys[j]>pivot)j--;
                if(i<=j){
                    long x=keys[i];keys[i]=keys[j];keys[j]=x;
                    x=values[i];values[i]=values[j];values[j]=x;
                    i++;j--;
                }
            }
            if(j-lo<hi-i){if(lo<j)sort(keys,values,lo,j);lo=i;}
            else{if(i<hi)sort(keys,values,i,hi);hi=j;}
        }
    }

    public static final class Builder {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public Builder() {
        }

        private MatCollectionPolicy policy=MatCollectionPolicy.BALANCED;
        private long[] keys=new long[16],values=new long[16],lookupKeys=new long[32];
        private int[] lookup=new int[32];
        private int size;

        public Builder policy(MatCollectionPolicy policy){
            this.policy=Objects.requireNonNull(policy,"policy");
            return this;
        }

        public Builder put(long key,long value){
            int slot=find(key),stored=lookup[slot];
            if(stored!=0){values[stored-1]=value;return this;}
            if(size==keys.length){
                keys=Arrays.copyOf(keys,size*2);
                values=Arrays.copyOf(values,size*2);
            }
            keys[size]=key;values[size]=value;lookupKeys[slot]=key;lookup[slot]=++size;
            if(size*2>lookup.length)rehash();
            return this;
        }

        public MatAdaptiveLongLongMap build(){
            if(size==0)return new MatAdaptiveLongLongMap(
                    Encoding.SORTED,policy,0,0,new long[0],new long[0],new int[0],new long[0]);
            long[] sk=Arrays.copyOf(keys,size),sv=Arrays.copyOf(values,size);
            sort(sk,sv,0,size-1);
            long min=sk[0],max=sk[size-1];
            long span=Long.MAX_VALUE;
            try{long delta=Math.subtractExact(max,min);span=Math.addExact(delta,1);}
            catch(ArithmeticException ignored){ }
            long sortedBytes=16L*size;
            int hc=capacity(size);
            long hashBytes=20L*hc;
            long directBytes=span>0&&span<=MAX_DIRECT_SPAN
                    ?8L*span+8L*((span+63)>>>6):Long.MAX_VALUE;
            Encoding enc=switch(policy){
                case MIN_MEMORY->directBytes<sortedBytes&&directBytes<=hashBytes
                        ?Encoding.DIRECT:sortedBytes<=hashBytes?Encoding.SORTED:Encoding.HASH;
                case BALANCED->directBytes<=sortedBytes+sortedBytes/4
                        ?Encoding.DIRECT:size<128?Encoding.SORTED:Encoding.HASH;
                case FAST_LOOKUP->directBytes!=Long.MAX_VALUE?Encoding.DIRECT:Encoding.HASH;
            };
            if(enc==Encoding.SORTED)return new MatAdaptiveLongLongMap(
                    enc,policy,size,0,sk,sv,new int[0],new long[0]);
            if(enc==Encoding.DIRECT){
                int s=(int)span;
                long[] vals=new long[s],present=new long[(s+63)>>>6];
                for(int i=0;i<size;i++){
                    int d=(int)(sk[i]-min);vals[d]=sv[i];present[d>>>6]|=1L<<(d&63);
                }
                return new MatAdaptiveLongLongMap(
                        enc,policy,size,min,new long[0],vals,new int[0],present);
            }
            long[] hk=new long[hc],hv=new long[hc];
            int[] st=new int[hc];
            for(int i=0;i<size;i++){
                long key=sk[i],value=sv[i];
                int slot=mix(key)&(hc-1),distance=1;
                while(st[slot]!=0){
                    if(st[slot]<distance){
                        long swap=hk[slot];hk[slot]=key;key=swap;
                        swap=hv[slot];hv[slot]=value;value=swap;
                        int residentDistance=st[slot];st[slot]=distance;distance=residentDistance;
                    }
                    slot=(slot+1)&(hc-1);
                    if(distance==Integer.MAX_VALUE)
                        throw new IllegalStateException("hash probe distance exhausted");
                    distance++;
                }
                st[slot]=distance;hk[slot]=key;hv[slot]=value;
            }
            return new MatAdaptiveLongLongMap(enc,policy,size,0,hk,hv,st,new long[0]);
        }

        private int find(long key){
            int mask=lookup.length-1,slot=mix(key)&mask;
            while(true){
                int stored=lookup[slot];
                if(stored==0||lookupKeys[slot]==key)return slot;
                slot=(slot+1)&mask;
            }
        }

        private void rehash(){
            long[] oldKeys=Arrays.copyOf(keys,size);
            lookup=new int[lookup.length*2];
            lookupKeys=new long[lookup.length];
            for(int i=0;i<size;i++){
                int slot=find(oldKeys[i]);
                lookupKeys[slot]=oldKeys[i];
                lookup[slot]=i+1;
            }
        }
    }
}
