/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Map;

/**
 * Trusted, application-provided transition validator; never loaded from stored data.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/memory/MementoGuard.java}
 * blob {@code a018cca0d9bce380e00689c9e7dc362b523697af} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@FunctionalInterface
public interface MementoGuard {
    /** Reject invalid/unauthorized transitions by throwing; inputs are immutable. */
    void verify(Memento before, Map<String, String> after);

    /** Conjunction preserves both validators, in explicit order. */
    default MementoGuard and(MementoGuard other) {
        java.util.Objects.requireNonNull(other, "other");
        return (before, after) -> { verify(before, after); other.verify(before, after); };
    }
}
