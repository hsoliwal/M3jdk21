// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections.apache.list;

import org.junit.jupiter.api.Test;

/** Runs the independent array-index and public-list contract oracle. */
public final class TreeListIndexTraversalTest {
    @Test
    void indexedAndThreadedWalksPreserveTheForkContract() throws Exception {
        TreeListIndexTraversalContract.main(new String[0]);
    }
}
