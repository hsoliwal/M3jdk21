# E4 reference checker: independent static review

**Disposition: ADMITTED FOR BOUNDED READ-ONLY AUDIT.** No defect was found in the requested scope for the exact 40,725-byte checker with SHA-256 `521c18944616fe46a3e347860c25a7283e1b74674c594e40996c787e01e96b81`. The coordinating owner was notified before invoking it. This report is a static admission, not the result of executing the checker or accepting receiving behavior.

The actual readiness file hashes to `7d238a9c1a4b42ae12811fe85b8d2e66e0aeacdd8e982a3aa16ccd1e82c30561`. Its initial state is `DRAFT_AST_PARSED_NOT_EXECUTED`; the task's differently transcribed hash was corrected by the parent before admission. Both the checker and readiness bytes match their intended identities.

## Review method and boundaries

The parent reviewer read the complete checker source and inspected its actual bound inventories, current context, custody index, plan, four proposed before/after resources, two invalid-UTF-8 local fixtures and restored root POM. The independent companion reviewer inspected custody exceptions, omitted-envelope pointers and the output surface. Standard-library parsing, file reads, byte comparisons and metadata checks were used; no owner module was imported or executed.

The review has 32 stable input files in `INPUT_SEALS.json`. `STATIC_AUDIT.json` records the checked structure and limits. The companion review and seals are bound separately. Source publication verification was not repeated. The parent assigned the receiving execution and its full frozen-input review to another reviewer; this report does not duplicate or replace that work.

## Accounting and reference checks

| Obligation | Static finding and actual bound structure |
|---|---|
| Final source objects | Exactly 19 unique objects, split 13 and six between the two affected mappings; the checker validates local byte identities, final source commit and complete recorded Git path chains |
| Final proof references | Exactly 55 unique proof bodies, matched to published entries and exact receiving reference fields |
| Copied receipts | 134 named receiving copies: 123 from the pinned custody additional-receipt set, eight additional source-result/POM records and three reserved records |
| Custody originals | 121 exact original files totaling 15,440,298 bytes; the checker compares original, staging and receiving-copy bytes and preserves prior-copy checks where recorded |
| Current input context | 527 unique current input paths, plus two separately admitted malformed-UTF-8 fixtures: 529 distinct local paths; ten current-context receipt copies remain separate |
| Root accounting | 343 root objects and 201 ordered Maven declarations, including profile context; the restored 119,981-byte POM was independently read, hash-checked and parsed to confirm those declarations |
| E3/E2 preservation | Required 89 E3 and 56 E2 payload files are checked by declared identities and direct byte equality against their preserved local authorities |
| Mapping preservation | The actual proposed map preserves all 46 ordered IDs, all 44 other whole records, all 20 gates and surrounding global data |
| History and target preservation | The two changed records retain 21 historical source objects and six receiver target objects; their previous target/supersession data remains unchanged |
| Installer boundary | Four exact output paths and seven unchanged guards; proposed preimages equal the preserved E3 owner bodies, and actual installed postimages must equal the sealed after-resources before the audit can pass |

The 134-copy total is derived from the actual admitted inputs, not inferred from the 123-entry subset alone. The checker requires equality between the complete origin-name set and the binding's receipt-name set, enforces each canonical receiving path, compares each copy with its exact origin, and rejects undeclared files in the source-recovery copy directory. The eight extra records and three reserved records therefore remain covered by the full copy loop.

The checker reconstructs the recorded Git tree objects and validates their hashes before traversing each final source/proof path. It compares the complete final root-entry inventory with those reconstructed entries. It reparses the exact root POM and verifies each root-coverage row's object identity, declaration paths and declaration contexts. This is bounded root accounting; it does not establish a complete source checkout or semantic dependency closure.

The actual proposed map and binding satisfy the reviewed structural constraints. This resource inspection is not a claim that the four files had already been materialized when the static review began. Installed outputs are independently required by the checker's runtime comparisons and the receiving execution receipt.

## Frozen inputs and prior execution evidence

The checker refuses to issue its audit without the completed `verification-v3/receipt.json`. It checks the source/destination/plan binding, reported 12-test Java success and six Python tests, expected exit codes and log/XML identities. It then re-reads every recorded fixed and external input and requires all 527 current-context paths, both invalid-UTF-8 fixtures, all 19 source paths and all 55 proof paths to be covered by the external set. Both malformed fixture identities must match their exact frozen bytes; they are not decoded or rewritten.

The inspected verification driver constructs those 529 distinct context/fixture entries before the run, admits their declared identities, and freezes them with the other source/proof inputs. The reference checker is a subsequent consistency audit of that evidence. It does not independently rerun JUnit, Python, the installer, native tests or source qualification, and does not substitute a second behavioral result for the original execution receipt.

## Custody and omitted-envelope limits

The pinned publication facts distinguish six mandatory direct bodies, 21 observed direct bodies, and exactly two malformed-UTF-8 identity-only fixtures. Both exceptions retain `raw_body_readback=false` and `remote_sha256_observed=false`, including the recorded failed probe/final attempts. The checker compares these values with the exact pinned expectations and publication custody fields; the exceptions cannot silently enter the direct-body set.

The 132 omitted originals remain explicit omissions: 42 original tree-call envelopes, one redundant readback and 89 transport batches. Their staging/copy fields are null. Each original local file is hash-checked, then connected to a hash-bound carried document through its recorded JSON pointer. Missing files, missing pointer targets or inconsistent paths/hashes fail the audit. The checker neither synthesizes envelopes nor claims complete original-envelope reproduction. Authenticity of earlier captured tool responses remains an established input assumption, not a new remote observation by this audit.

Source qualification remains blocked: current independent native 19 passes coexist with the executed upstream 66/67 result and one error. The checker preserves empty capability test lists, blocked mapping rows, false acceptance/export flags, unchanged Descriptor reconciliation and the historical receiving fixture. No whole-module, source-export or runnable-JDK promotion follows from an audit PASS.

## Mutation surface and disposition limits

The only filesystem mutations in the checker create a new `reference-audit-v3` directory and write its `REFERENCE_AUDIT.json`, after the substantive assertions have passed. An existing output is refused. The source has no subprocess, build, recipe, test, network or remote mutation call, and no write to the candidate, owner, POM or resource files. Assertions are guarded against optimized Python, and failures are not caught and relabeled as success.

Admission applies to the exact checker and its bounded intended invocation after receiving evidence is final. The owner subsequently announced successful execution while this static report was being sealed; that result and the final publication manifest require their separate executed-evidence review. No owner checker, project gate or remote action was performed by either static reviewer, and the subsequent announcement is not treated as proof within this report.
