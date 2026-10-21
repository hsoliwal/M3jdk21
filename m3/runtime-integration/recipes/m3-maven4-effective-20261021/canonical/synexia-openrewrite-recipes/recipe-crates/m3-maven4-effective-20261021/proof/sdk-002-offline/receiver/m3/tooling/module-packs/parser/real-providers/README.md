<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST: original-provider integration, not replacement parsers

Canonical source of truth is Synexia. Receiver is M3JDK21's **existing**
\`jdk.m3.parser\` module, stacked additively on merged #1496. The source
implementation is \`M3ASTAuthoritativeModels.java\` and does not introduce a
parallel source, token, AST, Maven ModelBuilder, Gradle daemon or JDT parser.

## Admission routes

| Route | Input owner | What is indexed | Original authority preserved |
| --- | --- | --- | --- |
| eclipseJdt | Original JDT ASTNode + exact captured characters | Original JDT DOM, ranges, recovered nodes | Eclipse working copy/parser/JDT bindings |
| mavenEffective | Maven 3.x ModelBuildingResult | Effective dependencies, management, plugins, executions, modules, profiles and repositories | ModelBuilder, inheritance, profiles and interpolation |
| maven4Effective | Maven 4 API ModelBuilderResult with a completed effective model | The same detached occurrence kinds, including subprojects with legacy modules fallback | The original Maven 4 builder, resolver/session, inheritance and profile activation |
| gradleTooling | Original Gradle Tooling GradleProject | Read-only project hierarchy and tasks | Gradle daemon and tooling model |
| gradleGroovyAst | Original Groovy ModuleNode | Selected downward Groovy grammar edges, including method calls | Groovy AST/Gradle DSL |
| gradleKotlinPsi | Original KtFile | KtFile PSI child topology and exact original UTF-16 TextRange | Kotlin compiler/Gradle Kotlin DSL |

The caller must create and own the original external object; these methods
**never construct another parser, task, daemon, or ModelBuilder**. They visit
bounded real provider objects synchronously and publish only a detached
\`M3ASTIndex\` backed by primitive kind/parent/sibling/range/flags lanes and
the original immutable \`M3File\` source.

## Exact source and semantic boundaries

All public APIs inherited from merged #1496 remain intact. The new routes
are additive. Original parsed source must equal the immutable M3File snapshot
for JDT and Kotlin PSI. Effective Maven/Gradle and Groovy AST data may combine
multiple sources; their UTF-16 coordinates are **unknown** rather than guessed.
The Kotlin PSI's own TextRange is used for exact coordinate spans after
checking that the original KtFile text equals M3File. Maven 4's distinct
ModelBuilderResult is not silently treated as Maven 3. The explicit maven4Effective
route reads its effective model once and refuses a validation-only result with
no effective model. It preserves the original M3File owner, admits no guessed
source positions, and applies source, node and projected-depth budgets. The
qualified fork and its separate source-build gate are documented in
[maven4/README.md](maven4/README.md). Other provider versions are not implied.

No model strings, compiler handles, PSI, JDT DOM, Task, Project or Map.Entry
objects are retained inside the index. Reflection is used only at the
**optional** public API boundary so the parser JMOD does not drag optional
Eclipse, Maven, Kotlin, Groovy or Gradle runtime dependencies into startup.

The parser on a source file remains the syntax authority; the effective Maven
model and Gradle Tooling model are separate semantic outputs with no implied
one-to-one source occurrence identity. Source rewriting must still be
authorized by the original JDT/LTK/Groovy/Kotlin/Maven tooling, never the
detached index alone.

## Reproduction and acceptance

1. Verify the canonical exact-Git-blob source manifest.
2. Run source-sealed Maven recipe (verify/apply/fixed point).
3. Strict Java 21 compile and standalone 17-check public-shape oracle.
4. Run actual Eclipse JDT ASTParser, Maven 3.9.11 ModelBuilder with activated
   profile, and Groovy 4.0.26 SourceUnit parser. Run original Kotlin PSI via
   compiler-embeddable 2.0.21, and original Gradle Tooling API 8.12 against a
   real project daemon.
5. Retain all no-drift refusal receipts, run original M3 parser contracts and
   linked JMOD image tests, then cross-platform/HotSpot qualification.

These test versions are **sample donor pins**, not claims that all future
version combinations work. The standalone oracle uses API-shaped doubles
to catch bookkeeping errors; it is not real-library proof. CI jobs using the
actual libraries are distinct, and their result must be consulted before
promotion.

## Deferred, not claimed

- Eclipse PDE/OSGi bundle classloader, live workspace reconcile and attributed
  binding coverage across working-copy changes.
- Full Maven 4 distribution/plugin execution, complete parent/BOM/plugin
  matrices, and the customized Maven Resolver, m2e, Tycho and mvnd forks. The
  bounded Maven 4 effective-model route has its own real-fork provider gate;
  that gate does not promote these separate runtime/build owners.
- End-to-end Gradle Groovy/Kotlin daemon worker and incremental/build-cache
  orchestration, beyond the real Tooling project smoke.
- Jackson full M3AST pool-based zero-wrapper indexing and low-allocation
  string/source-span authority. A separate adapter now consumes the **real**
  Jackson JsonParser into primitive M3ASTIndex occurrences; an exact 100 MB and
  100 MiB index benchmark is wired into CI, but remains unverified until run.
  Earlier Jackson AST 100 MB/100 MiB round-trip CI passed independently.
- Full rebuilt M3JDK21/HotSpot/jtreg/JCK and cross-platform JNI.
