// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.tests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.m3.parser.M3ASTAuthoritativeModels;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.Sources;
import org.apache.maven.impl.standalone.ApiRunner;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/** Real pinned Maven 4 implementation, incumbent Maven 3 and JDT; no provider stubs. */
public final class M3Maven4ProviderContract {
    private static final M3ASTIngress.Limits ROOM = new M3ASTIngress.Limits(200_000, 20_000, 64);
    private static int checks;
    private M3Maven4ProviderContract() { }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static void refuses(Class<? extends Throwable> type, Action action, String reason) throws Exception {
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) { check(true, reason); return; }
            throw new AssertionError(reason + ": wrong failure " + failure, failure);
        }
        throw new AssertionError(reason + ": admitted");
    }

    private static M3ASTIndex apply4(Method entry, M3File source, Object result,
            M3ASTIngress.Limits limits) throws Exception {
        try { return (M3ASTIndex) entry.invoke(null, source, result, limits); }
        catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        }
    }

    private static long count(M3ASTIndex index, String kind) {
        long value = 0;
        for (int node = 0; node < index.nodeCount(); node++) if (index.kind(node).equals(kind)) value++;
        return value;
    }

    private static String shape(M3ASTIndex index) {
        StringBuilder value = new StringBuilder(index.domain().name());
        for (int node = 0; node < index.nodeCount(); node++) {
            value.append('|').append(index.kind(node)).append(':').append(index.parent(node))
                    .append(':').append(index.flags(node)).append(':').append(index.start(node))
                    .append(':').append(index.end(node)).append(':').append(index.firstChild(node))
                    .append(':').append(index.nextSibling(node)).append(':').append(index.childCount(node));
        }
        return value.toString();
    }

    private static M3File file(Path path) throws Exception {
        return M3File.read(path, StandardCharsets.UTF_8, 200_000);
    }

    private static Path write(Path directory, String relative, String text) throws Exception {
        Path file = directory.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return file;
    }

    private static ModelBuilderRequest request(Session session, Path pom, List<String> active) {
        return ModelBuilderRequest.builder().session(session)
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .source(Sources.buildSource(pom)).activeProfileIds(active).build();
    }

    private static void projection(Method entry, M3File source, ModelBuilderResult original) throws Exception {
        Model model = original.getEffectiveModel();
        M3ASTIndex index = apply4(entry, source, original, ROOM);
        check(index.source() == source, "existing M3File identity retained");
        check(index.domain() == M3ASTIndex.Domain.MAVEN_POM, "existing Maven domain retained");
        check(count(index, "MAVEN_EFFECTIVE_MODEL") == 1, "one effective model root");
        check(count(index, "MAVEN_DEPENDENCY") == model.getDependencies().size(), "actual effective dependencies");
        check(count(index, "MAVEN_PROFILE") == model.getProfiles().size(), "actual profile definitions");
        check(count(index, "MAVEN_REPOSITORY") == model.getRepositories().size(), "actual effective repositories");
        check(count(index, "MAVEN_PLUGIN_REPOSITORY") == model.getPluginRepositories().size(), "actual plugin repositories");
        check(count(index, "MAVEN_INHERITED_PARENT") == (model.getParent() == null ? 0 : 1), "actual parent presence");
        if (model.getDependencyManagement() != null) {
            check(count(index, "MAVEN_MANAGED_DEPENDENCY")
                    == model.getDependencyManagement().getDependencies().size(), "actual inherited/BOM management");
        }
        if (model.getBuild() != null) {
            check(count(index, "MAVEN_PLUGIN") == model.getBuild().getPlugins().size(), "actual build plugins");
            long executions = model.getBuild().getPlugins().stream().mapToLong(p -> p.getExecutions().size()).sum();
            long dependencies = model.getBuild().getPlugins().stream().mapToLong(p -> p.getDependencies().size()).sum();
            check(count(index, "MAVEN_PLUGIN_EXECUTION") == executions, "original plugin execution groups");
            check(count(index, "MAVEN_PLUGIN_DEPENDENCY") == dependencies, "original plugin dependency groups");
            if (model.getBuild().getPluginManagement() != null) {
                check(count(index, "MAVEN_MANAGED_PLUGIN")
                        == model.getBuild().getPluginManagement().getPlugins().size(), "original managed plugins");
            }
        }
        for (int node = 0; node < index.nodeCount(); node++) {
            check(index.start(node) == -1 && index.end(node) == -1, "no fabricated effective source positions");
            check(index.flags(node) == M3ASTIndex.SOURCE_UNVERIFIED && !index.hasExactRange(node), "explicit source uncertainty");
            check(node == 0 ? index.parent(node) == -1 : index.parent(node) >= 0 && index.parent(node) < node,
                    "parent ordinals precede occurrences");
        }
        refuses(IllegalStateException.class, () -> index.sourceText(0), "effective root has no exact source slice");
        check(shape(index).equals(shape(apply4(entry, source, original,
                new M3ASTIngress.Limits(source.length(), index.nodeCount(), 4)))), "exact source/node/depth budget accepted");
        refuses(IllegalArgumentException.class, () -> apply4(entry, source, original,
                new M3ASTIngress.Limits(source.length(), index.nodeCount() - 1, 64)), "node budget enforced");
    }

    @SuppressWarnings("deprecation")
    private static void jdt(Path directory) throws Exception {
        String text = "package demo; class Cafe { String text = \"𝄞 not a class\"; int n = 42; }";
        M3File source = M3File.of(directory.resolve("Cafe.java").toUri(), text);
        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(text.toCharArray());
        parser.setResolveBindings(false);
        CompilationUnit original = (CompilationUnit) parser.createAST(null);
        check(original.getProblems().length == 0, "actual incumbent JDT parser accepted source");
        M3ASTIndex index = M3ASTAuthoritativeModels.eclipseJdt(source, original, text, ROOM);
        check(index.source() == source && index.sourceText(0).equals(text), "JDT keeps original source generation and ranges");
        check(count(index, "JDT_TypeDeclaration") == 1 && count(index, "JDT_StringLiteral") == 1,
                "actual JDT syntax authority unchanged");
        refuses(IllegalArgumentException.class,
                () -> M3ASTAuthoritativeModels.eclipseJdt(source, original, text + "// changed", ROOM),
                "JDT generation drift remains refused");
        System.out.println("INCUMBENT_JDT_SHAPE " + shape(index));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !(args[0].equals("baseline") || args[0].equals("candidate"))) {
            throw new IllegalArgumentException("baseline|candidate plus an absent fixture directory required");
        }
        Path directory = Path.of(args[1]).toAbsolutePath();
        if (Files.exists(directory)) throw new IllegalArgumentException("fixture output must be absent");
        Files.createDirectories(directory);
        Path userHome = Path.of(System.getProperty("user.home"));
        check(userHome.startsWith(directory.getParent()), "isolated test user.home supplied by runner");
        write(userHome, ".m2/settings.xml", "<settings><offline>true</offline></settings>");
        Path local = directory.resolve("repository");
        String bom = """
                <project><modelVersion>4.0.0</modelVersion><groupId>com.example</groupId>
                <artifactId>offline-bom</artifactId><version>1</version><packaging>pom</packaging>
                <dependencyManagement><dependencies><dependency><groupId>from.bom</groupId>
                <artifactId>managed</artifactId><version>7</version></dependency></dependencies></dependencyManagement>
                </project>
                """;
        write(local, "com/example/offline-bom/1/offline-bom-1.pom", bom);
        String parent = """
                <project><modelVersion>4.0.0</modelVersion><groupId>com.example</groupId>
                <artifactId>parent</artifactId><version>1</version><packaging>pom</packaging>
                <modules><module>child</module></modules><properties><inherited.version>9</inherited.version></properties>
                <dependencies><dependency><groupId>inherited</groupId><artifactId>one</artifactId>
                <version>${inherited.version}</version></dependency></dependencies>
                <dependencyManagement><dependencies><dependency><groupId>com.example</groupId>
                <artifactId>offline-bom</artifactId><version>1</version><type>pom</type><scope>import</scope>
                </dependency></dependencies></dependencyManagement>
                <build><plugins><plugin><groupId>fixture</groupId><artifactId>plugin</artifactId><version>1</version>
                <executions><execution><id>fixture-run</id><goals><goal>never-executed</goal></goals></execution></executions>
                <dependencies><dependency><groupId>fixture</groupId><artifactId>plugin-dependency</artifactId><version>1</version>
                </dependency></dependencies></plugin></plugins></build>
                </project>
                """;
        String child = """
                <project><modelVersion>4.0.0</modelVersion><parent><groupId>com.example</groupId>
                <artifactId>parent</artifactId><version>1</version></parent><artifactId>child</artifactId>
                <dependencies><dependency><groupId>from.bom</groupId><artifactId>managed</artifactId></dependency></dependencies>
                <profiles><profile><id>ci</id><dependencies><dependency><groupId>profile</groupId>
                <artifactId>active</artifactId><version>2</version></dependency></dependencies></profile></profiles>
                </project>
                """;
        Files.createDirectories(directory.resolve("project/.mvn"));
        Path parentPom = write(directory, "project/pom.xml", parent);
        Path childPom = write(directory, "project/child/pom.xml", child);
        Session session = ApiRunner.createSession(null, local, ApiRunner.SecurityMode.REQUIRED);
        ModelBuilder builder = session.getService(ModelBuilder.class);
        check(builder.getClass().getName().equals("org.apache.maven.impl.model.DefaultModelBuilder"), "real pinned Maven 4 builder");
        ModelBuilderResult childResult = builder.newSession().build(request(session, childPom, List.of("ci")));
        Model effective = childResult.getEffectiveModel();
        check(effective != null && effective.getDependencies().size() == 3, "real inheritance and activated profile");
        check(effective.getDependencies().stream().anyMatch(d -> d.getGroupId().equals("from.bom") && d.getVersion().equals("7")),
                "actual offline imported BOM supplied managed version");
        check(effective.getDependencies().stream().anyMatch(d -> d.getGroupId().equals("inherited") && d.getVersion().equals("9")),
                "actual inherited property interpolated by Maven");
        check(childResult.getActivePomProfiles().stream().anyMatch(p -> p.getId().equals("ci")), "Maven owns activation result");

        String legacyPom = """
                <project><modelVersion>4.0.0</modelVersion><groupId>legacy</groupId><artifactId>proof</artifactId>
                <version>1</version><profiles><profile><id>ci</id><dependencies><dependency><groupId>active</groupId>
                <artifactId>dependency</artifactId><version>2</version></dependency></dependencies></profile></profiles></project>
                """;
        Path legacyFile = write(directory, "legacy/pom.xml", legacyPom);
        ModelBuildingResult legacy = new DefaultModelBuilderFactory().newInstance().build(
                new DefaultModelBuildingRequest().setPomFile(legacyFile.toFile()).setActiveProfileIds(List.of("ci"))
                        .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL).setProcessPlugins(false));
        M3ASTIndex legacyIndex = M3ASTAuthoritativeModels.mavenEffective(file(legacyFile), legacy, ROOM);
        check(count(legacyIndex, "MAVEN_DEPENDENCY") == 1, "actual Maven 3 provider preserved");
        System.out.println("INCUMBENT_MAVEN3_SHAPE " + shape(legacyIndex));
        jdt(directory);
        refuses(IllegalArgumentException.class,
                () -> M3ASTAuthoritativeModels.mavenEffective(file(childPom), childResult, ROOM),
                "Maven 4 result is not silently accepted as Maven 3");

        Method entry = null;
        try { entry = M3ASTAuthoritativeModels.class.getMethod("maven4Effective", M3File.class, Object.class, M3ASTIngress.Limits.class); }
        catch (NoSuchMethodException absent) {
            if (args[0].equals("candidate")) throw new AssertionError("Maven 4 entry missing", absent);
        }
        if (args[0].equals("baseline")) {
            check(entry == null, "frozen baseline has no Maven 4 API");
            System.out.println("M3_MAVEN4_BASELINE_GAP checks=" + checks);
            return;
        }
        final Method maven4 = Objects.requireNonNull(entry);
        M3File childSource = file(childPom);
        projection(maven4, childSource, childResult);
        ModelBuilderResult parentResult = builder.newSession().build(request(session, parentPom, List.of()));
        M3ASTIndex parentIndex = apply4(maven4, file(parentPom), parentResult, ROOM);
        check(count(parentIndex, "MAVEN_MODULE") == 1, "legacy modules fallback follows actual Maven 4 model");
        ModelBuilderResult validation = builder.newSession().validate(request(session, childPom, List.of("ci")));
        check(validation.getRawModel() != null && validation.getEffectiveModel() == null, "real validate-only result is incomplete");
        refuses(IllegalArgumentException.class, () -> apply4(maven4, childSource, validation, ROOM), "raw result is not an effective model");
        refuses(IllegalArgumentException.class, () -> apply4(maven4, childSource, legacy, ROOM), "Maven 3 result not silently accepted as Maven 4");
        refuses(IllegalArgumentException.class, () -> apply4(maven4, childSource, new Object(), ROOM), "foreign provider refused");
        refuses(NullPointerException.class, () -> apply4(maven4, childSource, null, ROOM), "null provider refused");
        refuses(IllegalArgumentException.class, () -> apply4(maven4, childSource, childResult,
                new M3ASTIngress.Limits(childSource.length() - 1, 20_000, 64)), "source budget enforced");
        refuses(IllegalArgumentException.class, () -> apply4(maven4, childSource, childResult,
                new M3ASTIngress.Limits(childSource.length(), 20_000, 3)), "four-level plugin occurrence depth enforced");

        // Instrument only the real result interface; every returned model is the actual builder's object.
        AtomicInteger accesses = new AtomicInteger();
        Object observed = Proxy.newProxyInstance(ModelBuilderResult.class.getClassLoader(),
                new Class<?>[] {ModelBuilderResult.class}, (proxy, method, values) -> {
                    if (!method.getName().equals("getEffectiveModel") || method.getParameterCount() != 0) {
                        throw new AssertionError("projection called a builder/source/child accessor: " + method);
                    }
                    accesses.incrementAndGet();
                    return childResult.getEffectiveModel();
                });
        Files.writeString(childPom, "not XML anymore");
        M3ASTIndex observedIndex = apply4(maven4, childSource, observed, ROOM);
        check(accesses.get() == 1, "completed model read once; no parse, resolution or child traversal");
        check(shape(observedIndex).equals(shape(apply4(maven4, childSource, childResult, ROOM))),
                "detached index does not reopen the modified POM");
        check(childResult.getEffectiveModel() == effective && effective.getDependencies().size() == 3,
                "original effective model identity and values unchanged");

        Files.createDirectories(directory.resolve("modern/.mvn"));
        Path modern = write(directory, "modern/pom.xml", """
                <project xmlns="http://maven.apache.org/POM/4.1.0"><modelVersion>4.1.0</modelVersion><groupId>modern</groupId><artifactId>parent</artifactId>
                <version>1</version><packaging>pom</packaging><subprojects><subproject>one</subproject>
                <subproject>two</subproject></subprojects></project>
                """);
        for (String name : List.of("one", "two")) write(directory, "modern/" + name + "/pom.xml",
                "<project xmlns=\"http://maven.apache.org/POM/4.1.0\"><modelVersion>4.1.0</modelVersion><parent><groupId>modern</groupId>"
                        + "<artifactId>parent</artifactId><version>1</version></parent><artifactId>" + name + "</artifactId></project>");
        ModelBuilderResult modernResult = builder.newSession().build(request(session, modern, List.of()));
        check(modernResult.getEffectiveModel().getSubprojects().equals(List.of("one", "two")), "real Maven 4 subprojects");
        check(count(apply4(maven4, file(modern), modernResult, ROOM), "MAVEN_MODULE") == 2,
                "subproject occurrences use the existing module kind");
        projection(maven4, file(modern), modernResult);
        System.out.println("M3_MAVEN4_REAL_PROVIDER_PASS checks=" + checks);
    }
}
