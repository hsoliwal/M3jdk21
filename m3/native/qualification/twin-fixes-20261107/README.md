<!-- SPDX-License-Identifier: Apache-2.0 -->
# twin-fixes-20261107 (m3/native): donor fixes #16771 and #16775 ported onto jdk.m3.jni

The twins `jdk.m3.jni.M3NativeLeaseScan` and `jdk.m3.jni.M3JniArrayReferences` were relocated from the donor's
primitives module by recipe `recipe/scoped-leases-20261106` (311d9e6c71a). The donor merged one fix for each on
develop. This recipe ports both with the method of `m3/collections/qualification/doop-facade-fixes-20261107` (#2687):
hash-pinned donor pre/post snapshots, relocation with the twin's own map, 3-way merge against the pinned twin blob.

| Leaf | Donor PR (merged) | Merge commit | Twin | Fix |
| --- | --- | --- | --- | --- |
| N | #16771 | `bee80508c334ec11d9f82ad05106b556f6036546` | `M3NativeLeaseScan` | `collect` falls back to Java when a loadable library lacks the full-scan export |
| J | #16775 | `7627894f9879c025a2cb2ceece536e3918259189` | `M3JniArrayReferences` | `retainSources`/`releaseSources` hold the table monitor across the pair |

The pins are the merge commits on `hsoliwal/com.synexia` develop (`PROVENANCE.tsv`: git blobs and sha256 of every
donor byte read). Merge order on develop: #16772 (`jdk.m3.collections.M3TransientFacadeStore`, RESTORED_SINCE M3jdk21
#2742 by peer 22 while this lane ran, confirmed hunk by hunk in `DECISION.tsv`, not touched here) < #16775 < #16771 <
#16781 (greedy job-sequencing DSU: NO_TWIN, `DECISION.tsv`); the leaf-N merge is the last one that carries a JNI fix
and its tree holds both of them, so the donor oracle closures are compiled at that commit. Both
donor pre snapshots are the very blobs the relocation recipe read (`056cf09973d4`, `9986c7a6b84a`, named in the twins'
RELOCATE anchors), so the twins' pre-state equals the relocated donor pre byte for byte (`ours_vs_base_hunks=0`).
`M3ScopedLeaseTable`, whose monitor leaf J reuses, is also unchanged at both merges (blob `6e845b2af4a4`).

## Leaf N: full-scan missing native export

`System.loadLibrary("m3_scoped_leases")` can succeed for a library that lacks `collectNative` (an older peer, or the
window-only shape). `collectWindow` already caught the `UnsatisfiedLinkError` of its native call and took the Java path;
`collect` did not, and the error escaped. The port wraps the one `collectNative` call site in the same fallback to
`collectJava`: same preflight, same stable candidate prefix, same trailing output, no new allocation, no JNI symbol or
descriptor change (`javap -p` identical, bytecode of `collect` changed, 12 members unchanged).

Without a library the relocated test is pure Java parity and passes on master too (the donor says so: "C11
missing-export fixture is a separate executable proof"). The discriminating proof is the donor's fixture
`oracle/native/MissingFullSymbolLibrary.c` (a loadable library exporting only `JNI_OnLoad`): built with gcc 13.3
`-std=c11 -Wall -Wextra -Werror` under WSL and, for the host gates, with MinGW gcc as `m3_scoped_leases.dll`. With it
on `java.library.path`, master fails 2 of the 4 tests with `UnsatisfiedLinkError` and the oracle refuses; head passes
4/4 and the oracle prints `PASS: 10000 real JNI-loaded missing-symbol full/window scan comparisons`, byte-identical to
the donor oracle on the donor's own classes, on JIT, -Xint and C1 under `-Xcheck:jni`.

## Leaf J: paired source borrow atomicity

`retainSources(table, left, right)` and `releaseSources` each made two monitor transitions on the table
(`beginJniArrayAccess`/`endJniArrayAccess` are `synchronized`), so an observer under the table monitor could see one
edge of the pair (`jniArrayBorrowCount(left) != jniArrayBorrowCount(right)`, or an aliased count of 1). The port holds
`synchronized (table)` across each pair, as the module's `beginBatch`/`endBatch` already do; the table's methods are
reentrant on the same monitor. Alias counts twice, second-acquire rollback, reverse release and failure suppression are
unchanged; O(1), no new lock, no ABI or descriptor change (`javap -p` identical, bytecode of the two members changed, 14
unchanged). On master the relocated stress tests fail (`half pair`, `half alias`) and the donor oracle fails the same
way; on head both pass and the oracle output is byte-identical to the donor oracle on the donor's classes.

## Mechanics (recipe-first, no hand edit)

`apply.py` carries no module name: it finds the relocation recipe by the MERGE3 targets among the FILE rows of the
`RELOCATION_MAP.tsv` files under `recipe/` (here `scoped-leases-20261106`), imports that recipe's functions
(`read_map`, `relocate`, headers, anchor) from its file and reuses its map (the PACKAGE row to `jdk.m3.jni`, EXTERNAL
junit, JNI and TEXT rows); the same text drove the record-only ALREADY check of the `m3/collections` twin against
`recipe/transient-facade-20261106` (`DECISION.tsv`, row #16772).

* `MERGE3` rows: donor pre and post snapshots relocated by the map, the RELOCATE anchor of base and theirs set to the
  host's, `git merge-file ours base theirs` with ours = the pinned M3JDK preimage (blobs `4137f8e077b9`,
  `43d82aa97b18`, master aa2372843bf). One `//FLLDCIM: MRL` anchor above `collect` and above `retainSources`; an
  `ANCHOR` row adds the one above `releaseSources`.
* `FILE` rows: the two donor tests, the two donor oracles (package `com.synexia.primitives -> jdk.m3.jni` into
  `oracle/jdk/m3/jni/`; `NativeFullSymbolFallbackOracle -> M3NativeFullSymbolFallbackOracle`, the module's M3 type
  naming, declared by the target stem) and the C fixture into `oracle/native/`.

Each chain starts at a pinned preimage and each row pins its postimage sha256 (`--seal` wrote them). All pins are
verified before the first write; a drifted target is refused (exit 2) and nothing is written; the second run reports
`changed=0`.

### Per-hunk mapping and 3-way result (`HUNKS.tsv`)

| Leaf | Hunk | Donor pre -> post | Relocated base -> theirs | Host ours -> result | Change |
| --- | --- | --- | --- | --- | --- |
| N | 1 | 52,1 -> 52,7 | 54,1 -> 54,7 | 54,1 -> 54,7 | `collectNative` call wrapped in the `UnsatisfiedLinkError` fallback |
| J | 1-3 | 101,4 / 106,3 / 110,1 | 103,4 / 108,3 / 112,1 | same as relocated | `retainSources` body under `synchronized (table)` |
| J | 4-7 | 118,3 / 122,3 / 126,1 / 128,1 | 120,3 / 124,3 / 128,1 / 130,1 | same as relocated | `releaseSources` body under `synchronized (table)` |

Every hunk is CLEAN_THEIRS, conflicts 0. The relocated heads equal the donor post snapshots except for the header and
package lines, the RELOCATE anchor and the MRL anchors.

## Gates

`GATES.tsv` has the commands and numbers; `DECISION.tsv` the four twin decisions of the day (two PORTED here, #16772
RESTORED_SINCE #2742, #16781 NO_TWIN). Host tools: `tools/mutants.py` (the fixture library on `java.library.path`
for the LIBRARY leaf's test only, the module's eight JUnit classes as the family), `tools/classscope.py`,
`oracle/oracles.sh` (donor vs relocated oracles on JIT/-Xint/C1 under `-Xcheck:jni`, the relocated oracles on master
classes, `M3ScopedLeaseDump` 100,000 operations master vs head). Linux side: `oracle/wsl-gates.sh` (fixture with gcc
`-Werror`, oracle N on master and head, the relocated JUnit test with the fixture on master and head, and the module's
`recipe/scoped-leases-20261106/native-matrix.sh` on the head sources: the real C17 library, the three exported
symbols, every self-test under `-Xcheck:jni` in three modes).

```sh
python -I m3/native/qualification/twin-fixes-20261107/apply.py --donor-git <blobless donor clone> [--check]
```
