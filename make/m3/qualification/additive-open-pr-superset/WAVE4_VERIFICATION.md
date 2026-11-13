<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 Wave 4 — complete source-path custody, HOLD (2026-11-03)

**Source paths verified / product and all-PR convergence not verified.**

- Current-master source baseline: `172e39d5e69a8f7775c25ad1ce2bfaf2bf5d1246`.
- Source review HEAD before this passive receipt: `afa83dee734c54053915d2902e047a55581d9a58`.
- Draft review PR [#2361](https://github.com/hsoliwal/M3jdk21/pull/2361); no change to `master`.
- The full paginated GitHub PR file inventory contained **384 unique changed paths**, PR `changed_files=384`. The GitHub comparison API truncated this diff at 300 files and must not be used for complete coverage.
- The original 14 pinned donor PR file sets contained **390 distinct names**. **Six** already existed in current master through ObjectAdapter Java/JNI tests/receipts. The remaining **384** equal the complete reviewed PR file set, with **zero missing, extra or duplicates**.
- The accompanying `WAVE4_SOURCE_CENSUS.tsv` contains all 384 path-to-original-PR-to-head-SHA mappings, copied after independent readback of the dedicated [Google Drive source ledger](https://docs.google.com/document/d/1I05L0ZHgeKHLhFBWUZefC9F0dcVCbInyQouxiTRaoBU/edit?usp=drivesdk) at revision `AHj4eMSpCNLvGSRd3kdgSr8HDv4CrOmQcaA5eq0xFdWp3xiPlTM8GkNCW0czzoBqWaZp0HvQT3Httsm1OiSFYk92s9QftTErCkW43a_ETgQ`.
- This is a **complete pathname census**, NOT exhaustive content/mode-bytes proof. Specific source blobs were validated during individual source-carrying merges; full 384-blob/mode comparison and combined-head Java/JNI/JDK release tests remain pending.

## Exact donor PR and merge lineage

The first integrated source stack is [#2337](https://github.com/hsoliwal/M3jdk21/pull/2337), a seven-head source superset; its original nested donor histories are retained. Review-only two-parent transport merges preserve additional original PRs:

| Transport PR | Original donor | Merge commit |
| --- | --- | --- |
| #2355 | #2350 Spring ABI | `a7a909acdbbfd0c986d0fc2c48addabd025dab44` |
| #2356 | #2351 token Java/JNI wave4 | `7ef3a407c193dd5114638c6f2a6bbaf4ddca45e7` |
| #2360 | newer master/source | `f6eb30e4b5137df4bb73db8936a9b02308a53dbb` |
| #2362 | #2354 case/Liang Java/JNI | `9bce9d4705e3431a6243b8c0627409fea297a763` |
| #2363 | #2262 LC/HR/GfG lane search | `e4bddc3222e91295eb6a71197d84ee47f9b923ed` |
| #2364 | #2258 UTF-16 Java/JNI | `2643c93975558341d2fdeb0306ee14f0bf774a5b` |
| #2365 | #2251 mindex atom tests | `c8cbef635e86be3c2c68c3e120dc64e792a3920e` |
| #2366 | #2252 Swing atom tests | `44df1a136757af1b00056f22c5917fa22f0de6a1` |
| #2367 | later master/source | `f89362c62fdedb166d8a156972686bb1f28e91ec` |
| #2369 | #2234 structured logging | `572b00bf3919357668ae8e17130ffa7df81a9a3b` |
| #2370 | #2264 Inflater regression | `b09843442f4fd2cec6c3fec236d7e337717f99fc` |
| #2371 | #2253 arrays/collections CI | `78514c4678eed196e37458ef54e8e9aef9a60e84` |
| #2372 | #2237 javac specifications | `a472bfab7f0036c0047d1c39ce5eabeb510a9dcd` |
| #2373 | #2241 HotSpot parity | `b91f4c7f2df086a05e8c31adc8bacd34d96b1f5e` |
| #2374 | #2242 zero-mask JNI | `b15b60684f5e0f9af3056f32a48850dcd6af66a7` |
| #2375 | #2261 JBS backport inventory | `afa83dee734c54053915d2902e047a55581d9a58` |

Original donor PRs remain separately reviewable; source merges were non-squash and only targeted the review branch. Preservation refs were created before the source-bearing stages.

## HOLD / not all hsoliwal PRs

An earlier non-atomic search observed **819 open `hsoliwal/*` PRs**, but provider rate limits prevented two complete stable account-wide PR-head inventories. Many unresolved PRs overlap `M3IndexMaskCompactor`, the Object alias/vtable, mindex tooling, compiler modules, source manifests or older whole-source superset histories. They must be compared by exact source atom, NOT blindly overwritten.

Fast-search algorithms from LeetCode, HackerRank and GeeksforGeeks are indexed as category/test donors; GitHub source donors require pinned revisions and license gates. None of these sources are permission to copy challenge solutions.

**Before any release:** complete SHA/mode audit of all 384 source files, canonical Synexia OpenRewrite/JDT fixed-point execution on the merged head, source-built Java 21/jtreg/JNI/C17/HotSpot/GC/class-unloading/binary-ABI tests, provenance and full CI, plus new exact current-master no-loss proof. The extra `toAdpater` alias/seventh Object vtable from #2320 is excluded. No merge to default master/develop is authorized by this receipt.
