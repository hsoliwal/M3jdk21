// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command line over the same code as the javac Plugin.
 *
 * <pre>
 * atomize --root=DIR --out=DIR [--registry=FILE] [--register] PATH...
 * plan    --atomize=DIR --out=DIR
 * gate    --path=P --before=FILE --after=FILE [--again=FILE] [--baseline-sha=HEX] --out=DIR
 *         [--recipe=ID] [--memory-root=DIR] [--memory=RELATIVE_CANDIDATES_TSV]
 * chain   --root=DIR --proposal=DIR [--again=DIR] --out=DIR [--write] [--registry=FILE]
 *         [--recipe=ID] [--memory-root=DIR] [--memory=RELATIVE_CANDIDATES_TSV]
 * registry-verify FILE
 * </pre>
 * Exit 0 PASS, 1 REFUSED or FAIL, 2 usage, 3 HOLD. Output is UTF-8 regardless of platform charset.
 */
//FLLDCIM: MOP — CLI and javac Plugin share M3Atomizer, M3PatternRegistry and M3RefactorGate
public final class M3Mop {
    private static final String MEMORY = "RECIPE_MEMORY_CANDIDATES.tsv";
    private static final String RECIPE = "com.m3.mop.UnnamedRecipe";

    private M3Mop() { }

    private record Args(String verb, Map<String, String> options, List<String> positional) {
        String get(String key) {
            String value = options.get(key);
            if (value == null) throw new IllegalArgumentException("missing --" + key);
            return value;
        }

        Optional<Path> path(String key) { return Optional.ofNullable(options.get(key)).map(Path::of); }

        Path memoryRoot() { return path("memory-root").orElse(Path.of(get("out"))); }
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        int status = run(args, out);
        if (status != 0) Runtime.getRuntime().exit(status);
    }

    /** Runs one verb, prints machine-readable lines to {@code out}, returns the exit status. */
    public static int run(String[] args, PrintStream out) {
        try {
            return dispatch(parse(args), out);
        } catch (IllegalArgumentException misuse) {
            return usage(out, misuse.getMessage());
        } catch (IOException | UncheckedIOException failure) {
            out.println("M3_MOP_FAIL " + M3Atom.clean(String.valueOf(failure.getMessage())));
            return 1;
        }
    }

    private static int dispatch(Args parsed, PrintStream out) throws IOException {
        return switch (parsed.verb()) {
            case "atomize" -> atomize(parsed, out);
            case "plan" -> plan(parsed, out);
            case "gate" -> gate(parsed, out);
            case "chain" -> chain(parsed, out);
            case "registry-verify" -> registry(parsed, out);
            default -> usage(out, "unknown verb " + parsed.verb());
        };
    }

    private static int atomize(Args args, PrintStream out) throws IOException {
        Path root = Path.of(args.get("root"));
        List<Path> files = sources(args.positional().isEmpty() ? List.of(args.get("root")) : args.positional());
        M3AtomizeRun.Summary summary = M3AtomizeRun.publish(M3Atomizer.parse(root, files),
                new M3AtomizeRun.Options(Path.of(args.get("out")), args.path("registry"),
                        args.options().containsKey("register"), "cli"));
        out.println(summary.line());
        return 0;
    }

    private static int plan(Args args, PrintStream out) throws IOException {
        out.println(M3PatternPlan.write(Path.of(args.get("atomize")), Path.of(args.get("out"))).line());
        return 0;
    }

    private static int gate(Args args, PrintStream out) throws IOException {
        M3RefactorGate.Decision decision = M3RefactorGate.check(new M3RefactorGate.Input(args.get("path"),
                read(Path.of(args.get("before"))), read(Path.of(args.get("after"))),
                args.path("again").map(M3Mop::read), Optional.ofNullable(args.options().get("baseline-sha"))));
        Path dir = Path.of(args.get("out"));
        M3MopReceipt.write(dir.resolve("RECEIPT.jsonl"), List.of(M3RefactorGate.receipt("gate", decision)));
        int memory = M3RefactorGate.remember(args.memoryRoot(), args.options().getOrDefault("memory", MEMORY),
                args.options().getOrDefault("recipe", RECIPE), List.of(decision));
        out.println("M3_MOP_GATE " + decision.status() + " " + decision.verdict() + " path=" + decision.path()
                + " memoryRows=" + memory + " detail=" + M3Atom.clean(decision.detail()));
        return decision.admitted() ? 0 : decision.refused() ? 1 : 3;
    }

    private static int chain(Args args, PrintStream out) throws IOException {
        M3MopChain.Result result = M3MopChain.run(new M3MopChain.Options(Path.of(args.get("root")),
                Path.of(args.get("proposal")), args.path("again"), Path.of(args.get("out")),
                args.options().containsKey("write"), args.path("registry"),
                args.options().getOrDefault("recipe", RECIPE), args.memoryRoot(),
                args.options().getOrDefault("memory", MEMORY)));
        result.stages().forEach(stage -> out.println("M3_MOP_CHAIN " + stage.name() + " " + stage.status()
                + " " + M3Atom.clean(stage.detail())));
        return result.passed() ? 0 : result.refusedOrFailed() ? 1 : 3;
    }

    private static int registry(Args args, PrintStream out) throws IOException {
        Path file = Path.of(args.positional().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("registry-verify FILE")));
        out.println("M3_MOP_REGISTRY PASS rows=" + M3PatternRegistry.read(file).rows().size()
                + " schema=" + M3PatternRegistry.SCHEMA);
        return 0;
    }

    private static Args parse(String[] args) {
        if (args.length == 0) throw new IllegalArgumentException("no verb");
        List<String> rest = Stream.of(args).skip(1).toList();
        Map<String, String> options = rest.stream().filter(arg -> arg.startsWith("--"))
                .collect(Collectors.toMap(arg -> arg.substring(2).split("=", 2)[0],
                        arg -> arg.contains("=") ? arg.split("=", 2)[1] : "true"));
        Set<String> known = Set.of("root", "out", "registry", "register", "atomize", "path", "before",
                "after", "again", "baseline-sha", "recipe", "memory-root", "memory", "proposal", "write");
        if (!known.containsAll(options.keySet())) throw new IllegalArgumentException("unknown option " + options.keySet());
        return new Args(args[0], options, rest.stream().filter(arg -> !arg.startsWith("--")).toList());
    }

    private static List<Path> sources(List<String> paths) throws IOException {
        try {
            return paths.stream().map(Path::of).flatMap(path -> Files.isDirectory(path)
                    ? walk(path) : Stream.of(path)).distinct().sorted().toList();
        } catch (UncheckedIOException failure) {
            throw failure.getCause();
        }
    }

    private static Stream<Path> walk(Path directory) {
        try {
            return M3MopChain.javaFiles(directory, Set.of()).stream();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static int usage(PrintStream out, String reason) {
        out.println("M3_MOP_USAGE " + M3Atom.clean(String.valueOf(reason))
                + " verbs=atomize|plan|gate|chain|registry-verify");
        return 2;
    }
}
