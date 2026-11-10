/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Objects;

/**
 * Open-addressed 128-bit UID -> reference table with primitive UID lanes and no entry nodes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextUidRefMap.java}
 * blob {@code c891f126625f5c804b70c452828a609dc9e56da7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class UidRefMap<V> {
    private static final int EMPTY=0, USED=1, DELETED=2;
    private long[] high;
    private long[] low;
    private Object[] values;
    private byte[] states;
    private int size;

    public UidRefMap(){ this(8); }
    public UidRefMap(int expected){
        int capacity=8;
        while(capacity < Math.max(1,expected)*2) capacity<<=1;
        high=new long[capacity]; low=new long[capacity]; values=new Object[capacity]; states=new byte[capacity];
    }
    public int size(){ return size; }
    @SuppressWarnings("unchecked")
    public V get(long hi,long lo){
        int slot=find(hi,lo);
        return slot<0?null:(V)values[slot];
    }    @SuppressWarnings("unchecked")
    public V put(long hi,long lo,V value){
        Objects.requireNonNull(value,"value");
        if((size+1)*10 >= high.length*6) resize(high.length<<1);
        int mask=high.length-1, slot=mix(hi,lo)&mask, deleted=-1;
        while(states[slot]!=EMPTY){
            if(states[slot]==USED && high[slot]==hi && low[slot]==lo){
                V old=(V)values[slot]; values[slot]=value; return old;
            }
            if(states[slot]==DELETED && deleted<0) deleted=slot;
            slot=(slot+1)&mask;
        }
        if(deleted>=0) slot=deleted;
        states[slot]=USED; high[slot]=hi; low[slot]=lo; values[slot]=value; size++; return null;
    }    @SuppressWarnings("unchecked")
    public V remove(long hi,long lo){
        int slot=find(hi,lo);
        if(slot<0) return null;
        V old=(V)values[slot]; values[slot]=null; states[slot]=DELETED; size--; return old;
    }
    private int find(long hi,long lo){
        int mask=high.length-1, slot=mix(hi,lo)&mask;
        while(states[slot]!=EMPTY){
            if(states[slot]==USED && high[slot]==hi && low[slot]==lo) return slot;
            slot=(slot+1)&mask;
        }
        return -1;
    }
    private void resize(int capacity){
        long[] oh=high, ol=low; Object[] ov=values; byte[] os=states;
        high=new long[capacity]; low=new long[capacity]; values=new Object[capacity]; states=new byte[capacity];
        int oldSize=size; size=0;
        for(int i=0;i<os.length;i++) if(os[i]==USED) putRaw(oh[i],ol[i],ov[i]);
        if(size!=oldSize) throw new IllegalStateException("rehash size mismatch");
    }
    private void putRaw(long hi,long lo,Object value){
        int mask=high.length-1, slot=mix(hi,lo)&mask;
        while(states[slot]==USED) slot=(slot+1)&mask;
        states[slot]=USED; high[slot]=hi; low[slot]=lo; values[slot]=value; size++;
    }
    private static int mix(long hi,long lo){
        long x=hi ^ Long.rotateLeft(lo,29);
        x^=x>>>33; x*=0xff51afd7ed558ccdl; x^=x>>>33; x*=0xc4ceb9fe1a85ec53l; x^=x>>>33;
        return (int)(x^(x>>>32));
    }
}
