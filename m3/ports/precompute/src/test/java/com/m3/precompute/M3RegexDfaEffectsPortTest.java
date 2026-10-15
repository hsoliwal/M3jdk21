// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import org.junit.jupiter.api.Test;

/**
 * Exact, independently authored M3 receiver qualification for Synexia's
 * compositional DFA effects. No java.util.regex replacement or JNI dispatch.
 */
final class M3RegexDfaEffectsPortTest {
    @Test
    void wholeInputAndFragmentCompositionPreserveIndependentOracles() throws Exception {
        M3RegexDfaEffectsProof.run(null);
    }

    @Test
    void retainedRangeTreeAndSnapshotReplacePreserveState() throws Exception {
        M3RegexDfaEffectsIndexProof.run(null);
    }
}
