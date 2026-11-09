/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal and contributors - source-built compiler runtime contracts
 *******************************************************************************/
package org.eclipse.jdt.core.tests.dom;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import junit.framework.TestCase;
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

/** Checks the actual runtime closure of the original, source-built ECJ. */
public class JdtSourceCompatibilityTest extends TestCase {

    public void testEcjCompilesJava21AndRunsAnnotationProcessor() throws Exception {
        Path directory = Files.createTempDirectory("jdt-source-runtime-");
        try {
            Path input = directory.resolve("X.java");
            Files.writeString(input, "public record X(int value) { /** retained documentation */ public int doubled() { return value * 2; } }");
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaCompiler compiler = new EclipseCompiler();
            AtomicInteger processed = new AtomicInteger();
            try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
                files.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(directory));
                JavaCompiler.CompilationTask task = compiler.getTask(null, files, diagnostics,
                        List.of("--release", "21"), null, files.getJavaFileObjectsFromPaths(List.of(input)));
                task.setProcessors(List.of(new AbstractProcessor() {
                    @Override
                    public Set<String> getSupportedAnnotationTypes() {
                        return Set.of("*");
                    }

                    @Override
                    public SourceVersion getSupportedSourceVersion() {
                        return SourceVersion.latestSupported();
                    }

                    @Override
                    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
                        if (!round.processingOver()) {
                            TypeElement type = this.processingEnv.getElementUtils().getTypeElement("X");
                            assertNotNull(type);
                            assertEquals(1, type.getRecordComponents().size());
                            Element method = type.getEnclosedElements().stream()
                                    .filter(element -> element.getSimpleName().contentEquals("doubled"))
                                    .findFirst().orElseThrow();
                            assertTrue(this.processingEnv.getElementUtils().getDocComment(method).contains("retained documentation"));
                            processed.incrementAndGet();
                        }
                        return false;
                    }
                }));
                assertTrue(diagnostics.getDiagnostics().toString(), task.call());
            }
            assertEquals(1, processed.get());
            byte[] output = Files.readAllBytes(directory.resolve("X.class"));
            assertEquals(65, ((output[6] & 0xff) << 8) | (output[7] & 0xff));
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(file);
                }
            }
        }
    }

    public void testCompilerClassBytecodeHasUpstreamJava17Target() throws IOException {
        try (var input = EclipseCompiler.class.getResourceAsStream("EclipseCompiler.class")) {
            assertNotNull(input);
            byte[] header = input.readNBytes(8);
            assertEquals(61, ((header[6] & 0xff) << 8) | (header[7] & 0xff));
        }
    }
}
