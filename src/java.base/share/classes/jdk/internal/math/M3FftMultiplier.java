/*
 * @(#)FftMultiplier.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package jdk.internal.math;

import java.math.BigInteger;

import static jdk.internal.math.M3FastDoubleMath.fastScalb;
import static jdk.internal.math.M3FastDoubleSwar.fma;

/**
 * Provides methods for multiplying two {@link BigInteger}s using the
 * {@code FFT algorithm}.
 * <p>
 * This code is based on {@code bigint} by Timothy Buktu.
 * <p>
 * References:
 * <dl>
 * <dt>bigint, Copyright 2013 Timothy Buktu, 2-clause BSD License.<br>
 * Note: We only use portions from this project, that have been marked with 2-clause BSD License
 * in this file <a href="https://github.com/tbuktu/bigint/blob/617c8cd8a7c5e4fb4d919c6a4d11e2586107f029/LICENSE">LICENSE</a>.
 * </dt>
 * <dd><a href="https://github.com/tbuktu/bigint/tree/floatfft">github.com</a></dd>
 * </dl>
 */
class M3FftMultiplier {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** 19 bits per FFT point. */
    private static final int POINT_BITS_19 = 19;
    /** log2 of the most FFT points that 19 bits per point allow. */
    private static final int POINT_BITS_19_LOG_MAX_POINTS = 9;
    /** 18 bits per FFT point. */
    private static final int POINT_BITS_18 = 18;
    /** log2 of the most FFT points that 18 bits per point allow. */
    private static final int POINT_BITS_18_LOG_MAX_POINTS = 10;
    /** 17 bits per FFT point. */
    private static final int POINT_BITS_17 = 17;
    /** log2 of the most FFT points that 17 bits per point allow. */
    private static final int POINT_BITS_17_LOG_MAX_POINTS = 12;
    /** 16 bits per FFT point. */
    private static final int POINT_BITS_16 = 16;
    /** log2 of the most FFT points that 16 bits per point allow. */
    private static final int POINT_BITS_16_LOG_MAX_POINTS = 14;
    /** 15 bits per FFT point. */
    private static final int POINT_BITS_15 = 15;
    /** log2 of the most FFT points that 15 bits per point allow. */
    private static final int POINT_BITS_15_LOG_MAX_POINTS = 16;
    /** 14 bits per FFT point. */
    private static final int POINT_BITS_14 = 14;
    /** log2 of the most FFT points that 14 bits per point allow. */
    private static final int POINT_BITS_14_LOG_MAX_POINTS = 18;
    /** 13 bits per FFT point. */
    private static final int POINT_BITS_13 = 13;
    /** log2 of the most FFT points that 13 bits per point allow. */
    private static final int POINT_BITS_13_LOG_MAX_POINTS = 20;
    /** 12 bits per FFT point. */
    private static final int POINT_BITS_12 = 12;
    /** log2 of the most FFT points that 12 bits per point allow. */
    private static final int POINT_BITS_12_LOG_MAX_POINTS = 21;
    /** 11 bits per FFT point. */
    private static final int POINT_BITS_11 = 11;
    /** log2 of the most FFT points that 11 bits per point allow. */
    private static final int POINT_BITS_11_LOG_MAX_POINTS = 23;
    /** 10 bits per FFT point. */
    private static final int POINT_BITS_10 = 10;
    /** log2 of the most FFT points that 10 bits per point allow. */
    private static final int POINT_BITS_10_LOG_MAX_POINTS = 25;
    /** 9 bits per FFT point. */
    private static final int POINT_BITS_9 = 9;
    /** log2 of the most FFT points that 9 bits per point allow. */
    private static final int POINT_BITS_9_LOG_MAX_POINTS = 27;
    /** 8 bits per FFT point: the fewest, for the largest inputs. */
    private static final int POINT_BITS_8 = 8;
    /** x / HALF_DIVISOR is half of x. */
    private static final int HALF_DIVISOR = 2;
    /** pi / 2 is the first quadrant that the roots of unity cover. */
    private static final double QUADRANT_PI_FACTOR = 0.5;
    /** Index of the top bit of an int: INT_MSB_INDEX - numberOfLeadingZeros(n) is floor(log2(n)). */
    private static final int INT_MSB_INDEX = 31;
    /** FFT stages done by one radix-4 pass. */
    private static final int RADIX_4_STAGES = 2;
    /** x / QUARTER_DIVISOR is a quarter of x. */
    private static final int QUARTER_DIVISOR = 4;
    /** x * THREE_QUARTERS_NUMERATOR / QUARTER_DIVISOR is three quarters of x. */
    private static final int THREE_QUARTERS_NUMERATOR = 3;
    /** Points of one radix-2 butterfly. */
    private static final int RADIX_2_SPAN = 2;
    /** cos(2 pi / 3): the real part of a primitive cube root of unity. */
    private static final double MINUS_ONE_HALF = -0.5;
    /** Radix of the length-3 transform; sqrt(RADIX_3) / 2 is sin(pi / 3). */
    private static final int RADIX_3 = 3;
    /** One half. */
    private static final double ONE_HALF = 0.5;
    /** x / THIRD_DIVISOR is a third of x. */
    private static final int THIRD_DIVISOR = 3;
    /** oneThird * TWO_THIRDS_NUMERATOR ends the second third. */
    private static final int TWO_THIRDS_NUMERATOR = 2;
    /** Most bits per FFT point that still leave slack in an int. */
    private static final int MAX_POINT_BITS = 25;
    /** Assertion message: too many bits per FFT point. */
    private static final String MSG_POINT_BITS_NO_SLACK = " does not fit into an int with slack";
    /** Bits of an int. */
    private static final int INT_BITS = 32;
    /** Bytes of one int for each of the two result parts (real and imaginary). */
    private static final int TWO_PARTS_INT_BYTES = 8;
    /** Rounds a bit count up to whole ints. */
    private static final int INT_BITS_ROUND_UP = 31;
    /** Bits of a byte. */
    private static final int BITS_PER_BYTE = 8;
    /** Converts a bit index to a byte index and back. */
    private static final int BITS_TO_BYTES_SHIFT = 3;
    /** Bytes of an int. */
    private static final int INT_BYTES = 4;
    /** Assertion message: operand a is null. */
    private static final String MSG_A_NULL = "a==null";
    /** Assertion message: operand b is null. */
    private static final String MSG_B_NULL = "b==null";
    /** Bits of an int, as a long (the range check runs in 64 bits). */
    private static final long INT_BITS_LONG = 32L;
    /** ArithmeticException message: the product exceeds the supported BigInteger range. */
    private static final String MSG_RANGE_OVERFLOW = "BigInteger would overflow supported range";
    /** Mixed-radix (3 * 2^n) transforms need a log2 length above this. */
    private static final int MIN_MIXED_RADIX_LOG_LENGTH = 3;
    /** log2 length reduction of the power-of-two parts of a 3 * 2^n transform. */
    private static final int MIXED_RADIX_LOG_SHIFT = 2;
    /** log2 length reduction of the mixed-radix twiddle factors. */
    private static final int TWIDDLES_LOG_SHIFT = 4;
    /** Moves the int sign bit to bit 0. */
    private static final int INT_SIGN_SHIFT = 31;

