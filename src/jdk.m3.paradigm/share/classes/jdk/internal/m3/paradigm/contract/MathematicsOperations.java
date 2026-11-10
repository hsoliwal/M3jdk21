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
 * Mathematics operations - pure deterministic mathematics.
 *
 * @since IOP 1.1
 * @m3.pattern synexia:bop
 * @m3.algorithm Uses stream reductions and mutable holders for factorial, binomial and Fibonacci arithmetic, odd-divisor trial scanning, Euclidean remainders and multiply-before-divide LCM arithmetic; long overflow and Long.MIN_VALUE absolute-value behavior are unchecked.
 * @m3.dataStructures Four static final double fields, long/int scalars, LongStream/IntStream pipelines, single/two-element long arrays and a Lombok-generated static logger.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/crystalline/MathematicsOperations.java}
 * blob {@code 360dce1eb176ee5e2d57551907e3a4d5c709cd18} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MathematicsOperations {
    private static final PlaceholderLogger log = PlaceholderLogger.of(MathematicsOperations.class);


    /**
     * Private constructor for the static declaration owner.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body; ordinary callers cannot construct this owner.
     * @m3.dataStructures No declared instance fields or constructor parameters.
     */
    private MathematicsOperations() {}

    /** The golden ratio φ = (1 + √5) / 2.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the field with (1+Math.sqrt(5))/2 using double square root, addition and division; stores the rounded result.
     * @m3.dataStructures One static final double field.
     */
    public static final double PHI = (1 + Math.sqrt(5)) / 2;

    /** Euler's number e ≈ 2.71828.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from Math.E without recomputing the mathematical constant.
     * @m3.dataStructures One static final double field.
     */
    public static final double E = Math.E;

    /** Pi π ≈ 3.14159.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from Math.PI without recomputing the mathematical constant.
     * @m3.dataStructures One static final double field.
     */
    public static final double PI = Math.PI;

    /** Euler-Mascheroni constant γ ≈ 0.5772.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes the double field from the decimal literal 0.5772156649015329; retains its binary floating-point representation.
     * @m3.dataStructures One static final double field.
     */
    public static final double GAMMA = 0.5772156649015329;

    /**
     * Compute factorial n! (deterministic).
     *
     * @param n
     *            non-negative integer
     * @return n factorial
     * @m3.pattern synexia:bop
     * @m3.algorithm Rejects negative n, returns one for zero or one, then reduces LongStream.rangeClosed(2,n) by long multiplication with identity one; overflow wraps without detection.
     * @m3.dataStructures One int input, a LongStream range, long accumulator values and a long result.
     */
    public static long factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be non-negative");
        if (n <= 1) return 1;
        return java.util.stream.LongStream.rangeClosed(2, n).reduce(1, (a, b) -> a * b);
    }

    /**
     * Compute binomial coefficient C(n,k) (deterministic).
     *
     * @param n
     *            total items
     * @param k
     *            items to choose
     * @return binomial coefficient
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns zero for k outside [0,n] and one for k equal to zero or n; otherwise scans min(k,n-k) indices and repeatedly evaluates result*(n-i)/(i+1) with unchecked long multiplication followed by integer division.
     * @m3.dataStructures Two int inputs, one final int range bound, a single-element long array, an IntStream and a long result.
     */
    public static long binomial(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        final int kFinal = k > n - k ? n - k : k;
        long[] result = {1};
        java.util.stream.IntStream.range(0, kFinal).forEach(i -> result[0] = result[0] * (n - i) / (i + 1));
        return result[0];
    }

    /**
     * Fibonacci number (deterministic).
     *
     * @param n
     *            index (0-indexed)
     * @return F_n
     * @m3.pattern synexia:bop
     * @m3.algorithm Rejects negative n, returns n for zero or one, then updates the pair [0,1] for indices two through n using long addition and shifts; overflow wraps without detection.
     * @m3.dataStructures One int index, a two-element long array, an IntStream range, a temporary long and a long result.
     */
    public static long fibonacci(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be non-negative");
        if (n <= 1) return n;
        long[] fib = {0, 1};
        java.util.stream.IntStream.rangeClosed(2, n).forEach(i -> {
            long temp = fib[0] + fib[1];
            fib[0] = fib[1];
            fib[1] = temp;
        });
        return fib[1];
    }

    /**
     * Is n prime? (deterministic).
     *
     * @param n
     *            number to test
     * @return true if prime
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns false below two, true at two and false for other even inputs; scans odd long divisors starting at three while i*i compares less than or equal to n and returns noneMatch for divisibility. The unchecked squared-divisor bound can overflow.
     * @m3.dataStructures One long input, a LongStream iterate state, remainder/squared-divisor intermediates and a boolean result.
     */
    public static boolean isPrime(long n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        return java.util.stream.LongStream.iterate(3, i -> i * i <= n, i -> i + 2)
                .noneMatch(i -> n % i == 0);
    }

    /**
     * Greatest common divisor (Euclidean algorithm - deterministic).
     *
     * @param a
     *            first number
     * @param b
     *            second number
     * @return GCD(a, b)
     * @m3.pattern synexia:bop
     * @m3.algorithm Applies Math.abs to both long inputs, repeatedly replaces the pair with b and a%b until b is zero, then returns a; Math.abs(Long.MIN_VALUE) remains negative, so a nonnegative result is not guaranteed.
     * @m3.dataStructures Two mutable long parameters, one temporary long and a long result.
     */
    // NFL: Euclidean algorithm — mutable swap pair — loop preserved per M3 §3
    public static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    /**
     * Least common multiple (deterministic).
     *
     * @param a
     *            first number
     * @param b
     *            second number
     * @return LCM(a, b)
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns zero if either input is zero; otherwise evaluates Math.abs(a*b)/gcd(a,b), retaining unchecked product overflow, the Long.MIN_VALUE absolute-value result and integer division.
     * @m3.dataStructures Two long inputs, an intermediate long product, a gcd result and a long quotient.
     */
    public static long lcm(long a, long b) {
        if (a == 0 || b == 0) return 0;
        return Math.abs(a * b) / gcd(a, b);
    }
}
