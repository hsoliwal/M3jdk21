// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServiceOrderRecoveryRecipeTest {
    @Test void exactIntermediatePomRecoveryAndFixedPoint() throws Exception {
        for (String label : List.of("canonical", "receiver")) {
            String recovery = "service-order-" + label;
            var recipe = new M3HashPinnedTextSnapshotRecipe("m3-ast-wiring-" + recovery + "-20261017");
            var rows = WiringSupport.manifest("text", recovery);
            var before = WiringSupport.inputs(rows, Path.of("preimages", recovery));
            var changed = WiringSupport.run(recipe, before);
            assertEquals(1, changed.size());
            assertEquals(rows.getFirst()[2], WiringSupport.hash(changed.getFirst().printAll()));
            assertTrue(WiringSupport.run(recipe, changed).isEmpty());
            assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe, List.of()));
            if (Boolean.getBoolean("m3.wiring.recoverServiceOrder")) {
                WiringSupport.materialize(label, recipe, rows);
            }
        }
    }
}
