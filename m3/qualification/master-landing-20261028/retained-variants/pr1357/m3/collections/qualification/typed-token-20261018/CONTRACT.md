<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Typed token predicate contract

This pass extends target 872e70d20f3f3223a72eb5f67b001528b7fb2cb6 and canonical
a6fb7629316c0f610a06bba4ab17ffadb092e36e. Packed-token RX7 remains unchanged.
M3JDK21 is the Java/JNI product; Synexia owns the reusable source recipe.

The selected behavioral atom is already present: M3RegexDpPlan.advanceMatched
consumes a word describing matching predicates, and its Effect composes/repeats
primitive transition rows through existing Java/JNI owners. Add package-private
typed-token construction/advance/summary entries while preserving all existing
public/protected descriptors, character semantics, callback schedules and native
symbols. This is an explicitly admitted package-scope additive feature, not a
claim that adding typed predicates is a FILE-local optimization.

Add internal M3TokenRegexPlan, using that existing state engine. A compiled
predicate optionally tests exact token kind and domain-local symbol identity,
and always tests required/forbidden long flag masks. Predicates form a sequence;
selected predicates repeat zero or more times. Each transition consumes exactly
one token. The natural one-word bound is 63 predicates plus the initial state;
larger programs refuse explicitly. This bounded grammar does not claim T-Rex
captures, arbitrary alternation, lookaround or backreferences.

Inputs are immutable primitive kind/symbol/flag columns with one domain identity.
Construction clones caller arrays; slicing shares those owned arrays with checked
offset/length coordinates. Patterns and inputs require the same domain object by
identity. Equal numeric symbols from different domains are not interchangeable.
There is no retained text, token object per slot, regex parsing or callback to
Java from JNI. A symbol column is semantic only if its producer supplies resolved
symbol identity; a lexical spelling ID cannot authorize a scoped rename.

Full matching consumes the whole input view. Find returns the earliest token
start, then shortest accepted end, relative to that view; empty matches are legal.
This follows the existing IndexRegex selection contract, not T-Rex greediness.
Effects retain only bounded primitive state rows and their compiled plan. Their
length counts tokens; then/repeated obey existing plan identity, overflow and
checkpoint rules, with the same native fallback and row-composition owners.

Reuse evidence: Synexia IndexRegex (earliest/shortest selection), MIndexTokenPattern
(token-domain masks), and MIndexASTQuery (kind/identity/required/forbidden flags),
all pinned in the preceding catalogue at 140c263a90f9803b9c46103c63355a286e0515f0.
No T-Rex or challenge-platform source is copied. Preserve OpenJDK GPLv2 Classpath
headers in M3RegexDpPlan and its unchanged native source; new first-party facade,
tests and recipe evidence carry Apache-2.0 and Synexia attribution.

Prove with an independent scalar DP oracle over predicate arrays, exhaustive small
alphabets, deterministic randomized inputs, 0/1/63 predicate boundaries, bit 63,
domain/range/immutability checks, and effect composition/repeat checks. A javac
ScannerFactory fixture must distinguish identifiers from comments/string/text
block content and account for Java Unicode escapes. It proves lexical matching,
not resolved-symbol rewriting. Freeze and rerun existing character-plan tests and
API/JNI headers; use actual linked production JNI with call counters, -Xcheck:jni,
fatal UBSan and interpreter parity. Keep the host JNI helper outside JDK source
discovery. Qualify the actual unchanged OpenRewrite owners before receiving.

The full modified-JDK image, full reactor/platform regression matrix and semantic
rewrite integration remain separate pending gates. No global receiving phase
advances; no previously published receipt is rewritten.

## Serial find refinement

After qualifying the initial implementation and freezing its DP oracles, replace
only the private search strategy in the same file. Restarting from every token
can take quadratic time on a long failed search. Retain the earliest origin at
each state instead: transitions depend only on state and the next token mask,
so an earlier origin dominates later origins at that same state. Process ends in
ascending order and update the selected match only for a strictly earlier start.
This preserves earliest-start/shortest-end selection, including empty matches.
Reuse M3RegexDpPlan.advanceTokenMask for every transition; do not duplicate its
epsilon-closure logic. Cost is O(tokens * predicates squared) worst case, with
two bounded int rows (at most 64 entries each). Compare against the frozen scalar
DP oracle, the initial facade descriptors and a bounded long-miss resource probe.
The additive grammar, predicate semantics and JNI owners remain unchanged.
