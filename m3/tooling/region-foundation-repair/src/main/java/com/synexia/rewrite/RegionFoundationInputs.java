// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Task-specific candidate construction; actual delivery uses existing OpenRewrite owners. */
public final class RegionFoundationInputs {
    public static final String BASE = "855afe57a679aeb454052630de7beddcb25f5f8f";
    public static final String STRING = "src/java.base/share/classes/java/lang/String.java";
    public static final String WORKFLOW = ".github/workflows/m3-foundation.yml";
    public static final String MAPPING = "m3/docs/name-mapping.json";
    public static final String STRING_BEFORE = "b7a074c6f366397847538b6b1bc6a5e5fecfb94809ab70fda40ef58406595b13";
    public static final String WORKFLOW_BEFORE = "e3625a7f61ece03df4ac90102e45a299ed3f571ca889295da4899fcd8759d5ad";
    public static final String MAPPING_BEFORE = "fa9832d48ec7dc0334fcb3a02e1b665612ca3a9f3dfe13cd104ff3f2c42871ac";
    private static final String GUARD = "\n        // Preserve non-positive region semantics before M3 slicing or UTF-16 scaling.\n"
            + "        if (len <= 0) {\n            return true;\n        }";
    private RegionFoundationInputs() { }

    public record Pair(String path, String before, String after) { }

    /** Exact preimage qualification. No caller-supplied digest may widen this task. */
    public static List<Pair> candidates(Path base) throws IOException {
        String source = sealed(base, STRING, STRING_BEFORE);
        String workflow = sealed(base, WORKFLOW, WORKFLOW_BEFORE);
        String mapping = sealed(base, MAPPING, MAPPING_BEFORE);
        return List.of(new Pair(STRING, source, stringCandidate(source)),
                new Pair(WORKFLOW, workflow, workflowCandidate(workflow)),
                new Pair(MAPPING, mapping, mappingCandidate(mapping)));
    }

    public static String sealed(Path base, String path, String expected) throws IOException {
        String source = Files.readString(base.resolve(path), StandardCharsets.UTF_8);
        if (!sha256(source).equals(expected)) throw new IllegalStateException("preimage drift: " + path);
        return source;
    }

