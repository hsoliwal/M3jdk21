// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.packs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XmlParser;

/**
 * Exact pre/postimage registration of the new subreactor at m3/pom.xml.
 * Pattern/IOP role: BuildAdapter. Authority: MULTI_MODULE, not FILE.
 */
public final class M3RegisterModulePacksRecipe extends Recipe {
    @Override public String getDisplayName() { return "Register the M3 module-pack reactor"; }
    @Override public String getDescription() {
        return "Add the reviewed module-pack reactor only at the exact inspected M3 POM preimage, refusing drift.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                if (!source.getSourcePath().toString().replace('\\', '/').equals("m3/pom.xml")) return tree;
                String before = resource("reactor-before.xml");
                String after = resource("reactor-after.xml");
                if (source.printAll().equals(after)) return tree;
                if (!source.printAll().equals(before)) throw new IllegalStateException("M3_REACTOR_PREIMAGE_DRIFT");
                SourceFile parsed = XmlParser.builder().build().parse(context, after).findFirst().orElseThrow();
                parsed = parsed.withId(source.getId());
                parsed = parsed.withSourcePath(source.getSourcePath());
                parsed = parsed.withMarkers(source.getMarkers());
                parsed = parsed.withFileAttributes(source.getFileAttributes());
                return parsed;
            }
        };
    }

    static String resource(String name) {
        try (var input = M3RegisterModulePacksRecipe.class.getResourceAsStream(
                "/com/m3/rewrite/packs/" + name)) {
            if (input == null) throw new IllegalStateException("Missing recipe resource: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
