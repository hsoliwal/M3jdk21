// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Runs the same real-owner JDK21 standalone oracle in Maven. */
final class M3LinkedIndexSplitTest {
    @Test
    void indexedLinksWithoutElementBuffers() {
        M3LinkedIndexSplitContract.main(new String[0]);
    }
}
