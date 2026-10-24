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
// Modified 2026: array/index/mask key-only storage, without stored Entry objects.
package com.m3.collections;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * Set with a single key reference plane, primitive hash offsets and occupancy
 * masks. It does not allocate Map.Entry or per-element Node objects.
 */
public final class M3HashSet<E> extends AbstractSet<E>
        implements Cloneable, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final M3HashMap<E, Object> keys;

    public M3HashSet() { keys = new M3HashMap<>(true); }

    public M3HashSet(Collection<? extends E> elements) {
        this();
        addAll(Objects.requireNonNull(elements, "elements"));
    }

    @Override public int size() { return keys.size(); }
    @Override public boolean contains(Object item) { return keys.containsKey(item); }
    @Override public boolean add(E item) {
        if (keys.findSlot(item) >= 0) return false;
        keys.put(item, null);
        return true;
    }
    @Override public boolean remove(Object item) {
        int slot = keys.findSlot(item);
        if (slot < 0) return false;
        keys.eraseAt(slot);
        return true;
    }
    @Override public void clear() { keys.clear(); }
    @Override public Iterator<E> iterator() { return keys.slots(0); }

    /** Physical key references; there is no value plane or stored entry. */
    public int allocatedReferenceSlots() { return keys.allocatedReferenceSlots(); }

    public void verifyInvariants() { keys.verifyInvariants(); }

    @Override public M3HashSet<E> clone() {
        M3HashSet<E> copy = new M3HashSet<>();
        for (int slot = keys.firstSlot(); slot >= 0; slot = keys.nextSlot(slot))
            copy.add(keys.keyAt(slot));
        return copy;
    }
}
