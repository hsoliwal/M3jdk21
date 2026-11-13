# four-library-bags-20261106: the packed long bag and map of the four-library counted tranche

Census leaf **L012** of the M3JDK convergence programme: crate `m3-four-library-bags` of hsoliwal/com.synexia
(synexia-common counted-collection tranche), received into `m3/collections` (package `jdk.m3.collections`) by
recipe `RELOCATE_FOUR_LIBRARY_BAGS v1` (`relocate.py`, derived from `immutable-pools-20261106/relocate.py` v2).
The synexia-primitives lane (session com-synexia-63) confirmed this leaf belongs here: synexia-common is a different
source module from its lane, on condition that every owner name is checked against `jdk.m3.collections` first.

Donor: hsoliwal/com.synexia develop `d5ad5090d87ef8477a56317e60ac0d29c65029f7` (2026-11-06). The owner postimages
and proofs are the crate's sealed recipe templates (`.java.txt`, SHA-256 equal to the crate `manifest.tsv`); the
donor's live synexia-common tree still holds the preimages because the crate output was never promoted, so the
postimages are the source of this relocation. The packed-map closure comes from the live synexia-common tree.
Every donor path, blob, SHA-256 and URL is in `PROVENANCE.tsv`; the rows and reasons are in `RELOCATION_MAP.tsv`.

## Decisions (owner check first)

Full table: [DECISION.tsv](DECISION.tsv). Checked against master `dbce93c3abf` and the open PRs before porting.

| Donor | Verdict | M3 name | Master owner compared |
|---|---|---|---|
| PackedLongLongHashMap | NEC, relocated under a distinct name | `M3PackedLongLongHashMap` | `M3LongLongHashMap`: other addTo/deletion/iterator contract, no trim/filter |
| PackedLongBag | NEC, relocated under a distinct name | `M3PackedLongBag` | `M3LongBag`: add returns the previous count and accepts zero |
| LongBag (closure) | NEC, role name | `M3PackedLongBagContract` | `M3LongBag` is a final first-party class of another contract |
| PackedLongLongMap, PackedHashAtoms, PackedSupport, LongLongPredicate (closure) | ABSORB | `M3PackedLongLongMap`, `M3PackedHashAtoms`, `M3PackedSupport`, `M3LongLongPredicate` | none |
| LongLongConsumer (closure) | ALREADY | `M3LongLongConsumer` (master) | byte-equal body (relocate.py `ALREADY body_equal=TRUE`) |
| BagAlgebraProof, BagMapFailureProof | ABSORB (test) | `M3BagAlgebraProof`, `M3BagMapFailureProof` | none |
| DenseObjectBag | NEC, HOLD | reserved `M3DenseObjectBag` | `M3CountedBag`, `M3ObjectCounts` |
| FourLibraryBagProof | SKIP | - | needs DenseObjectBag and real Guava/Commons/Eclipse |
| BagCountProbe, BagCountNativeProof, BagCountChunkBoundaryProof, bag_counts.c | SKIP | - | the JNI library they load is not in M3JDK |
| PrimitiveBag, PrimitiveBagAlgebraProof | HANDOFF | - | synexia-primitives lane; `M3PrimitiveBag` not on master |
| PackedLongLongSwissHashMap | SKIP | - | benchmark-only; Abseil attribution needed |

**Contract distinctions kept, not mapped.** `M3PackedLongBag` add/remove take a positive amount and return the
NEW count (remove floors at zero); `setCount` returns the OLD count; `setCount(value, expected, new)` is not a
concurrent CAS; algebra methods return detached snapshots (Guava's are live views). `M3LongBag` returns the
previous count, accepts zero, and remove returns the number removed. Neither is a Guava/Eclipse/Commons/JDK
Collection adapter. Unifying them is a later leaf; this one decides no equivalence (NEC rows).

**DenseObjectBag is held**, not dropped: its counts live in Eclipse Collections' `ObjectIntHashMap`, and
`jdk.m3.collections` requires only `jdk.m3.arrays`. A relocation would need a module dependency (pom.xml and
module-info.java, outside this leaf's new-files scope) or a re-hosting on an M3 object-to-int store, which is an
adaptation with its own contract review (`M3ObjectCounts` keeps insertion order, has no `compact()`, and returns
previous counts). The name `M3DenseObjectBag` is free and recorded for that leaf.

## Name check

On master `dbce93c3abf` (147,987 paths): no file named `M3PackedLongLongHashMap`, `M3PackedLongBag`,
`M3PackedLongBagContract`, `M3PackedLongLongMap`, `M3PackedHashAtoms`, `M3PackedSupport`, `M3LongLongPredicate`,
`M3BagAlgebraProof`, `M3BagMapFailureProof`, `M3FourLibraryBagsDump`, `M3FourLibraryBagsReferenceModel`,
`M3FourLibraryBagsTest` or `M3DenseObjectBag` anywhere under `m3/` (including `m3/collections/implementations/*`),
and no type declaration of those names in `m3/collections` (git grep). Open PRs: 12, none adds a file of those
names. Re-run on the rebased master immediately before the push (PR body).

## Run

```sh
python -I m3/collections/recipe/four-library-bags-20261106/relocate.py --donor-git <com.synexia clone> --donor-rev d5ad5090d87ef8477a56317e60ac0d29c65029f7
JAVA_HOME=<JDK 21> bash m3/collections/recipe/four-library-bags-20261106/verify.sh <com.synexia clone> d5ad5090d87e <scratch>
```

`relocate.py` is idempotent (`--check` exits 1 when a run would change a file) and writes `PROVENANCE.tsv` with
`body_match` read back from disk. `differential.sh` compiles the donor classes verbatim (`--donor-subset`) and
replays the proofs and `M3FourLibraryBagsDump` through both class sets; the dump driver is one source, mapped back
to donor names through the TYPE rows for the donor side. Gate results: [GATES.tsv](GATES.tsv); lessons:
[RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv); module test sets: [TEST_BASELINE_DIFF.txt](TEST_BASELINE_DIFF.txt).

## Files

- main: `M3PackedLongLongHashMap`, `M3PackedLongBag`, `M3PackedLongBagContract`, `M3PackedLongLongMap`,
  `M3PackedHashAtoms` (package-private), `M3PackedSupport` (package-private), `M3LongLongPredicate`
- test: `M3BagAlgebraProof`, `M3BagMapFailureProof` (relocated); `M3FourLibraryBagsTest` (JUnit wrapper: algebra
  proof, both failure proofs in forked JVMs, reference model at 100,000 ops), `M3FourLibraryBagsReferenceModel`
  (plain-Java HashMap and Guava-style models), `M3FourLibraryBagsDump` (differential driver, not a test)
- recipe: this directory. `pom.xml` and `module-info.java` are unchanged.
