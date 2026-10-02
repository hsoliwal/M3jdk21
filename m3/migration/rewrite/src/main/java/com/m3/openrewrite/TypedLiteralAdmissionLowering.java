/* Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0 */
package com.m3.openrewrite;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.util.Set;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Conservative Java21 compiler recipe kernel, independent of OpenRewrite's scheduler.
 * Only explicit M3Text.fromString(String-constant + String-constant) admissions are lowered.
 * Ordinary String expressions, receivers with evaluation, and unresolved files remain untouched.
 * The owner class closure is exact-pinned; this initial version accepts a classes directory only.
 */
public final class TypedLiteralAdmissionLowering {
    public static final String OWNER_CLASS_ROOT =
            "6823dc6df52d98d110a63fcbd8a3cb8f06d519b357f2b780c992b7375c668530";
    private static final String OWNER = "com.m3.text.M3Text";
    private static final int MAX_SOURCE_UNITS = 262144;
    private static final int MAX_OPERANDS = 64;
    private TypedLiteralAdmissionLowering() { }

    public enum Status { CHANGED, UNCHANGED, REFUSED }
    public record Result(Status status, String before, String after, int changes, String reason,
                         String beforeSha256, String afterSha256, String ownerClassRoot) {
        public Result {
            Objects.requireNonNull(status); Objects.requireNonNull(before); Objects.requireNonNull(after);
            Objects.requireNonNull(reason); Objects.requireNonNull(ownerClassRoot);
            if (changes < 0 || !sha(before).equals(beforeSha256) || !sha(after).equals(afterSha256)) {
                throw new IllegalArgumentException("inconsistent lowering receipt");
            }
        }
        /** Exact reversal only; user changes after transformation are never overwritten. */
        public String rollback(String actual) {
            if (!after.equals(actual) || !afterSha256.equals(sha(actual))) throw new IllegalArgumentException("rollback source drift");
            return before;
        }
    }
    private record Edit(int start, int end, String replacement) { }
    private record Analysis(List<Edit> edits, boolean errors) { }

    public static Result lower(String fileName, String source, Path ownerClasses) {
        Objects.requireNonNull(source); Objects.requireNonNull(ownerClasses);
        if (fileName == null || !fileName.matches("[A-Za-z_$][A-Za-z0-9_$]*\\.java")
                || source.length() > MAX_SOURCE_UNITS
                || !StandardCharsets.UTF_8.newEncoder().canEncode(source)) {
            return result(Status.REFUSED, source, source, 0, "unsupported filename, encoding or source bound");
        }
        try {
            OwnerSnapshot owner = snapshot(ownerClasses);
            if (!OWNER_CLASS_ROOT.equals(owner.root())) {
                return result(Status.REFUSED, source, source, 0, "canonical owner class closure differs");
            }
            Analysis input = analyze(fileName, source, owner, true);
            if (input.errors()) return result(Status.REFUSED, source, source, 0, "unresolved or invalid Java21 source");
            if (input.edits().isEmpty()) return result(Status.UNCHANGED, source, source, 0, "no eligible explicit constant admission");
            List<Edit> edits = input.edits().stream().sorted(Comparator.comparingInt(Edit::start).reversed()).toList();
            StringBuilder output = new StringBuilder(source);
            int next = source.length();
            for (Edit edit : edits) {
                if (edit.start() < 0 || edit.end() > next || edit.start() >= edit.end()) {
                    return result(Status.REFUSED, source, source, 0, "overlapping or invalid source coordinates");
                }
                output.replace(edit.start(), edit.end(), edit.replacement());
                next = edit.start();
            }
            if (output.length() > MAX_SOURCE_UNITS) return result(Status.REFUSED, source, source, 0, "expanded source bound");
            String transformed = output.toString();
            if (analyze(fileName, transformed, owner, false).errors()) {
                return result(Status.REFUSED, source, source, 0, "transformed source did not type-check");
            }
            return result(Status.CHANGED, source, transformed, edits.size(), "resolved immutable String constants only");
        } catch (IOException | RuntimeException | StackOverflowError error) {
            return result(Status.REFUSED, source, source, 0, "compiler or owner observation failed: " + error.getClass().getSimpleName());
        }
    }

