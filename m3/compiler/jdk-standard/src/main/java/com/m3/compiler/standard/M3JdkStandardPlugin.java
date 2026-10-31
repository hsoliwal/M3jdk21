// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.compiler.standard;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;

/**
 * Opt-in javac plugin that holds M3-owned Java to the source standard of the JDK itself.
 *
 * <p>The checks run once per compilation unit on the parse tree javac has already built: Java
 * naming conventions, deterministic type/method quality checks (including no reflection parameter
 * names such as {@code arg0}), explicit imports, the 100-column limit and the jcheck whitespace
 * rules. Doc comments are left to javac's own {@code -Xdoclint:all/protected}, which is how the JDK
 * build checks them. Each finding is a compiler diagnostic tagged
 * {@code [M3JdkStandard RULE]}; the option {@code report=<path>} also writes every finding as a TSV
 * row (file, line, column, rule, message), and {@code mode=warning} lowers diagnostics from errors
 * to warnings. The option {@code scheme=m3} adds the M3JDK21 naming scheme: packages
 * {@code jdk.m3.<area>} or {@code jdk.internal.m3.<area>}, and no name carried over from Synexia
 * ({@code Synexia*}, {@code SYNEXIA_*} become {@code M3*}, {@code M3_*}).
 */
public final class M3JdkStandardPlugin implements Plugin {

    /** The checked rules; every diagnostic names exactly one. */
    enum Rule {
        WHITESPACE, LINE_LENGTH, WILDCARD_IMPORT, PACKAGE_NAME, TYPE_NAME, METHOD_NAME,
        NAME_QUALITY, CONSTANT_NAME, FIELD_NAME, PARAMETER_NAME, LOCAL_NAME,
        TYPE_PARAMETER_NAME, NAME_SCHEME
    }

    private static final int MAX_COLUMNS = 100;
    private static final Pattern PACKAGE = Pattern.compile("[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*");
    private static final Pattern TYPE = Pattern.compile("[A-Z][A-Za-z0-9]*");
    private static final Pattern MEMBER = Pattern.compile("[a-z][A-Za-z0-9]*");
    private static final Pattern CONSTANT = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");
    private static final Pattern TYPE_PARAMETER = Pattern.compile("[A-Z][0-9]?");
    private static final Pattern REFLECTION_NAME = Pattern.compile("arg[0-9]+");
    private static final Set<String> SERIAL_FIELDS =
            Set.of("serialVersionUID", "serialPersistentFields");
    /** M3JDK21 scheme: exported packages jdk.m3.area, internal packages jdk.internal.m3.area. */
    private static final Pattern M3_PACKAGE =
            Pattern.compile("jdk\\.(internal\\.)?m3(\\.[a-z][a-z0-9]*)*");
    /** Names carried over from Synexia: Synexia*, synexia-*, SYNEXIA_* become M3*, m3-*, M3_*. */
    private static final Pattern LEGACY_NAME = Pattern.compile("(?i).*synexia.*");

    private final List<String> rows = new ArrayList<>();
    private boolean m3Scheme;
    private Diagnostic.Kind kind = Diagnostic.Kind.ERROR;
    private Optional<Path> report = Optional.empty();
    private Trees trees;

    /** Creates the plugin; javac instantiates it through the service loader. */
    public M3JdkStandardPlugin() {
    }

    @Override
    public String getName() {
        return "M3JdkStandard";
    }

    @Override
    public void init(JavacTask task, String... args) {
        trees = Trees.instance(task);
        Arrays.stream(args).forEach(this::option);
        task.addTaskListener(new TaskListener() {
            @Override
            public void finished(TaskEvent event) {
                if (event.getKind() == TaskEvent.Kind.PARSE) {
                    new Unit(event.getCompilationUnit()).check();
                } else if (event.getKind() == TaskEvent.Kind.COMPILATION) {
                    report.ifPresent(M3JdkStandardPlugin.this::write);
                }
            }
        });
    }

