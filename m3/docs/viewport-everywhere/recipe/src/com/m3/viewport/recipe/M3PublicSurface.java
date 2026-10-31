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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

/**
 * Makes the package-private surface of a class public: the class itself, its nested types, constructors and methods.
 * Fields, private and protected members, interface members, enum constructors and record members are not touched.
 * The copied viewport classes were package-private inside SWT; in M3 they are consumed across modules.
 */
public final class M3PublicSurface extends Recipe {
	@Override
	public String getDisplayName () {
		return "M3 public surface for package-private classes";
	}

	@Override
	public String getDescription () {
		return "Add the public modifier to package-private classes, constructors and methods.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor () {
		return new JavaIsoVisitor<ExecutionContext> () {
			@Override
			public J.ClassDeclaration visitClassDeclaration (J.ClassDeclaration classDecl, ExecutionContext ctx) {
				J.ClassDeclaration c = super.visitClassDeclaration (classDecl, ctx);
				if (!packagePrivate (c.getModifiers ()) || inInterface () || c.getKind () == J.ClassDeclaration.Kind.Type.Annotation) return c;
				if (!c.getModifiers ().isEmpty ()) return c.withModifiers (prepend (c.getModifiers ()));
				J.ClassDeclaration.Kind kind = c.getPadding ().getKind ();
				Space before = c.getLeadingAnnotations ().isEmpty () ? Space.EMPTY : kind.getPrefix ();
				return c.withModifiers (List.of (modifier (before))).getPadding ().withKind (kind.withPrefix (Space.SINGLE_SPACE));
			}

			@Override
			public J.MethodDeclaration visitMethodDeclaration (J.MethodDeclaration method, ExecutionContext ctx) {
				J.MethodDeclaration m = super.visitMethodDeclaration (method, ctx);
				J.ClassDeclaration owner = getCursor ().firstEnclosing (J.ClassDeclaration.class);
				if (owner == null || !packagePrivate (m.getModifiers ()) || owner.getKind () != J.ClassDeclaration.Kind.Type.Class) return m;
				if (!m.getModifiers ().isEmpty ()) return m.withModifiers (prepend (m.getModifiers ()));
				boolean annotated = !m.getLeadingAnnotations ().isEmpty ();
				if (m.getPadding ().getTypeParameters () != null) {
					J.TypeParameters tp = m.getPadding ().getTypeParameters ();
					return m.withModifiers (List.of (modifier (annotated ? tp.getPrefix () : Space.EMPTY)))
							.getPadding ().withTypeParameters (tp.withPrefix (Space.SINGLE_SPACE));
				}
				if (m.getReturnTypeExpression () != null) {
					Space before = annotated ? m.getReturnTypeExpression ().getPrefix () : Space.EMPTY;
					return m.withModifiers (List.of (modifier (before)))
							.withReturnTypeExpression (m.getReturnTypeExpression ().withPrefix (Space.SINGLE_SPACE));
				}
				Space before = annotated ? m.getName ().getPrefix () : Space.EMPTY;
				return m.withModifiers (List.of (modifier (before))).withName (m.getName ().withPrefix (Space.SINGLE_SPACE));
			}

			private boolean inInterface () {
				J.ClassDeclaration owner = getCursor ().getParentOrThrow ().firstEnclosing (J.ClassDeclaration.class);
				return owner != null && owner.getKind () != J.ClassDeclaration.Kind.Type.Class;
			}
		};
	}

	static boolean packagePrivate (List<J.Modifier> modifiers) {
		return modifiers.stream ().noneMatch (m -> m.getType () == J.Modifier.Type.Public
				|| m.getType () == J.Modifier.Type.Private || m.getType () == J.Modifier.Type.Protected);
	}

	static List<J.Modifier> prepend (List<J.Modifier> modifiers) {
		List<J.Modifier> out = new ArrayList<> (modifiers.size () + 1);
		out.add (modifier (modifiers.get (0).getPrefix ()));
		out.add (modifiers.get (0).withPrefix (Space.SINGLE_SPACE));
		out.addAll (modifiers.subList (1, modifiers.size ()));
		return out;
	}

	static J.Modifier modifier (Space prefix) {
		return new J.Modifier (Tree.randomId (), prefix, Markers.EMPTY, null, J.Modifier.Type.Public, List.of ());
	}
}
