// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Narrowest physical target scope inferable from exact Java source paths. */
public final class M3ScopeInference {
    private static final String M3_CPU_ROOT = "m3/tooling/module-packs/cpu-tools/";
    private static final String M3_CPU_SOURCE_ROOT = M3_CPU_ROOT + "src/";
    private static final String M3_FILE_SOURCE_ROOT = "m3/tooling/module-packs/text-tools/src/jdk.m3.io/";
    private static final Set<String> M3_CPU_MODULES = Set.of(
            "com.m3.tools.cpu",
            "com.m3.tools.codec.varint", "com.m3.tools.constraint.interval",
            "com.m3.tools.graph.paths", "com.m3.tools.graph.topology",
            "com.m3.tools.plan.priority", "com.m3.tools.stats.streaming",
            "com.m3.tools.text.diff", "com.m3.tools.text.frequency",
            "com.m3.tools.text.index", "com.m3.tools.text.ngram",
            "com.m3.tools.text.normalize", "com.m3.tools.text.regex",
            "com.m3.tools.text.search", "com.m3.tools.text.similarity",
            "com.m3.tools.text.tokenize", "com.m3.tools.verify.sha256");

    private M3ScopeInference() {}

    public static M3EditScope inferJavaTargets(List<String> targetPaths) {
        List<String> paths = List.copyOf(Objects.requireNonNull(targetPaths, "targetPaths"));
        if (paths.isEmpty()) throw new IllegalArgumentException("targetPaths");
        if (paths.size() == 1) {
            describeJavaTarget(paths.getFirst());
            return M3EditScope.FILE;
        }

        Set<String> modules = new HashSet<>();
        Set<String> packages = new HashSet<>();
        for (String path : paths) {
            JavaLocation location = describeJavaTarget(path);
            modules.add(location.module());
            packages.add(location.module() + "|" + location.packageName());
        }
        if (modules.size() > 1) return M3EditScope.MULTI_MODULE;
        if (packages.size() > 1) return M3EditScope.MODULE;
        return M3EditScope.PACKAGE;
    }

    /**
     * Path locality cannot infer declaration visibility or exported API authority.
     *
     * <p>VISIBILITY and LIBRARY_API must be explicitly declared by the recipe/work packet.
     */
    public static M3EditScope requireExplicit(
            M3EditScope inferred,
            M3EditScope declaredFloor) {
        return Objects.requireNonNull(inferred, "inferred")
                .promote(Objects.requireNonNull(declaredFloor, "declaredFloor"));
    }

