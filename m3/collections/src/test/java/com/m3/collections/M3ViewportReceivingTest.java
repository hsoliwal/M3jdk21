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

/** Ordinary Maven/JUnit discovery; native-required execution is separately explicit. */
class M3ViewportReceivingTest {
    @Test void residentWindowBounds() { M3ViewportPlanProof.main(new String[0]); }
    @Test void rowGeometryAndProjection() { M3ViewportTreeProof.main(new String[0]); }
}
