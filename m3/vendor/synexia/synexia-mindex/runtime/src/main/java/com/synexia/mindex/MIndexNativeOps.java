// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Optional JNI accelerator for exact operations over packed MIndex atom and content-ID arrays.
 *
 * <p>The Java implementation is canonical. Native code is only selected when
 * the shared library is available (or required explicitly by system property).</p>
 */
public final class MIndexNativeOps {
    public static final String MODE_PROPERTY = "synexia.mindex.native";
    private static final String LIBRARY = "synexia_mindex";

    private static final long HASH_SEED = 0x4D494E4441544F4DL;
    private static final long HASH_P1 = 0x9E3779B185EBCA87L;
    private static final long HASH_P2 = 0xC2B2AE3D27D4EB4FL;
    private static final long HASH_P3 = 0x165667B19E3779F9L;
    private static final long AVALANCHE_1 = 0xff51afd7ed558ccdL;
    private static final long AVALANCHE_2 = 0xc4ceb9fe1a85ec53L;

    private static final NativeState STATE = load();

    private record NativeState(boolean available, String mode, String detail) {}

    private MIndexNativeOps() {}

    public static boolean available() {
        return STATE.available();
    }

    public static String mode() {
        return STATE.mode();
    }

    public static String status() {
        return STATE.available() ? "loaded:" + LIBRARY : STATE.detail();
    }

