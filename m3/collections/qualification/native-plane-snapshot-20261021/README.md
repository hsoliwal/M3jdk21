# M3JDK21 first-class JNI: bounded shared-state plane snapshots (2026-10-21)

**Product and source authority:** M3JDK21, with its existing `m3/collections/src/main/native/collections/m3_index_path_masks.c` JNI C17 owner and `M3IndexPathMasks.java` Java public contract. This is a stacked successor of [PR #1663](https://github.com/hsoliwal/M3jdk21/pull/1663), not an M3 + JDK compatibility wrapper. `com.synexia` supplies qualified reusable transformation recipes and algorithms; Java/HotSpot/JNI ownership and acceptance remain in M3JDK21.

## Why native snapshots can help

The parent JNI selector already batches 256 contiguous logical offsets and rows and caches up to eight stage handles. It still calls `GetIntArrayRegion` for every logical row/stage to read one state word, even when many logical occurrences repeatedly address the same physical slot. Android ART's native JNI implementation implements primitive region transfers; its official JNI guidance recommends minimizing marshalling and the number of boundary crossings. The new **single replaceable C17 behavioral atom** snapshots *eligible small state planes* once and then performs their full-width mask computations entirely against native stack memory.

- Only for **1–8 cached stages** and **at least 512 logical positions**, when *every* state plane has length **at most 256**. The existing higher-level JNI dispatcher still selects only cached stage counts with >=8 rows.
- Maximum native scratch for state words: **8 x 256 x sizeof(jint) = 8192 bytes per call**; the original 256-row logical-offset/row buffers are unchanged. No `malloc`, critical array pin, retained Java heap address or Java object reference outside the call.
- Use `GetIntArrayRegion` once per eligible plane; `m3_matches_cached` performs the original per-stage bounds and required/excluded state checks against the snapshot. A failed native copy preserves its pending JNI exception and releases each local stage reference before returning.
- Short scans, any plane longer than 256, 9+ stages, all existing signed/negative/strided paths and ABI symbols retain their established behavior. The snapshot is call-local and never published or retained between calls.
- The Java API already requires borrowed source arrays to remain stable for an operation; no new concurrency or mutation guarantees are claimed. In particular, this fast path does not attempt to detect unsynchronized concurrent mutations during its two native passes. Output arrays remain detached and exactly sized.

## Serial source-sealed Maven/OpenRewrite transformation

The project uses **one** native owner, exact before/after source SHA-256, and separate Java/C recipes. The native recipe is `com.synexia.rewrite.M3NativePlaneSnapshot20261021`, using the existing `M3HashPinnedJavaSnapshotRecipe` with `lst:false` for this exact C17 owner. The test recipe `com.synexia.rewrite.M3NativePlaneSnapshotTest20261021` uses `lst:true` on a Java 21 test.

- Previous #1663 native postimage / this PR preimage: `854924fb4ac65949bd4d77230040db1ec21dce0a65ed4722121fd9f0f867274d`.
- This PR native postimage: `8852b10d79c9c134019965cfc2c915fdb993ace3fb39cf26e859154d0439c0e3`.
- Test source postimage: `ede33293d17330a742084c1f5e935943e164d5b45410758a868538d90b96a17b`.

Original source is pinned as an unchanged parent template, not copied to a new production owner. Source drift refuses rather than overwrites modifications. Actual Maven/OpenRewrite recipe execution, fixed-point replay, and source-built dependency admission require separate evidence; manifest equality alone cannot claim them.

## Java 21 / checked JNI native proof

```bash
bash m3/collections/qualification/native-plane-snapshot-20261021/verify.sh
```

The focused proof checks the source and recipe hashes, uses Java 21 `--release 21 -Xlint:all -Werror` to compile the **actual M3JDK21 owner**, and builds the original #1663 JNI C code and this successor as separate native libraries with C17 `-Werror` and UBSan. It runs the new deterministic repeated-physical-slot suite **plus** #1663's bulk-path and #1545's stage-cache suites against each binary with `-Xcheck:jni`, comparing full deterministic transcripts.

Tests span 0/1/511/512/513/1024/2048 logical rows, physical capacities 1/7/127/255/256/257/1024, 1/4/8/9 stages, contiguous and reverse/strided paths, signed row IDs, duplicate indices, zeros, contradictory and sign-bit masks, unequal plane lengths, and invalid physical geometry. Both snapshot-eligible and fallback cases are required.

## Donor catalogue and acceptance

See `m3/collections/algorithm-catalog/native-plane-snapshot-20261021.tsv` for searchable problem categories, complexity, selection rationale, rejected alternatives, source URLs and rights boundaries. Android ART, DEX and Android JNI guidance are **reference designs**, not source copied into M3JDK21; LeetCode/HackerRank/GeeksforGeeks contribute algorithm patterns only.

Keep this change a **draft** until GitHub's actual Java/C17 parity proof, source/recipe fixed-point, complete collections/JNI build and native memory tests succeed. Android ART/ARM64 device startup, PSS/RSS, GC, battery, thermal performance and full Java 21 jtreg/JCK remain open. Neither a speedup nor Android application compatibility has been established by source inspection.
