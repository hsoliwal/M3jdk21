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
import java.util.function.LongBinaryOperator;

/**
 * Range-update Template Method with long-monoid and long-action Strategy bindings.
 * Laws: combine associative with identity; map(action,x+y) distributes over adjacent lengths;
 * compose(new,old) means new AFTER old; actionIdentity maps every value to itself.
 * Values and tags use two flat long[] arrays. Arithmetic is wholly defined by supplied policies.
 * O(log n) apply/fold for constant-cost policies; O(n) build. Source copied, thread-confined.
 * Callbacks must not reenter. If a callback throws, the instance is POISONED and all subsequent
 * access fails rather than exposing a partially updated representation. No callback rollback.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/LongLazySegmentTree.java}
 * blob {@code 347c09477fddb60f085b3e0f4440018d53c22fc0} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3LongLazySegmentTree {
    private final int size,base;
    private final long identity,actionIdentity;
    private final long[] tree,tags;
    private final LongBinaryOperator combine,compose;
    private final M3LongRangeAction mapping;
    private boolean poisoned;
    public M3LongLazySegmentTree(long[] values,long identity,LongBinaryOperator combine,
            long actionIdentity,M3LongRangeAction mapping,LongBinaryOperator compose) {
        Objects.requireNonNull(values,"values");this.combine=Objects.requireNonNull(combine,"combine");
        this.mapping=Objects.requireNonNull(mapping,"mapping");this.compose=Objects.requireNonNull(compose,"compose");
        size=values.length;if(size>(1<<29)) throw new IllegalArgumentException("capacity");
        this.identity=identity;this.actionIdentity=actionIdentity;
        int b=1;while(b<size)b<<=1;base=b;
        tree=new long[base*2];tags=new long[base];Arrays.fill(tree,identity);Arrays.fill(tags,actionIdentity);
        System.arraycopy(values,0,tree,base,size);
        for(int i=base-1;i>0;i--)tree[i]=combine.applyAsLong(tree[i*2],tree[i*2+1]);
    }
    public int size(){return size;}
    public long payloadBytes(){return 8L*(tree.length+(long)tags.length);}
    private void healthy(){if(poisoned)throw new IllegalStateException("policy failure poisoned tree");}
    public long all(){healthy();return tree[1];}
    public void apply(int from,int to,long action){
        healthy();Objects.checkFromToIndex(from,to,size);if(from==to)return;
        try{update(1,0,base,from,to,action);}catch(RuntimeException|Error failure){poisoned=true;throw failure;}
    }
    public long fold(int from,int to){
        healthy();Objects.checkFromToIndex(from,to,size);if(from==to)return identity;
        try{return query(1,0,base,from,to);}catch(RuntimeException|Error failure){poisoned=true;throw failure;}
    }
    private void act(int node,int length,long action){
        tree[node]=mapping.apply(action,tree[node],length);
        if(node<base)tags[node]=compose.applyAsLong(action,tags[node]);
    }
    private void push(int node,int length){
        if(node<base && tags[node]!=actionIdentity){
            act(node*2,length/2,tags[node]);act(node*2+1,length/2,tags[node]);tags[node]=actionIdentity;
        }
    }
    private void update(int node,int lo,int hi,int from,int to,long action){
        if(from<=lo && hi<=to){act(node,hi-lo,action);return;}
        push(node,hi-lo);int mid=(lo+hi)>>>1;
        if(from<mid)update(node*2,lo,mid,from,to,action);
        if(to>mid)update(node*2+1,mid,hi,from,to,action);
        tree[node]=combine.applyAsLong(tree[node*2],tree[node*2+1]);
    }
    private long query(int node,int lo,int hi,int from,int to){
        if(from<=lo && hi<=to)return tree[node];push(node,hi-lo);int mid=(lo+hi)>>>1;
        if(to<=mid)return query(node*2,lo,mid,from,to);
        if(from>=mid)return query(node*2+1,mid,hi,from,to);
        return combine.applyAsLong(query(node*2,lo,mid,from,to),query(node*2+1,mid,hi,from,to));
    }
}
