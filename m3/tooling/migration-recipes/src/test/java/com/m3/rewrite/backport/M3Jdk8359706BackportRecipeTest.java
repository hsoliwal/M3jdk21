// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class M3Jdk8359706BackportRecipeTest {
    @Test
    void recipeBindsJava21UpdateLineFeatureAndMandatoryMacRepair() {
        Recipe recipe = new M3Jdk8359706BackportRecipe();

        assertEquals(
                "3da873b1aa23c64e47a5da3352b7b48e8c2c74cd",
                M3Jdk8359706BackportRecipe.JDK21U_COMMIT);
        assertEquals(
                "b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e",
                M3Jdk8359706BackportRecipe.MAINLINE_COMMIT);
        assertEquals(
                "f696cb16462f0201a10540ca36049472a79eba94",
                M3Jdk8359706BackportRecipe.MACOS_REPAIR_COMMIT);
        assertEquals(
                "3a109f49feb19f313632be6a2aa24ba7d9b7269b",
                M3Jdk8359706BackportRecipe.MACOS_REPAIR_MAINLINE);
        assertEquals(2, recipe.getRecipeList().size());

        var text = assertInstanceOf(
                M3Jdk21HashPinnedTextSnapshotRecipe.class,
                recipe.getRecipeList().get(0));
        var java = assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class,
                recipe.getRecipeList().get(1));

        assertEquals("jdk21u-open-fds-8359706-text", text.getCrateName());
        assertEquals("m3-jdk21u-open-fds-8359706-java", java.getCrateName());
        assertEquals(7, text.targetPaths().size());
        assertEquals(1, java.targetPaths().size());
        assertTrue(text.targetPaths().contains(
                "src/hotspot/os/bsd/os_bsd.cpp"));
        assertTrue(java.targetPaths().contains(
                "test/jdk/sun/tools/jcmd/TestJcmdSanity.java"));
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("jdk-8380236"));
    }

    @Test
    void checkedInCandidateIsExactRecipeFixedPoint() throws Exception {
        M3Jdk8359706BackportRecipe recipe = new M3Jdk8359706BackportRecipe();
        List<SourceFile> sources = currentCandidateSources(recipe);

        var run = recipe.run(
                new InMemoryLargeSourceSet(sources),
                context(),
                1);

        assertTrue(run.getChangeset().getAllResults().isEmpty());
        String bsd = Files.readString(
                root().resolve("src/hotspot/os/bsd/os_bsd.cpp"));
        assertTrue(bsd.contains(
                "precond(buflen >= sizeof(struct proc_fdinfo));"));
        assertTrue(bsd.contains(
                "Open File Descriptors: > %zu"));
    }

    @Test
    void anyPostimageDriftFailsClosed() throws Exception {
        M3Jdk8359706BackportRecipe recipe = new M3Jdk8359706BackportRecipe();
        List<SourceFile> sources = new ArrayList<>(currentCandidateSources(recipe));

        int target = indexOf(
                sources,
                "src/hotspot/os/linux/os_linux.cpp");
        PlainText linux = assertInstanceOf(PlainText.class, sources.get(target));
        sources.set(
                target,
                linux.withText(linux.getText() + "\n// drift\n"));

        assertThrows(
                RuntimeException.class,
                () ->
                        recipe.run(
                                        new InMemoryLargeSourceSet(sources),
                                        context(),
                                        1)
                                .getChangeset()
                                .getAllResults());
    }

    private static List<SourceFile> currentCandidateSources(
            M3Jdk8359706BackportRecipe recipe) throws Exception {
        var text = (M3Jdk21HashPinnedTextSnapshotRecipe) recipe.getRecipeList().get(0);
        var java = (M3Jdk21HashPinnedSnapshotRecipe) recipe.getRecipeList().get(1);
        ArrayList<SourceFile> sources = new ArrayList<>();

        for (String target : text.targetPaths()) {
            sources.add(
                    PlainText.builder()
                            .sourcePath(Path.of(target))
                            .text(Files.readString(root().resolve(target)))
                            .build());
        }

        InMemoryExecutionContext context = context();
        for (String target : java.targetPaths()) {
            String source = Files.readString(root().resolve(target));
            SourceFile parsed =
                    JavaParser.fromJavaVersion()
                            .build()
                            .parseInputs(
                                    List.of(
                                            Parser.Input.fromString(
                                                    Path.of(target),
                                                    source)),
                                    null,
                                    context)
                            .findFirst()
                            .orElseThrow();
            sources.add(parsed);
        }
        return List.copyOf(sources);
    }

    private static int indexOf(List<SourceFile> sources, String path) {
        for (int index = 0; index < sources.size(); index++) {
            if (path.equals(
                    sources.get(index)
                            .getSourcePath()
                            .toString()
                            .replace('\\', '/'))) {
                return index;
            }
        }
        throw new IllegalStateException("missing test source " + path);
    }

    private static Path root() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(
                            current.resolve(
                                    "m3/tooling/migration-recipes/pom.xml"))
                    && Files.isRegularFile(
                            current.resolve(
                                    "src/hotspot/share/runtime/os.hpp"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate M3JDK21 repository root");
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }
}