    public static final double COS_0_25 = Math.cos(0.25 * Math.PI);
    public static final double SIN_0_25 = Math.sin(0.25 * Math.PI);
    /**
     * The threshold value for using floating point FFT multiplication.
     * If the number of bits in each mag array is greater than the
     * Toom-Cook threshold, and the number of bits in at least one of
     * the mag arrays is greater than this threshold, then FFT
     * multiplication will be used.
     */
    private static final int FFT_THRESHOLD = 33220;
    /**
     * This constant limits {@code mag.length} of BigIntegers to the supported
     * range.
     */
    private static final int MAX_MAG_LENGTH = Integer.MAX_VALUE / Integer.SIZE + 1; // (1 << 26)
    /**
     * for FFTs of length up to 3*2^19
     */
    private static final int ROOTS3_CACHE_SIZE = 20;
    /**
     * for FFTs of length up to 2^19
     */
    private static final int ROOTS_CACHE2_SIZE = 20;
    /**
     * The threshold value for using 3-way Toom-Cook multiplication.
     */
    private static final int TOOM_COOK_THRESHOLD = 240 * 8;
    /**
     * Sets of complex roots of unity. The set at index k contains 2^k
     * elements representing all (2^(k+2))-th roots between 0 and pi/2.
     * Used for FFT multiplication.
     */
    private volatile static ComplexVector[] ROOTS2_CACHE = new ComplexVector[ROOTS_CACHE2_SIZE];
    /**
     * Sets of complex roots of unity. The set at index k contains 3*2^k
     * elements representing all (3*2^(k+2))-th roots between 0 and pi/2.
     * Used for FFT multiplication.
     */
    private volatile static ComplexVector[] ROOTS3_CACHE = new ComplexVector[ROOTS3_CACHE_SIZE];

    /**
     * Returns the maximum number of bits that one double precision number can fit without
     * causing the multiplication to be incorrect.
     *
     * @param bitLen length of this number in bits
     * @return the maximum number of bits
     */
    static int bitsPerFftPoint(int bitLen) {
        if (bitLen <= POINT_BITS_19 * (1 << POINT_BITS_19_LOG_MAX_POINTS)) {
            return POINT_BITS_19;
        }
        if (bitLen <= POINT_BITS_18 * (1 << POINT_BITS_18_LOG_MAX_POINTS)) {
            return POINT_BITS_18;
        }
        if (bitLen <= POINT_BITS_17 * (1 << POINT_BITS_17_LOG_MAX_POINTS)) {
            return POINT_BITS_17;
        }
        if (bitLen <= POINT_BITS_16 * (1 << POINT_BITS_16_LOG_MAX_POINTS)) {
            return POINT_BITS_16;
        }
        if (bitLen <= POINT_BITS_15 * (1 << POINT_BITS_15_LOG_MAX_POINTS)) {
            return POINT_BITS_15;
        }
        if (bitLen <= POINT_BITS_14 * (1 << POINT_BITS_14_LOG_MAX_POINTS)) {
            return POINT_BITS_14;
        }
        if (bitLen <= POINT_BITS_13 * (1 << POINT_BITS_13_LOG_MAX_POINTS)) {
            return POINT_BITS_13;
        }
        if (bitLen <= POINT_BITS_12 * (1 << POINT_BITS_12_LOG_MAX_POINTS)) {
            return POINT_BITS_12;
        }
        if (bitLen <= POINT_BITS_11 * (1 << POINT_BITS_11_LOG_MAX_POINTS)) {
            return POINT_BITS_11;
        }
        if (bitLen <= POINT_BITS_10 * (1 << POINT_BITS_10_LOG_MAX_POINTS)) {
            return POINT_BITS_10;
        }
        if (bitLen <= POINT_BITS_9 * (1 << POINT_BITS_9_LOG_MAX_POINTS)) {
            return POINT_BITS_9;
        }
        return POINT_BITS_8;
    }

    /**
     * Returns n-th complex roots of unity for the angles 0..pi/2, suitable
     * for a transform of length n.
     * They are used as twiddle factors and as weights for the right-angle transform.
     * n must be 1 or an even number.
     */
    private static ComplexVector calculateRootsOfUnity(int n) {
        if (n == 1) {
            ComplexVector v = new ComplexVector(1);
            v.real(0, 1);
            v.imag(0, 0);
            return v;
        }
        ComplexVector roots = new ComplexVector(n);
        roots.set(0, 1.0, 0.0);
        double cos = COS_0_25;
        double sin = SIN_0_25;
        roots.set(n / HALF_DIVISOR, cos, sin);
        double angleTerm = QUADRANT_PI_FACTOR * Math.PI / n;
        for (int i = 1; i < n / HALF_DIVISOR; i++) {
            double angle = angleTerm * i;
            cos = Math.cos(angle);
            sin = Math.sin(angle);
            roots.set(i, cos, sin);
            roots.set(n - i, sin, cos);
        }
        return roots;
    }

