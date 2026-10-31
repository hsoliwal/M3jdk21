# Independent review of executed M3jdk21 E3 receiving evidence

## Result

**The bounded E3 metadata installation proof passes, while capability completion remains blocked.** The actual execution contains **11 passing Java tests**, **six passing Python tests**, and ten recorded command steps with their expected exit codes. The installer applies all four existing-file updates, reaches a zero-write fixed point, rolls back four files, reapplies four files, and finishes with zero writes on check. Normal mapping validation succeeds; completion returns its expected exit code **2** and reports incomplete inventory and remaining obligations.

This review performed only file reads, parsing, identity checks, and complete byte comparisons. It did not run or rerun project tests, builds, recipes, the installer, the normal validator, the reference checker, or source/native code. Review artifacts were written only in this directory.

The exact reviewed execution is under `work/m3jdk21-current/successor-e03/verification/`. It binds:

- Published source commit: **`d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906`**.
- Existing receiving preimage commit: **`df06cdee5a8526f573b3ad89622824d0b49e2f25`**.
- Canonical installer plan seal: **`af65912789780e214f09644b38f542db5f7670b83ee1fada228bce08f8f9b30e`**.
- Installer recipe ID: **`jcc-source-final-handoff-20261005/1`**.
- Named OpenRewrite recipe: **`com.m3.rewrite.backport.JccSourceFinalHandoff`**.

The plan seal was independently recomputed from the canonical plan body excluding its seal field. Every output requires an existing before-image; none assumes an absent file.

## Actual command results

| Recorded step | Exit | Observed state | Writes |
|---|---:|---|---:|
| Check E2 preimages | 0 | Before | 0 |
| Maven `clean verify` for the actual retained text recipe | 0 | 11 Java tests pass | — |
| Actual sealed-installer packet contract | 0 | Six Python tests pass | — |
| Apply E3 | 0 | After | 4 |
| Immediate apply fixed point | 0 | After | 0 |
| Rollback | 0 | Before | 4 |
| Reapply | 0 | After | 4 |
| Final check | 0 | After | 0 |
| Whole-map validate | 0 | `MIGRATION_MANIFEST_VALID completion=INCOMPLETE` | — |
| Require completion | **2, expected** | `MIGRATION_MANIFEST_VALID completion=INCOMPLETE` | — |

Each individual command JSON matches the corresponding entry in the aggregate execution receipt, including command arguments, working directory, ordered timestamps, expected/actual exits, and output identities. The stdout and stderr files match their recorded byte counts, SHA-256 values, Git blob identities, and modes.

The completion refusal is an expected successful negative check. It is not successful completion of the migration. The recorded stderr begins with incomplete full-source inventory, incomplete semantic dependency/ownership closure, an absent pinned exhaustive inventory receipt, and uninspected domains, followed by outstanding record obligations.

## Test evidence and counts

The copied `JccSourceFinalHandoffTest.xml` is byte-identical to the actual Surefire build report. It contains **11 testcases, zero failures, zero errors, and zero skips**, and the executed method names exactly match the 11 methods in the frozen Java test source. Maven compiler input lists show exactly **one retained production owner**, `M3Jdk21HashPinnedTextSnapshotRecipe`, and **one test source**, `JccSourceFinalHandoffTest`. The actual test JVM is Java 21.0.2. All **11 test-loaded resource copies** exactly match the source resource bytes. The examined runtime classpath contains no source-provider output.

The verifier POM configures Java release 21, all lint checks, and failure on warnings. Maven ran the focused verifier using `clean verify` with offline resolution and explicit empty user settings. Its stdout reports build success. Stderr retains the SLF4J notice that no `StaticLoggerBinder` was present and a no-operation logger was selected; this did not produce a test failure. This focused verifier is not the full owning-module gate.

The Java tests exercise actual named recipe activation and serialization, exact four-file result maps, before-image identity, unchanged fresh second application, preserved record IDs/order and global fields, old source/target lineage, unchanged historical receiver/Descriptor evidence, blocked acceptance, and refusal of each drifted, missing, duplicate, or scan-to-visit-mutated target.

The Python log names all **six actual test methods** with `ok` and ends with `Ran 6 tests` and `OK`. These tests load the retained `m3/migration/recipe.py` against the actual sealed E3 packet. Their negative loops cover:

| Negative workspace condition | Conditions | Operations per condition | Refusal calls |
|---|---:|---:|---:|
| Each missing or changed preimage | 8 | 3 | 24 |
| Each missing or changed guard | 14 | 3 | 42 |
| Every proper mixed before/after subset | 14 | 3 | 42 |
| Each foreign edited postimage | 4 | 3 | 12 |
| Each missing postimage | 4 | 3 | 12 |
| **Total** | **44** | **Check / apply / rollback** | **132** |

