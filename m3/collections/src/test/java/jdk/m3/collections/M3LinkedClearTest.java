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
package jdk.m3.collections;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Runs the real-owner clear/reuse and escaped-view contract in Maven. */
final class M3LinkedClearTest {
    //FLLDCIM: MRL - stale-contracts-20261106 L1: JUnit wrapper; bound-split check follows 4650910eff1
    @Test
    void clearRetainsCapacityAndInvalidatesEscapedViews() throws Exception {
        String library = System.getProperty("m3.arrays.native.path");
        if (library != null && !library.isBlank()) {
            M3LinkedClearContract.main(new String[0]);
            return;
        }
        // Every M3LinkedList check runs first; M3LinkedHashMap/Set tables need the m3_arrays JNI library,
        // which the default build does not load (qualification/jni-test-gating-20261106).
        UnsatisfiedLinkError missing = Assertions.assertThrows(UnsatisfiedLinkError.class,
                () -> M3LinkedClearContract.main(new String[0]));
        Assertions.assertTrue(Arrays.stream(missing.getStackTrace())
                .anyMatch(frame -> "mapAndSet".equals(frame.getMethodName())), "only map/set may need JNI");
        Assumptions.abort("M3LinkedList half passed; map/set half needs -Pm3-collections-native");
    }
}