    /**
     * Performs an FFT of length 2^n on the vector {@code a}.
     * This is a decimation-in-frequency implementation.
     *
     * @param a     input and output, must be a power of two in size
     * @param roots an array that contains one set of roots at indices
     *              log2(a.length), log2(a.length)-2, log2(a.length)-4, ...
     *              Each roots[s] must contain 2^s roots of unity such that
     *              {@code roots[s][k] = e^(pi*k*i/(2*roots.length))},
     *              i.e., they must cover the first quadrant.
     */
    private static void fft(ComplexVector a, ComplexVector[] roots) {
        int n = a.length;
        int logN = INT_MSB_INDEX - Integer.numberOfLeadingZeros(n);
        MutableComplex a0 = new MutableComplex();
        MutableComplex a1 = new MutableComplex();
        MutableComplex a2 = new MutableComplex();
        MutableComplex a3 = new MutableComplex();

        // do two FFT stages at a time (radix-4)
        MutableComplex omega1 = new MutableComplex();
        MutableComplex omega2 = new MutableComplex();
        int s = logN;
        for (; s >= RADIX_4_STAGES; s -= RADIX_4_STAGES) {
            ComplexVector rootsS = roots[s - RADIX_4_STAGES];
            int m = 1 << s;
            for (int i = 0; i < n; i += m) {
                for (int j = 0; j < m / QUARTER_DIVISOR; j++) {
                    omega1.set(rootsS, j);
                    // computing omega2 from omega1 is less accurate than Math.cos() and Math.sin(),
                    // but it is the same error we'd incur with radix-2, so we're not breaking the
                    // assumptions of the Percival paper.
                    omega1.squareInto(omega2);

                    int idx0 = i + j;
                    int idx1 = i + j + m / QUARTER_DIVISOR;
                    int idx2 = i + j + m / HALF_DIVISOR;
                    int idx3 = i + j + m * THREE_QUARTERS_NUMERATOR / QUARTER_DIVISOR;

                    // radix-4 butterfly:
                    //   a[idx0] = (a[idx0] + a[idx1]      + a[idx2]      + a[idx3])      * w^0
                    //   a[idx1] = (a[idx0] + a[idx1]*(-i) + a[idx2]*(-1) + a[idx3]*i)    * w^1
                    //   a[idx2] = (a[idx0] + a[idx1]*(-1) + a[idx2]      + a[idx3]*(-1)) * w^2
                    //   a[idx3] = (a[idx0] + a[idx1]*i    + a[idx2]*(-1) + a[idx3]*(-i)) * w^3
                    // where w = omega1^(-1) = conjugate(omega1)
                    a.addInto(idx0, a, idx1, a0);
                    a0.add(a, idx2);
                    a0.add(a, idx3);

                    a.subtractTimesIInto(idx0, a, idx1, a1);
                    a1.subtract(a, idx2);
                    a1.addTimesI(a, idx3);
                    a1.multiplyConjugate(omega1);

                    a.subtractInto(idx0, a, idx1, a2);
                    a2.add(a, idx2);
                    a2.subtract(a, idx3);
                    a2.multiplyConjugate(omega2);

                    a.addTimesIInto(idx0, a, idx1, a3);
                    a3.subtract(a, idx2);
                    a3.subtractTimesI(a, idx3);
                    a3.multiply(omega1);   // Bernstein's trick: multiply by omega^(-1) instead of omega^3

                    a0.copyInto(a, idx0);
                    a1.copyInto(a, idx1);
                    a2.copyInto(a, idx2);
                    a3.copyInto(a, idx3);
                }
            }
        }

        // do one final radix-2 step if there is an odd number of stages
        if (s > 0) {
            for (int i = 0; i < n; i += RADIX_2_SPAN) {
                // omega = 1

                //    a0 = a[i];
                //    a1 = a[i + IMAG];
                //    a[i] += a1;
                //    a[i + IMAG] = a0 - a1;
                a.copyInto(i, a0);
                a.copyInto(i + ComplexVector.IMAG, a1);
                a.add(i, a1);
                a0.subtractInto(a1, a, i + 1);
            }
        }
    }

    /**
     * Performs FFTs or IFFTs of size 3 on the vector {@code (a0[i], a1[i], a2[i])}
     * for each {@code i}. The output is placed back into {@code a0, a1, and a2}.
     *
     * @param a0    inputs / outputs for the first FFT coefficient
     * @param a1    inputs / outputs for the second FFT coefficient
     * @param a2    inputs / outputs for the third FFT coefficient
     * @param sign  1 for a forward FFT, -1 for an inverse FFT
     * @param scale 1 for a forward FFT, 1/3 for an inverse FFT
     */
    private static void fft3(ComplexVector a0, ComplexVector a1, ComplexVector a2, int sign, double scale) {
        double omegaImag = sign * MINUS_ONE_HALF * Math.sqrt(RADIX_3);   // imaginary part of omega for n=3: sin(sign*(-2)*pi*1/3)
        for (int i = 0; i < a0.length; i++) {
            double a0Real = a0.real(i) + a1.real(i) + a2.real(i);
            double a0Imag = a0.imag(i) + a1.imag(i) + a2.imag(i);
            double c = omegaImag * (a2.imag(i) - a1.imag(i));
            double d = omegaImag * (a1.real(i) - a2.real(i));
            double e = ONE_HALF * (a1.real(i) + a2.real(i));
            double f = ONE_HALF * (a1.imag(i) + a2.imag(i));
            double a1Real = a0.real(i) - e + c;
            double a1Imag = a0.imag(i) + d - f;
            double a2Real = a0.real(i) - e - c;
            double a2Imag = a0.imag(i) - d - f;
            a0.real(i, a0Real * scale);
            a0.imag(i, a0Imag * scale);
            a1.real(i, a1Real * scale);
            a1.imag(i, a1Imag * scale);
            a2.real(i, a2Real * scale);
            a2.imag(i, a2Imag * scale);
        }
    }

    /**
     * Performs an FFT of length 3*2^n on the vector {@code a}.
     * Uses the 4-step algorithm to decompose the 3*2^n FFT into 2^n FFTs of
     * length 3 and 3 FFTs of length 2^n.
     * See https://www.nas.nasa.gov/assets/pdf/techreports/1989/rnr-89-004.pdf
     *
     * @param a      input and output, must be 3*2^n in size for some n>=2
     * @param roots2 an array that contains one set of roots at indices
     *               log2(a.length/3), log2(a.length/3)-2, log2(a.length/3)-4, ...
     *               Each roots[s] must contain 2^s roots of unity such that
     *               {@code roots[s][k] = e^(pi*k*i/(2*roots.length))},
     *               i.e., they must cover the first quadrant.
     * @param roots3 must be the same length as {@code a} and contain roots of
     *               unity such that {@code roots[k] = e^(pi*k*i/(2*roots3.length))},
     *               i.e., they need to cover the first quadrant.
     */
    private static void fftMixedRadix(ComplexVector a, ComplexVector[] roots2, ComplexVector roots3) {
        int oneThird = a.length / THIRD_DIVISOR;
        ComplexVector a0 = new ComplexVector(a, 0, oneThird);
        ComplexVector a1 = new ComplexVector(a, oneThird, oneThird * TWO_THIRDS_NUMERATOR);
        ComplexVector a2 = new ComplexVector(a, oneThird * TWO_THIRDS_NUMERATOR, a.length);

        // step 1: perform a.length/3 transforms of length 3
        fft3(a0, a1, a2, 1, 1);

        // step 2: multiply by roots of unity
        MutableComplex omega = new MutableComplex();
        for (int i = 0; i < a.length / QUARTER_DIVISOR; i++) {
            omega.set(roots3, i);
            // a0[i] *= omega^0; a1[i] *= omega^1; a2[i] *= omega^2
            a1.multiplyConjugate(i, omega);
            a2.multiplyConjugate(i, omega);
            a2.multiplyConjugate(i, omega);
        }
        for (int i = a.length / QUARTER_DIVISOR; i < oneThird; i++) {
            omega.set(roots3, i - a.length / QUARTER_DIVISOR);
            // a0[i] *= omega^0; a1[i] *= omega^1; a2[i] *= omega^2
            a1.multiplyConjugateTimesI(i, omega);
            a2.multiplyConjugateTimesI(i, omega);
            a2.multiplyConjugateTimesI(i, omega);
        }

        // step 3 is not needed

        // step 4: perform 3 transforms of length a.length/3
        fft(a0, roots2);
        fft(a1, roots2);
        fft(a2, roots2);
    }

