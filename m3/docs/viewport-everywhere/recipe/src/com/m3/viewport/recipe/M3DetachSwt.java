/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import java.util.List;
import java.util.Map;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

/**
 * Detaches copied viewport sources from SWT without changing behaviour.
 * <ul>
 * <li>{@code SWT.error (SWT.ERROR_X)} becomes {@code throw new IllegalArgumentException ("...")} with the exact message
 * that {@code SWT.error} builds for the illegal-argument codes ({@code SWT.findErrorText});</li>
 * <li>imports of {@code org.eclipse.swt} packages are removed (the copies hold no SWT reference afterwards).</li>
 * </ul>
 */
public final class M3DetachSwt extends Recipe {
	static final Map<String, String> MESSAGES = Map.of (
			"ERROR_NULL_ARGUMENT", "Argument cannot be null",
			"ERROR_INVALID_ARGUMENT", "Argument not valid",
			"ERROR_INVALID_RANGE", "Index out of bounds");

	@Override
	public String getDisplayName () {
		return "M3 detach viewport copies from SWT";
	}

	@Override
	public String getDescription () {
		return "Replace SWT.error illegal-argument calls by the identical throw and drop org.eclipse.swt imports.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor () {
		return new JavaVisitor<ExecutionContext> () {
			@Override
			public J visitCompilationUnit (J.CompilationUnit cu, ExecutionContext ctx) {
				J.CompilationUnit c = (J.CompilationUnit) super.visitCompilationUnit (cu, ctx);
				List<J.Import> kept = c.getImports ().stream ().filter (i -> !i.getTypeName ().startsWith ("org.eclipse.swt")
						&& !i.getPackageName ().startsWith ("org.eclipse.swt")).toList ();
				return kept.size () == c.getImports ().size () ? c : c.withImports (kept);
			}

			@Override
			public J visitMethodInvocation (J.MethodInvocation method, ExecutionContext ctx) {
				J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation (method, ctx);
				String message = message (m);
				if (message == null) return m;
				return throwOf (message).withPrefix (m.getPrefix ());
			}
		};
	}

	/** Parses {@code throw new IllegalArgumentException ("message")} in SWT spacing; each call yields fresh ids. */
	static J.Throw throwOf (String message) {
		String unit = "class M3T { void m () { throw new IllegalArgumentException (\"" + message + "\"); } }";
		J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion ().build ().parse (unit).toList ().get (0);
		J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses ().get (0).getBody ().getStatements ().get (0);
		return (J.Throw) m.getBody ().getStatements ().get (0);
	}

	static String message (J.MethodInvocation m) {
		if (!"error".equals (m.getSimpleName ()) || !(m.getSelect () instanceof J.Identifier select)
				|| !"SWT".equals (select.getSimpleName ()) || m.getArguments ().size () != 1) return null;
		Expression arg = m.getArguments ().get (0);
		return arg instanceof J.FieldAccess field && field.getTarget () instanceof J.Identifier target
				&& "SWT".equals (target.getSimpleName ()) ? MESSAGES.get (field.getSimpleName ()) : null;
	}
}
