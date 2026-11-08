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
package jdk.internal.m3.collections.shape;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Shared lazy dense path/payload lanes; subclasses own admission and search.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/AbstractPrefixSet.java}
 * blob {@code c8b5b82acb671c107e9598f381ed8a50e6a2ccc6} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
abstract class AbstractPrefixSet<E> extends ShapeGuard implements ShapePrefixSet<E> {
    private static final String[] EMPTY_PATHS = new String[0];
    private static final Object[] EMPTY_VALUES = new Object[0];
    String[] paths = EMPTY_PATHS;
    Object[] values = EMPTY_VALUES;
    int size;
    AbstractPrefixSet(CollectionRequirements required) { super(required); }
    abstract int exact(String path);
    final void room(int minimum) {
        if (minimum <= paths.length) { return; }
        int capacity = grown(paths.length, minimum, required.maximumSize());
        String[] nextPaths = Arrays.copyOf(paths, capacity);
        Object[] nextValues = Arrays.copyOf(values, capacity);
        paths = nextPaths; values = nextValues;
    }
    final void eraseRange(int start, int end) {
        int removed = end - start;
        System.arraycopy(paths, end, paths, start, size - end);
        System.arraycopy(values, end, values, start, size - end);
        Arrays.fill(paths, size - removed, size, null);
        Arrays.fill(values, size - removed, size, null);
        size -= removed;
    }
    @Override public final int size() { return size; }
    @Override public final boolean containsPath(String path) { PrefixPaths.validate(path); return exact(path) >= 0; }
    @Override public final boolean removePath(String path) {
        writable(); PrefixPaths.validate(path); int at = exact(path);
        if (at < 0) { return false; } eraseRange(at, at + 1); return true;
    }
    @Override public final void clear() { writable(); paths = EMPTY_PATHS; values = EMPTY_VALUES; size = 0; }
    @Override public final ShapeStorage storage() { return new ShapeStorage(2L * paths.length, 0, paths.length == 0 ? 0 : 2, 0); }
    @Override public final void forEach(BiConsumer<? super String, ? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try { for (int i = 0; i < size; i++) { action.accept(paths[i], read(values, i)); } }
        finally { leave(); }
    }
}
