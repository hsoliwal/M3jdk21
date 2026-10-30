// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import com.m3.mop.M3RefactorGate.Decision;
import com.m3.mop.M3RefactorGate.Input;
import com.m3.mop.M3RefactorGate.Verdict;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Refactor pillar: every refusal class, HOLD without a fixed point, and candidate memory rows. */
public final class M3RefactorGateContract {
    private static int checks;
    private static final String PATH = "p/Api.java";
    private static final String BEFORE = "package p;\n"
            + "//FLLDCIM: MOP — api anchor\n"
            + "public class Api {\n"
            + "    public static final int LIMIT = 5;\n"
            + "    public int f(int x) { return x + 1; }\n"
            + "    int g(int[] xs) { int s = 0; for (int x : xs) { s += x; } return s; }\n"
            + "    public record R(int n) { }\n"
            + "    public enum E { A, B }\n"
            + "}\n";

    private M3RefactorGateContract() { }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static Decision gate(String after, Optional<String> again, Optional<String> baseline) {
        return M3RefactorGate.check(new Input(PATH, BEFORE, after, again, baseline));
    }

    private static Verdict verdict(String after) {
        return gate(after, Optional.of(after), Optional.empty()).verdict();
    }

    private static String sixteenLines(String name) {
        return "    int " + name + "() {\n" + "        int t = 0;\n".repeat(14) + "        return t; }\n";
    }

    private static void admission() {
        String after = BEFORE.replace("return x + 1;", "return 1 + x;");
        check(verdict(after) == Verdict.ADMITTED, "body-only change with fixed point is ADMITTED");
        check(gate(after, Optional.empty(), Optional.empty()).verdict() == Verdict.HOLD_FIXED_POINT_NOT_RUN,
                "no second application -> HOLD, never ADMITTED");
        check(gate(after, Optional.of(after + "\n"), Optional.empty()).verdict() == Verdict.REFUSED_NOT_FIXED_POINT,
                "second application changes output -> REFUSED_NOT_FIXED_POINT");
        check(gate(after, Optional.of(after), Optional.of("0".repeat(64))).verdict() == Verdict.REFUSED_DRIFT,
                "before differs from atomize baseline -> REFUSED_DRIFT");
        check(gate(after, Optional.of(after), Optional.of(M3MopReceipt.sha256(BEFORE))).admitted(),
                "matching baseline admits");
        check(verdict(BEFORE.replace("//FLLDCIM: MOP — api anchor\n", "")) == Verdict.REFUSED_ANCHOR_LOSS,
                "anchor removed -> REFUSED_ANCHOR_LOSS");
        check(verdict(BEFORE.replace("return x + 1;", "return x +;")) == Verdict.REFUSED_PARSE,
                "unparsable output -> REFUSED_PARSE");
    }

    /** Discriminators against the #13966 gate: narrowing and static flips were admitted there. */
    private static void apiDrift() {
        List.of(BEFORE.replace("public int f(", "protected int f("),
                BEFORE.replace("public int f(", "public static int f("),
                BEFORE.replace("public int f(int x)", "public long f(int x)"),
                BEFORE.replace("    int g(", "    public int h() { return 0; }\n    int g("),
                BEFORE.replace("public class Api", "class Api"),
                BEFORE.replace("public record R(int n)", "public record R(long n)"),
                BEFORE.replace("A, B", "B, A"),
                BEFORE.replace("LIMIT = 5", "LIMIT = 6"))
                .forEach(after -> check(verdict(after) == Verdict.REFUSED_API_DRIFT,
                        "exposed contract drift refused: " + after.lines().filter(l -> !BEFORE.contains(l)).toList()));
        check(verdict(BEFORE.replace("int g(int[] xs)", "int g(int[] ys)").replace("for (int x : xs)", "for (int x : ys)"))
                == Verdict.ADMITTED, "package-private change is not API drift");
    }

