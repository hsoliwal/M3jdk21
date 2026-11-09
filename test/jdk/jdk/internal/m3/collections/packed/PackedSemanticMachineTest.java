// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PackedSemanticMachineTest {
    @Test
    void sixtyFourBySixtyFourRelationIsOnlyFiveHundredTwelvePayloadBytes() {
        PackedRelationMatrix matrix = new PackedRelationMatrix(64);
        assertEquals(64, matrix.payloadLongs());
        assertEquals(512L, matrix.payloadBytes());
    }

    @Test
    void relationClosureCompilesCausalChainsIntoOneRow() {
        PackedRelationMatrix matrix = new PackedRelationMatrix(64);
        matrix.relate(1, 2);
        matrix.relate(2, 3);
        matrix.relate(3, 4);

        matrix.transitiveClosureInPlace();

        assertTrue(matrix.relates(1, 2));
        assertTrue(matrix.relates(1, 3));
        assertTrue(matrix.relates(1, 4));
        assertFalse(matrix.relates(4, 1));
    }

    @Test
    void relationMatrixCrossesLongWordBoundaries() {
        PackedRelationMatrix matrix = new PackedRelationMatrix(130);
        matrix.relate(0, 64);
        matrix.relate(64, 129);
        matrix.transitiveClosureInPlace();

        assertTrue(matrix.relates(0, 64));
        assertTrue(matrix.relates(0, 129));
        assertEquals(3, matrix.wordsPerRow());
    }

    @Test
    void actionAdmissionLifecycleIsEntirelyPacked() {
        PackedSemanticMachine machine = PackedActionAdmissionUseCase.machine();
        PackedSemanticState state = PackedActionAdmissionUseCase.newState();

        machine.scalar(state, PackedActionAdmissionUseCase.CONFIDENCE, 220);
        machine.scalar(state, PackedActionAdmissionUseCase.PRIORITY, 230);

        assertTrue(state.active(PackedActionAdmissionUseCase.HIGH_CONFIDENCE));
        assertTrue(state.active(PackedActionAdmissionUseCase.HIGH_PRIORITY));
        assertFalse(state.active(PackedActionAdmissionUseCase.ADMITTED));

        machine.activate(state, PackedActionAdmissionUseCase.REQUESTED);
        machine.activate(state, PackedActionAdmissionUseCase.IDENTIFIED);
        machine.activate(state, PackedActionAdmissionUseCase.AUTHENTICATED);
        machine.activate(state, PackedActionAdmissionUseCase.POLICY_OK);
        machine.activate(state, PackedActionAdmissionUseCase.INPUT_READY);
        machine.activate(state, PackedActionAdmissionUseCase.RESOURCE_OK);

        assertTrue(state.active(PackedActionAdmissionUseCase.ADMITTED));

        machine.activate(state, PackedActionAdmissionUseCase.STARTED);
        assertTrue(state.active(PackedActionAdmissionUseCase.EXECUTING));

        // 185 is inside the 177..191 hysteresis dead band: previous HIGH_CONFIDENCE remains.
        machine.scalar(state, PackedActionAdmissionUseCase.CONFIDENCE, 185);
        assertTrue(state.active(PackedActionAdmissionUseCase.HIGH_CONFIDENCE));
        assertTrue(state.active(PackedActionAdmissionUseCase.EXECUTING));

        // Crossing the OFF threshold clears the derived fact and stops execution, but the
        // historical admission/start facts remain. Crossing ON again resumes mechanically.
        machine.scalar(state, PackedActionAdmissionUseCase.CONFIDENCE, 170);
        assertFalse(state.active(PackedActionAdmissionUseCase.HIGH_CONFIDENCE));
        assertFalse(state.active(PackedActionAdmissionUseCase.EXECUTING));
        assertTrue(state.active(PackedActionAdmissionUseCase.ADMITTED));
        assertTrue(state.active(PackedActionAdmissionUseCase.STARTED));

        machine.scalar(state, PackedActionAdmissionUseCase.CONFIDENCE, 200);
        assertTrue(state.active(PackedActionAdmissionUseCase.HIGH_CONFIDENCE));
        assertTrue(state.active(PackedActionAdmissionUseCase.EXECUTING));

        // Resource pressure derives PRESSURE_HIGH -> BLOCKED and stops execution.
        machine.scalar(state, PackedActionAdmissionUseCase.RESOURCE_PRESSURE, 240);
        assertTrue(state.active(PackedActionAdmissionUseCase.PRESSURE_HIGH));
        assertTrue(state.active(PackedActionAdmissionUseCase.BLOCKED));
        assertFalse(state.active(PackedActionAdmissionUseCase.EXECUTING));
        assertTrue(state.active(PackedActionAdmissionUseCase.ADMITTED));

        // Lower pressure removes the derived blocker and execution resumes mechanically.
        machine.scalar(state, PackedActionAdmissionUseCase.RESOURCE_PRESSURE, 100);
        assertFalse(state.active(PackedActionAdmissionUseCase.PRESSURE_HIGH));
        assertFalse(state.active(PackedActionAdmissionUseCase.BLOCKED));
        assertTrue(state.active(PackedActionAdmissionUseCase.EXECUTING));

        // Failure stops execution, emits ALERT, and is retryable while risk is low.
        machine.activate(state, PackedActionAdmissionUseCase.FAILED);
        assertTrue(state.active(PackedActionAdmissionUseCase.FAILED));
        assertTrue(state.active(PackedActionAdmissionUseCase.ALERT));
        assertTrue(state.active(PackedActionAdmissionUseCase.RETRYABLE));
        assertFalse(state.active(PackedActionAdmissionUseCase.EXECUTING));

        // High risk becomes a semantic blocker and suppresses retry.
        machine.scalar(state, PackedActionAdmissionUseCase.RISK, 230);
        assertTrue(state.active(PackedActionAdmissionUseCase.RISK_HIGH));
        assertTrue(state.active(PackedActionAdmissionUseCase.BLOCKED));
        assertFalse(state.active(PackedActionAdmissionUseCase.RETRYABLE));

        // Lowering risk makes the existing failure retryable again.
        machine.scalar(state, PackedActionAdmissionUseCase.RISK, 0);
        assertFalse(state.active(PackedActionAdmissionUseCase.RISK_HIGH));
        assertFalse(state.active(PackedActionAdmissionUseCase.BLOCKED));
        assertTrue(state.active(PackedActionAdmissionUseCase.RETRYABLE));

        // A newly activated terminal outcome wins over the older mutually exclusive outcome.
        machine.activate(state, PackedActionAdmissionUseCase.SUCCEEDED);
        assertTrue(state.active(PackedActionAdmissionUseCase.SUCCEEDED));
        assertFalse(state.active(PackedActionAdmissionUseCase.FAILED));
        assertFalse(state.active(PackedActionAdmissionUseCase.RETRYABLE));
        assertEquals(0, machine.violationCount(state));
    }

    @Test
    void invalidTransitionRollsBackAtomically() {
        PackedSemanticMachine machine = PackedActionAdmissionUseCase.machine();
        PackedSemanticState state = PackedActionAdmissionUseCase.newState();

        machine.activate(state, PackedActionAdmissionUseCase.REQUESTED);

        assertThrows(
                IllegalStateException.class,
                () -> machine.activate(state, PackedActionAdmissionUseCase.STARTED));

        assertTrue(state.active(PackedActionAdmissionUseCase.REQUESTED));
        assertFalse(state.active(PackedActionAdmissionUseCase.STARTED));

        long[] rejectedViolations = machine.newMask();
        machine.copyLastViolationsInto(rejectedViolations);
        assertTrue(PackedSemanticMasks.get(
                rejectedViolations, PackedActionAdmissionUseCase.ADMITTED));

        // The rolled-back state itself is valid; violation evidence belongs to the rejected request.
        assertEquals(0, machine.violationCount(state));
    }

    @Test
    void derivedFactsCannotBeMutatedExternally() {
        PackedSemanticMachine machine = PackedActionAdmissionUseCase.machine();
        PackedSemanticState state = PackedActionAdmissionUseCase.newState();

        assertThrows(
                IllegalArgumentException.class,
                () -> machine.activate(state, PackedActionAdmissionUseCase.ADMITTED));
        assertFalse(state.active(PackedActionAdmissionUseCase.ADMITTED));
    }

    @Test
    void compileFreezesConfigurationAndTrimsRuleCapacity() {
        PackedSemanticMachine machine = new PackedSemanticMachine(8, 1, 64);
        machine.addRule(
                machine.newMask(),
                machine.newMask(),
                machine.newMask(),
                machine.newMask(1),
                machine.newMask());

        long before = machine.rulePayloadBytes();
        machine.compile();
        long after = machine.rulePayloadBytes();

        assertTrue(machine.compiled());
        assertTrue(after < before);
        assertThrows(IllegalStateException.class, () -> machine.imply(1, 2));
        assertThrows(
                IllegalStateException.class,
                () -> machine.addRule(
                        machine.newMask(),
                        machine.newMask(),
                        machine.newMask(),
                        machine.newMask(),
                        machine.newMask()));
    }

    @Test
    void compiledReferenceProgramHasDeterministicPrimitivePayload() {
        PackedSemanticMachine machine = PackedActionAdmissionUseCase.machine();

        assertEquals(17, machine.ruleCount());
        assertEquals(608L, machine.relationPayloadBytes());
        assertEquals(799L, machine.rulePayloadBytes());
        assertEquals(1_487L, machine.machinePayloadBytes());
    }

    @Test
    void multiFactTransitionChecksGuardsAgainstSameTransactionClears() {
        PackedSemanticMachine machine = PackedActionAdmissionUseCase.machine();
        PackedSemanticState state = PackedActionAdmissionUseCase.newState();

        machine.scalar(state, PackedActionAdmissionUseCase.CONFIDENCE, 220);
        for (int fact : new int[] {
                PackedActionAdmissionUseCase.REQUESTED,
                PackedActionAdmissionUseCase.IDENTIFIED,
                PackedActionAdmissionUseCase.AUTHENTICATED,
                PackedActionAdmissionUseCase.POLICY_OK,
                PackedActionAdmissionUseCase.INPUT_READY,
                PackedActionAdmissionUseCase.RESOURCE_OK
        }) {
            machine.activate(state, fact);
        }
        assertTrue(state.active(PackedActionAdmissionUseCase.ADMITTED));

        long[] set = machine.newMask(PackedActionAdmissionUseCase.STARTED);
        long[] clear = machine.newMask(PackedActionAdmissionUseCase.ADMITTED);

        assertThrows(IllegalArgumentException.class, () -> machine.transition(state, set, clear));
        assertTrue(state.active(PackedActionAdmissionUseCase.ADMITTED));
        assertFalse(state.active(PackedActionAdmissionUseCase.STARTED));
    }

    @Test
    void sameFactCannotBeRequestedInSetAndClearMasks() {
        PackedSemanticMachine machine = PackedActionAdmissionUseCase.machine();
        PackedSemanticState state = machine.newState();
        long[] same = machine.newMask(PackedActionAdmissionUseCase.REQUESTED);

        assertThrows(
                IllegalArgumentException.class,
                () -> machine.transition(state, same, same));
        assertFalse(state.active(PackedActionAdmissionUseCase.REQUESTED));
    }

    @Test
    void scalarAndFactStateRollbackTogetherOnInvariantFailure() {
        PackedSemanticMachine machine = new PackedSemanticMachine(2, 1, 1);
        int required = 0;
        int derived = 1;

        machine.externallyMutable(derived, false);
        machine.require(derived, required);
        machine.addScalarRule(
                machine.newMask(),
                machine.newMask(),
                machine.newMask(),
                0,
                200,
                255,
                machine.newMask(derived),
                machine.newMask());
        machine.compile();

        PackedSemanticState state = machine.newState();

        assertThrows(IllegalStateException.class, () -> machine.scalar(state, 0, 255));
        assertEquals(0, state.scalarUnsigned(0));
        assertFalse(state.active(derived));

        long[] violations = machine.lastViolationsCopy();
        assertTrue(PackedSemanticMasks.get(violations, required));
    }

    @Test
    void oscillatingRuleProgramIsDetectedAndRolledBack() {
        PackedSemanticMachine machine = new PackedSemanticMachine(1, 0, 2);
        int flag = 0;

        machine.externallyMutable(flag, false);
        machine.addRule(
                machine.newMask(flag),
                machine.newMask(),
                machine.newMask(),
                machine.newMask(),
                machine.newMask(flag));
        machine.addRule(
                machine.newMask(),
                machine.newMask(),
                machine.newMask(flag),
                machine.newMask(flag),
                machine.newMask());
        machine.compile();

        PackedSemanticState state = machine.newState();

        assertThrows(IllegalStateException.class, () -> machine.recompute(state));
        assertFalse(state.active(flag));
    }

    @Test
    void stateCopyAndClearPreservePackedShape() {
        PackedSemanticState source = PackedActionAdmissionUseCase.newState();
        PackedSemanticState target = PackedActionAdmissionUseCase.newState();

        source.activate(PackedActionAdmissionUseCase.REQUESTED);
        source.scalarUnsigned(PackedActionAdmissionUseCase.CONFIDENCE, 201);
        target.copyFrom(source);

        assertTrue(target.active(PackedActionAdmissionUseCase.REQUESTED));
        assertEquals(201, target.scalarUnsigned(PackedActionAdmissionUseCase.CONFIDENCE));
        assertEquals(12L, target.payloadBytes());

        target.clear();
        assertEquals(0, target.activeCount());
        assertEquals(0, target.scalarUnsigned(PackedActionAdmissionUseCase.CONFIDENCE));
    }

    @Test
    void referenceProgramPrimitivePayloadMatchesGuideline() {
        PackedSemanticMachine machine = PackedActionAdmissionUseCase.machine();

        assertEquals(608L, machine.relationPayloadBytes());
        // Both merge-parent factories contain 17 rules, each retaining 47 primitive bytes.
        assertEquals(17, machine.ruleCount());
        assertEquals(799L, machine.rulePayloadBytes());
    }

    @Test
    void actionAdmissionStatePayloadIsTwelveBytes() {
        PackedSemanticState state = PackedActionAdmissionUseCase.newState();

        // 19 facts fit in one long; four scalar channels fit in four bytes.
        assertEquals(1, state.wordCount());
        assertEquals(4, state.scalarCount());
        assertEquals(12L, state.wordCount() * 8L + state.scalarCount());
    }
}
