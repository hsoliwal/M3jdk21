/*******************************************************************************
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
package jdk.m3.collections;

import java.util.Arrays;

/**
 * Approximate distinct-long cardinality using one byte register per bucket.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/LongHyperLogLog.java}
 * blob {@code c6a01bdae6c09e47eb26051e4a9b4948eb325140} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3LongHyperLogLog {
    private final byte[] registers;
    private final int precision;
    private final int mask;
    /** Empty byte registers have an exact cardinality estimate of zero. */
    private long cachedEstimate;
    private boolean estimateKnown = true;

    public M3LongHyperLogLog(int precision) {
        if (precision < 4 || precision > 18) {
            throw new IllegalArgumentException("precision must be 4..18");
        }
        this.precision = precision;
        registers = new byte[1 << precision];
        mask = registers.length - 1;
    }

    public int precision() { return precision; }
    public int registerCount() { return registers.length; }
    public long payloadBytes() { return registers.length; }

    public boolean add(long value) {
        long hash = M3LongBloomFilter.mix64(value);
        int index = (int) (hash >>> (64 - precision)) & mask;
        long remaining = (hash << precision) | (1L << (precision - 1));
        int rank = Long.numberOfLeadingZeros(remaining) + 1;
        int current = Byte.toUnsignedInt(registers[index]);
        if (rank <= current) return false;
        registers[index] = (byte) rank;
        estimateKnown = false;
        return true;
    }

    public long estimate() {
        if (estimateKnown) return cachedEstimate;
        int m = registers.length;
        double sum = 0.0d;
        int zeros = 0;
        for (byte register : registers) {
            int value = Byte.toUnsignedInt(register);
            sum += Math.scalb(1.0d, -value);
            if (value == 0) zeros++;
        }
        double estimate = alpha(m) * m * (double) m / sum;
        if (estimate <= 2.5d * m && zeros != 0) {
            estimate = m * Math.log(m / (double) zeros);
        }
        long result = estimate >= Long.MAX_VALUE ? Long.MAX_VALUE
                : Math.max(0L, Math.round(estimate));
        cachedEstimate = result;
        estimateKnown = true;
        return result;
    }

    public void merge(M3LongHyperLogLog other) {
        if (other.precision != precision) throw new IllegalArgumentException("precision mismatch");
        boolean changed = false;
        for (int i = 0; i < registers.length; i++) {
            if (Byte.toUnsignedInt(other.registers[i]) > Byte.toUnsignedInt(registers[i])) {
                registers[i] = other.registers[i];
                changed = true;
            }
        }
        if (changed) estimateKnown = false;
    }

    public void clear() {
        Arrays.fill(registers, (byte) 0);
        cachedEstimate = 0L;
        estimateKnown = true;
    }

    private static double alpha(int m) {
        return switch (m) {
            case 16 -> 0.673d;
            case 32 -> 0.697d;
            case 64 -> 0.709d;
            default -> 0.7213d / (1.0d + 1.079d / m);
        };
    }
}
