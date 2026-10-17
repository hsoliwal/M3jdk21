// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleSuccessorRecipeTest {
    @Test void exactIntermediateSuccessorsAndFixedPoint() throws Exception {
        for (String label : List.of("service-preservation-canonical", "hierarchy-closure-receiver", "optional-transitives-receiver")) {
            var recipe = new M3HashPinnedTextSnapshotRecipe("m3-ast-wiring-" + label + "-20261017");
            var rows = WiringSupport.manifest("text", label);
            var before = WiringSupport.inputs(rows, Path.of("preimages", label));
            var changed = WiringSupport.run(recipe, before);
            assertEquals(1, changed.size());
            assertEquals(rows.getFirst()[2], WiringSupport.hash(changed.getFirst().printAll()));
            assertTrue(WiringSupport.run(recipe, changed).isEmpty());
            assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe, List.of()));
            var drift = WiringSupport.parse(before.getFirst().getSourcePath(), before.getFirst().printAll() + "\n");
            assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe, List.of(drift)));
            if (Boolean.getBoolean("m3.wiring.applySuccessors")) {
                WiringSupport.materialize(label.endsWith("canonical") ? "canonical" : "receiver", recipe, rows);
            }
        }
    }
}
