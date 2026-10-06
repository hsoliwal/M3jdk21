// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Frozen conditional/default table over one {@link M3FormulaTable}.
 *
 * <p>This is one concrete primitive implementation of conditional assessment mechanics. It stores
 * only canonical formula coordinates and caller-supplied nonnegative priorities. It does not claim
 * ownership of conditional reasoning and does not infer a ranking formalism that the caller did not
 * explicitly supply.</p>
 */
public final class M3ConditionalTable {
    public enum Assessment {
        IRRELEVANT,
        VERIFIED,
        FALSIFIED,
        BOTH,
        UNRESOLVED
    }

    private final M3FormulaTable formulas;
    private final long[] conditionalIds;
    private final long[] antecedentFormulaIds;
    private final long[] consequentFormulaIds;
    private final int[] priorities;

    private M3ConditionalTable(
            M3FormulaTable formulas,
            long[] conditionalIds,
            long[] antecedentFormulaIds,
            long[] consequentFormulaIds,
            int[] priorities) {
        this.formulas = formulas;
        this.conditionalIds = conditionalIds;
        this.antecedentFormulaIds = antecedentFormulaIds;
        this.consequentFormulaIds = consequentFormulaIds;
        this.priorities = priorities;
    }

    public static Builder builder(M3FormulaTable formulas) {
        return new Builder(formulas);
    }

    public M3FormulaTable formulas() {
        return formulas;
    }

    public int size() {
        return conditionalIds.length;
    }

    public long antecedent(long conditionalId) {
        return antecedentFormulaIds[checkedRow(conditionalId)];
    }

    public long consequent(long conditionalId) {
        return consequentFormulaIds[checkedRow(conditionalId)];
    }

    public int priority(long conditionalId) {
        return priorities[checkedRow(conditionalId)];
    }

    public long[] conditionalIds() {
        return conditionalIds.clone();
    }

    public Assessment assess(
            long conditionalId, M3FormulaTable.AtomTruthProvider atoms) {
        int row = checkedRow(conditionalId);
        M3FormulaTable.Evaluator evaluator = formulas.evaluator(atoms);
        return assess(
                evaluator.truth(antecedentFormulaIds[row]),
                evaluator.truth(consequentFormulaIds[row]));
    }

    /**
     * Compile all conditional assessments directly from a frozen formula/model truth image.
     */
    public AssessmentImage assess(M3FormulaTruthIndex truth) {
        M3FormulaTruthIndex checked = Objects.requireNonNull(truth, "truth");
        if (checked.formulas() != formulas) {
            throw new IllegalArgumentException("truth image must share formula table");
        }

        long[] models = checked.modelIds();
        byte[] states = new byte[Math.multiplyExact(models.length, conditionalIds.length)];
        int[] worldRanks = new int[models.length];

        for (int model = 0; model < models.length; model++) {
            int maxFalsifiedPriority = -1;
            for (int conditional = 0; conditional < conditionalIds.length; conditional++) {
                int antecedentRow = checked.formulaRow(antecedentFormulaIds[conditional]);
                int consequentRow = checked.formulaRow(consequentFormulaIds[conditional]);
                if (antecedentRow < 0 || consequentRow < 0) {
                    throw new IllegalArgumentException("truth image missing conditional formula");
                }
                Assessment assessment =
                        assess(
                                checked.truthAt(antecedentRow, model),
                                checked.truthAt(consequentRow, model));
                states[model * conditionalIds.length + conditional] =
                        (byte) assessment.ordinal();
                if (assessment == Assessment.FALSIFIED) {
                    maxFalsifiedPriority =
                            Math.max(maxFalsifiedPriority, priorities[conditional]);
                }
            }
            worldRanks[model] =
                    maxFalsifiedPriority < 0 ? 0 : Math.addExact(maxFalsifiedPriority, 1);
        }

        return new AssessmentImage(this, models, states, worldRanks);
    }

    public long retainedPrimitiveBytes() {
        return (long) (conditionalIds.length
                        + antecedentFormulaIds.length
                        + consequentFormulaIds.length)
                        * Long.BYTES
                + (long) priorities.length * Integer.BYTES;
    }

