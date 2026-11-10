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
 * Shared metric names used by precompute providers and the COP planner.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/precompute/PrecomputeMetrics.java}
 * blob {@code ecc1c9ed552c8cd8c90da453403dacaf21a9fe08} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PrecomputeMetrics {
    public static final String BUILD_MICROS = "buildMicros";
    public static final String QUERY_MICROS = "queryMicros";
    public static final String RETAINED_BYTES = CapabilityProfile.MEMORY_BYTES;
    public static final String REUSE_SCORE = "reuseScore";
    public static final String LOCALITY_SCORE = "localityScore";
    public static final String EXACTNESS_SCORE = "exactnessScore";

    private PrecomputeMetrics() {
    }
}
