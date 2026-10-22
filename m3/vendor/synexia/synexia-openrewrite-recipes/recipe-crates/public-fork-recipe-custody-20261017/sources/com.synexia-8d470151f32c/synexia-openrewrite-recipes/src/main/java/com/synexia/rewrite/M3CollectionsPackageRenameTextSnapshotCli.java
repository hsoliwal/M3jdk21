// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
package com.synexia.rewrite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** File adapter for existing, exact hash-pinned workflow/document metadata snapshots. */
public final class M3CollectionsPackageRenameTextSnapshotCli {
    private M3CollectionsPackageRenameTextSnapshotCli() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4 || (args.length == 4 && !"--apply".equals(args[3]))) {
            throw new IllegalArgumentException("ROOT CRATE RECEIPT [--apply]");
        }
        Path root = Path.of(args[0]).toRealPath();
        String crate = args[1];
        var authority = new M3HashPinnedTextSnapshotRecipe(crate);
        Map<String, String> before = new LinkedHashMap<>();
        List<SourceFile> input = new ArrayList<>();
        for (String path : authority.targetPaths()) {
            Path file = safe(root, path);
            if (!Files.isRegularFile(file)) throw new IllegalArgumentException("Required metadata absent: " + path);
            String text = Files.readString(file);
            before.put(path, text);
            input.add(parse(path, text));
        }
        Map<String, String> after = new LinkedHashMap<>(before);
        after.putAll(execute(crate, input));
        List<SourceFile> complete = after.entrySet().stream().map(e -> parse(e.getKey(), e.getValue())).toList();
        if (!execute(crate, complete).isEmpty()) throw new IllegalStateException("Text fixedpoint");
        List<SourceFile> drift = new ArrayList<>(complete);
        SourceFile first = drift.getFirst();
        drift.set(0, parse(first.getSourcePath().toString(), first.printAll() + "\nDRIFT\n"));
        refuse(crate, drift, "drift");
        refuse(crate, complete.subList(1, complete.size()), "missing");
        List<SourceFile> duplicate = new ArrayList<>(complete);
        duplicate.add(first.withId(Tree.randomId()));
        refuse(crate, duplicate, "duplicate");
        for (String path : before.keySet()) {
            if (!Files.readString(safe(root, path)).equals(before.get(path))) throw new IllegalStateException("Concurrent metadata drift");
        }
        Path receipt = Path.of(args[2]);
        Files.createDirectories(receipt.toAbsolutePath().getParent());
        StringBuilder rows = new StringBuilder("path\tbefore_sha256\tafter_sha256\tfixedpoint\trefusals\n");
        for (String path : before.keySet()) {
            rows.append(path).append('\t').append(M3CollectionsPackageRenameMetadataRecipe.sha256(before.get(path)))
                    .append('\t').append(M3CollectionsPackageRenameMetadataRecipe.sha256(after.get(path)))
                    .append("\tPASS\tDRIFT_MISSING_DUPLICATE_PASS\n");
        }
        Files.writeString(receipt, rows.toString());
        if (args.length == 4) for (String path : before.keySet()) Files.writeString(safe(root, path), after.get(path));
        System.out.println("text_snapshot targets=" + after.size() + " fixedpoint=PASS refusal_controls=3 apply=" + (args.length == 4));
    }

    private static Path safe(Path root, String path) throws Exception {
        Path file = root.resolve(path).normalize();
        if (!file.startsWith(root) || !file.toRealPath().startsWith(root)) throw new IllegalArgumentException("Metadata escapes root");
        for (Path current = file; current != null && current.startsWith(root); current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new IllegalArgumentException("Symlink metadata");
        }
        return file;
    }

    private static SourceFile parse(String path, String text) {
        return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
    }

    private static Map<String, String> execute(String crate, List<SourceFile> input) {
        List<Throwable> errors = new ArrayList<>();
        var context = new InMemoryExecutionContext(errors::add);
        var results = new M3HashPinnedTextSnapshotRecipe(crate)
                .run(new InMemoryLargeSourceSet(input), context).getChangeset().getAllResults();
        if (!errors.isEmpty()) throw new IllegalStateException("Text snapshot recipe error", errors.getFirst());
        Map<String, String> outputs = new LinkedHashMap<>();
        for (var result : results) {
            if (result.getAfter() == null) throw new IllegalStateException("Unexpected metadata deletion");
            outputs.put(result.getAfter().getSourcePath().toString(), result.getAfter().printAll());
        }
        return outputs;
    }

    private static void refuse(String crate, List<SourceFile> input, String kind) {
        try { execute(crate, input); }
        catch (RuntimeException expected) { return; }
        throw new IllegalStateException("Missing text refusal: " + kind);
    }
}
