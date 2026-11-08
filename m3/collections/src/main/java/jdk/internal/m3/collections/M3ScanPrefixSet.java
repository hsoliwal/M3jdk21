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

/**
 * Small-set scan; compacts surviving entries once when a new parent subsumes children.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ScanPrefixSet.java}
 * blob {@code 0677b7319edd21b9de357ea9dd9b89e407302285} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3ScanPrefixSet<E> extends M3AbstractPrefixSet<E> {
    M3ScanPrefixSet(M3CollectionRequirements required) { super(required); }
    @Override int exact(String path) {
        for (int i = 0; i < size; i++) { if (paths[i].equals(path)) { return i; } }
        return -1;
    }
    @Override public boolean covers(String path) {
        M3PrefixPaths.validate(path);
        for (int i = 0; i < size; i++) { if (M3PrefixPaths.covers(paths[i], path)) { return true; } }
        return false;
    }
    @Override public boolean add(String path, E value) {
        writable(); M3PrefixPaths.validate(path); element(value);
        int removed = 0;
        for (int i = 0; i < size; i++) {
            if (M3PrefixPaths.covers(paths[i], path)) { return false; }
            if (M3PrefixPaths.covers(path, paths[i])) { removed++; }
        }
        growing(size - removed); room(size - removed + 1);
        int write = 0;
        for (int i = 0; i < size; i++) {
            if (!M3PrefixPaths.covers(path, paths[i])) { paths[write] = paths[i]; values[write++] = values[i]; }
        }
        Arrays.fill(paths, write, size, null); Arrays.fill(values, write, size, null);
        paths[write] = path; values[write] = value; size = write + 1; return true;
    }
}
