<!-- SPDX-License-Identifier: Apache-2.0 -->
# Range boundary consolidation, 2026-10-29

Canonical source-sealed OpenRewrite recipe: [Synexia PR #14106](https://github.com/hsoliwal/com.synexia/pull/14106).
Target baseline `master` commit: `052ebcf7a26352c0285251eb671ee6c3640fd3ff`. Exact preimage Git blob:
`327a56f12e09ae82ea0d0f10bdcf3a0628f1636f`; SHA-256:
`8aa30b4b23c638b53f5fe9e6f895bb3205a51af1df0bf2afdefb38d4c21ab090`.
Sealed postimage SHA-256:
`b0773d86378844ca38e9a299f27918bae7df2494676aacda8baea25e96b576c9`.
New JUnit source SHA-256:
`2426af84aa8fe86e20c246db9ed7880b48be12d038e18e92937a2b68e2674ba9`.

## Changed owners

- `m3/collections/src/main/java/com/m3/collections/M3LongRangeSet.java`: replace linear first/last range scans with existing signed-long
  `M3LaneSearch` lower/upper bounds, and compute union extrema from the
  boundary slots. Disjoint sorted nonadjacent intervals guarantee equivalence.
- `m3/collections/src/test/java/com/m3/collections/M3LongRangeSetBisectionTest.java`: independent 20,000-operation boolean-lane oracle, signed extrema,
  adjacency/empty/reversed interval edge cases, capacity and callback-epoch behavior.
- One passive problem-category catalogue; no new runtime dependency, name
  migration, JDK package, JNI symbol or array-owner change.

## Evidence and remaining holds

- **PASS**: independent stock OpenJDK 21.0.12.1, `--release 21 -Xlint:all
  -Werror`, 360,000 deterministic original-vs-candidate add/remove
  transitions across signed-long extremes; physical aligned lane contents,
  capacity, size and sorted/nonadjacent invariants compared after each step.
- **NOT RUN**: actual canonical OpenRewrite SDK compilation/replay/fixed-point;
  exact-source full target Java21/JUnit reactor; M3JDK21 image/jtreg/JNI;
  JMH/production workload benchmarks and multi-platform gates.
- Asymptotic boundary discovery is O(log r) versus O(r), but total edits
  remain O(r) in the worst case because packed lanes move. No wall-time
  speedup is claimed.

Keep this PR **draft** until executable evidence is available. No force-push,
rebase, squash, class rename or direct master modification.
