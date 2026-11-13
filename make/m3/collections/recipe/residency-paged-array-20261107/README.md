<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe residency-paged-array-20261107: M3PagedArray and M3MemoryPressure into jdk.m3.collections

This is the P2-116 family of the residency fold: census2 crates `m3-pressure-admission-sampling-20261013` and
`m3-resident-pressure-cadence-20261014`, plus the M3PagedArray / M3MemoryPressure / libm3_residency rows that #2566's
`DECISION.tsv` deferred. Every donor class has an owner decision (`DECISION.tsv`) recorded before the port.

* Donor: hsoliwal/com.synexia develop `165ceb5a020291e99ebeb89783b88e8a9686ae5c`. The relocated blobs come from
  `4a2e6452a7a` (2026-10-31). The crates' postimages are earlier states of the same files (SKIP rows).
* Host: M3JDK master `a3ee7b9f5848a0df33f46fcd9335ebebf1f63b8d` (decisions), gates re-run after the rebase onto
  `e03bb28d54b`; module `m3/collections`, package `jdk.m3.collections`.
* The fold's other rows are in `make/m3/collections/qualification/residency-fold-20261107/DECISION.tsv`: the
  pressure-sampling atom and P2-079.

| Donor | Verdict | Target |
| --- | --- | --- |
| M3PagedArray (656) | ABSORB | `jdk.m3.collections.M3PagedArray` |
| M3MemoryPressure (113) | ABSORB | `jdk.m3.collections.M3MemoryPressure` (the array's Probe/Snapshot seam) |
| libm3_residency (C, header, CMake, native test) | DEFER | its own native leaf: WSL gate, plus a decision against java.base container metrics |
| 4 self-test mains | ABSORB | `src/test`, JUnit endpoints in `M3PagedArrayResidencyTest` |
| crate postimages | SUPERSEDED / ALREADY | the relocated develop blobs |
| M3ResidentPressureCadenceRecipeTest | OUT | com.synexia OpenRewrite crate test |

## Why ABSORB and not EXTEND

`M3PagedArray` is a growable array addressed by index. Its pages spill to private files and page back in under a
page-count and sampled-headroom budget. `M3ObjectResidency` is a fixed-size slot arena with a byte budget and a
caller-supplied per-slot spill store. Folding one into the other would be a rewrite and would put two storage models in
one owner.

`M3MemoryPressure` is the array's injectable sampling seam: Probe, a Snapshot with physical evidence, and the optional
native loader. `M3RuntimeHeap` is protected and owns a different capability, growth-capacity policy. The heap fields of
the two classes are the same Runtime reads (NEC row). No owner body is edited.

## The port

`relocate.py` is the code of `transient-facade-20261106/relocate.py` v4, unchanged; only its docstring title
differs. It relocates the 6 FILE rows (2 main, 4 test mains) with body_match 6/6 and no DROP, REWRITE or TYPE rows.
Every donor body line is kept. `M3PagedArrayResidencyTest` (M3JDK-authored) gives the mains JUnit endpoints:
pressure policy, reuse mode, behavior mode, sampling mode `java`, and the report. The sampling self test's
fixture/legacy/native modes need the deferred test-only JNI ABI.

`differential.sh` compiles the six donor sources in their own package and runs the same mains and arguments on both
builds, normal and `-Xint`. The arguments are: none, `behavior`, `resources`, `java`, and the report. It also runs
`oracle/M3PagedArrayReferenceDifferential` on both builds: seeded add/get/set/pin/trim/stats against
`java.util.ArrayList`, with an injected Probe that drives headroom admission, physical-pressure eviction and the
1-in-1024 hot-path trim. Outputs must be byte-identical. Live heap digits, thread-allocated bytes and nanos are masked.

```
python -I make/m3/collections/recipe/residency-paged-array-20261107/relocate.py --donor-git <com.synexia clone> --donor-rev 165ceb5a020291e99ebeb89783b88e8a9686ae5c --check
JAVA_HOME=<JDK 21> JTMP=<tmp> bash make/m3/collections/recipe/residency-paged-array-20261107/differential.sh <com.synexia clone> 165ceb5a020291e99ebeb89783b88e8a9686ae5c m3/collections/target/classes m3/collections/target/test-classes <out> 60 400
```

Gate results: `GATES.tsv`. Corrections: `RECIPE_MEMORY.tsv`.

## Open items

* libm3_residency (DEFER): an owner decision against `jdk.internal.platform.CgroupMetrics` (memory.high and cgroup
  ancestry are the deltas), then a WSL gate (gcc 13.3 -Werror, -Xcheck:jni) and the sampling self test's fixture,
  legacy and native modes.
* The array keeps the donor's page bookkeeping: `ArrayList<Page>`, `LinkedHashMap<Integer, Page>` LRU and one Page
  object per page, not per value. Moving it to primitive lanes (M3-COLLECTION-JNI-ARRAY-STORAGE-1) is a follow-up
  owner edit with its own differential.
