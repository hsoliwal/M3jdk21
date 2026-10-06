// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Frozen finite-domain regex result image over canonical M3 String IDs.
 *
 * <p>Synexia uses RE2/J as an external bounded regex lane and retains compiled finite-domain
 * results over canonical values. {@code java.base} deliberately has no RE2/J dependency: this
 * internal adaptation uses JDK {@link Pattern} as the exact verifier, evaluates each canonical
 * coordinate once, and retains only primitive result geometry.</p>
 *
 * <p>The backing remains the text authority. This image retains no String, char array, byte array,
 * matcher, pattern, or copied spelling.</p>
 */
public final class M3RegexDomainIndex {
    public enum Mode {
        MATCHES,
        FIND,
        LOOKING_AT
    }

    public record Budget(int maxValues, long maxUtf16Units, long maxRetainedBytes) {
        public static final Budget DEFAULT =
                new Budget(65_536, 16L * 1024L * 1024L, 16L * 1024L * 1024L);

        public Budget {
            if (maxValues < 0 || maxUtf16Units < 0L || maxRetainedBytes < 0L) {
                throw new IllegalArgumentException("regex-domain budget must be nonnegative");
            }
        }
    }

    private final M3StringBacking backing;
    private final long[] ids;
    private final long[] matchesBits;
    private final long[] findBits;
    private final long[] lookingAtBits;
    private final int[] firstFindStarts;
    private final int[] firstFindEnds;
    private final int patternHash;
    private final int patternFlags;

    private M3RegexDomainIndex(
            M3StringBacking backing,
            long[] ids,
            long[] matchesBits,
            long[] findBits,
            long[] lookingAtBits,
            int[] firstFindStarts,
            int[] firstFindEnds,
            int patternHash,
            int patternFlags) {
        this.backing = Objects.requireNonNull(backing, "backing");
        this.ids = ids;
        this.matchesBits = matchesBits;
        this.findBits = findBits;
        this.lookingAtBits = lookingAtBits;
        this.firstFindStarts = firstFindStarts;
        this.firstFindEnds = firstFindEnds;
        this.patternHash = patternHash;
        this.patternFlags = patternFlags;
    }

    public static M3RegexDomainIndex compile(
            M3StringBacking backing, long[] ids, String expression) {
        return compile(backing, ids, expression, 0, Budget.DEFAULT);
    }

    public static M3RegexDomainIndex compile(
            M3StringBacking backing,
            long[] inputIds,
            String expression,
            int flags,
            Budget budget) {
        Objects.requireNonNull(backing, "backing");
        Objects.requireNonNull(inputIds, "inputIds");
        Objects.requireNonNull(expression, "expression");
        Budget checkedBudget = Objects.requireNonNull(budget, "budget");

        long[] ids = canonicalIds(inputIds);
        if (ids.length > checkedBudget.maxValues()) {
            throw new IllegalArgumentException("regex-domain value budget exceeded");
        }

        long totalUnits = 0L;
        for (long id : ids) {
            totalUnits = Math.addExact(totalUnits, backing.length(id));
            if (totalUnits > checkedBudget.maxUtf16Units()) {
                throw new IllegalArgumentException("regex-domain UTF-16 budget exceeded");
            }
        }

        int words = (ids.length + 63) >>> 6;
        long retained =
                Math.addExact(
                        Math.multiplyExact((long) ids.length, Long.BYTES + 2L * Integer.BYTES),
                        Math.multiplyExact((long) words * 3L, Long.BYTES));
        if (retained > checkedBudget.maxRetainedBytes()) {
            throw new IllegalArgumentException("regex-domain retained-byte budget exceeded");
        }

        Pattern pattern = Pattern.compile(expression, flags);
        long[] matches = new long[words];
        long[] find = new long[words];
        long[] lookingAt = new long[words];
        int[] starts = new int[ids.length];
        int[] ends = new int[ids.length];
        Arrays.fill(starts, -1);
        Arrays.fill(ends, -1);

        for (int row = 0; row < ids.length; row++) {
            BackingSequence sequence = new BackingSequence(backing, ids[row], 0, backing.length(ids[row]));
            Matcher matcher = pattern.matcher(sequence);
            if (matcher.matches()) set(matches, row);

            matcher.reset();
            if (matcher.lookingAt()) set(lookingAt, row);

            matcher.reset();
            if (matcher.find()) {
                set(find, row);
                starts[row] = matcher.start();
                ends[row] = matcher.end();
            }
        }

        return new M3RegexDomainIndex(
                backing,
                ids,
                matches,
                find,
                lookingAt,
                starts,
                ends,
                expression.hashCode(),
                flags);
    }

