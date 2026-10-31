// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

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

final class M3ImportPlanTest {
    @TempDir
    Path temp;

    @Test
    void plansFullDeltaAndStagesOnlyChangedCandidates() throws Exception {
        Fixture fixture = fixture();
        M3ImportPlan.Plan plan =
                M3ImportPlan.plan(fixture.m3jdk(), fixture.manifest());
        Map<String, M3ImportPlan.Row> rows =
                plan.rows().stream()
                        .collect(Collectors.toMap(M3ImportPlan.Row::targetPath, Function.identity()));

        assertEquals(5, rows.size());
        assertEquals(
                M3ImportPlan.Action.KEEP,
                rows.get(fixture.keepTarget()).action());
        assertEquals(
                M3ImportPlan.Lane.OPENREWRITE_RECIPE,
                rows.get(fixture.keepTarget()).lane());
        assertEquals(
                M3ImportPlan.Action.REPLACE,
                rows.get(fixture.replaceTarget()).action());
        assertEquals(
                M3ImportPlan.Lane.CONVERGENCE_JAVA,
                rows.get(fixture.replaceTarget()).lane());
        assertEquals(
                M3ImportPlan.Action.ADD,
                rows.get(fixture.addTarget()).action());
        assertEquals(
                M3ImportPlan.Lane.CONVERGENCE_NATIVE,
                rows.get(fixture.addTarget()).lane());
        assertEquals(
                M3ImportPlan.Lane.OPENREWRITE_RESOURCE,
                rows.get(fixture.resourceTarget()).lane());
        assertEquals(
                M3ImportPlan.Action.STALE,
                rows.get(fixture.staleTarget()).action());
        assertEquals(3, plan.changedCount());
        assertEquals(1, plan.staleCount());
        assertFalse(plan.sourceWriteAuthority());
        assertFalse(plan.promotionAuthority());
        assertEquals(64, plan.root().length());
        assertTrue(plan.toTsv().startsWith(M3ImportPlan.HEADER + "\n"));
        assertTrue(plan.toTsv().endsWith("# root\t" + plan.root() + "\n"));

        byte[] oldReplace = Files.readAllBytes(fixture.m3jdk().resolve(fixture.replaceTarget()));
        Path out = Path.of("m3/build/m3-import/full");
        M3ImportPlan.Plan staged =
                M3ImportPlan.stage(
                        fixture.synexia(), fixture.m3jdk(), fixture.manifest(), out);
        assertEquals(plan, staged);

        Path stagedRoot = fixture.m3jdk().resolve(out);
        assertTrue(Files.isRegularFile(stagedRoot.resolve("PLAN.tsv")));
        assertTrue(Files.isRegularFile(stagedRoot.resolve("ROOT")));
        assertFalse(
                Files.exists(stagedRoot.resolve("candidate").resolve(fixture.keepTarget())));
        assertEquals(
                sha256(Files.readAllBytes(fixture.synexia().resolve(fixture.replaceSource()))),
                sha256(Files.readAllBytes(
                        stagedRoot.resolve("candidate").resolve(fixture.replaceTarget()))));
        assertEquals(
                sha256(Files.readAllBytes(fixture.synexia().resolve(fixture.addSource()))),
                sha256(Files.readAllBytes(
                        stagedRoot.resolve("candidate").resolve(fixture.addTarget()))));
        assertEquals(
                sha256(Files.readAllBytes(fixture.synexia().resolve(fixture.resourceSource()))),
                sha256(Files.readAllBytes(
                        stagedRoot.resolve("candidate").resolve(fixture.resourceTarget()))));

        assertEquals(
                sha256(oldReplace),
                sha256(Files.readAllBytes(fixture.m3jdk().resolve(fixture.replaceTarget()))));
        assertTrue(Files.isRegularFile(fixture.m3jdk().resolve(fixture.staleTarget())));

        assertEquals(
                staged,
                M3ImportPlan.stage(
                        fixture.synexia(), fixture.m3jdk(), fixture.manifest(), out));
    }

