<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Bounded regex preparation and collection-owner review

This catalogue connects existing M3JDK21 owners to problem categories and reviewed external references. It is pinned to product commit `c831c4a77f0e3e10aac94178217efda882ba4be5`. The four capability records remain pending global/metadata/publication gates. Their exact current-product receiving, installed-image behavior and resource evidence is now bound in the [public qualification summary](../../qualification/regex-language-mastery-20261021/QUALIFICATION.json), including matched failures and adverse observations. The final nullable-prefix source and prior-candidate lineage are hash-bound in the JSON. The global phase remains `STRING_QUALIFICATION`; family and repository completion remain false.

`CATALOGUE.tsv` reuses the eight-column layout of the existing [indexed live-view catalogue](../indexed-live-views-20261021.tsv): stable row ID, category, target owner, candidate, source, decision, cost and contract evaluation. `CATALOGUE.json` adds exact product preimage/candidate hashes and external source/license identities. These are data files, not a new search engine, executable admission manifest or competing capability registry. Stable capability IDs belong to `migration.records` in [the main naming map](../../../docs/name-mapping.json).

## Candidate boundaries

| Proposed capability ID | Existing owner | Bounded change |
| --- | --- | --- |
| `m3.regex.common-class-precompute` | `M3StringFacts`, `M3StringRegexDp` | Two primitive masks capture fourteen explicit class predicates during existing preparation. Supported warm decisions reuse exact known facts without rescanning text. |
| `m3.regex.pattern-boolean-unicode` | `M3PatternQuery.BooleanPlan` | The existing compiled-node chain records actual BMP-unit versus code-point consumption and keeps both possible frontiers for supplementary input. Scalar and wide ASCII paths remain. |
| `m3.token.regex-occurrence-reuse` | `M3TokenPredicateCache` | Owner-local exact row/token results for immutable String input; one recent primitive word and a lazy bounded primitive table. Mutable sources and vocabulary-backed regex rows remain uncached. |
| `m3.collections.indexed-array-growth-atomicity` | `M3IndexedArray` | Allocate all seven replacement planes before publishing any owner field. A failed copy must leave the previous owner state intact. |

The fourteen common-class predicates are digit, word, space, lower, upper, alphabetic, alphanumeric, hexadecimal, ASCII, lowercase hexadecimal, uppercase hexadecimal, dot, horizontal space and vertical space under the explicitly recognized grammar. “Common” names the curated set. No usage-frequency dataset or claim that these are the most frequent regexes is supplied. Cold and unsupported expressions continue through the existing matching paths, with Pattern fallback preserving validation and exceptions; the existing dot/star DP remains part of those paths.

The Unicode candidate supports supplementary pairs and lone surrogates within the existing at-most-256-atom Boolean chain grammar. It does not add a general regex parser. Unsupported graph shapes and flags retain their existing admission decisions. The original Matcher remains authoritative for captures, positions and observable match state. The optional native ASCII oracle still refuses non-ASCII input. When every admitted atom is nullable and the live first star has general code-point width, the final candidate reuses the exact complete frontier across the prefix accepted by that predicate, then resumes the unchanged recurrence at the first rejected code point. It adds no retained metadata or admission limit.

The collection change is failure consistency for an existing synchronized owner. It preserves that synchronization and supplies no new lock-free or concurrent-publication guarantee or array/collection backend promotion. Old and replacement arrays are simultaneously reachable until all copies finish; the resulting owner layout and successful allocation count do not by themselves establish unchanged peak live memory.

## Fast category lookup

Use ordinary TSV search from the repository root:

```sh
rg -n -i 'unicode|surrogate|token|growth|offset|mask|filter|sort' m3/collections/algorithm-catalog/regex-language-mastery-20261021/CATALOGUE.tsv
rg -n 'm3\.regex\.common-class-precompute|REFUSED' m3/collections/algorithm-catalog/regex-language-mastery-20261021/CATALOGUE.tsv
```

Rows follow the requested LeetCode → HackerRank → GeeksforGeeks discovery order. The platforms describe problem shapes: whole-input recognition, wildcard grammar, first/count/all literal matches, dictionary segmentation, weighted multi-pattern occurrence counts, classes/runs, stateful Pattern behavior and language cardinality. Similar categories do not make those contracts interchangeable. In particular, wildcard star is not postfix repetition; a count is not a first index; language cardinality is not one-input recognition; and a restricted challenge is not a general Java Pattern oracle. No challenge solution, editorial or submission code is included.

The collection rows reuse the existing product catalogues for dynamic arrays, physical offsets, state masks, rank/select, stable indirect sorting and live versus detached views. Their historical verification statements retain their original scope. Reading or indexing those rows supplies no new behavior, native or resource proof. Live predicates, comparator effects, mutable referents, generations, stale handles and logical-versus-physical coordinates continue to constrain reuse.

## External reference decisions

| Reference | Exact reviewed revision | Licence and disposition |
| --- | --- | --- |
| TheAlgorithms/Java KMP, Aho-Corasick and Rabin-Karp | `d9b86d1992b8bd4cadbd832822c792628117e69d` | MIT source/license inspected. Reference eligible with notices; no implementation imported. Result shape, ownership and empty/null contracts require separate target review. |
| google/re2j Pattern and README | `951a6159fdc0a4931593c70442926f86031edf40` | BSD-3-Clause source/license inspected. Refused as a Java Pattern replacement or oracle because documented API/semantic differences matter here. |
| doocs/leetcode licence | `5f6429aec9a7fc90b29c984246b617aad7394b2a` | CC-BY-SA-4.0 licence inspected; no solution/editorial inspected or imported. Any adaptation needs its own redistribution review. |

Exact public file URLs, license URLs, Git blob IDs and SHA-256 identities are in the JSON and linked TSV rows. Reference eligibility is not code-import authorization or a claim of blanket license compatibility. No externally licensed implementation is admitted by this catalogue.

## Publication and evidence

This product packet contains first-party product descriptions and public source references. It excludes private convergence source bodies, raw donor mirrors, credentials, local absolute paths and build outputs. Canonical recipe execution and exact product receiving remain separate obligations; a product receipt can link their reviewed identities without relocating their private implementation into `java.base`.

Retain the current source-file notices. `M3StringFacts`, `M3StringRegexDp`, `M3PatternQuery` and `M3IndexedArray` retain their existing GPLv2/Classpath-exception notices; `M3TokenPredicateCache` retains its existing Apache-2.0 component notice. The existing public-target and intake policies remain authoritative for any future source transfer.

This catalogue does not itself execute tests. Read the linked public qualification summary and its exact scoped receipt identities for the current results. It claims no universal speedup, whole-heap result, complete Unicode/regex coverage, full JDK compatibility or family completion.
