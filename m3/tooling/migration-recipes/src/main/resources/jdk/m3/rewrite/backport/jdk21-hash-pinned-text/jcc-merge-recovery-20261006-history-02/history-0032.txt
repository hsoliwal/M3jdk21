# V3 publication custody for two malformed UTF-8 fixtures

The source manifest, preflight, publisher, payload and V2 verifier remain byte-identical. The earlier raw-download gate failed and remains preserved in RAW_READBACK_FAILURE_OBSERVATION.json and its independent additive review. V3 changes the work-only publication evidence method; it does not repair or promote a source test.

## Actual capability and limit

The GitHub connector decoded malformed UTF-8 and re-encoded replacement characters even when fetch_file requested base64. NUL-containing valid UTF-8 content is preserved. The private repository has no available independent lossless raw-download tool in this session. No synthesized downloaded body is permitted.

Under normal Git/GitHub content addressing, a captured exact base64 create request and an authentic matching returned blob ID, subsequently bound by complete immutable tree reads, provide Git object custody for the intended uploaded bytes. This does not recover a remote byte array or remotely observe its SHA-256.

Exactly two exceptions are admitted:

- 59b4ec83f687499a54f15695b9b246599f9579d7: four-byte parser-bad-manifest-utf8/parser-dependencies.tsv.
- 24cdc62e634934527d551324c2449bcf951b5589: 22-byte parser-bad-utf8/Dependency.java.txt.

No other mismatch or file can use this route. Both NUL fixtures and all four production updates remain mandatory direct byte-for-byte body checks.

## Frozen inputs and verification

raw_custody_v3.py binds the unchanged manifest and transport index, original failed attempt, four newly captured idempotent create calls with full actual arguments and responses, the unreferenced verification commit, all 17 actual complete/nontruncated verification tree GETs and four actual immutable-ref file responses.

Every uploaded request must decode exactly to the frozen local payload and match its length, SHA-256 and recomputed Git blob. Every returned creation SHA must match. Complete verification trees are independently rehashed and compared to the exact four-addition plan, preserving all base siblings. The file responses must show two exact NUL bodies and the specifically observed UTF-8 replacement behavior for the two exceptions.

The pre-payload probe receipt is a Git object custody result with two failed raw body reads. It is never a successful raw-download receipt.

verify_publication_v3.py retains every V2 manifest, local byte/mode, exact restoration, production update, complete final tree, parent, ref, PR and no-force check. It verifies all 830 distinct final changed trees and 1,115 changed ancestor paths. It additionally binds direct-file response provenance and matches each read path, requested immutable ref, response fields and independently returned tree size.

The six required direct bodies stay strict. The two exception blobs are explicitly excluded from downloaded-body entries and recorded separately with raw_body_readback=false and remote_sha256_observed=false. Their final exact-commit file responses must retain the observed failure, and their exact path, mode, size and Git blob must resolve through the final tree.

## Publication order and output

1. Independently review these concrete V3 scripts and inputs.
2. Execute the narrow raw custody probe against the captured actual evidence.
3. Execute the unchanged 89 final payload tree batches, starting from be92's original root and requiring final root 0361e4a07b77a01df170523033fef01c4052ddc5.
4. Create the final source commit with sole parent be92c62ece9023b5c33676716a1076d00e26120a. The verification commit is not its parent.
5. Read all final required bodies at that immutable commit and all final changed trees. Execute V3 --before-branch to produce FINAL_COMMIT_CUSTODY_VERIFIED_BEFORE_BRANCH.
6. Only then create the fresh source branch and draft PR. No force update or merge.
7. Read commit/ref/PR again and execute final V3. The receipt status is PUBLISHED_CUSTODY_VERIFIED, with six required direct bodies, two identity-only bodies and all_required_blob_bodies_read_back=false.

The receiving pipeline must accept and preserve this distinct status and evidence basis explicitly. It may not rename V3 custody into V2 raw-download success. Source qualification stays BLOCKED; source export and receiving gates are not admitted.

References: https://git-scm.com/docs/git-hash-object and https://docs.github.com/en/rest/git/blobs .
