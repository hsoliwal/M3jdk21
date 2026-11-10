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

import jdk.m3.collections.PrimitiveArrayDeque;
import jdk.m3.collections.PrimitiveKind;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * FIFO semantics over the shared exact-width primitive deque core.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveQueue.java}
 * blob {@code d91909442e2ed41cbfadc80a412b8919b9be9aba} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (primitives queue facade after API_UNION U5 (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PrimitiveQueue {
    private final PrimitiveArrayDeque values;

    public PrimitiveQueue(PrimitiveKind kind) {
        values = new PrimitiveArrayDeque(Objects.requireNonNull(kind, "kind"));
    }

    public PrimitiveKind kind() { return values.kind(); }
    public int size() { return values.size(); }
    public boolean isEmpty() { return values.isEmpty(); }
    public long payloadBytes() { return values.payloadBytes(); }

    public void offerBits(long bits) { values.addLastBits(bits); }

    public long peekBits() {
        if (values.isEmpty()) throw new NoSuchElementException();
        return values.peekFirstBits();
    }

    public long pollBits() {
        if (values.isEmpty()) throw new NoSuchElementException();
        return values.removeFirstBits();
    }

    public void clear() { values.clear(); }
}
