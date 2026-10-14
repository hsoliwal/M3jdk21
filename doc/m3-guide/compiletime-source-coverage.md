<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Compile-time production-source documentation coverage

This map routes all **19 production Java source files** physically under
`m3/ports/compiletime/src/main/java/com/m3/compiletime` at
[public source pin `8758991645037126985d56f16a208fec8a1eef38`](https://github.com/hsoliwal/M3jdk21/tree/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime).
The complete unfiltered pinned directory listing contains these 19 files;
none is omitted because it is package-private. There is no package-info.java
in that directory. These are source-file owners in **one package**, not 19
packages, 19 public APIs or the whole M3 estate.

The four references separate inventory/reporting (8 owners), regex
analysis/image/gate (6), harvest/differential receipts (2), and item-residency
scanning (3). A route identifies documentation scope. It does not mean a
method's semantics, malformed-input behavior, resource cost or runtime
integration has passed qualification. Builds, tests, examples, benchmarks and
policy/recipe/custody verifiers for these documentation passes are **NOT_RUN**.

## Project, package and dependency boundaries

- The containing project is standalone
  `com.m3:m3-compiletime-precompute:0.1.0-SNAPSHOT`, compiled for Java 21;
  its [POM](../../m3/ports/compiletime/pom.xml) has no parent or modules
- It depends on ASM, ASM tree, ASM analysis, the separate `m3-precompute`
  artifact and test JUnit. Surefire disables the module path and adds the
  reflective regex package open. Use its explicit
  [dependency-first build instructions](../../m3/ports/compiletime/README.md)
- No module-info.java occurs anywhere in the complete compiletime project
  tree. The artifact is not a member of the
  [root control reactor's module/profile module lists](../../m3/pom.xml).
  Java package membership does not imply JPMS admission or root-reactor build
- The same project also has three production annotation sources in
  `com.m3.residency`: M3BehaviourOnly, M3Retains and M3Transient. They are
  residency-scanner dependencies, outside this 19-owner denominator; their
  presence does not count as another completed package
- The project has four JUnit classes and three fixtures. The
  [29-target custody manifest](../../m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/m3-compiletime-regex-precompute/manifest.tsv)
  copies 19 + 3 production sources and those seven test/fixture files. Custody
  copies are not additional implementation owners or semantic qualification
- The separate [precompute package map](precompute-source-coverage.md),
  [bootstrap Pattern/query reference](bootstrap-pattern-query-contracts.md)
  and [existing compiletime design](../../m3/docs/m3-compiletime-precompute.md)
  retain their own source, license, execution and authority boundaries

## Complete owner routes

All owner names below have a `.java` suffix. Visibility is top-level Java
visibility; public nested records or members are documented by their owning
reference and are not extra source owners.

| Production source owner | Visibility and role | Dedicated documentation route | Remaining qualification boundary |
| --- | --- | --- | --- |
| `M3BytecodeOffsets` | Package-private; Class-file bytecode positions | [Item-residency scan and sidecar](compiletime-residency-reference.md) | Malformed/truncated class geometry and exact versus synthetic negative offsets |
| `M3CallSiteCatalog` | Public; Catalog of recognized method signatures | [Inventory and report contracts](compiletime-inventory-reference.md) | Finite catalog and argument-position coverage, not whole-program call resolution |
| `M3ClassScan` | Package-private; ASM per-class scan and operand provenance | [Inventory and report contracts](compiletime-inventory-reference.md) | Frame budgets/failures, syntactic switch heuristics, skipped inputs and no full constant propagation |
| `M3CompileTimeStringInventory` | Public; Inventory CLI and ordered corpus scans | [Inventory and report contracts](compiletime-inventory-reference.md) | Filesystem/parse failure behavior and actual invocation coverage |
| `M3ConstantRegexDifferential` | Public; Finite stock comparisons and measurements | [Harvest and differential receipts](compiletime-harvest-reference.md) | Different harvested/oracle operation domains, counters and finite-corpus limits |
| `M3ConstantRegexGate` | Public; Explicit regex wrapper and formula evaluator | [Regex analysis, image and gate](compiletime-regex-reference.md) | Shared mutable stack, flags pairing, malformed entry behavior and broader Matcher semantics |
| `M3ConstantRegexHarvester` | Public; Regex harvest, image files and cost receipts | [Harvest and differential receipts](compiletime-harvest-reference.md) | Stable scanned/hashed inputs, sample scope, partial outputs and end-to-end execution |
| `M3ConstantRegexImage` | Public; Packed image reader and decoder | [Regex analysis, image and gate](compiletime-regex-reference.md) | Retained caller storage; incomplete hostile-input validation, decoded mutable arrays and mapping lifetime |
| `M3ConstantRegexImageWriter` | Public; Ordered packed-image producer | [Regex analysis, image and gate](compiletime-regex-reference.md) | Caller-supplied Analysis/Req consistency, collision scope, size/operand budgets |
| `M3ContentKey` | Public; Spelling-free key record | [Regex analysis, image and gate](compiletime-regex-reference.md) | Mutable/custom CharSequence input and finite-input collision checks; no authentication |
| `M3EscapeFlow` | Package-private; ASM escape/value-use classification | [Item-residency scan and sidecar](compiletime-residency-reference.md) | Conservative dataflow, merge and annotation/seed assumptions, actual classification tests |
| `M3FormatPlan` | Public; Formatter syntax-plan classification | [Inventory and report contracts](compiletime-inventory-reference.md) | Subset/record-array validity and absence of a runtime Formatter replacement |
| `M3InventoryReport` | Package-private; Long-form summary and regex tables | [Inventory and report contracts](compiletime-inventory-reference.md) | Exact schema/counter interpretations, partial writes and whole-input coverage |
| `M3InventoryState` | Package-private; Mutable counters and content-keyed run state | [Inventory and report contracts](compiletime-inventory-reference.md) | Serial state ownership, counting/overflow limits and no runtime admission |
| `M3ItemReferenceScan` | Public; Item residency sites and sidecar CLI | [Item-residency scan and sidecar](compiletime-residency-reference.md) | Two-pass hierarchy assumptions, 14-column schema, BCI fallback and sidecar meaning |
| `M3PatternNodes` | Package-private; Reflective private Pattern-tree adapter | [Regex analysis, image and gate](compiletime-regex-reference.md) | JDK-specific fields/classes, initialization and node/walk budget edges |
| `M3RegexRequirement` | Public; Necessary formulas and narrow exact literals | [Regex analysis, image and gate](compiletime-regex-reference.md) | Fabricated records, all flag/surrogate combinations, candidate versus complete match semantics |
| `M3SiteRecorder` | Package-private; Call-site classification and accounting | [Inventory and report contracts](compiletime-inventory-reference.md) | Split fastpath/nonconstant flag exclusions and finite classifier coverage |
| `M3Tsv` | Public; UTF-16 TSV escaping and decoding | [Inventory and report contracts](compiletime-inventory-reference.md) | Inverse on emitted cells, not a strict arbitrary-input validator |

## Reading evidence without promoting it

- The source-body/member references and pinned identities are documentation
  evidence. A matching Git blob proves which bytes were inspected, not that
  the implementation is correct or safe for every input
- The regex reference reads five requirement tests and four image tests.
  Other direct tests and fixtures belong to the inventory/residency
  references. Test-source assertions remain NOT_RUN; a title saying “proof”
  or “any byte” does not enlarge the actual test domain
- The harvest reference separates source-described future invocations from
  the design's historical 2026-10-13 measurements. Source review does not rerun
  those receipts or qualify the present runtime
- Image verification checks header/digests/aggregate geometry; it is not a
  complete hostile-image validator. A true necessary condition is only a
  candidate unless the separately admitted pure-literal path is complete
- Item-retention classifications and binary sidecars describe conservative
  scanner metadata, not a change in GC/lifetime behavior or a runtime consumer
- Package routing does not imply completion of project builds, dependent
  packages, vendor/custody trees, native/JDK integration, other repositories,
  license admission or the M3 porting sequence

The [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md) remains
unchanged. This map and its references add documentation/navigation only.

## Pinned production-file identities

The unfiltered directory listing and the complete project tree agree on this
set. These Git blob SHA-1 identities are independent of the eight-word digest
formats used inside runtime image and receipt files.

| Source file | Git blob |
| --- | --- |
| [M3BytecodeOffsets.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3BytecodeOffsets.java) | `4cd6f079f22c1800571054aefb6481a478758b36` |
| [M3CallSiteCatalog.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3CallSiteCatalog.java) | `b8adfe9ff668b4fbf01f9380cec74c255c530ae0` |
| [M3ClassScan.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ClassScan.java) | `eca944fa53ed8c084b978ad1a8b0e1b290773bd0` |
| [M3CompileTimeStringInventory.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3CompileTimeStringInventory.java) | `e6f85a51ba51e0b64a6079866f875bd109c9f3c6` |
| [M3ConstantRegexDifferential.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexDifferential.java) | `60c2a55cf818c809bb5f1dd4a61df2f4e3ffb215` |
| [M3ConstantRegexGate.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexGate.java) | `a8d3b1301d51c10aacab1fa4ce6bd9c2c17075ab` |
| [M3ConstantRegexHarvester.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexHarvester.java) | `d2d957060725e06a330fe7097b3b0a477c4e159f` |
| [M3ConstantRegexImage.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexImage.java) | `0b4a62e543ab2b6ddfe794edd1ec4ee2a83db319` |
| [M3ConstantRegexImageWriter.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexImageWriter.java) | `c19bd540361a8f4e3e2e7333103791bb6deb910c` |
| [M3ContentKey.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ContentKey.java) | `734e35504bacb787a1e47bc7e39fac7212b1e810` |
| [M3EscapeFlow.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3EscapeFlow.java) | `5e4e63059fc6b1a54ab66636aef2c9e0ae1e65cb` |
| [M3FormatPlan.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3FormatPlan.java) | `1e042908ba6775a1c82e3f842211817b7e451df1` |
| [M3InventoryReport.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3InventoryReport.java) | `006ae5a8877788ce4152cca1a1a937e903880a8f` |
| [M3InventoryState.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3InventoryState.java) | `578f5daf77970ad79a89c80f36f9093eb259b619` |
| [M3ItemReferenceScan.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ItemReferenceScan.java) | `cda993a1cb5654b34e3e339c4306bbb1c28881a4` |
| [M3PatternNodes.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3PatternNodes.java) | `0b640e6355aef525203648919d0da63ed32bdbec` |
| [M3RegexRequirement.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3RegexRequirement.java) | `bc7f55e9ea68d210263ae82bc8dab403ea63ef70` |
| [M3SiteRecorder.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3SiteRecorder.java) | `8cca345da7f6c856d44189ba45d009157f42667f` |
| [M3Tsv.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3Tsv.java) | `db1279c9f70861c6e14a2fe5b3d0573414a3572e` |
