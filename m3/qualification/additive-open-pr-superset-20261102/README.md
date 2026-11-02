<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 additive master-derived review superset — 2026-11-02

**Status: DRAFT / HOLD, not a JDK or JNI release.** The user requested "SUPERSET ADDITIVE MERGE ALL." This branch admits the four **currently qualified-for-source-convergence** and **pairwise path-disjoint** open PRs listed below. It does **not** represent every open PR across `hsoliwal/*` or the older [M3JDK21 #2255](https://github.com/hsoliwal/M3jdk21/pull/2255) 45,542-file landing, which has unresolved source/baseline and compiler holds. Neither `master` nor any original PR head may be force-pushed or overwritten.

## Frozen current master and exact donor inventory

Frozen `master@ce005d25b29fb6246e013bffb462afd5ed070362`. All four donors have exactly this Git SHA as their PR base and `compare(master,head)` reported **zero behind**:

| Open PR | Exact head | Source paths vs master | Changes | Intent |
|---|---|---:|---|---|
| [#2317](https://github.com/hsoliwal/M3jdk21/pull/2317) | `4586851d6c237052851b654c46c6611715ee6bd7` | 6 | 6 added | Java/JNI `Object.toAdapter` behavioral tests and exact provenance |
| [#2319](https://github.com/hsoliwal/M3jdk21/pull/2319) | `af8885c1229cc7916e605f3adc2715b8cbade315` | 18 | 9 added, 9 modified | First-party parallel array lanes, collection cursor/iterator behavior and tests |
| [#2321](https://github.com/hsoliwal/M3jdk21/pull/2321) | `ce7051f77ca08a0f1c8230f476a04bb143b4553e` | 12 | 2 added, 10 modified | Reactor impact, UTF-16 and native text-mask kernels with existing tests |
| [#2325](https://github.com/hsoliwal/M3jdk21/pull/2325) | `1edbbda245dea9a616feb37ecea43ef4c0ef050c` | 3 | 1 added, 2 modified | Java21 and C native varint packing-size refinement |

All **39 changed filenames are unique** across the four pinned PR head diffs. **0 deleted/renamed paths**, 0 unreviewed per-file collisions, and 0 implicit file moves. These are receipt and source-identity conditions, not proof of behavioral equivalence.

**Explicit exclusion/contract lock:** [#2320](https://github.com/hsoliwal/M3jdk21/pull/2320) would add a misspelled public `Object.toAdpater` alias and HotSpot behavior; the corrected public contract is `<T> T toAdapter(Class<T> clazz)`. Do not silently merge, introduce or rename public Java APIs. The API-affecting PR needs separate explicit unlock and compatibility proof. The older #2255 superset's Java naming, HotSpot, native, public API and build effects remain on independent HOLD; do not replace this current `master` tree with an old 45k-file fork.

## Additive serial merge procedure

1. Pin all four head SHAs, `master` and the 39 unique changed filenames; create the integration branch from `master`.
2. Publish **this document first**. Retain the complete source contracts, package/layout and ABI of the current `master`.
3. Merge original open donor heads **serially** into this new branch, using normal Git two-parent merge commits (never squash/rebase or `ours` history-only merges). Each donor must contribute exactly its 6/18/12/3 reviewed added/modified paths. Verify with compare after every merge that no previously merged path changed, that the donor's source blobs/modes match, and that both parents remain ancestors. On conflict or drift, stop and do not report success.
4. Preserve original PR review threads and heads. Open one **draft** superset PR into `master`; do not promote without actual JDK21 javac/HotSpot/jtreg/JNI, Maven, collections/arrays unit and cross-platform gates.

## Licensing and donor categories

LeetCode categories, HackerRank problem shapes and GeeksforGeeks problem families are classification/test references only; do not copy their editorials or solution code. Existing first-party Synexia/M3JDK21 collections/algorithms and tested JNI source are implementation owners. External GitHub donor source requires exact pinned commit/blob hash, SPDX/legal compatibility and independently qualified behavior before source integration.

## Required verification

Execute patch → lint → compile → JUnit/jtreg → JNI/C or C++ native ABI check → runtime/benchmark in that order, with immutable source hashes, change ledgers, compiler/test logs and no skipped suites. The GitHub-only source-tree integration does not substitute for these tests. **HOLD** means no release or merge into `master` until evidence exists, even when GitHub's mergeable flag is true.
