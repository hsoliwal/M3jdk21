<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered first-visible atoms: target receiving scope

**PENDING full target acceptance.** This packet describes a bounded refinement
of the existing M3JDK21 filtered sorted/navigable collection wrappers. M3JDK21
remains the integrated SDK. Synexia owns the reusable recipe, catalogue and
source qualification.

## Runtime stack and exact scope

This change is stacked on [M3JDK21 #1642](https://github.com/hsoliwal/M3JDK21/pull/1642)
at `6d9bad615851f346e3062e3034ccd74cbe2776af`. Its wrapper source provenance
and required preimage remain pinned to [#1603](https://github.com/hsoliwal/M3JDK21/pull/1603)
at `7c0d2493edbf93ac5cb726667ce83046d3519e3e`. The selected-source
custody checked that exact wrapper preimage and both absent new test paths
before application. Parent source-stack verification binds the #1642 ancestry;
the fresh Java gate operates on a selected-source subset, not a full checkout.

| Receiving path under `m3/collections/` | Before SHA-256 | After SHA-256 |
| --- | --- | --- |
| `src/main/java/com/m3/collections/M3FilteredViews.java` | `fb1f4da6cbfcaf27385bad58eb810f22b1f8f5577b73bd04fc8c06c871c5035c` | `4209587c4cc43537e1ce0d7b316d1824a4de0f25154fef1a87cbe7e9ae9871ea` |
| `src/test/java/com/m3/collections/M3FirstVisibleAtomContract.java` | `ABSENT` | `cea097e4b1cf67a3cd2d8fdd4df5f29751db7d767daf785732c350003acede29` |
| `src/test/java/com/m3/collections/M3FirstVisibleAtomTest.java` | `ABSENT` | `c16fa40bc6768618df372d8d43936f5761d3375c35dfeeb9da15b84dc4fcb44d` |

The production diff replaces only two private `firstVisible` helpers and adds
a source modification notice. The other two paths are the focused standalone
contract probe and its JUnit adapter. Public/protected production signatures,
backing collection owners and dependencies remain unchanged.

The existing native changes from #1642 must remain exact: the compactor retains
SHA-256 `58f403c3b2677ecb8c7c36d9e04233760c1477f4d770f431efddc5318edf9ad2`
and `M3MaskPositionAtomsTest.java` retains
`53d22b261f4376f41e735dc5121d3d8f97ee08f4eafdf99e3f615d0330b22548`.
This Java-only recipe has no native receiving target or new JNI export.

## Direct reuse and preserved selection behavior

The adopted donor is the existing Apache-2.0
[`M3MutableFilteredViews.firstMatch`](https://github.com/hsoliwal/M3JDK21/blob/7c0d2493edbf93ac5cb726667ce83046d3519e3e/m3/collections/src/main/java/com/m3/collections/M3MutableFilteredViews.java),
Git blob `37b669dfe22a3b6be9e68aa59f9dd94e96fc3d49`. The donor owner is
unchanged. Its direct first-match loop supplies the set helper and the map
helper without introducing a new collection abstraction.

Each helper preserves cursor construction, `hasNext`/`next` order, predicate
and comparator demand, first accepted reference, null/empty behavior and stop
position. The map still reads the key/value pair for the predicate and then
constructs `SimpleImmutableEntry`, whose separate second getter pair remains
at its original point. Caller-owned throwable identity and callback/termination
prefixes are covered by the focused traces. Selection remains O(k) in the
visited prefix with O(1) selection state.

The removed private adapters change internal stack-frame shape. This packet
does not claim byte-identical internal stack traces or equivalent results for
callbacks deliberately inspecting those frames with `StackWalker` or throwable
stack introspection. It also makes no exhaustive VM allocation-failure/OOME
or allocation-timing equivalence claim. There is no measured speedup,
allocation-byte or zero-allocation claim; the JIT may already eliminate an
adapter. The evidence supports this stated scope, not universal observational
equivalence over arbitrary caller implementations.

`boundedEntry` and the range factories stay unchanged. The earlier intentional
catch-scope repair is excluded. Its original **100 late-range failures, three
real comparator failures and initial comparator-origin failure remain**.
Preserving those expected failures establishes continuity of the existing
behavior; it does not repair the defects.

## Canonical recipe and evidence

Canonical recipe:
[`com.synexia.m3.FilteredFirstVisibleRuntime20261021`](https://github.com/hsoliwal/com.synexia/blob/0a8af61d5ca4361612b46aa071a14d215e328fc3/synexia-openrewrite-recipes/recipe-crates/m3-filtered-first-visible-runtime-20261021/README.md),
published through [Synexia D pull request](https://github.com/hsoliwal/com.synexia/pull/12666) and stacked
on Synexia #12650. The canonical immutable revision is
`0a8af61d5ca4361612b46aa071a14d215e328fc3`.

- Actual source-replay proof SHA-256: `688e24e3c80dfa2ea64bc6cfbb952291b3eeb5898a3cd8a6e20d2565513cecfe`.
- Three-file receiver root: `759d990c9a943e42e9cb399ccd92b4a13ddc3feee07ba462b3645f0d5ad81ae6`.
- Exact source/phase/receipt identities: `RECEIVING.json`.

The baseline, set-only intermediate and final wrapper were qualified in the
canonical packet with **44 commands and 38 exact output comparisons**. Each
stage covered 64 normal selection cases and 1,104 caller-owned
`IllegalArgumentException`/`AssertionError` fault probes. They produced the
same selection tape:
`53a3ac80b4ba43ad3df0c2397041513fb02346c2ba2cb2ddebc12b7540329b99`.
Existing bounded/navigation/entry/readonly/depth corpora, normal range controls,
public/protected API and the deliberately retained failures were also compared.

The actual Maven/OpenRewrite composition qualified three file phases and two
serial Java atoms. The four existing snapshot/scope engines remain unchanged.
The set atom's exact output feeds the map atom, and their final bytes equal the
composed owner phase. Each phase retains its fixed-point and refusal checks;
the eight synthetic chain controls are separately identified.

The later canonical emitted-source verifier executed **nine new commands and
six exact comparisons**, including the actual receiving JUnit adapter: one test
passed with 64 normal cases and 1,104 fault probes. The original 44 commands and
38 comparisons were hash-verified and reused; they were not rerun by those
nine commands. All 76 frozen qualification files were checked. The expected
100 late-range failures, three comparator failures and original factory-origin
failure remained identical. Eight isolated copier invocations separately
covered clean application, exact replay and six refusal cases, preserving the
excluded native-owner sentinel.

### Fresh target execution and bindings

Fourteen fresh target commands passed their specified outcomes, with eight
exact output comparisons. The qualified copier checked, applied and replayed
the three-file receiver. Strict Java21 compilation used the actual receiving
sources. Both the direct focused probe and the one-test JUnit adapter reproduced
64 normal cases and 1,104 caller-owned failure probes per invocation. They are
two invocations of the same corpus, not 2,336 distinct cases. Public/protected
API and the stable-read tape matched; 120 exception-control cases passed 240
checks. The 100 late-range failures, three real-comparator failures and original
comparator-origin failure matched their expected exit code 1 and exact output.

All five selected native-lane source files retained their bytes, modes, mtimes
and inodes. No native test ran in this wrapper gate, and the broad historical
44-command/38-comparison matrix was not rerun. `VERIFICATION.md` documents the
selected-source reproduction and its limits.

| Binding | Receipt |
| --- | --- |
| Target preimage/native custody | `preimage-custody.json` |
| Qualified application | `evidence/copier-apply.receipt.json` |
| Exact replay | `evidence/copier-replay.receipt.json` |
| Fresh target test result | `evidence/results.json` |
| Fresh target commands | `evidence/commands.json` |
| Fresh output comparisons | `evidence/comparisons.json` |
| Target source readback | `m3/collections/qualification/m3-filtered-first-visible-runtime-20261021/source-blobs-readback.json` |

`RECEIVING.json` binds the observed local application and verification receipts
by exact hash, the published canonical recipe revision and the actual GitHub
source-blob readback. Full receiving acceptance remains PENDING. The readback
compares all three source content objects before branch publication; final PR
file-set and commit reachability are separate publication checks.

## Catalogue, attribution and receiving limits

The [canonical problem-category catalogue](https://github.com/hsoliwal/com.synexia/blob/0a8af61d5ca4361612b46aa071a14d215e328fc3/synexia-openrewrite-recipes/recipe-crates/m3-filtered-first-visible-runtime-20261021/catalogue.tsv)
records both first-visible atoms, the retained range-repair refusal and the
serial recipe composition. LeetCode Search Insert Position, HackerRank Ice
Cream Parlor and GeeksforGeeks Linear Search were compared as categories.
Binary search requires indexed/monotone assumptions absent from arbitrary
predicate iteration; pair-sum lookup has a different result contract. The
existing first-party direct loop supplies the adopted implementation. No
challenge/editorial body or its performance figures are imported.

`NOTICE.md` retains the canonical notice and target-specific attribution.
Existing component LICENSE/NOTICE and per-file notices remain authoritative.
The intake remains `INDEPENDENT_APACHE_MODULE`; Synexia is authoring/proof
tooling with no new runtime dependency. This does not relicense OpenJDK or any
external repository.

This target-local mapping points to existing
`m3/docs/name-mapping.json#/family_name_mapping/3`. The root map and ordered
receiving plan remain unchanged: STRING remains active. Generic task admission,
its donor prerequisite, whole-repository serial review, installed-source
inventory, whole-module/reactor/image/jtreg execution and broader platform,
lifetime/concurrency and resource matrices remain pending. No automatic phase
advance, family completion or global acceptance follows from this packet.
