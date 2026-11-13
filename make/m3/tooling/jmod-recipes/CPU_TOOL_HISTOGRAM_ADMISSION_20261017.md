<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 CPU byte histogram / run source admission, 2026-10-17

## Source and contract ownership

Synexia is the canonical Maven/OpenRewrite and donor-comparison owner.
M3JDK21 owns the independent opt-in com.m3.runtime.capability JPMS/JMOD
target. Existing SourceArtifact.Span retains the immutable byte payload.
ByteHistogram256 retains only primitive counters and source coordinates;
it creates no second string interner and never replaces java.lang.String.

## Category and algorithm comparison

| Category | Candidate | Decision | Work / retention |
| --- | --- | --- | --- |
| Byte frequencies | 256 primitive long counters | SELECT | O(N), 2048 counter bytes |
| Byte frequencies | boxed HashMap/Entry graph | REJECT | O(N), per-key objects |
| Byte frequencies | streams groupingBy | REJECT | extra boxing/collectors |
| Entropy | Shannon over exact counters | SELECT | O(256), no model |
| Byte RLE | two-pass immutable span scan + callback | SELECT | O(N), O(1) resident |
| Byte RLE | per-run heap objects | REJECT | O(R) retained objects |
| Native scan | existing LfCount image with optional JNI symbol | SELECT_CANDIDATE | bounded 2 KiB C stack array, one Java long[256] |
| Native scan | new native loader and auto-dispatch | REJECT | duplicate ABI/lifetime |
| Whole CPUTOOLS corpus | full explicit category inventory | CATALOGUE_ONLY | not 110 tool implementations |

Related competitive-programming *shapes* include LeetCode 242 and 350,
HackerRank Sparse Arrays, and GeeksforGeeks frequency tables and run-length
encoding. None of those editorial/submission sources is copied. Existing
Synexia algorithm implementations, JDK NIO, and LGPL/GPL/BSD/Apache donor
rights retain their original exact license boundaries.

## Bounded proof and deferral

The authoring-time strict JDK21 and optional C11 JNI kernel passed with
a temporary API-shaped SourceArtifact/LfCount harness: 21,572 Java,
31,171 JNI assertions (-Xcheck:jni, generated JNI headers).
This is NOT proof of the complete target capability-runtime, source owners,
whole-JDK jtreg, cross-OS JNI or faster-than-Java performance.

Required: qualified OpenRewrite Maven recipe with exact pre/post SHA256,
AST/text parser-safe source admission, SDK generation/fixed point/refusal,
actual target module Java21 and C11 JNI parity, old LF/byte-search
regressions, optional stock JMOD/jlink, modified runtime/jtreg, and
CPU, heap and native/process-memory observations. Continue serial FILE
passes; copy produced source only through the qualified recipe.

The category matrix explicitly distinguishes actual owner, partial
mechanism, candidate, and deferred domain. Never equate a catalogue
with a running SAT/SMT/graph/vision/network tool.
