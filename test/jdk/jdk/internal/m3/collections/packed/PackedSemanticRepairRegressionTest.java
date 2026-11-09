// SPDX-License-Identifier: Apache-2.0

package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PackedSemanticRepairRegressionTest {
    @Test
    void scalarOnlyRuleDoesNotRequireAnAnyFact() {
        PackedRuleProgram rules = scalarRules(new long[] {0});
        long[] set = {0};
        rules.evaluate(new long[] {0}, new byte[] {(byte) 200}, set, new long[] {0});
        assertEquals(2L, set[0]);
    }

    @Test
    void scalarRuleWithAnyMaskStillRequiresAMatchingFact() {
        PackedRuleProgram rules = scalarRules(new long[] {1});
        long[] set = {0};
        rules.evaluate(new long[] {0}, new byte[] {(byte) 200}, set, new long[] {0});
        assertEquals(0L, set[0]);
        rules.evaluate(new long[] {1}, new byte[] {(byte) 200}, set, new long[] {0});
        assertEquals(2L, set[0]);
    }

    @Test
    void scalarRangeRemainsInclusive() {
        PackedRuleProgram rules = scalarRules(new long[] {0});
        IntStream.of(99, 100, 200, 201).forEach(value -> checkRange(rules, value));
    }

    @Test
    void ratioRetainsNearestRoundingAcrossAllHalfBoundaries() {
        IntStream.range(0, 255).forEach(PackedSemanticRepairRegressionTest::checkHalf);
    }

    @Test
    void ratioRetainsNearestRoundingForLargeValues() {
        assertEquals(255, PackedU8.ratio(Long.MAX_VALUE - 1, Long.MAX_VALUE));
        assertEquals(128, PackedU8.ratio(1, 2));
        assertEquals(127, PackedU8.ratio(Long.MAX_VALUE / 2, Long.MAX_VALUE));
        assertEquals(128, PackedU8.ratio(Long.MAX_VALUE / 2 + 1, Long.MAX_VALUE));
        IntStream.range(0, 10_000).forEach(PackedSemanticRepairRegressionTest::checkRandomRatio);
    }

    @Test
    void ratioBoundaryValidationAndSaturationArePreserved() {
        assertEquals(0, PackedU8.ratio(Long.MIN_VALUE, 1));
        assertEquals(0, PackedU8.ratio(0, Long.MAX_VALUE));
        assertEquals(255, PackedU8.ratio(Long.MAX_VALUE, 1));
        assertEquals(255, PackedU8.ratio(Long.MAX_VALUE, Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> PackedU8.ratio(1, 0));
        assertThrows(IllegalArgumentException.class, () -> PackedU8.ratio(0, -1));
    }

    @Test
    void mutablePrerequisiteClearedInSameTransactionRejectsAtomically() {
        PackedSemanticMachine machine = guardedMachine();
        PackedSemanticState state = machine.newState();
        state.activate(0);
        assertThrows(IllegalStateException.class,
                () -> machine.transition(state, machine.newMask(1), machine.newMask(0)));
        assertTrue(state.active(0));
        assertFalse(state.active(1));
        assertTrue(PackedSemanticMasks.get(machine.lastViolationsCopy(), 0));
    }

    @Test
    void mutablePrerequisiteSetInSameTransactionSatisfiesGuard() {
        PackedSemanticMachine machine = guardedMachine();
        PackedSemanticState state = machine.newState();
        machine.transition(state, machine.newMask(0, 1), machine.newMask());
        assertTrue(state.active(0));
        assertTrue(state.active(1));
    }

    @Test
    void immutableOwnershipRejectionStillPrecedesGuardRejection() {
        PackedSemanticMachine machine = new PackedSemanticMachine(2, 0);
        machine.guard(1, 0);
        machine.externallyMutable(0, false);
        machine.compile();
        PackedSemanticState state = machine.newState();
        state.activate(0);
        assertThrows(IllegalArgumentException.class,
                () -> machine.transition(state, machine.newMask(1), machine.newMask(0)));
        assertTrue(state.active(0));
        assertFalse(state.active(1));
    }

    @Test
    void referenceConstantInventoryHasOneDeclarationPerContract() {
        assertEquals(33, PackedActionAdmissionUseCase.class.getFields().length);
        assertEquals(19, PackedActionAdmissionUseCase.FACT_COUNT);
        assertEquals(4, PackedActionAdmissionUseCase.SCALAR_COUNT);
        IntStream.range(0, 19).forEach(fact ->
                assertFalse(PackedActionAdmissionUseCase.factName(fact).isBlank()));
    }

    private static PackedRuleProgram scalarRules(long[] any) {
        PackedRuleProgram rules = new PackedRuleProgram(2, 1, 1);
        rules.addScalarRule(new long[] {0}, any, new long[] {0}, 0, 100, 200,
                new long[] {2}, new long[] {0});
        return rules;
    }

    private static void checkRange(PackedRuleProgram rules, int value) {
        long[] set = {0};
        rules.evaluate(new long[] {0}, new byte[] {(byte) value}, set, new long[] {0});
        assertEquals(value >= 100 && value <= 200 ? 2L : 0L, set[0]);
    }

    private static void checkHalf(int floor) {
        long midpoint = 4L * floor + 2;
        assertEquals(floor, PackedU8.ratio(midpoint - 1, 1020));
        assertEquals(floor + 1, PackedU8.ratio(midpoint, 1020));
        assertEquals(floor + 1, PackedU8.ratio(midpoint + 1, 1020));
    }

    private static void checkRandomRatio(int seed) {
        SplittableRandom random = new SplittableRandom(seed);
        long denominator = random.nextLong(1, Long.MAX_VALUE);
        long numerator = random.nextLong(denominator);
        BigInteger scaledTwice = BigInteger.valueOf(numerator).multiply(BigInteger.valueOf(510));
        BigInteger divisor = BigInteger.valueOf(denominator);
        int expected = scaledTwice.add(divisor).divide(divisor.shiftLeft(1)).intValueExact();
        assertEquals(expected, PackedU8.ratio(numerator, denominator));
    }

    private static PackedSemanticMachine guardedMachine() {
        PackedSemanticMachine machine = new PackedSemanticMachine(2, 0);
        machine.guard(1, 0);
        machine.compile();
        return machine;
    }
}
