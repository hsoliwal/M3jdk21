/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK 21 from cpullm-java/src/main/java/jdk/internal/m3/cpullm/facets/FacetNode.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.facets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/** Summary-of-summaries over evidence coordinates; no intermediate text contraction. */
public final class FacetNode {
    private final FacetCorpus corpus;
    private final int[] rows;
    private final List<String> children;
    private final String semanticRoot, lineageRoot;

    private FacetNode(FacetCorpus corpus, int[] rows, List<String> children) {
        this.corpus = corpus; this.rows = rows; this.children = List.copyOf(children);
        var semantic = new FacetCorpus.Seal("SX-FACET-SCOPE-1").text(corpus.root()).number(rows.length);
        for (int row : rows) semantic.text(corpus.evidenceId(row));
        semanticRoot = semantic.finish();
        var lineage = new FacetCorpus.Seal("SX-FACET-NODE-1").text(semanticRoot).number(children.size());
        children.forEach(lineage::text); lineageRoot = lineage.finish();
    }
    public static FacetNode leaf(FacetCorpus corpus, int... evidenceRows) {
        Objects.requireNonNull(corpus, "corpus"); Objects.requireNonNull(evidenceRows, "rows");
        if (evidenceRows.length > FacetCorpus.MAX_ATOMS) throw new IllegalArgumentException("node coordinate budget");
        int[] sorted = evidenceRows.clone(); Arrays.sort(sorted);
        for (int i = 0; i < sorted.length; i++) {
            Objects.checkIndex(sorted[i], corpus.size());
            if (i > 0 && sorted[i] == sorted[i - 1]) throw new IllegalArgumentException("duplicate leaf coordinate");
        }
        return new FacetNode(corpus, sorted, List.of());
    }
    public static FacetNode all(FacetCorpus corpus) {
        int[] rows = new int[corpus.size()]; for (int i = 0; i < rows.length; i++) rows[i] = i; return leaf(corpus, rows);
    }
    public static FacetNode combine(List<FacetNode> input) {
        if (input.isEmpty() || input.size() > 1024) throw new IllegalArgumentException("fan-in 1..1024 required");
        FacetCorpus corpus = Objects.requireNonNull(input.getFirst(), "child").corpus;
        var children = new TreeMap<String, FacetNode>();
        for (FacetNode child : input) {
            Objects.requireNonNull(child, "child");
            if (!corpus.root().equals(child.corpus.root())) throw new IllegalArgumentException("cross-corpus fan-in");
            children.put(child.lineageRoot, child);
        }
        // Balanced two-pointer unions avoid multiplying work by every preceding child length.
        List<int[]> level = new ArrayList<>(); for (FacetNode child : children.values()) level.add(child.rows);
        while (level.size() > 1) {
            List<int[]> next = new ArrayList<>();
            for (int i = 0; i < level.size(); i += 2) {
                FacetCorpus.cancelled();
                next.add(i + 1 == level.size() ? level.get(i) : union(level.get(i), level.get(i + 1), corpus.size()));
            }
            level = next;
        }
        return new FacetNode(corpus, level.getFirst().clone(), List.copyOf(children.keySet()));
    }
    static int[] union(int[] left, int[] right, int maximum) {
        int[] result = new int[Math.min(maximum, Math.addExact(left.length, right.length))];
        int a = 0, b = 0, out = 0;
        while (a < left.length || b < right.length) {
            int value;
            if (b == right.length || (a < left.length && left[a] < right[b])) value = left[a++];
            else if (a == left.length || right[b] < left[a]) value = right[b++];
            else { value = left[a++]; b++; }
            result[out++] = value;
        }
        return Arrays.copyOf(result, out);
    }
    public FacetCorpus corpus() { return corpus; }
    public int[] evidenceRows() { return rows.clone(); }
    int[] internalRows() { return rows; }
    public List<String> childLineageRoots() { return children; }
    public String semanticRoot() { return semanticRoot; }
    public String lineageRoot() { return lineageRoot; }
}
