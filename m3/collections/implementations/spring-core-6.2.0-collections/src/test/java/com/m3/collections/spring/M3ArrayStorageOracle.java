/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/

// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project

package com.m3.collections.spring;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;


/**
 * Independently compile the exact preimages of the two array storage owners.
 *
 * @author Hitesh Soliwal and Contributors to the Synexia Project
 * @since 7.1
 */
public final class M3ArrayStorageOracle implements AutoCloseable {

	private static final Map<String, String> SOURCE_HASHES = Map.of(
			"com.m3.collections.spring.CompositeIterator",
			"0c80731fd927bcaf3b6f3969974b577c1e02492d8639984a598c3f307663ab5e");

	private final URLClassLoader loader;


	public M3ArrayStorageOracle(Path work) throws Exception {
		var compiler = ToolProvider.getSystemJavaCompiler();
		Assert.state(compiler != null, "A JDK is required for the original-source proof");
		List<Path> sources = new ArrayList<>();
		for (Map.Entry<String, String> entry : SOURCE_HASHES.entrySet()) {
			String leaf = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1) + ".java";
			byte[] bytes;
			try (InputStream input = getClass().getResourceAsStream(
					"/m3/array-storage-20261024/original/" + leaf + ".txt")) {
				Assert.state(input != null, "Missing original " + leaf);
				bytes = input.readAllBytes();
			}
			String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
			Assert.state(entry.getValue().equals(hash), "Original source drift: " + leaf);
			Path source = work.resolve("sources/" + entry.getKey().replace('.', '/') + ".java");
			Files.createDirectories(source.getParent());
			Files.write(source, bytes);
			sources.add(source);
		}
		Path classes = work.resolve("classes");
		Files.createDirectories(classes);
		String classPath = Path.of(Assert.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		try (var files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
			List<String> options = List.of("--release", "17", "-g", "-proc:none", "-classpath", classPath,
					"-d", classes.toString());
			if (!Boolean.TRUE.equals(compiler.getTask(null, files, diagnostics, options, null,
					files.getJavaFileObjectsFromPaths(sources)).call())) {
				throw new IllegalStateException("Original source compilation failed: " + diagnostics.getDiagnostics());
			}
		}
		this.loader = new URLClassLoader(new URL[] {classes.toUri().toURL()}, getClass().getClassLoader()) {
			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				synchronized (getClassLoadingLock(name)) {
					boolean original = SOURCE_HASHES.keySet().stream()
							.anyMatch(owner -> name.equals(owner) || name.startsWith(owner + "$"));
					if (!original) {
						return super.loadClass(name, resolve);
					}
					Class<?> loaded = findLoadedClass(name);
					if (loaded == null) {
						loaded = findClass(name);
					}
					if (resolve) {
						resolveClass(loaded);
					}
					return loaded;
				}
			}
		};
	}


	public Class<?> originalClass(String name) throws ClassNotFoundException {
		return this.loader.loadClass(name);
	}

	public static Object invoke(Method method, Object target, Object... arguments) {
		try {
			method.setAccessible(true);
			return method.invoke(target, arguments);
		}
		catch (InvocationTargetException ex) {
			if (ex.getCause() instanceof RuntimeException runtime) {
				throw runtime;
			}
			if (ex.getCause() instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException(ex.getCause());
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public void close() throws IOException {
		this.loader.close();
	}

}
