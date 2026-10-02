// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * Conservative locality inference for module-relative Java source paths.
 *
 * <p>This helper intentionally infers only physical target locality. VISIBILITY cannot be
 * inferred from paths and must be declared explicitly by a recipe that changes declaration
 * visibility. MULTI_MODULE and LIBRARY_API are orchestration decisions and must also be declared
 * explicitly by the coordinating recipe or task packet.
 */
public final class M3ScopeInference {
    private static final String MAIN = "src/main/java/";
    private static final String TEST = "src/test/java/";

    private M3ScopeInference() {}

    public static M3EditScope forJavaPaths(Collection<String> paths) {
        Objects.requireNonNull(paths, "paths");
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("at least one Java target is required");
        }

        Iterator<String> iterator = paths.iterator();
        String first = normalizedJavaPath(iterator.next());
        if (!iterator.hasNext()) {
            return M3EditScope.FILE;
        }

        String firstSourceRoot = sourceRoot(first);
        String firstPackage = packageDirectory(first);
        boolean samePackageBoundary = true;
        while (iterator.hasNext()) {
            String path = normalizedJavaPath(iterator.next());
            if (!firstSourceRoot.equals(sourceRoot(path))
                    || !firstPackage.equals(packageDirectory(path))) {
                samePackageBoundary = false;
            }
        }
        return samePackageBoundary ? M3EditScope.PACKAGE : M3EditScope.MODULE;
    }

    private static String normalizedJavaPath(String value) {
        Objects.requireNonNull(value, "path");
        String path = value.replace('\\', '/');
        if (!(path.startsWith(MAIN) || path.startsWith(TEST)) || !path.endsWith(".java")) {
            throw new IllegalArgumentException("not a module-relative Java source path: " + value);
        }
        if (path.contains("/../") || path.endsWith("/..") || path.contains("/./")) {
            throw new IllegalArgumentException("non-canonical Java source path: " + value);
        }
        return path;
    }

    private static String sourceRoot(String path) {
        return path.startsWith(MAIN) ? MAIN : TEST;
    }

    private static String packageDirectory(String path) {
        String root = sourceRoot(path);
        String relative = path.substring(root.length());
        int slash = relative.lastIndexOf('/');
        return slash < 0 ? "" : relative.substring(0, slash);
    }
}
