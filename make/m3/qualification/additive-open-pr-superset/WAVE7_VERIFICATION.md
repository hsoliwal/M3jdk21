<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 wave 7: exact source custody and qualification holds (2026-11-03)

**DRAFT/HOLD. This receipt documents a review-branch source merge, not a qualified release.**

## Frozen branch heads

- Existing product-feature ancestor [#2359](https://github.com/hsoliwal/M3jdk21/pull/2359): `360172d689d67d7e433536fb08b579dcb206f32e`, which already contains the previous wave 6 [#2381](https://github.com/hsoliwal/M3jdk21/pull/2381) source merge.
- New draft [#2385](https://github.com/hsoliwal/M3jdk21/pull/2385), source head before this passive receipt: `4b1e653715ed252daceeb97d6b8d01fd3e7434ba`.
- Branch: `m3/additive-current-source-wave7-20261103`.
- Preservation ref: `preserve/m3jdk21-product-2359-before-wave7-20261103` at `360172d689d67d7e433536fb08b579dcb206f32e`.
- Review branch before SmartBuild: `preserve/m3jdk21-wave7-before-smart-build-20261103`.
- Review branch before Windows CI: `preserve/m3jdk21-wave7-before-windows-longpath-20261103`.

## Actual two-parent GitHub source transports

- Original PR [#2183](https://github.com/hsoliwal/M3jdk21/pull/2183), immutable head `c50316c067a7625652393f0100ee77ce73d17e63`, through [#2382](https://github.com/hsoliwal/M3jdk21/pull/2382): merged `1d4396b274e1cd7ef5c7e06a44d566db77543350`. Three paths, two additions and one 57-line java.base ProgressMonitors extension. Preimage and merged output Git blobs matched the donor exactly. Original canonical Synexia source-changing recipe [#14627](https://github.com/hsoliwal/com.synexia/pull/14627).
- Original PR [#2195](https://github.com/hsoliwal/M3jdk21/pull/2195), immutable head `514546be9e19816f469b62c37005c9cb7743a3d8`, through [#2383](https://github.com/hsoliwal/M3jdk21/pull/2383): merged `343feb204de3be8eb968a4ad79ca20e33e80857c`. Five added files, no source replacement. Java receiver Git blob matched donor. Optional build delegation; NOT a mandatory java.base dependency.
- Original PR [#2246](https://github.com/hsoliwal/M3jdk21/pull/2246), immutable head `f524094a4d790bb1c25109c6b3065d49e2e9dbdf`, through [#2384](https://github.com/hsoliwal/M3jdk21/pull/2384): merged `4b1e653715ed252daceeb97d6b8d01fd3e7434ba`. Two paths, one additive 4-line Windows workflow change and one new pin-drift record. Modified workflow's old Git blob was checked byte-identical at original source and immediately prior to merge.

For every transport: original donor HEAD was pinned, regular GitHub **merge** (not rebase/squash/force), actual two-parent commit, exact original (path,status) delivery, and **zero source deletions**. The three donor sets have **10 distinct changed paths**, exactly matching the complete paginated PR #2385 file list with **zero missing, extra, or duplicate** entries.

A separately committed `WAVE7_SOURCE_CENSUS.tsv` contains all 10 donor path/head/status mappings. This is a path-level census and selected source-blob comparison, NOT exhaustive content/mode proof of the entire prior #2359 tree.

## Explicitly held donors and correctness issues

- FastDoubleParser/Jackson [#2233](https://github.com/hsoliwal/M3jdk21/pull/2233) modifies 30 vendor/overlay files; its own donor receipt reports a long hexadecimal significand rounding difference (example `0x8Db1Ffd473B83a5fp196`) associated with `java.base` parser absorption [#2215](https://github.com/hsoliwal/M3jdk21/pull/2215). It requires actual Java/Jackson differential oracle, exact source/licence admission and original Maven shading/ABI gates; **not merged**.
- Public [#2320](https://github.com/hsoliwal/M3jdk21/pull/2320) introduces the separate `toAdpater` compatibility alias and HotSpot virtual slot; its source/ABI approval is separate. **Not merged**.
- Public fork-local recipe duplicates and overlapping existing owner files must be reconciled through canonical Synexia source-sealed recipes before release.

## Remaining gates

The account-wide `hsoliwal/*` open PR frontier is incompletely enumerated; a previous 819-open-PR count was a non-atomic snapshot. Full exact per-repository PR census/patch DAG, all source blobs and executable modes, Java21 Maven/OpenRewrite fixed-point, source-built JDK/jtreg/HotSpot/JNI/C17/ABI/class unloading/GC/benchmarks, and hosted CI on the resulting wave7 HEAD are **NOT QUALIFIED**. Do not merge to protected default branches or make source-equivalence claims based solely on Git ancestry.

**Master and Synexia develop remain outside this source-transport operation.**
