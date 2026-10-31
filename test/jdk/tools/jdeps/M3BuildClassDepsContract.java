/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only,
 * as published by the Free Software Foundation. Oracle designates
 * this particular file as subject to the "Classpath" exception.
 */
package com.sun.tools.jdeps;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Dependencies;
import com.sun.tools.classfile.Dependency;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Genuine JDK javac -> .class -> original jdeps finder differential proof. */
public final class M3BuildClassDepsContract {
    private static int checks;
    private M3BuildClassDepsContract() { }
    private static void check(boolean value, String detail) {
        checks++; if (!value) throw new AssertionError(detail);
    }
    private static void refuse(Throwing code) throws Exception {
        checks++;
        try { code.run(); throw new AssertionError("expected fail-closed IOException"); }
        catch (IOException expected) { }
    }
    @FunctionalInterface private interface Throwing { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("m3-jdeps-contract-");
        try {
            Path src = temp.resolve("src/p/Probe.java");
            Files.createDirectories(src.getParent());
            Files.writeString(src, "package p; public class Probe extends java.util.ArrayList<String> { "
                    + "public java.util.List<java.time.Instant> dates; "
                    + "private java.util.concurrent.ConcurrentHashMap<String, Integer> work; "
                    + "public java.util.List<java.time.Instant> take(java.time.Instant x) { "
                    + "return java.util.List.of(x); } }\n");
            Path output = temp.resolve("classes");
            Files.createDirectory(output);
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            check(compiler != null, "original JDK compiler present");
            try (StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null)) {
                boolean passed = compiler.getTask(null, manager, null, List.of(
                        "--release", "21", "-proc:none", "-d", output.toString()),
                        null, manager.getJavaFileObjectsFromPaths(List.of(src))).call();
                check(passed, "original javac fixture compiled");
            }
            Path file = output.resolve("p/Probe.class");
            byte[] bytecode = Files.readAllBytes(file);
            var all = M3BuildClassDeps.inspect(file, M3BuildClassDeps.Scope.CLASS, 100_000, 1000);
            var api = M3BuildClassDeps.inspect(file, M3BuildClassDeps.Scope.PUBLIC_API, 100_000, 1000);
            check("p.Probe".equals(all.className()), "exact source class identity");
            check(all.targets().length > 2 && api.targets().length > 1, "original JDK jdeps finds real targets");
            check(all.hasTarget("java.util.concurrent.ConcurrentHashMap"), "full class dependencies include private bytecode");
            check(api.hasTarget("java.time.Instant"), "API signatures expose external type");
            check(!api.hasTarget("java.util.concurrent.ConcurrentHashMap"), "API finder excludes private declaration");
            check(all.hasTarget("java.time.Instant"), "class finder retains public signature");
            check(Arrays.equals(all.contentSha256(), MessageDigest.getInstance("SHA-256").digest(bytecode)), "byte-exact SHA-256");
            String[] escaped = all.targets(); escaped[0] = "tampered";
            check(!all.hasTarget("tampered"), "defensive sorted target names");
            byte[] hash = all.contentSha256(); hash[0] ^= 1;
            check(!Arrays.equals(hash, all.contentSha256()), "defensive source digest");
            StringWriter text = new StringWriter();
            all.writeTo(new PrintWriter(text));
            String[] lines = text.toString().split("\\R");
            check(lines.length == all.targets().length + 1, "single header plus one row per target");
            check(lines[0].startsWith("M3JDK21_CLASS_DEPS_V1\tCLASS\t"), "stable machine schema");
            check(lines[0].split("\\t", -1).length == 5, "exact wire header column count");
            Set<String> actual = new HashSet<>();
            for (int i = 1; i < lines.length; i++) {
                check(lines[i].startsWith("D\t"), "explicit dependency row");
                actual.add(new String(Base64.getUrlDecoder().decode(lines[i].substring(2)), StandardCharsets.UTF_8));
            }
            check(actual.equals(Set.of(all.targets())), "lossless Base64URL class identities");
            ClassFile original = ClassFile.read(new ByteArrayInputStream(bytecode));
            Set<String> oracle = new HashSet<>();
            for (Dependency dependency : Dependencies.getClassDependencyFinder().findDependencies(original))
                oracle.add(dependency.getTarget().getClassName());
            check(oracle.equals(actual), "differential against original JDK jdeps dependency finder");
            M3BuildClassDeps provider = new M3BuildClassDeps();
            StringWriter out = new StringWriter(), err = new StringWriter();
            check(provider.name().equals("m3-class-deps"), "native ToolProvider name");
            check(provider.run(new PrintWriter(out), new PrintWriter(err), file.toString()) == 0, "tool provider executes original analyzer");
            check(out.toString().equals(text.toString()), "tool output matches direct SDK API");
            check(provider.run(new PrintWriter(new StringWriter()), new PrintWriter(new StringWriter()),
                    "--api", file.toString()) == 0, "API selection works");
            check(provider.run(new PrintWriter(out), new PrintWriter(err), "--bogus", file.toString()) == 2, "unknown option refused");
            Path symlink = temp.resolve("link.class");
            Files.createSymbolicLink(symlink, file);
            refuse(() -> M3BuildClassDeps.inspect(symlink, M3BuildClassDeps.Scope.CLASS, 100_000, 1000));
            refuse(() -> M3BuildClassDeps.inspect(file, M3BuildClassDeps.Scope.CLASS, 8, 1000));
            refuse(() -> M3BuildClassDeps.inspect(file, M3BuildClassDeps.Scope.CLASS, 100_000, 1));
            Path broken = temp.resolve("broken.class"); Files.write(broken, new byte[] {1, 2, 3});
            refuse(() -> M3BuildClassDeps.inspect(broken, M3BuildClassDeps.Scope.CLASS, 100, 10));
            for (int n = 0; n < 250; n++) {
                var same = M3BuildClassDeps.inspect(file, M3BuildClassDeps.Scope.CLASS, 100_000, 1000);
                check(Arrays.equals(same.targets(), all.targets()), "deterministic indexed collector");
            }
            System.out.println("M3_JDK_BUILD_DEPS_PASS checks=" + checks);
        } finally {
            // Test fixtures live in system temp, not in source worktree.
        }
    }
}
