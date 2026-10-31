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
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Actual original Maven/OpenRewrite source-sealed recipe, not an ad hoc patcher. */
final class M3FiveSdkOwnerRepairRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/m3-five-sdk-owner-repair-20261018/";
    private static final String SDK = "m3/collections/src/main/java/com/m3/collections/M3CollectionSdk.java";
    private static final String PROOF = "m3/collections/src/test/java/com/m3/collections/M3SdkOwnerFactorySelfTest.java";
    private static final String ADAPTER = "m3/collections/src/test/java/com/m3/collections/M3SdkOwnerFactoryTest.java";

    private static String resource(String name) throws Exception {
        try (InputStream input = M3FiveSdkOwnerRepairRecipeTest.class.getResourceAsStream(ROOT + name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static SourceFile source(String path, String text) {
        return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
    }
    private static Map<String, SourceFile> run(List<SourceFile> input) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var changes = new M3FiveSdkOwnerRepairRecipe().run(
                new InMemoryLargeSourceSet(input), context, 1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), () -> "recipe errors: " + errors);
        Map<String, SourceFile> output = new TreeMap<>();
        for (var change : changes) {
            SourceFile after = change.getAfter();
            assertNotNull(after);
            assertTrue(output.put(after.getSourcePath().toString().replace('\\', '/'), after) == null);
        }
        return output;
    }
    @Test void actualChangeAndFixedPoint() throws Exception {
        SourceFile sdk = source(SDK, resource("M3CollectionSdk.java.before"));
        var emitted = run(List.of(sdk));
        assertEquals(3, emitted.size());
        assertEquals(resource("M3CollectionSdk.java.after"), emitted.get(SDK).printAll());
        assertEquals(resource("M3SdkOwnerFactorySelfTest.java.txt"), emitted.get(PROOF).printAll());
        assertEquals(resource("M3SdkOwnerFactoryTest.java.txt"), emitted.get(ADAPTER).printAll());
        assertEquals(1, emitted.get(SDK).printAll().split("readOnlyFilteredSortedSet\\(", -1).length - 1);
        assertEquals(1, emitted.get(SDK).printAll().split("readOnlyFilteredSortedMap\\(", -1).length - 1);
        assertTrue(run(List.copyOf(emitted.values())).isEmpty());
    }
    @Test void fourPartialInstallationsInBothOrders() throws Exception {
        var all = run(List.of(source(SDK, resource("M3CollectionSdk.java.before"))));
        for (int mask = 0; mask < 4; mask++) {
            for (boolean reversed : List.of(false, true)) {
                var input = new ArrayList<SourceFile>();
                input.add((mask & 1) == 0
                        ? source(SDK, resource("M3CollectionSdk.java.before")) : all.get(SDK));
                if ((mask & 2) != 0) input.add(all.get(PROOF));
                if (reversed) Collections.reverse(input);
                input.add(source("unrelated.txt", "preserve\\n"));
                var changes = run(input);
                input.addAll(changes.values());
                assertTrue(run(input).isEmpty());
            }
        }
    }
    @Test void refusesDriftMissingOwnerAndDuplicate() throws Exception {
        assertThrows(RuntimeException.class, () -> run(List.of()));
        assertThrows(RuntimeException.class, () -> run(List.of(source(SDK, "drift\\n"))));
        SourceFile original = source(SDK, resource("M3CollectionSdk.java.before"));
        assertThrows(RuntimeException.class, () -> run(List.of(original, original)));
        var all = run(List.of(original));
        assertThrows(RuntimeException.class, () -> run(List.of(
                original, source(PROOF, "unexpected test\\n"))));
        assertEquals(3, all.size());
    }
}
