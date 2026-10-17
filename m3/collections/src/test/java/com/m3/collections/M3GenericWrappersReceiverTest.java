// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Cross-owner generic filtered Collection/Set/Map and navigation contracts. */
final class M3GenericWrappersReceiverTest {
    @Test
    void genericJdkAndM3Backings() throws Exception {
        M3CollectionViewsReceiverSelfTest.main(new String[0]);
    }

    @Test
    void retainedFirstPartyMapContractCorpus() {
        M3FilteredMapsSelfTest.main(new String[0]);
    }
}
