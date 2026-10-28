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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class M3ShadowStreamsTest {
    @Test void physicalReadWriteMirrorsAndFailures() throws Exception {
        M3ShadowStreamsContract.main(new String[0]);
    }

    @Test void originalWeightedChildIsCreditedByPhysicalBytes() throws Exception {
        M3DefaultProgressMonitor parent = new M3DefaultProgressMonitor();
        parent.beginTask("parent", 100L);
        ByteArrayOutputStream follower = new ByteArrayOutputStream();
        try (InputStream primary = parent.shadowInput(
                new ByteArrayInputStream(new byte[]{1, 2, 3, 4}), 25L, 4L, follower)) {
            assertEquals(4, primary.readAllBytes().length);
        }
        assertArrayEquals(new byte[]{1, 2, 3, 4}, follower.toByteArray());
        assertEquals(25L, parent.state().worked());
        assertFalse(parent.state().done());
    }
}
