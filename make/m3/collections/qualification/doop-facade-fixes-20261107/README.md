<!-- SPDX-License-Identifier: Apache-2.0 -->
# doop-facade-fixes-20261107: two donor fixes ported onto jdk.m3.collections.M3TransientFacadeStore

The twin `jdk.m3.collections.M3TransientFacadeStore` was relocated from the donor's primitives module by recipe
`recipe/transient-facade-20261106` (6b2521d9f84, #2566). Its last fixes are the double-dispose twins 6a48d67ce59 and
2ea36542537 (#2591/#2601). The donor then fixed two more defects in two stacked draft PRs. This recipe ports both.

| Leaf | Donor PR (draft) | Pinned head | Fix |
| --- | --- | --- | --- |
| A | #16767 | `6a534f23a85f05bc4a27031066b46189afaab6b1` | factory reentrant duplicate admission: no leak of the unadmitted facade |
| B | #16769 (stacked on #16767) | `ff83fd2a685bc60014ba91b2817de544d63e7374` | paired idle growth through the BackingCopier seam |

**The donor PRs are open drafts. This port is pinned to the two heads above (`PROVENANCE.tsv`: git blobs and sha256
of every donor byte read). If either head moves, re-check: `apply.py` refuses with "donor drift" until the pins are
re-sealed against the new heads and the gates are re-run.**

## Leaf A: factory reentrant duplicate admission

A reentrant factory can admit the logical id itself before it returns: a nested `expose(id)`, or a nested
`withInternal(id, ..)` in a retaining mode. The outer call then inserts the facade it created, `insert` throws
`IllegalStateException("duplicate facade id: <id>")`, and that facade was never admitted and never disposed (a leak).
The port adds a private `insertCreated` and uses it at the two creation-to-resident call sites (`expose` and the
retained branch of `withInternal`). `restoreSlot` and the other restore paths are unchanged. On an insert failure,
`insertCreated` disposes the created instance only if it is not already resident, by reference identity. A shared
factory instance that a nested call admitted is never disposed. The primary exception is rethrown. A cleanup failure
is added to it as a suppressed exception.

## Leaf B: paired idle growth

`ensureReusableCapacity(int)` now delegates to a package-private seam `ensureReusableCapacity(int, BackingCopier)` with
the existing `ARRAY_COPY` copier. The seam stages both idle copies (`Object[]` facades, `long[]` ids), rejects a
`null` or wrongly sized copy, and only then publishes the pair. It reuses the existing `BackingCopier` and
`ARRAY_COPY`. No new type is added.

**Finding:** the donor PR says the method published the grown `Object[]` before it allocated the matching `long[]`.
Neither the donor pre-snapshot (sha256 `953b2860...`) nor the M3JDK master twin has that defect. Both already copy
into two locals and publish them afterwards; the shape dates back to the donor's 4a2e6452a7a. On the production path
(`ARRAY_COPY` = `Arrays.copyOf`, never `null`, never short), leaf B does not change behaviour. What it adds is the
fault-injection seam, the null and length validation of the staged pair, and six tests that keep the paired
publication. The gates show this directly. On master's class, test B does not compile because the seam is absent.
On a shim that puts master's unchanged body behind the seam (`MUTANTS.tsv` row `SHIM_B`), 2 of the 6 tests fail: the
null-copy and malformed-copy tests, which need the validation. The two publication-order tests pass on that shim.
Mutant `B1` reintroduces the defect the donor describes (publishing the object lane before the id copy), and the
relocated test kills it (3 of 6 tests fail).

## Mechanics (recipe-first, no hand edit)

`apply.py` reads `EDITS.tsv` and `PROVENANCE.tsv`. It imports the functions of
`recipe/transient-facade-20261106/relocate.py` (`read_map`, `relocate_file`) and reuses that recipe's
`RELOCATION_MAP.tsv` (PACKAGE, TYPE `LazyElementFlags -> M3LazyElementFlags`, EXTERNAL junit rows).

* `MERGE3` rows: the donor's hash-pinned pre and post snapshots are relocated by the map. The RELOCATE anchor line
  of base and theirs is set to the host's own anchor, because that line is not donor content. Then
  `git merge-file ours base theirs` runs with ours = the pinned M3JDK preimage (git blob `2532106fc847`, master
  f1cdc3672c5). The leaves apply serially: B's ours is A's result.
* `FILE` rows relocate the two new donor tests (M3JDK header, mapped package, one RELOCATE anchor naming this recipe).
* `ORACLE` rows relocate the two default-package donor oracles into `oracle/jdk/m3/collections/` (package
  `jdk.m3.collections`). Their bodies are unchanged.
* The two `MERGE3` rows each insert one `//FLLDCIM: MRL` anchor: above `insertCreated` (and its Javadoc), and above
  `ensureReusableCapacity(int)`.

Each chain starts at a pinned preimage, and each row pins its postimage sha256. All pins are verified before the
first write. A drifted target is refused (exit 2) and nothing is written. A second run reports `changed=0`.

### Per-hunk mapping and 3-way result (`HUNKS.tsv`)

The pre-state of the twin equals the relocated donor pre-snapshot byte for byte. For leaf A, `ours_vs_base_hunks=0`,
so the 3-way merge degenerates to theirs. For leaf B, ours differs from base by one line (A's MRL anchor), far from
B's hunks. Conflicts: 0. Hunks are difflib opcodes; ranges are `start,length`.

| Leaf | Hunk | Donor pre -> post | Relocated base -> theirs | Host ours -> result | 3-way | Change |
| --- | --- | --- | --- | --- | --- | --- |
| A | 1 | 160,1 -> 160,1 | 159,1 -> 159,1 | 159,1 -> 159,1 | CLEAN_THEIRS | `expose`: `insert` -> `insertCreated` |
| A | 2 | 195,1 -> 195,1 | 194,1 -> 194,1 | 194,1 -> 194,1 | CLEAN_THEIRS | retained `withInternal`: `insert` -> `insertCreated` |
| A | 3 | 443,0 -> 443,28 | 442,0 -> 442,28 | 442,0 -> 442,28 | CLEAN_THEIRS | new private `insertCreated` (+ Javadoc) |
| B | 1 | 414,0 -> 414,9 | 413,0 -> 413,9 | 413,0 -> 413,9 | CLEAN_THEIRS | `ensureReusableCapacity(int)` delegates; seam declared |
| B | 2 | 415,0 -> 424,1 | 414,0 -> 423,1 | 414,0 -> 423,1 | CLEAN_THEIRS | `Objects.requireNonNull(copies, "copies")` |
| B | 3 | 417,2 -> 427,7 | 416,2 -> 426,7 | 416,2 -> 426,7 | CLEAN_THEIRS | staged copies via the copier, null + length checks |

Name and package mapping: `com.synexia.primitives` -> `jdk.m3.collections` and `LazyElementFlags` ->
`M3LazyElementFlags`. No hunk line carries a mapped name. The relocated head equals the donor post-snapshot except
for 13 lines: the header and package (3), the RELOCATE anchor (1), 7 `M3LazyElementFlags` lines that the relocation
already renamed, and the 2 new MRL anchors.

## Gates

`GATES.tsv` has the commands and numbers. The tools are `tools/mutants.py`, `tools/classscope.py`,
`tools/regression_oracles.sh`, `oracle/oracles.sh` and `oracle/classify_diff.py`.
`oracle/DoopFacadeFixesDifferential.java` is the seeded differential over the existing public API.

```sh
python -I make/m3/collections/qualification/doop-facade-fixes-20261107/apply.py --donor-git <blobless donor clone> [--check]
```
