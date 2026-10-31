/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Owning-module entry point for the original-owner endpoint regression corpus. */
final class M3FilteredEndpointRestorationTest {
    @Test void restoresStrictEndpointsAcrossPrivateAndPublicOwners() {
        M3FilteredEndpointRestorationContract.main(new String[0]);
    }
}
