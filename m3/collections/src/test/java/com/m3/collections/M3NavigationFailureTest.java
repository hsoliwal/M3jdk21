/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3NavigationFailureTest {
    @Test void comparatorFailuresAreNotRetried() {
        M3NavigationFailureContract.comparatorFailureIsNotRetried();
    }
    @Test void opaqueRangeFailuresAreNotSwallowed() {
        M3NavigationFailureContract.opaqueOwnerFailureIsPreserved();
    }
    @Test void existingEmptyOwnerAdmissionIsPreserved() {
        M3NavigationFailureContract.emptyOwnerAdmissionIsPreserved();
    }
    @Test void arrayTreeOwnersKeepBoundedNavigation() {
        M3NavigationFailureContract.m3ArrayTreeOwnersRemainSupported();
    }
}
