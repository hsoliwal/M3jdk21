/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/index/HybridSearchKernelTest.java (blob dc2bcb638e11) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import org.junit.jupiter.api.Test;

class HybridSearchKernelTest {
    @Test
    void boundedSelectionMatchesIndependentFullSort() throws Exception {
        HybridSearchKernelChecks.verifySelector(TopKSelector.javaSelector());
    }

    @Test
    void candidateBudgetsPreserveLegacySaturation() throws Exception {
        HybridSearchKernelChecks.verifyOptions();
    }

    @Test
    void weightedFusionMatchesIndependentFullSort() {
        HybridSearchKernelChecks.verifyFusion();
    }

    @Test
    void boundedPreparationPreservesLifecycleAndConcurrentPublication() throws Exception {
        HybridSearchKernelChecks.verifyCache();
    }
}
