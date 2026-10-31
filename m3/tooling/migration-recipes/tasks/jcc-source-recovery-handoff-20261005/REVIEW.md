# JCC source recovery handoff — E4 execution and delivery scope

The additive E4 receiving recipe passed its focused tests, exact four-file installation, fixed point, rollback/replay and explicit reference audit. **Capability completion remains blocked.** This packet records the published current source recovery, including its independently executed Linux JNI lane, while preserving the unresolved upstream source gate and all existing destination acceptance requirements.

The source epoch is [`0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8`](https://github.com/hsoliwal/com.synexia/commit/0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8), root `0361e4a07b77a01df170523033fef01c4052ddc5`, in [source PR9362](https://github.com/hsoliwal/com.synexia/pull/9362). Its sole parent and current execution baseline are `be92c62ece9023b5c33676716a1076d00e26120a`; earlier `0b8dc` and `d1cf` results remain separate history. The receiving preimage is [`0994ecd65e86600f417f4d8702838d8c2663af61`](https://github.com/hsoliwal/M3jdk21/commit/0994ecd65e86600f417f4d8702838d8c2663af61), root `7e8f39ac999174f8d5b6028d638a68d8c317b070`. Publication is intended as an additive commit to [receiving PR147](https://github.com/hsoliwal/M3jdk21/pull/147), subject to a fresh branch/state check. This local report does not assert that the new receiving commit has already been published.

## Four operational outputs

The canonical hash-pinned text recipe and sealed installer materialized exactly these four existing files. All other publication payload paths are additive task, recipe, test or evidence files.

| Operational file | Materialized SHA-256 |
|---|---|
| `m3/docs/jcc-source-handoff.md` | `d5e18258027f457a1fcdc45ff30b4e2e7a0723825ea95b651933af23f27fd28d` |
| `m3/docs/name-mapping.json` | `eb42442866c73c38027b481a84c0473bea422e4628d0794fa31fc61d9f1a818e` |
| `m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv` | `1c4eedbf3f155bac469693da312c577d26999e7776ca1f85870f6cce125f1e35` |
| `m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json` | `f8b27e3582eb494af2c8dd47225976f974be6edc2f6c26313975064d48739497` |

The plan content seal is `0aa1f4a3e080c36d233066f0a6e1dc8e1047e6f0592dba60829b8cca3177df64`. The plan's content seal is distinct from the file hash of serialized `plan.json`.

The transition preserves all 46 ordered mapping IDs, the 44 other complete records, all 20 gates and global data, the Descriptor reconciliation, 21 whole historical source objects and six receiver target objects with their histories. The two JCC records remain blocked, their capability `tests` lists remain empty, and export and destination gate flags remain false. The existing compiler/atom/recipe owners remain authoritative; no second registry or compiler is introduced.

The frozen handoff document describes receiving work as planned at its pre-execution boundary. Its exact bytes were then tested and materialized. This additive report and the actual receipts below record the subsequent execution without changing those tested bytes.

## Actual focused receiving execution

The ten-stage runner executed on 2026-10-05 from 17:40:48 to 17:40:56 UTC. The [actual receipt](verification/receipt.json), per-stage command records and untruncated stdout/stderr are preserved alongside the [invocation capture](execution-invocation/EXECUTION.json). The receipt SHA-256 is `1f4ac9825d975520a04b7dc5ac1b282d96be32601c7abe2b0cfb676b0ff2df09`.

| Stage | Observed outcome |
|---|---|
| Exact E3 preimage check | Four files in before state; zero writes; exit 0 |
| Focused Maven recipe verification | 12 Java tests passed; no failures, errors or skips; exit 0 |
| Python packet contract | Six methods passed; exit 0 |
| Apply | Four writes; after state |
| Fixed point | Zero writes; after state |
| Rollback | Four writes; exact before state |
| Replay | Four writes; exact after state |
| Final check | Zero writes; after state |
| Ordinary whole-map validation | `MIGRATION_MANIFEST_VALID completion=INCOMPLETE`; exit 0 |
| Completion command | Expected exit 2; incomplete/blocked obligations retained |

The six Python tests exercise 44 negative workspace conditions across check/apply/rollback: 132 refusal calls derived from the executed loops. They are not 132 test methods or individually persisted event traces. The [actual Surefire XML](verification/JccSourceRecoveryHandoffTest.xml) records the twelve distinct Java cases.

All **1,008 pre-run input identities** remained exact: 391 fixed receiving paths and 617 external source/proof paths. This includes all 527 current baseline context files and the two separately admitted malformed-UTF8 fixture bodies. Declared bytes, SHA-256, Git blob identity, regular-file/path requirements and modes were admitted before creating the execution output, then rechecked after execution. All four operational outputs equal their admitted afterimages and retain their modes.

The [independent execution review](independent-execution-review/REVIEW.md) rechecked the actual logs, XML, ten expected exits, input identities and four outputs without rerunning the gates. Its disposition is `FOCUSED_RECEIVING_EXECUTION_PASSED_COMPLETION_BLOCKED`.

This was the focused verification project using the retained recipe owner and existing test infrastructure. It does not claim the owning migration module's 99% coverage gate, full module verification, whole JDK acceptance, or a new pass of the original four-test receiving behavior fixture. The unchanged receiver fixture remains historical evidence at `df06cdee5a8526f573b3ad89622824d0b49e2f25`.

## Explicit reference and root accounting audit

The [explicit reference audit](reference-audit/REFERENCE_AUDIT.json) passed at 17:42:55 UTC, SHA-256 `5867bffd36e65f2fd755921d0dbf9981a006bc0e38134b2d0e4e6830030900ea`. The checker is a read-only body, identity, Git-tree path and lineage audit. Its [independent static admission](checker-static-review/REVIEW.md) is separate from the actual run and from behavioral qualification. The [independent actual reference review](final-gate-review/REVIEW.md) reconciles the executed output with the bound inventories, installed resources and separate receiving-execution authority; it admits this bounded audit outcome for packaging. Final manifest and remote publication remain separate.

The audit verifies 19 active source bodies and 55 proof bodies against the actual source commit/root, 182 destination reference occurrences across 170 distinct paths, 134 exact source-recovery copies, ten current-context receipt copies, the 1,008 frozen inputs and all four materialized outputs. It also preserves 89 nonoperational E3 payloads and 56 nonoperational E2 payloads. The ordinary validator's blocked-row hash path is intentionally limited; this explicit audit supplies the separate active-reference check and reports no unverified active hash reference in the two requested rows.

Current source context is **527 paths**, distinct from the **19 designated mapping bindings**. Main compilation membership is 446: the prior 443 owners plus `M3MasteryClassLoader`, `M3MasteryContractSurface` and `PSource`. Parent context is 109 paths: 88 main owners, 19 RE2 sources and two test sources. The current native header is bound separately within that context. None of these counts means complete repository semantic closure.

Fresh source-root and exact root-POM accounting gives 343 root entries: 255 trees, 87 blobs and one Gitlink. The exact POM has 201 distinct module declarations: 190 direct and 11 nested. Eight root trees contain those nested declarations; four are also among the 190 direct modules and four are additional containers, yielding 194 distinct root trees referenced. Every root entry is accounted for, with no unresolved declaration root component. Directories are not inferred to be Maven modules, and this is not a whole-file denominator or full-build claim. See [the root accounting](source-recovery/ROOT_ACCOUNTING.json) and [the exact restored POM](source-recovery/source-results/ROOT_POM.xml).

## Source outcomes retained without acceptance transfer

The [source aggregate](source-recovery/source-results/RECOVERY_RESULT.json), [source report](source-recovery/source-results/RECOVERY_REPORT.md), [reviewed execution input](source-recovery/source-results/E4_EXECUTION_INPUT.json) and [final mechanical replay review](source-recovery/source-results/FINAL_T03_REVIEW.md) are exact copies of the actual source records. The active source binding includes four current production repairs and their retained surrounding owners; no historical pass is transferred merely because a filename is unchanged.

| Source lane | Actual recorded outcome |
|---|---|
| Strict current compilation | 446-owner candidate compiled |
| Public API comparison | 24 outward types / 31 classfiles / 277 members retained; separate package-private helper has zero outward types |
| Parser candidate | 43 tests passed; baseline's 13 corresponding failures remain separate observations |
| Current units | 119 passed: growth 1, Mastery 15, core 91, parent 12 |
| Observation controls | Eight JUnit methods and a separate 19-case campaign passed |
| Selected upstream suite | **67 executed: 66 passed, one error**, missing `AtomicPrimitiveEqualsUsesGet`; zero failures/skips |
| Independent Linux JNI lane | 19 passed: seven donor and twelve parent tests; RXL 72 and RXM compiler six output rows matched byte-for-byte |
| Final mechanical replay | 70 stages; six writes; five independent crate disk replays made zero writes; 1,446 classfiles and 34 resources matched |

The Linux JNI lane used the recorded GCC/JDK, sanitizer and `-Xcheck:jni` conditions. It does not qualify other platforms, turn API call counts into native entry counts or move regex transitions out of Java. The final mechanical replay did not rerun the earlier behavior/native lanes.

**Overall source qualification remains BLOCKED.** The upstream error, source-canon verification/history/pattern admission, full reactor and export requirements remain outstanding. Receiving completion, full module coverage and JDK promotion remain independently blocked. CI observations and subsequent branch/mainline changes are separate evidence; this packet makes no green-CI or live-mainline equivalence claim.

## Publication custody and carried evidence

The [actual source publication receipt](source-recovery/PUBLICATION_CUSTODY.json), SHA-256 `95f325ce57dadab60031f41514d9a9c5fa3ef6eb29a41e3c9a8c81e2dee996b8`, records `PUBLISHED_CUSTODY_VERIFIED`. It distinguishes 21 successful direct body reads, including all 19 designated owners and two NUL fixtures, from the six mandatory direct bodies: four production bodies and two NUL fixtures.

Exactly two allowlisted malformed-UTF8 fixtures could not be recovered byte-for-byte through the connector. Their captured exact base64 create arguments, returned Git identities and final tree size/mode/identity establish the explicitly scoped custody method. Both retain `raw_body_readback=false` and `remote_sha256_observed=false`; failed read attempts remain in evidence. Authentic GitHub object identities and ordinary Git content addressing are assumptions of this method. No remote raw byte array or SHA-256 is claimed for those two exceptions.

The [custody evidence index](source-recovery/custody-v3/CUSTODY_EVIDENCE_INDEX.json) makes the carry boundary explicit. It covers 121 exact original files plus the index/prose; eight source-result/POM files and three reserved records bring the source-recovery copy count to 134. The 132 omitted originals are individually hash/pointer-bound: 42 raw tree-call envelopes, one redundant readback and 89 upload batches. Normalized final tree bodies are carried, but complete raw-envelope reproduction and a complete source checkout are not claimed. Workspace-only provenance paths are retained verbatim and mapped to available receiving copies where carried.

## Environment and post-execution packaging

The [toolchain restoration receipt](environment/toolchain-restoration/RESTORATION.json) records recovery from an existing exact local cache after older workspace bodies were unavailable. The cause of that absence was not established. Seven historical tool anchors and 52 additional Maven boot/library JAR identities matched; 516 resulting file/link identities were compared, including 205 existing license symlinks. The original configured paths and versions were retained. This is current byte/copy and retained-anchor evidence, not a historical full-JDK loaded-byte attestation.

Evidence and review files copied after the execution are **additive packaging**, outside the 1,008-path run freeze. Their exact copies are checked by the final publication manifest and review; they are not retroactively claimed as test inputs. Original review snapshots are carried for the execution and checker reviews. Some earlier review directories carry their direct reports/seals only; references to their original workspace `inputs/` snapshots remain provenance and do not claim that every such snapshot is reproduced. The active bound inputs, recipe resources, logs and carried custody objects have their own exact copies and identity records.

The `work-tooling/` scripts preserve the actual workspace authoring/auditing implementations and their absolute source-evidence paths. They are audit provenance, not a claim that the source evidence can be regenerated from an arbitrary fresh destination checkout. The repository-native recipe, resources, focused Java/Python tests and sealed installer remain the executable receiving change. Final Git publication and readback are a separate parent-owned step after this local packet is sealed.

## Separate donor catalogue restoration

The original donor catalogue was delivered through historical [source PR9142](https://github.com/hsoliwal/com.synexia/pull/9142) to the earlier task branch. Its five documentation files were absent from both the current be92 baseline and the tested source recovery commit `0c53…`. They are now restored exactly by the documentation-only companion [source PR9367](https://github.com/hsoliwal/com.synexia/pull/9367), commit `4990edbf4ba4afaa14e0ba2d7613331c31e21e9e`, root `1bd86f643f0e08ecb1b5a43d020bf0110e797d68`, with sole parent `0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8`. The source code and execution epoch remain pinned to `0c53…`.

The separately scoped [component binding](donor-catalogue/COMPONENT_BINDING.json), SHA-256 `f02ed2799bd8b7c200a9b4777f549bb7c733751f873a441531f3eec35d73b617`, binds five exact historical documents totaling 78,467 bytes and eighteen exact supporting evidence copies. Its [source publication receipt](donor-catalogue/evidence/SOURCE_PUBLICATION.json), SHA-256 `8dadc9f70a6a2fa1f069bcc0883cef2c3d2218e701fdd166203c7304a3c50e25`, records direct byte comparison of all five documents, six complete changed or reused tree objects, and exact commit/parent/branch/PR readback. The historical authoring receipt's `published: false` is preserved as original content; the new publication receipt records the later restoration.

| Exact receiving documentation copy | Purpose |
|---|---|
| [DONOR_CATALOGUE.md](donor-catalogue/DONOR_CATALOGUE.md) | Categorized donor references and applicability |
| [donor_catalogue.json](donor-catalogue/donor_catalogue.json) | Structured category and provenance catalogue |
| [EXECUTED_TEST_MAPPING.md](donor-catalogue/EXECUTED_TEST_MAPPING.md) | Historical catalogue-to-test mapping and its stated scope |
| [INTEGRATION.md](donor-catalogue/INTEGRATION.md) | Historical source integration notes |
| [AUTHORING_RECEIPT.json](donor-catalogue/AUTHORING_RECEIPT.json) | Preserved original authoring identity and status |

These documents retain their original source-relative links and historical statements. For those links, use the immutable companion [source catalogue](https://github.com/hsoliwal/com.synexia/blob/4990edbf4ba4afaa14e0ba2d7613331c31e21e9e/synexia-openrewrite-recipes/recipes/atom-pattern-mastery-20261005/jcc/donors/DONOR_CATALOGUE.md) and each document's `source_url` in the component binding. A relative link in an exact receiving copy does not imply that its historical source target exists at the same destination-relative path. The original 68-test aggregate remains historical; the proposed 1,871-case expansion is not claimed as executed. The current donor evidence remains the seven actually executed donor methods in the independent native lane at source `0c53…`, as separately linked by the component binding.

This 24-file documentation component was staged **after** the focused receiving execution and reference audit. It is outside their 1,008-input freeze and 182-reference occurrence count; its copies and publication custody receive a separate final documentation/manifest review. No source or receiving test was rerun, no four-file afterimage was changed, and no capability, export or destination gate was promoted. The current source README was retained; the companion restoration contains only the five formerly missing donor documents.
