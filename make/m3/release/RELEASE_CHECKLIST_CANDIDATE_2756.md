<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Release checklist: `m3jdk-21.0.14-m3.1-preview`

**Current candidate: HOLD.** Prepare and review exact source, naming,
documentation and execution evidence before publication. These steps preserve
existing ancestry and evidence requirements. The [candidate notes](RELEASE_NOTES.md)
state the present coverage and pending product gates.

## 1. Freeze the candidate identity

| Parameter | Required value |
| --- | --- |
| Display name / machine product | `M3JDK 21` / `M3JDK21` |
| Version | `21.0.14`, matching `make/conf/version-numbers.conf` |
| Preview tag | `m3jdk-21.0.14-m3.1-preview` |
| Release branch | `release/m3jdk-21.0.14-m3.1-preview` |
| Upstream baseline in current contract | `890adb6410dab4606a4f26a942aed02fb2f55387` |
| Preparation baseline | `b9cc756f726de31af10652b5583d9bba58ba27b2` |
| Final candidate commit and tree | Record actual received source identity; never substitute the preparation baseline |
| Integration candidate (2026-11-09) | `1528a255c477` on `release-candidate/m3jdk-21.0.14-m3.1-preview-integration-e14d2c9b`; tree `59ea4931c1c4` = `master` `e8e1e837a640` + the open fix PRs; 7 of 11 gates PASS; admission **HOLD**, see [RELEASE_PREPARATION.md](RELEASE_PREPARATION.md#2026-11-09-integration-candidate) |
| Measured candidate (2026-11-09) | `fe51a3c9ff2f09140be4348e24caa0b8bd9fd18c` on `release-candidate/m3jdk-21.0.14-m3.1-preview-809625db`; tree `30651922a8b3c8b7f80cb89dc04fc5ab5069f50d` = `master` `809625db76e9`; one commit over the baseline, dated 2026-11-09 (cut date). Admission: **HOLD**, see [RELEASE_PREPARATION.md](RELEASE_PREPARATION.md#2026-11-09-measured-candidate) |

Use an isolated full checkout of the final receiving commit. Record `git rev-parse
HEAD`, `git rev-parse HEAD^{tree}` and `git status --porcelain`. Receipts must bind
the clean final source. Preserve append-only source history; do not delete/rebuild
a release branch or manufacture a backdated commit.

The existing contract still requires `single_fork_commit_required: true` and
exact upstream ancestry. This preparation does not change that gate. A candidate
that fails it remains HOLD until an explicit reviewed policy/ancestry resolution;
naming fixes and successful tests do not override it.

## 2. Check names, schema, documentation and release tooling

```sh
python3 -m pip install -r make/m3/migration/requirements.txt
python3 make/m3/tooling/name-contract/check.py
python3 make/m3/tooling/name-contract/audit.py --strict
python3 -m unittest discover -s make/m3/tooling/name-contract -p 'test_*.py' -v
python3 -m unittest discover -s make/m3/release -p 'test_*.py' -v
```

Keep the stable `migration.records[].id` values, exact current package/module
names, historical lineage and pending states. Preserve public Java/JNI/ABI
contracts, including both `toAdapter` and **`toAdpater`**. Verify that notes use
the configured version and distinguish source/tooling checks from runtime
coverage. Record raw commands, environment, results and artifact hashes.

## 3. Receive the canonical execution result

Execute the existing source-sealed recipe in `hsoliwal/com.synexia` against the
exact target preimages. Preserve canonical source seals, real SDK execution,
fixed point and drift/refusal outcomes. Transfer only verified product
postimages, target tests and data-only provenance receipts into M3JDK21.
The `M3_MAVEN_CONTROL_REACTOR` gate is owned by Synexia; product gates remain
M3JDK21-owned. The target must not execute receipt command text or copy a recipe
engine to satisfy this step.

Inventory retained legacy recipe sources and their callers before a separate
content-preserving custody cleanup. Keep `legacy_recipe_custody` on HOLD until
that migration is actually qualified. Passive lineage metadata does not waive
external checkout/build/runtime dependency blockers.

## 4. Produce current-source product evidence

Run the required foundation, precompute Java/JNI, String UTF-16, public API/module,
source-built JDK/jtreg and applicable native/platform gates on the exact final
candidate. Keep the OS, architecture, compiler/JDK, flags, raw result and hashes
with each receipt. Resolve the missing bound `STRING_UTF16_CONTRACT` producer
explicitly; do not omit the gate or substitute release-tooling unit tests.

For each selected release platform, require its exact-run JDK bundle and
matching tests. An absent or failed selected platform stays HOLD. Qualify
additional architecture/mobile/runtime claims separately. Preserve comparative
performance data before making any performance claim.

## 5. Assemble and verify the proof pack

Write the pack outside the source checkout. Use `M3_RELEASE_EVIDENCE_V1`, exact
`source_commit`/`source_tree`, all eleven mandatory gate IDs, correct owners, executed
commands, zero exits, environment records and safe manifest-relative artifact
paths with SHA-256. The canonical gate also needs its repository, canonical
commit and recipe ID. Synthetic test-fixture receipts are not admissible evidence.

```sh
python3 -I make/m3/release/verify_release_admission.py --root . \
  --json-out /absolute/path/to/proof/admission-development.json \
  --queue-out /absolute/path/to/proof/README.md \
  --sha256-out /absolute/path/to/proof/SHA256SUMS
python3 -I make/m3/release/verify_release_admission.py --root . --release \
  --evidence /absolute/path/to/proof/evidence.json \
  --json-out /absolute/path/to/proof/admission-release.json
```

Review retained-source differences, rights/NOTICE, claim text and execution
coverage. Both `policy_valid` and `release_ready` must be true. Any missing,
stale, failed or pending gate keeps publication on HOLD. A draft artifact or
packaging run does not upgrade that state.

## 6. Publication boundary

Only after admission, bind the annotated tag and release to the exact reviewed
commit. Include the current notes, evidence manifest, raw receipts/logs,
admission result, rights files and `SHA256SUMS`. Attach only bundles built and
qualified at that commit for their stated platforms. Verify names and hashes
against the proof pack. Published source and tags remain immutable.

This preparation does not create a tag, publish a release, merge a source PR,
change repository secrets or relax any mandatory gate.
