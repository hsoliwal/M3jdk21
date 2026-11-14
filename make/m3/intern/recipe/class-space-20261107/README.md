<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe class-space-20261107 (RELOCATE_CLASS_SPACE v1)

Census pass 2, leaves **P2-119** (crate `m3-classspace-failure-atomic-growth-20261030`: `M3ClassSpace`,
`M3GrowthSafetyJUnitTest`, `M3GrowthFaultProbe`) and **P2-126** (crate `m3-classspace-canonical-open-20261030`:
`M3ClassSpaceArchive`, `M3ClassSpaceArchiveSelfTest`): the class-space family of the first-party donor
`hsoliwal/com.synexia` (develop `d5ad5090d87e`), relocated into the existing owner module `m3/intern`
(module `org.m3jdk.intern`, exported package `jdk.m3.intern`), recipe-first and byte-for-byte.

| Owner | Kind | Contract |
|---|---|---|
| `M3ClassSpace` | public | owner-scoped, budgeted store of immutable classfile-byte candidates: one `byte[]` arena, `int` offset / length / chain lanes, packed SHA-256 lane; SHA-256 selects candidates, full bytes decide equality; owner-local `int` ordinals; duplicate lookup before the budget check; failure-atomic growth; package-private `internRegion`, all-or-nothing `internUtf8Batch`, bounded `copyInto` |
| `M3ClassSpaceArchive` | package-private | immutable `M3CSIMG1` frame of one owner (24-byte header, 40-byte cells with entry SHA-256, payload, root SHA-256): `freeze`, `openCopy` / `openCanonicalCopy`, zero-copy `openTrustedMapped` / `openCanonicalTrustedMapped`, `PinnedView` (`verifyRoot`, `copyVerifiedAt`, `snapshotCanonicalCopy`), radix `verifyCanonicalIds`, `restore` |
| `M3ClassFileAtoms` | public (closure) | read-only constant-pool scanner (primitive lanes, envelope grammar only, never a verifier); `Plan.internInto` (serial CP order) and `internAtomicallyInto` (all-or-nothing) |

None of them is a class loader, HotSpot `Klass` table, CDS archive or class-equivalence oracle; no `Class`,
`ClassLoader` or native pointer is retained (`bind` returns an unregistered caller-owned pair).

## Decisions ([DECISION.tsv](DECISION.tsv))

* What a class space is: an interning contract over class-image and symbol bytes, not a CDS-like archive (D0).
* Owner: `m3/intern` already owns owner-scoped exact-byte interning and its SHA-256 archive container, so the family
  joins it instead of a new `m3/classspace` module (D1); no `java.base` consumer (D2). `M3InternSpace.ByteStore` and
  `M3InternArchiveGuard` share the shape but not the contract (budget, SHA-256 lane, failure atomicity, batch
  admission, wire format): equivalence is not decided, both are kept side by side (NEC, D4 / D5).
* `MIndexClassPool` / `MIndexClassIndex` (session com-synexia-4d, census P2-099) intern class *metadata* through a
  text pool, not class-image bytes: no overlap, no hand-off (D3).
* Lineage: the develop-live crate sources are the head of the family; the census named intermediate postimages
  (`M3ClassSpace` 234e3e9d, `M3ClassSpaceArchive` 2600a6e8) that later crates take as preimages (D8,
  [RELOCATION_MAP.tsv](RELOCATION_MAP.tsv) header).
* The two JNI natives are optional differential oracles: their declarations and C files are relocated into the test
  tree and run only by the differential gate (D6). `M3StaticUsePlan` (P2-133) is a separate leaf in `m3/compiler` (D11).
* FOSS ([FOSS_REUSE_DECISION.tsv](FOSS_REUSE_DECISION.tsv)): OpenJDK HotSpot / CDS are architectural references only;
  ASM, classgraph and `jdk.internal.classfile` are not M3 owners; no third-party expression is relocated.

## Oracles

* Donor oracles relocated with their wrappers: `M3ClassSpaceSelfTest` (503 assertions; 1,432 with the JNI lane, the
  crate README figure), `M3ClassFileAtomsSelfTest` (216; 291 with JNI, the README figure),
  `M3ClassSpaceRegionSelfTest` (32,398), `M3ClassSpaceAtomicBatchSelfTest` (367,604), `M3ClassSpaceArchiveSelfTest`
  (10,169; 10,265 with JNI), `M3ClassSpacePinnedMappedSelfTest` (4,035), `M3ClassSpacePinnedSnapshotScopedTest` (8),
  `M3ArchiveCopyToSelfTest` (6,441, the README figure), `M3ArchiveFreezeParitySelfTest` (10,594 checks, archive
  SHA-256 `ac568e1f...`, the README figures), `M3GrowthSafetyJUnitTest` (3), `M3Sha256BatchReuseJUnitTest` (3).
  The region and atomic-batch oracles scan the compiled `M3ClassFileAtoms.class` as their fixture, so their counts
  follow its constant pool: 32,344 / 367,568 (the README figure) with `javac` defaults, 32,398 / 367,604 with `-g`
  as Maven compiles; donor and relocated agree under the same flags.
* `M3ClassSpaceDifferentialDump` (test scope, new): 100,000 serial operations and a 25,000-operation 4 KiB-budget
  stream against an exact-bytes model, a forced-collision owner, 10,000 region / batch rounds (all-or-nothing batches,
  invalid ranges), the archive round trip (restore, mapped file, pinned view, snapshot, file reopen: identical
  frames), 18 single-byte corruptions (each refused) and eight JDK class files (serial and atomic interning agree);
  1,106,598 checks.
* Failure-atomic growth: the donor's `inject-growth-faults.py`, applied to scratch copies only, reproduces the two
  recorded baseline defects on the crate preimage and `FAULT_REPLAY_PASS lanes / buckets` on the donor head and on
  the relocated owner.
* Differential ([differential.sh](differential.sh)): donor closure compiled from the donor tree vs the relocated
  classes: nine oracle outputs, the dump (default JIT and `-Xint`), the three JNI lanes (`-Xcheck:jni`), the
  allocation bytes per freeze and `javap -p` of ten classes are identical; four one-line mutants are refused.

## Run

```sh
python -I m3/intern/recipe/class-space-20261107/relocate.py --donor-git <com.synexia clone> \
    --donor-rev d5ad5090d87ef8477a56317e60ac0d29c65029f7 [--check]
JAVA_HOME=<JDK 21> bash m3/intern/recipe/class-space-20261107/verify.sh <com.synexia clone> \
    d5ad5090d87ef8477a56317e60ac0d29c65029f7 <scratch> [maven-repo-local [tail [base-rev]]]
```

Gate results: [GATES.tsv](GATES.tsv); lessons: [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv).

## Open items

* NEC proposals D4 / D5 (one byte-store contract for `M3InternSpace.ByteStore` and `M3ClassSpace`, one archive frame)
  need their own leaf and differential.
* Donor HOLD gates stay HOLD: HotSpot / CDS / Metaspace integration, class unloading, hidden classes, JVMTI.
