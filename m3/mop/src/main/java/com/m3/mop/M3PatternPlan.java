// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Turns an atomize run's PATTERNS.tsv into a versioned, scope-aware pattern plan.
 *
 * <p>The input tables are checked against the atomize MANIFEST.sha256 first; a mismatch is
 * PATTERN_INPUT_DRIFT and no plan is written. Each (pattern, state) group gets the smallest scope
 * containing all of its paths (FILE, PACKAGE, MODULE, PROJECT) and the next mechanical action.
 * Ported from Synexia M3PatternPlan (#13966).</p>
 */
//FLLDCIM: MOP — pattern plan consumes atomize evidence; refuses drifted input
public final class M3PatternPlan {
    public static final String FILE_NAME = "PATTERN_PLAN.tsv";
    public static final String HEADER = "ordinal\tpatternSchema\tpatternId\tpatternVersion\tstate"
            + "\tatoms\tpaths\trequiredScope\tnextAction\tpromotionAuthority";
    private static final Map<String, String> ACTIONS = Map.of(
            M3PatternRegistry.State.KNOWN_PATTERN.name(), "REUSE_EXISTING_RECIPE",
            M3PatternRegistry.State.UNPROVEN_LEAF.name(), "PROVE_FILE_GATE",
            M3PatternRegistry.State.UNKNOWN_PATTERN.name(), "KEEP_LEAF_OR_MINT_PATTERN");

    private M3PatternPlan() { }

    /** Plan outcome: rows written and groups per required scope. */
    public record Result(int rows, Map<String, Long> scopes) {
        public String line() {
            return "M3_MOP_PATTERN_PLAN rows=" + rows + " " + scopes.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(" "));
        }
    }

    public static Result write(Path atomizeDirectory, Path out) throws IOException {
        List<List<String[]>> ordered = groups(atomizeDirectory);
        List<String> rows = IntStream.range(0, ordered.size())
                .mapToObj(index -> row(index + 1, ordered.get(index))).toList();
        M3MopReceipt.write(out.resolve(FILE_NAME), Stream.concat(Stream.of(HEADER), rows.stream()).toList());
        Map<String, Long> scopes = rows.stream().map(row -> row.split("\t")[7]).collect(
                Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()));
        Result result = new Result(rows.size(), scopes);
        M3MopReceipt.write(out.resolve("RECEIPT.jsonl"), List.of(M3MopReceipt.line("pattern-plan",
                "pattern-plan", M3MopReceipt.PASS, fields(result, out))));
        return result;
    }

    /** PATTERNS.tsv rows grouped by (patternId, state), after the manifest drift check. */
    private static List<List<String[]>> groups(Path atomizeDirectory) throws IOException {
        M3AtomizeRun.verifyManifest(atomizeDirectory, "PATTERN_INPUT_DRIFT");
        Map<String, List<String[]>> groups = Files.readAllLines(atomizeDirectory.resolve("PATTERNS.tsv"),
                StandardCharsets.UTF_8).stream().skip(1).map(line -> line.split("\t", -1))
                .collect(Collectors.groupingBy(cells -> cells[6] + '\t' + cells[8], TreeMap::new,
                        Collectors.toList()));
        return List.copyOf(groups.values());
    }

    /** Smallest scope containing every path: FILE, PACKAGE, MODULE or PROJECT. */
    static String scope(Set<String> paths) {
        if (paths.size() == 1) return "FILE";
        if (same(paths, M3PatternPlan::directory)) return "PACKAGE";
        return same(paths, M3PatternPlan::module) ? "MODULE" : "PROJECT";
    }

    private static String row(int ordinal, List<String[]> group) {
        String[] first = group.get(0);
        Set<String> paths = group.stream().map(cells -> cells[1]).collect(Collectors.toSet());
        int version = group.stream().mapToInt(cells -> Integer.parseInt(cells[7])).max().orElse(0);
        return String.join("\t", Integer.toString(ordinal), first[5], first[6], Integer.toString(version),
                first[8], Integer.toString(group.size()), Integer.toString(paths.size()), scope(paths),
                ACTIONS.getOrDefault(first[8], "UNKNOWN_STATE"), "false");
    }

    private static Map<String, Object> fields(Result result, Path out) throws IOException {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("rows", result.rows());
        result.scopes().forEach(fields::put);
        fields.put("planSha256", M3MopReceipt.sha256(Files.readAllBytes(out.resolve(FILE_NAME))));
        return fields;
    }

    private static boolean same(Set<String> paths, Function<String, String> key) {
        return paths.stream().map(key).distinct().count() == 1;
    }

    private static String directory(String path) {
        return path.substring(0, Math.max(0, path.lastIndexOf('/')));
    }

    private static String module(String path) {
        int marker = path.indexOf("/src/");
        return marker < 0 ? "." : path.substring(0, marker);
    }
}
