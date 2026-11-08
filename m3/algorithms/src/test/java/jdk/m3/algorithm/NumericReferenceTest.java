// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static jdk.m3.algorithm.SortingRangeStreamingReferenceTest.report;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

//FLLDCIM: ORACLE - M3JDK-authored reference oracle of recipe/algorithm-extras-20261106 (the donor catalogue has no tests)
/**
 * Reference oracle of M3NumericAlgorithms: every result is compared with exact BigInteger arithmetic, including the
 * overflow refusals (an exact result outside long must throw ArithmeticException, never wrap).
 */
class NumericReferenceTest {
    private static final long SEED = 20261106L;
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final long[] EXTREMES = {Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L, Long.MAX_VALUE - 1,
        Long.MAX_VALUE, 1L << 62, -(1L << 62), 1L << 32};

    private static long anyLong(SplittableRandom random) {
        int tier = random.nextInt(10);
        return tier == 0 ? EXTREMES[random.nextInt(EXTREMES.length)] : tier < 5 ? random.nextLong()
                : random.nextLong(-100_000, 100_001);
    }

    private static BigInteger big(long value) {
        return BigInteger.valueOf(value);
    }

    @Test
    void gcdAndLcmMatchBigInteger() {
        SplittableRandom random = new SplittableRandom(SEED);
        long cases = 0;
        for (int i = 0; i < 200_000; i++, cases++) {
            long a = anyLong(random);
            long b = anyLong(random);
            BigInteger gcd = big(a).abs().gcd(big(b).abs());
            if (gcd.compareTo(LONG_MAX) > 0) {
                assertThrows(ArithmeticException.class, () -> NumericAlgorithms.gcd(a, b), a + "," + b);
            } else {
                assertEquals(gcd.longValueExact(), NumericAlgorithms.gcd(a, b), a + "," + b);
            }
            BigInteger lcm = a == 0 || b == 0 ? BigInteger.ZERO : big(a).multiply(big(b)).abs().divide(gcd);
            if (lcm.compareTo(LONG_MAX) > 0) {
                assertThrows(ArithmeticException.class, () -> NumericAlgorithms.lcm(a, b), a + "," + b);
            } else {
                assertEquals(lcm.longValueExact(), NumericAlgorithms.lcm(a, b), a + "," + b);
            }
        }
        report("numeric", "gcd+lcm", cases * 2, cases * 2);
    }

    @Test
    void modularArithmeticMatchesBigInteger() {
        SplittableRandom random = new SplittableRandom(SEED + 1);
        long cases = 0;
        for (int i = 0; i < 100_000; i++, cases++) {
            long base = anyLong(random);
            long exponent = random.nextInt(10) == 0 ? random.nextLong(0, 4) : random.nextLong(0, Long.MAX_VALUE);
            long modulus = random.nextInt(10) == 0 ? EXTREMES[random.nextInt(EXTREMES.length)]
                    : random.nextBoolean() ? random.nextLong(1, Long.MAX_VALUE) : random.nextLong(1, 1_000);
            if (modulus <= 0) {
                assertThrows(IllegalArgumentException.class, () -> NumericAlgorithms.modPow(base, exponent,
                        modulus));
                continue;
            }
            assertEquals(big(base).modPow(big(exponent), big(modulus)).longValueExact(),
                    NumericAlgorithms.modPow(base, exponent, modulus), base + "^" + exponent + "%" + modulus);
        }
        assertThrows(IllegalArgumentException.class, () -> NumericAlgorithms.modPow(2, -1, 7));
        report("numeric", "modPow", cases + 1, cases + 1);
        long inverses = 0;
        for (int i = 0; i < 100_000; i++, inverses++) {
            long value = anyLong(random);
            long modulus = random.nextInt(10) == 0 ? random.nextLong(-3, 2) : random.nextLong(2, Long.MAX_VALUE);
            if (modulus <= 1) {
                assertThrows(IllegalArgumentException.class, () -> NumericAlgorithms.modInverse(value, modulus));
            } else if (!big(value).gcd(big(modulus)).equals(BigInteger.ONE)) {
                assertThrows(ArithmeticException.class, () -> NumericAlgorithms.modInverse(value, modulus));
            } else {
                assertEquals(big(value).modInverse(big(modulus)).longValueExact(),
                        NumericAlgorithms.modInverse(value, modulus));
            }
        }
        report("numeric", "modInverse", inverses, inverses);
    }

