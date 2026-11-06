// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.tooling.atomjit.M3OpenJdkAtomJitPatternGate.Phase;
import jdk.m3.tooling.atomjit.M3OpenJdkAtomJitPatternGate.Receipt;
import jdk.m3.tooling.atomjit.M3OpenJdkAtomJitPatternGate.Status;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-m3-recipe/src/test/java/com/synexia/m3/recipe/M3OpenJdkAtomJitPatternGateTest.java blob a121d76f8fa7; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
final class M3OpenJdkAtomJitPatternGateTest {

    @Test
    void requiresSerialJavaAndNativeProofBeforePatternization() {
        var plan = plan(true);
        var receipts = complete(plan);
        var reviewed = M3OpenJdkAtomJitPatternGate.review(plan, receipts);
        assertEquals(2, reviewed.atoms());
        assertTrue(reviewed.patternizeReady());
        assertTrue(reviewed.structurallyQualified());
        assertTrue(reviewed.blockers().isEmpty());
        assertFalse(reviewed.promotionAuthority());
        assertFalse(reviewed.independentlyVerified());
        assertTrue(reviewed.root().matches("[0-9a-f]{64}"));
        assertEquals(reviewed.root(), M3OpenJdkAtomJitPatternGate.review(plan, receipts).root());
    }

