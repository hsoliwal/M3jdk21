<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 direct regex DFA token/composite receiver (2026-10-18)

This is a target-owned `java.base` enhancement, **not M3 + JDK**, a separate regex
interpreter, a collection adapter, or a declaration that Java Pattern/Matcher has
been completely replaced. The existing `jdk.internal.mindex.M3RX7` remains the
runtime implementation owner. Synexia owns only its reusable, source-sealed recipe,
algorithm comparison and donors. The `String -> arrays -> collections -> AST/compiler`
receiving sequence is not promoted by this narrow change.

## Changes and invariants

- `M3RX7.matchesCandidate(CharSequence)` consumes continuous UTF-16 units through
  `charAt` without `toString()`, `substring()` or a flat copy. The existing
  `matchesCandidate(String)` descriptor stays available.
- `matchesChunksCandidate` consumes caller-supplied chunk coordinates with one
  continuous DFA frontier; matches are not independently restarted at seams.
- `prepareTokenIndex` composes the existing DFA transition table with packed
  ASCII token slices, returning state-major `short[]` transition lanes and
  `long[]` valid-token masks; backwards reachability uses only primitive index
  arrays. No `Map.Entry`, per-state objects, token payload retention, or
  second canonical spelling owner is introduced.
- The optional libjava JNI kernel computes bounded state/token rows directly
  from primitive arrays. If a compatible native symbol is unavailable,
  Java computes identical rows. JNI neither keeps Java pointers nor passes
  object predicates/comparators or canonical owner references.
- Precompute refuses above 1,048,576 state×token cells, 32MiB transition work,
  8MiB result-array budget, or the runtime free-memory admission gate. It does
  not attempt to cache an entire unbounded 128k-token vocabulary.
- ASCII-only DFA candidates return UNKNOWN (-1) for unsupported Unicode
  during text matching; only admitted ASCII token dictionaries may be indexed.
  No new public `String`, `Pattern`, `Matcher` or HotSpot contract is claimed.

## Source and qualification

Canonical generator/custody:
`hsoliwal/com.synexia/synexia-openrewrite-recipes/recipes/m3jdk21-token-dfa-20261018`.
It checks the exact old Git blob and every sealed postimage and rejects drift.
Do not patch generated output after recipe execution; correct the recipe,
rerun, then compare file-by-file.

Host OpenJDK 21 patch-module oracle: **36,020 assertions PASS**, covering
arbitrary token composition, partition seams, non-flattening input, state
reachability, validation and mutation of temporary inputs.
Native C17 `-Wall -Wextra -Werror -fsyntax-only`: **PASS**.
These are component-level checks, **not** a source-built M3JDK21 receipt.

**NOT_RUN/HOLD:** full M3JDK21 java.base/HotSpot compilation, jtreg using its
actual runtime, linked libjava/JNI execution and Java-vs-JNI parity, MSVC/ARM,
GC/JIT/compact-string behavior, `Pattern/Matcher` captures/find/regions,
resource-pressure stress, JMH/JFR/RSS regressions, complete SourceSeal SDK
execution, and the broader collection-family/runtime acceptance. Do not merge
on the strength of component tests alone.
