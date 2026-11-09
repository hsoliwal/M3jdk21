// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.SplittableRandom;

/**
 * NUMBER_PARSE_ABSORB v2 probe. "accept" counts the literals of BigParseDifferential's corpus that the fast parser
 * (jdk.internal.math.M3BigNumbers, needs --add-exports java.base/jdk.internal.math=ALL-UNNAMED) accepts. "bench" times
 * new BigInteger(s) and new BigDecimal(s) for decimal literals of the given digit counts: the median of 9 rounds, in
 * nanoseconds (median of 2001 rounds up to 10,000 digits, else 9). Run it on the stock JDK (bench only) and with the patched java.base to compare.
 */
public final class BigParseProbe {
    public static void main(String[] args) throws Exception {
        if (args[0].equals("accept")) {
            Class<?> c = Class.forName("jdk.internal.math.M3BigNumbers");
            var bi = c.getMethod("decimalBigIntegerOrNull", String.class);
            var bd = c.getMethod("bigDecimalOrNull", char[].class, int.class, int.class);
            SplittableRandom r = new SplittableRandom(1);
            int total = 0, integers = 0, decimals = 0;
            for (int i = 0; i < 20000; i++) {
                StringBuilder b = new StringBuilder();
                int n = 1 + (int) Math.exp(r.nextDouble() * Math.log(5000));
                for (int k = 0; k < n; k++) b.append((char) ('0' + r.nextInt(10)));
                String integer = b.toString();
                String decimal = integer + "." + integer.substring(0, Math.min(integer.length(), 40)) + "e-7";
                total++;
                if (bi.invoke(null, integer) != null) integers++;
                if (bd.invoke(null, decimal.toCharArray(), 0, decimal.length()) != null) decimals++;
            }
            System.out.println("accept literals=" + total + " BigInteger=" + integers + " BigDecimal=" + decimals);
            return;
        }
        for (String arg : args[1].split(",")) {
            int n = Integer.parseInt(arg);
            SplittableRandom r = new SplittableRandom(n);
            StringBuilder b = new StringBuilder(n);
            for (int k = 0; k < n; k++) b.append((char) ('0' + (k == 0 ? 1 + r.nextInt(9) : r.nextInt(10))));
            String s = b.toString();
            String d = s.substring(0, n / 2) + "." + s.substring(n / 2);
            int rounds = n <= 10_000 ? 2001 : 9;
            long[] ti = new long[rounds], td = new long[rounds];
            long sink = 0;
            for (int round = 0; round < rounds; round++) {
                long t0 = System.nanoTime();
                sink += new BigInteger(s).bitLength();
                long t1 = System.nanoTime();
                sink += new BigDecimal(d).scale();
                long t2 = System.nanoTime();
                ti[round] = t1 - t0;
                td[round] = t2 - t1;
            }
            java.util.Arrays.sort(ti);
            java.util.Arrays.sort(td);
            System.out.println("bench digits=" + n + " BigInteger_ns=" + ti[rounds / 2] + " BigDecimal_ns=" + td[rounds / 2]
                    + " sink=" + (sink & 1));
        }
    }
}
