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

## Source generation, compilation scope, and unchanged class files

M3JDK21's existing compiler owns parsing, symbols, diagnostics, and generation.
`M3AST.open(task, retainSyntax, scope)` adds an explicit source admission to that
same task. `Scope.file`, `Scope.packageSources`, `Scope.moduleSources`, and
`Scope.projectSources` take the exact source URIs selected by the caller. Package
and module scopes also check the original compiler's declaration/membership facts.
Bind the scope before starting the task or registering framework consumers.
The default two-argument and one-argument entrypoints retain their behavior.

Existing Maven/Gradle compiler-argument configuration can select the same source
boundary with `-Xplugin:M3AST scope=file`, `scope=package:my.package`,
`scope=module:my.module`, or `scope=project`. The original plugin derives admitted
URIs from javac's already selected input file objects without reading characters.
The build tool still owns which files are selected and which binary paths/options
are supplied. File scope requires exactly one selected input. Multiple scope
options and unknown scope values are refused. Scope options may accompany the
existing `lossless` plugin argument. No new build-tool parser or compiler task is
created. The finite source-patch test invokes the actual plugin's `init` method;
service discovery under the final M3JDK21 module descriptor remains a product-JDK
gate, not a capability inferred from the supplied stock JDK's descriptor.

The caller supplies unchanged dependencies on the original compiler's class path,
module path, or already entered context. For example, compile a changed `A.java`
against existing `B.class` and `C.class`; those dependencies' source files do not
need to be opened, parsed, or compiled. A new compiler context may still read the
class files to resolve their symbols. This API does not introduce a global symbol
cache or promise zero binary I/O across independent compiler contexts.

```java
// task is the original JavacTask with its existing options and binary paths.
M3AST.Scope scope = M3AST.Scope.file(selectedSource.toUri());
try (M3AST ast = M3AST.open(task, true, scope)) {
    ast.addConsumer(consumer);
    boolean compiled = task.call();
}
```

All three original source-opening routes enforce admission before character I/O:
explicit source reads, implicit class completion, and silent module/package-name
probes. A source outside the selected set is a caller configuration refusal through
javac's existing client-exception path, with failed session cleanup. It is not an
invented symbol or a successful partial compilation. Include any intentionally
generated source and required source descriptor in the admission; otherwise supply
the corresponding binary. This does not alter file-manager lookup preference:
if `-Xprefer:source` chooses an excluded source, admission refuses that read.

`-implicit:none` alone only suppresses implicit class generation. The scope test
includes a control that still parses an implicit dependency with that option and
an admitted-task case that refuses the same dependency before its source read.
See the [Java 21 javac specification](https://docs.oracle.com/en/java/javase/21/docs/specs/man/javac.html).

One authoritative syntax generation can serve multiple projections. The scope
contract checks that parse/analyze/generate on the same original task share the
actual compilation unit and that consumers share an event-local projection.
It checks two successive source generations against unchanged dependency bytes,
single-file analysis without generation, package boundaries, module dependencies,
project package relationships, missing dependencies, lifecycle, and source refusal.
Run it through the existing `build.py` lint/compile/test/runtime chain; the linked
image run uses the same compiled compiler owners and precompiled test consumer.

Unchanged syntax is not proof that attributed facts remain valid. Compiler build,
language options, class/module paths and their content, processors, generated
sources, and dependency ABI can invalidate semantic facts. Existing Synexia
`M3CompilationContext.sealed`, `M3JavacASTIndex`, `MIndexJavaAST.capture`, and
`M3ASTWorkspaceCache` already own source/context-bound capture and bounded reuse.
Their admitted snapshots are reusable through those owners; live javac trees and
symbols must not be transplanted into a different compiler context.

## Original framework ownership and qualification boundary

The source audit retains existing work in the Maven, Gradle, NetBeans, and
OpenRewrite forks. Maven's compiler configuration selects M3AST on the existing
compile/testCompile invocation; Gradle attaches to its original JavacTask;
NetBeans retains its existing task, parsed trees, class path, and selected phase
work. Synexia's `M3OpenRewriteSharedAST` uses the existing Java 21 visitor on an
already attributed javac unit. No new framework parser or parallel compiler is
introduced by source admission.

JDT rewrite plans and physical DOM mutation are distinct inputs. A direct
projection may consume the existing plan's revised children and original
move/copy nodes. Such a projection must retain original parse binding semantics,
reject physical-tree drift, require exclusive access to the mutable plan, and
leave transformed offsets unknown unless an actual position owner proves them.
Text formatting and a new semantic attribution are separate operations.

The audit does not certify every frontend as parse-once. OpenRewrite's Maven
parser still constructs RawPom and XML trees in separate parses. The existing
JSON AST draft's legacy serializer-based mapper overloads still honor custom
serializers; direct tree binding is qualified on that draft's own follow-up.
JDT text rewrite formatting may call its parser internally. These remain explicit
owner-specific review items, not capabilities inferred from the compiler scope
tests. This finite qualification does not advance whole-family receiving status
or replace a full M3JDK21/HotSpot platform build.

The source-built qualification seals the original compiler descriptor and checks
its `uses com.sun.source.util.M3AST.Consumer` declaration. The stock-JDK test
process grants that same service use before invoking the actual plugin's `init`;
the grant is test-only and does not patch a production descriptor. Original
service discovery through the rebuilt M3JDK21 descriptor remains a separate
full-product build gate.
