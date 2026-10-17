// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.repair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.xml.ChangeTagValue;
import org.openrewrite.xml.XmlParser;

/** One reusable Maven/OpenRewrite XML recipe with fail-closed SHA256 source custody. */
public final class M3CollectionDependencyRepair {
    private M3CollectionDependencyRepair() { }

    public static final String MODULE = "m3/collections/pom.xml";
    public static final String RECIPE = "m3/collections/recipe/pom.xml";

    public static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public static String resource(Path crate, String suffix) throws IOException {
        return Files.readString(crate.resolve("resources").resolve(suffix),
                StandardCharsets.UTF_8);
    }

    private static ChangeTagValue visitor(String path) {
        String before = path.equals(MODULE) ? "5.10.2-m3-source-1" : "8.17.1-m3-source-1";
        String after = path.equals(MODULE) ? "5.10.2" : "8.17.1";
        return new ChangeTagValue("/project/dependencies/dependency/version", before, after);
    }

    private static String emitOne(String path, String before, String after) {
        var context = new InMemoryExecutionContext(failure -> {
            throw new IllegalStateException(failure);
        });
        List<Parser.Input> sources = List.of(Parser.Input.fromString(Path.of(path), before));
        List<SourceFile> parsed = XmlParser.builder().build().parseInputs(sources, null, context).toList();
        if (parsed.size() != 1 || !parsed.getFirst().getClass().getName().contains("Xml")) {
            throw new IllegalStateException("XML parser did not produce a typed source for " + path);
        }
        var run = visitor(path).run(new InMemoryLargeSourceSet(parsed), context, 1);
        var results = run.getChangeset().getAllResults();
        if (results.size() != 1 || results.getFirst().getAfter() == null) {
            throw new IllegalStateException("expected exactly one OpenRewrite XML edit: " + path);
        }
        String emitted = results.getFirst().getAfter().printAll();
        if (!emitted.equals(after)) {
            throw new IllegalStateException("OpenRewrite produced unexpected XML postimage: " + path);
        }
        return emitted;
    }

    /**
     * Emit only the reviewed two XML source candidates; never mutate the checkout.
     * Source text must match the original exact SHA or the expected fixed point.
     */
    public static Map<String, String> emit(
            Path crate, Map<String, String> inputs) throws IOException {
        if (!inputs.keySet().equals(java.util.Set.of(MODULE, RECIPE))) {
            throw new IllegalStateException("missing, extra or duplicate Maven XML source");
        }
        Map<String, String> changed = new LinkedHashMap<>();
        for (String path : List.of(MODULE, RECIPE)) {
            String prefix = path.equals(MODULE) ? "collections" : "recipe";
            String expectedBefore = resource(crate, "pre/" + prefix + "-pom.xml.txt");
            String expectedAfter = resource(crate, prefix + "-pom.xml.txt");
            String input = inputs.get(path);
            if (input == null || (!sha256(input).equals(sha256(expectedBefore))
                    && !sha256(input).equals(sha256(expectedAfter)))) {
                throw new IllegalStateException("M3 Maven source SHA drift: " + path);
            }
            if (!input.equals(expectedBefore) && !input.equals(expectedAfter)) {
                throw new IllegalStateException("SHA collision or non-UTF8 source drift: " + path);
            }
            if (input.equals(expectedBefore)) {
                changed.put(path, emitOne(path, input, expectedAfter));
            }
        }
        return changed;
    }
}
