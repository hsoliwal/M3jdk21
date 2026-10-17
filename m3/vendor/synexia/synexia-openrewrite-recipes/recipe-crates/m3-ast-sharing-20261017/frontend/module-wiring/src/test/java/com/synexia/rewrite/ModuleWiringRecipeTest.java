// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;

class ModuleWiringRecipeTest {
    @Test void actualModuleCandidatesFixedPointAndDriftRefusal() throws Exception {
        StringBuilder receipt = new StringBuilder("label\tresults\tfixed_point_results\n");
        for (String label : List.of("canonical", "receiver")) {
            var recipe = WiringSupport.named(label.equals("canonical") ? "ASTModuleWiringCanonical" : "ASTModuleWiringTarget");
            var rows = new ArrayList<>(WiringSupport.manifest("text", label));
            if (label.equals("receiver")) rows.addAll(WiringSupport.manifest("java", label));
            var before = WiringSupport.inputs(rows, Path.of("preimages", label));
            var changed = WiringSupport.run(recipe, before);
            assertEquals(rows.size(), changed.size());
            var after = WiringSupport.merge(before, changed);
            for (var file : after) {
                var row = rows.stream().filter(r -> r[0].equals(file.getSourcePath().toString())).findFirst().orElseThrow();
                assertEquals(row[2], WiringSupport.hash(file.printAll()));
                if (file.getSourcePath().toString().equals("synexia-mindex/compiler/src/main/resources/META-INF/services/com.sun.source.util.Plugin")) {
                    assertEquals("com.synexia.mindex.compiler.MIndexCompilerPlugin\n"
                            + "com.synexia.mindex.experiment.compiler.MIndexCompilerPlugin\n"
                            + "com.synexia.mindex.compiler.ast.M3ASTPlugin\n", file.printAll());
                    assertEquals("com.synexia.mindex.compiler.MIndexCompilerPlugin\n"
                            + "com.synexia.mindex.experiment.compiler.MIndexCompilerPlugin\n",
                            before.stream().filter(x -> x.getSourcePath().equals(file.getSourcePath())).findFirst().orElseThrow().printAll());
                }
                if (file.getSourcePath().toString().endsWith("pom.xml")) {
                    var factory = DocumentBuilderFactory.newInstance();
                    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                    factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(file.printAll().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                }
                WiringSupport.write(Path.of("target/candidate", label), file);
            }
            assertTrue(WiringSupport.run(recipe, after).isEmpty());
            Collections.reverse(after);
            assertTrue(WiringSupport.run(recipe, after).isEmpty());
            assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe, List.of()));
            var duplicate = new ArrayList<>(before);
            duplicate.add(before.getFirst());
            assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe, duplicate));
            var drift = WiringSupport.parse(before.getFirst().getSourcePath(), before.getFirst().printAll() + "\n<!-- source drift -->\n");
            assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe, List.of(drift)));
            WiringSupport.materialize(label, recipe, rows);
            receipt.append(label).append('\t').append(changed.size()).append("\t0\n");
        }
        Files.writeString(Path.of("target/SDK_RECEIPT.tsv"), receipt);
    }

    @Test void exactTaskOwnersPreserveOtherServicesAndRefuseManifestScopeDrift() throws Exception {
        List<String> unrelatedPaths = List.of(
                "m3/ports/compiletime/src/main/resources/META-INF/services/com.sun.source.util.PluginOther",
                "m3/ports/compiletime-other/src/main/resources/META-INF/services/com.sun.source.util.Plugin",
                "synexia-mindex/compiler/src/test/resources/META-INF/services/com.sun.source.util.Plugin",
                "synexia-mindex/other/src/main/resources/META-INF/services/com.sun.source.util.Plugin",
                "m3/ports/compiletime/src/main/resources/META-INF/services/java.lang.Runnable");
        for (String label : List.of("canonical", "receiver")) {
            String name = label.equals("canonical") ? "ASTModuleWiringCanonical" : "ASTModuleWiringTarget";
            var rows = new ArrayList<>(WiringSupport.manifest("text", label));
            if (label.equals("receiver")) rows.addAll(WiringSupport.manifest("java", label));
            var before = WiringSupport.inputs(rows, Path.of("preimages", label));
            for (String path : unrelatedPaths) before.add(WiringSupport.parse(Path.of(path), "unrelated.Provider\n"));
            var changed = WiringSupport.run(WiringSupport.named(name), before);
            assertEquals(rows.size(), changed.size());
            assertTrue(changed.stream().noneMatch(x -> unrelatedPaths.contains(x.getSourcePath().toString())));
            assertTrue(WiringSupport.run(WiringSupport.named(name), WiringSupport.merge(before, changed)).isEmpty());
        }
        String resource = "/com/synexia/rewrite/hash-pinned-text/m3-ast-wiring-receiver-20261017/manifest.tsv";
        Path manifest = Path.of(getClass().getResource(resource).toURI());
        String original = Files.readString(manifest);
        try {
            Files.writeString(manifest, original.replace("META-INF/services/com.sun.source.util.Plugin\t",
                    "META-INF/services/java.lang.Runnable\t"));
            assertThrows(IllegalStateException.class, () -> WiringSupport.named("ASTModuleWiringTarget"));
        } finally { Files.writeString(manifest, original); }
    }

    @Test void templateTamperRefuses() throws Exception {
        String root = "/com/synexia/rewrite/hash-pinned-text/m3-ast-wiring-receiver-20261017/";
        var rows = WiringSupport.manifest("text", "receiver");
        Path template = Path.of(getClass().getResource(root + rows.getFirst()[3]).toURI());
        byte[] original = Files.readAllBytes(template);
        try {
            Files.writeString(template, "<!-- tampered template -->\n");
            assertThrows(IllegalStateException.class, () -> WiringSupport.run(WiringSupport.named("ASTModuleWiringTarget"), List.of()));
        } finally { Files.write(template, original); }
    }
}
