// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Frozen formula-by-model truth image over one {@link M3FormulaTable}.
 *
 * <p>Two primitive bit planes are retained for every formula: asserted-positive and
 * asserted-negative. The four M3 truth states are direct bit addressing. This is an internal
 * projection of the specific two-bitplane precompute technique used by the Synexia formula-truth
 * implementation; it is not a claim of ownership over logic or reasoning.</p>
 */
public final class M3FormulaTruthIndex {
    @FunctionalInterface
    public interface ModelAtomTruthProvider {
        M3Truth truth(long modelId, long atomId);
    }

    public record Budget(int maxFormulas, int maxModels, long maxRetainedBytes) {
        public static final Budget DEFAULT =
                new Budget(65_536, 65_536, 256L * 1024L * 1024L);

        public Budget {
            if (maxFormulas < 0 || maxModels < 0 || maxRetainedBytes < 0L) {
                throw new IllegalArgumentException("truth-index budget must be nonnegative");
            }
        }
    }

    private final M3FormulaTable formulas;
    private final M3CoordinateSpace modelSpace;
    private final long[] formulaIds;
    private final long[] modelIds;
    private final int wordsPerFormula;
    private final long[] positiveBits;
    private final long[] negativeBits;

    private M3FormulaTruthIndex(
            M3FormulaTable formulas,
            M3CoordinateSpace modelSpace,
            long[] formulaIds,
            long[] modelIds,
            int wordsPerFormula,
            long[] positiveBits,
            long[] negativeBits) {
        this.formulas = formulas;
        this.modelSpace = Objects.requireNonNull(modelSpace, "modelSpace");
        this.formulaIds = formulaIds;
        this.modelIds = modelIds;
        this.wordsPerFormula = wordsPerFormula;
        this.positiveBits = positiveBits;
        this.negativeBits = negativeBits;
    }

    public static M3FormulaTruthIndex compile(
            M3FormulaTable formulas,
            M3CoordinateSpace modelSpace,
            long[] modelIds,
            ModelAtomTruthProvider provider) {
        return compile(formulas, modelSpace, modelIds, provider, Budget.DEFAULT);
    }

    public static M3FormulaTruthIndex compile(
            M3FormulaTable formulaTable,
            M3CoordinateSpace modelSpace,
            long[] inputModelIds,
            ModelAtomTruthProvider provider,
            Budget budget) {
        M3FormulaTable formulas = Objects.requireNonNull(formulaTable, "formulas");
        M3CoordinateSpace checkedModelSpace =
                Objects.requireNonNull(modelSpace, "modelSpace");
        long[] models = canonicalIds(Objects.requireNonNull(inputModelIds, "modelIds"));
        ModelAtomTruthProvider truthProvider = Objects.requireNonNull(provider, "provider");
        Budget checkedBudget = Objects.requireNonNull(budget, "budget");

        if (formulas.size() > checkedBudget.maxFormulas()) {
            throw new IllegalArgumentException("formula budget exceeded");
        }
        if (models.length > checkedBudget.maxModels()) {
            throw new IllegalArgumentException("model budget exceeded");
        }

        long[] formulaIds = formulas.formulaIds();
        int wordsPerFormula = (models.length + 63) >>> 6;
        long words = Math.multiplyExact((long) formulaIds.length, (long) wordsPerFormula);
        long retained =
                Math.addExact(
                        Math.multiplyExact(
                                (long) (formulaIds.length + models.length), Long.BYTES),
                        Math.multiplyExact(Math.multiplyExact(words, 2L), Long.BYTES));
        if (words > Integer.MAX_VALUE || retained > checkedBudget.maxRetainedBytes()) {
            throw new IllegalArgumentException("truth-index retained-byte budget exceeded");
        }

        long[] positive = new long[(int) words];
        long[] negative = new long[(int) words];
        for (int modelRow = 0; modelRow < models.length; modelRow++) {
            long modelId = models[modelRow];
            M3FormulaTable.Evaluator evaluator =
                    formulas.evaluator(
                            atomId ->
                                    Objects.requireNonNull(
                                            truthProvider.truth(modelId, atomId),
                                            "model atom truth"));
            int word = modelRow >>> 6;
            long bit = 1L << (modelRow & 63);
            for (int formulaRow = 0; formulaRow < formulaIds.length; formulaRow++) {
                M3Truth truth = evaluator.truth(formulaIds[formulaRow]);
                int cell = formulaRow * wordsPerFormula + word;
                if (truth.assertedTrue()) positive[cell] |= bit;
                if (truth.assertedFalse()) negative[cell] |= bit;
            }
        }

        return new M3FormulaTruthIndex(
                formulas,
                checkedModelSpace,
                formulaIds,
                models,
                wordsPerFormula,
                positive,
                negative);
    }

