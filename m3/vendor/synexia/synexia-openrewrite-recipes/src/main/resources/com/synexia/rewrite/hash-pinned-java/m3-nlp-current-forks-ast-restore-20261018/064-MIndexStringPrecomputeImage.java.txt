// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable primitive geometry derived from one canonical MIndexString.
 *
 * <p>All positional arrays use java.lang.String UTF-16 indexing. Code-point statistics are a
 * separate sparse lane and never change String-compatible indices.</p>
 */
public final class MIndexStringPrecomputeImage {
    public record Fingerprint(long first, long second) { }
    public record Span(int startInclusive, int endExclusive) {
        public Span {
            if (startInclusive < 0 || endExclusive < startInclusive)
                throw new IllegalArgumentException("invalid span");
        }
        public int length() { return endExclusive - startInclusive; }
    }

    private static final long HASH_BASE_1 = 0x9e3779b185ebca87L;
    private static final long HASH_BASE_2 = 0xc2b2ae3d27d4eb4fL;
    private static final long[] EMPTY_LONGS = new long[0];
    private static final int[] EMPTY_INTS = new int[0];

    private final MIndexString value;
    private final MIndexStringPrecomputeProfile profile;
    private final MIndexStringMetadata metadata;
    private final int firstCodePoint;
    private final int lastCodePoint;
    private final int runCount;
    private final int longestRun;
    private final long asciiLow;
    private final long asciiHigh;
    private final int[] histogramCodePoints;
    private final int[] histogramCounts;
    private final int[] prefixFunction;
    private final int[] zFunction;
    private final int[] oddPalindrome;
    private final int[] evenPalindrome;
    private final long[] prefixHash1;
    private final long[] prefixHash2;
    private final long[] powers1;
    private final long[] powers2;
    private final long[] bloom;
    private final long[] minHash;
    private final long simHash;
    private final int[] suffixArray;
    private final int[] suffixRank;
    private final int[] lcp;
    private final int[] rmqLogs;
    private final int[][] rmq;
    private final long estimatedBytes;
    private final String rootSha256;

    private MIndexStringPrecomputeImage(
            MIndexString value,
            String spelling,
            MIndexStringPrecomputeProfile profile,
            MIndexStringMetadata metadata,
            int firstCodePoint,
            int lastCodePoint,
            int runCount,
            int longestRun,
            long asciiLow,
            long asciiHigh,
            int[] histogramCodePoints,
            int[] histogramCounts,
            int[] prefixFunction,
            int[] zFunction,
            int[] oddPalindrome,
            int[] evenPalindrome,
            long[] prefixHash1,
            long[] prefixHash2,
            long[] powers1,
            long[] powers2,
            long[] bloom,
            long[] minHash,
            long simHash,
            int[] suffixArray,
            int[] suffixRank,
            int[] lcp,
            int[] rmqLogs,
            int[][] rmq) {
        this.value = value;
        this.profile = profile;
        this.metadata = metadata;
        this.firstCodePoint = firstCodePoint;
        this.lastCodePoint = lastCodePoint;
        this.runCount = runCount;
        this.longestRun = longestRun;
        this.asciiLow = asciiLow;
        this.asciiHigh = asciiHigh;
        this.histogramCodePoints = histogramCodePoints;
        this.histogramCounts = histogramCounts;
        this.prefixFunction = prefixFunction;
        this.zFunction = zFunction;
        this.oddPalindrome = oddPalindrome;
        this.evenPalindrome = evenPalindrome;
        this.prefixHash1 = prefixHash1;
        this.prefixHash2 = prefixHash2;
        this.powers1 = powers1;
        this.powers2 = powers2;
        this.bloom = bloom;
        this.minHash = minHash;
        this.simHash = simHash;
        this.suffixArray = suffixArray;
        this.suffixRank = suffixRank;
        this.lcp = lcp;
        this.rmqLogs = rmqLogs;
        this.rmq = rmq;
        this.estimatedBytes = estimateBytes();
        this.rootSha256 = computeRootSha256(spelling);
    }

