// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressMonitor;
import java.util.List;
import java.util.Objects;

/**
 * Immutable prefix-index facade over the canonical prepared Aho-Corasick trie.
 *
 * <p>This type owns no node/edge storage. Preparation delegates to
 * {@link M3MultiPatternSearch#prepare(List, M3ProgressMonitor)} and every query reuses that
 * immutable automaton. Duplicate input patterns are retained as multiplicity, matching static
 * prefix-count workloads. Online mutable challenge APIs can batch/rebuild through this immutable
 * boundary without creating a competing trie representation.</p>
 */
//FLLDCIM: RELOCATE - donor blob eb61d57d5d1f at commit a7b7bb639802 (repository, path, URL and digests: m3/algorithms/recipe/corpus-search-20261108/PROVENANCE.tsv); recipe m3/algorithms/recipe/corpus-search-20261108/relocate.py v1
public final class M3PrefixIndex {
    private final M3MultiPatternSearch.Prepared automaton;

    private M3PrefixIndex(M3MultiPatternSearch.Prepared automaton) {
        this.automaton = Objects.requireNonNull(automaton, "automaton");
    }

    public static M3PrefixIndex prepare(List<byte[]> values) {
        return prepare(values, null);
    }

    public static M3PrefixIndex prepare(
            List<byte[]> values, M3ProgressMonitor monitor) {
        return new M3PrefixIndex(M3MultiPatternSearch.prepare(values, monitor));
    }

    public int size() {
        return automaton.patternCount();
    }

    public int prefixCount(byte[] prefix) {
        return prefixCount(prefix, null);
    }

    public int prefixCount(byte[] prefix, M3ProgressMonitor monitor) {
        return M3MultiPatternSearch.prefixCount(automaton, prefix, monitor);
    }

    public int exactCount(byte[] value) {
        return exactCount(value, null);
    }

    public int exactCount(byte[] value, M3ProgressMonitor monitor) {
        return M3MultiPatternSearch.exactCount(automaton, value, monitor);
    }

    public boolean contains(byte[] value) {
        return exactCount(value) != 0;
    }

    public boolean startsWith(byte[] prefix) {
        return prefixCount(prefix) != 0;
    }

    M3MultiPatternSearch.Prepared preparedAutomaton() {
        return automaton;
    }
}
