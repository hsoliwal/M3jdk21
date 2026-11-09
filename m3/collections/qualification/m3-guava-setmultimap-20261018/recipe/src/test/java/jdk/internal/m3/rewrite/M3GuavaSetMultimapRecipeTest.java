/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Execute the existing hash-sealed OpenRewrite scheduler, not an alternate generator. */
final class M3GuavaSetMultimapRecipeTest {
    private static final String MAIN =
            "m3/collections/interop/src/main/java/jdk/m3/collections/interop/M3GuavaSetMultimap.java";
    private static final String TEST =
            "m3/collections/interop/src/test/java/jdk/m3/collections/interop/M3GuavaSetMultimapTest.java";
    private static final String HASH_MAIN = "4175df8f0a09c1a6ea5e10dfd16fb688096a1a9169699cad668210cea56f6454";
    private static final String HASH_TEST = "ecac9e556d7b02e000d0d60ad8035f95216a165b23b2f7824d39fe6eee2f816d";

    private static List<SourceFile> dummy() {
        return List.of(PlainText.builder().sourcePath(Path.of("unrelated.txt")).text("KEEP").build());
    }

    @Test void sourceSealAndFixedPoint() {
        ArrayList<Throwable> errors = new ArrayList<>();
        var result = new M3GuavaSetMultimapRecipe().run(
                new InMemoryLargeSourceSet(dummy()), new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        Map<String, SourceFile> generated = new HashMap<>();
        for (var change : result.getChangeset().getAllResults()) {
            assertNull(change.getBefore());
            SourceFile after = change.getAfter();
            assertNotNull(after);
            assertNull(generated.put(after.getSourcePath().toString().replace('\\', '/'), after));
        }
        assertEquals(2, generated.size());
        assertEquals(HASH_MAIN, M3GpuToolJoinManifest.sha256(generated.get(MAIN).printAll()));
        assertEquals(HASH_TEST, M3GpuToolJoinManifest.sha256(generated.get(TEST).printAll()));

        var all = new ArrayList<>(dummy());
        all.addAll(generated.values());
        ArrayList<Throwable> secondErrors = new ArrayList<>();
        var again = new M3GuavaSetMultimapRecipe().run(
                new InMemoryLargeSourceSet(all), new InMemoryExecutionContext(secondErrors::add));
        assertTrue(secondErrors.isEmpty(), secondErrors.toString());
        assertEquals(0, again.getChangeset().getAllResults().size());
    }

    @Test void occupiedDifferentSourceRefuses() {
        var inputs = new ArrayList<>(dummy());
        inputs.add(PlainText.builder().sourcePath(Path.of(MAIN)).text("drift").build());
        ArrayList<Throwable> errors = new ArrayList<>();
        boolean refused = false;
        try {
            new M3GuavaSetMultimapRecipe().run(
                    new InMemoryLargeSourceSet(inputs), new InMemoryExecutionContext(errors::add));
        } catch (RuntimeException expected) { refused = true; }
        assertTrue(refused || !errors.isEmpty(), "occupied/mismatched output must refuse");
    }

    @Test void namedOpenRewriteDiscovery() {
        var recipe = Environment.builder().scanClassLoader(getClass().getClassLoader()).build()
                .activateRecipes("jdk.internal.m3.GuavaSetMultimap20261018");
        assertTrue(recipe.validateAll().stream().allMatch(v -> v.isValid()));
    }
}
