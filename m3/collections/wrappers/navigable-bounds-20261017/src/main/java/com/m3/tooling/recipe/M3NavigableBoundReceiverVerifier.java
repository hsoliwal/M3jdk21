// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.recipe;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Compiles the installed receiver with Java 21 and runs its no-dependency differential proof. */
public final class M3NavigableBoundReceiverVerifier {
    private M3NavigableBoundReceiverVerifier() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: receiver.java proof.java");
        }
        Path receiver = Path.of(args[0]).toAbsolutePath().normalize();
        Path proof = Path.of(args[1]).toAbsolutePath().normalize();
        M3NavigableBoundRecipe.main(new String[] {"--verify", receiver.toString()});
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) throw new IllegalStateException("JDK21_JAVAC_REQUIRED");
        Path output = Files.createTempDirectory("m3-navigation-proof-");
        try {
            int status = javac.run(null, System.out, System.err, "--release", "21",
                    "-Xlint:all", "-Werror", "-d", output.toString(),
                    receiver.toString(), proof.toString());
            if (status != 0) throw new AssertionError("M3_NAV_BOUND_JAVAC_FAILED:" + status);
            try (URLClassLoader loader = new URLClassLoader(
                    new URL[] {output.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                Class<?> corpus = Class.forName(
                        "com.m3.collections.M3NavigableBoundaryProof", true, loader);
                try {
                    corpus.getMethod("main", String[].class)
                            .invoke(null, (Object) new String[0]);
                } catch (InvocationTargetException failure) {
                    if (failure.getCause() instanceof Exception ex) throw ex;
                    if (failure.getCause() instanceof Error ex) throw ex;
                    throw failure;
                }
            }
        } finally {
            try (var paths = Files.walk(output)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
