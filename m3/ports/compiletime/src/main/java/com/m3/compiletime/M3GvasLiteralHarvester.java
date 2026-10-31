/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.compiletime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

/**
 * Reuses the existing class-file/ASM constant inventory, not another compiler scanner.
 * The output image and offsets are preparation artifacts; no class file is rewritten here.
 *
 * <p>Example: {@code --out prepared --jars jars.txt [--jdk] [--max-bytes 67108864]}.
 * The complete image is rejected on any parse/analyzer failure or capacity overflow.</p>
 */
public final class M3GvasLiteralHarvester {
    private M3GvasLiteralHarvester() { }

    public static void main(String[] args) throws IOException {
        String requestedDestination = M3CompileTimeStringInventory.arg(args, "--out", null);
        if (requestedDestination == null) throw new IllegalArgumentException("required --out DIRECTORY");
        String jarFile = M3CompileTimeStringInventory.arg(args, "--jars", null);
        boolean jdk = List.of(args).contains("--jdk");
        if (jarFile == null && !jdk) throw new IllegalArgumentException("requires --jdk or --jars");
        int max = Integer.parseInt(M3CompileTimeStringInventory.arg(args,
                "--max-bytes", Integer.toString(M3GvasLiteralImageWriter.MAX_DEFAULT_BYTES)));
        Path out = Path.of(requestedDestination);
        if (Files.exists(out)) throw new IOException("output already exists: " + out);
        M3InventoryState state = new M3InventoryState();
        M3CompileTimeStringInventory scanner = new M3CompileTimeStringInventory(state, null, false);
        List<String> jars = jarFile == null ? List.of() : Files.readAllLines(Path.of(jarFile)).stream()
                .map(String::trim).filter(s -> !s.isEmpty()).sorted().toList();
        if (jdk) scanner.scanJdk();
        if (!jars.isEmpty()) scanner.scanJars(jars);
        for (var corpus : state.totals) {
            for (var metric : corpus.entrySet()) {
                if ((metric.getKey().contains("failure") || metric.getKey().contains("exception"))
                        && metric.getValue() != 0L) {
                    throw new IOException("compiler inventory incomplete: " + metric);
                }
            }
        }
        M3GvasLiteralImageWriter.Result image =
                M3GvasLiteralImageWriter.build(state.ldc.keySet(), max);
        // Detect dual-key collisions even though runtime lookup additionally verifies every UTF-16 unit.
        TreeMap<M3ContentKey, String> keys = new TreeMap<>((a, b) -> {
            int c = Long.compareUnsigned(a.h1(), b.h1());
            return c != 0 ? c : Long.compareUnsigned(a.h2(), b.h2());
        });
        StringBuilder offsets = new StringBuilder("ordinal\tutf16_length\th1\th2\tbyte_offset\tjava_hash\n");
        for (int ordinal = 0; ordinal < image.rows().size(); ordinal++) {
            M3GvasLiteralImageWriter.LiteralRow row = image.rows().get(ordinal);
            M3ContentKey key = M3ContentKey.of(row.text(), 0);
            String old = keys.putIfAbsent(key, row.text());
            if (old != null && !old.equals(row.text())) {
                throw new IOException("M3 compiletime content-key collision");
            }
            offsets.append(ordinal).append('\t').append(row.text().length()).append('\t')
                    .append(Long.toUnsignedString(key.h1(), 16)).append('\t')
                    .append(Long.toUnsignedString(key.h2(), 16)).append('\t')
                    .append(row.byteOffset()).append('\t').append(row.hash()).append('\n');
        }
        StringBuilder origins = new StringBuilder("source\tsha256\tidentity\n");
        if (jdk) origins.append("jrt\t-\t").append(System.getProperty("java.runtime.version")).append('\n');
        for (String jar : jars) {
            Path file = Path.of(jar);
            origins.append("jar\t").append(M3ConstantRegexHarvester.sha256Hex(file)).append('\t')
                    .append(file.getFileName()).append('\n');
        }
        // Publish the complete image + offsets + receipt as one no-clobber directory move; never
        // expose an already-live inode to compilation. Aborted staging is removed.
        Path destination = out.toAbsolutePath().normalize();
        Path staging = Files.createTempDirectory(destination.getParent(), ".m3-gvas-staging-");
        try {
            M3GvasLiteralImageWriter.writeNew(staging.resolve("literals.m3gvas"), image);
            Files.writeString(staging.resolve("COMPILETIME_LITERAL_OFFSETS.tsv"), offsets);
            Files.writeString(staging.resolve("INPUT_SET_MANIFEST.tsv"), origins);
            long classes = state.totals.stream().mapToLong(m -> m.getOrDefault("all\tclasses", 0L)).sum();
            Files.writeString(staging.resolve("PRECOMPUTE_RECEIPT.tsv"),
                    "metric\tvalue\nimage_sha256\t" + image.sha256()
                    + "\nimage_bytes\t" + image.bytes().length
                    + "\nunique_literals\t" + image.rows().size()
                    + "\nclasses_scanned\t" + classes
                    + "\nproducer\tM3GvasLiteralHarvester\n");
            Files.move(staging, destination); // no REPLACE_EXISTING: refuse a concurrent destination
        } finally {
            if (Files.exists(staging)) {
                try (var files = Files.list(staging)) {
                    for (Path file : files.toList()) Files.delete(file);
                }
                Files.delete(staging);
            }
        }
        M3CompileTimeStringInventory.log("M3_GVAS_LITERAL_HARVEST rows=" + image.rows().size()
                + " bytes=" + image.bytes().length + " sha256=" + image.sha256());
    }
}
