/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.donors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dependency-free positive, refusal and local-Git tests for the M3 donor catalogue. */
public final class DonorCatalogueSelfTest {
    private static int checks;

    private DonorCatalogueSelfTest() {
    }

    /** Runs all tests. */
    public static void main(String[] args) throws Exception {
        String configured = System.getProperty("m3.catalogue.dir");
        if (configured == null || configured.isBlank()) {
            throw new IllegalArgumentException("-Dm3.catalogue.dir=<module-root> is required");
        }
        Path root = Path.of(configured).toAbsolutePath().normalize();
        positiveCatalogue(root);
        refusalCatalogue(root);
        deltaFixture(root);
        System.out.println("M3_DONOR_CATALOGUE_SELF_TEST\tchecks=" + checks + "\tresult=PASS");
    }

    private static void positiveCatalogue(Path root) throws Exception {
        DonorCatalogue.Catalogue catalogue = DonorCatalogue.load(root);
        check(catalogue.donors().size() == 3, "three donor rows");
        check(catalogue.sources().size() == 9, "nine source rows");
        check(catalogue.capabilities().size() == 10, "ten capability rows");
        check(catalogue.importModes().size() == DonorCatalogue.ImportMode.values().length,
                "all import modes are declared");
        check(catalogue.semanticSha256().matches("[0-9a-f]{64}"), "semantic SHA-256");

        DonorCatalogue.Donor bisheng = catalogue.donors().get("bisheng-jdk-21");
        check(bisheng.family() == DonorCatalogue.DonorFamily.OPENJDK_DOWNSTREAM,
                "BiSheng family is OpenJDK downstream");
        check(bisheng.revision().equals("cd7d2e7cf826d33ea91ccd8cb1f6e81f4342fd36"),
                "BiSheng pin is exact");
        check(bisheng.upstreamRevision().equals("ba94eace682733f66e623c089bcafcf4a1b8e39c"),
                "OpenJDK release pin is exact");

        DonorCatalogue.Donor excelsior = catalogue.donors().get("excelsior-jet");
        check(excelsior.availability() == DonorCatalogue.Availability.DOCUMENTATION_ONLY,
                "Excelsior core is documentation-only");
        check(!excelsior.defaultImportMode().directSourceAllowed(),
                "Excelsior core default cannot carry source");

        Path first = Files.createTempFile("m3-donor-plan-first-", ".tsv");
        Path second = Files.createTempFile("m3-donor-plan-second-", ".tsv");
        DonorCatalogue.writePlan(catalogue, first);
        DonorCatalogue.writePlan(catalogue, second);
        check(Files.mismatch(first, second) == -1L, "plan generation is byte-identical");
        List<String> plan = Files.readAllLines(first, StandardCharsets.UTF_8);
        check(plan.get(0).equals(DonorCatalogue.PLAN_HEADER), "plan header is exact");
        check(plan.size() == 11, "plan contains one row per capability");
        check(plan.stream().filter(line -> line.contains("\tDIRECT_REVIEW\t")).count() == 2,
                "only two BiSheng capabilities enter direct review");
        check(plan.stream().filter(line -> line.contains("\tCLEAN_ROOM_SPEC\t")).count() == 8,
                "eight Excelsior capabilities require specification reimplementation");
        check(plan.stream().filter(line -> line.contains("\ttrue\tDIRECT_REVIEW\t")).count() == 2,
                "source-copy flag is true only for compatible BiSheng candidates");
        check(plan.stream().filter(line -> line.contains("\tfalse\tCLEAN_ROOM_SPEC\t")).count() == 8,
                "source-copy flag is false for Excelsior candidates");
    }

