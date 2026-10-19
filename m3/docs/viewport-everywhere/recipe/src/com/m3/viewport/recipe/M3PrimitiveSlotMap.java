/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import java.util.HashSet;
import java.util.Set;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

/**
 * Replaces boxed {@code HashMap<Long, Integer>} slot maps by {@code M3LongLongHashMap} (open addressing, no entry
 * objects, no boxing), with the same answers:
 * <ul>
 * <li>the field {@code HashMap<Long, Integer> m = new HashMap<> ()} becomes {@code M3LongLongHashMap m = new
 * M3LongLongHashMap ()};</li>
 * <li>{@code Integer v = m.get (k);} becomes {@code int v = (int) m.getOrDefault (k, -1L);} (slots are never
 * negative, so -1 stands for absent);</li>
 * <li>{@code v == null} and {@code v != null} on such a local become {@code v < 0} and {@code v >= 0}.</li>
 * </ul>
 * {@code put}, {@code clear} keep their spelling ({@code put (long, long)} accepts the int slot).
 */
public final class M3PrimitiveSlotMap extends Recipe {
	@Override
	public String getDisplayName () {
		return "M3 primitive slot map";
	}

	@Override
	public String getDescription () {
		return "Replace boxed HashMap<Long, Integer> slot maps by M3LongLongHashMap with -1 for absent.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor () {
		return new JavaVisitor<ExecutionContext> () {
			private final Set<String> maps = new HashSet<> ();
			private final Set<String> slots = new HashSet<> ();

			@Override
			public J visitClassDeclaration (J.ClassDeclaration classDecl, ExecutionContext ctx) {
				for (Statement s : classDecl.getBody ().getStatements ()) {
					if (s instanceof J.VariableDeclarations v && isBoxedSlotMap (v)) {
						v.getVariables ().forEach (n -> maps.add (n.getSimpleName ()));
					}
				}
				return super.visitClassDeclaration (classDecl, ctx);
			}

			@Override
			public J visitVariableDeclarations (J.VariableDeclarations multiVariable, ExecutionContext ctx) {
				J.VariableDeclarations v = (J.VariableDeclarations) super.visitVariableDeclarations (multiVariable, ctx);
				if (isBoxedSlotMap (v) && v.getVariables ().size () == 1) {
					String mods = v.getModifiers ().stream ().map (m -> m.getType ().name ().toLowerCase ()).reduce ("", (a, b) -> a + b + " ");
					String name = v.getVariables ().get (0).getSimpleName ();
					return member (mods + "M3LongLongHashMap " + name + " = new M3LongLongHashMap ();").withPrefix (v.getPrefix ());
				}
				if (v.getVariables ().size () == 1 && named (v.getTypeExpression (), "Integer")
						&& v.getVariables ().get (0).getInitializer () instanceof J.MethodInvocation call
						&& "get".equals (call.getSimpleName ()) && call.getSelect () instanceof J.Identifier map
						&& maps.contains (map.getSimpleName ()) && call.getArguments ().size () == 1) {
					String name = v.getVariables ().get (0).getSimpleName ();
					slots.add (name);
					String key = call.getArguments ().get (0).printTrimmed (new Cursor (getCursor (), call));
					return local ("int " + name + " = (int) " + map.getSimpleName () + ".getOrDefault (" + key + ", -1L);")
							.withPrefix (v.getPrefix ());
				}
				return v;
			}

			@Override
			public J visitBinary (J.Binary binary, ExecutionContext ctx) {
				J.Binary b = (J.Binary) super.visitBinary (binary, ctx);
				boolean eq = b.getOperator () == J.Binary.Type.Equal, ne = b.getOperator () == J.Binary.Type.NotEqual;
				if ((eq || ne) && b.getLeft () instanceof J.Identifier id && slots.contains (id.getSimpleName ())
						&& b.getRight () instanceof J.Literal lit && lit.getValue () == null) {
					return expression (id.getSimpleName () + (eq ? " < 0" : " >= 0")).withPrefix (b.getPrefix ());
				}
				return b;
			}
		};
	}

	static boolean isBoxedSlotMap (J.VariableDeclarations v) {
		return v.getTypeExpression () instanceof J.ParameterizedType type && named (type.getClazz (), "HashMap")
				&& type.getTypeParameters () != null && type.getTypeParameters ().size () == 2
				&& named (type.getTypeParameters ().get (0), "Long") && named (type.getTypeParameters ().get (1), "Integer");
	}

	static boolean named (Object tree, String name) {
		return tree instanceof J.Identifier id && name.equals (id.getSimpleName ());
	}

	private static J.ClassDeclaration unit (String body) {
		J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion ().build ().parse ("class M3S {\n" + body + "\n}").toList ().get (0);
		return cu.getClasses ().get (0);
	}

	static J.VariableDeclarations member (String declaration) {
		return ((J.VariableDeclarations) unit (declaration).getBody ().getStatements ().get (0)).withId (Tree.randomId ());
	}

	static J.VariableDeclarations local (String declaration) {
		J.MethodDeclaration m = (J.MethodDeclaration) unit ("void m () { " + declaration + " }").getBody ().getStatements ().get (0);
		return ((J.VariableDeclarations) m.getBody ().getStatements ().get (0)).withId (Tree.randomId ());
	}

	static J expression (String text) {
		J.MethodDeclaration m = (J.MethodDeclaration) unit ("boolean m () { return " + text + "; }").getBody ().getStatements ().get (0);
		return ((J.Return) m.getBody ().getStatements ().get (0)).getExpression ().withPrefix (Space.EMPTY);
	}
}
