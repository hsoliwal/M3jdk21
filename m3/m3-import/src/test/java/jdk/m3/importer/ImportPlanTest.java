// SPDX-License-Identifier: Apache-2.0
package jdk.m3.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ImportPlanTest {
    @TempDir
    Path temp;

    @Test
    void plansFullDeltaAndStagesOnlyChangedCandidates() throws Exception {
        Fixture fixture = fixture();
        ImportPlan.Plan plan =
                ImportPlan.plan(fixture.m3jdk(), fixture.manifest());
        Map<String, ImportPlan.Row> rows =
                plan.rows().stream()
                        .collect(Collectors.toMap(ImportPlan.Row::targetPath, Function.identity()));

        assertEquals(5, rows.size());
        assertEquals(
                ImportPlan.Action.KEEP,
                rows.get(fixture.keepTarget()).action());
        assertEquals(
                ImportPlan.Lane.OPENREWRITE_RECIPE,
                rows.get(fixture.keepTarget()).lane());
        assertEquals(
                ImportPlan.Action.REPLACE,
                rows.get(fixture.replaceTarget()).action());
        assertEquals(
                ImportPlan.Lane.CONVERGENCE_JAVA,
                rows.get(fixture.replaceTarget()).lane());
        assertEquals(
                ImportPlan.Action.ADD,
                rows.get(fixture.addTarget()).action());
        assertEquals(
                ImportPlan.Lane.CONVERGENCE_NATIVE,
                rows.get(fixture.addTarget()).lane());
        assertEquals(
                ImportPlan.Lane.OPENREWRITE_RESOURCE,
                rows.get(fixture.resourceTarget()).lane());
        assertEquals(
                ImportPlan.Action.STALE,
                rows.get(fixture.staleTarget()).action());
        assertEquals(3, plan.changedCount());
        assertEquals(1, plan.staleCount());
        assertFalse(plan.sourceWriteAuthority());
        assertFalse(plan.promotionAuthority());
        assertEquals(64, plan.root().length());
        assertTrue(plan.toTsv().startsWith(ImportPlan.HEADER + "\n"));
        assertTrue(plan.toTsv().endsWith("# root\t" + plan.root() + "\n"));

        byte[] oldReplace = Files.readAllBytes(fixture.m3jdk().resolve(fixture.replaceTarget()));
        Path out = Path.of("m3/build/m3-import/full");
        ImportPlan.Plan staged =
                ImportPlan.stage(
                        fixture.m3(), fixture.m3jdk(), fixture.manifest(), out);
        assertEquals(plan, staged);

        Path stagedRoot = fixture.m3jdk().resolve(out);
        assertTrue(Files.isRegularFile(stagedRoot.resolve("PLAN.tsv")));
        assertTrue(Files.isRegularFile(stagedRoot.resolve("ROOT")));
        assertFalse(
                Files.exists(stagedRoot.resolve("candidate").resolve(fixture.keepTarget())));
        assertEquals(
                sha256(Files.readAllBytes(fixture.m3().resolve(fixture.replaceSource()))),
                sha256(Files.readAllBytes(
                        stagedRoot.resolve("candidate").resolve(fixture.replaceTarget()))));
        assertEquals(
                sha256(Files.readAllBytes(fixture.m3().resolve(fixture.addSource()))),
                sha256(Files.readAllBytes(
                        stagedRoot.resolve("candidate").resolve(fixture.addTarget()))));
        assertEquals(
                sha256(Files.readAllBytes(fixture.m3().resolve(fixture.resourceSource()))),
                sha256(Files.readAllBytes(
                        stagedRoot.resolve("candidate").resolve(fixture.resourceTarget()))));

        assertEquals(
                sha256(oldReplace),
                sha256(Files.readAllBytes(fixture.m3jdk().resolve(fixture.replaceTarget()))));
        assertTrue(Files.isRegularFile(fixture.m3jdk().resolve(fixture.staleTarget())));

        assertEquals(
                staged,
                ImportPlan.stage(
                        fixture.m3(), fixture.m3jdk(), fixture.manifest(), out));
    }

    @Test
    void sourceOnlyVerifierAllowsReplacementButRefusesSourceDrift() throws Exception {
        Fixture fixture = fixture();
        Importer.verifySources(fixture.m3(), fixture.manifest());

        assertThrows(
                IllegalStateException.class,
                () -> Importer.verify(
                        fixture.m3(), fixture.m3jdk(), fixture.manifest()));

        Files.writeString(
                fixture.m3().resolve(fixture.addSource()),
                "// SPDX-License-Identifier: Apache-2.0\nchanged\n");
        assertThrows(
                IllegalStateException.class,
                () -> Importer.verifySources(fixture.m3(), fixture.manifest()));
        assertThrows(
                IllegalStateException.class,
                () -> ImportPlan.stage(
                        fixture.m3(),
                        fixture.m3jdk(),
                        fixture.manifest(),
                        Path.of("m3/build/m3-import/drift")));
    }

    @Test
    void writePlanIsCreateOrVerifyAndRefusesOutsideBuildAndDrift() throws Exception {
        Fixture fixture = fixture();
        ImportPlan.Plan plan =
                ImportPlan.plan(fixture.m3jdk(), fixture.manifest());

        Path relative = Path.of("m3/build/m3-import/plan-only");
        ImportPlan.writePlan(fixture.m3jdk(), relative, plan);
        ImportPlan.writePlan(fixture.m3jdk(), relative, plan);
        Path planFile = fixture.m3jdk().resolve(relative).resolve("PLAN.tsv");
        assertEquals(plan.toTsv(), Files.readString(planFile));

        Files.writeString(planFile, "drift\n");
        assertThrows(
                IllegalStateException.class,
                () -> ImportPlan.writePlan(fixture.m3jdk(), relative, plan));

        assertThrows(
                IllegalArgumentException.class,
                () -> ImportPlan.writePlan(
                        fixture.m3jdk(), Path.of("m3/build"), plan));
        assertThrows(
                IllegalArgumentException.class,
                () -> ImportPlan.writePlan(
                        fixture.m3jdk(), Path.of("outside"), plan));
        assertThrows(
                IllegalArgumentException.class,
                () -> ImportPlan.writePlan(
                        fixture.m3jdk(), temp.resolve("absolute-outside"), plan));

        Path build = fixture.m3jdk().resolve("m3/build");
        Files.createDirectories(build);
        Path outside = temp.resolve("symlink-outside");
        Files.createDirectories(outside);
        Path link = build.resolve("link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
            Assumptions.assumeTrue(false, "symbolic links unavailable");
        }
        assertThrows(
                IOException.class,
                () -> ImportPlan.writePlan(
                        fixture.m3jdk(), Path.of("m3/build/link/run"), plan));
    }

    @Test
    void laneClassificationCoversEveryRegisteredDeliveryCategory() {
        String target = "m3/vendor/m3/other/file.txt";
        assertEquals(
                ImportPlan.Lane.OPENREWRITE_RECIPE,
                ImportPlan.lane("openrewrite-java", target));
        assertEquals(
                ImportPlan.Lane.OPENREWRITE_TEST,
                ImportPlan.lane("openrewrite-tests", target));
        assertEquals(
                ImportPlan.Lane.OPENREWRITE_RESOURCE,
                ImportPlan.lane("openrewrite-resources", target));
        assertEquals(
                ImportPlan.Lane.CONVERGENCE_JAVA,
                ImportPlan.lane("convergence-java", target));
        assertEquals(
                ImportPlan.Lane.CONVERGENCE_TEST,
                ImportPlan.lane("convergence-tests", target));
        assertEquals(
                ImportPlan.Lane.CONVERGENCE_NATIVE,
                ImportPlan.lane("convergence-native", target));
        assertEquals(
                ImportPlan.Lane.M3_CLONER,
                ImportPlan.lane("m3-cloner-java", target));
        assertEquals(
                ImportPlan.Lane.M3_CLONER,
                ImportPlan.lane("m3-cloner-tests", target));
        assertEquals(
                ImportPlan.Lane.M3INDEX,
                ImportPlan.lane("m3index-source", target));
        assertEquals(
                ImportPlan.Lane.M3_RECIPE,
                ImportPlan.lane("m3-recipe-source", target));
        assertEquals(
                ImportPlan.Lane.OTHER_APACHE,
                ImportPlan.lane("other", target));

        assertEquals(
                ImportPlan.Lane.OPENREWRITE_RECIPE,
                ImportPlan.lane(
                        "stale",
                        "m3/vendor/m3/m3-openrewrite-recipes/src/main/java/p/R.java"));
        assertEquals(
                ImportPlan.Lane.OPENREWRITE_TEST,
                ImportPlan.lane(
                        "stale",
                        "m3/vendor/m3/m3-openrewrite-recipes/src/test/java/p/RTest.java"));
        assertEquals(
                ImportPlan.Lane.CONVERGENCE_TEST,
                ImportPlan.lane(
                        "stale",
                        "m3/vendor/m3/m3-code-convergence/src/test/java/p/Test.java"));
        assertEquals(
                ImportPlan.Lane.M3INDEX,
                ImportPlan.lane(
                        "stale",
                        "m3/vendor/m3/m3-m3index/src/main/java/p/A.java"));
        assertEquals(
                ImportPlan.Lane.M3_RECIPE,
                ImportPlan.lane(
                        "stale",
                        "m3/vendor/m3/m3-recipe/src/main/java/p/A.java"));
    }

    @Test
    void rowAndPlanValidationFailClosed() {
        ImportPlan.Row valid =
                new ImportPlan.Row(
                        "openrewrite-java",
                        "src/A.java",
                        "m3/vendor/m3/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "ABSENT",
                        ImportPlan.Action.ADD,
                        ImportPlan.Lane.OPENREWRITE_RECIPE);

        assertThrows(
                IllegalArgumentException.class,
                () -> new ImportPlan.Row(
                        "x",
                        "src/A.java",
                        "outside/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "ABSENT",
                        ImportPlan.Action.ADD,
                        ImportPlan.Lane.OTHER_APACHE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ImportPlan.Row(
                        "x",
                        "src/A.java",
                        "m3/vendor/m3/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "2".repeat(64),
                        ImportPlan.Action.ADD,
                        ImportPlan.Lane.OTHER_APACHE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ImportPlan.Row(
                        "stale",
                        "src/A.java",
                        "m3/vendor/m3/A.java",
                        "STALE",
                        "STALE",
                        "2".repeat(64),
                        ImportPlan.Action.STALE,
                        ImportPlan.Lane.OTHER_APACHE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ImportPlan.Row(
                        "openrewrite-java",
                        "src/A.java",
                        "m3/vendor/m3/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "2".repeat(64),
                        ImportPlan.Action.KEEP,
                        ImportPlan.Lane.OPENREWRITE_RECIPE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ImportPlan.Row(
                        "openrewrite-java",
                        "src/A.java",
                        "m3/vendor/m3/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "1".repeat(64),
                        ImportPlan.Action.REPLACE,
                        ImportPlan.Lane.OPENREWRITE_RECIPE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ImportPlan.Row(
                        "openrewrite-java",
                        "NONE",
                        "m3/vendor/m3/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "1".repeat(64),
                        ImportPlan.Action.KEEP,
                        ImportPlan.Lane.OPENREWRITE_RECIPE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ImportPlan.Row(
                        "openrewrite-java",
                        "src/A.java",
                        "m3/vendor/m3/A.java",
                        "STALE",
                        "1".repeat(64),
                        "1".repeat(64),
                        ImportPlan.Action.KEEP,
                        ImportPlan.Lane.OPENREWRITE_RECIPE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ImportPlan.Plan(
                        "a".repeat(40),
                        "b".repeat(64),
                        List.of(),
                        ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ImportPlan.Plan(
                        "a".repeat(40),
                        "b".repeat(64),
                        List.of(valid, valid),
                        ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ImportPlan.Plan(
                        "a".repeat(40),
                        "b".repeat(64),
                        List.of(valid),
                        "c".repeat(64)));
    }

    @Test
    void cliPlansStagesAndRejectsBadInvocation() throws Exception {
        Fixture fixture = fixture();
        Path manifestFile = temp.resolve("manifest.tsv");
        Files.writeString(manifestFile, fixture.manifest().toTsv());

        ImportPlanCli.main(new String[] {
            "plan",
            manifestFile.toString(),
            fixture.m3jdk().toString(),
            "m3/build/m3-import/cli-plan"
        });
        assertTrue(Files.isRegularFile(
                fixture.m3jdk()
                        .resolve("m3/build/m3-import/cli-plan/PLAN.tsv")));

        ImportPlanCli.main(new String[] {
            "stage",
            manifestFile.toString(),
            fixture.m3().toString(),
            fixture.m3jdk().toString(),
            "m3/build/m3-import/cli-stage"
        });
        assertTrue(Files.isRegularFile(
                fixture.m3jdk()
                        .resolve("m3/build/m3-import/cli-stage/ROOT")));

        assertThrows(
                IllegalArgumentException.class,
                () -> ImportPlanCli.main(new String[0]));
        assertThrows(
                IllegalArgumentException.class,
                () -> ImportPlanCli.main(new String[] {"other"}));
        assertThrows(
                IllegalArgumentException.class,
                () -> ImportPlanCli.main(new String[] {"plan"}));
        assertThrows(
                IllegalArgumentException.class,
                () -> ImportPlanCli.main(new String[] {"stage"}));
    }

    @Test
    void strictStageRequiresExactCleanM3Checkout() throws Exception {
        Fixture fixture = fixture();
        git(fixture.m3(), "init");
        git(fixture.m3(), "config", "user.email", "m3@example.invalid");
        git(fixture.m3(), "config", "user.name", "M3 Test");
        git(fixture.m3(), "add", ".");
        git(fixture.m3(), "commit", "-m", "fixture");
        String head = git(fixture.m3(), "rev-parse", "HEAD").trim();

        ImportManifest strictManifest =
                new ImportManifest(
                        head,
                        fixture.manifest().targetId(),
                        fixture.manifest().entries(),
                        "");
        Path manifestFile = temp.resolve("strict-stage-manifest.tsv");
        Files.writeString(manifestFile, strictManifest.toTsv());

        ImportPlanCli.main(
                new String[] {
                    "stage-strict",
                    manifestFile.toString(),
                    fixture.m3().toString(),
                    fixture.m3jdk().toString(),
                    "m3/build/m3-import/strict-stage"
                });
        assertTrue(
                Files.isRegularFile(
                        fixture.m3jdk()
                                .resolve("m3/build/m3-import/strict-stage/ROOT")));

        Files.writeString(
                fixture.m3().resolve(fixture.keepSource()),
                Files.readString(fixture.m3().resolve(fixture.keepSource())) + "// drift\n");
        assertThrows(
                IllegalStateException.class,
                () ->
                        ImportPlanCli.main(
                                new String[] {
                                    "stage-strict",
                                    manifestFile.toString(),
                                    fixture.m3().toString(),
                                    fixture.m3jdk().toString(),
                                    "m3/build/m3-import/strict-stage-drift"
                                }));
        assertFalse(
                Files.exists(
                        fixture.m3jdk()
                                .resolve("m3/build/m3-import/strict-stage-drift")));
    }

    private static String git(Path root, String... args) throws Exception {
        String[] command = new String[args.length + 3];
        command[0] = "git";
        command[1] = "-C";
        command[2] = root.toString();
        System.arraycopy(args, 0, command, 3, args.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        return output;
    }

    @Test
    void absentVendorTreeProducesOnlyManifestRows() throws Exception {
        Path root = temp.resolve("m3-empty");
        Files.createDirectories(root);
        byte[] bytes = apache("package p; final class A {}\n");
        ImportManifest manifest =
                new ImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(entry(
                                "m3-recipe-source",
                                "m3-recipe/src/A.java",
                                "m3/vendor/m3/m3-recipe/src/A.java",
                                bytes,
                                ImportManifest.Mode.APACHE_SOURCE)),
                        "");
        ImportPlan.Plan plan = ImportPlan.plan(root, manifest);
        assertEquals(1, plan.rows().size());
        assertEquals(ImportPlan.Action.ADD, plan.rows().getFirst().action());
        assertEquals(ImportPlan.Lane.M3_RECIPE, plan.rows().getFirst().lane());
    }

    private Fixture fixture() throws Exception {
        Path m3 = temp.resolve("synexia");
        Path m3jdk = temp.resolve("m3jdk");
        Files.createDirectories(m3);
        Files.createDirectories(m3jdk);

        String keepSource =
                "m3-openrewrite-recipes/src/main/java/jdk/internal/m3/rewrite/Keep.java";
        String replaceSource =
                "m3-code-convergence/src/main/java/jdk/internal/m3/convergence/Replace.java";
        String addSource =
                "m3-code-convergence/src/main/native/add.c";
        String resourceSource =
                "m3-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3.yml";

        String keepTarget = "m3/vendor/m3/" + keepSource;
        String replaceTarget = "m3/vendor/m3/" + replaceSource;
        String addTarget = "m3/vendor/m3/" + addSource;
        String resourceTarget = "m3/vendor/m3/" + resourceSource;
        String staleTarget = "m3/vendor/m3/obsolete/Old.java";

        byte[] keep = apache("package jdk.internal.m3.rewrite; final class Keep {}\n");
        byte[] replace = apache("package jdk.internal.m3.convergence; final class Replace { int v = 2; }\n");
        byte[] add = apache("int m3_add(int a, int b) { return a + b; }\n");
        byte[] resource = "---\ntype: specs.openrewrite.org/v1beta/recipe\nname: jdk.internal.m3.M3\n".getBytes(StandardCharsets.UTF_8);
        byte[] stale = apache("package obsolete; final class Old {}\n");

        write(m3.resolve(keepSource), keep);
        write(m3.resolve(replaceSource), replace);
        write(m3.resolve(addSource), add);
        write(m3.resolve(resourceSource), resource);

        write(m3jdk.resolve(keepTarget), keep);
        write(
                m3jdk.resolve(replaceTarget),
                apache("package jdk.internal.m3.convergence; final class Replace { int v = 1; }\n"));
        write(m3jdk.resolve(staleTarget), stale);

        ImportManifest manifest =
                new ImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(
                                entry(
                                        "openrewrite-java",
                                        keepSource,
                                        keepTarget,
                                        keep,
                                        ImportManifest.Mode.APACHE_SOURCE),
                                entry(
                                        "convergence-java",
                                        replaceSource,
                                        replaceTarget,
                                        replace,
                                        ImportManifest.Mode.APACHE_SOURCE),
                                entry(
                                        "convergence-native",
                                        addSource,
                                        addTarget,
                                        add,
                                        ImportManifest.Mode.APACHE_SOURCE),
                                entry(
                                        "openrewrite-resources",
                                        resourceSource,
                                        resourceTarget,
                                        resource,
                                        ImportManifest.Mode.APACHE_RECIPE_RESOURCE)),
                        "");

        return new Fixture(
                m3,
                m3jdk,
                manifest,
                keepSource,
                replaceSource,
                addSource,
                resourceSource,
                keepTarget,
                replaceTarget,
                addTarget,
                resourceTarget,
                staleTarget);
    }

    private static ImportManifest.Entry entry(
            String category,
            String source,
            String target,
            byte[] bytes,
            ImportManifest.Mode mode) {
        return new ImportManifest.Entry(
                category,
                source,
                target,
                sha256(bytes),
                "Apache-2.0",
                mode);
    }

    private static byte[] apache(String body) {
        return ("// SPDX-License-Identifier: Apache-2.0\n" + body)
                .getBytes(StandardCharsets.UTF_8);
    }

    private static void write(Path path, byte[] bytes) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private record Fixture(
            Path m3,
            Path m3jdk,
            ImportManifest manifest,
            String keepSource,
            String replaceSource,
            String addSource,
            String resourceSource,
            String keepTarget,
            String replaceTarget,
            String addTarget,
            String resourceTarget,
            String staleTarget) {}
}
