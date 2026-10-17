<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 AST sharing and leaf-first hierarchy

This packet connects the active Java compiler to the existing Synexia AST owners and exposes the receiving APIs in `com.m3.compiletime.ast`. It includes qualified transformation recipes, generated Java and JNI source custody, explicit compilation identities, bounded index residency, optional OpenRewrite conversion, and the L0–L5 hierarchy.

The canonical implementation revision is [`1bbe38f1746e78b334e74abb01cc932920023978`](https://github.com/hsoliwal/com.synexia/commit/1bbe38f1746e78b334e74abb01cc932920023978). The reviewed original source baseline is `e244d68fdee88d4a406be48e4ff1d31b60a17988`, reconciled against `892e3f6c51d78bc0948e8a8cf736055b4c70eafa`. The original M3JDK21 discovery baseline is `e336c083c90bd882030e9a370949136e191386f2`; the final compiler composition and receiving base are `4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`. Exact upstream recipe owners are reused from `3aa8e45d93f5fed721fdb64579b655f4a432a308`. The handoff records the resulting source hashes and finite qualification scope.

## Explicit processing levels

| Level | Scope | Input relationship |
|---|---|---|
| L0 | Per-file facts and atoms | Immutable admitted compiler source, source/context identity, and existing AST atoms |
| L1 | File summary | The file's L0 facts and atoms |
| L2 | Package | Its file summaries |
| L3 | Module | Its packages |
| L4 | Project | Its modules |
| L5 | Repository | Its projects |

`MIndexASTHierarchy` is the canonical hierarchy owner; `M3ASTHierarchy` is its receiving counterpart. Their associated facts types preserve the same meanings. Package is L2 and project is L4. Existing parser-granularity scopes retain their separate contracts.

The hierarchy stores child ranges and indexes in primitive arrays and evaluates admitted providers from leaves upward. Each published result binds source, complete caller-declared compilation context, provider, phase, and explicit dependency closure. Existing partial-AST and swap owners enforce lazy access and leases; a changed source generation receives a different identity.

## Compiler sharing and isolation

`M3File` freezes the source before parsing. The small, target-owned `M3ASTPC` hook supplies the exact characters accepted by the active javac parser before the ordinary PARSE notification. `M3JavacASTIndex` observes that task, exposes callback-scoped compiler views, and creates an immutable `M3AST` projection only when a consumer requests it. Multiple consumers share that projection; the observer creates no second parse or attribution task.

`M3CompilationContext` separates source identity from compilation context. Its isolated mode uses a fresh session identity. Sealed reuse requires the caller to provide complete ordered identities for options, dependencies, processors and source generation. The standalone plugin reports that cross-process semantic reuse is disabled. Live compiler trees, symbols and classloaders are not written to the persistent store.

`M3ASTWorkspaceCache` delegates residency and eviction to the existing `TieredSwapCache`. Leases pin active values. Cache identities bind source, context, provider, schema and phase; a checksummed frame rejects mismatched or corrupt persisted values before decoding. The existing SQLite JNI store provides cold storage. Resident limits account for declared payload weight and entries; javac's own live heap is outside that budget.

The compiler patch preserves the current target-owned `com.sun.source.util.M3AST` API, `M3ASTPC` plugin, exact-source retention, and primitive-image consumer. It adds the immutable-index observer and a callback on the existing primitive-image command's task. The qualified fixture preserves the generated images and sidecars byte for byte in both supported modes. The upstream module descriptor continues to own the built-in `M3AST` service registration.

## Optional OpenRewrite integration

The separate `m3/ports/compiletime-openrewrite` module adapts the pinned OpenRewrite 8.17.1 Java 21 visitor to the original javac unit and source. It requires `-XDcompilePolicy=simple`, `-XDallowStringFolding=false`, compatible javac exports, completed FLOW state for every top-level type, and an exact source-print check. It creates a fresh visitor for each demanded file and a session-owned type cache.

The adapter refuses incomplete, erroneous, lowered, foreign or reentrant compiler state. Its adversarial fixture demonstrates that a stock visitor can trigger class completion and another source read; that attempt is detected and no LST is published. The adapter does not claim that attempted reads can be undone. The core compiler module has no OpenRewrite runtime dependency.

## Enable the compiler plugin

After producing the artifacts with the [reproduction instructions](REPRODUCE.md), a standalone javac invocation is:

```sh
"$JAVA_HOME/bin/javac" \
  -J--patch-module=jdk.compiler=/absolute/m3-javac-patch.jar \
  -J--add-exports=jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED \
  -processorpath /absolute/m3-ast-sharing.jar \
  '-Xplugin:M3ASTIndex out=/absolute/m3-output' \
  -XDallowStringFolding=false Example.java
```

The `M3ASTIndex` plugin writes immutable AST images and a compilation receipt. Its name keeps it distinct from the upstream built-in `M3AST` plugin and its existing options. Its service entry is installed after the plugin class compiles. The canonical service descriptor preserves its two existing compiler providers and adds the index provider.

## Recipe and catalogue invariant

The implementation is produced by Maven projects executing the existing class-backed OpenRewrite snapshot recipes. Java changes pass through Java compilation units. Every target and template is hash-pinned before output is admitted. The recorded gates include exact output, duplicate and drift refusal, no-change replay, compilation, contracts and the applicable runtime/JNI tests. Corrections are made in recipes and regenerated through those recipes.

The [catalogue index](CATALOGUE_INDEX.tsv) points to the searchable operation catalogues and file review records. Entries identify the problem category, existing owner or donor, selected and rejected approaches, license, complexity, measured evidence and limits. GitHub sources are pinned. LeetCode, HackerRank and GeeksforGeeks supply discovery and adversarial problem shapes; challenge solution text is not imported as licensed implementation code.

The accompanying build-support packet extends the existing JMOD owners to preserve complete Maven, Gradle and Ant distribution layouts, selected module closure, launchers, native files and legal material. Its build-tool qualification is recorded separately so release-distribution checks cannot be mistaken for source-fork qualification.

## Qualification

| Area | Result | Evidence |
|---|---|---|
| Maven/OpenRewrite recipes | Exact Java/text outputs, drift and missing/duplicate-owner refusal, fixed points, and actual application passed. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/frontend/evidence/FINAL_UPSTREAM_COMPOSITION_SDK.json) |
| Public recipe and source custody | Four exact providers; seven clean Maven authoring compilations; four repeated Java/text SDK tests; all 128 receiving sources match, and a one-file change is refused. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/integration/evidence/provider-custody/QUALIFICATION.json) |
| Immutable source, compiler views and codec | 26 tests; 103 canonical runtime sources, 10 receiving facades and 4 compiler sources compiled strictly. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/frontend/runtime-tests/evidence/QUALIFICATION.json) |
| Bounded cache and real JNI | 14 cache runtime tests including 2 SQLite JNI tests; 16 javac-generated native exports; fresh native rebuild matches the tested binary. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/cache/evidence/QUALIFICATION.json) |
| L0–L5 hierarchy | 40 canonical/receiving hierarchy contracts on the final composed artifacts; immutable coordinate-prefix snapshots and overflow-safe existing partial-cache accounting. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/frontend/hierarchy/evidence/final-composed/QUALIFICATION.json) |
| Existing compiler API and service registration | Unmodified upstream test: 70 checks in a linked 5-module image with hash checks enabled; 9 separate coexistence checks for both plugins. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/frontend/upstream-contract/evidence/QUALIFICATION.json) |
| Primitive images | 18 checks; image and sidecar bytes preserved in both modes, using the original compiler task. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/frontend/primitive-image/evidence/RESULTS.json) |
| Ordinary Maven module | 138 exact inputs; 52 normal tests and 54 selected-native tests; required missing-library failure; upstream leaf self-test and service discovery passed. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/frontend/module-wiring/evidence/RESULTS.json) |
| Optional OpenRewrite 8.17.1 | Stock visitor consumes the same javac units; source/type identity and reference class bytes preserved; core works without Rewrite; reentry refuses publication. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/frontend/openrewrite/evidence/RESULTS.json) |
| Authoritative name mapping | 12 scoped mappings, 38 source/manifest pins; existing mappings and broader phase gates preserved. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/frontend/registry/evidence/QUALIFICATION.json) |
| Full canonical reactor | Unavailable: the sparse checkout omits child modules required by the pinned parent. Canonical standalone sources and the M3 receiving modules have separate successful proofs. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/frontend/module-wiring/evidence/RESULTS.json) |
| Custom source-built dependency coordinates | Normal Maven proof uses the receiver's documented ASM 9.6 / JUnit 5.10.2 CI overrides; the custom source-artifact defaults were not qualified here. | [receipt](../../../m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017/frontend/module-wiring/evidence/RESULTS.json) |
| Whole JDK, jtreg/JCK and other platforms | Separate gates; no broader phase promotion is inferred from these finite Linux/Java21 proofs. | Separate repository gate |

The plugin artifact SHA-256 is `f4baf0ae24e58c290a7de403c0571dccbe35c825694729aca40c3408db5b905c`; the javac patch SHA-256 is `3f50d59cca1cf7cf2811c0bb1da9314b71240c7f81f37854ed6fc021f03681b4`. The full receipt and source boundaries are in [handoff.json](handoff.json).

These finite gates do not establish a complete JDK build, jtreg/JCK qualification, universal plugin compatibility, a performance improvement outside the measured workloads, or promotion of the existing global receiving phases. The name-map changes preserve those broader gate states and distinguish this scoped compiler ingress from the full historical compiler-precompute counterpart.