    @Test
    void sourceOnlyVerifierAllowsReplacementButRefusesSourceDrift() throws Exception {
        Fixture fixture = fixture();
        M3Importer.verifySources(fixture.synexia(), fixture.manifest());

        assertThrows(
                IllegalStateException.class,
                () -> M3Importer.verify(
                        fixture.synexia(), fixture.m3jdk(), fixture.manifest()));

        Files.writeString(
                fixture.synexia().resolve(fixture.addSource()),
                "// SPDX-License-Identifier: Apache-2.0\nchanged\n");
        assertThrows(
                IllegalStateException.class,
                () -> M3Importer.verifySources(fixture.synexia(), fixture.manifest()));
        assertThrows(
                IllegalStateException.class,
                () -> M3ImportPlan.stage(
                        fixture.synexia(),
                        fixture.m3jdk(),
                        fixture.manifest(),
                        Path.of("m3/build/m3-import/drift")));
    }

    @Test
    void writePlanIsCreateOrVerifyAndRefusesOutsideBuildAndDrift() throws Exception {
        Fixture fixture = fixture();
        M3ImportPlan.Plan plan =
                M3ImportPlan.plan(fixture.m3jdk(), fixture.manifest());

        Path relative = Path.of("m3/build/m3-import/plan-only");
        M3ImportPlan.writePlan(fixture.m3jdk(), relative, plan);
        M3ImportPlan.writePlan(fixture.m3jdk(), relative, plan);
        Path planFile = fixture.m3jdk().resolve(relative).resolve("PLAN.tsv");
        assertEquals(plan.toTsv(), Files.readString(planFile));

        Files.writeString(planFile, "drift\n");
        assertThrows(
                IllegalStateException.class,
                () -> M3ImportPlan.writePlan(fixture.m3jdk(), relative, plan));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3ImportPlan.writePlan(
                        fixture.m3jdk(), Path.of("m3/build"), plan));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ImportPlan.writePlan(
                        fixture.m3jdk(), Path.of("outside"), plan));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ImportPlan.writePlan(
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
                () -> M3ImportPlan.writePlan(
                        fixture.m3jdk(), Path.of("m3/build/link/run"), plan));
    }

    @Test
    void laneClassificationCoversEveryRegisteredDeliveryCategory() {
        String target = "m3/vendor/synexia/other/file.txt";
        assertEquals(
                M3ImportPlan.Lane.OPENREWRITE_RECIPE,
                M3ImportPlan.lane("openrewrite-java", target));
        assertEquals(
                M3ImportPlan.Lane.OPENREWRITE_TEST,
                M3ImportPlan.lane("openrewrite-tests", target));
        assertEquals(
                M3ImportPlan.Lane.OPENREWRITE_RESOURCE,
                M3ImportPlan.lane("openrewrite-resources", target));
        assertEquals(
                M3ImportPlan.Lane.CONVERGENCE_JAVA,
                M3ImportPlan.lane("convergence-java", target));
        assertEquals(
                M3ImportPlan.Lane.CONVERGENCE_TEST,
                M3ImportPlan.lane("convergence-tests", target));
        assertEquals(
                M3ImportPlan.Lane.CONVERGENCE_NATIVE,
                M3ImportPlan.lane("convergence-native", target));
        assertEquals(
                M3ImportPlan.Lane.M3_CLONER,
                M3ImportPlan.lane("m3-cloner-java", target));
        assertEquals(
                M3ImportPlan.Lane.M3_CLONER,
                M3ImportPlan.lane("m3-cloner-tests", target));
        assertEquals(
                M3ImportPlan.Lane.M3INDEX,
                M3ImportPlan.lane("m3index-source", target));
        assertEquals(
                M3ImportPlan.Lane.M3_RECIPE,
                M3ImportPlan.lane("m3-recipe-source", target));
        assertEquals(
                M3ImportPlan.Lane.OTHER_APACHE,
                M3ImportPlan.lane("other", target));

        assertEquals(
                M3ImportPlan.Lane.OPENREWRITE_RECIPE,
                M3ImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/m3-openrewrite-recipes/src/main/java/p/R.java"));
        assertEquals(
                M3ImportPlan.Lane.OPENREWRITE_TEST,
                M3ImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/m3-openrewrite-recipes/src/test/java/p/RTest.java"));
        assertEquals(
                M3ImportPlan.Lane.CONVERGENCE_TEST,
                M3ImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/m3-code-convergence/src/test/java/p/Test.java"));
        assertEquals(
                M3ImportPlan.Lane.M3INDEX,
                M3ImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/m3-m3index/src/main/java/p/A.java"));
        assertEquals(
                M3ImportPlan.Lane.M3_RECIPE,
                M3ImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/m3-m3-recipe/src/main/java/p/A.java"));
    }

    @Test
    void rowAndPlanValidationFailClosed() {
        M3ImportPlan.Row valid =
                new M3ImportPlan.Row(
                        "openrewrite-java",
                        "src/A.java",
                        "m3/vendor/synexia/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "ABSENT",
                        M3ImportPlan.Action.ADD,
                        M3ImportPlan.Lane.OPENREWRITE_RECIPE);

        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportPlan.Row(
                        "x",
                        "src/A.java",
                        "outside/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "ABSENT",
                        M3ImportPlan.Action.ADD,
                        M3ImportPlan.Lane.OTHER_APACHE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportPlan.Row(
                        "x",
                        "src/A.java",
                        "m3/vendor/synexia/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "2".repeat(64),
                        M3ImportPlan.Action.ADD,
                        M3ImportPlan.Lane.OTHER_APACHE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportPlan.Row(
                        "stale",
                        "src/A.java",
                        "m3/vendor/synexia/A.java",
                        "STALE",
                        "STALE",
                        "2".repeat(64),
                        M3ImportPlan.Action.STALE,
                        M3ImportPlan.Lane.OTHER_APACHE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportPlan.Row(
                        "openrewrite-java",
                        "src/A.java",
                        "m3/vendor/synexia/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "2".repeat(64),
                        M3ImportPlan.Action.KEEP,
                        M3ImportPlan.Lane.OPENREWRITE_RECIPE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportPlan.Row(
                        "openrewrite-java",
                        "src/A.java",
                        "m3/vendor/synexia/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "1".repeat(64),
                        M3ImportPlan.Action.REPLACE,
                        M3ImportPlan.Lane.OPENREWRITE_RECIPE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportPlan.Row(
                        "openrewrite-java",
                        "NONE",
                        "m3/vendor/synexia/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "1".repeat(64),
                        M3ImportPlan.Action.KEEP,
                        M3ImportPlan.Lane.OPENREWRITE_RECIPE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportPlan.Row(
                        "openrewrite-java",
                        "src/A.java",
                        "m3/vendor/synexia/A.java",
                        "STALE",
                        "1".repeat(64),
                        "1".repeat(64),
                        M3ImportPlan.Action.KEEP,
                        M3ImportPlan.Lane.OPENREWRITE_RECIPE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportPlan.Plan(
                        "a".repeat(40),
                        "b".repeat(64),
                        List.of(),
                        ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportPlan.Plan(
                        "a".repeat(40),
                        "b".repeat(64),
                        List.of(valid, valid),
                        ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3ImportPlan.Plan(
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

        M3ImportPlanCli.main(new String[] {
            "plan",
            manifestFile.toString(),
            fixture.m3jdk().toString(),
            "m3/build/m3-import/cli-plan"
        });
        assertTrue(Files.isRegularFile(
                fixture.m3jdk()
                        .resolve("m3/build/m3-import/cli-plan/PLAN.tsv")));

        M3ImportPlanCli.main(new String[] {
            "stage",
            manifestFile.toString(),
            fixture.synexia().toString(),
            fixture.m3jdk().toString(),
            "m3/build/m3-import/cli-stage"
        });
        assertTrue(Files.isRegularFile(
                fixture.m3jdk()
                        .resolve("m3/build/m3-import/cli-stage/ROOT")));

        assertThrows(
                IllegalArgumentException.class,
                () -> M3ImportPlanCli.main(new String[0]));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ImportPlanCli.main(new String[] {"other"}));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ImportPlanCli.main(new String[] {"plan"}));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3ImportPlanCli.main(new String[] {"stage"}));
    }

    @Test
    void strictStageRequiresExactCleanSynexiaCheckout() throws Exception {
        Fixture fixture = fixture();
        git(fixture.synexia(), "init");
        git(fixture.synexia(), "config", "user.email", "m3@example.invalid");
        git(fixture.synexia(), "config", "user.name", "M3 Test");
        git(fixture.synexia(), "add", ".");
        git(fixture.synexia(), "commit", "-m", "fixture");
        String head = git(fixture.synexia(), "rev-parse", "HEAD").trim();

        M3ImportManifest strictManifest =
                new M3ImportManifest(
                        head,
                        fixture.manifest().targetId(),
                        fixture.manifest().entries(),
                        "");
        Path manifestFile = temp.resolve("strict-stage-manifest.tsv");
        Files.writeString(manifestFile, strictManifest.toTsv());

        M3ImportPlanCli.main(
                new String[] {
                    "stage-strict",
                    manifestFile.toString(),
                    fixture.synexia().toString(),
                    fixture.m3jdk().toString(),
                    "m3/build/m3-import/strict-stage"
                });
        assertTrue(
                Files.isRegularFile(
                        fixture.m3jdk()
                                .resolve("m3/build/m3-import/strict-stage/ROOT")));

        Files.writeString(
                fixture.synexia().resolve(fixture.keepSource()),
                Files.readString(fixture.synexia().resolve(fixture.keepSource())) + "// drift\n");
        assertThrows(
                IllegalStateException.class,
                () ->
                        M3ImportPlanCli.main(
                                new String[] {
                                    "stage-strict",
                                    manifestFile.toString(),
                                    fixture.synexia().toString(),
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
        M3ImportManifest manifest =
                new M3ImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(entry(
                                "m3-recipe-source",
                                "m3-m3-recipe/src/A.java",
                                "m3/vendor/synexia/m3-m3-recipe/src/A.java",
                                bytes,
                                M3ImportManifest.Mode.APACHE_SOURCE)),
                        "");
        M3ImportPlan.Plan plan = M3ImportPlan.plan(root, manifest);
        assertEquals(1, plan.rows().size());
        assertEquals(M3ImportPlan.Action.ADD, plan.rows().getFirst().action());
        assertEquals(M3ImportPlan.Lane.M3_RECIPE, plan.rows().getFirst().lane());
    }

    private Fixture fixture() throws Exception {
        Path synexia = temp.resolve("synexia");
        Path m3jdk = temp.resolve("m3jdk");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);

        String keepSource =
                "m3-openrewrite-recipes/src/main/java/jdk/internal/m3/rewrite/Keep.java";
        String replaceSource =
                "m3-code-convergence/src/main/java/jdk/internal/m3/convergence/Replace.java";
        String addSource =
                "m3-code-convergence/src/main/native/add.c";
        String resourceSource =
                "m3-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3.yml";

        String keepTarget = "m3/vendor/synexia/" + keepSource;
        String replaceTarget = "m3/vendor/synexia/" + replaceSource;
        String addTarget = "m3/vendor/synexia/" + addSource;
        String resourceTarget = "m3/vendor/synexia/" + resourceSource;
        String staleTarget = "m3/vendor/synexia/obsolete/Old.java";

        byte[] keep = apache("package jdk.internal.m3.rewrite; final class Keep {}\n");
        byte[] replace = apache("package jdk.internal.m3.convergence; final class Replace { int v = 2; }\n");
        byte[] add = apache("int m3_add(int a, int b) { return a + b; }\n");
        byte[] resource = "---\ntype: specs.openrewrite.org/v1beta/recipe\nname: com.synexia.M3\n".getBytes(StandardCharsets.UTF_8);
        byte[] stale = apache("package obsolete; final class Old {}\n");

        write(synexia.resolve(keepSource), keep);
        write(synexia.resolve(replaceSource), replace);
        write(synexia.resolve(addSource), add);
        write(synexia.resolve(resourceSource), resource);

        write(m3jdk.resolve(keepTarget), keep);
        write(
                m3jdk.resolve(replaceTarget),
                apache("package jdk.internal.m3.convergence; final class Replace { int v = 1; }\n"));
        write(m3jdk.resolve(staleTarget), stale);

        M3ImportManifest manifest =
                new M3ImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(
                                entry(
                                        "openrewrite-java",
                                        keepSource,
                                        keepTarget,
                                        keep,
                                        M3ImportManifest.Mode.APACHE_SOURCE),
                                entry(
                                        "convergence-java",
                                        replaceSource,
                                        replaceTarget,
                                        replace,
                                        M3ImportManifest.Mode.APACHE_SOURCE),
                                entry(
                                        "convergence-native",
                                        addSource,
                                        addTarget,
                                        add,
                                        M3ImportManifest.Mode.APACHE_SOURCE),
                                entry(
                                        "openrewrite-resources",
                                        resourceSource,
                                        resourceTarget,
                                        resource,
                                        M3ImportManifest.Mode.APACHE_RECIPE_RESOURCE)),
                        "");

        return new Fixture(
                synexia,
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

    private static M3ImportManifest.Entry entry(
            String category,
            String source,
            String target,
            byte[] bytes,
            M3ImportManifest.Mode mode) {
        return new M3ImportManifest.Entry(
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
            Path synexia,
            Path m3jdk,
            M3ImportManifest manifest,
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
