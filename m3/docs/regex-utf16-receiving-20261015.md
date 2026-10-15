<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3 regex UTF-16 boundary receiver

Source parent: `hsoliwal/com.synexia@8587fb3c094cf1cc7e5fa15748eb744e013785c2`.
Target parent: `hsoliwal/M3jdk21@c8af42d9a6df6f3fd2c648ff7f8d98cf086fb87f`.
Recipe: `com.synexia.rewrite.M3Jdk21RegexUtf16Recipe`; canonical recipe and tests remain in Synexia.
This stage is a target-owned partial receiver, not full regex/HotSpot or compiler-image delivery.

## Same owners; explicit new capability

`M3RegexDpPlan.summarizeUtf16(CharSequence,Runnable)` adds code-point-correct input decoding for
its existing ASCII-alphanumeric/dot/star pattern grammar. `summarize`, `supports`, `advance`,
and all existing public descriptors retain their earlier non-surrogate contracts. No flags,
character classes, capture offsets, assertions or lookarounds are added. `Pattern` remains the
full Java regex and syntax authority. Public String/Pattern/Matcher interfaces are unchanged.

The decoder follows the delayed-high shape already used by M3StringFacts.scan and its seam
logic in compose, while effect composition reuses Synexia's existing fragment-relation idea.
This is a target-owned implementation, not copied challenge code or a donor runtime dependency.

An Effect carries an open transition relation which excludes a final high surrogate. It has
one boolean recording that pending boundary and, only when the fragment begins with a low
surrogate, a second relation for the fragment with that first unit omitted. No actual surrogate
value or text is stored: for this strictly ASCII literal grammar both supplementary characters
and lone surrogates have exactly the dot predicate. Widening literal predicates requires a new
boundary-state design and compatibility proof; these flags cannot encode arbitrary Unicode literals.

If the left effect has a pending high and the right begins with a low, composition consumes
one dot predicate and the right's skipped-first relation. Otherwise it flushes the lone high
before the right relation. An empty fragment is a true identity. Complete-input `apply` flushes
any remaining high without mutating the open relation. Parenthesization does not change the
answer. Repetition uses the same boundary-aware composition, so a low-high fragment repeated
creates pairs across copies rather than treating each copy as an independently closed string.

Boundary-free effects retain existing Java/native power routing. Boundary-bearing powers use
logarithmic composition and the existing JNI composition functions where eligible. All native
source, headers' JNI declarations and the three exports are unchanged in this stage; no text
or decoder state is passed across JNI. Native absence retains Java fallback. Every supplied
checkpoint in repeated/then retains the previous schedule and overflow/negative-count precedence.

## Receiver, costs and retained metadata

The existing M3StringRegexDp receiver first uses its proven legacy summary. Only after a surrogate
refusal does it charge a second bounded scan and call summarizeUtf16 on the immutable canonical
fragment. Ordinary scalar inputs still retain their original JDK fallback. This extends exact
optimization admission for the existing repeated canonical cohort, not the public regex language.
The original owner/coordinate/plan identity checks, cache slots, soft-reference pressure policy,
source limits and recursion cap remain. Composition and powers are conservatively charged for
two row relations plus a deferred predicate, without raising MAX_EFFECT_WORK.

Only Effect metadata gains two private fields; String, M3String and canonical owners gain none.
The optional additional long[q] array costs at most 512 payload bytes per effect (q<=64).
For the existing 64 plan/128 effect slots the worst array-payload bound is now
192*68*8 + 128*2*64*8 = 235520 bytes, up from169984. Object headers, references, scalar fields,
active concurrent cache generations and temporary work are excluded. This is a capacity bound,
not a heap measurement or strict instantaneous-residency guarantee. Soft clearing is not LRU.

New preparation is O(UTF16_units*q), at most two owned row arrays, one ordered read per unit and
no toString/substring/export. Legacy preparation retains its prior fixed-point optimization.
Boundary composition is O(q^2); boundary powers are O(q^2 log(count+1)), O(q) live row payload
and O(q log(count+1)) total allocated row payload. Boundary-free power retains three Java work
rows or the prior one-call native implementation. A metadata-only carrier is not canonical text.

## Qualification boundary

The paired Synexia task tests actual generated plan and existing target-symbol JNI, including
all 65536 single units, all1048576 valid surrogate pairs, fragment seams, empty fragments,
associative composition, arbitrary frontiers, repetitions, overflowing lengths, callback traces,
old entry-point refusal, missing-library fallback and shared-effect concurrency. It also reuses
previous scalar/pressure/native regressions and kills malformed boundary implementations.
The source-built M3JDK jtreg test is provided separately. Receiver control tests use explicit
TEST_ONLY canonical-owner/cursor fixtures: their results are not actual HotSpot storage proof.

Maven/OpenRewrite SDK, full reactor/JDK/libjava build, actual M3/CompactStrings jtreg, Windows,
retained/native memory and end-to-end benchmarks remain pending. No general performance gain
is claimed. Full parsing/capture/find/images/compiler lowering, strict LRU/payload spilling,
whole-estate catalogue, DOOP/SWT/Swing and homepage rollout remain separate open requirements.
