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
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatIntList.java}
 * blob {@code 821f776b5eaef9923ebd1b27bc11bf488fe15a8a} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatIntList implements Iterable<Integer> {
    private final int[] values; private final int offset; private final int size;
    public MatIntList(int[] values){ this(Objects.requireNonNull(values,"values").clone(),0,values.length,true); }
    private MatIntList(int[] values,int offset,int size,boolean trusted){
        this.values=values;this.offset=offset;this.size=size;Objects.checkFromIndexSize(offset,size,values.length);
    }
    static MatIntList trustedView(int[] values,int offset,int size){ return new MatIntList(values,offset,size,true); }
    public int size(){return size;} public boolean isEmpty(){return size==0;}
    public int getInt(int index){return values[offset+Objects.checkIndex(index,size)];}
    public Integer get(int index){return getInt(index);}
    public long primitivePayloadBytes(){return values.length*4L;}
    public MatCollectionPlan plan(){return new MatCollectionPlan(MatStorageKind.PRIMITIVE_INT_LIST,primitivePayloadBytes(),size,size,"int[] owner/view");}
    public MatIntList slice(int from,int to){Objects.checkFromToIndex(from,to,size);return trustedView(values,offset+from,to-from);}
    public int indexOf(int value){for(int i=0;i<size;i++)if(values[offset+i]==value)return i;return -1;}
    public boolean contains(int value){return indexOf(value)>=0;}
    public int[] toArray(){return Arrays.copyOfRange(values,offset,offset+size);}
    public IntStream intStream(){return Arrays.stream(values,offset,offset+size);}
    @Override public PrimitiveIterator.OfInt iterator(){return intStream().iterator();}
    @Override public boolean equals(Object other){
        if(this==other)return true;
        if(!(other instanceof MatIntList list)||size!=list.size)return false;
        for(int i=0;i<size;i++)if(getInt(i)!=list.getInt(i))return false;
        return true;
    }
    @Override public int hashCode(){int h=1;for(int i=0;i<size;i++)h=31*h+getInt(i);return h;}
}
