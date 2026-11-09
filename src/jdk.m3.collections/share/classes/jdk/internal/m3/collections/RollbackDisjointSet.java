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
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Reversible partition maintenance, not a persistent union-find. Union by size gives O(log n)
 * depth without path compression; avoiding compression makes each union undoable in O(1).
 * Snapshots are depths in the CURRENT history prefix, not durable IDs across abandoned branches.
 * Three int[] lanes, at most n-1 successful unions outstanding; no allocation after construction.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/RollbackDisjointSet.java}
 * blob {@code 9d49a2340e882d480c4e4f6fe7a6d05749c76f88} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class RollbackDisjointSet {
    private final int[] parent,child,oldSize;
    private int depth,components;
    public RollbackDisjointSet(int size) {
        if(size<0) throw new IllegalArgumentException("size");
        parent=new int[size];Arrays.fill(parent,-1);child=new int[size];oldSize=new int[size];components=size;
    }
    public int size() { return parent.length; }
    public int components() { return components; }
    public int snapshot() { return depth; }
    public long payloadBytes() { return 12L*parent.length; }
    public int leader(int value) {
        Objects.checkIndex(value,parent.length);while(parent[value]>=0) value=parent[value];return value;
    }
    public int componentSize(int value) { return -parent[leader(value)]; }
    public boolean union(int a,int b) {
        a=leader(a);b=leader(b);if(a==b) return false;
        if(parent[a]>parent[b] || (parent[a]==parent[b] && a>b)) { int t=a;a=b;b=t; }
        child[depth]=b;oldSize[depth++]=parent[b];parent[a]+=parent[b];parent[b]=a;components--;return true;
    }
    public void rollback(int checkpoint) {
        if(checkpoint<0 || checkpoint>depth) throw new IllegalArgumentException("checkpoint is not a current prefix");
        while(depth>checkpoint) {
            int node=child[--depth],root=parent[node];parent[root]-=oldSize[depth];parent[node]=oldSize[depth];components++;
        }
    }
}
