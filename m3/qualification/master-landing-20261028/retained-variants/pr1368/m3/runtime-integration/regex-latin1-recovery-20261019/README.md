<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Actual String owner and regex recovery proof

This is a fresh 2026-10-19 proof packet. The earlier executor's uncommitted
candidate files and raw 2026-10-18 logs are unavailable. None of those historical
passes, timings or digests is represented as retained evidence here.

The receiving phase remains **STRING**. This packet exercises the actual complete
M3String owner classes on a Java 21 host and the actual production C JNI entry. It
does not load a replacement platform String.class, build an M3 VM image, run the
whole jtreg/platform matrix, or qualify HotSpot/GC/CDS/JVMTI and all eight collection
backends. Those remain separate product gates.

## Existing owners and behavior

The changed production owners are `java.lang.M3String`, `java.lang.M3StringRegexDp`
and `libjava/M3StringRegexDp.c`. The full clipped visible-leaf cursor window is the
exact complete M3String.java from target PR #1371, commit
`9f0ee2fb57699e1d94b7133691664be7bf5eeb8c`. It retains code-unit coordinates,
arbitrary forward/reverse reads, nested slice clipping and repeated leaf occurrences.
No String/cursor public API or canonical storage owner is added.

The regex baseline is the complete Java/C from target PR #1376, commit
`46f8ad628ce66fbc8b41895f0146edbe00e3aca5`, including its corrected bounded epsilon
closure and validated native sparse/dense dispatch. The fresh candidate adds a
single physical-width-one dispatch in the existing Java/C owners, checking dead
state at 64-unit block boundaries computed from remaining length. Zero frontier
is absorbing; physical bytes cannot hide a surrogate. This permits a byte atom's
dead tail to be skipped after at most one current block. The earlier per-unit
candidate v1 is retained as rejected: three of its cases exceeded the unchanged
1.10 median/two-of-three paired rule. This v2 candidate has no relaxed gate. The original generic Java loop and complete native
validation prefix and original scan loops are retained byte-for-byte. Physical
width two keeps its late-surrogate UNKNOWN behavior even when its logical coder
is Latin1. Tuple recursion still visits later children, and owner reachability
fences, exact range/cache identity, grammar thresholds and caller fallback remain.

The authoring baseline is target commit
`0d24944a633e9b5ce226c287b333124993b4c477`. The receiving parent advanced to
`a0e9d6c0c95960deb21d45f3800e592b890a4d28`; the 18 Java source dependencies and C
owner used here have no overlap with that complete 232-path change. Direct current
blob checks also cover all 15 owners, String.java and the native C bytes. The
new receiving parent is preserved; its separate image receipt does not qualify
this new candidate image.

## Canonical recipe authority

The source engine is the unchanged Synexia PR #12379 SDK at
`dba1bcd0cb1c1e8249cce19be95b466f797b4fb7`. Canonical recovery manifests/templates
use the existing Java and text snapshot recipes, test and focused POM. No source
applicator is added to the target runtime.

| Dataset | Recipe |
|---|---|
| `m3-catalogue-regex-latin1-recovery-v2-20261018` | `com.synexia.m3.M3CatalogueRegexLatin1RecoveryV220261018` |
| `m3-catalogue-regex-receiving-recovery-v2-20261018` | `com.synexia.m3.M3CatalogueRegexReceivingRecoveryV220261018` |

The dataset suffix follows the existing unchanged POM resource glob. This packet's
recovery and evidence date is 2026-10-19. The first dataset transforms the published
closure Java/C. The receiving dataset applies the three selected owner postimages
and these target-native fixtures to the exact current receiving preimages. Recipe
first pass, reverse input order, fixed point, drift/missing/duplicate refusal and
independent-worker execution must pass before actual source compilation is admitted.

## Fresh runtime sequence

Keep this README, the runner and its source-seals.json byte-identical. Any later
publication handoff belongs in a new adjacent document. The runner uses source
seals to reject stale fixtures, support symbols and production inputs.

`--repo` is the applied receiving checkout (or the exact SDK-emitted union).
`--symbols` is its `src/java.base/share/classes` directory or an exact pinned
symbol checkout. Only the 18 named source files are copied into an isolated source
context; 15 complete M3String owners are compiled. The three remaining files are
compiler context only. `-implicit:none`, strict warnings and class-load assertions
prevent accidental String.class or replacement helper emission. The two changed
owners are checked against their own source seals rather than the old symbol copy.
`--before` is a checkout containing the exact #1376 Java/C paths. The full-window
cursor is held equal on both sides of the width1 experiment.

