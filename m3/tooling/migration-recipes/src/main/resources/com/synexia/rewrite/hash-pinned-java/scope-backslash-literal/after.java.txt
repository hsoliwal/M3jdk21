// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * Conservative physical locality inference for Maven and OpenJDK repository paths.
 *
 * <p>Path locality can infer FILE/PACKAGE/MODULE/MULTI_MODULE only. VISIBILITY and LIBRARY_API
 * remain explicit semantic authorities and are never inferred from filenames.</p>
 */
public final class M3ScopeInference {
    private M3ScopeInference() {}

    /** Strict Java-source compatibility API. */
    public static M3EditScope forJavaPaths(Collection<String> paths) {
        Objects.requireNonNull(paths, "paths");
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("at least one Java target is required");
        }
        for (String path : paths) {
            String normalized = normalizedPath(path);
            if (!normalized.endsWith(".java")) {
                throw new IllegalArgumentException("not a Java source path: " + path);
            }
        }
        return forPaths(paths);
    }

    /** Infer the narrowest physical boundary for exact repository-relative targets. */
    public static M3EditScope forPaths(Collection<String> paths) {
        Objects.requireNonNull(paths, "paths");
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("at least one target is required");
        }

        Iterator<String> iterator = paths.iterator();
        Location first = location(normalizedPath(iterator.next()));
        if (!iterator.hasNext()) {
            return M3EditScope.FILE;
        }

        boolean sameModule = true;
        boolean samePackageBoundary = true;
        while (iterator.hasNext()) {
            Location next = location(normalizedPath(iterator.next()));
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
        if (path.startsWith("src/main/java/")) {
            return fromRelative("<current>", "src/main/java/", path.substring("src/main/java/".length()));
        }
        if (path.startsWith("src/test/java/")) {
            return fromRelative("<current>", "src/test/java/", path.substring("src/test/java/".length()));
        }

        if (path.startsWith("src/")) {
            String[] parts = path.split("/", -1);
            if (parts.length >= 5 && "classes".equals(parts[3])) {
                String module = parts[1];
                String root = "src/" + parts[1] + "/" + parts[2] + "/classes/";
                if (path.length() <= root.length()) {
                    throw new IllegalArgumentException("target path must name a file");
                }
                String relative = path.substring(root.length());
                return fromRelative(module, root, relative);
            }
            if (parts.length >= 2 && !parts[1].isBlank()) {
                String module = parts[1];
                String root = "src/" + module + "/";
                if (path.length() <= root.length()) {
                    throw new IllegalArgumentException("target path must name a file");
                }
                return fromRelative(module, root, path.substring(root.length()));
            }
            throw new IllegalArgumentException("target path must name a file");
        }

        if (path.startsWith("test/")) {
            String[] parts = path.split("/", -1);
            String family = parts.length > 1 && !parts[1].isBlank() ? parts[1] : "test";
            String root = "test/" + family + "/";
            String relative = path.length() > root.length() ? path.substring(root.length()) : "";
            return fromRelative("<jdk-test:" + family + ">", root, relative);
        }

        if (path.startsWith("make/modules/")) {
            String[] parts = path.split("/", -1);
            String module = parts.length > 2 && !parts[2].isBlank() ? parts[2] : "<build>";
            String root = "make/modules/" + module + "/";
            String relative = path.length() > root.length() ? path.substring(root.length()) : "";
            return fromRelative(module, root, relative);
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
