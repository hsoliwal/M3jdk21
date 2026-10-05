// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.InstallIndexStringCompatibility;
import com.m3.rewrite.M3Java21ConvergenceRecipe;
import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates;
import com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe;
import com.m3.rewrite.atom.M3DocumentPureIntAtomRecipe;
import com.m3.rewrite.atom.M3PureIntConvergenceRecipe;
import com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe;
import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import com.m3.rewrite.backport.M3VerbatimJavaPairRecipe;
import com.m3.rewrite.backport.M3Jep458BackportRecipe;
import com.m3.rewrite.backport.M3Jep493RuntimeLinkCoreRecipe;
import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import com.synexia.rewrite.M3MIndexJoinedCharsViewRecipe;
import com.synexia.rewrite.M3SegmentedLaneNativeRecipe;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3RecipeScopeDeclarationTest {
    @Test
    void everyCurrentRecipeHasExternalScopeAuthority() {
        assertEquals(15, M3RecipeScopeRegistry.size());
        assertTrue(M3RecipeScopeRegistry.registered(M3MIndexJoinedCharsViewRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3SegmentedLaneNativeRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(InstallIndexStringCompatibility.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3HashPinnedJavaSnapshotRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3AtomizePureIntReturnRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3InventoryPureIntAtomCandidates.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3PatternizePureIntAtomRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3DocumentPureIntAtomRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3PureIntConvergenceRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3Java21ConvergenceRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3Jdk21HashPinnedSnapshotRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3Jdk21HashPinnedTextSnapshotRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3VerbatimJavaPairRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3Jep458BackportRecipe.class.getName()));
        assertTrue(M3RecipeScopeRegistry.registered(M3Jep493RuntimeLinkCoreRecipe.class.getName()));
        var jep = M3RecipeScopeRegistry.require(M3Jep458BackportRecipe.class);
        assertEquals(M3EditScope.LIBRARY_API, jep.minimumScope());
        assertEquals(M3ContractMode.EXPLICIT_CONTRACT_CHANGE, jep.contractMode());
        assertFalse(jep.fileLocalMechanical(List.of("src/main/java/a/A.java")));
        var jep493 = M3RecipeScopeRegistry.require(M3Jep493RuntimeLinkCoreRecipe.class);
        assertEquals(M3EditScope.MODULE, jep493.minimumScope());
        assertEquals(M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING, jep493.contractMode());
        assertFalse(jep493.fileLocalMechanical(List.of(
                "src/jdk.jlink/share/classes/jdk/tools/jlink/internal/JRTArchive.java")));
        assertFalse(M3RecipeScopeRegistry.registered(null));
        assertFalse(M3RecipeScopeRegistry.registered("missing.Recipe"));
        assertThrows(NullPointerException.class, () -> M3RecipeScopeRegistry.require((Class<?>) null));
        assertThrows(NullPointerException.class, () -> M3RecipeScopeRegistry.require((String) null));
        assertThrows(IllegalArgumentException.class, () -> M3RecipeScopeRegistry.require("missing.Recipe"));
    }

    @Test
    void fixedAndTargetInferredScopesFollowTheM3PromotionOrder() {
        var filePolicy = M3RecipeScopeRegistry.require(M3MIndexJoinedCharsViewRecipe.class);
        assertEquals(M3EditScope.MODULE, filePolicy.resolve(List.of("src/main/java/a/A.java")));
        assertFalse(filePolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var nativePolicy = M3RecipeScopeRegistry.require(M3SegmentedLaneNativeRecipe.class);
        assertEquals(M3EditScope.FILE, nativePolicy.resolve(List.of("src/main/java/a/A.java")));

        var modulePolicy = M3RecipeScopeRegistry.require(InstallIndexStringCompatibility.class);
        assertEquals(M3EditScope.MODULE, modulePolicy.resolve(List.of("src/main/java/a/A.java")));
        assertFalse(modulePolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var atomPolicy = M3RecipeScopeRegistry.require(M3AtomizePureIntReturnRecipe.class);
        assertEquals(M3EditScope.FILE, atomPolicy.resolve(List.of("src/main/java/a/A.java")));
        assertTrue(atomPolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        var inventoryPolicy = M3RecipeScopeRegistry.require(M3InventoryPureIntAtomCandidates.class);
        assertEquals(M3EditScope.FILE, inventoryPolicy.resolve(List.of("src/main/java/a/A.java")));
        assertTrue(inventoryPolicy.fileLocalMechanical(List.of("src/main/java/a/A.java")));

        for (Class<?> recipe : List.of(
                M3PatternizePureIntAtomRecipe.class,
                M3DocumentPureIntAtomRecipe.class,
                M3PureIntConvergenceRecipe.class,
                M3Java21ConvergenceRecipe.class,
                M3Jdk21HashPinnedSnapshotRecipe.class,
                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                M3VerbatimJavaPairRecipe.class)) {
            var policy = M3RecipeScopeRegistry.require(recipe);
            assertEquals(M3EditScope.FILE, policy.resolve(List.of("src/main/java/a/A.java")));
            assertTrue(policy.fileLocalMechanical(List.of("src/main/java/a/A.java")));
        }

        var jdkJavaPolicy = M3RecipeScopeRegistry.require(M3Jdk21HashPinnedSnapshotRecipe.class);
        assertEquals(
                M3EditScope.FILE,
                jdkJavaPolicy.resolve(List.of(
                        "src/jdk.javadoc/share/classes/jdk/javadoc/internal/Foo.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                jdkJavaPolicy.resolve(List.of(
                        "src/jdk.javadoc/share/classes/jdk/javadoc/internal/A.java",
                        "src/jdk.javadoc/share/classes/jdk/javadoc/internal/B.java")));
        assertEquals(
                M3EditScope.MULTI_MODULE,
                jdkJavaPolicy.resolve(List.of(
                        "src/java.base/share/classes/java/lang/A.java",
                        "src/jdk.javadoc/share/classes/jdk/javadoc/B.java")));

        var jdkTextPolicy = M3RecipeScopeRegistry.require(M3Jdk21HashPinnedTextSnapshotRecipe.class);
        assertEquals(
                M3EditScope.MODULE,
                jdkTextPolicy.resolve(List.of(
                        "src/jdk.javadoc/share/classes/jdk/javadoc/internal/resources/standard.properties",
                        "src/jdk.javadoc/share/man/javadoc.md")));

        var inferredPolicy = M3RecipeScopeRegistry.require(M3HashPinnedJavaSnapshotRecipe.class);
        assertEquals(
                M3EditScope.FILE,
                inferredPolicy.resolve(List.of("src/main/java/a/A.java")));
        assertEquals(
                M3EditScope.PACKAGE,
                inferredPolicy.resolve(List.of(
                        "src/main/java/a/A.java",
                        "src/main/java/a/B.java")));
        assertEquals(
                M3EditScope.MODULE,
                inferredPolicy.resolve(List.of(
                        "src/main/java/a/A.java",
                        "src/main/java/b/B.java")));
    }

    @Test
    void currentRegistryIsBehaviorAndContractPreserving() {
        for (Class<?> recipe : List.of(
                M3MIndexJoinedCharsViewRecipe.class,
                M3SegmentedLaneNativeRecipe.class,
                InstallIndexStringCompatibility.class,
                M3HashPinnedJavaSnapshotRecipe.class,
                M3AtomizePureIntReturnRecipe.class,
                M3InventoryPureIntAtomCandidates.class,
                M3PatternizePureIntAtomRecipe.class,
                M3DocumentPureIntAtomRecipe.class,
                M3PureIntConvergenceRecipe.class,
                M3Java21ConvergenceRecipe.class)) {
            assertEquals(
                    M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                    M3RecipeScopeRegistry.require(recipe).contractMode());
        }
    }
}