Run the following stages in order with a new output directory:

```sh
python3 run_actual_owners.py --stage static --jdk "$JDK21" --repo "$TARGET" --symbols "$SYMBOLS" --before "$CLOSURE_BASE" --output "$PROOF"
python3 run_actual_owners.py --stage compile --jdk "$JDK21" --repo "$TARGET" --symbols "$SYMBOLS" --before "$CLOSURE_BASE" --output "$PROOF"
python3 run_actual_owners.py --stage test --jdk "$JDK21" --repo "$TARGET" --symbols "$SYMBOLS" --before "$CLOSURE_BASE" --output "$PROOF"
python3 run_actual_owners.py --stage benchmark --jdk "$JDK21" --repo "$TARGET" --symbols "$SYMBOLS" --before "$CLOSURE_BASE" --output "$PROOF"
```

Every attempted stage retains commands, exit codes, output seals and child CPU/RSS
observations. A failed stage cannot be overwritten. The runner seals its isolated source context
after static setup and its compiled classes/headers/libraries after compilation;
later stages refuse any missing, extra or changed intermediate artifact. The
benchmark selection may
reject a candidate even though the measurement stage executed successfully; read
`BENCHMARK_SELECTION.json`, not only the stage status.

The owner test calls actual private closure/Java-kernel methods and actual public
dispatch through canonical atom/tuple objects backed by temporary test-owned native
memory. It covers exhaustive closure graphs, boundary/random frontiers, 7/8 and
31/32 token transitions, accepting-only/zero incoming state, high bytes/NEL, both
UTF-16 byte orders, exact slices, rolling plans, cold/warm effect caches and a later
tuple child's surrogate. The retained cursor corpus uses an independent char-array
oracle, nested/repeated slices, empty children, reverse/random access, surrogate seams,
streams and large logical coordinates. These tests do not simulate canonical VM
admission or a GC reclamation race.

Java owner/cursor tests run under JIT and `-Xint`. Production C compiles as its own
translation unit under C17, `-Wall -Wextra -Werror -pedantic`, both normal and UBSan
configurations. Both the adapter corpus and real owner native dispatch run with
`-Xcheck:jni`, with required-native fallback rejected. Full owner member descriptors,
the actual generated regex JNI header and the two expected JNI exports are checked.

## Frozen bounded performance decision

The 24 cases are 10 prepared Java kernels (five scenarios × physical widths one/two)
and 14 actual production-C JNI cases (seven scenarios × both widths). Java consumes
8192 units; native consumes 256 units, with an additional zero-length validation case.
Java scenarios are dense-live, small-live, early-dead, late-dead and incoming-dead.
Native also separates sparse-live and validation. Sparse-live retains only two
consuming frontiers throughout its input.

Each case has 2000 warmup calls and seven measured 5000-call samples. Each side runs
in three fresh paired JVM forks, ordered before/after, after/before, before/after.
Reject the candidate if any case's median paired after/before ratio exceeds 1.10
and at least two of the three individual paired ratios exceed 1.10. Every pair is
retained. No throughput promise or universal memory/performance conclusion follows
from this host matrix. Cursor latencies are outside this width1 selection matrix.

Java timings include the prepared MethodHandle call. JNI timings include the fixture
buffer borrow. Preparation cost, first-call latency and thread allocation counters
are reported separately. Temporary native owner bytes are counted by the fixture.
The production C scan makes no explicit heap allocation by source inspection;
JNI implementation native allocation is unmeasured. Per-process RSS is a high-water
mark, not retained Java heap. The exact production primitive-cache capacity remains
588544 bytes; object/header/allocator overhead and full VM retained memory are outside
that bound.

## Source and license lineage

The Helix `regex-cursor` reference is commit
`fd4c51c73587981219daf9a36184aabbddae78ee`, MIT OR Apache-2.0, copyright Pascal Kuthe.
It supplied chunk-state and navigation ideas only. No Rust code is copied, no byte
index is used as a UTF-16 coordinate, and unbuffered streaming is not claimed.

First-party recovery scripts and probes retain Apache-2.0 notices. Reused graph and
JNI fixtures retain their published notices. The closure corpus is reused under its
offered Apache-2.0 option. The complete cursor test and generated production Java/C
retain their original GPLv2/Classpath notices; the recipe license does not relicense
those payloads. Exact original fixture pins and complete per-file hashes are retained
in canonical custody records.
