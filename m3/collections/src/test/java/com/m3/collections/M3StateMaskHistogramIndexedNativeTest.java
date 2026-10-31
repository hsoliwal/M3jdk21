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

import org.junit.jupiter.api.Test;

/** Owning JUnit5 binding for indexed original-array JNI differential proof. */
final class M3StateMaskHistogramIndexedNativeTest {
    @Test void indexedNativeAndJavaParity() throws Exception {
        M3StateMaskHistogramIndexedNativeContract.main(new String[0]);
    }
}
