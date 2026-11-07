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
package jdk.m3.collections.graph;

import java.util.Objects;

/**
 * Immutable indexed directed graph. Two int[] lanes; no vertex/edge object. Parallel edges and
 * self loops are legal. Construction snapshots and validates; adjacency order is input order.
 * This representation adapter is shared by SCC, implication solving and bit-matrix closure.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/IntCsrGraph.java}
 * blob {@code b23af7724ebc7a38caafae818c4c44e12e0160c6} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IntCsrGraph {
    private static final Runnable NO_CHECKPOINT = () -> { };
    private final int[] offsets,edges;
    private IntCsrGraph(int[] offsets,int[] edges){this.offsets=offsets;this.edges=edges;}
    public static IntCsrGraph copyOf(int[] offsets,int[] edges){
        Objects.requireNonNull(offsets,"offsets");Objects.requireNonNull(edges,"edges");
        if(offsets.length==0 || offsets[0]!=0 || offsets[offsets.length-1]!=edges.length)throw new IllegalArgumentException("CSR endpoints");
        for(int i=1;i<offsets.length;i++)if(offsets[i]<offsets[i-1])throw new IllegalArgumentException("CSR monotonic offsets");
        int n=offsets.length-1;for(int e:edges)Objects.checkIndex(e,n);
        return new IntCsrGraph(offsets.clone(),edges.clone());
    }
    public static IntCsrGraph fromEdges(int vertices,int[] from,int[] to){
        return fromEdges(vertices,from,to,NO_CHECKPOINT);
    }
    /**
     * Build the same stable adjacency with cooperative checkpoints during validation and copying.
     * The callback must not mutate the input lanes. A callback failure publishes no graph;
     * neither input lane is modified. Checkpoint counts are implementation details.
     *
     * @param vertices vertex count
     * @param from source IDs in input edge order
     * @param to destination IDs in input edge order
     * @param checkpoint nonnull callback, which may throw to cancel construction
     * @return a complete immutable snapshot
     */
    public static IntCsrGraph fromEdges(int vertices,int[] from,int[] to,Runnable checkpoint){
        Objects.requireNonNull(from,"from");Objects.requireNonNull(to,"to");
        Objects.requireNonNull(checkpoint,"checkpoint");
        if(vertices<0 || vertices==Integer.MAX_VALUE || from.length!=to.length)throw new IllegalArgumentException("graph shape");
        checkpoint.run();
        for(int i=0;i<from.length;i++){checkpoint.run();Objects.checkIndex(from[i],vertices);Objects.checkIndex(to[i],vertices);}
        int[] offsets=new int[vertices+1];for(int v:from){checkpoint.run();offsets[v+1]++;}
        for(int i=1;i<offsets.length;i++){checkpoint.run();offsets[i]+=offsets[i-1];}
        int[] cursor=offsets.clone(),edges=new int[from.length];
        for(int i=0;i<from.length;i++){checkpoint.run();edges[cursor[from[i]]++]=to[i];}
        checkpoint.run();
        return new IntCsrGraph(offsets,edges);
    }
    public int vertices(){return offsets.length-1;}
    public int edgeCount(){return edges.length;}
    public int start(int vertex){return offsets[Objects.checkIndex(vertex,vertices())];}
    public int end(int vertex){return offsets[Objects.checkIndex(vertex,vertices())+1];}
    public int target(int edge){return edges[Objects.checkIndex(edge,edges.length)];}
    public long payloadBytes(){return 4L*(offsets.length+(long)edges.length);}
    public IntCsrGraph reverse(){
        int n=vertices();int[] off=new int[n+1];for(int v:edges)off[v+1]++;
        for(int i=1;i<=n;i++)off[i]+=off[i-1];
        int[] cursor=off.clone(),reversed=new int[edges.length];
        for(int v=0;v<n;v++)for(int e=offsets[v];e<offsets[v+1];e++)reversed[cursor[edges[e]]++]=v;
        return new IntCsrGraph(off,reversed);
    }
}
