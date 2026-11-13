/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal and contributors - real source-built JDT ingress contracts
 *******************************************************************************/
package org.eclipse.jdt.core.tests.dom;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import junit.framework.TestCase;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;

/**
 * Finite integration proof using the original JDT parser, public structural
 * descriptors, mutable child lists, bindings and ASTRewrite. The receiver builds
 * the tested JDT classes from the locked source tree; no shape doubles are used.
 */
public class M3JdtIngressProof extends TestCase {

    private record Occurrence(ASTNode node, int parent, int depth, List<Integer> children) { }

    private static final class Oracle {
        final List<Occurrence> rows = new ArrayList<>();
        final IdentityHashMap<ASTNode, Integer> coordinates = new IdentityHashMap<>();
        int maximumDepth;

        Oracle(ASTNode root) {
            append(root, -1, 1);
        }

        private int append(ASTNode node, int parent, int depth) {
            int row = this.rows.size();
            assertNull("a DOM occurrence has exactly one structural parent",
                    this.coordinates.put(node, row));
            List<Integer> children = new ArrayList<>();
            this.rows.add(new Occurrence(node, parent, depth, children));
            this.maximumDepth = Math.max(this.maximumDepth, depth);
            for (Object value : node.structuralPropertiesForType()) {
                StructuralPropertyDescriptor descriptor = (StructuralPropertyDescriptor) value;
                if (descriptor.isChildProperty()) {
                    ASTNode child = (ASTNode) node.getStructuralProperty(descriptor);
                    if (child != null) children.add(append(child, row, depth + 1));
                } else if (descriptor.isChildListProperty()) {
                    for (Object child : (List<?>) node.getStructuralProperty(descriptor)) {
                        children.add(append((ASTNode) child, row, depth + 1));
                    }
                }
            }
            return row;
        }
    }

