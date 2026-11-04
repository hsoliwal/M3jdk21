/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jacoco.cli.internal.core.analysis.Analyzer;
import org.jacoco.cli.internal.core.analysis.CoverageBuilder;
import org.jacoco.cli.internal.core.analysis.IClassCoverage;
import org.jacoco.cli.internal.core.analysis.IMethodCoverage;
import org.jacoco.cli.internal.core.tools.ExecFileLoader;

/**
 * REACH gate, analysis side: for every atom of ATOMS.tsv, loads the execution data its own dynamic test wrote
 * (ReachListener) and checks with the JaCoCo analyzer (shaded in jacococli-nodeps) that the atom's method executed
 * at least one instruction in that test. An interface method has no code: {@code M3RowOffsets} is checked through
 * its implementation {@code M3PrefixRowOffsets}; {@code M3SorterLanes.Host} is implemented by the test, which asserts
 * the call itself. Usage: ReachAnalyzer patch_classes reach_dir ATOMS.tsv ATOM_SPEC.tsv REACH.tsv
 */
public final class ReachAnalyzer {

    private ReachAnalyzer() {
    }

    record Atom(String id, String pkg, String file, String owner, String name, String kind, List<String> params) {
    }

    public static void main(String[] args) throws IOException {
        Path patch = Path.of(args[0]);
        Path reach = Path.of(args[1]);
        List<Atom> atoms = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(args[2]), StandardCharsets.UTF_8)) {
            String[] f = line.split("\t", -1);
            if (line.startsWith("#") || f[0].equals("atom_id")) {
                continue;
            }
            List<String> params = new ArrayList<>();
            if (!f[7].equals("-")) {
                for (String p : f[7].split(",(?![^<]*>)")) {
                    params.add(p.replaceAll("<.*>", ""));
                }
            }
            atoms.add(new Atom(f[0], f[1].substring(f[1].lastIndexOf('.') + 1), f[2], f[3], f[4], f[5], params));
        }
        Map<String, String[]> spec = new HashMap<>();
        for (String line : Files.readAllLines(Path.of(args[3]), StandardCharsets.UTF_8)) {
            String[] f = line.split("\t", -1);
            if (!f[0].equals("atom_id")) {
                spec.put(f[0], f);
            }
        }
        int reached = 0;
        int interfaces = 0;
        int missing = 0;
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Path.of(args[4]), StandardCharsets.UTF_8))) {
            w.print("atom\ttest\ttarget\tcovered_instructions\tinstructions\tmethods_reached_in_test\tverdict\n");
            for (Atom a : atoms) {
                String[] s = spec.get(a.id());
                String cls = s[2].substring(s[2].indexOf('/') + 1);
                String exec = (cls + "__" + s[3]).replaceAll("[^A-Za-z0-9_.()#,$-]", "_") + ".exec";
                if (a.owner().endsWith(".Host")) {
                    interfaces++;
                    w.printf("%s\t%s\t%s\t-\t-\t-\tINTERFACE (implemented by the test, which asserts the call)\n", a.id(),
                            exec, a.owner() + "#" + a.name());
                    continue;
                }
                File file = reach.resolve(exec).toFile();
                if (!file.isFile()) {
                    missing++;
                    w.printf("%s\t%s\t-\t-\t-\t-\tNO_EXEC_DATA\n", a.id(), exec);
                    continue;
                }
                ExecFileLoader loader = new ExecFileLoader();
                loader.load(file);
                CoverageBuilder builder = new CoverageBuilder();
                new Analyzer(loader.getExecutionDataStore(), builder).analyzeAll(patch.resolve("sun/swing/" + a.pkg()).toFile());
                String target = internalName(a);
                IMethodCoverage hit = null;
                int methodsReached = 0;
                for (IClassCoverage c : builder.getClasses()) {
                    for (IMethodCoverage m : c.getMethods()) {
                        if (m.getInstructionCounter().getCoveredCount() > 0) {
                            methodsReached++;
                        }
                        if (c.getName().equals(target) && m.getName().equals(methodName(a)) && matches(a, c.getName(), m.getDesc())) {
                            hit = m;
                        }
                    }
                }
                boolean ok = hit != null && hit.getInstructionCounter().getCoveredCount() > 0;
                if (ok) {
                    reached++;
                } else {
                    missing++;
                }
                if (a.file().equals("M3RowOffsets") && ok) {
                    interfaces++;
                }
                w.printf("%s\t%s\t%s\t%s\t%s\t%d\t%s\n", a.id(), exec, target + "::" + methodName(a) + (hit == null ? "" : hit.getDesc()),
                        hit == null ? "-" : hit.getInstructionCounter().getCoveredCount(),
                        hit == null ? "-" : hit.getInstructionCounter().getTotalCount(), methodsReached,
                        ok ? (a.file().equals("M3RowOffsets") ? "REACHED (implementation M3PrefixRowOffsets)" : "REACHED")
                                : (hit == null ? "NO_METHOD" : "NOT_REACHED"));
            }
        }
        System.out.printf("reach atoms=%d reached=%d interface_rows=%d not_reached=%d%n", atoms.size(), reached, interfaces,
                missing);
        if (missing > 0) {
            System.exit(1);
        }
    }

    static String internalName(Atom a) {
        String[] parts = a.owner().split("\\.");
        String top = a.file().equals("M3RowOffsets") ? "M3PrefixRowOffsets" : parts[0];
        StringBuilder b = new StringBuilder("sun/swing/").append(a.pkg()).append('/').append(top);
        for (int i = 1; i < parts.length; i++) {
            b.append('$').append(parts[i].equals("<anonymous>") ? "1" : parts[i]);
        }
        return b.toString();
    }

    static String methodName(Atom a) {
        return a.kind().equals("constructor") ? "<init>" : a.name();
    }

    /** Parameter types of {@code desc} by simple name; an inner-class constructor's outer instance is skipped. */
    static boolean matches(Atom a, String cls, String desc) {
        List<String> types = new ArrayList<>();
        int i = 1;
        while (desc.charAt(i) != ')') {
            int dims = 0;
            while (desc.charAt(i) == '[') {
                dims++;
                i++;
            }
            String t;
            char c = desc.charAt(i);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                String name = desc.substring(i + 1, end);
                t = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('$')) + 1);
                i = end + 1;
            } else {
                t = switch (c) {
                    case 'I' -> "int";
                    case 'J' -> "long";
                    case 'Z' -> "boolean";
                    case 'B' -> "byte";
                    case 'C' -> "char";
                    case 'S' -> "short";
                    case 'F' -> "float";
                    default -> "double";
                };
                i++;
            }
            types.add(t + "[]".repeat(dims));
        }
        if (types.equals(a.params())) {
            return true;
        }
        String outer = cls.contains("$") ? cls.substring(cls.lastIndexOf('/') + 1, cls.lastIndexOf('$')) : "";
        return !types.isEmpty() && types.get(0).equals(outer) && types.subList(1, types.size()).equals(a.params());
    }
}
