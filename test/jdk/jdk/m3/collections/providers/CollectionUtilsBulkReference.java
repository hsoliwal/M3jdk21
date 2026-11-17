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
package jdk.m3.collections.providers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * The pre-patch code path of CollectionUtils.containsAll, removeCount, removeCountRest and
 * removeRange. Statements are verbatim; the only edits are a behaviour-preserving method split
 * (the inner coll1 scan of containsAll) and long message literals wrapped as compile-time
 * constant concatenations, so every message is the same string.
 */
final class CollectionUtilsBulkReference {

    private CollectionUtilsBulkReference() {
    }

    static boolean containsAll(final Collection<?> coll1, final Collection<?> coll2) {
        Objects.requireNonNull(coll1, "coll1");
        Objects.requireNonNull(coll2, "coll2");
        if (coll2.isEmpty()) {
            return true;
        }
        final Set<Object> elementsAlreadySeen = new HashSet<>();
        for (final Object nextElement : coll2) {
            if (elementsAlreadySeen.contains(nextElement)) {
                continue;
            }
            if (!scanCurrent(coll1, nextElement, elementsAlreadySeen)) {
                return false;
            }
        }
        return true;
    }

    private static boolean scanCurrent(final Collection<?> coll1, final Object nextElement,
            final Set<Object> elementsAlreadySeen) {
        boolean foundCurrentElement = false;
        for (final Object p : coll1) {
            elementsAlreadySeen.add(p);
            if (Objects.equals(nextElement, p)) {
                foundCurrentElement = true;
                break;
            }
        }
        return foundCurrentElement;
    }

    static <E> Collection<E> removeCount(final Collection<E> input, int startIndex, int count) {
        Objects.requireNonNull(input, "input");
        if (startIndex < 0) {
            throw new IndexOutOfBoundsException("The start index can't be less than 0.");
        }
        if (count < 0) {
            throw new IndexOutOfBoundsException("The count can't be less than 0.");
        }
        if (input.size() < startIndex + count) {
            throw new IndexOutOfBoundsException("The sum of start index and count can't be "
                    + "greater than the size of collection.");
        }
        final Collection<E> result = new ArrayList<>(count);
        final Iterator<E> iterator = input.iterator();
        return removeCountRest(count, startIndex, iterator, result);
    }

    private static <E> Collection<E> removeCountRest(int count, int startIndex,
            final Iterator<E> iterator, final Collection<E> result) {
        while (count > 0) {
            if (startIndex > 0) {
                startIndex -= 1;
                iterator.next();
                continue;
            }
            count -= 1;
            result.add(iterator.next());
            iterator.remove();
        }
        return result;
    }

    static <E> Collection<E> removeRange(final Collection<E> input, final int startIndex,
            final int endIndex) {
        Objects.requireNonNull(input, "input");
        if (endIndex < startIndex) {
            throw new IllegalArgumentException("The end index can't be less than the start index.");
        }
        if (input.size() < endIndex) {
            throw new IndexOutOfBoundsException("The end index can't be greater than the size "
                    + "of collection.");
        }
        return removeCount(input, startIndex, endIndex - startIndex);
    }
}