    private static Map<String, String> options() {
        Map<String, String> options = new HashMap<>();
        JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);
        options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.DISABLED);
        return options;
    }

    private static CompilationUnit parse(String text, boolean bindings, boolean recovery) {
        ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setCompilerOptions(options());
        parser.setSource(text.toCharArray());
        parser.setUnitName("X.java");
        parser.setResolveBindings(bindings);
        parser.setBindingsRecovery(recovery);
        parser.setStatementsRecovery(recovery);
        if (bindings) parser.setEnvironment(new String[0], new String[0], new String[0], true);
        return (CompilationUnit) parser.createAST(null);
    }

    private static void assertValid(CompilationUnit unit) {
        for (IProblem problem : unit.getProblems()) {
            assertFalse(problem.toString(), problem.isError());
        }
    }

    private static M3File source(String name, String text) {
        return M3File.of(URI.create("memory:///" + name + ".java"), text);
    }

    private static M3ASTIndex compare(String name, M3File source, ASTNode root) {
        Oracle oracle = new Oracle(root);
        M3ASTIngress.Limits limits = new M3ASTIngress.Limits(
                Math.max(1, source.length()), oracle.rows.size(), oracle.maximumDepth);
        M3ASTIndex actual = M3ASTIngress.eclipseJdt(source, root, limits);
        assertSame(source, actual.source());
        assertSame(source.text(), actual.source().text());
        assertEquals(M3ASTIndex.Domain.ECLIPSE_JDT, actual.domain());
        assertEquals(oracle.rows.size(), actual.nodeCount());
        int[] next = new int[actual.nodeCount()];
        Arrays.fill(next, -1);
        for (Occurrence owner : oracle.rows) {
            for (int index = 1; index < owner.children().size(); index++) {
                next[owner.children().get(index - 1)] = owner.children().get(index);
            }
        }
        int recovered = 0;
        for (int row = 0; row < actual.nodeCount(); row++) {
            Occurrence expected = oracle.rows.get(row);
            ASTNode node = expected.node();
            String where = name + " row " + row + " " + node.getClass().getSimpleName();
            assertEquals(where, "JDT_" + node.getClass().getSimpleName(), actual.kind(row));
            assertEquals(where, expected.parent(), actual.parent(row));
            if (expected.parent() >= 0) {
                assertSame(where, oracle.rows.get(expected.parent()).node(), node.getParent());
            }
            assertEquals(where, expected.children().size(), actual.childCount(row));
            assertEquals(where, expected.children().isEmpty() ? -1 : expected.children().getFirst().intValue(),
                    actual.firstChild(row));
            assertEquals(where, next[row], actual.nextSibling(row));
            int start = node.getStartPosition();
            int end = start < 0 ? -1 : start + node.getLength();
            assertEquals(where, start, actual.start(row));
            assertEquals(where, end, actual.end(row));
            int flags = M3ASTIndex.SYNTAX_ONLY;
            if (start >= 0) flags |= M3ASTIndex.EXACT_RANGE;
            if ((node.getFlags() & (ASTNode.RECOVERED | ASTNode.MALFORMED)) != 0) {
                flags |= M3ASTIndex.RECOVERED;
                recovered++;
            }
            assertEquals(where, flags, actual.flags(row));
            assertEquals(where, start >= 0, actual.hasExactRange(row));
            if (start >= 0) {
                assertEquals(where, source.text().substring(start, end), actual.sourceText(row));
            } else {
                try {
                    actual.sourceText(row);
                    fail(where + " unknown positions must not materialize source text");
                } catch (IllegalStateException expectedRefusal) {
                    assertTrue(expectedRefusal.getMessage().contains("range"));
                }
            }
        }
        System.out.println("M3JDT_REAL_INGRESS fixture=" + name + " nodes=" + actual.nodeCount()
                + " depth=" + oracle.maximumDepth + " recovered=" + recovered);
        return actual;
    }

    private static String fingerprint(M3ASTIndex index) {
        StringBuilder result = new StringBuilder(index.source().utf16Sha256());
        for (int row = 0; row < index.nodeCount(); row++) {
            result.append('|').append(index.kind(row)).append(':').append(index.parent(row))
                    .append(':').append(index.firstChild(row)).append(':').append(index.nextSibling(row))
                    .append(':').append(index.childCount(row)).append(':').append(index.start(row))
                    .append(':').append(index.end(row)).append(':').append(index.flags(row));
        }
        return result.toString();
    }

    private static void assertRefused(String messagePart, Runnable operation) {
        try {
            operation.run();
            fail("expected bounded refusal: " + messagePart);
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.toString(), expected.getMessage().contains(messagePart));
        }
    }

    private static Block body(CompilationUnit unit, int method) {
        return ((TypeDeclaration) unit.types().getFirst()).getMethods()[method].getBody();
    }

    public void testJava21RecordsPatternsGuardsAndUtf16Ranges() {
        String text = """
                /** Supplementary character: 🙂 */
                record Pair(int left, int right) {}
                class X {
                    int choose(Object value) {
                        return switch (value) {
                            case Pair(int left, int right) when left > right -> left;
                            case String label -> label.length();
                            default -> 0;
                        };
                    }
                }
                """;
        CompilationUnit unit = parse(text, false, false);
        assertValid(unit);
        assertTrue(text.length() > text.codePointCount(0, text.length()));
        M3ASTIndex index = compare("records-patterns-utf16", source("X", text), unit);
        List<String> kinds = new ArrayList<>();
        for (int row = 0; row < index.nodeCount(); row++) kinds.add(index.kind(row));
        assertTrue(kinds.toString(), kinds.contains("JDT_RecordDeclaration"));
        assertTrue(kinds.toString(), kinds.contains("JDT_RecordPattern"));
        assertTrue(kinds.toString(), kinds.contains("JDT_GuardedPattern"));
    }

    public void testRecoveryFlagsAndOriginalRanges() {
        String text = "class X { void run() { if (true) { call( ; }";
        CompilationUnit unit = parse(text, false, true);
        assertTrue(unit.getProblems().length > 0);
        M3ASTIndex index = compare("recovery", source("Broken", text), unit);
        boolean recovered = false;
        for (int row = 0; row < index.nodeCount(); row++) {
            recovered |= (index.flags(row) & M3ASTIndex.RECOVERED) != 0;
        }
        assertTrue("fixture must exercise a real JDT recovery/malformed flag", recovered);
    }

    public void testWideListsAndIndependentResourceLimits() {
        StringBuilder input = new StringBuilder("class X { void run() {\n");
        for (int index = 0; index < 96; index++) input.append("call(").append(index).append(");\n");
        String text = input.append("} }\n").toString();
        CompilationUnit unit = parse(text, false, false);
        assertValid(unit);
        assertEquals(96, body(unit, 0).statements().size());
        M3File file = source("Wide", text);
        compare("wide-lists", file, unit);
        Oracle oracle = new Oracle(unit);
        assertRefused("node budget", () -> M3ASTIngress.eclipseJdt(file, unit,
                new M3ASTIngress.Limits(text.length(), oracle.rows.size() - 1, oracle.maximumDepth)));
        assertRefused("depth budget", () -> M3ASTIngress.eclipseJdt(file, unit,
                new M3ASTIngress.Limits(text.length(), oracle.rows.size(), oracle.maximumDepth - 1)));
        assertRefused("source", () -> M3ASTIngress.eclipseJdt(file, unit,
                new M3ASTIngress.Limits(text.length() - 1, oracle.rows.size(), oracle.maximumDepth)));
    }

    @SuppressWarnings("unchecked")
    public void testSameParentEditsAndUnknownPositionsKeepOldSnapshot() {
        String text = "class X { void run() { first(); second(); third(); } }";
        CompilationUnit unit = parse(text, false, false);
        assertValid(unit);
        M3File file = source("Edits", text);
        M3ASTIndex before = compare("before-list-edits", file, unit);
        String sealed = fingerprint(before);
        List<Statement> statements = body(unit, 0).statements();
        Statement first = statements.removeFirst();
        statements.add(first);
        EmptyStatement inserted = unit.getAST().newEmptyStatement();
        statements.add(1, inserted);
        assertEquals(-1, inserted.getStartPosition());
        M3ASTIndex after = compare("after-list-edits", file, unit);
        assertEquals(before.nodeCount() + 1, after.nodeCount());
        assertEquals(sealed, fingerprint(before));
        assertSame(first, statements.getLast());
        int insertedRow = new Oracle(unit).coordinates.get(inserted);
        assertFalse(after.hasExactRange(insertedRow));
        assertEquals(M3ASTIndex.NO_RANGE, after.start(insertedRow));
        assertEquals(M3ASTIndex.NO_RANGE, after.end(insertedRow));
    }

    public void testAstRewriteThenReparseCreatesNewSourceGeneration() throws Exception {
        String text = "class X { void run() { /* 🙂 */ first(); second(); } }";
        CompilationUnit unit = parse(text, false, false);
        assertValid(unit);
        M3File original = source("Rewrite", text);
        M3ASTIndex before = compare("before-rewrite", original, unit);
        String sealed = fingerprint(before);
        Block block = body(unit, 0);
        ASTNode first = (ASTNode) block.statements().getFirst();
        ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
        rewrite.remove(first, null);
        rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY)
                .insertLast(unit.getAST().newEmptyStatement(), null);
        assertSame(first, block.statements().getFirst());
        assertEquals(2, block.statements().size());
        Document document = new Document(text);
        rewrite.rewriteAST(document, options()).apply(document);
        String edited = document.get();
        assertFalse(text.equals(edited));
        CompilationUnit reparsed = parse(edited, false, false);
        assertValid(reparsed);
        M3File generation = source("Rewrite", edited);
        compare("rewrite-reparsed", generation, reparsed);
        assertFalse(original.utf16Sha256().equals(generation.utf16Sha256()));
        assertEquals(sealed, fingerprint(before));
        assertSame(first, block.statements().getFirst());
    }

    @SuppressWarnings("unchecked")
    public void testCrossParentMoveWithStaleRangeIsExplicitlyRefused() {
        String text = "class X { void a() { first(); } void b() { second(); } }";
        CompilationUnit unit = parse(text, false, false);
        assertValid(unit);
        M3File file = source("StaleMove", text);
        M3ASTIndex before = compare("before-cross-parent-move", file, unit);
        String sealed = fingerprint(before);
        List<Statement> from = body(unit, 0).statements();
        List<Statement> to = body(unit, 1).statements();
        Statement moved = from.removeFirst();
        to.add(moved);
        assertSame(body(unit, 1), moved.getParent());
        assertTrue(moved.getStartPosition() < body(unit, 1).getStartPosition());
        assertRefused("child escapes known parent range", () ->
                M3ASTIngress.eclipseJdt(file, unit, M3ASTIngress.Limits.defaults()));
        assertEquals(sealed, fingerprint(before));
    }

    public void testBindingsRemainWithTheOriginalJdtOwner() {
        String text = "record X(int value) { int doubled() { return value * 2; } }";
        CompilationUnit unit = parse(text, true, false);
        assertValid(unit);
        RecordDeclaration type = (RecordDeclaration) unit.types().getFirst();
        ITypeBinding binding = type.resolveBinding();
        assertNotNull(binding);
        assertEquals("X", binding.getName());
        M3ASTIndex index = compare("resolved-record", source("X", text), unit);
        assertSame(binding, type.resolveBinding());
        assertSame(binding, type.getName().resolveBinding());
        for (int row = 0; row < index.nodeCount(); row++) {
            assertTrue((index.flags(row) & M3ASTIndex.SYNTAX_ONLY) != 0);
        }
    }
}
