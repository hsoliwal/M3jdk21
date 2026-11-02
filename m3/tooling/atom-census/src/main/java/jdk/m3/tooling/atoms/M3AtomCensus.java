/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
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
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
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
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.m3.tooling.atoms.contract.M3AtomKind;

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
    /** Opt-in parse-once structural ledger, distinct from the legacy static-method census. */
    static final String SEMANTIC_HEADER = "atom_id\trepo\tpath\tkind\towner\tname\tsignature"
            + "\tstart_utf16\tend_utf16\tsource_sha256\tpattern_id\tpublic_contract_locked";

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

    /** A source-bounded semantic atom; NOT an admission or product proof. */
    record SemanticAtom(String repo, String path, M3AtomKind kind, String owner, String name,
            String signature, long start, long end, String sourceSha256, String pattern, int ordinal) {
        String atomId() {
            String stable = path + '\0' + owner + '\0' + kind + '\0' + name + '\0' + signature
                    + '\0' + ordinal;
            return "A" + digest(stable);
        }

        String row() {
            return String.join("\t", atomId(), repo, path, kind.name(), owner, name,
                    signature, Long.toString(start), Long.toString(end), sourceSha256, pattern, "true");
        }
    }

    /** Exact source-slice digest, not Git's blob object identity. */
    static String digest(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new AssertionError("SHA-256 required by JDK", unavailable);
        }
    }

    /** Deterministic, collision-refusing semantic candidate ledger. */
    static List<String> semanticLines(List<SemanticAtom> atoms) {
        List<SemanticAtom> sorted = atoms.stream().sorted(Comparator
                .comparing(SemanticAtom::path)
                .thenComparingLong(SemanticAtom::start)
                .thenComparingLong(SemanticAtom::end)
                .thenComparing(a -> a.kind().name())
                .thenComparing(SemanticAtom::name)
                .thenComparingInt(SemanticAtom::ordinal)).toList();
        List<String> lines = new ArrayList<>();
        lines.add(SEMANTIC_HEADER);
        Set<String> ids = new HashSet<>();
        for (SemanticAtom atom : sorted) {
            String row = atom.row();
            if (!ids.add(row.substring(0, row.indexOf('\t')))) {
                throw new IllegalArgumentException("duplicate semantic atom identity: " + atom.path());
            }
            lines.add(row);
        }
        return lines;
    }

    private M3AtomCensus() {
    }

    /**
     * Produces the historical static-method census, an opt-in semantic census,
     * or the same single-parse semantic census plus conservative JNI candidates.
     *
     * @param args optional {@code --semantic} or {@code --jni}, repository label,
     *             pattern rules, output directory and one or more source roots
     * @throws IOException when source discovery, javac parsing or output fails
     */
    public static void main(String[] args) throws IOException {
        boolean jni = args.length > 0 && args[0].equals("--jni");
        boolean semantic = jni || args.length > 0 && args[0].equals("--semantic");
        int start = semantic ? 1 : 0;
        if (args.length - start < 4) {
            throw new IllegalArgumentException("usage: [--semantic|--jni] <repo> <PATTERN_RULES.tsv> <out-dir> <source-root>...");
        }
        String repo = args[start];
        List<Rule> rules = rules(Path.of(args[start + 1]));
        List<Atom> atoms = new ArrayList<>();
        List<SemanticAtom> structural = semantic ? new ArrayList<>() : null;
        List<M3JniDeclarationProjection.Candidate> jniCandidates = jni ? new ArrayList<>() : null;
        for (int i = start + 3; i < args.length; i++) {
            atoms.addAll(census(repo, Path.of(args[i]), rules, structural, jniCandidates));
        }
        // All diagnostics, identities and inputs are validated before publishing any files.
        List<String> semanticRows = semantic ? semanticLines(structural) : null;
        List<String> jniRows = jni ? M3JniDeclarationProjection.lines(jniCandidates) : null;
        Path out = Files.createDirectories(Path.of(args[start + 2]));
        Files.write(out.resolve("ATOM_CENSUS_" + repo + ".tsv"), census(atoms), StandardCharsets.UTF_8);
        List<String> summary = summary(atoms);
        Files.write(out.resolve("PATTERN_SUMMARY_" + repo + ".tsv"), summary, StandardCharsets.UTF_8);
        if (semantic) {
            Files.write(out.resolve("SEMANTIC_CENSUS_" + repo + ".tsv"), semanticRows, StandardCharsets.UTF_8);
        }
        if (jni) {
            Files.write(out.resolve("JNI_DECLARATIONS_" + repo + ".tsv"), jniRows, StandardCharsets.UTF_8);
        }
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
        return census(repo, sourceRoot, rules, null);
    }

    /** Single javac parse for both legacy metrics and opt-in semantic source atoms. */
    static List<Atom> census(String repo, Path sourceRoot, List<Rule> rules,
            List<SemanticAtom> semantic) throws IOException {
        return census(repo, sourceRoot, rules, semantic, null);
    }

    /** Optional JNI declaration binding shares the exact same single javac parse. */
    static List<Atom> census(String repo, Path sourceRoot, List<Rule> rules,
            List<SemanticAtom> semantic, List<M3JniDeclarationProjection.Candidate> jni)
            throws IOException {
        Path root = sourceRoot.toAbsolutePath().normalize();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path file : walk.toList()) {
                if (Files.isSymbolicLink(file)
                        && (file.toString().endsWith(".java") || Files.isDirectory(file))) {
                    throw new IOException("symlinked Java input or source directory: " + file);
                }
                if (file.toString().endsWith(".java")) {
                    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                        throw new IOException("nonregular Java input: " + file);
                    }
                    files.add(file);
                }
            }
        }
        files.sort(Comparator.naturalOrder());
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        List<Atom> atoms = new ArrayList<>();
        try (StandardJavaFileManager fm = javac.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            for (int from = 0; from < files.size(); from += BATCH) {
                List<Path> batch = files.subList(from, Math.min(files.size(), from + BATCH));
                Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(batch);
                List<String> errors = new ArrayList<>();
                JavacTask task = (JavacTask) javac.getTask(null, fm, diagnostic -> {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        errors.add(diagnostic.getCode() + ":" + diagnostic.getLineNumber());
                    }
                }, List.of("-proc:none"), null, units);
                SourcePositions positions = Trees.instance(task).getSourcePositions();
                int parsed = 0;
                for (CompilationUnitTree unit : task.parse()) {
                    parsed++;
                    String path = root.relativize(Path.of(unit.getSourceFile().toUri()))
                            .toString().replace('\\', '/');
                    String text = unit.getSourceFile().getCharContent(true).toString();
                    new UnitScanner(repo, path, unit, text, positions, rules, atoms, semantic, jni)
                            .scan(unit, null);
                }
                if (!errors.isEmpty() || parsed != batch.size()) {
                    throw new IOException("incomplete javac source parse: batch=" + from
                            + " expected=" + batch.size() + " parsed=" + parsed + " errors=" + errors);
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
    private static final class UnitScanner extends TreePathScanner<Void, Void> {
        private final String repo;
        private final String path;
        private final CompilationUnitTree unit;
        private final String text;
        private final SourcePositions positions;
        private final List<Rule> rules;
        private final List<Atom> atoms;
        private final Deque<String> types = new ArrayDeque<>();
        private final Deque<String> semanticTypes = new ArrayDeque<>();
        private final List<SemanticAtom> semantic;
        private final List<M3JniDeclarationProjection.Candidate> jni;
        private final Deque<ClassTree> classes = new ArrayDeque<>();
        private final Deque<String> unresolvedClasses = new ArrayDeque<>();
        private final Map<String, Integer> occurrences = new HashMap<>();
        private int anonymousIndex;

        UnitScanner(String repo, String path, CompilationUnitTree unit, String text, SourcePositions positions,
                List<Rule> rules, List<Atom> atoms, List<SemanticAtom> semantic,
                List<M3JniDeclarationProjection.Candidate> jni) {
            this.repo = repo;
            this.path = path;
            this.unit = unit;
            this.text = text;
            this.positions = positions;
            this.rules = rules;
            this.atoms = atoms;
            this.semantic = semantic;
            this.jni = jni;
            if (semantic != null) {
                semantic.add(new SemanticAtom(repo, path, M3AtomKind.FILE_CONTAINER,
                        "<file>", path, "FILE", 0, text.length(), digest(text), UNKNOWN, 0));
            }
        }

        /** Bind the exact syntax-tree source slice, without re-parsing or fabricating gate evidence. */
        private SemanticAtom add(M3AtomKind kind, String name, String signature, Tree tree) {
            if (semantic == null) {
                return null;
            }
            long start = positions.getStartPosition(unit, tree);
            long end = positions.getEndPosition(unit, tree);
            if (start < 0 || end <= start || end > text.length()) {
                throw new IllegalArgumentException("unbound semantic source span: " + path + ":" + name);
            }
            String owner = String.join(".", semanticTypes);
            String label = name.replace('\t', ' ').replace('\n', ' ');
            String spec = signature.replace('\t', ' ').replace('\n', ' ');
            String key = owner + '\0' + kind.name() + '\0' + label + '\0' + spec;
            int ordinal = occurrences.merge(key, 1, Integer::sum);
            String source = text.substring((int) start, (int) end);
            String pattern = kind == M3AtomKind.JAVA_METHOD
                    ? rules.stream().filter(r -> r.matches(label, source))
                            .map(Rule::pattern).findFirst().orElse(UNKNOWN)
                    : UNKNOWN;
            SemanticAtom atom = new SemanticAtom(repo, path, kind,
                    owner.isEmpty() ? "<package>" : owner,
                    label, spec, start, end, digest(source), pattern, ordinal);
            semantic.add(atom);
            return atom;
        }

        @Override
        public Void visitClass(ClassTree tree, Void unused) {
            String name = tree.getSimpleName().toString();
            String semanticName = name.isEmpty() ? "<anonymous" + (++anonymousIndex) + ">" : name;
            // Existing static-method census deliberately uses javac's original empty
            // anonymous name. A separate semantic stack retains its opt-in identity.
            types.addLast(name);
            semanticTypes.addLast(semanticName);
            classes.addLast(tree);
            Tree parent = getCurrentPath().getParentPath() == null
                    ? null : getCurrentPath().getParentPath().getLeaf();
            String unresolved = name.isEmpty() ? "UNRESOLVED_ANONYMOUS_CLASS"
                    : parent != null && !(parent instanceof ClassTree)
                            && !(parent instanceof CompilationUnitTree)
                                    ? "UNRESOLVED_LOCAL_CLASS"
                                    : unresolvedClasses.peekLast();
            unresolvedClasses.addLast(unresolved == null ? "" : unresolved);
            add(M3AtomKind.DECLARATION_ONLY, semanticName, tree.getKind().name(), tree);
            super.visitClass(tree, unused);
            unresolvedClasses.removeLast();
            classes.removeLast();
            semanticTypes.removeLast();
            types.removeLast();
            return null;
        }

        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
            if (semantic != null && getCurrentPath().getParentPath() != null
                    && getCurrentPath().getParentPath().getLeaf() instanceof ClassTree) {
                add(M3AtomKind.DECLARATION_ONLY, tree.getName().toString(),
                        String.valueOf(tree.getType()), tree);
                if (tree.getInitializer() != null) {
                    add(M3AtomKind.JAVA_FIELD_INITIALIZER, tree.getName().toString(),
                            String.valueOf(tree.getType()), tree.getInitializer());
                }
            }
            return super.visitVariable(tree, unused);
        }

        @Override
        public Void visitBlock(BlockTree tree, Void unused) {
            if (semantic != null && getCurrentPath().getParentPath() != null
                    && getCurrentPath().getParentPath().getLeaf() instanceof ClassTree) {
                add(tree.isStatic() ? M3AtomKind.JAVA_STATIC_INITIALIZER
                        : M3AtomKind.JAVA_INSTANCE_INITIALIZER,
                        tree.isStatic() ? "<clinit>" : "<instance-init>", "()", tree);
            }
            return super.visitBlock(tree, unused);
        }

        @Override
        public Void visitLambdaExpression(LambdaExpressionTree tree, Void unused) {
            String parameters = tree.getParameters().stream()
                    .map(p -> p.getType() == null ? "var" : p.getType().toString())
                    .collect(Collectors.joining(",", "(", ")"));
            add(M3AtomKind.JAVA_LAMBDA, "<lambda>", parameters, tree);
            return super.visitLambdaExpression(tree, unused);
        }

        @Override
        public Void visitMethod(MethodTree tree, Void unused) {
            if (semantic != null) {
                M3AtomKind kind = tree.getBody() == null
                        ? (tree.getModifiers().getFlags().contains(Modifier.NATIVE)
                                ? M3AtomKind.JAVA_NATIVE_DECLARATION : M3AtomKind.DECLARATION_ONLY)
                        : (tree.getName().contentEquals("<init>")
                                ? M3AtomKind.JAVA_CONSTRUCTOR : M3AtomKind.JAVA_METHOD);
                String signature = tree.getParameters().stream().map(p -> String.valueOf(p.getType()))
                        .collect(Collectors.joining(",", "(", ")")) + String.valueOf(tree.getReturnType());
                SemanticAtom atom = add(kind, tree.getName().toString(), signature, tree);
                if (kind == M3AtomKind.JAVA_NATIVE_DECLARATION && jni != null) {
                    String unresolved = unresolvedClasses.peekLast();
                    String binary = (unit.getPackageName() == null ? ""
                            : unit.getPackageName().toString().replace('.', '/') + '/')
                            + String.join("$", semanticTypes);
                    String symbol = unresolved.isEmpty()
                            ? M3JniDeclarationProjection.shortName(binary, tree.getName().toString())
                            : "-";
                    long aliases = classes.peekLast().getMembers().stream()
                            .filter(member -> member instanceof MethodTree method
                                    && method.getName().contentEquals(tree.getName())).count();
                    String resolution = !unresolved.isEmpty() ? unresolved
                            : aliases > 1 ? "OVERLOAD_REQUIRES_JAVAC_H"
                                    : "SHORT_SYMBOL_CANDIDATE";
                    jni.add(new M3JniDeclarationProjection.Candidate(atom.atomId(), repo, path,
                            binary.replace('/', '.'), tree.getName().toString(), signature,
                            atom.start(), atom.end(), atom.sourceSha256(), symbol, resolution));
                }
            }
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