    public static JavaLocation describeJavaTarget(String value) {
        String path = Objects.requireNonNull(value, "path").replace('\\', '/');
        if (path.isBlank()
                || path.startsWith("/")
                || path.indexOf('\0') >= 0
                || path.contains("/../")
                || path.startsWith("../")
                || path.endsWith("/..")
                || !path.endsWith(".java")) {
            throw new IllegalArgumentException("normalized relative Java path required: " + value);
        }

        if (canonicalStandaloneGuard(path))
            return new JavaLocation("tools/m3-superset", "com.synexia.m3.superset");
        String module;
        String relative;

        JavaLocation cpuTools = m3CpuToolsLocation(path);
        if (cpuTools != null) return cpuTools;
        if (m3CpuToolsReservedPath(path)) {
            throw new IllegalArgumentException("unknown Java path in reserved M3 CPU layout: " + value);
        }

        if (path.startsWith("src/")) {
            String[] parts = path.split("/", -1);
            if (parts.length >= 5 && "classes".equals(parts[3])) {
                module = parts[1];
                relative = String.join("/", java.util.Arrays.copyOfRange(parts, 4, parts.length));
                return location(module, relative, value);
            }
        }

        if (path.startsWith("test/")) {
            String[] parts = path.split("/", -1);
            // startsWith("test/") guarantees the family slot, including an empty slot.
            String testFamily = parts[1];
            module = "<jdk-test:" + testFamily + ">";
            relative = parts.length > 2
                    ? String.join("/", java.util.Arrays.copyOfRange(parts, 2, parts.length))
                    : parts[parts.length - 1];
            return location(module, relative, value);
        }

        int swtManual = path.indexOf("/ManualTests/");
        if (path.startsWith("tests/org.eclipse.swt.tests/") && swtManual >= 0) {
            module = path.substring(0, swtManual);
            relative = path.substring(swtManual + "/ManualTests/".length());
            return location(module, relative, value);
        }

        int swtJUnit = path.indexOf("/JUnit Tests/");
        if (path.startsWith("tests/org.eclipse.swt.tests/") && swtJUnit >= 0) {
            module = path.substring(0, swtJUnit);
            relative = path.substring(swtJUnit + "/JUnit Tests/".length());
            return location(module, relative, value);
        }

        if (path.startsWith("bundles/")) {
            String[] parts = path.split("/", -1);
            for (int index = 0; index + 2 < parts.length; index++) {
                if (!parts[index].startsWith("Eclipse SWT")) continue;
                module = String.join("/", java.util.Arrays.copyOfRange(parts, 0, index + 1));
                relative = String.join("/", java.util.Arrays.copyOfRange(parts, index + 2, parts.length));
                return location(module, relative, value);
            }
        }

        if ((path.startsWith("bundles/") || path.startsWith("tests/") || path.startsWith("widgets/")
                || path.startsWith("examples/"))
                && path.contains("/src/")) {
            int source = path.indexOf("/src/");
            module = path.substring(0, source);
            relative = path.substring(source + "/src/".length());
            return location(module, relative, value);
        }

        // Admit only the existing Draw2d PDE packages, retaining Windows path normalization.
        if (path.startsWith("org.eclipse.draw2d/src/org/eclipse/draw2d/")
                || path.startsWith("org.eclipse.draw2d.tests/src/org/eclipse/draw2d/test/")) {
            for (String part : path.split("/", -1)) {
                if (part.isBlank() || part.equals(".") || part.equals("..")
                        || part.chars().anyMatch(Character::isISOControl)) {
                    throw new IllegalArgumentException("normalized relative Java path required: " + value);
                }
            }
            int source = path.indexOf("/src/");
            return location(path.substring(0, source),
                    path.substring(source + "/src/".length()), value);
        }

        // The existing CPULLM JNI parity test has a legacy physical location.
        // Preserve its actual module and package; refuse all adjacent unknown paths.
        if (path.equals("cpullm-java/facet-release/tests/FacetExactCoverageChecks.java")) {
            return new JavaLocation("cpullm-java", "com.synexia.cpullm.facets");
        }

        // Lucene core uses Gradle's src/java and src/test roots instead of Maven paths.
        // Qualify only its Apache Lucene Java package; other Gradle paths still refuse.
        if (path.startsWith("lucene/core/src/java/org/apache/lucene/")
                || path.startsWith("lucene/core/src/test/org/apache/lucene/")) {
            int source = path.indexOf("/src/");
            String luceneModule = path.substring(0, source);
            String relativePath = path.substring(source + "/src/".length());
            relativePath = relativePath.startsWith("java/")
                    ? relativePath.substring("java/".length())
                    : relativePath.substring("test/".length());
            return location(luceneModule, relativePath, value);
        }

        // M3 core retains a deliberate legacy source/test layout rather than Maven's roots.
        // Resolve both to the same physical module/package without opening sibling legacy trees.
        String m3TextMain = "m3/core/src/com/m3/text/";
        String m3TextTest = "m3/core/test/com/m3/text/";
        if (path.startsWith(m3TextMain)) {
            return location("m3/core", "com/m3/text/" + path.substring(m3TextMain.length()), value);
        }
        if (path.startsWith(m3TextTest)) {
            return location("m3/core", "com/m3/text/" + path.substring(m3TextTest.length()), value);
        }

        int main = path.indexOf("/src/main/java/");
        int test = path.indexOf("/src/test/java/");
        int markerIndex = main >= 0 ? main : test;
        String marker = main >= 0 ? "/src/main/java/" : "/src/test/java/";
        if (markerIndex >= 0) {
            module = path.substring(0, markerIndex);
            relative = path.substring(markerIndex + marker.length());
        } else if (path.startsWith("src/main/java/")) {
            module = "<current>";
            relative = path.substring("src/main/java/".length());
        } else if (path.startsWith("src/test/java/")) {
            module = "<current>";
            relative = path.substring("src/test/java/".length());
        } else {
            throw new IllegalArgumentException("Java Maven source path required: " + value);
        }

        if (module.isBlank()) module = "<current>";
        return location(module, relative, value);
    }

