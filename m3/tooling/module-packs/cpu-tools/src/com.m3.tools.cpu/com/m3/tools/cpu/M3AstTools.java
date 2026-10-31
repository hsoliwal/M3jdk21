// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import java.util.function.Consumer;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.io.M3File;
import jdk.m3.text.Canonical;

/**
 * Detached syntax inventories from javac's existing M3AST callback.
 *
 * <p>Capture never starts a parser, asks for attribution, or retains a tree,
 * compiler task, type, element, Unit or Trees object. Structural coordinates
 * occupy flat primitive tables; public fact records are created on access.
 * Symbol names here are declared syntax names, not resolved bindings or a
 * portable semantic-analysis cache. Missing compiler positions are counted
 * explicitly rather than fabricated.</p>
 */
public final class M3AstTools {
    public static final int MAX_NODES = 100_000;
    public static final int MAX_DEPTH = 256;
    public static final int MAX_FILES = 1024;
    public static final int MAX_UTF16 = 16 * 1024 * 1024;
    public static final long MAX_TOTAL_UTF16 = 64L * 1024 * 1024;
    private static final Tree.Kind[] KINDS = Tree.Kind.values();

    private M3AstTools() { }

    public record Span(int start, int end) {
        public Span {
            if (start < 0 || end < start) throw new IllegalArgumentException("invalid UTF-16 range");
        }
        public int length() { return end - start; }
    }

    /** Raw SHA is present only when an actual M3File was exactly bound to the compiler input. */
    public record SourceIdentity(URI uri, String utf16Sha256, int utf16Length,
                                 Optional<String> rawSha256) {
        public SourceIdentity {
            Objects.requireNonNull(uri, "uri");
            checkDigest(utf16Sha256);
            Objects.requireNonNull(rawSha256, "rawSha256").ifPresent(M3AstTools::checkDigest);
            if (utf16Length < 0 || utf16Length > MAX_UTF16) {
                throw new IllegalArgumentException("source UTF-16 budget");
            }
        }
    }

    public record StructuralFact(Tree.Kind kind, Span span, int parent, int depth) {
        public StructuralFact {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(span, "span");
            if (parent < -1 || depth < 0) throw new IllegalArgumentException("structural coordinate");
        }
    }

    public record SymbolFact(String name, String kind, Span span) {
        public SymbolFact {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(span, "span");
        }
    }

    public record ImportFact(String qualifiedName, boolean staticImport, Span span) {
        public ImportFact {
            Objects.requireNonNull(qualifiedName, "qualifiedName");
            Objects.requireNonNull(span, "span");
        }
    }

    public record MethodFact(String name, int parameterCount, boolean constructor,
                             Span span, Optional<Span> body) {
        public MethodFact {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(span, "span");
            Objects.requireNonNull(body, "body");
            if (parameterCount < 0) throw new IllegalArgumentException("parameter count");
        }
    }

    /** Immutable source-bound arrays. List access creates projections without a retained node cache. */
    public static final class Snapshot {
        private final SourceIdentity source;
        private final M3AST.Phase phase;
        private final int visited;
        private final int unpositioned;
        private final int[] structures;
        private final String[] symbolNames;
        private final String[] symbolKinds;
        private final int[] symbolNodes;
        private final String[] importNames;
        private final int[] importNodes;
        private final String[] methodNames;
        private final int[] methodRows;
        private final String sha256;

        private Snapshot(SourceIdentity source, M3AST.Phase phase, Capture captured,
                         ProgressMonitor progress) {
            this.source = source;
            this.phase = phase;
            this.visited = captured.visited;
            this.unpositioned = captured.unpositioned;
            this.structures = captured.structures.freeze();
            this.symbolNames = captured.symbolNames.toArray(String[]::new);
            this.symbolKinds = captured.symbolKinds.toArray(String[]::new);
            this.symbolNodes = captured.symbolNodes.freeze();
            this.importNames = captured.importNames.toArray(String[]::new);
            this.importNodes = captured.importNodes.freeze();
            this.methodNames = captured.methodNames.toArray(String[]::new);
            this.methodRows = captured.methodRows.freeze();
            this.sha256 = receipt(progress);
        }

