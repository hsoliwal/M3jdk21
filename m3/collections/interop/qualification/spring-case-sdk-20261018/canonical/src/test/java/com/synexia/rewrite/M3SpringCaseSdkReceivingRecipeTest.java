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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Actual existing Maven/OpenRewrite Java LST and plain-text recipe replay. */
final class M3SpringCaseSdkReceivingRecipeTest {
    private static final String JAVA_ROOT = "/com/synexia/rewrite/hash-pinned-java/m3-spring-case-sdk-20261018/";
    private static final String TEXT_ROOT = "/com/synexia/rewrite/hash-pinned-text/m3-spring-case-sdk-metadata-20261018/";
    private record Target(String path, String before, String after, String template) { }

    private static String resource(String path) throws Exception {
        try (InputStream in = M3SpringCaseSdkReceivingRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing recipe resource: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String sha256(String contents) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(contents.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<Target> targets(String root) throws Exception {
        var out = new ArrayList<Target>();
        String previous = "";
        for (String line : resource(root + "manifest.tsv").lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(4, cells.length);
            assertTrue(cells[0].compareTo(previous) > 0, "manifest order");
            assertEquals(cells[2], sha256(resource(root + cells[3])));
            out.add(new Target(cells[0], cells[1], cells[2], cells[3]));
            previous = cells[0];
        }
        return out;
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(cause -> {
            throw new IllegalStateException("OpenRewrite Java/text candidate", cause);
        });
    }

    private static Map<String,SourceFile> executeJava(List<SourceFile> installed) {
        var result = new M3HashPinnedJavaSnapshotRecipe("m3-spring-case-sdk-20261018")
                .run(new InMemoryLargeSourceSet(installed), context(), 1);
        var changed = new TreeMap<String,SourceFile>();
        for (var item : result.getChangeset().getAllResults()) {
            SourceFile after = item.getAfter();
            assertNotNull(after);
            assertTrue(changed.put(after.getSourcePath().toString().replace('\\', '/'), after) == null);
        }
        return changed;
    }

    private static Map<String,SourceFile> executeText(List<SourceFile> installed) {
        var result = new M3HashPinnedTextSnapshotRecipe("m3-spring-case-sdk-metadata-20261018")
                .run(new InMemoryLargeSourceSet(installed), context(), 1);
        var changed = new TreeMap<String,SourceFile>();
        for (var item : result.getChangeset().getAllResults()) {
            SourceFile after = item.getAfter();
            assertNotNull(after);
            assertTrue(changed.put(after.getSourcePath().toString().replace('\\','/'), after) == null);
        }
        return changed;
    }

    private static List<SourceFile> oldJava() throws Exception {
        var input = new ArrayList<SourceFile>();
        for (Target row : targets(JAVA_ROOT)) {
            if (row.before.equals("ABSENT")) continue;
            String source = resource(JAVA_ROOT + "before/" + row.template);
            assertEquals(row.before, sha256(source));
            input.addAll(JavaParser.fromJavaVersion().build().parseInputs(
                    List.of(Parser.Input.fromString(Path.of(row.path), source)), null, context()).toList());
        }
        assertEquals(3,input.size());
        return input;
    }

    @Test void javaLstsGeneratedExactlyAndFixedPoint() throws Exception {
        List<SourceFile> source = oldJava();
        var first = executeJava(source);
        assertEquals(4, first.size());
        for (var row : targets(JAVA_ROOT)) {
            assertNotNull(first.get(row.path));
            assertEquals(resource(JAVA_ROOT + row.template), first.get(row.path).printAll());
            assertEquals(row.after, sha256(first.get(row.path).printAll()));
        }
        assertTrue(executeJava(List.copyOf(first.values())).isEmpty());
    }

    @Test void partialJavaStatesDoNotOverwriteOrDuplicate() throws Exception {
        var before = oldJava();
        var full = executeJava(before);
        for (int presentMask = 0; presentMask < 2; presentMask++) {
            var partial = new ArrayList<SourceFile>(before);
            if (presentMask != 0) {
                String newPath = "m3/collections/interop/src/test/java/com/m3/collections/interop/M3SpringCaseInsensitiveSdkFactoryTest.java";
                partial.add(full.get(newPath));
            }
            var emitted = executeJava(partial);
            assertEquals(4 - presentMask, emitted.size());
        }
        var duplicate = new ArrayList<>(before);
        duplicate.add(before.getFirst());
        assertThrows(RuntimeException.class, () -> executeJava(duplicate));
        var missing = new ArrayList<>(before);
        missing.removeFirst();
        assertThrows(RuntimeException.class, () -> executeJava(missing));
    }

    @Test void exactTextMetadataAndFixedPoint() throws Exception {
        var before = new ArrayList<SourceFile>();
        for (var row : targets(TEXT_ROOT)) {
            String original = resource(TEXT_ROOT + "before/" + row.template);
            assertEquals(row.before, sha256(original));
            before.add(PlainText.builder().sourcePath(Path.of(row.path)).text(original).build());
        }
        var changed = executeText(before);
        assertEquals(2, changed.size());
        for (var row : targets(TEXT_ROOT)) {
            assertEquals(row.after, sha256(changed.get(row.path).printAll()));
        }
        assertTrue(executeText(List.copyOf(changed.values())).isEmpty());
        assertThrows(RuntimeException.class,
                () -> executeText(List.of(PlainText.builder()
                        .sourcePath(Path.of(targets(TEXT_ROOT).getFirst().path))
                        .text("unexpected metadata drift").build(), before.getLast())));
    }
}
