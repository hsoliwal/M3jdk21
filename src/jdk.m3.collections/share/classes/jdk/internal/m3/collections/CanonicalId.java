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
 * Scoped dense identifier for a value admitted to an {@link IndexedCanonicalPool}.
 *
 * <p>The local id is meaningful only together with the pool id and generation. Calling
 * {@link CanonicalPool#clear()} starts a new generation and intentionally invalidates ids from
 * the previous generation.</p>
 *
 * <p>This identifier is not a Java heap address, a JNI reference, an identity hash, or a native
 * pointer. Its pool id belongs to the allocating registry/class-loader lifetime; it is not a
 * portable persisted or cross-process identity. Mapped/IPC descriptors must instead retain the
 * existing authoritative image/namespace and generation and resolve their IDs in that owner.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/canonical/CanonicalId.java}
 * blob {@code 0734bb4ac31115a3a4d58ab302e4a2cfb34e47f7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record CanonicalId(long poolId, long generation, int localId) {
    public CanonicalId {
        if (poolId <= 0) {
            throw new IllegalArgumentException("poolId must be positive");
        }
        if (generation <= 0) {
            throw new IllegalArgumentException("generation must be positive");
        }
        if (localId < 0) {
            throw new IllegalArgumentException("localId must be non-negative");
        }
    }
}
