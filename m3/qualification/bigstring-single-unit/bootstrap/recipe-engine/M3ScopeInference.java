// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026-10-19: share exact M3 Guava source-root admission.
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026-10-20: resolve the existing direct M3 core test layout.
// Modified 2026-11-13: retain exact installed Guava hash-owner module/package identities.
// Modified 2026-11-13: preserve five Eclipse proof paths and two exact M3 Guava receiver paths.
package com.synexia.rewrite;

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
                || ABSOLUTE_WINDOWS_DRIVE_PREFIX.matcher(path).find()
                || path.indexOf('\0') >= 0
                || path.contains("/../")
                || path.startsWith("../")
                || path.endsWith("/..")
                || path.contains("/./")
                || path.startsWith("./")
                || path.endsWith("/.")
                || !path.endsWith(".java")) {
            throw new IllegalArgumentException("normalized relative Java path required: " + value);
        }

        if (path.equals("tools/java/VerifyTranscriptIntegration.java"))
            return new JavaLocation("tools/java", "<default>");
        // Exact standalone Java21 estate-reactor files; do not widen tools/** admission.
        if (path.equals("tools/M3EstateReactor.java")
                || path.equals("tools/M3EstateReactorTest.java"))
            return new JavaLocation("tools", "<default>");
        // Existing source-launcher workflow guard has a default package and a fixed path.
        if (path.equals("synexia-code-convergence/tools/WorkflowStructureCheck.java"))
            return new JavaLocation("synexia-code-convergence", "<default>");
        if (canonicalStandaloneGuard(path))
            return new JavaLocation("tools/m3-superset", "com.synexia.m3.superset");
        if (swtReleaseContractPath(path))
            return new JavaLocation("tools/release", "org.eclipse.swt.internal.residency");
        // Existing canonical scan runner, not a general standalone tools layout.
        if (path.equals("synexia-openrewrite-recipes/crates/m3-mop/leetcodify/tools/M3LeetCodifyRun.java")) {
            return new JavaLocation("synexia-openrewrite-recipes/crates/m3-mop/leetcodify",
                    "com.synexia.rewrite.mop.leetcodify.tools");
        }
        // Admit only this exact private Synexia Maven task oracle, not adjacent proof files.
        // Its Java package is com.synexia.primitives although its physical owner is
        // the canonical recipe crate, outside a normal src/test/java directory.
        if (canonicalJniCleanupProof(path)) {
            return new JavaLocation(
                    "synexia-openrewrite-recipes/recipe-crates/m3-doop-jni-array-batch-20261030",
                    "com.synexia.primitives");
        }
        if (canonicalM3HashMapProductParityProof(path)) {
            return new JavaLocation(
                    "synexia-openrewrite-recipes/crates/m3-entry-key-value-fleet-20261102/native-sdk-proof",
                    "com.synexia.rewrite");
        }
        // These exact existing contract files declare com.m3.file outside a Maven root.
        if (path.equals("m3/ports/file/qualification/m3fmt-shared-lexicon-20261021/M3FmtFrozenDictionaryContract.java")
                || path.equals("m3/ports/file/qualification/m3fmt-shared-lexicon-20261021/M3FmtLexiconFrameContract.java")) {
            return new JavaLocation("m3/ports/file/qualification/m3fmt-shared-lexicon-20261021", "com.m3.file");
        }
        // One existing receiver contract declares a package outside a Java source root.
        if (m3FilteredDequeContractPath(path)) {
            return new JavaLocation("m3/collections/verification/filtered-object-deques-20261017",
                    "jdk.m3.collections");
        }
        JavaLocation indexedProof = eclipseIndexedProofLocation(path);
        if (indexedProof != null) return indexedProof;
        String module;
        String relative;

        JavaLocation cpuTools = m3CpuToolsLocation(path);
        if (cpuTools != null) return cpuTools;
        if (m3CpuToolsReservedPath(path)) {
            throw new IllegalArgumentException("unknown Java path in reserved M3 CPU layout: " + value);
        }

        // Match the inventoried migrated core owners and the exact existing source-envelope owner.
        if (switch (path) {
            case "m3/core/src/jdk/m3/text/LocalM3Arena.java" -> true;
            case "m3/core/src/jdk/m3/text/LocalM3StringPiece.java" -> true;
            case "m3/core/src/jdk/m3/text/M3LangDexSourceEnvelope.java" -> true;
            case "m3/core/src/jdk/m3/text/M3LexiconPrecompute.java" -> true;
            case "m3/core/src/jdk/m3/text/M3PrecomputePayload.java" -> true;
            case "m3/core/src/jdk/m3/text/M3StringPiece.java" -> true;
            case "m3/core/src/jdk/m3/text/SharedLexiconCatalog.java" -> true;
            case "m3/core/src/jdk/m3/text/SharedLexiconFamilySidecarCatalog.java" -> true;
            case "m3/core/src/jdk/m3/text/SharedLexiconImage.java" -> true;
            case "m3/core/src/jdk/m3/text/SharedRelatedLexemeCatalog.java" -> true;
            case "m3/core/src/jdk/m3/text/StorageIdentity.java" -> true;
            default -> false;
        }) return new JavaLocation("m3/core", "jdk.m3.text");

        if (path.startsWith("m3/core/src/com/m3/text/")) {
            return location("m3/core", path.substring("m3/core/src/".length()), value);
        }

        if (path.equals("m3/tooling/module-packs/parser/qualification/owner-current-20261019/proof-drivers/proof-src/M3ProjectionCacheResourceProbe.java")) {
            return location("m3/tooling/module-packs/parser/qualification/owner-current-20261019",
                    "M3ProjectionCacheResourceProbe.java", value);
        }
        String collectionProofRoot = "m3/collections/qualification/owner-current-20261019/proof-src/";
        if (path.equals(collectionProofRoot + "com/m3/collections/M3FilteredListMutationWitness.java")
                || path.equals(collectionProofRoot + "com/m3/collections/M3IndexOrderWindowResourceProbe.java")) {
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

        // File-sealed ECJ NLS annotation repairs; retain physical product modules.
        if (path.equals("org.eclipse.jdt.debug/model/org/eclipse/jdt/internal/debug/core/breakpoints/JavaExceptionBreakpoint.java") || path.equals("org.eclipse.jdt.debug/model/org/eclipse/jdt/internal/debug/core/breakpoints/JavaMethodBreakpoint.java")) {
            return new JavaLocation("org.eclipse.jdt.debug", "org.eclipse.jdt.internal.debug.core.breakpoints");
        }
        if (path.equals("org.eclipse.jdt.core.manipulation/common/org/eclipse/jdt/core/manipulation/internal/javadoc/CoreJavaDoc2HTMLTextReader.java")) {
            return new JavaLocation("org.eclipse.jdt.core.manipulation", "org.eclipse.jdt.core.manipulation.internal.javadoc");
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

        // Exact existing java.base contract test; adjacent standalone tests remain unregistered.
        if (path.equals("m3/core/test/java/lang/M3CodeTextSignalBatchTest.java")) {
            return new JavaLocation("m3/core", "java.lang");
        }

        // Exact inventoried standalone M3 owners; adjacent unknown roots still refuse.
        if (path.equals("m3/core/src/com/m3/text/M3GvasLiteralImage.java")
                || path.equals("m3/core/test/com/m3/text/M3GvasLookupIndexContractTest.java")) {
            return new JavaLocation("m3/core", "com.m3.text");
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

        if (path.equals("guava/src/com/google/common/collect/ArrayListMultimap.java")) {
            return new JavaLocation("guava", "com.google.common.collect");
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
        String m3DirectTestRoot = "m3/core/test/";
        if (path.startsWith(m3DirectTestRoot)) {
            String filename = path.substring(m3DirectTestRoot.length());
            if (filename.length() > ".java".length() && filename.indexOf('/') < 0
                    && filename.chars().noneMatch(Character::isISOControl)) {
                return location("m3/core", filename, value);
            }
        }

        JavaLocation installedGuavaHash = m3InstalledGuavaHashLocation(path);
        if (installedGuavaHash != null) return installedGuavaHash;

        JavaLocation guava = guavaSourceLocation(path);
        if (guava != null) return guava;

        // JRE and Android are distinct physical modules with the same package names.
        if (path.startsWith("guava/src/com/m3/collections/guava/")
                || path.startsWith("android/guava/src/com/m3/collections/guava/")) {
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

    /** Exact existing Eclipse proof layout shared with the Java snapshot path gate. */
    static JavaLocation eclipseIndexedProofLocation(String path) {
        String receiver = "m3/collections/implementations/eclipse-collections-14.0.0-snapshot-00a668d6/";
        String proof = "m3/indexed-rb/proofs/";
        boolean inReceiver = path.startsWith(receiver + proof);
        String root = inReceiver ? receiver + proof : proof;
        if (!path.startsWith(root)) return null;
        String file = path.substring(root.length());
        boolean shared = file.equals("IndexedTreeArenaTest.java")
                || file.equals("IndexedTreeArenaProjectionTest.java");
        if (!shared && !(inReceiver && file.equals("IndexedSortedMapForEachTest.java"))) return null;
        return new JavaLocation(root.substring(0, root.length() - 1),
                (inReceiver ? "jdk.m3" : "com.m3") + ".collections.eclipse.impl.map.sorted.mutable");
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
    // The installed Guava package layout is non-Maven; register only these reviewed owners.
    static JavaLocation m3InstalledGuavaHashLocation(String path) {
        if (path == null) return null;
        final String root = "m3/collections/implementations/guava-33.4.8/";
        String module = switch (path) {
            case root + "guava/src/jdk/m3/collections/guava/collect/CompactHashMap.java" -> root + "guava";
            case root + "guava-tests/test/jdk/m3/collections/guava/collect/M3CompactMapHashCodeTest.java" -> root + "guava-tests";
            case root + "android/guava/src/jdk/m3/collections/guava/collect/CompactHashMap.java" -> root + "android/guava";
            case root + "android/guava-tests/test/jdk/m3/collections/guava/collect/M3CompactMapHashCodeTest.java" -> root + "android/guava-tests";
            default -> null;
        };
        return module == null ? null : new JavaLocation(module, "jdk.m3.collections.guava.collect");
    }

    static JavaLocation guavaSourceLocation(String path) {
        if (path == null || !path.endsWith(".java") || path.length() > 4096
                || path.indexOf('\\') >= 0) return null;
        // These existing receiver files use jdk.m3 under a pinned donor snapshot.
        // Keep admission at FILE identity; neighboring versions and owners are not implied.
        String receiver = "m3/collections/implementations/guava-33.4.8/";
        if (path.equals(receiver + "guava/src/jdk/m3/collections/guava/collect/HashBiMap.java")) {
            return new JavaLocation(receiver + "guava", "jdk.m3.collections.guava.collect");
        }
        if (path.equals(receiver + "guava-tests/test/jdk/m3/collections/guava/collect/HashBiMapStringFloodingTest.java")) {
            return new JavaLocation(receiver + "guava-tests", "jdk.m3.collections.guava.collect");
        }
        for (String root : List.of(
                "guava/src/", "guava-tests/test/", "guava-testlib/src/",
                "guava-tests/benchmark/",
                "android/guava/src/", "android/guava-tests/test/",
                "android/guava-testlib/src/", "android/guava-tests/benchmark/")) {
            if (!path.startsWith(root + "com/m3/collections/guava/")) continue;
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
    /** The one source-sealed original JNI RED3 proof, not a general proof/ bypass. */
    static boolean canonicalJniCleanupProof(String path) {
        return "synexia-openrewrite-recipes/recipe-crates/m3-doop-jni-array-batch-20261030/proof/M3JniLegacyCleanupRegression.java".equals(path);
    }

    /** One existing same-process JNI proof driver; neighboring proof files stay refused. */
    static boolean canonicalM3HashMapProductParityProof(String path) {
        return "synexia-openrewrite-recipes/crates/m3-entry-key-value-fleet-20261102/native-sdk-proof/M3HashMapProductParity.java".equals(path);
    }

    /** Exact existing standalone owners; adjacent files and path aliases are not admitted. */
    static boolean swtReleaseContractPath(String path) {
        return "tools/release/java/org/eclipse/swt/internal/residency/M3SwtHeadlessContract.java"
                .equals(path);
    }

    /** Exact existing receiver contract; neighboring and default-package paths remain unregistered. */
    static boolean m3FilteredDequeContractPath(String path) {
        return "m3/collections/verification/filtered-object-deques-20261017/M3FilteredDequeContract.java"
                .equals(path);
    }

    /** Exact existing standalone owners; adjacent files and path aliases are not admitted. */
    public static boolean canonicalStandaloneGuard(String path) {
        return "synexia-code-convergence/tools/WorkflowStructureCheck.java".equals(path)
                || "tools/M3EstateReactor.java".equals(path)
                || "tools/M3EstateReactorTest.java".equals(path)
                || "tools/m3-superset/java/com/synexia/m3/superset/PinnedGitObjects.java".equals(path)
                || "tools/m3-superset/java/com/synexia/m3/superset/MergeCodeLossGuard.java".equals(path)
                || "tools/m3-superset/java/com/synexia/m3/superset/StackConflictResolver.java".equals(path)
                || "tools/m3-superset/java/com/synexia/m3/superset/IndependentHunkUnion.java".equals(path);
    }
}
