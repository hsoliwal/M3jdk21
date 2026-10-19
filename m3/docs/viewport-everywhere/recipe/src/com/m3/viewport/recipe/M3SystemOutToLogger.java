/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import java.util.ArrayList;
import java.util.List;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

/**
 * Brief rule 22 (JUL logging): {@code System.out.println (x)} becomes {@code LOG.log (System.Logger.Level.INFO, x)},
 * {@code System.err.println (x)} the same at {@code WARNING}, and {@code System.out.printf (f, args)} logs
 * {@code String.format (f, args)}. A top-level class that logs gets
 * {@code private static final System.Logger LOG = System.getLogger ("<class>")} (System.Logger is backed by
 * java.util.logging). Classes that already declare {@code LOG} keep it.
 */
public final class M3SystemOutToLogger extends Recipe {
	@Override
	public String getDisplayName () {
		return "M3 System.out to System.Logger";
	}

	@Override
	public String getDescription () {
		return "Replace System.out/err println and printf by System.Logger calls and declare the logger.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor () {
		return new JavaVisitor<ExecutionContext> () {
			private boolean logged;

			@Override
			public J visitCompilationUnit (J.CompilationUnit cu, ExecutionContext ctx) {
				logged = false;
				J.CompilationUnit c = (J.CompilationUnit) super.visitCompilationUnit (cu, ctx);
				if (!logged || c.getClasses ().isEmpty ()) return c;
				J.ClassDeclaration top = c.getClasses ().get (0);
				boolean declared = top.getBody ().getStatements ().stream ().anyMatch (s -> s instanceof J.VariableDeclarations v
						&& v.getVariables ().stream ().anyMatch (n -> n.getSimpleName ().equals ("LOG")));
				if (declared) return c;
				String pkg = c.getPackageDeclaration () == null ? "" : c.getPackageDeclaration ().getPackageName () + ".";
				J.VariableDeclarations field = (J.VariableDeclarations) unit ("private static final System.Logger LOG = System.getLogger (\""
						+ pkg + top.getSimpleName () + "\");").getBody ().getStatements ().get (0);
				List<Statement> body = new ArrayList<> (top.getBody ().getStatements ());
				String indent = body.isEmpty () ? "\n\t" : "\n" + body.get (0).getPrefix ().getIndent ();
				body.add (0, field.withId (Tree.randomId ()).withPrefix (Space.format (indent)));
				List<J.ClassDeclaration> classes = new ArrayList<> (c.getClasses ());
				classes.set (0, top.withBody (top.getBody ().withStatements (body)));
				return c.withClasses (classes);
			}

			@Override
			public J visitMethodInvocation (J.MethodInvocation method, ExecutionContext ctx) {
				J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation (method, ctx);
				if (!(m.getSelect () instanceof J.FieldAccess stream) || !(stream.getTarget () instanceof J.Identifier system)
						|| !"System".equals (system.getSimpleName ())) return m;
				String level = switch (stream.getSimpleName ()) {
					case "out" -> "INFO";
					case "err" -> "WARNING";
					default -> null;
				};
				if (level == null) return m;
				List<String> args = new ArrayList<> ();
				for (Expression a : m.getArguments ()) {
					if (!(a instanceof J.Empty)) args.add (a.printTrimmed (new Cursor (getCursor (), a)));
				}
				String message = switch (m.getSimpleName ()) {
					case "println", "print" -> args.isEmpty () ? "\"\"" : "String.valueOf (" + args.get (0) + ")";
					case "printf", "format" -> "String.format (" + String.join (", ", args) + ")";
					default -> null;
				};
				if (message == null) return m;
				logged = true;
				return expression ("LOG.log (System.Logger.Level." + level + ", " + message + ")").withPrefix (m.getPrefix ());
			}
		};
	}

	private static J.ClassDeclaration unit (String body) {
		J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion ().build ().parse ("class M3L {\n" + body + "\n}").toList ().get (0);
		return cu.getClasses ().get (0);
	}

	static J expression (String text) {
		J.MethodDeclaration m = (J.MethodDeclaration) unit ("void m () { " + text + "; }").getBody ().getStatements ().get (0);
		return m.getBody ().getStatements ().get (0);
	}
}