    public static MIndexStringPrecomputeImage compile(
            MIndexString value, MIndexStringPrecomputeProfile profile) {
        MIndexString canonical = Objects.requireNonNull(value, "value").intern();
        MIndexStringPrecomputeProfile wanted = Objects.requireNonNull(profile, "profile");
        return compile(canonical, wanted, null);
    }

    /** Cache-owned monotone upgrade. Retained immutable primitive lanes never need rebuilding. */
    MIndexStringPrecomputeImage extend(MIndexStringPrecomputeProfile wanted) {
        Objects.requireNonNull(wanted, "profile");
        if (profile.includes(wanted)) return this;
        return compile(value, profile.union(wanted), this);
    }

    private static MIndexStringPrecomputeImage compile(
            MIndexString canonical, MIndexStringPrecomputeProfile wanted,
            MIndexStringPrecomputeImage previous) {
        String text = canonical.toString();
        MIndexStringMetadata metadata = previous == null
                ? MIndexStringMetadata.analyze(canonical) : previous.metadata;

        Stats stats = has(previous, MIndexStringPrecomputeProfile.HISTOGRAM)
                ? new Stats(previous.firstCodePoint, previous.lastCodePoint,
                        previous.runCount, previous.longestRun, previous.asciiLow,
                        previous.asciiHigh, previous.histogramCodePoints, previous.histogramCounts)
                : wanted.has(MIndexStringPrecomputeProfile.HISTOGRAM)
                ? stats(text) : Stats.empty();
        int[] prefix = has(previous, MIndexStringPrecomputeProfile.PREFIX_FUNCTION)
                ? previous.prefixFunction : wanted.has(MIndexStringPrecomputeProfile.PREFIX_FUNCTION)
                ? prefixFunction(text) : EMPTY_INTS;
        int[] z = has(previous, MIndexStringPrecomputeProfile.Z_FUNCTION)
                ? previous.zFunction : wanted.has(MIndexStringPrecomputeProfile.Z_FUNCTION)
                ? zFunction(text) : EMPTY_INTS;
        Palindromes palindromes = has(previous, MIndexStringPrecomputeProfile.PALINDROME)
                ? new Palindromes(previous.oddPalindrome, previous.evenPalindrome)
                : wanted.has(MIndexStringPrecomputeProfile.PALINDROME)
                ? manacher(text) : Palindromes.empty();
        Rolling rolling = has(previous, MIndexStringPrecomputeProfile.ROLLING_HASH)
                ? new Rolling(previous.prefixHash1, previous.prefixHash2, previous.powers1, previous.powers2)
                : wanted.has(MIndexStringPrecomputeProfile.ROLLING_HASH)
                ? rolling(text) : Rolling.empty();
        Sketch sketch = has(previous, MIndexStringPrecomputeProfile.SKETCH)
                ? new Sketch(previous.bloom, previous.minHash, previous.simHash)
                : wanted.has(MIndexStringPrecomputeProfile.SKETCH)
                ? sketch(text) : Sketch.empty();
        Suffix suffix = has(previous, MIndexStringPrecomputeProfile.SUFFIX_ARRAY)
                ? new Suffix(previous.suffixArray, previous.suffixRank, previous.lcp,
                        previous.rmqLogs, previous.rmq)
                : wanted.has(MIndexStringPrecomputeProfile.SUFFIX_ARRAY)
                ? suffix(text) : Suffix.empty();

        return new MIndexStringPrecomputeImage(
                canonical, text, wanted, metadata,
                stats.firstCodePoint, stats.lastCodePoint, stats.runCount, stats.longestRun,
                stats.asciiLow, stats.asciiHigh, stats.codePoints, stats.counts,
                prefix, z, palindromes.odd, palindromes.even,
                rolling.prefix1, rolling.prefix2, rolling.power1, rolling.power2,
                sketch.bloom, sketch.minHash, sketch.simHash,
                suffix.array, suffix.rank, suffix.lcp, suffix.logs, suffix.rmq);
    }

    private static boolean has(MIndexStringPrecomputeImage previous, int lane) {
        return previous != null && previous.profile.has(lane);
    }

