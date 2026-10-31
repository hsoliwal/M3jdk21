<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 Regex derivative Java/JNI receiver — isolated qualification

Copyright 2026 Hitesh Soliwal and contributors.

Status: **review candidate**, not java.base promotion or public regex implementation change.

## Provenance and ownership

Canonical convergence/recipe owner: `hsoliwal/com.synexia` at
`892e3f6c51d78bc0948e8a8cf736055b4c70eafa`. The source-blob-to-target
table is [DERIVATIVE_REGEX_PROVENANCE.tsv](DERIVATIVE_REGEX_PROVENANCE.tsv).
Source belongs to Synexia first-party Apache-2.0 code. No solution source from
LeetCode, HackerRank or GeeksForGeeks is copied. OpenJDK files remain unchanged.

The published target owns `com.m3.precompute.M3RegexDerivative` and
`M3RegexPredicateAlphabet` as independent receiver code. The canonical
portable source/recipes remain in Synexia. `java.util.regex.Pattern`,
`Matcher` and String contracts are not changed by this patch.

## Typed algebra and canonical precompute

The typed expression domain includes `empty`, `epsilon`, literals,
concatenation, alternation, star, intersection and complement.
Derivative closure is hash-consed, memoized and bounded before publication.
After closure the compiler precomputes:

1. nullable/accepting bytes, DFA state partition minimization;
2. alphabet minterm-to-execution-class quotient;
3. reverse live/dead reachability and viable-next-class masks;
4. power tables for repeated equal-class runs (`T, T², T⁴, ...`);
5. deterministic program SHA-256 and primitive-byte accounting.

All matching state is numeric; the input shadow stores RLE predicate classes,
not a second canonical spelling buffer. This is a non-backtracking regular
language matcher, **not** a general Java regex replacement. Non-surrogate BMP
UTF-16 only: surrogate input and unsupported semantics refuse explicitly.
In particular no captures, Java backreferences, flags, arbitrary lookarounds,
regions, `find`, `lookingAt`, Unicode case folding or Java-specific boundary
rules are claimed. Full `java.util.regex` semantics remain authoritative.

`asciiEffects()` borrows the existing `M3RegexDfaEffects` compositional
execution owner and its optional `NativeJoin` ABI. It does **not** create
another fragment-expression cache. Only ASCII fragments can use this route;
non-ASCII input must use the direct supported matcher or the existing JDK owner.

## JNI/native ownership

The C11 kernel consumes a frozen integer run-power image with
`[bit][class][state]` addressing; it retains no Java heap pointer or input.
The JNI entry point is target-named and loads the existing
`m3_precompute` native library. Java remains the oracle; JNI execution is
optional and explicit JNI request fails closed if unavailable/incompatible.
CMake's strict C warnings and native CTest cover geometry/malformed-input
refusal. The existing JVM loader owns the shared library lifecycle.

## Verification commands

```sh
mvn -B -ntp -f m3/ports/precompute/pom.xml -Dtest=M3RegexDerivativeTest,M3RegexDerivativeEffectsBridgeTest test
mvn -B -ntp -f m3/ports/precompute/pom.xml -Pnative -Dtest=M3RegexDerivativeTest,M3RegexDerivativeEffectsBridgeTest test
```

Prior donor evidence is not M3JDK21 acceptance. The target Maven and native
runs, OpenRewrite exact receiver replay/fixed-point, full matched-image
java.base/jtreg and CPU/startup/retained-heap/native-memory measurements are
**PENDING until actually executed and attached to this PR**.

## Algorithm-category catalogue

| Category | Candidate/owner | Reason and status |
| --- | --- | --- |
| Regex parsing/equation derivative | Synexia MIndexDerivativeRegex, Brzozowski derivatives | Reuse pinned first-party implementation; direct adapted copy with attribution |
| DFA canonicalization and minimization | Synexia symbolic derivative compiler | Compatible with typed regular-algebra full matching; bounded state transition closure |
| Repeated identical symbols | binary lifting and RLE-state powers | Reuse qualified donor arithmetic; no repeated tree derivation in hot path |
| Ordered composition/range query | Existing M3RegexDfaEffects, AtCoder segtree catalogue | Reuse existing target execution; no new parallel effect engine |
| LeetCode 10 regular-expression matching | category/contract evidence only | Dot/star subset oracle; **not** a comprehensive Java regex reference and no solution copied |
| HackerRank regular expressions | challenge category evidence only | Cases to catalogue for future proof; no unlicensed solution copied |
| GeeksForGeeks regex derivatives | explanatory/algorithm evidence only | Independent review required before suggesting promotion |
| Microsoft RE# symbolic Boolean derivatives | architecture research reference only | No code copied, semantics may differ |
| Native C11/JNI | Synexia derivative C kernel + existing M3 precompute ABI | Adapted symbols, strict bounded geometry, optional parity gate |

## Broader receiving order

Follow `m3/docs/m3jdk21-porting-invariant.md`:
String/regex qualification → arrays → collections → AST/compiler.
Filtered and sorted collection wrappers require separate concrete JDK
collection contract proof (backed views, fail-fast/snapshot semantics, nulls,
order, iterators, serialization and concurrency). Their work remains explicit
**PENDING**, not silently merged with this String/regex receiver.
