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

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * LIFO semantics over the shared exact-width primitive list core.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveStack.java}
 * blob {@code 82fc712bd14c25d2fa7058b7de7d05aa806e557b} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveStack {
    private final M3PrimitiveArrayList values;

    public M3PrimitiveStack(M3PrimitiveKind kind) {
        values = new M3PrimitiveArrayList(Objects.requireNonNull(kind, "kind"));
    }

    public M3PrimitiveKind kind() { return values.kind(); }
    public int size() { return values.size(); }
    public boolean isEmpty() { return values.isEmpty(); }
    public long payloadBytes() { return values.payloadBytes(); }

    public void pushBits(long bits) { values.addBits(bits); }

    public long peekBits() {
        if (values.isEmpty()) throw new NoSuchElementException();
        return values.getBits(values.size() - 1);
    }

    public long popBits() {
        if (values.isEmpty()) throw new NoSuchElementException();
        return values.removeAtBits(values.size() - 1);
    }

    public void clear() { values.clear(); }
}
