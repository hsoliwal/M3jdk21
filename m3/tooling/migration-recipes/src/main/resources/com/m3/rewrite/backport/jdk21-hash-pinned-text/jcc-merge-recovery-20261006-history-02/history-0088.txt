# Publication readback verifier V2: independent static admission

**No remaining source-level blocker for the stated publication-custody check.** The exact V2
source is 9,623 bytes, SHA-256
`8cc5fd5c5df99ae8f48d0fbbf4618ada99be429af9cb39839511940192bd8639`.
This is static admission of the prepared verifier. It is not an execution, publication result,
compiler/test result, or acceptance of an unseen final manifest/readback.

## Corrected V1 findings

Both exact helper files are now checked with standard-library SHA-256 **before their imports**:

| Helper | Pinned SHA-256 |
|---|---|
| audit_base.py | `839c35c611de2dc02f2a3f1233131b24c79d77e109aaf010df9689dc8e04893b` |
| prepare_publication.py, V3 | `3cd42b4b5c4ba1e0eb01cace40bff7cb2fe967d65ac4e9a646b70d7daf4404fd` |

V2 retains the actual local bytes for every required canonical or base64-transported payload
object. Each decoded remote object is compared directly with those bytes, in addition to its
size, Git blob identity and SHA-256. The required-object set must be present; duplicate blob
responses are rejected. Repeated payload paths sharing the same Git blob use one identical
required byte sequence. The receipt reports the number of **distinct required blob objects**,
which must not be confused with the number of payload paths referencing them.

The final receipt includes both helper identities and the byte-comparison count. The V1-to-V2
diff contains these changes and retains the other reviewed checks. V1 remains preserved.

## Reviewed verification logic

The imported helpers perform no publication, credential scan or filesystem mutation merely
by being imported. The verifier rejects optimized Python before relying on assertions. Its
output is confined to the publication workspace; `exact_write` refuses to replace a different
existing output.

The source requires the named repository, branch, base commit/tree and preflight status/schema.
It checks the sealed manifest hash, one-to-one payload path membership, every source file's
bytes/hash/Git blob/mode, and the exact four allowed existing-path postimages. It verifies the
restoration count and payload totals against the preflight, then reconstructs the expected
root and every changed tree, preserving unchanged sibling entries.

For the observed Git trees, it rejects duplicate/direct-entry path defects and truncated
responses, recomputes each Git tree identity, compares every complete changed tree with the
expected entries, and resolves every published payload from the expected root. Every resolved
entry must have the exact blob type, mode, Git identity and byte size. Required remote bodies
are strictly base64-decoded, checked for Git identity and compared directly with local bytes;
raw fixture bytes are not decoded as text or normalized.

It then requires a commit with exactly the expected parent and tree, a branch ref pointing to
that commit, and an open, unmerged draft PR whose head SHA/branch, head/base repository, base
branch and canonical PR URL agree. The captured nonforced-ref-update flag must be false.
It emits the custody booleans only after these checks.

The 381 trees in the sealed local base model were independently rehashed from their raw Git
entry encoding during this static review. This checks the available base model; it does not
simulate an unavailable final payload or remote readback.

## Required actual evidence and limits

The checker consumes a **trusted, already-sealed V3 preflight** and actual full Git/PR readback
data supplied by the owner. It does not authenticate a fabricated JSON document as a network
response, reconstruct a raw Git commit object from commit metadata, or itself perform any
remote read. Preserve the actual read requests/responses and provenance with the final run.
The nonforced-update flag comes from captured operation metadata, not from an immutable Git
tree object. The owner must separately verify the exact requested PR body; this script does
not assert body/title equality or mergeability.

The preflight is responsible for its restoration/admission checks, private-data scan and raw
fixture transport admission. This postpublication checker binds and revalidates its payload,
tree and identity data; it does not replace those distinct gates. Source execution, old
failures, unchanged historical parser proof, original-owner/upstream tests, separate JNI,
final writer replay, full reactor/JDK obligations and E4 receiving acceptance retain their
own receipts and limits. Publication-custody success cannot promote any of them.

No verifier, imported project module, test, build, recipe, Java/native program or remote action
was executed by this reviewer. Static parsing, direct source/diff reads, byte comparisons and
independent hashing of the existing base model are the only checks. `INPUT_SEALS.json` and
`FILE_SEALS.json` bind this review and its exact inspected source copies. Actual final-input
and publication admission remains pending its real evidence.
