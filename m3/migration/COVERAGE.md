# MIndex -> M3 migration coverage — pass 1

Pinned source: `hsoliwal/com.synexia@75fb1abaecb56969bea2914520bbe819f130632b`  
Pinned target baseline: `hsoliwal/M3jdk21@3029cff40e927aefcca444a6ef759a562204579c`

This report is intentionally fail-open only about evidence and fail-closed about completion: a discovered item is not considered migrated merely because a similarly named file exists.

## Inventory coverage

Committed machine-readable inventory shards:

| Source module | Tree/commit scope | Rows | State |
| --- | --- | ---: | --- |
| synexia-indexstring | tree `5c4bf95da683a752ddcddf79bfd063d894639c58` | 2,723 | pinned, every row has disposition |
| synexia-mindex | tree `3269b67624dfecfcfc858c746d20d32bb72ab3f2` | 2,214 | pinned, every row has disposition |
| synexia-mindex-indexstring-bridge | tree `f22e2048bb9b709b2462aa874784c7bd7807a5d7` | 29 | pinned |
| synexia-indexstring-compiler | tree `114c1c04700e9e76d9514690222d313dcdea1fd5` | 163 | pinned as Route B owner |
| synexia-indexstring-jini | tree `8010015d18a583757a277819fe1006da477273e0` | 12 | pinned optional native/JNI surface |
| synexia-indexstring-maven-plugin | tree `4138ef8507bf46aa61de484306be2f970da74cc7` | 40 | pinned transformation/tooling surface |

Total committed rows: **5,181**.

The GitHub recursive tree for the complete Synexia repository returned `truncated=true` at 39,372 entries, so it is not accepted as exhaustive evidence. The active core module trees above are individually untruncated.

Tree-pinned but not yet materialized as row-by-row shards:
- `synexia-mat-index-string@993fa6caae8aa436ce800ee4571c79ae3bc240b6`
- `synexia-dictlang@ab1a651e0d0bb205152bd58a29e73175f5113a94`
- `synexia-openrewrite-recipes@112f35d11ef07bf03035dd28322577fc8487d9b8`
- related primitives/unicodex/application modules discovered from the root inventory

The GitHub write connector refused the MatIndex and dictlang shard publications during this pass. They remain visible as partial tree-pinned mappings rather than being called complete.

## Canonical owner reconciliation

- Text owner: `com.synexia.indexstring.MIndexString` and its resolver/tuple storage.
- Range owner: `com.synexia.indexstring.SubMIndexString`; arbitrary UTF-16 ranges are preserved.
- Legacy `com.synexia.mindex.SubMIndexString` is a distinct compatibility contract because it rejects surrogate-pair-splitting ranges.
- Structural atom owner: `com.synexia.indexstring.MIndexAtomStore`.
- Structural projections: `MIndexAst` and `MIndexDag`.
- Existing structural conversion owner: `synexia-mindex-indexstring-bridge/MIndexCanonicalBridge`.
- Inventory/contract/search tooling is reused from `m3-java-inventory`, `m3-java-contracts` and `m3-fast-search`; it is not moved into `java.base`.

## Route status

### Route A — explicit stock-JVM view

Implemented on this branch:
- `com.m3.text.M3String`
- VM-local weak canonical admission
- exact UTF-16 equality/hash/compare
- retained-range substring/subSequence
- payload-reusing join/concat
- Java-hash composition across joins
- code-point behavior across segment seams
- independent mutable `char[]` outputs
- exact KMP search across seams
- JDK regex over the CharSequence view
- explicit ordinary-String materialization boundary

Local source-closure verification on OpenJDK 21.0.11 passed **15,536 checks** in default, `-Xint`, `-XX:-CompactStrings`, and C2-only modes. This was not a complete repository build.

Open Route A gates:
- direct immutable mapped lexicon backing; current M3 foundation copies mapped records into a local arena
- bounded/balanced descriptor policy for very large repeated compositions
- complete Java 21 String-surface differential matrix
- exact-head hosted build/CI

### Route B — compiler lowering

The compiler and Maven-plugin surfaces are inventoried separately. The canonical runtime membrane remains `MIndexStringCompilerRuntime`. No M3jdk21 compiler-lowering port is claimed yet. Evaluation order, side effects, nulls, exceptions, overload resolution, invokedynamic concat, mixed callers and identity-sensitive boundaries remain mandatory acceptance gates.

### Route C — modified complete JDK

Open draft PR #6 at `3776d6e674d6c9b04539ca24aca2504aa88d4a57` is the current inspected runtime candidate. Its checked-in historical receipt reports 86/0 flag-off tests and 16/2 enabled tests. The two enabled failures are StringJoiner OOME expectation tests. Compiled segmented execution, full jtreg/JCK and live SA attach remain open. Those receipts are historical evidence for that exact candidate only.

## Migration-control artifacts

- `baseline.json`: exact source/target/PR pins
- `migration-schema-v1.json`: manifest vocabulary
- `migration-mappings-v1.json`: contract-aware mappings and conflicts
- `inventory/*.tsv`: file-level pinned inventory/dispositions
- `recipes/route-a-v1/recipe.json`: exact pre/post hashes and rollback preimage
- `recipes/route-a-v1/preimage/LocalM3Arena.txt`: exact rollback source
- `tests/RouteAStringTest.java`: differential Route A test source

The connector refused publication of the executable recipe runner and the in-place refresh of the older `m3/docs/name-mapping.json`; both are open migration-control gates.

## Completion state

**Partial.** Route A has a tested stock-JVM implementation slice. Route B is inventoried but not ported. Route C remains an open modified-JDK candidate with mandatory failures/gates. Several source families are only tree-pinned. The migration must not be described as complete.
