// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

class MaskPredecessorRecipeTest {
    @Test void replayOriginalReceiverFromItsOriginalInputs() throws Exception {
        String root = "m3jdk21-mask-family-receiver-20261016";
        for (String kind : List.of("java", "text")) {
            String crate = kind.equals("java") ? root : root + "-native";
            String prefix = "/com/synexia/rewrite/hash-pinned-" + kind + "/" + crate + "/";
            var rows = MaskRecipeSupport.manifest(kind, crate);
            var texts = new TreeMap<String, String>();
            for (var row : rows) if (!row[1].equals("ABSENT")) {
                String before = MaskRecipeSupport.resource(prefix + "pre/" + row[3]);
                assertEquals(row[1], MaskRecipeSupport.hash(before));
                texts.put(row[0], before);
            }
            Recipe recipe = kind.equals("java") ? new M3HashPinnedJavaSnapshotRecipe(crate) : new M3HashPinnedTextSnapshotRecipe(crate);
            var before = MaskRecipeSupport.parse(texts);
            var changed = MaskRecipeSupport.run(recipe, before);
            assertEquals(rows.size(), changed.size());
            var after = MaskRecipeSupport.merge(before, changed);
            for (var file : after) {
                var row = rows.stream().filter(x -> x[0].equals(file.getSourcePath().toString())).findFirst().orElseThrow();
                assertEquals(row[2], MaskRecipeSupport.hash(file.printAll()));
            }
            assertTrue(MaskRecipeSupport.run(recipe, after).isEmpty());
            MaskRecipeSupport.write(MaskRecipeSupport.output().resolve("predecessor"), after);
        }
    }
}
