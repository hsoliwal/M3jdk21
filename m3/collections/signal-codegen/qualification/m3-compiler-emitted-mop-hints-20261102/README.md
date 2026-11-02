<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 compiler-emitted machine-oriented pattern hints

This is an **opt-in javac annotation-processing producer**, not a second detector,
runtime profiler, GoF proof or automatic refactor. It extends the existing
\`SortableProcessor\` source-time pass, consuming its already-manufactured 22
\`M3CompilerFacts\` primitive count/validity columns. The unchanged
\`M3MachineHintRules\` returns seven tri-state review candidates. Known-negative,
unknown and candidate remain different states.

## Compiler hint contract

The original javac compiler can emit informational \`NOTE\` diagnostics directly
against each compiled \`@Sortable\` type, without any new source scan or build-phase
dependency. Default \`off\` preserves all existing compiler output and generated
\`FooM3Sortable\` source bytes.

\`-Am3.machineHints=notes\`: human-readable candidate NOTES (not warnings; safe
under \`-Werror\`).

\`-Am3.machineHints=receipts\`: write one compact deterministic TSV per compiled
annotated top-level type to the existing \`CLASS_OUTPUT\` via \`Filer\`. Location:
\`META-INF/m3/pattern-hints/<qualified/type>.tsv\`.

\`-Am3.machineHints=both\`: both surfaces. Invalid modes produce a compiler ERROR
instead of silently degrading or changing defaults.

The versioned \`M3_MOP_HINT_RECEIPT_V1\` includes qualified type identity
(base64url UTF-8), fact-known bit mask, tri-state hint masks, 22 fact rows,
seven hint rows and SHA-256 checksum of the UTF-8 body. \`UNKNOWN\` is never
fabricated as zero or "not observed"; unknown source/runtime facts have no
source-generation authority. The checksum is a receipt content identity,
**not** an exact source identity: M3File/M3AST must independently attach the
compiler environment and verified source-generation seal.

Compilation failure may leave a generated resource; Maven must consume receipts
**only after a successful compiler lifecycle** and independent manifest/source
identity checks. No receipt implies that its candidate is a true GoF instance.

## Maven 21 opt-in

Keep the existing canonical \`maven-compiler-plugin\` owner and configured
\`SortableProcessor\` annotation processor. Add the option to that compiler's
existing \`compilerArgs\`:

~~~xml
<compilerArgs>
  <arg>-Am3.machineHints=both</arg>
</compilerArgs>
~~~

Maven can then consume the module's actual compiled output under
\`target/classes/META-INF/m3/pattern-hints/\`. Aggregate receipts by qualified
type and compiler/M3File generation, not by sorting filenames alone. The
Synexia GoF \`M3RelationshipPatternizerRecipe\` remains the structural
multi-class matching owner; \`M3AST\` owns source/semantic attribution and
cross-module graph joins. This slice does not make Maven auto-apply a recipe;
it provides compiler-origin candidate evidence for its existing gates.

## Exact recipe and source custody

Canonical source owner: \`hsoliwal/com.synexia\`,
\`synexia-openrewrite-recipes/recipes/m3-compiler-emitted-mop-hints-20261102\`.
Use its named Maven/OpenRewrite recipe
\`com.synexia.m3jdk.CompilerEmittedMopHints20261102\` to receive exact pinned
Java source/test postimages and additive runner script. Original
\`SortableProcessor.java\` preimage is sealed and retained.

The new emitter and exact modified processor preserve original public API,
generated companion source, M3JDK21 Java21 compatibility and zero additional
runtime dependencies. Source passes are serial and replay to a zero-change
fixed point; no rebase or force push of \`develop\`/\`master\`.

## Verification and scope limits

\`bash m3/collections/signal-codegen/verify.sh\` runs strict Java21
\`-proc:none -Xlint:all -Werror\` compilation of producer and tests, existing
M3 compiler-fact/rule oracles, and the new genuine JavaCompiler annotation
processing test. The new test exercises off/notes/receipts/both and invalid
options, compiler NOTE vs WARNING, unaltered generated companions, identical
per-type receipts, missing facts, and SHA-256 checksum.

CI of the original full OpenJDK compiler, all Maven modules, actual JNI
and differential behavior, JIT/jtreg and GoF semantic equivalence must
still pass before promotion. This first emission applies to \`@Sortable\`
types; extending unannotated Java sources belongs in the original
\`M3ASTPC\` ANALYZE consumer, not in a second AST parser. The next scope
also requires original graph facts for factory/observer/strategy behavior
before any hint can be upgraded from "review candidate".
