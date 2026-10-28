// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/** Only the five source-retained method annotations may change after the doclint stage. */
final class M3ProgressScopeLintRecipeTest {
    private static final String CRATE = "m3-progress-scope-lint-20261018";
    @Test void sdkExactAnnotationsAllPartialStatesAndRefusals() throws Exception {
        var recipe = new M3ProgressScopeLintRecipe();
        var before = M3SourceOnlyRecipeProofSupport.sources(CRATE, false);
        var after = M3SourceOnlyRecipeProofSupport.generate(CRATE, recipe);
        var paths = new ArrayList<>(after.keySet());
        assertEquals(4, paths.size());
        int annotations = 0;
        for (String path : paths) {
            String marker = "    @SuppressWarnings(\"try\")\n";
            annotations += after.get(path).split(java.util.regex.Pattern.quote(marker), -1).length - 1;
            assertEquals(before.get(path), after.get(path).replace(marker, ""), path);
        }
        assertEquals(5, annotations);
        for (int state = 0; state < 16; state++) {
            var partial = new TreeMap<>(before);
            var expected = new TreeMap<>(after);
            for (int bit = 0; bit < paths.size(); bit++) if ((state & (1 << bit)) != 0) {
                partial.put(paths.get(bit), after.get(paths.get(bit)));
                expected.remove(paths.get(bit));
            }
            assertEquals(expected, M3SourceOnlyRecipeProofSupport.run(recipe,
                    M3SourceOnlyRecipeProofSupport.parse(partial)));
        }
        M3SourceOnlyRecipeProofSupport.refuseChanges(CRATE, recipe);
    }
    @Test void strictCompilerBytecodeAndPublicApiParity() throws Exception {
        M3SourceOnlyRecipeProofSupport.generate(CRATE, new M3ProgressScopeLintRecipe());
        M3SourceOnlyRecipeProofSupport.external("verify_scope_lint.py");
    }
}
