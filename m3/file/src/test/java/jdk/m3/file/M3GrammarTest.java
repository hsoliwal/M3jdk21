// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;
//FLLDCIM: RESTORE - source m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/test/java/jdk/m3/file/M3GrammarTest.java blob 51717187ad6751a63bb26a3020568f4e3a2d7d8a (retained PR #1619 head; parent 0cd0944136c1 blob 8aa80a8b47e38de4bb90ca35936734b686d8c7a5 https://github.com/hsoliwal/M3jdk21/blob/0cd0944136c1/m3/file/src/test/java/com/m3/file/M3GrammarTest.java); recipe m3/file/qualification/retained-port-20261107 TEST

import org.junit.jupiter.api.Test;

/** Runs the same independent deterministic contract through the owning Maven suite. */
class M3GrammarTest {
    @Test
    void paragraphContracts() {
        M3GrammarContract.main(new String[0]);
    }
}