    public MIndexString value() { return value; }
    public MIndexStringPrecomputeProfile profile() { return profile; }
    public MIndexStringMetadata metadata() { return metadata; }
    public long estimatedBytes() { return estimatedBytes; }

    /**
     * Deterministic SHA-256 seal over the canonical spelling, selected profile and every retained
     * primitive precompute lane.
     *
     * <p>Process-local content indices are deliberately excluded so identical input/profile pairs
     * retain the same root regardless of admission order.</p>
     */
    public String rootSha256() { return rootSha256; }
    public int firstCodePoint() { return firstCodePoint; }
    public int lastCodePoint() { return lastCodePoint; }
    public int runCount() { return runCount; }
    public int longestRun() { return longestRun; }
    public long asciiPresenceLow() { return asciiLow; }
    public long asciiPresenceHigh() { return asciiHigh; }
    public long simHash64() { require(MIndexStringPrecomputeProfile.SKETCH); return simHash; }

    public int distinctCodePoints() { return histogramCodePoints.length; }

    public int codePointFrequency(int codePoint) {
        int index = Arrays.binarySearch(histogramCodePoints, codePoint);
        return index < 0 ? 0 : histogramCounts[index];
    }

    public int[] prefixFunction() {
        require(MIndexStringPrecomputeProfile.PREFIX_FUNCTION);
        return prefixFunction.clone();
    }

    /** Number of immutable KMP failure entries retained for this canonical spelling. */
    public int prefixFunctionLength() {
        require(MIndexStringPrecomputeProfile.PREFIX_FUNCTION);
        return prefixFunction.length;
    }

    /**
     * One immutable KMP failure entry without cloning the complete table.
     *
     * <p>This is the zero-allocation hot-path access used by compiler/JDK String shadows after the
     * canonical needle has explicitly admitted {@link MIndexStringPrecomputeProfile#LITERAL_PATTERN}.
     * The array itself never escapes.</p>
     */
    public int prefixFunctionAt(int index) {
        require(MIndexStringPrecomputeProfile.PREFIX_FUNCTION);
        return prefixFunction[Objects.checkIndex(index, prefixFunction.length)];
    }

    public int[] zFunction() {
        require(MIndexStringPrecomputeProfile.Z_FUNCTION);
        return zFunction.clone();
    }

    public int minimalPeriod() {
        require(MIndexStringPrecomputeProfile.PREFIX_FUNCTION);
        int n = value.length();
        if (n == 0) return 0;
        int period = n - prefixFunction[n - 1];
        return n % period == 0 ? period : n;
    }

    public int[] borders() {
        require(MIndexStringPrecomputeProfile.PREFIX_FUNCTION);
        int count = 0;
        for (int k = prefixFunction.length == 0 ? 0 : prefixFunction[prefixFunction.length - 1];
                k > 0; k = prefixFunction[k - 1]) count++;
        int[] result = new int[count];
        int index = count;
        for (int k = prefixFunction.length == 0 ? 0 : prefixFunction[prefixFunction.length - 1];
                k > 0; k = prefixFunction[k - 1]) result[--index] = k;
        return result;
    }

    public Span longestPalindrome() {
        require(MIndexStringPrecomputeProfile.PALINDROME);
        int bestStart = 0;
        int bestLength = 0;
        for (int center = 0; center < oddPalindrome.length; center++) {
            int length = oddPalindrome[center] * 2 - 1;
            int start = center - oddPalindrome[center] + 1;
            if (length > bestLength) {
                bestLength = length;
                bestStart = start;
            }
        }
        for (int center = 0; center < evenPalindrome.length; center++) {
            int length = evenPalindrome[center] * 2;
            int start = center - evenPalindrome[center];
            if (length > bestLength) {
                bestLength = length;
                bestStart = start;
            }
        }
        return new Span(bestStart, bestStart + bestLength);
    }

    public boolean isPalindrome(int start, int end) {
        require(MIndexStringPrecomputeProfile.PALINDROME);
        Objects.checkFromToIndex(start, end, value.length());
        int length = end - start;
        if (length < 2) return true;
        if ((length & 1) != 0) {
            int center = start + length / 2;
            return oddPalindrome[center] >= length / 2 + 1;
        }
        int center = start + length / 2;
        return evenPalindrome[center] >= length / 2;
    }

