// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tooling.recipe.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Dependency-free smoke and refusal tests; invoked as part of the Maven recipe proof. */
public final class M3RecipeAgentProof {
    private M3RecipeAgentProof() {}

    private static M3RecipeAgent.Action action(String id, M3RecipeAgent.Operation kind,
                                                String path, String regex, String replacement,
                                                Map<String, String> seals, int count, List<String> parents) {
        return new M3RecipeAgent.Action(id, kind, new M3RecipeAgent.Target(M3RecipeAgent.TargetKind.FILE, path, "*"),
                regex, replacement, "", seals, count, parents, M3RecipeAgent.Authority.FILE, "");
    }

    public static void main(String[] ignored) throws Exception {
        Path root = Files.createTempDirectory("m3-agent-proof");
        try {
            Files.createDirectories(root.resolve("folder"));
            Files.writeString(root.resolve("folder/Sample.java"), "int value = 1;\n");
            M3RecipeAgent agent = new M3RecipeAgent();
            String old = "int value = 1;\n";
            var literal = action("literal", M3RecipeAgent.Operation.LITERAL, "folder/Sample.java",
                    "value", "count", Map.of("folder/Sample.java", M3RecipeAgent.sha(old)), 1, List.of());
            var regex = action("regex", M3RecipeAgent.Operation.REGEX, "folder/Sample.java",
                    "= (\\d+);", "= $1 /* pinned */;",
                    Map.of("folder/Sample.java", M3RecipeAgent.sha("int count = 1;\n")), 1,
                    List.of("literal"));
            var plan = agent.preview(root, List.of(regex, literal));
            check(plan.orderedActions().equals(List.of("literal", "regex")), "topology");
            check(plan.changes().size() == 1, "plan size");
            check(Files.readString(root.resolve("folder/Sample.java")).equals(old), "dry run changed bytes");
            refusal(() -> agent.apply(root, List.of(regex, literal), "wrong", false));
            agent.apply(root, List.of(regex, literal), plan.fingerprint(), false);
            check(Files.readString(root.resolve("folder/Sample.java")).equals("int count = 1 /* pinned */;\n"), "apply");
            refusal(() -> agent.preview(root, List.of(regex, literal)));
            refusal(() -> agent.preview(root, List.of(
                    action("a", M3RecipeAgent.Operation.LITERAL, "folder/Sample.java", "count", "x",
                            Map.of("folder/Sample.java", M3RecipeAgent.sha("int count = 1 /* pinned */;\n")), 1, List.of()),
                    action("b", M3RecipeAgent.Operation.LITERAL, "folder/Sample.java", "count", "y",
                            Map.of("folder/Sample.java", M3RecipeAgent.sha("int count = 1 /* pinned */;\n")), 1, List.of()))));
            refusal(() -> agent.preview(root, List.of(
                    action("a", M3RecipeAgent.Operation.LITERAL, "folder/Sample.java", "count", "x",
                            Map.of("folder/Sample.java", M3RecipeAgent.sha("int count = 1 /* pinned */;\n")), 1, List.of("b")),
                    action("b", M3RecipeAgent.Operation.LITERAL, "folder/Sample.java", "count", "y",
                            Map.of("folder/Sample.java", M3RecipeAgent.sha("int count = 1 /* pinned */;\n")), 1, List.of("a")))));
            String now = Files.readString(root.resolve("folder/Sample.java"));
            var tree = new M3RecipeAgent.Action("folder", M3RecipeAgent.Operation.REGEX,
                    new M3RecipeAgent.Target(M3RecipeAgent.TargetKind.FOLDER, "folder", "*.java"),
                    "count", "total", "", Map.of("folder/Sample.java", M3RecipeAgent.sha(now)),
                    1, List.of(), M3RecipeAgent.Authority.FOLDER, "");
            var folderPlan = agent.preview(root, List.of(tree));
            check(folderPlan.changes().size() == 1, "folder scope");
            Files.writeString(root.resolve("folder/Unexpected.java"), "none\n");
            final var exactTree = tree;
            refusal(() -> agent.preview(root, List.of(exactTree)));
            Files.delete(root.resolve("folder/Unexpected.java"));
            agent.apply(root, List.of(tree), folderPlan.fingerprint(), false);
            check(Files.readString(root.resolve("folder/Sample.java")).contains("total"), "folder apply");

            String narrowBefore = Files.readString(root.resolve("folder/Sample.java"));
            var narrow = action("narrow", M3RecipeAgent.Operation.LITERAL, "folder/Sample.java",
                    "total", "shared", Map.of("folder/Sample.java", M3RecipeAgent.sha(narrowBefore)),
                    1, List.of());
            var widenDenied = new M3RecipeAgent.Action("widen", M3RecipeAgent.Operation.LITERAL,
                    new M3RecipeAgent.Target(M3RecipeAgent.TargetKind.FOLDER, "folder", "*.java"),
                    "shared", "combined", "",
                    Map.of("folder/Sample.java", M3RecipeAgent.sha(narrowBefore.replace("total", "shared"))),
                    1, List.of("narrow"), M3RecipeAgent.Authority.FOLDER, "");
            refusal(() -> agent.preview(root, List.of(narrow, widenDenied)));
            var widenAllowed = new M3RecipeAgent.Action("widen", M3RecipeAgent.Operation.LITERAL,
                    widenDenied.target(), widenDenied.pattern(), widenDenied.replacement(),
                    widenDenied.destination(), widenDenied.preimages(), widenDenied.expectedMatches(),
                    widenDenied.dependsOn(), widenDenied.authority(), widenDenied.providerId(), true);
            check(agent.preview(root, List.of(narrow, widenAllowed)).changes().size() == 1,
                    "explicit scoped promotion");

            String value = Files.readString(root.resolve("folder/Sample.java"));
            var move = new M3RecipeAgent.Action("move", M3RecipeAgent.Operation.MOVE_FILE,
                    new M3RecipeAgent.Target(M3RecipeAgent.TargetKind.FILE, "folder/Sample.java", "*"),
                    "", "", "folder/Moved.java", Map.of("folder/Sample.java", M3RecipeAgent.sha(value)),
                    0, List.of(), M3RecipeAgent.Authority.FILE, "");
            var movePlan = agent.preview(root, List.of(move));
            refusal(() -> agent.apply(root, List.of(move), movePlan.fingerprint(), false));
            agent.apply(root, List.of(move), movePlan.fingerprint(), true);
            check(!Files.exists(root.resolve("folder/Sample.java")), "move src gone");
            check(Files.readString(root.resolve("folder/Moved.java")).equals(value), "move dest");

            var create = new M3RecipeAgent.Action("create", M3RecipeAgent.Operation.CREATE_FILE,
                    new M3RecipeAgent.Target(M3RecipeAgent.TargetKind.FILE, "folder/New.java", "*"),
                    "", "class New {}\n", "", Map.of(), 0, List.of(), M3RecipeAgent.Authority.FILE, "");
            var createPlan = agent.preview(root, List.of(create));
            agent.apply(root, List.of(create), createPlan.fingerprint(), false);
            check(Files.readString(root.resolve("folder/New.java")).equals("class New {}\n"), "create");
            var delete = new M3RecipeAgent.Action("delete", M3RecipeAgent.Operation.DELETE_FILE,
                    new M3RecipeAgent.Target(M3RecipeAgent.TargetKind.FILE, "folder/New.java", "*"),
                    "", "", "", Map.of("folder/New.java", M3RecipeAgent.sha("class New {}\n")),
                    0, List.of(), M3RecipeAgent.Authority.FILE, "");
            var deletePlan = agent.preview(root, List.of(delete));
            agent.apply(root, List.of(delete), deletePlan.fingerprint(), true);
            check(!Files.exists(root.resolve("folder/New.java")), "delete");

            String before = Files.readString(root.resolve("folder/Moved.java"));
            var semantic = new M3RecipeAgent.Action("ast", M3RecipeAgent.Operation.SEMANTIC,
                    new M3RecipeAgent.Target(M3RecipeAgent.TargetKind.FILE, "folder/Moved.java", "*"),
                    "", "", "", Map.of("folder/Moved.java", M3RecipeAgent.sha(before)),
                    0, List.of(), M3RecipeAgent.Authority.FILE, "m3ast");
            refusal(() -> agent.preview(root, List.of(semantic)));
            M3RecipeAgent semanticAgent = new M3RecipeAgent(List.of(new M3RecipeAgent.SemanticProvider() {
                public String id() { return "m3ast"; }
                public String rewrite(String path, String source, M3RecipeAgent.Action act) {
                    return source.replace("total", "number");
                }
            }));
            check(semanticAgent.preview(root, List.of(semantic)).changes().size() == 1, "provider SPI");
            refusal(() -> new M3RecipeAgent.Target(M3RecipeAgent.TargetKind.FILE, "../out", "*"));
            refusal(() -> new M3RecipeAgent.Target(M3RecipeAgent.TargetKind.FILE, "/etc/passwd", "*"));
            System.out.println("M3_RECIPE_AGENT_PROOF_PASS");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path file : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void refusal(ThrowingRunnable runnable) throws Exception {
        try { runnable.run(); }
        catch (IllegalArgumentException | IllegalStateException expected) { return; }
        throw new AssertionError("expected fail-closed refusal");
    }

    private interface ThrowingRunnable { void run() throws Exception; }
}
