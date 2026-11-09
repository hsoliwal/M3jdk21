// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
// Census tool (not shipped). LOMBOK-CENSUS v1: parses every manifest file with javac's own parser (the one AST; no
// symbol attribution, no annotation processing) and emits one row per Lombok annotation use, per lombok import and
// per lombok val/var local type. Annotation names resolve against LOMBOK-API v1 through the file's own imports
// (single-type imports shadow on-demand imports, JLS 6.4.1); a lombok name missing from the API is kept as
// UNKNOWN:<name>. Attribute values are normalized: literals and enum constants verbatim, of/exclude field lists as
// <field-list>, class literals as <class-list>, onX annotation lists as @Name, anything else as <expr>.
// Usage: java LombokCensus.java <manifest.tsv> <LOMBOK_API.tsv> <out.tsv>
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public final class LombokCensus {
    private static final String TAB = "\t";
    private static final String NL = "\n";
    private static final String NONE = "-";
    private static final String LOMBOK_DOT = "lombok.";
    private static final String STAR_SUFFIX = ".*";
    private static final String UNKNOWN = "UNKNOWN:";
    private static final String FIELD_LIST = "<field-list>";
    private static final String CLASS_LIST = "<class-list>";
    private static final String EXPR = "<expr>";
    private static final String CLASS_LITERAL = "class";
    private static final String VALUE_KEY = "value";
    private static final String ONX_KEY = "onX";
    private static final Set<String> FIELD_REF_KEYS = Set.of("of", "exclude");
    private static final Set<String> ONX_WRAPPERS = Set.of("__", "_");
    private static final Set<String> LOCAL_TYPES = Set.of("val", "var");
    private static final String KIND_ANNOTATION = "ANNOTATION";
    private static final String KIND_IMPORT = "IMPORT";
    private static final String KIND_LOCAL_TYPE = "LOCAL_TYPE";
    private static final String KIND_PARSE_ERROR = "PARSE_ERROR";
    private static final String HEADER = String.join(TAB, "origin", "module", "variant", "path", "line", "kind", "item",
            "target", "attributes", "owner", "member");
    private static final int MANIFEST_COLUMNS = 7;
    private static final int API_FQN = 0;
    private static final int API_KIND = 1;
    private static final List<String> JAVAC_OPTIONS = List.of("-proc:none", "-implicit:none", "-Xlint:none",
            "-encoding", "UTF-8");

    record Source(String origin, String rev, String module, String variant, String path, String sha, Path staged) { }

    record Row(Source src, long line, String kind, String item, String target, String attributes, String owner,
               String member) {
        String tsv() {
            return String.join(TAB, src.origin(), src.module(), src.variant(), src.path(), Long.toString(line), kind,
                    clean(item), target, clean(attributes), owner, member);
        }
    }

    private static final Comparator<Row> ORDER = Comparator.comparing((Row r) -> r.src().origin())
            .thenComparing(r -> r.src().path()).thenComparingLong(Row::line).thenComparing(Row::kind)
            .thenComparing(Row::item).thenComparing(Row::target).thenComparing(Row::attributes);

    private LombokCensus() { }

    public static void main(String[] args) throws IOException {
        List<Source> sources = readManifest(Path.of(args[0]));
        Set<String> api = readApi(Path.of(args[1]));
        List<Row> rows = scan(sources, api);
        rows.sort(ORDER);
        List<String> lines = new ArrayList<>(rows.size() + 1);
        lines.add(HEADER);
        rows.forEach(r -> lines.add(r.tsv()));
        Files.writeString(Path.of(args[2]), String.join(NL, lines) + NL, StandardCharsets.UTF_8);
        Map<String, Long> byKind = rows.stream().collect(Collectors.groupingBy(Row::kind, Collectors.counting()));
        System.out.println("LOMBOK-CENSUS v1 files=" + sources.size() + " rows=" + rows.size() + " " + byKind);
    }

    private static List<Source> readManifest(Path manifest) throws IOException {
        return Files.readAllLines(manifest, StandardCharsets.UTF_8).stream().skip(1).map(l -> l.split(TAB, -1))
                .filter(c -> c.length == MANIFEST_COLUMNS)
                .map(c -> new Source(c[0], c[1], c[2], c[3], c[4], c[5], Path.of(c[6]))).toList();
    }

    private static Set<String> readApi(Path api) throws IOException {
        return Files.readAllLines(api, StandardCharsets.UTF_8).stream().skip(1).map(l -> l.split(TAB, -1))
                .filter(c -> KIND_ANNOTATION.equals(c[API_KIND])).map(c -> c[API_FQN]).collect(Collectors.toSet());
    }

    private static List<Row> scan(List<Source> sources, Set<String> api) throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
        List<Row> rows = new ArrayList<>();
        // Identical blobs at several repository paths share one staged file: parse it once, emit a row per path.
        Map<Path, List<Source>> byPath = sources.stream().collect(Collectors.groupingBy(LombokCensus::key,
                LinkedHashMap::new, Collectors.toList()));
        try (StandardJavaFileManager fm = javac.getStandardFileManager(diags, Locale.ROOT, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjectsFromPaths(new ArrayList<>(byPath.keySet()));
            Map<URI, List<Source>> byUri = new HashMap<>();
            files.forEach(f -> byUri.put(f.toUri().normalize(), byPath.get(Path.of(f.toUri()).toAbsolutePath()
                    .normalize())));
            JavacTask task = (JavacTask) javac.getTask(null, fm, diags, JAVAC_OPTIONS, null, files);
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            for (CompilationUnitTree cu : task.parse()) {
                new Scan(byUri.get(cu.getSourceFile().toUri().normalize()), cu, positions, api, rows).scan(cu, null);
            }
            diags.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR && d.getSource() != null)
                    .forEach(d -> byUri.get(d.getSource().toUri().normalize()).forEach(s -> rows.add(new Row(s,
                            d.getLineNumber(), KIND_PARSE_ERROR, d.getCode(), NONE, NONE, NONE, NONE))));
        }
        return rows;
    }

    private static Path key(Source s) {
        return s.staged().toAbsolutePath().normalize();
    }

    static String clean(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    static String simple(String qualified) {
        return qualified.substring(qualified.lastIndexOf('.') + 1);
    }

    /** One compilation unit: imports first (the scanner visits them before the type declarations). */
    private static final class Scan extends TreePathScanner<Void, Void> {
        private final List<Source> srcs;
        private final CompilationUnitTree cu;
        private final SourcePositions positions;
        private final Set<String> api;
        private final List<Row> out;
        private final Map<String, String> singleImports = new HashMap<>();
        private final List<String> lombokStarPackages = new ArrayList<>();

        Scan(List<Source> srcs, CompilationUnitTree cu, SourcePositions positions, Set<String> api, List<Row> out) {
            this.srcs = srcs;
            this.cu = cu;
            this.positions = positions;
            this.api = api;
            this.out = out;
        }

        @Override
        public Void visitImport(ImportTree it, Void unused) {
            String q = it.getQualifiedIdentifier().toString();
            if (!it.isStatic() && q.endsWith(STAR_SUFFIX) && q.startsWith(LOMBOK_DOT)) {
                lombokStarPackages.add(q.substring(0, q.length() - STAR_SUFFIX.length()));
            } else if (!it.isStatic() && !q.endsWith(STAR_SUFFIX)) {
                singleImports.put(simple(q), q);
            }
            if (q.startsWith(LOMBOK_DOT)) {
                emit(it, KIND_IMPORT, q, it.isStatic() ? "static" : NONE, NONE, NONE, NONE);
            }
            return null;
        }

        @Override
        public Void visitAnnotation(AnnotationTree a, Void unused) {
            resolve(a.getAnnotationType().toString()).ifPresent(fqn -> emit(a, KIND_ANNOTATION,
                    api.contains(fqn) ? fqn : UNKNOWN + fqn, target(getCurrentPath()), attributes(a),
                    owner(getCurrentPath()), member(getCurrentPath())));
            return super.visitAnnotation(a, unused);
        }

        @Override
        public Void visitVariable(VariableTree v, Void unused) {
            if (v.getType() instanceof IdentifierTree id && LOCAL_TYPES.contains(id.getName().toString())) {
                resolve(id.getName().toString()).ifPresent(fqn -> emit(v, KIND_LOCAL_TYPE, fqn, "LOCAL_VARIABLE",
                        NONE, owner(getCurrentPath()), v.getName().toString()));
            }
            return super.visitVariable(v, unused);
        }

        private Optional<String> resolve(String written) {
            if (written.startsWith(LOMBOK_DOT)) {
                return Optional.of(written);
            }
            int dot = written.indexOf('.');
            String head = dot < 0 ? written : written.substring(0, dot);
            String imported = singleImports.get(head);
            if (imported != null) {
                return imported.startsWith(LOMBOK_DOT)
                        ? Optional.of(imported + (dot < 0 ? "" : written.substring(dot))) : Optional.empty();
            }
            return lombokStarPackages.stream().map(p -> p + "." + written).filter(api::contains).findFirst();
        }

        private void emit(Tree t, String kind, String item, String target, String attrs, String owner, String member) {
            long line = cu.getLineMap().getLineNumber(positions.getStartPosition(cu, t));
            srcs.forEach(src -> out.add(new Row(src, line, kind, item, target, attrs, owner, member)));
        }
    }

    static String target(TreePath annotationPath) {
        TreePath parent = annotationPath.getParentPath();
        if (parent.getLeaf() instanceof CompilationUnitTree) {
            return "PACKAGE";
        }
        if (!(parent.getLeaf() instanceof ModifiersTree)) {
            return "TYPE_USE";
        }
        TreePath decl = parent.getParentPath();
        Tree outer = decl.getParentPath() == null ? null : decl.getParentPath().getLeaf();
        return switch (decl.getLeaf()) {
            case ClassTree c -> "TYPE:" + c.getKind();
            case MethodTree m -> m.getReturnType() == null ? "CONSTRUCTOR" : "METHOD";
            case VariableTree v -> variableTarget(v, outer);
            default -> decl.getLeaf().getKind().toString();
        };
    }

    static String variableTarget(VariableTree v, Tree outer) {
        boolean isStatic = v.getModifiers().getFlags().contains(Modifier.STATIC);
        if (outer instanceof ClassTree c) {
            return c.getKind() == Tree.Kind.RECORD && !isStatic ? "RECORD_COMPONENT"
                    : isStatic ? "FIELD:static" : "FIELD";
        }
        return outer instanceof MethodTree || outer instanceof LambdaExpressionTree ? "PARAMETER" : "LOCAL_VARIABLE";
    }

    static String owner(TreePath path) {
        for (TreePath p = path; p != null; p = p.getParentPath()) {
            if (p.getLeaf() instanceof ClassTree c && p != path.getParentPath().getParentPath()) {
                return c.getSimpleName().toString();
            }
        }
        return NONE;
    }

    static String member(TreePath annotationPath) {
        TreePath parent = annotationPath.getParentPath();
        if (!(parent.getLeaf() instanceof ModifiersTree)) {
            return NONE;
        }
        return switch (parent.getParentPath().getLeaf()) {
            case ClassTree c -> c.getSimpleName().toString();
            case MethodTree m -> m.getName().toString();
            case VariableTree v -> v.getName().toString();
            default -> NONE;
        };
    }

    static String attributes(AnnotationTree a) {
        String joined = a.getArguments().stream().map(LombokCensus::attribute).sorted()
                .collect(Collectors.joining(";"));
        return joined.isEmpty() ? NONE : joined;
    }

    static String attribute(ExpressionTree e) {
        if (e instanceof AssignmentTree as) {
            String key = as.getVariable().toString();
            return key + "=" + value(key, as.getExpression());
        }
        return VALUE_KEY + "=" + value(VALUE_KEY, e);
    }

    static String value(String key, ExpressionTree e) {
        return switch (e.getKind()) {
            case BOOLEAN_LITERAL, INT_LITERAL, LONG_LITERAL, CHAR_LITERAL, IDENTIFIER -> e.toString();
            case STRING_LITERAL -> FIELD_REF_KEYS.contains(key) ? FIELD_LIST : e.toString();
            case MEMBER_SELECT -> memberSelect((MemberSelectTree) e);
            case NEW_ARRAY -> array(key, (NewArrayTree) e);
            case ANNOTATION, TYPE_ANNOTATION -> annotationValue((AnnotationTree) e);
            default -> EXPR;
        };
    }

    static String memberSelect(MemberSelectTree m) {
        if (m.getIdentifier().contentEquals(CLASS_LITERAL)) {
            return CLASS_LIST;
        }
        String s = m.toString();
        return s.startsWith(LOMBOK_DOT) ? s.substring(s.lastIndexOf('.', s.lastIndexOf('.') - 1) + 1) : s;
    }

    static String array(String key, NewArrayTree n) {
        if (FIELD_REF_KEYS.contains(key)) {
            return FIELD_LIST;
        }
        List<? extends ExpressionTree> init = n.getInitializers() == null ? List.of() : n.getInitializers();
        List<String> values = init.stream().map(x -> value(key, x)).distinct().sorted().toList();
        return values.equals(List.of(CLASS_LIST)) ? CLASS_LIST : "{" + String.join(",", values) + "}";
    }

    static String annotationValue(AnnotationTree a) {
        String type = a.getAnnotationType().toString();
        if (!ONX_WRAPPERS.contains(type)) {
            return "@" + simple(type);
        }
        Function<ExpressionTree, ExpressionTree> unwrap = x -> x instanceof AssignmentTree as ? as.getExpression() : x;
        return a.getArguments().stream().map(unwrap).map(x -> value(ONX_KEY, x)).collect(Collectors.joining(","));
    }
}