    public M3StringBacking backing() {
        return backing;
    }

    public int size() {
        return ids.length;
    }

    public int patternHash() {
        return patternHash;
    }

    public int patternFlags() {
        return patternFlags;
    }

    public boolean test(long id, Mode mode) {
        int row = row(id);
        if (row < 0) return false;
        return get(bits(Objects.requireNonNull(mode, "mode")), row);
    }

    public boolean matches(long id) {
        return test(id, Mode.MATCHES);
    }

    public boolean find(long id) {
        return test(id, Mode.FIND);
    }

    public boolean lookingAt(long id) {
        return test(id, Mode.LOOKING_AT);
    }

    /** First precomputed find span, or {@code -1} when the ID has no find match. */
    public int firstFindStart(long id) {
        int row = row(id);
        return row < 0 ? -1 : firstFindStarts[row];
    }

    /** First precomputed find end, or {@code -1} when the ID has no find match. */
    public int firstFindEnd(long id) {
        int row = row(id);
        return row < 0 ? -1 : firstFindEnds[row];
    }

    public long[] ids(Mode mode) {
        long[] selected = new long[ids.length];
        int count = 0;
        long[] bits = bits(Objects.requireNonNull(mode, "mode"));
        for (int word = 0; word < bits.length; word++) {
            long value = bits[word];
            while (value != 0L) {
                int bit = Long.numberOfTrailingZeros(value);
                int row = (word << 6) + bit;
                if (row < ids.length) selected[count++] = ids[row];
                value &= value - 1L;
            }
        }
        return Arrays.copyOf(selected, count);
    }

    public long retainedPrimitiveBytes() {
        return (long) ids.length * Long.BYTES
                + (long) (matchesBits.length + findBits.length + lookingAtBits.length) * Long.BYTES
                + (long) (firstFindStarts.length + firstFindEnds.length) * Integer.BYTES;
    }

    private long[] bits(Mode mode) {
        return switch (mode) {
            case MATCHES -> matchesBits;
            case FIND -> findBits;
            case LOOKING_AT -> lookingAtBits;
        };
    }

    private int row(long id) {
        int low = 0;
        int high = ids.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int compared = Long.compareUnsigned(ids[middle], id);
            if (compared < 0) low = middle + 1;
            else if (compared > 0) high = middle - 1;
            else return middle;
        }
        return -1;
    }

    private static long[] canonicalIds(long[] input) {
        Long[] boxed = new Long[input.length];
        for (int index = 0; index < input.length; index++) {
            long id = input[index];
            if (id == 0L) throw new IllegalArgumentException("M3 String ID must be nonzero");
            boxed[index] = id;
        }
        Arrays.sort(boxed, Long::compareUnsigned);
        long[] output = new long[boxed.length];
        int count = 0;
        for (Long boxedId : boxed) {
            long id = boxedId;
            if (count == 0 || output[count - 1] != id) output[count++] = id;
        }
        return Arrays.copyOf(output, count);
    }

    private static boolean get(long[] bits, int row) {
        return (bits[row >>> 6] & (1L << (row & 63))) != 0L;
    }

    private static void set(long[] bits, int row) {
        bits[row >>> 6] |= 1L << (row & 63);
    }

    /**
     * Zero-copy CharSequence over one canonical backing coordinate.
     *
     * <p>Subsequences are coordinates into the same backing; they never materialize Strings.</p>
     */
    private static final class BackingSequence implements CharSequence {
        private final M3StringBacking backing;
        private final long id;
        private final int start;
        private final int length;

        BackingSequence(M3StringBacking backing, long id, int start, int length) {
            this.backing = backing;
            this.id = id;
            this.start = start;
            this.length = length;
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public char charAt(int index) {
            Objects.checkIndex(index, length);
            return backing.charAt(id, start + index);
        }

        @Override
        public CharSequence subSequence(int from, int to) {
            Objects.checkFromToIndex(from, to, length);
            return new BackingSequence(backing, id, start + from, to - from);
        }

        @Override
        public String toString() {
            return backing.utf16View(id)
                    .subSequence(start, start + length)
                    .toString();
        }
    }
}
