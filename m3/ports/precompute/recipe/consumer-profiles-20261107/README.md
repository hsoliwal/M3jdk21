<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe consumer-profiles-20261107: build-time consumer demand profiles for index precompute

Census pass-2 leaf P2-115 of the M3JDK convergence programme (crate `m3-precompute-consumer-profiles-20261017`, rows
NO_OWNER). Relocates, by recipe, into the existing precompute lane `m3/ports/precompute` (package `jdk.m3.precompute`):

- `M3IndexPrecomputeConsumerProfiles`: seven reviewed consumer profiles (REGEX, M3_STRING, COLLECTIONS, READER, WRITER,
  SWING, UTIL) over capability ids with scores 1..200 (immutable maps), `forConsumer`, `weights`, and
  `blend(shares 0..1024, buildPenalty >= 0)` combining consumer demand deterministically in enum order with exact
  arithmetic; an all-zero demand is refused. Profiles nominate capabilities; they compile, cache or execute nothing.
- its closure: `M3IndexPrecomputeCapability` (60 stable capability ids with `id()` and a `QueryCost`) and
  `M3IndexPrecomputeWorkload` (immutable weight vector + build penalty, `Builder`, the balanced `languageRuntime()`).

| Target | Scope | Donor (hsoliwal/com.synexia develop `165ceb5a0202`, synexia-index-precompute) |
|---|---|---|
| `M3IndexPrecomputeCapability` | main | `IndexPrecomputeCapability.java`, blob `851b246a9f57` (closure) |
| `M3IndexPrecomputeWorkload` | main | `IndexPrecomputeWorkload.java`, blob `59f076375372` (closure) |
| `M3IndexPrecomputeConsumerProfiles` | main | `IndexPrecomputeConsumerProfiles.java`, blob `b2ea8dcc26c7` (= crate postimage) |
| `M3IndexPrecomputeConsumerProfilesOracle` | test | `IndexPrecomputeConsumerProfilesContract.java`, blob `e83acacb4e47` (= crate postimage) |
| `M3IndexPrecomputeConsumerProfilesTest` | test | `IndexPrecomputeConsumerProfilesTest.java`, blob `e496c3a5128c` (= crate postimage) |
| `M3IndexPrecomputeConsumerProfilesDump` | test | authored: reflective donor-vs-relocated dump with a long-arithmetic reference model |
| `M3IndexPrecomputeConsumerProfilesReferenceTest` | test | authored: JUnit wrapper of the dump |

## Decisions

| Donor / owner | Decision | Why |
|---|---|---|
| profiles, oracle, bridge | ABSORB (relocated) | no owner on master (no capability ids, workload weights or consumer profiles anywhere outside vendor Lucene locals) |
| capability enum, workload vector | ABSORB (closure) | same-package types the profile class needs; JDK-only |
| `IndexPrecomputePlanner`, `IndexPrecomputeMultipassCompiler` | HELD | the planner family that consumes a workload is a separate absorb; the profile javadoc links to both (GATES.tsv `doclint_note`) |
| vendor `MIndexPrecomputePlan`, `MIndexPrecomputePressureBudget` | NEC module owners | other precompute layers (typed pass graph, memory admission); untouched |
| crate postimages, `.superset/conflicts` 65-constant capability variant | OMIT | postimages = main-path blobs; the variant's consumers are conflict-preserved files only |

## Run

```
python -I m3/ports/precompute/recipe/consumer-profiles-20261107/relocate.py --donor-git <com.synexia clone> --donor-rev 165ceb5a020291e99ebeb89783b88e8a9686ae5c [--check]
bash m3/ports/precompute/recipe/consumer-profiles-20261107/verify.sh <com.synexia clone> 165ceb5a020291e99ebeb89783b88e8a9686ae5c <scratch> <baseline m3/ports/precompute build>
```

`relocate.py`, `class_identity.sh` and `verify.sh` are copies of `utf16-stitch-20261106` (relocate.py: only its
docstring names this leaf; verify.sh: javap parity reads `donor-classes`). `javap_parity.sh` adds the `$`-separated
package spelling of synthetic names and survives a DIFFERS listing; `differential.sh` (from `int-ring-buffer-20261106`)
compares the TYPE-mapped oracle report and the JIT / `-Xint` dumps. Results: GATES.tsv; provenance: PROVENANCE.tsv;
lessons: RECIPE_MEMORY.tsv.
