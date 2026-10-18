// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.jmod;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
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
    private static final long OUTPUT_BUDGET = 4 * 1024 * 1024;
    private static final long RESERVE = 8L * 1024 * 1024 * 1024;

    private M3ModuleDescriptorApply() {
    }

    public static void main(String[] args) {
        System.exit(apply(Path.of(args[0]), Path.of(args[1]), Path.of(args[2])));
    }

    public static int apply(Path root, Path manifest, Path receipt) {
        root = root.toAbsolutePath().normalize();
        receipt = receipt.toAbsolutePath().normalize();
        preflight(root, manifest, receipt);
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
            Path target = root.resolve(M3ModuleDescriptorRecipe.descriptorTarget(
                    M3ModuleDescriptorRecipe.slash(after.getSourcePath())));
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
            noReparse(path.toAbsolutePath().normalize());
            checkSpace(path, OUTPUT_BUDGET);
            Files.createDirectories(path.toAbsolutePath().getParent());
            noReparse(path.toAbsolutePath().normalize());
            Files.writeString(path, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void preflight(Path root, Path manifest, Path receipt) {
        noReparse(root);
        noReparse(receipt);
        if (!Files.isDirectory(root) || receipt.startsWith(root) || Files.exists(receipt, LinkOption.NOFOLLOW_LINKS)
                || !receipt.getFileName().toString().endsWith(".tsv")) {
            throw new IllegalArgumentException("existing repository root and a fresh external TSV receipt are required");
        }
        for (Path part = receipt.getParent(); part != null; part = part.getParent()) {
            if (Files.exists(part.resolve(".git"), LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("receipt cannot overwrite a source/Git tree");
            }
        }
        long bytes = 0;
        long allocated = 0;
        Set<Path> directories = new HashSet<>();
        try {
            for (M3ModuleDescriptorRecipe.Row row : M3ModuleDescriptorRecipe.rows(manifest)) {
                Path target = root.resolve(M3ModuleDescriptorRecipe.descriptorTarget(row.target())).normalize();
                if (!target.startsWith(root)) {
                    throw new IllegalArgumentException("descriptor escaped the repository");
                }
                noReparse(target);
                long size = Files.size(Path.of(row.generated()));
                bytes = Math.addExact(bytes, size);
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                    if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || Files.size(target) > 512 * 1024) {
                        throw new IllegalArgumentException("descriptor target must be a bounded regular file");
                    }
                } else {
                    allocated = Math.addExact(allocated, fileAllocation(size, allocationUnit(target)));
                    missingDirectories(target, directories);
                }
            }
            allocated = Math.addExact(allocated, fileAllocation(64 * 1024, allocationUnit(receipt)));
            missingDirectories(receipt, directories);
            for (Path directory : directories) {
                allocated = Math.addExact(allocated, allocationUnit(directory));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (bytes > 512 * 1024) {
            throw new IllegalArgumentException("descriptor payload exceeds 512 KiB within the 4 MiB output allowance");
        }
        if (allocated > OUTPUT_BUDGET) {
            throw new IllegalArgumentException("descriptor allocation exceeds the 4 MiB output allowance");
        }
        checkSpace(root, OUTPUT_BUDGET);
        checkSpace(receipt, OUTPUT_BUDGET);
    }

    static long fileAllocation(long bytes, long unit) {
        if (bytes < 0 || unit <= 0) {
            throw new IllegalArgumentException("invalid allocation measurement");
        }
        return Math.multiplyExact(Math.max(1, Math.addExact(bytes, unit - 1) / unit), unit);
    }

    private static void missingDirectories(Path file, Set<Path> directories) {
        for (Path parent = file.getParent(); parent != null && !Files.exists(parent, LinkOption.NOFOLLOW_LINKS);
                parent = parent.getParent()) {
            directories.add(parent);
        }
    }

    private static long allocationUnit(Path path) throws IOException {
        Path existing = path.toAbsolutePath().normalize();
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        long unit = Files.getFileStore(existing).getBlockSize();
        if (unit <= 0) {
            throw new IllegalArgumentException("cannot measure output allocation unit");
        }
        return unit;
    }

    private static void noReparse(Path path) {
        try {
            for (Path part = path; part != null; part = part.getParent()) {
                if (Files.exists(part, LinkOption.NOFOLLOW_LINKS)
                        && (Files.isSymbolicLink(part) || !part.toRealPath().equals(part.toRealPath(LinkOption.NOFOLLOW_LINKS)))) {
                    throw new IllegalArgumentException("reparse path refused: " + part);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void checkSpace(Path path, long budget) {
        Path existing = path.toAbsolutePath().normalize();
        if (System.getProperty("os.name").startsWith("Windows")
                && !existing.getRoot().toString().equalsIgnoreCase("K:\\")) {
            throw new IllegalArgumentException("Windows output must use the explicit K storage lane");
        }
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        try {
            if (Files.getFileStore(existing).getUsableSpace() - budget < RESERVE) {
                throw new IllegalArgumentException("8 GiB reserve would be crossed");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
