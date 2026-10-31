<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Original java.base sorted-view and bitmap implementation

M3JDK21 is the JDK. This change refines three original production owners directly:
`java.util.ReverseOrderSortedSetView`, `jdk.internal.mindex.M3BitLane28`, and its existing
`src/java.base/share/native/libjava/M3BitLane28.c` peer. No runtime wrapper, second
collection hierarchy, extra payload owner or Synexia dependency is added.

Canonical source/recipe packet: hsoliwal/com.synexia PR #12442, exact commit
`e6489b972ea0fd045386bf28e57e9acf53bbad6f`, crate
`synexia-openrewrite-recipes/recipes/jdk-sorted-mask-20261019`.
The complete canonical contract, category evaluations, preimages/postimages, replay
fixtures, Maven/SDK tests and evidence remain there. `verify.py` is its exact Apache-2.0
proof-tool mirror (Git blob ae38bd39b85d8afb881758cee5d302d5dbd631dc), not a runtime layer.

## Production behavior

The original reversed sorted-set implementation now separates bound presence, iterator
readiness and removal eligibility from legal null values using state masks. It returns
the backing add result, retains nested bounds, uses comparator-equivalent membership,
and reports SortedSet spliterator characteristics. Subset removal acts on the last
returned representative even after lookahead; backed removeIf/clear/retainAll work.
Fourteen retained baseline witnesses are intentional contract corrections, not claims
of behavioral equality to the broken baseline.

The existing Java/C bitmap counter masks only the endpoint words of each partial page;
interior words are counted directly. Segmented primitive arrays, coordinates, lazy exact
rank/select counts, input validation, JNI Region capture and the existing export remain.
No automatic native dispatch is added. Existing owner names and public JDK APIs remain.

## Run independently

On Linux with a Java 21 JDK, GCC, nm and Python 3, from this checkout:

```sh
python3 m3/qualification/jdk-sorted-mask-20261019/verify.py \
  --target "$PWD" --output /absolute/new-directory-outside-the-checkout
```

No network or Synexia checkout is needed for the target-source run. The script checks
all sealed inputs, compiles actual java.base sources, generates JNI headers, builds
the actual C peer with strict conversion warnings and UBSan, and executes normal,
interpreter and C1 checks with -Xcheck:jni. It writes only a fresh external scratch tree.
For baseline reconstruction and serial transformation replay, use the canonical README's
`--replay --synexia ... --fixtures ...` command. Never call replay against unknown drift.

## Actual focused evidence

The completed baseline-to-candidate replay executed 46 commands, reproducing the same
14 baseline wrapper failures. Each subsequent candidate invocation passed 273297
sorted-view assertions and 212597 bitmap assertions. The bitmap observation digest is
c64eda10cb2ca6b0180df360621ccc404c4fdfe353da3b5dda0e133a54ee40e7.
Interpreter, C1, new-Java/original-native-peer and per-stage checks pass. Each FILE applies
once, replays with zero writes and passes five refusal cases. Additional existing-kernel
in-memory qualification passed 16 ordered subsets and 112 checks. Outer-class public/
protected descriptors, JNI header and native export compare unchanged. These counts are
standalone assertions, not JUnit counts or unique aggregate coverage across reruns.

Execution loads exact source patches into the bootstrap java.base module on the recorded
host Java 21. It is NOT a source-built complete M3JDK21 image or actual jtreg harness run.
The test-only native peer load does not establish complete production libjava linkage.

The one-page warm microprobe reports median Java 172.64905 -> 18.92021 ns/query and
explicit native 304.79731 -> 271.85064. JNI remains slower in this workload; no dispatch
change. Page payload stays 512 bytes; optimized GCC frame stays 624 bytes. Three fresh
alternating JVM pairs, fixed heap, no JMH/CPU affinity; no whole-JDK/heap/RSS speedup claim.
See microprobe.json and the canonical EVIDENCE.md for raw values and scope.

## Pending acceptance

Keep draft. Maven/OpenRewrite SDK/JUnit are authored but not run (Maven/dependencies
unavailable). Full JDK build, production native linkage, jtreg, full reactor, historical
source-pin successor admission, global recipe registration, other platforms and broad
resource/GC/CDS qualification remain pending. This does not complete all collections,
all sorted/filtered wrappers, or the earlier whole-chat program; no phase is advanced.

The sorted-view source keeps its OpenJDK GPLv2 with Classpath Exception header. Existing
Apache bitmap sources and first-party tests/proof tools retain their notices. No direct
challenge/editorial code is copied and recipe licensing does not relicense target source.
Existing historical Apache-in-src licensing admission is not certified by this refinement.
