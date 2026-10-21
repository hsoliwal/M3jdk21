// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import org.junit.jupiter.api.Test;

/** Runs the same independent deterministic contract through the owning Maven suite. */
class M3GrammarTest {
    @Test
    void paragraphContracts() {
        M3GrammarContract.main(new String[0]);
    }
}
