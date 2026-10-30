// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Chain: atomize -> pattern plan -> gated all-or-nothing refactor -> verify, with receipts. */
public final class M3MopChainContract {
    private static int checks;
    private static final String A = "package p;\n//FLLDCIM: MOP — a\npublic class A {\n"
            + "    public int f(int x) { return x + 1; }\n}\n";
    private static final String A2 = A.replace("x + 1", "1 + x");
    private static final String B = "package p;\n//FLLDCIM: MOP — b\npublic class B {\n"
            + "    public int g(int y) { return y * 2; }\n}\n";
    private static final String B_LOST = B.replace("//FLLDCIM: MOP — b\n", "");

    private M3MopChainContract() { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private record Repo(Path root, Path proposal, Path again, Path out) { }

    private static Repo repo(Path work, String name, String a, String b, boolean again) throws IOException {
        Path base = Files.createDirectories(work.resolve(name));
        Repo repo = new Repo(base.resolve("root"), base.resolve("proposal"), base.resolve("again"), base.resolve("out"));
        put(repo.root(), "m/src/main/java/p/A.java", A);
        put(repo.root(), "m/src/main/java/p/B.java", B);
        put(repo.proposal(), "m/src/main/java/p/A.java", a);
        if (b != null) put(repo.proposal(), "m/src/main/java/p/B.java", b);
        if (again) {
            put(repo.again(), "m/src/main/java/p/A.java", a);
            if (b != null) put(repo.again(), "m/src/main/java/p/B.java", b);
        }
        return repo;
    }

    private static void put(Path root, String path, String text) throws IOException {
        Files.createDirectories(root.resolve(path).getParent());
        Files.writeString(root.resolve(path), text, StandardCharsets.UTF_8);
    }

    private static M3MopChain.Result chain(Repo repo, boolean write, boolean again) throws IOException {
        return M3MopChain.run(new M3MopChain.Options(repo.root(), repo.proposal(),
                again ? Optional.of(repo.again()) : Optional.empty(), repo.out(), write, Optional.empty(),
                "com.synexia.rewrite.DemoRecipe", repo.out(), "RECIPE_MEMORY_CANDIDATES.tsv"));
    }

    private static String stage(M3MopChain.Result result, String name) {
        return result.stages().stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow().status();
    }

    private static String file(Repo repo, String name) throws IOException {
        return Files.readString(repo.root().resolve("m/src/main/java/p/" + name), StandardCharsets.UTF_8);
    }

    private static void dryRun(Path work) throws Exception {
        Repo repo = repo(work, "dry", A2, null, true);
        M3MopChain.Result result = chain(repo, false, true);
        check(result.passed() && result.written().isEmpty(), "dry chain passes without writing");
        check(file(repo, "A.java").equals(A), "dry chain leaves sources unchanged");
        check(Files.readAllLines(repo.out().resolve("CHAIN.tsv")).stream().skip(1).map(l -> l.split("\t")[0])
                .toList().equals(List.of("atomize", "pattern-plan", "refactor", "verify")), "four ordered stages");
        byte[] receipt = Files.readAllBytes(repo.out().resolve("RECEIPT.jsonl"));
        chain(repo, false, true);
        check(java.util.Arrays.equals(receipt, Files.readAllBytes(repo.out().resolve("RECEIPT.jsonl"))),
                "dry chain receipt is byte-identical on rerun");
    }

    private static void writeRun(Path work) throws Exception {
        Repo repo = repo(work, "write", A2, null, true);
        M3MopChain.Result result = chain(repo, true, true);
        check(result.passed() && result.written().equals(List.of("m/src/main/java/p/A.java")),
                "write chain promotes exactly the admitted file");
        check(file(repo, "A.java").equals(A2) && file(repo, "B.java").equals(B), "only A changed");
        check(result.stages().get(3).detail().equals("changedPaths=1 expected=1 secondRunChanges=0"),
                "verify: changed paths == promoted files, second run changes nothing");
    }

    /** Discriminator: one refused file blocks every write, including admitted ones. */
    private static void allOrNothing(Path work) throws Exception {
        Repo repo = repo(work, "refused", A2, B_LOST, true);
        M3MopChain.Result result = chain(repo, true, true);
        check(stage(result, "refactor").equals(M3MopReceipt.REFUSED) && result.written().isEmpty(),
                "a refusal blocks all writes");
        check(file(repo, "A.java").equals(A) && file(repo, "B.java").equals(B), "sources untouched after refusal");
        check(stage(result, "verify").equals(M3MopReceipt.PASS) && result.memoryRows() == 1,
                "verify passes with zero changes; one memory row");
        check(M3RecipeMemory.read(repo.out().resolve("RECIPE_MEMORY_CANDIDATES.tsv")).get(0).trigger()
                .equals("REFUSED_ANCHOR_LOSS"), "memory row names the refusal");
        check(Files.readString(repo.out().resolve("RECEIPT.jsonl")).contains("\"stage\":\"refactor-file\",\"status\":\"REFUSED\""),
                "per-file refusal receipt");
    }

    private static void holdAndBaseline(Path work) throws Exception {
        Repo hold = repo(work, "hold", A2, null, false);
        M3MopChain.Result held = chain(hold, true, false);
        check(stage(held, "refactor").equals(M3MopReceipt.HOLD) && held.written().isEmpty()
                && file(hold, "A.java").equals(A), "no second application -> HOLD, nothing written");
        Repo extra = repo(work, "extra", A2, null, true);
        put(extra.proposal(), "m/src/main/java/p/New.java", "package p;\nclass New { }\n");
        put(extra.again(), "m/src/main/java/p/New.java", "package p;\nclass New { }\n");
        M3MopChain.Result result = chain(extra, true, true);
        check(result.decisions().stream().anyMatch(d -> d.verdict() == M3RefactorGate.Verdict.REFUSED_NOT_IN_BASELINE)
                && result.written().isEmpty(), "file without atomize identity refused");
        Repo cli = repo(work, "cli", A2, null, false);
        int status = M3Mop.run(new String[] {"chain", "--root=" + cli.root(), "--proposal=" + cli.proposal(),
            "--out=" + cli.out(), "--write"}, new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        check(status == 3, "CLI chain HOLD exits 3");
    }

    public static void main(String[] args) throws Exception {
        Path work = Files.createTempDirectory("m3-mop-chain");
        dryRun(work);
        writeRun(work);
        allOrNothing(work);
        holdAndBaseline(work);
        System.out.println("M3_MOP_CHAIN_CONTRACT_PASS checks=" + checks);
    }
}