    static BigInteger fromFftVector(ComplexVector fftVec, int signum, int bitsPerFftPoint) {
        assert bitsPerFftPoint <= MAX_POINT_BITS : bitsPerFftPoint + MSG_POINT_BITS_NO_SLACK;

        int fftLen = (int) Math.min(fftVec.length, ((long) MAX_MAG_LENGTH * INT_BITS) / bitsPerFftPoint + 1);
        int magLen = (int) (TWO_PARTS_INT_BYTES * ((long) fftLen * bitsPerFftPoint + INT_BITS_ROUND_UP) / INT_BITS);
        byte[] mag = new byte[magLen];
        int base = 1 << bitsPerFftPoint;
        int bitMask = base - 1;
        int bitPadding = INT_BITS - bitsPerFftPoint;
        long carry = 0;
        int bitLength = mag.length * BITS_PER_BYTE;
        int bitIdx = bitLength - bitsPerFftPoint;
        int magComponent = 0;
        int prevIdx = Math.min(Math.max(0, bitIdx >> BITS_TO_BYTES_SHIFT), mag.length - INT_BYTES);
        for (int part = 0; part <= 1; part++) {   // 0=real, 1=imaginary
            for (int fftIdx = 0; fftIdx < fftLen; fftIdx++) {
                long fftElem = Math.round(fftVec.part(fftIdx, part)) + carry;
                carry = fftElem >> bitsPerFftPoint;

                int idx = Math.min(Math.max(0, bitIdx >> BITS_TO_BYTES_SHIFT), mag.length - INT_BYTES);
                magComponent >>>= (prevIdx - idx) << BITS_TO_BYTES_SHIFT;
                int shift = bitPadding - bitIdx + (idx << BITS_TO_BYTES_SHIFT);
                magComponent |= (fftElem & bitMask) << shift;
                M3FastDoubleSwar.writeIntBE(mag, idx, magComponent);

                prevIdx = idx;
                bitIdx -= bitsPerFftPoint;
            }
        }
        return new BigInteger(signum, mag);
    }

    /**
     * Returns sets of complex roots of unity. For k=logN, logN-2, logN-4, ...,
     * the return value contains all k-th roots between 0 and pi/2.
     *
     * @param logN for a transform of length 2^logN
     */
    private static ComplexVector[] getRootsOfUnity2(int logN) {
        ComplexVector[] roots = new ComplexVector[logN + 1];
        for (int i = logN; i >= 0; i -= RADIX_4_STAGES) {
            if (i < ROOTS_CACHE2_SIZE) {
                if (ROOTS2_CACHE[i] == null) {
                    ROOTS2_CACHE[i] = calculateRootsOfUnity(1 << i);
                }
                roots[i] = ROOTS2_CACHE[i];
            } else {
                roots[i] = calculateRootsOfUnity(1 << i);
            }
        }
        return roots;
    }

    /**
     * Returns sets of complex roots of unity. For k=logN, logN-2, logN-4, ...,
     * the return value contains all k-th roots between 0 and pi/2.
     *
     * @param logN for a transform of length 3*2^logN
     */
    private static ComplexVector getRootsOfUnity3(int logN) {
        if (logN < ROOTS3_CACHE_SIZE) {
            if (ROOTS3_CACHE[logN] == null) {
                ROOTS3_CACHE[logN] = calculateRootsOfUnity(RADIX_3 << logN);
            }
            return ROOTS3_CACHE[logN];
        } else {
            return calculateRootsOfUnity(RADIX_3 << logN);
        }
    }

    /**
     * Performs an inverse FFT of length 2^n on the vector {@code a}.
     * This is a decimation-in-time implementation.
     *
     * @param a     input and output, must be a power of two in size
     * @param roots an array that contains one set of roots at indices
     *              log2(a.length), log2(a.length)-2, log2(a.length)-4, ...
     *              Each roots[s] must contain 2^s roots of unity such that
     *              {@code roots[s][k] = e^(pi*k*i/(2*roots.length))},
     *              i.e., they must cover the first quadrant.
     */
    private static void ifft(ComplexVector a, ComplexVector[] roots) {
        int n = a.length;
        int logN = INT_MSB_INDEX - Integer.numberOfLeadingZeros(n);
        MutableComplex a0 = new MutableComplex();
        MutableComplex a1 = new MutableComplex();
        MutableComplex a2 = new MutableComplex();
        MutableComplex a3 = new MutableComplex();
        MutableComplex b0 = new MutableComplex();
        MutableComplex b1 = new MutableComplex();
        MutableComplex b2 = new MutableComplex();
        MutableComplex b3 = new MutableComplex();

        int s = 1;
        // do one radix-2 step if there is an odd number of stages
        if (logN % RADIX_4_STAGES != 0) {
            for (int i = 0; i < n; i += RADIX_2_SPAN) {
                // omega = 1
                a.copyInto(i + 1, a2);
                a.copyInto(i, a0);
                a.add(i, a2);
                a0.subtractInto(a2, a, i + 1);
            }
            s++;
        }

        // do the remaining stages two at a time (radix-4)
        MutableComplex omega1 = new MutableComplex();
        MutableComplex omega2 = new MutableComplex();
        for (; s <= logN; s += RADIX_4_STAGES) {
            ComplexVector rootsS = roots[s - 1];
            int m = 1 << (s + 1);
            for (int i = 0; i < n; i += m) {
                for (int j = 0; j < m / QUARTER_DIVISOR; j++) {
                    omega1.set(rootsS, j);
                    // computing omega2 from omega1 is less accurate than Math.cos() and Math.sin(),
                    // but it is the same error we'd incur with radix-2, so we're not breaking the
                    // assumptions of the Percival paper.
                    omega1.squareInto(omega2);

                    int idx0 = i + j;
                    int idx1 = i + j + m / QUARTER_DIVISOR;
                    int idx2 = i + j + m / HALF_DIVISOR;
                    int idx3 = i + j + m * THREE_QUARTERS_NUMERATOR / QUARTER_DIVISOR;

                    // radix-4 butterfly:
                    //   a[idx0] = a[idx0]*w^0 + a[idx1]*w^1      + a[idx2]*w^2      + a[idx3]*w^3
                    //   a[idx1] = a[idx0]*w^0 + a[idx1]*i*w^1    + a[idx2]*(-1)*w^2 + a[idx3]*(-i)*w^3
                    //   a[idx2] = a[idx0]*w^0 + a[idx1]*(-1)*w^1 + a[idx2]*w^2      + a[idx3]*(-1)*w^3
                    //   a[idx3] = a[idx0]*w^0 + a[idx1]*(-i)*w^1 + a[idx2]*(-1)*w^2 + a[idx3]*i*w^3
                    // where w = omega1
                    a.copyInto(idx0, a0);
                    a.multiplyInto(idx1, omega1, a1);
                    a.multiplyInto(idx2, omega2, a2);
                    a.multiplyConjugateInto(idx3, omega1, a3);   // Bernstein's trick: multiply by omega^(-1) instead of omega^3

                    a0.addInto(a1, b0);
                    b0.add(a2);
                    b0.add(a3);

                    a0.addTimesIInto(a1, b1);
                    b1.subtract(a2);
                    b1.subtractTimesI(a3);

                    a0.subtractInto(a1, b2);
                    b2.add(a2);
                    b2.subtract(a3);

                    a0.subtractTimesIInto(a1, b3);
                    b3.subtract(a2);
                    b3.addTimesI(a3);

                    b0.copyInto(a, idx0);
                    b1.copyInto(a, idx1);
                    b2.copyInto(a, idx2);
                    b3.copyInto(a, idx3);
                }
            }
        }

        // divide all vector elements by n
        for (int i = 0; i < n; i++) {
            a.timesTwoToThe(i, -logN);
        }
    }

