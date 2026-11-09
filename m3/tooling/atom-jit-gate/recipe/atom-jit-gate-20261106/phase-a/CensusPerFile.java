// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Phase A NEC probe: runs the census CLI (--semantic) once per file so one refused file does not hide the rest.
// args: <PATTERN_RULES.tsv> <corpus root> <out dir> <source dir>...
import java.io.PrintStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public final class CensusPerFile {
    public static void main(String[] a) throws Exception {
        Path rules = Path.of(a[0]); Path root = Path.of(a[1]); Path out = Path.of(a[2]);
        List<Path> files = new ArrayList<>();
        for (int i = 3; i < a.length; i++) try (Stream<Path> w = Files.walk(root.resolve(a[i]))) {
            w.filter(p -> p.toString().endsWith(".java")).forEach(files::add); }
        files.sort(Comparator.naturalOrder());
        List<String> rows = new ArrayList<>(); List<String> refusals = new ArrayList<>();
        PrintStream saved = System.out;
        Path tmp = Files.createTempDirectory(out.toAbsolutePath().getParent(), "census-one-");
        int ok = 0;
        for (Path f : files) {
            String rel = root.relativize(f).toString().replace(java.io.File.separatorChar, '/');
            Path src = tmp.resolve("src"); Path dst = tmp.resolve("out");
            deleteTree(src); deleteTree(dst);
            Files.createDirectories(src.resolve(rel).getParent());
            Files.copy(f, src.resolve(rel));
            try {
                System.setOut(new PrintStream(OutputStream.nullOutputStream()));
                Class.forName("jdk.m3.tooling.atoms.M3AtomCensus").getMethod("main", String[].class)
                        .invoke(null, (Object) new String[] {"--semantic", "one", rules.toString(), dst.toString(), src.toString()});
                List<String> lines = Files.readAllLines(dst.resolve("SEMANTIC_CENSUS_one.tsv"), StandardCharsets.UTF_8);
                rows.addAll(lines.subList(1, lines.size()));
                ok++;
            } catch (java.lang.reflect.InvocationTargetException e) {
                refusals.add(rel + "\t" + e.getCause().getClass().getSimpleName() + "\t" + e.getCause().getMessage());
            } finally {
                System.setOut(saved);
            }
        }
        deleteTree(tmp);
        Collections.sort(rows); Collections.sort(refusals);
        Files.createDirectories(out);
        Files.write(out.resolve("CENSUS_ROWS.tsv"), rows, StandardCharsets.UTF_8);
        Files.write(out.resolve("CENSUS_REFUSALS.tsv"), refusals, StandardCharsets.UTF_8);
        System.out.println("FILES=" + files.size() + " CENSUS_OK=" + ok + " CENSUS_REFUSED=" + refusals.size() + " ROWS=" + rows.size());
    }

    static void deleteTree(Path p) throws Exception {
        if (!Files.exists(p)) return;
        try (Stream<Path> w = Files.walk(p)) {
            for (Path q : w.sorted(Comparator.reverseOrder()).toList()) Files.delete(q);
        }
    }
}