    private static void refusalCatalogue(Path root) throws Exception {
        mutateAndReject(root, "unsorted donors", temp -> {
            Path file = temp.resolve("catalogue/DONORS.tsv");
            List<String> lines = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
            String second = lines.get(1);
            lines.set(1, lines.get(2));
            lines.set(2, second);
            writeLines(file, lines);
        }, "strictly sorted");

        mutateAndReject(root, "mutable donor revision", temp -> replace(
                temp.resolve("catalogue/DONORS.tsv"),
                "cd7d2e7cf826d33ea91ccd8cb1f6e81f4342fd36",
                "master"), "immutable");

        mutateAndReject(root, "proprietary direct mode", temp -> replaceFirst(
                temp.resolve("catalogue/DONORS.tsv"),
                "\tSPEC_REIMPLEMENT\t-\t-\tDOCUMENTATION_ONLY\tREFERENCE_ONLY",
                "\tPATCH\t-\t-\tDOCUMENTATION_ONLY\tREFERENCE_ONLY"),
                "proprietary runtime cannot default");

        mutateAndReject(root, "GPL3 direct capability", temp -> replaceFirst(
                temp.resolve("catalogue/CAPABILITIES.tsv"),
                "excelsior-aot-native-image\texcelsior-jet\tAOT_NATIVE_IMAGE\texcelsior-api-readme\tSPEC_REIMPLEMENT",
                "excelsior-aot-native-image\texcelsior-jet\tAOT_NATIVE_IMAGE\texcelsior-api-readme\tPATCH"),
                "direct-source mode");

        mutateAndReject(root, "unknown evidence source", temp -> replaceFirst(
                temp.resolve("catalogue/CAPABILITIES.tsv"),
                "excelsior-api-readme\tSPEC_REIMPLEMENT",
                "missing-source\tSPEC_REIMPLEMENT"), "unknown evidence_source_id");

        mutateAndReject(root, "missing final LF", temp -> {
            Path file = temp.resolve("catalogue/SOURCES.tsv");
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Files.writeString(file, text.substring(0, text.length() - 1), StandardCharsets.UTF_8);
        }, "exactly one LF");

        mutateAndReject(root, "import-mode executable drift", temp -> replaceFirst(
                temp.resolve("contract/IMPORT_MODES.tsv"),
                "PATCH\ttrue\ttrue\tfalse",
                "PATCH\tfalse\ttrue\tfalse"), "disagrees with executable mode contract");

        mutateAndReject(root, "source path traversal", temp -> replaceFirst(
                temp.resolve("catalogue/SOURCES.tsv"),
                "src/hotspot/share/jprofilecache/jitProfileCache.hpp",
                "../jitProfileCache.hpp"), "traversal");

        mutateAndReject(root, "unauthorized cross-donor evidence", temp -> replaceFirst(
                temp.resolve("catalogue/CAPABILITIES.tsv"),
                "bisheng-jit-profile-cache\tbisheng-jdk-21\tJIT_PROFILE_PERSISTENCE\tbisheng-jit-profile-cache-header",
                "bisheng-jit-profile-cache\tbisheng-jdk-21\tJIT_PROFILE_PERSISTENCE\texcelsior-api-readme"),
                "cross-donor evidence");
    }