    /**
     * Performs an inverse FFT of length 3*2^n on the vector {@code a}.
     * Uses the 4-step algorithm to decompose the 3*2^n FFT into 2^n FFTs of
     * length 3 and 3 FFTs of length 2^n.
     * See https://www.nas.nasa.gov/assets/pdf/techreports/1989/rnr-89-004.pdf
     *
     * @param a      input and output, must be 3*2^n in size for some n>=2
     * @param roots2 an array that contains one set of roots at indices
     *               log2(a.length/3), log2(a.length/3)-2, log2(a.length/3)-4, ...
     *               Each roots[s] must contain 2^s roots of unity such that
     *               {@code roots[s][k] = e^(pi*k*i/(2*roots.length))},
     *               i.e., they must cover the first quadrant.
     * @param roots3 must be the same length as {@code a} and contain roots of
     *               unity such that {@code roots[k] = e^(pi*k*i/(2*roots3.length))},
     *               i.e., they need to cover the first quadrant.
     */
    private static void ifftMixedRadix(ComplexVector a, ComplexVector[] roots2, ComplexVector roots3) {
        int oneThird = a.length / THIRD_DIVISOR;
        ComplexVector a0 = new ComplexVector(a, 0, oneThird);
        ComplexVector a1 = new ComplexVector(a, oneThird, oneThird * TWO_THIRDS_NUMERATOR);
        ComplexVector a2 = new ComplexVector(a, oneThird * TWO_THIRDS_NUMERATOR, a.length);

        // step 1: perform 3 transforms of length a.length/3
        ifft(a0, roots2);
        ifft(a1, roots2);
        ifft(a2, roots2);

        // step 2: multiply by roots of unity
        MutableComplex omega = new MutableComplex();
        for (int i = 0; i < a.length / QUARTER_DIVISOR; i++) {
            omega.set(roots3, i);
            // a0[i] *= omega^0; a1[i] *= omega^1; a2[i] *= omega^2
            a1.multiply(i, omega);
            a2.multiply(i, omega);
            a2.multiply(i, omega);
        }
        for (int i = a.length / QUARTER_DIVISOR; i < oneThird; i++) {
            omega.set(roots3, i - a.length / QUARTER_DIVISOR);
            // a0[i] *= omega^0; a1[i] *= omega^1; a2[i] *= omega^2
            a1.multiplyByIAnd(i, omega);
            a2.multiplyByIAnd(i, omega);
            a2.multiplyByIAnd(i, omega);
        }

        // step 3 is not needed

        // step 4: perform a.length/3 transforms of length 3
        fft3(a0, a1, a2, -1, 1.0 / THIRD_DIVISOR);
    }

    /**
     * Returns a BigInteger whose value is {@code (a * b)}.
     *
     * @param a value a
     * @param b value b
     * @return {@code this * val}
     * @implNote An implementation may offer better algorithmic
     * performance when {@code a == b}.
     */
    static BigInteger multiply(BigInteger a, BigInteger b) {
        assert a != null : MSG_A_NULL;
        assert b != null : MSG_B_NULL;

        if (b.signum() == 0 || a.signum() == 0) {
            return BigInteger.ZERO;
        }
        // Squaring is slightly faster than multiplication.
        // We check for identity here and not for equality, because an equality check of big integers is very expensive.
        if (b == a) {
            return square(b);
        }

        int xlen = a.bitLength();
        int ylen = b.bitLength();
        if ((long) xlen + ylen > INT_BITS_LONG * MAX_MAG_LENGTH) {
            throw new ArithmeticException(MSG_RANGE_OVERFLOW);
        }

        if (xlen > TOOM_COOK_THRESHOLD
                && ylen > TOOM_COOK_THRESHOLD
                && (xlen > FFT_THRESHOLD || ylen > FFT_THRESHOLD)) {
            return multiplyFft(a, b);
        }
        return a.multiply(b);
    }