    public Fingerprint substringFingerprint(int start, int end) {
        require(MIndexStringPrecomputeProfile.ROLLING_HASH);
        Objects.checkFromToIndex(start, end, value.length());
        int length = end - start;
        long first = prefixHash1[end] - prefixHash1[start] * powers1[length];
        long second = prefixHash2[end] - prefixHash2[start] * powers2[length];
        return new Fingerprint(first, second);
    }

    /** Exact rolling fingerprint for the complete canonical UTF-16 spelling. */
    public Fingerprint fullFingerprint() {
        return substringFingerprint(0, value.length());
    }

    /**
     * Compose exact rolling fingerprints without rereading either character range.
     *
     * <p>The result is the same fingerprint produced by scanning {@code left + right}. Java long
     * overflow is intentional and part of this internal fingerprint algebra. Fingerprints remain
     * candidate evidence only; callers must exactly verify equality before returning a
     * String-contract result.</p>
     */
    public static Fingerprint composeFingerprints(
            Fingerprint left, Fingerprint right, int rightLength) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (rightLength < 0) throw new IllegalArgumentException("negative rightLength");
        return new Fingerprint(
                left.first() * power(HASH_BASE_1, rightLength) + right.first(),
                left.second() * power(HASH_BASE_2, rightLength) + right.second());
    }

    private static long power(long base, int exponent) {
        long result = 1L;
        long factor = base;
        int remaining = exponent;
        while (remaining != 0) {
            if ((remaining & 1) != 0) result *= factor;
            factor *= factor;
            remaining >>>= 1;
        }
        return result;
    }

    public boolean maybeContainsQGram(int hash) {
        require(MIndexStringPrecomputeProfile.SKETCH);
        long mixed = mix64(Integer.toUnsignedLong(hash));
        int a = (int) mixed & 255;
        int b = (int) (mixed >>> 16) & 255;
        return bit(bloom, a) && bit(bloom, b);
    }

    public long minHash(int lane) {
        require(MIndexStringPrecomputeProfile.SKETCH);
        return minHash[Objects.checkIndex(lane, minHash.length)];
    }

    public int minHashLanes() {
        require(MIndexStringPrecomputeProfile.SKETCH);
        return minHash.length;
    }

    public int[] suffixArray() {
        require(MIndexStringPrecomputeProfile.SUFFIX_ARRAY);
        return suffixArray.clone();
    }

    public int suffixRank(int suffixStart) {
        require(MIndexStringPrecomputeProfile.SUFFIX_ARRAY);
        return suffixRank[Objects.checkIndex(suffixStart, suffixRank.length)];
    }

    public int lcpSuffixes(int leftStart, int rightStart) {
        require(MIndexStringPrecomputeProfile.SUFFIX_ARRAY);
        Objects.checkIndex(leftStart, value.length());
        Objects.checkIndex(rightStart, value.length());
        if (leftStart == rightStart) return value.length() - leftStart;
        int leftRank = suffixRank[leftStart];
        int rightRank = suffixRank[rightStart];
        int from = Math.min(leftRank, rightRank);
        int to = Math.max(leftRank, rightRank) - 1;
        return rmq(from, to);
    }

    public Span longestRepeatedSubstring() {
        require(MIndexStringPrecomputeProfile.SUFFIX_ARRAY);
        int best = 0;
        int start = 0;
        for (int i = 0; i < lcp.length; i++) {
            if (lcp[i] > best) {
                best = lcp[i];
                start = suffixArray[i];
            }
        }
        return new Span(start, start + best);
    }

    public int suffixSearch(CharSequence pattern) {
        require(MIndexStringPrecomputeProfile.SUFFIX_ARRAY);
        Objects.requireNonNull(pattern, "pattern");
        String text = value.toString();
        int low = 0;
        int high = suffixArray.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int comparison = compareSuffix(text, suffixArray[middle], pattern);
            if (comparison < 0) low = middle + 1;
            else if (comparison > 0) high = middle - 1;
            else return suffixArray[middle];
        }
        return -1;
    }

    private int rmq(int from, int to) {
        if (from > to) return 0;
        int length = to - from + 1;
        int level = rmqLogs[length];
        int width = 1 << level;
        return Math.min(rmq[level][from], rmq[level][to - width + 1]);
    }

    private void require(int lane) {
        if (!profile.has(lane))
            throw new IllegalStateException("precompute lane not present: " + lane);
    }

    private String computeRootSha256(String spelling) {
        final MessageDigest digest = sha256();
        digest.update("MINDEX-STRING-PRECOMPUTE-2".getBytes(StandardCharsets.US_ASCII));

        // UTF-8 replacement bytes are lossy for distinct unpaired UTF-16 surrogates.
        // Seal the exact Java String units, including an exceptional UTF-16 sidecar.
        updateInt(digest, spelling.length());
        for (int index = 0; index < spelling.length(); index++) {
            char unit = spelling.charAt(index);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
        updateInt(digest, profile.mask());

        updateInt(digest, metadata.utf16Length());
        updateInt(digest, metadata.codePointLength());
        updateInt(digest, metadata.utf8Length());
        updateInt(digest, metadata.javaHashCode());
        updateLong(digest, metadata.contentHash64());
        updateLong(digest, metadata.rollingHash64());
        updateInt(digest, metadata.flags());
        updateInt(digest, metadata.numericMask());
        if (metadata.canConvertToInt32()) {
            updateInt(digest, metadata.int32Value());
        }
        if (metadata.canConvertToInt64()) {
            updateLong(digest, metadata.int64Value());
        }
        if (metadata.canConvertToFloat32()) {
            updateInt(digest, metadata.float32RawBits());
        }
        if (metadata.canConvertToFloat64()) {
            updateLong(digest, metadata.float64RawBits());
        }
        updateLong(digest, metadata.lexicalBits());
        updateLong(digest, metadata.semanticBits());
        updateLong(digest, metadata.relationBits());
        updateInt(digest, metadata.sourceStart());
        updateInt(digest, metadata.sourceEnd());

        updateInt(digest, firstCodePoint);
        updateInt(digest, lastCodePoint);
        updateInt(digest, runCount);
        updateInt(digest, longestRun);
        updateLong(digest, asciiLow);
        updateLong(digest, asciiHigh);
        updateInts(digest, histogramCodePoints);
        updateInts(digest, histogramCounts);
        updateInts(digest, prefixFunction);
        updateInts(digest, zFunction);
        updateInts(digest, oddPalindrome);
        updateInts(digest, evenPalindrome);
        updateLongs(digest, prefixHash1);
        updateLongs(digest, prefixHash2);
        updateLongs(digest, powers1);
        updateLongs(digest, powers2);
        updateLongs(digest, bloom);
        updateLongs(digest, minHash);
        updateLong(digest, simHash);
        updateInts(digest, suffixArray);
        updateInts(digest, suffixRank);
        updateInts(digest, lcp);
        updateInts(digest, rmqLogs);
        updateInt(digest, rmq.length);
        for (int[] level : rmq) updateInts(digest, level);

        return HexFormat.of().formatHex(digest.digest());
    }

    private static void updateExactString(
            final MessageDigest digest,
            final MIndexString value) {
        if (!value.requiresUtf16Sidecar()) {
            digest.update((byte) 1);
            final ByteBuffer utf8 = value.utf8View();
            updateInt(digest, utf8.remaining());
            digest.update(utf8);
            return;
        }

        digest.update((byte) 2);
        final String exact = value.toString();
        updateInt(digest, exact.length());
        for (int index = 0; index < exact.length(); index++) {
            final char unit = exact.charAt(index);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void updateInts(final MessageDigest digest, final int[] values) {
        updateInt(digest, values.length);
        for (int value : values) updateInt(digest, value);
    }

    private static void updateLongs(final MessageDigest digest, final long[] values) {
        updateInt(digest, values.length);
        for (long value : values) updateLong(digest, value);
    }

    private static void updateInt(final MessageDigest digest, final int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static void updateLong(final MessageDigest digest, final long value) {
        for (int shift = 56; shift >= 0; shift -= 8) {
            digest.update((byte) (value >>> shift));
        }
    }

    private long estimateBytes() {
        long bytes = 128;
        bytes += 4L * (histogramCodePoints.length + histogramCounts.length
                + prefixFunction.length + zFunction.length + oddPalindrome.length
                + evenPalindrome.length + suffixArray.length + suffixRank.length
                + lcp.length + rmqLogs.length);
        bytes += 8L * (prefixHash1.length + prefixHash2.length + powers1.length
                + powers2.length + bloom.length + minHash.length);
        for (int[] level : rmq) bytes += 4L * level.length;
        return bytes;
    }

    private static Stats stats(String text) {
        if (text.isEmpty()) return Stats.empty();
        int[] all = text.codePoints().toArray();
        int first = all[0];
        int last = all[all.length - 1];
        long low = 0L;
        long high = 0L;
        for (int codePoint : all) {
            if (codePoint >= 0 && codePoint < 64) low |= 1L << codePoint;
            else if (codePoint < 128) high |= 1L << (codePoint - 64);
        }

        int runs = 0;
        int longest = 0;
        int current = 0;
        int previous = -1;
        for (int codePoint : all) {
            if (runs == 0 || codePoint != previous) {
                runs++;
                current = 1;
                previous = codePoint;
            } else {
                current++;
            }
            longest = Math.max(longest, current);
        }

        int[] sorted = all.clone();
        Arrays.sort(sorted);
        int distinct = 0;
        for (int i = 0; i < sorted.length; i++)
            if (i == 0 || sorted[i] != sorted[i - 1]) distinct++;
        int[] codePoints = new int[distinct];
        int[] counts = new int[distinct];
        int output = -1;
        for (int codePoint : sorted) {
            if (output < 0 || codePoints[output] != codePoint) {
                codePoints[++output] = codePoint;
                counts[output] = 1;
            } else {
                counts[output]++;
            }
        }
        return new Stats(first, last, runs, longest, low, high, codePoints, counts);
    }

    private static int[] prefixFunction(String text) {
        int n = text.length();
        int[] pi = new int[n];
        for (int i = 1; i < n; i++) {
            int j = pi[i - 1];
            while (j > 0 && text.charAt(i) != text.charAt(j)) j = pi[j - 1];
            if (text.charAt(i) == text.charAt(j)) j++;
            pi[i] = j;
        }
        return pi;
    }

    private static int[] zFunction(String text) {
        int n = text.length();
        int[] z = new int[n];
        if (n != 0) z[0] = n;
        int left = 0;
        int right = 0;
        for (int i = 1; i < n; i++) {
            if (i < right) z[i] = Math.min(right - i, z[i - left]);
            while (i + z[i] < n && text.charAt(z[i]) == text.charAt(i + z[i])) z[i]++;
            if (i + z[i] > right) {
                left = i;
                right = i + z[i];
            }
        }
        return z;
    }

    private static Palindromes manacher(String text) {
        int n = text.length();
        int[] odd = new int[n];
        int left = 0;
        int right = -1;
        for (int i = 0; i < n; i++) {
            int radius = i > right ? 1 : Math.min(odd[left + right - i], right - i + 1);
            while (i - radius >= 0 && i + radius < n
                    && text.charAt(i - radius) == text.charAt(i + radius)) radius++;
            odd[i] = radius;
            if (i + radius - 1 > right) {
                left = i - radius + 1;
                right = i + radius - 1;
            }
        }
        int[] even = new int[n];
        left = 0;
        right = -1;
        for (int i = 0; i < n; i++) {
            int radius = i > right ? 0 : Math.min(even[left + right - i + 1], right - i + 1);
            while (i - radius - 1 >= 0 && i + radius < n
                    && text.charAt(i - radius - 1) == text.charAt(i + radius)) radius++;
            even[i] = radius;
            if (i + radius - 1 > right) {
                left = i - radius;
                right = i + radius - 1;
            }
        }
        return new Palindromes(odd, even);
    }

    private static Rolling rolling(String text) {
        int n = text.length();
        long[] p1 = new long[n + 1];
        long[] p2 = new long[n + 1];
        long[] pow1 = new long[n + 1];
        long[] pow2 = new long[n + 1];
        pow1[0] = 1L;
        pow2[0] = 1L;
        for (int i = 0; i < n; i++) {
            long unit = text.charAt(i) + 1L;
            p1[i + 1] = p1[i] * HASH_BASE_1 + unit;
            p2[i + 1] = p2[i] * HASH_BASE_2 + unit;
            pow1[i + 1] = pow1[i] * HASH_BASE_1;
            pow2[i + 1] = pow2[i] * HASH_BASE_2;
        }
        return new Rolling(p1, p2, pow1, pow2);
    }

    private static Sketch sketch(String text) {
        long[] bloom = new long[4];
        long[] minimum = {~0L, ~0L, ~0L, ~0L};
        int[] weights = new int[64];
        int grams = Math.max(1, text.length() - 2);
        for (int i = 0; i < grams; i++) {
            long raw = 0xcbf29ce484222325L;
            int end = Math.min(text.length(), i + 3);
            for (int j = i; j < end; j++) raw = (raw ^ text.charAt(j)) * 0x100000001b3L;
            long mixed = mix64(raw);
            setBit(bloom, (int) mixed & 255);
            setBit(bloom, (int) (mixed >>> 16) & 255);
            for (int lane = 0; lane < minimum.length; lane++) {
                long candidate = mix64(mixed ^ (0x9e3779b97f4a7c15L * (lane + 1L)));
                if (Long.compareUnsigned(candidate, minimum[lane]) < 0) minimum[lane] = candidate;
            }
            for (int bit = 0; bit < 64; bit++)
                weights[bit] += ((mixed >>> bit) & 1L) != 0 ? 1 : -1;
        }
        long sim = 0L;
        for (int bit = 0; bit < 64; bit++) if (weights[bit] >= 0) sim |= 1L << bit;
        return new Sketch(bloom, minimum, sim);
    }

    private static Suffix suffix(String text) {
        int n = text.length();
        if (n == 0) return Suffix.empty();
        if (n == 1) return new Suffix(new int[1], new int[1], EMPTY_INTS, new int[2], new int[0][]);
        int[] array = new int[n];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) array[i] = i;
        int[] work = new int[n];
        int[] nextRank = new int[n];
        // Two byte passes compress the UTF-16 alphabet into dense ranks. Small strings no
        // longer allocate two 65,537-entry count arrays, including for exceptional surrogates.
        int[] counts = new int[Math.max(256, Math.addExact(n, 1))];
        for (int shift = 0; shift <= 8; shift += 8) {
            Arrays.fill(counts, 0, 256, 0);
            for (int suffix : array) counts[(text.charAt(suffix) >>> shift) & 255]++;
            for (int i = 1; i < 256; i++) counts[i] += counts[i - 1];
            for (int i = n - 1; i >= 0; i--) {
                int suffix = array[i];
                work[--counts[(text.charAt(suffix) >>> shift) & 255]] = suffix;
            }
            int[] swap = array;
            array = work;
            work = swap;
        }
        int classes = 1;
        rank[array[0]] = 0;
        for (int i = 1; i < n; i++) {
            if (text.charAt(array[i - 1]) != text.charAt(array[i])) classes++;
            rank[array[i]] = classes - 1;
        }
        for (int step = 1; step < n && classes < n;) {
            radixSort(array, work, rank, step, classes, counts);
            int nextClasses = 1;
            nextRank[array[0]] = 0;
            for (int i = 1; i < n; i++) {
                int left = array[i - 1];
                int right = array[i];
                if (rank[left] != rank[right]
                        || rankAt(rank, left + step) != rankAt(rank, right + step)) {
                    nextClasses++;
                }
                nextRank[right] = nextClasses - 1;
            }
            int[] swap = rank;
            rank = nextRank;
            nextRank = swap;
            classes = nextClasses;
            if (classes == n || step > n / 2) break;
            step <<= 1;
        }

        int[] inverse = new int[n];
        for (int i = 0; i < n; i++) inverse[array[i]] = i;
        int[] lcp = new int[Math.max(0, n - 1)];
        int common = 0;
        for (int start = 0; start < n; start++) {
            int position = inverse[start];
            if (position == n - 1) {
                common = 0;
                continue;
            }
            int other = array[position + 1];
            while (start + common < n && other + common < n
                    && text.charAt(start + common) == text.charAt(other + common)) common++;
            lcp[position] = common;
            if (common > 0) common--;
        }

        int[] logs = new int[Math.max(2, lcp.length + 1)];
        for (int i = 2; i < logs.length; i++) logs[i] = logs[i >>> 1] + 1;
        int levels = lcp.length == 0 ? 0 : logs[lcp.length] + 1;
        int[][] rmq = new int[levels][];
        if (levels != 0) {
            rmq[0] = lcp.clone();
            for (int level = 1; level < levels; level++) {
                int width = 1 << level;
                int half = width >>> 1;
                int entries = lcp.length - width + 1;
                rmq[level] = new int[Math.max(0, entries)];
                for (int i = 0; i < entries; i++)
                    rmq[level][i] = Math.min(rmq[level - 1][i], rmq[level - 1][i + half]);
            }
        }
        return new Suffix(array, inverse, lcp, logs, rmq);
    }

    private static void radixSort(int[] suffixes, int[] work, int[] ranks, int step, int classes, int[] counts) {
        countingSort(suffixes, work, ranks, step, classes, counts);
        countingSort(work, suffixes, ranks, 0, classes, counts);
    }

    private static void countingSort(int[] input, int[] output, int[] ranks,
            int offset, int classes, int[] counts) {
        Arrays.fill(counts, 0, classes + 1, 0);
        for (int suffix : input) counts[key(ranks, suffix + offset)]++;
        for (int i = 1; i <= classes; i++) counts[i] += counts[i - 1];
        for (int i = input.length - 1; i >= 0; i--) {
            int suffix = input[i];
            int key = key(ranks, suffix + offset);
            output[--counts[key]] = suffix;
        }
    }

    private static int key(int[] ranks, int index) {
        return index < ranks.length ? ranks[index] + 1 : 0;
    }

    private static int rankAt(int[] ranks, int index) {
        return index < ranks.length ? ranks[index] : -1;
    }

    private static int compareSuffix(String text, int start, CharSequence pattern) {
        int common = Math.min(text.length() - start, pattern.length());
        for (int i = 0; i < common; i++) {
            int comparison = Character.compare(text.charAt(start + i), pattern.charAt(i));
            if (comparison != 0) return comparison;
        }
        if (pattern.length() == common) return 0;
        return -1;
    }

    private static boolean bit(long[] bloom, int bit) {
        return (bloom[bit >>> 6] & (1L << (bit & 63))) != 0;
    }

    private static void setBit(long[] bloom, int bit) {
        bloom[bit >>> 6] |= 1L << (bit & 63);
    }

    private static long mix64(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        return mixed ^ (mixed >>> 33);
    }

    private record Stats(int firstCodePoint, int lastCodePoint, int runCount, int longestRun,
            long asciiLow, long asciiHigh, int[] codePoints, int[] counts) {
        static Stats empty() {
            return new Stats(-1, -1, 0, 0, 0L, 0L, EMPTY_INTS, EMPTY_INTS);
        }
    }
    private record Palindromes(int[] odd, int[] even) {
        static Palindromes empty() { return new Palindromes(EMPTY_INTS, EMPTY_INTS); }
    }
    private record Rolling(long[] prefix1, long[] prefix2, long[] power1, long[] power2) {
        static Rolling empty() {
            return new Rolling(EMPTY_LONGS, EMPTY_LONGS, EMPTY_LONGS, EMPTY_LONGS);
        }
    }
    private record Sketch(long[] bloom, long[] minHash, long simHash) {
        static Sketch empty() { return new Sketch(EMPTY_LONGS, EMPTY_LONGS, 0L); }
    }
    private record Suffix(int[] array, int[] rank, int[] lcp, int[] logs, int[][] rmq) {
        static Suffix empty() {
            return new Suffix(EMPTY_INTS, EMPTY_INTS, EMPTY_INTS, EMPTY_INTS, new int[0][]);
        }
    }
}
