// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Negative-assertion wrapper of the tie-order mutant {@link M3HammingTopKTieOrderMutant} (the owner with the tie
 * order of {@code worse()} reversed, written into the test tree by the MUTANT and SUBST rows of the recipe map).
 * The oracle {@link M3HammingTopKContract} is bound to the owner, so this test is how the mutant stays out of the
 * green run while the run still proves the oracle's teeth: on the oracle's first all-tie input (trial 0: n=2048,
 * every candidate equal to the query, k=2) the owner returns indexes 0,1 in (distance asc, index asc) order and the
 * mutant returns them reversed, which is exactly the "topk oracle failure k=2 n=2048" the donor crate documents;
 * on a tie-free input the mutant agrees with the owner, so the mutation is a tie-order mutation only.
 * differential.sh additionally compiles the oracle renamed onto the mutant (scratch only) and expects that failure.
 */
//FLLDCIM: TOP - negative control of the oracle; recipe m3/indexdb/recipe/hamming-topk-20261106 (MUTANT + SUBST rows)
final class M3HammingTopKTieOrderMutantTest {
    private static final long ORACLE_SEED = 0x4d33544845415053L;
    private static final int ORACLE_FIRST_N = 2048;

    @Test
    void mutantReversesTheTieOrderOnTheOraclesFirstAllTieInput() {
        long query = new Random(ORACLE_SEED).nextLong();
        long[] candidates = new long[ORACLE_FIRST_N];
        Arrays.fill(candidates, query);
        M3HammingTopK.Match[] owner = M3HammingTopK.nearest(query, candidates, 2);
        M3HammingTopKTieOrderMutant.Match[] mutant = M3HammingTopKTieOrderMutant.nearest(query, candidates, 2);
        int[] ownerIndexes = Arrays.stream(owner).mapToInt(M3HammingTopK.Match::index).toArray();
        int[] mutantIndexes = Arrays.stream(mutant).mapToInt(M3HammingTopKTieOrderMutant.Match::index).toArray();
        assertArrayEquals(new int[] {0, 1}, ownerIndexes, "owner: distance asc, index asc");
        assertArrayEquals(new int[] {1, 0}, mutantIndexes, "mutant: tie order reversed (oracle failure k=2 n=2048)");
        assertFalse(Arrays.equals(ownerIndexes, mutantIndexes));
        assertEquals(0, owner[0].distance());
        assertEquals(0, mutant[0].distance());
        assertArrayEquals(ownerIndexes, Arrays.stream(M3HammingTopK.fromDistances(new int[ORACLE_FIRST_N], candidates, 2))
                .mapToInt(M3HammingTopK.Match::index).toArray(), "owner prepared path agrees with its streaming path");
    }

    @Test
    void mutantAgreesWithTheOwnerWithoutTies() {
        long[] candidates = new long[64];
        for (int i = 0; i < candidates.length; i++) candidates[i] = (1L << i) - 1;
        for (int k : new int[] {0, 1, 7, 63, 64, 70}) {
            M3HammingTopK.Match[] owner = M3HammingTopK.nearest(0L, candidates, k);
            M3HammingTopKTieOrderMutant.Match[] mutant = M3HammingTopKTieOrderMutant.nearest(0L, candidates, k);
            assertArrayEquals(Arrays.stream(owner).mapToInt(M3HammingTopK.Match::index).toArray(),
                    Arrays.stream(mutant).mapToInt(M3HammingTopKTieOrderMutant.Match::index).toArray(), "k=" + k);
            assertArrayEquals(Arrays.stream(owner).mapToInt(M3HammingTopK.Match::distance).toArray(),
                    Arrays.stream(mutant).mapToInt(M3HammingTopKTieOrderMutant.Match::distance).toArray(), "k=" + k);
            for (int i = 0; i < owner.length; i++) assertEquals(i, owner[i].distance(), "distinct distances 0..63");
        }
    }
}
