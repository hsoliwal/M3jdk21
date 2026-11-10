/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Atom census of any Java repository (pass 1 of atomize, JIT, patternize): every static method with a body is an
 * atom candidate. javac's own parser reads the sources (no second AST, no attribution, so any source root parses,
 * java.base included). Per atom: statements, loops, calls, purity (no field write, no synchronized) and whether it
 * writes array elements; its pattern is the first row of PATTERN_RULES.tsv whose name and body expressions match,
 * else UNKNOWN_PATTERN. Output: ATOM_CENSUS_&lt;repo&gt;.tsv and PATTERN_SUMMARY_&lt;repo&gt;.tsv, sorted, so a second run
 * over the same sources is byte-identical.
 *
 * <p>CLI: {@code java jdk.m3.tooling.atoms.M3AtomCensus <repo> <PATTERN_RULES.tsv> <out-dir> <source-root>...}
 */
public final class M3AtomCensus {

    static final String UNKNOWN = "UNKNOWN_PATTERN";
    static final String ANY = "-";
    static final String CENSUS_HEADER = "repo\tpath\ttype\tmethod\tsignature\tstatements\tloops\tcalls\tpure"
            + "\twrites_arrays\tpattern";
    private static final int BATCH = 200;

    /** One row of PATTERN_RULES.tsv: the pattern, a method-name expression and a body expression ({@code -}: any). */
    record Rule(String pattern, Pattern name, Pattern body) {
        boolean matches(String method, String source) {
            return (name == null || name.matcher(method).find()) && (body == null || body.matcher(source).find());
        }
    }

    /** One atom candidate. */
    record Atom(String repo, String path, String type, String method, String signature, int statements, int loops,
            int calls, boolean pure, boolean writesArrays, String pattern) {
        String row() {
            return String.join("\t", repo, path, type, method, signature, Integer.toString(statements),
                    Integer.toString(loops), Integer.toString(calls), Boolean.toString(pure),
                    Boolean.toString(writesArrays), pattern);
        }
    }