        public SourceIdentity source() { return source; }
        public M3AST.Phase phase() { return phase; }
        public int visitedNodes() { return visited; }
        public int unpositionedNodes() { return unpositioned; }
        public int structureCount() { return structures.length / 5; }
        /**
         * Deterministic identity receipt for the exact source, syntax inventory and linked
         * CPU-tool implementation. It is neither a signature nor evidence that the facts
         * are semantically correct; compare a fresh actual compiler capture when replaying.
         */
        public String sha256() { return sha256; }
        /** Counter payload only, excluding arrays' headers, String metadata and caller projections. */
        public long retainedPrimitiveBytes() {
            return 4L * (structures.length + symbolNodes.length + importNodes.length + methodRows.length);
        }

        public StructuralFact structure(int ordinal) {
            int at = Objects.checkIndex(ordinal, structureCount()) * 5;
            return new StructuralFact(KINDS[structures[at]],
                    new Span(structures[at + 1], structures[at + 2]),
                    structures[at + 3], structures[at + 4]);
        }

        public List<StructuralFact> structures() { return new FactList<>(structureCount(), this::structure); }

        public List<SymbolFact> symbols() {
            return new FactList<>(symbolNames.length, ordinal -> new SymbolFact(
                    symbolNames[ordinal], symbolKinds[ordinal], span(symbolNodes[ordinal])));
        }

        public List<ImportFact> imports() {
            return new FactList<>(importNames.length, ordinal -> new ImportFact(
                    importNames[ordinal], (importNodes[ordinal] & 1) != 0,
                    span(importNodes[ordinal] >>> 1)));
        }

        public List<MethodFact> methods() {
            return new FactList<>(methodNames.length, ordinal -> {
                int at = ordinal * 5;
                Optional<Span> body = methodRows[at + 3] < 0 ? Optional.empty()
                        : Optional.of(new Span(methodRows[at + 3], methodRows[at + 4]));
                return new MethodFact(methodNames[ordinal], methodRows[at + 1],
                        methodRows[at + 2] != 0, span(methodRows[at]), body);
            });
        }

        private Span span(int node) {
            int at = Objects.checkIndex(node, structureCount()) * 5;
            return new Span(structures[at + 1], structures[at + 2]);
        }

        private String receipt(ProgressMonitor progress) {
            progress.checkCancelled();
            Canonical hash = new Canonical("m3-cpu-ast-snapshot-v1")
                    .text(CpuTools.implementationSha256())
                    .text(source.uri().toASCIIString()).text(source.utf16Sha256())
                    .number(source.utf16Length()).number(source.rawSha256().isPresent() ? 1 : 0);
            source.rawSha256().ifPresent(hash::text);
            hash.text(phase.name()).number(visited).number(unpositioned).number(structureCount());
            for (int at = 0; at < structures.length; at += 5) {
                progress.checkCancelled();
                hash.text(KINDS[structures[at]].name());
                for (int field = 1; field < 5; field++) hash.number(structures[at + field]);
            }
            hash.number(symbolNames.length);
            for (int i = 0; i < symbolNames.length; i++) {
                progress.checkCancelled();
                utf16(hash, symbolNames[i], progress);
                utf16(hash, symbolKinds[i], progress);
                hash.number(symbolNodes[i]);
            }
            hash.number(importNames.length);
            for (int i = 0; i < importNames.length; i++) {
                progress.checkCancelled();
                utf16(hash, importNames[i], progress);
                hash.number(importNodes[i]);
            }
            hash.number(methodNames.length);
            for (int i = 0; i < methodNames.length; i++) {
                progress.checkCancelled();
                utf16(hash, methodNames[i], progress);
                for (int field = 0; field < 5; field++) hash.number(methodRows[i * 5 + field]);
            }
            progress.checkCancelled();
            return hash.finish();
        }
    }