    /** Discriminator against a total-count ratchet: a LOOP removed cannot pay for a METHOD_15 added. */
    private static void limitRatchet() {
        String swapped = BEFORE.replace("for (int x : xs) { s += x; }", "s += xs.length;")
                .replace("    public record", sixteenLines("k") + "    public record");
        Decision decision = gate(swapped, Optional.of(swapped), Optional.empty());
        check(decision.verdict() == Verdict.REFUSED_LIMIT_RATCHET && decision.detail().startsWith("METHOD_15 0->1"),
                "per-kind ratchet refuses METHOD_15 growth: " + decision.detail());
        String loops = BEFORE.replace("    public record", "    int w() { int i = 0; while (i < 3) { i++; } return i; }\n    public record");
        check(verdict(loops) == Verdict.REFUSED_LIMIT_RATCHET, "added LOOP refused");
    }

    private static void memory(Path work) throws Exception {
        Path root = Files.createDirectories(work.resolve("memory"));
        String ledger = "recipes/demo/RECIPE_MEMORY_CANDIDATES.tsv";
        Decision refused = gate(BEFORE.replace("public int f(", "protected int f("), Optional.empty(), Optional.empty());
        Decision held = gate(BEFORE, Optional.empty(), Optional.empty());
        check(M3RefactorGate.remember(root, ledger, "com.synexia.rewrite.DemoRecipe", List.of(refused, held)) == 1,
                "one candidate row per refusal, none for HOLD");
        check(M3RefactorGate.remember(root, ledger, "com.synexia.rewrite.DemoRecipe", List.of(refused)) == 0,
                "rerun over the same refusal keeps one row");
        List<M3RecipeMemory.Entry> rows = M3RecipeMemory.read(root.resolve(ledger));
        check(rows.size() == 1 && rows.get(0).trigger().equals("REFUSED_API_DRIFT")
                && rows.get(0).guard().equals("M3RefactorGate.REFUSED_API_DRIFT")
                && rows.get(0).correction().startsWith("REFUSED@p/Api.java#"), "row is M3RecipeMemory schema");
        check(!Files.exists(root.resolve("recipes/demo/RECIPE_MEMORY.tsv")), "proven memory untouched");
        checks++;
        try {
            M3RecipeMemory.appendCandidate(root, "recipes/demo/RECIPE_MEMORY.tsv",
                    M3RefactorGate.entry(Instant.now(), "com.synexia.rewrite.DemoRecipe", refused));
            throw new AssertionError("candidate row reached the proven memory");
        } catch (IOException expected) {
            // Candidates never enter RECIPE_MEMORY.tsv.
        }
    }

    private static void cli(Path work) throws Exception {
        Path before = Files.writeString(work.resolve("before.java"), BEFORE, StandardCharsets.UTF_8);
        Path same = Files.writeString(work.resolve("same.java"), BEFORE, StandardCharsets.UTF_8);
        Path narrowed = Files.writeString(work.resolve("narrowed.java"),
                BEFORE.replace("public int f(", "int f("), StandardCharsets.UTF_8);
        String common = "--path=" + PATH + " --before=" + before + " --out=" + work.resolve("cli");
        check(run(common + " --after=" + same + " --again=" + same) == 0, "CLI ADMITTED exits 0");
        check(run(common + " --after=" + same) == 3, "CLI HOLD exits 3");
        check(run(common + " --after=" + narrowed + " --again=" + narrowed) == 1, "CLI REFUSED exits 1");
        check(Files.readString(work.resolve("cli/RECEIPT.jsonl")).contains("\"verdict\":\"REFUSED_API_DRIFT\""),
                "gate receipt names the verdict");
        check(M3RecipeMemory.read(work.resolve("cli/RECIPE_MEMORY_CANDIDATES.tsv")).size() == 1,
                "CLI refusal wrote one candidate memory row");
        check(run("gate --path=x") == 2, "missing option is usage error 2");
    }

    private static int run(String line) {
        String[] args = line.startsWith("gate ") ? line.split(" ") : ("gate " + line).split(" ");
        return M3Mop.run(args, new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    }

    public static void main(String[] args) throws Exception {
        Path work = Files.createTempDirectory("m3-mop-gate");
        admission();
        apiDrift();
        limitRatchet();
        memory(work);
        cli(work);
        System.out.println("M3_MOP_REFACTOR_GATE_CONTRACT_PASS checks=" + checks);
    }
}