    private M3AtomCensus() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            throw new IllegalArgumentException("usage: <repo> <PATTERN_RULES.tsv> <out-dir> <source-root>...");
        }
        List<Rule> rules = rules(Path.of(args[1]));
        List<Atom> atoms = new ArrayList<>();
        for (int i = 3; i < args.length; i++) {
            atoms.addAll(census(args[0], Path.of(args[i]), rules));
        }
        Path out = Files.createDirectories(Path.of(args[2]));
        Files.write(out.resolve("ATOM_CENSUS_" + args[0] + ".tsv"), census(atoms), StandardCharsets.UTF_8);
        List<String> summary = summary(atoms);
        Files.write(out.resolve("PATTERN_SUMMARY_" + args[0] + ".tsv"), summary, StandardCharsets.UTF_8);
        summary.forEach(System.out::println);
    }

    /** The rules of the table, in order; the first column names the pattern. */
    static List<Rule> rules(Path tsv) throws IOException {
        List<Rule> rules = new ArrayList<>();
        List<String> lines = Files.readAllLines(tsv, StandardCharsets.UTF_8);
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            rules.add(new Rule(cells[0], ANY.equals(cells[1]) ? null : Pattern.compile(cells[1]),
                    ANY.equals(cells[2]) ? null : Pattern.compile(cells[2])));
        }
        return rules;
    }

    /** The atom candidates of every .java file under ROOT, in path and source order. */
    static List<Atom> census(String repo, Path sourceRoot, List<Rule> rules) throws IOException {
        Path root = sourceRoot.toAbsolutePath().normalize();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        List<Atom> atoms = new ArrayList<>();
        try (StandardJavaFileManager fm = javac.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            for (int from = 0; from < files.size(); from += BATCH) {
                List<Path> batch = files.subList(from, Math.min(files.size(), from + BATCH));
                Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(batch);
                JavacTask task = (JavacTask) javac.getTask(null, fm, d -> { }, List.of("-proc:none"), null, units);
                SourcePositions positions = Trees.instance(task).getSourcePositions();
                for (CompilationUnitTree unit : task.parse()) {
                    String path = root.relativize(Path.of(unit.getSourceFile().toUri())).toString().replace('\\', '/');
                    String text = unit.getSourceFile().getCharContent(true).toString();
                    new UnitScanner(repo, path, unit, text, positions, rules, atoms).scan(unit, null);
                }
            }
        }
        return atoms;
    }

    static List<String> census(List<Atom> atoms) {
        List<String> lines = new ArrayList<>();
        lines.add(CENSUS_HEADER);
        atoms.forEach(a -> lines.add(a.row()));
        return lines;
    }

    /** Atoms per pattern, most frequent first, with the share of all atoms and of the pure ones. */
    static List<String> summary(List<Atom> atoms) {
        Map<String, int[]> counts = new TreeMap<>();
        for (Atom a : atoms) {
            int[] c = counts.computeIfAbsent(a.pattern(), k -> new int[2]);
            c[0]++;
            c[1] += a.pure() ? 1 : 0;
        }
        List<String> lines = new ArrayList<>();
        lines.add("pattern\tatoms\tpure\tshare_percent");
        counts.entrySet().stream()
                .sorted((x, y) -> x.getValue()[0] != y.getValue()[0] ? y.getValue()[0] - x.getValue()[0]
                        : x.getKey().compareTo(y.getKey()))
                .forEach(e -> lines.add(e.getKey() + "\t" + e.getValue()[0] + "\t" + e.getValue()[1] + "\t"
                        + String.format(java.util.Locale.ROOT, "%.1f", 100.0 * e.getValue()[0] / Math.max(1, atoms.size()))));
        lines.add("TOTAL\t" + atoms.size() + "\t" + atoms.stream().filter(Atom::pure).count() + "\t100.0");
        return lines;
    }

    /** Walks one compilation unit: the enclosing type names, and each static method with a body. */
    private static final class UnitScanner extends TreeScanner<Void, Void> {
        private final String repo;
        private final String path;
        private final CompilationUnitTree unit;
        private final String text;
        private final SourcePositions positions;
        private final List<Rule> rules;
        private final List<Atom> atoms;
        private final Deque<String> types = new ArrayDeque<>();

        UnitScanner(String repo, String path, CompilationUnitTree unit, String text, SourcePositions positions,
                List<Rule> rules, List<Atom> atoms) {
            this.repo = repo;
            this.path = path;
            this.unit = unit;
            this.text = text;
            this.positions = positions;
            this.rules = rules;
            this.atoms = atoms;
        }

        @Override
        public Void visitClass(ClassTree tree, Void unused) {
            types.addLast(tree.getSimpleName().toString());
            super.visitClass(tree, unused);
            types.removeLast();
            return null;
        }

        @Override
        public Void visitMethod(MethodTree tree, Void unused) {
            if (tree.getBody() != null && tree.getModifiers().getFlags().contains(Modifier.STATIC)) {
                BodyScanner body = new BodyScanner(tree);
                body.scan(tree.getBody(), null);
                long start = positions.getStartPosition(unit, tree);
                long end = positions.getEndPosition(unit, tree);
                String source = start >= 0 && end > start ? text.substring((int) start, (int) end) : "";
                String method = tree.getName().toString();
                String pattern = rules.stream().filter(r -> r.matches(method, source)).map(Rule::pattern)
                        .findFirst().orElse(UNKNOWN);
                String signature = tree.getParameters().stream().map(p -> p.getType().toString())
                        .collect(Collectors.joining(",", "(", ")")) + String.valueOf(tree.getReturnType());
                boolean pure = !body.writesFields && !body.synchronizes
                        && !tree.getModifiers().getFlags().contains(Modifier.SYNCHRONIZED);
                atoms.add(new Atom(repo, path, String.join(".", types), method, signature.replace('\t', ' '),
                        body.statements, body.loops, body.calls, pure, body.writesArrays, pattern));
            }
            return super.visitMethod(tree, unused);
        }
    }

    /** Measures one method body: statements, loops, calls, and writes to fields or array elements. */
    private static final class BodyScanner extends TreeScanner<Void, Void> {
        private final Set<String> locals = new HashSet<>();
        int statements;
        int loops;
        int calls;
        boolean writesFields;
        boolean writesArrays;
        boolean synchronizes;

        BodyScanner(MethodTree method) {
            method.getParameters().forEach(p -> locals.add(p.getName().toString()));
        }

        @Override
        public Void scan(Tree tree, Void unused) {
            if (tree instanceof StatementTree) {
                statements++;
            }
            if (tree instanceof ForLoopTree || tree instanceof EnhancedForLoopTree || tree instanceof WhileLoopTree
                    || tree instanceof DoWhileLoopTree) {
                loops++;
            }
            return super.scan(tree, unused);
        }

        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
            locals.add(tree.getName().toString());
            return super.visitVariable(tree, unused);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
            calls++;
            return super.visitMethodInvocation(tree, unused);
        }

        @Override
        public Void visitSynchronized(SynchronizedTree tree, Void unused) {
            synchronizes = true;
            return super.visitSynchronized(tree, unused);
        }

        @Override
        public Void visitAssignment(AssignmentTree tree, Void unused) {
            target(tree.getVariable());
            return super.visitAssignment(tree, unused);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void unused) {
            target(tree.getVariable());
            return super.visitCompoundAssignment(tree, unused);
        }

        @Override
        public Void visitUnary(UnaryTree tree, Void unused) {
            switch (tree.getKind()) {
                case PREFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_INCREMENT, POSTFIX_DECREMENT -> target(tree.getExpression());
                default -> { }
            }
            return super.visitUnary(tree, unused);
        }

        /** A write: to an array element, to a local or parameter (no effect), or to a field (an effect). */
        private void target(ExpressionTree variable) {
            switch (variable.getKind()) {
                case ARRAY_ACCESS -> writesArrays = true;
                case IDENTIFIER -> writesFields |= !locals.contains(((IdentifierTree) variable).getName().toString());
                default -> writesFields = true; // a member select (this.f, Type.f, o.f) or anything else
            }
        }
    }
}
