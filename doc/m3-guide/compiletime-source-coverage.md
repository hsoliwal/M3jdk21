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
  [29-target custody manifest](../../m3/tooling/migration-recipes/src/main/resources/jdk/m3/rewrite/backport/jdk21-hash-pinned/m3-compiletime-regex-precompute/manifest.tsv)
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
| [M3BytecodeOffsets.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3BytecodeOffsets.java) | `b2320826fd113910e0871af43e4db11da8149c50` |
| [M3CallSiteCatalog.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3CallSiteCatalog.java) | `7bca56b75f1508f28496bb3b49e9d0c75b6a8e38` |
| [M3ClassScan.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3ClassScan.java) | `515e64b8d05fa6e2e95b2eadd9444d2ed0e11986` |
| [M3CompileTimeStringInventory.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3CompileTimeStringInventory.java) | `e8a3bf23421a20a32fafb2a0512cd5821bec1c55` |
| [M3ConstantRegexDifferential.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3ConstantRegexDifferential.java) | `cf569162caf181495486bacbb1f2a473ccae81d7` |
| [M3ConstantRegexGate.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3ConstantRegexGate.java) | `4c9c295041e47ea43d66c4d56daca527dcaadd49` |
| [M3ConstantRegexHarvester.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3ConstantRegexHarvester.java) | `82bf34b941573dab31b7850acf9058f97ef8c0e7` |
| [M3ConstantRegexImage.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3ConstantRegexImage.java) | `7523bc8e105c5365385c055feb85c0deb9504b2e` |
| [M3ConstantRegexImageWriter.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3ConstantRegexImageWriter.java) | `ff6a82e42ffb621ee1e49e5e2764b3fef75730c2` |
| [M3ContentKey.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3ContentKey.java) | `9d562196c1de0f57bb52f24f9c3aef788008b050` |
| [M3EscapeFlow.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3EscapeFlow.java) | `9eda59508ea15cb6ac72fa4cf9c74a08ed84193d` |
| [M3FormatPlan.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3FormatPlan.java) | `7f9ce9fc7de1553537243d1e1dd60af9828b45b3` |
| [M3InventoryReport.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3InventoryReport.java) | `52ae4a1e23943c39862b7f00fdca3750121ec52b` |
| [M3InventoryState.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3InventoryState.java) | `4ff81277a954327fd39079ba3d27698140c978c7` |
| [M3ItemReferenceScan.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3ItemReferenceScan.java) | `d9d94ebf3c1b88a8a09003846526ea77d8d6ae37` |
| [M3PatternNodes.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3PatternNodes.java) | `0b8bc735b876629db73e1ddbbd0cd796bc34e777` |
| [M3RegexRequirement.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3RegexRequirement.java) | `6dd3479f9ed3db72e3a498a5bdeea9b08715d50f` |
| [M3SiteRecorder.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3SiteRecorder.java) | `c094bbba5f7775fe84fea258d35e778201776362` |
| [M3Tsv.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/M3Tsv.java) | `0a12120890dd67d565dcff370ebb3b3b080115f2` |
