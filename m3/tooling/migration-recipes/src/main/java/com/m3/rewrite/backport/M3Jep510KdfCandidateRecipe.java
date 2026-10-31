// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Complete source recipe DAG for the unpromoted JEP 510 candidate.
 *
 * <p>Product API/provider postimages run before their exact security-test postimages. Build,
 * jtreg, runtime, A3, fixed-point and canonical-readback gates remain external acceptance gates.</p>
 */
public final class M3Jep510KdfCandidateRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Materialize JEP 510 KDF candidate and security tests";
    }

    @Override
    public String getDescription() {
        return "Composes the exact KDF product crate followed by the pinned HKDF security/jtreg "
                + "test crate; the result remains an unpromoted M3JDK21 candidate.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-510",
                "kdf",
                "security",
                "recipe-dag",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jep510KdfBackportRecipe(),
                new M3Jep510KdfSecurityTestsRecipe());
    }
}
