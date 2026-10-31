# M3JDK atom/pattern spectrum result

The current-base reviewed candidate passed the complete focused sequence on 2026-10-05 at 08:02:44 UTC:
static source/corpus pins, Checkstyle 12.3.1, strict Java 21 production and test compilation,
39 JUnit cases with zero failures/errors/skips, then a source-bound runtime receipt audit.
The final recipe outputs and compiled owners have identical SHA-256 hashes.

The publication baseline is `hsoliwal/M3jdk21` master
`da958d00d24154c0db87beca0ec80a7df2b43b73`, root tree
`548f3d7c434c49c3989fc722f34ebba2ba9f0c5a`. Initial qualification and the unchanged real-source
corpus remain pinned to `e8fdab9a7abc4b0273438ce9e5d0ffd6255b1247`, root tree
`7d03f7bd4aab860c74cb436c082eca50d87c8391`. Every authored mutation is under
`m3/tooling/migration-recipes`. This result does not establish full default-module coverage,
an OpenJDK build, jtreg, or JNI/native correctness.

## Production changes and their executed causes

| Owner | Observed failure | Reviewed correction |
| --- | --- | --- |
| `M3AtomizePureIntReturnRecipe` | Return-leading and block-end comments were lost; later independent review reproduced loss of comments immediately before the original semicolon | Preserve the original expression tree, block prefix/end, return-leading comments and return statement right padding on the generated atom |
| Same atomizer | The complete generated permutation case exhausted its fixed 768 MiB heap during context-sensitive template parsing | Use the context-free typed-int template, whose local declaration/return needs no surrounding imports or source bindings |
| `M3PatternizePureIntAtomRecipe` | A comment containing `PURE_INT_EXPRESSION_NOT_ADMITTED` suppressed the canonical role marker | Compare the complete comment text after surrounding whitespace is stripped; keep the original lookalike comment |

The existing FILE-local primitive-int eligibility rules, method/class surfaces, dependency versions,
scope limits and recipe composition are unchanged. Three existing test fixtures also change: two
unused imports found by Checkstyle are removed, and the newly upstream coverage fixture receives a
real scheduled recipe context while retaining its explicit detached-node visits and accessor
assertions. No existing test or assertion was removed. Root files, JDK product source and JNI ABI
were untouched.

Both production files were materialized by the existing `M3HashPinnedJavaSnapshotRecipe` with the
`atom-pattern-spectrum` resource crate. The crate tests prove both exact outputs, refusal of drift
in either preimage, and unchanged replay after fresh parsing. The current upstream snapshot recipe
source is preserved unchanged.

| Production owner | Baseline SHA-256 | Final SHA-256 |
| --- | --- | --- |
| Atomizer | `8384244946df1f840406aaf71438b4f0dba311dfd7eba7b421a064d63c31bf3b` | `4f6c17a035e64106f172b488fded9b565d1af48d6a8ecbd4700b3ff0cc2cd1e1` |
| Patternizer | `2d100ed5d5810232b90b46f55f99c907f54a195b679b58f295dd065c8f03db69` | `b831177cbaaeca359a0b397eb4c74c0caf86c27666db702ef4492a42c210cd9f` |

Independent source reviews covered these final candidate hashes. A review finding was reproduced
as a failing regression before the final atomizer image was materialized and verified.

## Current-master integration

Publication preflight compared the original baseline with current master: 21 intervening commits
changed 23 files. All 41 originally intended write paths still matched their preimages or remained
absent, and none of the 100 pinned corpus paths changed. The current helper and two new upstream
test classes were added to the local execution closure before the publication run.

The shared snapshot helper is retained at Git blob `717c3e01f8f0407ce96439e3ecc96190b10c1eed`,
SHA-256 `9cf77c6e68a534e84b88230ca517fa6c5a76fb3e101ea3a1f68aa5222c3ec4d9`.
Its one-argument constructor continues to use Java LSTs. This task does not select or qualify its
new explicit PlainText opt-in, and the helper is not an authored change in the final patch.

The two newly selected upstream test files add eleven cases. Their actual integration failures
produced two bounded fixture corrections: removal of an unused import and execution of detached
inventory visits inside a real scheduled recipe cycle. Both detached methods are still explicitly
visited, every accessor assertion is retained, and an invocation counter verifies that the fixture
ran. No cycle metadata is fabricated. These two additional existing-file edits bring the patch to
five modifications and 38 additions, all inside the original authoring namespace.

[BASELINE.json](evidence/BASELINE.json) records the publication/source baselines, current helper,
source-preimage authority and compact preflight results. The complete preflight and integration
diffs are retained in the indexed raw evidence. The final compilation includes eight production and
twelve test sources; all 39 selected JUnit cases pass.

