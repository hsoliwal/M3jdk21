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
package jdk.m3.collections;

/**
 * Runtime boundary for a provider callback failure.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ProviderLoadException.java}
 * blob {@code 08dce2e4b62bd126ad1750febdefd075eba37e53} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3ProviderLoadException extends RuntimeException {
    // Preserve the historical computed UID; checked by the exact preimage fixture.
    private static final long serialVersionUID = 8132423011135672925L;

    private final long logicalId;

    public M3ProviderLoadException(long logicalId, Throwable cause) {
        super("Provider load failed for logical id " + logicalId, cause);
        this.logicalId = logicalId;
    }

    public long logicalId() {
        return logicalId;
    }
}
