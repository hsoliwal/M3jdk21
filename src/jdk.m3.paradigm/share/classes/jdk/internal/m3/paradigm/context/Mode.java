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

/**
 * Semantic mutation mode for a wrapped context.
 *
 * <p>The three modes deliberately separate base topology from local semantic
 * mutability:
 * <ul>
 *   <li>{@link #DYNAMIC}: base may be rebound and local state may change.</li>
 *   <li>{@link #FIXED}: base identity is fixed, local state may still change.</li>
 *   <li>{@link #FROZEN}: neither base nor local semantic state may change.</li>
 * </ul>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextMode.java}
 * blob {@code 6226fb750d892771106512253fe47b476a425800} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum Mode {
    DYNAMIC,
    FIXED,
    FROZEN;

    /**
     * Whether the wrapped base may be replaced.
     *
     * @return true only for dynamic contexts
     */
    public boolean rebasable() {
        return this == DYNAMIC;
    }

    /**
     * Whether local semantic state may change.
     *
     * @return false only for frozen contexts
     */
    public boolean mutable() {
        return this != FROZEN;
    }

    /**
     * Whether base identity is fixed.
     *
     * <p>Frozen contexts are necessarily fixed too.
     *
     * @return true for fixed and frozen contexts
     */
    public boolean fixed() {
        return this != DYNAMIC;
    }

    /**
     * Whether all semantic mutation is forbidden.
     *
     * @return true only for frozen contexts
     */
    public boolean frozen() {
        return this == FROZEN;
    }

    /**
     * Combines two modes without increasing mutability.
     *
     * <p>FROZEN dominates FIXED, which dominates DYNAMIC.
     *
     * @param other other mode
     * @return least-mutable mode that satisfies both inputs
     */
    public Mode restrict(Mode other) {
        if (other == null) {
            throw new NullPointerException("other");
        }
        if (this == FROZEN || other == FROZEN) {
            return FROZEN;
        }
        if (this == FIXED || other == FIXED) {
            return FIXED;
        }
        return DYNAMIC;
    }
}
