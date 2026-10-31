# E4 V3 reference checker: custody and output subreview

No defect found in the bounded static review of checker `521c18944616fe46a3e347860c25a7283e1b74674c594e40996c787e01e96b81` (40,725 bytes). This is static admission of the inspected custody/output logic, not a claim that the checker executed, that pending afterimages were materialized, or that the receiving pipeline passed. The readiness file explicitly says `DRAFT_AST_PARSED_NOT_EXECUTED`, `audit_executed=false`, and requires the coordinating owner's completed-output notification before execution.

The whole checker was parsed as source without importing it. Its imports are standard-library modules; no owner checker, recipe, build, process or network invocation is present. The only filesystem mutations are `out.mkdir()` and `path.write_text(...)` at the end of `main()`. The output directory is fixed to `successor-e04/reference-audit-v3`, must not already exist, and the only output file is `REFERENCE_AUDIT.json`. Missing-input errors and failed assertions precede those mutations, including the custody-index check and the required completed `verification-v3/receipt.json` checks. The checker refuses optimized Python, so its assertions cannot silently disappear in an accepted invocation. Ordinary filesystem failures during the final mkdir/write remain possible and must be treated as an unsuccessful invocation; this is not an atomic-output guarantee.

The custody expectations are independently hash-pinned. They identify exactly these two source readback exceptions:

| Fixture | Git blob | Original bytes |
| --- | --- | ---: |
| `parser-bad-manifest-utf8/parser-dependencies.tsv` | `59b4ec83f687499a54f15695b9b246599f9579d7` | 4 |
| `parser-bad-utf8/Dependency.java.txt` | `24cdc62e634934527d551324c2449bcf951b5589` | 22 |

The checker compares custody fields with the exact pinned source-publication receipt and requires six mandatory direct bodies, two identity-only bodies, four direct production bodies, two direct NUL bodies, and false `malformed_utf8_body_readback` and `all_required_blob_bodies_read_back`. The 21 observed direct bodies are separately required to be exactly the 19 designated source bodies plus the two NUL fixtures. The two exception identities cannot appear in that direct-body set.

For the 21 direct observations, the checker requires the immutable source commit, exact expected request path/repository, corresponding structured-response SHA/content/encoding, strict base64 decoding after whitespace removal, exact local-byte equality, independent decoded byte/SHA-256/Git identity, and matching source-tree membership. It binds the original readback document to the pinned publication receipt. It does not perform a new connector read or independently authenticate the original call. The direct-response condition rejects an explicit `isError=true` rather than requiring the field to be present and false; in this audit that check consumes an already hash-bound captured document and does not expand the accepted custody exceptions.

For the two exceptions, local identities and final tree memberships must match the separately recorded creation Git identity, byte count and local-upload SHA-256. Each claimed method must be `BASE64_CREATE_AND_REMOTE_GIT_TREE_IDENTITY`. Both overall and failed probe/final entries must retain `raw_body_readback=false` and `remote_sha256_observed=false`, with read attempts classified as `FAILED_UTF8_REENCODING`. This audit checks consistency with the already trusted published custody result; it does not re-execute the earlier create/read audit or claim to recover missing remote byte arrays.

The custody evidence index and additional-receipt map are hard-pinned by SHA-256. The index distinguishes 121 exact carried originals and 123 additional receipts from 132 omitted local originals. It requires false original-reference-rewrite, complete-original-envelope-reproduction, complete-source-checkout, source-export and destination-gate claims, with no structured subobject extractions. Each carried original must equal its staged and receiving copies byte-for-byte.

The 132 omissions are explicit records, not stand-in envelope files. They must have null staged paths and null planned copy names, unique original paths, nonempty reasons, and these exact counts:

| Omission status | Count | Carried pointer basis |
| --- | ---: | --- |
| Original local envelopes not carried | 42 | `FINAL_READBACK.json` / `actual_tool_capture_references` |
| Redundant local readback not carried | 1 | `BEFORE_BRANCH_CUSTODY.json` / `readback_path` with matching readback hash |
| Transport batches not carried | 89 | `transport/INDEX.json` / `batches` entries |

The checker rehashes each actual original local file and follows the declared pointer in a checked carried document. A missing file, missing pointer, differing referenced original path or differing SHA-256 raises rather than being replaced by an inferred successful observation. It returns `complete_raw_envelope_reproduction_claim=false`. These are local pointer/identity validations of previously captured evidence, not a claim that the omitted originals are packaged remotely or reproducible from the receiving payload alone.

The output preserves publication custody separately from source and receiving acceptance. It requires source qualification BLOCKED, upstream 66 passes out of 67 with one error, independent native 19 without aggregate promotion, and false source-export/destination acceptance. It requires a completed verification receipt and frozen-input rechecks before issuing the audit, while explicitly stating that receiving behavior was not rerun and no build, behavioral test or remote operation was performed by this checker. Earlier source captures remain trusted prior evidence; this new static review and a future local reference audit do not create new source, JNI, platform or whole-module execution claims.

The parent independently owns accounting, mapping, copied-receipt completeness, frozen-context checks and actual materialization/gate evidence. This subreview inspected only the relevant code and pinned expectation/index/readiness resources; it did not rerun source publication verification or rehash all 121 carried and 132 omitted bodies. All five inspected inputs were rehashed unchanged. No owner files, prior seals, project gates or remote objects were modified.
