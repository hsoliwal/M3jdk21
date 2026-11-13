// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3ByteSuffixIndex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HexFormat;

/** Full naïve lexicographic suffix sort, substring set and positional search oracles. */
public final class M3ByteSuffixIndexOracle {
    private static long checks;
    private static final int[] DOMAIN = {0, 1, 128, 255};

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail + " @" + checks);
    }

    private static int compare(byte[] data, int left, int right) {
        while (left < data.length && right < data.length) {
            int a = data[left++] & 0xff;
            int b = data[right++] & 0xff;
            if (a != b) return Integer.compare(a, b);
        }
        return Integer.compare(data.length - left, data.length - right);
    }

    private static int[] referenceSuffixArray(byte[] data) {
        Integer[] refs = new Integer[data.length];
        for (int i = 0; i < refs.length; i++) refs[i] = i;
        Arrays.sort(refs, (a, b) -> compare(data, a, b));
        int[] values = new int[refs.length];
        for (int i = 0; i < refs.length; i++) values[i] = refs[i];
        return values;
    }

    private static int[] referencePositions(byte[] data, byte[] pattern) {
        if (pattern.length == 0) {
            int[] all = new int[data.length + 1];
            for (int i = 0; i < all.length; i++) all[i] = i;
            return all;
        }
        int[] found = new int[data.length];
        int used = 0;
        for (int i = 0; i + pattern.length <= data.length; i++) {
            boolean matched = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) { matched = false; break; }
            }
            if (matched) found[used++] = i;
        }
        return Arrays.copyOf(found, used);
    }

    private static long bruteDistinct(byte[] data) {
        Set<String> distinct = new HashSet<>();
        for (int i = 0; i < data.length; i++) {
            for (int j = i + 1; j <= data.length; j++) {
                distinct.add(HexFormat.of().formatHex(data, i, j));
            }
        }
        return distinct.size();
    }

    private static void checkSearch(M3ByteSuffixIndex index, byte[] text, byte[] pattern) {
        int[] reference = referencePositions(text, pattern);
        int[] matches = index.positions(pattern);
        check(Arrays.equals(matches, reference), "positions");
        check(index.count(pattern) == reference.length, "count");
        check(index.first(pattern) == (reference.length == 0 ? -1 : reference[0]), "first");
        check(index.positions(pattern) != matches, "detached positions");
        if (matches.length > 0) matches[0] = Integer.MIN_VALUE;
        check(Arrays.equals(index.positions(pattern), reference), "positions isolation");
        int[] offsets = {0, pattern.length};
        int[] pairs = index.queryBatch(pattern, offsets);
        check(pairs.length == 2 && pairs[0] == reference.length
                && pairs[1] == (reference.length == 0 ? -1 : reference[0]), "batch pair");
        if (pairs.length > 0) pairs[0] ^= 31;
        check(index.queryBatch(pattern, offsets)[0] == reference.length, "batch isolation");
    }

    private static void inspect(byte[] text, List<byte[]> patterns, boolean small) {
        byte[] original = text.clone();
        M3ByteSuffixIndex index = M3ByteSuffixIndex.copyOf(text);
        check(index.size() == text.length, "size");
        check(Arrays.equals(index.bytes(), original), "original copy");
        check(index.bytes() != index.bytes(), "detached byte view");
        int[] sa = index.suffixArray();
        check(sa != index.suffixArray(), "detached suffix array");
        boolean[] seen = new boolean[sa.length];
        for (int i = 0; i < sa.length; i++) {
            check(sa[i] >= 0 && sa[i] < sa.length && !seen[sa[i]], "suffix permutation");
            seen[sa[i]] = true;
            if (i > 0) check(compare(original, sa[i - 1], sa[i]) < 0, "strict suffix order");
        }
        if (small) {
            check(Arrays.equals(sa, referenceSuffixArray(original)), "brute suffix array");
            check(index.distinctSubstringCount() == bruteDistinct(original), "distinct count");
        } else {
            check(index.distinctSubstringCount() >= 0
                    && index.distinctSubstringCount() <= ((long)sa.length*(sa.length+1))/2,
                    "long distinct bounds");
        }
        if (sa.length > 0) sa[0] = -999;
        check(Arrays.equals(index.suffixArray(), small ? referenceSuffixArray(original)
                : index.suffixArray()), "suffix isolation");
        for (byte[] pattern : patterns) checkSearch(index, original, pattern);
        if (text.length > 0) text[0] ^= 0x3f;
        check(Arrays.equals(index.bytes(), original), "source mutation isolation");
    }

    private static void enumerate(List<byte[]> samples, byte[] lane, int length) {
        if (length == lane.length) {
            samples.add(lane.clone());
            return;
        }
        for (int value : DOMAIN) {
            lane[length] = (byte)value;
            enumerate(samples, lane, length + 1);
        }
    }

    private static void reject(Runnable call, String description) {
        try { call.run(); throw new AssertionError("accepted " + description); }
        catch (IllegalArgumentException | NullPointerException expected) { checks++; }
    }

    public static void main(String[] args) {
        List<byte[]> patterns = new ArrayList<>();
        for (int length = 0; length <= 3; length++) enumerate(patterns, new byte[length], 0);
        for (int length = 0; length <= 5; length++) {
            List<byte[]> texts = new ArrayList<>();
            enumerate(texts, new byte[length], 0);
            for (byte[] text : texts) inspect(text, patterns, true);
        }
        byte[] aba = {'a','b','a','b','a'};
        checkSearch(M3ByteSuffixIndex.copyOf(aba), aba, new byte[]{'a','b','a'});
        Random rng = new Random(0x29fa193defL);
        for (int trial = 0; trial < 1500; trial++) {
            byte[] text = new byte[rng.nextInt(110)];
            rng.nextBytes(text);
            if ((trial & 1) == 0) Arrays.fill(text, (byte)rng.nextInt(3));
            List<byte[]> queries = new ArrayList<>();
            for (int q = 0; q < 15; q++) {
                byte[] pattern = new byte[rng.nextInt(17)];
                rng.nextBytes(pattern);
                if (pattern.length > 0 && (trial % 3) == 0) Arrays.fill(pattern, (byte)0);
                queries.add(pattern);
            }
            inspect(text, queries, trial < 50);
        }
        byte[] repeated = new byte[M3ByteSuffixIndex.MAX_TEXT];
        Arrays.fill(repeated, (byte)0xff);
        M3ByteSuffixIndex huge = M3ByteSuffixIndex.copyOf(repeated);
        check(huge.size() == 131072 && huge.distinctSubstringCount() == 131072L,
                "maximum identical lane");
        check(huge.count(new byte[]{(byte)255}) == repeated.length, "max all-byte count");
        check(huge.first(new byte[]{(byte)255,(byte)255}) == 0, "max first");
        check(huge.count(new byte[0]) == repeated.length+1, "max empty matches");
        check(huge.queryBatch(new byte[0], new int[]{0,0})[0] == repeated.length+1,
                "max empty batch");
        M3ByteSuffixIndex empty = M3ByteSuffixIndex.copyOf(new byte[0]);
        check(empty.distinctSubstringCount() == 0, "empty distinct");
        check(empty.count(new byte[0]) == 1, "empty text boundary");
        check(empty.first(new byte[]{0}) == -1, "empty first");
        check(Arrays.equals(empty.positions(new byte[0]), new int[]{0}), "empty positions");
        check(empty.queryBatch(new byte[0],new int[]{0}).length == 0, "empty batch");
        reject(() -> M3ByteSuffixIndex.copyOf(null), "null source");
        reject(() -> M3ByteSuffixIndex.copyOf(new byte[131073]), "overlarge source");
        reject(() -> empty.first(null), "null pattern");
        reject(() -> empty.count(new byte[4097]), "overlarge pattern");
        reject(() -> empty.queryBatch(null, new int[]{0}), "null packed");
        reject(() -> empty.queryBatch(new byte[0], null), "null offsets");
        reject(() -> empty.queryBatch(new byte[0], new int[0]), "no offset");
        reject(() -> empty.queryBatch(new byte[1], new int[]{0,0}), "not fully covered");
        reject(() -> empty.queryBatch(new byte[2], new int[]{0,2,1}), "nonmonotone");
        reject(() -> empty.queryBatch(new byte[4097], new int[]{0,4097}), "long slice");
        reject(() -> empty.queryBatch(new byte[0], new int[8194]), "too many queries");
        reject(() -> empty.queryBatch(new byte[2_000_001], new int[]{0,2_000_001}), "too many bytes");
        System.out.println("M3_BYTE_SUFFIX_JAVA_PASS checks=" + checks);
    }
}