    /**
     * Multiplies two BigIntegers using a floating-point FFT.
     * <p>
     * Floating-point math is inaccurate; to ensure the output of the FFT and
     * IFFT rounds to the correct result for every input, the provably safe
     * FFT error bounds from "Rapid Multiplication Modulo The Sum And
     * Difference of Highly Composite Numbers" by Colin Percival, pg. 392
     * (<a href="https://www.daemonology.net/papers/fft.pdf">fft.pdf</a>) are used, the vector is
     * "balanced" before the FFT, and accurate twiddle factors are used.
     * <p>
     * This implementation incorporates several features compared to the
     * standard FFT algorithm
     * (<a href="https://en.wikipedia.org/wiki/Cooley%E2%80%93Tukey_FFT_algorithm">Cooley Tukey FFT algorithm</a>):
     * <ul>
     * <li>It uses a variant called right-angle convolution which weights the
     *     vector before the transform. The benefit of the right-angle
     *     convolution is that when multiplying two numbers of length n, an
     *     FFT of length n suffices whereas a regular FFT needs length 2n.
     *     This is because the right-angle convolution places half of the
     *     result in the real part and the other half in the imaginary part.
     *     See: Discrete Weighted Transforms And Large-Integer Arithmetic by
     *     Richard Crandall and Barry Fagin.
     * <li>FFTs of length 3*2^n are supported in addition to 2^n.
     * <li>Radix-4 butterflies; see
     *     https://www.nxp.com/docs/en/application-note/AN3666.pdf
     * <li>Bernstein's conjugate twiddle trick for a small speed gain at the
     *     expense of (further) reordering the output of the FFT which is not
     *     a problem because it is reordered back in the IFFT.
     * <li>Roots of unity are cached
     * </ul>
     * FFT vectors are stored as arrays of primitive doubles (two array
     * elements are needed for representing one complex number). Storing them
     * as arrays of primitive doubles instead of as MutableComplex objects is
     * memory efficient,
     * but in some cases below ~10^6 decimal digits, it hurts speed because
     * it requires additional copying. Ideally this would be implemented using
     * value types when they become available.
     *
     * @param a value a
     * @param b value b
     * @return a*b
     */
    static BigInteger multiplyFft(BigInteger a, BigInteger b) {
        int signum = a.signum() * b.signum();
        byte[] aMag = (a.signum() < 0 ? a.negate() : a).toByteArray();
        byte[] bMag = (b.signum() < 0 ? b.negate() : b).toByteArray();
        int bitLen = Math.max(aMag.length, bMag.length) * BITS_PER_BYTE;
        int bitsPerPoint = bitsPerFftPoint(bitLen);
        int fftLen = (bitLen + bitsPerPoint - 1) / bitsPerPoint + 1;   // +1 for a possible carry, see toFFTVector()
        int logFFTLen = INT_BITS - Integer.numberOfLeadingZeros(fftLen - 1);

        // Use a 2^n or 3*2^n transform, whichever is shortest
        int fftLen2 = 1 << (logFFTLen);   // rounded to 2^n
        int fftLen3 = fftLen2 * THREE_QUARTERS_NUMERATOR / QUARTER_DIVISOR;   // rounded to 3*2^n
        if (fftLen < fftLen3 && logFFTLen > MIN_MIXED_RADIX_LOG_LENGTH) {
            ComplexVector[] roots2 = getRootsOfUnity2(logFFTLen - MIXED_RADIX_LOG_SHIFT);   // roots for length fftLen/3 which is a power of two
            ComplexVector weights = getRootsOfUnity3(logFFTLen - MIXED_RADIX_LOG_SHIFT);
            ComplexVector twiddles = getRootsOfUnity3(logFFTLen - TWIDDLES_LOG_SHIFT);
            ComplexVector aVec = toFftVector(aMag, fftLen3, bitsPerPoint);
            aVec.applyWeights(weights);
            fftMixedRadix(aVec, roots2, twiddles);
            ComplexVector bVec = toFftVector(bMag, fftLen3, bitsPerPoint);
            bVec.applyWeights(weights);
            fftMixedRadix(bVec, roots2, twiddles);
            aVec.multiplyPointwise(bVec);
            ifftMixedRadix(aVec, roots2, twiddles);
            aVec.applyInverseWeights(weights);
            return fromFftVector(aVec, signum, bitsPerPoint);
        } else {
            ComplexVector[] roots = getRootsOfUnity2(logFFTLen);
            ComplexVector aVec = toFftVector(aMag, fftLen2, bitsPerPoint);
            aVec.applyWeights(roots[logFFTLen]);
            fft(aVec, roots);
            ComplexVector bVec = toFftVector(bMag, fftLen2, bitsPerPoint);
            bVec.applyWeights(roots[logFFTLen]);
            fft(bVec, roots);
            aVec.multiplyPointwise(bVec);
            ifft(aVec, roots);
            aVec.applyInverseWeights(roots[logFFTLen]);
            return fromFftVector(aVec, signum, bitsPerPoint);
        }
    }

    /**
     * Returns a BigInteger whose value is {@code (this<sup>2</sup>)}.
     *
     * @return {@code this<sup>2</sup>}
     */
    static BigInteger square(BigInteger a) {
        if (a.signum() == 0) {
            return BigInteger.ZERO;
        }
        return a.bitLength() < FFT_THRESHOLD ? a.multiply(a) : squareFft(a);
    }

    static BigInteger squareFft(BigInteger a) {
        // M3: square of the magnitude (toByteArray() of a negative is two's complement; multiplyFft negates too)
        byte[] mag = (a.signum() < 0 ? a.negate() : a).toByteArray();
        int bitLen = mag.length * BITS_PER_BYTE;
        int bitsPerPoint = bitsPerFftPoint(bitLen);
        int fftLen = (bitLen + bitsPerPoint - 1) / bitsPerPoint + 1;   // +1 for a possible carry, see toFFTVector()
        int logFFTLen = INT_BITS - Integer.numberOfLeadingZeros(fftLen - 1);

        // Use a 2^n or 3*2^n transform, whichever is shorter
        int fftLen2 = 1 << (logFFTLen);   // rounded to 2^n
        int fftLen3 = fftLen2 * THREE_QUARTERS_NUMERATOR / QUARTER_DIVISOR;   // rounded to 3*2^n
        if (fftLen < fftLen3) {
            fftLen = fftLen3;
            ComplexVector vec = toFftVector(mag, fftLen, bitsPerPoint);
            ComplexVector[] roots2 = getRootsOfUnity2(logFFTLen - MIXED_RADIX_LOG_SHIFT);   // roots for length fftLen/3 which is a power of two
            ComplexVector weights = getRootsOfUnity3(logFFTLen - MIXED_RADIX_LOG_SHIFT);
            ComplexVector twiddles = getRootsOfUnity3(logFFTLen - TWIDDLES_LOG_SHIFT);
            vec.applyWeights(weights);
            fftMixedRadix(vec, roots2, twiddles);
            vec.squarePointwise();
            ifftMixedRadix(vec, roots2, twiddles);
            vec.applyInverseWeights(weights);
            return fromFftVector(vec, 1, bitsPerPoint);
        } else {
            fftLen = fftLen2;
            ComplexVector vec = toFftVector(mag, fftLen, bitsPerPoint);
            ComplexVector[] roots = getRootsOfUnity2(logFFTLen);
            vec.applyWeights(roots[logFFTLen]);
            fft(vec, roots);
            vec.squarePointwise();
            ifft(vec, roots);
            vec.applyInverseWeights(roots[logFFTLen]);
            return fromFftVector(vec, 1, bitsPerPoint);
        }
    }

