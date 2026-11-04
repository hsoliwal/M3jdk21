# Source-only M3JDK21 recovery + SimHash union — verified 2026-11-04

**Transport PR:** https://github.com/hsoliwal/M3jdk21/pull/2462 — MERGED to isolated source branch, NOT `master`.

**Original immutable heads:** #2392 `000299bb3b4d35d4c638d820e02beb9a22865f9b` (30 added source/receipt paths); #2407 `e6782bb9f1ebb134a8a00025bd9c46a905d6d6c6` (3 added source/test/receipt paths). Both start at the same frozen master base `172e39d5e69a8f7775c25ad1ce2bfaf2bf5d1246`. Source path intersection was zero. Both original donor PRs remain drafts and source heads were not rewritten.

**Native GitHub method=merge SHA:** `b58574d283d7317b10e7fa3be928a1d57e744b48`.

**Verified from GitHub:**
- First-parent receiver immediately before merge: `44398a16a770160d860eba0bab3ad0f087f63262`.
- First-parent resulting delta: **3 ADDED, 0 MODIFIED, 0 DELETED, 0 RENAMED**.
- Ancestry compare from each original source head to resulting merge is ahead with behind=0; both histories retained.
- Stage branch equaled exact merged commit before adding source-proof receipts.
- GitHub original-donor vs merged-file blob SHA-1 readbacks: **3/3 EXACT**, zero missing/mismatched. Table: `EXACT_3_SOURCE_BLOBS.tsv`.
- Source file identities retained: `M3IndexDbChunkSimHash.java`, `M3IndexDbChunkSimHashTest.java`, plus their original qualification receipt; prior 30 recovery files untouched.
- No force-push, rebase, donor deletion, squash, live ABI/naming rewrite, or master/product branch update.

**HARD HOLD for M3JDK21 promotion:** actual canonical com.synexia Maven/OpenRewrite `Recipe.run`/`Result.getAfter()`, fixed point/refusal; strict Java21/JUnit/JNI C17 ABI/JVMTI/GC, complete standalone JDK/JMOD/JTReg/JCK, license/NOTICE/privacy, benchmarks and hosted exact-head CI have NOT been proven on the merged stage.

`ALL_PRS_MERGED=false`. `PRODUCTION_QUALIFIED=false`. This is an exact source-history superset, not a released or functionally validated JDK.
