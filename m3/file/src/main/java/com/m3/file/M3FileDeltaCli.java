// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Build-tool-neutral read-only TSV projection of M3FileDelta evidence.
 *
 * <p>Maven, Gradle and other tools must generate complete exact-hash manifests using
 * their own task input/processor/classpath model; this command does not discover
 * inputs, infer source-to-class edges, modify cache state or authorize a skipped task.
 * The output is a candidate comparison receipt, not a build lifecycle decision.
 */
public final class M3FileDeltaCli {
    private static final int MANIFEST_LIMIT = 32 * 1024 * 1024;
    private static final String HEADER = "M3FILEDELTA_MANIFEST_V1";

    private M3FileDeltaCli() { }

    /** Arguments: before.tsv after.tsv before-context-sha256 after-context-sha256. */
    public static void main(String[] args) throws IOException {
        Objects.requireNonNull(args, "args");
        if (args.length != 4) {
            throw new IllegalArgumentException("expected before.tsv after.tsv before-context-sha256 after-context-sha256");
        }
        execute(Path.of(args[0]), Path.of(args[1]), args[2], args[3], System.out);
    }

    static void execute(Path before, Path after, String beforeContext, String afterContext,
                        PrintStream output) throws IOException {
        Objects.requireNonNull(output, "output");
        var oldState = M3FileDelta.snapshot(read(before), beforeContext);
        var newState = M3FileDelta.snapshot(read(after), afterContext);
        var result = M3FileDelta.compare(oldState, newState);
        output.println("M3FILEDELTA_RECEIPT_V1");
        output.println("BEFORE\t" + result.previousRoot());
        output.println("AFTER\t" + result.currentRoot());
        output.println("CONTEXT_CHANGED\t" + result.contextChanged());
        output.println("IDENTICAL_EVIDENCE\t" + result.sameEvidence());
        for (var delta : result.changes()) {
            output.println(delta.kind() + "\t" + delta.path() + "\t" + delta.change()
                    + "\t" + delta.abi() + "\t" + nullField(delta.previousSha256())
                    + "\t" + nullField(delta.currentSha256()));
        }
    }

    private static String nullField(String value) { return value == null ? "-" : value; }

    private static List<M3FileDelta.Entry> read(Path manifest) throws IOException {
        Objects.requireNonNull(manifest, "manifest");
        byte[] bytes = M3File.of(manifest).readAllBytes(MANIFEST_LIMIT);
        String body;
        try {
            body = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (java.nio.charset.CharacterCodingException invalid) {
            throw new IOException("manifest is not strict UTF-8", invalid);
        }
        List<M3FileDelta.Entry> entries = new ArrayList<>();
        boolean headerSeen = false;
        for (String line : body.split("\n", -1)) {
            if (line.isEmpty()) continue;
            if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
            if (!headerSeen) {
                if (!HEADER.equals(line)) throw new IOException("wrong M3FileDelta manifest version");
                headerSeen = true;
                continue;
            }
            if (entries.size() == M3FileDelta.MAX_ENTRIES) {
                throw new IOException("manifest entry budget exceeded");
            }
            String[] cells = line.split("\t", -1);
            if (cells.length != 5) throw new IOException("invalid M3FileDelta manifest record");
            try {
                var kind = M3FileDelta.Kind.valueOf(cells[0]);
                long length = Long.parseLong(cells[2]);
                String abi = cells[4].equals("-") ? null : cells[4];
                entries.add(new M3FileDelta.Entry(kind, cells[1], length, cells[3], abi));
            } catch (IllegalArgumentException invalid) {
                throw new IOException("invalid M3FileDelta manifest record", invalid);
            }
        }
        if (!headerSeen) throw new IOException("empty M3FileDelta manifest");
        return List.copyOf(entries);
    }
}
