// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.jmod;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.text.PlainText;

/**
 * Writes the generated module descriptors of the vendored libraries into the M3 refactor layer
 * (design section 6.2, provenance kind {@code m3-descriptor-for-jmod}). The upstream byte-for-byte modules are never
 * touched: each descriptor is a new file at its manifest target. Contract:
 * <ul>
 *   <li>input: {@code DESCRIPTOR_MANIFEST.tsv} (module, artifacts, unit_jars, target, sha256, generated); a generated
 *       file whose bytes do not match its pinned sha256 is refused before anything is written;</li>
 *   <li>absent target: generated with exactly the pinned bytes;</li>
 *   <li>present target with the pinned bytes: unchanged (second run = fixed point);</li>
 *   <li>present target with other bytes: never overwritten, marked with a {@link Markup.Error} (drift refusal).</li>
 * </ul>
 */
public final class M3ModuleDescriptorRecipe extends ScanningRecipe<M3ModuleDescriptorRecipe.Accumulator> {

    @Option(displayName = "Descriptor manifest", description = "Path of DESCRIPTOR_MANIFEST.tsv written by descriptor_gen.py.",
            example = "build/jmod/descriptors/DESCRIPTOR_MANIFEST.tsv")
    private final String manifest;

    public M3ModuleDescriptorRecipe(String manifest) {
        this.manifest = manifest;
    }

    public String getManifest() {
        return manifest;
    }

    @Override
    public String getDisplayName() {
        return "Write M3 module descriptors for JMOD packaging";
    }

    @Override
    public String getDescription() {
        return "Adds hash-pinned module-info.java descriptors for vendored libraries without one to the M3 refactor layer.";
    }

    /** Pinned descriptors by repository target path, plus what the scan found. */
    public static final class Accumulator {
        final Map<String, String> wanted = new LinkedHashMap<>();
        final Set<String> present = new TreeSet<>();
        final Set<String> drifted = new TreeSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        Accumulator acc = new Accumulator();
        for (Row row : rows(Path.of(manifest))) {
            String text = read(Path.of(row.generated()));
            if (!sha256(text).equals(row.sha256())) {
                throw new IllegalStateException("generated descriptor does not match its pin: " + row.generated());
            }
            acc.wanted.put(row.target(), text);
        }
        return acc;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile sf) {
                    String path = slash(sf.getSourcePath());
                    String want = acc.wanted.get(path);
                    if (want != null) {
                        acc.present.add(path);
                        if (!want.equals(sf.printAll())) {
                            acc.drifted.add(path);
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        List<SourceFile> created = new ArrayList<>();
        acc.wanted.forEach((target, text) -> {
            if (!acc.present.contains(target)) {
                created.add(PlainText.builder().id(Tree.randomId()).sourcePath(Path.of(target)).markers(Markers.EMPTY)
                        .charsetName(StandardCharsets.UTF_8.name()).text(text).build());
            }
        });
        return created;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile sf && acc.drifted.contains(slash(sf.getSourcePath()))) {
                    return Markup.error(sf, new IllegalStateException("descriptor drift: refusing to overwrite " + sf.getSourcePath()));
                }
                return tree;
            }
        };
    }

    /** One manifest row. */
    public record Row(String module, String artifacts, String unitJars, String target, String sha256, String generated) {
    }

    public static List<Row> rows(Path manifest) {
        try {
            if (Files.size(manifest) > 512 * 1024) {
                throw new IllegalArgumentException("descriptor manifest exceeds 512 KiB");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<String> lines = List.of(read(manifest).split("\n"));
        List<Row> rows = new ArrayList<>();
        Set<String> targets = new TreeSet<>();
        for (String line : lines.subList(1, lines.size())) {
            if (!line.isBlank()) {
                String[] c = line.split("\t", -1);
                if (c.length != 6) {
                    throw new IllegalArgumentException("descriptor manifest requires six columns");
                }
                descriptorTarget(c[3]);
                if (!targets.add(c[3])) {
                    throw new IllegalArgumentException("duplicate descriptor target: " + c[3]);
                }
                rows.add(new Row(c[0], c[1], c[2], c[3], c[4], c[5].strip()));
                if (rows.size() > 128) {
                    throw new IllegalArgumentException("descriptor manifest exceeds 128 rows");
                }
            }
        }
        return rows;
    }

    static Path descriptorTarget(String target) {
        String[] parts = target.split("/", -1);
        if (target.indexOf('\\') >= 0 || target.indexOf(':') >= 0 || Path.of(target).isAbsolute()
                || parts.length != 6 || !parts[0].equals("m3") || !parts[1].equals("vendor")
                || !parts[2].equals("third_party-m3") || !parts[3].equals("descriptors")
                || parts[4].length() > 128 || !parts[4].matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")
                || !parts[5].equals("module-info.java")) {
            throw new IllegalArgumentException("unsafe descriptor target: " + target);
        }
        return Path.of(target);
    }

    static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String slash(Path path) {
        return path.toString().replace('\\', '/');
    }
}
