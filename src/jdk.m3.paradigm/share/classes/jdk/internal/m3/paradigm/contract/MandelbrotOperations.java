/* ******************************************************************************
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
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.collections.PlaceholderLogger;

/**
 * Mandelbrot set operations - deterministic iteration.
 *
 * @since IOP 1.1
 * @m3.pattern synexia:bop
 * @m3.algorithm Composes ComplexNumber.square and add, scans zero-based iteration indices until a squared-modulus comparison exceeds four, and evaluates strict cardioid and period-two-bulb comparisons using doubles.
 * @m3.dataStructures ComplexNumber references/results, one single-element ComplexNumber array, an IntStream, primitive indices/doubles and a Lombok-generated static logger.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/crystalline/MandelbrotOperations.java}
 * blob {@code 45c23eca98f6f7a9f8ff875539f1a226405d078f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MandelbrotOperations {
    private static final PlaceholderLogger log = PlaceholderLogger.of(MandelbrotOperations.class);


    /**
     * Private constructor for the static declaration owner.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body; ordinary callers cannot construct this owner.
     * @m3.dataStructures No declared instance fields or constructor parameters.
     */
    private MandelbrotOperations() {}

    /**
     * The Mandelbrot iteration: z_{n+1} = z_n² + c.
     *
     * @param z
     *            current point
     * @param c
     *            constant
     * @return next iteration z² + c
     * @m3.pattern synexia:bop
     * @m3.algorithm Calls z.square().add(c) in source order, allocating the underlying ComplexNumber results and preserving their double arithmetic; a null operand fails when dereferenced.
     * @m3.dataStructures Two ComplexNumber input references and the records produced by square and add.
     */
    public static ComplexNumber mandelbrotIteration(ComplexNumber z, ComplexNumber c) {
        return z.square().add(c);
    }

    /**
     * Check if point c is in the Mandelbrot set.
     *
     * @param c
     *            the point to test
     * @param maxIterations
     *            maximum iterations
     * @return iteration count when escaped
     * @m3.pattern synexia:bop
     * @m3.algorithm Starts z at ComplexNumber.ZERO, sequentially updates the array element for each index in IntStream.range(0,maxIterations), and returns the first index whose updated squared modulus is strictly above four; if none exists returns maxIterations, including nonpositive bounds without dereferencing c. NaN modulus comparisons are false.
     * @m3.dataStructures One ComplexNumber input, one single-element ComplexNumber array, an IntStream with findFirst, an optional int and an int fallback.
     */
    public static int mandelbrotEscapeTime(ComplexNumber c, int maxIterations) {
        ComplexNumber[] z = {ComplexNumber.ZERO};
        return java.util.stream.IntStream.range(0, maxIterations)
                .filter(i -> {
                    z[0] = mandelbrotIteration(z[0], c);
                    return z[0].modulusSquared() > 4;
                })
                .findFirst()
                .orElse(maxIterations);
    }

    /**
     * Is point definitely in the Mandelbrot set?
     *
     * @param c
     *            the point to test
     * @return true if definitely in set
     * @m3.pattern synexia:bop
     * @m3.algorithm Reads the real and imaginary doubles, forms q=(x-0.25)*(x-0.25)+y*y, tests q*(q+(x-0.25)) strictly below 0.25*y*y, then tests (x+1)*(x+1)+y*y strictly below 0.0625; equality is excluded and double rounding or overflow is retained.
     * @m3.dataStructures One ComplexNumber reference, three local double values, arithmetic intermediates and a boolean result; null fails at the first component access.
     */
    public static boolean isDefinitelyInSet(ComplexNumber c) {
        double x = c.real();
        double y = c.imag();
        double q = (x - 0.25) * (x - 0.25) + y * y;
        if (q * (q + (x - 0.25)) < 0.25 * y * y) return true;
        return (x + 1) * (x + 1) + y * y < 0.0625;
    }
}
