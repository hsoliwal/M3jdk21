// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical inventory for every admitted text-tool surface in this module.
 *
 * <p>Discovery never grants execution authority. Effectful tools remain in their explicit
 * owners and cannot be invoked by {@link M3TextToolExecutor}.</p>
 */
public final class M3TextToolCatalog {
    public enum InputShape { NONE, ONE, TWO, ONE_OR_MORE }
    public enum OutputShape { FRAME, FRAMES, ARGUMENT_BATCHES, EFFECT }
    public enum Owner {
        M3_TEXT_OPERATORS,
        TEXT_TOOLS,
        STRUCTURED_TEXT_TOOLS,
        JSON_LINES_TOOLS,
        FILE_TOOLS
    }
    public enum Effect {
        PURE,
        FILESYSTEM_READ,
        FILESYSTEM_WRITE,
        PROCESS_EXECUTION,
        NATIVE_FILESYSTEM
    }

    /** Immutable semantic skeleton for one tool atom. */
    public record Spec(String id, InputShape input, OutputShape output, Owner owner,
                       Effect effect, String contract) {
        public Spec {
            id = Ids.id(id);
            Objects.requireNonNull(input, "input");
            Objects.requireNonNull(output, "output");
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(effect, "effect");
            Canonical.validUnicode(contract);
            if (contract.isBlank() || contract.length() > 2048) {
                throw new IllegalArgumentException("invalid tool contract");
            }
        }

        public String sha256() {
            return new Canonical("m3-text-tool-spec-v1")
                    .text(id).text(input.name()).text(output.name())
                    .text(owner.name()).text(effect.name()).text(contract).finish();
        }
    }

