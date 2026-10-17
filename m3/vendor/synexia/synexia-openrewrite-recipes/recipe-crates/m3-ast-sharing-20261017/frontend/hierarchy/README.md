# Lazy leaf-first AST hierarchy

This packet implements actual provider evaluation from existing per-file AST atoms through repository summaries in both `MIndexASTHierarchy` and its `M3ASTHierarchy` counterpart. `publish` builds bounded immutable identity/dependency metadata without loading an AST or evaluating a provider. The first acquisition evaluates children before their parent and uses the existing partial-AST and workspace cache owners. The receiver adds no parser, AST pool, LRU, or persistent store implementation.

| Level | Meaning | Input to an uncached provider evaluation |
| --- | --- | --- |
| L0 | Per-file facts and atoms | Exact source/context facts and an admitted projection from the existing `MIndexPartialAstMaster` |
| L1 | File summary | That file's L0 facts |
| L2 | Package | Its file summaries |
| L3 | Module | Its package summaries |
| L4 | Project | Its module summaries |
| L5 | Repository | Its project summaries |

The default `counts()` provider computes files, source UTF16 units, expanded nodes, per-file distinct atoms, and maximum depth. Its field name `expandedNodesSaturated` deliberately preserves the existing AST owner's saturated `expandedSize()` contract. Distinct atom counts are per-file counts added across files; they do not claim global cross-file deduplication. Other provider vectors have fixed ordered fields, a provider ID and schema, bounded arity, nonnegative values, and checked aggregate arithmetic.

## Existing owners and exact receiving paths

Canonical source lives in `synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/ast/`: `MIndexASTHierarchy` and `MIndexASTHierarchyFacts`. The receiver maps them to `m3/ports/compiletime/src/main/java/com/m3/compiletime/ast/`: `M3ASTHierarchy` and `M3ASTHierarchyFacts`. The packet also admits exact canonical supporting sources under `m3/vendor/synexia/`. All Java product changes and four contract test classes are outputs of `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`.

The implementation reuses `MIndexPartialAstMaster`, its adapter/key/projection/scope/pressure owners, and its bounded segmented cache for per-file admission. Those scopes describe parser granularity and remain unchanged. Existing partial compiler and leaf-index owners organize intra-projection computation or source edit spans; their purpose differs from repository containment and dependency invalidation. `M3ASTWorkspaceCache` supplies the existing `TieredSwapCache` lease and `SwapStore` frame boundary for optional cold hierarchy facts.

Two existing owner corrections are included as explicit sealed atoms. `MIndexAST.snapshot(int)` checks the coordinate-prefix limit before allocating reference tables and preserves immutable rows through the root coordinate. Later unrelated pool appends cannot change a previously supplied root's admission size. Earlier rows in the coordinate prefix remain necessary to preserve IDs and count toward the declared snapshot estimate; no AST row is reimported. `MIndexPartialAstCache` computes percentages without multiplication overflow and trims before adding large declared weights. It preserves the existing probation/protected policy. The original one-entry `Long.MAX_VALUE` budget defect is reproduced in `evidence/counterexamples/partial-cache-long-overflow/`.

An additive upstream owner appeared at canonical commit `892e3f6c51d78bc0948e8a8cf736055b4c70eafa` after this packet's source scope had been qualified. `MIndexASTLeafIndex` already provides a detached L0–L5 primitive evidence and mask-selection projection. It builds eagerly from supplied atom IDs and states, preserves physical offsets, and hashes its rollups. It does not supply compiler-context/provider isolation, explicit dependency invalidation, lazy partial-master resolution, or swap leases. It is suitable as a detached atom-state projection used by a provider; it is not included in this packet's compiled closure. The exact reviewed source and comparison are retained in `evidence/upstream-892e/`; this packet does not claim to replace that owner or to have qualified an adapter to it.

## Identity, invalidation, and lifetime

Every source input carries the full source URI, exact UTF16 source digest, actual compiler context, provider, schema, and phase. The hierarchy has a separate caller-sealed configuration context so the graph can change without conflating compilation generations. File suppliers return an already captured immutable `MIndexASTDocument` or `M3AST`; the hierarchy never invokes a parser. At admission it verifies the exact frozen source, raw document UTF16 digest, source length, budget, and occurrence atom owner before handing a partial projection to the provider.

Publish order is deterministic. Qualified addresses include repository, project, module, package and source identity. Duplicate source URIs, missing local dependencies, and limit violations refuse before replacing the current plan. Explicit local dependency lists are sorted and their transitive closure is bounded and cycle-safe. A source or dependency change changes the affected file seals and their containment ancestors. An unchanged address and full identity keep their prior record ordinal. Hashes are never truncated into cache IDs. External dependencies and compiler options belong in the supplied context pins.

Plans use level-sorted nodes and flattened child-offset/index arrays. Accessors return detached arrays. Previously returned plans remain immutable and may still be acquired; retaining old plans and their suppliers is the caller's responsibility. A hierarchy owner needs an exclusive cache/store record namespace. The caller must not mix independent ordinal allocators into that namespace.

Cache misses evaluate child facts before a parent. Successful immutable facts can spill through the existing checksummed cache frame, reload with the same identity, and be held under protected active leases. Cancellation, source refusal, provider schema drift, malformed output, codec corruption, or failed child evaluation publishes no successful parent. The hierarchy and partial cache limits are declared payload/work estimates. They do not bound process RSS, caller-held plans, compiler trees, or shared token backing. A held entry may make a new admission fail under the existing NORMAL capacity policy; this packet does not relax that policy.

