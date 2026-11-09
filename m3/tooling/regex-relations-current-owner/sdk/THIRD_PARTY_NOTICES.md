# Third-party notices: current-owner regex relation continuation

The recipe adapter, manifest kernel, qualifier and Synexia lineage are Apache-2.0. The M3StringRegexDp.java baseline/candidate and M3StringHierarchicalRegexTest.java fixtures retain their GPLv2 with Classpath exception headers. Packaging them as sealed recipe resources does not relicense those files under Apache-2.0.

The already-public M3DotStarRelations.java, M3StateRelation.java and M3StateRelation.c are Apache-2.0 target adaptations of Synexia MIndexDotStarRelations / MIndexStateRelation; original notices are retained. Exact donors, revisions, paths and the pre-code reuse decision are in FOSS_REUSE_DECISION.tsv and the outer source-binding.json.

The unchanged OpenRewrite adapter1c1b1b568d369f688f6953161636bf36b484bf5a and manifest kernel38df5f97bbeec8a8a8cfc77091d56bda925c07f2 provide transformation behavior. The unchanged Maven POMd79867bcc09ad6329e12418e15907e54b8ab025f retains OpenRewrite8.17.1 and JUnit5.10.2 dependencies. No challenge-site implementation or private dataset is copied. No new product runtime dependency, native ABI or alternative regex parser is introduced.

## Initial current-owner source draft, superseded before execution

This task imports the exact existing SDK from hsoliwal/com.synexia at 515f2f2153aa6afbc758da34c495c7223ee7997a (tree 955bcfbcfb09a72a327f3d541a74c1f1a45b733d). The Maven POM, two engines, adapter and named descriptor remain byte-identical. The actual M3JDK21 BEFORE is M3StringRegexDp.java blob 0f99affbec7a0d995e0a18ac4cf7f2e815da6e3b at d6941087a0453aa5e0b448e7d448d43dcc4c707b. The adapted output preserves every one of its 12,888 UTF-8 bytes and applies only the five original 3,352-byte donor insertions. The newer carry closure and CodeUnitCursor regions are preserved. Four helper/numeric JNI/jtreg custody owners remain exact.

The new qualification source remains Apache-2.0; the imported GPLv2+Classpath product/test fixtures retain their original headers and rights. No dependency version or source-qualified coordinate alias is substituted. Runtime application, matched-JDK execution, JNI equivalence and performance remain NOT_RUN. The pre-code decision is task PLAN.json at foundation 3429cd844962920a8952d09c641c6bb29c966a63 and its root FOSS_REUSE_DECISION.tsv row.

## Lazy dispatch and additive jtreg refinement, 2026-10-19

Root review decision e97c8b82cd305ac6e0f9e50390eb2539e362f5ce supersedes the initial five-insertion candidate. The revised DP contains four insertions and initializes Access only inside the eligible OOM-protected relation branch. The current 12,888-byte DP BEFORE remains exact, including its newer carry closure and cursor traversal. The existing 4,610-byte hierarchical jtreg BEFORE remains exact and receives four reversible additions: route proof imports and helper, a genuine eligible 33-atom production warmup, and the route-proof call. The additive route donor is 0a91c9d3266525152ef64990eeb09af4c005f23d with source binding 93c85e732116e08e39338af7a05e9f6847c59404 from the existing Synexia route-acceptance recipe. Its platform Proxy delegates to the actual adapter, counts root-source calls and is restored in finally. No new runtime API or dependency is added.

Both DP and jtreg derived fixtures retain GPLv2 with the Classpath exception. Three helper and numeric Java/C custody owners remain byte-identical Apache-2.0 imports. All original jtreg cases and both run configurations survive removal of only the four additions. Source review, authored counters and recipe replay do not qualify actual matched-JVM dispatch, JNI, CPU or retention behavior; all remain NOT_RUN.
