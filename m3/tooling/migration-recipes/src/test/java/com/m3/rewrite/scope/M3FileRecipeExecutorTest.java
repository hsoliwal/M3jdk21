// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class M3FileRecipeExecutorTest {
    private static final M3RecipeScopePolicy FILE_POLICY = new M3RecipeScopePolicy(
            M3EditScope.FILE,
            M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
            false);

    @Test
    void registeredFileRecipeRunsToFixedPointAndThenBecomesNoOp() {
        SourceFile before = parse(
                "src/main/java/example/Sample.java",
                """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """);

        var first = M3FileRecipeExecutor.applyToFixedPoint(
                new M3AtomizePureIntReturnRecipe(), before, 4);
        assertTrue(first.changed());
        assertEquals(1, first.mutationPasses());
        assertTrue(first.source().printAll().contains("M3-IOP: PURE_INT_EXPRESSION"));

        var second = M3FileRecipeExecutor.applyToFixedPoint(
                new M3AtomizePureIntReturnRecipe(), first.source(), 4);
        assertFalse(second.changed());
        assertEquals(0, second.mutationPasses());
        assertEquals(first.source().printAll(), second.source().printAll());
    }

    @Test
    void refusesInvalidInputsAndBroaderAuthority() {
        SourceFile source = parse(
                "src/main/java/example/A.java",
                "package example; final class A {}");

        assertThrows(
                IllegalArgumentException.class,
                () -> M3FileRecipeExecutor.applyToFixedPoint(
                        new NoOpRecipe(), FILE_POLICY, source, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3FileRecipeExecutor.applyToFixedPoint(
                        new NoOpRecipe(),
                        new M3RecipeScopePolicy(
                                M3EditScope.PACKAGE,
                                M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING,
                                false),
                        source,
                        2));
        assertThrows(
                NullPointerException.class,
                () -> M3FileRecipeExecutor.applyToFixedPoint(
                        null, FILE_POLICY, source, 2));
        assertThrows(
                NullPointerException.class,
                () -> M3FileRecipeExecutor.applyToFixedPoint(
                        new NoOpRecipe(), null, source, 2));
        assertThrows(
                NullPointerException.class,
                () -> M3FileRecipeExecutor.applyToFixedPoint(
                        new NoOpRecipe(), FILE_POLICY, null, 2));
    }

    @Test
    void refusesRenameDeleteAndNonConvergence() {
        SourceFile source = parse(
                "src/main/java/example/A.java",
                "package example; final class A {}");

        assertThrows(
                IllegalStateException.class,
                () -> M3FileRecipeExecutor.applyToFixedPoint(
                        new RenameRecipe(), FILE_POLICY, source, 2));
        assertThrows(
                IllegalStateException.class,
                () -> M3FileRecipeExecutor.applyToFixedPoint(
                        new DeleteRecipe(), FILE_POLICY, source, 2));
        assertThrows(
                IllegalStateException.class,
                () -> M3FileRecipeExecutor.applyToFixedPoint(
                        new ToggleClassNameRecipe(), FILE_POLICY, source, 2));
    }

    @Test
    void executionRecordProtectsItsOwnContract() {
        SourceFile source = parse(
                "src/main/java/example/A.java",
                "package example; final class A {}");
        var unchanged = new M3FileRecipeExecutor.Execution(source, 0);
        var changed = new M3FileRecipeExecutor.Execution(source, 2);

        assertFalse(unchanged.changed());
        assertTrue(changed.changed());
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3FileRecipeExecutor.Execution(source, -1));
        assertThrows(
                NullPointerException.class,
                () -> new M3FileRecipeExecutor.Execution(null, 0));
    }

    private static SourceFile parse(String path, String source) {
        var context = new org.openrewrite.InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        List<Parser.Input> inputs = List.of(Parser.Input.fromString(Path.of(path), source));
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .findFirst()
                .orElseThrow();
    }

    private abstract static class TestRecipe extends Recipe {
        @Override
        public String getDescription() {
            return "M3 FILE executor test recipe.";
        }
    }

    private static final class NoOpRecipe extends TestRecipe {
        @Override
        public String getDisplayName() {
            return "No-op";
        }
    }

    private static final class RenameRecipe extends TestRecipe {
        @Override
        public String getDisplayName() {
            return "Rename";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(
                        J.CompilationUnit compilationUnit, ExecutionContext context) {
                    return super.visitCompilationUnit(compilationUnit, context)
                            .withSourcePath(Path.of("src/main/java/example/B.java"));
                }
            };
        }
    }

    private static final class DeleteRecipe extends TestRecipe {
        @Override
        public String getDisplayName() {
            return "Delete";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(
                        J.CompilationUnit compilationUnit, ExecutionContext context) {
                    return null;
                }
            };
        }
    }

    private static final class ToggleClassNameRecipe extends TestRecipe {
        @Override
        public String getDisplayName() {
            return "Toggle class name";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(
                        J.ClassDeclaration classDeclaration, ExecutionContext context) {
                    J.ClassDeclaration visited =
                            super.visitClassDeclaration(classDeclaration, context);
                    String next = "A".equals(visited.getSimpleName()) ? "B" : "A";
                    return visited.withName(visited.getName().withSimpleName(next));
                }
            };
        }
    }
}
