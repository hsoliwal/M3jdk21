// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.compiler.ast;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import com.synexia.mindex.ast.MIndexAST;
import com.synexia.mindex.ast.MIndexASTEdge;
import com.synexia.mindex.ast.MIndexASTPool;
import com.synexia.mindex.experiment.Digests;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import javax.lang.model.element.Name;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Parse-only javac ingress using exported Tree interfaces. It captures complete getter structure,
 * not bindings or behavioral equivalence, and never modifies compiler Trees or source files.
 */
public final class MIndexJavaAST {
    private MIndexJavaAST() { }

    // Public Tree API metadata is invariant for a loaded interface class. No source,
    // Tree, pool coordinate, parse result, or dependency evidence is retained here.
    private static final ClassValue<List<Method>> GETTERS = new ClassValue<>() {
        @Override protected List<Method> computeValue(Class<?> api) {
            Map<String, Method> getters = new TreeMap<>();
            List<Method> methods = new ArrayList<>(List.of(api.getMethods()));
            methods.sort(Comparator.comparing(Method::getName).thenComparing(m -> m.getReturnType().getName()));
            for (Method method : methods) {
                String name = method.getName();
                if (method.getParameterCount() == 0 && (name.startsWith("get") || name.startsWith("is"))
                        && !Set.of("getKind", "getSourceFile", "getLineMap").contains(name)) {
                    getters.putIfAbsent(name, method);
                }
            }
            return List.copyOf(getters.values());
        }
    };


