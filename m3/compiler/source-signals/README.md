<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 — source-time compiler signal facts (candidate)

Copyright 2026 Hitesh Soliwal and contributors.

## Ownership and phase

This is **M3JDK21 product-target** source-time compiler tooling in an independent `m3/**`
proving module, not a Synexia runtime layer or a replacement public JDK API. `jdk.compiler`
installation remains separately gated by the required String -> arrays -> collections -> AST/compiler
receiving order. The canonical Maven recipe, postimages, provenance and fixed-point/refusal tests
belong in Synexia's `synexia-openrewrite-recipes` and are furnished here as a proposed intake crate.

This plugin **does not replace** the existing M3 `@Sortable` producer in PR #1312: that JSR-269
processor generates typed companion source and a caller-owned `double[]` absolute-weight projection.
This `com.sun.source.util.Plugin` observes javac's **attributed ANALYZE** events and writes a
separate, deterministic, compiler-owned facts image. It makes no AST mutation and introduces no
new JNI sort ABI, collection backing store, global all-pairs cache, or runtime reflection.

## Signals captured

- Package, qualified class, kind and modifiers.
- Declared fields, methods, constructors and resolved signatures.
- Supertype/interface and generic type dependencies, annotations.
- Compiler-attributed source call edges: `DIRECT` for static/private/constructor calls,
  `DISPATCH_CANDIDATE` for nonfinal instance calls; reflection and invokedynamic are not closed.
- Source-expression counts: fields, methods, constructors, direct dependencies, invocations,
  allocation sites, member references, direct/candidate/unresolved calls, loops, branches,
  try/catch and lambdas.
- Per-family unsigned sorted 64-bit FNV-1a token sets as **heuristic features**, not object
  equality, exact source identity, ordinal name ordering, or a metric certificate.

Metadata records are encoded as a **textual versioned intermediate artifact** for review:
`S` scalar rows, `T` primitive token rows and `E` source-attributed call-edge rows. The file is
sorted deterministically by qualified type, feature family, unsigned token, and edge. A SHA-256
trailer seals the file bytes. Emission is atomic where the filesystem supports it. Production
M3 owners may later consume or compact it into a primitive image using a separately sealed recipe.

Absolute domain ordering remains supplied by `@Sortable`/`ISignal` and interpreted through
`Double.compare`. Numeric signal separation has meaning only within a declared domain scale;
comparison sign, coordinate separation, and semantic graph distance remain distinct evidence.

### Usage

Compile this module (Maven), then make it visible on javac's processor path:

```sh
mvn -f m3/compiler/source-signals/pom.xml test
javac --release 21 -proc:none \
  -processorpath m3/compiler/source-signals/target/classes \
  '-Xplugin:M3SourceSignals out=/absolute/path/source-signals.tsv include=annotated' \
  -d classes src/main/java/**/*.java
```

`include=annotated` (default) admits only top-level types annotated with
`com.m3.collections.sortable.Sortable`. `include=all` admits all analyzed top-level types,
which is useful for an explicit inventory; no repository-wide implicit analyzer is installed.
The `@Sortable` annotation is an external SOURCE-retained marker: this plugin never loads annotated
classes or creates a second annotation type. There is no required runtime dependency.

## Tests and acceptance

`M3SourceSignalPluginJUnitTest` runs the same focused harness through Maven/JUnit.
`M3SourceSignalPluginSelfTest` invokes a *real* Java 21 `JavaCompiler` + ServiceLoader, covering
attributed static/virtual/constructor edges, annotated-only selection, source argument ordering,
field/method declaration permutations, source mutation invalidation, SHA-256 trailer, and invalid
option rejection. These are focused tests, not JDK image/jtreg/JCK parity or full compiler tests.

Required before installation in `src/jdk.compiler` or release: canonical Synexia recipe acceptance,
source seal and license checks, full compiler module/regression suite, IDEA/Eclipse/Javac and Java
21 compatibility, incremental module builds, compiler errors and cancellation behavior, method
references/lambda/poly expressions, resource budgets and output image versioning, and exact
phase-promotion authorization. Static call references are **not runtime reference counts**.

## Donor review

See `qualification/CATEGORY_REVIEW.tsv`. Lombok's annotation-processing/AST implementation is
**design evidence only**; no Lombok source was copied. Javac's published Plugin/TaskListener APIs
supply the execution path. LeetCode/HackerRank/GeeksforGeeks organize sorting, ranking and graph
problem families, but their challenge solutions are not shipped as JDK code. Existing M3 Java/JNI
radix sorting and progressive evidence remain the consumers of projected primitive signals.