    @Test
    void primesMatchBigIntegerAndTrialDivision() {
        long cases = 0;
        for (int value = -10; value <= 100_000; value++, cases++) {
            assertEquals(value >= 2 && big(value).isProbablePrime(64), NumericAlgorithms.isPrime(value), "" + value);
        }
        SplittableRandom random = new SplittableRandom(SEED + 2);
        for (int i = 0; i < 20_000; i++, cases++) {
            int value = i < 4 ? new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE, 2_147_483_629, 46_337 * 46_337}[i]
                    : random.nextInt();
            assertEquals(value >= 2 && big(value).isProbablePrime(64), NumericAlgorithms.isPrime(value), "" + value);
        }
        report("numeric", "isPrime", cases, cases);
        int limit = 200_000;
        int[] primes = NumericAlgorithms.sievePrimes(limit);
        int[] spf = NumericAlgorithms.smallestPrimeFactors(limit);
        int cursor = 0;
        for (int value = 0; value <= limit; value++) {
            boolean prime = NumericAlgorithms.isPrime(value);
            if (prime) {
                assertEquals(value, primes[cursor++]);
            }
            int smallest = 0;
            for (int d = 2; value >= 2 && smallest == 0; d++) {
                smallest = value % d == 0 ? d : (long) d * d > value ? value : 0;
            }
            assertEquals(smallest, spf[value], "spf " + value);
        }
        assertEquals(primes.length, cursor);
        assertEquals(0, NumericAlgorithms.sievePrimes(1).length);
        assertThrows(IllegalArgumentException.class, () -> NumericAlgorithms.sievePrimes(-1));
        report("numeric", "sievePrimes+smallestPrimeFactors", 2L * (limit + 1), 2L * (limit + 1));
        long factorizations = 0;
        for (int i = 0; i < 10_000; i++, factorizations++) {
            long value = i < 3 ? new long[] {1L, 2L, 999_999_999_989L}[i]
                    : random.nextBoolean() ? random.nextLong(1, 1_000_000) : random.nextLong(1, 10_000_000_000L);
            long[] flat = NumericAlgorithms.primeFactorization(value);
            BigInteger product = BigInteger.ONE;
            long previous = 1;
            for (int k = 0; k < flat.length; k += 2) {
                assertTrue(flat[k] > previous && big(flat[k]).isProbablePrime(64), value + " factor " + flat[k]);
                assertTrue(flat[k + 1] >= 1);
                product = product.multiply(big(flat[k]).pow((int) flat[k + 1]));
                previous = flat[k];
            }
            assertEquals(big(value), product, "" + value);
        }
        assertThrows(IllegalArgumentException.class, () -> NumericAlgorithms.primeFactorization(0));
        report("numeric", "primeFactorization", factorizations + 1, factorizations + 1);
    }

    @Test
    void combinatoricsMatchExactFormulas() {
        BigInteger[] factorial = new BigInteger[601];
        factorial[0] = BigInteger.ONE;
        for (int i = 1; i < factorial.length; i++) {
            factorial[i] = factorial[i - 1].multiply(big(i));
        }
        long cases = 0;
        for (int n = 0; n <= 200; n++) {
            for (int k = 0; k <= n; k++, cases++) {
                assertEquals(factorial[n].divide(factorial[k].multiply(factorial[n - k])),
                        NumericAlgorithms.binomial(n, k), n + "C" + k);
            }
        }
        assertThrows(IllegalArgumentException.class, () -> NumericAlgorithms.binomial(3, 4));
        assertThrows(IllegalArgumentException.class, () -> NumericAlgorithms.binomial(-1, 0));
        report("numeric", "binomial", cases + 2, cases + 2);
        BigInteger a = BigInteger.ZERO;
        BigInteger b = BigInteger.ONE;
        for (int n = 0; n <= 3_000; n++) {
            assertEquals(a, NumericAlgorithms.fibonacci(n), "F" + n);
            BigInteger next = a.add(b);
            a = b;
            b = next;
        }
        assertThrows(IllegalArgumentException.class, () -> NumericAlgorithms.fibonacci(-1));
        report("numeric", "fibonacci", 3_002, 3_002);
        for (int n = 0; n <= 300; n++) {
            assertEquals(factorial[2 * n].divide(factorial[n + 1].multiply(factorial[n])),
                    NumericAlgorithms.catalan(n), "C" + n);
        }
        assertThrows(IllegalArgumentException.class, () -> NumericAlgorithms.catalan(-1));
        report("numeric", "catalan", 302, 302);
        assertArrayEquals(new long[] {2, 2, 3, 1}, NumericAlgorithms.primeFactorization(12));
    }
}
