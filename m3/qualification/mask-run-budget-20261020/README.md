# M3JDK21 Java.base mask-run budget — 2026-10-20

**Product owner:** `src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java`.
This changes an existing **M3JDK21 JDK implementation atom** used by its `java.util.ArrayList`/Vector integration. It is not a new external M3-on-JDK layer. The existing `Object[]` remains authoritative, deletion bits are relative to the source range, and `System.arraycopy` retains the VM write-barrier path.

## Behavioral refinement
The original prepass counted *all* deletion-run starts before determining whether `runCount > max(4, length >>> 7)`. Once this monotone count is over budget, additional words cannot change the algorithm decision, so the new `fragmented(...)` returns immediately. The short-range scalar path, over-budget scalar path, run-copy path, output identities, tail handling, and exceptions are otherwise retained. No new array, per-element wrapper, native pointer, public API or flag is introduced. This saves mask-word reads on heavily fragmented masks; it does not establish a universal speedup.

## Recipe authority
- Existing qualified Maven/OpenRewrite engine: `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe`.
- Recipe: `com.synexia.rewrite.M3MaskRunBudget20261020` (in `META-INF/rewrite/m3-mask-run-budget-20261020.yml`).
- Crate: `hash-pinned-java/m3-mask-run-budget-20261020`: entire before/post source SHA-256 and absent-test guard. Both are Java LST targets, not untyped string patching.
- Do **not** edit the installed product Java source to iterate: update the source template, digest, tests and recipe, rerun, then generate the source. Source drift is a refusal, not permission to weaken the guard.
- One source file/one behavioral atom per application. Apply/review the serial output and confirm fixed-point replay before promotion.

## Reproduce the Java 21 proof
```sh
bash m3/qualification/mask-run-budget-20261020/verify.sh
```
This uses `javac` / `java --patch-module java.base` against the exact installed M3JDK21 class. It checks SHA-256 and runs 1,194 seeded identity/range differentials, including 64-bit word seams and invalid inputs. No Maven dependencies are needed for this narrow independent product test.

For actual Maven/OpenRewrite replay, first install the repository's pinned Synexia recipe dependencies, then activate `com.synexia.rewrite.M3MaskRunBudget20261020` in the existing Maven control reactor. Run the recipe against a candidate tree with the **preimage** present; run it again on the postimage for a fixed point; refuse unknown drift. The parser and recipe execution are not claimed as run by the local Java-only check.

## Android and JNI boundary
DEX indexing and ART's mobile memory discipline are **design inputs to the M3JDK21 product**, not an Android-facing facade. This atom reduces prepass work on the same mask owner; the existing primitive JNI compactor is not changed here, since passing Java object references to JNI would violate current ownership/GC boundaries. The separate Android ARM64 ART/JNI and native-memory/power proof remains open.

## Evidence and remaining gates
`verify.sh` / Java 21 local run: 1,194 scenarios passed, compiler `-Xlint:all -Werror`; catalogue documents baseline, donor evaluation and selection. No matched full JDK build, jtreg/JCK matrix, Maven recipe execution on GitHub, JNI binary rebuild or device measurement is claimed. Keep release gates unchanged.
