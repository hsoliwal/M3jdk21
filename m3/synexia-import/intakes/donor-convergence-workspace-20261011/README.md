# Synexia donor-convergence workspace intake — 2026-10-11

Status: **exact Apache-2.0 source custody only; no product acceptance**.

This intake makes the canonical recipe/mastery source and registry visible to the M3 receiver without declaring the source module complete or creating a runtime dependency.

## Pinned source

- repository: `hsoliwal/com.synexia`
- commit: `2be201d6ff52ac563bee1f6848ff8d2d436fe5c8`
- recipe tree: `f24f249896ec3b569a4a4a73e04c0423d7412650`
- source assets and Git blob identities: `SOURCE_PROVENANCE.tsv`
- receiver role and holds: `TARGET_MAP.tsv`
- Synexia policy restoration PR: [#10260](https://github.com/hsoliwal/com.synexia/pull/10260)

## Scope

The packet contains the canonical recipe-home index and two source entrypoints: recipe mastery and the read-only donor-convergence workspace program. Both Java files retain their `SPDX-License-Identifier: Apache-2.0` header. The shared vendor `LICENSE` and `NOTICE` are retained.

## Full-estate mapping

All three custody rows bind to the existing `OPENREWRITE_RECIPES` family in `m3/synexia-import/current-full-borrow/synexia-estate.tsv`. This is a partial materialization of that family; the estate row remains `SOURCE_PIN_ONLY` until its complete export and receiving obligations are satisfied. No second family or canonical owner is created.

## Deliberate limits

- This is not the full 34k+ observed recipe tree.
- The two entrypoint files are not compiled here because their complete source/dependency closure has not been mirrored.
- A canonical-index owner blob mismatch is recorded as HOLD; it is not silently rewritten in M3.
- No Java/HotSpot/JNI runtime source, public API, or promotion state changes.
- No target family is marked accepted from source hashes alone.

The next source-side action is to correct the stale canonical recipe-home owner pin through Synexia's Maven/OpenRewrite recipe workflow, then export the corrected snapshot with new provenance. M3 keeps the exact source revision and target status in this intake; it does not become the canonical recipe owner.
