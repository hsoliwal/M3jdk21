<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Integrated M3JDK21 continuation, 2026-10-21

M3JDK21 owns the actual Java/HotSpot/JNI runtime. This change modifies the existing
`src/java.base/share/classes/jdk/internal/mindex/M3RegexDpPlan.java` and
`src/java.base/share/native/libjava/M3RegexDpPlan.c`; it is not a runtime overlay,
new public String/Pattern implementation, renamed JDK API or Synexia dependency.

The canonical recipe is `synexia-openrewrite-recipes/recipe-crates/m3-chat-convergence-20261021`
in hsoliwal/com.synexia. It reuses M3CompositeIndexReplay/M3GpuToolJoinManifest, with
exact before/after source seals and the existing target-owned implementations as
immutable Git-blob templates. Fetch the receiving branch to obtain those blobs;
recipe execution itself performs no network access. No new recipe engine is introduced.

## Selected behavioral atoms

- UTF-16 all-state effects reuse only exact fixed points under the same predicate,
  or proven absorbing-zero lanes. Source charAt order, every unit read, surrogate
  seams and checkpoint cadence remain intact. The plan still admits only its existing
  bounded ASCII literal/dot/star grammar, not arbitrary Java Pattern syntax.
- Eligible dense, wide, boundary-free powers without callbacks use the existing
  repeat0 JNI entry once. Sparse cases, supplied checkpoints and surrogate-boundary
  effects retain Java execution. Missing native symbols remain optimization misses.
- Native composition reuses exact homogeneous/adjacent frontiers; all input lanes
  are validated before shortcuts. Stack staging preserves overlap behavior. Native
  powers use exact idempotence, never a hash or guessed convergence condition.
- Public members and all three native entry signatures are unchanged. No new
  retained text, pointer, per-character JNI crossing or global workspace is added.

## Reproduction

`python3 m3/compatibility/chat-convergence-20261021/verify-core.py --output /tmp/m3-core-fresh`
compiles the actual owners, generates JNI headers and runs Java fallback, real checked
JNI, forced-portable bit scanning, fatal UBSan and interpreter cases on Linux.
`--java-only` omits native execution explicitly. The script patches only these actual
classes into a stock JDK for isolation; it does not claim a complete M3JDK21 image.

The larger retained review packet also executes split-surrogate/all-entry-state
corpora, native partial overlaps, missing-repeat-symbol fallback and dispatch/work
instrumentation. These results are distinct from this small hosted entry point.
An initial wide stock Pattern oracle timed out; the hosted oracle now bounds that
backtracking comparison while retaining wide-state split/power checks.

## Scope and open gates

No rename is performed; the existing internal regex name-map ownership is preserved.
The String -> arrays -> collections phase remains unpromoted. Source-built JDK,
jtreg/JCK, JIT/GC/CDS, Windows/platform execution, full name/full-borrow gates and
production CPU/heap/native-memory qualification are not established by these tests.
Prior microbenchmarks include sparse-case regressions and larger native stack use;
no universal speedup is claimed. Keep this candidate draft until those gates pass.

Filtered/sorted-view and lexicon repairs are reviewed separately in their existing
owners. ChromeLLM's Synexia #12812 remains a convergence candidate; copying its app
client into java.base would not constitute runtime integration. Broad AST/compiler,
M3FS, semantic/code synthesis, corpus intake and all-provider collection parity are
not silently counted as delivered by this bounded continuation.
