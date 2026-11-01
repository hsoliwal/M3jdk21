# Actual invalid-UTF8 transport failure and custody judgment

**The required raw-byte readback actually failed.** The previous transport review remains a historical conditional admission; its pending raw-fixture condition was not satisfied. No successful normalized raw-body entry may be synthesized from the local fixture to conceal this failure.

This is an additive review of `RAW_READBACK_FAILURE_OBSERVATION.json`. Earlier source, fixture, manifest, publisher, verifier and review artifacts remain unchanged.

## Actual evidence

GitHub returned the planned unreferenced verification tree `bf2b8fe43cda47f0c6363c8d376f61419e647e4d` and verification commit `9dab34bd9e34e4d661004482248b5b02d94e0816`. The retained actual commit GET JSON binds that tree and be92 as its sole parent. These are verification objects, not the final source publication.

The four file calls were awaited, but only response indexes 0 and 1 were retained in this failure record. Their request refs and returned display URLs identify the verification commit and expected fixture paths. The missing response bodies for indexes 2 and 3 are not classified as unrun, passing or failing.

| Retained response | Expected bytes | Returned bytes | Result |
|---|---:|---:|---|
| NUL manifest, index 0 | 126 | 126 | Direct byte equality passes |
| Invalid-UTF8 manifest, index 1 | 4 | 6 | Direct equality and recomputed Git identity fail |

The invalid-UTF8 fixture is exactly hex `23 c3 28 0a`, base64 `I8MoCg==`, with Git blob `59b4ec83f687499a54f15695b9b246599f9579d7`. The returned base64 decodes to hex `23 ef bf bd 28 0a`: the invalid byte was replaced with the UTF-8 encoding of U+FFFD. Those returned bytes have Git identity `e7899a723174a98aabfe18ddbd3178156a00f92f`, while the response still reports the original expected blob SHA.

The response's `isError=false`, base64 label and reported Git SHA therefore do not establish a successful raw-byte read. The retained strict byte and identity comparisons correctly refuse it. The behavior is consistent with text decoding followed by base64 encoding within the retrieval path; this evidence does not establish where inside the service/client stack that conversion occurs.

The root observation states that no final payload batches, final source commit, source branch or PR had been created at this observation. The intended before-payload boundary held. The source compiler/test results are unaffected by this transport failure.

## Capability boundary

The existing `fetch`, `fetch_blob` and `fetch_file` paths have demonstrated text handling unsuitable for these malformed UTF-8 bodies. I did not repeat them. Advertised alternative download tools are restricted to private image attachments or existing Actions artifacts, so neither is a documented arbitrary repository-blob reader. This bounded metadata inspection does not claim that all possible GitHub clients lack binary support. Root separately owns repository access and actual authorized reads; this reviewer inspected no credentials and attempted no alternate endpoint or access-control bypass.

The official [GitHub blob API documentation](https://docs.github.com/en/rest/git/blobs) describes base64 blob creation and a returned object SHA, and documents base64 content in the ordinary REST blob response. The connector behavior observed here differs from a lossless forwarding of that representation. The [Git object hashing documentation](https://git-scm.com/docs/git-hash-object) describes object identity as computed from object type and contents, with an option to avoid content filters. These primary documents support the following custody judgment; they do not certify this connector's byte handling.

## Independent judgment on a different custody strategy

A separately versioned Git object identity strategy is justified for this requested publication scope under normal Git/GitHub content-address assumptions. It is a different and more limited evidence basis than obtaining the two remote byte arrays. It must not receive a label claiming that the original V2 raw-body requirement passed.

Root has narrowed the proposed exception to exactly the two frozen invalid-UTF8 blobs: `59b4ec83f687499a54f15695b9b246599f9579d7` and `24cdc62e634934527d551324c2449bcf951b5589`. Both NUL fixtures and all four production owners retain mandatory direct-byte checks, giving six direct bodies and two object-identity-only bodies.

The concrete revised verifier must require:

1. Exactly those two enumerated exceptions, with their unchanged manifest paths, modes, lengths, local SHA-256 and Git identities. No generic exception for an arbitrary mismatch or missing body.
2. Captured actual base64 create arguments for all four raw fixtures. Strictly decoded request bytes must equal the frozen local/index byte arrays directly; length, SHA-256 and raw Git object identity must also match.
3. Complete captured creation responses whose actual returned Git SHA equals the independently calculated expected identity. Expected values must not fill missing response fields.
4. Actual complete, nontruncated verification and final tree observations at exact commits, binding all four raw paths, blob types, modes, SHAs and sizes. The final expected root and sole be92 parent remain unchanged.
5. Successful direct decoded-byte readbacks for the six required bodies, with exact immutable request refs and preserved actual wrappers. The two invalid-UTF8 responses remain recorded as failed body observations.
6. A distinct V3 receipt that reports six direct bodies and two object-identity-only bodies, sets raw-body-readback false for only those two, preserves the original failure/V2 history, and keeps source qualification, export and destination states separate.

This approach depends on GitHub returning true stored-object identities and on Git content addressing. It does not independently recover those two remote byte arrays or establish a remotely observed SHA-256 for them. The local SHA-256 remains a local/request identity; the remote binding is through the returned Git object ID and immutable tree membership.

The user authorized source implementation and GitHub PR publication. No higher instruction identified in this task fixes this particular raw-download mechanism. Transparently revising a work-only transport protocol, while preserving its actual failed attempt and retaining precise acceptance boundaries, is not a bypass of user permission or project CI/tests. The frozen concrete V3 implementation and its actual future results still require their separate review; this note gives the conceptual judgment only.

## Review seal

The independent read-only inspector checked 12 stable inputs, recomputed the returned and expected raw identities, verified the retained verification-commit binding and confirmed that every artifact in the earlier conditional-admission seal is unchanged. It passed on its first execution. It performed no remote read/write, owner-script execution, source edit, build, test, recipe, Java or native command.

`REVIEW.json` contains exact per-response byte counts, hashes and normalized transport payloads. `INPUT_SEALS.json` binds the additive failure observation and unchanged earlier admission. The actual original raw-read gate remains failed; a future separately admitted V3 custody route must preserve that fact.
