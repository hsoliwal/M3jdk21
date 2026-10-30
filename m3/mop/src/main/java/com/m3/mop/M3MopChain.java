// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import com.m3.mop.M3RefactorGate.Decision;
import com.m3.mop.M3RefactorGate.Verdict;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * One MOP run: atomize, then pattern plan, then gated refactor, then verify.
 *
 * <p>Stage 1 atomizes every Java file under {@code root} (the drift baseline); stage 2 plans the
 * versioned patterns; stage 3 gates every proposed file (a recipe's output mirrored under
 * {@code proposal}, its second application under {@code again}) and promotes all or nothing, only
 * with {@code write}, after re-reading every target; stage 4 re-atomizes and requires that exactly
 * the promoted files changed (none in a dry run) and that a second pass changes nothing. The recipe
 * engine stays external (OpenRewrite, JDT or any tool); the chain is its admission harness.
 * Ported from Synexia M3MopChainMojo (#13966) without the Maven and JDT dependencies.</p>
 */
//FLLDCIM: MOP — chain composes atomize, patternize and refactor gate; no replacement engine
public final class M3MopChain {
    private static final String GOAL = "m3-mop-chain";

    private M3MopChain() { }

    /** Chain inputs; {@code memoryLedger} is relative to {@code memoryRoot}. */
    public record Options(Path root, Path proposal, Optional<Path> again, Path out, boolean write,
            Optional<Path> registry, String recipe, Path memoryRoot, String memoryLedger) {
        public Options {
            Stream.of(root, proposal, again, out, registry, recipe, memoryRoot, memoryLedger)
                    .forEach(Objects::requireNonNull);
        }
    }

    public record Stage(String name, String status, String detail) { }

    public record Result(List<Stage> stages, List<Decision> decisions, List<String> written,
            int memoryRows) {
        public boolean passed() {
            return stages.stream().allMatch(stage -> stage.status().equals(M3MopReceipt.PASS));
        }

        public boolean refusedOrFailed() {
            return stages.stream().anyMatch(stage -> stage.status().equals(M3MopReceipt.FAIL)
                    || stage.status().equals(M3MopReceipt.REFUSED));
        }
    }

    public static Result run(Options options) throws IOException {
        Path out = options.out().toAbsolutePath().normalize();
        List<Stage> stages = new ArrayList<>();
        M3AtomizeRun.Summary atoms = atomize(options, out.resolve("atomize"));
        stages.add(new Stage("atomize", M3MopReceipt.PASS, atoms.line()));
        M3PatternPlan.Result plan = M3PatternPlan.write(out.resolve("atomize"), out.resolve("pattern"));
        stages.add(new Stage("pattern-plan", M3MopReceipt.PASS, plan.line()));
        Map<String, String> baseline = M3AtomizeRun.files(out.resolve("atomize"), "BASELINE_INPUT_DRIFT");
        Result refactor = refactor(options, baseline);
        stages.addAll(refactor.stages());
        stages.add(verify(options, out, baseline, refactor.written()));
        return publish(out, new Result(List.copyOf(stages), refactor.decisions(), refactor.written(),
                refactor.memoryRows()), options.write());
    }

    /** Stage 3: gate every proposal (dry), then promote all or nothing. */
    private static Result refactor(Options options, Map<String, String> baseline) throws IOException {
        List<String> proposals = javaFiles(options.proposal(), Set.of()).stream()
                .map(file -> relative(options.proposal(), file)).toList();
        List<Decision> decisions = proposals.stream().map(path -> gate(options, baseline, path)).toList();
        boolean promote = options.write() && !decisions.isEmpty() && decisions.stream().allMatch(Decision::admitted);
        List<Decision> late = promote ? lateDrift(options, decisions) : List.of();
        List<String> written = promote && late.isEmpty() ? promote(options, proposals) : List.of();
        List<Decision> all = Stream.concat(decisions.stream(), late.stream()).toList();
        int memory = M3RefactorGate.remember(options.memoryRoot(), options.memoryLedger(), options.recipe(), all);
        return new Result(List.of(refactorStage(all, written, memory)), all, written, memory);
    }

    private static M3AtomizeRun.Summary atomize(Options options, Path out) throws IOException {
        Path root = options.root().toAbsolutePath().normalize();
        List<Path> sources = javaFiles(root, excluded(options));
        return M3AtomizeRun.publish(M3Atomizer.parse(root, sources),
                new M3AtomizeRun.Options(out, options.registry(), false, GOAL));
    }

    private static Decision gate(Options options, Map<String, String> baseline, String path) {
        Path target = options.root().resolve(path);
        String after = read(options.proposal().resolve(path));
        if (!baseline.containsKey(path) || !Files.isRegularFile(target)) {
            return new Decision(path, Verdict.REFUSED_NOT_IN_BASELINE, "no atomize identity", "ABSENT",
                    M3MopReceipt.sha256(after));
        }
        Optional<String> again = options.again().map(dir -> dir.resolve(path))
                .filter(Files::isRegularFile).map(M3MopChain::read);
        return M3RefactorGate.check(new M3RefactorGate.Input(path, read(target), after, again,
                Optional.of(baseline.get(path))));
    }

    /** All or nothing: re-read every target before the first write. */
    private static List<Decision> lateDrift(Options options, List<Decision> decisions) {
        return decisions.stream()
                .filter(d -> !M3MopReceipt.sha256(read(options.root().resolve(d.path()))).equals(d.beforeSha256()))
                .map(d -> new Decision(d.path(), Verdict.REFUSED_DRIFT, "changed on disk after prepare",
                        d.beforeSha256(), d.afterSha256())).toList();
    }

    private static List<String> promote(Options options, List<String> proposals) {
        proposals.forEach(path -> write(options.root().resolve(path), read(options.proposal().resolve(path))));
        return proposals;
    }

    private static Stage refactorStage(List<Decision> decisions, List<String> written, int memory) {
        String status = decisions.stream().anyMatch(Decision::refused) ? M3MopReceipt.REFUSED
                : decisions.stream().allMatch(Decision::admitted) ? M3MopReceipt.PASS : M3MopReceipt.HOLD;
        Map<Verdict, Long> verdicts = decisions.stream()
                .collect(Collectors.groupingBy(Decision::verdict, Collectors.counting()));
        return new Stage("refactor", status, "proposals=" + decisions.size() + " written=" + written.size()
                + " memoryRows=" + memory + " verdicts=" + new java.util.TreeMap<>(verdicts));
    }

    private static Stage verify(Options options, Path out, Map<String, String> baseline, List<String> written)
            throws IOException {
        atomize(options, out.resolve("verify"));
        Map<String, String> now = M3AtomizeRun.files(out.resolve("verify"), "VERIFY_INPUT_DRIFT");
        Set<String> changed = Stream.concat(baseline.keySet().stream(), now.keySet().stream())
                .filter(path -> !Objects.equals(baseline.get(path), now.get(path)))
                .collect(Collectors.toCollection(TreeSet::new));
        boolean exact = changed.equals(new TreeSet<>(written));
        long second = written.stream().filter(path -> !read(options.root().resolve(path))
                .equals(read(options.proposal().resolve(path)))).count();
        return new Stage("verify", exact && second == 0 ? M3MopReceipt.PASS : M3MopReceipt.FAIL,
                "changedPaths=" + changed.size() + " expected=" + written.size()
                        + " secondRunChanges=" + (written.isEmpty() ? "NOT_RUN" : Long.toString(second)));
    }

    private static Result publish(Path out, Result result, boolean write) throws IOException {
        M3MopReceipt.write(out.resolve("CHAIN.tsv"), Stream.concat(Stream.of("stage\tstatus\tdetail"),
                result.stages().stream().map(s -> String.join("\t", s.name(), s.status(),
                        M3Atom.clean(s.detail())))).toList());
        Stream<String> stages = result.stages().stream().map(stage -> {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("detail", stage.detail());
            fields.put("write", write);
            return M3MopReceipt.line(GOAL, stage.name(), stage.status(), fields);
        });
        M3MopReceipt.write(out.resolve("RECEIPT.jsonl"), Stream.concat(stages,
                result.decisions().stream().map(d -> M3RefactorGate.receipt(GOAL, d))).toList());
        return result;
    }

    private static Set<Path> excluded(Options options) {
        return Stream.concat(Stream.of(options.proposal(), options.out()), options.again().stream())
                .map(path -> path.toAbsolutePath().normalize()).collect(Collectors.toSet());
    }

    /** Sorted regular *.java files under {@code root}, skipping excluded trees and .git. */
    static List<Path> javaFiles(Path root, Set<Path> excluded) throws IOException {
        Path base = root.toAbsolutePath().normalize();
        try (Stream<Path> walk = Files.walk(base)) {
            return walk.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> excluded.stream().noneMatch(path::startsWith))
                    .filter(path -> !base.relativize(path).toString().replace('\\', '/').contains(".git/"))
                    .sorted().toList();
        }
    }

    private static String relative(Path root, Path file) {
        return root.toAbsolutePath().normalize().relativize(file).toString().replace('\\', '/');
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static void write(Path file, String text) {
        try {
            M3MopReceipt.writeBytes(file, text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
