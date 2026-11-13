// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import com.fasterxml.jackson.core.io.schubfach.DoubleToDecimal;
import com.fasterxml.jackson.core.io.schubfach.FloatToDecimal;
import java.util.SplittableRandom;

/** Jackson's Schubfach copy vs the JDK's Double.toString/Float.toString: random raw bits plus edge values. */
public final class SchubfachVsJdk {
    public static void main(String[] args) {
        long n = Long.parseLong(args[0]);
        SplittableRandom r = new SplittableRandom(Long.parseLong(args[1]));
        long diffs = 0;
        double[] edges = {0.0, -0.0, Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL, 1e7, 1e-3, 9.999999999999999e22,
            1e23, 2e-323, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.1, 1.0E-5, 123456789.0};
        for (double d : edges) diffs += check(d);
        for (long i = 0; i < n; i++) {
            diffs += check(Double.longBitsToDouble(r.nextLong()));
            diffs += checkF(Float.intBitsToFloat(r.nextInt()));
            diffs += check((double) r.nextInt(1_000_000_000) / Math.pow(10, r.nextInt(25)));
        }
        System.out.println("samples=" + (3 * n + edges.length) + " diffs=" + diffs);
    }
    static int check(double d) {
        String a = Double.toString(d), b = DoubleToDecimal.toString(d);
        if (a.equals(b)) return 0;
        System.out.println("DOUBLE " + Long.toHexString(Double.doubleToRawLongBits(d)) + " jdk=" + a + " jackson=" + b);
        return 1;
    }
    static int checkF(float f) {
        String a = Float.toString(f), b = FloatToDecimal.toString(f);
        if (a.equals(b)) return 0;
        System.out.println("FLOAT " + Integer.toHexString(Float.floatToRawIntBits(f)) + " jdk=" + a + " jackson=" + b);
        return 1;
    }
}
