/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

/**
 * Makes the package-private, non-static array fields of a class public: the lanes of a lane-holding class are its
 * data contract (DOOP), and code in other packages fills them (for example radix keys). Other fields are not touched.
 */
public final class M3PublicLanes extends Recipe {
	@Override
	public String getDisplayName () {
		return "M3 public lanes";
	}

	@Override
	public String getDescription () {
		return "Add the public modifier to package-private non-static array fields.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor () {
		return new JavaIsoVisitor<ExecutionContext> () {
			@Override
			public J.VariableDeclarations visitVariableDeclarations (J.VariableDeclarations multiVariable, ExecutionContext ctx) {
				J.VariableDeclarations v = super.visitVariableDeclarations (multiVariable, ctx);
				boolean field = getCursor ().firstEnclosing (J.MethodDeclaration.class) == null
						&& getCursor ().firstEnclosing (J.Lambda.class) == null
						&& getCursor ().firstEnclosing (J.ClassDeclaration.class) != null;
				if (!field || !(v.getTypeExpression () instanceof J.ArrayType) || !M3PublicSurface.packagePrivate (v.getModifiers ())
						|| v.hasModifier (J.Modifier.Type.Static)) return v;
				if (!v.getModifiers ().isEmpty ()) return v.withModifiers (M3PublicSurface.prepend (v.getModifiers ()));
				J.Modifier pub = new J.Modifier (Tree.randomId (), Space.EMPTY, Markers.EMPTY, null, J.Modifier.Type.Public, List.of ());
				return v.withModifiers (List.of (pub)).withTypeExpression (v.getTypeExpression ().withPrefix (Space.SINGLE_SPACE));
			}
		};
	}
}
