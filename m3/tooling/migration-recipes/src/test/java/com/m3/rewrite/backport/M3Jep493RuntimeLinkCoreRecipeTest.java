// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

final class M3Jep493RuntimeLinkCoreRecipeTest {
    private static final Set<String> EXPECTED = Set.of(
            "src/jdk.jlink/share/classes/jdk/tools/jlink/internal/JRTArchive.java",
            "src/jdk.jlink/share/classes/jdk/tools/jlink/internal/LinkableRuntimeImage.java",
            "src/jdk.jlink/share/classes/jdk/tools/jlink/internal/runtimelink/JimageDiffGenerator.java",
            "src/jdk.jlink/share/classes/jdk/tools/jlink/internal/runtimelink/ResourceDiff.java",
            "src/jdk.jlink/share/classes/jdk/tools/jlink/internal/runtimelink/ResourcePoolReader.java");

    @Test
    void wrapperPinsFinalGaDonorAndModuleScope() {
        var recipe = new M3Jep493RuntimeLinkCoreRecipe();
        assertEquals(
                "2ec358082f0896480bdbfcb289b4ba2bff0dd828",
                M3Jep493RuntimeLinkCoreRecipe.UPSTREAM_IMPLEMENTATION_COMMIT);
        assertEquals("jdk-24+36", M3Jep493RuntimeLinkCoreRecipe.DONOR_GA);
        assertEquals("jdk24-jep493-runtime-link-core", M3Jep493RuntimeLinkCoreRecipe.CRATE);
        assertEquals(1, recipe.getRecipeList().size());

        var delegate = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class,
                recipe.getRecipeList().getFirst());
        assertEquals(M3Jep493RuntimeLinkCoreRecipe.CRATE, delegate.getCrateName());

        var policy = M3RecipeScopeRegistry.require(M3Jep493RuntimeLinkCoreRecipe.class);
        assertEquals(M3EditScope.MODULE, policy.minimumScope());
        assertEquals(M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING, policy.contractMode());
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("jep-493"));
        assertTrue(recipe.getTags().contains("hash-pinned"));
    }

    @Test
    void emptyJdk21PreimageGeneratesFiveJava21SourcesThenFixedPoint() {
        var recipe = new M3Jep493RuntimeLinkCoreRecipe();
        var first = recipe.run(new InMemoryLargeSourceSet(List.of()), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();
        assertEquals(5, changes.size());

        TreeSet<String> paths = new TreeSet<>();
        List<SourceFile> generated = changes.stream()
                .map(Result::getAfter)
                .peek(source -> paths.add(normalized(source)))
                .toList();

        assertEquals(new TreeSet<>(EXPECTED), paths);
        assertTrue(generated.stream().allMatch(source -> source.printAll().contains("package ")));

        var second = recipe.run(new InMemoryLargeSourceSet(generated), context(), 1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void finalGaCoreDoesNotReintroduceRemovedRuntimeImageLinkException() {
        assertTrue(EXPECTED.stream().noneMatch(path -> path.endsWith("RuntimeImageLinkException.java")));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new IllegalStateException(error);
        });
    }

    private static String normalized(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }
}
