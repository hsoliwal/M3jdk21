// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/SearchEngines.java (blob 2e4b02f1bc26) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/** Deterministic provider selection with Java correctness fallback. */
public final class SearchEngines {
    private SearchEngines() {}

    public static SearchEngine best() {
        List<SearchEngine> candidates = new ArrayList<>();
        candidates.add(new JniSearchEngine());
        ServiceLoader.load(SearchEngine.class).forEach(candidates::add);
        candidates.add(new JavaAdaptiveSearchEngine());
        return candidates.stream()
                .filter(SearchEngine::available)
                .max(Comparator.comparingInt(SearchEngine::priority)
                        .thenComparing(SearchEngine::name))
                .orElseThrow();
    }
}
