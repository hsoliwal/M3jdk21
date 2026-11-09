<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Composed integration catalogue

`composed-integration-20261021.tsv` is a UTF-8, tab-separated catalogue of 43
decisions. Its first row is the column schema; every subsequent row records a
problem category, operation, incumbent, candidate, exact source, disposition,
contract analysis, complexity/storage, performance scope, rights and verification.
Search categories and operations with `rg`, or read it with any TSV parser.

The original 31 decisions derive from the canonical combined-integration task.
The receiving edition retains their source lineage and adds three finite resource
observations and nine receiving/refusal/composition decisions. Verification fields
distinguish source transformation proof, isolated candidate proofs and actual
built/linked JDK tests. Rejected candidates remain searchable and do not become
qualified by association with accepted rows.

This catalogue and its first-party explanatory text are Apache-2.0. That notice
does not relicense linked donors, code templates, historical fixtures or emitted
JDK sources. Their per-file notices and source-custody records control. Historical
headerless and copyright-only files retain their recorded status. LeetCode,
HackerRank and GeeksforGeeks pages were used for discovery and review categories;
no solution, editorial or tutorial implementation was copied from them.

See `m3/docs/composed-integration-20261021/README.md`, `M3_INVARIANT.md`,
`SOURCE_RECEIPT.json` and `FILE_PASSES.tsv` for the receiving lineage, exact
qualified frontier, runtime evidence and continuation rules. The canonical
recipes and donor evaluations are reviewed in
[Synexia #12680](https://github.com/hsoliwal/com.synexia/pull/12680).
