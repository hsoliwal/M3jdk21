// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/SearchEngines.java (blob 2e4b02f1bc26) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/** Deterministic provider selection with Java correctness fallback. */
public final class M3SearchEngines {
    private M3SearchEngines() {}

    public static M3SearchEngine best() {
        List<M3SearchEngine> candidates = new ArrayList<>();
        candidates.add(new M3JniSearchEngine());
        ServiceLoader.load(M3SearchEngine.class).forEach(candidates::add);
        candidates.add(new M3JavaAdaptiveSearchEngine());
        return candidates.stream()
                .filter(M3SearchEngine::available)
                .max(Comparator.comparingInt(M3SearchEngine::priority)
                        .thenComparing(M3SearchEngine::name))
                .orElseThrow();
    }
}
