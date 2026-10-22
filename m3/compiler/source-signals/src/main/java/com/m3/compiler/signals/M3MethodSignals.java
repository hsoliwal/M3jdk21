/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.compiler.signals;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.WhileLoopTree;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

/**
 * Optional attributed, method-level source facts from the existing M3SourceSignals javac plugin.
 * No AST/bytecode mutation and no new javac plugin. Records are method identifiers, not runtime
 * call counts, exact dynamic-dispatch targets, a purity/escape proof or JIT profiling evidence.
 * The original S/T/E image remains byte-for-byte unchanged when methodOut is not supplied.
 */
final class M3MethodSignals {
    static final String SCHEMA = "M3JDK21-METHOD-SIGNALS/1";
    private static final int MAX_METHODS = 1_000_000;
    private static final int MAX_CALL_SHAPES = 2_000_000;

    private final Trees trees;
    private final Path output;
    private final Function<ExecutableElement, String> signature;
    private final TreeMap<String, MethodFacts> methods = new TreeMap<>();
    private int callShapes;

    M3MethodSignals(Trees trees, Path output,
                    Function<ExecutableElement, String> signature) {
        this.trees = Objects.requireNonNull(trees, "trees");
        this.output = Objects.requireNonNull(output, "output");
        this.signature = Objects.requireNonNull(signature, "signature");
    }

    /** Collect only the already-admitted top-level class; nested types own separate evidence. */
    void collect(TreePath attributedTypePath) {
        Objects.requireNonNull(attributedTypePath, "attributedTypePath");
        if (!(attributedTypePath.getLeaf() instanceof ClassTree root)) {
            throw new IllegalArgumentException("expected attributed source class");
        }
        new SourceMethods(root).scan(attributedTypePath, null);
    }

    private final class SourceMethods extends TreePathScanner<Void, Void> {
        private final ClassTree root;
        private MethodFacts current;

        SourceMethods(ClassTree root) { this.root = root; }

        @Override public Void visitClass(ClassTree tree, Void unused) {
            return tree == root ? super.visitClass(tree, unused) : null;
        }

        @Override public Void visitMethod(MethodTree tree, Void unused) {
            Element symbol = trees.getElement(getCurrentPath());
            if (!(symbol instanceof ExecutableElement method)) return null;
            String id = signature.apply(method);
            MethodFacts facts = methods.get(id);
            if (facts != null) {
                throw new IllegalStateException("duplicate attributed method: " + id);
            }
            if (methods.size() >= MAX_METHODS) throw new IllegalStateException("method budget exceeded");
            facts = new MethodFacts(id);
            methods.put(id, facts);
            MethodFacts previous = current;
            current = facts;
            try { return super.visitMethod(tree, unused); }
            finally { current = previous; }
        }