These are **132 refusal operation calls inside six tests**, not 132 additional tests. The count follows the completed, unconditional loops in the frozen test source. Each refused operation compares complete workspace snapshots containing directory names, file names, exact file bytes, and modes. The successful path tests compare complete before/after bytes, preserve four deliberately different target modes, preserve seven guards and unrelated binary bytes, and verify that no lock or journal remains. Exact rollback assertions are inside this executed Python suite. The direct operational command receipts separately report rollback state/write counts; no intermediate operational filesystem snapshot is invented from those receipts.

## Frozen inputs and the four resulting files

All **229 frozen inputs**—166 local fixed inputs and 63 external source/proof inputs—still match their pre-execution byte counts, SHA-256 values, Git blob identities, and file modes. The four operational paths are excluded from that fixed-input count because their updates are intentional. Their before resources are independently byte-identical to the retained actual E2 operational files; their current bytes are independently identical to the complete sealed E3 after resources. All four original operational file modes are preserved.

| Operational path | Final SHA-256 |
|---|---|
| `m3/docs/jcc-source-handoff.md` | `59ed76969f86486e7adb377409e284f69577fe02754006fdfe5b3938a16760a0` |
| `m3/docs/name-mapping.json` | `e4ae516fea9822a4bfe26669e9749e167f52bb30049d520339ee42f3779bc0b3` |
| `m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv` | `59c4c970ac75a56aa5d5a3e2f3e88f5caa831eb06be788266c725202b8c64b12` |
| `m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json` | `049c7c1987c05a15f4152fcb8f261a65245d72a25688d5a530a4afa5405cd3ef` |

The four manifest rows exactly match the plan's paths, before identities, after identities, and resource names. All seven guards retain their pinned content. The current map has 46 records; the two requested JCC capability rows remain `blocked` with empty `tests` arrays. All four acceptance flags remain explicitly false.

## Normal validation and the separate reference audit

Normal mapping validation conditionally checks target/recipe content hashes only for implemented records and checks executed evidence for tested records. The requested JCC rows remain blocked, so the ordinary validator's success **does not certify their target, recipe, or evidence hashes**.

The separate explicit audit is `successor-e03/reference-audit/REFERENCE_AUDIT.json`, SHA-256 **`a4af3c44bc1afa6f83881c17ced55f418940dbd792bfd687cbb3a717c44887cd`**. It reports passing bounded reference and lineage consistency. This reviewer independently rechecked actual local file identities for all:

- **17 source reference occurrences**.
- **46 source-proof reference occurrences**.
- **45 destination reference occurrences across 34 distinct paths**.

It also independently compared complete bytes for **all 56 preserved E2 payload files** against their retained E2 counterparts. The source reference audit uses existing exact publication/readback evidence and does not represent a new remote fetch by this reviewer.

The explicit audit separately accounts for the 46 ordered records, 44 unchanged other records, 20 unchanged gates, preserved full prior source/target objects, 343 root objects, and 201 paired Maven declarations. Those root/declaration counts describe the audited inventory representation; they do not establish complete repository semantic closure or source-export readiness.

The audit preserves its initial checker and a written failure note: that checker expected pipe separators within two declaration fields, while the actual TSV uses semicolons. Its final code differs only at those two comparisons and at an output-directory guard that preserves the earlier artifacts while refusing to overwrite the final audit report. This was an inspection correction with no candidate edit or project test/installer rerun. The retained note is not described as a raw command traceback. `INSPECTION_METHOD_NOTES.json` records the corresponding review-program comparison correction.

## Qualification limits

This E3 execution qualifies the bounded metadata recipe, installer behavior, exact current outputs, and reference consistency. It provides **no new receiver behavior execution, JNI execution, complete owning-module coverage result, source-export admission, or JDK product/image acceptance**. The four original receiver behavior tests remain historical E2 evidence.

The source qualification remains the separately recorded result: 91 core passes, eight parent passes, and 66 passes plus one error in the 67-test upstream gate. Its 13 later stages, including JNI, remain unexecuted. Receiving metadata success does not transfer or promote that source qualification.

No fresh remote publication or CI result is claimed by this review. A bounded scan of the 34 current verification logs/JSON/XML files found no matches for the enumerated credential-shaped patterns. This is not a universal secret audit. Every input read by this inspection remained stable during the inspection.

`INSPECTION.json` contains the independently checked results, `INPUT_SEALS.json` identifies all inspected inputs, `inspect_receiving.py` is the read-only inspection program, and `FILE_SEALS.json` binds this review package.
