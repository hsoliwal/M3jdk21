// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import java.util.SplittableRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The CPU-list parser of {@link NumaTopology} (recipe make/m3/optane/recipe/pmem-accelerator-20261107, census L047)
 * against (1) the hand-derived table of the donor SelfTest (its eleven parseCpuList checks, the only part of that
 * 465-line self-test that runs without the accelerator jmod family), (2) a canonical-formatter round trip over
 * seeded random CPU sets, and (3) a regex reference grammar over seeded random short strings, accept/refuse and set
 * compared. The parser is a pure Java atom; no native library and no NUMA host is needed.
 */
//FLLDCIM: RELOCATE - M3JDK-authored reference model of recipe make/m3/optane/recipe/pmem-accelerator-20261107 (census L047); the donor SelfTest checks are the hand table below
final class NumaTopologyCpuListTest {
    private static final int LIMIT = 65535;
    /** The reference grammar: elements of an optional +, digits, an optional -+digits range; ASCII trim; commas. */
    private static final Pattern ELEMENT = Pattern.compile("[\\x00- ]*\\+?([0-9]+)(?:-\\+?([0-9]+))?[\\x00- ]*");

    private static BitSet set(int... cpus) {
        BitSet out = new BitSet();
        for (int cpu : cpus) out.set(cpu);
        return out;
    }

    @Test void donorSelfTestTable() {
        assertEquals(7, NumaTopology.parseCpuList("0-3,7,10-11").cardinality());
        assertEquals(set(0, 1, 2, 3, 7, 10, 11), NumaTopology.parseCpuList("0-3,7,10-11"));
        assertEquals(65536, NumaTopology.parseCpuList("0-65535").cardinality());
        assertEquals(3, NumaTopology.parseCpuList("+1,1-3").cardinality());
        assertEquals(set(1, 2, 3), NumaTopology.parseCpuList("+1,1-3"));
        for (String invalid : new String[] {"1-", "1,", ",", "1,,2", "2-1", "65536", "0-65536", "1--2"}) {
            assertThrows(IllegalArgumentException.class, () -> NumaTopology.parseCpuList(invalid), invalid);
        }
    }

    @Test void edgeTable() {
        assertEquals(new BitSet(), NumaTopology.parseCpuList(null));
        assertEquals(new BitSet(), NumaTopology.parseCpuList(""));
        assertEquals(new BitSet(), NumaTopology.parseCpuList("   "));
        assertEquals(set(5), NumaTopology.parseCpuList("5"));
        assertEquals(set(5), NumaTopology.parseCpuList(" 5 "));
        assertEquals(set(5), NumaTopology.parseCpuList("\t5\n"));
        assertEquals(set(7), NumaTopology.parseCpuList("007"));
        assertEquals(set(2, 3, 9), NumaTopology.parseCpuList("2-3 , 9"));
        assertEquals(set(4), NumaTopology.parseCpuList("4-4"));
        assertEquals(set(1, 2), NumaTopology.parseCpuList("1-+2"));
        assertEquals(set(LIMIT), NumaTopology.parseCpuList("65535"));
        for (String invalid : new String[] {"0 - 2", "-1", "+", "1-+", "a", "1a", "1,a", "65536-65537", "99999", " , "}) {
            assertThrows(IllegalArgumentException.class, () -> NumaTopology.parseCpuList(invalid), invalid);
        }
    }

    /** Canonical "a-b,c" text of a set, with random spaces around commas and random leading pluses. */
    private static String format(BitSet cpus, SplittableRandom random) {
        StringBuilder text = new StringBuilder();
        int from = cpus.nextSetBit(0);
        while (from >= 0) {
            int to = cpus.nextClearBit(from) - 1;
            if (!text.isEmpty()) text.append(random.nextInt(4) == 0 ? " , " : ",");
            if (random.nextInt(8) == 0) text.append('+');
            text.append(from);
            if (to > from) text.append('-').append(to);
            from = cpus.nextSetBit(to + 1);
        }
        return text.toString();
    }

    @Test void formatterRoundTrip() {
        SplittableRandom random = new SplittableRandom(0x4E554D41_20261107L);
        int sets = 0;
        for (int trial = 0; trial < 4000; trial++) {
            BitSet cpus = new BitSet();
            int runs = random.nextInt(6);
            for (int run = 0; run < runs; run++) {
                int start = random.nextInt(LIMIT + 1);
                int length = random.nextInt(random.nextInt(4) == 0 ? 2000 : 8);
                cpus.set(start, Math.min(LIMIT, start + length) + 1);
            }
            BitSet parsed = NumaTopology.parseCpuList(format(cpus, random));
            assertEquals(cpus, parsed, "trial " + trial);
            sets++;
        }
        assertEquals(4000, sets);
    }

    /** The regex reference: null for a refusal, else the set; independent of the one-pass parser. */
    private static BitSet reference(String cpus) {
        if (cpus.isBlank()) return new BitSet();
        BitSet out = new BitSet();
        for (String element : cpus.split(",", -1)) {
            Matcher matcher = ELEMENT.matcher(element);
            if (!matcher.matches()) return null;
            long low = Long.parseLong(matcher.group(1));
            long high = matcher.group(2) == null ? low : Long.parseLong(matcher.group(2));
            if (low > LIMIT || high > LIMIT || high < low) return null;
            out.set((int) low, (int) high + 1);
        }
        return out;
    }

    @Test void referenceGrammarFuzz() {
        char[] alphabet = {'0', '1', '5', '6', '9', ',', '-', '+', ' ', 'x'};
        SplittableRandom random = new SplittableRandom(0x43505520_4C495354L);
        int accepted = 0, refused = 0;
        for (int trial = 0; trial < 200_000; trial++) {
            int length = random.nextInt(8);
            StringBuilder text = new StringBuilder(length);
            for (int i = 0; i < length; i++) text.append(alphabet[random.nextInt(alphabet.length)]);
            String cpus = text.toString();
            BitSet expected = reference(cpus);
            BitSet actual;
            try {
                actual = NumaTopology.parseCpuList(cpus);
            } catch (IllegalArgumentException refusal) {
                actual = null;
            }
            assertEquals(expected, actual, "input " + cpus.replace(' ', '_'));
            if (expected == null) refused++; else accepted++;
        }
        assertTrue(accepted > 10_000 && refused > 10_000, "accepted=" + accepted + " refused=" + refused);
    }
}
