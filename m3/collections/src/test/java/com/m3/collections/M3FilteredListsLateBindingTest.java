// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** JUnit adapter for the independent main() oracle against the real M3 module. */
final class M3FilteredListsLateBindingTest {
    @Test void sourceMutationsBeforeBindingAndAfterBinding() {
        M3FilteredListsLateBindingProof.main(new String[0]);
    }
}
