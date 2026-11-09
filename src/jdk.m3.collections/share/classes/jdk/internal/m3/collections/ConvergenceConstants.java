/* *****************************************************************************
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


/**
 * Constants for Discrete Integral Convergence Engine.
 *
 * <p>
 * Extracted from {@link DiscreteIntegralEngine} to separate concerns and
 * improve maintainability. All strings are interned for memory efficiency.
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/ConvergenceConstants.java}
 * blob {@code 2b1cd7ea1f7ea461311daca26b86e60892a1a8ff} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ConvergenceConstants {

    private ConvergenceConstants() {
        // Constants only
    }

    // --- INTERNED STRING CONSTANTS ---

    private static final TokenInterner INTERNER = TokenInterner.getInstance();

    /** Header for memory display. */
    public static final String HEADER_MEMORY = INTERNER.intern("=== Convergence Memory ===");

    /** Format string for statistics. */
    public static final String FMT_STATS = INTERNER.intern("Passes: %d | Events: %d | Chunks: %d");

    /** Format string for convergence status. */
    public static final String FMT_CONVERGENCE = INTERNER.intern("Converged: {} | Progress: {}%");

    /** Indent prefix for output. */
    public static final String PREFIX_INDENT = INTERNER.intern("  ");

    /** Header for demo output. */
    public static final String HEADER_DEMO = INTERNER.intern("Discrete Multi-Pass Convergence Demo");

    /** Message explaining memory reaching. */
    public static final String MSG_MEMORY_REACHED =
            INTERNER.intern("Memory is not stored - it is REACHED through convergence.");

    /** Header for event log section. */
    public static final String HEADER_EVENT_LOG = INTERNER.intern("\nEvent Log:");

    /** Separator line (50 chars). */
    public static final String SEPARATOR_50 = INTERNER.intern("-".repeat(50));

    /** Separator line (60 chars). */
    public static final String SEPARATOR_60 = INTERNER.intern("=".repeat(60));

    // --- DEFAULT VALUES ---

    /** Default convergence energy threshold. */
    public static final double DEFAULT_CONVERGENCE_THRESHOLD = 0.1;

    /** Default convergence window (consecutive quiet passes). */
    public static final int DEFAULT_CONVERGENCE_WINDOW = 3;

    /** Default drop threshold. */
    public static final double DEFAULT_DROP_THRESHOLD = 0.3;

    /** Default decay lambda. */
    public static final double DEFAULT_DECAY_LAMBDA = 0.9;

    /** Default fluid recall weight. */
    public static final double DEFAULT_FLUID_RECALL_WEIGHT = 0.2;

    // --- DEMO VALUES ---

    /** Demo decay lambda. */
    public static final double DEMO_DECAY_LAMBDA = 0.85;

    /** Demo drop threshold. */
    public static final double DEMO_DROP_THRESHOLD = 0.2;

    /** Demo convergence threshold. */
    public static final double DEMO_CONVERGENCE_THRESHOLD = 0.15;

    /** Demo convergence window. */
    public static final int DEMO_CONVERGENCE_WINDOW = 3;

    /** Demo max passes. */
    public static final int DEMO_MAX_PASSES = 10;
}
