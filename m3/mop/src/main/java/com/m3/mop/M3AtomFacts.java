// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreeScanner;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lexical effect, loop and literal facts of one atom body, read from the javac tree.
 *
 * <p>MUTATES = assignment through a member select or array element; ASSIGNS = assignment to a
 * simple name; IO = known I/O receiver or I/O type allocation; CALLS = any invocation. A repeated
 * non-trivial literal is a CONST_HOIST signal. Ported from Synexia M3AtomFacts (#13966).</p>
 */
//FLLDCIM: MOP — effect classes are syntax evidence, never a purity proof
final class M3AtomFacts extends TreeScanner<Void, Void> {
    enum Effect { ALLOCATES, ASSIGNS, CALLS, IO, MUTATES, SYNC, THROWS }

    private static final Set<String> IO_TYPES = Set.of(
            "FileInputStream", "FileOutputStream", "FileReader", "FileWriter",
            "RandomAccessFile", "Socket", "ServerSocket", "ProcessBuilder", "PrintWriter",
            "PrintStream", "Scanner", "ZipFile", "URL");
    private static final Set<String> IO_RECEIVERS = Set.of(
            "Files", "System.out", "System.err", "System.in", "Runtime.getRuntime()",
            "Channels", "FileChannel", "Desktop");
    private static final Set<String> TRIVIAL_LITERALS = Set.of("0", "1", "-1", "", "true", "false");

    private final EnumSet<Effect> effects = EnumSet.noneOf(Effect.class);
    private final Map<String, Integer> literals = new HashMap<>();
    private boolean loop;

    private M3AtomFacts() { }

    static M3AtomFacts of(Tree body) {
        M3AtomFacts facts = new M3AtomFacts();
        if (body != null) facts.scan(body, null);
        return facts;
    }

    String effects() {
        String joined = effects.stream().map(Enum::name).collect(Collectors.joining("|"));
        return joined.isEmpty() ? "PURE" : joined;
    }

    boolean throwsStatement() { return effects.contains(Effect.THROWS); }

    boolean loop() { return loop; }

    boolean repeatedLiteral() { return literals.values().stream().anyMatch(count -> count > 1); }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
        effects.add(Effect.CALLS);
        if (node.getMethodSelect() instanceof MemberSelectTree select
                && IO_RECEIVERS.contains(select.getExpression().toString())) {
            effects.add(Effect.IO);
        }
        return super.visitMethodInvocation(node, unused);
    }

    @Override
    public Void visitNewClass(NewClassTree node, Void unused) {
        effects.add(Effect.ALLOCATES);
        String type = node.getIdentifier().toString().replaceAll("<.*", "");
        if (IO_TYPES.contains(type.substring(type.lastIndexOf('.') + 1))) effects.add(Effect.IO);
        return super.visitNewClass(node, unused);
    }

    @Override
    public Void visitNewArray(NewArrayTree node, Void unused) {
        effects.add(Effect.ALLOCATES);
        return super.visitNewArray(node, unused);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void unused) {
        target(node.getVariable());
        return super.visitAssignment(node, unused);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void unused) {
        target(node.getVariable());
        return super.visitCompoundAssignment(node, unused);
    }

    @Override
    public Void visitUnary(UnaryTree node, Void unused) {
        if (node.getKind().name().endsWith("CREMENT")) target(node.getExpression());
        return super.visitUnary(node, unused);
    }

    @Override
    public Void visitSynchronized(SynchronizedTree node, Void unused) {
        effects.add(Effect.SYNC);
        return super.visitSynchronized(node, unused);
    }

    @Override
    public Void visitThrow(ThrowTree node, Void unused) {
        effects.add(Effect.THROWS);
        return super.visitThrow(node, unused);
    }

    @Override
    public Void visitForLoop(ForLoopTree node, Void unused) {
        loop = true;
        return super.visitForLoop(node, unused);
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
        loop = true;
        return super.visitEnhancedForLoop(node, unused);
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, Void unused) {
        loop = true;
        return super.visitWhileLoop(node, unused);
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, Void unused) {
        loop = true;
        return super.visitDoWhileLoop(node, unused);
    }

    @Override
    public Void visitLiteral(LiteralTree node, Void unused) {
        String value = String.valueOf(node.getValue());
        if (node.getValue() != null && !TRIVIAL_LITERALS.contains(value)) {
            literals.merge(node.getKind().name() + ':' + value, 1, Integer::sum);
        }
        return super.visitLiteral(node, unused);
    }

    private void target(ExpressionTree variable) {
        Tree.Kind kind = variable.getKind();
        boolean external = kind == Tree.Kind.MEMBER_SELECT || kind == Tree.Kind.ARRAY_ACCESS;
        effects.add(external ? Effect.MUTATES : Effect.ASSIGNS);
    }
}
