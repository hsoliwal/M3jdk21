// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * M3JDK21 target-path scope adapter.
 *
 * <p>Java-source scope inference delegates to canonical Synexia. M3JDK21 retains only the
 * target-specific inference required for non-Java OpenJDK build/resources paths such as make
 * fragments, completion files and documentation.</p>
 */
public final class M3ScopeInference {
    private M3ScopeInference() {}

    /** Canonical Java-source scope inference supplied by Synexia. */
    public static M3EditScope forJavaPaths(Collection<String> paths) {
        Objects.requireNonNull(paths, "paths");
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("at least one Java target is required");
        }
        List<String> normalized = paths.stream().map(M3ScopeInference::normalizedPath).toList();
        if (normalized.stream().anyMatch(path -> !path.endsWith(".java"))) {
            throw new IllegalArgumentException("all targets must be Java source paths");
        }
        return M3EditScope.fromCanonical(
                com.synexia.rewrite.M3ScopeInference.inferJavaTargets(normalized));
    }

    /**
     * Infer the narrowest physical boundary for exact repository-relative targets.
     *
     * <p>All-Java batches delegate to Synexia. Mixed/non-Java target batches stay here because
     * OpenJDK build/layout files are a target-repository concern rather than a reusable Java recipe
     * rule.</p>
     */
    public static M3EditScope forPaths(Collection<String> paths) {
        Objects.requireNonNull(paths, "paths");
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("at least one target is required");
        }

        List<String> normalized = paths.stream().map(M3ScopeInference::normalizedPath).toList();
        if (normalized.stream().allMatch(path -> path.endsWith(".java"))) {
            return forJavaPaths(normalized);
        }

        Iterator<String> iterator = normalized.iterator();
        Location first = location(iterator.next());
        if (!iterator.hasNext()) {
            return M3EditScope.FILE;
        }

        boolean sameModule = true;
        boolean samePackageBoundary = true;
        while (iterator.hasNext()) {
            Location next = location(iterator.next());
            if (!first.module().equals(next.module())) {
                sameModule = false;
                samePackageBoundary = false;
            } else if (!first.sourceRoot().equals(next.sourceRoot())
                    || !first.packageDirectory().equals(next.packageDirectory())) {
                samePackageBoundary = false;
            }
        }

        if (!sameModule) {
            return M3EditScope.MULTI_MODULE;
        }
        return samePackageBoundary ? M3EditScope.PACKAGE : M3EditScope.MODULE;
    }

    private static String normalizedPath(String value) {
        Objects.requireNonNull(value, "path");
        String path = value.replace('\\', '/');
        if (path.isBlank()
                || path.startsWith("/")
                || path.matches("^[A-Za-z]:/.*")
                || path.indexOf(0) >= 0
                || path.contains("//")
                || path.contains("/../")
                || path.startsWith("../")
                || path.endsWith("/..")
                || path.contains("/./")
                || path.startsWith("./")) {
            throw new IllegalArgumentException(
                    "canonical repository-relative path required: " + value);
        }
        return path;
    }

    private static Location location(String path) {
        if (path.startsWith("make/modules/")) {
            String[] parts = path.split("/", -1);
            if (parts.length < 4 || parts[2].isBlank()) {
                throw new IllegalArgumentException("target path must name a module build file");
            }
            String module = parts[2];
            String root = "make/modules/" + module + "/";
            return fromRelative(module, root, path.substring(root.length()));
        }

        if (path.startsWith("make/")) {
            return fromRelative("<build>", "make/", path.substring("make/".length()));
        }

        int slash = path.indexOf('/');
        String root = slash < 0 ? "" : path.substring(0, slash + 1);
        String relative = slash < 0 ? path : path.substring(slash + 1);
        String module = slash < 0 ? "<repository>" : "<" + path.substring(0, slash) + ">";
        return fromRelative(module, root, relative);
    }

    private static Location fromRelative(String module, String sourceRoot, String relative) {
        if (relative.isBlank() || relative.endsWith("/")) {
            throw new IllegalArgumentException("target path must name a file");
        }
        int slash = relative.lastIndexOf('/');
        String packageDirectory = slash < 0 ? "" : relative.substring(0, slash);
        return new Location(module, sourceRoot, packageDirectory);
    }

    private record Location(String module, String sourceRoot, String packageDirectory) {}
}
