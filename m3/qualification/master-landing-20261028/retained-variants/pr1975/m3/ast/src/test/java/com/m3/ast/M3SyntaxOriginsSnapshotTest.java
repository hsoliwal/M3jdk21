// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import org.junit.jupiter.api.Test;

/** JUnit gate for the source-external parser-agnostic provenance wire format. */
final class M3SyntaxOriginsSnapshotTest {
    @Test void physicalProvenanceRoundTrip() {
        M3SyntaxOriginsSnapshotContract.main(new String[0]);
    }
}
