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
 * Serializable remote-safe view of one immutable precompute catalog generation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/precompute/PrecomputeCatalogSnapshot.java}
 * blob {@code 4a741720bbe33a7ecc5d7b80b3abfb2dc2fc40ff} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record PrecomputeCatalogSnapshot(
        long generation,
        String rootHash,
        String[] providerIds,
        String[] bindingIds,
        String[] capabilities) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public PrecomputeCatalogSnapshot {
        if (generation < 0L) throw new IllegalArgumentException("generation < 0");
        PrecomputeHashes.requireSha256(rootHash, "rootHash");
        providerIds = Objects.requireNonNull(providerIds, "providerIds").clone();
        bindingIds = Objects.requireNonNull(bindingIds, "bindingIds").clone();
        capabilities = Objects.requireNonNull(capabilities, "capabilities").clone();
        if (providerIds.length != bindingIds.length || bindingIds.length != capabilities.length) {
            throw new IllegalArgumentException("catalog lane length mismatch");
        }
    }

    @Override
    public String[] providerIds() { return providerIds.clone(); }

    @Override
    public String[] bindingIds() { return bindingIds.clone(); }

    @Override
    public String[] capabilities() { return capabilities.clone(); }
}
