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

import java.util.Objects;

/**
 * Subset/superset zeta and Mobius templates over the ring Z/(2^64). All long arithmetic WRAPS.
 * One in-place butterfly skeleton binds lattice direction and forward/inverse operation.
 * Array length must be a positive power of two. O(n log n) additions; no work arrays for transform.
 * OR/AND convolutions reuse that skeleton, snapshot inputs and allocate only long[] work lanes.
 * This is exact ring convolution, not floating-point FFT and not division-based XOR convolution.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/SubsetTransforms.java}
 * blob {@code d4d37afd847815d4747b173ae40d8754625c9696} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3SubsetTransforms {
    private M3SubsetTransforms(){}
    public static void zeta(long[] values,boolean subset){transform(values,subset,false);}
    public static void mobius(long[] values,boolean subset){transform(values,subset,true);}
    private static void transform(long[] values,boolean subset,boolean inverse){
        validate(values);int n=values.length;
        for(int bit=1;bit<n;bit<<=1)for(int mask=0;mask<n;mask++){
            if(((mask&bit)!=0)==subset){int from=mask^bit;values[mask]=inverse?values[mask]-values[from]:values[mask]+values[from];}
        }
    }
    public static long[] convolution(long[] left,long[] right,boolean bitwiseOr){
        validate(left);validate(right);if(left.length!=right.length)throw new IllegalArgumentException("lane lengths");
        long[] a=left.clone(),b=right.clone();zeta(a,bitwiseOr);zeta(b,bitwiseOr);
        for(int i=0;i<a.length;i++)a[i]*=b[i];mobius(a,bitwiseOr);return a;
    }
    private static void validate(long[] values){Objects.requireNonNull(values,"values");if(values.length==0 || (values.length&(values.length-1))!=0)throw new IllegalArgumentException("power-of-two length");}
}
