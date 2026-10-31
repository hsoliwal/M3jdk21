// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.modulepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Read-only CLI projection. Pattern/IOP role: Facade.
 * Arguments: comma-separated roots followed by archive paths/directories.
 * Emits selected descriptor/provenance facts as escaped TSV; never admits a pack.
 */
public final class M3ModuleInventory {
    private M3ModuleInventory() {}

    /** Inspect and validate using the same kernel used by OpenRewrite. */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) throw new IllegalArgumentException("Usage: roots archive-or-directory...");
        M3ModuleInspector inspector = new M3ModuleInspector();
        ArrayList<M3ModuleArtifact> artifacts = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            for (Path file : paths(Path.of(args[i]))) artifacts.add(inspector.inspect(file));
        }
        List<M3ModuleArtifact> selected = new M3ModuleGraph()
                .resolve(artifacts, Arrays.asList(args[0].split(",", -1)));
        System.out.println("module\tversion\tkind\tsha256\tpath\trequires\tuses\tprovides\tnative_entries");
        for (M3ModuleArtifact artifact : selected) {
            var descriptor = artifact.descriptor();
            System.out.println(String.join("\t", field(descriptor.name()),
                    field(descriptor.rawVersion().orElse("")), artifact.kind(), artifact.sha256(),
                    field(artifact.path().toString()),
                    field(descriptor.requires().stream().map(Object::toString).sorted().toList().toString()),
                    field(descriptor.uses().stream().sorted().toList().toString()),
                    field(descriptor.provides().stream().map(Object::toString).sorted().toList().toString()),
                    field(artifact.nativeEntries().toString())));
        }
    }

    /** Expand a directory in stable order; do not recurse or follow artifact dependencies by guessing. */
    public static List<Path> paths(Path input) throws IOException {
        if (!Files.isDirectory(input)) return List.of(input);
        try (var files = Files.list(input)) {
            return files.filter(file -> file.toString().endsWith(".jar")
                    || file.toString().endsWith(".jmod")).sorted().toList();
        }
    }

    private static String field(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\r", "\\r").replace("\n", "\\n");
    }
}
