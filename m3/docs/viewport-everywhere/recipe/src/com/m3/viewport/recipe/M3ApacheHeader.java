/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

/**
 * Replaces the leading licence comment of a source copied from the SWT viewport classes with the m3/** Apache-2.0
 * header, keeps every donor attribution line of the old comment, and names the source file. A file whose header
 * already starts with the M3 copyright line is left unchanged, so a second pass changes nothing.
 */
public final class M3ApacheHeader extends Recipe {
	static final String COPYRIGHT = " * Copyright 2026 Hitesh Soliwal and contributors";
	static final String ORIGIN = "hsoliwal/eclipse.platform.swt 553eefd41d9961d303c41e0e4e181712e64a1e45";
	private static final Pattern DROPPED = Pattern.compile (
			"^\\s*\\*?\\s*Copyright|SPDX-License-Identifier|Eclipse Public License|accompanies this distribution|"
			+ "is available at|epl-2\\.0|program and the accompanying|Dual-licensed|apache\\.org/licenses|"
			+ "^\\s*\\*?\\s*\\**/?\\s*$");

	@Override
	public String getDisplayName () {
		return "M3 Apache-2.0 header for viewport sources";
	}

	@Override
	public String getDescription () {
		return "Replace the leading licence comment with the m3 Apache-2.0 header, keeping donor attribution lines.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor () {
		return new JavaIsoVisitor<ExecutionContext> () {
			@Override
			public J.CompilationUnit visitCompilationUnit (J.CompilationUnit cu, ExecutionContext ctx) {
				J.Package pkg = cu.getPackageDeclaration ();
				if (pkg == null) return cu;
				List<Comment> comments = new ArrayList<> (cu.getPrefix ().getComments ());
				comments.addAll (pkg.getPrefix ().getComments ());
				if (cu.getPrefix ().getComments ().isEmpty () && !comments.isEmpty () && comments.get (0) instanceof TextComment first
						&& first.getText ().startsWith ("\n" + COPYRIGHT)) return cu;
				String source = cu.getClasses ().isEmpty () ? "?" : cu.getClasses ().get (0).getSimpleName ();
				String text = header (source, kept (comments));
				TextComment comment = new TextComment (true, text, "\n", Markers.EMPTY);
				return cu.withPrefix (Space.EMPTY).withPackageDeclaration (pkg.withPrefix (Space.build ("", List.of (comment))));
			}
		};
	}

	static List<String> kept (List<Comment> comments) {
		List<String> lines = new ArrayList<> ();
		if (comments.isEmpty () || !(comments.get (0) instanceof TextComment first)) return lines;
		for (String line : first.getText ().split ("\n")) {
			if (!DROPPED.matcher (line).find ()) lines.add (line.startsWith (" *") ? line : " * " + line.strip ());
		}
		return lines;
	}

	static String header (String source, List<String> kept) {
		StringBuilder b = new StringBuilder ("\n").append (COPYRIGHT).append ('\n');
		b.append (" * SPDX-License-Identifier: Apache-2.0\n *\n");
		b.append (" * Derived by recipe (m3/docs/viewport-everywhere/recipe) from ").append (ORIGIN).append (",\n");
		b.append (" * ").append (source).append (".java (EPL-2.0 at source; authority: ");
		b.append ("m3/docs/viewport-everywhere/RELICENSE_RECORD.md).\n");
		for (String line : kept) b.append (line).append ('\n');
		return b.append (' ').toString ();
	}
}
