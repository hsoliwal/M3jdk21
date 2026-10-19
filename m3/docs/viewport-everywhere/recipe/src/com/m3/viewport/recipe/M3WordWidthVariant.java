/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import java.util.ArrayList;
import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TextComment;

/**
 * Template-first generation (TmOP): turns a {@code long}-word bit-mask class into its {@code int}-word variant
 * (for GWT/RAP builds, which have no 64-bit words). The mapping is the word-width arithmetic of such code and nothing
 * else, so it is applied only to files named in the recipe list:
 * <ul>
 * <li>primitive {@code long} becomes {@code int} (lanes, parameters, returns, locals);</li>
 * <li>{@code Long.bitCount} and the other {@code Long} intrinsics become their {@code Integer} forms;</li>
 * <li>literals: {@code 64} to {@code 32}, {@code 63} to {@code 31}, {@code 6} to {@code 5} (shift amounts), and
 * long literals lose their {@code L} suffix.</li>
 * </ul>
 * The class itself is renamed by a {@code ChangeType} row before this recipe runs.
 */
public final class M3WordWidthVariant extends Recipe {
	static final String VARIANT_LINE = " int-word variant generated from the long-word template by M3WordWidthVariant (TmOP; GWT/RAP builds).";

	@Override
	public String getDisplayName () {
		return "M3 word-width variant (long words to int words)";
	}

	@Override
	public String getDescription () {
		return "Generate the int-word variant of a long-word bit-mask class.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor () {
		return new JavaIsoVisitor<ExecutionContext> () {
			@Override
			public J.CompilationUnit visitCompilationUnit (J.CompilationUnit cu, ExecutionContext ctx) {
				J.CompilationUnit c = super.visitCompilationUnit (cu, ctx);
				J.Package pkg = c.getPackageDeclaration ();
				if (pkg == null || pkg.getPrefix ().getComments ().isEmpty ()
						|| !(pkg.getPrefix ().getComments ().get (0) instanceof TextComment first)
						|| first.getText ().contains (VARIANT_LINE)) return c;
				String text = first.getText ();
				int end = text.lastIndexOf ('\n');
				String marked = text.substring (0, end) + "\n *" + VARIANT_LINE + text.substring (end);
				List<Comment> comments = new ArrayList<> (pkg.getPrefix ().getComments ());
				comments.set (0, first.withText (marked));
				return c.withPackageDeclaration (pkg.withPrefix (pkg.getPrefix ().withComments (comments)));
			}

			@Override
			public J.Primitive visitPrimitive (J.Primitive primitive, ExecutionContext ctx) {
				J.Primitive p = super.visitPrimitive (primitive, ctx);
				return p.getType () == JavaType.Primitive.Long ? p.withType (JavaType.Primitive.Int) : p;
			}

			@Override
			public J.Identifier visitIdentifier (J.Identifier identifier, ExecutionContext ctx) {
				J.Identifier id = super.visitIdentifier (identifier, ctx);
				if (!"Long".equals (id.getSimpleName ())) return id;
				J parent = getCursor ().getParentOrThrow ().getValue ();
				boolean target = parent instanceof J.FieldAccess f && f.getTarget () == identifier;
				return target ? id.withSimpleName ("Integer") : id;
			}

			@Override
			public J.Literal visitLiteral (J.Literal literal, ExecutionContext ctx) {
				J.Literal l = super.visitLiteral (literal, ctx);
				if (l.getType () == JavaType.Primitive.Long && l.getValue () instanceof Long v) {
					String source = l.getValueSource () == null ? v.toString () : l.getValueSource ();
					String trimmed = source.endsWith ("L") || source.endsWith ("l") ? source.substring (0, source.length () - 1) : source;
					return l.withType (JavaType.Primitive.Int).withValue ((int) (long) v).withValueSource (trimmed);
				}
				if (l.getType () == JavaType.Primitive.Int && l.getValue () instanceof Integer v) {
					int mapped = switch (v) {
						case 64 -> 32;
						case 63 -> 31;
						case 6 -> 5;
						default -> v;
					};
					if (mapped != v) return l.withValue (mapped).withValueSource (Integer.toString (mapped));
				}
				return l;
			}
		};
	}
}
