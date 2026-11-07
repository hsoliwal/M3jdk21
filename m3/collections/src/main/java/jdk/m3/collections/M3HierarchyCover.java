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

/**
 * Hierarchical scope-cover reduction. Binding obligation: selecting a node retrieves its ENTIRE
 * descendant subtree, not just that node. Shape: validate forest -> preorder intervals -> remove
 * requested intervals covered by an earlier requested ancestor. No string-prefix approximation.
 * parent[v]=-1 marks a root. Duplicate requests collapse. Output order is deterministic preorder,
 * not request order. O(V+requests) primitive work/storage; no source mutation or recursive DFS.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/HierarchyCover.java}
 * blob {@code 5499c955c82d9bf2c5f83146178b32984a40938f} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3HierarchyCover {
    private M3HierarchyCover(){}
    public static int[] reduce(int[] parent,int[] requested){
        Objects.requireNonNull(parent,"parent");Objects.requireNonNull(requested,"requested");int n=parent.length,edges=0;
        for(int p:parent){if(p!=-1){Objects.checkIndex(p,n);edges++;}}
        for(int v:requested)Objects.checkIndex(v,n);
        int[] from=new int[edges],to=new int[edges];int w=0;
        for(int v=0;v<n;v++)if(parent[v]>=0){from[w]=parent[v];to[w++]=v;}
        M3IntCsrGraph graph=M3IntCsrGraph.fromEdges(n,from,to);
        int[] order=new int[n],end=new int[n],stack=new int[n],cursor=new int[n];int count=0;
        for(int root=0;root<n;root++)if(parent[root]==-1){
            int depth=0;stack[depth++]=root;cursor[root]=graph.start(root);order[count++]=root;
            while(depth>0){
                if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("scope reduction interrupted");
                int v=stack[depth-1];
                if(cursor[v]<graph.end(v)){int child=graph.target(cursor[v]++);stack[depth++]=child;cursor[child]=graph.start(child);order[count++]=child;}
                else{end[v]=count;depth--;}
            }
        }
        if(count!=n)throw new IllegalArgumentException("parent relation has a cycle");
        long[] wanted=new long[(int)(((long)n+63)>>>6)];for(int v:requested)wanted[v>>>6]|=1L<<(v&63);
        int[] result=new int[Math.min(n,requested.length)];int selected=0,coveredUntil=0;
        for(int i=0;i<n;i++){int v=order[i];if(i>=coveredUntil && (wanted[v>>>6]&(1L<<(v&63)))!=0){result[selected++]=v;coveredUntil=end[v];}}
        return Arrays.copyOf(result,selected);
    }
}
