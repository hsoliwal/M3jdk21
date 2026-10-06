// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.Objects;

/**
 * Frozen Q32 probability mass by formula and four-valued truth state.
 *
 * <p>The source probability vector remains probability authority and the formula-truth image
 * remains formula/model truth authority. This class is only one concrete primitive aggregation
 * implementation and does not claim ownership of probabilistic reasoning.</p>
 */
public final class M3FormulaProbabilityIndex {
    private final M3FormulaTruthIndex truth;
    private final M3ProbabilityVector probabilities;
    private final long[] formulaIds;
    private final long[] trueMassQ32;
    private final long[] falseMassQ32;
    private final long[] bothMassQ32;
    private final long[] unknownMassQ32;

    private M3FormulaProbabilityIndex(
            M3FormulaTruthIndex truth,
            M3ProbabilityVector probabilities,
            long[] formulaIds,
            long[] trueMassQ32,
            long[] falseMassQ32,
            long[] bothMassQ32,
            long[] unknownMassQ32) {
        this.truth = truth;
        this.probabilities = probabilities;
        this.formulaIds = formulaIds;
        this.trueMassQ32 = trueMassQ32;
        this.falseMassQ32 = falseMassQ32;
        this.bothMassQ32 = bothMassQ32;
        this.unknownMassQ32 = unknownMassQ32;
    }

    public static M3FormulaProbabilityIndex compile(
            M3FormulaTruthIndex truth,
            M3ProbabilityVector probabilities) {
        M3FormulaTruthIndex checkedTruth = Objects.requireNonNull(truth, "truth");
        M3ProbabilityVector checkedProbabilities =
                Objects.requireNonNull(probabilities, "probabilities");

        if (!checkedTruth.modelSpace().equals(checkedProbabilities.coordinateSpace())) {
            throw new IllegalArgumentException(
                    "probability vector and truth image use different model coordinate spaces");
        }

        long[] formulaIds = checkedTruth.formulaIds();
        long[] models = checkedTruth.modelIds();
        long[] trueMass = new long[formulaIds.length];
        long[] falseMass = new long[formulaIds.length];
        long[] bothMass = new long[formulaIds.length];
        long[] unknownMass = new long[formulaIds.length];

        long[] modelMass = new long[models.length];
        for (int modelRow = 0; modelRow < models.length; modelRow++) {
            modelMass[modelRow] = checkedProbabilities.massQ32(models[modelRow]);
        }
        for (int probabilityRow = 0;
                probabilityRow < checkedProbabilities.size();
                probabilityRow++) {
            long key = checkedProbabilities.keyAt(probabilityRow);
            if (checkedTruth.modelRow(key) < 0) {
                throw new IllegalArgumentException(
                        "probability vector contains model outside truth image");
            }
        }

        for (int formulaRow = 0; formulaRow < formulaIds.length; formulaRow++) {
            for (int modelRow = 0; modelRow < models.length; modelRow++) {
                long mass = modelMass[modelRow];
                switch (checkedTruth.truthAt(formulaRow, modelRow)) {
                    case TRUE ->
                            trueMass[formulaRow] = Math.addExact(trueMass[formulaRow], mass);
                    case FALSE ->
                            falseMass[formulaRow] = Math.addExact(falseMass[formulaRow], mass);
                    case BOTH ->
                            bothMass[formulaRow] = Math.addExact(bothMass[formulaRow], mass);
                    case UNKNOWN ->
                            unknownMass[formulaRow] =
                                    Math.addExact(unknownMass[formulaRow], mass);
                }
            }
            long total =
                    Math.addExact(
                            Math.addExact(trueMass[formulaRow], falseMass[formulaRow]),
                            Math.addExact(bothMass[formulaRow], unknownMass[formulaRow]));
            if (!checkedProbabilities.isEmpty() && total != M3ProbabilityVector.ONE_Q32) {
                throw new IllegalArgumentException(
                        "probability mass does not cover formula truth universe");
            }
        }

        return new M3FormulaProbabilityIndex(
                checkedTruth,
                checkedProbabilities,
                formulaIds,
                trueMass,
                falseMass,
                bothMass,
                unknownMass);
    }

    public M3FormulaTruthIndex truth() {
        return truth;
    }

    public M3ProbabilityVector probabilities() {
        return probabilities;
    }

    public long massQ32(long formulaId, M3Truth state) {
        int row = checkedFormulaRow(formulaId);
        return switch (Objects.requireNonNull(state, "state")) {
            case TRUE -> trueMassQ32[row];
            case FALSE -> falseMassQ32[row];
            case BOTH -> bothMassQ32[row];
            case UNKNOWN -> unknownMassQ32[row];
        };
    }

    public long assertedTrueMassQ32(long formulaId) {
        int row = checkedFormulaRow(formulaId);
        return Math.addExact(trueMassQ32[row], bothMassQ32[row]);
    }

    public long assertedFalseMassQ32(long formulaId) {
        int row = checkedFormulaRow(formulaId);
        return Math.addExact(falseMassQ32[row], bothMassQ32[row]);
    }

    public long classicalMassQ32(long formulaId) {
        int row = checkedFormulaRow(formulaId);
        return Math.addExact(trueMassQ32[row], falseMassQ32[row]);
    }

    public long nonClassicalMassQ32(long formulaId) {
        int row = checkedFormulaRow(formulaId);
        return Math.addExact(bothMassQ32[row], unknownMassQ32[row]);
    }

    public double trueProbability(long formulaId) {
        return massQ32(formulaId, M3Truth.TRUE) / (double) M3ProbabilityVector.ONE_Q32;
    }

    public long retainedPrimitiveBytes() {
        return (long) (formulaIds.length
                        + trueMassQ32.length
                        + falseMassQ32.length
                        + bothMassQ32.length
                        + unknownMassQ32.length)
                * Long.BYTES;
    }

    private int checkedFormulaRow(long formulaId) {
        int row = truth.formulaRow(formulaId);
        if (row < 0) throw new IllegalArgumentException("unknown formula ID");
        return row;
    }
}