        @Override public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
            if (current != null) {
                current.invocations++;
                edge("call", trees.getElement(getCurrentPath()));
            }
            return super.visitMethodInvocation(tree, unused);
        }
        @Override public Void visitNewClass(NewClassTree tree, Void unused) {
            if (current != null) {
                current.newObjects++;
                edge("new", trees.getElement(getCurrentPath()));
            }
            return super.visitNewClass(tree, unused);
        }
        @Override public Void visitNewArray(NewArrayTree tree, Void unused) {
            if (current != null) current.newArrays++;
            return super.visitNewArray(tree, unused);
        }
        @Override public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
            if (current != null) {
                current.memberReferences++;
                edge("reference", trees.getElement(getCurrentPath()));
            }
            return super.visitMemberReference(tree, unused);
        }
        @Override public Void visitForLoop(ForLoopTree tree, Void p) {
            if (current != null) current.loops++;
            return super.visitForLoop(tree, p);
        }
        @Override public Void visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
            if (current != null) current.loops++;
            return super.visitEnhancedForLoop(tree, p);
        }
        @Override public Void visitWhileLoop(WhileLoopTree tree, Void p) {
            if (current != null) current.loops++;
            return super.visitWhileLoop(tree, p);
        }
        @Override public Void visitDoWhileLoop(DoWhileLoopTree tree, Void p) {
            if (current != null) current.loops++;
            return super.visitDoWhileLoop(tree, p);
        }
        @Override public Void visitIf(IfTree tree, Void p) {
            if (current != null) current.branches++;
            return super.visitIf(tree, p);
        }
        @Override public Void visitConditionalExpression(ConditionalExpressionTree tree, Void p) {
            if (current != null) current.branches++;
            return super.visitConditionalExpression(tree, p);
        }
        @Override public Void visitSwitch(SwitchTree tree, Void p) {
            if (current != null) current.branches++;
            return super.visitSwitch(tree, p);
        }
        @Override public Void visitSwitchExpression(SwitchExpressionTree tree, Void p) {
            if (current != null) current.branches++;
            return super.visitSwitchExpression(tree, p);
        }
        @Override public Void visitTry(TryTree tree, Void p) {
            if (current != null) current.tries++;
            return super.visitTry(tree, p);
        }
        @Override public Void visitCatch(CatchTree tree, Void p) {
            if (current != null) current.catches++;
            return super.visitCatch(tree, p);
        }
        @Override public Void visitLambdaExpression(LambdaExpressionTree tree, Void p) {
            if (current != null) current.lambdas++;
            return super.visitLambdaExpression(tree, p);
        }
        @Override public Void visitAssignment(AssignmentTree tree, Void p) {
            if (fieldWrite(tree.getVariable())) current.fieldWrites++;
            return super.visitAssignment(tree, p);
        }
        @Override public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
            if (fieldWrite(tree.getVariable())) current.fieldWrites++;
            return super.visitCompoundAssignment(tree, p);
        }
        @Override public Void visitUnary(UnaryTree tree, Void p) {
            if (tree.getKind() == Tree.Kind.PREFIX_INCREMENT
                    || tree.getKind() == Tree.Kind.POSTFIX_INCREMENT
                    || tree.getKind() == Tree.Kind.PREFIX_DECREMENT
                    || tree.getKind() == Tree.Kind.POSTFIX_DECREMENT) {
                if (fieldWrite(tree.getExpression())) current.fieldWrites++;
            }
            return super.visitUnary(tree, p);
        }
        @Override public Void visitMemberSelect(MemberSelectTree tree, Void p) {
            if (current != null && isField(trees.getElement(getCurrentPath()))) {
                current.fieldReferences++;
            }
            return super.visitMemberSelect(tree, p);
        }
        @Override public Void visitIdentifier(com.sun.source.tree.IdentifierTree tree, Void p) {
            if (current != null && isField(trees.getElement(getCurrentPath()))) {
                current.fieldReferences++;
            }
            return super.visitIdentifier(tree, p);
        }

        private boolean fieldWrite(Tree variable) {
            return current != null && isField(trees.getElement(new TreePath(getCurrentPath(), variable)));
        }

        private void edge(String shape, Element element) {
            if (!(element instanceof ExecutableElement callee)) {
                current.unresolved++;
                return;
            }
            boolean direct = callee.getKind() == ElementKind.CONSTRUCTOR
                    || callee.getModifiers().contains(Modifier.STATIC)
                    || callee.getModifiers().contains(Modifier.PRIVATE);
            if (direct) current.direct++; else current.dispatchCandidates++;
            String key = shape + ':' + (direct ? "DIRECT" : "DISPATCH_CANDIDATE")
                    + ':' + signature.apply(callee);
            if (!current.calls.containsKey(key) && ++callShapes > MAX_CALL_SHAPES) {
                throw new IllegalStateException("method edge budget exceeded");
            }
            current.calls.merge(key, 1, Math::addExact);
        }
    }

    private static boolean isField(Element element) {
        return element != null && element.getKind() == ElementKind.FIELD;
    }

    /** A scalar row identifies a method by its attributed signature and fourteen source counts. */
    private static final class MethodFacts {
        final String id;
        final TreeMap<String, Integer> calls = new TreeMap<>();
        int invocations, newObjects, newArrays, memberReferences, direct, dispatchCandidates;
        int unresolved, loops, branches, tries, catches, lambdas, fieldReferences, fieldWrites;
        MethodFacts(String id) { this.id = id; }
    }

    /** Publish atomically when possible; identical output is an exact no-op. */
    void publish() {
        List<String> lines = new ArrayList<>();
        lines.add("# " + SCHEMA);
        lines.add("# M=exact source AST counts; C=attributed method-call candidates, not runtime observations");
        lines.add("# method identity is the attributed owner/signature, not a lexical hash");
        for (MethodFacts row : methods.values()) {
            lines.add(String.join("\t", "M", escape(row.id),
                    Integer.toString(row.invocations), Integer.toString(row.newObjects),
                    Integer.toString(row.newArrays), Integer.toString(row.memberReferences),
                    Integer.toString(row.direct), Integer.toString(row.dispatchCandidates),
                    Integer.toString(row.unresolved), Integer.toString(row.loops),
                    Integer.toString(row.branches), Integer.toString(row.tries),
                    Integer.toString(row.catches), Integer.toString(row.lambdas),
                    Integer.toString(row.fieldReferences), Integer.toString(row.fieldWrites)));
            for (var edge : row.calls.entrySet()) {
                lines.add("C\t" + escape(row.id) + "\t" + escape(edge.getKey()) + "\t"
                        + edge.getValue());
            }
        }
        String payload = String.join("\n", lines) + "\n";
        String sealed = payload + "# sha256\t" + sha256(payload) + "\n";
        try {
            Path parent = output.getParent();
            if (parent == null) throw new IOException("method output has no parent");
            Files.createDirectories(parent);
            if (Files.isRegularFile(output)
                    && sealed.equals(Files.readString(output, StandardCharsets.UTF_8))) return;
            Path staged = Files.createTempFile(parent, ".m3-method-", ".tmp");
            try {
                Files.writeString(staged, sealed, StandardCharsets.UTF_8);
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
            throw new IllegalStateException("failed to publish method source signals", failure);
        }
    }

    private static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
