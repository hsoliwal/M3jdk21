<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 2026-11-01c: source-preserving UTF-16 + admission-count fan-in

**DRAFT/HOLD — staged source integration, not installed JDK admission.**

The exact first parent is #2257 at
`e2f10f42ebf36edd3fdfde4deee5f796e0a38a6d`, Git tree
`62fb033c73fa4cdab15f9afd9074bb72020ae050`.
It retained 82 historically frozen open PR HEADs as Git ancestry and 211
independently grafted original source files. Those 82 HEADs are not
equivalent to 82 implemented/qualified source integrations.

## Two real original-HEAD two-parent merges

| Donor | Original HEAD as second parent | Source merge |
| --- | --- | --- |
| #2258 Java/JNI exact UTF-16 identity atom | `31f83f899d421ff196f39ab9a4bbc5e302edb66f` | `e93f8aa16663db79d28c426434a33c0474dae16e` |
| #2259 optional gate FAIL admission count | `86e18b6fb709b9d63c4d929dcf3327cfcaf5f56d` | `0bb4f9a72940f1745f5b604ef909cfa3ae20077f` |

The original five mutable receiver targets were independently checked at
the exact historical donor-base commit and at #2257's source tree. Every
target preimage Git blob SHA matches *exactly* before substitution:

| Target | Original & receiving preimage SHA |
| --- | --- |
| `m3/arrays/src/main/java/jdk/m3/arrays/M3ArrayNative.java` | `a4b03536ff3cc60a4f6a2ddb91c62994fd9a4327` |
| `m3/arrays/src/main/native/m3_arrays_jni.c` | `4d5304b16fc7b9d9258b45fa07f5ae7f577292cb` |
| `m3/arrays/src/test/java/jdk/m3/arrays/M3ArrayNativeTest.java` | `0c1ac8a755e60fb97eb82bf17e44ce206ab8d047` |
| `m3/tooling/atom-census/src/main/java/jdk/m3/tooling/atoms/contract/M3AtomCoverageLedger.java` | `8771fada3220adc50267074c4475e8f438330378` |
| `m3/tooling/atom-census/src/test/java/jdk/m3/tooling/atoms/contract/M3AtomCoverageVerifierSelfTest.java` | `b9b412c3dc78fb1322453019cdb7c6b6c710db97` |

The three #2258 qualification files and two #2259 qualification files
were absent in the receiving tree. GitHub Git Data readback of the
original donor trees showed all ten exact resulting file modes as 100644
and sealed each postimage by its original blob SHA. There is no text
reconstruction, code transplant from challenge sites, or reformatting.

**GitHub first-parent compare:** 10 paths changed, 5 added, 5 modified,
0 deleted, 0 renamed, zero commits behind, final pre-receipt tree
`e664d7c35cb487cdcb917c521d16c9eb086e4620`.
No old fork or original donor head was rebased, squashed, deleted or
force-updated. No `master` write occurred.

## Required HOLD before promotion

The two implementation changes remain contract-locked to their original
owners and source licenses. The full canonical Synexia Maven/OpenRewrite
SDK, fixed-point/refusal, source-built Java21 JUnit/jtreg, HotSpot C1/C2,
actual JNI/C17/ABI, GC, platform matrix, installed image, release gating,
and performance tests are **NOT EXECUTED on this combined head**.
A previously passing donor test does not certify this new integrated head.

The rest of the 84-open / 219-closed-unmerged M3JDK21 discovery snapshot
is not folded live here. Keep unreconciled paths and source variants
as HOLD; this staged PR must not be promoted to `master` until qualified.
