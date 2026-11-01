/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Replaces a string literal whose whole value equals {@code from} with {@code to}. Used only on ported tests, where
 * reflective class names (for example {@code "org.eclipse.swt.widgets.VirtualRowExtents"}) follow the type rename.
 */
public final class M3ChangeStringLiteral extends Recipe {
	@Option(displayName = "From", description = "Exact literal value to replace.", example = "VirtualTreeTopology")
	private final String from;

	@Option(displayName = "To", description = "Replacement literal value.", example = "M3TreeTopology")
	private final String to;

	@JsonCreator
	public M3ChangeStringLiteral (@JsonProperty("from") String from, @JsonProperty("to") String to) {
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
		return "M3 exact string literal change";
	}

	@Override
	public String getDescription () {
		return "Replace string literals equal to a value; only for reflective names in ported tests.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor () {
		return new JavaIsoVisitor<ExecutionContext> () {
			@Override
			public J.Literal visitLiteral (J.Literal literal, ExecutionContext ctx) {
				if (!from.equals (literal.getValue ())) return literal;
				return literal.withValue (to).withValueSource ('"' + to + '"');
			}
		};
	}
}
