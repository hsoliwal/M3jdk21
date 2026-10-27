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
package com.m3.collections.apache.list;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class M3TreeListArrayStorageTest {
    @Test
    void randomizedListAndIteratorContractsWithSlotReuse() throws ReflectiveOperationException {
        for (int seed = 0; seed < 4; seed++) {
            M3TreeListArrayStorageProof.exercise(new ArrayList<>(), new TreeList<>(), 87121L + seed, 3000, false);
        }
    }

    @Test
    void bulkAppendAndFailureReuseStorageAtBoundedLiveSize() throws ReflectiveOperationException {
        M3TreeListArrayStorageProof.boundedBulkReuse(new TreeList<>(), new TreeList<>());
    }

    @Test
    void failedBalancedBuildReleasesUncommittedPayloadAndCanGrowAgain() throws ReflectiveOperationException {
        // TreeList intentionally builds the complete source before attaching its subtree.
        M3TreeListArrayStorageProof.failedBulkBuild(new TreeList<>(), new TreeList<>());
    }
}