    /** Javac syntax positions select the two actual method bodies; payload text is opaque. */
    public static String stringCandidate(String source) throws IOException {
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaFileObject input = new SimpleJavaFileObject(URI.create("string:///String.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreErrors) { return source; }
        };
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler required");
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("--release", "21", "-proc:none"), null, List.of(input));
            CompilationUnitTree unit = task.parse().iterator().next();
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
                throw new IllegalArgumentException("invalid Java syntax");
            }
            var positions = Trees.instance(task).getSourcePositions();
            List<Integer> insertions = new ArrayList<>();
            int found = 0;
            for (Tree declaration : unit.getTypeDecls()) {
                if (!(declaration instanceof ClassTree type) || !type.getSimpleName().contentEquals("String")) continue;
                for (Tree member : type.getMembers()) {
                    if (!(member instanceof MethodTree method) || !method.getName().contentEquals("regionMatches")) continue;
                    int count = method.getParameters().size();
                    if (count != 4 && count != 5) continue;
                    found++;
                    if (method.getBody() == null) throw new IllegalArgumentException("missing region body");
                    List<? extends StatementTree> statements = method.getBody().getStatements();
                    int bound = count == 4 ? 0 : 1;
                    if (statements.size() <= bound + 1 || statements.get(bound).getKind() != Tree.Kind.IF) {
                        throw new IllegalArgumentException("bounds statement drift");
                    }
                    int end = Math.toIntExact(positions.getEndPosition(unit, statements.get(bound)));
                    String bounds = source.substring(Math.toIntExact(positions.getStartPosition(unit, statements.get(bound))), end);
                    if (!bounds.contains("(long)length() - len") || !bounds.contains("(long)other.length() - len")) {
                        throw new IllegalArgumentException("bounds contract drift");
                    }
                    StatementTree next = statements.get(bound + 1);
                    String nextText = source.substring(Math.toIntExact(positions.getStartPosition(unit, next)),
                            Math.toIntExact(positions.getEndPosition(unit, next)));
                    if (nextText.equals("if (len <= 0) {\n            return true;\n        }")) continue;
                    if (!(next instanceof VariableTree variable) || !variable.getName().contentEquals("leftM3")) {
                        throw new IllegalArgumentException("M3 boundary drift");
                    }
                    insertions.add(end);
                }
            }
            if (found != 2) throw new IllegalArgumentException("expected exactly two region overloads");
            insertions.sort(Comparator.reverseOrder());
            var candidate = new StringBuilder(source);
            for (int position : insertions) candidate.insert(position, GUARD);
            return candidate.toString();
        }
    }

    /** Exact workflow source, not a global escape normalizer. All other jobs remain byte-identical. */
    public static String workflowCandidate(String source) {
        String jobEnv = "    env:\n      TEMP: ${{ runner.temp }}\n      TMP: ${{ runner.temp }}\n      TMPDIR: ${{ runner.temp }}\n";
        String step = "      - name: Compile and execute the isolated owner contract\n        shell: bash\n";
        String stepEnv = "        env:\n          TEMP: ${{ runner.temp }}\n          TMP: ${{ runner.temp }}\n          TMPDIR: ${{ runner.temp }}\n";
        String candidate = replaceOne(source, jobEnv, "");
        candidate = replaceOne(candidate, step, step + stepEnv);
        int boundary = candidate.indexOf("  foundation:\n");
        if (boundary < 0) throw new IllegalArgumentException("foundation job missing");
        String first = candidate.substring(0, boundary);
        if (first.chars().filter(c -> c == '\\').count() != 10) throw new IllegalArgumentException("shell escape drift");
        first = first.replace("\\\\", "\\");
        return first + candidate.substring(boundary);
    }

    public static String mappingCandidate(String source) {
        return replaceOne(source, "  \"mappings\": [\n", "  \"mappings\": [\n" + """
                    {
                      "source": "OpenJDK JDK-8316879, jdk21u 6a4e4920193b1a2fe7780007f1613ba972433280",
                      "target": "java.lang.String.regionMatches (both overloads)",
                      "recipe": "com.synexia.rewrite.M3JdkRegionFoundationRepair",
                      "status": "source-bound repair candidate; matched M3JDK runtime admission pending",
                      "target_rule": "Preserve ordered bounds/null checks, then return true for non-positive lengths before M3 slices, facts or UTF-16 scaling; no public API or native ABI change",
                      "gate": "recipe refusal/fixed-point, Java21 regression spectrum, compact on/off, M3-backed jtreg, full matched runtime"
                    },
                """);
    }

    private static String replaceOne(String source, String before, String after) {
        int at = source.indexOf(before);
        if (at < 0 || source.indexOf(before, at + 1) >= 0) throw new IllegalArgumentException("ambiguous task anchor");
        return source.substring(0, at) + after + source.substring(at + before.length());
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    /** Candidate export only: never writes into the source checkout. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("BASE_CHECKOUT CANDIDATE_DIRECTORY");
        Path base = Path.of(args[0]).toRealPath();
        Path out = Path.of(args[1]).toAbsolutePath().normalize();
        if (out.startsWith(base) || base.startsWith(out)) throw new IllegalArgumentException("separate output directory required");
        List<Pair> pairs = candidates(base);
        StringBuilder seals = new StringBuilder("path\tbefore_sha256\tafter_sha256\n");
        for (Pair pair : pairs) {
            Path path = out.resolve(pair.path());
            Files.createDirectories(path.getParent());
            Files.writeString(path, pair.after(), StandardCharsets.UTF_8);
            seals.append(pair.path()).append('\t').append(sha256(pair.before())).append('\t')
                    .append(sha256(pair.after())).append('\n');
        }
        Files.writeString(out.resolve("manifest.tsv"), seals.toString(), StandardCharsets.UTF_8);
        System.out.print(seals);
    }
}