    /**
     * Find a byte pattern in immutable pooled or mapped UTF-8 slices without joining their payloads.
     * The result is an absolute byte offset in {@code haystack}, not a UTF-16 character offset.
     * Only genuine direct segments enter JNI in explicit {@code required} mode. Auto mode and
     * heap-backed pool segments use the exact Java KMP path until a measured admission policy exists.
     */
    public static int indexOfBytes(MIndexByteSequence haystack, byte[] needle) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        if (needle.length == 0) return 0;
        if (needle.length > haystack.length()) return -1;
        if (STATE.available() && "required".equals(STATE.mode())) {
            for (int index = 0; index < haystack.segmentCount(); index++) {
                if (!haystack.segment(index).isDirect()) {
                    return referenceIndexOfBytes(haystack, needle);
                }
            }
            ByteBuffer[] segments = new ByteBuffer[haystack.segmentCount()];
            for (int index = 0; index < segments.length; index++) {
                segments[index] = haystack.segment(index).slice().asReadOnlyBuffer();
            }
            try {
                int result = nativeIndexOfDirectSegments(segments, needle);
                if (result != -2) return result;
                if ("required".equals(STATE.mode())) {
                    throw new IllegalStateException("JNI direct buffers unavailable");
                }
            } catch (UnsatisfiedLinkError missingNewSymbol) {
                if ("required".equals(STATE.mode())) throw missingNewSymbol;
            }
        }
        return referenceIndexOfBytes(haystack, needle);
    }

    static int indexOfBytes(
            byte[] haystack,
            int haystackStart,
            int haystackEnd,
            byte[] needle,
            int needleStart,
            int needleEnd) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        Objects.checkFromToIndex(haystackStart, haystackEnd, haystack.length);
        Objects.checkFromToIndex(needleStart, needleEnd, needle.length);
        if (needleStart == needleEnd) return 0;
        if (needleEnd - needleStart > haystackEnd - haystackStart) return -1;
        if (!STATE.available()) {
            return referenceIndexOfBytes(
                    haystack, haystackStart, haystackEnd, needle, needleStart, needleEnd);
        }
        try {
            return nativeIndexOfBytes(
                    haystack, haystackStart, haystackEnd, needle, needleStart, needleEnd);
        } catch (UnsatisfiedLinkError missingNewSymbol) {
            if ("required".equals(STATE.mode())) throw missingNewSymbol;
            return referenceIndexOfBytes(
                    haystack, haystackStart, haystackEnd, needle, needleStart, needleEnd);
        }
    }

    public static boolean contentEquals(long[] left, long[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length != right.length) return false;
        return STATE.available() ? nativeEquals(left, right) : Arrays.equals(left, right);
    }

    public static boolean contentEquals(int[] left, int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (left.length != right.length) return false;
        return STATE.available() ? nativeEqualsInts(left, right) : Arrays.equals(left, right);
    }

    public static int compare(long[] left, long[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return STATE.available() ? nativeCompare(left, right) : referenceCompare(left, right);
    }

    public static int compare(int[] left, int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return STATE.available() ? nativeCompareInts(left, right) : referenceCompare(left, right);
    }

    public static int commonPrefix(long[] left, long[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return STATE.available() ? nativeCommonPrefix(left, right) : referenceCommonPrefix(left, right);
    }

    public static int commonPrefix(int[] left, int[] right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return STATE.available()
                ? nativeCommonPrefixInts(left, right)
                : referenceCommonPrefix(left, right);
    }

    public static long hash64(long[] values) {
        Objects.requireNonNull(values, "values");
        return STATE.available() ? nativeHash64(values) : referenceHash64(values);
    }

    public static long hash64(int[] values) {
        Objects.requireNonNull(values, "values");
        return STATE.available() ? nativeHash64Ints(values) : referenceHash64(values);
    }

    /**
     * Intersects two ascending signed-int lanes and emits each shared value once.
     *
     * <p>A zero limit means unbounded up to the shorter input. Inputs are expected to be sorted;
     * this matches the primitive posting contract used by MIndex indexes. The Java implementation
     * is the semantic oracle and fallback when JNI is unavailable.</p>
     */
    public static int[] intersectSorted(int[] left, int[] right, int limit) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        return intersectSortedRanges(
                left, 0, left.length, right, 0, right.length, limit);
    }

    /**
     * Intersects ascending signed-int slices and emits each shared value once.
     *
     * <p>This is the zero-copy posting primitive used by Jini/precompute indexes. Slice bounds are
     * validated before JNI; Java is the exact semantic oracle and fallback.</p>
     */
    public static int[] intersectSortedRanges(
            int[] left,
            int leftFrom,
            int leftTo,
            int[] right,
            int rightFrom,
            int rightTo,
            int limit) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.checkFromToIndex(leftFrom, leftTo, left.length);
        Objects.checkFromToIndex(rightFrom, rightTo, right.length);
        if (limit < 0) throw new IllegalArgumentException("limit must be >= 0");
        if (!STATE.available()) {
            return referenceIntersectSortedRanges(
                    left, leftFrom, leftTo, right, rightFrom, rightTo, limit);
        }
        try {
            return nativeIntersectSortedIntRanges(
                    left, leftFrom, leftTo, right, rightFrom, rightTo, limit);
        } catch (UnsatisfiedLinkError missingNewSymbol) {
            if ("required".equals(STATE.mode())) throw missingNewSymbol;
            return referenceIntersectSortedRanges(
                    left, leftFrom, leftTo, right, rightFrom, rightTo, limit);
        }
    }

    /**
     * Stable indexes of fingerprints within a Hamming radius, ordered by distance then input index.
     * Candidate discovery only; similarity never grants reuse or rewrite authority.
     */
    public static int[] hammingTopK(long[] fingerprints, long target, int maxDistance, int limit) {
        Objects.requireNonNull(fingerprints, "fingerprints");
        if (maxDistance < 0 || maxDistance > Long.SIZE) throw new IllegalArgumentException("maxDistance");
        if (limit < 1 || limit > 4096) throw new IllegalArgumentException("limit");
        if (!STATE.available()) return referenceHammingTopK(fingerprints, target, maxDistance, limit);
        try {
            return nativeHammingTopK(fingerprints, target, maxDistance, limit);
        } catch (UnsatisfiedLinkError missing) {
            if ("required".equals(STATE.mode())) throw missing;
            return referenceHammingTopK(fingerprints, target, maxDistance, limit);
        }
    }

    /**
     * Stable softmax over finite logits.
     *
     * <p>The Java implementation is the semantic oracle. JNI uses the same max-subtracted
     * algorithm when the optional MIndex native library is active.</p>
     */
    public static double[] softmax(double[] logits) {
        Objects.requireNonNull(logits, "logits");
        if (logits.length == 0) {
            throw new IllegalArgumentException("softmax requires at least one logit");
        }
        for (double value : logits) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("softmax logits must be finite");
            }
        }
        if (!STATE.available()) return referenceSoftmax(logits);
        try {
            return nativeSoftmax(logits);
        } catch (UnsatisfiedLinkError missingNewSymbol) {
            if ("required".equals(STATE.mode())) throw missingNewSymbol;
            return referenceSoftmax(logits);
        }
    }

    static int referenceIndexOfBytes(
            MIndexByteSequence haystack, byte[] needle) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        if (needle.length == 0) return 0;
        if (needle.length > haystack.length()) return -1;
        int[] failure = new int[needle.length];
        for (int index = 1, matched = 0; index < needle.length; index++) {
            while (matched > 0 && needle[index] != needle[matched]) matched = failure[matched - 1];
            if (needle[index] == needle[matched]) matched++;
            failure[index] = matched;
        }
        int absolute = 0;
        int matched = 0;
        for (int segment = 0; segment < haystack.segmentCount(); segment++) {
            ByteBuffer bytes = haystack.segment(segment).slice();
            for (int offset = 0; offset < bytes.remaining(); offset++, absolute++) {
                byte value = bytes.get(offset);
                while (matched > 0 && value != needle[matched]) matched = failure[matched - 1];
                if (value == needle[matched]) matched++;
                if (matched == needle.length) return absolute + 1 - matched;
            }
        }
        return -1;
    }

    static int referenceIndexOfBytes(
            byte[] haystack,
            int haystackStart,
            int haystackEnd,
            byte[] needle,
            int needleStart,
            int needleEnd) {
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(needle, "needle");
        Objects.checkFromToIndex(haystackStart, haystackEnd, haystack.length);
        Objects.checkFromToIndex(needleStart, needleEnd, needle.length);
        int needleLength = needleEnd - needleStart;
        if (needleLength == 0) return 0;
        int haystackLength = haystackEnd - haystackStart;
        if (needleLength > haystackLength) return -1;

        int last = haystackEnd - needleLength;
        for (int candidate = haystackStart; candidate <= last; candidate++) {
            if (haystack[candidate] != needle[needleStart]) continue;
            int matched = 1;
            while (matched < needleLength
                    && haystack[candidate + matched] == needle[needleStart + matched]) {
                matched++;
            }
            if (matched == needleLength) return candidate - haystackStart;
        }
        return -1;
    }

    static double[] referenceSoftmax(double[] logits) {
        double maximum = logits[0];
        for (int index = 1; index < logits.length; index++) {
            maximum = Math.max(maximum, logits[index]);
        }

        double[] result = new double[logits.length];
        double total = 0.0d;
        for (int index = 0; index < logits.length; index++) {
            double weight = Math.exp(logits[index] - maximum);
            result[index] = weight;
            total += weight;
        }
        if (!(total > 0.0d) || !Double.isFinite(total)) {
            throw new IllegalStateException("softmax normalization failed");
        }
        for (int index = 0; index < result.length; index++) {
            result[index] /= total;
        }
        return result;
    }

    static int referenceCompare(long[] left, long[] right) {
        int common = Math.min(left.length, right.length);
        for (int index = 0; index < common; index++) {
            int compared = Long.compareUnsigned(left[index], right[index]);
            if (compared != 0) return compared;
        }
        return Integer.compare(left.length, right.length);
    }

    static int referenceCompare(int[] left, int[] right) {
        int common = Math.min(left.length, right.length);
        for (int index = 0; index < common; index++) {
            int compared = Integer.compareUnsigned(left[index], right[index]);
            if (compared != 0) return compared;
        }
        return Integer.compare(left.length, right.length);
    }

    static int referenceCommonPrefix(long[] left, long[] right) {
        int common = Math.min(left.length, right.length);
        int index = 0;
        while (index < common && left[index] == right[index]) index++;
        return index;
    }

    static int referenceCommonPrefix(int[] left, int[] right) {
        int common = Math.min(left.length, right.length);
        int index = 0;
        while (index < common && left[index] == right[index]) index++;
        return index;
    }

    static int[] referenceHammingTopK(long[] values, long target, int maxDistance, int limit) {
        int maximum = Math.min(values.length, limit);
        int[] indexes = new int[maximum];
        int[] distances = new int[maximum];
        int count = 0;
        for (int index = 0; index < values.length; index++) {
            int distance = Long.bitCount(values[index] ^ target);
            if (distance > maxDistance) continue;
            int position = count;
            while (position > 0) {
                int previousDistance = distances[position - 1];
                int previousIndex = indexes[position - 1];
                if (previousDistance < distance
                        || previousDistance == distance && previousIndex < index) break;
                if (position < maximum) {
                    distances[position] = previousDistance;
                    indexes[position] = previousIndex;
                }
                position--;
            }
            if (position < maximum) {
                distances[position] = distance;
                indexes[position] = index;
                if (count < maximum) count++;
            }
        }
        return count == indexes.length ? indexes : Arrays.copyOf(indexes, count);
    }

    static int[] referenceIntersectSorted(int[] left, int[] right, int limit) {
        return referenceIntersectSortedRanges(
                left, 0, left.length, right, 0, right.length, limit);
    }

    static int[] referenceIntersectSortedRanges(
            int[] left,
            int leftFrom,
            int leftTo,
            int[] right,
            int rightFrom,
            int rightTo,
            int limit) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.checkFromToIndex(leftFrom, leftTo, left.length);
        Objects.checkFromToIndex(rightFrom, rightTo, right.length);
        if (limit < 0) throw new IllegalArgumentException("limit must be >= 0");

        int maximum = Math.min(leftTo - leftFrom, rightTo - rightFrom);
        if (limit > 0) maximum = Math.min(maximum, limit);
        int[] output = new int[maximum];
        int leftIndex = leftFrom;
        int rightIndex = rightFrom;
        int count = 0;
        while (leftIndex < leftTo && rightIndex < rightTo && count < maximum) {
            int leftValue = left[leftIndex];
            int rightValue = right[rightIndex];
            if (leftValue < rightValue) {
                leftIndex++;
            } else if (leftValue > rightValue) {
                rightIndex++;
            } else {
                if (count == 0 || output[count - 1] != leftValue) {
                    output[count++] = leftValue;
                }
                leftIndex++;
                rightIndex++;
                while (leftIndex < leftTo && left[leftIndex] == leftValue) leftIndex++;
                while (rightIndex < rightTo && right[rightIndex] == rightValue) rightIndex++;
            }
        }
        return count == output.length ? output : Arrays.copyOf(output, count);
    }

    static int[] matmulInt8Packed(
            MIndexInt8MatmulPlan plan,
            byte[] left,
            int rows,
            int[] bias) {
        Objects.requireNonNull(plan, "plan");
        plan.validateExecution(left, rows, bias);
        if (!STATE.available()) {
            return plan.referenceMultiply(left, rows, bias);
        }
        return nativeMatmulInt8Packed(
                left,
                rows,
                plan.inner(),
                plan.columns(),
                plan.paddedInner(),
                plan.paddedColumns(),
                plan.packedRightUnsafe(),
                bias);
    }

    static long referenceHash64(long[] values) {
        long hash = HASH_SEED ^ ((long) values.length * HASH_P1);
        for (long value : values) {
            hash = mixValue(hash, value);
        }
        return finishHash(hash);
    }

    static long referenceHash64(int[] values) {
        long hash = HASH_SEED ^ ((long) values.length * HASH_P1);
        for (int value : values) {
            hash = mixValue(hash, Integer.toUnsignedLong(value));
        }
        return finishHash(hash);
    }

    private static long mixValue(long hash, final long value) {
        long mixed = value + HASH_P2;
        mixed ^= mixed >>> 33;
        mixed *= AVALANCHE_1;
        mixed ^= mixed >>> 33;
        mixed *= AVALANCHE_2;
        mixed ^= mixed >>> 33;
        hash ^= mixed;
        return Long.rotateLeft(hash, 27) * HASH_P1 + HASH_P3;
    }

    private static long finishHash(long hash) {
        hash ^= hash >>> 33;
        hash *= AVALANCHE_1;
        hash ^= hash >>> 33;
        hash *= AVALANCHE_2;
        return hash ^ (hash >>> 33);
    }

    private static NativeState load() {
        String mode = System.getProperty(MODE_PROPERTY, "auto").strip().toLowerCase(Locale.ROOT);
        if (!mode.equals("auto") && !mode.equals("off") && !mode.equals("required")) {
            throw new IllegalArgumentException(
                    "Unsupported " + MODE_PROPERTY + " value: " + mode + " (expected auto/off/required)");
        }
        if (mode.equals("off")) return new NativeState(false, mode, "disabled");

        try {
            System.loadLibrary(LIBRARY);
            return new NativeState(true, mode, "loaded");
        } catch (UnsatisfiedLinkError error) {
            if (mode.equals("required")) throw error;
            String message = error.getMessage();
            return new NativeState(false, mode, message == null ? "native library unavailable" : message);
        }
    }

    private static native int nativeIndexOfBytes(
            byte[] haystack,
            int haystackStart,
            int haystackEnd,
            byte[] needle,
            int needleStart,
            int needleEnd);

    private static native int nativeIndexOfDirectSegments(ByteBuffer[] segments, byte[] needle);

    private static native boolean nativeEquals(long[] left, long[] right);
    private static native int nativeCompare(long[] left, long[] right);
    private static native int nativeCommonPrefix(long[] left, long[] right);
    private static native long nativeHash64(long[] values);

    private static native boolean nativeEqualsInts(int[] left, int[] right);
    private static native int nativeCompareInts(int[] left, int[] right);
    private static native int nativeCommonPrefixInts(int[] left, int[] right);
    private static native long nativeHash64Ints(int[] values);
    private static native int[] nativeIntersectSortedInts(int[] left, int[] right, int limit);
    private static native int[] nativeIntersectSortedIntRanges(
            int[] left,
            int leftFrom,
            int leftTo,
            int[] right,
            int rightFrom,
            int rightTo,
            int limit);
    private static native int[] nativeHammingTopK(
            long[] fingerprints, long target, int maxDistance, int limit);
    private static native double[] nativeSoftmax(double[] logits);

    private static native int[] nativeMatmulInt8Packed(
            byte[] left,
            int rows,
            int inner,
            int columns,
            int paddedInner,
            int paddedColumns,
            byte[] packedRight,
            int[] bias);
}
