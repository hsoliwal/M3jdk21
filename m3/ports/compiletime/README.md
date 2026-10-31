# M3JDK compile-time String precompute port

Status: isolated Apache-2.0 port. Not on the bootstrap path, no `src/**` change, no public JDK
API. Design: [`m3/docs/m3-compiletime-precompute.md`](../../docs/m3-compiletime-precompute.md).

| Class | Role |
|---|---|
| `M3CompileTimeStringInventory` | one-JVM ASM inventory of compile-time-known strings over `jrt:/` and a jar list |
| `M3ClassScan`, `M3CallSiteCatalog`, `M3SiteRecorder` | per-class scan; `SourceInterpreter` proves call-site operands constant |
| `M3RegexRequirement`, `M3PatternNodes` | build-time requirement walk over the stock `Pattern` node tree |
| `M3ContentKey` | spelling-free content key (UTF-16 + flags) |
| `M3ConstantRegexImageWriter`, `M3ConstantRegexImage` | `M3CTRX01` int-lane image; mmap reader with refusal |
| `M3ConstantRegexGate` | fail-open runtime consumer (`m3.precompute.constantRegex.image`) |
| `M3ConstantRegexHarvester` | jar set → image + key table + receipt, with measured per-key cost |
| `M3ConstantRegexDifferential` | stock vs gated proof and measurement; oracle-harness soundness |
| `M3FormatPlan` | int-image plan of a constant `Formatter` string (inventory classification) |

## Build and run (one JVM at a time)

```sh
mvn -B -ntp -f m3/ports/precompute/pom.xml install -DskipTests
mvn -B -ntp -f m3/ports/compiletime/pom.xml test
mvn -B -ntp -q -f m3/ports/compiletime/pom.xml dependency:build-classpath -Dmdep.outputFile=cp.txt
CP="m3/ports/compiletime/target/classes:$(cat cp.txt)"
OPENS="--add-opens java.base/java.util.regex=ALL-UNNAMED"
java -Xmx2g $OPENS -cp "$CP" com.m3.compiletime.M3CompileTimeStringInventory --out inv --jdk --jars jars.lst
java -Xmx2g $OPENS -cp "$CP" com.m3.compiletime.M3ConstantRegexHarvester --out harvest --jdk --jars jars.lst
java -Xmx2g $OPENS -cp "$CP" com.m3.compiletime.M3ConstantRegexDifferential --harvest harvest
```

`jars.lst` holds one jar path per line. Outputs of the 2026-10-13 run are in `inventory/` and
`evidence/`; the binary image is reproducible from the harvester and is not committed (its
SHA-256 is in `evidence/HARVEST_RECEIPT.tsv`).

## Custody

```sh
python3 m3/ports/compiletime/seal_crate.py                    # regenerate the recipe crate
python3 m3/ports/compiletime/verify_compiletime_precompute.py # refuse on any drift
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml -Dtest=M3CompileTimeRegexPrecomputeRecipeTest test
```

## Detailed source references

- [Inventory and report contracts](../../../doc/m3-guide/compiletime-inventory-reference.md)
- [Item-residency scanner and sidecar contracts](../../../doc/m3-guide/compiletime-residency-reference.md)

These references describe source at their stated pin. Their source/test review does not rerun the historical receipts or establish build/runtime acceptance.

Also see the [regex analysis, image and gate reference](../../../doc/m3-guide/compiletime-regex-reference.md),
[harvest and differential receipt reference](../../../doc/m3-guide/compiletime-harvest-reference.md)
and [complete nineteen-source package map](../../../doc/m3-guide/compiletime-source-coverage.md).
The map includes internal helpers while keeping the separate residency
annotations, dependency projects and runtime qualification outside its count.
