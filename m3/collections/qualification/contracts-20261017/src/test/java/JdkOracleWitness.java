/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import java.util.TreeSet;

/** Independent JDK observations for two retained failing collection fixtures. */
public final class JdkOracleWitness {
    public static void main(String[] args) {
        TreeSet<Long> survivors = new TreeSet<>();
        for (long i = 1; i <= 100_000; i++) {
            survivors.add(i * 7);
            survivors.add(-i * 7);
        }
        survivors.removeIf(v -> (Math.abs(v) / 7) % 3 != 0);
        if (survivors.size() != 66_666 || survivors.contains(0L)
                || survivors.higher(-21L) != 21L) throw new AssertionError();
        TreeSet<Long> seeded = new TreeSet<>();
        for (long i = -200; i <= 200; i += 3) seeded.add(i);
        var view = seeded.subSet(-50L, true, 50L, false);
        if (view.add(7L) || !view.add(8L)) throw new AssertionError();
        System.out.println("JDK_ORACLE_PASS retained=66666 absentZero=true seam=-21,+21 duplicate7=false new8=true");
    }
}
