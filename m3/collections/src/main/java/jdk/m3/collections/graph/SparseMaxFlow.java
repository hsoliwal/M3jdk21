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
 * Iterative Dinic with a fixed edge budget. Shape: residual BFS layers -> blocking-flow path
 * search -> residual update. Forward/reverse edges are adjacent primitive slots, paired by e^1.
 * No edge objects, recursion by vertex depth, or capacity sentinel. Capacities are 0..MAX_VALUE.
 * augment returns NEW flow bounded by limit, allowing totals larger than long across calls.
 * Calls on one residual network must keep the same distinct terminals until resetFlow().
 * Cancellation can leave a valid partial feasible flow; it does not roll back prior augmentations.
 * O(V^2 E) worst case; O(V+E) primitive payload. Thread-confined.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/SparseMaxFlow.java}
 * blob {@code 4f9d57ab1420923cf71f767d51358e30b8e933f6} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SparseMaxFlow {
    private static final Runnable NO_CHECKPOINT = () -> { };
    private final int[] head,to,next,level,cursor,queue,pathVertex,pathEdge;
    private final long[] residual,bottleneck;
    private int used,sourceLock=-1,sinkLock=-1;
    public SparseMaxFlow(int vertices,int edgeCapacity){
        if(vertices<0 || edgeCapacity<0 || edgeCapacity>(Integer.MAX_VALUE-8)/2)throw new IllegalArgumentException("network bounds");
        head=new int[vertices];Arrays.fill(head,-1);to=new int[edgeCapacity*2];next=new int[to.length];residual=new long[to.length];
        level=new int[vertices];cursor=new int[vertices];queue=new int[vertices];pathVertex=new int[vertices];pathEdge=new int[vertices];bottleneck=new long[vertices];
    }
    public int addEdge(int from,int destination,long capacity){
        Objects.checkIndex(from,head.length);Objects.checkIndex(destination,head.length);
        if(capacity<0)throw new IllegalArgumentException("negative capacity");if(used==to.length)throw new IllegalStateException("edge budget");
        int id=used;to[used]=destination;next[used]=head[from];residual[used]=capacity;head[from]=used++;
        to[used]=from;next[used]=head[destination];head[destination]=used++;return id;
    }
    public int edgeCount(){return used/2;}
    public long flowOn(int edge){if(edge<0 || edge>=used || (edge&1)!=0)throw new IllegalArgumentException("forward edge ID");return residual[edge^1];}
    public long payloadBytes(){return 4L*(6L*head.length+2L*to.length)+8L*(residual.length+(long)bottleneck.length);}
    public void resetFlow(){for(int e=0;e<used;e+=2){residual[e]+=residual[e+1];residual[e+1]=0;}sourceLock=-1;sinkLock=-1;}
    public long augment(int source,int sink,long limit){
        return augment(source,sink,limit,NO_CHECKPOINT);
    }
    /**
     * Add at most {@code limit} flow with cooperative checkpoints during residual scans/search.
     * A callback failure leaves a feasible partial flow: callbacks never run midway through
     * updating an augmenting path. The callback must not reenter or mutate this network.
     * Thread interruption is also checked. A zero limit performs no search or checkpoint.
     *
     * @param source source vertex
     * @param sink distinct sink vertex
     * @param limit nonnegative bound on additional flow, not total flow
     * @param checkpoint nonnull callback, which may throw to cancel
     * @return additional flow sent by this call
     */
    public long augment(int source,int sink,long limit,Runnable checkpoint){
        Objects.requireNonNull(checkpoint,"checkpoint");
        Objects.checkIndex(source,head.length);Objects.checkIndex(sink,head.length);
        if(source==sink || limit<0)throw new IllegalArgumentException("terminals/limit");
        if(sourceLock>=0 && (source!=sourceLock || sink!=sinkLock))throw new IllegalStateException("residual terminal mismatch");
        sourceLock=source;sinkLock=sink;long total=0;
        while(total<limit && layers(source,sink,checkpoint)){
            System.arraycopy(head,0,cursor,0,head.length);
            for(long sent;total<limit && (sent=sendOne(source,sink,limit-total,checkpoint))!=0;)total+=sent;
        }
        return total;
    }
    /**
     * Snapshot vertices reachable from {@code source} through positive residual capacity.
     * This is the source side of a minimum cut only after maximum flow has been reached.
     * Does not change residual capacities or terminal locks. The result is independently owned.
     * The callback must not reenter or mutate this network; failure publishes no snapshot.
     *
     * @param source start vertex
     * @param checkpoint nonnull cooperative callback
     * @return one reachability flag per vertex
     */
    public boolean[] sourceSide(int source,Runnable checkpoint){
        Objects.checkIndex(source,head.length);Objects.requireNonNull(checkpoint,"checkpoint");
        boolean[] seen=new boolean[head.length];int h=0,t=0;queue[t++]=source;seen[source]=true;
        while(h<t){
            check(checkpoint);int v=queue[h++];
            for(int e=head[v];e!=-1;e=next[e]){
                check(checkpoint);int target=to[e];
                if(residual[e]>0 && !seen[target]){seen[target]=true;queue[t++]=target;}
            }
        }
        check(checkpoint);return seen;
    }
    private boolean layers(int source,int sink,Runnable checkpoint){
        Arrays.fill(level,-1);int h=0,t=0;queue[t++]=source;level[source]=0;
        while(h<t){check(checkpoint);int v=queue[h++];for(int e=head[v];e!=-1;e=next[e]){check(checkpoint);if(residual[e]>0 && level[to[e]]<0){level[to[e]]=level[v]+1;queue[t++]=to[e];}}}
        return level[sink]>=0;
    }
    private long sendOne(int source,int sink,long limit,Runnable checkpoint){
        int depth=0;pathVertex[0]=source;bottleneck[0]=limit;
        while(depth>=0){
            check(checkpoint);int v=pathVertex[depth];
            // Commit a complete path before another callback can observe/cancel the flow.
            if(v==sink){long amount=bottleneck[depth];for(int d=0;d<depth;d++){int e=pathEdge[d];residual[e]-=amount;residual[e^1]+=amount;}return amount;}
            int e=cursor[v];
            while(e!=-1 && (residual[e]==0 || level[to[e]]!=level[v]+1)){check(checkpoint);e=next[e];cursor[v]=e;}
            if(e==-1){level[v]=-1;if(depth==0)return 0;depth--;cursor[pathVertex[depth]]=next[pathEdge[depth]];}
            else{pathEdge[depth]=e;pathVertex[depth+1]=to[e];bottleneck[depth+1]=Math.min(bottleneck[depth],residual[e]);depth++;}
        }
        return 0;
    }
    private static void check(Runnable checkpoint){
        if(Thread.currentThread().isInterrupted())throw new CancellationException("flow interrupted");
        checkpoint.run();
    }
}
