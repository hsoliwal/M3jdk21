// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/** Real-file Java21 deterministic reactor oracle; no extra libraries. */
public final class M3ReactorImpactPlanContract {
    private static int checks;
    private M3ReactorImpactPlanContract() { }
    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }
    private static void same(int[] expected, M3ReactorImpactPlan plan, String reason) {
        checks++;
        if (!Arrays.equals(expected, plan.impactedProjects())) {
            throw new AssertionError(reason + " expected=" + Arrays.toString(expected)
                    + " actual=" + Arrays.toString(plan.impactedProjects()));
        }
    }
    private static void reject(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError("unexpected exception", problem);
        }
        throw new AssertionError("missing refusal: " + type.getName());
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-reactor-impact-");
        try {
            List<Path> names = List.of(Path.of("api/A.java"), Path.of("core/B.java"),
                    Path.of("app/C.java"), Path.of("tools/D.java"), Path.of("pom.xml"));
            for (Path name : names) {
                Path path = root.resolve(name);
                Files.createDirectories(path.getParent());
                Files.writeString(path, name.toString());
            }
            var first = M3FileDeltaLedger.scan(root, null, names);
            var steady = M3FileDeltaLedger.scan(root, first, names);
            int[] projectForFile = new int[steady.slots()];
            for (int i = 0; i < projectForFile.length; i++) {
                String n = names.get(i).toString().replace('\\','/');
                int slot = steady.physicalSlot(Path.of(n));
                projectForFile[slot] = switch (n) {
                    case "api/A.java" -> 0;
                    case "core/B.java" -> 1;
                    case "app/C.java" -> 2;
                    case "tools/D.java" -> 3;
                    default -> -1;
                };
            }
            // api -> core -> app; standalone tools. Kahn order = [api,tools,core,app].
            int[] reactorOffsets = {0,1,2,2,2}, reactorTargets = {1,2};
            int[] noDeps = new int[steady.slots()+1];
            byte[] unsafe = new byte[steady.slots()];
            var cleanFiles = M3BuildImpactPlan.compute(steady, true,
                    noDeps, new int[0], unsafe);
            var clean = M3ReactorImpactPlan.compute(steady, cleanFiles,
                    projectForFile, 4, true, reactorOffsets, reactorTargets);
            check(clean.size() == 0 && !clean.requiresConservativeReactor(), "no file delta");
            Files.writeString(root.resolve("api/A.java"), "changed API");
            var changed = M3FileDeltaLedger.scan(root, steady, names);
            var impacted = M3BuildImpactPlan.compute(changed, true,
                    noDeps, new int[0], unsafe);
            // A separately captured generation has the same direct delta masks.
            // It must never accept graph/ABI impact facts from the previous capture.
            var sameMaskOtherGeneration = M3FileDeltaLedger.scan(root, steady, names);
            check(sameMaskOtherGeneration != changed, "different captured snapshot identity");
            check(Arrays.equals(sameMaskOtherGeneration.changedSlots(M3FileDeltaLedger.Kind.SOURCE),
                    changed.changedSlots(M3FileDeltaLedger.Kind.SOURCE)), "equal direct source mask");
            reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                    sameMaskOtherGeneration, impacted, projectForFile, 4, true,
                    reactorOffsets, reactorTargets));
            var reactor = M3ReactorImpactPlan.compute(changed, impacted,
                    projectForFile, 4, true, reactorOffsets, reactorTargets);
            same(new int[] {0,1,2}, reactor, "dependency closure");
            check(reactor.reasonAt(0) == M3ReactorImpactPlan.FILE_IMPACT, "direct project");
            check((reactor.reasonAt(2) & M3ReactorImpactPlan.DEPENDENT_PROJECT) != 0,
                    "transitive dependent");
            check(reactor.reasonAt(3) == 0, "unaffected tools project");
            check(!reactor.requiresConservativeReactor(), "verified project graph");
            int[] alias = reactor.impactedProjects(); alias[0] = -1;
            check(reactor.impactedProjects()[0] == 0, "immutable physical project directory");
            var unknownGraph = M3ReactorImpactPlan.compute(changed, impacted,
                    projectForFile, 4, false, null, null);
            same(new int[] {0,1,2,3}, unknownGraph, "incomplete graph full rebuild");
            check(unknownGraph.requiresConservativeReactor(), "reactor fail closed");
            int[] absentOwner = projectForFile.clone();
            absentOwner[changed.physicalSlot(Path.of("api/A.java"))] = -1;
            var unknownOwner = M3ReactorImpactPlan.compute(changed, impacted,
                    absentOwner, 4, true, reactorOffsets, reactorTargets);
            same(new int[] {0,3,1,2}, unknownOwner, "unresolved owner full topo rebuild");
            check(unknownOwner.requiresConservativeReactor(), "unknown physical owner fail closed");
            Files.writeString(root.resolve("pom.xml"), "reactor model changed");
            var dirtyModel = M3FileDeltaLedger.scan(root, changed, names);
            var buildFiles = M3BuildImpactPlan.compute(dirtyModel, true,
                    noDeps, new int[0], unsafe);
            var all = M3ReactorImpactPlan.compute(dirtyModel, buildFiles,
                    projectForFile, 4, true, reactorOffsets, reactorTargets);
            same(new int[] {0,3,1,2}, all, "Maven model rebuild in verified topo order");
            check(all.requiresConservativeReactor(), "build model change invalidates reactor");
            reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                    changed, impacted, projectForFile, 4, true,
                    new int[] {0,1,2,3,3}, new int[] {1,2,0}));
            reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                    changed, impacted, projectForFile, 4, true,
                    reactorOffsets, new int[] {-1,2}));
            reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                    changed, impacted, projectForFile, 4, false,
                    reactorOffsets, reactorTargets));
            reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                    changed, impacted, new int[0], 4, true,
                    reactorOffsets, reactorTargets));
            reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                    steady, impacted, projectForFile, 4, true,
                    reactorOffsets, reactorTargets));
            snapshotIdentityContract(root.resolve("snapshot-identity"));
            graphPropagationContract(root.resolve("graph-propagation"));
            System.out.println("M3_REACTOR_IMPACT_PASS checks=" + checks);
        } finally {
            try (var stream = Files.walk(root)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); }
                    catch (IOException cause) { throw new java.io.UncheckedIOException(cause); }
                });
            }
        }
    }

    private static void snapshotIdentityContract(Path root) throws Exception {
        Files.createDirectories(root);
        List<Path> names = List.of(Path.of("A.java"), Path.of("B.java"));
        for (Path name : names) Files.writeString(root.resolve(name), name.toString());
        var first = M3FileDeltaLedger.scan(root, null, names);
        var steady = M3FileDeltaLedger.scan(root, first, names);
        int a = steady.physicalSlot(Path.of("A.java"));
        int b = steady.physicalSlot(Path.of("B.java"));
        int[] owners = new int[steady.slots()];
        owners[a] = 0;
        owners[b] = 1;
        int[] reactorOffsets = {0, 1, 1}, reactorTargets = {1};
        int[] noFileEdges = new int[steady.slots() + 1];
        byte[] unsafe = new byte[steady.slots()];

        Files.writeString(root.resolve("A.java"), "first changed source");
        var changed = M3FileDeltaLedger.scan(root, steady, names);
        var repeated = M3FileDeltaLedger.scan(root, steady, names);
        check(changed != repeated, "repeated observation is a distinct snapshot");
        check(changed.slots() == repeated.slots(), "repeated observation retains slot dimensions");
        for (int slot = 0; slot < changed.slots(); slot++) {
            check(changed.stateAt(slot) == repeated.stateAt(slot),
                    "repeated observation retains every delta state bit");
            check(Arrays.equals(changed.digestAt(slot), repeated.digestAt(slot)),
                    "repeated observation retains exact content hashes");
        }
        var impacted = M3BuildImpactPlan.compute(changed, true,
                noFileEdges, new int[0], unsafe);
        var conservative = M3BuildImpactPlan.compute(changed, false, null, null, null);

        Files.writeString(root.resolve("A.java"), "later changed source");
        var later = M3FileDeltaLedger.scan(root, steady, names);
        check(changed.slots() == later.slots(), "different source retains slot dimensions");
        for (int slot = 0; slot < changed.slots(); slot++) {
            check(changed.stateAt(slot) == later.stateAt(slot),
                    "different source retains every delta state bit");
        }
        check(!Arrays.equals(changed.digestAt(a), later.digestAt(a)),
                "different source has a different content hash");
        for (boolean verified : new boolean[] {true, false}) {
            int[] offsets = verified ? reactorOffsets : null;
            int[] targets = verified ? reactorTargets : null;
            for (M3BuildImpactPlan plan : new M3BuildImpactPlan[] {impacted, conservative}) {
                var retained = M3ReactorImpactPlan.compute(changed, plan,
                        owners, 2, verified, offsets, targets);
                same(new int[] {0, 1}, retained, "original immutable snapshot remains usable");
                check(retained.requiresConservativeReactor()
                        == (!verified || plan.requiresConservativeBuild()),
                        "snapshot binding preserves conservative selection");
                reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                        repeated, plan, owners, 2, verified, offsets, targets));
                reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                        later, plan, owners, 2, verified, offsets, targets));
            }
        }

        var clean = M3FileDeltaLedger.scan(root, later, names);
        var cleanRepeated = M3FileDeltaLedger.scan(root, clean, names);
        check(clean != cleanRepeated, "no-op observations have distinct identity");
        for (int slot = 0; slot < clean.slots(); slot++) {
            check(!clean.changedAt(slot) && !cleanRepeated.changedAt(slot),
                    "both no-op observations have no direct delta");
            check(clean.stateAt(slot) == cleanRepeated.stateAt(slot)
                    && Arrays.equals(clean.digestAt(slot), cleanRepeated.digestAt(slot)),
                    "no-op observations retain state and content");
        }
        var cleanFiles = M3BuildImpactPlan.compute(clean, true,
                noFileEdges, new int[0], unsafe);
        for (boolean verified : new boolean[] {true, false}) {
            int[] offsets = verified ? reactorOffsets : null;
            int[] targets = verified ? reactorTargets : null;
            var noOp = M3ReactorImpactPlan.compute(clean, cleanFiles,
                    owners, 2, verified, offsets, targets);
            check(noOp.size() == 0 && !noOp.requiresConservativeReactor(),
                    "exact no-op observation remains empty for either graph mode");
            reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                    cleanRepeated, cleanFiles, owners, 2, verified, offsets, targets));
        }

        Files.delete(root.resolve("A.java"));
        var removed = M3FileDeltaLedger.scan(root, clean, names);
        check(removed.physicalSlot(Path.of("A.java")) == a && !removed.liveAt(a)
                && (removed.stateAt(a) & M3FileDeltaLedger.REMOVED) != 0,
                "deleted source retains its physical owner slot");
        var removedFiles = M3BuildImpactPlan.compute(removed, true,
                noFileEdges, new int[0], unsafe);
        var removal = M3ReactorImpactPlan.compute(removed, removedFiles,
                owners, 2, true, reactorOffsets, reactorTargets);
        same(new int[] {0, 1}, removal, "source deletion reaches downstream projects");
        check(removal.reasonAt(0) == M3ReactorImpactPlan.FILE_IMPACT
                && removal.reasonAt(1) == M3ReactorImpactPlan.DEPENDENT_PROJECT,
                "deletion preserves direct and dependent project reasons");
        check(!removal.requiresConservativeReactor(), "verified deletion remains incremental");
    }

    private static void graphPropagationContract(Path root) throws Exception {
        Files.createDirectories(root);
        List<Path> names = List.of(Path.of("A.java"), Path.of("B.java"),
                Path.of("C.java"), Path.of("D.java"));
        for (Path name : names) Files.writeString(root.resolve(name), "baseline");
        var first = M3FileDeltaLedger.scan(root, null, names);
        var steady = M3FileDeltaLedger.scan(root, first, names);
        int[] fileSlots = names.stream().mapToInt(steady::physicalSlot).toArray();
        int[] noFileEdges = new int[steady.slots() + 1];
        byte[] unsafe = new byte[steady.slots()];
        var snapshots = new M3FileDeltaLedger.Snapshot[16];
        var plans = new M3BuildImpactPlan[16];
        for (int seed = 0; seed < snapshots.length; seed++) {
            for (int file = 0; file < names.size(); file++) {
                Files.writeString(root.resolve(names.get(file)),
                        (seed & (1 << file)) == 0 ? "baseline" : "changed");
            }
            snapshots[seed] = M3FileDeltaLedger.scan(root, steady, names);
            plans[seed] = M3BuildImpactPlan.compute(snapshots[seed], true,
                    noFileEdges, new int[0], unsafe);
        }

        MessageDigest observations = MessageDigest.getInstance("SHA-256");
        int cases = 0;
        for (int edgeMask = 0; edgeMask < 64; edgeMask++) {
            boolean[][] graph = new boolean[4][4];
            for (int from = 0, edge = 0; from < 4; from++) {
                for (int to = from + 1; to < 4; to++, edge++) {
                    graph[from][to] = (edgeMask & (1 << edge)) != 0;
                }
            }
            // Independent all-pairs closure oracle; production uses neither a matrix nor this recurrence.
            boolean[][] reaches = new boolean[4][];
            for (int from = 0; from < 4; from++) reaches[from] = graph[from].clone();
            for (int via = 0; via < 4; via++) {
                for (int from = 0; from < 4; from++) {
                    for (int to = 0; to < 4; to++) {
                        reaches[from][to] |= reaches[from][via] && reaches[via][to];
                    }
                }
            }
            for (int a = 0; a < 4; a++) for (int b = 0; b < 4; b++) {
                if (a == b) continue;
                for (int c = 0; c < 4; c++) {
                    if (c == a || c == b) continue;
                    int[] project = {a, b, c, 6 - a - b - c};
                    int[] owners = new int[steady.slots()];
                    for (int file = 0; file < 4; file++) owners[fileSlots[file]] = project[file];
                    // A second encoding reverses adjacency order and repeats each edge.
                    for (int encoding = 0; encoding < 2; encoding++) {
                        int[] offsets = new int[5];
                        int[] targets = new int[Integer.bitCount(edgeMask) * (encoding + 1)];
                        for (int physical = 0, at = 0; physical < 4; physical++) {
                            offsets[physical] = at;
                            for (int from = 0; from < 4; from++) {
                                if (project[from] != physical) continue;
                                for (int step = 0; step < 4; step++) {
                                    int to = encoding == 0 ? step : 3 - step;
                                    if (graph[from][to]) {
                                        targets[at++] = project[to];
                                        if (encoding == 1) targets[at++] = project[to];
                                    }
                                }
                            }
                            offsets[physical + 1] = at;
                        }
                        int[] ownerCopy = owners.clone(), offsetCopy = offsets.clone();
                        int[] targetCopy = targets.clone();
                        for (int seed = 0; seed < snapshots.length; seed++) {
                            var plan = M3ReactorImpactPlan.compute(snapshots[seed], plans[seed],
                                    owners, 4, true, offsets, targets);
                            int[] expected = new int[4];
                            for (int from = 0; from < 4; from++) {
                                if ((seed & (1 << from)) == 0) continue;
                                expected[project[from]] |= M3ReactorImpactPlan.FILE_IMPACT;
                                for (int to = 0; to < 4; to++) {
                                    if (reaches[from][to]) {
                                        expected[project[to]] |= M3ReactorImpactPlan.DEPENDENT_PROJECT;
                                    }
                                }
                            }
                            int expectedSize = 0;
                            for (int physical = 0; physical < 4; physical++) {
                                check(plan.reasonAt(physical) == expected[physical],
                                        "DAG reason masks match independent reachability");
                                if (expected[physical] != 0) expectedSize++;
                                observe(observations, plan.reasonAt(physical));
                            }
                            int[] selected = plan.impactedProjects();
                            check(selected.length == expectedSize && plan.size() == expectedSize,
                                    "DAG selection is exactly the reached project set");
                            int[] position = {-1, -1, -1, -1};
                            observe(observations, selected.length);
                            for (int at = 0; at < selected.length; at++) {
                                int physical = selected[at];
                                check(physical >= 0 && physical < 4 && position[physical] == -1
                                                && expected[physical] != 0,
                                        "DAG selection has valid unique affected projects");
                                position[physical] = at;
                                observe(observations, physical);
                            }
                            for (int from = 0; from < 4; from++) {
                                for (int to = 0; to < 4; to++) {
                                    if (reaches[from][to] && expected[project[from]] != 0) {
                                        check(position[project[from]] < position[project[to]],
                                                "selected dependencies precede their consumers");
                                    }
                                }
                            }
                            check(!plan.requiresConservativeReactor(), "complete DAG stays verified");
                            check(Arrays.equals(owners, ownerCopy) && Arrays.equals(offsets, offsetCopy)
                                            && Arrays.equals(targets, targetCopy),
                                    "reactor retains caller-owned primitive input lanes unchanged");
                            cases++;
                        }
                    }
                }
            }
        }
        check(cases == 49_152, "bounded complete four-rank DAG/seed/label/edge-encoding corpus");
        String signature = HexFormat.of().formatHex(observations.digest());
        // Frozen on receiver 7c23ff3612ef08dc265bd3bab5461e4dcf3ee47c before topology reuse.
        check(signature.equals("ce417921d7289b429e670d8cf81bcf0ba56481eee4536bb777acddcef586f4cd"),
                "exact baseline reason masks and deterministic project order are preserved");
        System.out.println("M3_REACTOR_DAG_PERMUTATIONS_PASS cases=" + cases
                + " sha256=" + signature);

        int[] owners = new int[steady.slots()];
        for (int file = 0; file < 4; file++) owners[fileSlots[file]] = file;
        int[] cycleOffsets = {0, 1, 2, 2, 2}, cycleTargets = {1, 0};
        reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                snapshots[0], plans[0], owners, 4, true, cycleOffsets, cycleTargets));
        var conservative = M3BuildImpactPlan.compute(snapshots[1], false, null, null, null);
        reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                snapshots[1], conservative, owners, 4, true, cycleOffsets, cycleTargets));
        reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                snapshots[0], plans[0], owners, 4, true,
                new int[] {0, 1, 1, 1, 1}, new int[] {0}));
        reject(IllegalArgumentException.class, () -> M3ReactorImpactPlan.compute(
                snapshots[0], plans[0], owners, 4, true,
                new int[] {0, 1, 1, 1, 1}, new int[] {4}));

        int projects = 16_384;
        int[] chainOffsets = new int[projects + 1], chainTargets = new int[projects - 1];
        for (int physical = 1; physical < projects; physical++) {
            chainOffsets[physical + 1] = physical;
            chainTargets[physical - 1] = physical - 1;
        }
        Arrays.fill(owners, 0);
        owners[fileSlots[0]] = projects - 1;
        var chain = M3ReactorImpactPlan.compute(snapshots[1], plans[1], owners,
                projects, true, chainOffsets, chainTargets);
        int[] selected = chain.impactedProjects();
        check(selected.length == projects, "deep chain propagates without recursive traversal");
        for (int at = 0; at < projects; at++) {
            check(selected[at] == projects - 1 - at, "topology is independent of physical slot order");
            check(chain.reasonAt(selected[at]) == (at == 0 ? M3ReactorImpactPlan.FILE_IMPACT
                            : M3ReactorImpactPlan.DEPENDENT_PROJECT),
                    "deep chain retains exact direct/dependent reasons");
        }
    }

    private static void observe(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }
}
