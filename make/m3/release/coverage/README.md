# Reviewed changed-atom inventory (`M3_CHANGED_ATOM_COVERAGE`)

`changed-atoms.tsv` is the reviewed, complete inventory read by
`../build_changed_atom_coverage.py` (`--inventory`, with `--reviewed-complete-inventory`).

## Scope

Decided by the user through the release coordinator: the M3-owned atoms the gate
measures, i.e. every JaCoCo-measured method of the `jdk.m3.text` classes compiled
from `m3/core/src`, changed since the upstream OpenJDK baseline
`890adb6410dab4606a4f26a942aed02fb2f55387` (8314062, 2023-08-09).

None of `m3/core/src` exists upstream, so every method in it is changed:

```sh
git ls-tree 890adb6410dab4606a4f26a942aed02fb2f55387 m3     # prints nothing: no m3/ tree
git ls-tree --name-only 890adb6410dab4606a4f26a942aed02fb2f55387 src/ | grep -ci m3   # 0
git ls-files m3/core/src | wc -l                            # 27 files (26 Java + module-info)
```

## Derivation

```sh
bash ./make/m3/build.sh                      # with the JaCoCo agent, see below
bash ./make/m3/release/run_extra_core_contracts.sh
java -jar org.jacoco.cli-0.8.12-nodeps.jar report jacoco.exec \
  --classfiles m3/build/classes --sourcefiles m3/core/src --xml jacoco.xml --csv jacoco.csv
python3 make/m3/release/derive_changed_atom_inventory.py --report jacoco.xml --root . \
  --classfiles m3/build/classes --carry-exemptions make/m3/release/coverage/changed-atoms.tsv \
  --out make/m3/release/coverage/changed-atoms.tsv
```

JaCoCo analyzes every class in `--classfiles` whether or not it ran, so the method
set does not depend on which tests executed; `--classfiles` also refuses a compiled
class missing from the report. Re-run with `--check make/m3/release/coverage/changed-atoms.tsv`
to prove the committed file is byte-identical. Result: 607 atoms in 82 classes;
341 have no JaCoCo `BRANCH` counter and carry the explicit `branchless_explanation`;
18 carry a reviewed `exemption` (below). Exemptions are review data: `--carry-exemptions`
copies them by id and refuses one whose atom disappeared.
`id` is `<binary class name>#<method><JVM descriptor>`.

The measurement environment is the gate's: JaCoCo 0.8.12 runtime agent via
`JAVA_TOOL_OPTIONS="-javaagent:<agent>=destfile=<exec>,append=true,includes=jdk.m3.text.*"`,
`M3JDK`/`JAVA_HOME` set to a JDK 21, and a C compiler (`CC`, default `cc`) for the
JNI-backed owners built by `run_extra_core_contracts.sh`.

## Gate rule and the atoms below 90%

An atom passes at >=90% instrumented line and branch coverage, or as a reviewed
exemption: its `exemption` cell proves, with existing `path:line` citations, that the
uncovered branch only re-checks an invariant its callers already enforce. The check
stays in the product. `build_changed_atom_coverage.py --require-pass` and
`verify_release_admission.py` both apply the rule.

The 21 atoms below 90% in the first measurement were routed as follows.

| Atom | Route | Invariant (full proof with citations in `changed-atoms.tsv`) |
|---|---|---|
| `M3BigString#concat` | covered | new `chunkDirectoryLimit` case reaches the 2^31 directory refusal; 20/22 branches |
| `M3BigString#appendTo` | B | `from < to` always true; guard removed in a separate PR |
| `SharedLexiconCatalog#sha256(Path)` | B | `read != 0` always true; guard removed in a separate PR |
| `LocalM3Arena#lambda$identity$0` | A | identity counter needs 2^63-1 copies |
| `M3BigString#<init>` | A | every caller passes an admitted range |
| `M3BigString$Storage#<init>` | A | callers build aligned arrays of positive chunks, bounded piece count |
| `M3BigString$Storage#findChild` | A | public `charAt` bounds the offset |
| `M3BigString$Storage#rootSummary` | A | `summary()` returns early for empty ranges |
| `M3LangDexSourceEnvelope#increment` | A | counter is below the receipt's long count |
| `M3LangDexSourceEnvelope#requireCanonicalLong` | A | all call sites pass `nullable=true` |
| `M3LanguageCorpusManifest#safeHttps` | A | an opaque URI has no host, refused earlier |
| `M3RopeTextIndex#findChar` | A | position checked; constructor builds consistent spans |
| `M3WordRows#open` | A | size check and `decode`'s own catch |
| `SharedLexiconCatalog#imageAt` | A | lifecycle locks; lazy-open race only |
| `SharedLexiconCatalog#imageText` | A | only `open()` verifies text, after mapping all shards |
| `SharedLexiconFamilySidecarCatalog#digestBytes` | A | `digest()` admitted 64 lower-case hex digits |
| `SharedLexiconFamilySidecarCatalog#fileHeader` | A | closed family list |
| `SharedLexiconFamilySidecarCatalog#key` | A | closed family list |
| `SharedLexiconFamilySidecarCatalog#readIndex` | A | `rows()` count and positional family check |
| `SharedLexiconFamilySidecarCatalog#safeChild` | A | file names fixed by `FILES` |
| `SharedLexiconFamilySidecarCatalog$SpellAccumulator#build` | A | `add()` sets `frequencies` on creation |

Route B atoms reach 100% once the separate guard-removal PR lands; until then
those two atoms are the only ones below the gate rule.
