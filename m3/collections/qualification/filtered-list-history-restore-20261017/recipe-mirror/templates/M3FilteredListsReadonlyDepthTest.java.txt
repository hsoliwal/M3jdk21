// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Actual module JUnit harness of the isolated repeated-readonly contract oracle. */
final class M3FilteredListsReadonlyDepthTest {
    @Test void nestedReadOnlyNeverRecursesOrLeaksWriteAccess() {
        M3FilteredListsReadonlyDepthProof.main(new String[0]);
    }
}
