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
package jdk.m3.collections.bits;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable bit-vector with word-prefix rank and ordinal selection. Shape: packed membership
 * -> rank -> select. State is one long[] and one int[]; no Boolean or bit node.
 * rank1 counts [0,end). select arguments are zero-based occurrence ordinals, not bit positions.
 * Unused high tail bits are cleared. Source is copied. Rank O(1), select O(log(words)+64).
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/PackedBitVector.java}
 * blob {@code 13e24b0e02976ae8725ac4cdf62ee3c7aefe459c} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedBitVector {
    private final int length;
    private final long[] words;
    private final int[] prefix;
    public PackedBitVector(long[] source, int length) {
        Objects.requireNonNull(source, "source");
        if (length < 0 || source.length != (int)(((long)length + 63) >>> 6))
            throw new IllegalArgumentException("bit-vector shape");
        this.length = length;
        words = source.clone();
        if ((length & 63) != 0) words[words.length-1] &= (1L << (length & 63))-1;
        prefix = new int[words.length+1];
        for (int i=0; i<words.length; i++) prefix[i+1] = prefix[i]+Long.bitCount(words[i]);
    }
    public int length() { return length; }
    public int ones() { return prefix[words.length]; }
    public long payloadBytes() { return 8L*words.length + 4L*prefix.length; }
    public boolean get(int at) { Objects.checkIndex(at,length); return (words[at>>>6] & (1L << (at&63))) != 0; }
    public int rank1(int end) {
        Objects.checkFromToIndex(0,end,length);
        int w=end>>>6, b=end&63;
        return prefix[w] + (b==0 ? 0 : Long.bitCount(words[w] & ((1L<<b)-1)));
    }
    public int rank0(int end) { return end-rank1(end); }
    public int select1(int ordinal) { return select(ordinal,true); }
    public int select0(int ordinal) { return select(ordinal,false); }
    private int select(int ordinal, boolean one) {
        Objects.checkIndex(ordinal,one ? ones() : length-ones());
        int lo=0, hi=words.length;
        while (lo<hi) {
            int mid=(lo+hi)>>>1;
            int count=one ? prefix[mid+1] : (int)Math.min(length,64L*(mid+1))-prefix[mid+1];
            if (count<=ordinal) lo=mid+1; else hi=mid;
        }
        int before=one ? prefix[lo] : lo*64-prefix[lo];
        long bits=one ? words[lo] : ~words[lo];
        int remaining=length-lo*64;
        if (remaining<64) bits &= (1L<<remaining)-1;
        for (int n=ordinal-before; n>0; n--) bits &= bits-1;
        return lo*64+Long.numberOfTrailingZeros(bits);
    }
    public long[] toWords() { return Arrays.copyOf(words,words.length); }
}