    private int checkedRow(long conditionalId) {
        int row = binarySearchUnsigned(conditionalIds, conditionalId);
        if (row < 0) throw new IllegalArgumentException("unknown conditional ID");
        return row;
    }

    private static Assessment assess(M3Truth antecedent, M3Truth consequent) {
        Objects.requireNonNull(antecedent, "antecedent");
        Objects.requireNonNull(consequent, "consequent");
        if (antecedent == M3Truth.FALSE) return Assessment.IRRELEVANT;
        if (antecedent != M3Truth.TRUE) return Assessment.UNRESOLVED;
        return switch (consequent) {
            case TRUE -> Assessment.VERIFIED;
            case FALSE -> Assessment.FALSIFIED;
            case BOTH -> Assessment.BOTH;
            case UNKNOWN -> Assessment.UNRESOLVED;
        };
    }

    public static final class AssessmentImage {
        private final M3ConditionalTable table;
        private final long[] modelIds;
        private final byte[] states;
        private final int[] worldRanks;

        private AssessmentImage(
                M3ConditionalTable table,
                long[] modelIds,
                byte[] states,
                int[] worldRanks) {
            this.table = table;
            this.modelIds = modelIds;
            this.states = states;
            this.worldRanks = worldRanks;
        }

        public Assessment assessment(long modelId, long conditionalId) {
            int model = binarySearchUnsigned(modelIds, modelId);
            if (model < 0) throw new IllegalArgumentException("unknown model ID");
            int conditional = table.checkedRow(conditionalId);
            return Assessment.values()[
                    Byte.toUnsignedInt(states[model * table.size() + conditional])];
        }

        public int worldRank(long modelId) {
            int model = binarySearchUnsigned(modelIds, modelId);
            if (model < 0) throw new IllegalArgumentException("unknown model ID");
            return worldRanks[model];
        }

        public long retainedPrimitiveBytes() {
            return (long) modelIds.length * Long.BYTES
                    + states.length
                    + (long) worldRanks.length * Integer.BYTES;
        }
    }

    public static final class Builder {
        private final M3FormulaTable formulas;
        private final TreeMap<Long, Draft> drafts = new TreeMap<>(Long::compareUnsigned);

        private Builder(M3FormulaTable formulas) {
            this.formulas = Objects.requireNonNull(formulas, "formulas");
        }

        public Builder add(
                long conditionalId,
                long antecedentFormulaId,
                long consequentFormulaId,
                int priority) {
            if (conditionalId == 0L) {
                throw new IllegalArgumentException("conditional ID must be nonzero");
            }
            if (!formulas.contains(antecedentFormulaId)
                    || !formulas.contains(consequentFormulaId)) {
                throw new IllegalArgumentException("conditional references unknown formula");
            }
            if (priority < 0) {
                throw new IllegalArgumentException("priority must be nonnegative");
            }
            Draft draft =
                    new Draft(
                            antecedentFormulaId,
                            consequentFormulaId,
                            priority);
            if (drafts.putIfAbsent(conditionalId, draft) != null) {
                throw new IllegalArgumentException("duplicate conditional ID");
            }
            return this;
        }

        public M3ConditionalTable build() {
            long[] ids = new long[drafts.size()];
            long[] antecedents = new long[drafts.size()];
            long[] consequents = new long[drafts.size()];
            int[] priorities = new int[drafts.size()];
            int row = 0;
            for (var entry : drafts.entrySet()) {
                ids[row] = entry.getKey();
                antecedents[row] = entry.getValue().antecedent;
                consequents[row] = entry.getValue().consequent;
                priorities[row] = entry.getValue().priority;
                row++;
            }
            return new M3ConditionalTable(
                    formulas, ids, antecedents, consequents, priorities);
        }
    }

    private record Draft(long antecedent, long consequent, int priority) {}

    private static int binarySearchUnsigned(long[] values, long wanted) {
        int low = 0;
        int high = values.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int compared = Long.compareUnsigned(values[middle], wanted);
            if (compared < 0) low = middle + 1;
            else if (compared > 0) high = middle - 1;
            else return middle;
        }
        return -1;
    }
}