    public M3FormulaTable formulas() {
        return formulas;
    }

    public M3CoordinateSpace formulaSpace() {
        return formulas.formulaSpace();
    }

    public M3CoordinateSpace atomSpace() {
        return formulas.atomSpace();
    }

    public M3CoordinateSpace modelSpace() {
        return modelSpace;
    }

    public int formulaCount() {
        return formulaIds.length;
    }

    public int modelCount() {
        return modelIds.length;
    }

    public long[] formulaIds() {
        return formulaIds.clone();
    }

    public long[] modelIds() {
        return modelIds.clone();
    }

    public M3Truth truth(long formulaId, long modelId) {
        return truthAt(checkedFormulaRow(formulaId), checkedModelRow(modelId));
    }

    M3Truth truthAt(int formulaRow, int modelRow) {
        int checkedFormula = Objects.checkIndex(formulaRow, formulaIds.length);
        int checkedModel = Objects.checkIndex(modelRow, modelIds.length);
        int word = checkedModel >>> 6;
        long bit = 1L << (checkedModel & 63);
        int cell = checkedFormula * wordsPerFormula + word;
        int bits = 0;
        if ((positiveBits[cell] & bit) != 0L) bits |= 1;
        if ((negativeBits[cell] & bit) != 0L) bits |= 2;
        return M3Truth.fromBits(bits);
    }

    public long[] modelsWithTruth(long formulaId, M3Truth wanted) {
        int formulaRow = checkedFormulaRow(formulaId);
        M3Truth checkedWanted = Objects.requireNonNull(wanted, "wanted");
        long[] scratch = new long[modelIds.length];
        int count = 0;
        int base = formulaRow * wordsPerFormula;
        for (int word = 0; word < wordsPerFormula; word++) {
            long positive = positiveBits[base + word];
            long negative = negativeBits[base + word];
            long matches =
                    switch (checkedWanted) {
                        case TRUE -> positive & ~negative;
                        case FALSE -> negative & ~positive;
                        case BOTH -> positive & negative;
                        case UNKNOWN -> ~(positive | negative);
                    };
            if (word == wordsPerFormula - 1 && (modelIds.length & 63) != 0) {
                matches &= (1L << (modelIds.length & 63)) - 1L;
            }
            while (matches != 0L) {
                int bit = Long.numberOfTrailingZeros(matches);
                scratch[count++] = modelIds[(word << 6) + bit];
                matches &= matches - 1L;
            }
        }
        return Arrays.copyOf(scratch, count);
    }

    public long retainedPrimitiveBytes() {
        return (long) (formulaIds.length + modelIds.length + positiveBits.length + negativeBits.length)
                * Long.BYTES;
    }

    int formulaRow(long formulaId) {
        return binarySearchUnsigned(formulaIds, formulaId);
    }

    int modelRow(long modelId) {
        return binarySearchUnsigned(modelIds, modelId);
    }

    private int checkedFormulaRow(long formulaId) {
        int row = formulaRow(formulaId);
        if (row < 0) throw new IllegalArgumentException("unknown formula ID");
        return row;
    }

    private int checkedModelRow(long modelId) {
        int row = modelRow(modelId);
        if (row < 0) throw new IllegalArgumentException("unknown model ID");
        return row;
    }

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

    private static long[] canonicalIds(long[] input) {
        Long[] boxed = new Long[input.length];
        for (int index = 0; index < input.length; index++) {
            long id = input[index];
            if (id == 0L) throw new IllegalArgumentException("model ID must be nonzero");
            boxed[index] = id;
        }
        Arrays.sort(boxed, Long::compareUnsigned);
        long[] result = new long[boxed.length];
        int count = 0;
        for (Long boxedId : boxed) {
            long id = boxedId;
            if (count == 0 || result[count - 1] != id) result[count++] = id;
        }
        return Arrays.copyOf(result, count);
    }
}
