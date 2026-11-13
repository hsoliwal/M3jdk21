# absorb-iop-20261110

The second jdk.m3.paradigm batch moves synexia-iop into `jdk.internal.m3.paradigm.contract`, which is internal and
not exported. It lands 246 main sources and 4 tests, and grows 8 owners already landed in
`jdk.internal.m3.paradigm.annotation`. User decisions from 2026-11-10 apply: "Union into AnOP owners", and IOP's
`Pattern` enum lands as `contract.PatternKind`.

## Selection (SEL_iop_v6.tsv, HELD.tsv)

| Count | What |
|------:|------|
| 325 | synexia-iop classes selected by select_multi v1.4 (SLF4J_FACADE=1) |
| 275 | eligible |
| 48 | held by selection: they import lombok.experimental, com.synexia.context.core, com.synexia.anop or com.synexia.cop, or need a held class |
| 2 | hubs held: InteractionMode, Library |
| 12 | twins of landed annotation owners, retired onto those owners (UNION.tsv) |
| 263 | placed in `contract` |
| 17 | held because jdk_naming would rewrite code, not just comments (see below) |
| 246 | main sources landed |

## One owner per concept (UNION.tsv, UNION_JAVAP.tsv)

IOP shares 13 simple names with the annotation package:
- 10 with AnOP;
- IFieldSpec, IMethodSpec and IParamSpec, which landed later with LazyInit (#2919).

None of them gets a second owner. The last 3 were found by a cross-package duplicate check over the module (see
Gates). The sprawl gate checks only jdk.m3.collections owners, so it does not catch these.

- **Union (3 more owners).** MEMBER_UNION v1.3 copies:
  - IFieldSpec: +25 elements and the nested Lifecycle, Option and Source enums.
  - IMethodSpec: +9 elements and the nested DebtLevel enum.
  - IParamSpec: +11 elements and the nested DebtLevel enum.

  No element has a different type on the two sides. javap -p shows 0 removed. 11 contract files now import the
  annotation owners. DOCLINT_REPAIR removed 3 copied `@see` lines; they pointed at contract types that the
  annotation package does not see.

- **Union (5 owners).** MEMBER_UNION v1.3 copies each IOP-only member into the landed annotation owner, with its
  javadoc and imports:
  - AIGenClass +2
  - AIGenMethod +3
  - AIGenerated +4
  - HashOps +4, including the nested CompareResult enum
  - ValidationUtils +11

  javap -p before and after shows 0 removed and only these additions (UNION_JAVAP.tsv).
- **Conflict.** `AIGenerated.model()` is `Model.ModelType` in AnOP and `String` in IOP. The owner keeps AnOP's
  version. IOP's version is not copied, and no IOP code reads it.
- **Equal or subset (4 owners).** Model is identical modulo package. The IOP members of LLM, Listener and Stability
  are subsets of the owner's members. IOP references resolve to the annotation owners.
- **Pattern.** It lands as `contract.PatternKind`. Three files also use `java.util.regex.Pattern`:
  - CourseClassifier keeps its regex import unrenamed.
  - DagPatternRuntime and EipPatternRuntime use it fully qualified; both are among the 17 held classes.

## Held for a naming decision (17)

jdk_naming would move `SynexiaPattern*` files and rewrite hash-domain string constants. Those are code changes,
not comment changes, so the closure is held:
- AllPatternFamilyRuntime, AopPatternRuntime, DagPatternRuntime, DistributedPatternRuntime, EipPatternRuntime
- IAnOPAnnotation, MicroservicePatternRuntime, SpringPatternRuntime
- PatternCatalog, PatternDescriptor, PatternEngine, PatternInvocation, PatternOutcome, PatternRecipe,
  PatternRecipeBook
- SynexiaPattern, SynexiaPatternRuntime

## Recipes

- **RELOCATE_M3_OWNER v6.3 with relocate_v5_lib v5.3.** Uses a module-scoped call table (call_iop.tsv).
  - **Defect found by the proof:** the simple-name rename `Pattern` → `PatternKind` also renamed 5 nested enums
    that are unrelated to the top-level enum: AIImplementationHint, IAIImplementationHint, IDagSpec, IPromptSpec
    and IopParadigm each have a nested `Pattern`. javap -c reported their `$Pattern` class files as MISSING.
  - **v5.3 rule:** a type that a file declares as a nested member shadows the same-package top-level type of that
    name throughout the outer body. So a rename of that simple name never applies in that file.
  - **Result:** replaying v5.3 on the donor sources gives the same text as the reverted files.
- **SLF4J_TO_PLACEHOLDER_LOGGER v1.1.** 18 files use `PlaceholderLogger`.
- **MEMBER_UNION v1.3.**
  - v1.1: skips members nested in a block already copied.
  - v1.2: an annotation element `default {}` ends at its `;`.
  - v1.3: carries the donor imports the copied members use.
- **DOCLINT_REPAIR v1.4.** Comment-only. The doclint error count went 66 → 14 → 0. New rules:
  - `unknown tag: X` is handled outside `<pre>`: `<X` becomes `&lt;X`, or a leading `@X` becomes `&#64;X`.
  - Malformed HTML is escaped at javac's caret column.
  - An invalid-uri `@see <a href>` block is removed.
  - A table missing its caption gets `<caption>`.
- **Banner.** `/* ****`.
- **DONOR_BUG_FIX.** `AnnotationsTest.testIAICapabilityExists` asserted `isAnnotation()` on IAICapability, which
  is the value enum of `@IAICapabilities`. The test now asserts `isEnum()`. The same fix is merged in com.synexia
  #16980 (d1f3494a), and the proof's donor side uses that blob.

## Proof (PROOF.tsv)

The donor side compiles with lombok 1.18.46 and slf4j-api 2.0.17. Its AnnotationsTest is the #16980 blob.
`norm_iop.sed` is the gen_norm_v5 map over the landed map, which includes the 12 twins mapped onto the annotation
owners. It adds the slf4j facade rules and strips constant-pool indices.
- **javap -c:** 0 of the eligible class files differ. This is on the second run; the first run reported 5
  `$Pattern` class files MISSING, which was the rename defect described under Recipes.
- **Mutant:** caught.
- **JUnit:** 243 tests on the donor and 243 relocated, all passing on both sides.
- **javap -v:** 18 class files differ. FACADE_RESIDUAL v1.1 (facade_residual.py) shows that all 52 class files of
  the 18 facade users differ only in the declared logger shape:
  - the `PlaceholderLogger.of` InterfaceMethodref, against slf4j's LoggerFactory Methodref;
  - the slf4j pool entries;
  - lombok's CLASS-retention `@lombok.Generated` marker on the generated `log` field.

  FACADE_ALL over every eligible class pair: 872 equal, 18 differ only in the facade shape, 0 residual (FACADE_ALL.txt).

## Gates

| Gate | Result |
|------|--------|
| Compile | All 395 jdk.m3.paradigm sources of this landing compile against jdk.m3.collections under `-Xlint:all,-module -Werror -Xdoclint:all/protected,-missing`: 0 diagnostics, 1020 class files |
| Doclint | `-Xdoclint:all,-missing` reports 0 errors on the batch plus the union owners |
| jdk_naming | Fixed point on src/jdk.m3.paradigm and on m3/paradigm (0 moves, 0 rewrites) |
| Sprawl gate | new=246, refused=0 |
| One owner (module) | No simple name is declared in more than one jdk.m3.paradigm package (`ls */*.java \| sort \| uniq -d` is empty) |
| Custody | jdk.m3.collections sources are untouched; module-source-successor `--check` reports changed=0 |
| Default suite | The werror reactor (m3/arrays, m3/collections, m3/paradigm) passes. m3/collections: 2841 tests, 0 failures, 0 errors, 151 skipped. m3/paradigm: 243 tests in 4 classes, including nested ones, with 0 failures and 0 errors. The first run failed one test, testIAICapabilityExists; that is the DONOR_BUG_FIX above. |