    private void option(String arg) {
        if (arg.equals("mode=warning")) {
            kind = Diagnostic.Kind.WARNING;
        } else if (arg.equals("scheme=m3")) {
            m3Scheme = true;
        } else if (arg.startsWith("report=")) {
            report = Optional.of(Path.of(arg.substring("report=".length())));
        } else {
            throw new IllegalArgumentException("M3JdkStandard: unknown option " + arg);
        }
    }

    private void write(Path path) {
        String header = "file\tline\tcolumn\trule\tmessage\n";
        String body = rows.stream().sorted().map(row -> row + "\n").collect(Collectors.joining());
        try {
            Files.writeString(path, header + body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** One compilation unit: its text, its line map and the first tree that starts on each line. */
    private final class Unit extends TreePathScanner<Void, Void> {
        private final CompilationUnitTree root;
        private final SourcePositions positions;
        private final LineMap lineMap;
        private final TreeMap<Long, Tree> firstTreeOnLine = new TreeMap<>();

        Unit(CompilationUnitTree root) {
            this.root = root;
            this.positions = trees.getSourcePositions();
            this.lineMap = root.getLineMap();
        }

        void check() {
            Optional.ofNullable(root.getPackageName()).map(Object::toString)
                    .filter(name -> !PACKAGE.matcher(name).matches())
                    .ifPresent(name ->
                            flag(Rule.PACKAGE_NAME, root.getPackage(), "package " + name));
            Optional.ofNullable(root.getPackageName()).map(Object::toString)
                    .filter(name -> m3Scheme && !M3_PACKAGE.matcher(name).matches())
                    .ifPresent(name -> flag(Rule.NAME_SCHEME, root.getPackage(), "package " + name
                            + ": M3JDK21 packages are jdk.m3.<area> or jdk.internal.m3.<area>"));
            scan(new TreePath(root), null);
            String[] lines = text().split("\n", -1);
            IntStream.range(0, lines.length).forEach(i -> checkLine(i + 1L, lines[i]));
        }

        @Override
        public Void scan(Tree tree, Void unused) {
            Optional.ofNullable(tree).map(t -> positions.getStartPosition(root, t))
                    .filter(start -> start >= 0)
                    .map(lineMap::getLineNumber)
                    .ifPresent(line -> firstTreeOnLine.putIfAbsent(line, tree));
            return super.scan(tree, unused);
        }

        @Override
        public Void visitImport(ImportTree tree, Void unused) {
            if (tree.getQualifiedIdentifier().toString().endsWith(".*")) {
                flag(Rule.WILDCARD_IMPORT, tree, "import " + tree.getQualifiedIdentifier()
                        + ": import each type by name");
            }
            return super.visitImport(tree, unused);
        }

        @Override
        public Void visitClass(ClassTree tree, Void unused) {
            String name = tree.getSimpleName().toString();
            if (!name.isEmpty()) {
                name(Rule.TYPE_NAME, TYPE, name, tree);
                if (TYPE.matcher(name).matches()) {
                    checkNameQuality("type", name, tree, M3NameQuality.typeIssue(name));
                }
            }
            tree.getTypeParameters().forEach(this::typeParameter);
            return super.visitClass(tree, unused);
        }

        @Override
        public Void visitMethod(MethodTree tree, Void unused) {
            String name = tree.getName().toString();
            if (!name.equals("<init>")) {
                name(Rule.METHOD_NAME, MEMBER, name, tree);
                if (MEMBER.matcher(name).matches() && !overrides(tree)) {
                    checkNameQuality("method", name, tree, M3NameQuality.methodIssue(name));
                }
            }
            tree.getTypeParameters().forEach(this::typeParameter);
            return super.visitMethod(tree, unused);
        }

        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
            String name = tree.getName().toString();
            Tree parent = getCurrentPath().getParentPath().getLeaf();
            if (parent instanceof ClassTree owner) {
                field(owner, tree, name);
            } else if (parent instanceof MethodTree || parent instanceof LambdaExpressionTree) {
                parameter(tree, name);
            } else if (!name.equals("_")) {
                Rule rule = parent instanceof CatchTree ? Rule.PARAMETER_NAME : Rule.LOCAL_NAME;
                name(rule, MEMBER, name, tree);
            }
            return super.visitVariable(tree, unused);
        }

        private void field(ClassTree owner, VariableTree tree, String name) {
            Set<Modifier> flags = tree.getModifiers().getFlags();
            boolean constant = owner.getKind() == Tree.Kind.INTERFACE
                    || owner.getKind() == Tree.Kind.ANNOTATION_TYPE
                    || flags.containsAll(Set.of(Modifier.STATIC, Modifier.FINAL));
            if (!SERIAL_FIELDS.contains(name)) {
                Rule rule = constant ? Rule.CONSTANT_NAME : Rule.FIELD_NAME;
                name(rule, constant ? CONSTANT : MEMBER, name, tree);
            }
        }

        private void parameter(VariableTree tree, String name) {
            if (REFLECTION_NAME.matcher(name).matches()) {
                flag(Rule.PARAMETER_NAME, tree, "parameter " + name
                        + " is a reflection name: use the name the API declares");
            } else if (!name.equals("_")) {
                name(Rule.PARAMETER_NAME, MEMBER, name, tree);
            }
        }

        private void typeParameter(TypeParameterTree tree) {
            name(Rule.TYPE_PARAMETER_NAME, TYPE_PARAMETER, tree.getName().toString(), tree);
        }

        private boolean overrides(MethodTree tree) {
            return tree.getModifiers().getAnnotations().stream()
                    .map(annotation -> annotation.getAnnotationType().toString())
                    .anyMatch(annotation -> annotation.equals("Override")
                            || annotation.equals("java.lang.Override"));
        }

        private void checkNameQuality(String category, String name, Tree tree,
                Optional<String> issue) {
            issue.ifPresent(reason -> flag(Rule.NAME_QUALITY, tree,
                    category + " " + name + ": " + reason));
        }

        private void name(Rule rule, Pattern pattern, String name, Tree tree) {
            if (!pattern.matcher(name).matches()) {
                flag(rule, tree, name + " does not match " + pattern.pattern());
            }
            if (m3Scheme && LEGACY_NAME.matcher(name).matches()) {
                flag(Rule.NAME_SCHEME, tree,
                        name + ": Synexia names become M3 names (M3*, m3-*, M3_*)");
            }
        }

        private void checkLine(long line, String text) {
            String body = text.endsWith("\r") ? text.substring(0, text.length() - 1) : text;
            Tree at = firstTreeOnLine.getOrDefault(line, root);
            if (text.indexOf('\r') >= 0) {
                flagLine(Rule.WHITESPACE, at, line, body.length() + 1, "carriage return");
            }
            if (body.indexOf('\t') >= 0) {
                flagLine(Rule.WHITESPACE, at, line, body.indexOf('\t') + 1, "tab character");
            }
            if (!body.isEmpty() && Character.isWhitespace(body.charAt(body.length() - 1))) {
                flagLine(Rule.WHITESPACE, at, line, body.length(), "trailing whitespace");
            }
            if (body.length() > MAX_COLUMNS && !body.matches("(import|package) .*|.*https?://.*")) {
                flagLine(Rule.LINE_LENGTH, at, line, MAX_COLUMNS + 1,
                        body.length() + " columns, limit " + MAX_COLUMNS);
            }
        }

        private void flag(Rule rule, Tree tree, String message) {
            long start = positions.getStartPosition(root, tree);
            long line = lineMap.getLineNumber(start);
            flagLine(rule, tree, line, lineMap.getColumnNumber(start), message);
        }

        private void flagLine(Rule rule, Tree tree, long line, long column, String message) {
            String where = "line " + line + ":" + column + " ";
            trees.printMessage(kind, "[M3JdkStandard " + rule + "] " + where + message, tree, root);
            rows.add(String.join("\t", root.getSourceFile().getName(), Long.toString(line),
                    Long.toString(column), rule.name(), message));
        }

        private String text() {
            try {
                return root.getSourceFile().getCharContent(true).toString();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
