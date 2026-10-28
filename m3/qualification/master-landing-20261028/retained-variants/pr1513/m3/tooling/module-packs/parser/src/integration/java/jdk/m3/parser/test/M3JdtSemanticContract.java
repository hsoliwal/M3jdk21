/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;

/** Real JDT headless compiler, binding, recovery and source-rewrite qualification. */
public final class M3JdtSemanticContract {
    private static int checks;
    private static final M3ASTIngress.Limits LIMITS = new M3ASTIngress.Limits(1_000_000, 100_000, 256);
    private M3JdtSemanticContract() { }
    private static void eq(Object expected, Object actual, String label) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(label + ": " + expected + " != " + actual);
    }
    private static void ok(boolean value, String label) { eq(true, value, label); }
    private static CompilationUnit parse(String text, boolean bindings) {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);
        parser.setCompilerOptions(options);
        parser.setSource(text.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("Sample.java");
        parser.setEnvironment(new String[0], new String[0], null, true);
        parser.setResolveBindings(bindings);
        parser.setBindingsRecovery(true);
        parser.setStatementsRecovery(true);
        return (CompilationUnit) parser.createAST(null);
    }
    private static IBinding binding(ASTNode node) {
        if (node instanceof Name name) return name.resolveBinding();
        if (node instanceof MethodDeclaration declaration) return declaration.resolveBinding();
        if (node instanceof VariableDeclaration declaration) return declaration.resolveBinding();
        if (node instanceof TypeDeclaration declaration) return declaration.resolveBinding();
        if (node instanceof MethodInvocation invocation) return invocation.resolveMethodBinding();
        return null;
    }
    private static List<ASTNode> nodes(CompilationUnit unit) {
        List<ASTNode> values = new ArrayList<>();
        unit.accept(new ASTVisitor(true) {
            @Override public void preVisit(ASTNode node) { values.add(node); }
        });
        return values;
    }
    private static M3ASTIndex verify(String text, boolean resolve) {
        CompilationUnit unit = parse(text, resolve);
        M3File source = M3File.of(URI.create("memory:/Sample.java"), text);
        long generation = unit.getAST().modificationCount();
        M3ASTIndex syntax = M3ASTIngress.eclipseJdt(source, unit, LIMITS);
        M3ASTIndex index = M3ASTIngress.eclipseJdtResolved(source, unit, LIMITS);
        ok(!syntax.hasSemanticProjection(), "legacy call is syntax only");
        ok(index.hasSemanticProjection(), "explicit semantic projection");
        ok(index.source() == source, "same immutable source owner");
        eq(generation, unit.getAST().modificationCount(), "original AST not mutated");
        List<ASTNode> original = nodes(unit);
        eq(original.size(), index.nodeCount(), "real visitor occurrence count");
        int bound = 0;
        for (int row = 0; row < original.size(); row++) {
            ASTNode node = original.get(row);
            eq("JDT_" + node.getClass().getSimpleName(), index.kind(row), "visitor order");
            eq(syntax.kind(row), index.kind(row), "legacy structural kind");
            eq(syntax.parent(row), index.parent(row), "legacy parent");
            eq(syntax.start(row), index.start(row), "legacy start");
            eq(syntax.end(row), index.end(row), "legacy end");
            eq(node.getStartPosition(), index.start(row), "UTF16 offset");
            if (index.hasExactRange(row)) {
                eq(text.substring(index.start(row), index.end(row)), index.sourceText(row), "exact spelling");
            }
            IBinding binding = binding(node);
            if (binding != null) {
                bound++;
                eq(binding.getKey(), index.bindingKey(row), "binding key");
                eq(binding.getKind(), index.bindingKind(row), "binding kind");
                eq(binding.getModifiers(), index.bindingModifiers(row), "binding modifiers");
                eq((binding.isRecovered() ? M3ASTIndex.BINDING_RECOVERED : 0)
                        | (binding.isDeprecated() ? M3ASTIndex.BINDING_DEPRECATED : 0),
                        index.bindingFlags(row), "binding flags");
            }
            if (node instanceof org.eclipse.jdt.core.dom.Expression expression) {
                var type = expression.resolveTypeBinding();
                eq(type == null ? null : type.getKey(), index.typeKey(row), "expression type");
            }
        }
        if (resolve && !text.startsWith("module")) ok(bound > 0, "real resolved bindings observed");
        if (!resolve) {
            for (int row = 0; row < index.nodeCount(); row++) eq(null, index.bindingKey(row), "no invented bindings");
        }
        var problems = unit.getProblems();
        eq(problems.length, index.problemCount(), "diagnostics count");
        for (int i = 0; i < problems.length; i++) {
            var problem = problems[i];
            eq(problem.getID(), index.problemId(i), "diagnostic id");
            eq(problem.getMessage(), index.problemMessage(i), "diagnostic message");
            eq(problem.getSourceStart(), index.problemStart(i), "diagnostic start");
            eq(problem.getSourceEnd() + 1, index.problemEnd(i), "diagnostic half-open end");
            eq((problem.isError() ? M3ASTIndex.PROBLEM_ERROR : 0)
                    | (problem.isWarning() ? M3ASTIndex.PROBLEM_WARNING : 0), index.problemFlags(i), "diagnostic flags");
        }
        return index;
    }
    private static void rewrite() throws Exception {
        String text = "class Sample { int count; int read() { return count; } } // count stays\n";
        CompilationUnit unit = parse(text, true);
        M3ASTIndex before = M3ASTIngress.eclipseJdtResolved(M3File.of(URI.create("memory:/Sample.java"), text), unit, LIMITS);
        ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
        unit.accept(new ASTVisitor() {
            @Override public boolean visit(SimpleName name) {
                if (name.getIdentifier().equals("count")) rewrite.replace(name, unit.getAST().newSimpleName("total"), null);
                return true;
            }
        });
        Document document = new Document(text);
        rewrite.rewriteAST(document, JavaCore.getOptions()).apply(document);
        String afterText = document.get();
        ok(afterText.contains("// count stays"), "JDT source rewrite preserves comment");
        ok(afterText.contains("return total"), "JDT rewrite updates reference");
        M3ASTIndex after = verify(afterText, true);
        eq(text, before.source().text(), "old snapshot retained");
        ok(!before.source().utf16Sha256().equals(after.source().utf16Sha256()), "edit invalidates source generation");
        eq(0, after.problemCount(), "rewritten source resolves");
    }
    private static void ranges() {
        M3File source = M3File.of(URI.create("memory:/ranges"), "0123456789");
        for (int depth : new int[] { 1, 2, 10, 1000 }) {
            var builder = M3ASTIndex.builder(source, M3ASTIndex.Domain.ECLIPSE_JDT, depth + 2);
            int root = builder.add(-1, "ROOT", -1, -1, 0), parent = root;
            for (int i = 0; i < depth; i++) parent = builder.add(parent, "UNKNOWN", -1, -1, 0);
            builder.add(parent, "LEAF", 8, 9, M3ASTIndex.EXACT_RANGE);
            try { builder.completeRange(root, 0, 5, true); throw new AssertionError("excluded descendant accepted"); }
            catch (IllegalArgumentException expected) { checks++; }
            builder.completeRange(root, 0, 10, true);
            eq(depth + 2, builder.seal().nodeCount(), "refusal is non-mutating");
            var second = M3ASTIndex.builder(source, M3ASTIndex.Domain.ECLIPSE_JDT, depth + 2);
            parent = second.add(-1, "ROOT", 0, 5, M3ASTIndex.EXACT_RANGE);
            for (int i = 0; i < depth; i++) parent = second.add(parent, "UNKNOWN", -1, -1, 0);
            try { second.add(parent, "LEAF", 8, 9, M3ASTIndex.EXACT_RANGE); throw new AssertionError("ancestor escape accepted"); }
            catch (IllegalArgumentException expected) { checks++; }
        }
    }
    public static void main(String[] args) throws Exception {
        System.out.println("JDT_ORIGIN=" + ASTParser.class.getProtectionDomain().getCodeSource().getLocation());
        for (String text : List.of(
                "class Sample { int n; int run(int x) { return x + n; } }",
                "import java.util.*; class Sample { List<String> xs = List.of(\"a\"); int run() { return xs.size(); } }",
                "record Sample(int x) { int twice() { return x * 2; } }",
                "sealed interface S permits Sample {} final class Sample implements S {}",
                "class Sample { String s = \"\"\"\nclass Fake { /* decoy */ }\n\"\"\"; String emoji = \"😀\"; }",
                "/** Documentation {@link String}. */ class Sample { @Deprecated int old() { return 1; } int n = old(); }",
                "class Sample { Missing unresolved; int f() { return absent; } }",
                "class Sample { int f() { return ; } }",
                "class Sample { Object f(Object x) { return switch(x) { case String s -> s.length(); default -> 0; }; } }")) {
            verify(text, true); verify(text, false);
        }
        rewrite(); ranges();
        System.out.println("M3_JDT_SEMANTICS_PASS checks=" + checks);
    }
}
