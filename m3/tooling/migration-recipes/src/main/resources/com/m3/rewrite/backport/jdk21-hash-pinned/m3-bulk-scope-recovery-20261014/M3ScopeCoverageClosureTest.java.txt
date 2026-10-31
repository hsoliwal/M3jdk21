// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3PureIntConvergenceRecipe;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3ScopeCoverageClosureTest {
    @Test
    void editScopeFormsTheExactMonotonicAuthorityLattice() {
        M3EditScope[] scopes = M3EditScope.values();
        for (int granted = 0; granted < scopes.length; granted++) {
            for (int required = 0; required < scopes.length; required++) {
                assertEquals(
                        granted >= required,
                        scopes[granted].permits(scopes[required]));
                assertEquals(
                        scopes[Math.max(granted, required)],
                        scopes[granted].promote(scopes[required]));
            }
        }

        assertEquals(M3EditScope.VISIBILITY, M3EditScope.FILE.next());
        assertEquals(M3EditScope.PACKAGE, M3EditScope.VISIBILITY.next());
        assertEquals(M3EditScope.MODULE, M3EditScope.PACKAGE.next());
        assertEquals(M3EditScope.MULTI_MODULE, M3EditScope.MODULE.next());
        assertEquals(M3EditScope.LIBRARY_API, M3EditScope.MULTI_MODULE.next());
        assertEquals(M3EditScope.LIBRARY_API, M3EditScope.LIBRARY_API.next());
        assertEquals(2, M3ContractMode.values().length);
    }

    @Test
    void scopePolicyCoversFixedInferredAndExplicitContractModes() {
        assertThrows(
                NullPointerException.class,
                () -> new M3RecipeScopePolicy(
                        null,
                        M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                        false));
        assertThrows(
                NullPointerException.class,
                () -> new M3RecipeScopePolicy(
                        M3EditScope.FILE,
                        null,
                        false));

        var fixedFile = new M3RecipeScopePolicy(
                M3EditScope.FILE,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                false);
        assertEquals(M3EditScope.FILE, fixedFile.resolve(List.of()));
        assertTrue(fixedFile.fileLocalMechanical(List.of()));

        var fixedPackage = new M3RecipeScopePolicy(
                M3EditScope.PACKAGE,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                false);
        assertEquals(M3EditScope.PACKAGE, fixedPackage.resolve(List.of("ignored")));
        assertFalse(fixedPackage.fileLocalMechanical(List.of("ignored")));

        var explicit = new M3RecipeScopePolicy(
                M3EditScope.LIBRARY_API,
                M3ContractMode.EXPLICIT_CONTRACT_CHANGE,
                false);
        assertEquals(M3EditScope.LIBRARY_API, explicit.resolve(List.of()));
        assertFalse(explicit.fileLocalMechanical(List.of()));

        var inferred = new M3RecipeScopePolicy(
                M3EditScope.FILE,
                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                true);
        assertEquals(
                M3EditScope.FILE,
                inferred.resolve(List.of("src/main/java/p/A.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                inferred.resolve(List.of(
                        "src/main/java/p/A.java",
                        "src/main/java/p/B.java")));
        assertEquals(
                M3EditScope.MULTI_MODULE,
                inferred.resolve(List.of(
                        "src/java.base/share/classes/java/lang/String.java",
                        "src/java.logging/share/classes/java/util/logging/Logger.java")));
        assertThrows(IllegalArgumentException.class, () -> inferred.resolve(List.of()));
    }

    @Test
    void registryCoversKnownUnknownNullAndClassLookupPaths() {
        List<String> targetNames = List.of(
                "com.m3.rewrite.InstallIndexStringCompatibility",
                "com.m3.rewrite.M3Java21ConvergenceRecipe",
                "com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe",
                "com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe",
                "com.m3.rewrite.backport.M3VerbatimJavaPairRecipe",
                "com.m3.rewrite.backport.M3Jep458BackportRecipe",
                "com.m3.rewrite.backport.M3Jep485StreamGatherersBackportRecipe",
                "com.m3.rewrite.backport.M3Jdk8338587ShakeXofBackportRecipe",
                "com.m3.rewrite.backport.M3Jdk8357439JcmdCompletionBackportRecipe",
                "com.m3.rewrite.backport.M3Jep496MlKemBackportRecipe",
                "com.m3.rewrite.backport.M3Jep497MlDsaBackportRecipe",
                "com.m3.rewrite.backport.M3Jep510KdfBackportRecipe",
                "com.m3.rewrite.backport.M3Jep510KdfSecurityTestsRecipe",
                "com.m3.rewrite.backport.M3Jep510KdfCandidateRecipe",
                "com.m3.rewrite.backport.M3ReleaseJepAuthorityRepairRecipe",
                "com.m3.rewrite.synexia.M3SynexiaImportInventoryRecipe",
                "com.m3.rewrite.semantic.M3SemanticHashRecipe",
                "com.m3.rewrite.a3.M3A3AlgorithmCatalogueRecipe",
                "com.m3.rewrite.a3.M3A3SerialFileWorkRecipe",
                "com.m3.rewrite.backport.M3BulkExecutorGateRepairRecipe",
                "com.m3.rewrite.backport.M3BulkExecutorNameMappingRecipe",
                "com.m3.rewrite.backport.M3BulkExecutorPortRecipe",
                "com.m3.rewrite.backport.M3BulkFreezeSourceBootstrapRecipe",
                "com.m3.rewrite.backport.M3BulkLiveRegistrationRecoveryRecipe",
                "com.m3.rewrite.backport.M3BulkOutputIsolationRecipe",
                "com.m3.rewrite.backport.M3BulkOutputMetadataFastPathRecipe",
                "com.m3.rewrite.backport.M3BulkRegistrationCiBootstrapRecipe",
                "com.m3.rewrite.backport.M3BulkRegistrationFreezeReceiverRecipe",
                "com.m3.rewrite.backport.M3BulkSchedulerProviderRecoveryRecipe",
                "com.m3.rewrite.backport.M3BulkSchedulerRecoveryNameMappingRecipe",
                "com.m3.rewrite.backport.M3BulkSchedulerRecoveryWorkflowRecipe",
                "com.m3.rewrite.backport.M3ParallelBulkBridgeNameMappingRecipe",
                "com.m3.rewrite.backport.M3ParallelBulkBridgeReceiverRecipe",
                "com.m3.rewrite.backport.M3ParallelBulkCompilerRepairRecipe",
                "com.m3.rewrite.backport.M3TornadoBulkAdapterMappingRecipe",
                "com.m3.rewrite.backport.M3TornadoBulkAdapterPortRecipe",
                "com.m3.rewrite.backport.M3TornadoBulkGeometryRepairReceiverRecipe",
                "com.m3.rewrite.backport.M3TornadoProviderUpgradeReceiverRecipe");

        assertEquals(16, com.synexia.rewrite.M3RecipeScopeRegistry.size());
        assertEquals(targetNames.size(), M3RecipeScopeRegistry.targetOverlaySize());
        assertEquals(
                com.synexia.rewrite.M3RecipeScopeRegistry.size() + targetNames.size(),
                M3RecipeScopeRegistry.size());

        List<String> canonicalNames = List.of(
                "org.openrewrite.java.RemoveUnusedImports",
                "com.synexia.rewrite.M3MIndexJoinedCharsViewRecipe",
                "com.synexia.rewrite.M3SegmentedLaneNativeRecipe",
                "com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe",
                "com.synexia.rewrite.atom.M3AtomizePureIntReturnRecipe",
                "com.synexia.rewrite.atom.M3InventoryPureIntAtomCandidates",
                "com.synexia.rewrite.atom.M3PatternizePureIntAtomRecipe",
                "com.synexia.rewrite.atom.M3DocumentPureIntAtomRecipe",
                "com.synexia.rewrite.atom.M3PureIntConvergenceRecipe",
                "com.synexia.rewrite.semantic.M3SemanticHashRecipe",
                "com.synexia.rewrite.M3PureIntAtomizeRecipe",
                "com.synexia.rewrite.M3PureIntInventoryRecipe",
                "com.synexia.rewrite.M3PureIntPatternizeRecipe",
                "com.synexia.rewrite.M3PureIntDocumentationRecipe",
                "com.synexia.rewrite.M3PureIntConvergenceRecipe",
                "com.synexia.rewrite.M3Jdk21FileConvergenceRecipe");
        assertEquals(16, canonicalNames.size());
        assertEquals(38, targetNames.size());
        for (String name : canonicalNames) {
            assertTrue(M3RecipeScopeRegistry.registered(name), name);
            assertNotNull(M3RecipeScopeRegistry.require(name), name);
        }
        for (String name : targetNames) {
            assertTrue(M3RecipeScopeRegistry.registered(name), name);
            assertNotNull(M3RecipeScopeRegistry.require(name), name);
        }

        assertEquals(
                M3EditScope.FILE,
                M3RecipeScopeRegistry.require(M3PureIntConvergenceRecipe.class)
                        .minimumScope());
        assertFalse(M3RecipeScopeRegistry.registered(null));
        assertFalse(M3RecipeScopeRegistry.registered("missing.Recipe"));
        assertThrows(
                NullPointerException.class,
                () -> M3RecipeScopeRegistry.require((String) null));
        assertThrows(
                NullPointerException.class,
                () -> M3RecipeScopeRegistry.require((Class<?>) null));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeScopeRegistry.require("missing.Recipe"));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3RecipeScopeRegistry.require(M3ScopeCoverageClosureTest.class));
    }


    @Test
    void bulkRecipeScopesEscalateWithActualJdkModuleBoundaries() {
        List<String> bulkNames = List.of(
                "com.m3.rewrite.backport.M3BulkExecutorGateRepairRecipe",
                "com.m3.rewrite.backport.M3BulkExecutorNameMappingRecipe",
                "com.m3.rewrite.backport.M3BulkExecutorPortRecipe",
                "com.m3.rewrite.backport.M3BulkFreezeSourceBootstrapRecipe",
                "com.m3.rewrite.backport.M3BulkLiveRegistrationRecoveryRecipe",
                "com.m3.rewrite.backport.M3BulkOutputIsolationRecipe",
                "com.m3.rewrite.backport.M3BulkOutputMetadataFastPathRecipe",
                "com.m3.rewrite.backport.M3BulkRegistrationCiBootstrapRecipe",
                "com.m3.rewrite.backport.M3BulkRegistrationFreezeReceiverRecipe",
                "com.m3.rewrite.backport.M3BulkSchedulerProviderRecoveryRecipe",
                "com.m3.rewrite.backport.M3BulkSchedulerRecoveryNameMappingRecipe",
                "com.m3.rewrite.backport.M3BulkSchedulerRecoveryWorkflowRecipe",
                "com.m3.rewrite.backport.M3ParallelBulkBridgeNameMappingRecipe",
                "com.m3.rewrite.backport.M3ParallelBulkBridgeReceiverRecipe",
                "com.m3.rewrite.backport.M3ParallelBulkCompilerRepairRecipe",
                "com.m3.rewrite.backport.M3TornadoBulkAdapterMappingRecipe",
                "com.m3.rewrite.backport.M3TornadoBulkAdapterPortRecipe",
                "com.m3.rewrite.backport.M3TornadoBulkGeometryRepairReceiverRecipe",
                "com.m3.rewrite.backport.M3TornadoProviderUpgradeReceiverRecipe");
        assertEquals(19, bulkNames.size());
        for (String name : bulkNames) {
            M3RecipeScopePolicy policy = M3RecipeScopeRegistry.require(name);
            assertTrue(policy.inferFromTargets(), name);
            assertEquals(M3EditScope.MODULE, policy.minimumScope(), name);
            assertEquals(M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                    policy.contractMode(), name);
            assertEquals(M3EditScope.MODULE,
                    policy.resolve(List.of("src/main/java/p/A.java")), name);
            assertEquals(M3EditScope.MULTI_MODULE,
                    policy.resolve(List.of(
                            "src/java.base/share/classes/jdk/internal/vm/parallel/BulkTask.java",
                            "src/java.logging/share/classes/java/util/logging/Logger.java")), name);
            assertFalse(policy.fileLocalMechanical(List.of("src/main/java/p/A.java")), name);
        }
    }

    @Test
    void inferenceCoversMavenOpenJdkBuildTestAndRepositoryLayouts() {
        assertEquals(
                M3EditScope.FILE,
                M3ScopeInference.forJavaPaths(List.of("src/main/java/p/A.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                M3ScopeInference.forPaths(List.of(
                        "src/main/java/p/A.java",
                        "src/main/java/p/B.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                M3ScopeInference.forPaths(List.of(
                        "src/main/java/p/A.java",
                        "src/test/java/p/ATest.java")));
        assertEquals(
                M3EditScope.MODULE,
                M3ScopeInference.forPaths(List.of(
                        "src/main/java/p/A.java",
                        "src/main/java/q/B.java")));

        assertEquals(
                M3EditScope.PACKAGE,
                M3ScopeInference.forPaths(List.of(
                        "src/java.base/share/classes/java/lang/String.java",
                        "src/java.base/share/classes/java/lang/Object.java")));
        assertEquals(
                M3EditScope.MODULE,
                M3ScopeInference.forPaths(List.of(
                        "src/java.base/share/classes/java/lang/String.java",
                        "src/java.base/linux/classes/java/lang/Linux.java")));
        assertEquals(
                M3EditScope.MULTI_MODULE,
                M3ScopeInference.forPaths(List.of(
                        "src/java.base/share/classes/java/lang/String.java",
                        "src/java.logging/share/classes/java/util/logging/Logger.java")));

        assertEquals(
                M3EditScope.PACKAGE,
                M3ScopeInference.forPaths(List.of(
                        "src/demo/internal/A.txt",
                        "src/demo/internal/B.txt")));
        assertEquals(
                M3EditScope.PACKAGE,
                M3ScopeInference.forPaths(List.of(
                        "test/jdk/java/lang/A.java",
                        "test/jdk/java/lang/B.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                M3ScopeInference.forPaths(List.of(
                        "make/modules/java.base/A.gmk",
                        "make/modules/java.base/B.gmk")));
        assertEquals(
                M3EditScope.PACKAGE,
                M3ScopeInference.forPaths(List.of(
                        "make/common/A.gmk",
                        "make/common/B.gmk")));
        assertEquals(
                M3EditScope.PACKAGE,
                M3ScopeInference.forPaths(List.of(
                        "docs/internal/A.md",
                        "docs/internal/B.md")));
        assertEquals(
                M3EditScope.FILE,
                M3ScopeInference.forPaths(List.of("README.md")));
        assertEquals(
                M3EditScope.FILE,
                M3ScopeInference.forJavaPaths(List.of("src\\main\\java\\p\\A.java")));
    }

    @Test
    void inferenceRejectsEveryNonCanonicalPathFamily() {
        assertThrows(NullPointerException.class, () -> M3ScopeInference.forPaths(null));
        assertThrows(IllegalArgumentException.class, () -> M3ScopeInference.forPaths(List.of()));
        assertThrows(NullPointerException.class, () -> M3ScopeInference.forJavaPaths(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forJavaPaths(List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forJavaPaths(List.of("src/main/java/p/A.txt")));

        for (String path : List.of(
                "",
                "   ",
                "/src/main/java/p/A.java",
                "C:/src/main/java/p/A.java",
                "src/main/java/p/A\0.java",
                "src//main/java/p/A.java",
                "src/main/java/p/../A.java",
                "../src/main/java/p/A.java",
                "src/main/java/p/..",
                "src/main/java/p/./A.java",
                "./src/main/java/p/A.java")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> M3ScopeInference.forPaths(List.of(path)),
                    path);
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forPaths(List.of("src/java.base/share/classes/")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forPaths(List.of("test/jdk/")));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ScopeInference.forPaths(List.of("make/modules/java.base/")));
    }
}
