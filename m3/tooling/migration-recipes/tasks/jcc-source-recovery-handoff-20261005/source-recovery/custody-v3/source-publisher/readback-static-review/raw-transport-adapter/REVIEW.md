# Raw GitHub contents transport adaptation review

**ADMITTED for the documented exact-contents verification sequence.** The change adapts the readback mechanism to the actual GitHub tool capability. It leaves the source payload, final manifest, publisher, V2 verifier, final tree and final source-parent requirement unchanged. This is a static and byte-arithmetic admission; it does not claim that the pending raw-fixture or final publication readbacks have already passed.

## Frozen identities

| Input | SHA-256 or Git identity |
|---|---|
| Adapter note | `c12b3bb6415f89a94e946d8937b50e9f974e6bcf79f4f6ecdb388dc1cdfa16c6` |
| Final source manifest | `9270a6b4098960e2b7c7bd87b17325b648c4de363957f36028bacc9c1955e328` |
| Unchanged V2 verifier | `8cc5fd5c5df99ae8f48d0fbbf4618ada99be429af9cb39839511940192bd8639` |
| Raw verification plan | `00fa5b9e4e99621ed6b5aac3c0da4c6506f82e921a3da42fdd4645285653e319` |
| Capability observation | `fba19b92e8b20eb4ef2ba2c6dbc6d48d5bce01eec5763362f62324fe27ab410a` |
| Four creation-response records | `1213f1a8c1da0a7032f1dc9507c5aef152eaf94ea39db0212f3684be06778a5a` |
| Exact be92 base commit | `be92c62ece9023b5c33676716a1076d00e26120a` |
| Exact be92 base tree | `3c4f32b66633a251ba2c117090830252a7e2da03` |
| Unreferenced verification tree | `bf2b8fe43cda47f0c6363c8d376f61419e647e4d` |
| Unchanged final source tree | `0361e4a07b77a01df170523033fef01c4052ddc5` |

The publisher, audit helper and base tree model also match their previously admitted pins. The final manifest and all four raw fixture bodies match the unchanged prepared transport index. No final manifest regeneration is required.

## Observed capability and preserved failed attempt

The retained `fetch` git/blobs handler response and `fetch_blob` response contain decoded text; neither supplies the required base64 envelope. The initial adapter refused that shape. It is correctly classified as a transport-envelope refusal, not a compiler, recipe or JUnit failure.

The tool's current advertised `fetch_file` schema explicitly supports `encoding="base64"` and an explicit commit ref. A retained read-only capability probe at be92 supplies actual `sha`, `encoding` and base64 `content` fields, with no remote size. Its decoded body equals the preserved be92 `M3MasteryClassLoader.java` bytes directly and recomputes to the returned Git blob. This confirms the observed response shape for that ordinary source file. It does not establish success for the four malformed fixtures before they are actually read at the verification commit.

The original capability observation explicitly records that the very first creation response was not captured. That historical limitation remains intact. The separate later `RAW_BLOB_CREATIONS.json` contains four actual creation wrappers; their returned SHAs match all four admitted raw blob identities. Those retained records do not retroactively alter the original uncaptured-response flag. Lossless contents readbacks remain required.

## Exact verification tree and sequence

The reviewer independently reconstructed Git tree bytes from the pinned base model and the four admitted raw fixture entries, without importing or running the publisher helper. The computed root is exactly `bf2b8fe43cda47f0c6363c8d376f61419e647e4d`. All 17 changed-tree records match the prepared plan, including before/after identities, complete entry sets and unchanged sibling counts. Every existing base entry is preserved; the only leaf additions are the four manifest-approved raw fixture paths and modes.

This is an explicit change to the old transport index's “before any tree references them” sequence: an unreferenced four-fixture verification tree and commit are created before base64 contents readback. The preserved operational boundary is that **all four raw bodies are verified before any final-payload batch**, and **all four raw plus four canonical final bodies are verified before source-branch creation**. The note states this change openly rather than claiming the old literal sequence occurred.

The verification commit uses be92 as parent and receives no branch/ref/PR. It is not the final publication revision and must not become the final source commit's parent. The final batches still start from the original be92 tree and must produce the unchanged expected final tree. The final source commit still has be92 as its sole parent.

Each contents request must identify the exact immutable verification or final commit, use base64 encoding, and request the complete file without line slicing. Root owns the sequencing and actual request/response capture. Any decoded-byte, encoding, SHA or expected-tree disagreement blocks the dependent payload/branch action.

## Normalization and unchanged V2 checks

The proposed normalized blob entry copies `sha`, `encoding` and `content` from the actual contents response. It retains the complete original wrapper and requested repository/path/ref. These values must not be filled from local expectations. Because the wrapper omits size, the normalized size is explicitly derived from decoding its actual payload; it is not represented as an independently observed remote length.

The frozen V2 verifier strips its existing base64 transport whitespace, uses strict base64 decoding, recomputes the Git blob identity, checks decoded length and expected SHA-256, and directly compares every required decoded body with independently pinned local bytes. Expected manifest lengths and actual Git tree-entry lengths are separately checked. Therefore deriving the wrapper's missing size does not eliminate the expected-length or exact-body checks.

V2 does not itself validate wrapper-to-normalized-field copying or enforce the caller's pre-branch ordering. Those remain root-owned evidence and control boundaries and are explicitly named in the adapter. The resulting entries must be described as normalized contents-API responses, not unmodified git/blobs REST envelopes. Actual tree/commit/ref/PR records remain the separately required remote JSON responses.

The companion normalization review found no issue with these boundaries and remains sealed under `execution-static-review/current-owner-upstream-review/raw-transport-normalization-review`, with report SHA-256 `de9811cd42a3c2f35ed2a5f119a2409c1f6e958f87cf0517acca66cd21813da0` and artifact seal `4f13e6d91407256bee343357a4074c69d5637a18344b796191284ace7b2884f6`.

## Disposition and review limits

The limited adaptation is admitted for execution under the stated checks. The reviewer inspected 19 stable inputs and independently verified the raw-tree arithmetic, current four creation responses and ordinary-source capability probe. The read-only inspector passed on its first execution. It made no remote request and executed no publisher, source program, build, test, recipe, Java or native command. It changed no owner artifact or earlier seal.

The actual four raw-fixture contents reads, final required body reads, commit/ref/PR readback and final V2 custody result remain to be executed and recorded by root. The verification objects are custody aids only. They do not alter source execution evidence, repair the upstream failure, or promote the BLOCKED aggregate, export, destination or whole-system qualification.
