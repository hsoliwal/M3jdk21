// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable normalized Q32 probability mass over canonical M3 coordinates.
 *
 * <p>This is one concrete M3 implementation of a normalized primitive probability vector.
 * It does not claim ownership of probability or probabilistic reasoning as a domain.</p>
 */
public final class M3ProbabilityVector {
    public static final long ONE_Q32 = 1L << 32;

    private final long[] keys;
    private final long[] massQ32;
    private final long[] cumulativeQ32;

    private M3ProbabilityVector(long[] keys, long[] massQ32) {
        this.keys = keys;
        this.massQ32 = massQ32;
        this.cumulativeQ32 = new long[massQ32.length];
        long cumulative = 0L;
        for (int index = 0; index < massQ32.length; index++) {
            cumulative = Math.addExact(cumulative, massQ32[index]);
            cumulativeQ32[index] = cumulative;
        }
        if (keys.length != 0 && cumulative != ONE_Q32) {
            throw new IllegalArgumentException("probability mass must sum exactly to Q32 one");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static M3ProbabilityVector uniform(long... keys) {
        Builder builder = builder();
        for (long key : Objects.requireNonNull(keys, "keys")) {
            builder.weight(key, 1L);
        }
        return builder.build();
    }

    public int size() {
        return keys.length;
    }

    public boolean isEmpty() {
        return keys.length == 0;
    }

    public long keyAt(int index) {
        return keys[Objects.checkIndex(index, keys.length)];
    }

    public long massQ32At(int index) {
        return massQ32[Objects.checkIndex(index, massQ32.length)];
    }

    public long massQ32(long key) {
        int row = row(key);
        return row < 0 ? 0L : massQ32[row];
    }

    public double probability(long key) {
        return massQ32(key) / (double) ONE_Q32;
    }

    public boolean isNormalized() {
        return keys.length == 0 || cumulativeQ32[cumulativeQ32.length - 1] == ONE_Q32;
    }

    public long mostProbableKey() {
        if (keys.length == 0) throw new IllegalStateException("empty probability vector");
        int best = 0;
        for (int index = 1; index < keys.length; index++) {
            if (massQ32[index] > massQ32[best]
                    || (massQ32[index] == massQ32[best]
                            && Long.compareUnsigned(keys[index], keys[best]) < 0)) {
                best = index;
            }
        }
        return keys[best];
    }

    public long selectByUnitQ32(long unitQ32) {
        if (keys.length == 0) throw new IllegalStateException("empty probability vector");
        if (unitQ32 < 0L || unitQ32 >= ONE_Q32) {
            throw new IllegalArgumentException("unitQ32 outside [0,2^32)");
        }
        int low = 0;
        int high = cumulativeQ32.length - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (unitQ32 < cumulativeQ32[middle]) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return keys[low];
    }

    public long retainedPrimitiveBytes() {
        return (long) (keys.length + massQ32.length + cumulativeQ32.length) * Long.BYTES;
    }

    private int row(long key) {
        int low = 0;
        int high = keys.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int compared = Long.compareUnsigned(keys[middle], key);
            if (compared < 0) {
                low = middle + 1;
            } else if (compared > 0) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return -1;
    }

    private record Draft(long key, long weight) {}
    private record Remainder(int row, BigInteger value, long key) {}

    public static final class Builder {
        private final List<Draft> drafts = new ArrayList<>();

        public Builder weight(long key, long weight) {
            if (key == 0L) throw new IllegalArgumentException("probability key must be nonzero");
            if (weight < 0L) throw new IllegalArgumentException("weight must be nonnegative");
            drafts.add(new Draft(key, weight));
            return this;
        }

        public M3ProbabilityVector build() {
            if (drafts.isEmpty()) {
                return new M3ProbabilityVector(new long[0], new long[0]);
            }

            ArrayList<Draft> ordered = new ArrayList<>(drafts);
            ordered.sort((left, right) -> Long.compareUnsigned(left.key(), right.key()));

            BigInteger total = BigInteger.ZERO;
            for (int index = 0; index < ordered.size(); index++) {
                Draft draft = ordered.get(index);
                if (index > 0 && ordered.get(index - 1).key() == draft.key()) {
                    throw new IllegalArgumentException("duplicate probability key");
                }
                total = total.add(BigInteger.valueOf(draft.weight()));
            }
            if (total.signum() == 0) {
                throw new IllegalArgumentException("at least one weight must be positive");
            }

            BigInteger one = BigInteger.valueOf(ONE_Q32);
            long[] keys = new long[ordered.size()];
            long[] mass = new long[ordered.size()];
            ArrayList<Remainder> remainders = new ArrayList<>(ordered.size());

            long assigned = 0L;
            for (int row = 0; row < ordered.size(); row++) {
                Draft draft = ordered.get(row);
                keys[row] = draft.key();
                BigInteger scaled = BigInteger.valueOf(draft.weight()).multiply(one);
                BigInteger[] div = scaled.divideAndRemainder(total);
                mass[row] = div[0].longValueExact();
                assigned = Math.addExact(assigned, mass[row]);
                remainders.add(new Remainder(row, div[1], draft.key()));
            }

            long leftover = ONE_Q32 - assigned;
            remainders.sort(
                    (left, right) -> {
                        int compared = right.value().compareTo(left.value());
                        return compared != 0
                                ? compared
                                : Long.compareUnsigned(left.key(), right.key());
                    });
            if (leftover > remainders.size()) {
                throw new AssertionError("Q32 normalization remainder invariant");
            }
            for (int index = 0; index < (int) leftover; index++) {
                int row = remainders.get(index).row();
                mass[row] = Math.addExact(mass[row], 1L);
            }

            return new M3ProbabilityVector(keys, mass);
        }
    }
}
