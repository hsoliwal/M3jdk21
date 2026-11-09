// SPDX-License-Identifier: Apache-2.0
package jdk.m3.regexcursor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** JUnit entrance for source-generation token lane projection; JNI has a separate explicit gate. */
final class GrammarTokenProjectionTest {
    @Test
    void exactSpanOracleWithRefusalsAndConcurrency() throws Exception {
        assertTrue(GrammarTokenProjectionProof.verify(5_000) > 20_000);
    }
}
