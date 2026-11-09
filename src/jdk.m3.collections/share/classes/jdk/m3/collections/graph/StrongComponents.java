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

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Iterative Kosaraju decomposition over shared CSR. Shape: finish-order traversal, reverse
 * traversal, component projection. No recursive vertex stack or per-edge/vertex objects.
 * O(V+E) work and primitive workspace. Result IDs are deterministic and topological:
 * an edge between distinct components goes from a smaller ID to a larger ID.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/StrongComponents.java}
 * blob {@code 9b24f233fb8ee2c015da82064a372c36b1527716} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StrongComponents {
    private StrongComponents(){}
    public static final class Result {
        private final int count;
        private final int[] ids;
        private Result(int count,int[] ids){this.count=count;this.ids=ids;}
        public int count(){return count;}
        public int vertices(){return ids.length;}
        public int componentOf(int vertex){return ids[Objects.checkIndex(vertex,ids.length)];}
        public int[] toArray(){return ids.clone();}
    }
    public static Result compute(IntCsrGraph graph){
        Objects.requireNonNull(graph,"graph");int n=graph.vertices(),finished=0;
        int[] stack=new int[n],cursor=new int[n],order=new int[n];long[] seen=new long[(int)(((long)n+63)>>>6)];
        for(int root=0;root<n;root++){
            if((seen[root>>>6]&(1L<<(root&63)))!=0)continue;
            int depth=0;stack[depth++]=root;seen[root>>>6]|=1L<<(root&63);cursor[root]=graph.start(root);
            while(depth>0){
                check();int v=stack[depth-1];
                if(cursor[v]<graph.end(v)){
                    int to=graph.target(cursor[v]++);
                    if((seen[to>>>6]&(1L<<(to&63)))==0){seen[to>>>6]|=1L<<(to&63);cursor[to]=graph.start(to);stack[depth++]=to;}
                }else{order[finished++]=v;depth--;}
            }
        }
        IntCsrGraph reversed=graph.reverse();int[] ids=new int[n];Arrays.fill(ids,-1);int count=0;
        for(int i=n-1;i>=0;i--){
            int root=order[i];if(ids[root]>=0)continue;
            int depth=0;stack[depth++]=root;ids[root]=count;
            while(depth>0){
                check();int v=stack[--depth];
                for(int e=reversed.start(v);e<reversed.end(v);e++){
                    int to=reversed.target(e);if(ids[to]<0){ids[to]=count;stack[depth++]=to;}
                }
            }
            count++;
        }
        return new Result(count,ids);
    }
    private static void check(){if(Thread.currentThread().isInterrupted())throw new CancellationException("SCC interrupted");}
}