    private static final List<Spec> SPECS = List.of(
            pure("cat", InputShape.ONE_OR_MORE, OutputShape.FRAME, Owner.M3_TEXT_OPERATORS,
                    "Exact framed concatenation; unterminated boundaries fuse without normalization."),
            pure("comm", InputShape.TWO, OutputShape.FRAMES, Owner.TEXT_TOOLS,
                    "Sorted multiset comparison returning left-only, common, and right-only frames."),
            pure("csplit", InputShape.ONE, OutputShape.FRAMES, Owner.TEXT_TOOLS,
                    "Split before Java-regex matching lines; empty partitions are omitted."),
            pure("csv-cut", InputShape.ONE, OutputShape.FRAME, Owner.STRUCTURED_TEXT_TOOLS,
                    "RFC-4180-style CSV projection by zero-based columns with canonical LF output."),
            pure("cut", InputShape.ONE, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Zero-based half-open Unicode-scalar range, clipped independently per line."),
            pure("diff", InputShape.TWO, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Exact full-file LF diff; no fuzzy matching and no-change yields an empty frame."),
            pure("env-get", InputShape.ONE, OutputShape.FRAME, Owner.STRUCTURED_TEXT_TOOLS,
                    "Strict dotenv key lookup without interpolation or evaluation."),
            pure("fold", InputShape.ONE, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Break by Unicode-scalar count while preserving each original final ending."),
            pure("grep-literal", InputShape.ONE, OutputShape.FRAME, Owner.M3_TEXT_OPERATORS,
                    "Prepared case-sensitive UTF-16 literal line selection with optional inversion."),
            pure("grep-regex", InputShape.ONE, OutputShape.FRAME, Owner.M3_TEXT_OPERATORS,
                    "Java-regex line selection with optional inversion; no hard regex CPU claim."),
            pure("head", InputShape.ONE, OutputShape.FRAME, Owner.M3_TEXT_OPERATORS,
                    "First bounded number of line records."),
            pure("ini-get", InputShape.ONE, OutputShape.FRAME, Owner.STRUCTURED_TEXT_TOOLS,
                    "Strict INI section/key lookup without interpolation or duplicate overwrite."),
            pure("join", InputShape.TWO, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Stable inner equijoin on the first tab-separated field."),
            pure("jsonl-pointer", InputShape.ONE, OutputShape.FRAME, Owner.JSON_LINES_TOOLS,
                    "Strict RFC-6901 extraction from every JSON Lines record, preserving source value text."),
            pure("nl", InputShape.ONE, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Prefix lines with an exact long sequence and tab."),
            pure("paste", InputShape.ONE_OR_MORE, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Row-wise many-frame paste with an explicit separator and canonical LF rows."),
            pure("patch", InputShape.TWO, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Apply only the exact full-file diff form; source drift and malformed hunks refuse."),
            pure("shuf", InputShape.ONE, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Deterministic Fisher-Yates line permutation from an explicit seed."),
            pure("sort", InputShape.ONE, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Stable locale-independent UTF-16 lexical line order."),
            pure("split", InputShape.ONE, OutputShape.FRAMES, Owner.TEXT_TOOLS,
                    "In-memory fixed-line partitions with no implicit filesystem writes."),
            pure("tail", InputShape.ONE, OutputShape.FRAME, Owner.M3_TEXT_OPERATORS,
                    "Last bounded number of line records."),
            pure("tr", InputShape.ONE, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Literal Unicode-scalar translation; empty target deletes."),
            pure("tsort", InputShape.ONE, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Lexically deterministic topological order with cycle refusal."),
            pure("tsv-cut", InputShape.ONE, OutputShape.FRAME, Owner.STRUCTURED_TEXT_TOOLS,
                    "Quoted TSV projection by zero-based columns with canonical LF output."),
            pure("uniq", InputShape.ONE, OutputShape.FRAME, Owner.TEXT_TOOLS,
                    "Collapse adjacent equal line content, retaining the first ending."),
            pure("uniq-count", InputShape.ONE, OutputShape.FRAME, Owner.M3_TEXT_OPERATORS,
                    "Collapse adjacent equal content as count<TAB>content, retaining the first ending."),
            pure("wc", InputShape.ONE, OutputShape.FRAME, Owner.M3_TEXT_OPERATORS,
                    "Count LF, whitespace-delimited words, UTF-8 bytes, or Unicode scalars."),
            pure("xargs-plan", InputShape.ONE, OutputShape.ARGUMENT_BATCHES, Owner.M3_TEXT_OPERATORS,
                    "Create immutable literal argv batches; never spawn a shell or process."),
            effect("find", InputShape.NONE, Owner.FILE_TOOLS, Effect.FILESYSTEM_READ,
                    "Deterministic bounded path discovery under an explicit host root."),
            effect("mkfifo", InputShape.NONE, Owner.FILE_TOOLS, Effect.NATIVE_FILESYSTEM,
                    "Explicit POSIX FIFO creation under host authority."),
            effect("tee", InputShape.ONE, Owner.FILE_TOOLS, Effect.FILESYSTEM_WRITE,
                    "Atomic staged UTF-8 publication to an explicit host path."),
            effect("xargs-execute", InputShape.ONE, Owner.TEXT_TOOLS, Effect.PROCESS_EXECUTION,
                    "Host-supplied callback execution over admitted literal argument batches.")
    );

    private static final Map<String, Spec> BY_ID = index();
    private static final String SHA256 = fingerprint();

    private M3TextToolCatalog() { }

    public static List<Spec> specs() {
        return SPECS;
    }

    public static Spec require(String id) {
        Spec spec = BY_ID.get(Ids.id(id));
        if (spec == null) throw new IllegalArgumentException("unknown text tool: " + id);
        return spec;
    }

    /** Deterministic bounded fuzzy discovery; a hit is not execution authority. */
    public static List<Spec> lookup(String query, int limit) {
        Objects.requireNonNull(query, "query");
        if (query.length() > 256 || limit < 0 || limit > SPECS.size()) {
            throw new IllegalArgumentException("invalid lookup");
        }
        String key = query.toLowerCase(Locale.ROOT);
        return SPECS.stream()
                .sorted(Comparator.comparingInt((Spec spec) -> distance(key, spec.id()))
                        .thenComparing(Spec::id))
                .limit(limit)
                .toList();
    }

    public static String sha256() {
        return SHA256;
    }

    private static Spec pure(String id, InputShape input, OutputShape output,
                             Owner owner, String contract) {
        return new Spec(id, input, output, owner, Effect.PURE, contract);
    }

    private static Spec effect(String id, InputShape input, Owner owner,
                               Effect effect, String contract) {
        return new Spec(id, input, OutputShape.EFFECT, owner, effect, contract);
    }

    private static Map<String, Spec> index() {
        Map<String, Spec> result = new LinkedHashMap<>();
        for (Spec spec : SPECS) {
            if (result.put(spec.id(), spec) != null) {
                throw new ExceptionInInitializerError("duplicate text tool: " + spec.id());
            }
        }
        return Map.copyOf(result);
    }

    private static String fingerprint() {
        Canonical hash = new Canonical("m3-text-tool-catalog-v1").number(SPECS.size());
        SPECS.stream().sorted(Comparator.comparing(Spec::id))
                .forEach(spec -> hash.text(spec.sha256()));
        return hash.finish();
    }

    private static int distance(String left, String right) {
        int[] row = new int[right.length() + 1];
        for (int column = 0; column < row.length; column++) row[column] = column;
        for (int line = 1; line <= left.length(); line++) {
            int diagonal = row[0];
            row[0] = line;
            for (int column = 1; column <= right.length(); column++) {
                int old = row[column];
                row[column] = Math.min(Math.min(row[column] + 1, row[column - 1] + 1),
                        diagonal + (left.charAt(line - 1) == right.charAt(column - 1) ? 0 : 1));
                diagonal = old;
            }
        }
        return row[right.length()];
    }
}
