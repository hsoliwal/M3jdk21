<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact-commit publication provenance adapter

This plan precedes adapter authoring. Only the coordinator executes the adapter,
after creating the final unreferenced Synexia commit. This is bounded publication
orchestration around the unchanged provenance checker, with no recipe, product,
compiler, test, benchmark, remote mutation or full-build role.

## Inputs and refusals

The CLI accepts `--task-root`, `--objects-json`, `--manifest`, `--base-sha`,
`--head-sha` and `--output-dir`. Output must be a previously absent child of
`task-root/publication-v2`. Input JSON has exactly `commits` and `trees` lists of
raw Git API commit objects and nonrecursive, nontruncated tree objects. Reject
unknown fields, duplicate JSON keys/object identities, unsafe paths, malformed
SHA-1/SHA-256 values, unsupported changed modes, missing required objects and
source drift. The committed chain must reach the explicit base; base is the
only shallow boundary. Unchanged opaque subtrees may remain unhydrated.

Use the established final manifest list with `repo: syn`, `path`, `source`,
`sha256`, `before_sha256` and optional `status`, `bytes`, `git_blob`, `phase`.
Only A/M actions are permitted. A has a null before hash; M has the exact pinned
input before hash. The complete manifest must include the global provenance
ledger and all documentation/registry changes. No filtering of foreign entries
or implicit omission of paths is allowed.

## Exact Git object construction

Reconstruct unsigned commits from the API tree, ordered parents, name/email,
UTC epoch timestamps with `+0000`, and the actual message. Try only the supplied
message and its single terminal-LF alternative, accepting an exact object hash
match. For signed commits, validate the API `verification.payload` against the
metadata, insert its exact multiline signature as a `gpgsig` header and require
the supplied commit SHA. This verifies object identity, not cryptographic signer
trust. Unsupported extra commit headers are refused.

Encode tree entries in Git byte ordering, treating directories as names ending
in `/`; normalize API `040000`/`40000` to Git `40000`. Verify every supplied
commit/tree object SHA in memory before any `git hash-object -w`. Compare the
base/head trees recursively wherever identities differ, retaining unchanged
opaque subtree identities, and require the exact manifest A/M path set.

## Partial checkout and unchanged gate

Verify pinned checker SHA256
`55e8183c58e4bf3608355e650e3be18609feab70f7baa75c935716b396d24900`
and policy SHA256
`4a3bbc60b2960774058c8a822999e8668239c9192571633480463d47d31381dc`.
The unchanged LICENSE intake has SHA256
`1eb85fc97224598dad1852b5d6483bbcf0aa8608790dcc657a5a2a761ae9c8c6`
and Git blob `29f81d812f3e768fa89638d1f72920dbfd1413a8`.
Require those files' exact blob identities and modes in both base and head.
Verify every manifest postimage against the actual head tree and every modified
preimage against the actual base tree and pinned input. Materialize only these
regular files and the unchanged authorities, without rewriting bytes or notices.

Create an isolated local Git repository, write only preverified objects, mark
base shallow, and install local base/head refs. Require both exact commit IDs
to resolve and require the unique merge base to equal the explicit base before
the checker can exercise its fallback logic. Compare actual Git `base...head`
name/status output to the manifest. Execute the unchanged checker CLI using
explicit base/head hashes, final ledger and policy. Require successful exit,
zero violations, exact reported base/head, and exact `PROVENANCE_FILES.tsv`
status/path/hash equality including the ledger. Preserve all command logs and
failures; occupied output directories are never reused or deleted.

The receipt must identify supplied object and source seals, exact refs and merge
base, checker outputs, materialized versus opaque scope, and the unchanged
checker/policy/LICENSE. This establishes the committed changed-file provenance
gate in a partial object checkout. It does not assert a complete clone, signature
trust, remote publication, full repository build or product acceptance.
