<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 parser framework JMOD

## Requested contract

Provide the optional `jdk.m3.parser` Java 21 module under the existing module-pack
owner. Reuse the Synexia grammar implementation and the existing M3JDK String and
compiler owners. The user request of 2026-10-18 authorizes this additive parser
module. Existing Java SE, String, javac, and MIndex interfaces remain intact.
This independent Apache module does not advance the ordered runtime receiving
phases or assert whole-family migration.

## Ownership and interfaces

* `jdk.m3.parser.M3File` is an immutable source snapshot. It retains one `String`,
  its source URI, and an exact UTF-16 big-endian SHA-256 seal. File-byte identity,
  when available, is recorded separately. Source offsets are UTF-16 code-unit
  offsets, ranges are half-open, and all ranges must be bounds checked. File
  input is bounded and strictly decoded. A changed file creates a new snapshot;
  retained snapshots and parse results remain valid.
* The parser accepts ordinary `String`. On M3JDK this is backed by the existing
  package-private `java.lang.M3String`; the module adds no second M3String owner,
  reflective access, `java.base` export, or global text pool. Token and node
  spellings are ranges over the same immutable source.
* `jdk.m3.parser.grammar` receives the minimal closed lexer, grammar builder,
  Earley recognition/parsing, analysis, and packed-tree implementation from
  `com.synexia.indexstring.grammar`. Capability-to-owner mappings and exact donor
  hashes are recorded before materialization. Optional donor runtime indexing
  hooks are excluded explicitly, rather than pulling a Synexia runtime into JDK.
* `M3Parser` joins a source snapshot with a grammar and explicit lexer/parser
  budgets. Its detached result owns the source and the grammar parse result.
  Reusing a framework instance does not share mutable parse-session state.
* `M3JavacParser` attaches to a caller-owned `JavacTask` through the existing
  `com.sun.source.util.M3AST` and its identity-memoized projections. It records
  detached primitive node kinds/parents/source ranges while the Unit is live.
  It never retains a Unit, Tree, Trees, task, Element, or TypeMirror in a result.
  The existing javac parser runs once; the generic grammar engine is not used to
  substitute for Java parsing or attribution. Compiler positions that javac
  marks unavailable remain explicitly unavailable.

The generic grammar parse tree and the javac projection are different contracts.
No claim is made that a generic parse tree is an attributed Java AST. Generic
grammar import formats, full language packs, AST-spec converters, speculative
accelerators, and JNI extensions are separate donor capabilities outside this
minimal parser-runtime closure.

## Bounded input sessions and reviewed adaptations

The default per-input limits are 1,048,576 source UTF-16 units, 10,000 tokens,
250,000 aggregate Earley states, 100,000 tree nodes, tree depth 256, and
10,000,000 deterministic work units. Root depth is one and terminal children
count toward depth. `M3GrammarLimits` allows explicit smaller or larger admitted
limits; the recursive tree builder never admits a depth greater than 256.
Snapshotting, lexing, token dispatch, all recognition/parsing passes and tree
construction share one budget for a text invocation. Resource refusal throws
`M3GrammarException`. Grammar construction, fixed-point analysis and the first
immutable decision-index construction are configuration operations outside this
per-input budget. This is not a bound on total process memory or grammar setup.

The donor's successful optimized and unfiltered parses retain their selection
order. Only if both fail, a final unfiltered pass replays earlier same-position
nullable completions; this repairs late waiters such as `S -> B A; B -> A;
A -> epsilon`. All passes consume the same budget. Every frozen grammar owns
its own lazy decision index, and dispatch checks grammar object identity.
Noncryptographic fingerprints remain metadata and cannot establish equivalence.
These source adaptations are recorded in the canonical recipe's grammar map
and receiving diff, with exact donor and postimage hashes.

The received configurable lexer preserves the donor's broad numeric and quoting
rules. In particular, `NUMBER` alone is not a strict JSON-number or Java-number
validator. Java syntax continues through javac and `M3AST`.

## Canonical recipe and source custody

Canonical reusable recipe authority stays in `hsoliwal/com.synexia`, using the
existing `M3HashPinnedJavaSnapshotRecipe` and its standalone Maven recipe test
pattern. The source closure is revision pinned; every mapped source carries its
original Apache rights, attribution, and adaptation notice. Target module source
is an exact admitted recipe postimage. New-path admission refuses an occupied
different file; a repeated application has no diff. Donor and target source drift
is a refusal, not an invitation to overwrite.

`jdk.compiler` remains GPLv2 with the Classpath exception. Its existing sources
are compiled only as the source patch used for compatibility tests; they are not
copied into the Apache parser module. On a matching built M3JDK the compiler API
is provided by that JDK. A stock-JDK source-patch test is reported separately
from a full custom JDK/HotSpot build.

