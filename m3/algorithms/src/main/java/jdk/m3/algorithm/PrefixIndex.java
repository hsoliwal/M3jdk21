// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
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
public final class PrefixIndex {
    private final MultiPatternSearch.Prepared automaton;

    private PrefixIndex(MultiPatternSearch.Prepared automaton) {
        this.automaton = Objects.requireNonNull(automaton, "automaton");
    }

    public static PrefixIndex prepare(List<byte[]> values) {
        return prepare(values, null);
    }

    public static PrefixIndex prepare(
            List<byte[]> values, ProgressMonitor monitor) {
        return new PrefixIndex(MultiPatternSearch.prepare(values, monitor));
    }

    public int size() {
        return automaton.patternCount();
    }

    public int prefixCount(byte[] prefix) {
        return prefixCount(prefix, null);
    }

    public int prefixCount(byte[] prefix, ProgressMonitor monitor) {
        return MultiPatternSearch.prefixCount(automaton, prefix, monitor);
    }

    public int exactCount(byte[] value) {
        return exactCount(value, null);
    }

    public int exactCount(byte[] value, ProgressMonitor monitor) {
        return MultiPatternSearch.exactCount(automaton, value, monitor);
    }

    public boolean contains(byte[] value) {
        return exactCount(value) != 0;
    }

    public boolean startsWith(byte[] prefix) {
        return prefixCount(prefix) != 0;
    }

    MultiPatternSearch.Prepared preparedAutomaton() {
        return automaton;
    }
}
