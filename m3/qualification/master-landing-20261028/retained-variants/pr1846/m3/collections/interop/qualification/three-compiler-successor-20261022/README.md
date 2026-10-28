<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21: three compiler-atom refinements over an immutable collection substrate

M3JDK21 is the **standalone Java/JNI product**, not stock JDK plus an M3
adapter layer. Its original parallel dynamic arrays, selected physical
`int[]` offsets, byte/bit masks, filtered and stable sorted views, and JNI
mask selection remain unchanged in this work.

## Failed full five-provider integration test

The exact-head product run
[39765922097](https://github.com/hsoliwal/M3jdk21/actions/runs/39765922097)
passed the original source census, original OpenRewrite recipe test phases,
and compiled all 32 production interop Java classes. The Java 21
`maven-compiler-plugin:testCompile` then rejected these test-local atoms:

| Original test | Bounded correction | Reason |
| --- | --- | --- |
| M3ApacheCollectionsTest | `@SuppressWarnings("deprecation")` on the one Apache Commons Collections 4.5 Predicate field | Preserve real Apache API compatibility without weakening global `-Xlint:all -Werror` |
| M3FiveLibraryKeyRangesTest | `import java.util.Map;` | Needed for its existing `Map.of` assertion |
| M3FiveProviderFlagSelectionTest | Use existing `M3LibraryCollections.<String,Integer>springMultiValueMap()` | `M3FiveProviderDirectSdk` is not a shipped M3JDK21 owner; the canonical existing factory returns the same parent-owned `M3SpringArrayMultiValueMap` |

Each Java source change is **exactly one replacement atom**. The canonical
Maven/OpenRewrite Java LST recipe, preimages and source outputs are in
Synexia under
`com.synexia.m3.M3Jdk21ThreeInteropCompilerRepairs20261022`.
No assertions, `@Test` method names, Java public APIs or production
Java/JNI source files are deleted or renamed. LeetCode/HackerRank/GfG
array/sorting categories remain references; no donor solution is imported.

## Strictly additive source history

The previous M3JDK21 snapshot
`31c8621d5fcad43708edada2ceb418af1f5e9e6c`
owns an already admitted **76-file interop source inventory** and the
**44-source/158-JUnit-method test inventory**. Its verified 36 historical
owners, seven old→revised owners, 40 additions, one earlier Java21 syntax
fix, and four source/method ledgers are immutable evidence.

This receiver adds a *separate* `receipts/changed-current-03.tsv` containing
exact old/new SHA-256 and Git blob identities for the three test refinements.
The `verify_current_three.py` source gate:
1. Replays the untouched 36+7+40 source verifier in the *preceding checkout*
   using the historical authoring commit `a720780b1a5ab41ea27182b9b7ad873efdf533b4`.
2. Requires identical 76 tracked paths between the prior/current product trees.
3. Confirms 73 sources are byte-identical; the remaining three must match the
   exact authorized before/after source images and retain Apache-2.0 provenance.
4. Rejects any changes to preceding immutable 36/40/158 receipts and
   verification sources.
5. Parses old and new JUnit source method names for the three revised files
   and requires exact equality. The unchanged prior 44-source ledger still
   enumerates **158 JUnit test identities**.
6. Only after the real original `mvn verify` succeeds, requires the original
   full Surefire verifier to find every one of those 158 JUnit methods once,
   with no missing/extra cases, failures, errors or skips.

Eight additional negative tests reject missing or symlinked revision
receipts, extra/duplicate/reordered paths, altered SHA-256/Git identities,
wrong prior-generation roles, forged recipe identities, and fabricated CI
acceptance. No wildcard source hash, test-count reduction, code bypass or
fallback to a competing transformation engine is admitted.

## Boundaries and acceptance

Synexia alone owns the reusable Maven/OpenRewrite transformation, source
templates, donor catalogue, and deterministic recipe/fixed-point tests.
M3JDK21 receives only exact generated Java test source, CI/QA scripts,
provenance, and executable product contract tests.

The unchanged original product compile, full JDK/Guava/Eclipse
Collections/Apache/Spring JUnit, and available JNI mask-parity gates remain
required. This is **not** proof of full JDK bootstrap or SWT/EMF/JDT
compatibility or a performance improvement. Promote only after exact-head
product and Synexia CI evidence is green; do not rebase Synexia `develop`.
Git stores the published PR; M3File/M3AST retain the working delta/DAG model.
