<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Compile-time string inventory and report reference

This reference documents eight source owners in `com.m3.compiletime` at public source pin [`8758991645037126985d56f16a208fec8a1eef38`](https://github.com/hsoliwal/M3jdk21/commit/8758991645037126985d56f16a208fec8a1eef38). The complete owner bodies, the named tests, and the caller paths below were read. This is source-contract documentation: compilation, tests, examples, benchmarks and custody verification are **NOT_RUN** for this documentation change. It does not establish runtime acceptance or compatibility with arbitrary input class files.

The [original design][design] retains the dated 2026-10-13 inventory and performance evidence. Those results are historical, not a new measurement. See [compile-time source coverage](compiletime-source-coverage.md) for the 19-owner package map, [constant-regex analysis and image](compiletime-regex-reference.md), [harvest and differential receipts](compiletime-harvest-reference.md), and [item-residency scanning](compiletime-residency-reference.md). `M3BytecodeOffsets` belongs to the residency scanner; this inventory does not use it.

## Project and ownership boundary

The owning [POM][pom] is the standalone Maven artifact `com.m3:m3-compiletime-precompute:0.1.0-SNAPSHOT`, with no parent POM or child modules. It targets Java 21; compiler settings are `proc=none`, `-Xlint:all`, `-Werror`. Its production dependencies are `org.ow2.asm:asm`, `asm-tree`, `asm-analysis` at default version `9.6-m3-source-1`, and the separate `com.m3:m3-precompute:0.1.0-SNAPSHOT` artifact. Test JUnit defaults to `5.10.2-m3-source-1`. The POM comments describe upstream-version overrides; they are configuration options, not evidence that either configuration compiled here.

Surefire disables the module path, uses `-Xmx1g`, and opens `java.base/java.util.regex` to unnamed modules for the stock-regex node inspection used by sibling tools. There is no project `module-info.java`; a Maven artifact is not a named JPMS module. Neither this project nor [ports/precompute][precompute-pom] is included in the ordinary or profile module lists of the [M3 control reactor][reactor]. The [project README][readme] gives the explicit dependency-install, standalone-test and classpath build order. Its commands and custody commands were read, not executed.

The source headers identify these tools as Apache-2.0. This reference neither moves them into the bootstrap JDK nor changes any source, descriptor, license, workflow or recipe-custody input. The `com.m3.precompute` API is a dependency with [its own reference](regex-precompute-reference.md), not additional owner coverage here.

## Pipeline and entry point

[Inventory `main`][inventory] creates the output directory, opens the long-table writer, scans requested JDK modules and/or jars, then writes a summary and constant-regex corpus. It is a sequential, in-process bytecode inspection; it does not invoke methods in scanned classes or rewrite their bytecode. The regex and MessageFormat classifications do invoke host-library parsers on recovered strings.

| CLI token | Source behavior |
| --- | --- |
| `--out DIR` | Default `target/compiletime-inventory`; creates directories; named outputs are overwritten with ordinary file writers |
| `--jdk` | Scan the running JVM's `jrt:/modules`, not a separately supplied JDK installation |
| `--jars LIST` | Read all lines of a jar-list file; trim, drop blank lines, sort paths; scan each remaining path |
| `--regex-only` | Filter non-`b` **catalogued call-site recording** in `M3SiteRecorder`; it does not disable `ldc`, annotation, enum, concat, switch or special `String.equals` collection |

`arg(args,name,fallback)` returns the value following the first matching token that has a successor. It does not validate options, reject unknown tokens, check that the successor is a value, or diagnose a trailing missing value; that case falls back. The jar-list reader here does **not** recognize `#` comments. Duplicate paths remain duplicate scan inputs. A run with neither input option still creates output files. The in-package constructor accepts a null `rows` writer, as used by the harvester; that skips only per-unit output.

For JDK input, modules and class paths are sorted; `module-info.class` is filtered before scanning. For jars, all `.class` entries are sorted; `module-info.class` is counted as `all/module_info_skipped`, then `META-INF/versions/` entries as `all/multi_release_classes_skipped`. Multi-release versions are not selected according to the host runtime. The unit name is a module name or jar basename, so different paths with the same basename are not separately identified in output. Each completed unit's sorted metric rows are flushed. Progress and parse diagnostics go to stderr.

## Catalogue and constant recognition

[M3CallSiteCatalog][catalog] returns `Optional<Site>` from `lookup(owner,name,desc)`. Owners use JVM internal slash names; descriptors are JVM descriptors. A `Site(category,kind,arg)` identifies one operand: a zero-based argument or `-1` for the receiver. Matching precedence is exact `owner + '.' + name + desc`, then registered descriptor prefixes for that owner/name, then an exception-name heuristic. There is no hierarchy resolution, inherited-member search, custom registration API or lookup normalization.

| Category | Registered families and operand |
| --- | --- |
| `b` | `Pattern.compile(String[,int])`, `Pattern.matches(String,CharSequence)`; `String.matches`, `replaceAll`, `replaceFirst`, `split` overloads, `splitWithDelimiters`; Scanner `useDelimiter`, `findInLine`, `hasNext`, `next` string overloads. Pattern/string operand is argument 0 |
| `c` | `String.format`, `PrintStream`/`PrintWriter.printf` and `format`, `Formatter.format`: argument 0, or 1 after Locale. `String.formatted` uses the receiver. `MessageFormat.format` and its String/String+Locale constructors use argument 0 |
| `f` | ResourceBundle `getString`, `getObject`, `getStringArray`, `containsKey`; System property access/set and `getenv`; Integer/Long/Boolean property helpers; Properties access/set; selected `sun.security.action` helpers/constructors and `jdk.internal.misc.VM.getSavedProperty`. All use argument 0. `System.Logger.log(Level,ResourceBundle,String,...)` uses argument 2 |
| `g`, string operations | String search/replace/ignore-case operands at argument 0, StringBuilder/StringBuffer String append, StringBuilder String constructor |
| `g`, names | Class `forName`; descriptor-prefix matches for Class method/field lookup at argument 0 and selected MethodHandles.Lookup `find*` members at argument 1; Unsafe `objectFieldOffset(Class,String)` at argument 1; Charset `forName` and String `getBytes(String)` at argument 0 |
| `g`, logging | SLF4J trace/debug/info/warn/error prefix matches at argument 0 or 1 after Marker; JUL `log` at argument 1 and named level methods at 0; PlatformLogger level methods at 0; System.Logger `log(Level,String,...)` at 1 |
| `g`, exception messages | Constructor owner name ends in `Exception` or `Error`, with exactly `(String)V` or `(String,Throwable)V`; argument 0. The name suffix is not a verified Throwable subtype |

The [registration bodies][catalog] are the exact overload/descriptor authority. Prefix matches intentionally cover more than a single exact descriptor. `String.equals(Object)` is recognized separately by `M3ClassScan`, not returned by this catalogue.

[M3ClassScan][class-scan] builds an ASM tree with frames skipped. For a method containing catalogued calls or `String.equals`, it computes `instructions.size() * (maxLocals + maxStack + 1)` in long arithmetic. At at most **4,000,000 frame cells**, it requests `Analyzer<SourceValue>`/`SourceInterpreter`; larger methods increment `all/methods_over_frame_budget`. An `AnalyzerException` or `RuntimeException` increments `all/methods_analysis_failed`. Either failure leaves sites countable but operands unavailable.

A String is recovered only when the selected frame stack value has exactly one source instruction and that instruction is a String `LDC`. It does not evaluate arbitrary expressions, follow field values or perform the alias fixpoint used by the residency scanner. Merged provenance, non-String sources and absent/unreachable frames are not constant proof. Stack argument indexing uses descriptor argument count, not raw JVM slot width. `intOperand` accepts an `int` descriptor operand from `ICONST_M1` through `ICONST_5`, `BIPUSH`/`SIPUSH`, or an Integer `LDC`; absent/nonconstant results and constant `-1` share the `-1` result.

## What is counted

`M3InventoryState` is mutable state for one sequential run. `beginUnit` resets only the current unit map and selects corpus ordinal 0 (`jdk21`) or 1 (`vendor`); totals and content tables remain global. `add` and `inc` update both current-unit and current-corpus counters under `category + '\t' + metric`. Lengths are Java UTF-16 `String.length()` units, not UTF-8 bytes or Unicode code points.

| Lane | Source-defined observations |
| --- | --- |
| `all` | Successfully constructed ClassReader increments `classes`; wrapper failures increment `class_parse_failures`; analyzer/budget failures and jar exclusions have separate counters |
| `a` | Constant-pool entries with tag 8; every String `LDC` instruction, its units and length bucket; per-class/content pairs in the global `ldc` map |
| `b` | Catalogued regex sites, recovered constants, and classifications described below |
| `c` | Catalogued format sites, Formatter plan kinds, MessageFormat argument-format element counts or invalidity, and per-class/content format pairs |
| `d` | StringConcatFactory bootstrap sites; `makeConcatWithConstants` recipes count U+0001 argument markers, U+0002 constant markers, remaining literal units, string constant-tag units, recipe literal-unit length buckets and class/recipe pairs; other factory calls increment `indy_make_concat_sites` |
| `e` | String hashCode immediately followed by a real lookup/table switch; hash-key span/count; later same-local `ALOAD`, String `LDC`, `String.equals` triples; SwitchBootstraps sites and String bootstrap arguments |
| `f` | Collected class/field/method ordinary and type annotations plus parameter annotations; recursively encountered String, enum-name, class and nested-annotation values and method defaults; enum classes/names; catalogued keys |
| `g` | String ConstantValue fields and units; non-switch `String.equals` sites with receiver/argument constant counts; other catalogue families and simple logging placeholder signals |

The annotation collector does not enumerate every ASM annotation location: instruction, try/catch and local-variable type annotations are not visited. The string-switch recognizer is a syntactic scan of following instructions using the same local variable, not a control-flow proof that every matched equality is an actual case label. A recognized equality is excluded from the separate String.equals lane. Counts are source-defined observations of the scanned class files and these recognizers, not execution frequencies, a resolved call graph, or an exhaustive census of all equivalent source constructs.

`seen(table,text)` increments a content's per-corpus class count only once for the current global `classSerial`; duplicate input scans receive new serials. `Seen` stores `int[2] classes` and `lastSerial`, with no overflow handling. The nonnegative-length bucket labels are `len_0000`, `len_0001-0001`, `len_0002-0003`, doubling intervals through `len_0512-1023`, then `len_1024+`. The helper does not validate arbitrary negative caller input.

### Recording and regex handoff

[M3SiteRecorder][recorder] first increments `<kind>_sites`; an unavailable frame increments `<kind>_unanalyzed` and returns. A recovered String adds `<kind>_const` and `<kind>_const_units`. Category `f` has no extra classification. Category `g` adds only `<kind>_with_placeholders` or `_no_placeholders` for `log_template*` kinds, using substring tests for `{}` or `{0`, not a logging-format parser.

For kinds beginning `string_split`, `splitFastpath` accepts a single non-metacharacter or a backslash followed by a character that is not an ASCII letter or digit, excluding surrogate code units. Its exact metacharacter and ASCII-range checks are in the source. These sites increment `regex_class_SPLIT_FASTPATH` and never enter the regex key table. Only `pattern_compile_flags` uses the recovered second `int` argument; a negative flag result increments `regex_flags_not_constant` and also does not enter that table. Other kinds use flags 0.

Each remaining distinct `pattern + NUL + decimal flags` key stores the original pattern, flags, per-corpus site counts, sorted API kinds and sorted `corpus:unit` names. Classification is cached once using `M3RegexRequirement.analyze`; `shapeLiteral` is true for `Pattern.LITERAL` or a literal result from dependency `M3RegexShape`. Per-site metrics include `regex_class_<analysis kind>` and optionally `regex_shape_literal_sites`. The meaning and limits of requirement analysis belong to the [regex reference](compiletime-regex-reference.md), not to a claim that this inventory validates all regex behavior.

## Formatter plan contract

[M3FormatPlan][format-plan] is a public record `(Kind kind, int literalSegments, int conversions, int[] image)`. It produces coordinates into the caller's original format spelling, not a copied spelling store or an executable Formatter replacement.

| Image record | Meaning |
| --- | --- |
| `[1, begin, end]` | `LITERAL`: half-open UTF-16 range of the original format |
| `[2, conversion, flags, width, precision, argIndex]` | `CONVERSION`: packed conversion, flag mask and parsed optional numbers |

Flag bit order follows `-#+ 0,(<`. Missing width, precision and argument index are `-1`. Explicit index is stored as written, not decremented. More than nine decimal digits saturate to `Integer.MAX_VALUE`; at most nine are parsed as an int. For a date/time prefix `t` or `T`, the prefix stays in the low 16 bits and the following code unit is packed into the high 16 bits. Literal segments count only nonempty runs. `%%`/`%n` occupy conversion records but do not increment the `conversions` count.

`NO_CONVERSION` means that count is zero; `SIMPLE` means every counted conversion is bare lowercase `%s` or `%d` with no flags, width, precision or index; otherwise the result is `GENERAL`. An incomplete/unrecognized specifier returns `INVALID` with the partial image, literals and conversions collected before that specifier. The returned array is trimmed to used length. Ordinary literal/conversion scanning is linear apart from resizing copies; storage grows with input length.

The source comment's grammar and “never throws” descriptions must not be read as stronger validation than the implementation. The parser accepts repeated flags by OR-ing their bits, does not check flag/conversion compatibility or argument-index validity, and accepts any following code unit after `t`/`T`. For example, source inspection shows that a date suffix is packed without a validity lookup. Null input still dereferences `format.length()`; allocation failures are not caught. `Formatter` remains the semantic authority for actual formatting and argument-dependent errors. The record has no defensive-copy constructor or array accessor: callers can construct inconsistent records and mutate the supplied/returned `int[]`; record equality does not provide deep array equality.

The recorder caches only `Kind` per distinct Formatter spelling. MessageFormat strings take a separate path: `new MessageFormat(text).getFormats().length` produces `message_format_plan_NO_ARGUMENT` or `_ARGUMENTS` plus `message_format_elements`, or `_INVALID` after `IllegalArgumentException`. It uses the host default-locale constructor, even for a catalogued call with an explicit Locale. Both formatter families share the format content/class-pair table. There is no runtime format-plan consumer among these eight owners.

## Output schemas and ordering

All three direct inventory outputs use the JDK UTF-8 default for `Files.newBufferedWriter(Path)`. Writers are ordinary truncating writes, not an atomic multi-file transaction. Only the stated cells are escaped.

| File | Exact ordered header | Row semantics |
| --- | --- | --- |
| `COMPILETIME_STRING_INVENTORY.tsv` | `corpus`, `unit`, `category`, `metric`, `value` | Sorted category/metric keys within each traversed unit; corpus `jdk21` or `vendor`; numeric long value; unit names are written raw |
| `COMPILETIME_STRING_SUMMARY.tsv` | `corpus`, `category`, `metric`, `value` | JDK rows, vendor rows, then `all`; sorted category/metric keys; aggregate and content-dedup metrics |
| `CONSTANT_REGEX_CORPUS.tsv` | `pattern`, `flags`, `kind`, `matcher_state_admitted`, `shape_literal`, `atoms`, `jdk21_sites`, `vendor_sites`, `apis`, `unit_count`, `first_unit`, `reason` | Distinct retained regex keys in TreeMap String-key order; `pattern` and `reason` escaped; booleans rendered as Java text; API kinds comma-joined and sorted; unit names sorted, with the first name emitted |

`COMPILETIME_STRING_UNITS.tsv`, mentioned in the design, is a separate projection, **not** a fourth output written by these Java owners. Missing sparse counters are not universally written as zero. The `jdk21` column/corpus label is hardcoded; the CLI does not verify that the executing JVM is JDK 21 or embed its version in these three files. Labels and unit basenames are not escaped, so arbitrary tabs/newlines in those names would break fixed-column parsing.

[M3InventoryReport][report] sums the two corpus counter maps for `all`, then recomputes distinct-content metrics from the shared tables rather than adding per-corpus distinct counts. For each of `a/ldc`, `d/concat_recipe` and `c/format_string`, the metric suffixes are:

- `_distinct`: number of contents with at least one class in the selected corpus/union
- `_distinct_units`: sum of UTF-16 lengths once per distinct content
- `_in_2plus_classes`: contents appearing in more than one counted class
- `_class_pairs`: sum of class counts
- `_redundant_class_copies`: class pairs minus distinct contents
- `_redundant_class_copy_units`: sum of `(classes - 1) * content.length()` for repeated contents

The union also reports `a/ldc_distinct_strings_in_both_corpora`. Regex summary metrics count distinct retained keys, their analysis kinds, shape-literal status and matcher-state admission, including a key only if it has sites in that corpus. API/site deduplication is different from per-class content deduplication; neither means two jar basenames with the same name were one input.

### TSV codec and malformed cells

[M3Tsv][tsv] is a public stateless cell codec, not a row parser. `escape` preserves printable ASCII `0x20..0x7e` except backslash; encodes backslash as `\\`, tab/newline/carriage return as `\t`, `\n`, `\r`; and every other UTF-16 code unit as lowercase `\u` plus four hex digits. A supplementary character is two escaped surrogate units; an unpaired surrogate is preserved as its individual code unit.

`unescape(escape(text))` is the supported round trip for a non-null String. For arbitrary input, unknown `\x` escapes lose the backslash and yield `x`; a terminal backslash is retained. `\u` consumes exactly four following characters and delegates them to `Integer.parseInt(...,16)`, which can accept a leading sign; substrings rejected by that parser throw `NumberFormatException`, and short text throws a substring bounds exception. Null is not accepted. No row-width, type, version, checksum or input-size checks are supplied. The differential runner uses `unescape` on harvested pattern/input cells; malformed external files can therefore fail rather than receiving a controlled schema error.

## Failures, resource use and lifetime

- Checked IO failures reading lists, jars, classes or outputs propagate as `IOException`; lazy `Files.list`/`Files.walk` traversal can throw `UncheckedIOException`, and other runtime failures may escape. Output directories/files may already exist or contain partial results
- `classBytes` catches `RuntimeException` from the per-class scan, increments `class_parse_failures`, logs, and continues. Mutations already made for that class are not rolled back; even `classes` may already have incremented before a later failure
- An over-budget or failed analyzer still records each catalogue site as unanalyzed. An unreachable frame is also unavailable; it need not mean the whole analyzer failed
- The frame-cell budget limits analyzer admission per method, not overall heap use, class size, jar expansion, number of units, distinct contents or regex/MessageFormat parse time. Input classes and lists are read fully, and class trees and dedup tables consume memory
- Inventory state, nested arrays/maps and recorder are unsynchronized, mutable and run-scoped. No concurrency or reuse/reset protocol is provided. The class serial and per-content class/site counters are ints; arithmetic overflow is not handled
- There are no repository-source locations or bytecode offsets in these outputs. Call-site counts cannot alone identify a source edit, prove safe rewriting, or authorize bootstrap promotion

## Declared-member map

This map includes every explicit field, constructor, method, nested type and static initializer in the eight owners. Types in signatures are the imported Java/ASM types in the linked source. Compiler-generated record accessors/canonical constructor/equality methods and implicit constructors are not separately declared methods; their exposure is noted where relevant. Private helpers are mapped for maintainers, not offered as supported external APIs.

| Owner | Explicit declarations and role |
| --- | --- |
| [M3CallSiteCatalog][catalog] (public final) | Public nested record `Site(String category, String kind, int arg)`; private static final Strings `S`, `O`, `CS`, `LOC`, `P`, `STR`; private static final maps `EXACT`, `PREFIX`; static initializer invokes the six registration groups; private constructor `M3CallSiteCatalog()`; private static `regex()`, `format()`, `keys()`, `needles()`, `reflection()`, `logging()`; private static `put(String,String,int,String,String,String)` and `prefix(String,String,int,String,String,String)` populate tables; public static `Optional<Site> lookup(String,String,String)`; private static `boolean throwableName(String)` |
| [M3ClassScan][class-scan] (package-private final) | Private static final `long MAX_FRAME_CELLS`, Strings `CONCAT_FACTORY`, `SWITCH_BOOTSTRAPS`; private final `M3InventoryState state`, `M3SiteRecorder recorder`; package constructor `M3ClassScan(M3InventoryState,boolean)` and `void scan(byte[])`; private `constantPool(ClassReader)`, `annotations(ClassNode)`, `annotationValue(Object)`, `fields(ClassNode)`, `method(String,MethodNode)`, `ldcString(String)`, `indy(InvokeDynamicInsnNode)`, `concat(String,InvokeDynamicInsnNode)`, `stringSwitch(MethodInsnNode,Set<AbstractInsnNode>)`, `operands(String,MethodNode,List<AbstractInsnNode>,Set<AbstractInsnNode>)`, `equalsSite(MethodInsnNode,Frame<SourceValue>)`; private static `addAll(List<AnnotationNode>,List<? extends AnnotationNode>,List<? extends AnnotationNode>)`, `params(List<AnnotationNode>,List<AnnotationNode>[])`, `boolean isHashCode(MethodInsnNode)`, `AbstractInsnNode realPrevious(AbstractInsnNode)`, `SourceValue stackValue(MethodInsnNode,Frame<SourceValue>,int)`; package static `boolean isStringEquals(MethodInsnNode)`, `AbstractInsnNode realNext(AbstractInsnNode)`, `Optional<String> operand(MethodInsnNode,Frame<SourceValue>,int)`, `int intOperand(MethodInsnNode,Frame<SourceValue>,int)`. Unmarked private methods here return void |
| [M3CompileTimeStringInventory][inventory] (public final) | Private final `state`, `scan`, `rows` of types `M3InventoryState`, `M3ClassScan`, `BufferedWriter`; package constructor `(M3InventoryState,BufferedWriter,boolean)`; public static `void main(String[]) throws IOException`; package static `String arg(String[],String,String)`, `void log(String)`; package `void scanJdk() throws IOException`, `void scanJars(List<String>) throws IOException`; private `void jarEntry(ZipFile,ZipEntry) throws IOException`, `void classBytes(byte[],String)`, `void flushUnit() throws IOException` |
| [M3FormatPlan][format-plan] (public record) | Record components `Kind kind`, `int literalSegments`, `int conversions`, `int[] image`; public nested enum `Kind` with `NO_CONVERSION`, `SIMPLE`, `GENERAL`, `INVALID`; public static final ints `LITERAL=1`, `CONVERSION=2`; private static final Strings `FLAGS`, `CONVERSIONS`; public static `M3FormatPlan parse(String)`; private static `int[] specifier(String,int)`, `int digitsEnd(String,int)`, `int parseBounded(String,int,int)`, `int[] grow(int[],int)`, `int put(int[],int,int...)` |
| [M3InventoryReport][report] (package-private final) | Private constructor `M3InventoryReport()`; package static `void write(M3InventoryState,Path) throws IOException`; private static `void rows(BufferedWriter,String,Map<String,Long>) throws IOException`, `void dedup(Map<String,Long>,String,String,Map<String,M3InventoryState.Seen>,int)`, `void regex(Map<String,Long>,M3InventoryState,int)`, `void regexCorpus(M3InventoryState,Path) throws IOException` |
| [M3InventoryState][state] (package-private final) | Package static final `String[] CORPORA`; package nested static final class `Seen` with final `int[] classes` and `int lastSerial`; package nested static final class `RegexKey` with final `String pattern`, `int flags`, `int[] sites`, `TreeSet<String> apis`, `units`, mutable `M3RegexRequirement.Analysis analysis`, `boolean shapeLiteral`, and constructor `RegexKey(String,int)`; outer final maps `ldc`, `concatRecipes`, `formats` of String→Seen, `regex` of String→RegexKey, `formatKinds` of String→M3FormatPlan.Kind; final `List<Map<String,Long>> totals`; mutable `Map<String,Long> unit`, `int corpus`, `String unitName`, `int classSerial`; package `void beginUnit(int,String)`, `void add(String,String,long)`, `void inc(String,String)`, `boolean seen(Map<String,Seen>,String)`, static `String bucket(int)`. Outer and `Seen` constructors are implicit package-private |
| [M3SiteRecorder][recorder] (package-private final) | Private final `M3InventoryState state`, `boolean regexOnly`; package constructor `(M3InventoryState,boolean)`; package `void record(M3CallSiteCatalog.Site,Optional<String>,boolean,int)` and static `boolean splitFastpath(String)`; private `void regex(String,String,int)`, `void format(String,String)`, `void messageFormat(String)`, `void other(String,String)`; private static `M3InventoryState.RegexKey classify(String,int)` |
| [M3Tsv][tsv] (public final) | Private constructor `M3Tsv()`; public static `String escape(String)` and `String unescape(String)` |

## Caller and test evidence

The inventory constructs the class scanner; that scanner owns the recorder, queries the catalogue and updates shared state. The report consumes state only at the end. The [harvester][harvester] constructs an inventory with `rows=null, regexOnly=true`, reads state regex keys/ldc contents, and uses the TSV codec for output. The [differential runner][differential] reuses CLI/log helpers and decodes harvested cells. The residency scanner reuses only inventory argument/log helpers among these entry-point calls. These are reviewed caller paths, not an exhaustive dynamically resolved call graph.

| Test source and method | Assertions read; all NOT_RUN |
| --- | --- |
| [M3FormatPlanTest][format-test] `plansAreIntImagesOverFormatCoordinates` | Exact int image for `id=%s n=%d%n`, SIMPLE and two counted conversions; GENERAL for a flags/width/precision/date-index example; NO_CONVERSION for percent/newline; INVALID for `%q` and trailing `%` |
| Same class, `tsvEscapingIsReversible` | One round trip containing tab, backslash, newline, accented non-ASCII and an unpaired high surrogate |
| [M3RegexRequirementTest][regex-test] `splitFastpathMirrorsStringSplit` | Accept comma and escaped dot; reject dot, `\d`, and comma-space. The other four methods concern sibling regex analysis, not additional inventory test coverage |

The complete project test listing has four JUnit classes and seven Java test/fixture files. It has no dedicated catalogue, class-scanner, inventory/report CLI, malformed-TSV or formatter-invalid-input matrix test class. The focused assertions above do not cover IO interruption, class corruption, analyzer budget/failure, alias loss, duplicate basenames, all catalogue overloads or every Formatter rule. No compile/test result is inferred from reading them.

## Source provenance

All source links below are immutable links at the source pin above. The eight primary source identities are recorded here so this reference can be reviewed independently of later branch changes.

| Owner | Git blob SHA-1 | SHA-256 |
| --- | --- | --- |
| [M3CallSiteCatalog](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3CallSiteCatalog.java) | `b8adfe9ff668b4fbf01f9380cec74c255c530ae0` | `9a7a70368f5bdd8c5a9d4a89f828da117a2d5b09d5b3ad940bcf092bf21b71c3` |
| [M3ClassScan](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ClassScan.java) | `eca944fa53ed8c084b978ad1a8b0e1b290773bd0` | `69b449a216ccbd1adaf16ced4c2e179505b7b3c4d44b17ca5b7f52a2a35f89db` |
| [M3CompileTimeStringInventory](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3CompileTimeStringInventory.java) | `e6f85a51ba51e0b64a6079866f875bd109c9f3c6` | `6890b8e6f277eee20db07370db6ec16da63431bdb69687f99df5801a34d4369b` |
| [M3FormatPlan](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3FormatPlan.java) | `1e042908ba6775a1c82e3f842211817b7e451df1` | `1bbdb9e7d0161d1e3dcb0abc800509cab08080ea3cd2892b94bd6ac1c00db5e6` |
| [M3InventoryReport](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3InventoryReport.java) | `006ae5a8877788ce4152cca1a1a937e903880a8f` | `22d791343d4001427f6122f0f7c37d538aebad6039a772302487a06a718ade36` |
| [M3InventoryState](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3InventoryState.java) | `578f5daf77970ad79a89c80f36f9093eb259b619` | `2e22774e67cb4460f72784d164ecb6ba382f8ceba38cb9e757dac3f0325d5228` |
| [M3SiteRecorder](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3SiteRecorder.java) | `8cca345da7f6c856d44189ba45d009157f42667f` | `bf927b3073f19544408497cae8cb4f5ddd19fa0cd3fb54ebccbc249da55e8c89` |
| [M3Tsv](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3Tsv.java) | `db1279c9f70861c6e14a2fe5b3d0573414a3572e` | `ddaa8225b21ebf1a89ea17db591d9eacd98d2cc754fde51bd0fbc55145ba1547` |

[pom]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/pom.xml
[readme]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/README.md
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/pom.xml
[design]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/docs/m3-compiletime-precompute.md
[precompute-pom]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/precompute/pom.xml
[catalog]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3CallSiteCatalog.java
[class-scan]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ClassScan.java
[inventory]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3CompileTimeStringInventory.java
[format-plan]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3FormatPlan.java
[report]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3InventoryReport.java
[state]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3InventoryState.java
[recorder]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3SiteRecorder.java
[tsv]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3Tsv.java
[harvester]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexHarvester.java
[differential]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ConstantRegexDifferential.java
[format-test]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/test/java/com/m3/compiletime/M3FormatPlanTest.java
[regex-test]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/ports/compiletime/src/test/java/com/m3/compiletime/M3RegexRequirementTest.java