The source-snapshot seal contract is traced specifically to the pinned
`synexia-indexstring/.../ast/MIndexASTParser.java`. There is no assertion that a
class named `MIndexSourceFileSnapshot` was found. `M3File.read` fills the bounded
file-admission gap using standard JDK file and charset APIs. It snapshots the
bytes observed by one read, without asserting an atomic filesystem revision.

## Verification order and deliverables

1. Inventory, document, record preimages, and materialize reviewed recipe files.
2. Lint source/doc/manifest structure and run the unchanged policy gates.
3. Compile all production, compiler-patch, fixture, and recipe-test sources.
4. Run grammar/source/M3AST contracts and recipe fixed-point/refusal tests.
5. Create modular JAR and JMOD with the existing `jmod_pack.py` owner; inspect
   the descriptor and legal entries; link and run precompiled consumers.
6. Compare independent deterministic replay outputs, record all logs, source and
   artifact hashes, and retain explicit pending gates. No performance gain or
   full M3JDK build is inferred from finite correctness tests.

All verification commands are recorded with status and output. A failure stops
dependent stages. A source change begins a new lint/compile/test/runtime pass.
Compilation of test consumers is completed before test execution. Final review
includes the target PR and canonical Synexia recipe PR; neither is auto-merged.

## One javac geometry, one detailed index (consolidation 2026-10-23)

Three detached surfaces describe a javac unit today:

| Surface | Lanes | Owner |
|---------|-------|-------|
| `M3JavacTree` | kind, parent, start, end | `M3JavacParser.projection` (geometry fast path) |
| `M3ASTIndex` (`Domain.JAVAC`) | the four geometry lanes plus details: lines, names, literals, modifiers, enclosing type and executable | `M3ASTIngress.javac`, `M3JavacParser.indexProjection` |
| `M3JavacFactImage` | the four geometry lanes, sealed to source and build context | `M3JavacFactImage.capture` |

The geometry lanes are a strict subset of the index. `M3JavacTree.of(M3ASTIndex)` is the
geometry surface as a view over the one index: no second projection, no copied lanes, only a
kind-translation lane per kind-vocabulary entry. `M3JavacFactImage.capture(M3ASTIndex, ...)`
captures from that view and yields the bytes of the geometry projection of the same unit;
`M3JavacTreeViewContract` proves row equality and image equality on a real parse event.

The geometry projection stays for callers that never need details; both projections share
the original event cache. Analyzers (lint, clustering, facts) read the index lanes; none of
them builds another tree. The compiletime port's `com.m3.compiletime.ast.M3AST` wrapper over
`MIndexASTDocument` is a separate owner with its own atom pool. These files may be similar.
Please confirm via hash or diff before any merge of that owner into this module.

## Culprit lanes (2026-10-23)

`M3Culprits.scan(M3ASTIndex, Limits)` evaluates the M3 hard limits as lane queries over one
JAVAC-domain index: loops, methods and top-level classes over their line limits, `return null`,
`System.out`/`System.err`, magic numbers and floating strings, with the same constant and
message exclusions the Synexia multipass marks and the recipe compiler's lint apply. No javac
tree, second parser or OpenRewrite LST is touched; the result is two primitive lanes (occurrence
row, culprit ordinal). A comment mark written by the OpenRewrite recipe and a finding produced
here name the same site, so the analyzer no longer needs its own tree.

## Facts contract and driver provisioning (2026-10-23)

`M3FactsWriterContract` is a registered consumer contract (`M3_FACTS_WRITER_PASS`): one real javac parse,
one JAVAC-domain `M3ASTIndex` through `M3ASTIngress.javac`, one `M3FactsWriter.write` into the consumer work
directory, then JDBC readback of `target/m3/facts.sqlite`. It asserts that the `m3_ast_meta` marker equals
`M3FactsWriter.SCHEMA`, that the per-generation file and node rows and the generation totals equal the
returned `Result`, that `committed` is true, that every node lane round-trips exactly (one ordered lane
digest compared against the index), that a repeated generation is refused whole by the generation primary
key with the first generation intact while a new generation is appended, that a missing driver surfaces the
writer's declared `SQLException`, and that a facts path which cannot be a regular file is refused by the
writer's own `IOException`.

Driver rule: the module keeps `requires static transitive java.sql` and no driver dependency. `build.py`
resolves the newest local Xerial `sqlite-jdbc` 3.45.x jar from the local Maven repository
(`--maven-repository`, default `~/.m2/repository`) together with the one `slf4j-api` version its sibling
POM declares (`org.sqlite.JDBC` cannot initialize without it), seals both paths and SHA-256 values in
`INPUTS.json` (owner `maven-repository`, with the POM) and `lint-structure.log`, and places them only on
the consumer test classpath, never on the module path, the JAR, the JMOD or the linked image. An absent
jar refuses the sequence with one line naming the expected path; nothing is downloaded.
