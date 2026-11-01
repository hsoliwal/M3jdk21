// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Modified 2026-10-07: retain concurrent A3 export checks and check the exact README authority boundary.
package jdk.m3.importer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PublicAtomPatternReceiverTest {

    @Test
    void publicModeIsSelfContainedAndMaintainerModeIsExplicit() throws Exception {
        Path repository = repositoryRoot();
        String run = Files.readString(repository.resolve(".m3/atomize-patternize/run.sh"));
        String verify = Files.readString(repository.resolve(".m3/atomize-patternize/public-verify.sh"));
        String readme = Files.readString(repository.resolve(".m3/atomize-patternize/README.md"));

        int publicMode = run.indexOf("M3_ATOM_PATTERN_MODE:-public");
        int publicDispatch = run.indexOf("public-verify.sh");
        int inventoryRequirement = run.indexOf("M3_PLAN_INVENTORY_REQUIRED");
        int privateVersion = run.indexOf("M3_RECIPE_VERSION");

        assertTrue(publicMode >= 0);
        assertTrue(publicDispatch > publicMode);
        assertTrue(inventoryRequirement > publicDispatch);
        assertTrue(privateVersion > publicDispatch);
        assertTrue(run.contains("maintainer)"));
        assertTrue(run.contains("jdk.internal.m3.EveryModuleAtomPatternMastery"));

        assertTrue(verify.contains("M3CanonicalFamilyPolicy.java"));
        assertTrue(verify.contains("M3ImportManifest.java"));
        assertTrue(verify.contains("M3Importer.java"));
        assertTrue(verify.contains("M3ImportCli.java"));
        assertTrue(verify.contains("verify-target"));
        assertTrue(verify.contains("--release 21"));
        assertTrue(verify.contains("-Xlint:all"));
        assertTrue(verify.contains("-Werror"));
        assertTrue(verify.contains("jdk-a3-recipe-export.tsv"));
        assertTrue(verify.contains("b691460aebcd458f5d5fb0d508b4a4a05ef8d6c5"));
        assertTrue(verify.contains("PUBLIC_A3_EXPORT_SOURCE_DRIFT"));
        assertTrue(verify.contains("a3ExportFiles"));
        assertFalse(verify.contains("rewrite-maven-plugin"));
        assertFalse(verify.contains("M3_RECIPE_VERSION"));
        assertFalse(verify.contains("com.synexia:m3-openrewrite-recipes"));

        assertTrue(readme.contains("fresh public clone"));
        assertTrue(readme.contains("M3_ATOM_PATTERN_MODE=maintainer"));
        assertTrue(readme.contains(
                "Neither mode grants source-copy, replacement, merge, semantic-equivalence or promotion authority."));
    }

    private static Path repositoryRoot() {
        Path module = Path.of("").toAbsolutePath().normalize();
        if ("m3-import".equals(module.getFileName().toString())) {
            return module.getParent().getParent();
        }
        Path multi = Path.of(
                        System.getProperty("maven.multiModuleProjectDirectory", "."))
                .toAbsolutePath()
                .normalize();
        return "m3".equals(multi.getFileName().toString())
                ? multi.getParent()
                : multi;
    }
}
