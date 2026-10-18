/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.viewport.recipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/**
 * Applies one named recipe to a set of Java files parsed together, so references between the files are typed.
 * <p>Usage: {@code M3BatchApply <recipe> <out-root|-> <file>...}. With an out root each result is written to
 * {@code <out-root>/<package path>/<class>.java} (the location after renames); with {@code -} files are rewritten in
 * place. Prints {@code CHANGED} or {@code UNCHANGED} with source and target per file.</p>
 */
public final class M3BatchApply {
	private M3BatchApply () {
	}

	public static void main (String[] args) throws IOException {
		if (args.length < 3) {
			System.err.println ("usage: M3BatchApply <recipe> <out-root|-> <file>...");
			System.exit (2);
		}
		Recipe recipe = Environment.builder ().scanRuntimeClasspath ("com.m3.viewport.recipe").build ().activateRecipes (args[0]);
		ExecutionContext ctx = new InMemoryExecutionContext (e -> {
			throw new IllegalStateException (e);
		});
		List<Path> inputs = new ArrayList<> ();
		List<String> texts = new ArrayList<> ();
		for (int i = 2; i < args.length; i++) {
			inputs.add (Path.of (args[i]));
			texts.add (Files.readString (Path.of (args[i]), StandardCharsets.UTF_8));
		}
		List<SourceFile> parsed = JavaParser.fromJavaVersion ().build ().parse (ctx, texts.toArray (String[]::new)).toList ();
		if (parsed.size () != inputs.size ()) throw new IllegalStateException ("parsed " + parsed.size () + " of " + inputs.size ());
		Map<SourceFile, Path> origin = new HashMap<> ();
		for (int i = 0; i < parsed.size (); i++) origin.put (parsed.get (i), inputs.get (i));
		Map<Path, String> after = new HashMap<> ();
		for (Result r : recipe.run (new InMemoryLargeSourceSet (parsed), ctx).getChangeset ().getAllResults ()) {
			if (r.getBefore () != null && r.getAfter () != null) after.put (origin.get (r.getBefore ()), r.getAfter ().printAll ());
		}
		for (int i = 0; i < parsed.size (); i++) write (args[1], inputs.get (i), texts.get (i), after.get (inputs.get (i)));
	}

	private static void write (String outRoot, Path input, String before, String changed) throws IOException {
		String text = changed == null ? before : changed;
		Path target = "-".equals (outRoot) ? input : Path.of (outRoot).resolve (location (text));
		Files.createDirectories (target.toAbsolutePath ().getParent ());
		if (changed != null || !"-".equals (outRoot)) Files.writeString (target, text, StandardCharsets.UTF_8);
		System.out.println ((changed == null ? "UNCHANGED\t" : "CHANGED\t") + input + "\t" + target);
	}

	static String location (String text) {
		List<SourceFile> cu = JavaParser.fromJavaVersion ().build ().parse (text).toList ();
		J.CompilationUnit unit = (J.CompilationUnit) cu.get (0);
		String pkg = unit.getPackageDeclaration () == null ? "" : unit.getPackageDeclaration ().getPackageName ().replace ('.', '/') + "/";
		return pkg + unit.getClasses ().get (0).getSimpleName () + ".java";
	}
}
