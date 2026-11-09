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
package jdk.m3.collections.bits;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import jdk.m3.collections.graph.IntCsrGraph;

/**
 * Word-parallel Warshall closure. Shape: if i reaches k, OR k's row into i's row.
 * Boolean state is a flat long[] matrix, not boolean[][] or per-edge objects.
 * Reflexive semantics are explicit; with reflexive=false a diagonal bit needs a cycle.
 * O(V^2 * ceil(V/64)) worst-case word work; O(V*ceil(V/64)) storage. Build is cancellable
 * by thread interruption; no partial object is published.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/BitMatrixClosure.java}
 * blob {@code fedb77c367021c7008fe5c9ed57fd4008f23b7e3} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class BitMatrixClosure {
    private final int vertices,width;
    private final long[] matrix;
    private BitMatrixClosure(int vertices,int width,long[] matrix){this.vertices=vertices;this.width=width;this.matrix=matrix;}
    public static BitMatrixClosure compute(IntCsrGraph graph,boolean reflexive){
        Objects.requireNonNull(graph,"graph");int n=graph.vertices(),width=(int)(((long)n+63)>>>6);
        long[] rows=new long[Math.multiplyExact(n,width)];
        for(int v=0;v<n;v++){
            if(reflexive)rows[v*width+(v>>>6)]|=1L<<(v&63);
            for(int e=graph.start(v);e<graph.end(v);e++){int to=graph.target(e);rows[v*width+(to>>>6)]|=1L<<(to&63);}
        }
        for(int k=0;k<n;k++){
            if(Thread.currentThread().isInterrupted())throw new CancellationException("closure interrupted");
            int word=k>>>6;long mask=1L<<(k&63);
            for(int v=0;v<n;v++)if((rows[v*width+word]&mask)!=0){
                for(int w=0;w<width;w++)rows[v*width+w]|=rows[k*width+w];
            }
        }
        return new BitMatrixClosure(n,width,rows);
    }
    public boolean reaches(int from,int to){Objects.checkIndex(from,vertices);Objects.checkIndex(to,vertices);return (matrix[from*width+(to>>>6)]&(1L<<(to&63)))!=0;}
    public int countFrom(int from){Objects.checkIndex(from,vertices);int total=0;for(int w=0;w<width;w++)total+=Long.bitCount(matrix[from*width+w]);return total;}
    public long payloadBytes(){return 8L*matrix.length;}
}
