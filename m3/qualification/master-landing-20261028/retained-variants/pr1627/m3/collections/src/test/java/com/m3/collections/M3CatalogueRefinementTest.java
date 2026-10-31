/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3CatalogueRefinementTest {
    @Test void reentrantSelectionRestoresEnclosingCallbacks() {
        M3CatalogueRefinementContract.main(new String[] {"reentrant"});
    }
    @Test void callbackInvalidationSurvivesCapturePublication() {
        M3CatalogueRefinementContract.main(new String[] {"invalidation"});
    }
    @Test void packedSelectionPreservesOccurrencesAndDirectoryOwnership() {
        M3CatalogueRefinementContract.main(new String[] {"packed"});
    }
}