    public static Snapshot capture(M3AST.Unit unit) throws IOException {
        return capture(unit, null, ProgressMonitors.current());
    }

    public static Snapshot capture(M3AST.Unit unit, ProgressMonitor monitor) throws IOException {
        return capture(unit, null, monitor);
    }

    /** Optional file binding checks URI, exact characters and the distinct character digest. */
    public static Snapshot capture(M3AST.Unit unit, M3File file, ProgressMonitor monitor) throws IOException {
        ProgressMonitor progress = progress(monitor);
        progress.checkCancelled();
        progress.begin("Capture shared compiler source", ProgressMonitor.UNKNOWN_WORK);
        try { return captureSource(unit, file, progress); }
        finally { progress.done(); }
    }

    public static M3AST.Consumer consumer(Consumer<? super Snapshot> sink) {
        return consumer(sink, ProgressMonitors.current());
    }

    /** Register this on an existing M3AST handle before the caller starts its compiler task. */
    public static M3AST.Consumer consumer(Consumer<? super Snapshot> sink, ProgressMonitor monitor) {
        Objects.requireNonNull(sink, "sink");
        ProgressMonitor progress = progress(monitor);
        progress.checkCancelled();
        return new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws IOException {
                sink.accept(capture(unit, progress));
            }
        };
    }

    public static List<Snapshot> parse(List<M3File> files) throws IOException {
        return parse(files, ProgressMonitors.current());
    }

    /**
     * Explicit convenience task: syntax only, -proc:none, no attribution or code generation.
     * The admitted immutable String references are supplied directly to javac; files are not
     * reopened. Captured hashes describe these snapshots even if their paths later change.
     * Source and detached-fact budgets do not bound javac's parsing CPU or peak memory.
     */
    public static List<Snapshot> parse(List<M3File> files, ProgressMonitor monitor) throws IOException {
        ProgressMonitor progress = progress(monitor);
        progress.checkCancelled();
        Map<URI, M3File> sources = sources(files);
        if (sources.isEmpty()) return List.of();
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("M3 jdk.compiler is required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var results = new ArrayList<Snapshot>(sources.size());
        var inputs = new ArrayList<JavaFileObject>(sources.size());
        for (M3File source : sources.values()) inputs.add(new SnapshotInput(source));
        progress.begin("Parse immutable source snapshots", ProgressMonitor.UNKNOWN_WORK);
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "-source", "21"), null, inputs);
            try (M3AST frontend = M3AST.open(task, true)) {
                Exception[] callbackFailure = new Exception[1];
                frontend.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws IOException {
                        try {
                            progress.checkCancelled();
                            if (!hasErrors(diagnostics)) {
                                M3File file = sources.get(unit.source().uri());
                                if (file == null) throw new IOException("compiler introduced an unbound source");
                                results.add(captureSource(unit, file, progress));
                            }
                        } catch (IOException | RuntimeException failure) {
                            callbackFailure[0] = failure;
                            throw failure;
                        }
                    }
                });
                try {
                    task.parse();
                } catch (RuntimeException compilerFailure) {
                    // javac wraps listener failures. Restore only this callback's own
                    // refusal, preserving the public admission/cancellation contract.
                    if (callbackFailure[0] instanceof IOException io) throw io;
                    if (callbackFailure[0] instanceof RuntimeException runtime) throw runtime;
                    throw compilerFailure;
                }
                progress.checkCancelled();
                for (var diagnostic : diagnostics.getDiagnostics()) {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        throw new IOException("Java syntax rejected at " + diagnostic.getPosition()
                                + ": " + diagnostic.getCode());
                    }
                }
                if (results.size() != sources.size()) throw new IOException("incomplete compiler source capture");
            }
            return List.copyOf(results);
        } finally {
            progress.done();
        }
    }

    private static Snapshot captureSource(M3AST.Unit unit, M3File file, ProgressMonitor progress) throws IOException {
        Objects.requireNonNull(unit, "unit");
        progress.checkCancelled();
        M3AST.Source source = unit.source();
        if (source.text().length() > MAX_UTF16) throw new IllegalArgumentException("source UTF-16 budget");
        if (file != null && (!file.uri().equals(source.uri())
                || !file.utf16Sha256().equals(source.utf16Sha256())
                || !file.text().equals(source.text()))) {
            throw new IllegalArgumentException("M3File does not match this exact compiler source");
        }
        SourceIdentity identity = new SourceIdentity(source.uri(), source.utf16Sha256(),
                source.text().length(), file == null ? Optional.empty() : Optional.of(file.rawSha256()));
        Capture captured = new Capture(unit.tree(), unit.trees().getSourcePositions(),
                source.text().length(), progress);
        captured.scan(unit.tree(), null);
        progress.checkCancelled();
        return new Snapshot(identity, unit.phase(), captured, progress);
    }

    private static Map<URI, M3File> sources(List<M3File> files) {
        Objects.requireNonNull(files, "files");
        if (files.size() > MAX_FILES) throw new IllegalArgumentException("source file count budget");
        Map<URI, M3File> sources = new LinkedHashMap<>();
        long total = 0;
        for (M3File file : files) {
            Objects.requireNonNull(file, "file");
            if (sources.putIfAbsent(file.uri(), file) != null) throw new IllegalArgumentException("duplicate source URI");
            total += file.length();
            if (total > MAX_TOTAL_UTF16) throw new IllegalArgumentException("total source UTF-16 budget");
        }
        return sources;
    }

    private static boolean hasErrors(DiagnosticCollector<JavaFileObject> diagnostics) {
        return diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
    }

    private static final class SnapshotInput extends SimpleJavaFileObject {
        private final M3File file;
        SnapshotInput(M3File file) { super(file.uri(), Kind.SOURCE); this.file = file; }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return file.text(); }
    }

    private static final class Capture extends TreeScanner<Void, Void> {
        private final CompilationUnitTree root;
        private final SourcePositions positions;
        private final int length;
        private final ProgressMonitor progress;
        private final Rows structures = new Rows();
        private final ArrayList<String> symbolNames = new ArrayList<>();
        private final ArrayList<String> symbolKinds = new ArrayList<>();
        private final Ints symbolNodes = new Ints();
        private final ArrayList<String> importNames = new ArrayList<>();
        private final Ints importNodes = new Ints();
        private final ArrayList<String> methodNames = new ArrayList<>();
        private final Rows methodRows = new Rows();
        private int visited, unpositioned, depth = -1, parent = -1, index = -1;

        Capture(CompilationUnitTree root, SourcePositions positions, int length, ProgressMonitor progress) {
            this.root = root;
            this.positions = positions;
            this.length = length;
            this.progress = progress;
        }

        @Override public Void scan(Tree tree, Void unused) {
            if (tree == null) return null;
            progress.checkCancelled();
            if (++visited > MAX_NODES || ++depth > MAX_DEPTH) {
                throw new IllegalArgumentException("AST node/depth budget exceeded");
            }
            progress.worked(1);
            int previousParent = parent, previousIndex = index;
            Span span = tree == root ? new Span(0, length) : position(tree);
            if (span == null) {
                unpositioned++;
                index = -1;
            } else {
                index = structures.add(tree.getKind().ordinal(), span.start(), span.end(), parent, depth);
                parent = index;
            }
            try { return super.scan(tree, unused); }
            finally { parent = previousParent; index = previousIndex; depth--; }
        }

        private Span position(Tree tree) {
            long start = positions.getStartPosition(root, tree);
            long end = positions.getEndPosition(root, tree);
            if (start == -1 || end == -1) return null;
            if (start < 0 || end < start || end > length) {
                throw new IllegalArgumentException("compiler range outside frozen UTF-16 source");
            }
            return new Span((int) start, (int) end);
        }

        private void symbol(String name, String kind) {
            if (index >= 0) {
                symbolNames.add(name);
                symbolKinds.add(kind);
                symbolNodes.add(index);
            }
        }

        @Override public Void visitClass(ClassTree node, Void unused) {
            if (!node.getSimpleName().isEmpty()) symbol(node.getSimpleName().toString(), node.getKind().name());
            return super.visitClass(node, unused);
        }

        @Override public Void visitMethod(MethodTree node, Void unused) {
            String name = node.getName().toString();
            symbol(name, "METHOD");
            if (index >= 0) {
                Span body = node.getBody() == null ? null : position(node.getBody());
                methodNames.add(name);
                methodRows.add(index, node.getParameters().size(), node.getReturnType() == null ? 1 : 0,
                        body == null ? -1 : body.start(), body == null ? -1 : body.end());
            }
            return super.visitMethod(node, unused);
        }

        @Override public Void visitVariable(VariableTree node, Void unused) {
            symbol(node.getName().toString(), "VARIABLE");
            return super.visitVariable(node, unused);
        }

        @Override public Void visitTypeParameter(TypeParameterTree node, Void unused) {
            symbol(node.getName().toString(), "TYPE_PARAMETER");
            return super.visitTypeParameter(node, unused);
        }

        @Override public Void visitImport(ImportTree node, Void unused) {
            if (index >= 0) {
                importNames.add(node.getQualifiedIdentifier().toString());
                importNodes.add((index << 1) | (node.isStatic() ? 1 : 0));
            }
            return super.visitImport(node, unused);
        }

        @Override public Void visitModule(ModuleTree node, Void unused) {
            symbol(node.getName().toString(), "MODULE");
            return super.visitModule(node, unused);
        }

        @Override public Void visitPackage(PackageTree node, Void unused) {
            symbol(node.getPackageName().toString(), "PACKAGE");
            return super.visitPackage(node, unused);
        }
    }

    private static final class Rows {
        private int[] values = new int[320];
        private int size;
        int add(int a, int b, int c, int d, int e) {
            if (size / 5 == MAX_NODES) throw new IllegalArgumentException("fact count budget");
            if (size + 5 > values.length) values = Arrays.copyOf(values, Math.min(MAX_NODES * 5, values.length * 2));
            int row = size / 5;
            values[size++] = a; values[size++] = b; values[size++] = c;
            values[size++] = d; values[size++] = e;
            return row;
        }
        int[] freeze() { return Arrays.copyOf(values, size); }
    }

    private static final class Ints {
        private int[] values = new int[64];
        private int size;
        void add(int value) {
            if (size == MAX_NODES) throw new IllegalArgumentException("fact count budget");
            if (size == values.length) values = Arrays.copyOf(values, Math.min(MAX_NODES, values.length * 2));
            values[size++] = value;
        }
        int[] freeze() { return Arrays.copyOf(values, size); }
    }

    private static final class FactList<T> extends AbstractList<T> implements RandomAccess {
        private final int size;
        private final java.util.function.IntFunction<T> at;
        FactList(int size, java.util.function.IntFunction<T> at) { this.size = size; this.at = at; }
        @Override public T get(int index) { return at.apply(Objects.checkIndex(index, size)); }
        @Override public int size() { return size; }
    }

    private static ProgressMonitor progress(ProgressMonitor monitor) {
        return monitor == null ? ProgressMonitors.current() : monitor;
    }

    /** Length-framed UTF-16 units preserve even isolated surrogates in compiler metadata. */
    private static void utf16(Canonical hash, String text, ProgressMonitor progress) {
        hash.number(text.length());
        for (int i = 0; i < text.length(); i++) {
            if ((i & 1023) == 0) progress.checkCancelled();
            hash.number(text.charAt(i));
        }
    }

    private static void checkDigest(String value) {
        if (value == null || value.length() != 64) throw new IllegalArgumentException("SHA-256 encoding");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (!(ch >= '0' && ch <= '9') && !(ch >= 'a' && ch <= 'f')) {
                throw new IllegalArgumentException("SHA-256 encoding");
            }
        }
    }
}