    public static MIndexASTDocument parse(Path path, MIndexASTPool pool, int release) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(pool, "pool");
        if (Files.size(path) > pool.limits().sourceChars() * 4L) { throw new IOException("source size limit"); }
        String source;
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            StringBuilder buffer = new StringBuilder();
            char[] chunk = new char[8192];
            int length;
            while ((length = reader.read(chunk)) >= 0) {
                if (length > pool.limits().sourceChars() - buffer.length()) { throw new IOException("source size limit"); }
                buffer.append(chunk, 0, length);
            }
            source = buffer.toString();
        }
        return parse(path.getFileName().toString(), source, pool, release);
    }

    /** Syntax errors are rejected before the pool is touched. Multi-atom admission is append-only. */
    public static MIndexASTDocument parse(String name, String source, MIndexASTPool pool, int release) throws IOException {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(pool, "pool");
        if (source.length() > pool.limits().sourceChars()) { throw new IOException("source size limit"); }
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) { throw new IllegalStateException("a JDK with jdk.compiler is required"); }
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var input = new SimpleJavaFileObject(URI.create("string:///MIndexInput.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
        };
        try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("-proc:none", "--release", Integer.toString(release)), null, List.of(input));
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
                throw new IOException("Java syntax rejected: " + diagnostics.getDiagnostics());
            }
            if (units.size() != 1) { throw new IOException("expected one compilation unit"); }
            CompilationUnitTree unit = units.get(0);
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            return capture(name, source, unit, positions, pool, release);
        } catch (StackOverflowError excessiveNesting) {
            throw new IOException("javac nesting exceeds parser stack", excessiveNesting);
        }
    }

    /**
     * Capture an existing compiler unit without another parse or attribution task.
     * The caller supplies the exact parser characters and owns diagnostics/phase validity.
     * Syntax must be captured before attribution or lowering changes the compiler tree.
     */
    public static MIndexASTDocument capture(String name, String source, CompilationUnitTree unit,
            SourcePositions positions, MIndexASTPool pool, int release) throws IOException {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(pool, "pool");
        if (source.length() > pool.limits().sourceChars()) { throw new IOException("source size limit"); }
        IdentityHashMap<Tree, Shape> shapes = new IdentityHashMap<>();
        List<Tree> preorder = new ArrayList<>();
        List<Tree> postorder = new ArrayList<>();
        ArrayDeque<Frame> pending = new ArrayDeque<>();
        shapes.put(unit, shape(unit));
        preorder.add(unit);
        pending.push(new Frame(unit));
        while (!pending.isEmpty()) {
            Frame frame = pending.peek();
            Shape shape = shapes.get(frame.tree);
            if (frame.next < shape.children.size()) {
                Tree child = shape.children.get(frame.next++).tree;
                if (!shapes.containsKey(child)) {
                    if (shapes.size() >= pool.limits().nodes() || pending.size() >= pool.limits().depth()) {
                        throw new IOException("syntax node/depth limit");
                    }
                    shapes.put(child, shape(child));
                    preorder.add(child);
                    pending.push(new Frame(child));
                }
            } else { postorder.add(frame.tree); pending.pop(); }
        }
        Shape rootShape = shapes.get(unit);
        rootShape.attributes.put("@schema", "javac-public-tree/v1");
        rootShape.attributes.put("@jdk", Integer.toString(Runtime.version().feature()));
        rootShape.attributes.put("@release", Integer.toString(release));
        IdentityHashMap<Tree, MIndexAST> atoms = new IdentityHashMap<>();
        for (Tree tree : postorder) {
            Shape shape = shapes.get(tree);
            List<MIndexASTEdge> edges = new ArrayList<>(shape.children.size());
            for (Child child : shape.children) {
                MIndexAST atom = atoms.get(child.tree);
                if (atom == null) { throw new IOException("cyclic or unsupported public tree"); }
                edges.add(new MIndexASTEdge(child.role, atom));
            }
            atoms.put(tree, pool.intern(tree.getKind().name(), shape.attributes, edges));
        }
        List<MIndexASTOccurrence> occurrences = new ArrayList<>(preorder.size());
        for (Tree tree : preorder) {
            long start = positions.getStartPosition(unit, tree);
            long end = positions.getEndPosition(unit, tree);
            if (start < 0 || end < 0) { start = -1; end = -1; }
            occurrences.add(new MIndexASTOccurrence(atoms.get(tree), Math.toIntExact(start), Math.toIntExact(end)));
        }
        return new MIndexASTDocument(name, source, sourceHash(source), atoms.get(unit), occurrences);
    }

    private static Shape shape(Tree tree) throws IOException {
        Class<? extends Tree> api = tree.getKind().asInterface();
        if (api == null) { throw new IOException("unsupported tree kind: " + tree.getKind()); }
        Shape result = new Shape();
        for (Method getter : GETTERS.get(api)) {
            try { value(result, getter.getName(), getter.invoke(tree)); }
            catch (IllegalAccessException | InvocationTargetException failure) {
                throw new IOException("cannot read public syntax getter " + api.getName() + "." + getter.getName(), failure);
            }
        }
        return result;
    }

    private static void value(Shape shape, String role, Object value) throws IOException {
        if (value instanceof Tree child) {
            shape.attributes.put(role, "tree");
            shape.children.add(new Child(role, child));
        } else if (value instanceof List<?> list) {
            shape.attributes.put(role, "list:" + list.size());
            for (int i = 0; i < list.size(); i++) { value(shape, role + "[" + i + "]", list.get(i)); }
        } else if (value instanceof Set<?> set) {
            List<String> items = new ArrayList<>();
            for (Object item : set) { items.add(scalar(item)); }
            items.sort(String::compareTo);
            shape.attributes.put(role, "set:" + items.size());
            for (int i = 0; i < items.size(); i++) { shape.attributes.put(role + "[" + i + "]", items.get(i)); }
        } else { shape.attributes.put(role, scalar(value)); }
    }

    private static String scalar(Object value) throws IOException {
        if (value == null) { return "null"; }
        if (value instanceof Name name) { return "name:" + name; }
        if (value instanceof Enum<?> item) { return "enum:" + item.getDeclaringClass().getName() + ":" + item.name(); }
        if (value instanceof Double number) { return "double:" + Long.toUnsignedString(Double.doubleToRawLongBits(number)); }
        if (value instanceof Float number) { return "float:" + Integer.toUnsignedString(Float.floatToRawIntBits(number)); }
        if (value instanceof String || value instanceof Character || value instanceof Boolean
                || value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return value.getClass().getName() + ":" + value;
        }
        throw new IOException("unsupported syntax scalar: " + value.getClass().getName());
    }

    private static String sourceHash(String source) {
        byte[] units = new byte[Math.multiplyExact(source.length(), 2)];
        for (int i = 0; i < source.length(); i++) {
            units[2 * i] = (byte) (source.charAt(i) >>> 8);
            units[2 * i + 1] = (byte) source.charAt(i);
        }
        return Digests.sha256(units);
    }

    private static final class Shape {
        final Map<String, String> attributes = new TreeMap<>();
        final List<Child> children = new ArrayList<>();
    }
    private record Child(String role, Tree tree) { }
    private static final class Frame {
        final Tree tree;
        int next;
        Frame(Tree tree) { this.tree = tree; }
    }
}
