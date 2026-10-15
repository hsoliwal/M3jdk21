<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Compile-time item-residency scanner and sidecar reference

This reference covers `M3ItemReferenceScan`, `M3EscapeFlow` and `M3BytecodeOffsets` in `com.m3.compiletime` at public source pin [`8758991645037126985d56f16a208fec8a1eef38`](https://github.com/hsoliwal/M3jdk21/commit/8758991645037126985d56f16a208fec8a1eef38). Their complete bodies, current test/fixture bodies and the annotation dependencies were read. Compilation, examples, tests, benchmarks and custody checks are **NOT_RUN** for this documentation change. Reading an analysis implementation and its assertions is not runtime acceptance, proof of general escape-analysis soundness, or verification of the separate SWT consumer.

The [original item-residency design][design] records a specific 2026-10-13 corpus and historical two-test execution. It remains historical evidence. The current source has seven JUnit methods and a **14-column site TSV**; this page records the current source contracts instead of silently substituting the older abbreviated schema or old test count. See the [19-owner package map](compiletime-source-coverage.md) and the [inventory/report reference](compiletime-inventory-reference.md) for shared CLI helpers and project boundaries.

## Ownership and lifecycle

The three owners live in the standalone Java 21 classpath artifact `com.m3:m3-compiletime-precompute:0.1.0-SNAPSHOT`. The [POM][pom] directly depends on ASM, ASM tree and ASM analysis (default `9.6-m3-source-1`), and `com.m3:m3-precompute:0.1.0-SNAPSHOT`; JUnit (`5.10.2-m3-source-1`) is test scoped. It has no parent or child modules, and the project contains no `module-info.java`. Surefire explicitly disables the module path. This is not a member of the [M3 control reactor][reactor]. Build order is the explicit standalone order in the [project README][readme]; none of those commands ran here.

These source files carry Apache-2.0 headers. The three CLASS-retained annotations under `com.m3.residency` are dependencies in the same Maven artifact but a different package. Reading them does not close that package's separate documentation scope. No SWT/runtime source or another repository is changed or certified by this reference.

The scanner is mutable, sequential state: hierarchy tables, annotations, rows, summary counters and sidecar records persist for its instance lifetime. It has an implicit public no-argument constructor but no public instance scanning API, reset, synchronization or defensive snapshot API. In-package callers use `hierarchy`, `closeTransientSet`, `sites` and `write`; the public supported invocation surface declared in this file is `main(String[])`. A new instance is used by the CLI and tests.

## CLI, inputs and two passes

[Main and input construction][scanner] accept:

| Token | Source behavior |
| --- | --- |
| `--out DIR` | Default `target/item-reference`; main creates directories before scanning |
| `--jdk` | One unit `("jdk","jrt:/")`, placed first; scan the running JVM's modules |
| `--jars LIST` | Each occurrence adds sorted, trimmed nonblank/non-comment jar paths under corpus label `jars` |
| `--label NAME=LIST` | Each occurrence splits at the first `=` and adds sorted jar paths under `NAME` |

Both jar-list forms omit lines whose trimmed text starts with `#`. This differs from the string-inventory CLI, which does not omit comments. Input groups retain command-line order after the optional JDK group; sorting occurs within each list, not across all groups. Duplicate jars/classes are not deduplicated. Labels and paths are not validated; malformed `--label` without `=` can fail by indexing the missing split element. Reused inventory `arg` returns the first following `--out` token value and falls back for a trailing option without a value. Unknown options are ignored. No inputs means a scan with seed types but no site rows.

`run(units,true)` walks all inputs for hierarchy/annotations, `closeTransientSet()` computes transient types, and `run(units,false)` reopens/reads them for sites. Input bytes are not pinned between passes, and this CLI does not write an input hash manifest. JDK modules/classes and jar entries are sorted. Both paths exclude module descriptors; jars also exclude `META-INF/versions/` class entries rather than selecting a multi-release runtime view. Unit is a module name or jar basename, so equal basenames from different directories are not distinguishable by that field. Complete class bytes are read into memory.

### Hierarchy and annotation admission

`hierarchy` skips code, frames and debug information, records only superclass links (`superOf.putIfAbsent`) and scans visible/invisible class and **method** annotations. It stores annotated methods as the full `owner + '.' + name + descriptor`, keeping overload identity. Multiple copies of a class preserve its first superclass entry while annotation sets accumulate.

| Dependency annotation | Declared target/retention | What this scanner actually reads |
| --- | --- | --- |
| [M3Transient][transient-annotation] | TYPE, CLASS | Visible/invisible annotations on the class seed transient type names |
| [M3Retains][retains-annotation] | METHOD or PARAMETER, CLASS | Visible/invisible **method** annotations seed retaining method descriptors |
| [M3BehaviourOnly][behaviour-annotation] | METHOD or PARAMETER, CLASS | Visible/invisible **method** annotations seed behavior-only method descriptors |

Parameter annotations are permitted by the annotation declarations, but `hierarchy` does not inspect parameter-annotation arrays. Do not rely on a parameter-only annotation changing this scanner's result. Class annotation closure follows superclass names, not interface inheritance. Its traversal checks at most 64 successive names per candidate class; it does not compute an unbounded graph fixpoint or report cycles/depth truncation.

The fixed seeds are SWT widgets `Item`, `TableItem`, `TreeItem`, `TableColumn`, `TreeColumn`, `TabItem`, `ToolItem`, `MenuItem`, `CoolItem`, `ExpandItem`, `TaskItem`, `TrayItem`; SWT custom `CTabItem`, `TableTreeItem`; Nebula grid `GridItem`, `GridColumn`; and Swing `javax/swing/tree/TreePath`. Each uses its internal JVM slash-qualified name. The transient set contains seeds even if no corresponding class was supplied, annotated types, and scanned classes whose recorded superclass chain reaches a seed/annotated root within the guard.

### Obtaining sites and analyzer budget

`obtained` recognizes an ordinary ASM `MethodInsnNode` other than `<init>` whose declared return type is a transient object or **one-dimensional** array of one, and `NEW` of a transient type. It does not infer transience from a runtime subtype, arbitrary field loads, method parameters, multidimensional arrays or invokedynamic return values. A NEW is a site in its own right; this recognition does not require proving that a later constructor completes. Constructor calls themselves are not additional obtaining sites.

For each nonempty method with at least one obtaining site, the scanner runs ASM `Analyzer<SourceValue>` with `SourceInterpreter` if `instructions.size() * (maxLocals + maxStack + 1) <= 4,000,000`. Over-budget methods increment `methodsOverBudget`; analyzer exceptions/runtime failures increment `methodsFailed`. In either case all obtaining sites in that method receive `ANALYSIS_FAILED` and normally classify UNKNOWN. An unreachable individual frame does not trigger that method-wide fallback; the escape walk skips null frames.

The budget bounds analyzer admission, not total memory or CPU. All site rows are retained until output, hierarchy and summary tables grow with input, and each obtaining site allocates a method-sized taint array and performs its own repeated fixpoint scan. There is no site count, class size, archive expansion, overall heap, time or fixpoint-iteration cap.

## Forward def-use and classification

[M3EscapeFlow][escape] seeds a boolean array at one obtaining instruction's ASM index. It repeatedly scans every instruction until none newly inherits taint, using the source sets in the already-computed frames. It then ORs the effect of every consuming instruction with a non-null frame.

Propagation is implemented for `ALOAD` from a tainted local; `ASTORE`, `CHECKCAST`, `DUP`; the listed `DUP_X1`, `DUP2`, `DUP_X2`, `DUP2_X1`, `DUP2_X2` stack depths; and `AALOAD` whose array source is tracked. It also recognizes the exact static `java/util/Objects.requireNonNull` descriptors `(Object)Object`, `(Object,String)Object`, `(Object,Supplier)Object`, tracking their first argument as the returned alias. Other return-alias helpers, including `requireNonNullElse`, have no such propagation rule.

This is a finite source/taint approximation with explicit opcode rules. It does not analyze a callee body or give a blanket guarantee for every aliasing language construct. “BEHAVIOUR_ONLY” means the implemented analysis produced zero recognized escape bits, subject to its recognized obtaining sites and propagation/consumption rules.

| Bit | Integer value | Kind | Recognized tracked-value use |
| ---: | ---: | --- | --- |
| 0 | 1 | `FIELD` | Value operand of PUTFIELD or PUTSTATIC |
| 1 | 2 | `ARRAY` | Value operand of AASTORE |
| 2 | 4 | `COLLECTION` | Argument to a recognized `java/util/` name-based collection/map mutator or `asList` |
| 3 | 8 | `RETURN` | ARETURN value |
| 4 | 16 | `LAMBDA` | Any tracked invokedynamic argument, except a StringConcatFactory bootstrap owner |
| 5 | 32 | `IDENTITY_COMPARE` | IF_ACMPEQ/IF_ACMPNE operands; recognized equals/comparison callees below |
| 6 | 64 | `IDENTITY_HASH` | Recognized hash/key-lookup uses below |
| 7 | 128 | `MONITOR` | MONITORENTER value |
| 8 | 256 | `ARG_UNKNOWN` | Tracked non-receiver argument not covered by an earlier callee rule |
| 9 | 512 | `ANALYSIS_FAILED` | Method frame analysis failed or exceeded budget |
| 10 | 1024 | `RETAINS_CALLEE` | Tracked argument to an annotated or seeded retaining callee |

`RETAINING` is the OR of bits 0–7 and 10 (1279). `classOf(mask)` returns `SITE_RETAINS=2` if any retaining bit is present; otherwise zero mask returns `SITE_BEHAVIOUR_ONLY=1`, and every other nonzero mask returns `SITE_UNKNOWN=0`. Mixed retaining and unknown bits classify RETAINS. `kinds(0)` is `-`; nonzero known bits become comma-separated names in bit order. The helper does not validate unknown high bits; those bits have no name in its eleven-entry list.

### Receiver versus argument and callee precedence

A tracked receiver of an instance call is treated as behavior except method name/descriptor `hashCode()I` (IDENTITY_HASH) or `equals(Object)Z` (IDENTITY_COMPARE). These receiver rules are not restricted to `java/lang/Object`. Tracked non-receiver arguments are classified independently, so one call can add bits for both roles. `MONITOREXIT`, POP, ordinary branches and opcodes absent from `use` add no bits.

For each tracked argument, `callee` follows this exact first-match order:

1. Full-descriptor method in `retainingMethods`, or owner/name in `RETAINING_CALLEES`: RETAINS_CALLEE
2. Full-descriptor method in `behaviourMethods`, or owner/name in `BEHAVIOUR_CALLEES`: no bit
3. Owner starts `org/eclipse/swt/` or `org/eclipse/nebula/` and name is in `BEHAVIOUR_NAMES_ON_SWT`: no bit
4. `java/lang/System.identityHashCode`: IDENTITY_HASH
5. `java/util/Objects.equals`: IDENTITY_COMPARE; any Objects name starting `hash`: IDENTITY_HASH
6. Any `java/util/` owner: collection-name set or `asList` → COLLECTION; `contains`, `indexOf`, `lastIndexOf` → IDENTITY_COMPARE; remaining key-lookup names → IDENTITY_HASH
7. Otherwise ARG_UNKNOWN

The fixed name sets are part of the current heuristic contract:

| Set | Exact contents (owners use slash names) |
| --- | --- |
| `BEHAVIOUR_CALLEES` | `java/lang/String.valueOf`, `java/lang/StringBuilder.append`, `java/lang/StringBuffer.append`, `java/util/Objects.requireNonNull`, `java/io/PrintStream.println`, `java/io/PrintStream.print` |
| `BEHAVIOUR_NAMES_ON_SWT` | `indexOf`, `showItem`, `setTopItem`, `setInsertMark`, `select`, `deselect`, `setSelection`, `showColumn`, `isSelected` |
| `RETAINING_CALLEES` | `org/eclipse/swt/custom/TableEditor.setEditor`, `TreeEditor.setEditor` and `TableTreeEditor.setEditor` in that same custom package; `org/eclipse/swt/widgets/Widget.setData` |
| `COLLECTION_NAMES` | `add`, `addAll`, `put`, `putAll`, `putIfAbsent`, `offer`, `offerFirst`, `offerLast`, `push`, `addFirst`, `addLast`, `set`, `addElement`, `insertElementAt`, `setElementAt`, `compute`, `computeIfAbsent`, `merge`, `replace` |
| `KEY_LOOKUP_NAMES` | `get`, `containsKey`, `remove`, `getOrDefault`, `contains`, `indexOf`, `lastIndexOf` |

The fixed sets generally match owner/name without descriptors or subtype resolution; only annotation sets and requireNonNull **alias propagation** use exact descriptors. Method annotations apply to any tracked argument of that method, not an argument-index-specific policy. If both annotation kinds are present, retaining wins. `Objects.equals` is an identity-comparison classification here, despite the older design prose listing it among behavior-only seeds. The source does not implement a general ControlEditor-subclass retaining rule.

StringConcatFactory invokedynamic arguments add no escape bit; other invokedynamic arguments use the LAMBDA bit regardless of whether their bootstrap is literally a lambda factory. These names describe source classifications, not an independently verified library retention contract.

## Bytecode offsets and site identity

[M3BytecodeOffsets][offsets] decodes raw Code attributes because ASM tree instruction nodes do not retain bytecode offsets. `of(ClassReader)` skips fields and maps `methodName + descriptor` to `int[]` byte offsets, starting each code array at BCI 0. `decode` records each instruction start and advances by `size`.

`size` handles 2/3/4/5-byte opcode groups, conditional branches and short goto/jsr, wide variants (6 bytes for wide iinc, 4 otherwise), and table/lookup switches aligned relative to `pc+1`. Table length uses signed low/high ints; lookup length uses signed pair count. Ordinary unlisted opcodes default to length 1. The raw decoder assumes valid class-file structure: it does not validate magic/attribute boundaries/opcode legality, positive switch lengths, arithmetic overflow or an exact terminal BCI. It is not a hostile-class-file validator and has no bounded malformed-input recovery loop.

The scanner separately counts real ASM instructions (opcode at least 0), ignoring labels, frame and line nodes. If the decoded array exists but its length differs from that real-instruction count, `offsetMismatches` increments and the method discards all decoded offsets. Missing or discarded offsets use `-1 - realInstructionOrdinal`: these negative values are fallback identities, **not** physical byte offsets. An absent offset array itself does not increment `offsetMismatches`.

`siteKey(owner,name,desc,bci)` hashes `owner + '.' + name + desc + '@' + decimal bci` with FNV-1a-32 over Java UTF-16 code units: initial bits `0x811C9DC5`, XOR each char, multiply by `16777619` with int overflow. Corpus, jar/module unit, transient type and callee do not enter the key. Owner/name/descriptor must agree with the consumer's naming conventions; fallback negative BCI does not prove matching runtime lookup. The test recomputes one SWT-named vector locally; no external consumer was executed or independently reviewed here.

## Output contracts

[Write and record][scanner] produce three files after both passes. The two text files use UTF-8 via `Files.newBufferedWriter(Path)`. Strings are joined directly with tabs: no M3Tsv escaping is applied. Operator labels or unusual names containing tabs/newlines can therefore break the tabular layout. Writes truncate existing named files, are sequential rather than atomic as a group, and can leave partial outputs after failure. In-package `write` assumes the output directory already exists; main creates it.

### ITEM_REFERENCE_SITES.tsv: 14 columns

| Position | Header | Value |
| ---: | --- | --- |
| 1 | `corpus` | Input label (`jdk`, `jars`, or supplied label) |
| 2 | `unit` | Module name or jar basename |
| 3 | `class` | ASM internal class name |
| 4 | `method` | Method name, including special JVM names where applicable |
| 5 | `desc` | Method descriptor |
| 6 | `bci` | Decimal raw bytecode offset or negative ordinal fallback |
| 7 | `obtain` | `new` or `invoke` |
| 8 | `transient_type` | Internal transient element/object name; an array site stores its element type |
| 9 | `callee` | Invoke owner/name, **without descriptor**, or `transient_type.<new>` |
| 10 | `site_class` | `UNKNOWN`, `BEHAVIOUR_ONLY`, or `RETAINS` |
| 11 | `kinds` | `-` or comma-separated known mask names |
| 12 | `mask` | Decimal mask |
| 13 | `site_key` | `Integer.toHexString` 32-bit bits: lowercase, unpadded, no `0x`; a negative int is rendered in two's-complement hex |
| 14 | `scope` | `internal` when the caller class starts with the transient type's package prefix, otherwise `caller` |

Scope is a textual package-prefix test including its slash, so it also includes subpackages; it is not access-control, ownership or module evidence. For a type in the default package the prefix is empty and all callers satisfy it. Site rows retain traversal/class/method/instruction order and are not deduplicated by site key.

### ITEM_REFERENCE_SUMMARY.tsv: variable-width bucket rows

The literal header is `bucket`, `behaviour_only`, `retains`, `unknown`, `total`. For every site, six bucket keys are incremented:

- `ALL`
- `corpus`
- `corpus + TAB + transient_type`
- `SCOPE + TAB + internal|caller`
- `corpus + TAB + SCOPE + TAB + internal|caller`
- `OBTAIN + TAB + new|invoke`

Rows are sorted by the entire String bucket key. Because the keys themselves contain literal tabs, **the file is not uniformly five columns**: take the final four fields as the four numeric counts and the preceding one to three fields as the bucket key. Labels that overlap reserved bucket forms can combine counters; there is no namespace escaping or collision validation. Each site contributes to six views, so summing the entire file is not a total-site count.

The final row is a different META record with these exact fields:

`META`, `classes_with_sites_scanned=<classes>`, `methods_with_sites=<methods>`, `analysis_failed=<methodsFailed>`, `over_budget=<methodsOverBudget>`, `offset_mismatch_methods=<offsetMismatches>`, `transient_types=<transientTypes.size()>`.

Despite the label `classes_with_sites_scanned`, `classes` increments for **every successfully ASM-parsed class** in the sites pass, before checking whether it contains any obtaining site. `methods` counts methods with obtaining sites; failure/budget and mismatch counts are method counts. Hierarchy/site parse failures are logged but have no META failure counter. Treat META as metadata, not a numeric bucket row.

### item-reference-sites.m3irs: big-endian sidecar

The design calls this format `M3IRS01`; that is a format name, not a literal seven-byte ASCII header. The writer uses `DataOutputStream.writeInt` and emits exactly:

| Byte offset | Int32 contents |
| ---: | --- |
| 0 | Magic `0x4D334952` (`M3IR`) |
| 4 | Version `1` |
| 8 | Unique site-key count `n` |
| `12 + 8*i` | Site key, for row `i` |
| `16 + 8*i` | `(mask << 2) \| siteClass`, for row `i` |

All integers are big-endian. Rows sort by Java **signed** Integer order, not unsigned key magnitude or the hex String order. Writer size is `12 + 8*n` bytes for an ordinarily completed output. The low two kind-word bits carry classification (0/1/2); the upper bits carry the eleven-bit mask. There is no embedded class identity string, checksum, input manifest, timestamp, trailer or extra schema-validation block in this writer.

When any sites share a key, `record` merges their masks by OR and their class by priority RETAINS > UNKNOWN > BEHAVIOUR_ONLY. This includes repeated identities across jars as well as actual 32-bit hash collisions; the writer does not distinguish them or keep a collision counter. TSV rows remain separate while binary entries merge, so row and binary-entry counts can differ. The merge avoids replacing a recorded retaining/unknown classification with behavior-only, but does not establish hash uniqueness or consumer compatibility. This packet contains no sidecar reader; reader refusal behavior is outside its coverage.

## Failure boundaries and operating limits

- Checked list, jar, class and write IO failures propagate as `IOException`; lazy `Files.list`/`Files.walk` traversal can throw `UncheckedIOException`, and invalid paths/options or other runtime failures may escape. The CLI has no structured usage/error result or atomic output rollback
- Hierarchy parsing catches `RuntimeException`, logs `HIERARCHY_PARSE_FAIL`, and continues. A partially updated hierarchy is not rolled back
- Site ClassReader construction/tree parsing catches `RuntimeException`, logs `SITE_PARSE_FAIL <unit> ...`, and skips that class. Raw offset decoding is **outside** that catch; decoder failures and later scan/record failures can propagate
- The analyzer has the stated conservative ANALYSIS_FAILED fallback, but an exception in escape propagation is not wrapped into that mask by `method`
- `closeTransientSet` logs its size and annotated count. The CLI's DONE site count means rows were written; the source does not independently verify the binary or compare it with a runtime consumer
- No test/corpus pass, closed-world completeness or bounded process memory claim follows from the finite per-method analyzer threshold

## Declared-member map

This is the complete explicit declaration map for the three primary owners, including private/package-private implementation contracts. Compiler-supplied implicit constructors are distinguished from explicit methods. Constants/sets and nested interface exposure here do not make them public APIs.

| Owner | Declarations |
| --- | --- |
| [M3BytecodeOffsets][offsets] (package-private final) | Private constructor `M3BytecodeOffsets()`; package static `Map<String,int[]> of(ClassReader)`, `int[] decode(ClassReader,int,int)`; private static `int skipMember(ClassReader,int)`, `int size(ClassReader,int,int)`; no fields |
| [M3EscapeFlow][escape] (package-private final) | Private constructor `M3EscapeFlow()`; package static `int escape(M3ItemReferenceScan,MethodNode,Frame<SourceValue>[],AbstractInsnNode)`, `int callee(M3ItemReferenceScan,MethodInsnNode)`; private static `boolean tainted(InsnList,SourceValue,boolean[])`, `boolean top(InsnList,Frame<SourceValue>,int,boolean[])`, `boolean propagates(InsnList,AbstractInsnNode,Frame<SourceValue>,boolean[])`, `int use(M3ItemReferenceScan,InsnList,AbstractInsnNode,Frame<SourceValue>,boolean[])`, `int indy(InsnList,InvokeDynamicInsnNode,Frame<SourceValue>,boolean[])`, `int call(M3ItemReferenceScan,InsnList,MethodInsnNode,Frame<SourceValue>,boolean[])`; no fields |
| [M3ItemReferenceScan][scanner] (public final): constants | Package static final ints `FIELD`, `ARRAY`, `COLLECTION`, `RETURN`, `LAMBDA`, `IDENTITY_COMPARE`, `IDENTITY_HASH`, `MONITOR`, `ARG_UNKNOWN`, `ANALYSIS_FAILED`, `RETAINS_CALLEE`, `RETAINING`, `SITE_UNKNOWN`, `SITE_BEHAVIOUR_ONLY`, `SITE_RETAINS`; String arrays `KIND_NAMES`, `CLASS_NAMES`, `SEED_TYPES`; long `MAX_FRAME_CELLS`; Strings `TRANSIENT`, `RETAINS`, `BEHAVIOUR_ONLY`; `Set<String>` fields `BEHAVIOUR_CALLEES`, `BEHAVIOUR_NAMES_ON_SWT`, `RETAINING_CALLEES`, `COLLECTION_NAMES`, `KEY_LOOKUP_NAMES`. Arrays have mutable contents despite final references; Set.of sets reject mutation |
| M3ItemReferenceScan: state | Package final `Map<String,String> superOf`; `Set<String> annotatedTransient`, `retainingMethods`, `behaviourMethods`; mutable `Set<String> transientTypes`; final `List<String[]> rows`, `Map<String,long[]> summary`, `Map<Integer,int[]> sidecar`; mutable longs `classes`, `methods`, `methodsFailed`, `methodsOverBudget`, `offsetMismatches` |
| M3ItemReferenceScan: invocation and input | Implicit public no-arg constructor; public static `void main(String[]) throws IOException`; package static `List<String[]> units(String[]) throws IOException`; private static `void addJars(List<String[]>,String,String) throws IOException`, `void walkJdk(String,ClassSink) throws IOException`, `void walkJar(String,Path,ClassSink) throws IOException`; package nested interface `ClassSink` declaring implicitly public abstract `void accept(String,String,byte[])`; package `void run(List<String[]>,boolean) throws IOException` |
| M3ItemReferenceScan: analysis and records | Package `void hierarchy(byte[])`, `void closeTransientSet()`, `void sites(String,String,byte[])`; private static `boolean has(List<AnnotationNode>,String)`, `int[] ordinals(MethodNode)`; private `String transientOf(String)`, `String obtained(AbstractInsnNode)`, `void method(String,String,ClassNode,MethodNode,List<AbstractInsnNode>,int[])`, `void record(String,String,ClassNode,MethodNode,AbstractInsnNode,int,int)`; package static `int classOf(int)`, `String kinds(int)`, `int siteKey(String,String,String,int)`; package `void write(Path) throws IOException` |

## Caller and test evidence

`M3ItemReferenceScan.sites` is the reviewed caller of `M3BytecodeOffsets.of`; `method` is the reviewed caller of `M3EscapeFlow.escape`. The latter consults scanner annotation sets/constants and calls `callee`. The scanner reuses `M3CompileTimeStringInventory.arg` and `log`, not its string-counting state or class scanner. The current JUnit class creates the scanner directly and feeds fixture class bytes through the hierarchy/sites methods; it does not invoke the output CLI.

All seven methods in [M3ItemReferenceScanTest][test] were read completely:

| Method | Focused assertion scope; all NOT_RUN |
| --- | --- |
| `classifiesEveryEscapeKind` | Annotated FakeItem enters the transient set; expected behavior, loop behavior, field/collection/return/identity/lambda/retaining-callee/unknown/array/hash/new classifications; all produced fixture BCIs are nonnegative |
| `siteKeyMatchesTheSwtEngineVector` | Locally recomputes FNV-1a-32 for one SWT-named owner/method/descriptor/BCI vector |
| `requireNonNullReturnedAliasesKeepTheirFieldAndReturnEscapes` | Field escape through all three recognized requireNonNull return aliases and return escape through the one-argument overload |
| `returnedAliasCastAndLocalCopiesKeepTheirEscape` | Cast/local-copy alias still has FIELD; requireNonNull followed by receiver behavior has no escape bit |
| `behaviourAnnotationDoesNotAuthorizeAnotherOverload` | Annotation on a String overload does not classify a FakeItem overload as behavior-only |
| `retainingAnnotationDoesNotClassifyAnotherOverload` | Retaining annotation on a String overload does not classify a different overload as RETAINS_CALLEE |
| `otherNullHelpersRemainUnknown` | `Objects.requireNonNullElse` remains ARG_UNKNOWN |

Fixtures [FakeItem][fake-item], [FakeTable][fake-table] and [ItemUsers][item-users] provide the class annotation, method annotations, array loop, retaining/behavior methods, and alias/overload cases. The test named `classifiesEveryEscapeKind` does not explicitly assert MONITOR or ANALYSIS_FAILED, despite its broad name. It does not cover sidecar write/read, variable-width summary parsing, signed sort, collision merge, negative-BCI fallback, malformed Code attributes, CLI parsing, parameter annotations, depth-64 hierarchy limits, or the complete seed catalogue. No claim of tested support for those cases is made here.

## Source provenance

The following are the three primary source identities at the pin above. Dependency/test links are pinned separately below. The historical design's corpus counts and binary hash are not regenerated by this documentation work.

| Owner | Git blob SHA-1 | SHA-256 |
| --- | --- | --- |
| [M3BytecodeOffsets](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3BytecodeOffsets.java) | `4cd6f079f22c1800571054aefb6481a478758b36` | `07b105d523774c1ac21f2c8190aa518c502113ffd9423815c8e62b2e2f7ed1ae` |
| [M3EscapeFlow](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3EscapeFlow.java) | `5e4e63059fc6b1a54ab66636aef2c9e0ae1e65cb` | `7d07697c8211dae24fba49eb8f5011064abc80a850f254d76fb9c56644329894` |
| [M3ItemReferenceScan](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ItemReferenceScan.java) | `cda993a1cb5654b34e3e339c4306bbb1c28881a4` | `d17344ac9c3488860e1487fe0072427c4a5688fc8a68393f8b782d92bd49a4ae` |

[pom]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/pom.xml
[readme]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/README.md
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/pom.xml
[design]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/docs/m3-item-residency-metadata.md
[scanner]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ItemReferenceScan.java
[escape]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3EscapeFlow.java
[offsets]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3BytecodeOffsets.java
[transient-annotation]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/residency/M3Transient.java
[retains-annotation]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/residency/M3Retains.java
[behaviour-annotation]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/residency/M3BehaviourOnly.java
[test]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/test/java/com/m3/compiletime/M3ItemReferenceScanTest.java
[fake-item]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/test/java/com/m3/compiletime/fixture/FakeItem.java
[fake-table]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/test/java/com/m3/compiletime/fixture/FakeTable.java
[item-users]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/test/java/com/m3/compiletime/fixture/ItemUsers.java
