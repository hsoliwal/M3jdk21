/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Removes imports that the type renames leave behind when a file moves package: imports of the file's own package,
 * and imports from {@code com.m3.viewport} inside {@code com.m3.collections} (the collections module does not depend
 * on the viewport core; such imports come only from Javadoc references).
 */
public final class M3TidyImports extends Recipe {
	static final String COLLECTIONS = "com.m3.collections";
	static final String VIEWPORT = "com.m3.viewport";

	@Override
	public String getDisplayName () {
		return "M3 tidy imports after package moves";
	}

	@Override
	public String getDescription () {
		return "Drop same-package imports and viewport imports inside the collections package.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor () {
		return new JavaIsoVisitor<ExecutionContext> () {
			@Override
			public J.CompilationUnit visitCompilationUnit (J.CompilationUnit cu, ExecutionContext ctx) {
				if (cu.getPackageDeclaration () == null) return cu;
				String pkg = cu.getPackageDeclaration ().getPackageName ();
				List<J.Import> kept = cu.getImports ().stream ().filter (i -> keep (pkg, i)).toList ();
				return kept.size () == cu.getImports ().size () ? cu : cu.withImports (kept);
			}
		};
	}

	static boolean keep (String pkg, J.Import i) {
		if (i.isStatic ()) return true;
		String name = i.getQualid ().printTrimmed ();
		return !topLevelIn (name, pkg) && !(pkg.equals (COLLECTIONS) && topLevelIn (name, VIEWPORT));
	}

	/** True when {@code name} is {@code pkg.Type} or {@code pkg.*} (not a nested type, which still needs its import). */
	static boolean topLevelIn (String name, String pkg) {
		return name.startsWith (pkg + ".") && name.indexOf ('.', pkg.length () + 1) < 0;
	}
}
