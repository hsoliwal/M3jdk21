/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Run the identical oracle in the Maven collections tests and JNI qualification. */
final class M3RequiredExcludedWindowTest {
    @Test
    void javaReferencePreservesAllIndexedSelectionContracts() {
        assertTrue(M3RequiredExcludedWindowContract.run(
                M3IndexMaskCompactor.Backend.JAVA) >= 12_000);
    }

    @Test
    void optionalJniHasTheSameObservableResults() {
        if (Boolean.getBoolean("m3.test.native")) {
            assertTrue(M3IndexMaskCompactor.nativeAvailable(),
                    "explicit JNI qualification requires loaded library");
        }
        if (M3IndexMaskCompactor.nativeAvailable()) {
            assertTrue(M3RequiredExcludedWindowContract.run(
                    M3IndexMaskCompactor.Backend.NATIVE_REQUIRED) >= 12_000);
        }
    }
}
