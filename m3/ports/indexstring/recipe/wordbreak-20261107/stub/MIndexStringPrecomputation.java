package com.synexia.indexstring;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
/**
 * Donor-side stub (differential only): the donor constructor's row rules (first occurrence wins, keyed by
 * MIndexString equality; one FrozenChars per row) and the four members the vocabulary tree and plan read.
 */
public final class MIndexStringPrecomputation {
    private final MIndexString[] values; private final FrozenChars[] chars; private final Map<MIndexString, Integer> rows;
    private final MIndexVocabularySearchTree tree;
    private MIndexStringPrecomputation(Collection<? extends MIndexString> vocabulary) {
        Map<MIndexString, Integer> positions = new LinkedHashMap<>();
        for (MIndexString value : vocabulary) {
            Objects.requireNonNull(value, "vocabulary element");
            if (!positions.containsKey(value)) positions.put(value, positions.size());
        }
        rows = positions;
        values = positions.keySet().toArray(MIndexString[]::new);
        chars = new FrozenChars[values.length];
        for (int i = 0; i < values.length; i++) chars[i] = FrozenChars.copyOf(values[i].toString().toCharArray());
        tree = MIndexVocabularySearchTree.compile(this);
    }
    public static MIndexStringPrecomputation compile(Collection<? extends MIndexString> vocabulary) {
        return new MIndexStringPrecomputation(Objects.requireNonNull(vocabulary, "vocabulary"));
    }
    public int size() { return values.length; }
    public MIndexString valueAt(int row) { return values[Objects.checkIndex(row, values.length)]; }
    public FrozenChars chars(MIndexString value) {
        Integer row = rows.get(Objects.requireNonNull(value, "value"));
        if (row == null) throw new IllegalArgumentException("value was not precomputed");
        return chars[row];
    }
    public MIndexVocabularySearchTree vocabularyTree() { return tree; }
}
