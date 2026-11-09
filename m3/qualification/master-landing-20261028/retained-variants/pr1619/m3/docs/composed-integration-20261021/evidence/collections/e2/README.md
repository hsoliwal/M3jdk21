<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# E2 linked SDK successor qualification

The freshly rebuilt source SDK was linked through the unchanged repository JMOD packager. The resulting **E2** image passes installed ownership/packaging checks and the focused String, public-core, and native probes recorded here. The complete **491-test linked collection suite remains E1 evidence**; E2 reuses it with verified unchanged collection source, JAR, JMOD, and native inputs.

## Source checkpoint

`SOURCE_EPOCH.json` identifies E2 as the published E1 implementation commit `f35342202149b98465bb4e9e6be807eb0cf3f762` plus the exact receiving-stage postimages. It records all **55 composed postimages**, their receiving receipt hashes, and before/after seals for the two changed runtime owners:

| Runtime owner | Admitted successor |
| --- | --- |
| `src/java.base/share/classes/java/lang/invoke/StringConcatFactory.java` | Nullable statically typed String arguments preserve ordinary concat conversion semantics |
| `src/java.base/share/classes/java/lang/M3StringPool.java` | Tuple canonicalization compares exact child owner/range coordinates without unfolding a shared large logical payload |

The source SDK differs from E1 at exactly **`lib/modules` and `jmods/java.base.jmod`** among the 76 sealed source-SDK inputs. The preserved E1 source SDK matches every original E1 seal. E2's seven receiving-stage file postimages also include the associated tests, budget fixture, and concat guard/manifest changes. The containing integration's separate `SOURCE_RECEIPT` supplies the final published E2 commit; this subpacket identifies the execution checkpoint by exact hashes and does not assume that publication has already occurred.

All **567 collection source/fixture seals**, generated native headers, product C inputs, the published collection JAR, and the existing packager scripts match E1. The collection JMOD is byte-identical to E1:

`94aefb435be9818d2155bef4b0177f06978cdf2ecbcf416b8b213895f2261f16`

The fresh E2 linked module store is:

`5d32a676e1adc57c0cfd405111b1a35f35d69701f1c3d626d88a8c02ffdb5c3c`

## Freshly executed results

| Check | E2 result |
| --- | --- |
| Existing packager and module validation | Pass; 29 selected modules |
| Collection inventory | All 512 class files present; 511 ordinary JMOD class bodies equal the JAR |
| Product JNI | Ten embedded release libraries match exact strip-debug references; 21 generated exports |
| Legal and metadata | Four legal notices and nine module metadata resources preserved |
| Installed ownership | Seven core/module classes resolve through their expected `jrt:` modules; String, M3String, and CollectionViews remain bootstrap-owned |
| Concat reference DAG | 23 checks, height 10, in each of compact and UTF-16 modes |
| Canonical tuple DAG | 59 checks, height 12, in each mode |
| Logical length/UTF-8 overflow | 111 checks in each mode |
| Retained public-core fixtures | Seven runs pass: bootstrap 77; current owner 14,694; identity 46 and 540; copy-on-write 120; refinement 1,828 plus two size checks |
| Installed native addenda | Catalogue 8,438 checks; radix 417,038 checks |
| Targeted class origins | All ten production collection classes observed in these native runs originate in `jrt:/com.m3.collections` |

There is no `java.base` patch in these runs. External collection **test classes only** are patched into the named module, with zero production-class overlap. The String fixtures run against the installed image with M3 storage enabled, checked JNI, a 512 MiB heap limit, and both CompactStrings modes. The public-core checks use their retained E1 commands against the new image.

`RUNS.json` contains the 23 actual commands, exit codes, durations, bounded output and log hashes. `SOURCE_SDK_INPUTS.json` binds all 76 source-SDK inputs and confirms JNI header equivalence with the reused native compilation inputs. `OWNERSHIP.json` records inventory, native/legal hashes and class origins. Source, input, image, and historical E1 receipt hashes were checked again after execution.

## Reproduction and evidence boundary

Use the staging instructions in the parent collections README with a **fresh** `LINKED_WORK_E2` and `PACK_OUT_E2`. Reuse the E1 collection module/native inputs only after checking the source and binary hashes in `SOURCE_EPOCH.json`. Stage the three current fixtures under `inputs/string-sources`: `M3StringConcatReferenceDagTest.java`, `M3StringCanonicalDagTest.java`, and `M3StringConcatLengthOverflowTest.java`. The retained public-core fixtures and external test classes have the same layout as E1.

The normalized command variables are `REPO`, `SOURCE_SDK_E2`, `SOURCE_SDK_E1`, `LINKED_WORK_E2`, `PACK_OUT_E2`, and `IMAGE_E2`. Expand those variables in the recorded argument arrays and execute each command without shell evaluation, preserving the argument boundaries. Use a fresh output path: the existing packager and JDK `jlink` refuse an existing image destination. The image's Java tools compile and execute the focused fixtures.

The first incremental source-image build encountered that existing-destination refusal after compilation. The parent retained its log, preserved the E1 source image, and reran the standard build successfully into the fresh destination. Both build-log hashes are retained in `SOURCE_SDK_INPUTS.json`. This was an output-directory refusal, not a reason to weaken compiler or behavioral gates.

The parent E1 public packet remains byte-for-byte unchanged. `PACKET_MANIFEST.json` in this directory seals only this additive subpacket, and `RAW_EVIDENCE_SHA256.tsv` binds its original raw receipts/logs. No binaries, settings, raw environment, or Surefire XML are distributed here. The containing core packet owns the wider jtreg results; the six String mode runs here are a focused installed-image gate. No receiving phase, full provider/platform claim, JCK claim, or new resource-performance claim follows from this E2 relink.
