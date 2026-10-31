<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Fresh qualification of retained wrappers

The current four-owner wrapper dependency closure compiles with Java 21 using
`--release 21 -Xlint:all -Werror -implicit:none` and an empty source path. The
unchanged historical JUnit gate passes **1/1**, with zero skips, aborts, or
failures. Its original `readonly-policy` contract performs **25 checks**.
Compiler output is empty. No production source or historical fixture was changed.

The complete preserved standalone oracle was also executed once per mode. It
has **five passing modes and four failing modes**. The default `readonly` mode
and `comparator-failure` stop at the same first failure, so these represent three
distinct unresolved findings, not four independent defects. This is bounded
retained-wrapper evidence, not a claim that all wrapper contracts pass.

## Actual outcomes from this run

| Original mode | Exit | Fresh result |
| --- | ---: | --- |
| `readonly-policy` | 0 | 25 checks pass: unconditional iterator removal refusal, reference identity, lazy construction, and preserved callback/exception effects. |
| `readonly-effects` | 0 | Eight checks pass for the preserved callback tape, exception identity, and retry behavior. |
| `bounded-map` | 0 | 64 checks pass against the materialized map oracle, including ascending and descending ranges. |
| `bounded-set` | 0 | 64 checks pass against the materialized mutable-set oracle, including ascending and descending ranges. |
| `interior-null` | 0 | All six original lawful-comparator checks pass on the current implementation. |
| `readonly-linear` | 1 | Original work bound fails: 787,456 predicate calls for a 1,024-element backing list. Later assertions are not reached. |
| `readonly-failfast` | 1 | Original backing-mutation witness does not throw `ConcurrentModificationException`. Later assertions are not reached. |
| `comparator-failure` | 1 | Original read-only bounded-map witness swallows the comparator-owned `IllegalArgumentException`. The later mutable-set assertion is not reached. |
| `readonly` (original no-argument default) | 1 | Stops at that same comparator-owned exception witness after the policy checks. |

Counts overlap between modes and the JUnit invocation; they are not a count of
unique assertions. The original oracle and its no-argument default are intact.
The accepted read-only guard remains the exact historical postimage; this run
does not introduce a traversal optimization or change callback demand. Historical
pending-null documentation remains historical provenance, while the current
six-check null-navigation result is explicitly passing.

## Source and API custody

Production inputs were read with `git show` from the immutable local collections
tree **`985d672bf250aa090b2399d626f0e3e1b541135d`**, using the repository
`/workspace/scratch/53bcd62c244c/M3JDK21`. No working-tree sources were copied.
The parent source inventory identifies target commit
`95d0266ff15ae1f11876cce7360b155ef33a1862`; the execution pin is the exact tree.

The closure consists of `M3FilteredLists`, `M3FilteredViews`,
`M3MutableFilteredViews`, and their package-private `M3SlotSpliterators`
dependency. All other production dependencies are in `java.base`. Public factory
signatures and JVM descriptors were captured by the actual JDK `javap` command
in [public-api.log](public-api.log).

| Owner | SHA-256 |
| --- | --- |
| `M3FilteredLists.java` | `032bffd456e67c7568b028489ceec85f24510469b3a0e00571ab36cd17ae3b75` |
| `M3FilteredViews.java` | `8419315c3058bce6add897b7ffa7c6763bbb6b85418bb35e190ba8a4d2dd70c4` |
| `M3MutableFilteredViews.java` | `3c65c0d918b6cb39d4c27f98699fa97067d2aa3a7fb76ea48b2fd6092a9d0178` |
| `M3SlotSpliterators.java` | `9257ce0c9c95e3afef20624631ba048aa3d60ef53a1189cd2bbabc07da2bba2f` |

The exact `M3WrapperConvergenceContract` and `M3WrapperConvergenceTest` fixtures
come from the published `wrapper-contract-v2` and `wrapper-junit-v2` recipe
custody. Their SHA-256 values are respectively
`2913aeae2f20b80c669398e5727ad9bc000eccf3bb225d85bdd672c17d4fb847` and
`fe071ef7e801a9bb1d57501512f0dd606f0c08652e100e6132dc7c4e4a8f6327`.
Both were checked against their exact manifests and the custody inventory's Git
blob identities. Historical `PASS.md` records are not counted as fresh tests.

The current `M3FilteredLists` Git blob is
`10c4d16cc9cd62d36f22646988718422caa2311e`; its bytes also equal the accepted
historical read-only guard postimage. All local input hashes and all four Git
owner hashes were checked again after execution. [SOURCES.json](SOURCES.json)
and [SOURCE_SHA256.tsv](SOURCE_SHA256.tsv) contain the detailed pins.

## Execution and limits

Execution used the actual **Temurin 21.0.12.1+1** JDK at
`/workspace/scratch/e736b6177c79/m3fs-vfs-20261020/toolchain/jdk21`, with the
existing unmodified generic JUnit launcher and pinned JUnit 5.10.2 / Platform
1.10.2 dependencies. The fresh compile produced 36 class files, containing only
this owner closure, the two original fixtures, and the launcher. There are no
production class overlays. The standalone modes ran without JUnit jars on their
runtime class path.

[COMMANDS.json](COMMANDS.json) records the exact compile, API inspection, JUnit,
and standalone commands, exit codes, and log hashes. The top-level
[command-receipt.json](command-receipt.json) records the driver invocation.
[RESULTS.json](RESULTS.json) is the final machine-readable report. The finalizer
only corrected an initial static narrative to the observed passing interior-null
result; it changed no command outcome and reran no test.
The exact superseded initial receipt is retained as
[RESULTS.initial.json](RESULTS.initial.json); its static null-failure narrative
is not a current result. [RESULTS.amendment.json](RESULTS.amendment.json) records
both report hashes, the correction reason, and the unchanged execution fields.

This is one fresh retained-wrapper gate. It does not qualify the complete
collections module, Maven, native code, performance, or a source-built OpenJDK.
The unresolved comparator, iterator-work, and fail-fast witnesses remain in the
unchanged fixture. No source repair or new contract policy was attempted.

`PUBLICATION_FILES.tsv` lists the text evidence and exact source snapshots to
retain. Compiled binaries remain local execution artifacts and are represented
by [CLASSES.json](CLASSES.json).
