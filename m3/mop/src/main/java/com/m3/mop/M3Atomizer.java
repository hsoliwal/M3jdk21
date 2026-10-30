// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import com.m3.mop.M3AtomTyping.Owner;
import com.m3.mop.M3AtomTyping.Source;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Atomize pillar: decomposes a javac compilation unit into non-overlapping typed atoms.
 *
 * <p>{@link #atomize} is the single projection; the javac Plugin calls it on the compiler's own
 * tree and the CLI/refactor gate call it on a parse-only {@link JavacTask} tree. There is no
 * second parser and no lexer: every fact comes from {@code com.sun.source} trees and spans.</p>
 */
//FLLDCIM: MOP — atomize: one javac AST, Plugin and CLI share this projection
public final class M3Atomizer {
    public static final String FILES_HEADER = "path\tsha256\tbytes\tatoms";
    private static final List<String> PARSE_OPTIONS = List.of("-proc:none", "--release", "21");

    private M3Atomizer() { }

    /** One atomized source file; {@code sha256} hashes the UTF-8 bytes of the decoded text. */
    public record Unit(String path, String sha256, long bytes, List<M3Atom> atoms) {
        public Unit {
            atoms = List.copyOf(atoms);
        }

        public String row() {
            return String.join("\t", path, sha256, Long.toString(bytes), Integer.toString(atoms.size()));
        }
    }

    /** Atomizes one javac compilation unit (Plugin and CLI entry point). */
    public static Unit atomize(String path, CompilationUnitTree unit, SourcePositions positions)
            throws IOException {
        String text = unit.getSourceFile().getCharContent(true).toString();
        String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
        Source source = new Source(path, packageName, unit, positions, text);
        List<M3Atom> atoms = new ArrayList<>();
        new Scanner(source, atoms).scan(unit, null);
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        return new Unit(path, M3MopReceipt.sha256(bytes), bytes.length, atoms.stream()
                .sorted(Comparator.comparingInt(M3Atom::startChar).thenComparing(M3Atom::name)).toList());
    }

    /** Parses and atomizes files with one parse-only javac task. */
    public static List<Unit> parse(Path root, List<Path> files) throws IOException {
        JavaCompiler compiler = compiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics,
                Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics, PARSE_OPTIONS,
                    null, manager.getJavaFileObjectsFromPaths(files));
            return units(task, diagnostics, unit -> logicalPath(root, unit.getSourceFile()));
        }
    }

    /** Parses and atomizes one in-memory source (refactor gate before/after images). */
    public static Unit parse(String path, String text) throws IOException {
        JavaCompiler compiler = compiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavacTask task = (JavacTask) compiler.getTask(null, null, diagnostics, PARSE_OPTIONS, null,
                List.of(new MemorySource(path, text)));
        return units(task, diagnostics, unit -> path).get(0);
    }

    /** Path relative to {@code root} with '/' separators, or the normalized absolute path. */
    public static String logicalPath(Path root, JavaFileObject file) {
        URI uri = file.toUri();
        Path path = "file".equals(uri.getScheme()) ? Path.of(uri) : Path.of(file.getName());
        Path base = root.toAbsolutePath().normalize();
        Path absolute = path.toAbsolutePath().normalize();
        Path logical = absolute.startsWith(base) ? base.relativize(absolute) : absolute;
        return logical.toString().replace('\\', '/');
    }

    private static List<Unit> units(JavacTask task, DiagnosticCollector<JavaFileObject> diagnostics,
            java.util.function.Function<CompilationUnitTree, String> path) throws IOException {
        List<? extends CompilationUnitTree> trees = StreamSupport.stream(task.parse().spliterator(), false).toList();
        Optional<String> error = diagnostics.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR).findFirst()
                .map(d -> d.getSource().getName() + ":" + d.getLineNumber() + " " + d.getCode());
        if (error.isPresent()) throw new IOException("M3_PARSE_ERROR " + error.get());
        SourcePositions positions = Trees.instance(task).getSourcePositions();
        try {
            return trees.stream().map(unit -> unchecked(path.apply(unit), unit, positions)).toList();
        } catch (UncheckedIOException failure) {
            throw failure.getCause();
        }
    }

    private static Unit unchecked(String path, CompilationUnitTree unit, SourcePositions positions) {
        try {
            return atomize(path, unit, positions);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static JavaCompiler compiler() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IOException("M3_JDK_COMPILER_UNAVAILABLE");
        return compiler;
    }

    private static final class MemorySource extends SimpleJavaFileObject {
        private final String text;

        MemorySource(String path, String text) {
            super(URI.create("string:///" + path.replace('\\', '/')), Kind.SOURCE);
            this.text = text;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    /** Emits TYPE header, METHOD and FIELD atoms; bodies and initializers stay inside their atom. */
    private static final class Scanner extends TreeScanner<Void, Void> {
        private final Source source;
        private final List<M3Atom> atoms;
        private final Deque<Owner> owners = new ArrayDeque<>();

        Scanner(Source source, List<M3Atom> atoms) {
            this.source = source;
            this.atoms = atoms;
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            if (node.getSimpleName().isEmpty() || !source.spans(node)) return null;
            Owner self = owner(owners.peek(), node);
            atoms.add(M3AtomTyping.type(source, owners.peek(), self, node));
            owners.push(self);
            try {
                node.getMembers().forEach(member -> scan(member, null));
            } finally {
                owners.pop();
            }
            return null;
        }

        @Override
        public Void visitMethod(MethodTree node, Void unused) {
            if (!owners.isEmpty() && source.spans(node)) {
                atoms.add(M3AtomTyping.method(source, owners.peek(), node));
            }
            return null;
        }

        @Override
        public Void visitVariable(VariableTree node, Void unused) {
            if (!owners.isEmpty() && source.spans(node) && !component(owners.peek(), node)) {
                add(M3AtomTyping.field(source, owners.peek(), node));
            }
            return null;
        }

        /** Record header components belong to the TYPE header atom and its contract. */
        private static boolean component(Owner owner, VariableTree node) {
            return owner.kind() == Tree.Kind.RECORD
                    && !node.getModifiers().getFlags().contains(Modifier.STATIC);
        }

        @Override
        public Void visitBlock(BlockTree node, Void unused) {
            return null;
        }

        private void add(M3Atom field) {
            M3Atom last = atoms.isEmpty() ? null : atoms.get(atoms.size() - 1);
            boolean group = last != null && "FIELD".equals(last.kind())
                    && last.owner().equals(field.owner()) && last.startChar() == field.startChar();
            if (group) atoms.set(atoms.size() - 1, M3AtomTyping.mergeFields(source, last, field));
            else atoms.add(field);
        }

        private Owner owner(Owner parent, ClassTree node) {
            String simple = node.getSimpleName().toString();
            String prefix = source.packageName().isEmpty() ? "" : source.packageName() + ".";
            String qualified = parent == null ? prefix + simple : parent.qualified() + "$" + simple;
            Set<Modifier> flags = node.getModifiers().getFlags();
            boolean explicit = flags.contains(Modifier.PUBLIC) || flags.contains(Modifier.PROTECTED);
            boolean reachable = (parent == null || parent.reachable())
                    && (explicit || parent != null && parent.implicitPublic() && !flags.contains(Modifier.PRIVATE));
            boolean implicitPublic = node.getKind() == Tree.Kind.INTERFACE
                    || node.getKind() == Tree.Kind.ANNOTATION_TYPE;
            return new Owner(qualified, reachable, implicitPublic, node.getKind());
        }
    }
}
