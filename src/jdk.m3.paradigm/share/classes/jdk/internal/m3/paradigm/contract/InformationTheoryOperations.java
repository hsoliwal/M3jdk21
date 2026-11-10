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

import java.util.Arrays;
/**
 * Information theory operations - Shannon's laws.
 *
 * @since IOP 1.1
 * @m3.pattern synexia:bop
 * @m3.algorithm Combines positive-term filtering, binary boundary comparisons, a positive outcome-count guard, KL length and zero-denominator checks, and Math.log ratios; distributions are not normalized or comprehensively validated.
 * @m3.dataStructures Caller-supplied double arrays, double and int scalars, DoubleStream/IntStream pipelines and a Lombok-generated static logger.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/crystalline/InformationTheoryOperations.java}
 * blob {@code 526bc436a3cbb4da94f4864e126013a84781987b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class InformationTheoryOperations {
    private static final PlaceholderLogger log = PlaceholderLogger.of(InformationTheoryOperations.class);


    /**
     * Private constructor for the static declaration owner.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body; ordinary callers cannot construct this owner.
     * @m3.dataStructures No declared instance fields or constructor parameters.
     */
    private InformationTheoryOperations() {}

    /**
     * Shannon entropy: H = -Σ p_i log₂(p_i).
     *
     * @param probabilities
     *            probability distribution
     * @return entropy in bits
     * @m3.pattern synexia:bop
     * @m3.algorithm Streams the supplied array, keeps only values strictly greater than zero, maps each to -p*log2(p) and uses DoubleStream.sum; nonpositive values and NaN are skipped, normalization is unchecked and a null array fails.
     * @m3.dataStructures One caller-owned double array, a DoubleStream pipeline and its double sum; the array is neither copied nor modified.
     */
    public static double shannonEntropy(double... probabilities) {
        return Arrays.stream(probabilities)
                .filter(p -> p > 0)
                .map(p -> -p * log2(p))
                .sum();
    }

    /**
     * Binary entropy function: H(p) = -p log₂(p) - (1-p) log₂(1-p).
     *
     * @param p
     *            probability of one outcome
     * @return binary entropy in bits
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns positive zero when p is less than or equal to zero or greater than or equal to one; otherwise evaluates -p*log2(p)-(1-p)*log2(1-p) in source order, with NaN passing the comparisons.
     * @m3.dataStructures One double parameter, double logarithm/arithmetic intermediates and one double result.
     */
    public static double binaryEntropy(double p) {
        if (p <= 0 || p >= 1) return 0;
        return -p * log2(p) - (1 - p) * log2(1 - p);
    }

    /**
     * Maximum entropy for n outcomes: H_max = log₂(n).
     *
     * @param outcomes
     *            number of outcomes
     * @return maximum entropy in bits
     * @m3.pattern synexia:bop
     * @m3.algorithm Throws IllegalArgumentException for outcomes less than or equal to zero; otherwise converts the int argument to double and returns log2(outcomes).
     * @m3.dataStructures One int input, its double conversion and a double result or IllegalArgumentException.
     */
    public static double maxEntropy(int outcomes) {
        if (outcomes <= 0) throw new IllegalArgumentException("Outcomes must be positive");
        return log2(outcomes);
    }

    /**
     * KL Divergence: D_KL(P||Q) = Σ P(x) log(P(x)/Q(x)).
     *
     * @param p
     *            true distribution
     * @param q
     *            approximate distribution
     * @return KL divergence in bits
     * @m3.pattern synexia:bop
     * @m3.algorithm Checks equal array lengths, then returns positive infinity if any positive p has a zero q of either sign; otherwise streams indices with both values positive and sums p*log2(p/q) with DoubleStream.sum. Null arrays fail and no normalization is performed.
     * @m3.dataStructures Two caller-owned double arrays, an IntStream short-circuit predicate, an indexed DoubleStream sum and a boolean flag; neither array is copied or modified.
     */
    public static double klDivergence(double[] p, double[] q) {
        if (p.length != q.length) throw new IllegalArgumentException("Distributions must have same length");
        boolean hasInfinite = java.util.stream.IntStream.range(0, p.length)
                .anyMatch(i -> p[i] > 0 && q[i] == 0);
        if (hasInfinite) return Double.POSITIVE_INFINITY;
        return java.util.stream.IntStream.range(0, p.length)
                .filter(i -> p[i] > 0 && q[i] > 0)
                .mapToDouble(i -> p[i] * log2(p[i] / q[i]))
                .sum();
    }

    /** Logarithm base 2.
     * @m3.pattern synexia:bop
     * @m3.algorithm Evaluates Math.log(x)/Math.log(2) with ordinary double division; zero, negative, NaN and infinite inputs retain the underlying logarithm semantics.
     * @m3.dataStructures One double argument, two Math.log results and a double quotient.
     */
    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
