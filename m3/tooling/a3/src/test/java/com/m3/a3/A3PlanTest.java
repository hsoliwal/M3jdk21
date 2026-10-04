// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class A3PlanTest {

    @TempDir
    Path root;

    @Test
    void plansJepJbsCommunityAndChallengeRowsWithoutErasingAuthority()
            throws Exception {
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
                backports.resolve("CHALLENGE_CATEGORY_EVIDENCE.tsv"),
                """
                category	source	review_order	problem_count	representative_ids	evidence_only	source_repository	source_path	source_blob
                SEARCH	LEETCODE	1	3	28,3303,438	true	hsoliwal/com.synexia	synexia-fastsearch/src/main/resources/problem-catalogue.tsv	f29f61ff48d06a42bd7a532c2973a6beb986d852
                SEARCH	HACKERRANK	2	4	find-a-string,string-similarity,save-humanity,determining-dna-health	true	hsoliwal/com.synexia	synexia-fastsearch/src/main/resources/problem-catalogue.tsv	f29f61ff48d06a42bd7a532c2973a6beb986d852
                SEARCH	GEEKSFORGEEKS	3	2	search-pattern0205,count-occurences-of-anagrams5839	true	hsoliwal/com.synexia	synexia-fastsearch/src/main/resources/problem-catalogue.tsv	f29f61ff48d06a42bd7a532c2973a6beb986d852
                """);

        List<A3Plan.Row> rows = A3Plan.load(root);

        assertEquals(10, rows.size());
        assertEquals(A3Plan.Lane.DIRECT, require(rows, "JEP-485").lane());
        assertEquals(A3Plan.Lane.SYSTEM, require(rows, "JEP-534").lane());
        assertEquals(A3Plan.Lane.HOLD, require(rows, "JEP-533").lane());
        assertEquals(A3Plan.Lane.BLOCK, require(rows, "JEP-486").lane());
        assertEquals(A3Plan.Lane.ADMIT, require(rows, "JDK-1").lane());
        assertEquals(A3Plan.Lane.ADAPT, require(rows, "JDK-2").lane());
        assertEquals(A3Plan.Lane.REVIEW, require(rows, "PACK-A3").lane());
        assertEquals(
                A3Plan.Lane.REVIEW,
                require(rows, "CAT-SEARCH-1-LEETCODE").lane());
        assertEquals(
                "evidence-only",
                require(rows, "CAT-SEARCH-2-HACKERRANK").disposition());
        assertTrue(
                require(rows, "CAT-SEARCH-3-GEEKSFORGEEKS")
                        .upstream()
                        .contains("f29f61ff48d06a42bd7a532c2973a6beb986d852"));
        assertEquals(
                List.of(
                        "CAT-SEARCH-1-LEETCODE",
                        "CAT-SEARCH-2-HACKERRANK",
                        "CAT-SEARCH-3-GEEKSFORGEEKS"),
                rows.stream()
                        .filter(row -> row.kind() == A3Plan.Kind.CAT)
                        .map(A3Plan.Row::id)
                        .toList());
        assertEquals(
                "reject-compat",
                require(rows, "JEP-486").disposition());

        A3Plan.write(root, Path.of("m3/build/a3/plan.tsv"));
        String plan =
                Files.readString(root.resolve("m3/build/a3/plan.tsv"));
        assertTrue(plan.contains("JEP-534"));
        assertTrue(plan.contains("\tSYSTEM\t"));
        assertTrue(plan.contains("PACK-A3"));
        assertTrue(plan.contains("CAT-SEARCH-1-LEETCODE"));
    }

    @Test
    void challengeEvidenceCannotWidenAuthorityOrReorderPlatforms()
            throws Exception {
        Path backports = root.resolve("m3/backports");
        Files.createDirectories(backports);
        minimalCoreCatalogues(backports);

        Path challenge = backports.resolve("CHALLENGE_CATEGORY_EVIDENCE.tsv");
        Files.writeString(
                challenge,
                """
                category	source	review_order	problem_count	representative_ids	evidence_only	source_repository	source_path	source_blob
                SEARCH	HACKERRANK	1	1	find-a-string	true	hsoliwal/com.synexia	problem-catalogue.tsv	f29f61ff48d06a42bd7a532c2973a6beb986d852
                """);
        assertThrows(java.io.IOException.class, () -> A3Plan.load(root));

        Files.writeString(
                challenge,
                """
                category	source	review_order	problem_count	representative_ids	evidence_only	source_repository	source_path	source_blob
                SEARCH	LEETCODE	1	1	28	false	hsoliwal/com.synexia	problem-catalogue.tsv	f29f61ff48d06a42bd7a532c2973a6beb986d852
                """);
        assertThrows(java.io.IOException.class, () -> A3Plan.load(root));

        Files.writeString(
                challenge,
                """
                category	source	review_order	problem_count	representative_ids	evidence_only	source_repository	source_path	source_blob
                SEARCH	UNKNOWN	1	1	x	true	hsoliwal/com.synexia	problem-catalogue.tsv	f29f61ff48d06a42bd7a532c2973a6beb986d852
                """);
        assertThrows(java.io.IOException.class, () -> A3Plan.load(root));
    }

    private static void minimalCoreCatalogues(Path backports) throws Exception {
        Files.writeString(
                backports.resolve("JEP_CATALOGUE.tsv"),
                "release\tjep\ttitle\tdomain\tdisposition\treason\tsuperseded_by\n");
        Files.writeString(
                backports.resolve("UPSTREAM_CHANGE_SEEDS.tsv"),
                "release\tjbs\ttitle\tcomponent\tdisposition\treason\tupstream_commit\n");
        Files.writeString(
                backports.resolve("COMMUNITY_CAPABILITY_CANDIDATES.tsv"),
                "capability_id\tplane\tcandidate\tevidence_type\tevidence_ref\tupstream_join_key\tpackaging_candidate\tstatus\tselected_for_distribution\tnext_proof\n");
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
