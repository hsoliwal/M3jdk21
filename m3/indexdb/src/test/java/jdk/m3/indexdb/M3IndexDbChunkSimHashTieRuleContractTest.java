// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Contract pin of the SimHash bit-vote tie rule of the live fingerprint owner {@link M3IndexDbChunkSimHash}.
 *
 * <p><b>The rule.</b> {@link M3IndexDbChunkSimHash#simHash64()} sets bit {@code b} of the signature when the
 * weighted vote sum of bit {@code b} is {@code >= 0}: a bit whose votes cancel exactly (a tie) is {@code 1}, never
 * {@code 0}. The only {@code 0} signature over tied positions is the empty window ({@code featureCount == 0}),
 * which returns {@code 0L} explicitly before any vote is read. This test pins the rule so that a future change of
 * it is a conscious one (this surefire run turns red); it is the precondition of porting any fingerprint consumer
 * onto the owner (lane owner decision 2026-11-06, recipe {@code m3/indexdb/recipe/hamming-topk-20261106}).
 *
 * <p><b>Construction of an exact tie.</b> {@link M3IndexDbChunkSimHash#fromFeatures} adds {@code +w} to every bit
 * set in a feature of weight {@code w} and {@code -w} to every bit clear in it. For a window {@code W} of features
 * {@code f_i} with weights {@code w_i} and a tie mask {@code M}, the mirrored window {@code W ++ (f_i ^ M)} with
 * the same weights has, on every bit of {@code M}, the pair contribution {@code +w_i - w_i = 0} (a vote sum of
 * exactly zero whatever the weights) and, on every other bit, twice the vote of {@code W} (the same sign, hence the
 * same majority bit). The owner's own {@link M3IndexDbChunkSimHash#voteCopy()} proves the construction inside the
 * test (every bit of {@code M} reads 0, every other bit reads {@code 2 * vote(W)}) before the signature is asserted:
 * {@code simHash64(W ++ (W ^ M)) == simHash64(W) | M}, 1 on every tied position and the majority bit elsewhere;
 * the same holds through {@link M3IndexDbChunkSimHash#merge}. The pair {@code {f, ~f}} with equal weights ties all
 * 64 bits and yields {@code -1L}.
 *
 * <p><b>Reference and figures.</b> The untied behaviour is asserted against an independent reference written here
 * (the straightforward per-bit vote count, {@link #referenceVotes}) over 10,000 seeded unit-weight windows of 0..64
 * random features: the first 10,000 windows of the NEC lane of {@code M3HammingTopKDump --fingerprint} (seed
 * {@code 0x4d3346494e475052}, same draws), so the printed figures are a prefix of that recorded differential. The
 * test prints the window classes (untied, tied, empty, tied bits), the agreement of the owner with the reference
 * under the ties-to-1 rule (every window) and the exact tie-mask difference to the ties-to-0 rule.
 *
 * <p><b>NEC.</b> com.synexia's {@code SimHash64.fingerprint(long[])} resolves a tie to 0 ({@code votes > 0}); this
 * owner resolves it to 1 ({@code votes >= 0}). Over 20,000 seeded unit-weight windows the two agreed on the 10,220
 * untied or empty windows and differed exactly on the tie mask of the other 9,780 (0 unexplained; recipe
 * hamming-topk-20261106, RECIPE_MEMORY.tsv row "fingerprint (NEC)"). The difference is recorded as NEC in the
 * convergence ledger: no equivalence is decided, {@link M3IndexDbChunkSimHash} is the one fingerprint owner, and a
 * donor consumer that expects ties to 0 goes through a recipe-level adapter with its own differential, never a
 * second fingerprint owner.
 */
//FLLDCIM: CONTRACT - pins the tie rule (vote >= 0 -> bit 1) of M3IndexDbChunkSimHash.simHash64 before any fingerprint consumer is ported; NEC vs com.synexia SimHash64.fingerprint (tie -> 0), recipe m3/indexdb/recipe/hamming-topk-20261106
final class M3IndexDbChunkSimHashTieRuleContractTest {
    private static final String DOMAIN = "m3:indexdb:tie-rule-contract:v1";
    /** The window seed of the NEC lane of M3HammingTopKDump (this test replays its first 10,000 windows). */
    private static final long WINDOW_SEED = 0x4d3346494e475052L;
    private static final long CONSTRUCTION_SEED = 0x4d33544945525545L;
    private static final int WINDOWS = 10_000;
    private static final int CONSTRUCTIONS = 2_000;
    private static final int MAX_WINDOW = 64;
    private static final int MAX_CONSTRUCTION_FEATURES = 32;
    private static final int MAX_CONSTRUCTION_WEIGHT = 64;
    private static final int BITS = Long.SIZE;

    @Test
    void everyTiedPositionIsOneAndTheMajorityBitStandsElsewhere() {
        Random random = new Random(CONSTRUCTION_SEED);
        Construction count = new Construction();
        for (int trial = 0; trial < CONSTRUCTIONS; trial++) {
            int n = 1 + random.nextInt(MAX_CONSTRUCTION_FEATURES);
            long[] features = new long[n];
            int[] weights = new int[n];
            for (int i = 0; i < n; i++) {
                features[i] = random.nextLong();
                weights[i] = 1 + random.nextInt(MAX_CONSTRUCTION_WEIGHT);
            }
            assertTieConstruction(features, weights, maskShape(random, trial), count);
        }
        assertEquals(CONSTRUCTIONS * (long) BITS, count.tiedPositions + count.majorityPositions + count.accidentalTies);
        System.out.println("M3_SIMHASH_TIE_RULE_CONSTRUCTION constructions=" + CONSTRUCTIONS
                + " tied_positions_one=" + count.tiedPositions + " majority_positions=" + count.majorityPositions
                + " accidental_ties_one=" + count.accidentalTies + " seed=0x" + Long.toHexString(CONSTRUCTION_SEED));
    }

    @Test
    void complementPairTiesAllSixtyFourBitsToOneAndTheEmptyWindowStaysZero() {
        for (int weight : new int[] {1, 2, 7, 1 << 20, Integer.MAX_VALUE}) {
            long feature = 0x4d33_5349_4d48_4153L ^ weight;
            var pair = M3IndexDbChunkSimHash.fromFeatures(DOMAIN,
                    new long[] {feature, ~feature}, 0, new int[] {weight, weight}, 0, 2);
            assertTrue(Arrays.stream(pair.voteCopy()).allMatch(vote -> vote == 0L), "all 64 votes cancel");
            assertEquals(-1L, pair.simHash64(), "all 64 tied positions are 1, weight " + weight);
            assertEquals(2L, pair.featureCount());
            assertEquals(0L, pair.repeat(0).simHash64(), "featureCount 0 is the explicit 0, not 64 ties");
        }
        assertEquals(0L, M3IndexDbChunkSimHash.empty(DOMAIN).simHash64(), "empty identity is 0");
        assertEquals(0L, M3IndexDbChunkSimHash.fromFeatures(DOMAIN, new long[0], 0, new int[0], 0, 0).simHash64());
    }

    @Test
    void tenThousandSeededUnitWeightWindowsAgreeWithTheIndependentVoteCount() {
        Random random = new Random(WINDOW_SEED);
        Windows count = new Windows();
        for (int window = 0; window < WINDOWS; window++) {
            int n = random.nextInt(MAX_WINDOW + 1);
            long[] features = new long[n];
            for (int i = 0; i < n; i++) features[i] = random.nextLong();
            int[] ones = new int[n];
            Arrays.fill(ones, 1);
            long owner = M3IndexDbChunkSimHash.fromFeatures(DOMAIN, features, 0, ones, 0, n).simHash64();
            count.add(n, owner, referenceVotes(features, ones));
        }
        assertFigures(count);
        System.out.println("M3_SIMHASH_TIE_RULE_CONTRACT windows=" + WINDOWS + " untied=" + count.untied
                + " tied=" + count.tied + " empty=" + count.empty + " tied_bits=" + count.tiedBits
                + " agree_untied=" + count.agreeUntied + " agree_ties_to_one=" + count.agreeTiesToOne
                + " agree_ties_to_zero_untied_or_empty=" + count.agreeTiesToZeroUntiedOrEmpty
                + " differ_exactly_on_tiemask=" + count.differExactlyOnTieMask + " unexplained=" + count.unexplained
                + " seed=0x" + Long.toHexString(WINDOW_SEED));
    }

    private static void assertFigures(Windows count) {
        assertEquals(WINDOWS, count.untied + count.tied + count.empty, "every window classified once");
        assertEquals(count.untied, count.agreeUntied, "owner == reference on every untied window");
        assertEquals(WINDOWS, count.agreeTiesToOne, "owner == reference(ties -> 1) on every window");
        assertEquals(count.untied + count.empty, count.agreeTiesToZeroUntiedOrEmpty,
                "owner == reference(ties -> 0) exactly on the untied and empty windows");
        assertEquals(count.tied, count.differExactlyOnTieMask, "owner ^ reference(ties -> 0) == tie mask when tied");
        assertEquals(0L, count.unexplained, "no window outside the three classes");
        assertTrue(count.tied > 0 && count.untied > 0 && count.empty > 0, "all three classes exercised");
    }

    private static long maskShape(Random random, int trial) {
        return switch (trial % 5) {
            case 0 -> 0L;
            case 1 -> -1L;
            case 2 -> 1L << random.nextInt(BITS);
            case 3 -> random.nextLong() & random.nextLong();
            default -> random.nextLong();
        };
    }

    /** Mirrors the window through {@code mask}, proves the zero vote sums, then asserts the pinned signature. */
    private static void assertTieConstruction(long[] features, int[] weights, long mask, Construction count) {
        int n = features.length;
        long[] mirrored = new long[2 * n];
        int[] doubled = new int[2 * n];
        for (int i = 0; i < n; i++) {
            mirrored[i] = features[i];
            mirrored[n + i] = features[i] ^ mask;
            doubled[i] = weights[i];
            doubled[n + i] = weights[i];
        }
        var base = M3IndexDbChunkSimHash.fromFeatures(DOMAIN, features, 0, weights, 0, n);
        var mirror = M3IndexDbChunkSimHash.fromFeatures(DOMAIN, mirrored, n, doubled, n, n);
        var tied = M3IndexDbChunkSimHash.fromFeatures(DOMAIN, mirrored, 0, doubled, 0, 2 * n);
        assertVotes(base.voteCopy(), tied.voteCopy(), mask);
        assertSignature(base, tied, mask, count);
        assertEquals(tied.simHash64(), base.merge(mirror).simHash64(), "same rule through merge");
    }

    private static void assertVotes(long[] baseVotes, long[] tiedVotes, long mask) {
        for (int bit = 0; bit < BITS; bit++) {
            boolean inMask = ((mask >>> bit) & 1L) != 0L;
            assertEquals(inMask ? 0L : 2L * baseVotes[bit], tiedVotes[bit], "construction: vote of bit " + bit);
        }
    }

    private static void assertSignature(M3IndexDbChunkSimHash base, M3IndexDbChunkSimHash tied, long mask,
            Construction count) {
        long[] votes = tied.voteCopy();
        long signature = tied.simHash64();
        long tieMask = tieMaskOf(votes);
        assertEquals(mask, signature & mask, "every tied position is 1");
        assertEquals(base.simHash64() & ~mask, signature & ~mask, "majority bit elsewhere");
        assertEquals(base.simHash64() | mask, signature, "simHash64(W ++ (W ^ M)) == simHash64(W) | M");
        assertEquals(referenceTiesToOne(votes, tied.featureCount()), signature, "reference: ties -> 1");
        assertEquals(tieMask, signature ^ referenceTiesToZero(votes, tied.featureCount()),
                "differs from ties -> 0 exactly on the ties");
        assertEquals(mask, tieMask & mask, "the constructed ties are ties");
        for (int bit = 0; bit < BITS; bit++) {
            boolean set = ((signature >>> bit) & 1L) != 0L;
            if (votes[bit] != 0L) assertEquals(votes[bit] > 0L, set, "majority bit " + bit);
            count.record(((mask >>> bit) & 1L) != 0L, votes[bit]);
        }
    }

    /** Independent reference: the straightforward per-bit vote count, +w for a set bit and -w for a clear bit. */
    static long[] referenceVotes(long[] features, int[] weights) {
        long[] votes = new long[BITS];
        for (int bit = 0; bit < BITS; bit++) {
            for (int i = 0; i < features.length; i++) {
                votes[bit] += ((features[i] >>> bit) & 1L) != 0L ? weights[i] : -(long) weights[i];
            }
        }
        return votes;
    }

    /** The pinned rule of the owner: bit 1 on a non-negative vote (a tie votes 1); the empty window is 0. */
    static long referenceTiesToOne(long[] votes, long featureCount) {
        if (featureCount == 0) return 0L;
        long signature = 0L;
        for (int bit = 0; bit < BITS; bit++) if (votes[bit] >= 0L) signature |= 1L << bit;
        return signature;
    }

    /** The donor rule of com.synexia SimHash64.fingerprint (a tie votes 0), kept only to state the difference. */
    static long referenceTiesToZero(long[] votes, long featureCount) {
        if (featureCount == 0) return 0L;
        long signature = 0L;
        for (int bit = 0; bit < BITS; bit++) if (votes[bit] > 0L) signature |= 1L << bit;
        return signature;
    }

    /** The bits whose vote sum is exactly zero: where the two rules differ. */
    static long tieMaskOf(long[] votes) {
        long mask = 0L;
        for (int bit = 0; bit < BITS; bit++) if (votes[bit] == 0L) mask |= 1L << bit;
        return mask;
    }

    /** Position counts of the tie construction: constructed ties, majority positions, accidental ties of W. */
    private static final class Construction {
        long tiedPositions;
        long majorityPositions;
        long accidentalTies;

        void record(boolean constructed, long vote) {
            if (constructed) tiedPositions++;
            else if (vote != 0L) majorityPositions++;
            else accidentalTies++;
        }
    }

    /** Window classes and agreement counts of the seeded reference lane. */
    private static final class Windows {
        long untied;
        long tied;
        long empty;
        long tiedBits;
        long agreeUntied;
        long agreeTiesToOne;
        long agreeTiesToZeroUntiedOrEmpty;
        long differExactlyOnTieMask;
        long unexplained;

        void add(int n, long owner, long[] votes) {
            long tieMask = n == 0 ? 0L : tieMaskOf(votes);
            long toOne = referenceTiesToOne(votes, n);
            long toZero = referenceTiesToZero(votes, n);
            if (owner == toOne) agreeTiesToOne++;
            if (n == 0) {
                empty++;
                if (owner == toZero) agreeTiesToZeroUntiedOrEmpty++;
                else unexplained++;
            } else if (tieMask == 0L) {
                untied++;
                if (owner == toOne && owner == toZero) { agreeUntied++; agreeTiesToZeroUntiedOrEmpty++; }
                else unexplained++;
            } else {
                tied++;
                tiedBits += Long.bitCount(tieMask);
                if ((owner ^ toZero) == tieMask) differExactlyOnTieMask++;
                else unexplained++;
            }
        }
    }
}
