# JEP 485 Stream Gatherers — Java 21 source-sealed implementation packet

Status: candidate implementation; no compatibility or product acceptance claim yet.

## Baseline and authority

Repository: hsoliwal/M3jdk21.
Exact base: 8131f535300c005afd27443d0281ede11198522f.
Catalogue row: JEP 485, final Stream Gatherers, domain library, disposition candidate.
Existing reviewed donor crate: `jdk24-jep485-stream-gatherers`, 15 Java source/test targets.

This packet intentionally changes the exported `java.util.stream` API by adding the final Gatherer API and `Stream.gather`. It therefore requires explicit LIBRARY_API contract-change authority. It is not a file-local behavior-preserving refactor.

## Existing machinery

Use `M3Jdk21HashPinnedSnapshotRecipe` as the only source-materialization owner. Do not hand-edit the seven production Stream files or eight tests. The crate already pins four JDK21 preimages and eleven ABSENT targets by SHA-256 and reviewed donor postimages.

The recipe must refuse:
- any drifted non-ABSENT JDK21 preimage;
- an occupied ABSENT target;
- missing required owners;
- invalid/non-Java target objects;
- donor template hash drift;
- second-pass changes.

## Compatibility hypothesis to prove, not assume

JEP 485 is additive at the Java API level and uses Java syntax representable by the Java 21 parser, but source-level additivity does not establish runtime compatibility.

Mandatory proof includes:
1. exact preimage admission on current master;
2. OpenRewrite parse/round-trip and fixed point;
3. strict compilation of the modified `java.base` source closure against the matched JDK tree;
4. JEP 485 stream tests, including short-circuiting, parallel behavior, concurrent mapping, fold, scan and fixed/sliding windows;
5. existing stream regression tests;
6. API-diff review showing only the explicitly admitted Gatherer additions;
7. serialization/JMM/parallel-pipeline and exception-order review where observable;
8. full matched OpenJDK image and focused jtreg before promotion.

No preview JEP 461/473 contract is imported as a separate API. The final JDK24 JEP 485 source is the donor line.

## Verification order

diff -> lint/static checks -> compile -> JUnit/recipe tests -> matched-image jtreg/runtime.

## Output

Maintain STATUS.tsv, FINAL_REPORT.md, RUN_CONTEXT.tsv, PROVENANCE.tsv, VERIFY_CONTRACT.tsv and OUTPUT_CONTRACT.tsv. Whole-JDK/JEP completion remains false until the wider programme denominator closes.
