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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SynexiaImportPlanTest {
    @TempDir
    Path temp;

    @Test
    void plansFullDeltaAndStagesOnlyChangedCandidates() throws Exception {
        Fixture fixture = fixture();
        SynexiaImportPlan.Plan plan =
                SynexiaImportPlan.plan(fixture.m3jdk(), fixture.manifest());
        Map<String, SynexiaImportPlan.Row> rows =
                plan.rows().stream()
                        .collect(Collectors.toMap(SynexiaImportPlan.Row::targetPath, Function.identity()));

        assertEquals(5, rows.size());
        assertEquals(
                SynexiaImportPlan.Action.KEEP,
                rows.get(fixture.keepTarget()).action());
        assertEquals(
                SynexiaImportPlan.Lane.OPENREWRITE_RECIPE,
                rows.get(fixture.keepTarget()).lane());
        assertEquals(
                SynexiaImportPlan.Action.REPLACE,
                rows.get(fixture.replaceTarget()).action());
        assertEquals(
                SynexiaImportPlan.Lane.CONVERGENCE_JAVA,
                rows.get(fixture.replaceTarget()).lane());
        assertEquals(
                SynexiaImportPlan.Action.ADD,
                rows.get(fixture.addTarget()).action());
        assertEquals(
                SynexiaImportPlan.Lane.CONVERGENCE_NATIVE,
                rows.get(fixture.addTarget()).lane());
        assertEquals(
                SynexiaImportPlan.Lane.OPENREWRITE_RESOURCE,
                rows.get(fixture.resourceTarget()).lane());
        assertEquals(
                SynexiaImportPlan.Action.STALE,
                rows.get(fixture.staleTarget()).action());
        assertEquals(3, plan.changedCount());
        assertEquals(1, plan.staleCount());
        assertFalse(plan.sourceWriteAuthority());
        assertFalse(plan.promotionAuthority());
        assertEquals(64, plan.root().length());
        assertTrue(plan.toTsv().startsWith(SynexiaImportPlan.HEADER + "\n"));
        assertTrue(plan.toTsv().endsWith("# root\t" + plan.root() + "\n"));

        byte[] oldReplace = Files.readAllBytes(fixture.m3jdk().resolve(fixture.replaceTarget()));
        Path out = Path.of("m3/build/synexia-import/full");
        SynexiaImportPlan.Plan staged =
                SynexiaImportPlan.stage(
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
                SynexiaImportPlan.stage(
                        fixture.synexia(), fixture.m3jdk(), fixture.manifest(), out));
    }

    @Test
    void sourceOnlyVerifierAllowsReplacementButRefusesSourceDrift() throws Exception {
        Fixture fixture = fixture();
        SynexiaImporter.verifySources(fixture.synexia(), fixture.manifest());

        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImporter.verify(
                        fixture.synexia(), fixture.m3jdk(), fixture.manifest()));

        Files.writeString(
                fixture.synexia().resolve(fixture.addSource()),
                "// SPDX-License-Identifier: Apache-2.0\nchanged\n");
        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImporter.verifySources(fixture.synexia(), fixture.manifest()));
        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImportPlan.stage(
                        fixture.synexia(),
                        fixture.m3jdk(),
                        fixture.manifest(),
                        Path.of("m3/build/synexia-import/drift")));
    }

    @Test
    void writePlanIsCreateOrVerifyAndRefusesOutsideBuildAndDrift() throws Exception {
        Fixture fixture = fixture();
        SynexiaImportPlan.Plan plan =
                SynexiaImportPlan.plan(fixture.m3jdk(), fixture.manifest());

        Path relative = Path.of("m3/build/synexia-import/plan-only");
        SynexiaImportPlan.writePlan(fixture.m3jdk(), relative, plan);
        SynexiaImportPlan.writePlan(fixture.m3jdk(), relative, plan);
        Path planFile = fixture.m3jdk().resolve(relative).resolve("PLAN.tsv");
        assertEquals(plan.toTsv(), Files.readString(planFile));

        Files.writeString(planFile, "drift\n");
        assertThrows(
                IllegalStateException.class,
                () -> SynexiaImportPlan.writePlan(fixture.m3jdk(), relative, plan));

        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImportPlan.writePlan(
                        fixture.m3jdk(), Path.of("m3/build"), plan));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImportPlan.writePlan(
                        fixture.m3jdk(), Path.of("outside"), plan));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImportPlan.writePlan(
                        fixture.m3jdk(), temp.resolve("absolute-outside"), plan));
    }

    @Test
    void laneClassificationCoversEveryRegisteredDeliveryCategory() {
        String target = "m3/vendor/synexia/other/file.txt";
        assertEquals(
                SynexiaImportPlan.Lane.OPENREWRITE_RECIPE,
                SynexiaImportPlan.lane("openrewrite-java", target));
        assertEquals(
                SynexiaImportPlan.Lane.OPENREWRITE_TEST,
                SynexiaImportPlan.lane("openrewrite-tests", target));
        assertEquals(
                SynexiaImportPlan.Lane.OPENREWRITE_RESOURCE,
                SynexiaImportPlan.lane("openrewrite-resources", target));
        assertEquals(
                SynexiaImportPlan.Lane.CONVERGENCE_JAVA,
                SynexiaImportPlan.lane("convergence-java", target));
        assertEquals(
                SynexiaImportPlan.Lane.CONVERGENCE_TEST,
                SynexiaImportPlan.lane("convergence-tests", target));
        assertEquals(
                SynexiaImportPlan.Lane.CONVERGENCE_NATIVE,
                SynexiaImportPlan.lane("convergence-native", target));
        assertEquals(
                SynexiaImportPlan.Lane.M3_CLONER,
                SynexiaImportPlan.lane("m3-cloner-java", target));
        assertEquals(
                SynexiaImportPlan.Lane.M3_CLONER,
                SynexiaImportPlan.lane("m3-cloner-tests", target));
        assertEquals(
                SynexiaImportPlan.Lane.M3INDEX,
                SynexiaImportPlan.lane("m3index-source", target));
        assertEquals(
                SynexiaImportPlan.Lane.M3_RECIPE,
                SynexiaImportPlan.lane("m3-recipe-source", target));
        assertEquals(
                SynexiaImportPlan.Lane.OTHER_APACHE,
                SynexiaImportPlan.lane("other", target));

        assertEquals(
                SynexiaImportPlan.Lane.OPENREWRITE_RECIPE,
                SynexiaImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/synexia-openrewrite-recipes/src/main/java/p/R.java"));
        assertEquals(
                SynexiaImportPlan.Lane.OPENREWRITE_TEST,
                SynexiaImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/synexia-openrewrite-recipes/src/test/java/p/RTest.java"));
        assertEquals(
                SynexiaImportPlan.Lane.CONVERGENCE_TEST,
                SynexiaImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/synexia-code-convergence/src/test/java/p/Test.java"));
        assertEquals(
                SynexiaImportPlan.Lane.M3INDEX,
                SynexiaImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/synexia-m3index/src/main/java/p/A.java"));
        assertEquals(
                SynexiaImportPlan.Lane.M3_RECIPE,
                SynexiaImportPlan.lane(
                        "stale",
                        "m3/vendor/synexia/synexia-m3-recipe/src/main/java/p/A.java"));
    }

    @Test
    void rowAndPlanValidationFailClosed() {
        SynexiaImportPlan.Row valid =
                new SynexiaImportPlan.Row(
                        "openrewrite-java",
                        "src/A.java",
                        "m3/vendor/synexia/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "ABSENT",
                        SynexiaImportPlan.Action.ADD,
                        SynexiaImportPlan.Lane.OPENREWRITE_RECIPE);

        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaImportPlan.Row(
                        "x",
                        "src/A.java",
                        "outside/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "ABSENT",
                        SynexiaImportPlan.Action.ADD,
                        SynexiaImportPlan.Lane.OTHER_APACHE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaImportPlan.Row(
                        "x",
                        "src/A.java",
                        "m3/vendor/synexia/A.java",
                        "APACHE_SOURCE",
                        "1".repeat(64),
                        "2".repeat(64),
                        SynexiaImportPlan.Action.ADD,
                        SynexiaImportPlan.Lane.OTHER_APACHE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaImportPlan.Row(
                        "stale",
                        "src/A.java",
                        "m3/vendor/synexia/A.java",
                        "STALE",
                        "STALE",
                        "2".repeat(64),
                        SynexiaImportPlan.Action.STALE,
                        SynexiaImportPlan.Lane.OTHER_APACHE));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaImportPlan.Plan(
                        "a".repeat(40),
                        "b".repeat(64),
                        List.of(),
                        ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaImportPlan.Plan(
                        "a".repeat(40),
                        "b".repeat(64),
                        List.of(valid, valid),
                        ""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SynexiaImportPlan.Plan(
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

        SynexiaImportPlanCli.main(new String[] {
            "plan",
            manifestFile.toString(),
            fixture.m3jdk().toString(),
            "m3/build/synexia-import/cli-plan"
        });
        assertTrue(Files.isRegularFile(
                fixture.m3jdk()
                        .resolve("m3/build/synexia-import/cli-plan/PLAN.tsv")));

        SynexiaImportPlanCli.main(new String[] {
            "stage",
            manifestFile.toString(),
            fixture.synexia().toString(),
            fixture.m3jdk().toString(),
            "m3/build/synexia-import/cli-stage"
        });
        assertTrue(Files.isRegularFile(
                fixture.m3jdk()
                        .resolve("m3/build/synexia-import/cli-stage/ROOT")));

        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImportPlanCli.main(new String[0]));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImportPlanCli.main(new String[] {"other"}));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImportPlanCli.main(new String[] {"plan"}));
        assertThrows(
                IllegalArgumentException.class,
                () -> SynexiaImportPlanCli.main(new String[] {"stage"}));
    }

    @Test
    void absentVendorTreeProducesOnlyManifestRows() throws Exception {
        Path root = temp.resolve("m3-empty");
        Files.createDirectories(root);
        byte[] bytes = apache("package p; final class A {}\n");
        SynexiaImportManifest manifest =
                new SynexiaImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(entry(
                                "m3-recipe-source",
                                "src/A.java",
                                "m3/vendor/synexia/synexia-m3-recipe/src/A.java",
                                bytes,
                                SynexiaImportManifest.Mode.APACHE_SOURCE)),
                        "");
        SynexiaImportPlan.Plan plan = SynexiaImportPlan.plan(root, manifest);
        assertEquals(1, plan.rows().size());
        assertEquals(SynexiaImportPlan.Action.ADD, plan.rows().getFirst().action());
        assertEquals(SynexiaImportPlan.Lane.M3_RECIPE, plan.rows().getFirst().lane());
    }

    private Fixture fixture() throws Exception {
        Path synexia = temp.resolve("synexia");
        Path m3jdk = temp.resolve("m3jdk");
        Files.createDirectories(synexia);
        Files.createDirectories(m3jdk);

        String keepSource =
                "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/Keep.java";
        String replaceSource =
                "synexia-code-convergence/src/main/java/com/synexia/convergence/Replace.java";
        String addSource =
                "synexia-code-convergence/src/main/native/add.c";
        String resourceSource =
                "synexia-openrewrite-recipes/src/main/resources/META-INF/rewrite/m3.yml";

        String keepTarget = "m3/vendor/synexia/" + keepSource;
        String replaceTarget = "m3/vendor/synexia/" + replaceSource;
        String addTarget = "m3/vendor/synexia/" + addSource;
        String resourceTarget = "m3/vendor/synexia/" + resourceSource;
        String staleTarget = "m3/vendor/synexia/obsolete/Old.java";

        byte[] keep = apache("package com.synexia.rewrite; final class Keep {}\n");
        byte[] replace = apache("package com.synexia.convergence; final class Replace { int v = 2; }\n");
        byte[] add = apache("int m3_add(int a, int b) { return a + b; }\n");
        byte[] resource = "---\ntype: specs.openrewrite.org/v1beta/recipe\nname: com.synexia.M3\n";
        byte[] stale = apache("package obsolete; final class Old {}\n");

        write(synexia.resolve(keepSource), keep);
        write(synexia.resolve(replaceSource), replace);
        write(synexia.resolve(addSource), add);
        write(synexia.resolve(resourceSource), resource);

        write(m3jdk.resolve(keepTarget), keep);
        write(
                m3jdk.resolve(replaceTarget),
                apache("package com.synexia.convergence; final class Replace { int v = 1; }\n"));
        write(m3jdk.resolve(staleTarget), stale);

        SynexiaImportManifest manifest =
                new SynexiaImportManifest(
                        "a".repeat(40),
                        "m3jdk21",
                        List.of(
                                entry(
                                        "openrewrite-java",
                                        keepSource,
                                        keepTarget,
                                        keep,
                                        SynexiaImportManifest.Mode.APACHE_SOURCE),
                                entry(
                                        "convergence-java",
                                        replaceSource,
                                        replaceTarget,
                                        replace,
                                        SynexiaImportManifest.Mode.APACHE_SOURCE),
                                entry(
                                        "convergence-native",
                                        addSource,
                                        addTarget,
                                        add,
                                        SynexiaImportManifest.Mode.APACHE_SOURCE),
                                entry(
                                        "openrewrite-resources",
                                        resourceSource,
                                        resourceTarget,
                                        resource,
                                        SynexiaImportManifest.Mode.APACHE_RECIPE_RESOURCE)),
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

    private static SynexiaImportManifest.Entry entry(
            String category,
            String source,
            String target,
            byte[] bytes,
            SynexiaImportManifest.Mode mode) {
        return new SynexiaImportManifest.Entry(
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
            SynexiaImportManifest manifest,
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
