// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.math.BigInteger;
import java.util.Arrays;

/** Number-theory and combinatorics primitives with explicit overflow contracts. */
//FLLDCIM: RELOCATE - donor blob 96acbfd0afa9 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/algorithm-extras-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/algorithm-extras-20261106/relocate.py v1
public final class M3NumericAlgorithms {
    private M3NumericAlgorithms() {}

    public static long gcd(long left, long right) {
        if (left == Long.MIN_VALUE || right == Long.MIN_VALUE) {
            BigInteger a = BigInteger.valueOf(left).abs();
            BigInteger b = BigInteger.valueOf(right).abs();
            BigInteger gcd = a.gcd(b);
            if (gcd.bitLength() > 63) throw new ArithmeticException("gcd does not fit signed long");
            return gcd.longValueExact();
        }
        long a = Math.abs(left);
        long b = Math.abs(right);
        while (b != 0L) {
            long remainder = a % b;
            a = b;
            b = remainder;
        }
        return a;
    }

    public static long lcm(long left, long right) {
        if (left == 0L || right == 0L) return 0L;
        long gcd = gcd(left, right);
        long scaled = left / gcd;
        long product = Math.multiplyExact(scaled, right);
        if (product == Long.MIN_VALUE) throw new ArithmeticException("absolute lcm overflow");
        return Math.abs(product);
    }

    public static long modPow(long base, long exponent, long modulus) {
        if (exponent < 0L) throw new IllegalArgumentException("exponent must be non-negative");
        if (modulus <= 0L) throw new IllegalArgumentException("modulus must be positive");
        long result = 1L % modulus;
        long factor = Math.floorMod(base, modulus);
        long power = exponent;
        while (power != 0L) {
            if ((power & 1L) != 0L) result = multiplyMod(result, factor, modulus);
            power >>>= 1;
            if (power != 0L) factor = multiplyMod(factor, factor, modulus);
        }
        return result;
    }

    public static long modInverse(long value, long modulus) {
        if (modulus <= 1L) throw new IllegalArgumentException("modulus must be > 1");
        BigInteger mod = BigInteger.valueOf(modulus);
        return BigInteger.valueOf(value).mod(mod).modInverse(mod).longValueExact();
    }

    public static boolean isPrime(int value) {
        if (value < 2) return false;
        if ((value & 1) == 0) return value == 2;
        for (int divisor = 3; (long) divisor * divisor <= value; divisor += 2) {
            if (value % divisor == 0) return false;
        }
        return true;
    }

    /** All primes <= limit. */
    public static int[] sievePrimes(int limit) {
        if (limit < 0) throw new IllegalArgumentException("limit must be non-negative");
        if (limit < 2) return new int[0];
        boolean[] composite = new boolean[limit + 1];
        int count = 0;
        for (int value = 2; value <= limit; value++) {
            if (!composite[value]) {
                count++;
                if ((long) value * value <= limit) {
                    for (int multiple = value * value; multiple <= limit; multiple += value) {
                        composite[multiple] = true;
                    }
                }
            }
        }
        int[] primes = new int[count];
        int cursor = 0;
        for (int value = 2; value <= limit; value++) {
            if (!composite[value]) primes[cursor++] = value;
        }
        return primes;
    }

    /** Smallest prime factor table where result[x] is 0 for x<2 and otherwise the SPF of x. */
    public static int[] smallestPrimeFactors(int limit) {
        if (limit < 0) throw new IllegalArgumentException("limit must be non-negative");
        int[] spf = new int[limit + 1];
        for (int value = 2; value <= limit; value++) {
            if (spf[value] != 0) continue;
            spf[value] = value;
            if ((long) value * value <= limit) {
                for (int multiple = value * value; multiple <= limit; multiple += value) {
                    if (spf[multiple] == 0) spf[multiple] = value;
                }
            }
        }
        return spf;
    }

    /** Flattened [prime, exponent, prime, exponent, ...] for positive input. */
    public static long[] primeFactorization(long value) {
        if (value <= 0L) throw new IllegalArgumentException("value must be positive");
        if (value == 1L) return new long[0];
        long remaining = value;
        long[] output = new long[16];
        int size = 0;
        for (long divisor = 2L; divisor <= remaining / divisor; divisor += divisor == 2L ? 1L : 2L) {
            if (remaining % divisor != 0L) continue;
            int exponent = 0;
            while (remaining % divisor == 0L) {
                remaining /= divisor;
                exponent++;
            }
            if (size + 2 > output.length) output = Arrays.copyOf(output, output.length << 1);
            output[size++] = divisor;
            output[size++] = exponent;
        }
        if (remaining > 1L) {
            if (size + 2 > output.length) output = Arrays.copyOf(output, output.length << 1);
            output[size++] = remaining;
            output[size++] = 1L;
        }
        return Arrays.copyOf(output, size);
    }

    public static BigInteger binomial(int n, int k) {
        if (n < 0 || k < 0 || k > n) throw new IllegalArgumentException("requires 0<=k<=n");
        int selected = Math.min(k, n - k);
        BigInteger result = BigInteger.ONE;
        for (int i = 1; i <= selected; i++) {
            result = result.multiply(BigInteger.valueOf(n - selected + i))
                    .divide(BigInteger.valueOf(i));
        }
        return result;
    }

    public static BigInteger fibonacci(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be non-negative");
        return fibonacciPair(n)[0];
    }

    public static BigInteger catalan(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be non-negative");
        int doubled = Math.multiplyExact(2, n);
        return binomial(doubled, n).divide(BigInteger.valueOf(n + 1L));
    }

    private static BigInteger[] fibonacciPair(int n) {
        if (n == 0) return new BigInteger[] {BigInteger.ZERO, BigInteger.ONE};
        BigInteger[] pair = fibonacciPair(n >>> 1);
        BigInteger a = pair[0];
        BigInteger b = pair[1];
        BigInteger c = a.multiply(b.shiftLeft(1).subtract(a));
        BigInteger d = a.multiply(a).add(b.multiply(b));
        return (n & 1) == 0
                ? new BigInteger[] {c, d}
                : new BigInteger[] {d, c.add(d)};
    }

    private static long multiplyMod(long left, long right, long modulus) {
        long a = left;
        long b = right;
        long result = 0L;
        while (b != 0L) {
            if ((b & 1L) != 0L) result = addMod(result, a, modulus);
            b >>>= 1;
            if (b != 0L) a = addMod(a, a, modulus);
        }
        return result;
    }

    private static long addMod(long left, long right, long modulus) {
        return left >= modulus - right
                ? left - (modulus - right)
                : left + right;
    }
}
