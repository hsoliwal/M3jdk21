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
import java.util.stream.LongStream;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatLongList.java}
 * blob {@code dd3023a92ef5aca867b5ef1654da821b04e72dfa} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatLongList implements Iterable<Long>{
    private final long[] values;private final int offset;private final int size;
    public MatLongList(long[] values){this(Objects.requireNonNull(values,"values").clone(),0,values.length);}
    private MatLongList(long[] values,int offset,int size){Objects.checkFromIndexSize(offset,size,values.length);this.values=values;this.offset=offset;this.size=size;}
    public int size(){return size;} public long getLong(int index){return values[offset+Objects.checkIndex(index,size)];}
    public Long get(int index){return getLong(index);} public long[] toArray(){return Arrays.copyOfRange(values,offset,offset+size);}
    public MatLongList slice(int from,int to){Objects.checkFromToIndex(from,to,size);return new MatLongList(values,offset+from,to-from);}
    public LongStream longStream(){return Arrays.stream(values,offset,offset+size);}
    public long primitivePayloadBytes(){return values.length*8L;}
    public MatCollectionPlan plan(){return new MatCollectionPlan(MatStorageKind.PRIMITIVE_LONG_LIST,primitivePayloadBytes(),size,size,"long[] owner/view");}
    @Override public PrimitiveIterator.OfLong iterator(){return longStream().iterator();}
}
