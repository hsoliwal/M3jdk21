// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

/**
 * Census pass-2 C3 before/after differential of M3LaneAlgorithms (leaf P2-075): seeded rotateByCycles (int and
 * long lanes, any distance including Integer.MIN_VALUE/MAX_VALUE) and compact (int and long lanes, aligned keep
 * masks, bounded windows) over the master and the edited classes; plus rotate (the reversal rotation) as the
 * independent reference for every rotateByCycles call. The two hunks are behaviour-preserving for every array a
 * JVM can allocate below 2^30 elements, so the transcripts must be identical.
 * usage: java CensusLaneDifferential OPS   (prints one digest per operation family and an overall digest)
 */
public final class CensusLaneDifferential {
    private static final long SEED = 0x43335032303735L; // "C3P2075"

    private CensusLaneDifferential() { }

    public static void main(String[] args) throws Exception {
        int ops = Integer.parseInt(args[0]);
        Random random = new Random(SEED);
        MessageDigest all = MessageDigest.getInstance("SHA-256");
        long referenceMismatches = 0, compactions = 0, rotations = 0;
        for (int op = 0; op < ops; op++) {
            int length = random.nextInt(4) == 0 ? random.nextInt(4097) : random.nextInt(97);
            int from = random.nextInt(length + 1), to = from + random.nextInt(length - from + 1);
            int distance = switch (random.nextInt(6)) {
                case 0 -> Integer.MIN_VALUE;
                case 1 -> Integer.MAX_VALUE;
                case 2 -> 0;
                case 3 -> random.nextInt(length + 1) - length / 2;
                default -> random.nextInt();
            };
            if ((op & 1) == 0) {
                int[] a = new int[length];
                long[] b = new long[length];
                for (int i = 0; i < length; i++) { a[i] = random.nextInt(); b[i] = random.nextLong(); }
                int[] ra = a.clone();
                long[] rb = b.clone();
                M3LaneAlgorithms.rotateByCycles(a, from, to, distance);
                M3LaneAlgorithms.rotateByCycles(b, from, to, distance);
                M3LaneAlgorithms.rotate(ra, from, to, distance);
                M3LaneAlgorithms.rotate(rb, from, to, distance);
                if (!Arrays.equals(a, ra) || !Arrays.equals(b, rb)) referenceMismatches++;
                all.update(("R" + op + Arrays.hashCode(a) + ":" + Arrays.hashCode(b)).getBytes());
                rotations++;
            } else {
                int[] a = new int[length];
                long[] b = new long[length];
                long[] keep = new long[(length + 63) >>> 6];
                for (int i = 0; i < length; i++) { a[i] = random.nextInt(100); b[i] = random.nextLong(); }
                double density = random.nextDouble();
                for (int i = 0; i < length; i++) if (random.nextDouble() < density) keep[i >>> 6] |= 1L << (i & 63);
                long[] keepB = keep.clone();
                int wa = M3LaneAlgorithms.compact(a, keep, from, to);
                int wb = M3LaneAlgorithms.compact(b, keepB, from, to);
                all.update(("C" + op + ":" + wa + ":" + wb + ":" + Arrays.hashCode(a) + ":" + Arrays.hashCode(b)
                        + ":" + Arrays.hashCode(keep) + ":" + Arrays.hashCode(keepB)).getBytes());
                compactions++;
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte x : all.digest()) hex.append(String.format("%02x", x));
        System.out.println("LANE_DIFFERENTIAL ops=" + ops + " rotations=" + rotations + " compactions=" + compactions
                + " rotate_reference_mismatches=" + referenceMismatches + " sha256=" + hex);
    }
}
