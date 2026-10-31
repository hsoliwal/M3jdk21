/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import java.util.ArrayList;
import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

/**
 * Gives a public class without constructors the explicit, equivalent public no-argument constructor
 * ({@code -Xlint:missing-explicit-ctor} for exported packages), placed before the first method. Behaviour is
 * unchanged: the default constructor had the same access and an empty body.
 */
public final class M3ExplicitConstructor extends Recipe {
	@Override
	public String getDisplayName () {
		return "M3 explicit public constructor";
	}

	@Override
	public String getDescription () {
		return "Add the equivalent explicit public no-argument constructor to public classes that declare none.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor () {
		return new JavaIsoVisitor<ExecutionContext> () {
			@Override
			public J.ClassDeclaration visitClassDeclaration (J.ClassDeclaration classDecl, ExecutionContext ctx) {
				J.ClassDeclaration c = super.visitClassDeclaration (classDecl, ctx);
				if (c.getKind () != J.ClassDeclaration.Kind.Type.Class || !c.hasModifier (J.Modifier.Type.Public)) return c;
				List<Statement> body = c.getBody ().getStatements ();
				if (body.stream ().anyMatch (s -> s instanceof J.MethodDeclaration m && m.isConstructor ())) return c;
				int at = 0;
				while (at < body.size () && !(body.get (at) instanceof J.MethodDeclaration)) at++;
				List<Statement> out = new ArrayList<> (body);
				out.add (at, constructor (c.getSimpleName (), at == 0 ? "\n\t" : "\n\n\t"));
				return c.withBody (c.getBody ().withStatements (out));
			}
		};
	}

	static J.MethodDeclaration constructor (String name, String indent) {
		String unit = "class " + name + " {\n\tpublic " + name + " () {\n\t}\n}";
		J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion ().build ().parse (unit).toList ().get (0);
		J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses ().get (0).getBody ().getStatements ().get (0);
		return m.withId (Tree.randomId ()).withPrefix (Space.format (indent));
	}
}
