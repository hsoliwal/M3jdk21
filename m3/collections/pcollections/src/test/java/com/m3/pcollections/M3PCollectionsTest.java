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
package com.m3.pcollections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit bridge to the same dependency-free executable contract fixtures. */
class M3PCollectionsTest {
    @Test void vectorContracts() throws Exception { M3PVectorContract.main(new String[0]); }
    @Test void hashContracts() throws Exception { M3PHashContract.run(); }
    @Test void derivedContracts() throws Exception { M3PDerivedContract.run(); }
    @Test void viewContracts() throws Exception { M3PViewsContract.run(); }
}
