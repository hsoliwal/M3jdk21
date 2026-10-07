// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

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
        int privateVersion = run.indexOf("SYNEXIA_RECIPE_VERSION");

        assertTrue(publicMode >= 0);
        assertTrue(publicDispatch > publicMode);
        assertTrue(inventoryRequirement > publicDispatch);
        assertTrue(privateVersion > publicDispatch);
        assertTrue(run.contains("maintainer)"));
        assertTrue(run.contains("com.synexia.m3.EveryModuleAtomPatternMastery"));

        assertTrue(verify.contains("SynexiaCanonicalFamilyPolicy.java"));
        assertTrue(verify.contains("SynexiaImportManifest.java"));
        assertTrue(verify.contains("SynexiaImporter.java"));
        assertTrue(verify.contains("SynexiaImportCli.java"));
        assertTrue(verify.contains("verify-target"));
        assertTrue(verify.contains("--release 21"));
        assertTrue(verify.contains("-Xlint:all"));
        assertTrue(verify.contains("-Werror"));
        assertFalse(verify.contains("rewrite-maven-plugin"));
        assertFalse(verify.contains("SYNEXIA_RECIPE_VERSION"));
        assertFalse(verify.contains("com.synexia:synexia-openrewrite-recipes"));

        assertTrue(readme.contains("fresh public clone"));
        assertTrue(readme.contains("M3_ATOM_PATTERN_MODE=maintainer"));
        assertTrue(readme.contains("no mutation, source-copy"));
    }

    private static Path repositoryRoot() {
        Path module = Path.of("").toAbsolutePath().normalize();
        if ("synexia-import".equals(module.getFileName().toString())) {
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
