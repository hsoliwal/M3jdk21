// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;

class TextServiceEngineRecipeTest {
    @Test void actualTypedEngineChangeAndFixedPoint() throws Exception {
        var recipe = WiringSupport.named("ASTPluginServiceAdmission");
        var rows = WiringSupport.manifest("java", "engine");
        var before = WiringSupport.inputs(rows, Path.of("preimages/engine"));
        assertEquals(1, before.size());
        var changed = WiringSupport.run(recipe, before);
        assertEquals(1, changed.size());
        var after = changed.getFirst();
        assertInstanceOf(J.CompilationUnit.class, after);
        assertEquals(rows.getFirst()[2], WiringSupport.hash(after.printAll()));
        assertTrue(WiringSupport.run(recipe, changed).isEmpty());
        assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe, List.of()));
        assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe, List.of(before.getFirst(), before.getFirst())));
        var drift = WiringSupport.parse(before.getFirst().getSourcePath(), before.getFirst().printAll() + "\n// source drift\n");
        assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe, List.of(drift)));
        WiringSupport.write(Path.of("target/candidate/engine"), after);
        // Historical candidate only. Current canonical upstream already owns service paths.
        // Never materialize this superseded extension into a receiving source checkout.
        Files.writeString(Path.of("target/ENGINE_RECEIPT.tsv"), "representation\tJAVA_LST\nresults\t1\nfixed_point_results\t0\nbefore_sha256\t" + rows.getFirst()[1] + "\nafter_sha256\t" + rows.getFirst()[2] + "\n");
    }
}
