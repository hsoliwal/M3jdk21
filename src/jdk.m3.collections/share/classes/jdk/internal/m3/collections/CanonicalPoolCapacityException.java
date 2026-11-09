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
package jdk.internal.m3.collections;

import java.io.Serial;

/**
 * Admission would exceed an explicit canonical-pool bound. Existing entries are never evicted.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/canonical/CanonicalPoolCapacityException.java}
 * blob {@code b35d1ed5619418f5bb5f08453cf76ffed2bd04a0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CanonicalPoolCapacityException extends IllegalStateException {
    @Serial
    private static final long serialVersionUID = 1L;

    CanonicalPoolCapacityException() {
        super("canonical pool capacity exceeded; no eviction performed");
    }
}
