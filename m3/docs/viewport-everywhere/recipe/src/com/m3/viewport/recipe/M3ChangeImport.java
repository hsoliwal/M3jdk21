/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;

/**
 * Rewrites one non-static import whose qualified name equals {@code from} to {@code to}. Used for nested-type
 * imports (for example {@code ViewportRuntime.Selection}) that a file imports without importing the outer type.
 */
public final class M3ChangeImport extends Recipe {
	@Option(displayName = "From", description = "Qualified import name to replace.", example = "a.B.C")
	private final String from;

	@Option(displayName = "To", description = "Replacement qualified import name.", example = "x.Y.C")
	private final String to;

	@JsonCreator
	public M3ChangeImport (@JsonProperty("from") String from, @JsonProperty("to") String to) {
		this.from = from;
		this.to = to;
	}

	public String getFrom () {
		return from;
	}

	public String getTo () {
		return to;
	}

	@Override
	public String getDisplayName () {
		return "M3 exact import change";
	}

	@Override
	public String getDescription () {
		return "Replace an import whose qualified name equals a value.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor () {
		return new JavaIsoVisitor<ExecutionContext> () {
			@Override
			public J.Import visitImport (J.Import anImport, ExecutionContext ctx) {
				if (anImport.isStatic () || !from.equals (anImport.getQualid ().printTrimmed (new Cursor (getCursor (), anImport)))) return anImport;
				J.FieldAccess qualid = TypeTree.build (to);
				return anImport.withQualid (qualid.withPrefix (anImport.getQualid ().getPrefix ()));
			}
		};
	}
}
