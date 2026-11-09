// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3MultiRankedByteSubstrings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/** Independent TreeSet enumerator and binary content comparator, not suffix-array code. */
public final class M3MultiRankedByteSubstringsOracle {
    private static long checks;
    private static final byte[] SYMBOLS = {(byte) 255, (byte) 0, (byte) 1};
    private static void check(boolean success, String label) {
        checks++;
        if (!success) throw new AssertionError(label + " @ " + checks);
    }
    private static List<byte[]> reference(byte[][] inputs) {
        TreeSet<byte[]> unique = new TreeSet<>(Arrays::compareUnsigned);
        for (byte[] input : inputs) {
            for (int start = 0; start < input.length; start++) {
                for (int end = start + 1; end <= input.length; end++) {
                    unique.add(Arrays.copyOfRange(input, start, end));
                }
            }
        }
        return new ArrayList<>(unique);
    }
    private static void checkCorpus(byte[][] corpus, String label) {
        byte[][] before = new byte[corpus.length][];
        for (int i = 0; i < corpus.length; i++) before[i] = corpus[i].clone();
        M3MultiRankedByteSubstrings owner = M3MultiRankedByteSubstrings.copyOf(corpus);
        List<byte[]> expected = reference(corpus);
        int totalBytes = 0;
        for (byte[] doc : corpus) totalBytes += doc.length;
        check(owner.documentCount() == corpus.length, label + " documents");
        check(owner.totalBytes() == totalBytes, label + " bytes");
        check(owner.distinctCount() == expected.size(), label + " count");
        check(owner.rankOf(new byte[0]) == -1, label + " empty rank");
        for (int i = 0; i < expected.size(); i++) {
            byte[] value = expected.get(i);
            long rank = i + 1L;
            check(Arrays.equals(owner.select(rank), value), label + " value " + rank);
            check(owner.rankOf(value) == rank, label + " rankOf " + rank);
            int[] span = owner.selectSpan(rank);
            check(span.length == 3 && span[0] >= 0 && span[0] < corpus.length,
                    label + " span owner " + rank);
            check(span[1] >= 0 && span[2] == value.length
                    && span[1] + span[2] <= corpus[span[0]].length, label + " span bound");
            check(Arrays.equals(Arrays.copyOfRange(corpus[span[0]], span[1], span[1] + span[2]),
                    value), label + " span content");
            byte[] returned = owner.select(rank);
            if (returned.length != 0) returned[0] ^= 0x45;
            check(Arrays.equals(owner.select(rank), value), label + " detached bytes");
        }
        long[] ranks = new long[expected.size()];
        for (int i = 0; i < ranks.length; i++) ranks[i] = ranks.length - i;
        int[] triples = owner.selectSpans(ranks);
        check(triples.length == ranks.length * 3, label + " batch size");
        for (int i = 0; i < ranks.length; i++) {
            int[] one = owner.selectSpan(ranks[i]);
            check(one[0] == triples[3*i] && one[1] == triples[3*i+1] && one[2] == triples[3*i+2],
                    label + " batch content");
        }
        if (triples.length != 0) triples[0] ^= 42;
        check(owner.selectSpans(ranks).length == ranks.length * 3, label + " batch detached");
        byte[] absent = new byte[] {3, 3, 3, 3, 3, 3, 3, 3, 3};
        check(owner.rankOf(absent) == -1, label + " nonexistent");
        for (int i = 0; i < corpus.length; i++) check(Arrays.equals(corpus[i], before[i]), "input mutated");
    }
    private static void generate(List<byte[]> result, byte[] buffer, int at) {
        result.add(Arrays.copyOf(buffer, at));
        if (at == buffer.length) return;
        for (byte b : SYMBOLS) {
            buffer[at] = b;
            generate(result, buffer, at+1);
        }
    }
    private static void refusal(Runnable r, Class<? extends RuntimeException> kind) {
        try { r.run(); throw new AssertionError("invalid input admitted"); }
        catch (RuntimeException x) {check(kind.isInstance(x), "wrong refusal type: " + x);}
    }
    public static void main(String[] args) {
        List<byte[]> corpus = new ArrayList<>();
        generate(corpus, new byte[3], 0);
        for (byte[] a : corpus) for (byte[] b : corpus) {
            checkCorpus(new byte[][] {a,b}, "exhaustive pair");
        }
        checkCorpus(new byte[][]{}, "zero-doc");
        checkCorpus(new byte[][]{new byte[0],new byte[0]}, "empty-docs");
        checkCorpus(new byte[][]{new byte[]{'a','a','b'},new byte[]{'a','a','c'}}, "FindStrings sample");
        checkCorpus(new byte[][]{new byte[]{0,1,0},new byte[]{0,1,0},new byte[]{(byte)255}}, "duplicate docs");
        Random random = new Random(0x4d33574156453331L);
        for (int it = 0; it < 3500; it++) {
            int docCount = random.nextInt(5);
            byte[][] inputs = new byte[docCount][];
            for (int d = 0; d < docCount; d++) {
                inputs[d] = new byte[random.nextInt(7)];
                random.nextBytes(inputs[d]);
            }
            checkCorpus(inputs, "random " + it);
        }
        refusal(() -> M3MultiRankedByteSubstrings.copyOf(null), NullPointerException.class);
        refusal(() -> M3MultiRankedByteSubstrings.copyOf(new byte[][]{null}), NullPointerException.class);
        refusal(() -> M3MultiRankedByteSubstrings.copyOf(new byte[65][]), IllegalArgumentException.class);
        refusal(() -> M3MultiRankedByteSubstrings.copyOf(new byte[][]{new byte[131073]}), IllegalArgumentException.class);
        M3MultiRankedByteSubstrings tiny = M3MultiRankedByteSubstrings.copyOf(new byte[][]{new byte[]{1,1}});
        refusal(() -> tiny.select(0), IllegalArgumentException.class);
        refusal(() -> tiny.select(100), IllegalArgumentException.class);
        refusal(() -> tiny.rankOf(null), NullPointerException.class);
        refusal(() -> tiny.selectSpans(null), NullPointerException.class);
        refusal(() -> tiny.selectSpans(new long[32769]), IllegalArgumentException.class);
        refusal(() -> tiny.selectSpans(new long[]{0}), IllegalArgumentException.class);
        byte[][] big = {new byte[131072]};
        new Random(71031).nextBytes(big[0]);
        M3MultiRankedByteSubstrings large = M3MultiRankedByteSubstrings.copyOf(big);
        check(large.totalBytes() == 131072, "max input size");
        check(large.distinctCount() > Integer.MAX_VALUE, "64-bit distinct count");
        check(large.rankOf(large.select(large.distinctCount())) == large.distinctCount(), "last rank");
        byte[] snapshot = large.select(1);
        big[0][0] ^= 127;
        check(Arrays.equals(large.select(1), snapshot), "large caller mutation");
        System.out.println("M3_MULTI_RANKED_JAVA_PASS checks=" + checks);
    }
}
