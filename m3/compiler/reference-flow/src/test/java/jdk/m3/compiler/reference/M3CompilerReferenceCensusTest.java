// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.reference;

import org.junit.jupiter.api.Test;

final class M3CompilerReferenceCensusTest {
    @Test void compilerAliasCountsAreReadOnlyAndProvenanceBound() throws Exception {
        M3CompilerReferenceCensusProof.main(new String[0]);
    }
}
