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

import java.util.Optional;

/**
 * Explicit canonicalization scope for immutable values.
 *
 * <p>Implementations never infer immutability. A caller must not mutate a value, its canonical
 * key, or any state participating in equality after admission. Reference equality is meaningful
 * only inside the documented lifetime/scope of the concrete pool.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/canonical/CanonicalPool.java}
 * blob {@code 64dbfcb802327f7e7093c780c94ad8ce9668b93f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface CanonicalPool<T> {
    T intern(T candidate);

    Optional<T> find(T candidate);

    long poolId();

    long generation();

    int size();

    long retainedWeight();

    CanonicalPoolStats stats();

    /** Explicit identity-generation break. Implementations do not evict silently. */
    void clear();
}
