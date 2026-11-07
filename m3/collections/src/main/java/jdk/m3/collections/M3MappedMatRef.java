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

import java.util.Objects;

/**
 * O(1) process-local handle into one read-only mapped immutable pool artifact.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MappedMatRef.java}
 * blob {@code cb8c7d251ebfd7e390cf9755a134d60b05f930bf} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MappedMatRef<T> {
    private final M3MappedMatPool<T> pool;
    private final int id;

    M3MappedMatRef(M3MappedMatPool<T> pool, int id) {
        this.pool = Objects.requireNonNull(pool, "pool");
        this.id = id;
    }

    public M3MappedMatPool<T> pool() { return pool; }
    public int id() { return id; }
    public T value() { return pool.value(id); }
    public byte[] canonicalBytes() { return pool.bytes(id); }
    public long contentHash64() { return pool.contentHash64(id); }
    public M3MatPortableRef portableRef() { return pool.portableRef(id); }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof M3MappedMatRef<?> ref && pool == ref.pool && id == ref.id;
    }

    @Override
    public int hashCode() { return 31 * System.identityHashCode(pool) + id; }
}
