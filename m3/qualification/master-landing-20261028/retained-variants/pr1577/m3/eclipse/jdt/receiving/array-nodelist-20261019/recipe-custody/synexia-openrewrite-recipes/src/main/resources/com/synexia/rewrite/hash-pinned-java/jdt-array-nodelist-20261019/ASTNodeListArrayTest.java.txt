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
 *     Hitesh Soliwal and contributors - child-list contract regression tests
 *******************************************************************************/
package org.eclipse.jdt.core.tests.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

/** Tests the public contracts affected by the private NodeList storage change. */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ASTNodeListArrayTest extends TestCase {

	public static Test suite() {
		return new TestSuite(ASTNodeListArrayTest.class);
	}

	private static AST ast() {
		return AST.newAST(AST.getJLSLatest(), false);
	}

	private static ExpressionStatement statement(AST ast, String identifier) {
		return ast.newExpressionStatement(ast.newSimpleName(identifier));
	}

	private static Map<String, String> options() {
		Map<String, String> options = new HashMap<>();
		JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);
		return options;
	}

	private static CompilationUnit parse(String source, boolean bindings) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(source.toCharArray());
		parser.setCompilerOptions(options());
		parser.setUnitName("X.java");
		parser.setResolveBindings(bindings);
		if (bindings) {
			parser.setEnvironment(new String[0], new String[0], new String[0], true);
		}
		return (CompilationUnit) parser.createAST(null);
	}

	private static void expect(Class<? extends Throwable> type, Runnable operation) {
		try {
			operation.run();
			fail("Expected " + type.getName());
		} catch (Throwable failure) {
			if (!type.isInstance(failure)) {
				throw new AssertionError("Expected " + type.getName() + ", got " + failure, failure);
			}
		}
	}

	private static void assertChildren(Block block, List<ASTNode> expected) {
		List children = block.statements();
		assertEquals(expected.size(), children.size());
		assertEquals(expected, children);
		assertEquals(expected.hashCode(), children.hashCode());
		assertTrue(Arrays.equals(expected.toArray(), children.toArray()));
		for (int i = 0; i < expected.size(); i++) {
			ASTNode child = (ASTNode) children.get(i);
			assertSame(expected.get(i), child);
			assertSame(block, child.getParent());
			assertSame(block.getAST(), child.getAST());
			assertSame(Block.STATEMENTS_PROPERTY, child.getLocationInParent());
		}
	}

	public void testLiveListAndBounds() {
		AST ast = ast();
		Block block = ast.newBlock();
		List children = block.statements();
		assertSame(children, block.statements());
		assertTrue(children.isEmpty());
		expect(IndexOutOfBoundsException.class, () -> children.get(0));
		expect(IndexOutOfBoundsException.class, () -> children.get(-1));
		expect(IndexOutOfBoundsException.class, () -> children.add(1, ast.newEmptyStatement()));
		expect(IndexOutOfBoundsException.class, () -> children.remove(0));
		ASTNode child = ast.newEmptyStatement();
		children.add(child);
		assertSame(child, children.get(0));
		expect(IndexOutOfBoundsException.class, () -> children.get(1));
		expect(IndexOutOfBoundsException.class, () -> children.set(1, ast.newEmptyStatement()));
		assertChildren(block, List.of(child));
	}

	public void testGrowthShiftsAndIdentityAgainstArrayList() {
		AST ast = ast();
		Block block = ast.newBlock();
		List children = block.statements();
		List<ASTNode> expected = new ArrayList<>();
		Random random = new Random(0x4d33415354L);
		for (int step = 0; step < 4096; step++) {
			int operation = expected.isEmpty() ? 0 : random.nextInt(4);
			if (operation <= 1) {
				int index = random.nextInt(expected.size() + 1);
				ASTNode child = statement(ast, "v" + step);
				expected.add(index, child);
				children.add(index, child);
			} else if (operation == 2) {
				int index = random.nextInt(expected.size());
				ASTNode child = statement(ast, "v" + step);
				ASTNode old = expected.set(index, child);
				assertSame(old, children.set(index, child));
				assertNull(old.getParent());
				assertNull(old.getLocationInParent());
			} else {
				int index = random.nextInt(expected.size());
				ASTNode old = expected.remove(index);
				assertSame(old, children.remove(index));
				assertNull(old.getParent());
				assertNull(old.getLocationInParent());
			}
			assertChildren(block, expected);
		}
		children.clear();
		assertTrue(children.isEmpty());
		for (ASTNode child : expected) {
			assertNull(child.getParent());
		}
		ASTNode child = ast.newEmptyStatement();
		children.add(child);
		assertChildren(block, List.of(child));
	}

	public void testSameChildReplacementKeepsModificationCount() {
		AST ast = ast();
		Block block = ast.newBlock();
		ASTNode child = ast.newEmptyStatement();
		block.statements().add(child);
		child.setFlags(child.getFlags() | ASTNode.PROTECT);
		long count = ast.modificationCount();
		assertSame(child, block.statements().set(0, child));
		assertEquals(count, ast.modificationCount());
		assertSame(block, child.getParent());
	}

	public void testPublicSubtreeBytesKeepsHistoricalCostEstimate() {
		AST ast = ast();
		Block block = ast.newBlock();
		int empty = block.subtreeBytes();
		assertEquals(112, empty);
		for (int count = 1; count <= 20; count++) {
			ASTNode child = ast.newEmptyStatement();
			block.statements().add(child);
			assertEquals(empty + count * (4 + child.subtreeBytes()), block.subtreeBytes());
		}
		block.statements().subList(1, 20).clear();
		ASTNode child = (ASTNode) block.statements().get(0);
		assertEquals(empty + 4 + child.subtreeBytes(), block.subtreeBytes());
		block.statements().clear();
		assertEquals(empty, block.subtreeBytes());
	}

	public void testOwnershipTypesCyclesAndFailureOrder() {
		AST ast = ast();
		Block block = ast.newBlock();
		List children = block.statements();
		ASTNode child = ast.newEmptyStatement();
		children.add(child);
		expect(IllegalArgumentException.class, () -> children.add(child));
		expect(IllegalArgumentException.class, () -> children.add(ast().newEmptyStatement()));
		expect(ClassCastException.class, () -> children.add(ast.newSimpleName("wrongType")));
		expect(IllegalArgumentException.class, () -> children.add(block));
		expect(IllegalArgumentException.class, () -> children.add(-1, null));
		expect(ClassCastException.class, () -> children.add(-1, "wrongRuntimeType"));
		expect(IllegalArgumentException.class, () -> children.add(-1, child));
		expect(IllegalArgumentException.class, () -> children.set(-1, null));
		expect(ClassCastException.class, () -> children.set(-1, "wrongRuntimeType"));
		assertChildren(block, List.of(child));
	}

	public void testProtectionPreservesChildrenOnFailure() {
		AST ast = ast();
		Block block = ast.newBlock();
		ASTNode child = ast.newEmptyStatement();
		block.statements().add(child);
		child.setFlags(child.getFlags() | ASTNode.PROTECT);
		expect(IllegalArgumentException.class, () -> block.statements().remove(0));
		expect(IllegalArgumentException.class, () -> block.statements().set(0, ast.newEmptyStatement()));
		child.setFlags(child.getFlags() & ~ASTNode.PROTECT);
		block.setFlags(block.getFlags() | ASTNode.PROTECT);
		expect(IllegalArgumentException.class, () -> block.statements().add(ast.newEmptyStatement()));
		expect(IllegalArgumentException.class, () -> block.statements().remove(-1));
		expect(IllegalArgumentException.class, () -> block.statements().set(0, child));
		assertChildren(block, List.of(child));
	}

	public void testBulkFailureKeepsSuccessfulPrefix() {
		AST ast = ast();
		Block block = ast.newBlock();
		ASTNode existing = ast.newEmptyStatement();
		ASTNode added = ast.newEmptyStatement();
		ASTNode unvisited = ast.newEmptyStatement();
		block.statements().add(existing);
		expect(IllegalArgumentException.class,
				() -> block.statements().addAll(List.of(added, existing, unvisited)));
		assertChildren(block, List.of(existing, added));
		assertNull(unvisited.getParent());
	}

	public void testIteratorSubListAndListIteratorEdits() {
		AST ast = ast();
		Block block = ast.newBlock();
		ASTNode a = statement(ast, "a");
		ASTNode b = statement(ast, "b");
		ASTNode c = statement(ast, "c");
		ASTNode d = statement(ast, "d");
		block.statements().addAll(List.of(a, b, c));
		ListIterator iterator = block.statements().listIterator();
		assertSame(a, iterator.next());
		iterator.set(d);
		assertNull(a.getParent());
		assertSame(b, iterator.next());
		iterator.remove();
		assertNull(b.getParent());
		iterator.add(a);
		assertChildren(block, List.of(d, a, c));
		block.statements().subList(1, 3).clear();
		assertChildren(block, List.of(d));
		assertNull(a.getParent());
		assertNull(c.getParent());
		Iterator remaining = block.statements().iterator();
		assertSame(d, remaining.next());
		remaining.remove();
		assertTrue(block.statements().isEmpty());
		assertNull(d.getParent());
	}

	public void testVisitorInsertionBeforeAndAfterCursor() {
		AST ast = ast();
		Block block = ast.newBlock();
		ExpressionStatement a = statement(ast, "a");
		ExpressionStatement b = statement(ast, "b");
		ExpressionStatement c = statement(ast, "c");
		ExpressionStatement before = statement(ast, "before");
		ExpressionStatement after = statement(ast, "after");
		block.statements().addAll(List.of(a, b, c));
		List<ASTNode> visited = new ArrayList<>();
		block.accept(new ASTVisitor() {
			@Override
			public boolean visit(ExpressionStatement node) {
				visited.add(node);
				if (node == b) {
					block.statements().add(0, before);
					block.statements().add(after);
					block.statements().remove(node);
				}
				return true;
			}
		});
		assertEquals(List.of(a, b, c, after), visited);
		assertChildren(block, List.of(before, a, c, after));
		assertNull(b.getParent());
	}

	public void testNestedVisitorsKeepIndependentCursors() {
		AST ast = ast();
		Block block = ast.newBlock();
		ExpressionStatement a = statement(ast, "a");
		ExpressionStatement b = statement(ast, "b");
		ExpressionStatement c = statement(ast, "c");
		ExpressionStatement d = statement(ast, "d");
		block.statements().addAll(List.of(a, b, c));
		List<ASTNode> outer = new ArrayList<>();
		List<ASTNode> inner = new ArrayList<>();
		block.accept(new ASTVisitor() {
			@Override
			public boolean visit(ExpressionStatement node) {
				outer.add(node);
				if (node == b) {
					block.accept(new ASTVisitor() {
						@Override
						public boolean visit(ExpressionStatement nested) {
							inner.add(nested);
							if (nested == a) {
								block.statements().remove(a);
							} else if (nested == b) {
								block.statements().add(d);
							}
							return true;
						}
					});
				}
				return true;
			}
		});
		assertEquals(List.of(a, b, c, d), inner);
		assertEquals(List.of(a, b, c, d), outer);
		assertChildren(block, List.of(b, c, d));
	}

	public void testVisitorFailureReleasesCursor() {
		AST ast = ast();
		Block block = ast.newBlock();
		ExpressionStatement a = statement(ast, "a");
		ExpressionStatement b = statement(ast, "b");
		block.statements().addAll(List.of(a, b));
		expect(IllegalStateException.class, () -> block.accept(new ASTVisitor() {
			@Override
			public boolean visit(ExpressionStatement node) {
				throw new IllegalStateException("stop traversal");
			}
		}));
		block.statements().remove(a);
		block.statements().add(a);
		List<ASTNode> visited = new ArrayList<>();
		block.accept(new ASTVisitor() {
			@Override
			public boolean visit(ExpressionStatement node) {
				visited.add(node);
				return true;
			}
		});
		assertEquals(List.of(b, a), visited);
	}

	public void testRewriteUsesOriginalListAndPreservesComments() throws Exception {
		String source = "class X { void m() {\n    // retained comment \uD83D\uDE00\n    first();\n    second();\n  } }\n";
		CompilationUnit unit = parse(source, false);
		MethodDeclaration method = ((TypeDeclaration) unit.types().get(0)).getMethods()[0];
		Block body = method.getBody();
		ASTNode first = (ASTNode) body.statements().get(0);
		ASTNode second = (ASTNode) body.statements().get(1);
		ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
		ListRewrite statements = rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
		statements.remove(second, null);
		statements.insertAfter(unit.getAST().newEmptyStatement(), first, null);
		assertChildren(body, List.of(first, second));
		Document document = new Document(source);
		TextEdit edit = rewrite.rewriteAST(document, options());
		UndoEdit undo = edit.apply(document);
		assertTrue(document.get().contains("// retained comment \uD83D\uDE00"));
		assertTrue(document.get().contains("first();"));
		assertFalse(document.get().contains("second();"));
		CompilationUnit rewritten = parse(document.get(), false);
		assertEquals(0, rewritten.getProblems().length);
		undo.apply(document);
		assertEquals(source, document.get());
		assertChildren(body, List.of(first, second));
	}

	public void testRecordedMutationsGenerateEquivalentText() throws Exception {
		String source = "class X { void m() { first(); second(); } }";
		CompilationUnit unit = parse(source, false);
		unit.recordModifications();
		MethodDeclaration method = ((TypeDeclaration) unit.types().get(0)).getMethods()[0];
		Block body = method.getBody();
		ASTNode removed = (ASTNode) body.statements().remove(0);
		ASTNode replacement = unit.getAST().newEmptyStatement();
		body.statements().set(0, replacement);
		body.statements().add(removed);
		Document document = new Document(source);
		unit.rewrite(document, options()).apply(document);
		CompilationUnit roundTrip = parse(document.get(), false);
		assertTrue(unit.subtreeMatch(new ASTMatcher(true), roundTrip));
		assertTrue(document.get().contains("first();"));
		assertFalse(document.get().contains("second();"));
	}

	public void testResolvedBindingsRemainFromOriginalParse() {
		CompilationUnit unit = parse("class X { int m(int a) { return a; } }", true);
		List<SimpleName> references = new ArrayList<>();
		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(SimpleName node) {
				if (node.getIdentifier().equals("a")) {
					references.add(node);
				}
				return true;
			}
		});
		assertEquals(2, references.size());
		IBinding before = references.get(1).resolveBinding();
		assertNotNull(before);
		MethodDeclaration method = ((TypeDeclaration) unit.types().get(0)).getMethods()[0];
		method.parameters().clear();
		assertSame(before, references.get(1).resolveBinding());
		assertNull(references.get(0).getParent().getParent());
	}

	public void testFragmentListsAndDeepCopyHaveIndependentParents() {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_STATEMENTS);
		parser.setCompilerOptions(options());
		parser.setStatementsRecovery(true);
		parser.setSource("first(); second(); third();".toCharArray());
		Block original = (Block) parser.createAST(null);
		assertEquals(3, original.statements().size());
		Block copied = (Block) ASTNode.copySubtree(ast(), original);
		assertTrue(original.subtreeMatch(new ASTMatcher(), copied));
		for (int i = 0; i < 3; i++) {
			ASTNode a = (ASTNode) original.statements().get(i);
			ASTNode b = (ASTNode) copied.statements().get(i);
			assertNotSame(a, b);
			assertSame(original, a.getParent());
			assertSame(copied, b.getParent());
			assertEquals(a.getStartPosition(), b.getStartPosition());
			assertEquals(a.getLength(), b.getLength());
		}
		copied.statements().clear();
		assertEquals(3, original.statements().size());
		assertTrue(copied.statements().isEmpty());
	}
}