    private static void deltaFixture(Path catalogueRoot) throws Exception {
        Path repository = Files.createTempDirectory("m3-donor-delta-repo-");
        git(repository, "init", "-q");
        git(repository, "config", "user.name", "M3 Self Test");
        git(repository, "config", "user.email", "m3-self-test@example.invalid");
        Files.writeString(repository.resolve("Alpha.java"), "final class Alpha { int value() { return 1; } }\n",
                StandardCharsets.UTF_8);
        Files.writeString(repository.resolve("rename.txt"), "stable rename payload\n", StandardCharsets.UTF_8);
        Files.writeString(repository.resolve("delete.txt"), "delete me\n", StandardCharsets.UTF_8);
        git(repository, "add", "--", "Alpha.java", "rename.txt", "delete.txt");
        git(repository, "commit", "-q", "-m", "baseline");
        String upstream = git(repository, "rev-parse", "HEAD").strip();

        Files.writeString(repository.resolve("Alpha.java"), "final class Alpha { int value() { return 2; } }\n",
                StandardCharsets.UTF_8);
        git(repository, "mv", "rename.txt", "renamed.txt");
        Files.delete(repository.resolve("delete.txt"));
        Files.writeString(repository.resolve("Added.java"), "final class Added {}\n", StandardCharsets.UTF_8);
        git(repository, "add", "-A");
        git(repository, "commit", "-q", "-m", "vendor delta");
        String donorRevision = git(repository, "rev-parse", "HEAD").strip();

        DonorCatalogue.Donor fixtureDonor = new DonorCatalogue.Donor(
                "fixture-openjdk-downstream",
                "Fixture OpenJDK downstream",
                DonorCatalogue.DonorFamily.OPENJDK_DOWNSTREAM,
                "https://example.invalid/fixture",
                donorRevision,
                "GPL-2.0-only-with-classpath-exception",
                "GPL-2.0-only-with-classpath-exception",
                DonorCatalogue.ImportMode.PATCH,
                "https://example.invalid/upstream",
                upstream,
                DonorCatalogue.Availability.SOURCE_AVAILABLE,
                DonorCatalogue.DonorStatus.PINNED);

        Path first = Files.createTempDirectory("m3-donor-delta-first-");
        Path second = Files.createTempDirectory("m3-donor-delta-second-");
        OpenJdkDeltaInventory.Result result = OpenJdkDeltaInventory.generate(fixtureDonor, repository, first);
        OpenJdkDeltaInventory.generate(fixtureDonor, repository, second);
        check(result.mergeBase().equals(upstream), "delta merge base equals baseline commit");
        check(result.rows().size() == 4, "delta contains add modify delete and rename rows");
        Set<String> statuses = new HashSet<>();
        result.rows().forEach(row -> statuses.add(row.status()));
        check(statuses.equals(Set.of("A", "D", "M", "R")), "delta statuses are exact");
        check(result.rows().stream().anyMatch(row -> row.status().equals("R")
                        && row.oldPath().equals("rename.txt") && row.newPath().equals("renamed.txt")
                        && row.score().equals("100")),
                "rename identity and score are retained");
        check(Files.mismatch(first.resolve("DELTA.tsv"), second.resolve("DELTA.tsv")) == -1L,
                "delta output is byte-identical");
        check(Files.mismatch(first.resolve("RECEIPT.tsv"), second.resolve("RECEIPT.tsv")) == -1L,
                "delta receipt is byte-identical");
        check(result.deltaSha256().matches("[0-9a-f]{64}"), "delta receipt carries SHA-256");

        DonorCatalogue.Donor wrongFamily = new DonorCatalogue.Donor(
                "fixture-proprietary",
                "Fixture proprietary",
                DonorCatalogue.DonorFamily.PROPRIETARY_RUNTIME,
                "-", "-", "PROPRIETARY", "GPL-2.0-only-with-classpath-exception",
                DonorCatalogue.ImportMode.SPEC_REIMPLEMENT, "-", "-",
                DonorCatalogue.Availability.DOCUMENTATION_ONLY,
                DonorCatalogue.DonorStatus.REFERENCE_ONLY);
        expectFailure(() -> OpenJdkDeltaInventory.generate(wrongFamily, repository, first),
                "not an OPENJDK_DOWNSTREAM", "non-OpenJDK delta refusal");

        DonorCatalogue.Donor missingRevision = new DonorCatalogue.Donor(
                fixtureDonor.donorId(), fixtureDonor.projectName(), fixtureDonor.family(), fixtureDonor.repositoryUrl(),
                "0000000000000000000000000000000000000000", fixtureDonor.sourceLicense(),
                fixtureDonor.targetLicense(), fixtureDonor.defaultImportMode(), fixtureDonor.upstreamRepository(),
                fixtureDonor.upstreamRevision(), fixtureDonor.availability(), fixtureDonor.status());
        expectFailure(() -> OpenJdkDeltaInventory.generate(missingRevision, repository, first),
                "command failed", "missing donor commit refusal");

        DonorCatalogue.Catalogue catalogue = DonorCatalogue.load(catalogueRoot);
        check(catalogue.donors().containsKey("bisheng-jdk-21"), "fixture did not mutate canonical catalogue");
    }

    private static void mutateAndReject(
            Path root, String name, Mutation mutation, String expectedMessage) throws Exception {
        Path temp = Files.createTempDirectory("m3-donor-refusal-");
        copyTree(root, temp);
        mutation.apply(temp);
        expectFailure(() -> DonorCatalogue.load(temp), expectedMessage, name);
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.sorted().toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void writeLines(Path path, List<String> lines) throws IOException {
        Files.writeString(path, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private static void replace(Path path, String before, String after) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (!text.contains(before)) {
            throw new AssertionError("mutation token not found: " + before);
        }
        Files.writeString(path, text.replace(before, after), StandardCharsets.UTF_8);
    }

    private static void replaceFirst(Path path, String before, String after) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        int index = text.indexOf(before);
        if (index < 0) {
            throw new AssertionError("mutation token not found: " + before);
        }
        String changed = text.substring(0, index) + after + text.substring(index + before.length());
        Files.writeString(path, changed, StandardCharsets.UTF_8);
    }

    private static String git(Path repository, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(repository.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        byte[] output = process.getInputStream().readAllBytes();
        int exit = process.waitFor();
        String text = new String(output, StandardCharsets.UTF_8);
        if (exit != 0) {
            throw new IOException("git command failed: " + String.join(" ", command) + "\n" + text);
        }
        return text;
    }

    private static void expectFailure(ThrowingRunnable runnable, String expectedMessage, String name) throws Exception {
        try {
            runnable.run();
            throw new AssertionError(name + " did not fail");
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            String message = String.valueOf(exception.getMessage());
            if (!message.contains(expectedMessage)) {
                throw new AssertionError(name + " failed with unexpected message: " + message, exception);
            }
            checks++;
        }
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError(name);
        }
        checks++;
    }

    @FunctionalInterface
    private interface Mutation {
        void apply(Path root) throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
