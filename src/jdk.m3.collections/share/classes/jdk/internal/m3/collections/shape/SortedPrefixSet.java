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
package jdk.internal.m3.collections.shape;

/**
 * Sorted antichain: coverage uses the predecessor; descendants occupy one contiguous range.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/SortedPrefixSet.java}
 * blob {@code e78b163b86f7459fcf19aa016b735483762399ff} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class SortedPrefixSet<E> extends AbstractPrefixSet<E> {
    SortedPrefixSet(CollectionRequirements required) { super(required); }
    private int lowerBound(String path) {
        int low = 0, high = size;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (PrefixPaths.compare(paths[mid], path) < 0) { low = mid + 1; } else { high = mid; }
        }
        return low;
    }
    @Override int exact(String path) {
        int at = lowerBound(path); return at < size && paths[at].equals(path) ? at : -1;
    }
    private boolean covered(String path, int at) {
        return (at < size && paths[at].equals(path)) || (at > 0 && PrefixPaths.covers(paths[at - 1], path));
    }
    @Override public boolean covers(String path) { PrefixPaths.validate(path); return covered(path, lowerBound(path)); }
    @Override public boolean add(String path, E value) {
        writable(); PrefixPaths.validate(path); element(value); int at = lowerBound(path);
        if (covered(path, at)) { return false; }
        int end = at;
        while (end < size && PrefixPaths.covers(path, paths[end])) { end++; }
        int next = size - (end - at) + 1;
        growing(next - 1); room(next); eraseRange(at, end);
        System.arraycopy(paths, at, paths, at + 1, size - at);
        System.arraycopy(values, at, values, at + 1, size - at);
        paths[at] = path; values[at] = value; size++; return true;
    }
}
