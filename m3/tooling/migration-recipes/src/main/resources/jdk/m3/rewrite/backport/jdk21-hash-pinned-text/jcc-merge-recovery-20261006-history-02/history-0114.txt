# E4 derivation/binding correction review — T02

**Both T01 findings are resolved at source level for these exact revised bytes. No further blocker was found in this narrow correction review.** This does not admit bound resources or claim executed refusals, tests, derivation, authoring or receiving qualification. All inputs remain UNBOUND.

| Reviewed input | SHA-256 |
| --- | --- |
| `run_verification.py` | `c817f482570b292f54b04f7ec6f0e6025df30a1ad5ef4b02c44f1d47f01a2232` |
| `author_e04.py` | `8773314a48caf6072480c030a14bd794e30f7ea216895b02d33b359b3159631f` |
| `UNBOUND_READINESS_03.json` | `7c55917967ca23fa0afa64b51441e029f9a1dfa99e3403252d2bd9b18b6619be` |

## E4-DER-01 resolved: declared context admission precedes verification output

The runner adds explicit admission for all 527 unique current-context rows. It checks repository and be92 commit, canonical relative repository paths, exact local paths under the immutable baseline root, regular files and symlink refusal. It compares each actual body with all three declared content identities: byte length, SHA-256 and Git blob. Every context row must declare Git mode, which is checked using the actual executable bits. The baseline-root expression resolves to the intended task-root `recovery-baseline-be92` directory.

All ten current-context receipt copies receive the same content-identity checks under their exact receiving `source-current/` paths. Receipt modes are checked when declared; these ten rows have no declared mode, so their actual permission modes are frozen without inventing a prior mode assertion.

The helper checks file metadata around its read and against the final path observation, then returns the identity computed from that same byte read. Those returned identities become the frozen entries, avoiding an unverified second read that could substitute a different pre-start body. Both admission functions and construction of the complete input snapshot precede `OUT.mkdir()`, `inputs.json` writes and every subprocess/gate. The existing final equality loop still checks the admitted bodies and copies against their frozen identities.

This correction establishes the required check in the source. No candidate function was invoked and no negative-case test was executed by this reviewer. T01 independently established that all 527 actual bodies and ten copies matched their declarations at its review time; T02 does not relabel that observation as executed runner admission.

## E4-DER-02 resolved: receipt destinations are admitted before authoring writes

`admit_receipt_targets` rejects empty/dot/traversal/absolute/backslash and noncanonical names before reserved-name comparison. Consequently the reported `./PUBLICATION_READBACK.json` alias is rejected, and canonical reserved names cannot override the three mandatory receipts. All normalized destinations are checked for uniqueness and pairwise ancestor/descendant file conflicts, including a reserved filename used as a parent. Existing destination files, destination symlinks, symlink parents and existing non-directory parents are refused during authoring admission.

The author calls validation with `receipts_absent=True` before output generation and calls destination admission again before forming evidence links. Both calls precede the first afterresource, plan, manifest, Java sentinel or copied-receipt write. Ordinary I/O failures and concurrent filesystem mutation are outside this source-level preflight claim; no transaction or universal atomic-write guarantee is inferred.

The runner's call uses the new default `receipts_absent=False`, correctly matching its post-authoring lifecycle. Each required authored receipt must already exist as a regular nonsymlink file and match the hash-bound original. This avoids fixing authoring admission by accidentally preventing verification of the authored packet.

## Preserved behavior and evidence boundaries

The complete minimal diffs are preserved beside this report. Changes are limited to the two admission helpers/call sites, same-read frozen identities and two explanatory receipt flags. The source derivation, exact source verifier contract, 19-owner mappings, 46-record/44-other/20-gate preservation, 21 whole historical source objects, actual root/POM derivation, 12-Java/6-Python definitions, installer sequence, expected incomplete completion and blocked acceptance remain unchanged. The author still creates resources for the retained recipe/installer rather than directly modifying operational targets.

All 21 files covered by the original T01 review seal remain byte-exact. The owner's retained pre-correction tool copies equal the independent T01 copies. All readiness03 referenced file identities match. The 15 unaffected T01 source/input snapshots match their current originals, including the unchanged Java/Python test corpus, derivation, source verifier, context, template, UNBOUND inputs and four operational before-resources. This narrow review does not independently rerun the owner's complete 47-preparation/93-E3 payload inventory check.

The new crate still contains four before-resources and no afterresources or sealed plan. `AUTHORING_RESULT.json` and the verification directory remain absent. Both authoring inputs still declare UNBOUND. No candidate imports/functions/mains, generators, builds, tests, installers, validators or remote calls occurred. Inspection and independent hashing/JSON parsing/Python AST parsing are the only verification activity in this review.

Actual source publication, derived root/proof custody, concrete source outcome prose, all four afterresources, plan and explicit reference checks still require the separately planned bound-input review. Static resolution of these defects transfers no source, JNI, whole-module or JDK qualification.
