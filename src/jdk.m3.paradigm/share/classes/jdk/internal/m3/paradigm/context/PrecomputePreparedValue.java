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

import java.util.Objects;

/**
 * Domain-owned prepared value plus exact retained-byte declaration and replay root.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/precompute/PrecomputePreparedValue.java}
 * blob {@code 987a9d131ea1f35572de2042ebcd56017f4e46d6} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record PrecomputePreparedValue(
        Object value,
        long retainedBytes,
        String artifactRoot) {

    /** Explicit sentinel when the owner cannot prove retained bytes. */
    public static final long UNKNOWN_RETAINED_BYTES = -1L;

    public PrecomputePreparedValue {
        value = Objects.requireNonNull(value, "value");
        if (retainedBytes < UNKNOWN_RETAINED_BYTES) {
            throw new IllegalArgumentException("retainedBytes < -1");
        }
        PrecomputeHashes.requireSha256(artifactRoot, "artifactRoot");
    }

    /** Public clean-room sealing helper for downstream provider modules. */
    public static PrecomputePreparedValue sealed(
            Object value,
            long retainedBytes,
            String schema,
            String... fields) {
        return new PrecomputePreparedValue(
                value,
                retainedBytes,
                PrecomputeHashes.sha256(
                        Objects.requireNonNull(schema, "schema"),
                        fields));
    }
}
