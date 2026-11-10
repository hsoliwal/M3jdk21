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
bash ./m3/build.sh                      # with the JaCoCo agent, see below
bash ./m3/release/run_extra_core_contracts.sh
java -jar org.jacoco.cli-0.8.12-nodeps.jar report jacoco.exec \
  --classfiles m3/build/classes --sourcefiles m3/core/src --xml jacoco.xml --csv jacoco.csv
python3 m3/release/derive_changed_atom_inventory.py --report jacoco.xml --root . \
  --classfiles m3/build/classes --out m3/release/coverage/changed-atoms.tsv
```

JaCoCo analyzes every class in `--classfiles` whether or not it ran, so the method
set does not depend on which tests executed; `--classfiles` also refuses a compiled
class missing from the report. Re-run with `--check m3/release/coverage/changed-atoms.tsv`
to prove the committed file is byte-identical. Result: 607 atoms in 82 classes;
341 have no JaCoCo `BRANCH` counter and carry the explicit `branchless_explanation`.
`id` is `<binary class name>#<method><JVM descriptor>`.

The measurement environment is the gate's: JaCoCo 0.8.12 runtime agent via
`JAVA_TOOL_OPTIONS="-javaagent:<agent>=destfile=<exec>,append=true,includes=jdk.m3.text.*"`,
`M3JDK`/`JAVA_HOME` set to a JDK 21, and a C compiler (`CC`, default `cc`) for the
JNI-backed owners built by `run_extra_core_contracts.sh`.

## Atoms that cannot reach 90% without a product change

These methods hold defensive branches that no input can reach through any caller,
or only after 2^63 allocations. They are reported, not covered by test tricks:

| Atom | Line / branch | Unreachable part |
|---|---|---|
| `LocalM3Arena#lambda$identity$0` | 2/2, 1/2 | identity exhausted after `Long.MAX_VALUE` records |
| `M3BigString#<init>(Storage,long,long)` | 6/7, 3/6 | range check; every caller passes a valid range |
| `M3BigString#appendTo` | 10/10, 7/8 | `from < to` is always true |
| `M3BigString#concat` | 49/50, 19/22 | >2^31 pieces; `from >= to`; census drift |
| `M3BigString$Storage#<init>` | 80/82, 35/46 | unaligned arrays, empty chunk, >2^31 nodes, child census |
| `M3BigString$Storage#findChild` | 10/10, 5/6 | coordinate beyond the summary |
| `M3BigString$Storage#rootSummary` | 2/2, 1/2 | empty storage never asks for a root summary |
| `M3LangDexSourceEnvelope#increment` | 1/3, - | row count overflow at `Long.MAX_VALUE` rows |
| `M3LangDexSourceEnvelope#requireCanonicalLong` | 8/8, 5/6 | every call site passes `nullable=true` |
| `M3LanguageCorpusManifest#safeHttps` | 5/5, 7/8 | an opaque URI has no host, refused earlier |
| `M3RopeTextIndex#findChar` | 24/25, 19/22 | corrupt-geometry assertions |
| `M3WordRows#open` | 7/9, 4/4 | `ArithmeticException`/`IndexOutOfBoundsException` already caught in `decode` |
| `SharedLexiconCatalog#imageAt` | 15/15, 5/8 | `closed` under the lifecycle read lock; lost lazy-open race |
| `SharedLexiconCatalog#imageText` | 3/3, 1/2 | images are opened before sidecar validation |
| `SharedLexiconCatalog#sha256(Path)` | 8/8, 3/4 | a file `InputStream` never reads 0 bytes into a non-empty buffer |
| `SharedLexiconFamilySidecarCatalog#digestBytes` | 1/2, - | `digest()` already admitted 64 lower-case hex digits |
| `SharedLexiconFamilySidecarCatalog#fileHeader` | 6/7, 5/6 | `default` of a closed family list |
| `SharedLexiconFamilySidecarCatalog#key` | 8/8, 5/6 | `default` of a closed family list |
| `SharedLexiconFamilySidecarCatalog#readIndex` | 9/10, 10/12 | row count and duplicates already fixed by `rows()` and positions |
| `SharedLexiconFamilySidecarCatalog#safeChild` | 3/4, 8/14 | file names come from the fixed `FILES` map |
| `SharedLexiconFamilySidecarCatalog$SpellAccumulator#build` | 3/3, 1/2 | `add()` always sets `frequencies` first |

The gate can pass only after these branches are removed or rewritten in product
code. That is a release-owner decision; this inventory and its tests change no product code.
