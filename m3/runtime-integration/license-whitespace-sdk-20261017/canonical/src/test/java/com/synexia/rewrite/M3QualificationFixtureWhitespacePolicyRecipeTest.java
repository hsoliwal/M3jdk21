/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** Bounded source-tooling proof; preserves donor bytes and does not run a JDK build. */
final class M3QualificationFixtureWhitespacePolicyRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/";
    private static final String ATTRIBUTES = ".gitattributes";
    private static final String CUSTODY = ".github/workflows/m3-full-borrow-string-phase.yml";
    private static final String RECEIVER = ".github/workflows/m3-synexia-full-borrow-receiver.yml";
    private static final List<String> LICENSES = List.of(
            "m3/runtime-integration/string-successor-sdk-20261017/canonical/licenses/ADDITIONAL_LICENSE_INFO",
            "m3/runtime-integration/utf8-cursor-20261014/recipe/canonical/licenses/OpenJDK-ADDITIONAL_LICENSE_INFO",
            "m3/runtime-integration/license-whitespace-sdk-20261017/canonical/licenses/ADDITIONAL_LICENSE_INFO",
            "m3/runtime-integration/license-whitespace-sdk-20261017/canonical/src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/ADDITIONAL_LICENSE_INFO");
    private static final List<String> CHANGED = List.of(ATTRIBUTES, CUSTODY, RECEIVER);
    private static final String RAW_LICENSE_SHA =
            "a69bce275ba7a3570af6579cb0f55682cd75fedfcd49e0e8e9022270c447c916";
    private static final String SEALS =
            "          python3 -I -S -B m3/runtime-integration/string-successor-sdk-20261017/canonical/verify.py\n" +
            "          python3 -I -S -B m3/runtime-integration/utf8-cursor-20261014/recipe/canonical/verify.py\n";
    private record Applied(Map<String, String> files, int changes) { }
    private record Probe(int exit, String output) { }

    private static String contents(String name) throws IOException {
        try (var stream = M3QualificationFixtureWhitespacePolicyRecipeTest.class
                .getResourceAsStream(ROOT + name)) {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static M3GpuToolJoinManifest manifest() {
        return M3GpuToolJoinManifest.load(ROOT,
                M3QualificationFixtureWhitespacePolicyRecipeTest.class::getResourceAsStream,
                M3GpuToolJoinManifest.NOOP);
    }
    private static String beforeResource(String path) {
        return switch (path) {
            case ATTRIBUTES -> "before/gitattributes.txt";
            case CUSTODY -> "before/m3-full-borrow-string-phase.yml";
            case RECEIVER -> "before/m3-synexia-full-borrow-receiver.yml";
            default -> {
                assertTrue(LICENSES.contains(path), path);
                yield "before/ADDITIONAL_LICENSE_INFO";
            }
        };
    }
    private static Map<String, String> before() throws IOException {
        var input = new LinkedHashMap<String, String>();
        for (var target : manifest().targets()) {
            input.put(target.path(), contents(beforeResource(target.path())));
        }
        input.put("opaque/Unrelated.txt", "unrelated opaque owner remains unchanged\n");
        return input;
    }
    private static List<SourceFile> sources(Map<String, String> input) {
        var result = new ArrayList<SourceFile>();
        input.forEach((path, text) -> result.add(
                PlainText.builder().sourcePath(Path.of(path)).text(text).build()));
        return result;
    }
    private static Recipe namedRecipe() {
        Recipe recipe = Environment.builder().scanRuntimeClasspath("com.synexia.rewrite")
                .build().activateRecipes("com.synexia.m3.QualificationFixtureWhitespacePolicy");
        assertEquals(1, recipe.getRecipeList().size());
        var activated = recipe.getRecipeList().get(0);
        assertEquals("com.synexia.m3.QualificationFixtureWhitespacePolicy", activated.getName());
        assertEquals(List.of(M3QualificationFixtureWhitespacePolicyRecipe.class),
                activated.getRecipeList().stream().map(Object::getClass).toList());
        return recipe;
    }
    private static Applied apply(Map<String, String> input) {
        var errors = new ArrayList<Throwable>();
        var run = namedRecipe().run(new InMemoryLargeSourceSet(sources(input)),
                new InMemoryExecutionContext(errors::add));
        assertTrue(errors.isEmpty(), errors.toString());
        var output = new LinkedHashMap<>(input);
        var changes = run.getChangeset().getAllResults();
        for (var change : changes) {
            assertNotNull(change.getAfter());
            String path = change.getAfter().getSourcePath().toString().replace('\\', '/');
            assertTrue(CHANGED.contains(path), "only the three policy owners can change: " + path);
            output.put(path, change.getAfter().printAll());
        }
        return new Applied(output, changes.size());
    }

    @Test void allPartialStatesConvergeInBothOrdersToExactFixedPoint() throws Exception {
        var targets = manifest().targets().stream().toList();
        assertEquals(List.of(ATTRIBUTES, CUSTODY, RECEIVER, LICENSES.get(0), LICENSES.get(1),
                LICENSES.get(2), LICENSES.get(3)),
                targets.stream().map(M3GpuToolJoinManifest.Target::path).toList());
        for (var target : targets) {
            assertFalse(target.addition());
            assertFalse(target.removal());
            assertEquals(LICENSES.contains(target.path()), target.before().equals(target.after()));
            if (LICENSES.contains(target.path())) {
                assertEquals(RAW_LICENSE_SHA, target.before());
                assertEquals(null, target.text(), "KEEP rows must not carry replacement text");
            }
        }
        for (int state = 0; state < 8; state++) {
            var input = before();
            for (int i = 0; i < CHANGED.size(); i++) {
                if ((state & (1 << i)) != 0) {
                    input.put(CHANGED.get(i), manifest().target(CHANGED.get(i)).text());
                }
            }
            for (boolean reverse : new boolean[]{false, true}) {
                var ordered = new LinkedHashMap<String, String>();
                var paths = new ArrayList<>(input.keySet());
                if (reverse) Collections.reverse(paths);
                for (String path : paths) ordered.put(path, input.get(path));
                var result = apply(ordered);
                assertEquals(3 - Integer.bitCount(state), result.changes());
                assertEquals(input.size(), result.files().size());
                for (var target : targets) {
                    assertEquals(target.after(),
                            M3GpuToolJoinManifest.sha256(result.files().get(target.path())));
                }
                for (String path : LICENSES) {
                    assertEquals(contents("before/ADDITIONAL_LICENSE_INFO"), result.files().get(path));
                }
                assertEquals(input.get("opaque/Unrelated.txt"), result.files().get("opaque/Unrelated.txt"));
                assertEquals(0, apply(result.files()).changes());
                assertPolicyPreservation(result.files());
                if (state == 0 && !reverse) {
                    for (String path : CHANGED) retain("policy/" + path, result.files().get(path));
                }
            }
        }
    }
    private static void assertPolicyPreservation(Map<String, String> files) throws IOException {
        String addition =
                "\n# Preserve exact OpenJDK donor-license fixture bytes; SDK input seals remain mandatory.\n" +
                LICENSES.stream().map(path -> "/" + path + "\twhitespace=-blank-at-eol\n")
                        .collect(Collectors.joining());
        assertEquals(contents("before/gitattributes.txt") + addition, files.get(ATTRIBUTES));
        assertEquals(4L, files.get(ATTRIBUTES).lines()
                .filter(line -> line.endsWith("\twhitespace=-blank-at-eol")).count());
        for (String workflow : List.of(CUSTODY, RECEIVER)) {
            String after = files.get(workflow);
            assertEquals(contents(beforeResource(workflow)), after.replace(SEALS, ""));
            assertEquals(1, after.split(java.util.regex.Pattern.quote(SEALS), -1).length - 1);
            String anchor = "          set -euo pipefail\n" + SEALS + "          base=";
            assertTrue(after.contains(anchor), "raw seals must run before the existing diff command");
            assertEquals(contents(beforeResource(workflow)).lines()
                    .filter(line -> line.contains("git diff --check")).toList(),
                    after.lines().filter(line -> line.contains("git diff --check")).toList());
            assertFalse(SEALS.contains("--reports"));
            assertFalse(SEALS.contains("--self-test"));
            assertFalse(SEALS.contains("continue-on-error"));
        }
    }

    @Test void driftMissingAndDuplicateTargetsRefuseIncludingKeptLicenses() throws Exception {
        for (var target : manifest().targets()) {
            var input = before();
            input.put(target.path(), input.get(target.path()) + "foreign owner change\n");
            reject(sources(input), "source drift: " + target.path());
            input = before();
            input.remove(target.path());
            reject(sources(input), "required source missing: " + target.path());
            var duplicate = sources(before());
            duplicate.add(PlainText.builder().sourcePath(Path.of(target.path()))
                    .text(before().get(target.path())).build());
            reject(duplicate, "duplicate source target: " + target.path());
        }
    }
    private static void reject(List<SourceFile> input, String signature) {
        var errors = new ArrayList<Throwable>();
        try {
            var run = namedRecipe().run(new InMemoryLargeSourceSet(input),
                    new InMemoryExecutionContext(errors::add));
            assertTrue(run.getChangeset().getAllResults().isEmpty());
        } catch (IllegalStateException refusal) {
            errors.add(refusal);
        }
        StringBuilder observed = new StringBuilder();
        for (Throwable error : errors) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                observed.append(cause).append('\n');
            }
        }
        assertTrue(observed.toString().contains(signature), observed.toString());
    }

    @Test void realGitAdmitsOnlyExactCategoryAtFourLicensePaths(@TempDir Path temporary)
            throws Exception {
        Path repository = temporary.resolve("git-fixture");
        Files.createDirectory(repository);
        for (var entry : before().entrySet()) write(repository, entry.getKey(), entry.getValue());
        assertEquals(0, git(repository, "init", "--quiet").exit());
        assertEquals(0, git(repository, "add", "--intent-to-add", "--", ".").exit());
        var table = new StringBuilder("case\texit\ttrailing\tspaceBeforeTab\tblankAtEof\n");
        Probe baseline = gitCase(repository, "baseline", 2, 112, 0, 0, table);
        for (String path : LICENSES) assertEquals(28L, diagnosticCount(baseline.output(), path, "trailing whitespace."));
        Map<String, String> candidate = apply(before()).files();
        for (String path : CHANGED) write(repository, path, candidate.get(path));
        gitCase(repository, "candidate", 0, 0, 0, 0, table);
        String unrelated = "other/unrelated-trailing.txt";
        write(repository, unrelated, "nonexempt trailing space \n");
        assertEquals(0, git(repository, "add", "--intent-to-add", "--", unrelated).exit());
        gitCase(repository, "unrelated-trailing", 2, 1, 0, 0, table);
        write(repository, unrelated, "clean\n");
        for (int i = 0; i < LICENSES.size(); i++) {
            String nested = "nested/" + LICENSES.get(i);
            write(repository, nested, contents("before/ADDITIONAL_LICENSE_INFO"));
            assertEquals(0, git(repository, "add", "--intent-to-add", "--", nested).exit());
            Probe nestedProbe = gitCase(repository, "nested-root-path-" + i, 2, 28, 0, 0, table);
            assertEquals(28L, diagnosticCount(nestedProbe.output(), nested, "trailing whitespace."));
            write(repository, nested, "clean\n");
            String suffixed = LICENSES.get(i) + ".copy";
            write(repository, suffixed, contents("before/ADDITIONAL_LICENSE_INFO"));
            assertEquals(0, git(repository, "add", "--intent-to-add", "--", suffixed).exit());
            Probe suffixProbe = gitCase(repository, "suffixed-license-path-" + i, 2, 28, 0, 0, table);
            assertEquals(28L, diagnosticCount(suffixProbe.output(), suffixed, "trailing whitespace."));
            write(repository, suffixed, "clean\n");
        }
        for (int i = 0; i < LICENSES.size(); i++) {
            String path = LICENSES.get(i);
            String original = candidate.get(path);
            write(repository, path, original + " \tretained whitespace category\n");
            Probe tabs = gitCase(repository, "space-before-tab-" + i, 2, 0, 1, 0, table);
            assertEquals(1L, diagnosticCount(tabs.output(), path, "space before tab in indent."));
            write(repository, path, original + "\n");
            Probe eof = gitCase(repository, "blank-at-eof-" + i, 2, 0, 0, 1, table);
            assertEquals(1L, diagnosticCount(eof.output(), path, "new blank line at EOF."));
            write(repository, path, original);
        }
        for (String path : LICENSES) {
            assertEquals(RAW_LICENSE_SHA, M3GpuToolJoinManifest.sha256(Files.readString(repository.resolve(path))));
        }
        assertEquals(0, git(repository, "diff", "--check").exit());
        retain("logs/git-qualification.tsv", table.toString());
    }
    private static Probe gitCase(Path repository, String name, int exit, long trailing,
            long tabs, long eof, StringBuilder table) throws Exception {
        Probe probe = git(repository, "diff", "--check");
        assertEquals(exit, probe.exit(), probe.output());
        assertEquals(trailing, categoryCount(probe.output(), "trailing whitespace."), probe.output());
        assertEquals(tabs, categoryCount(probe.output(), "space before tab in indent."), probe.output());
        assertEquals(eof, categoryCount(probe.output(), "new blank line at EOF."), probe.output());
        assertEquals(trailing + tabs + eof, probe.output().lines()
                .filter(line -> line.matches(".+:[0-9]+: .+")).count(), probe.output());
        table.append(name).append('\t').append(probe.exit()).append('\t').append(trailing)
                .append('\t').append(tabs).append('\t').append(eof).append('\n');
        retain("logs/git/" + name + ".log", probe.output());
        return probe;
    }
    private static long categoryCount(String output, String message) {
        return output.lines().filter(line -> line.matches(".+:[0-9]+: " +
                java.util.regex.Pattern.quote(message))).count();
    }
    private static long diagnosticCount(String output, String path, String message) {
        return output.lines().filter(line -> line.matches(java.util.regex.Pattern.quote(path) +
                ":[0-9]+: " + java.util.regex.Pattern.quote(message))).count();
    }
    private static Probe git(Path directory, String... arguments) throws Exception {
        var command = new ArrayList<>(List.of(System.getProperty("m3.test.git", "git"),
                "-c", "core.attributesFile=/dev/null", "-c", "core.autocrlf=false",
                "-c", "core.eol=lf", "-c", "core.whitespace=blank-at-eol,blank-at-eof,space-before-tab"));
        Collections.addAll(command, arguments);
        var builder = new ProcessBuilder(command).directory(directory.toFile());
        builder.environment().keySet().removeIf(name -> name.startsWith("GIT_"));
        builder.environment().put("GIT_CONFIG_NOSYSTEM", "1");
        builder.environment().put("GIT_CONFIG_GLOBAL", "/dev/null");
        builder.environment().put("GIT_ATTR_NOSYSTEM", "1");
        return run(builder, directory.getParent().resolve("git-command.log"));
    }

    @Test void rawByteGuardsRejectTrimExtraMissingAndChangedLicense(@TempDir Path temporary)
            throws Exception {
        Path repository = temporary.resolve("raw-fixture");
        Files.createDirectory(repository);
        Map<String, String> candidate = apply(before()).files();
        for (var target : manifest().targets()) write(repository, target.path(), candidate.get(target.path()));
        writeLicenseSiblings(repository);
        var table = new StringBuilder("case\texit\n");
        Probe admitted = rawGuard(repository, temporary.resolve("raw-command.log"));
        assertEquals(0, admitted.exit(), admitted.output());
        table.append("candidate\t0\n");
        for (int i = 0; i < LICENSES.size(); i++) {
            String path = LICENSES.get(i);
            String original = candidate.get(path);
            String trimmed = original.lines().map(String::stripTrailing)
                    .collect(Collectors.joining("\n", "", "\n"));
            assertFalse(original.equals(trimmed));
            write(repository, path, trimmed);
            rawRefusal(repository, temporary, "trim-" + i, "receiving immutable license drift: " + path, table);
            write(repository, path, original + "\n");
            rawRefusal(repository, temporary, "extra-bytes-" + i, "receiving immutable license drift: " + path, table);
            Files.delete(repository.resolve(path));
            rawRefusal(repository, temporary, "missing-" + i, "receiving immutable license missing: " + path, table);
            write(repository, path, "X" + original.substring(1));
            rawRefusal(repository, temporary, "changed-" + i, "receiving immutable license drift: " + path, table);
            write(repository, path, original);
            if (i < 2) {
                String directory = path.substring(0, path.lastIndexOf('/'));
                String extra = directory + "/UNSEALED_LICENSE.txt";
                write(repository, extra, "unsealed extra license\n");
                rawRefusal(repository, temporary, "extra-file-" + i,
                        "receiving immutable license inventory drift: " + directory, table);
                Files.delete(repository.resolve(extra));
            }
        }
        assertEquals(0, rawGuard(repository, temporary.resolve("raw-command.log")).exit());
        retain("logs/raw-byte-guards.tsv", table.toString());
    }
    private static void writeLicenseSiblings(Path repository) throws IOException {
        Path sdk = Path.of(System.getProperty("m3.fixture.root"));
        String first = LICENSES.get(0).substring(0, LICENSES.get(0).lastIndexOf('/'));
        String second = LICENSES.get(1).substring(0, LICENSES.get(1).lastIndexOf('/'));
        for (String name : List.of("ASSEMBLY_EXCEPTION", "GPL-2.0.txt")) {
            Files.copy(sdk.resolve("licenses/" + name), repository.resolve(first + "/" + name));
        }
        Files.copy(sdk.resolve("licenses/GPL-2.0.txt"), repository.resolve(second + "/OpenJDK-LICENSE"));
    }
    private static void rawRefusal(Path repository, Path temporary, String name, String signature,
            StringBuilder table) throws Exception {
        Probe refused = rawGuard(repository, temporary.resolve("raw-command.log"));
        assertEquals(1, refused.exit(), refused.output());
        assertTrue(refused.output().contains(signature), refused.output());
        table.append(name).append('\t').append(refused.exit()).append('\n');
    }
    private static Probe rawGuard(Path repository, Path log) throws Exception {
        Path verifier = Path.of(System.getProperty("m3.fixture.root")).resolve("verify.py").toAbsolutePath();
        return run(new ProcessBuilder(System.getProperty("m3.test.python", "python3"),
                "-I", "-S", "-B", verifier.toString(), "--repository", repository.toString()), log);
    }
    private static Probe run(ProcessBuilder builder, Path log) throws Exception {
        var process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            assertTrue(process.waitFor(5, TimeUnit.SECONDS), "owned fixture process did not terminate");
            throw new AssertionError("bounded fixture process timed out");
        }
        return new Probe(process.exitValue(), Files.readString(log, StandardCharsets.UTF_8));
    }
    private static void write(Path root, String name, String text) throws IOException {
        Path output = root.resolve(name);
        Files.createDirectories(output.getParent());
        Files.writeString(output, text, StandardCharsets.UTF_8);
    }
    private static void retain(String path, String content) throws IOException {
        write(Path.of(System.getProperty("m3.fixture.root"))
                .resolve("target/emitted/license-whitespace"), path, content);
    }
}
