// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
// Pattern tool (not shipped). LOMBOK-PATTERN v1: parses each representative source and its delombok expansion with
// javac's parser (one AST) and emits the generated surface per case: added types, fields, methods and constructors
// (signature, annotations, body), changed modifiers and changed bodies. Rows that a case's baseline case also
// produces are removed (so @With is measured on top of @AllArgsConstructor, not with it). A row naming exactly one
// representative field is generalized: field name -> ${f}, capitalized -> ${F}, its declared type in the signature
// -> ${T} (primitive boolean kept, it selects is-getters); the representative class name -> ${Type} everywhere.
// Usage: java PatternDiff.java <cases.tsv> <in_root> <out_root> <profile> <out.tsv>
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public final class PatternDiff {
    private static final String TAB = "\t";
    private static final String NL = "\n";
    private static final String NONE = "-";
    private static final String TYPE_TOKEN = "${Type}";
    private static final String FIELD_TOKEN = "${f}";
    private static final String FIELD_CAP_TOKEN = "${F}";
    private static final String FIELD_TYPE_TOKEN = "${T}";
    private static final String BOOLEAN = "boolean";
    private static final String REP_DIR = "rep";
    private static final String JAVA = ".java";
    private static final String CTOR = "<init>";
    private static final String ARROW = " -> ";
    private static final String ID = "(?<![A-Za-z0-9_$])";
    private static final String ID_END = "(?![A-Za-z0-9_$])";
    private static final String QUALIFIED_ID = "(?<![A-Za-z0-9_$.])";
    private static final String CAP_END = "(?![a-z0-9_$])";
    private static final List<String> JAVAC_OPTIONS = List.of("-proc:none", "-implicit:none", "-Xlint:none",
            "-encoding", "UTF-8");
    private static final String HEADER = String.join(TAB, "profile", "case", "census_keys", "owner", "kind", "change",
            "fields", "signature", "annotations", "body");
    private static final int CASE_COLUMNS = 3;

    record Case(String id, String keys, String baseline) { }

    record Member(String kind, String modifiers, String annotations, String signature, String body) { }

    record Row(String owner, String kind, String change, String signature, String annotations, String body) { }

    record Field(String name, String type) { }

    private PatternDiff() { }

    public static void main(String[] args) throws IOException {
        List<Case> cases = Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8).stream().skip(1)
                .map(l -> l.split(TAB, -1)).filter(c -> c.length == CASE_COLUMNS)
                .map(c -> new Case(c[0], c[1], c[2])).toList();
        Map<String, CompilationUnitTree> in = parse(Path.of(args[1]), cases);
        Map<String, CompilationUnitTree> out = parse(Path.of(args[2]), cases);
        Map<String, List<Row>> raw = new TreeMap<>();
        cases.forEach(c -> raw.put(c.id(), normalize(c.id(), diff(in.get(c.id()), out.get(c.id())))));
        List<String> lines = new ArrayList<>(List.of(HEADER));
        for (Case c : cases) {
            List<Row> rows = new ArrayList<>(raw.get(c.id()));
            if (!c.baseline().isEmpty()) {
                rows.removeAll(Set.copyOf(raw.get(c.baseline())));
            }
            lines.addAll(generalize(args[3], c, rows, fields(in.get(c.id()))));
        }
        Files.writeString(Path.of(args[4]), String.join(NL, lines) + NL, StandardCharsets.UTF_8);
        System.out.println("LOMBOK-PATTERN v1 profile=" + args[3] + " cases=" + cases.size() + " rows="
                + (lines.size() - 1));
    }

    private static Map<String, CompilationUnitTree> parse(Path root, List<Case> cases) throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        Map<String, CompilationUnitTree> units = new LinkedHashMap<>();
        try (StandardJavaFileManager fm = javac.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<Path> paths = cases.stream().map(c -> root.resolve(REP_DIR).resolve(c.id() + JAVA)).toList();
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjectsFromPaths(paths);
            JavacTask task = (JavacTask) javac.getTask(null, fm, null, JAVAC_OPTIONS, null, files);
            for (CompilationUnitTree cu : task.parse()) {
                String name = Path.of(cu.getSourceFile().toUri()).getFileName().toString();
                units.put(name.substring(0, name.length() - JAVA.length()), cu);
            }
        }
        return units;
    }

    static List<Row> diff(CompilationUnitTree in, CompilationUnitTree out) {
        Map<String, ClassTree> inTypes = types(in);
        List<Row> rows = new ArrayList<>();
        types(out).forEach((path, o) -> {
            ClassTree i = inTypes.get(path);
            if (i == null) {
                rows.add(new Row(path, "TYPE", "ADDED", typeSignature(o), annotations(o.getModifiers()), ""));
                members(o).values().forEach(m -> rows.add(added(path, m)));
            } else {
                rows.addAll(changed(path, i, o));
            }
        });
        return rows;
    }

    static List<Row> changed(String path, ClassTree i, ClassTree o) {
        List<Row> rows = new ArrayList<>();
        String before = modifiers(i.getModifiers());
        String after = modifiers(o.getModifiers());
        if (!before.equals(after)) {
            rows.add(new Row(path, "TYPE", "MODIFIERS", typeSignature(o), "", before + ARROW + after));
        }
        Map<String, Member> im = members(i);
        members(o).forEach((key, m) -> {
            Member old = im.get(key);
            if (old == null) {
                rows.add(added(path, m));
                return;
            }
            if (!old.modifiers().equals(m.modifiers())) {
                rows.add(new Row(path, m.kind(), "MODIFIERS", m.signature(), "", old.modifiers() + ARROW
                        + m.modifiers()));
            }
            if (!old.body().equals(m.body())) {
                rows.add(new Row(path, m.kind(), "BODY", m.signature(), m.annotations(), m.body()));
            }
        });
        return rows;
    }

    static Row added(String path, Member m) {
        return new Row(path, m.kind(), "ADDED", join(m.modifiers(), m.signature()), m.annotations(), m.body());
    }

    static Map<String, ClassTree> types(CompilationUnitTree cu) {
        Map<String, ClassTree> types = new LinkedHashMap<>();
        cu.getTypeDecls().stream().filter(ClassTree.class::isInstance).map(ClassTree.class::cast)
                .forEach(c -> collect(c, c.getSimpleName().toString(), types));
        return types;
    }

    static void collect(ClassTree c, String path, Map<String, ClassTree> types) {
        types.put(path, c);
        c.getMembers().stream().filter(ClassTree.class::isInstance).map(ClassTree.class::cast)
                .forEach(n -> collect(n, path + "." + n.getSimpleName(), types));
    }

    static Map<String, Member> members(ClassTree c) {
        Map<String, Member> members = new LinkedHashMap<>();
        for (Tree t : c.getMembers()) {
            if (t instanceof VariableTree v) {
                members.put("F:" + v.getName(), new Member("FIELD", modifiers(v.getModifiers()),
                        annotations(v.getModifiers()), clean(v.getType() + " " + v.getName()),
                        v.getInitializer() == null ? "" : clean(v.getInitializer().toString())));
            } else if (t instanceof MethodTree m) {
                members.put("M:" + methodKey(m, c), method(m, c));
            }
        }
        return members;
    }

    static String methodKey(MethodTree m, ClassTree owner) {
        return name(m, owner) + "(" + m.getParameters().stream().map(p -> clean(p.getType().toString()))
                .collect(Collectors.joining(",")) + ")";
    }

    static Member method(MethodTree m, ClassTree owner) {
        boolean ctor = m.getReturnType() == null;
        String typeParams = m.getTypeParameters().isEmpty() ? "" : "<" + m.getTypeParameters().stream()
                .map(Object::toString).collect(Collectors.joining(", ")) + "> ";
        String params = m.getParameters().stream().map(p -> clean(p.toString())).collect(Collectors.joining(", "));
        String thrown = m.getThrows().isEmpty() ? "" : " throws " + m.getThrows().stream().map(Object::toString)
                .collect(Collectors.joining(", "));
        String sig = typeParams + (ctor ? "" : m.getReturnType() + " ") + name(m, owner) + "(" + params + ")" + thrown;
        BlockTree body = m.getBody();
        return new Member(ctor ? "CONSTRUCTOR" : "METHOD", modifiers(m.getModifiers()), annotations(m.getModifiers()),
                clean(sig), body == null ? "" : clean(body.toString()));
    }

    static String name(MethodTree m, ClassTree owner) {
        return m.getName().contentEquals(CTOR) ? owner.getSimpleName().toString() : m.getName().toString();
    }

    static String typeSignature(ClassTree c) {
        String ext = c.getExtendsClause() == null ? "" : " extends " + c.getExtendsClause();
        String impl = c.getImplementsClause().isEmpty() ? "" : " implements " + c.getImplementsClause().stream()
                .map(Object::toString).collect(Collectors.joining(", "));
        return join(modifiers(c.getModifiers()), c.getKind().toString().toLowerCase(Locale.ROOT) + " "
                + c.getSimpleName() + ext + impl);
    }

    static String modifiers(ModifiersTree m) {
        return m.getFlags().stream().sorted().map(Object::toString).collect(Collectors.joining(" "));
    }

    static String annotations(ModifiersTree m) {
        return m.getAnnotations().stream().map(a -> clean(a.toString())).collect(Collectors.joining(" "));
    }

    static List<Field> fields(CompilationUnitTree in) {
        return types(in).values().stream().flatMap(c -> c.getMembers().stream()).filter(VariableTree.class::isInstance)
                .map(VariableTree.class::cast).filter(v -> v.getType() != null)
                .map(v -> new Field(v.getName().toString(), clean(v.getType().toString()))).toList();
    }

    static List<Row> normalize(String caseId, List<Row> rows) {
        return rows.stream().map(r -> new Row(typed(r.owner(), caseId), r.kind(), r.change(),
                typed(r.signature(), caseId), typed(r.annotations(), caseId), typed(r.body(), caseId))).toList();
    }

    static String typed(String s, String caseId) {
        return s.replaceAll("\\b" + caseId + "(?=[A-Za-z]|\\b)", Matcher.quoteReplacement(TYPE_TOKEN));
    }

    static List<String> generalize(String profile, Case c, List<Row> rows, List<Field> fields) {
        Map<Row, Set<String>> merged = new LinkedHashMap<>();
        for (Row r : rows) {
            List<Field> named = fields.stream().filter(f -> mentions(r, f)).toList();
            Row g = named.size() == 1 ? substitute(r, named.get(0)) : r;
            String fieldList = named.size() == 1 ? named.get(0).name() + ":" + named.get(0).type() : NONE;
            merged.computeIfAbsent(g, k -> new TreeSet<>()).add(fieldList);
        }
        return merged.entrySet().stream().map(e -> String.join(TAB, profile, c.id(), c.keys(), e.getKey().owner(),
                e.getKey().kind(), e.getKey().change(), fieldsCell(e.getValue()), e.getKey().signature(),
                orNone(e.getKey().annotations()), orNone(e.getKey().body()))).sorted(Comparator.naturalOrder())
                .toList();
    }

    static String fieldsCell(Set<String> fields) {
        Set<String> named = new TreeSet<>(fields);
        named.remove(NONE);
        return named.isEmpty() ? NONE : String.join(",", named);
    }

    static boolean mentions(Row r, Field f) {
        String text = r.signature() + " " + r.body();
        return Pattern.compile(ID + Pattern.quote(f.name()) + ID_END).matcher(text).find()
                || Pattern.compile(Pattern.quote(capitalized(f.name())) + CAP_END).matcher(text).find();
    }

    static Row substitute(Row r, Field f) {
        String typeRe = QUALIFIED_ID + Pattern.quote(f.type()) + ID_END;
        String sig = sub(r.signature(), f);
        String generalizedSig = BOOLEAN.equals(f.type()) ? sig
                : sig.replaceAll(typeRe, Matcher.quoteReplacement(FIELD_TYPE_TOKEN));
        return new Row(r.owner(), r.kind(), r.change(), generalizedSig, sub(r.annotations(), f), sub(r.body(), f));
    }

    static String sub(String s, Field f) {
        String named = s.replaceAll(ID + Pattern.quote(f.name()) + ID_END, Matcher.quoteReplacement(FIELD_TOKEN));
        return named.replaceAll(Pattern.quote(capitalized(f.name())) + CAP_END,
                Matcher.quoteReplacement(FIELD_CAP_TOKEN));
    }

    static String capitalized(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    static String join(String modifiers, String rest) {
        return modifiers.isEmpty() ? rest : modifiers + " " + rest;
    }

    static String orNone(String s) {
        return s.isEmpty() ? NONE : s;
    }

    static String clean(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }
}
