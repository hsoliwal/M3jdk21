// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class A3PlanTest {

    @TempDir
    Path root;

    @Test
    void plansJepJbsCapabilityAndChallengeRowsWithoutErasingDisposition()
            throws Exception {
        writeBaseCatalogues();

        List<A3Plan.Row> rows = A3Plan.load(root);

        assertEquals(8, rows.size());
        assertEquals(
                A3Plan.Lane.DIRECT,
                require(rows, "JEP-485").lane());
        assertEquals(
                A3Plan.Lane.SYSTEM,
                require(rows, "JEP-534").lane());
        assertEquals(
                A3Plan.Lane.HOLD,
                require(rows, "JEP-533").lane());
        assertEquals(
                A3Plan.Lane.BLOCK,
                require(rows, "JEP-486").lane());
        assertEquals(
                A3Plan.Lane.ADMIT,
                require(rows, "JDK-1").lane());
        assertEquals(
                A3Plan.Lane.ADAPT,
                require(rows, "JDK-2").lane());
        assertEquals(
                A3Plan.Lane.REVIEW,
                require(rows, "PACK-A3").lane());
        assertEquals(
                A3Plan.Lane.REVIEW,
                require(rows, "SEARCH-FUZZY").lane());
        assertEquals(
                A3Plan.Kind.ALGO,
                require(rows, "SEARCH-FUZZY").kind());
        assertEquals(
                "reference-only",
                require(rows, "SEARCH-FUZZY").disposition());
        assertEquals(
                "reject-compat",
                require(rows, "JEP-486").disposition());

        A3Plan.write(root, Path.of("m3/build/a3/plan.tsv"));
        String plan =
                Files.readString(root.resolve("m3/build/a3/plan.tsv"));
        assertTrue(plan.contains("JEP-534"));
        assertTrue(plan.contains("\tSYSTEM\t"));
        assertTrue(plan.contains("PACK-A3"));
        assertTrue(plan.contains("SEARCH-FUZZY"));
    }

    @Test
    void joinsCompleteReleasedCommitInventoryWithoutReplacingSeedReview()
            throws Exception {
        writeBaseCatalogues();
        Path upstream =
                root.resolve("m3/build/a3/upstream/UPSTREAM_CHANGES.tsv");
        Files.createDirectories(upstream.getParent());
        Files.writeString(
                upstream,
                """
                release	base_ref	head_ref	commit	jbs_ids	subject	domain	javac_touch	grammar_touch	hotspot_compiler_touch	compatibility_signal	disposition	paths
                24	jdk-23+37	jdk-24+36	1111111111111111111111111111111111111111	JDK-100	Add a library fast path	core-libs	false	false	false	false	review	src/java.base/share/classes/java/lang/String.java
                25	jdk-24+36	jdk-25+36	2222222222222222222222222222222222222222	JDK-200	HotSpot optimizer improvement	hotspot	false	false	true	false	review-hotspot-compiler	src/hotspot/share/opto/example.cpp
                """);

        List<A3Plan.Row> rows =
                A3Plan.load(
                        root,
                        Path.of(
                                "m3/build/a3/upstream/UPSTREAM_CHANGES.tsv"));

        assertEquals(10, rows.size());
        A3Plan.Row library =
                require(
                        rows,
                        "1111111111111111111111111111111111111111");
        assertEquals(A3Plan.Kind.UPSTREAM, library.kind());
        assertEquals(A3Plan.Lane.REVIEW, library.lane());
        assertEquals("review", library.disposition());
        assertTrue(library.reason().contains("jbs=JDK-100"));
        assertTrue(library.reason().contains("paths=src/java.base"));

        A3Plan.Row hotspot =
                require(
                        rows,
                        "2222222222222222222222222222222222222222");
        assertEquals(A3Plan.Lane.SYSTEM, hotspot.lane());
        assertEquals(
                "2222222222222222222222222222222222222222",
                hotspot.upstream());

        assertEquals(
                A3Plan.Lane.ADMIT,
                require(rows, "JDK-1").lane(),
                "full inventory must not replace deeper seed review");
    }

    @Test
    void challengeTaxonomyFailsClosedOnAuthorityOrReviewOrderDrift()
            throws Exception {
        writeBaseCatalogues();
        Path taxonomy =
                root.resolve(
                        "m3/backports/CHALLENGE_SEARCH_TAXONOMY.tsv");

        String valid = Files.readString(taxonomy);
        Files.writeString(
                taxonomy,
                valid.replace(
                        "\tfalse\tprove",
                        "\ttrue\tprove"));
        assertThrows(IOException.class, () -> A3Plan.load(root));

        Files.writeString(
                taxonomy,
                valid.replace(
                        "LEETCODE>HACKERRANK>GEEKSFORGEEKS",
                        "HACKERRANK>LEETCODE>GEEKSFORGEEKS"));
        assertThrows(IOException.class, () -> A3Plan.load(root));
    }

    @Test
    void fullInventoryHeaderDriftFailsClosed() throws Exception {
        writeBaseCatalogues();
        Path upstream =
                root.resolve("m3/build/a3/upstream/UPSTREAM_CHANGES.tsv");
        Files.createDirectories(upstream.getParent());
        Files.writeString(
                upstream,
                "release\tcommit\n24\tdeadbeef\n");

        assertThrows(
                IOException.class,
                () ->
                        A3Plan.load(
                                root,
                                Path.of(
                                        "m3/build/a3/upstream/UPSTREAM_CHANGES.tsv")));
    }

    private void writeBaseCatalogues() throws Exception {
        Path backports = root.resolve("m3/backports");
        Files.createDirectories(backports);

        Files.writeString(
                backports.resolve("JEP_CATALOGUE.tsv"),
                """
                release	jep	title	domain	disposition	reason	superseded_by
                24	485	Stream Gatherers	library	candidate	final API	
                27	534	Compact Object Headers	vm-gc-runtime	candidate-high-risk	VM change	
                27	533	Structured Concurrency	library-runtime	hold-preview	preview	
                24	486	Disable Security Manager	compatibility-security	reject-compat	incompatible	
                """);
        Files.writeString(
                backports.resolve("UPSTREAM_CHANGE_SEEDS.tsv"),
                """
                release	jbs	title	component	disposition	reason	upstream_commit
                27	JDK-1	A tool leaf	tools	admitted	pinned	abc
                27	JDK-2	Adapted leaf	security	candidate-adapted	ported	def
                """);
        Files.writeString(
                backports.resolve("COMMUNITY_CAPABILITY_CANDIDATES.tsv"),
                """
                capability_id	plane	candidate	evidence_type	evidence_ref	upstream_join_key	packaging_candidate	status	selected_for_distribution	next_proof
                PACK-A3	TOOLING_PACK	A3 tooling	USER_REQUEST	R1	UNKNOWN	MAVEN	PENDING_ARTIFACT_PROOF	false	prove build
                """);
        Files.writeString(
                backports.resolve("CHALLENGE_SEARCH_TAXONOMY.tsv"),
                """
                category_id	family	problem_shape	reference_platforms	review_order	jdk_owner_hint	github_donor	license	source_copy_authority	next_proof
                SEARCH-FUZZY	SEARCH	bounded edit distance	LeetCode;HackerRank;GeeksforGeeks	LEETCODE>HACKERRANK>GEEKSFORGEEKS	M3 fuzzy tooling	apache/lucene	Apache-2.0	false	prove exact oracle
                """);
    }

    private static A3Plan.Row require(
            List<A3Plan.Row> rows,
            String id) {
        return rows.stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
