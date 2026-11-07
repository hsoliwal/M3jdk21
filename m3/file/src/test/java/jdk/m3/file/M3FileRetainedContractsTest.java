// SPDX-FileCopyrightText: 2026 Synexia, Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package jdk.m3.file;

import org.junit.jupiter.api.Test;

/**
 * Runs the independent deterministic contracts restored by m3/file/qualification/retained-port-20261107 through the
 * owning Maven suite (the pattern of M3GrammarTest over M3GrammarContract): the #1975 contracts had no JUnit wrapper
 * at their parent commit 9023729d8ae2, and the module's exec step (verify.py) does not run on a host without python3.
 */
class M3FileRetainedContractsTest {
    @Test
    void encodedEditsContract() throws Exception {
        M3FileEncodedEditsContract.main(new String[0]);
    }

    @Test
    void textCoordinatesContract() throws Exception {
        M3FileTextCoordinatesContract.main(new String[0]);
    }
}
