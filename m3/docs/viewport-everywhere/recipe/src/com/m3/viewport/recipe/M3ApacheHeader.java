/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
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
			+ "terms of the Apache License|which are available at|^\\s*\\*?\\s*Contributors:\\s*$|"
			+ "^\\s*\\*?\\s*\\**/?\\s*$");

	/** Source repository and commit named in the header; the SWT extraction commit when not set. */
	@Option(displayName = "Origin", description = "Source repository and commit.", required = false,
			example = "hsoliwal/nebula 79fdab533b25bd228e96a70feed55c905ab9aefb")
	private final String origin;

	/** Header style: {@code m3} (Apache-2.0, the m3/** modules) or {@code swt} (the Synexia dual header of the SWT fork). */
	@Option(displayName = "Style", description = "m3 or swt.", required = false, example = "swt")
	private final String style;

	public M3ApacheHeader () {
		this (null, null);
	}

	@JsonCreator
	public M3ApacheHeader (@JsonProperty("origin") String origin, @JsonProperty("style") String style) {
		this.origin = origin == null ? ORIGIN : origin;
		this.style = style == null ? "m3" : style;
	}

	public String getOrigin () {
		return origin;
	}

	public String getStyle () {
		return style;
	}

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
				boolean swt = "swt".equals (style);
				if (!comments.isEmpty () && comments.get (0) instanceof TextComment first
						&& first.getText ().startsWith (swt ? SWT_COPYRIGHT : "\n" + COPYRIGHT)) return cu;
				String source = cu.getClasses ().isEmpty () ? "?" : cu.getClasses ().get (0).getSimpleName ();
				String text = swt ? swtHeader (origin, source, kept (comments)) : header (origin, source, kept (comments));
				TextComment comment = new TextComment (true, text, "\n", Markers.EMPTY);
				return cu.withPrefix (Space.EMPTY).withPackageDeclaration (pkg.withPrefix (Space.build ("", List.of (comment))));
			}
		};
	}

	static final String SWT_COPYRIGHT = "******************************************************************************\n"
			+ " * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.";

	/** The Synexia header of the SWT fork (C:/m3-superset/tools/SYNEXIA_HEADER.java.txt) plus the derivation line. */
	static String swtHeader (String origin, String source, List<String> kept) {
		StringBuilder b = new StringBuilder (SWT_COPYRIGHT).append ("\n *\n");
		b.append (" * This program and the accompanying materials are made available under the\n");
		b.append (" * terms of the Apache License, Version 2.0, or (at your option) the Eclipse\n");
		b.append (" * Public License 2.0, which are available at\n");
		b.append (" * https://www.apache.org/licenses/LICENSE-2.0 and\n");
		b.append (" * https://www.eclipse.org/legal/epl-2.0/\n *\n");
		b.append (" * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\n *\n");
		b.append (" * Derived by recipe (M3jdk21 m3/docs/viewport-everywhere/recipe, SwtHome) from ").append (origin).append (",\n");
		b.append (" * com.m3.collections.").append (source).append (" (same author; the SWT copy of the viewport collections).\n");
		for (String line : kept) if (!line.contains ("initial API and implementation")) b.append (line).append ('\n');
		b.append (" *\n * Contributors:\n *     Hitesh Soliwal (Synexia) - initial API and implementation\n");
		return b.append (" ******************************************************************************").toString ();
	}

	static List<String> kept (List<Comment> comments) {
		List<String> lines = new ArrayList<> ();
		if (comments.isEmpty () || !(comments.get (0) instanceof TextComment first)) return lines;
		for (String line : first.getText ().split ("\n")) {
			if (!DROPPED.matcher (line).find ()) lines.add (line.startsWith (" *") ? line : " * " + line.strip ());
		}
		return lines;
	}

	static String header (String origin, String source, List<String> kept) {
		StringBuilder b = new StringBuilder ("\n").append (COPYRIGHT).append ('\n');
		b.append (" * SPDX-License-Identifier: Apache-2.0\n *\n");
		b.append (" * Derived by recipe (m3/docs/viewport-everywhere/recipe) from ").append (origin).append (",\n");
		b.append (" * ").append (source).append (".java; Apache-2.0 elected from its Apache-2.0 OR EPL-2.0 publication\n");
		b.append (" * (m3/docs/viewport-everywhere/LICENSE_ELECTION.tsv).\n");
		for (String line : kept) b.append (line).append ('\n');
		return b.append (' ').toString ();
	}
}
