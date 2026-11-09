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
package jdk.internal.m3.collections.bits;

import java.util.Objects;
import jdk.m3.collections.bits.PackedBitVector;

/**
 * 0/1 subset-sum reachability for nonnegative integer weights and a bounded sum domain.
 * Template transition R |= R &lt;&lt; weight, descending words so one item is never reused.
 * Handles zero shifts and cross-word carry explicitly; unused tail bits are always zero.
 * O(ceil((limit+1)/64)) per accepted weight; one long[] retained, no per-sum objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/BitsetSubsetSum.java}
 * blob {@code d9d1ecfc32f4544d854347ce460fbd07779ead9f} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class BitsetSubsetSum {
    private final int limit;
    private final long[] reachable;
    public BitsetSubsetSum(int limit) {
        if(limit<0 || limit==Integer.MAX_VALUE) throw new IllegalArgumentException("limit");
        this.limit=limit;reachable=new long[(int)(((long)limit+64)>>>6)];reachable[0]=1;
    }
    public int limit() { return limit; }
    public long payloadBytes() { return 8L*reachable.length; }
    public void addOnce(int weight) {
        if(weight<0) throw new IllegalArgumentException("negative weight");
        if(weight>limit || weight==0) return;
        int whole=weight>>>6,shift=weight&63;
        for(int out=reachable.length-1;out>=whole;out--) {
            int src=out-whole;
            long moved=reachable[src]<<shift;
            if(shift!=0 && src>0) moved|=reachable[src-1]>>>(64-shift);
            reachable[out]|=moved;
        }
        int tail=(limit+1)&63;
        if(tail!=0) reachable[reachable.length-1]&=(1L<<tail)-1;
    }
    public boolean contains(int sum) { Objects.checkIndex(sum,limit+1);return (reachable[sum>>>6]&(1L<<(sum&63)))!=0; }
    public PackedBitVector snapshot() { return new PackedBitVector(reachable,limit+1); }
    public static PackedBitVector solve(int[] weights,int limit) {
        Objects.requireNonNull(weights,"weights");
        for(int w:weights) if(w<0) throw new IllegalArgumentException("negative weight");
        BitsetSubsetSum state=new BitsetSubsetSum(limit);
        for(int w:weights) { if(Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();state.addOnce(w); }
        return state.snapshot();
    }
}
