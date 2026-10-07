// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void plansJepJbsAndCommunityRowsWithoutErasingDisposition()
            throws Exception {
        Path backports = root.resolve("m3/backports");
        Files.createDirectories(backports);

        Files.writeString(
                backports.resolve("JEP_CATALOGUE.tsv"),
                """
                release	jep	title	domain	disposition	reason	superseded_by
                24	485	Stream Gatherers	library	candidate	final API\t
                27	534	Compact Object Headers	vm-gc-runtime	candidate-high-risk	VM change\t
                27	533	Structured Concurrency	library-runtime	hold-preview	preview\t
                24	486	Disable Security Manager	compatibility-security	reject-compat	incompatible\t
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
                backports.resolve("ALGORITHM_CATALOGUE.tsv"),
                """
                atom_id	category	technique	disposition	scope	target_owner	leetcode_ref	hackerrank_ref	geeksforgeeks_ref	github_ref	github_license	reuse_policy	next_proof
                ALG-PREFIX-Z	STRING_SEARCH	Z_PREFIX	candidate-adapted	FILE	m3/algorithms/src/com/m3/algorithm/M3PrefixZ.java	https://leetcode.com/problems/sum-of-scores-of-built-strings/	https://www.hackerrank.com/challenges/string-similarity/problem	https://www.geeksforgeeks.org/dsa/z-algorithm-linear-time-pattern-searching-algorithm/	https://github.com/hsoliwal/com.synexia	Apache-2.0	ADAPT_PERMISSIVE	prove UTF-16 owner reuse
                """);

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
                "reject-compat",
                require(rows, "JEP-486").disposition());
        assertEquals(
                A3Plan.Lane.ADAPT,
                require(rows, "ALG-PREFIX-Z").lane());
        assertEquals(
                A3Plan.Kind.ALG,
                require(rows, "ALG-PREFIX-Z").kind());

        A3Plan.write(root, Path.of("m3/build/a3/plan.tsv"));
        String plan =
                Files.readString(root.resolve("m3/build/a3/plan.tsv"));
        assertTrue(plan.contains("JEP-534"));
        assertTrue(plan.contains("\tSYSTEM\t"));
        assertTrue(plan.contains("PACK-A3"));
        assertTrue(plan.contains("ALG-PREFIX-Z"));
        assertTrue(plan.contains("\tALG-PREFIX-Z\t"));
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
