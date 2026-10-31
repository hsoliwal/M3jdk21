# Exact source-publication successor for the JCC receiving ledger

This packet binds the two existing blocked JCC records to the published Synexia implementation at `d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906` and its exact source/proof objects. It is ready for a new receiving draft PR from `df06cdee5a8526f573b3ad89622824d0b49e2f25`. The existing receiving PR139 is merged history. This local packet does not assert that its new branch or commit has already been published.

The source publication is [com.synexia PR9285](https://github.com/hsoliwal/com.synexia/pull/9285), root `6f9b6a48f8cefa7a643f222c7aa1f621497725e7`. The exact source readback receipts verify 1,016 payload files and 112 preserved files; a separate full-body readback confirms all nine production changes. The source execution input remains `0b8dc32b9e8a616b7b7141bbdd88722839dc64bc`. Publication readback connects tested/materialized bytes to their later commit and does not relabel the earlier run as a fresh checkout test.

## Actual changes and retained authority

The existing `M3Jdk21HashPinnedTextSnapshotRecipe`, canonical `m3/docs/name-mapping.json`, schema, `migration.py` and sealed `recipe.py` installer own this change. `com.m3.rewrite.backport.JccSourceFinalHandoff` selects a new four-output crate. The plan seal is `af65912789780e214f09644b38f542db5f7670b83ee1fada228bce08f8f9b30e`; it is the canonical plan digest, distinct from the formatted plan file hash. The materialized mapping afterimage is SHA256 `e4ae516fea9822a4bfe26669e9749e167f52bb30049d520339ee42f3779bc0b3`.

Exactly four existing files change through that actual recipe/installer: canonical mapping, handoff document, binding receipt and root obligations. Every preimage equals its E2 published bytes, and all seven dependency guards stay exact. New crate resources, named YAML, Java/Python tests, focused proof POM and task evidence are additive. The original E2 one-update/three-absent crate, tests, proofs and other payload files remain intact. No production Java, native source, existing owning POM, schema, validator, installer or workflow is changed.

All 46 record IDs and their order, 44 other complete records, 20 gates and non-record globals remain exact. Both JCC statuses remain `blocked`, capability `tests` remain empty, and all broad acceptance flags remain false. All four old source artifact objects survive in canonical lineage with their exact previous commit/hash/ref/role values. The original receiving fixture changes only its null commit to the actual E2 commit and its role from candidate to pinned; the entire old null-commit object remains in previous targets. The Descriptor record and its complete reconciliation/history remain unchanged.

The 17 active source bindings comprise nine changed production owners, four unchanged mapped owners and four unchanged shared-transpiler contracts. Laboratory accounts for eleven and Java/JNI regression for six. All nine repairs, including the shared `M3OpenRewriteTranspiler`, are linked to actual source postimages and recipe Result receipts. The four donor/category/planning fixes do not establish native implementation or destination import.

## Executed receiving checks

All runs below used the final sealed afterimages and bound source commit. Exact commands, timestamps, stdout/stderr, frozen inputs and Surefire XML are retained under [execution](execution/receipt.json).

| Check | Observed result |
| --- | --- |
| Actual retained text recipe, named YAML and serialization | 11 JUnit tests; 11 passed, zero failures/errors/skips. |
| Actual retained installer contract | Six Python unittest methods; six passed. |
| Refusal coverage within those six tests | 44 negative workspace conditions, each through check/apply/rollback: 132 refusal calls. This is a subcase/call count, not 132 additional tests. |
| Actual E3 overlay sequence | Initial check before/0 writes; apply after/4; fixed point after/0; rollback before/4; replay after/4; final check after/0. |
| Whole operational map validation | Exit 0, `MIGRATION_MANIFEST_VALID completion=INCOMPLETE`. |
| Separate completion command | Expected exit 2 with explicit incomplete migration obligations. |
| Input stability | All 229 frozen input identities unchanged; four operational outputs exactly equal sealed afterresources. |

The Java tests exercise all four missing, drifted, duplicate and scan-to-visit mutated inputs, actual four-output transformation, unrelated-source preservation, fresh fixed point, exact history and blocked flags. The Python methods exercise eight preimage conditions, fourteen guard conditions, every fourteen proper mixed before/after subset, four edited postimages and four missing postimages. Refusal checks preserve workspace bytes, modes and directories, including unrelated binary content.

The focused execution used Java 21.0.2, Maven 3.9.12, Rewrite 8.17.1, JUnit 5.10.2, compiler plugin 3.13.0 and Surefire 3.2.5 with the existing offline repository and an explicit empty user settings file. It compiled the retained text owner and the new test, loading exactly ten crate resources plus the named YAML. SLF4J reported the retained absent logging binding and used its NOP logger; the actual build and all tests completed successfully. No test was excluded or skip flag supplied to obtain these results.

The original four receiver behavior tests were not rerun: their exact E2 owner/fixture/corpus/evidence bytes are preserved. These new metadata tests do not substitute for the owning module's 99% line/branch gate. The preserved frontier is 605 non-tree inputs, 32 acquired and 573 missing, with the exact offline JaCoCo 0.8.15 POM/JAR unavailable. No full owning-module or JDK acceptance is claimed.

## Separate explicit reference audit and independent review

The ordinary validator intentionally skips active file hashes for blocked capabilities. The separate [reference audit](reference-audit/REFERENCE_AUDIT.json) enumerates and verifies the actual bodies rather than treating ordinary map validation as their proof:

| Explicit audit dimension | Checked |
| --- | ---: |
| Active source artifact occurrences | 17 |
| Published source proof occurrences | 46 |
| Destination reference occurrences | 45 across 34 distinct paths |
| Nested original receiving fixture references | 14, included above |
| Copied publication/source/context receipts | 8, included above |
| Preserved E2 payload files | 56; the other four are the authorized existing updates |
| Final source root objects | 343 |
| Paired Maven declarations and contexts | 201 |
| Frozen execution input identities rechecked | 229 |

Source evidence is compared with actual local body lengths, SHA256, Git blobs, the transport manifest and recorded remote readbacks. Destination references are tied to E2 publication, retained owner acquisition or new sealed afterimages/receipt copies. Historical lineage is retained as historical evidence. The checker records no unresolved active hash reference in the two requested blocked rows. This is a bounded reference audit, not repository-wide dependency closure or source export.

The first checker attempt stopped on its own separator assumption: canonical TSV uses semicolons for multiple declaration/context values, while the checker initially expected pipes. The correction changed only the read-only checker. The failed checker text and explanation are retained; no candidate change or execution rerun occurred.

The [independent static review](independent-review/REVIEW.md) found no concrete defect. It separately read the complete bound Java/Python tests and retained owner, rechecked sealed plan/map/lineage, and reviewed source proof and root-accounting companions. Its review did not run builds/tests/installers or claim to have done so. The separate executed-evidence review is packaged under `final-gate-review/`; it inspects actual recorded commands/results and reference evidence without rerunning the tests.

## Source qualification remains blocked

The final source replay strictly compiled 443 first-party Java owners from 503 admitted original inputs; nine changed and 494 were byte-identical. Core 91/91 and parent 8/8 passed. The unchanged complete selected upstream gate ran 67 tests: 66 passed, zero failures, one error and zero skips. The error remains the incomplete static-analysis runtime pack naming `org.openrewrite.staticanalysis.AtomicPrimitiveEqualsUsesGet`. The driver exited 1 at `46-upstream-java`; thirteen later stages, including required native/JNI build and parity tests, did not run.

The API evidence covers 69 visible types across twelve owners relative to its explicitly scoped retained capture and narrower real-preimage comparisons. The complete original baseline did not compile. Composition records sixteen logical A/P/AP/PA schedules and thirty-two replay executions with matching serialized receipts; retained source observations are hashes, not complete final source/class maps. These limits remain explicit in the binding receipt and handoff document.

All 343 root entries are accounted with exact final object identities and 201 paired Maven declarations/contexts. The whole repository descendant/symbol and semantic dependency denominators remain unknown. Bounded source-owner tests do not mark entire root units tested, exported or delivered.

Source export, source-qualified destination materialization, complete destination gates and full capability readback remain false. The exact body publication receipt, finite local passes, metadata installation, whole-module coverage, CI and JDK/JNI admission are separate facts. Existing unresolved CI observations remain historical and are not converted into passes by this successor.

## Evidence reproducibility

The source-final receipt copies and public source proof references carry exact identity bindings. Execution files retain original local absolute paths as provenance. The scripts under `provenance/` record the actual acquisition-bound authoring, checker and execution procedure; they are not a new repository build entry point. From a full checkout, the additive tests use the existing focused POM and Python packet test listed in the handoff document. The canonical plan can be checked/applied/rolled back with the existing `m3/migration/recipe.py`; its four exact preimages and seven guards remain mandatory.

No new receiving publication commit is placed inside its own files, avoiding a self-reference cycle. The coordinating publisher must separately verify payload blobs/modes, retained base files, commit parent/root/ref and the new draft PR before describing delivery.
