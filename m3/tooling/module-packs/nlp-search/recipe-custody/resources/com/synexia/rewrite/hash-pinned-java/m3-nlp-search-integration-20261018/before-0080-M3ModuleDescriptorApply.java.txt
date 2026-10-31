// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.jmod;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainTextParser;

/**
 * Applies {@link M3ModuleDescriptorRecipe} to a repository root: parses the descriptor targets that already exist,
 * runs the recipe, writes generated files, then runs it again and requires zero changes (fixed point). Exit codes:
 * 0 applied or already applied, 3 drift refusal, 4 not a fixed point. Writes a receipt TSV
 * (target, before_sha256 | ABSENT, after_sha256, change).
 * <p>usage: M3ModuleDescriptorApply &lt;repo-root&gt; &lt;DESCRIPTOR_MANIFEST.tsv&gt; &lt;receipt.tsv&gt;
 */
public final class M3ModuleDescriptorApply {

    private M3ModuleDescriptorApply() {
    }

    public static void main(String[] args) {
        System.exit(apply(Path.of(args[0]), Path.of(args[1]), Path.of(args[2])));
    }

    public static int apply(Path root, Path manifest, Path receipt) {
        M3ModuleDescriptorRecipe recipe = new M3ModuleDescriptorRecipe(manifest.toString());
        List<Result> first = run(recipe, root, manifest);
        List<String> rows = new ArrayList<>(List.of("target\tbefore_sha256\tafter_sha256\tchange"));
        for (Result r : first) {
            if (r.getAfter() != null && r.getAfter().getMarkers().findFirst(Markup.Error.class).isPresent()) {
                write(receipt, String.join("\n", rows) + "\nREFUSED\t" + r.getAfter().getSourcePath() + "\n");
                return 3;
            }
        }
        for (Result r : first) {
            SourceFile after = r.getAfter();
            if (after == null) {
                continue;
            }
            String text = after.printAll();
            Path target = root.resolve(after.getSourcePath());
            write(target, text);
            rows.add(String.join("\t", M3ModuleDescriptorRecipe.slash(after.getSourcePath()),
                    r.getBefore() == null ? "ABSENT" : M3ModuleDescriptorRecipe.sha256(r.getBefore().printAll()),
                    M3ModuleDescriptorRecipe.sha256(text), r.getBefore() == null ? "ADDED" : "CHANGED"));
        }
        List<Result> second = run(recipe, root, manifest);
        boolean fixedPoint = second.isEmpty();
        rows.add("FIXED_POINT\t" + fixedPoint + "\tfirst_run_changes=" + first.size() + "\tsecond_run_changes=" + second.size());
        write(receipt, String.join("\n", rows) + "\n");
        return fixedPoint ? 0 : 4;
    }

    static List<Result> run(M3ModuleDescriptorRecipe recipe, Path root, Path manifest) {
        List<SourceFile> existing = new ArrayList<>();
        PlainTextParser parser = new PlainTextParser();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new IllegalStateException(t);
        });
        for (M3ModuleDescriptorRecipe.Row row : M3ModuleDescriptorRecipe.rows(manifest)) {
            Path file = root.resolve(row.target());
            if (Files.isRegularFile(file)) {
                parser.parse(ctx, M3ModuleDescriptorRecipe.read(file))
                        .map(sf -> sf.<SourceFile>withSourcePath(Path.of(row.target())))
                        .forEach(existing::add);
            }
        }
        return recipe.run(new InMemoryLargeSourceSet(existing), ctx).getChangeset().getAllResults();
    }

    static void write(Path path, String text) {
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
