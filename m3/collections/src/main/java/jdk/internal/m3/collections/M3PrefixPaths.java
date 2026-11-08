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

/**
 * Allocation-free canonical path validation and hierarchy-compatible comparison.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/PrefixPaths.java}
 * blob {@code 372d41e8fcd02e5769c344d0cba54c05b4d4e61f} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3PrefixPaths {
    private M3PrefixPaths() { }
    static void validate(String path) {
        Objects.requireNonNull(path, "path");
        if (path.isEmpty() || path.charAt(0) != '/') { throw new IllegalArgumentException("absolute path required"); }
        if (path.length() == 1) { return; }
        int start = 1;
        for (int at = 1; at <= path.length(); at++) {
            if (at == path.length() || path.charAt(at) == '/') {
                int length = at - start;
                if (length == 0 || (length == 1 && path.charAt(start) == '.')
                        || (length == 2 && path.charAt(start) == '.' && path.charAt(start + 1) == '.')) {
                    throw new IllegalArgumentException("noncanonical path segment");
                }
                start = at + 1;
            }
        }
    }
    static boolean covers(String parent, String child) {
        return parent.equals("/") || (child.startsWith(parent)
                && (child.length() == parent.length() || child.charAt(parent.length()) == '/'));
    }
    static int compare(String left, String right) {
        int limit = Math.min(left.length(), right.length());
        for (int i = 0; i < limit; i++) {
            char a = left.charAt(i), b = right.charAt(i);
            if (a != b) { return a == '/' ? -1 : b == '/' ? 1 : Character.compare(a, b); }
        }
        return Integer.compare(left.length(), right.length());
    }
}
