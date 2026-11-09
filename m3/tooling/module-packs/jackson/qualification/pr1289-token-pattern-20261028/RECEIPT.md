<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact PR #1289 token matching slice, stacked on the primitive AST core

Parent source: [M3JDK21 #2071](https://github.com/hsoliwal/M3jdk21/pull/2071)
`m3/pr1289-ast-core-receiver-20261028-gpt6` at `876f21f5f40481931e47fdeae7b27207adae5575`.
Original source: [#1289](https://github.com/hsoliwal/M3jdk21/pull/1289)
at `5ec89ae530defe1c7df70e9b3ffd985b9e21f293`.

This review branch depends on the four byte-exact primitive AST core sources in
#2071. It receives two *unchanged* original Java21 production sources into the
same original Jackson namespace and retains the API contracts of the current
M3JDK21 runtime. No new runtime library, parser vendor or Synexia recipe owner
is introduced.

| Original implementation | Exact Git blob |
| --- | --- |
| `src/main/java/jdk/m3/jackson/M3ASTTokenPattern.java` | `67b3222bb46fa34af4467ab5c5afee49596a940e` |
| `src/main/java/jdk/m3/jackson/M3TokenRegex.java` | `25cfbcd9b2203a77622c24b20a59c38b45aecefa` |

These source files require only the existing Java 21 library and the
immutable M3 AST core. They operate on already-retained primitive AST facts and
do not change `M3File`, the current Jackson `ObjectMapper`, any JNI exports,
module names, the JMOD builder, or original source-retention policies.

The standalone `M3ASTPatternCoreReceiptContract` exercises repeated direct
child occurrences, field predicates, ordinal spans, non-leaking container
boundaries, invalid predicates, full event matching, token span results,
budget/depth refusals and index-only traversal.

`.github/workflows/m3-json-pr1289-token-pattern-java21.yml` checks both
original Git blob identities, then compiles all seven self-contained core and
token matcher classes, both isolated contract mains, with
`javac --release 21 -proc:none -Xlint:all -Werror` and executes both.

**Status:** local test sources passed Java 21 parsing. Actual original-class
compilation, behavioral execution and all product/JDK/JMOD/JNI acceptance are
pending hosted GitHub Actions; they are not claimed passing.

**HOLD:** Neither this stacked review nor parent #2071 authorizes a merge of
historical #1289. Its six current-master owners remain divergent. Perform
canonical Synexia source-sealed recipe replay, exact API/ABI source preservation,
full Jackson/JMOD/JDK compilation and differential contracts before master
promotion. No force push, squash, rebase, source deletion or ancestry-only merge.
