// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NebulaM3SecondPassBindingTest {
    @Test
    void exactBindingIsAuthorityFreeAndContentIsPinned() {
        assertEquals("hsoliwal/com.synexia", NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY);
        assertEquals(
                "develop",
                NebulaM3SecondPassBinding.UPSTREAM_BRANCH);
        assertTrue(NebulaM3SecondPassBinding.UPSTREAM_COMMIT.matches("[0-9a-f]{40}"));
        assertEquals(8891, NebulaM3SecondPassBinding.UPSTREAM_PR);
        assertEquals(
                "com.synexia.rewrite.M3AtomPatternSignalChainRecipe",
                NebulaM3SecondPassBinding.RECIPE);
        assertEquals(4, NebulaM3SecondPassBinding.PASS_BUDGET);
        assertEquals(
                "com.synexia.rewrite.M3AtomPatternSignalChainRecipe",
                NebulaM3SecondPassBinding.SIGNAL_CHAIN_CLASS);
        assertEquals(
                "com.synexia.m3.recipe.OpenRewriteRecipeDagPlan",
                NebulaM3SecondPassBinding.CANONICAL_CATALOG);
        assertEquals("SHARED_JVM_COMPOSITE", NebulaM3SecondPassBinding.STATE_MODE);
        assertFalse(NebulaM3SecondPassBinding.EXTERNAL_LEAF_FAN_OUT);
        assertEquals("REQUESTED_CURRENT_NEBULA_REPROOF_20261013", NebulaM3SecondPassBinding.HOSTED_PROOF);
        assertFalse(NebulaM3SecondPassBinding.sourceMutationAuthority());
        assertFalse(NebulaM3SecondPassBinding.replacementAuthority());
        assertFalse(NebulaM3SecondPassBinding.promotionAuthority());
    }

    @Test
    void checkedInBindingLedgerMatchesJavaOwnerExactly() throws Exception {
        Path repository = repositoryRoot();
        Path ledger = repository.resolve("m3/catalogue/second-pass-recipe-binding.tsv");

        assertTrue(Files.isRegularFile(ledger));
        assertEquals(NebulaM3SecondPassBinding.tsv(), Files.readString(ledger));
    }

    @Test
    void exactBindingFailsClosedOnAnyDrift() {
        NebulaM3SecondPassBinding.requireExact(
                NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY,
                NebulaM3SecondPassBinding.UPSTREAM_BRANCH,
                NebulaM3SecondPassBinding.UPSTREAM_COMMIT,
                NebulaM3SecondPassBinding.RECIPE,
                NebulaM3SecondPassBinding.PASS_BUDGET);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3SecondPassBinding.requireExact(
                                NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY,
                                NebulaM3SecondPassBinding.UPSTREAM_BRANCH,
                                "0".repeat(40),
                                NebulaM3SecondPassBinding.RECIPE,
                                NebulaM3SecondPassBinding.PASS_BUDGET));
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("widgets"))
                    && Files.isDirectory(current.resolve("m3"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Nebula repository root not found");
    }
    @Test
    void policyAndConvergencePlanCarryTheExactReadOnlyGate() throws Exception {
        Path repository = repositoryRoot();
        String policy =
                Files.readString(repository.resolve(".m3/atom-pattern/policy.properties"));
        String plan = Files.readString(repository.resolve("m3/convergence/PLAN.tsv"));

        assertTrue(policy.contains(
                "java.secondPassRecipe=" + NebulaM3SecondPassBinding.RECIPE));
        assertTrue(policy.contains(
                "java.secondPassPassBudget=" + NebulaM3SecondPassBinding.PASS_BUDGET));
        assertTrue(policy.contains(
                "java.secondPassUpstreamCommit=" + NebulaM3SecondPassBinding.UPSTREAM_COMMIT));
        assertTrue(policy.contains(
                "java.secondPassSignalChainClass=" + NebulaM3SecondPassBinding.SIGNAL_CHAIN_CLASS));
        assertTrue(policy.contains(
                "java.secondPassCanonicalCatalog=" + NebulaM3SecondPassBinding.CANONICAL_CATALOG));
        assertTrue(policy.contains(
                "java.secondPassStateMode=" + NebulaM3SecondPassBinding.STATE_MODE));
        assertTrue(policy.contains("java.secondPassExternalLeafFanOut=false"));
        assertTrue(policy.contains("java.secondPassMutation=false"));
        assertTrue(policy.contains("java.secondPassReplacement=false"));
        assertTrue(policy.contains("java.secondPassPromotion=false"));

        assertTrue(plan.contains(
                "4\tSECOND_PASS_SIGNAL_CHAIN\t"
                        + NebulaM3SecondPassBinding.RECIPE
                        + "\tFILE\tREAD_ONLY\t"));
        assertTrue(plan.contains("9\tFILE_FIXED_POINT\t"));
    }

    @Test
    void rootPomUsesSavedCanonicalSecondPassRecipe() throws Exception {
        Path repository = repositoryRoot();
        String pom = Files.readString(repository.resolve("pom.xml"));

        assertTrue(pom.contains(
                "<recipe>" + NebulaM3SecondPassBinding.RECIPE + "</recipe>"));
    }

}
