# Grammar core receiving adaptations

This document precedes template materialization. The immutable donor is
`hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267` and the exact
24-file intake manifest is `parser-donor-input/SOURCE_MANIFEST.tsv`. The source
closure has been read in full. No donor file is changed by this receiving work.

## Semantic mapping and source ownership

Each exact `MIndex` class basename in that manifest maps to the corresponding
`M3` basename, in `jdk.m3.parser.grammar`. Grammar symbol IDs, production IDs,
terminal classes, FIRST/FOLLOW analysis, Earley packed state coordinates and
primitive parse-tree lanes retain their donor meanings. The detailed per-file
mapping and donor/template hashes will be recorded in `GRAMMAR_DONOR_MAP.tsv`.
Every mapped source keeps Apache-2.0 attribution and identifies its exact donor.

`M3TokenStream` owns one immutable `String` by identity and immutable primitive
source-range lanes. Lexing snapshots a mutable CharSequence once; callers must
not concurrently mutate the input during this snapshot. All token offsets are
UTF-16 half-open offsets. Text materialization uses String.substring; ordinary
matching uses the retained source and exact offset comparisons. Resolver-native
index/indexFrozen methods, the token ID lane, indexed/tokenId/tokenIds accessors
and the now-unused TokenSlice helper are omitted. They belong to optional legacy
Synexia runtime indexing, not this closed parser module.

## Per-call bounded parsing

`M3GrammarLimits` has components `(int maxSourceChars, int maxTokens,
int maxStates, int maxTreeNodes, int maxTreeDepth, long maxWork)`. Defaults are
1,048,576 source UTF-16 units; 10,000 tokens; 250,000 aggregate chart states;
100,000 tree nodes; depth 256; and 10,000,000 deterministic work units.
Source and token maxima may be zero. Source/token maxima may not exceed the
24-bit token-coordinate cap (16,777,215). States and tree nodes must be positive
and at most 67,108,864. Depth must be positive and at most 256 because the retained
tree assembly is recursive. Work must be positive. These are refusal bounds,
not preallocation requests or wall-clock deadlines.

Each lex/parse/validate call creates one private budget. When parsing text,
lexing and grammar processing share it. Predicted, scanned and completed states
consume the state bound only on first insertion; attempted transitions and
scans consume work, including duplicate states and failure fallbacks. Lexical
scans, exact terminal-match candidates and nullable replay are charged. Tree
nodes and depth are refused before reservation or recursive descent. Depth counts
all nodes, including terminals, with the root at depth one. Capacity
growth uses bounded arithmetic. `M3GrammarException` names the exceeded limit.
Grammar construction, its fixed-point analyses, immutable lexer-profile setup
and one-time decision-index construction are caller configuration operations
outside these per-input budgets; they are not claimed bounded by maxWork.

Existing no-limits constructors use defaults. New constructors accept explicit
limits for `M3GrammarLexer`, `M3GrammarParser` and `M3GrammarRecognizer`.
The received default policy is therefore intentionally bounded; no pre-existing
JDK or donor API is modified.

## Demonstrated nullable completion repair

The donor rejects the grammar `S -> B A; B -> A; A -> epsilon` on empty input.
At chart zero, A completes before completion of B adds `S -> B . A`. Predicting
A again deduplicates its already-completed state and never advances the late
waiter. The same defect occurs for `S -> B A "x"` on input `x`.

To preserve every prior successful retained derivation, the receiving parser
first runs the original optimized path. Failure runs the original unfiltered
path. Only a second failure enters an unfiltered repair pass. During that pass,
a nullable waiter consults already-processed same-position completions in their
original insertion order. All three passes share the same budget. If the repair
also fails, the original unfiltered diagnostics remain authoritative. Existing
successful ambiguous trees retain the original first backpointer decision.

## Decision-index identity repair

The donor process-global name cache treats a noncryptographic fingerprint as
grammar equivalence and retains grammar objects permanently. Its fingerprint
also omits the chosen start symbol; Java String hash collisions are sufficient
to produce different literal grammars with the same fingerprint. The received
grammar owns its own lazily initialized immutable decision index. Distinct
grammar objects do not share indexes on the basis of names or fingerprints.
M3GrammarDecisionIndexes.forGrammar remains a convenience forwarder; the global
cachedIndexCount accessor is omitted with the global cache. Dispatch admission
requires exact grammar object identity. Fingerprints remain informational.

## Qualification boundary

These are authored candidate templates. Root must run the prescribed order:
patch, lint, compile, tests, runtime, artifact/receipt verification. This worker
does not compile or execute parser code. Required counterexamples include late
nullable completion, existing successful ambiguity choices, literal hash
collisions across same-name grammar objects, exhausted budgets including
fallback/replay, source snapshots, exact UTF-16 spans and per-call isolation.