    /**
     * Converts this BigInteger into an array of complex numbers suitable for an FFT.
     * Populates the real parts and sets the imaginary parts to zero.
     */
    static ComplexVector toFftVector(byte[] mag, int fftLen, int bitsPerFftPoint) {
        assert bitsPerFftPoint <= MAX_POINT_BITS : bitsPerFftPoint + MSG_POINT_BITS_NO_SLACK;

        ComplexVector fftVec = new ComplexVector(fftLen);
        if (mag.length < INT_BYTES) {
            byte[] paddedMag = new byte[INT_BYTES];
            System.arraycopy(mag, 0, paddedMag, INT_BYTES - mag.length, mag.length);
            mag = paddedMag;
        }

        // Read fftPoint bits from right (least significant) to left (most significant)
        int base = 1 << bitsPerFftPoint;
        int halfBase = base / HALF_DIVISOR;
        int bitMask = base - 1;
        int bitPadding = INT_BITS - bitsPerFftPoint;
        int bitLength = mag.length * BITS_PER_BYTE;
        int carry = 0;// when we subtract base from a digit, we need to carry one
        int fftIdx = 0;
        for (int bitIdx = bitLength - bitsPerFftPoint; bitIdx > -bitsPerFftPoint; bitIdx -= bitsPerFftPoint) {
            int idx = Math.min(Math.max(0, bitIdx >> BITS_TO_BYTES_SHIFT), mag.length - INT_BYTES);
            int shift = bitPadding - bitIdx + (idx << BITS_TO_BYTES_SHIFT);
            int fftPoint = (M3FastDoubleSwar.readIntBE(mag, idx) >>> shift) & bitMask;

            // "balance" the output digits so -base/2 < digit < base/2
            fftPoint += carry;
            carry = (halfBase - fftPoint) >>> INT_SIGN_SHIFT;// if fftPoint>halfBase then carry:=1, else carry:=0
            fftPoint -= base & (-carry);//if (carry != 0) then  fftPoint -= base;

            fftVec.real(fftIdx, fftPoint);
            fftIdx++;
        }
        // final carry
        if (carry > 0) {
            fftVec.real(fftIdx, carry);
        }

        return fftVec;
    }


    final static class ComplexVector {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** Doubles per complex number (real and imaginary part). */
        private static final int COMPLEX_SIZE = 2;
        /** Factor of the cross term: (a + bi)^2 = a^2 - b^2 + 2abi. */
        private static final int SQUARE_CROSS_FACTOR = 2;

        /**
         * A complex number in an FFT double[] vector occupies 2^1 array elements.
         */
        private final static int COMPLEX_SIZE_SHIFT = 1;
        final static int IMAG = 1;
        final static int REAL = 0;
        /**
         * This arrays contains complex numbers.
         * <p>
         * A complex number occupies 2 consecutive array elements:
         * the real part and then the imaginary part.
         */
        private final double[] a;
        /**
         * The number of complex numbers stored in this vector.
         */
        private final int length;
        /**
         * Offset to the real part of a complex number.
         */
        private final int offset;

        ComplexVector(int length) {
            this.a = new double[length << COMPLEX_SIZE_SHIFT];
            this.length = length;
            this.offset = 0;
        }

        /**
         * Creates a view on another vector.
         *
         * @param c    the other vector
         * @param from start index of the view
         * @param to   end index of the view
         */
        ComplexVector(ComplexVector c, int from, int to) {
            this.length = to - from;
            this.a = c.a;
            this.offset = from << 1;
        }

        void add(int idxa, MutableComplex c) {
            a[realIdx(idxa)] += c.real;
            a[imagIdx(idxa)] += c.imag;
        }

        void addInto(int idxa, ComplexVector c, int idxc, MutableComplex destination) {
            destination.real = a[realIdx(idxa)] + c.real(idxc);
            destination.imag = a[imagIdx(idxa)] + c.imag(idxc);
        }

        void addTimesIInto(int idxa, ComplexVector c, int idxc, MutableComplex destination) {
            destination.real = a[realIdx(idxa)] - c.imag(idxc);
            destination.imag = a[imagIdx(idxa)] + c.real(idxc);
        }

        /**
         * Multiplies the elements of an FFT vector by 1/weight.
         * Used for the right-angle convolution.
         */
        void applyInverseWeights(ComplexVector weights) {
            int offw = weights.offset;
            double[] w = weights.a;
            int end = offset + length << 1;
            for (int offa = offset; offa < end; offa += COMPLEX_SIZE) {
                // the following code is the same as: this.multiplyConjugate(i, weights[i]);

                double real = a[offa + REAL];
                double imag = a[offa + IMAG];
                a[offa] = fma(real, w[offw + REAL], imag * w[offw + IMAG]);
                a[offa + IMAG] = fma(-real, w[offw + IMAG], imag * w[offw + REAL]);
                offw += COMPLEX_SIZE;
            }
        }

        /**
         * Multiplies the elements of an FFT vector by weights.
         * Doing this makes a regular FFT convolution a right-angle convolution.
         */
        void applyWeights(ComplexVector weights) {
            // The following code is the same as:
            //    for (int i=0;i<length;i++) this.multiply(i,weights,i);
            // We use the fact that all a.imag(i) = 0.0
            int offw = weights.offset;
            double[] w = weights.a;
            int end = offset + length << 1;
            for (int offa = offset; offa < end; offa += COMPLEX_SIZE) {
                double real = a[offa + REAL];
                a[offa + REAL] = real * w[offw + REAL];
                a[offa + IMAG] = real * w[offw + IMAG];
                offw += COMPLEX_SIZE;
            }
        }

        void copyInto(int idxa, MutableComplex destination) {
            destination.real = a[realIdx(idxa)];
            destination.imag = a[imagIdx(idxa)];
        }

        double imag(int idxa) {
            return a[(idxa << COMPLEX_SIZE_SHIFT) + offset + IMAG];
        }

        void imag(int idxa, double value) {
            a[(idxa << COMPLEX_SIZE_SHIFT) + offset + IMAG] = value;
        }

        private int imagIdx(int idxa) {
            return (idxa << COMPLEX_SIZE_SHIFT) + offset + IMAG;
        }

        /**
         * Multiplies {@code a} by {@code c}.
         * Stores the result in a[idx] and a[idx+1].
         */
        void multiply(int idxa, MutableComplex c) {
            int ri = realIdx(idxa);
            int ii = imagIdx(idxa);
            double real = a[ri];
            double imag = a[ii];
            a[ri] = fma(real, c.real, -imag * c.imag);
            a[ii] = fma(real, c.imag, +imag * c.real);
        }

        /**
         * Multiplies {@code a} by {@code c} and by {@code i}.
         * Stores the result in a[idx] and a[idx+1].
         */
        void multiplyByIAnd(int idxa, MutableComplex c) {
            int ri = realIdx(idxa);
            int ii = imagIdx(idxa);
            double real = a[ri];
            double imag = a[ii];
            a[ri] = fma(-real, c.imag, -imag * c.real);
            a[ii] = fma(real, c.real, -imag * c.imag);
        }

        /**
         * Multiplies {@code a} by the conjugate of {@code c}.
         * Stores the result in a[i] and a[i+1].
         */
        void multiplyConjugate(int idxa, MutableComplex c) {
            int ri = realIdx(idxa);
            int ii = imagIdx(idxa);
            double real = a[ri];
            double imag = a[ii];
            a[ri] = fma(real, c.real, +imag * c.imag);
            a[ii] = fma(-real, c.imag, +imag * c.real);
        }

