# M3 JDK consolidation authority

This packet freezes the source and target revisions used for the next consolidation wave.

The product target is the matched M3jdk21 image. Synexia remains source/provenance, recipe-development and comparative-test input; it is not a runtime dependency of java.base.

## Invariants

1. Java semantics are authoritative. JNI/HotSpot paths must prove parity.
2. Exact UTF-16 code-unit behavior is preserved, including NUL and unpaired surrogates.
3. Persistent identity uses stable owner/namespace/generation/offset geometry, never process pointers.
4. Source-changing work is recipe-first. A reviewed recipe and fixed-point/refusal evidence precede materialization.
5. Static signals, hashes and challenge-site evidence nominate or reject candidates; they never prove semantic equivalence.
6. LeetCode, HackerRank and GeeksforGeeks are taxonomy/constraint/counterexample evidence only. Code reuse requires separately pinned and license-reviewed donors.
7. The corrected SharedArrayPool geometry from Synexia #9083 is not promoted until its one-row multipage nonzero-language regression, reopen path and native parity execute on the exact source head.
8. M3-enabled acceptance must include compiled execution; interpreter-only evidence cannot close JIT gates.

## Required product closure

The final exact-head packet must contain a complete JDK image build and focused/broad regression evidence for String/Unicode/regex, JNI String APIs including modified UTF-8, C1/C2, GC/string dedup/interning, CDS, JVMTI/JFR/serviceability, shared-generation lifetime, native/platform behavior and retained-memory/performance comparisons.

No historical receipt is silently promoted to current evidence.
