// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.tests;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3ASTAuthoritativeModels;
import jdk.m3.parser.M3File;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.codehaus.groovy.control.SourceUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/** Real-library proof: no JDT/Maven/Groovy mock classes are on this classpath. */
public final class M3RealProvidersSmoke {
    private static final AtomicInteger CHECKS = new AtomicInteger();
    private static final M3ASTIngress.Limits LIMITS =
            new M3ASTIngress.Limits(2_000_000, 500_000, 500);
    private M3RealProvidersSmoke() { }
    private static M3File source(String name, String content) {
        return M3File.of(URI.create("memory:/" + name), content);
    }
    private static void check(boolean accepted, String description) {
        CHECKS.incrementAndGet();
        if (!accepted) throw new AssertionError(description);
    }
    private static long count(M3ASTIndex index, String kind) {
        long found = 0;
        for (int node = 0; node < index.nodeCount(); node++) {
            if (Objects.equals(index.kind(node), kind)) found++;
        }
        return found;
    }
    private static void eclipse() {
        String code = "package demo; class Cafe { String text = \"not a class\"; int n = 42; }";
        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(code.toCharArray());
        parser.setResolveBindings(false);
        CompilationUnit original = (CompilationUnit) parser.createAST(null);
        check(original.getProblems().length == 0, "JDT original syntax accepted");
        M3ASTIndex index = M3ASTAuthoritativeModels.eclipseJdt(
                source("Cafe.java", code), original, code, LIMITS);
        check(index.domain() == M3ASTIndex.Domain.ECLIPSE_JDT, "JDT domain");
        check(index.nodeCount() >= 8, "actual JDT DOM occurrences indexed");
        check(count(index, "JDT_TypeDeclaration") >= 1, "actual JDT type nodes");
        check(count(index, "JDT_StringLiteral") == 1, "JDT string literal");
        check(index.hasExactRange(0), "original JDT range retained");
        check(index.sourceText(0).equals(code), "full source exact");
        try {
            M3ASTAuthoritativeModels.eclipseJdt(
                    source("Cafe.java", code + "// changed"), original, code, LIMITS);
            throw new AssertionError("source-generation drift admitted");
        } catch (IllegalArgumentException expected) {
            check(true, "changed JDT source refused");
        }
    }
    private static void maven(Path directory) throws Exception {
        String pom = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">"
                + "<modelVersion>4.0.0</modelVersion><groupId>com.example</groupId>"
                + "<artifactId>m3test</artifactId><version>1</version>"
                + "<dependencies><dependency><groupId>x</groupId><artifactId>one</artifactId>"
                + "<version>1</version></dependency></dependencies>"
                + "<profiles><profile><id>ci</id><dependencies>"
                + "<dependency><groupId>y</groupId><artifactId>two</artifactId>"
                + "<version>2</version></dependency></dependencies></profile></profiles></project>";
        Path file = directory.resolve("pom.xml");
        Files.writeString(file, pom, StandardCharsets.UTF_8);
        ModelBuildingRequest request = new DefaultModelBuildingRequest()
                .setPomFile(file.toFile())
                .setActiveProfileIds(List.of("ci"))
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
                .setProcessPlugins(false);
        ModelBuildingResult original = new DefaultModelBuilderFactory().newInstance().build(request);
        check(original.getEffectiveModel() != null, "actual Maven effective model");
        check(original.getEffectiveModel().getDependencies().size() == 2,
                "Maven original activated profile dependency count");
        M3ASTIndex index = M3ASTAuthoritativeModels.mavenEffective(
                source("pom.xml", pom), original, LIMITS);
        check(index.domain() == M3ASTIndex.Domain.MAVEN_POM, "Maven domain");
        check(count(index, "MAVEN_DEPENDENCY")
                        == original.getEffectiveModel().getDependencies().size(),
                "Maven effective dependencies preserved");
        check(!index.hasExactRange(0), "effective model does not invent source range");
    }
    private static void groovy() throws Exception {
        String script = "plugins { id 'java' }\n"
                + "def groupName = 'com.example'\n"
                + "dependencies { implementation 'x:y:1' }\n"
                + "// implementation 'not:code:0'\n";
        SourceUnit unit = SourceUnit.create("build.gradle", script);
        unit.parse();
        unit.completePhase();
        unit.convert();
        check(unit.getAST() != null, "real Groovy parser returned ModuleNode");
        M3ASTIndex index = M3ASTAuthoritativeModels.gradleGroovyAst(
                source("build.gradle", script), unit.getAST(), LIMITS);
        check(index.domain() == M3ASTIndex.Domain.GRADLE_GROOVY, "Groovy domain");
        check(index.nodeCount() >= 5, "real Groovy AST child projection");
        check(count(index, "GROOVY_MethodCallExpression") >= 1, "Groovy calls indexed");
        check(!index.hasExactRange(0), "unknown Groovy source ranges not claimed");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("one absent directory required");
        Path directory = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(directory)) throw new IllegalArgumentException("output must be absent");
        Files.createDirectory(directory);
        eclipse();
        maven(directory);
        groovy();
        String marker = "M3_AST_REAL_PROVIDERS_PASS checks=" + CHECKS.get();
        Files.writeString(directory.resolve("RESULT.txt"), marker + "\n", StandardCharsets.UTF_8);
        System.out.println(marker);
    }
}
