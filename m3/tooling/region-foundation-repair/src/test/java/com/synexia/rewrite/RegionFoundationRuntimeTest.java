// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Freeze the real Recipe initialization failure in an isolated, bounded child JVM. */
final class RegionFoundationRuntimeTest {
    @TempDir Path temporary;

    @Test void declaredRuntimeBootstrapsRecipeWithoutMavenHostClasses() throws Exception {
        Result result = run("complete", classpath());
        assertEquals(0, result.exit(), result.output());
        assertEquals("RECIPE_RUNTIME_BOOTSTRAP_PASS", result.output().strip());
    }

    @Test void removingOnlyLoggingApiReproducesTheFrozenFailure() throws Exception {
        String original = classpath();
        String[] entries = original.split(Pattern.quote(File.pathSeparator));
        String withoutApi = Arrays.stream(entries)
                .filter(entry -> !Path.of(entry).getFileName().toString().equals("slf4j-api-1.7.36.jar"))
                .collect(Collectors.joining(File.pathSeparator));
        assertEquals(1, entries.length - withoutApi.split(Pattern.quote(File.pathSeparator)).length);
        Result result = run("missing-api", withoutApi);
        assertNotEquals(0, result.exit(), result.output());
        assertTrue(result.output().contains("NoClassDefFoundError: org/slf4j/LoggerFactory"), result.output());
        assertTrue(result.output().contains("org.openrewrite.Recipe.<clinit>"), result.output());
    }

    @Test void runtimeContainsOneApiAndOnePinnedBinding() throws Exception {
        ClassLoader loader = RegionFoundationRuntimeTest.class.getClassLoader();
        var api = Collections.list(loader.getResources("org/slf4j/LoggerFactory.class"));
        var binding = Collections.list(loader.getResources("org/slf4j/impl/StaticLoggerBinder.class"));
        assertEquals(1, api.size(), api.toString());
        assertEquals(1, binding.size(), binding.toString());
        assertTrue(api.getFirst().toString().contains("slf4j-api-1.7.36.jar"));
        assertTrue(binding.getFirst().toString().contains("slf4j-simple-1.7.36.jar"));
    }

    private static String classpath() {
        return System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    }

    private Result run(String name, String classpath) throws Exception {
        Path output = temporary.resolve(name + ".log");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Process child = new ProcessBuilder(java.toString(), "-cp", classpath, Probe.class.getName())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(child.waitFor(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS),
                    "runtime bootstrap timed out");
            assertTrue(Files.size(output) <= 65_536, "unexpected bootstrap output size");
            return new Result(child.exitValue(), Files.readString(output, StandardCharsets.UTF_8));
        } finally {
            if (child.isAlive()) {
                child.destroyForcibly();
                assertTrue(child.waitFor(5, TimeUnit.SECONDS), "child JVM did not terminate");
            }
        }
    }

    private record Result(int exit, String output) { }

    public static final class Probe {
        private Probe() { }
        public static void main(String[] args) throws Exception {
            Class.forName("org.openrewrite.Recipe");
            System.out.println("RECIPE_RUNTIME_BOOTSTRAP_PASS");
        }
    }
}
