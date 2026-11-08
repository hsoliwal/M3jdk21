// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import org.junit.jupiter.api.Test;

final class CompilerReferenceCensusTest {
    @Test void compilerAliasCountsAreReadOnlyAndProvenanceBound() throws Exception {
        CompilerReferenceCensusProof.main(new String[0]);
    }
}
