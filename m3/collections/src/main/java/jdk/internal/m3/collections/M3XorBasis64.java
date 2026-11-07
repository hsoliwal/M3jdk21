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

/**
 * Linear span over GF(2) of all 64-bit words. Template: eliminate the highest set bit using
 * its unique pivot. Values are bit patterns, not signed magnitudes; zero is always representable.
 * One long[64], O(64) insertion/membership/maximum. Mutations are thread-confined.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/XorBasis64.java}
 * blob {@code 1967893b906cfbe883759d05661bfe511bf2708e} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3XorBasis64 {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public M3XorBasis64() {
    }

    private final long[] pivots=new long[64];
    private int rank;
    public int rank() { return rank; }
    public long payloadBytes() { return 512; }
    public boolean add(long value) {
        for(int bit=63;bit>=0;bit--) if(((value>>>bit)&1)!=0) {
            if(pivots[bit]==0) { pivots[bit]=value;rank++;return true; }
            value^=pivots[bit];
        }
        return false;
    }
    public boolean contains(long value) {
        for(int bit=63;bit>=0;bit--) if(((value>>>bit)&1)!=0) value^=pivots[bit];
        return value==0;
    }
    /** Maximum unsigned value of seed XOR any element of this span. */
    public long maximumUnsigned(long seed) {
        for(int bit=63;bit>=0;bit--) { long next=seed^pivots[bit];if(Long.compareUnsigned(next,seed)>0) seed=next; }
        return seed;
    }
    public void merge(M3XorBasis64 other) {
        java.util.Objects.requireNonNull(other,"other");for(long pivot:other.pivots) add(pivot);
    }
}
