# Corrected V3 custody admission and actual input review

**The corrected T02 V3 programs and frozen custody input are admitted for the narrow probe and the subsequently conditioned publication sequence. No concrete review blocker remains.** This review does not claim execution of either owner program, completion of the final-payload batches, final body/tree verification or branch/PR publication.

The review combines full source inspection, independent arithmetic over the actual captured four-create/four-read/17-tree evidence, and a separate focused review of the main verifier and its T02 correction. The final source manifest and its existing publication-input admission remain unchanged.

## Exact admitted inputs

| Artifact | SHA-256 |
|---|---|
| Corrected `raw_custody_v3.py` | `45fe2a39cc936f94e20cf0051f3844e6d6b5ba59b87b2d3ca175bb60917603be` |
| Corrected `verify_publication_v3.py` | `45d1e83ba81dfaed9434fddb7df9f4962b7fbbb8cb1c9b29f90754c8fcf38baa` |
| `raw-custody-v3/CUSTODY_INPUT.json` | `22b58a9e97fd6bb4ebebc6a89dc13e368107c10429bff59191914a5e639c81df` |
| `raw-custody-v3/PROTOCOL.md` | `cdf3826e3725acded05b38031b70d9e7a8f8579540b38d65a2a8e38ce64209f5` |
| Preserved V2 verifier | `8cc5fd5c5df99ae8f48d0fbbf4618ada99be429af9cb39839511940192bd8639` |
| Final source manifest | `9270a6b4098960e2b7c7bd87b17325b648c4de363957f36028bacc9c1955e328` |
| Existing V3 publisher preflight | `13e56b17314e89370fb8ce3f8417b405b89b34db8a1868d968589550975db84f` |

The original unexecuted V3 helper/verifier drafts remain exact under `raw-custody-v3/draft-T01/`, at `a96f1aec…` and `7138709c…`. This review retains complete source snapshots, AST-only parses and exact T01/T02 textual differences. The older source review is preserved as a review of that unexecuted draft, not as evidence that its former mutable file locations still contain T01.

## Actual captured custody inputs

All four creation calls have complete actual argument and response records. Each declares the expected GitHub create-blob tool, repository, base64 encoding and exact permitted argument set. Strict decoding of each actual argument produces bytes equal to the frozen local payload and prepared transport content. Byte length, local SHA-256, recomputed Git blob ID and returned creation SHA all agree. There are exactly four distinct expected raw objects; no missing response is filled from an expectation.

The retained verification commit is `9dab34bd9e34e4d661004482248b5b02d94e0816`, with tree `bf2b8fe43cda47f0c6363c8d376f61419e647e4d` and sole parent `be92c62ece9023b5c33676716a1076d00e26120a`. Its parsed body equals the JSON in the complete actual retained GET wrapper, which reports success. Its request URL names that exact immutable commit.

All **17 actual verification tree GETs** are present, successful and nontruncated. Their request URLs name their returned tree identities. The reviewer independently recalculated each Git tree hash and compared every normalized entry with the admitted four-addition plan. Exact path/type/mode/blob/size traversal binds all four raw fixtures in the verification root. These checks confirm the observed verification trees; the earlier sealed plan review separately established preservation of every other base entry.

All **four actual file calls** are now captured with complete request objects and response wrappers. Each requests full-file base64 at the exact verification commit; response path/ref metadata matches. The observed outcomes are:

| Fixture | Original bytes | Returned bytes | Actual body result |
|---|---:|---:|---|
| NUL manifest | 126 | 126 | Exact direct match |
| Invalid UTF-8 manifest | 4 | 6 | Exact observed replacement-character failure |
| NUL dependency | 110 | 110 | Exact direct match |
| Invalid UTF-8 dependency | 22 | 24 | Exact observed replacement-character failure |

Both invalid UTF-8 response bodies differ from the originals, equal the specific UTF-8 replacement transformation, and recompute to Git identities different from the original SHAs still reported by the wrappers. Those results remain failed raw-body reads. The original earlier episode that retained only two responses also remains unchanged; the newer complete four-call evidence is a separate capture.

## Exact exception boundary

Only these two immutable objects use the alternate custody basis:

- `59b4ec83f687499a54f15695b9b246599f9579d7`, the four-byte invalid UTF-8 manifest.
- `24cdc62e634934527d551324c2449bcf951b5589`, the 22-byte invalid UTF-8 dependency.

The helper fixes their suffixes, sizes and local SHA-256 values and fixes the complete four-object raw-fixture set. It verifies that each exception is still invalid UTF-8 and that its downloaded representation exhibits exactly the recorded failure. Arbitrary files, new hashes, generic download failures and unrelated mismatches cannot use the exception path.

