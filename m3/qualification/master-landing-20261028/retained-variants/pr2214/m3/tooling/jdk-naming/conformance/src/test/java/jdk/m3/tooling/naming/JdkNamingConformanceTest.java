/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The repository conforms to the M3JDK21 naming standard, and this executor agrees with the rename recipe on the
 * fixtures of {@code test_jdk_naming.py} (the discriminators: kept spans, the JNI forms, the doubled prefix).
 */
class JdkNamingConformanceTest {

    private static Path root;
    private static NamingRules rules;

    @BeforeAll
    static void load() throws IOException {
        root = Path.of(System.getProperty("m3.repo.root", "../../../..")).toAbsolutePath().normalize();
        rules = NamingRules.load(root.resolve("m3/tooling/jdk-naming/NAMING_RULES.tsv"));
    }

    private static String rewritten(String line) {
        return rules.rewrite(line, new HashMap<>());
    }

    @Test
    void theTableLoadsInOrder() {
        List<String> names = rules.textRules().stream().map(NamingRules.Rule::name).toList();
        assertEquals("PKG_M3", names.get(0));
        assertEquals("DOUBLE", names.get(names.size() - 1));
        assertTrue(names.indexOf("TYPE_M3") < names.indexOf("TYPE"), "the specific rule precedes the general one");
        assertEquals("$1", NamingRules.replacement("\\1"));
        assertEquals("jdk.m3.", NamingRules.replacement("jdk.m3."));
    }

    @Test
    void packagesTypesConstantsAndNamesAreRenamed() {
        assertEquals("package jdk.internal.m3.x;", rewritten("package com.synexia.m3.x;"));
        assertEquals("import jdk.internal.m3.util.M3Helper;", rewritten("import com.synexia.util.SynexiaHelper;"));
        assertEquals("public final class M3Foo {", rewritten("public final class SynexiaFoo {"));
        assertEquals("getenv(\"M3_TOKEN\") \"m3-import\"", rewritten("getenv(\"SYNEXIA_TOKEN\") \"synexia-import\""));
        assertEquals("package jdk.m3.collections; import jdk.m3.arrays.B;",
                rewritten("package com.m3.collections; import com.m3.arrays.B;"));
        assertEquals("M3-LLM-FIRST-2 m3/vendor/m3/lib canonicalM3Owner M3SourceCanon",
                rewritten("SYNEXIA-LLM-FIRST-2 m3/vendor/synexia/lib canonicalSynexiaOwner M3SynexiaSourceCanon"));
    }

    @Test
    void keptLinesSpansAndProseStay() {
        String copyright = "// Copyright 2026 Synexia contributors";
        assertEquals(copyright, rewritten(copyright));
        assertEquals("<dependency><groupId>com.synexia</groupId><artifactId>m3-x</artifactId></dependency>",
                rewritten("<dependency><groupId>com.synexia</groupId><artifactId>synexia-x</artifactId></dependency>"));
        assertEquals("token: ${{ secrets.SYNEXIA_READ_TOKEN }} env: M3_MODE",
                rewritten("token: ${{ secrets.SYNEXIA_READ_TOKEN }} env: SYNEXIA_MODE"));
        assertEquals("// see https://github.com/hsoliwal/com.synexia/blob/x/SynexiaFoo.java; use M3Bar",
                rewritten("// see https://github.com/hsoliwal/com.synexia/blob/x/SynexiaFoo.java; use SynexiaBar"));
        assertEquals("// the Synexia tool, see vendor/m3/lib and Synexia-NOTICE",
                rewritten("// the Synexia tool, see vendor/synexia/lib and Synexia-NOTICE"));
        assertTrue(rules.keepsPath("m3/x/Synexia-NOTICE"));
        assertTrue(rules.keepsPath("M3-SYNEXIA-NOTICE.md"));
    }

    @Test
    void jniSymbolsHeadersFlattenedPathsAndDoubledPrefixes() {
        assertEquals("Java_jdk_internal_m3_common_collections_SegmentedBits_rank",
                rewritten("Java_com_synexia_common_collections_SegmentedBits_rank"));
        assertEquals("#include \"jdk_internal_m3_mindex_TextMetadata.h\"",
                rewritten("#include \"com_synexia_mindex_TextMetadata.h\""));
        assertEquals("Java_jdk_m3_fs_M3FsNative_open", rewritten("Java_com_m3_fs_M3FsNative_open"));
        assertEquals("src__main__java__jdk__internal__m3__search__Problem",
                rewritten("src__main__java__com__synexia__m3__search__Problem"));
        assertEquals("check_m3_family.py M3_MODE m3-x", rewritten("check_m3_m3_family.py M3_M3_MODE m3-m3-m3-x"));
        assertEquals("libsynexia_mindex_swap", rewritten("libsynexia_mindex_swap"));
    }

