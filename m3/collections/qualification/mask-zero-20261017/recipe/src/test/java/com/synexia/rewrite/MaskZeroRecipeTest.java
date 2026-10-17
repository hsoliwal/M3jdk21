// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MaskZeroRecipeTest {
    private static final String CRATE = "m3-mask-zero-20261017";
    private static final Set<String> OWNERS = Set.of(
            "m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java",
            "m3/collections/src/main/java/com/m3/collections/M3LongArrayList.java",
            "m3/collections/src/main/java/com/m3/collections/M3LongArrayDeque.java",
            "m3/collections/src/test/java/com/m3/collections/M3IndexMaskCompactorSelfTest.java",
            "m3/collections/src/main/native/collections/m3_index_mask_compactor.c",
            ".github/workflows/m3-collections-mask-receiver.yml");

    private static List<String[]> rows() throws Exception {
        var rows = new ArrayList<>(MaskRecipeSupport.manifest("java", CRATE));
        rows.addAll(MaskRecipeSupport.manifest("text", CRATE));
        assertEquals(OWNERS.size(), rows.size());
        assertEquals(OWNERS, rows.stream().map(r -> r[0]).collect(Collectors.toSet()));
        return rows;
    }

    @Test void actualQualifiedSuccessorFixedPointAndMaterialization() throws Exception {
        var rows = rows();
        var recipe = MaskRecipeSupport.named();
        assertTrue(containsProvider(recipe, M3HashPinnedJavaSnapshotRecipe.class));
        assertTrue(containsProvider(recipe, M3HashPinnedTextSnapshotRecipe.class));
        var before = MaskRecipeSupport.inputs(rows, Path.of("preimages"));
        assertEquals(6, before.size());
        var changed = MaskRecipeSupport.run(recipe, before);
        assertEquals(5, changed.size());
        assertTrue(changed.stream().noneMatch(x -> x.getSourcePath().toString().endsWith("/M3IndexMaskCompactor.java")));
        var after = MaskRecipeSupport.merge(before, changed);
        assertEquals(6, after.size());
        for (var file : after) {
            var row = rows.stream().filter(x -> x[0].equals(file.getSourcePath().toString())).findFirst().orElseThrow();
            assertEquals(row[2], MaskRecipeSupport.hash(file.printAll()));
        }
        assertTrue(MaskRecipeSupport.run(recipe, after).isEmpty());
        Collections.reverse(after);
        assertTrue(MaskRecipeSupport.run(recipe, after).isEmpty());
        MaskRecipeSupport.write(MaskRecipeSupport.output().resolve("before"), before);
        MaskRecipeSupport.write(MaskRecipeSupport.output().resolve("after"), after);
        String receiver = System.getProperty("m3.mask.receiver", "");
        if (!receiver.isBlank()) {
            Path root = Path.of(receiver);
            var current = MaskRecipeSupport.inputs(rows, root);
            var updates = MaskRecipeSupport.run(recipe, current);
            var applied = MaskRecipeSupport.merge(current, updates);
            assertEquals(6, applied.size());
            assertTrue(MaskRecipeSupport.run(recipe, applied).isEmpty());
            MaskRecipeSupport.write(root, updates);
            assertTrue(MaskRecipeSupport.run(recipe, MaskRecipeSupport.inputs(rows, root)).isEmpty());
        }
    }

    private static boolean containsProvider(org.openrewrite.Recipe recipe, Class<? extends org.openrewrite.Recipe> type) {
        return type.isInstance(recipe) || recipe.getRecipeList().stream().anyMatch(child -> containsProvider(child, type));
    }

    @Test void missingDuplicateDriftAndUnrelatedOwners() throws Exception {
        var rows = rows();
        var recipe = MaskRecipeSupport.named();
        var before = MaskRecipeSupport.inputs(rows, Path.of("preimages"));
        assertThrows(IllegalStateException.class, () -> MaskRecipeSupport.run(recipe, List.of()));
        for (int i = 0; i < before.size(); i++) {
            var missing = new ArrayList<>(before);
            missing.remove(i);
            assertThrows(IllegalStateException.class, () -> MaskRecipeSupport.run(recipe, missing));
        }
        var duplicate = new ArrayList<>(before);
        duplicate.add(before.getFirst());
        assertThrows(IllegalStateException.class, () -> MaskRecipeSupport.run(recipe, duplicate));
        var text = new TreeMap<String, String>();
        before.forEach(file -> text.put(file.getSourcePath().toString(), file.printAll()));
        String first = text.firstKey();
        text.put(first, text.get(first) + "\n# unadmitted drift\n");
        var drift = MaskRecipeSupport.parse(text);
        assertThrows(IllegalStateException.class, () -> MaskRecipeSupport.run(recipe, drift));
        String unrelated = "m3/collections/qualification/unrelated.md";
        var withOther = new ArrayList<>(before);
        withOther.addAll(MaskRecipeSupport.parse(java.util.Map.of(unrelated, "preserved\n")));
        assertTrue(MaskRecipeSupport.run(recipe, withOther).stream().noneMatch(f -> unrelated.equals(f.getSourcePath().toString())));
    }

    @Test void reviewedTemplateAndProviderSeals() throws Exception {
        rows();
        assertEquals("67f7b81126e4736c7b61b444fa6545153a61e23c012975ca796fcee623ab6625",
                MaskRecipeSupport.hash(Files.readString(Path.of("proof/pinned/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java"))));
        assertEquals("fbf1ea9efad784afef5ac201ff7fc324551f77a099ebfe2f79b70c3a17c731ef",
                MaskRecipeSupport.hash(Files.readString(Path.of("proof/pinned/com/synexia/rewrite/M3HashPinnedTextSnapshotRecipe.java"))));
        String resource = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
        var row = MaskRecipeSupport.manifest("java", CRATE).getFirst();
        Path template = Path.of(getClass().getResource(resource + row[3]).toURI());
        byte[] original = Files.readAllBytes(template);
        try {
            Files.writeString(template, "class Unadmitted {}\n");
            assertThrows(IllegalStateException.class, () -> MaskRecipeSupport.run(MaskRecipeSupport.named(), List.of()));
        } finally { Files.write(template, original); }
    }
}
