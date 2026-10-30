// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import com.m3.mop.M3PatternRegistry.State;
import com.m3.mop.M3PatternRegistry.Verdict;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Publishes one atomize run, identically for the javac Plugin and the CLI: FILES.tsv, ATOMS.tsv,
 * PATTERNS.tsv, a sha256sum-compatible MANIFEST.sha256, SUMMARY.txt and RECEIPT.jsonl, then
 * optionally appends new exemplars to the pattern registry.
 */
//FLLDCIM: MOP — one publication for Plugin and CLI; manifest guards downstream drift
public final class M3AtomizeRun {
    public static final List<String> TABLES = List.of("FILES.tsv", "ATOMS.tsv", "PATTERNS.tsv");
    public static final String MANIFEST = "MANIFEST.sha256";

    private M3AtomizeRun() { }

    /** Output directory, optional registry and whether new exemplars are registered. */
    public record Options(Path out, Optional<Path> registry, boolean register, String goal) {
        public Options {
            Stream.of(out, registry, goal).forEach(Objects::requireNonNull);
        }
    }

    /** Counts of one run; {@link #line()} is the machine-readable summary. */
    public record Summary(int files, int atoms, Map<State, Long> states, Map<String, Integer> limits,
            int registered, String manifestSha256) {
        public String line() {
            return "M3_MOP_ATOMIZE files=" + files + " atoms=" + atoms + " " + pairs(states) + " "
                    + pairs(limits) + " registered=" + registered;
        }

        Map<String, Object> fields() {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("files", files);
            fields.put("atoms", atoms);
            states.forEach((state, count) -> fields.put(state.name(), count));
            limits.forEach(fields::put);
            fields.put("registered", registered);
            fields.put("manifestSha256", manifestSha256);
            return fields;
        }

        private static String pairs(Map<?, ? extends Number> counts) {
            return counts.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(" "));
        }
    }

    public static Summary publish(List<M3Atomizer.Unit> units, Options options) throws IOException {
        List<M3Atomizer.Unit> sorted = units.stream()
                .sorted(Comparator.comparing(M3Atomizer.Unit::path)).toList();
        List<M3Atom> atoms = sorted.stream().flatMap(unit -> unit.atoms().stream()).toList();
        M3PatternRegistry registry = options.registry().isPresent()
                ? M3PatternRegistry.read(options.registry().get()) : M3PatternRegistry.empty();
        List<Verdict> verdicts = registry.classify(atoms);
        tables(options.out(), sorted, atoms, verdicts);
        String manifest = manifest(options.out());
        int registered = options.register() && options.registry().isPresent()
                ? M3PatternRegistry.register(options.registry().get(), verdicts) : 0;
        return receipts(options, new Summary(sorted.size(), atoms.size(),
                M3PatternRegistry.states(verdicts), limits(atoms), registered, manifest));
    }

    private static Summary receipts(Options options, Summary summary) throws IOException {
        M3MopReceipt.write(options.out().resolve("SUMMARY.txt"), List.of(summary.line()));
        M3MopReceipt.write(options.out().resolve("RECEIPT.jsonl"), List.of(
                M3MopReceipt.line(options.goal(), "atomize", M3MopReceipt.PASS, summary.fields())));
        return summary;
    }

    /** Refuses with {@code driftCode} when a table no longer matches MANIFEST.sha256. */
    public static void verifyManifest(Path directory, String driftCode) throws IOException {
        List<String> expected = Files.readAllLines(directory.resolve(MANIFEST), StandardCharsets.UTF_8);
        String actual = manifestLines(directory).stream().collect(Collectors.joining("\n"));
        if (!String.join("\n", expected).equals(actual)) {
            throw new IOException(driftCode + " " + directory.resolve(MANIFEST));
        }
    }

    /** Path to sha256 of every atomized file, read from a verified atomize directory. */
    public static Map<String, String> files(Path directory, String driftCode) throws IOException {
        verifyManifest(directory, driftCode);
        return Files.readAllLines(directory.resolve("FILES.tsv"), StandardCharsets.UTF_8).stream()
                .skip(1).map(line -> line.split("\t", -1))
                .collect(Collectors.toMap(cells -> cells[0], cells -> cells[1], (a, b) -> a,
                        java.util.TreeMap::new));
    }

    private static void tables(Path out, List<M3Atomizer.Unit> units, List<M3Atom> atoms,
            List<Verdict> verdicts) throws IOException {
        M3MopReceipt.write(out.resolve("FILES.tsv"), rows(M3Atomizer.FILES_HEADER, units,
                M3Atomizer.Unit::row));
        M3MopReceipt.write(out.resolve("ATOMS.tsv"), rows(M3Atom.HEADER, atoms, M3Atom::row));
        M3MopReceipt.write(out.resolve("PATTERNS.tsv"), rows(M3PatternRegistry.PATTERNS_HEADER,
                verdicts, Verdict::row));
    }

    private static <T> List<String> rows(String header, List<T> values, Function<T, String> row) {
        return Stream.concat(Stream.of(header), values.stream().map(row)).toList();
    }

    private static String manifest(Path out) throws IOException {
        Path written = M3MopReceipt.write(out.resolve(MANIFEST), manifestLines(out));
        return M3MopReceipt.sha256(Files.readAllBytes(written));
    }

    private static List<String> manifestLines(Path directory) throws IOException {
        try {
            return TABLES.stream().map(name -> sha(directory.resolve(name)) + "  " + name).toList();
        } catch (java.io.UncheckedIOException failure) {
            throw failure.getCause();
        }
    }

    private static String sha(Path file) {
        try {
            return M3MopReceipt.sha256(Files.readAllBytes(file));
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    private static Map<String, Integer> limits(List<M3Atom> atoms) {
        return M3RefactorGate.LIMITS.stream().collect(Collectors.toMap(Function.identity(),
                kind -> atoms.stream().mapToInt(atom -> atom.limit(kind)).sum(), Integer::sum,
                LinkedHashMap::new));
    }
}
