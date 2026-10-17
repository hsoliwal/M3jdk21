# Required UTF-16 n-gram precompute in M3 String — 2026-10-17

This is the M3JDK21 **runtime receiver**, stacked on product PR #1004 and canonical Synexia recipe PR #11999. The first-party donor owns algorithm/recipe provenance. JDK21 java.base/HotSpot/JNI still own execution and String semantics.

## Before → after

**Before:** `M3StringRegexDp.Plan` precomputes minimum length, mandatory literal character bits and fixed first/last units; `rejectsByPrecompute` consults prepared canonical source facts. A candidate with all required letters present (but impossible adjacency) still pays regex DP/effect/JNI work.

**After:** the *same* Plan derives two additional 64-bit OR masks for mandatory adjacent 2-grams and 3-grams from non-starred ASCII literal runs. `M3StringFacts` already stores those exact UTF-16 n-gram signals. A prepared source missing a required mask is a proven non-match. Absent facts or colliding/present bits fall through to existing full DP/native/Pattern logic. Stars or dot atoms reset adjacency; regex over regions/search/captures/backrefs remains with JDK Pattern/Matcher.

Examples:

```
source = "abXXbcYYac"; pattern = "a*abc.*";
letters present: a,b,c ; required pairs present: ab,bc ; required triple: abc ABSENT
=> no regex engine traversal required once canonical facts are ready

source = String.join("", "ab", "cXX"); pattern = "a*abc.*";
=> regex matches; the precomputed triple MUST include atom/tuple-seam adjacency
```

## Source identity and verification

* `M3StringFacts.java` source-before SHA256 `2152c154d3cdece330667ee09675fd21d86f3b483a23a3156b8a3626c3f9f420`; source-after `d18122b42e9e4bcb76fd3e807467a59160e012d7ac249efe9cc71e5f6b8faa22`.
* `M3StringRegexDp.java` source-before `4972d8c3a664867559273a63df340c042eab1fa40f21e7b2139422ed06f942ba`; source-after `9536a6df2042fe047f02ef07834edca91ce7a863e5b3c1daafffe55fdad48e6e`.
* `M3StringRegexDpTest.java` source-before `23b49a26ce30586758afb8cc7a7fecaee5a70833943fd3598c5b703d2a516cd9`; source-after `971290714bb056831ad844288e66562f88fa87fbf0c4cd17fa0716f9df797ffc`.
* Input/output versions are exact Synexia hash-pinned template bytes, not hand-modified generated output. Receiver guard checks all exact source stamps. Previous regex amalgamation/cursor source-extracted tests keep their historical before images and validate the current additive source.

## Cost and boundaries

* Only two `long` fields per prepared Plan; at most +5,120 primitive bytes over 320 potentially resident current/previous plans, separate from JVM object padding.
* No copied payloads, per-string fields, extra regex parser, new cache or JNI ABI.
* All filters require `factsIfPrepared()`; no automatic text scan solely for a speculative reject. Missing facts do not change correctness.
* Unicode surrogate and full Pattern syntax semantics remain with fallback. Collision/presence proves no match truth.
* Independent stock Java21 conservative filter oracle passed 130,000 checks (20,049 known true matches, 53,670 newly rejectable candidates) but is **not** a modified-JDK benchmark.
* Full M3JDK21 source build, jtreg, JCK, GC/JIT/CDS, optional JNI and measured CPU/allocations throughput gates are **not** satisfied by source-extracted tests alone.

Canonical development pipeline: inventory → dated additive convergence → algorithm catalogue → Inline → Atomize → Patternize → Comment → compile/tests → reviewable PR.
