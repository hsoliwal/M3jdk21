/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.runtime.spring;

import com.m3.runtime.test.ModuleFixture;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import static org.junit.jupiter.api.Assertions.*;

class OrdinarySpringModuleScanTest {
    @TempDir Path temporary;

    @Test
    void standardSpringResolverAndContextUseReceivingSourceForBootModule() throws Exception {
        Path jar = ModuleFixture.compileJar(temporary, "m3-spring-fixture.jar",
                Map.of("modulefixture/Hello.java", """
                    package modulefixture;
                    @org.springframework.stereotype.Component("moduleHello")
                    public class Hello {
                        public String value() { return "ordinary bean"; }
                    }
                    """), Map.of("Automatic-Module-Name", "m3.spring.fixture"), Map.of());
        Path compiledSource = Path.of(OrdinarySpringModuleScanTest.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getParent().resolve("classes");
        assertEquals(compiledSource.toRealPath(), Path.of(PathMatchingResourcePatternResolver.class
                .getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath(),
                "test must execute the compiled original Spring class with its receiving refinement");
        List<String> command = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dm3.module.resources.native-required=" + System.getProperty("m3.module.resources.native-required", "false"),
                "-Djava.library.path=" + System.getProperty("java.library.path"),
                "--module-path", jar.toString(), "--add-modules", "m3.spring.fixture",
                "-classpath", ModuleFixture.classPath(),
                OrdinarySpringMain.class.getName(), compiledSource.toString()));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean completed = process.waitFor(45, TimeUnit.SECONDS);
        if (!completed) process.destroyForcibly();
        assertTrue(completed, "ordinary Spring scan timed out");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("ORDINARY_SPRING_SOURCE_AND_MODULE_BEAN_OK"), output);
    }
}