## Newer-base publication audit

After draft [PR129](https://github.com/hsoliwal/M3jdk21/pull/129) was created at
`a96338d9b72ae4c71638948b2e5e8f851ac07726`, a subsequent read reported `mergeable=true` against
master `ccb4ace7f7d79f97ae2d9600de55960504cc3c21`, root tree
`f5533ab50b60297c955d94a8e1190ad91d0bf831`. This supersedes the creation-time false observation;
the reason for that initial observation was not established.

The exact comparison from the tested parent `da958d00d24154c0db87beca0ec80a7df2b43b73` contains
six intervening commits and fourteen changed files for the A3 CLI/lab, its workflow/documentation
and its separate delivery crate. None overlaps the 43 published paths, selected production/test
sources, verification controls, resource selections or 100 pinned corpus paths. The focused POM
explicitly limits test compilation to `com/m3/rewrite/atom/*.java`, so the added
`com/m3/rewrite/M3A3LabDeliveryRecipeTest.java` does not enter the focused run even though the test
source directory is the parent `src/test/java` directory. The new A3 resource crate is likewise
outside the two explicit resource includes.

[PR129_BASE_AUDIT.json](evidence/PR129_BASE_AUDIT.json) records exact commits/tree, changed paths,
selectors, empty intersections and source-manifest hashes. All 43 original delivery hashes and
100 corpus hashes were rechecked locally. No source integration or test rerun was needed. The 39
passing cases remain the executed `current-3` result on the tested parent; this newer-base audit
does not claim qualification of the new A3 features or execution against a synthetic merge.

## Generated transformation proof

The new generated project contains 324 admitted primitive-int expressions: nine outer operators,
nine inner operators and four shapes. Each expression is exercised over 225 boundary-value pairs.
All six orderings of the actual atomize, patternize and document recipes run until a complete
unchanged sweep is observed, with a hard maximum of four sweeps.

The final receipt contains 45 intermediate checks. Every intermediate is freshly parsed with
strict type validation, compiled by the existing in-memory compiler, and compared with both the
immutable original program and an independent arithmetic calculation. Across those intermediate
states, each arithmetic oracle supplies 3,280,500 scalar result comparisons. This is a finite test
denominator, not a proof over every Java program or every int pair.

The checks also compare declared class/member surfaces, exception/effect traces, and opaque source
containing code-like comments, string literals and text blocks. Separate fixtures reject altered
arithmetic, a changed class modifier, invalid Java, source-pin drift and recipe-source drift. The
comment regression preserves seven distinct syntax locations, including block and line comments
before the return semicolon. Marker tests distinguish exact roles from prefixed/suffixed lookalikes
and retain exact-marker idempotence with ordinary, tab and Unicode whitespace.

All six orders reach the same canonical source hash
`dc263c314e3fd8a233760a1417fdc671e42e6712088b562ba1275015dfba63a8`.
Three orders need two sweeps and three need three sweeps, including their terminal unchanged sweep.
Fresh composite replay makes no change. The original generated source hash stays
`054feef3ea1b420510d6697b14f541cc6df2d388685b2c3b249ad130dbfbd0f8` throughout.

The 17 initial-baseline JUnit cases remain in the focused run, including the existing generated
100-file atomizer corpus and original 54-expression project. Current master adds eleven upstream
cases, and this task adds eleven cases, bringing the final run to 39. [COUNTS.tsv](evidence/COUNTS.tsv) and
[PERMUTATIONS.tsv](evidence/PERMUTATIONS.tsv) retain the exact new denominator and hash chain.

## Real repository source evidence

The manifest pins 100 actual repository sources, totaling 4,004,365 bytes. Each source is checked
against its byte length, SHA-256 and Git blob SHA-1, and rechecked after execution. The original
files are read from the checkout; their contents and licensing headers are not copied into the
shipped recipe resources.

### Individual-file survey

| Source category | Files | No-op fixed point after strict type admission | Parse refused | Type attribution refused |
| --- | ---: | ---: | ---: | ---: |
| java.base | 50 | 4 | 14 | 32 |
| javac / jdk.compiler | 25 | 6 | 0 | 19 |
| M3 | 25 | 5 | 0 | 20 |
| Total | 100 | 15 | 14 | 71 |

Zero real-source candidates were transformed in this survey. The 15 no-op outcomes are source
admission/fixed-point observations. The 85 refusals remain explicit gaps, including Java platform
internal annotations, javac source relationships and missing M3 sibling types. A refused file is
never counted as a successful transformation or behavioral proof. The detailed rows remain in
[CORPUS_ADMISSION.tsv](evidence/CORPUS_ADMISSION.tsv).

### Complete M3 source group

The same 25 pinned M3 files were also supplied together to the existing parser and strict compiler.
All 25 passed type validation and compilation without relaxing guards or changing OpenRewrite
8.17.1. Supplying these sibling sources resolves the 20 M3 attribution gaps observed in the
individual-file lane; both lane results are retained separately.

The compiled original M3 PrefixZ implementation was exercised on all 511 binary strings of length
zero through eight and four additional UTF-16 cases containing NUL, unpaired surrogates or emoji.
All 515 inputs matched an independent direct calculation of every per-offset prefix length,
similarity sum and longest proper border. This is a proof for the original M3 sidecar source group
and finite inputs. It is not transformed real-source behavior, an OpenJDK build or JNI execution.
[M3_GROUP.tsv](evidence/M3_GROUP.tsv) records this separate result.

## Retained failures and corrections

| Phase | Result retained | Consequence |
| --- | --- | --- |
| `initial` | Static pins passed; Checkstyle failed the existing unused test import | Removed the import; no compiler/test gate ran after the failure |
| `round-2` | New comment regression failed `ENTRY_CONTRACT`: expected one occurrence, found zero | Authored the first exact-source atomizer repair |
| `bootstrap` | Positive candidate materialization passed; negative fixture incorrectly expected an error callback | Corrected the fixture to assert the existing recipe's direct exception |
| `bootstrap-2` | Both exact-source meta-recipe tests passed | Materialized the intermediate comment repair |
| `target-1` | 24 of 25 tests passed; complete spectrum test exhausted 768 MiB during context-sensitive template parsing | Retained the denominator and heap bound; qualified a context-free template |
| `marker-baseline` | One of two marker tests failed on a suffix lookalike | Added exact whole-role comparison |
| `final-bootstrap` | Both complete two-owner meta-recipe tests passed | Materialized the first complete two-owner candidate |
| `final-target` | All 28 tests and runtime receipt audit passed for the earlier comment denominator | Independent review identified the untested statement-tail location |
| `trailing-comment-baseline` | Tail regression failed `TAIL_CONTRACT`: expected one occurrence, found zero | Added right-padding custody to the recipe image and expanded comment fixtures |
| `trailing-bootstrap` | Both updated two-owner meta-recipe tests passed | Materialized the final reviewed atomizer image |
| `final-reviewed` | Static, Checkstyle, strict compilation, all 28 tests and runtime audit passed | Initial-base result retained before current-master integration |
| `final-current` | Current helper and two new upstream test files integrated; static pins passed, Checkstyle failed an unused incoming test import | Removed only that import; no compiler/test gates ran after the failure |
| `current-2` | Strict prerequisites passed; all 39 tests executed, with one incoming fixture lifecycle error | Kept the detached-node condition and supplied its context through public `Recipe.run` |
| `current-3` | Static, Checkstyle, strict compilation, targeted detached fixture, all 39 tests and runtime audit passed | Final current-base candidate ready for review |

The final Maven JUnit run took 1 minute 8 seconds in the shared execution environment. That timing
is an observation of this run, not a performance benchmark. The same 768 MiB test heap bound used
for the retained exhaustion failure was kept. Source/fixture changes invalidated later receipts;
their required preceding gates were rerun before new results were accepted.

[RAW_GATES.log](evidence/RAW_GATES.log) contains the unaltered gate logs and retained final Surefire
XML as indexed byte ranges. [RAW_GATES_INDEX.tsv](evidence/RAW_GATES_INDEX.tsv) supplies each original
relative path, offset, byte count and SHA-256 so the exact raw files can be recovered and checked.
[GATES.tsv](evidence/GATES.tsv), [JUNIT.tsv](evidence/JUNIT.tsv),
[FINAL_COMMANDS.json](evidence/FINAL_COMMANDS.json) and
[RUNTIME_AUDIT.json](evidence/RUNTIME_AUDIT.json) provide the compact stage and runtime receipts.

## Scope of the result

This task improves existing Java tooling recipes and supplies a reusable Maven qualification crate.
It does not upgrade dependencies, widen transformation eligibility, modify the default reactor,
relax the 0.99 line/branch coverage thresholds, or overwrite unrelated PR124/PR125 owners. The default
migration-recipes verification gate, OpenJDK configure/make/jtreg and JNI/native gates were not
executed by this focused lane. Their outcomes remain separate and cannot be inferred from these
39 passing tests. [README.md](README.md) gives the ordered reproduction commands and the recipe-first
process for subsequent bounded tasks.
