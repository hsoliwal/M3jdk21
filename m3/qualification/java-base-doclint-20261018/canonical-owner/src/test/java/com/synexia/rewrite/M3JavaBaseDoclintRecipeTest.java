// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** Comments alone are admitted; every compiler diagnostic is accounted for by owner. */
final class M3JavaBaseDoclintRecipeTest {
    private static final String CRATE = "m3-java-base-doclint-20261018";
    @Test void sdkCommentsFixedPointAndSerialStates() throws Exception {
        var recipe = new M3JavaBaseDoclintRecipe();
        var baseline = M3SourceOnlyRecipeProofSupport.sources(CRATE, false);
        var after = M3SourceOnlyRecipeProofSupport.generate(CRATE, recipe);
        assertEquals(12, after.size());
        for (String path : baseline.keySet()) {
            assertEquals(M3SourceOnlyRecipeProofSupport.syntax(baseline.get(path)),
                    M3SourceOnlyRecipeProofSupport.syntax(after.get(path)), path);
        }
        assertTrue(M3SourceOnlyRecipeProofSupport.run(recipe,
                M3SourceOnlyRecipeProofSupport.parse(after)).isEmpty());
        var partial = new TreeMap<>(baseline);
        for (String path : after.keySet()) {
            partial.put(path, after.get(path));
            var expected = new TreeMap<>(after);
            expected.entrySet().removeIf(e -> e.getValue().equals(partial.get(e.getKey())));
            assertEquals(expected, M3SourceOnlyRecipeProofSupport.run(recipe,
                    M3SourceOnlyRecipeProofSupport.parse(partial)));
        }
    }
    @Test void missingDuplicateDriftAndResourceCorruptionRefuse() throws Exception {
        M3SourceOnlyRecipeProofSupport.refuseChanges(CRATE, new M3JavaBaseDoclintRecipe());
    }
    @Test void strictCompilerAccountsForEachOwner() throws Exception {
        M3SourceOnlyRecipeProofSupport.generate(CRATE, new M3JavaBaseDoclintRecipe());
        M3SourceOnlyRecipeProofSupport.external("verify_doclint.py");
    }
}
