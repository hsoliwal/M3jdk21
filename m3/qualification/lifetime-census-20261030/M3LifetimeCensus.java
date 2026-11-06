/*
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.internal.m3.synthesis.lifetime.AllocationFact;
import jdk.internal.m3.synthesis.lifetime.Escape;
import jdk.internal.m3.synthesis.lifetime.LifetimeComposer;
import jdk.internal.m3.synthesis.lifetime.M3MethodLifetimeSummary;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Atom (pattern COMPILE_TIME_LIFETIME_DOOP_CENSUS): runs the Synexia compile-time lifetime analyzer
 * (jdk.internal.m3.synthesis.lifetime, #10869: escape lattice, refcount actions) over a source tree and maps every
 * allocation site to a DOOP class. Rules (DOOP_RULES): exception types -> COLD_EXCEPTION; NONE -> STACK_LOCAL
 * (scalar-replace or reuse scratch); RETURNED -> CALLER_SLOT (caller-provided slot or lane); CALLEE -> BORROWED
 * (callee-scoped, pass down); FIELD -> OWNER_SCOPED (lifetime equals the owner: an SoA lane slot of the owner);
 * CAPTURED -> CLOSURE; MAY_ESCAPE -> KEEP_OBJECT. Methods named like collection hot paths are marked HOT.
 * Usage: java -cp lt-out;. M3LifetimeCensus <source root> <sites.tsv> <summary.tsv>
 */
public final class M3LifetimeCensus {
    private static final java.util.Set<String> HOT = java.util.Set.of("get", "put", "contains", "containsKey", "next",
            "hasNext", "compare", "compareTo", "hashCode", "equals", "add", "remove", "indexOf", "size", "iterator", "set");

    private M3LifetimeCensus() { }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args[0]);
        Map<String, String> sources = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> read(root, p, sources));
        }
        List<M3MethodLifetimeSummary> all = LifetimeComposer.compose(sources);
        Map<String, Long> byEscape = new TreeMap<>();
        Map<String, Long> byDoop = new TreeMap<>();
        Map<String, Long> hotByDoop = new TreeMap<>();
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of(args[1]), StandardCharsets.UTF_8))) {
            out.print("owner\tmethod\tline\ttype\tvariable\tescape\trc_action\tlast_use\tdoop_class\thot\n");
            all.forEach(s -> s.allocations().forEach(a -> {
                String doop = doop(a);
                boolean hot = HOT.contains(s.methodName());
                byEscape.merge(a.escape().name(), 1L, Long::sum);
                byDoop.merge(doop, 1L, Long::sum);
                if (hot) {
                    hotByDoop.merge(doop, 1L, Long::sum);
                }
                out.print(s.owner() + "\t" + s.methodName() + "\t" + a.line() + "\t" + a.type().replace('\t', ' ') + "\t"
                        + a.variable() + "\t" + a.escape() + "\t" + a.action() + "\t" + a.lastUseLine() + "\t" + doop
                        + "\t" + (hot ? "HOT" : "-") + "\n");
            }));
        }
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of(args[2]), StandardCharsets.UTF_8))) {
            out.print("kind\tkey\tcount\n");
            out.print("files\t-\t" + sources.size() + "\n" + "methods\t-\t" + all.size() + "\n");
            byEscape.forEach((k, v) -> out.print("escape\t" + k + "\t" + v + "\n"));
            byDoop.forEach((k, v) -> out.print("doop\t" + k + "\t" + v + "\n"));
            hotByDoop.forEach((k, v) -> out.print("hot_doop\t" + k + "\t" + v + "\n"));
        }
        System.out.println("files=" + sources.size() + " methods=" + all.size() + " escape=" + byEscape + " doop=" + byDoop
                + " hot=" + hotByDoop);
    }

    private static void read(Path root, Path p, Map<String, String> sources) {
        try {
            sources.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p, StandardCharsets.UTF_8));
        } catch (IOException | java.io.UncheckedIOException e) {
            System.err.println("skip " + p + ": " + e.getMessage());
        }
    }

    static String doop(AllocationFact a) {
        String t = a.type().replaceAll("<.*", "");
        if (t.endsWith("Exception") || t.endsWith("Error") || t.equals("M3NotYetReceived") || t.equals("Unsupported")) {
            return "COLD_EXCEPTION";   // throw sites and the scaffold's grep-able M3NotYetReceived markers
        }
        Escape e = a.escape();
        return switch (e) {
            case NONE -> "STACK_LOCAL";
            case RETURNED -> "CALLER_SLOT";
            case CALLEE -> "BORROWED";
            case FIELD -> "OWNER_SCOPED";
            case CAPTURED -> "CLOSURE";
            case MAY_ESCAPE -> "KEEP_OBJECT";
        };
    }
}
