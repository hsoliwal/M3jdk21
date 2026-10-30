// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import com.m3.mop.M3PatternRegistry.State;
import com.m3.mop.M3PatternRegistry.Verdict;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Patternize pillar: KNOWN/UNPROVEN/UNKNOWN states, append-only versioned registry, plan scopes. */
public final class M3PatternContract {
    private static int checks;
    private static final String A = "package p;\npublic class A {\n"
            + "    public int same(int x) { return x + 1; }\n"
            + "    public int shape(int y) { return y * 2; }\n"
            + "    public String only() { synchronized (this) { return toString(); } }\n}\n";
    private static final String B = "package q;\npublic class B {\n"
            + "    public int same(int x) { return x + 1; }\n"
            + "    public int other(int z) { return z * 3; }\n}\n";
    private static final String C = "package r;\npublic class C {\n"
            + "    public int third(int w) { return w * 4; }\n}\n";

    private M3PatternContract() { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static void rejects(String reason, Checked action) throws Exception {
        checks++;
        try {
            action.run();
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError(reason + " did not refuse");
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private static State state(List<Verdict> verdicts, String name) {
        return verdicts.stream().filter(v -> v.atom().name().equals(name)).findFirst().orElseThrow().state();
    }

    private static List<M3Atom> atoms(String... sources) throws IOException {
        return java.util.stream.Stream.of(sources).map(M3PatternContract::unit)
                .flatMap(unit -> unit.atoms().stream()).toList();
    }

    private static M3Atomizer.Unit unit(String source) {
        try {
            return M3Atomizer.parse(source.substring(8, 9) + "/" + source.substring(source.indexOf("class ") + 6,
                    source.indexOf(" {")) + ".java", source);
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }

    private static void states() throws Exception {
        List<Verdict> verdicts = M3PatternRegistry.empty().classify(atoms(A, B));
        check(state(verdicts, "same") == State.KNOWN_PATTERN, "exact bytes recur -> KNOWN_PATTERN");
        check(state(verdicts, "shape") == State.UNPROVEN_LEAF, "same shape other bytes -> UNPROVEN_LEAF");
        check(state(verdicts, "only") == State.UNKNOWN_PATTERN, "no recurrence -> UNKNOWN_PATTERN");
        Verdict shape = verdicts.stream().filter(v -> v.atom().name().equals("shape")).findFirst().orElseThrow();
        check(shape.patternId().matches("M3P1-[0-9a-f]{16}") && shape.version() == 0, "unregistered pattern id");
        check(verdicts.stream().filter(v -> v.atom().name().equals("other")).findFirst().orElseThrow()
                .patternId().equals(shape.patternId()), "same shape -> same versioned pattern id");
    }

    /** Discriminator: without a persisted registry a lone atom of a known shape is UNKNOWN. */
    private static void registry(Path work) throws Exception {
        Path file = work.resolve("PATTERN_REGISTRY.tsv");
        List<Verdict> first = M3PatternRegistry.empty().classify(atoms(A, B));
        int added = M3PatternRegistry.register(file, first);
        byte[] prefix = Files.readAllBytes(file);
        check(added > 0 && M3PatternRegistry.read(file).rows().size() == added, "recurring exemplars registered");
        check(M3PatternRegistry.register(file, first) == 0, "second register appends nothing (fixed point)");
        M3PatternRegistry registry = M3PatternRegistry.read(file);
        List<Verdict> lone = registry.classify(atoms(C));
        check(state(lone, "third") == State.UNPROVEN_LEAF, "registered shape persists across runs");
        Verdict third = lone.stream().filter(v -> v.atom().name().equals("third")).findFirst().orElseThrow();
        check(third.version() >= 2, "versions increase per exemplar: " + third.version());
        check(M3PatternRegistry.register(file, lone) == 2, "new method and header exemplars appended");
        check(M3PatternRegistry.read(file).rows().stream().anyMatch(row -> row.exemplar().endsWith("#third")
                && row.version() == third.version() + 1), "new exemplar is the next version of its pattern");
        M3RecipeMemory.verifyAppendOnly(prefix, Files.readAllBytes(file));
        check(state(M3PatternRegistry.read(file).classify(atoms(C)), "third") == State.KNOWN_PATTERN,
                "registered exemplar bytes are KNOWN_PATTERN");
        check(Files.readString(file).lines().skip(1).allMatch(line -> line.startsWith("M3-PATTERN/1\tM3P1-")),
                "every row carries schema M3-PATTERN/1");
        tamper(work, Files.readString(file, StandardCharsets.UTF_8));
    }

    private static void tamper(Path work, String good) throws Exception {
        String[] lines = good.split("\n");
        String forged = lines[1].replaceFirst("\t[0-9a-f]{64}\t", "\t" + "0".repeat(64) + "\t");
        Path shape = write(work.resolve("forged.tsv"), good.replace(lines[1], forged));
        rejects("forged shape key", () -> M3PatternRegistry.read(shape));
        Path version = write(work.resolve("version.tsv"), good.replace(lines[1], lines[1].replaceFirst("\t1\t", "\t2\t")));
        rejects("non-contiguous version", () -> M3PatternRegistry.read(version));
        Path unknown = write(work.resolve("unknown.tsv"), good.replace(lines[1],
                lines[1].replaceAll("(KNOWN_PATTERN|UNPROVEN_LEAF)$", "UNKNOWN_PATTERN")));
        rejects("UNKNOWN_PATTERN registered", () -> M3PatternRegistry.read(unknown));
        Path crlf = write(work.resolve("crlf.tsv"), good.replace("\n", "\r\n"));
        rejects("CRLF registry", () -> M3PatternRegistry.read(crlf));
        Path duplicate = write(work.resolve("duplicate.tsv"), good + lines[1] + "\n");
        rejects("duplicate exemplar", () -> M3PatternRegistry.read(duplicate));
    }

    private static void plan(Path work) throws Exception {
        check(M3PatternPlan.scope(Set.of("m/src/main/java/p/A.java")).equals("FILE"), "FILE scope");
        check(M3PatternPlan.scope(Set.of("m/src/p/A.java", "m/src/p/B.java")).equals("PACKAGE"), "PACKAGE scope");
        check(M3PatternPlan.scope(Set.of("m/src/p/A.java", "m/src/q/B.java")).equals("MODULE"), "MODULE scope");
        check(M3PatternPlan.scope(Set.of("m/src/p/A.java", "n/src/q/B.java")).equals("PROJECT"), "PROJECT scope");
        Path atomize = work.resolve("atomize");
        M3AtomizeRun.publish(List.of(unit(A), unit(B)), new M3AtomizeRun.Options(atomize, Optional.empty(), false, "contract"));
        M3PatternPlan.Result result = M3PatternPlan.write(atomize, work.resolve("plan"));
        List<String> plan = Files.readAllLines(work.resolve("plan").resolve(M3PatternPlan.FILE_NAME));
        check(plan.get(0).equals(M3PatternPlan.HEADER) && result.rows() == plan.size() - 1, "plan rows");
        check(plan.stream().anyMatch(row -> row.contains("\tKNOWN_PATTERN\t") && row.contains("REUSE_EXISTING_RECIPE")),
                "KNOWN -> REUSE_EXISTING_RECIPE");
        check(plan.stream().skip(1).allMatch(row -> row.endsWith("\tfalse")), "plan grants no promotion authority");
        Files.writeString(atomize.resolve("PATTERNS.tsv"), "tampered\n", StandardCharsets.UTF_8);
        rejects("PATTERN_INPUT_DRIFT", () -> M3PatternPlan.write(atomize, work.resolve("plan2")));
        check(!Files.exists(work.resolve("plan2")), "drifted input writes no plan");
    }

    private static Path write(Path file, String text) throws IOException {
        return Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        Path work = Files.createTempDirectory("m3-mop-pattern");
        states();
        registry(work);
        plan(work);
        System.out.println("M3_MOP_PATTERN_CONTRACT_PASS checks=" + checks);
    }
}
