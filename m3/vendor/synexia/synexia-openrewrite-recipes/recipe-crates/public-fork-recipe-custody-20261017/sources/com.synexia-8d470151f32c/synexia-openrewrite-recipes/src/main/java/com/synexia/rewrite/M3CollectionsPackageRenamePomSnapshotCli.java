// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
package com.synexia.rewrite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

/** Thin module-local adapter to the incumbent exact POM snapshot recipe. */
public final class M3CollectionsPackageRenamePomSnapshotCli {
    private M3CollectionsPackageRenamePomSnapshotCli() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4 || (args.length == 4 && !"--apply".equals(args[3]))) {
            throw new IllegalArgumentException("ROOT CRATE RECEIPT [--apply]");
        }
        Path root = Path.of(args[0]).toRealPath();
        Path target = root.resolve("pom.xml");
        if (Files.isSymbolicLink(target)) throw new IllegalArgumentException("Symlink POM");
        String before = Files.readString(target);
        String crate = args[1];
        SourceFile source = parse(before);
        String after = execute(crate, List.of(source), before);
        if (!execute(crate, List.of(parse(after)), after).equals(after)) throw new IllegalStateException("POM fixedpoint");
        refuse(crate, List.of(parse(after + "\n<!-- drift -->\n")), "drift");
        refuse(crate, List.of(), "missing");
        refuse(crate, List.of(parse(after), parse(after).withId(Tree.randomId())), "duplicate");
        if (!Files.readString(target).equals(before)) throw new IllegalStateException("Concurrent POM drift");
        Path receipt = Path.of(args[2]);
        Files.createDirectories(receipt.toAbsolutePath().getParent());
        Files.writeString(receipt, "before_sha256\tafter_sha256\tfixedpoint\trefusals\n"
                + M3HashPinnedPomSnapshotRecipe.sha256(before) + "\t"
                + M3HashPinnedPomSnapshotRecipe.sha256(after) + "\tPASS\tRECIPE_DRIFT_HOST_MISSING_DUPLICATE_PASS\n");
        Files.writeString(receipt.resolveSibling("pom-candidate.xml"), after);
        if (args.length == 4) Files.writeString(target, after);
        System.out.println("pom_snapshot fixedpoint=PASS refusal_controls=3 apply=" + (args.length == 4));
    }

    private static SourceFile parse(String text) {
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        var source = XmlParser.builder().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of("pom.xml"), text)), null, context).toList();
        if (source.size() != 1 || !(source.getFirst() instanceof Xml.Document)
                || !source.getFirst().printAll().equals(text)) throw new IllegalStateException("Lossless POM parse");
        return source.getFirst();
    }

    private static String execute(String crate, List<SourceFile> sources, String unchanged) {
        // An empty LargeSourceSet never invokes the incumbent recipe's visitor.
        // The file host therefore admits exactly one module-local XML POM itself.
        if (sources.size() != 1 || !(sources.getFirst() instanceof Xml.Document)
                || !sources.getFirst().getSourcePath().equals(Path.of("pom.xml"))) {
            throw new IllegalArgumentException("Exactly one root XML POM input is required");
        }
        List<Throwable> errors = new ArrayList<>();
        var context = new InMemoryExecutionContext(errors::add);
        var result = new M3HashPinnedPomSnapshotRecipe(crate)
                .run(new InMemoryLargeSourceSet(sources), context).getChangeset().getAllResults();
        if (!errors.isEmpty()) throw new IllegalStateException("POM snapshot recipe error", errors.getFirst());
        if (result.isEmpty()) return unchanged;
        if (result.size() != 1 || result.getFirst().getAfter() == null) throw new IllegalStateException("Unexpected POM outputs");
        return result.getFirst().getAfter().printAll();
    }

    private static void refuse(String crate, List<SourceFile> sources, String kind) {
        try { execute(crate, sources, ""); }
        catch (RuntimeException expected) { return; }
        throw new IllegalStateException("Missing POM refusal: " + kind);
    }
}
