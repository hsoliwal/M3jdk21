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
import java.util.Objects;
import java.util.stream.IntStream;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatIdList.java}
 * blob {@code f39f2b9cba1acf81c0fc2fa32abc72dda55f79a9} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatIdList<T>{
    private final M3MatPool<T> pool;private final int[] ids;private final int offset;private final int size;
    public M3MatIdList(M3MatPool<T> pool,int[] ids){this(Objects.requireNonNull(pool,"pool"),Objects.requireNonNull(ids,"ids").clone(),0,ids.length,false);}
    private M3MatIdList(M3MatPool<T> pool,int[] ids,int offset,int size,boolean trusted){
        this.pool=pool;this.ids=ids;this.offset=offset;this.size=size;Objects.checkFromIndexSize(offset,size,ids.length);
        if(!trusted)for(int i=offset;i<offset+size;i++)pool.ref(ids[i]);
    }
    public static <T> Builder<T> builder(M3MatPool<T> pool){return new Builder<>(pool);}
    public M3MatPool<T> pool(){return pool;} public int size(){return size;}
    public int idAt(int index){return ids[offset+Objects.checkIndex(index,size)];}
    public M3MatRef<T> get(int index){return pool.ref(idAt(index));}
    public T materialize(int index){return pool.value(idAt(index));}
    public M3MatIdList<T> slice(int from,int to){Objects.checkFromToIndex(from,to,size);return new M3MatIdList<>(pool,ids,offset+from,to-from,true);}
    public int[] ids(){return Arrays.copyOfRange(ids,offset,offset+size);}
    public IntStream idStream(){return Arrays.stream(ids,offset,offset+size);}
    public long primitivePayloadBytes(){return ids.length*4L;}
    public M3MatCollectionPlan plan(){return new M3MatCollectionPlan(M3MatStorageKind.ID_LIST,primitivePayloadBytes(),size,pool.size(),"M3MatPool + int[] handles");}
    public static final class Builder<T>{
        private final M3MatPool<T> pool;private int[] ids=new int[16];private int size;
        private Builder(M3MatPool<T> pool){this.pool=Objects.requireNonNull(pool,"pool");}
        public Builder<T> add(M3MatRef<T> ref){Objects.requireNonNull(ref,"ref");if(ref.pool()!=pool)throw new IllegalArgumentException("different MAT pool");return addId(ref.id());}
        public Builder<T> addId(int id){pool.ref(id);if(size==ids.length)ids=Arrays.copyOf(ids,Math.multiplyExact(ids.length,2));ids[size++]=id;return this;}
        public M3MatIdList<T> build(){return new M3MatIdList<>(pool,Arrays.copyOf(ids,size),0,size,true);}
    }
}