    private static Analysis analyze(String fileName, String source, OwnerSnapshot owner, boolean collect) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) return new Analysis(List.of(), true);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<Edit> edits = new ArrayList<>();
        try (var manager = new ForwardingJavaFileManager<>(compiler.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            @Override public Iterable<JavaFileObject> list(JavaFileManager.Location location,
                    String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
                if (location == StandardLocation.CLASS_PATH) {
                    if (!kinds.contains(JavaFileObject.Kind.CLASS)) return List.of();
                    return owner.classes().stream().filter(c -> c.packageName().equals(packageName)
                            || (recurse && c.packageName().startsWith(packageName + ".")))
                            .map(c -> (JavaFileObject) c).toList();
                }
                return super.list(location, packageName, kinds, recurse);
            }
            @Override public JavaFileObject getJavaFileForInput(JavaFileManager.Location location,
                    String name, JavaFileObject.Kind kind) throws IOException {
                if (location == StandardLocation.CLASS_PATH) {
                    return owner.classes().stream().filter(c -> c.binaryName.equals(name)).findFirst().orElse(null);
                }
                return super.getJavaFileForInput(location, name, kind);
            }
            @Override public String inferBinaryName(JavaFileManager.Location location, JavaFileObject file) {
                return file instanceof FrozenClass frozen ? frozen.binaryName : super.inferBinaryName(location, file);
            }
        }) {
            JavaFileObject unit = new SimpleJavaFileObject(URI.create("string:///" + fileName), JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
            };
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("--release", "21", "-proc:none", "-Xlint:all", "-classpath", "."), null, List.of(unit));
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);
            task.analyze(); // No annotation processors, class generation, source execution or network.
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
                return new Analysis(List.of(), true);
            }
            Trees trees = Trees.instance(task);
            for (CompilationUnitTree tree : units) {
                if (tree.getPackageName() != null && tree.getPackageName().toString().startsWith("com.m3.text")) {
                    return new Analysis(List.of(), true); // Never rewrite the canonical owner's own source.
                }
                if (collect) new TreePathScanner<Void, Void>() {
                    @Override public Void visitMethodInvocation(MethodInvocationTree call, Void unused) {
                        Element element = trees.getElement(getCurrentPath());
                        if (element instanceof ExecutableElement method
                                && method.getEnclosingElement() instanceof TypeElement type
                                && type.getQualifiedName().contentEquals(OWNER)
                                && method.getSimpleName().contentEquals("fromString")
                                && method.getModifiers().contains(Modifier.STATIC)
                                && method.getParameters().size() == 1
                                && method.getParameters().getFirst().asType().toString().equals("java.lang.String")
                                && method.getReturnType().toString().equals(OWNER)
                                && call.getArguments().size() == 1 && call.getTypeArguments().isEmpty()
                                && noReceiverEvaluation(call, trees, tree)) {
                            ExpressionTree argument = unwrap(call.getArguments().getFirst());
                            if (argument.getKind() == Tree.Kind.PLUS) {
                                List<ExpressionTree> operands = constants(argument, trees, tree);
                                int start = position(trees.getSourcePositions().getStartPosition(tree, call), source.length());
                                int end = position(trees.getSourcePositions().getEndPosition(tree, call), source.length());
                                if (operands.size() >= 2 && start >= 0 && end > start) {
                                    String original = source.substring(start, end);
                                    // Refuse comments/Unicode-escaped tokens rather than dropping their source spelling.
                                    if (!original.contains("/*") && !original.contains("//") && !original.contains("\\u")) {
                                        StringBuilder replacement = new StringBuilder();
                                        boolean valid = true;
                                        for (int i = 0; i < operands.size(); i++) {
                                            ExpressionTree operand = operands.get(i);
                                            int a = position(trees.getSourcePositions().getStartPosition(tree, operand), source.length());
                                            int b = position(trees.getSourcePositions().getEndPosition(tree, operand), source.length());
                                            if (a < start || b > end || b <= a) { valid = false; break; }
                                            if (i != 0) replacement.append(".concat(");
                                            replacement.append(OWNER).append(".fromString(").append(source, a, b).append(')');
                                            if (i != 0) replacement.append(')');
                                        }
                                        if (valid) edits.add(new Edit(start, end, replacement.toString()));
                                    }
                                }
                            }
                        }
                        return super.visitMethodInvocation(call, unused);
                    }
                }.scan(tree, null);
            }
        }
        return new Analysis(List.copyOf(edits), false);
    }

    private static boolean noReceiverEvaluation(MethodInvocationTree call, Trees trees, CompilationUnitTree unit) {
        if (call.getMethodSelect() instanceof IdentifierTree) return true; // resolved static import
        if (!(call.getMethodSelect() instanceof MemberSelectTree select)) return false;
        Element qualifier = trees.getElement(TreePath.getPath(unit, select.getExpression()));
        return qualifier instanceof TypeElement; // instance/static receiver expressions are deliberately refused
    }
    private static List<ExpressionTree> constants(ExpressionTree root, Trees trees, CompilationUnitTree unit) {
        ArrayDeque<ExpressionTree> pending = new ArrayDeque<>(); pending.push(root);
        List<ExpressionTree> result = new ArrayList<>();
        while (!pending.isEmpty()) {
            if (pending.size() + result.size() > MAX_OPERANDS) return List.of();
            ExpressionTree expression = unwrap(pending.pop());
            if (expression instanceof BinaryTree binary && expression.getKind() == Tree.Kind.PLUS) {
                pending.push(binary.getRightOperand()); pending.push(binary.getLeftOperand());
            } else if (expression instanceof LiteralTree literal && literal.getValue() instanceof String) {
                result.add(expression);
            } else {
                Element element = trees.getElement(TreePath.getPath(unit, expression));
                if (!(element instanceof VariableElement variable) || !(variable.getConstantValue() instanceof String)) return List.of();
                if (expression instanceof MemberSelectTree select) {
                    Element qualifier = trees.getElement(TreePath.getPath(unit, select.getExpression()));
                    if (!(qualifier instanceof TypeElement) && !(qualifier instanceof PackageElement)) return List.of();
                } else if (!(expression instanceof IdentifierTree)) return List.of();
                result.add(expression);
            }
        }
        return result.size() > MAX_OPERANDS ? List.of() : List.copyOf(result);
    }
    private static ExpressionTree unwrap(ExpressionTree tree) {
        while (tree instanceof ParenthesizedTree parentheses) tree = parentheses.getExpression();
        return tree;
    }
    private static int position(long value, int length) { return value < 0 || value > length ? -1 : (int) value; }
    private static Result result(Status status, String before, String after, int count, String reason) {
        return new Result(status, before, after, count, reason, sha(before), sha(after), OWNER_CLASS_ROOT);
    }

    private static final class FrozenClass extends SimpleJavaFileObject {
        private final String binaryName;
        private final byte[] bytes;
        FrozenClass(String relative, byte[] owned) {
            super(URI.create("memory:///" + relative), Kind.CLASS);
            binaryName = relative.substring(0, relative.length() - 6).replace('/', '.');
            bytes = owned;
        }
        String packageName() { return binaryName.substring(0, binaryName.lastIndexOf('.')); }
        @Override public InputStream openInputStream() { return new ByteArrayInputStream(bytes); }
    }
    private record OwnerSnapshot(String root, List<FrozenClass> classes) { }

    /** The compiler reads this exact immutable byte snapshot, not a concurrently editable directory. */
    private static OwnerSnapshot snapshot(Path directory) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root) || Files.isSymbolicLink(root)) throw new IOException("invalid classes directory");
        Path packagePath = root.resolve("com/m3/text");
        for (Path p = packagePath; p != null && p.startsWith(root); p = p.getParent()) {
            if (Files.isSymbolicLink(p)) throw new IOException("symlinked owner");
        }
        StringBuilder framed = new StringBuilder();
        int remaining = 8388608;
        boolean owner = false;
        List<FrozenClass> frozen = new ArrayList<>();
        try (var stream = Files.walk(packagePath)) {
            List<Path> paths = stream.limit(1025).sorted().toList();
            if (paths.size() > 1024) throw new IOException("owner file bound");
            for (Path path : paths) {
                if (Files.isSymbolicLink(path)) throw new IOException("symlinked class");
                if (!path.toString().endsWith(".class")) continue;
                byte[] value;
                try (var input = Files.newInputStream(path)) { value = input.readNBytes(remaining + 1); }
                if (value.length > remaining) throw new IOException("owner byte bound");
                remaining -= value.length;
                String relative = root.relativize(path).toString().replace('\\', '/');
                owner |= relative.equals("com/m3/text/M3Text.class");
                frozen.add(new FrozenClass(relative, value));
                framed.append(relative.length()).append(':').append(relative).append(sha(value));
            }
        }
        if (!owner) throw new IOException("canonical class absent");
        return new OwnerSnapshot(sha(framed.toString()), List.copyOf(frozen));
    }
    /** Deterministic classfile identity, independent of process addresses and absolute paths. */
    public static String ownerClassRoot(Path directory) throws IOException { return snapshot(directory).root(); }
    private static String sha(String value) { return sha(value.getBytes(StandardCharsets.UTF_8)); }
    private static String sha(byte[] value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
