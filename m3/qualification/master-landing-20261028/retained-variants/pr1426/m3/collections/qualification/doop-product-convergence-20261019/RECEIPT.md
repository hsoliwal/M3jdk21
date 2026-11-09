<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 DOOP collection + java.base source convergence

**Product owner:** M3JDK21 `java.base` and its established `m3/collections` owner.
No M3-to-JDK runtime layer, duplicated object store, per-map entry-node
representation, or Synexia runtime jar is introduced.

## Dated two-parent additive merge

- Parent A: `9d3c0a6bfb790d033cc6753fd05c48597b3bb59c` (PR #1410,
  including original #1367 → #1362). This retains canonical M3
  `M3IndexedListSnapshot`, `M3GenericCollections`,
  `M3TreeMap`, `M3MutableFilteredViews`, `M3FilteredViews`,
  their original test suites, indexes, offset masks and live/immutable contracts.
- Parent B: `9444a38e70f2564c28fbe8d42930e47b4ec2e845` (PR #1416).
  This retains modified **actual** `java.util.EnumMap` and
  `java.util.IdentityHashMap` sources, jtreg-style oracles, and the
  source-specific CI and catalogue.
- The production PR paths are disjoint. Both original JDK map preimages
  on parent A have blob SHAs identical to `master`:
  `EnumMap.java=01cd736bbc1a9f9ea2dd8dbb298ff8819ad6b431`;
  `IdentityHashMap.java=77f06fb94100bd43d16ad9ba418a716c4e43778e`.
  Exactly seven parent-B changed file blobs were overlaid on parent A's
  Git tree, without changing any other blob or rewriting either parent.
- Merge tree `4113645d99e46eb65138ec15801b6bb1ddf9e3d5`,
  merge commit `489544371474e6d0c4c4ae212ad5236194bd22a0`.
  The merge has both parent commits and retains their additive histories.

## Additional primitive filtered/sorted predicate source reconciliation

A subsequent additive merge on this branch receives
[M3JDK21 #1413](https://github.com/hsoliwal/M3jdk21/pull/1413)
without overwriting any of the previous combined owners:

- Parent A `a48a2a7b16b9fdd05ca2339a32b6b499f2e7bdff`,
  the original combined product head.
- Parent B `45008413898bc27a4dc61c2bf19e1d433a84db01`,
  the original recipe-received balanced `M3LongPredicates` owner.
- Second additive merge commit `a80c4f3859fc73781b7bfc527c067759afc661e0`.
- Exactly three files transferred: original `M3LongPredicates.java`,
  original `M3LongDeepFilterSelfTest.java`, and its qualification
  receipt. These paths do not overlap the earlier merge and both
  existing Java source preimages were verified byte-identical to
  `master` before receiving the donor postimages.

The incumbent primitive predicate composition now maintains bounded
persistent AVL depth, exact left-to-right callback/short-circuit
behavior and stack-safe deeply nested filtered/sorted wrappers.
No per-row `Map.Entry`, second reference store, JNI export or public
API alteration was added. Synexia [#12408](https://github.com/hsoliwal/com.synexia/pull/12408)
remains the canonical recipe and catalogue authority.

The new integrated CI also runs `M3LongDeepFilterSelfTest`. Its
success/failure must be read from the **latest integration head**
rather than inferred from the original independent test results.

## Canonical existing physical owners

| Family | Logical operations | Physical layout / allocation |
| --- | --- | --- |
| `M3IndexedListSnapshot` | detached filter + stable sort | One GC-traced `Object[]` payload; selected `int[]` offsets and transient `byte[]` mask; `int[]` stable mergesort; optional existing JNI mask selector |
| `M3TreeMap` + mutable/read-only filtered views | live sorted/range/filter/search/keys/values/mutate | GC-traced reference arrays, AVL child/parent offsets and compact state; no retained `Map.Entry`, API-created entry only when requested |
| `java.util.EnumMap` in `java.base` | ordinal bulk `forEach`, `replaceAll`, `toString` | Original enum ordinal and value arrays; null marker distinguishes present-null from absent; subclass override preserves inherited behavior |
| `java.util.IdentityHashMap` in `java.base` | identity-sensitive `equals(Map)` and `toString` | Original alternating key/value `Object[]`; no entry materialization from own map; subclass override preserves inherited API |

This is a cross-owner source convergence, not a claim that all OpenJDK
collection families are converted: HashMap/LinkedHashMap/TreeMap/
WeakHashMap and other node-bearing JDK sources still require contract-driven
migration (JDK APIs, serialization, subclassing, iterator/GC behavior).

## Reusable canonical recipes, categories and donor evidence

- Synexia [#12366](https://github.com/hsoliwal/com.synexia/pull/12366):
  stable indexed filter+sort and mutable filtered M3TreeMap slot owners.
- Synexia [#12407](https://github.com/hsoliwal/com.synexia/pull/12407):
  read-only M3TreeMap direct-slot filtered/navigable view owner.
- Synexia [#12412](https://github.com/hsoliwal/com.synexia/pull/12412):
  native `java.base` EnumMap and IdentityHashMap ordinal/direct offset atoms.
- Full SHA-256 pre/post crates, donor category comparisons and license
  custody remain in their canonical Synexia sources. LeetCode,
  HackerRank and GeeksforGeeks are problem-category references only.
  OpenJDK Java sources preserve GPLv2 with Classpath Exception; tests
  under `test/jdk` retain GPL-2.0-only. M3 original collections
  and recipe/census metadata retain their existing Apache-2.0 terms.

## Verification — preserve truth across scopes

- Previously qualified individual M3 views:
  [CI 39185305177](https://github.com/hsoliwal/M3jdk21/actions/runs/39185305177)
  completed with 754,359 checks, but reported `JNI=false` for fusion.
- Previously qualified individual exact JDK `java.base` maps:
  [CI 39188109446](https://github.com/hsoliwal/M3jdk21/actions/runs/39188109446)
  compiled modified JDK classes using `--patch-module`; reported
  4,411 EnumMap and 78,847 IdentityHashMap checks.
- **New combined CI** `m3-doop-product-convergence-20261019.yml`
  compiles both original owners from this exact integrated head,
  builds the existing JNI C17 mask selector, and executes the
  indexed fusion oracle with `NATIVE_REQUIRED`, `-Xcheck:jni`.
  A separate job patches actual `java.base` and executes both JDK
  reference oracles. Inspect the workflow results, not just this receipt,
  before claiming integrated qualification.

OpenRewrite executable fixed point/refusal, complete Java 21
`java.base` image + jtreg, whole-repository reactor, HotSpot/GC/JNI
resource lifetime, JFR allocation/cost benchmarks, LFU/pressure and
installed-image parity **remain separate acceptance gates**.
Authoritative reference payloads must not be dropped under pressure;
M3LFU can reclaim only qualified/reconstructible derived data.

## Integrated execution after primitive predicate convergence

After the second additive source merge and the Windows-path-only CI
checkout correction, the exact integrated head passed the four
focused GitHub Actions workflows:

- [M3JDK21 DOOP Java/JNI convergence, run 39196975949](https://github.com/hsoliwal/M3jdk21/actions/runs/39196975949)
  — PASS both jobs. Java-only and `NATIVE_REQUIRED` JNI masks,
  checked native JNI crossings, and actual patched `java.base` oracles.
  Assertions across six pre-existing oracles total **857,528**.
- The same combined job executes original `M3LongDeepFilterSelfTest`:
  `M3_DEEP_FILTER_PASS assertions=3119 depth=100000`.
  Combined distinct assertions: **860,647**.
- [M3 balanced primitive views, run 39196976031](https://github.com/hsoliwal/M3jdk21/actions/runs/39196976031)
  — PASS on Linux, macOS **and Windows**. On Windows, both normal and
  `-Xint` with `-Xss256k` pass 3,119 assertions each; failure to
  checkout unrelated long recipe-custody paths was eliminated by
  restricting checkout to the original module owner directories.
- [M3 DOOP index views, run 39196976050](https://github.com/hsoliwal/M3jdk21/actions/runs/39196976050)
  — PASS.
- [Actual JDK map sources, run 39196976037](https://github.com/hsoliwal/M3jdk21/actions/runs/39196976037)
  — PASS.

These are reproducible focused CI receipts, **not** completed full
JDK HotSpot image, jtreg, JFR performance, synchronized/weak-reference,
M3LFU or memory pressure admission. Broader repository workflows may
still fail independently of these focused owner gates. Do not clear
the overall release status based only on these jobs.
