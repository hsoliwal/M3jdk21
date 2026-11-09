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
package jdk.internal.m3.collections;

import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Bounded exact-cover enumeration of up to 64 columns. Shape: choose the least-supported
 * uncovered column, select a compatible row, recurse on uncovered XOR row, then reuse the path.
 * No per-node allocation. Depth <=64; row masks and one int[columns] path are primitive.
 * Exponential search is explicit: worst-case O(nodeBudget * rows * columns).
 * Callback borrows path[0,length) until it returns and MUST NOT mutate it or the row masks.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/ExactCover64.java}
 * blob {@code b591d6116056852254b4ae51756da2f9ff788edf} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ExactCover64 {
    private ExactCover64(){}
    public enum Status{COMPLETE,NODE_LIMIT,SOLUTION_LIMIT}
    @FunctionalInterface public interface SolutionConsumer{void accept(int[] rowIndexes,int length);}
    public record Result(Status status,long visitedNodes,long solutions){}
    public static Result enumerate(int columns,long[] rows,long nodeBudget,long solutionBudget,SolutionConsumer sink){
        Objects.requireNonNull(rows,"rows");Objects.requireNonNull(sink,"sink");
        if(columns<0 || columns>64 || nodeBudget<1 || solutionBudget<1)throw new IllegalArgumentException("search bounds");
        long universe=columns==64?-1L:(1L<<columns)-1;
        for(long row:rows)if(row==0 || (row&~universe)!=0)throw new IllegalArgumentException("empty/out-of-domain row");
        Search search=new Search(rows,new int[columns],nodeBudget,solutionBudget,sink);
        search.visit(universe,0);return new Result(search.status,search.nodes,search.solutions);
    }
    private static final class Search{
        final long[] rows;final int[] path;final long nodeBudget,solutionBudget;final SolutionConsumer sink;
        long nodes,solutions;Status status=Status.COMPLETE;
        Search(long[] rows,int[] path,long nodes,long solutions,SolutionConsumer sink){this.rows=rows;this.path=path;nodeBudget=nodes;solutionBudget=solutions;this.sink=sink;}
        boolean visit(long uncovered,int depth){
            if(Thread.currentThread().isInterrupted())throw new CancellationException("exact-cover interrupted");
            if(nodes==nodeBudget){status=Status.NODE_LIMIT;return false;}nodes++;
            if(uncovered==0){
                sink.accept(path,depth);solutions++;
                if(solutions==solutionBudget){status=Status.SOLUTION_LIMIT;return false;}return true;
            }
            long chosen=0;int minimum=Integer.MAX_VALUE;
            for(long remaining=uncovered;remaining!=0;remaining&=remaining-1){
                long column=remaining&-remaining;int count=0;
                for(long row:rows)if((row&column)!=0 && (row&uncovered)==row)count++;
                if(count<minimum){chosen=column;minimum=count;if(count==0)return true;}
            }
            for(int r=0;r<rows.length;r++)if((rows[r]&chosen)!=0 && (rows[r]&uncovered)==rows[r]){
                path[depth]=r;if(!visit(uncovered^rows[r],depth+1))return false;
            }
            return true;
        }
    }
}
