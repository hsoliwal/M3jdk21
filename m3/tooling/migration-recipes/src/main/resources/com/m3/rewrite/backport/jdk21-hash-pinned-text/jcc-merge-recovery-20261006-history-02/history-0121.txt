# Additive E4 V3 custody reader — static review T01

**One narrow runner dependency-freeze omission requires correction before receiving execution. No schema, status or exception-scope blocker was found in the rest of the additive V3 proposal.** Final source publication is pending; all source-binding inputs remain UNBOUND. This review does not admit invented publication success or final resources.

## Exact proposal

| Artifact | SHA-256 |
| --- | --- |
| `derive_source_handoff_v3.py` | `0086ac594cf4e459b0bf903e8eea95a6aa38198b5d23cb6f140726afcd08f76b` |
| `author_e04_v3.py` | `db47c8ae008021e4a250004ae8631b1a40c9cd731c5c2001f41eacedac129359` |
| `run_verification_v3.py` | `2c0cc74d7c7faa821bec1969c20aa6e41f6cfd1aa29ec67cc6187b52878f38f9` |
| `publication_custody_v3.py` | `46cf912b0f464caeb9718b08d22f9cdd5b74875e3ab614ccee19c7b0a4dae7b7` |
| Proposal readiness T01 | `69722816b7744af9f4a1a69356b797616d04d54fae7008d438cf08131685b21b` |

All snapshots are preserved under `inputs/`. The exact old derive/author/runner bodies and the prior independent T01/T02 seals remain unchanged. Independent diffs equal the proposed additive diffs. No old entrypoint or source payload is overwritten by this proposal.

## E4-V3-01: two consumed fixture bodies omitted from runner freeze (low)

The new shared reader reads the two `LOCAL_EXPECTATIONS.json` invalid-UTF8 fixture `local_path` values at lines 106–108, checking their exact lengths, SHA-256 and Git identities and computing their failed replacement-encoding observations. The V3 runner adds the expectation JSON, designated comparison, source manifest/readback, custody input and failure observation to its external freeze, but not those two fixture bodies. Both paths are in `recovery-final-replay-be92` parser resources and are absent from the 19 designated-source and 527 baseline path sets. Unless separately supplied as proof references, a change after admission escapes the final frozen-input equality loop.

This is a during-run input-preservation omission, not a missing initial fixture-hash check or a false claim that the remote malformed bytes were recovered. Required correction is finite: explicitly add exactly these two reader-consumed local paths from the pinned expectations to the runner's admitted/frozen external input set before verification output. No broad recursive source scan or new fixture exception is needed. Root and owner were notified before actual source binding; original V3 bytes are frozen.

## Producer compatibility and exception boundaries

The full reviewed producer `verify_publication_v3.py` is `45d1e83ba81dfaed9434fddb7df9f4962b7fbbb8cb1c9b29f90754c8fcf38baa`; its explicitly pinned helper is `45fe2a39cc936f94e20cf0051f3844e6d6b5ba59b87b2d3ca175bb60917603be`. Both sources were read along with the separately sealed producer review. The receiver consumes their actual schema `jcc-source-recovery-publication/1` and accepts only `PUBLISHED_CUSTODY_VERIFIED`; it does not normalize this into the former successful-readback status.

The reader fixes the source manifest, source verifier, audit and publisher hashes, input/parent/root/branch and verification flags. It rejects the old output, be92, null/dummy and probe commit identities. It compares complete verified payload rows and counts against the exact manifest and requires the four production update/two NUL direct identities. Six is a mandatory direct-body denominator, not the total observed-read cap: the observed unique body set may contain additional exact designated owners, up to the 21-owner/NUL union. The two invalid UTF8 objects are excluded from that set.

The alternate method is restricted to the exact four-byte manifest and 22-byte Java fixture by full path, byte length, local SHA-256 and Git blob. The reader retains both probe and final failed replacement-encoding observations, verifies their transformed lengths/hashes, keeps `raw_body_readback=false`, `remote_sha256_observed=false` and `all_required_blob_bodies_read_back=false`, and preserves the exact Git content-address assumption. NUL fixtures, arbitrary files, alternate malformed sequences and generic read failures cannot enter this two-object exception.

The preserved producer performs the actual create-request/response, probe/final-tree, direct-body and branch/PR checks. The receiver checks the producer receipt and bound supporting records without replacing the producer with a new verifier. Receipt provenance remains the root-owned actual-capture boundary; hashes and self-declared JSON fields do not independently authenticate fabricated tool responses. The final actual receipt and invocation remain required evidence.

## Derivation, authoring and lifecycle

The V3 derivation routes through a separate UNBOUND input and `source-binding-v3` output. Existing exact final-root body traversal and root-POM proof remain unchanged. The derived source-publication object carries the exact custody summary/status and false export authority. Author validation requires the final input and derived inventory custody blocks to equal the reader output, preserving methods/counts/failure assumptions rather than allowing a stronger rewritten summary.

The copied publication receipt is reserved as `PUBLICATION_CUSTODY.json`; the former `PUBLICATION_READBACK.json` extra name is rejected. T02 canonical destination, ancestor/symlink and authored-copy identity checks remain intact. The runner routes to the additive author, separate input and `verification-v3` directory and includes the custody block in its eventual receipt. Its 527-body/ten-receipt admission, Java/Python corpus, installer lifecycle, incomplete-completion refusal and blocked acceptance are unchanged. All 46 IDs, 44 other records, 20 gates and 21 full historical source objects remain governed by the unchanged author/tests.

The handoff template distinguishes publication custody from source execution and adds an unresolved reviewed-custody section. The custody prose correctly describes six required direct reads, any additional observed reads and exactly two failed malformed-byte reads. All concrete outcomes and evidence-carry statements remain to be supplied from actual receipts; authored definitions are not test passes.

## Draft evidence index independently checked

All 35 staged originals, totaling 1,858,597 bytes, match their source files and declared byte/hash/Git-blob/mode identities. Their planned receiving names are canonical and unique without file/ancestor conflicts. All 21 distinct direct hash-bound dependencies in the actual custody input are included. Four separate creation captures equal the complete inline custody-input objects. The exact eight local expected bodies match their declared identities; the union of 19 designated owners and six mandatory direct objects contains 21 distinct allowed direct IDs and neither exception.

The index explicitly remains partial. It does not claim that the current 35 files provide a complete remotely inspectable final custody chain. Pending items remain: actual final publication receipt and invocation, final fixture call objects, selected final tree/path evidence (or explicit local-only classification), and a final receiving index distinguishing exact files, structured-subobject extractions and omitted local dependencies. This draft is not automatically mandatory through `additional_receipts`; final bound-input/reference review must ensure that the actual carried set, links and prose match the final reviewed index. No final-copy or final-source proof is inferred from staging.

## Review limits

Independent work was limited to source/diff review, JSON traversal, byte/hash comparisons, exact snapshots and AST parsing. No candidate module or function was imported/executed; no generator, project build, test, installer, validator or remote call ran. The prior T02 reviewed scripts and all their sealed artifacts remain byte-exact. Static custody admission transfers no source-test, export, JNI, whole-module or JDK acceptance. Correct E4-V3-01, then perform the separate actual bound-input review after real source publication.