For these two objects, custody rests on exact captured uploaded bytes, actual matching creation IDs and complete immutable Git tree membership under normal Git/GitHub content-address assumptions. It does not recover their remote byte arrays or independently observe their remote SHA-256 values. The helper explicitly records `raw_body_readback=false` and `remote_sha256_observed=false` and labels the SHA-256 of the local uploaded bytes accordingly.

Both NUL fixtures and all four production updates retain mandatory direct body checks. The main verifier requires exactly six required direct identities, rejects the two exception IDs from downloaded-body entries, and requires coverage of all six. No successful downloaded body is synthesized from a local template.

## Source guards and corrected provenance checks

The corrected helper now verifies that the parsed verification-commit body equals its retained original wrapper content and that the wrapper reports success. This closes the draft's unchecked internal body/wrapper relationship. Its bound-input routine checks the original supplied leaf path for a regular file and symlink before resolution, then checks resolved containment and content pins. It does not claim that every ancestor component is free of symlinks.

The main verifier's only T01/T02 change is the new helper hash pin. Before importing the custody helper, it verifies the preserved V2 and corrected helper identities, in addition to the existing audit and publisher pins. The standalone probe uses the frozen inspected helper modules and is limited to the probe receipt; the final V3 path performs the explicit import pins. Neither path claims operating-system-wide execution attestation.

For each direct final body, V3 checks the actual tool label, complete request-key set, repository, exact final-commit ref, base64 encoding and response display path. Normalized SHA/content/encoding must equal the original structured response fields. Derived byte size is explicitly labeled and must separately match the remote tree entry's size. Strict decoding, recomputed Git identity, expected length/SHA-256 and direct equality with pinned local bytes remain mandatory.

The supplied custody input is checksum-bound through the final readback reference; selection of the exact reviewed `22b58a9e…` input remains part of root's frozen readback assembly. Actual tool-call authenticity remains a caller evidence boundary: these consistency checks do not authenticate fabricated JSON independently of its capture provenance.

## Final publication conditions remain active

V3 retains all full-payload checks: exact local byte/mode identities for 2,796 files, four admitted production updates, 1,039 exact restorations, recomputed final root, complete remote changed-tree entries and every payload path's expected object/type/mode/size. The frozen plan contains **830 distinct final changed trees over 1,115 changed ancestor paths**. Their final actual GETs remain future publication evidence at this admission.

The final tree must remain `0361e4a07b77a01df170523033fef01c4052ddc5`. The final source commit must have only be92 as parent and cannot be the verification commit. All four final raw file responses must be captured at that final immutable commit, again retaining two direct NUL matches and two explicitly failed UTF-8 reads. Final tree membership must bind all four objects.

Only after the full final payload/tree/commit/custody/six-direct-body checks does `--before-branch` emit `FINAL_COMMIT_CUSTODY_VERIFIED_BEFORE_BRANCH`. That mode rejects supplied ref/PR fields and does not claim to verify a branch or PR. Root must execute it before creating the branch; the flag alone cannot prove chronological ordering or independently query branch absence.

Final mode repeats the complete checks and additionally requires the actual intended branch, exact commit, open draft PR, repository/base/head relationships and no-force state. Its status is `PUBLISHED_CUSTODY_VERIFIED`, with six required direct bodies, two identities verified without body recovery, and `all_required_blob_bodies_read_back=false`. It cannot be renamed as the older V2 successful-raw-readback status.

The initial narrow probe must succeed before final-payload batches. The before-branch receipt must succeed before branch/PR creation. These are actual future execution requirements; this review is an admission to execute the programs, not their success receipt.

## Seals and disposition

The principal reviewer checked **37 unique stable inputs**, independently parsed seven Python source files without import/execution, validated the complete captured 4/4/17 evidence, and rechecked all inputs unchanged. The inspector passed on its first execution. `REVIEW.json` contains each creation identity, tree receipt, resolved membership and file-read outcome. `INPUT_SEALS.json` binds the exact source/input bytes; source copies and draft differences are retained alongside it.

The focused companion main-verifier review and T02 delta addendum are bound by `COMPANION_BINDING.json`. Their respective artifact seals are `2322e736246e2fd11d94bc4c9c5d1a5717e0bd5be6179f7e4509a9242606d43c` and `05242c715b5d63567d13c26c801e9a2c96ca4e27f14434e3a96020cdcd374a97`. Both found no remaining blocker within their scopes.

No reviewer executed an owner helper, verifier, project build, test, recipe, Java/native command or remote action. No source, fixture, manifest, publisher, earlier verifier or sealed review was changed by the reviewer. The corrected V3 is conditionally admitted; source qualification remains BLOCKED and export/destination acceptance remain unapproved.
