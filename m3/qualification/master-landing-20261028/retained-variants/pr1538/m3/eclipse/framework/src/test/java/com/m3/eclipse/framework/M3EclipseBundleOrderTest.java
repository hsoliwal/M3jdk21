// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import com.m3.collections.M3IndexMaskCompactor;
import org.junit.jupiter.api.Test;

/** Invokes the same frozen real-OSGi oracle in the owning module test lifecycle. */
class M3EclipseBundleOrderTest {
    private static M3IndexMaskCompactor.Backend backend() {
        return M3IndexMaskCompactor.nativeAvailable()
                ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
    }

    @Test void capturedBundleIdentitiesAndEveryStateMask() {
        M3EclipseBundleOrderContract.capturedIdentityAndMasks(backend());
    }

    @Test void windowsFollowSelectionAndOrdering() {
        M3EclipseBundleOrderContract.windowsAndIsolation(backend());
    }

    @Test void invalidAndEmptyInputsRetainOwnerValidation() {
        M3EclipseBundleOrderContract.validationAndEmpty(backend());
    }

    @Test void requiredNativeDoesNotBecomeJavaFallback() {
        M3EclipseBundleOrderContract.nativeRefusal();
    }
}