## Qualification

`evidence/QUALIFICATION.json` seals the precise evidence and tested artifacts. The final source packet has six canonical and sixteen receiver rows. Actual SDK execution emitted the candidates, refused missing/duplicate/drifted preimages and changed templates, and reached a fixed point after application. Strict Java 21 compilation checked fourteen unique product FQNs with `-Xlint:all -Werror`.

Forty runtime checks passed: fifteen hierarchy and five facts-codec tests in each of the canonical and receiver packages. These cover zero-consumer laziness, the actual six-level provider order, stable ordinals, precise dependency invalidation, cycles and work budgets, old plans, context/provider/schema/phase isolation, cancellation and retry, concurrent consumers, spill/reload, active leases, source and atom ownership refusal, pool growth before first acquire, the existing cache's long arithmetic, immutable codec geometry, and an actual Java 21 compiler capture consumed without another parse.

The historical run used frontend artifact SHA-256 `3bbe637346b3ecf752bff60a1502f9b6eeb643448790c12b35fc9593fb807ed2` and compiler patch `10563c2088934d199984231d59b0980376ed3ba535c7aa3cd3cf625bd5d3f0ca`. Its receipt remains unchanged. A separate final composed-input recheck in `evidence/final-composed/QUALIFICATION.json` passed all forty contracts again, with zero errors, failures or skipped tests, using frontend `f4baf0ae24e58c290a7de403c0571dccbe35c825694729aca40c3408db5b905c` and patch `3f50d59cca1cf7cf2811c0bb1da9314b71240c7f81f37854ed6fc021f03681b4`. That recheck also strictly compiled all fourteen production owners with the corrected isolated-generation utility and built hierarchy artifact `2aa5f9dcea2b64006ff23306b0aa0929ed84f1002047bdae99fb4f45cc7880cb`. The initial mistaken pinned-capacity test expectation and original arithmetic counterexample remain in evidence history. This finite contract suite makes no full JDK, JCK, or global receiving-phase promotion claim.

## Reproduction from source custody

Use Java 21 and Maven with the pinned dependency versions in the POMs. Run JVMs sequentially with a bounded heap. The shared Java and text recipe source owners are the exact upstream pair sealed by `SOURCE_LINEAGE.json`; no alternate recipe engine is introduced.

The packet works from a canonical checkout or from the target's public `m3/vendor/synexia` custody tree. That mirror must include the shared recipe owners, the eight unchanged partial donors, this packet's retained preimages/resources, and the sibling frontend/cache packets. `prepare.py --canonical <source-root> --target <M3-root>` regenerates only proof resources, checks donor blobs and exact shared-owner hashes, and removes unreferenced numbered template resources. It never writes a product path. The changed existing owners' original bytes are retained here, so no unpublished canonical checkout is required to recover them. Test-only helpers come from pinned sibling cache resources.

From this directory, with a qualified sibling frontend base artifact available:

```sh
python3 prepare.py --canonical "$M3_SOURCE_ROOT" --target "$M3_TARGET_ROOT"
python3 build.py --stage bootstrap --jdk "$JAVA_HOME"
mvn -B -ntp test -Dm3.hierarchy.parserClasspath="$PWD/target/bootstrap/m3-ast-hierarchy.jar:$PWD/../target/m3-ast-sharing.jar"
python3 build.py --stage runtime --jdk "$JAVA_HOME"
mvn -B -ntp -f runtime-tests/pom.xml test
```

The bootstrap source compilation supplies parser types only. `build.py --stage runtime` accepts only actual SDK candidates with exact manifest hashes. To apply receiver outputs to a checkout after qualification, invoke the actual SDK application test:

```sh
mvn -B -ntp test -Dtest=M3ASTHierarchyRecipeTest#exactCandidatesAndFixedPoint \
  -Dm3.hierarchy.parserClasspath="$PWD/target/bootstrap/m3-ast-hierarchy.jar:$PWD/../target/m3-ast-sharing.jar" \
  -Dm3.hierarchy.materialize=true -Dm3.hierarchy.applicationOnly=true \
  -Dm3.hierarchy.labels=receiver -Dm3.hierarchy.target="$M3_TARGET_ROOT"
```

Canonical application uses `-Dm3.hierarchy.labels=canonical` and `-Dm3.hierarchy.synexia=<canonical-root>`. The default test qualification checks both manifests. The SDK application validates every resulting source and its fixed point before it reports success. All exact mappings and final source commit receipts are in the central registry and `m3/compiler/ast-sharing-20261017/handoff.json`.

Each compile uses a new temporary source/class directory, then publishes its complete JAR, admitted test inputs and receipt. It never reads old output classes. Parser consumers use the published bootstrap JAR. `--base-jar` can select another source-qualified frontend artifact, and `--output-directory` can isolate the published proof outputs; normal defaults remain within this packet. Java comes from `--jdk` or `JAVA_HOME`, with no private toolchain path fallback. `evidence/FINAL_GENERATED_PATHS.json` records the exact materialized cache/hierarchy production and test paths in both repositories, plus the registry output, and excludes unchanged upstream API/test custody.

## License and authority

New source, proof code and tests retain Apache-2.0 notices and the canonical contributor copyright. Exact donor source retains its original per-file notices. The original source-family pin is `e244d68fdee88d4a406be48e4ff1d31b60a17988`; the checked shared recipe pair comes from `3aa8e45d93f5fed721fdb64579b655f4a432a308`. `SOURCE_LINEAGE.json` and the sealed manifests identify the source bytes; the overall handoff supplies the actual final published commit receipt.