    @Test
    void dottedSegmentsRootPackageTypesPrefixLiteralsAndRecordKeys() {
        assertEquals("package jdk.m3.importer; import jdk.internal.m3.M3Planner; p.startsWith(\"com.synexia.\")",
                rewritten("package com.m3.synexia.importer; import com.synexia.M3Planner; p.startsWith(\"com.synexia.\")"));
        assertEquals("row[\"synexiaState\"] legal/synexia.md \"schema\": \"m3.recipe-source-custody/1\" https://synexia.com/x",
                rewritten("row[\"synexiaState\"] legal/synexia.md \"schema\": \"synexia.recipe-source-custody/1\" https://synexia.com/x"));
        assertEquals("m3/m3-import/src/main/java/jdk/m3/importer/M3ImportPlan.java",
                rules.renamedPath("m3/synexia-import/src/main/java/com/m3/synexia/importer/M3ImportPlan.java"));
        assertTrue(rules.keepsPath("src/java.base/share/legal/synexia.md"));
        assertEquals("package jdk.internal.m3.recipe; lab_jdk.internal.m3.rewrite.X recipe\\tjdk.internal.m3.rewrite.Y"
                + "\\njdk.m3.z.W ${m3.version} m3-tq/synexia.md.txt.before Java_jdk_internal_m3_recipe_X_f",
                rewritten("package org.synexia.m3.recipe; lab_com.synexia.rewrite.X recipe\\tcom.synexia.rewrite.Y"
                        + "\\ncom.m3.z.W ${m3.synexia.version} m3-tq/synexia.md.txt.before Java_org_synexia_m3_recipe_X_f"));
    }

    @Test
    void scopeAndLineageMirrorTheRecipe() {
        assertTrue(NamingScan.inScope("m3/x/Foo.java"));
        assertTrue(NamingScan.inScope(".github/workflows/w.yml"));
        assertTrue(NamingScan.inScope("m3/x/pom.xml"));
        assertFalse(NamingScan.inScope("m3/x/provenance.json"));
        assertFalse(NamingScan.inScope("m3/x/evidence/run.json"));
        assertFalse(NamingScan.inScope("m3/docs/notes.yml"));
        assertFalse(NamingScan.inScope("m3/x/README.md"));
        assertFalse(NamingScan.inScope("m3/x/NOTICE"));
        assertFalse(NamingScan.inScope("m3/tooling/jdk-naming/jdk_naming.py"));
        assertFalse(NamingScan.inScope("m3/release/check_standalone.py"));
        assertTrue(NamingScan.isLineage("m3/x/surefire-reports/TEST-a.xml"));
    }

    @Test
    void theRepositoryConformsToJdkNaming() throws IOException, InterruptedException {
        assertTrue(Files.isDirectory(root.resolve("m3")), "repository root " + root);
        String rev = System.getProperty("m3.naming.rev", "HEAD"); // blobs of a revision: a sparse checkout scans all
        List<NamingScan.Violation> violations = new NamingScan(rules, root).scan(rev);
        String report = violations.stream().limit(60).map(NamingScan.Violation::toString)
                .collect(Collectors.joining("\n"));
        assertEquals(0, violations.size(), violations.size() + " names do not conform to NAMING_STANDARD.md:\n"
                + report);
    }

    @Test
    void aCollidingLineageFileKeepsItsNameUnderItsRenamedDirectory() {
        NamingScan scan = new NamingScan(rules, root);
        Set<String> tracked = Set.of("m3/vendor/m3/image/com.m3.x-jar.log.txt", "m3/vendor/m3/image/jdk.m3.x-jar.log.txt",
                "m3/vendor/synexia/image/com.m3.y-jar.log.txt", "m3/x/com.m3.z-notes.txt");
        Map<String, Integer> claims = new HashMap<>();
        tracked.forEach(path -> claims.merge(rules.renamedPath(path), 1, Integer::sum));
        assertTrue(scan.pathConformant("m3/vendor/m3/image/com.m3.x-jar.log.txt", tracked, claims), "kept name");
        assertTrue(scan.pathConformant("m3/vendor/m3/image/jdk.m3.x-jar.log.txt", tracked, claims));
        assertFalse(scan.pathConformant("m3/vendor/synexia/image/com.m3.y-jar.log.txt", tracked, claims), "directory");
        assertFalse(scan.pathConformant("m3/x/com.m3.z-notes.txt", tracked, claims), "no collision: must be renamed");
        assertTrue(NamingScan.isLineage("m3/x/com.m3.x-jar.log.txt"));
        Set<String> evidence = Set.of("m3/t/evidence/synexia-preswitch/ROWS.txt", "m3/t/evidence/m3-preswitch/ROWS.txt");
        Map<String, Integer> evidenceClaims = new HashMap<>();
        evidence.forEach(path -> evidenceClaims.merge(scan.targetPath(path), 1, Integer::sum));
        assertTrue(scan.pathConformant("m3/t/evidence/synexia-preswitch/ROWS.txt", evidence, evidenceClaims),
                "kept in place: its name-kept target is taken too");
        assertEquals("m3/x/m3-openrewrite-recipes/licenses/Synexia-LICENSE",
                scan.targetPath("m3/x/synexia-openrewrite-recipes/licenses/Synexia-LICENSE"));
        assertEquals("python3 m3/compatibility/check-m3.py; dependency_on_synexia",
                rewritten("python3 m3/compatibility/check-synexia.py; dependency_on_synexia"));
        assertFalse(NamingScan.isLineage("m3/x/after-009.java.txt"));
    }

    @Test
    void aFixtureLineIsReportedWithItsRules() {
        List<NamingScan.Violation> found = new java.util.ArrayList<>();
        new NamingScan(rules, root).scanFile("m3/x/A.java",
                "class A {}\nimport com.synexia.x.SynexiaB;\n".getBytes(java.nio.charset.StandardCharsets.UTF_8), found);
        assertEquals(1, found.size());
        assertEquals(2, found.get(0).line());
        assertEquals("PKG,TYPE", found.get(0).rules());
        Map<String, Integer> counts = new HashMap<>();
        rules.rewrite("import com.synexia.x.SynexiaB;", counts);
        assertEquals(Map.of("PKG", 1, "TYPE", 1), counts);
    }
}
