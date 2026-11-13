<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 wave 6 — exact source custody (3 November 2026)

Status: **DRAFT / SOURCE PATHS VERIFIED / PRODUCT QUALIFICATION HOLD**.

## Frozen commits and actual merges

- Source baseline is existing product integration [#2359](https://github.com/hsoliwal/M3jdk21/pull/2359) at `16766fbe072ab56bb56901c12cdba9bbc928bbb7`, including the earlier [#2361](https://github.com/hsoliwal/M3jdk21/pull/2361) wave 4 source history as a real merge parent.
- This independent review branch was created from that exact SHA and preserved at `preserve/m3jdk21-2359-before-wave6-20261103`.
- Four non-squash GitHub two-parent merge commits were created, each into **only** `m3/additive-current-source-wave6-20261103`:

- Original [#2188](https://github.com/hsoliwal/M3jdk21/pull/2188) at `5e7f8d5a3be8de9b365f2dcab58f0dc30da0bbc0`, original-base `bb66a8c16642a4e898dbc5fc7c3af0d5ed572136`, transported via original preserved Git head at merge commit `f0895670a9d8d71c1bcf1eae9101eeb19344ea11`.
- Original [#2228](https://github.com/hsoliwal/M3jdk21/pull/2228) at `1d944909fee6e61983e3bea0bd84e9de72292a44`, original-base `52fb81c483c74448a12eecb63aa9465e42cdb82c`, transported via original preserved Git head at merge commit `d200566433d40a7be66316329e00532515543e49`.
- Original [#2222](https://github.com/hsoliwal/M3jdk21/pull/2222) at `00aa73734cdc8a713927568debdd8696ea825a7a`, original-base `52fb81c483c74448a12eecb63aa9465e42cdb82c`, transported via original preserved Git head at merge commit `8e8a674fdbfe4162bc8ed6ad2c4aef9cd902f643`.
- Original [#2220](https://github.com/hsoliwal/M3jdk21/pull/2220) at `04a05bb33c432baa502050142833bcfd63792bae`, original-base `315cd18d0092f70c212223bb6af1f4366591ed1b`, transported via original preserved Git head at merge commit `3d1e90e1dc835e2592ba64758442c3360149733f`.

These include 16 javac naming/census added files, four interop source-custody modifications, one CPU/M3File fixture registration workflow correction, and LeetCode-705-derived first-party `M3LongHashSet.add(long)` private atom plus its three qualification/tests files. LeetCode is a **problem category/test-shape reference**, not copied implementation source. Other HackerRank/GeeksforGeeks/GitHub donors remain subject to their own pinned licence and contract catalogue.

## Verified file-set closure

The complete paginated GitHub file list for draft PR2381 contained **25 unique paths** against its exact existing-feature baseline. The four independently fetched donor Git compare records have **25 distinct original paths** with original `added`/`modified` status and no `removed` path. The two sets are **identical**: 0 missing, 0 extra, 0 duplicates. The per-file mapping and source-head SHA is in adjacent `WAVE6_SOURCE_CENSUS.tsv`.

The original source preimage Git blobs were checked for all modified donor paths before merge:
- Four interop Python verifier/test files: **4/4 exact**.
- M3 CPU/file workflow: **1/1 exact**.
- `M3LongHashSet.java` original blob `3a64a039f4bdbb8c2ce6d995a7954f2075b1152c` exact.

Each transport's Git merge parents and before→after filename/status diff were separately checked; zero deletions observed. **This is not an exhaustive postmerge per-file 25-blob/mode audit or a Maven/JDK/JNI runtime proof.**

## Remaining HOLD

The wider `hsoliwal/*` GitHub PR frontier is incompletely enumerated (a prior non-atomic snapshot counted 819 open PRs). Do not treat this four-donor integration as all PRs merged. Full current-master state, Source/Binary ABI, Java21 SDK/JUnit/HotSpot/JNI/native C, receipt-bound OpenRewrite fixed point, class loading, GC, whole integration CI, and licence/provenance checks are still REQUIRED on the exact merged head.

No `M3jdk21` master or Synexia `develop` mutation is authorized by this PR. Do not rebase, squash, force-push, weaken historical source seals or approve the disputed `Object.toAdpater` alias / seventh vtable slot. Source edits continue to be owned by canonical Synexia recipes.
