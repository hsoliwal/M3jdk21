// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

//FLLDCIM: TOP - M3JDK-authored reference oracle of recipe/bit-ops-fold (the donors carry no tests): every folded bit
// kernel against a bit-by-bit formulation on random words and all positions
final class BitOpsReferenceTest {
    private static final int TRIALS = 2_000;

    private static int bit(int word, int position) {
        return (word >>> position) & 1;
    }

    @Test
    void positionHelpersAgreeWithBitByBitFormulation() {
        Random random = new Random(0x51BA7F00DL);
        for (int trial = 0; trial < TRIALS; trial++) {
            int word = random.nextInt();
            int position = random.nextInt(Integer.SIZE);
            int set = BitAlgorithms.setBit(word, position);
            int cleared = BitAlgorithms.clearBit(word, position);
            int toggled = BitAlgorithms.toggleBit(word, position);
            int value = random.nextInt(2);
            int updated = BitAlgorithms.updateBit(word, position, value);
            int lowCleared = BitAlgorithms.clearMsbToI(word, position);
            int highCleared = BitAlgorithms.clearIToLsb(word, position);
            assertEquals(bit(word, position) == 1, BitAlgorithms.isSet(word, position));
            assertEquals(bit(word, position), BitAlgorithms.getBit(word, position));
            for (int other = 0; other < Integer.SIZE; other++) {
                int expected = bit(word, other);
                assertEquals(other == position ? 1 : expected, bit(set, other), "setBit");
                assertEquals(other == position ? 0 : expected, bit(cleared, other), "clearBit");
                assertEquals(other == position ? 1 - expected : expected, bit(toggled, other), "toggleBit");
                assertEquals(other == position ? value : expected, bit(updated, other), "updateBit");
                assertEquals(other >= position ? 0 : expected, bit(lowCleared, other), "clearMsbToI");
                assertEquals(other <= position ? 0 : expected, bit(highCleared, other), "clearIToLsb");
            }
        }
    }

    @Test
    void powerOfTwoHelpersAgreeWithSearch() {
        Random random = new Random(0x2E0F7);
        for (int trial = 0; trial < TRIALS; trial++) {
            int word = 1 + random.nextInt(1 << 30);
            int next = 1;
            while (next < word) {
                next <<= 1;
            }
            int previous = next == word ? word : next >>> 1;
            assertEquals(next, BitAlgorithms.nextPowerOfTwo(word), "nextPowerOfTwo");
            assertEquals(next, BitAlgorithms.roundUpPow2(word), "roundUpPow2");
            assertEquals(previous, BitAlgorithms.prevPowerOfTwo(word), "prevPowerOfTwo");
            int highest = Integer.SIZE - 1;
            while (bit(word, highest) == 0) {
                highest--;
            }
            assertEquals(highest, BitAlgorithms.highestSetBit(word), "highestSetBit");
        }
        assertEquals(1, BitAlgorithms.nextPowerOfTwo(0));
        assertEquals(1, BitAlgorithms.nextPowerOfTwo(-5));
    }

    @Test
    void rightmostBitTricksAndHammingDistanceAgreeWithBitLoops() {
        Random random = new Random(0x7A1C);
        for (int trial = 0; trial < TRIALS; trial++) {
            int word = random.nextInt();
            int other = random.nextInt();
            int lowestOne = -1;
            int lowestZero = -1;
            int distance = 0;
            for (int position = 0; position < Integer.SIZE; position++) {
                if (lowestOne < 0 && bit(word, position) == 1) {
                    lowestOne = position;
                }
                if (lowestZero < 0 && bit(word, position) == 0) {
                    lowestZero = position;
                }
                distance += bit(word, position) ^ bit(other, position);
            }
            int oneMask = lowestOne < 0 ? 0 : 1 << lowestOne;
            int zeroMask = lowestZero < 0 ? 0 : 1 << lowestZero;
            assertEquals(oneMask, BitAlgorithms.isolateRightmost1(word), "isolateRightmost1");
            assertEquals(word & ~oneMask, BitAlgorithms.turnOffRightmost1(word), "turnOffRightmost1");
            assertEquals(zeroMask, BitAlgorithms.isolateRightmost0(word), "isolateRightmost0");
            assertEquals(word | zeroMask, BitAlgorithms.turnOnRightmost0(word), "turnOnRightmost0");
            assertEquals(distance, BitAlgorithms.hammingDistance(word, other), "hammingDistance");
        }
        assertTrue(BitAlgorithms.isSet(BitAlgorithms.turnOnRightmost0(0), 0));
        assertFalse(BitAlgorithms.isSet(BitAlgorithms.turnOffRightmost1(1), 0));
    }
}
