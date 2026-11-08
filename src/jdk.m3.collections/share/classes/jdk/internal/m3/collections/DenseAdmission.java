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

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Explicit filtered admission into a supplied collection, without altering its API.
 *
 * <p>No retained wrapper per element and no read-time filtering. The predicate runs
 * once before each attempted insertion. offerAll is sequential, not transactional.
 * The read-only view is live. A separate alias to the backing collection can bypass
 * this adapter; this is not an ownership/security boundary. Thread safety is that
 * of the backing collection and predicate, not a promise made by this adapter.
 *
 * @param <E> element type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/DenseAdmission.java}
 * blob {@code 0b94d3ebd5e9e9474814fa25f8bf73a38be3a202} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DenseAdmission<E> implements Admission<E> {
    private final Collection<E> backing;
    private final Collection<E> view;
    private final Predicate<? super E> predicate;

    public DenseAdmission(Collection<E> backing, Predicate<? super E> predicate) {
        this.backing = Objects.requireNonNull(backing, "backing");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.view = Collections.unmodifiableCollection(backing);
    }

    public boolean offer(E element) {
        return predicate.test(element) && backing.add(element);
    }

    public int offerAll(Iterable<? extends E> elements) {
        Objects.requireNonNull(elements, "elements");
        int accepted = 0;
        for (E element : elements) {
            if (offer(element)) { accepted++; }
        }
        return accepted;
    }

    public int size() { return backing.size(); }
    public Collection<E> view() { return view; }
    public boolean remove(Object element) { return backing.remove(element); }
    public void clear() { backing.clear(); }
}
