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

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Dense object-reference set that keeps only non-dominated elements.
 *
 * <p>If an existing element dominates a candidate, the candidate is rejected.
 * If the candidate dominates existing elements, those narrower elements are
 * compacted away before the candidate is appended. Storage is one Object[]
 * reference lane; there is no hash node, tree node or wrapper per element.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/DenseDominanceSet.java}
 * blob {@code 1e03e2f85a749a4fde9aa9bc9a2857467c6eb50f} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3DenseDominanceSet<E> {
    private Object[] elements;
    private int size;
    private final BiPredicate<? super E, ? super E> dominates;

    public M3DenseDominanceSet(BiPredicate<? super E, ? super E> dominates) {
        this(8, dominates);
    }

    public M3DenseDominanceSet(
            int initialCapacity, BiPredicate<? super E, ? super E> dominates) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        this.dominates = Objects.requireNonNull(dominates, "dominates");
        elements = new Object[Math.max(1, initialCapacity)];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int capacity() { return elements.length; }
    public int referenceSlots() { return elements.length; }

    public boolean add(E candidate) {
        Objects.requireNonNull(candidate, "candidate");

        for (int i = 0; i < size; i++) {
            E existing = elementAt(i);
            if (Objects.equals(existing, candidate) || dominates.test(existing, candidate)) {
                return false;
            }
        }

        int write = 0;
        for (int read = 0; read < size; read++) {
            E existing = elementAt(read);
            if (!dominates.test(candidate, existing)) {
                elements[write++] = existing;
            }
        }
        Arrays.fill(elements, write, size, null);
        ensure(write + 1);
        elements[write] = candidate;
        size = write + 1;
        return true;
    }

    public int addAll(Iterable<? extends E> source) {
        Objects.requireNonNull(source, "source");
        int added = 0;
        for (E element : source) if (add(element)) added++;
        return added;
    }

    public boolean contains(Object value) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(elements[i], value)) return true;
        }
        return false;
    }

    public boolean remove(Object value) {
        for (int i = 0; i < size; i++) {
            if (!Objects.equals(elements[i], value)) continue;
            int moved = size - i - 1;
            if (moved > 0) System.arraycopy(elements, i + 1, elements, i, moved);
            elements[--size] = null;
            return true;
        }
        return false;
    }

    public E get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return elementAt(index);
    }

    public void clear() {
        Arrays.fill(elements, 0, size, null);
        size = 0;
    }

    public void forEach(Consumer<? super E> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < size; i++) consumer.accept(elementAt(i));
    }

    public Object[] toArray() {
        return Arrays.copyOf(elements, size);
    }

    @SuppressWarnings("unchecked")
    private E elementAt(int index) {
        return (E) elements[index];
    }

    private void ensure(int required) {
        if (required <= elements.length) return;
        int next = elements.length + (elements.length >>> 1) + 1;
        if (next < required) next = required;
        elements = Arrays.copyOf(elements, next);
    }
}
