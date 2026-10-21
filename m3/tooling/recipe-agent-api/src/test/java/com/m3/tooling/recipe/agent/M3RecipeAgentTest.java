// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tooling.recipe.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exact-source recipe qualification: successful transforms and fail-closed refusals. */
class M3RecipeAgentTest {
    @Test
    void standaloneRefusalAndExecutionProof() throws Exception {
        M3RecipeAgentProof.main(new String[0]);
    }

    @Test
    void discoveryAndProviderSelection() {
        M3RecipeCatalogue catalogue = M3RecipeCatalogue.builtins();
        assertEquals("m3.text.regex", catalogue.choose("replace-regex", "java",
                M3RecipeAgent.Authority.FILE, Set.of()).id());
        assertTrue(catalogue.discover("java-refactor", "java",
                M3RecipeAgent.Authority.FILE).stream()
                .anyMatch(candidate -> candidate.providerId().equals("openrewrite")));
        assertThrows(IllegalArgumentException.class, () -> catalogue.choose(
                "java-refactor", "java", M3RecipeAgent.Authority.FILE, Set.of()));
        assertEquals("m3.semantic.java-refactor", catalogue.choose(
                "java-refactor", "java", M3RecipeAgent.Authority.FILE,
                Set.of("openrewrite")).id());
        assertThrows(IllegalArgumentException.class, () -> catalogue.choose(
                "unknown", "java", M3RecipeAgent.Authority.PUBLIC_API, Set.of()));
        assertFalse(catalogue.listAll().isEmpty());
    }

    @Test
    void sealAndFolderPlanAreReadOnly() throws Exception {
        Path workspace = Files.createTempDirectory("m3-agent-junit");
        try {
            Files.createDirectories(workspace.resolve("src"));
            Files.writeString(workspace.resolve("src/A.java"), "class A {}\n");
            Files.writeString(workspace.resolve("src/B.java"), "class B {}\n");
            M3RecipeAgent agent = new M3RecipeAgent();
            var target = new M3RecipeAgent.Target(M3RecipeAgent.TargetKind.FOLDER, "src", "*.java");
            Map<String, String> seals = agent.seal(workspace, target);
            assertEquals(Set.of("src/A.java", "src/B.java"), seals.keySet());
            var recipe = new M3RecipeAgent.Action("package-shared", M3RecipeAgent.Operation.REGEX,
                    target, "(?m)^class ([A-Z])", "final class $1", "", seals, 2, List.of(),
                    M3RecipeAgent.Authority.FOLDER, "");
            var dryRun = agent.preview(workspace, List.of(recipe));
            assertEquals(2, dryRun.changes().size());
            assertEquals("class A {}\n", Files.readString(workspace.resolve("src/A.java")));
            agent.apply(workspace, List.of(recipe), dryRun.fingerprint(), false);
            assertEquals("final class B {}\n", Files.readString(workspace.resolve("src/B.java")));
        } finally {
            try (var stream = Files.walk(workspace)) {
                for (Path path : stream.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
