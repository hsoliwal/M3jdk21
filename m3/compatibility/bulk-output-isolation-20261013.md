# M3JDK21 bulk-result primitive-array isolation

Status: **target-owned candidate; not promoted by documentation or PR merge**.

## Reproduced defect

`BulkExecution(Map<String,Object>, String, String)` used `Map.copyOf`. That copies map
structure, **not** `boolean[]`, `byte[]`, `short[]`, `char[]`, `int[]`, `long[]`,
`float[]` or `double[]` values. Receipt-bearing output therefore remained alterable
through the provider's original array or through a caller's returned output map.

Exact preimage Git blob: `fae2edd4a5eb6687c0a6968de20fb122f6d2ff51`.
Preimage SHA-256: `8ec68b77969ffd29145120061d649e69fd9ebc69b5732cf1eb4e9339a3a48acd`.

## Bounded fix

Constructor snapshots all eight **one-dimensional primitive-array types**. The accessor
returns new snapshots on every call. Non-array objects preserve their former reference
contract and are explicitly **not** claimed immutable. The receipt remains provider-supplied
evidence; no arbitrary SHA-256 string becomes an authenticated execution proof solely
through this output fix.

API signatures, provider registration, task geometry, String/Unicode, JNI/HotSpot and
TornadoVM runtime classes remain unchanged. The optional accelerator still depends on
exact CPU/backend verification by its original owner.

## Canonical recipe

- Synexia canonical PR: `hsoliwal/com.synexia#10859`
- Canonical class: `com.synexia.rewrite.M3Jdk21BulkOutputIsolationRecipe`
- M3JDK receiver: `com.m3.rewrite.backport.M3BulkOutputIsolationRecipe`
- Owner: `M3Jdk21HashPinnedSnapshotRecipe` (existing; not duplicated)
- Scope: one existing Java target + one ABSENT jtreg target, two exact source seals
- Intake: target-owned JDK code under existing GPL/OpenJDK headers

## Executed local evidence

OpenJDK 21.0.12.1, isolated `--patch-module java.base`:

- strict compilation with `-proc:none -Xlint:all,-module -Werror`: PASS
- exact unmodified source plus adversarial new test: **expected FAIL on check 1**
- fixed source: **25 checks PASS** in normal JVM
- same 25 checks PASS under `java -Xint`
- every target postimage matches its canonical resource Git blob

This does not establish full modified-JDK image/jtreg, end-to-end resource costs, the
behavior of arbitrary reference-valued objects, or a physical GPU speedup.

## Required hosted proof

`.github/workflows/m3-bulk-output-isolation.yml` runs isolated Java21 patch,
target receiver/JUnit/OpenRewrite fixed-point and full JDK image + focused jtreg as
separate failure gates. Keep any failed or unexecuted gate open; do not infer PASS
from queue or merge state.
