// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.compiler.signals;

import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;

/**
 * Opt-in javac analysis-time signals for M3JDK21, without modifying the public Java AST or ABI.
 *
 * <p>Compiler-attributed source facts are precomputed once, then serialized as deterministic,
 * immutable primitive/histogram data. Static call targets are candidate graph edges: virtual
 * dispatch, reflection, generated code and dynamically loaded classes are not exhaustively known.
 * Hash values are candidate features, never equality or lexical-ordering certificates.</p>
 */
public final class M3SourceSignalPlugin implements Plugin {
    private static final String ANNOTATION = "com.m3.collections.sortable.Sortable";
    private static final String SCHEMA = "M3JDK21-SOURCE-SIGNALS/1";
    private static final java.util.Comparator<Long> UNSIGNED = Long::compareUnsigned;

    private enum Family {
        PACKAGE, CLASS, FIELDS, METHODS, DEPENDENCIES, CALLS, INHERITANCE, ANNOTATIONS
    }

    private final TreeMap<String, Facts> facts = new TreeMap<>();
    private Path output;
    private Path methodOutput;
    private M3MethodSignals methodSignals;
    private boolean includeAll;
    private boolean written;
    private Trees trees;

    @Override
    public String getName() {
        return "M3SourceSignals";
    }

    @Override
    public void init(JavacTask task, String... args) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(args, "args");
        if (trees != null) throw new IllegalStateException("plugin instance already initialized");
        for (String argument : args) {
            if (argument.startsWith("out=")) {
                if (output != null || argument.length() == 4) {
                    throw new IllegalArgumentException("provide exactly one nonempty out=FILE");
                }
                output = Path.of(argument.substring(4)).toAbsolutePath().normalize();
            } else if (argument.startsWith("methodOut=")) {
                if (methodOutput != null || argument.length() == 10) {
                    throw new IllegalArgumentException("provide exactly one nonempty methodOut=FILE");
                }
                methodOutput = Path.of(argument.substring(10)).toAbsolutePath().normalize();
            } else if (argument.equals("include=all")) {
                includeAll = true;
            } else if (argument.equals("include=annotated")) {
                includeAll = false;
            } else {
                throw new IllegalArgumentException("unknown M3SourceSignals option: " + argument);
            }
        }
        if (output == null) throw new IllegalArgumentException("M3SourceSignals requires out=FILE");
        if (methodOutput != null && methodOutput.equals(output)) {
            throw new IllegalArgumentException("methodOut= must not alias the original out= image");
        }
        trees = Trees.instance(task);
        if (methodOutput != null) {
            methodSignals = new M3MethodSignals(trees, methodOutput,
                    M3SourceSignalPlugin::methodSignature);
        }
        task.addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent event) {
                // No compiler mutation: read complete, attributed declarations after ANALYZE.
            }

            @Override
            public void finished(TaskEvent event) {
                if (event.getKind() == TaskEvent.Kind.ANALYZE) collect(event);
                if (event.getKind() == TaskEvent.Kind.COMPILATION && !written) {
                    written = true;
                    publish();
                    if (methodSignals != null) methodSignals.publish();
                }
            }
        });
    }

    private void collect(TaskEvent event) {
        TypeElement type = event.getTypeElement();
        if (type == null || type.getEnclosingElement().getKind() != ElementKind.PACKAGE) return;
        if (!includeAll && !annotated(type)) return;
        TreePath path = trees.getPath(type);
        if (path == null || !(path.getLeaf() instanceof ClassTree root)) return;
        String qname = type.getQualifiedName().toString();
        if (qname.isEmpty() || facts.containsKey(qname)) return;
        Facts row = new Facts(qname);
        String pkg = type.getEnclosingElement().toString();
        row.add(Family.PACKAGE, "package:" + pkg);
        row.add(Family.CLASS, "class:" + qname);
        row.add(Family.CLASS, "kind:" + type.getKind());
        for (Modifier modifier : type.getModifiers()) row.add(Family.CLASS, "modifier:" + modifier);
        collectAnnotations(type, row);
        if (type.getSuperclass().getKind() != TypeKind.NONE) {
            row.add(Family.INHERITANCE, "extends:" + type.getSuperclass());
            dependencies(type.getSuperclass(), row, new HashSet<>());
        }
        for (TypeMirror iface : type.getInterfaces()) {
            row.add(Family.INHERITANCE, "implements:" + iface);
            dependencies(iface, row, new HashSet<>());
        }
        for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
            row.fieldCount++;
            row.add(Family.FIELDS, "field:" + field.getSimpleName() + ":" + field.asType());
            collectAnnotations(field, row);
            dependencies(field.asType(), row, new HashSet<>());
        }
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            row.methodCount++;
            addMethod(method, row);
        }
        for (ExecutableElement constructor : ElementFilter.constructorsIn(type.getEnclosedElements())) {
            row.constructorCount++;
            addMethod(constructor, row);
        }
        new SourceScanner(row, root).scan(path, null);
        facts.put(qname, row);
        if (methodSignals != null) methodSignals.collect(path);
    }

    private static void addMethod(ExecutableElement method, Facts row) {
        row.add(Family.METHODS, "declaration:" + methodSignature(method));
        for (VariableElement parameter : method.getParameters()) {
            dependencies(parameter.asType(), row, new HashSet<>());
        }
        dependencies(method.getReturnType(), row, new HashSet<>());
        for (TypeMirror thrown : method.getThrownTypes()) {
            dependencies(thrown, row, new HashSet<>());
        }
        collectAnnotations(method, row);
    }

    private static String methodSignature(ExecutableElement method) {
        StringBuilder value = new StringBuilder();
        Element owner = method.getEnclosingElement();
        value.append(owner).append('#').append(method.getSimpleName()).append('(');
        for (VariableElement parameter : method.getParameters()) {
            value.append(parameter.asType()).append(';');
        }
        value.append("):").append(method.getReturnType());
        return value.toString();
    }

    private static void collectAnnotations(Element element, Facts row) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            row.add(Family.ANNOTATIONS, "annotation:" + mirror.getAnnotationType());
            dependencies(mirror.getAnnotationType(), row, new HashSet<>());
        }
    }

    private static boolean annotated(TypeElement type) {
        for (AnnotationMirror mirror : type.getAnnotationMirrors()) {
            if (ANNOTATION.equals(mirror.getAnnotationType().toString())) return true;
        }
        return false;
    }

    /** Bounded traversal of compile-time type expressions, never runtime class loading. */
    private static void dependencies(TypeMirror mirror, Facts row, Set<String> visited) {
        if (mirror == null || visited.size() >= 256) return;
        String identifier = mirror.getKind() + ":" + mirror;
        if (!visited.add(identifier)) return;
        if (mirror instanceof ArrayType array) {
            dependencies(array.getComponentType(), row, visited);
        } else if (mirror instanceof DeclaredType declared) {
            Element element = declared.asElement();
            if (element instanceof TypeElement type) {
                String name = type.getQualifiedName().toString();
                if (!name.isEmpty()) {
                    row.dependencyNames.add(name);
                    row.add(Family.DEPENDENCIES, "depends:" + name);
                }
            }
            for (TypeMirror argument : declared.getTypeArguments()) {
                dependencies(argument, row, visited);
            }
        } else if (mirror instanceof TypeVariable variable) {
            dependencies(variable.getUpperBound(), row, visited);
        } else if (mirror instanceof WildcardType wildcard) {
            dependencies(wildcard.getExtendsBound(), row, visited);
            dependencies(wildcard.getSuperBound(), row, visited);
        } else if (mirror instanceof IntersectionType intersection) {
            for (TypeMirror part : intersection.getBounds()) dependencies(part, row, visited);
        } else if (mirror instanceof UnionType union) {
            for (TypeMirror part : union.getAlternatives()) dependencies(part, row, visited);
        }
    }

    private final class SourceScanner extends TreePathScanner<Void, Void> {
        private final Facts row;
        private final ClassTree root;

        private SourceScanner(Facts row, ClassTree root) {
            this.row = row;
            this.root = root;
        }

        @Override
        public Void visitClass(ClassTree tree, Void unused) {
            // Nested classes are independent semantic owners; do not attribute their calls to parent.
            return tree == root ? super.visitClass(tree, unused) : null;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
            row.callSiteCount++;
            recordCall("call", trees.getElement(getCurrentPath()));
            return super.visitMethodInvocation(tree, unused);
        }

        @Override
        public Void visitNewClass(NewClassTree tree, Void unused) {
            row.allocationSiteCount++;
            recordCall("new", trees.getElement(getCurrentPath()));
            return super.visitNewClass(tree, unused);
        }

        @Override
        public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
            row.memberReferenceCount++;
            recordCall("reference", trees.getElement(getCurrentPath()));
            return super.visitMemberReference(tree, unused);
        }

        private void recordCall(String shape, Element target) {
            if (!(target instanceof ExecutableElement callee)) {
                row.unresolvedCallCount++;
                return;
            }
            String kind;
            if (callee.getKind() == ElementKind.CONSTRUCTOR
                    || callee.getModifiers().contains(Modifier.STATIC)
                    || callee.getModifiers().contains(Modifier.PRIVATE)) {
                kind = "DIRECT";
                row.directCallCount++;
            } else {
                kind = "DISPATCH_CANDIDATE";
                row.dispatchCandidateCount++;
            }
            String key = shape + ':' + kind + ':' + methodSignature(callee);
            row.calls.merge(key, 1, Integer::sum);
            row.add(Family.CALLS, key);
            Element owner = callee.getEnclosingElement();
            if (owner instanceof TypeElement type) {
                String name = type.getQualifiedName().toString();
                if (!name.isEmpty()) {
                    row.dependencyNames.add(name);
                    row.add(Family.DEPENDENCIES, "calls:" + name);
                }
            }
        }

        @Override public Void visitForLoop(ForLoopTree t, Void p) {
            row.loopCount++; return super.visitForLoop(t, p);
        }
        @Override public Void visitEnhancedForLoop(EnhancedForLoopTree t, Void p) {
            row.loopCount++; return super.visitEnhancedForLoop(t, p);
        }
        @Override public Void visitWhileLoop(WhileLoopTree t, Void p) {
            row.loopCount++; return super.visitWhileLoop(t, p);
        }
        @Override public Void visitDoWhileLoop(DoWhileLoopTree t, Void p) {
            row.loopCount++; return super.visitDoWhileLoop(t, p);
        }
        @Override public Void visitIf(IfTree t, Void p) {
            row.branchCount++; return super.visitIf(t, p);
        }
        @Override public Void visitConditionalExpression(ConditionalExpressionTree t, Void p) {
            row.branchCount++; return super.visitConditionalExpression(t, p);
        }
        @Override public Void visitSwitch(SwitchTree t, Void p) {
            row.branchCount++; return super.visitSwitch(t, p);
        }
        @Override public Void visitSwitchExpression(SwitchExpressionTree t, Void p) {
            row.branchCount++; return super.visitSwitchExpression(t, p);
        }
        @Override public Void visitCatch(CatchTree t, Void p) {
            row.catchCount++; return super.visitCatch(t, p);
        }
        @Override public Void visitTry(TryTree t, Void p) {
            row.tryCount++; return super.visitTry(t, p);
        }
        @Override public Void visitLambdaExpression(LambdaExpressionTree t, Void p) {
            row.lambdaCount++; return super.visitLambdaExpression(t, p);
        }
    }

    private static final class Facts {
        private final String name;
        private final Map<Family, TreeSet<Long>> dimensions = new EnumMap<>(Family.class);
        private final Set<String> dependencyNames = new TreeSet<>();
        private final TreeMap<String, Integer> calls = new TreeMap<>();
        private int fieldCount, methodCount, constructorCount;
        private int callSiteCount, allocationSiteCount, memberReferenceCount;
        private int directCallCount, dispatchCandidateCount, unresolvedCallCount;
        private int loopCount, branchCount, catchCount, tryCount, lambdaCount;

        private Facts(String name) {
            this.name = name;
            for (Family family : Family.values()) {
                dimensions.put(family, new TreeSet<>(UNSIGNED));
            }
        }

        private void add(Family family, String token) {
            dimensions.get(family).add(hash64(token));
        }
    }

    private void publish() {
        List<String> lines = new ArrayList<>();
        lines.add("# " + SCHEMA);
        lines.add("# S=scalar counts (exact nonnegative integers); T=hashed token; E=attributed candidate edge");
        lines.add("# hashes and virtual call edges are heuristic, not exact equality or runtime dispatch proof");
        for (Facts row : facts.values()) {
            lines.add(String.join("\t", "S", escape(row.name),
                    Integer.toString(row.fieldCount), Integer.toString(row.methodCount),
                    Integer.toString(row.constructorCount), Integer.toString(row.dependencyNames.size()),
                    Integer.toString(row.callSiteCount), Integer.toString(row.allocationSiteCount),
                    Integer.toString(row.memberReferenceCount), Integer.toString(row.directCallCount),
                    Integer.toString(row.dispatchCandidateCount), Integer.toString(row.unresolvedCallCount),
                    Integer.toString(row.loopCount), Integer.toString(row.branchCount),
                    Integer.toString(row.tryCount), Integer.toString(row.catchCount),
                    Integer.toString(row.lambdaCount)));
            for (Family family : Family.values()) {
                for (long token : row.dimensions.get(family)) {
                    lines.add("T\t" + escape(row.name) + "\t" + family + "\t"
                            + Long.toUnsignedString(token, 16));
                }
            }
            for (Map.Entry<String, Integer> edge : row.calls.entrySet()) {
                lines.add("E\t" + escape(row.name) + "\t" + escape(edge.getKey()) + "\t"
                        + edge.getValue());
            }
        }
        String content = String.join("\n", lines) + "\n";
        content += "# sha256\t" + sha256(content) + "\n";
        try {
            Path parent = output.getParent();
            if (parent == null) throw new IOException("output has no parent directory");
            Files.createDirectories(parent);
            if (Files.isRegularFile(output)
                    && content.equals(Files.readString(output, StandardCharsets.UTF_8))) return;
            Path staged = Files.createTempFile(parent, ".m3signals-", ".tmp");
            try {
                Files.writeString(staged, content, StandardCharsets.UTF_8);
                try {
                    Files.move(staged, output, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(staged, output, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(staged);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("failed to publish compiler source signals", failure);
        }
    }

    private static long hash64(String text) {
        long value = 0xcbf29ce484222325L;
        for (byte octet : text.getBytes(StandardCharsets.UTF_8)) {
            value = (value ^ (octet & 0xffL)) * 0x100000001b3L;
        }
        return value;
    }

    private static String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
