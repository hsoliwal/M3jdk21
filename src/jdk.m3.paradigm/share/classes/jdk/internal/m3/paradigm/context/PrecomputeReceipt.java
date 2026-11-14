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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Exact local execution receipt; the prepared object itself is intentionally not serialized.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/precompute/PrecomputeReceipt.java}
 * blob {@code 263934b75bc21b5d6defea993d52d984dd4ec3a9} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record PrecomputeReceipt(
        String requestId,
        String providerId,
        String bindingId,
        String catalogRoot,
        String planRoot,
        String artifactRoot,
        long retainedBytes,
        String receiptRoot) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public PrecomputeReceipt {
        requestId = Objects.requireNonNull(requestId, "requestId");
        providerId = Objects.requireNonNull(providerId, "providerId");
        bindingId = Objects.requireNonNull(bindingId, "bindingId");
        PrecomputeHashes.requireSha256(catalogRoot, "catalogRoot");
        PrecomputeHashes.requireSha256(planRoot, "planRoot");
        PrecomputeHashes.requireSha256(artifactRoot, "artifactRoot");
        if (retainedBytes < PrecomputePreparedValue.UNKNOWN_RETAINED_BYTES) {
            throw new IllegalArgumentException("retainedBytes < -1");
        }
        PrecomputeHashes.requireSha256(receiptRoot, "receiptRoot");
    }
}
