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

import java.util.Objects;
import java.util.function.LongPredicate;
import jdk.m3.collections.M3PrimitiveArrayList;
import jdk.m3.collections.M3PrimitiveKind;

/**
 * Admission-filtered primitive list; rejected values are never appended to retained storage.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveFilteredList.java}
 * blob {@code 24399d62b77cf11b135878b31f041801be92b29d} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveFilteredList {
    private final M3PrimitiveArrayList delegate;
    private final LongPredicate admission;

    public M3PrimitiveFilteredList(M3PrimitiveKind kind, LongPredicate admission) {
        this(new M3PrimitiveArrayList(kind), admission);
    }

    public M3PrimitiveFilteredList(M3PrimitiveArrayList delegate, LongPredicate admission) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.admission = Objects.requireNonNull(admission, "admission");
    }

    public M3PrimitiveKind kind() { return delegate.kind(); }
    public int size() { return delegate.size(); }
    public boolean isEmpty() { return delegate.isEmpty(); }
    public long payloadBytes() { return delegate.payloadBytes(); }

    public boolean addBits(long bits) {
        if (!admission.test(bits)) return false;
        delegate.addBits(bits);
        return true;
    }

    public long getBits(int index) { return delegate.getBits(index); }
    public long removeAtBits(int index) { return delegate.removeAtBits(index); }
    public void clear() { delegate.clear(); }
    public Object toPrimitiveArray() { return delegate.toPrimitiveArray(); }
}
