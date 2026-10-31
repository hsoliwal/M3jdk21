<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 typed-token predicate matching

This continuation builds on M3JDK21 #1373 (`872e70d20f3f3223a72eb5f67b001528b7fb2cb6`)
and Synexia #12376 (`a6fb7629316c0f610a06bba4ab17ffadb092e36e`). The existing
M3RegexDpPlan state and JNI effect owners implement the new internal token plan.
M3JDK21 is the product; Synexia is the convergence and recipe workspace.

The input is immutable primitive kind/symbol/flag arrays with checked offset
views and identity-scoped symbols. Up to 63 predicates form a sequence with
selected zero-or-more repetitions. Every transition consumes one complete token.
The facade provides full matching, earliest-start/shortest-end finding and bounded
composable/repeated effects. Existing character methods and native ABI are unchanged.
The final find pass keeps earliest state origins, avoiding quadratic token restarts.

## Reproducible receiving order

Use the unchanged recipe crate in this directory's grandparent, with the same
four pinned OpenRewrite engines and Java 21 / Maven dependencies documented there.
Run prepare/SDK tests, sealing and receiver refusal gates before receiving each
of these datasets, in order:

1. `typed-token-20261018`: package-private predicate entries and initial token facade.
2. `typed-token-oracles-20261018`: frozen independent DP and javac lexer fixtures.
3. `typed-token-map-20261018`: one explicit additive package-scope naming-map entry.
4. `typed-token-find-20261018`: per-file search refinement, same facade descriptors.

Each dataset has five actual SDK tests and nine receiver cases. The unchanged
sealing suite passes eight cases. `logs/26-chain.log` records a clean baseline
receive through all four datasets and zero-write terminal replay. The initial
facade dataset is an ancestor of the find dataset, not a final-state replay.

Run `verify_component.py --target RECEIVER --baseline-source BASELINE_DP_JAVA
--output NEW_OUTPUT --jdk JAVA21_HOME` to reproduce the component proof. It runs
strict javac, C17 warnings-as-errors, actual production JNI linked with fatal
UBSan, Java/native/interpreter matching, old/new character observations, API/JNI
identity and resources. `qualification-final/COMMANDS.json` contains exact argv,
exit codes, CPU and per-child peak RSS. JNI registration warnings are expected
from the isolated host proof helper. The helper is outside JDK test discovery.
The unchanged production C blob is pinned in INPUTS.tsv.

## Results and limits

- Frozen independent DP: 176709 Java checks; 176710 checks in each linked-native
  and interpreter run. Boundary coverage includes 0/63 predicates, bit 63,
  immutable/sliced input, full-width identities/flags, effects and callbacks.
- 12001 existing character checks pass identically on baseline and candidate;
  public/protected API descriptors and generated JNI header are byte-identical.
  The initial/final token facade and nested-class public descriptors also match.
- Two jtreg tests pass using the host's patched java.base. javac's actual lexer
  identifies three target identifiers, including a Unicode escape, while excluding
  comments, strings, text blocks and partial identifiers. This is lexical proof;
  resolved compiler bindings are required before semantic rename.
- On the fixed 8000-token failed-search witness (three searches), three separate
  JVMs measured 796–838 ms CPU initially versus 1.4–2.2 ms after refinement.
  Allocated bytes were 152 versus 344 per witness, including measurement overhead.
  The new search uses two bounded int rows, at most 64 entries each. No universal
  throughput claim follows from this probe. Sources, commands, CPU/RSS and raw
  outputs are retained; full-match resource probes are also retained.
- The full-borrow lineage gate passes 32 families; the global phase stays STRING.

A manually scheduled test once ran before asynchronous compilation completed and
reported ClassNotFoundException. `logs/11-java-before-compile-finished.log` retains
that failure; the sequential component runner and subsequent runs pass. No oracle
or existing receipt was weakened or rewritten.

The bounded grammar does not include captures, arbitrary alternation, lookaround,
backreferences, a pattern parser or automatic source rewriting. This does not
certify a source-built modified JDK, the full Synexia reactor, hosted CI or a
non-Linux platform matrix. The three collection baseline failures recorded in
#1357/#12363 remain unresolved. See CONTRACT.md, CATALOGUE.tsv and VERIFICATION.json.
