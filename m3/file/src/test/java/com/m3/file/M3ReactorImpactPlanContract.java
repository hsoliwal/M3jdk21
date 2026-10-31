// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
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
}