    /**
     * Existing optional CPU JPMS layout, shared with the hash-pinned Java path gate.
     * Only these seventeen module owners and their exact package directories are
     * admitted; standalone fixtures and the incumbent file adapter are exact paths.
     * This does not admit other module packs, adjacent packages or arbitrary Java.
     */
    static JavaLocation m3CpuToolsLocation(String path) {
        if (path.length() > 4096 || path.indexOf('\\') >= 0) return null;
        if (path.equals(M3_FILE_SOURCE_ROOT + "jdk/m3/io/M3File.java")) {
            return new JavaLocation(M3_FILE_SOURCE_ROOT.substring(0, M3_FILE_SOURCE_ROOT.length() - 1), "jdk.m3.io");
        }
        if (path.equals(M3_CPU_ROOT + "fixtures/M3DonorToolsTest.java")
                || path.equals(M3_CPU_ROOT + "fixtures/M3SourceToolsTest.java")
                || path.equals(M3_CPU_ROOT + "fixtures/M3CpuToolsTest.java")) {
            return new JavaLocation(M3_CPU_ROOT + "fixtures", "<default>");
        }
        if (!path.startsWith(M3_CPU_SOURCE_ROOT)) return null;
        int ownerEnd = path.indexOf('/', M3_CPU_SOURCE_ROOT.length());
        if (ownerEnd < 0) return null;
        String owner = path.substring(M3_CPU_SOURCE_ROOT.length(), ownerEnd);
        if (!M3_CPU_MODULES.contains(owner)) return null;
        String relative = path.substring(ownerEnd + 1);
        String module = path.substring(0, ownerEnd);
        if (relative.equals("module-info.java")) return new JavaLocation(module, "<default>");
        String packageDirectory = owner.replace('.', '/') + "/";
        if (!relative.startsWith(packageDirectory)
                || !relative.substring(packageDirectory.length())
                        .matches("[A-Za-z_$][A-Za-z0-9_$]*\\.java")) return null;
        return new JavaLocation(module, owner);
    }

    /** These exact reserved roots must never fall through the older permissive layouts. */
    static boolean m3CpuToolsReservedPath(String path) {
        return path.startsWith(M3_CPU_ROOT) || path.startsWith(M3_FILE_SOURCE_ROOT);
    }

    private static JavaLocation location(
            String module,
            String relative,
            String original) {
        // Private validated domain: every caller retains the public .java suffix and
        // at least that filename while extracting its layout-specific relative path.
        int slash = relative.lastIndexOf('/');
        String packageName = slash < 0
                ? "<default>"
                : relative.substring(0, slash).replace('/', '.');
        return new JavaLocation(module, packageName);
    }

    public record JavaLocation(String module, String packageName) {
        public JavaLocation {
            if (module == null || module.isBlank()) throw new IllegalArgumentException("module");
            if (packageName == null || packageName.isBlank()) throw new IllegalArgumentException("packageName");
        }
    }
    /** Exact existing standalone owners; adjacent files and path aliases are not admitted. */
    public static boolean canonicalStandaloneGuard(String path) {
        return "tools/m3-superset/java/com/synexia/m3/superset/PinnedGitObjects.java".equals(path)
                || "tools/m3-superset/java/com/synexia/m3/superset/MergeCodeLossGuard.java".equals(path);
    }
}