        void multiplyConjugateInto(int idxa, MutableComplex c, MutableComplex destination) {
            double real = a[realIdx(idxa)];
            double imag = a[imagIdx(idxa)];
            destination.real = fma(real, c.real, +imag * c.imag);
            destination.imag = fma(-real, c.imag, +imag * c.real);
        }

        /**
         * Multiplies {@code (a[i].real,a[i+1].imaginary)} by the conjugate of {@code c}
         * and by {@code i}.
         * Stores the result in a[i] and a[i+1].
         */
        void multiplyConjugateTimesI(int idxa, MutableComplex c) {
            int ri = realIdx(idxa);
            int ii = imagIdx(idxa);
            double real = a[ri];
            double imag = a[ii];
            a[ri] = fma(-real, c.imag, +imag * c.real);
            a[ii] = fma(-real, c.real, -imag * c.imag);
        }

        void multiplyInto(int idxa, MutableComplex c, MutableComplex destination) {
            double real = a[realIdx(idxa)];
            double imag = a[imagIdx(idxa)];
            destination.real = fma(real, c.real, -imag * c.imag);
            destination.imag = fma(real, c.imag, +imag * c.real);
        }

        void multiplyPointwise(ComplexVector cvec) {
            // The following code is the same as:
            //    for (int i=0;i<length;i++) this.multiply(i,cvec,i);
            int offc = cvec.offset;
            double[] c = cvec.a;
            int end = offset + length << 1;
            for (int offa = offset; offa < end; offa += COMPLEX_SIZE) {
                // The following code is the same as: this.multiply(i,cvec[i]);
                double real = a[offa + REAL];
                double imag = a[offa + IMAG];
                double creal = c[offc + REAL];
                double cimag = c[offc + IMAG];
                a[offa + REAL] = fma(real, creal, -imag * cimag);
                a[offa + IMAG] = fma(real, cimag, +imag * creal);
                offc += COMPLEX_SIZE;
            }
        }

        double part(int idxa, int part) {
            return a[(idxa << COMPLEX_SIZE_SHIFT) + part];
        }

        double real(int idxa) {
            return a[(idxa << COMPLEX_SIZE_SHIFT) + offset];
        }

        void real(int idxa, double value) {
            a[(idxa << COMPLEX_SIZE_SHIFT) + offset] = value;
        }

        private int realIdx(int idxa) {
            return (idxa << COMPLEX_SIZE_SHIFT) + offset;
        }

        void set(int idxa, double real, double imag) {
            int idx = realIdx(idxa);
            a[idx] = real;
            a[idx + IMAG] = imag;
        }

        /**
         * The result is placed in the argument
         */
        void squarePointwise() {
            // The following code is the same as:
            //    for (int i=0;i<length;i++) this.square(i);
            int end = offset + length << 1;
            for (int offa = offset; offa < end; offa += COMPLEX_SIZE) {
                double real = a[offa + REAL];
                double imag = a[offa + IMAG];
                a[offa + REAL] = fma(real, real, -imag * imag);
                a[offa + IMAG] = SQUARE_CROSS_FACTOR * real * imag;
            }
        }

        void subtractInto(int idxa, ComplexVector c, int idxc, MutableComplex destination) {
            destination.real = a[realIdx(idxa)] - c.real(idxc);
            destination.imag = a[imagIdx(idxa)] - c.imag(idxc);
        }

        void subtractTimesIInto(int idxa, ComplexVector c, int idxc, MutableComplex destination) {
            destination.real = a[realIdx(idxa)] + c.imag(idxc);
            destination.imag = a[imagIdx(idxa)] - c.real(idxc);
        }

        void timesTwoToThe(int idxa, int n) {
            int ri = realIdx(idxa);
            int ii = imagIdx(idxa);
            double real = a[ri];
            double imag = a[ii];
            a[ri] = fastScalb(real, n);
            a[ii] = fastScalb(imag, n);
        }
    }

    final static class MutableComplex {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** Factor of the cross term: (a + bi)^2 = a^2 - b^2 + 2abi. */
        private static final int SQUARE_CROSS_FACTOR = 2;

        double real, imag;

        MutableComplex() {
        }

        void add(MutableComplex c) {
            real += c.real;
            imag += c.imag;
        }

        void add(ComplexVector c, int idxc) {
            real += c.real(idxc);
            imag += c.imag(idxc);
        }

        /**
         * Adds c to this number and stores the result in destination.
         * Leaves this number unmodified.
         */
        void addInto(MutableComplex c, MutableComplex destination) {
            destination.real = real + c.real;
            destination.imag = imag + c.imag;
        }

        /**
         * Adds c*i to this number.
         */
        void addTimesI(MutableComplex c) {
            real -= c.imag;
            imag += c.real;
        }

        void addTimesI(ComplexVector c, int idxc) {
            real -= c.imag(idxc);
            imag += c.real(idxc);
        }

        /**
         * Adds c*i to this number and stores the result in destination.
         * Leaves this number unmodified.
         */
        void addTimesIInto(MutableComplex c, MutableComplex destination) {
            destination.real = real - c.imag;
            destination.imag = imag + c.real;
        }

        void copyInto(ComplexVector c, int idxc) {
            c.real(idxc, real);
            c.imag(idxc, imag);
        }

        void multiply(MutableComplex c) {
            double temp = real;
            real = fma(temp, c.real, -imag * c.imag);
            imag = fma(temp, c.imag, imag * c.real);
        }

        /**
         * multiplies this number by the conjugate of c.
         */
        void multiplyConjugate(MutableComplex c) {
            double temp = real;
            real = fma(temp, c.real, imag * c.imag);
            imag = fma(-temp, c.imag, imag * c.real);
        }

        void set(ComplexVector c, int idxc) {
            real = c.real(idxc);
            imag = c.imag(idxc);
        }

        void squareInto(MutableComplex destination) {
            destination.real = fma(real, real, -imag * imag);
            destination.imag = SQUARE_CROSS_FACTOR * real * imag;
        }

        void subtract(MutableComplex c) {
            real -= c.real;
            imag -= c.imag;
        }

        void subtract(ComplexVector c, int idxc) {
            real -= c.real(idxc);
            imag -= c.imag(idxc);
        }

        void subtractInto(MutableComplex c, MutableComplex destination) {
            destination.real = real - c.real;
            destination.imag = imag - c.imag;
        }

        void subtractInto(MutableComplex c, ComplexVector destination, int idxd) {
            destination.real(idxd, real - c.real);
            destination.imag(idxd, imag - c.imag);
        }

        void subtractTimesI(MutableComplex c) {
            real += c.imag;
            imag -= c.real;
        }

        void subtractTimesI(ComplexVector c, int idxc) {
            real += c.imag(idxc);
            imag -= c.real(idxc);
        }

        void subtractTimesIInto(MutableComplex c, MutableComplex destination) {
            destination.real = real + c.imag;
            destination.imag = imag - c.real;
        }
    }
}