    @Test
    void rejectsMixedJdkBinariesAcrossJavaAndNativeAtomsEvenWithLocalC2Parity() {
        var plan = plan(true);
        var original = complete(plan);
        assertTrue(M3OpenJdkAtomJitPatternGate.review(plan, original)
                .structurallyQualified());

        ArrayList<Receipt> forged = new ArrayList<>(original);
        int second = Phase.values().length;
        for (Phase phase : new Phase[]{
                Phase.JIT_XINT, Phase.JIT_C2, Phase.JNI_PARITY}) {
            int index = second + phase.ordinal();
            Receipt row = forged.get(index);
            if (row.status() != Status.PASS) continue;
            forged.set(index, new Receipt(row.path(), row.atomId(),
                    row.beforeEnvelopeRoot(), phase, Status.PASS,
                    row.proofRoot(), sha('7'), row.behaviorRoot(),
                    row.compiledMethodObserved()));
        }
        var mismatch = assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkAtomJitPatternGate.review(plan, forged));
        assertTrue(mismatch.getMessage().contains("mixed JVM binaries"));
    }

    @Test
    void javaOnlyPlanRequiresExplicitReviewedJniNotApplicable() {
        var plan = plan(false);
        var rows = complete(plan);
        assertTrue(M3OpenJdkAtomJitPatternGate.review(plan, rows).structurallyQualified());
        ArrayList<Receipt> corrupt = new ArrayList<>(rows);
        corrupt.set(Phase.JNI_PARITY.ordinal(),
                receipt(plan.targets().getFirst(), Phase.JNI_PARITY, Status.PASS, sha('c'), true));
        assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkAtomJitPatternGate.review(plan, corrupt));
    }

    @Test
    void jitHoldBlocksPatternizationWithoutFakingNativeCompletion() {
        var plan = plan(false);
        ArrayList<Receipt> rows = new ArrayList<>(complete(plan));
        var target = plan.targets().getFirst();
        rows.set(Phase.JIT_C2.ordinal(), receipt(target, Phase.JIT_C2, Status.HOLD, "-", false));
        for (int index = Phase.PATTERNIZE.ordinal(); index < Phase.values().length; index++) {
            rows.set(index, receipt(target, Phase.values()[index], Status.NOT_RUN, "-", false));
        }
        var review = M3OpenJdkAtomJitPatternGate.review(plan, rows);
        assertFalse(review.patternizeReady());
        assertFalse(review.structurallyQualified());
        assertTrue(review.blockers().stream().anyMatch(row -> row.contains("JIT_C2:HOLD")));
    }

    @Test
    void refusesPrematurePatternPassAndIncorrectJavaNativeDigest() {
        var plan = plan(true);
        var rows = complete(plan);
        ArrayList<Receipt> premature = new ArrayList<>(rows);
        var first = plan.targets().getFirst();
        premature.set(Phase.JIT_C2.ordinal(),
                receipt(first, Phase.JIT_C2, Status.HOLD, "-", false));
        assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkAtomJitPatternGate.review(plan, premature));

        ArrayList<Receipt> mismatch = new ArrayList<>(rows);
        mismatch.set(Phase.JIT_C2.ordinal(), new Receipt(
                first.logicalPath(), first.atomId(), first.beforeEnvelopeRoot(),
                Phase.JIT_C2, Status.PASS, sha('c'), sha('d'), sha('f'), true));
        assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkAtomJitPatternGate.review(plan, mismatch));

        ArrayList<Receipt> nativeMismatch = new ArrayList<>(rows);
        nativeMismatch.set(Phase.JNI_PARITY.ordinal(), new Receipt(
                first.logicalPath(), first.atomId(), first.beforeEnvelopeRoot(),
                Phase.JNI_PARITY, Status.PASS, sha('c'), sha('f'), sha('e'), false));
        var jdkMismatch = nativeMismatch;
        assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkAtomJitPatternGate.review(plan, jdkMismatch));
    }

    @Test
    void refusesFalseC2ObservationNativeWaiverSourceDriftAndMissingPhase() {
        var plan = plan(true);
        var rows = complete(plan);
        var first = plan.targets().getFirst();
        assertThrows(IllegalArgumentException.class, () -> new Receipt(
                first.logicalPath(), first.atomId(), first.beforeEnvelopeRoot(),
                Phase.JIT_C2, Status.PASS, sha('c'), sha('d'), sha('e'), false));

        ArrayList<Receipt> waiver = new ArrayList<>(rows);
        waiver.set(Phase.JNI_PARITY.ordinal(),
                receipt(first, Phase.JNI_PARITY, Status.NOT_APPLICABLE, sha('c'), false));
        assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkAtomJitPatternGate.review(plan, waiver));

        ArrayList<Receipt> drift = new ArrayList<>(rows);
        drift.set(0, new Receipt(first.logicalPath(), first.atomId(), sha('f'),
                Phase.ATOMIZE, Status.PASS, sha('c'), "-", "-", false));
        assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkAtomJitPatternGate.review(plan, drift));

        ArrayList<Receipt> missing = new ArrayList<>(rows);
        missing.removeLast();
        assertThrows(IllegalArgumentException.class,
                () -> M3OpenJdkAtomJitPatternGate.review(plan, missing));
        assertNotEquals(plan.root(), M3OpenJdkAtomJitPatternGate.review(plan, rows).root());
    }

    private static M3OpenJdkAtomJitPatternGate.Plan plan(boolean nativeRequired) {
        ArrayList<M3OpenJdkAtomJitPatternGate.Target> targets = new ArrayList<>();
        targets.add(new M3OpenJdkAtomJitPatternGate.Target(
                "src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java",
                "jdk_mask_compactor", sha('a'), M3OpenJdkAtomJitPatternGate.SourceLane.JAVA));
        if (nativeRequired) targets.add(new M3OpenJdkAtomJitPatternGate.Target(
                "m3/collections/src/main/native/collections/m3_index_mask_compactor.c",
                "jni_index_mask_compactor", sha('b'),
                M3OpenJdkAtomJitPatternGate.SourceLane.JNI_NATIVE));
        // the donor planner's plan(...) canonical order: logical path, then atom id, then envelope root
        targets.sort(Comparator.comparing(M3OpenJdkAtomJitPatternGate.Target::logicalPath)
                .thenComparing(M3OpenJdkAtomJitPatternGate.Target::atomId)
                .thenComparing(M3OpenJdkAtomJitPatternGate.Target::beforeEnvelopeRoot));
        return new M3OpenJdkAtomJitPatternGate.Plan(sha('1'), targets);
    }

    private static List<Receipt> complete(M3OpenJdkAtomJitPatternGate.Plan plan) {
        ArrayList<Receipt> rows = new ArrayList<>();
        for (var target : plan.targets()) {
            for (Phase phase : Phase.values()) {
                Status status = phase == Phase.JNI_PARITY
                        && target.lane() != M3OpenJdkAtomJitPatternGate.SourceLane.JNI_NATIVE
                        ? Status.NOT_APPLICABLE : Status.PASS;
                rows.add(receipt(target, phase, status, sha('c'), true));
            }
        }
        return List.copyOf(rows);
    }

    private static Receipt receipt(M3OpenJdkAtomJitPatternGate.Target target,
            Phase phase, Status status, String witness, boolean compiled) {
        boolean runtime = status == Status.PASS
                && (phase == Phase.JIT_XINT || phase == Phase.JIT_C2
                        || phase == Phase.JNI_PARITY);
        return new Receipt(target.logicalPath(), target.atomId(), target.beforeEnvelopeRoot(),
                phase, status, witness, runtime ? sha('d') : "-",
                runtime ? (phase == Phase.JNI_PARITY ? sha('9') : sha('e')) : "-",
                compiled && phase == Phase.JIT_C2 && status == Status.PASS);
    }

    private static String sha(char digit) {
        return String.valueOf(digit).repeat(64);
    }
}
