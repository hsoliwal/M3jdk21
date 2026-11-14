<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Integrated token regular expressions

java.util.regex.TokenPattern and TokenMatcher are additive public java.base
utilities in M3JDK21. They match caller-supplied objects as tokens through the
existing JDK regex graph. Each input list element is exactly one matching unit,
including null. Predicates interpret token spelling, lexical kind, attributes
and identity directly, without serialization.

The canonical source recipe is
synexia-openrewrite-recipes/recipes/m3jdk21-integrated-utilities-20261018/
in the Synexia convergence workspace. The Maven recipe generates these receiver
files. The verify.py program executes qualification and does not transform
source. The category record and compiler-derived behavioral atoms support
subsequent serial, contract-preserving refinement through the same recipe.

## Public contracts

| Owner | Operations | Contract |
| --- | --- | --- |
| TokenPattern | token(Predicate), anyToken(), literal(T), empty() | One accepted element, one arbitrary element, Objects.equals(literal, token), or an empty sequence. Null is a valid token and literal. |
| TokenPattern | sequence(...), then(...), alternation(...), or(...) | Concatenation and ordered alternatives. Operand order is snapshotted. Zero sequence operands mean empty; zero alternatives are rejected. |
| TokenPattern | repeat(min,max[,policy]), optional, zeroOrMore, oneOrMore | Inclusive nonnegative bounds. Greedy, reluctant and possessive policies use existing JDK group algorithms; Integer.MAX_VALUE is the unbounded count. |
| TokenPattern | capture(), capture(name), groupCount(), namedGroups() | Groups numbered in left-to-right preorder, enclosing before enclosed. Names use the JDK ASCII letter/alphanumeric rule and are unique per composed pattern. Reused builder occurrences receive fresh slots. |
| TokenPattern | matcher(List) | Independent mutable matcher over a defensive shallow snapshot; composing a pattern reconstructs fresh graph nodes. |
| TokenMatcher | matches, lookingAt, find, find(start) | Whole-region, prefix and nonoverlapping search. Empty results advance the next search by one token. The find(start) method restores the full input region. |
| TokenMatcher | reset, reset(List), region | Discard visible match state after success. Snapshot creation and invalid range checks complete before prior visible state changes. |
| TokenMatcher | start, end, span, group, with optional number/name | Absolute half-open token coordinates. Group zero is the match; nonparticipating groups have -1 endpoints and null spans/lists; participating empty groups have equal endpoints and empty lists. |
| TokenMatcher | hasMatch, input, pattern, groupCount, namedGroups | Every false result invalidates visible captures. Input/capture lists are unmodifiable snapshot views. Retained captures stay attached to their original snapshot across reset. |
| TokenMatcher.Span | Constructor and record accessors | Validated immutable nonnegative half-open token range. |

For a caller-supplied Lexeme model:

    TokenPattern<Lexeme> identifier = TokenPattern.token(
            token -> token.kind() == Kind.IDENTIFIER);
    TokenPattern<Lexeme> whitespace = TokenPattern.token(
            token -> token.kind() == Kind.SPACE);
    TokenMatcher<Lexeme> matcher = identifier.capture("name")
            .then(whitespace.optional()).matcher(lexemes);
    while (matcher.find()) {
        List<Lexeme> name = matcher.group("name");
        TokenMatcher.Span location = matcher.span("name");
    }

Tokens determine lexical structure. String and comment tokens remain single
elements even when their spelling resembles source code. This API provides no
lexer, textual pattern parser, backreference or lookaround builder, and does not
claim compatibility with the complete Square T-Rex language. Callers can apply
an existing character regex within a selected token attribute when appropriate.

## Owner reuse and behavioral atoms

The exact existing owners are Pattern.java and Matcher.java at M3JDK21 commit
2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49. Their hashes and donor comparisons
appear in contract-catalogue.json. Neither owner changes in this addition.

TokenPattern.TokenNode supplies one consuming predicate transition. Existing
Pattern.Start, Branch, GroupHead, GroupTail, GroupCurly, Loop, LazyLoop, Curly,
Prolog and LastNode own control flow and captures. The repetition compiler
adapts Pattern.group0 so repeated nondeterministic alternatives retain group
backtracking behavior. Oracle attribution and GPL with Classpath exception are
retained for that source.

A private carrier CharSequence supplies one non-surrogate unit per token.
It encodes neither identities nor spelling. A fresh private empty Pattern
contains no text-derived M3 query plan. The direct package-private Matcher
constructor avoids invoking its lazy textual compiler. The public API exposes
neither the private owner nor the character matcher.

The existing character matcher's exhausted-empty search can return false while
retaining its internal first field. TokenMatcher tracks visible result validity
separately so every false result makes captures unavailable while engine search
positions remain intact. The existing character API is unchanged.

Immutable child plans do not retain redundant child engine instances. Each
compilation builds a graph proportional to pattern occurrences; repeatedly
extending a long fluent chain recompiles intermediate graphs. A known batch can
be composed with sequence(...). Input snapshots take linear time and memory.
Matching inherits the JDK backtracking engine's time, stack and memory limits and
can be exponential for ambiguous patterns. No speedup or linear matching bound
is claimed. Predicates must be stable during an operation and must not reenter
the active matcher. Their original exceptions propagate; reset after an
exceptional operation. This family introduces no native object callback path.

## Reproducible qualification

Run from the generated M3JDK21 checkout:

    python3 m3/runtime-integration/token-regex/verify.py \
      --java-home "$M3_TOKEN_JAVA_HOME" \
      --output /tmp/m3-token-regex-contract-results.json

The verifier isolates the two product sources, uses -Xlint:all -Werror for
product/test compilation, checks Javadoc, then executes the jtreg-style test in
mixed and interpreter modes. Its JSON receipt records actual commands, output
and source hashes.

The 2026-10-19 qualification passed **522,729 assertions per JVM mode**,
including **7,586 owner-engine differential cases** and **6,867 independent
nonpossessive language-oracle cases** per mode. Captures, endpoint selection and
possessive behavior are compared with the existing JDK engine. Failed-result
validity is checked against the token API's stated contract.

Coverage includes nested nullable repetition, overlapping predicates, ordered
alternatives, capture restoration, empty/absent groups, regions, input mutation,
nulls, callback exception identity, builder reuse, concurrent independent
matchers, misleading code-like token spelling, delimiter collisions and 65,537
distinct object tokens. Existing character regex features are regression-tested.

This qualification was rerun after a transient executor reset. The two product
sources were recovered byte-for-byte against their prior hashes. The contract
test was newly reconstructed, and its current execution receipt supersedes the
unrecovered earlier raw receipt. Historical counts are not presented as newly
executed evidence. Recovery provenance is retained with canonical source proof.

A complete M3JDK21 image build, full jtreg suite and platform qualification remain
pending. Patch-module evidence does not promote a broader lifecycle gate.

## License and donor custody

Product sources use GPL-2.0-only with Classpath-exception-2.0. TokenPattern retains
upstream Oracle attribution for the group-construction adaptation. The contract
test is GPL-2.0-only. Documentation, catalogue and verification tooling are
Apache-2.0. The canonical recipe preserves each payload's license.

Square T-Rex, NLTK, uCalc and Synexia token owners were evaluated as problem and
contract references. No Apache or proprietary donor expression was pasted into
the GPL product. T-Rex's complete textual language has its own ANTLR grammar and
runtime dependencies; NLTK's unescaped delimiter encoding does not preserve
arbitrary token boundaries; uCalc is a proprietary conceptual reference.
Pinned sources and decisions are retained in contract-catalogue.json.
