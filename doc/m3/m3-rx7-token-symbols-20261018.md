<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# RX7 token-symbol continuation: target-owned candidate

Status: bounded execution candidate, not full rich-token regex or JDK promotion.
Predecessor: M3JDK21 PR #1338, source 3e1a0b7b7d2498a62550861a6a5a9bd88a2314d7.
Canonical recipe predecessor: Synexia PR #12346, source 710588f0ca8ad1c2b0d6d9536f16aa881078b647.

## Distinct semantics

`prepareTokenIndex` composes character-DFA transitions over token spellings. Its
existing masks remain conservative with respect to the character alphabet.
`fromTokenDfa` receives an already-compiled lexical-symbol DFA. Each int column is
one entire symbol, independent of token spelling or rope leaf boundaries. Its
allowed masks use reachability over the supplied symbol edges. Neither operation
is an implementation of a rich-token pattern parser, automatic lexer or renamer.

A producer classifies overlapping predicates into disjoint symbol classes. Its
SHA-256 alphabet root must bind lexer/profile version, dictionary namespace and
generation, classification rules, normalization and comment/whitespace policy.
The bound `advance` overload rejects an unexpected root. Raw integer IDs and
program/alphabet hashes are not proofs of semantic equivalence. The unbound
methods remain available under the existing caller-responsibility contract.

Storage remains the existing canonical String/M3String owner. This patch retains
only primitive automaton rows, accepting flags, allowed masks and two provenance
roots per index. No token payload, per-token/entry object, Java text array,
stringified token stream, wrapper module or Synexia runtime dependency is added.
Source UTF-16 spans and long absolute token positions remain caller-owned.

## Execution and safety

`advance(state, ids, offset, length)` consumes a page and returns its frontier;
empty pages preserve state, including -1. IDs after a dead transition are still
validated. The input page is not retained and must not be mutated during a call.
A caller can process arbitrarily many pages with a long absolute position; this
is not a new long-index java.util.regex.Matcher or a streaming Java lexer.

JNI is optional inside the existing libjava owner. Calls are limited to 4,096
symbols; ID transfer uses 256-int stack pages. Ordinary JNI array-elements access
may copy automaton metadata, so this is not a zero-allocation claim. No critical
array pin, retained JNI pointer or new native library loader is in production.
Missing JNI and allocation failure use exact Java transitions. A failure after a
successful prefix restarts fallback from the original state, not the prefix state.

## Contract-preserving scope and target naming

Existing String/Pattern/Matcher and existing RX7 entry descriptors are preserved.
The reverse-reachability atom is generalized from 128 columns to a supplied width;
the old path still passes 128. The existing spelling-index mask contract is not
silently tightened. Captures, find-order, backreferences, lookaround, Unicode text
matching, lexer continuation and symbol-binding transforms are NOT implemented.

Owner stays `jdk.internal.mindex.M3RX7` / its existing `TokenIndex`; native owner
stays `src/java.base/share/native/libjava/M3RX7.c`. No new family-name mapping or
phase promotion is claimed. This is an explicit pending mapping/admission
receipt under the canonical porting invariant, not a replacement naming map.
Historical Java Apache-header/java.base intake review remains HOLD; native
OpenJDK-compatible notice is preserved. No third-party source is copied.

## Donor/category evaluation

- Square T-Rex: rich token predicates and sequence semantics; Apache-2.0. Design
  reference only. Do not transplant List/Map token storage or its backtracking
  object hierarchy. https://github.com/square/t-rex
- NLTK TokenSearcher: token-boundary query example, not a storage donor. It joins
  tokens into an angle-bracket-marked string, contrary to the no-flattening goal.
  https://www.nltk.org/api/nltk.text.TokenSearcher.html
- Java 21 lexical specification / existing javac scanner: use the real lexer for
  identifiers, strings, comments and Unicode escapes in tests. Lexical matches do
  not prove which declaration an identifier denotes.
  https://docs.oracle.com/javase/specs/jls/se21/html/jls-3.html
- Existing RX7: selected primitive state-major rows, masks and reverse-offset
  reachability. No new regex engine. Complexity: O(states*symbols) construction,
  O(page length) execution; bounded metadata and scratch admission.

## Required qualification

Run the canonical sealed recipe tests, then strict Java 21 compilation, existing
and new component tests, C17/Werror/fatal UBSan and -Xcheck:jni normal/fault lanes.
The native fault harness is test-only and must throw on a later page to prove
prefix retry behavior. Validate raw native bounds, dead-state IDs and malformed
visited destinations. Keep exact preimage/postimage SHA-256 receipts.

Full Maven/OpenRewrite SDK, source-built M3JDK/libjava linkage, actual jtreg, full
String/Pattern and canonical M3String execution, Windows/ARM, licensing intake,
full-family receiving, CPU/heap/native-memory measurements and phase gates must
remain separate from host component evidence. No speedup is asserted.
