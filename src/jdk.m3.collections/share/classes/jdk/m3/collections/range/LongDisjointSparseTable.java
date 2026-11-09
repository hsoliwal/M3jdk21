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
package jdk.m3.collections.range;

import java.util.Objects;
import java.util.function.LongBinaryOperator;

/**
 * Immutable O(1) ordered range fold for ANY associative long operation with two-sided identity.
 * Unlike an overlapping sparse table, neither idempotence nor commutativity is required.
 * Template: suffixes left of each binary split plus prefixes right of it. The highest differing
 * index bit chooses two disjoint pieces. Source copied; O(n log n) build/storage; two flat arrays.
 * The operation is a pure stable nonreentrant policy; exceptions in queries do not mutate state.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/LongDisjointSparseTable.java}
 * blob {@code 2d1526848b0450e70ce6bb5e5ab53f07ff19f704} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LongDisjointSparseTable {
    private final long[] source,table;
    private final LongBinaryOperator combine;
    private final long identity;
    public LongDisjointSparseTable(long[] values,long identity,LongBinaryOperator combine) {
        Objects.requireNonNull(values,"values");this.combine=Objects.requireNonNull(combine,"combine");this.identity=identity;
        source=values.clone();int n=source.length,levels=n<=1?0:32-Integer.numberOfLeadingZeros(n-1);
        table=new long[Math.multiplyExact(n,levels)];
        for(int level=0;level<levels;level++) {
            long width=1L<<(level+1),half=width>>>1;
            for(long start=0;start<n;start+=width) {
                int lo=(int)start,mid=(int)Math.min(n,start+half),hi=(int)Math.min(n,start+width),base=level*n;
                if(lo<mid) { table[base+mid-1]=source[mid-1];for(int i=mid-2;i>=lo;i--) table[base+i]=combine.applyAsLong(source[i],table[base+i+1]); }
                if(mid<hi) { table[base+mid]=source[mid];for(int i=mid+1;i<hi;i++) table[base+i]=combine.applyAsLong(table[base+i-1],source[i]); }
            }
        }
    }
    public int size() { return source.length; }
    public long payloadBytes() { return 8L*(source.length+(long)table.length); }
    public long fold(int from,int to) {
        Objects.checkFromToIndex(from,to,source.length);if(from==to) return identity;if(to-from==1) return source[from];
        int level=31-Integer.numberOfLeadingZeros(from^(to-1)),base=level*source.length;
        return combine.applyAsLong(table[base+from],table[base+to-1]);
    }
}
