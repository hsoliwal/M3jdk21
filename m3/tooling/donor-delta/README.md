<!-- SPDX-FileCopyrightText: 2026 Synexia contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK donor-delta admission candidate

**Scope:** additive, JDK 21, offline, read-only source analysis. This is *not* a patch merger or an optimizer.
The JDK 21 source/binary/behavior contracts and existing JNI ABI remain unchanged.

`M3DonorDeltaPlanner` accepts one locally available donor Git checkout and two **full immutable commit IDs**:

```text
M3DonorDeltaPlanner <donor-checkout> <upstream-ancestor-commit> <donor-commit>
                    <donor-id> <output-directory>
```

Only use the two-revision mode for a downstream fork where Git proves that the upstream revision is
an ancestor of the donor revision. This is a Git ancestry test, not a claim that the first revision
is the most appropriate or complete OpenJDK merge base. The exact baseline must be selected and
reviewed separately. Do not use this mode for Harmony, OpenJ9, OMR, Graal, ART or proprietary JET:
these belong in the architecture/reference catalogue and require a different comparison method.

The planner reads the exact Git trees and emits **CANDIDATES.tsv** plus **RECEIPT.tsv**, strictly ordered
by source path. It does not read or modify the worktree, fetch external networks, apply patches, import
foreign source, or infer semantic equality. Changes include `ADDED`, `REMOVED`, `MODIFIED`, `MODE_CHANGED`,
`TYPE_CHANGED`; moves remain distinct delete/add candidates until specifically reviewed. Submodule
gitlinks and symlinks remain visible, not silently treated as source code. Source identity is by immutable
Git object ID; SHA-256 of source bytes and per-file licences are **NOT_RUN** at this inventory stage.

Every candidate has `HOLD_UNQUALIFIED` and `REVIEW_ONLY` dispositions. No catalogue, Git SHA, source
similarity, public fork, noncommercial status or algorithm category grants source import, API mutation,
behavior substitution, JNI acceleration or release authority.

## Later stages (outside this leaf)

1. Select a baseline against OpenJDK 21u with known ancestry and per-file licence/NOTICE review.
2. Map the candidate to the current M3JDK owner and stable behavioral atom, full API/JNI contract and
   existing implementation. Consult the Synexia catalogue and LeetCode/HackerRank/GfG *categories*.
3. Bind source SHA-256, contract tests, negative/counterexample corpus, donor provenance, benchmarks.
4. Run the canonical Synexia Maven/OpenRewrite recipe on exact preimages, then lint, compile, test,
   interpreter/C1/C2, JNI/ABI/runtime, performance, and fixed-point verification in order.
5. Deliver only exact qualified postimages to M3JDK. Keep this tool and the recipe separate; do not
   copy canonical Synexia recipe sources into the public product.

The generated output directory is write-once: exact repeated outputs are idempotent, different bytes
refuse without overwrite. UTF-8-invalid, unsafe, ambiguous and tab/newline-bearing Git paths refuse
instead of creating unparsable TSV. Errors or unavailable Git objects refuse. Output does not include
source contents or performance results.

## Local standalone verification

Run `bash verify.sh` with JDK 21, Git, and a writable temporary directory. It compiles with
`--release 21 -Xlint:all -Werror`, performs synthetic Git topology/delta and refusal tests, and
checks byte-for-byte fixed point. The result is a **host tooling** receipt, not an M3JDK build.
