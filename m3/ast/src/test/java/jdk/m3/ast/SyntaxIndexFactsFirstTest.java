// SPDX-License-Identifier: Apache-2.0
package jdk.m3.ast;

import org.junit.jupiter.api.Test;

/** Two original-owner Java21 contracts for the pinned source-sealed recipe receiver. */
final class SyntaxIndexFactsFirstTest {
    @Test void earliestDuplicateAndParallelFacts() throws Exception {
        SyntaxIndexFactsFirstContract.main(new String[0]);
    }
    @Test void collisionFallbackAndOneTimeRefusal() throws Exception {
        ProbeGuardContract.main(new String[0]);
    }
}
