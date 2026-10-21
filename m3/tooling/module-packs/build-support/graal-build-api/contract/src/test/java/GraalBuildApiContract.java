/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;
import org.graalvm.buildtools.VersionInfo;
import org.graalvm.buildtools.agent.AgentConfiguration;
import org.graalvm.buildtools.agent.ConditionalAgentMode;
import org.graalvm.buildtools.agent.DirectAgentMode;
import org.graalvm.buildtools.agent.StandardAgentMode;
import org.graalvm.buildtools.utils.JarScanner;
import org.graalvm.reachability.DirectoryConfiguration;
import org.graalvm.reachability.GraalVMReachabilityMetadataRepository;
import org.graalvm.reachability.internal.FileSystemRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Independent finite installed API contract. The original common-library rules
 * are pinned in upstream/native-build-tools/common/docs/functional-spec.md,
 * sections 3.1, 3.1.1, 5.1 and 7. No build-tool or Native Image process runs.
 */
public final class GraalBuildApiContract {
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected a new contract work directory");
        }
        Path work = Path.of(args[0]).toAbsolutePath();
        Files.createDirectory(work);
        Path installed = Path.of(System.getProperty("java.home"), "lib", "m3-tools", "graal-build-api").toRealPath();
        Path utilsJar = origin(AgentConfiguration.class, installed.resolve("utils-1.1.13.jar"));
        origin(GraalVMReachabilityMetadataRepository.class, installed.resolve("graalvm-reachability-metadata-1.1.13.jar"));
        origin(JSONObject.class, installed.resolve("openjson-1.0.13.jar"));
        check(ModuleLayer.boot().findModule("jdk.zipfs").isPresent(), "installed ZIP provider module");
        check(ModuleLayer.boot().findModule("java.desktop").isPresent(), "original OpenJSON bean API module");
        check(ModuleLayer.boot().findModule("java.net.http").isPresent(), "original metadata HTTP utility module");
        absent("org.apache.maven.plugin.AbstractMojo");
        absent("org.gradle.api.Project");
        absent("org.junit.jupiter.api.Test");
        check("1.1.13".equals(VersionInfo.NBT_VERSION), "upstream build tools version");
        check("1.1.13".equals(VersionInfo.JUNIT_PLATFORM_NATIVE_VERSION), "upstream launcher version constant");
        check("1.0.14".equals(VersionInfo.METADATA_REPO_VERSION), "upstream repository version constant");

        agentContract(work);
        metadataContract(work);

        Path scan = work.resolve("installed-utils.properties");
        JarScanner.scanJar(utilsJar, scan);
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(scan)) {
            properties.load(reader);
        }
        check(Set.of(properties.getProperty("packages").split(",")).equals(Set.of(
                "org.graalvm.buildtools", "org.graalvm.buildtools.agent",
                "org.graalvm.buildtools.model.resources", "org.graalvm.buildtools.utils")),
                "original JarScanner traverses installed JAR through ZIP filesystem");
        JSONObject bean = new JSONObject(new Bean());
        check("installed-api".equals(bean.getString("name")) && bean.getBoolean("enabled"),
                "original OpenJSON public bean introspection");
        System.out.println(new JSONObject(Map.of("status", "PASS", "checks", checks,
                "javaHome", System.getProperty("java.home"), "installedJars", installed.toString(),
                "nativeImageExecution", "NOT_RUN", "agentLoaded", false,
                "wholeJdkBootstrap", "NOT_RUN")).toString());
    }

    private static Path origin(Class<?> type, Path expected) throws Exception {
        Path actual = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        check(Files.isSameFile(actual, expected), "installed original class origin " + type.getName());
        return actual;
    }

    private static void absent(String name) throws Exception {
        try {
            Class.forName(name);
            throw new AssertionError("Unexpected build/test framework in API process: " + name);
        } catch (ClassNotFoundException expected) {
            checks++;
        }
    }

    private static void agentContract(Path work) throws Exception {
        AgentConfiguration disabled = new AgentConfiguration();
        check(!disabled.isEnabled() && disabled.getAgentCommandLine().isEmpty(), "disabled agent emits nothing");
        Path defaultFilter = AgentConfiguration.getDefaultAccessFilterPath(work.resolve("agent"));
        StandardAgentMode standard = new StandardAgentMode();
        AgentConfiguration configured = new AgentConfiguration(List.of("caller.json"), List.of("user.json"),
                true, false, true, false, true, standard, defaultFilter.toString());
        check(configured.isEnabled(), "standard mode enabled");
        check(configured.getAgentCommandLine().equals(List.of(
                "access-filter-file=" + defaultFilter,
                "config-output-dir={output_dir}", "caller-filter-file=caller.json",
                "access-filter-file=user.json", "builtin-caller-filter=true", "builtin-heuristic-filter=false",
                "experimental-class-define-support=true", "experimental-unsafe-allocation-support=false",
                "track-reflection-metadata=true")), "standard options and default/user precedence");
        check(!Files.exists(defaultFilter), "argument construction has no filter-file write");
        check(configured.getAgentFiles().equals(List.of(defaultFilter.toString(), "caller.json", "user.json")),
                "original caller-owned input file inventory");
        AgentConfiguration.writeDefaultAccessFilter(defaultFilter);
        try (var input = AgentConfiguration.class.getResourceAsStream("/access-filter.json")) {
            check(input != null && Arrays.equals(Files.readAllBytes(defaultFilter), input.readAllBytes()),
                    "packaged original default filter resource materializes exactly");
        }
        check(standard.getNativeImageConfigureOptions(List.of("in-a", "in-b"), List.of("out")).equals(
                List.of("generate", "--input-dir=in-a", "--input-dir=in-b", "--output-dir=out")),
                "standard native-image-configure arguments only");
        check(standard.getNativeImageConfigureOptions(List.of(), List.of("out")).isEmpty(),
                "no input does not produce configure arguments");
        DirectAgentMode direct = new DirectAgentMode(List.of("access-filter-file=direct.json", "config-output-dir={output_dir}"));
        AgentConfiguration directConfiguration = new AgentConfiguration(defaultFilter.toString(), direct);
        check(directConfiguration.getAgentCommandLine().equals(List.of("access-filter-file=" + defaultFilter,
                "access-filter-file=direct.json", "config-output-dir={output_dir}")), "direct options retain precedence");
        check(direct.getNativeImageConfigureOptions(List.of("in"), List.of("out")).isEmpty(), "direct mode has no merge command");
        ConditionalAgentMode conditional = new ConditionalAgentMode("user-code.json", "extra.json", true);
        check(conditional.getAgentCommandLine().equals(List.of("config-output-dir={output_dir}",
                "experimental-conditional-config-part")), "parallel conditional collection arguments");
        check(conditional.getNativeImageConfigureOptions(List.of("in"), List.of("out")).equals(List.of(
                "generate-conditional", "--user-code-filter=user-code.json", "--class-name-filter=extra.json",
                "--input-dir=in", "--output-dir=out")), "conditional metadata configuration arguments");
    }

    private static void metadataContract(Path work) throws Exception {
        Path repo = work.resolve("repository");
        schemas(repo);
        write(repo.resolve("com.contract/library/index.json"), """
                [
                  {"tested-versions":["1.0"],"metadata-version":"stable"},
                  {"tested-versions":["2.0"],"metadata-version":"recent","latest":true,"override":true},
                  {"metadata-version":"compat","default-for":"3\\\\..*"}
                ]
                """);
        for (String directory : List.of("stable", "recent", "compat")) {
            write(repo.resolve("com.contract/library/" + directory + "/reflect-config.json"),
                    "[{\"name\":\"contract." + directory + "\"}]");
            write(repo.resolve("com.contract/library/" + directory + "/index.json"), "[]");
        }
        write(repo.resolve("com.contract/ignored/index.json"), "[{\"not-for-native-image\":true}]");
        write(repo.resolve("com.contract/requester/index.json"),
                "[{\"tested-versions\":[\"1.0\"],\"metadata-version\":\"request\",\"requires\":[\"com.contract:required\"]}]");
        write(repo.resolve("com.contract/requester/request/reflect-config.json"), "[]");
        write(repo.resolve("com.contract/required/index.json"), """
                [{"tested-versions":["1.0"],"metadata-version":"r1"},
                 {"tested-versions":["2.0"],"metadata-version":"r2","latest":true}]
                """);
        write(repo.resolve("com.contract/required/r1/reflect-config.json"), "[]");
        write(repo.resolve("com.contract/required/r2/reflect-config.json"), "[]");
        GraalVMReachabilityMetadataRepository repository = new FileSystemRepository(repo);
        selected(repository.findConfigurationsFor("com.contract:library:1.0"), repo, "stable", "1.0", false);
        selected(repository.findConfigurationsFor("com.contract:library:2.0"), repo, "recent", "2.0", true);
        selected(repository.findConfigurationsFor("com.contract:library:3.7"), repo, "compat", "3.7", false);
        check(repository.findConfigurationsFor("com.contract:library:9.0").isEmpty(), "unknown version has no implicit latest fallback");
        Set<DirectoryConfiguration> fallback = repository.findConfigurationsFor(query -> {
            query.forArtifacts("com.contract:library:9.0");
            query.useLatestConfigWhenVersionIsUntested();
        });
        selected(fallback, repo, "recent", "9.0", true);
        selected(repository.findConfigurationsFor(query -> query.forArtifact(artifact -> {
            artifact.gav("com.contract:library:9.0");
            artifact.forceConfigVersion("1.0");
        })), repo, "stable", "1.0", false);
        check(repository.findConfigurationsFor(query -> query.forArtifact(artifact -> {
            artifact.gav("com.contract:library:9.0");
            artifact.forceConfigVersion("absent");
        })).isEmpty(), "forced absent version remains absent");
        check(repository.findConfigurationsFor(query -> {
            query.useLatestConfigWhenVersionIsUntested();
            query.forArtifact(artifact -> {
                artifact.gav("com.contract:library:9.0");
                artifact.doNotUseLatestConfigWhenVersionIsUntested();
            });
        }).isEmpty(), "per-artifact policy controls fallback");
        check(repository.findConfigurationsFor(List.of("com.contract:library:1.0", "com.contract:library:1.0")).size() == 1,
                "identical queries do not duplicate configurations");
        check(repository.findConfigurationsFor(List.of("com.contract:library:1.0", "com.contract:library:2.0")).size() == 2,
                "collection query retains distinct selected configurations");
        check(repository.isCoveredByRepository("com.contract:library:1.0"), "known version coverage");
        check(!repository.isCoveredByRepository("com.contract:absent:1.0"), "unknown module is uncovered");
        check(repository.findConfigurationsFor("com.contract:absent:1.0").isEmpty(), "unknown module yields no directory");
        check(repository.isCoveredByRepository("com.contract:ignored:1.0")
                && repository.findConfigurationsFor("com.contract:ignored:1.0").isEmpty(),
                "not-for-native-image marker is coverage without a configuration");
        Set<String> indirect = repository.findConfigurationsFor("com.contract:requester:1.0").stream()
                .map(value -> value.getArtifactId() + ":" + value.getVersion() + ":" + value.getDirectory().getFileName())
                .collect(Collectors.toSet());
        check(indirect.equals(Set.of("requester:1.0:request", "required:1.0:r1")), "single-level required metadata selection");
        Set<String> direct = repository.findConfigurationsFor(List.of("com.contract:requester:1.0", "com.contract:required:2.0"))
                .stream().map(value -> value.getArtifactId() + ":" + value.getVersion() + ":" + value.getDirectory().getFileName())
                .collect(Collectors.toSet());
        check(direct.equals(Set.of("requester:1.0:request", "required:2.0:r2")), "direct required-module version owns its selection");
        expect(UnsupportedOperationException.class, fallback::clear, "original result membership is immutable");
        expect(IllegalArgumentException.class, () -> repository.findConfigurationsFor("malformed"), "invalid coordinate refusal");
        write(repo.resolve("com.contract/broken/index.json"), "[invalid-json");
        expect(JSONException.class, () -> repository.findConfigurationsFor("com.contract:broken:1.0"), "invalid JSON refusal");
        expect(IllegalStateException.class, () -> new FileSystemRepository(work.resolve("missing-schemas")), "missing repository schema refusal");
        Path bad = work.resolve("unsupported-schemas");
        write(bad.resolve("schemas/library-and-framework-list-schema-v1.0.0.json"), "{}");
        write(bad.resolve("schemas/metadata-library-index-schema-v3.0.0.json"), "{}");
        expect(IllegalStateException.class, () -> new FileSystemRepository(bad), "unsupported schema-major refusal");
        Path copied = work.resolve("copied");
        DirectoryConfiguration.copy(fallback, copied);
        Path selectedOutput = copied.resolve("META-INF/native-image/com.contract/library/9.0");
        check(Files.readString(selectedOutput.resolve("reflect-config.json")).equals("[{\"name\":\"contract.recent\"}]"),
                "original configuration copy preserves selected metadata bytes");
        check(!Files.exists(selectedOutput.resolve("index.json")), "configuration copy excludes repository index");
        check(Files.readString(selectedOutput.resolve("reachability-metadata.properties")).equals("override=true\n"),
                "configuration copy retains override behavior");
    }

    private static void selected(Set<DirectoryConfiguration> result, Path repo, String directory, String version, boolean override) {
        check(result.size() == 1, "one selected configuration: " + version);
        DirectoryConfiguration value = result.iterator().next();
        check(value.getDirectory().equals(repo.resolve("com.contract/library/" + directory)), "selected directory: " + directory);
        check(value.getGroupId().equals("com.contract") && value.getArtifactId().equals("library")
                && value.getVersion().equals(version) && value.isOverride() == override, "selected identity/version/override: " + version);
    }

    private static void schemas(Path repo) throws Exception {
        write(repo.resolve("schemas/library-and-framework-list-schema-v1.0.0.json"), "{}");
        write(repo.resolve("schemas/metadata-library-index-schema-v2.0.0.json"), "{}");
    }

    private static void write(Path path, String data) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, data);
    }

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
        checks++;
    }

    private static void expect(Class<? extends Throwable> expected, Action action, String label) throws Exception {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) {
                checks++;
                return;
            }
            throw new AssertionError(label + ": unexpected " + failure, failure);
        }
        throw new AssertionError(label + ": accepted invalid input");
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    public static final class Bean {
        public String getName() {
            return "installed-api";
        }

        public boolean isEnabled() {
            return true;
        }
    }
}
