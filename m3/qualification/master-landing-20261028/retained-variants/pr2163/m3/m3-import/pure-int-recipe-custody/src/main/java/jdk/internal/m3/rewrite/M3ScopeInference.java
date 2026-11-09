// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026-10-19: share exact M3 Guava source-root admission.
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026-10-20: resolve the existing direct M3 core test layout.
package jdk.internal.m3.rewrite;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Narrowest physical target scope inferable from exact Java source paths. */
public final class M3ScopeInference {
    private static final Pattern ABSOLUTE_WINDOWS_DRIVE_PREFIX = Pattern.compile("^[A-Za-z]:/");
    private static final String M3_CPU_ROOT = "m3/tooling/module-packs/cpu-tools/";
    private static final String M3_CPU_SOURCE_ROOT = M3_CPU_ROOT + "src/";
    private static final String M3_FILE_SOURCE_ROOT = "m3/tooling/module-packs/text-tools/src/jdk.m3.io/";
    private static final Set<String> M3_CPU_MODULES = Set.of(
            "jdk.m3.tools.cpu",
            "jdk.m3.tools.codec.varint", "jdk.m3.tools.constraint.interval",
            "jdk.m3.tools.graph.paths", "jdk.m3.tools.graph.topology",
            "jdk.m3.tools.plan.priority", "jdk.m3.tools.stats.streaming",
            "jdk.m3.tools.text.diff", "jdk.m3.tools.text.frequency",
            "jdk.m3.tools.text.index", "jdk.m3.tools.text.ngram",
            "jdk.m3.tools.text.normalize", "jdk.m3.tools.text.regex",
            "jdk.m3.tools.text.search", "jdk.m3.tools.text.similarity",
            "jdk.m3.tools.text.tokenize", "jdk.m3.tools.verify.sha256");

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
                || ABSOLUTE_WINDOWS_DRIVE_PREFIX.matcher(path).find()
                || path.indexOf('\0') >= 0
                || path.contains("/../")
                || path.startsWith("../")
                || path.endsWith("/..")
                || !path.endsWith(".java")) {
            throw new IllegalArgumentException("normalized relative Java path required: " + value);
        }

        if (canonicalStandaloneGuard(path))
            return new JavaLocation("tools/m3-superset", "jdk.internal.m3.superset");
        String module;
        String relative;

        JavaLocation cpuTools = m3CpuToolsLocation(path);
        if (cpuTools != null) return cpuTools;
        if (m3CpuToolsReservedPath(path)) {
            throw new IllegalArgumentException("unknown Java path in reserved M3 CPU layout: " + value);
        }

        if (path.startsWith("m3/core/src/jdk/m3/text/")) {
            return location("m3/core", path.substring("m3/core/src/".length()), value);
        }

        if (path.equals("m3/tooling/module-packs/parser/qualification/owner-current-20261019/proof-drivers/proof-src/M3ProjectionCacheResourceProbe.java")) {
            return location("m3/tooling/module-packs/parser/qualification/owner-current-20261019",
                    "M3ProjectionCacheResourceProbe.java", value);
        }
        String collectionProofRoot = "m3/collections/qualification/owner-current-20261019/proof-src/";
        if (path.equals(collectionProofRoot + "jdk/m3/collections/M3FilteredListMutationWitness.java")
                || path.equals(collectionProofRoot + "jdk/m3/collections/M3IndexOrderWindowResourceProbe.java")) {
            return location("m3/collections/qualification/owner-current-20261019",
                    path.substring(collectionProofRoot.length()), value);
        }

        // Preserve the physical JDT project/package identity for the reviewed PDE roots.
        String jdtDomRoot = "org.eclipse.jdt.core/dom/";
        String jdtTestRoot = "org.eclipse.jdt.core.tests.model/src/";
        if (path.startsWith(jdtDomRoot + "org/eclipse/jdt/core/dom/")) {
            return location("org.eclipse.jdt.core", path.substring(jdtDomRoot.length()), value);
        }
        if (path.startsWith(jdtTestRoot + "org/eclipse/jdt/core/tests/dom/")) {
            return location("org.eclipse.jdt.core.tests.model", path.substring(jdtTestRoot.length()), value);
        }
        if (path.equals("org.eclipse.jdt.core/model/org/eclipse/jdt/internal/core/builder/JavaBuilder.java")) {
            return new JavaLocation("org.eclipse.jdt.core", "org.eclipse.jdt.internal.core.builder");
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

        // The JDT rewrite owner and its existing placeholder store share one DOM module.
        // Keep this admission identical to the exact Java snapshot host target set.
        if (path.equals("org.eclipse.jdt.core/dom/org/eclipse/jdt/core/dom/rewrite/ASTRewrite.java")
                || path.equals("org.eclipse.jdt.core/dom/org/eclipse/jdt/internal/core/dom/rewrite/NodeInfoStore.java")) {
            return location("org.eclipse.jdt.core",
                    path.substring("org.eclipse.jdt.core/dom/".length()), value);
        }

        // The exact standalone regression belongs to JDT's existing test module.
        if (path.equals("org.eclipse.jdt.core.tests.model/src/org/eclipse/jdt/core/tests/dom/M3RewriteOriginalNodeContract.java")) {
            return location("org.eclipse.jdt.core.tests.model",
                    path.substring("org.eclipse.jdt.core.tests.model/src/".length()), value);
        }

        // Original Eclipse Search uses search/org/eclipse/search instead of src/java.
        if (path.startsWith("bundles/org.eclipse.search/search/org/eclipse/search/")) {
            return location("bundles/org.eclipse.search",
                    path.substring("bundles/org.eclipse.search/search/".length()), value);
        }

        int swtManual = path.indexOf("/ManualTests/");
        if (path.startsWith("tests/org.eclipse.swt.tests/") && swtManual >= 0) {
            module = path.substring(0, swtManual);
            relative = path.substring(swtManual + "/ManualTests/".length());
            return location(module, relative, value);
        }

        // Original Win32 test discovery belongs to its own physical platform bundle.
        // Keep this exact owner aligned with the Java snapshot admission rule.
        if (path.equals("tests/org.eclipse.swt.tests.win32/JUnit Tests/org/eclipse/swt/tests/win32/AllWin32Tests.java")) {
            return new JavaLocation("tests/org.eclipse.swt.tests.win32", "org.eclipse.swt.tests.win32");
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

        // The source-backed NLP integration is one explicit receiver module owner.
        String nlpPackage = switch (path) {
            case "m3/tooling/module-packs/nlp-search/src/jdk.m3.nlp.search/jdk/m3/nlp/search/LuceneSourceFields.java" -> "jdk.m3.nlp.search";
            case "m3/tooling/module-packs/nlp-search/src/jdk.m3.nlp.search/jdk/m3/nlp/search/SourceDocument.java" -> "jdk.m3.nlp.search";
            case "m3/tooling/module-packs/nlp-search/src/jdk.m3.nlp.search/jdk/m3/nlp/search/SourceSpans.java" -> "jdk.m3.nlp.search";
            case "m3/tooling/module-packs/nlp-search/tests/ModelSourceProbe.java" -> "jdk.m3.nlp.search.test";
            case "m3/tooling/module-packs/nlp-search/tests/SourceFactsProbe.java" -> "jdk.m3.nlp.search.test";
            case "m3/tooling/module-packs/nlp-search/tests/SourceIntegrationProbe.java" -> "jdk.m3.nlp.search.test";
            default -> null;
        };
        if (nlpPackage != null) return new JavaLocation("jdk.m3.nlp.search", nlpPackage);

        // Exact inventoried standalone M3 owners; adjacent unknown roots still refuse.
        if (path.equals("m3/core/src/jdk/m3/text/M3GvasLiteralImage.java")
                || path.equals("m3/core/test/jdk/m3/text/M3GvasLookupIndexContractTest.java")) {
            return new JavaLocation("m3/core", "jdk.m3.text");
        }

        // The existing CPULLM JNI parity test has a legacy physical location.
        // Preserve its actual module and package; refuse all adjacent unknown paths.
        if (path.equals("cpullm-java/facet-release/tests/FacetExactCoverageChecks.java")) {
            return new JavaLocation("cpullm-java", "jdk.internal.m3.cpullm.facets");
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

        if (path.equals("guava/src/com/google/common/collect/ArrayListMultimap.java")) {
            return new JavaLocation("guava", "com.google.common.collect");
        }

        // M3 core retains a deliberate legacy source/test layout rather than Maven's roots.
        // Resolve both to the same physical module/package without opening sibling legacy trees.
        String m3TextMain = "m3/core/src/jdk/m3/text/";
        String m3TextTest = "m3/core/test/jdk/m3/text/";
        if (path.startsWith(m3TextMain)) {
            return location("m3/core", "jdk/m3/text/" + path.substring(m3TextMain.length()), value);
        }
        if (path.startsWith(m3TextTest)) {
            return location("m3/core", "jdk/m3/text/" + path.substring(m3TextTest.length()), value);
        }
        String m3DirectTestRoot = "m3/core/test/";
        if (path.startsWith(m3DirectTestRoot)) {
            String filename = path.substring(m3DirectTestRoot.length());
            if (filename.length() > ".java".length() && filename.indexOf('/') < 0
                    && filename.chars().noneMatch(Character::isISOControl)) {
                return location("m3/core", filename, value);
            }
        }

        JavaLocation guava = guavaSourceLocation(path);
        if (guava != null) return guava;

        // JRE and Android are distinct physical modules with the same package names.
        if (path.startsWith("guava/src/jdk/m3/collections/guava/")
                || path.startsWith("android/guava/src/jdk/m3/collections/guava/")) {
            int source = path.indexOf("/src/");
            return location(path.substring(0, source),
                    path.substring(source + "/src/".length()), value);
        }

        // Keep the existing SDK text module in its physical pack and Java package.
        String m3TextTools = "m3/tooling/module-packs/text-tools/src/jdk.m3.text/";
        if (path.startsWith(m3TextTools)) {
            return location("m3/tooling/module-packs/text-tools",
                    path.substring(m3TextTools.length()), value);
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
                || path.equals(M3_CPU_ROOT + "fixtures/M3CpuToolsTest.java")
                || path.equals(M3_CPU_ROOT + "fixtures/M3GramIndexTest.java")
                || path.equals(M3_CPU_ROOT + "fixtures/M3GramIndexBenchmark.java")
                || path.equals(M3_CPU_ROOT + "fixtures/M3AffixIndexTest.java")
                || path.equals(M3_CPU_ROOT + "fixtures/M3AffixIndexBenchmark.java")
                || path.equals(M3_CPU_ROOT + "fixtures/M3BoundedDiffTest.java")
                || path.equals(M3_CPU_ROOT + "fixtures/M3PreparedRankBenchmark.java")
                || path.equals(M3_CPU_ROOT + "fixtures/M3IndexedDataContract.java")
                || path.equals(M3_CPU_ROOT + "fixtures/M3LogicFlowContract.java")) {
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

    /**
     * Recognizes the eight observed M3 Guava JRE/Android source roots, including benchmarks.
     *
     * <p>Shared with snapshot admission so source scope and writable-path discovery
     * cannot disagree. This physical-location fact supplies no API or rewrite authority.
     */
    static JavaLocation guavaSourceLocation(String path) {
        if (path == null || !path.endsWith(".java") || path.length() > 4096
                || path.indexOf('\\') >= 0) return null;
        for (String root : List.of(
                "guava/src/", "guava-tests/test/", "guava-testlib/src/",
                "guava-tests/benchmark/",
                "android/guava/src/", "android/guava-tests/test/",
                "android/guava-testlib/src/", "android/guava-tests/benchmark/")) {
            if (!path.startsWith(root + "jdk/m3/collections/guava/")) continue;
            for (String part : path.split("/", -1)) {
                if (part.isBlank() || part.equals(".") || part.equals("..")
                        || part.chars().anyMatch(Character::isISOControl)) return null;
            }
            int source = root.lastIndexOf('/', root.length() - 2);
            return location(root.substring(0, source), path.substring(root.length()), path);
        }
        return null;
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
        return "tools/m3-superset/java/jdk/internal/m3/superset/PinnedGitObjects.java".equals(path)
                || "tools/m3-superset/java/jdk/internal/m3/superset/MergeCodeLossGuard.java".equals(path)
                || "tools/m3-superset/java/jdk/internal/m3/superset/StackConflictResolver.java".equals(path);
    }
}
