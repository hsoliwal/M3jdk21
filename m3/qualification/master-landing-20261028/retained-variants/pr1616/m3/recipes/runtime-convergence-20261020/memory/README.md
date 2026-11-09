<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Canonical recipe correction memory

This packet records retained correction attempts for `com.synexia.m3jdk.RuntimeConvergence20261020` under the existing canonical Synexia crate. Read `../RECIPE_MEMORY.tsv`, `LESSONS.md` and the matching immutable events before the next recipe change or application. Historical catalogue pass 02 remains unchanged.

`RECIPE_MEMORY.tsv` uses the eight columns required by M3JDK21 `m3/mop/SELF_IMPROVING_RECIPES.md`. The `observed` field is an exact line in the cited source or log. The original uncommitted correction times and correction commit SHAs were not recorded; they are explicitly UNKNOWN. Every event has its actual retrospective `recorded_utc`, exact available source/evidence seals and a precise qualification status. The enclosing future canonical commit and receiving outcome require new linked events; do not edit these historical rows or events to fill them in after the fact.

## Integrity and representation

Event payloads follow MOP-RCM/1.0 section 4.1: UTF-8, no BOM/newline, sorted ASCII keys at every object level, no optional whitespace, scalar Unicode, six-character lowercase escapes for every control character, unescaped solidus, shortest signed 64-bit integer form. These records happen to contain no numeric fields. `parents` and `supersedes` are the only set-like fields: unique, sorted, lowercase SHA-256 event identifiers. Every other array is ordered. Unknown fields/schema versions are refused by the one-off authoring validator; the closed structural schema is retained in `EVENT.schema.json`. Digest names exclude their own hash and identify exact canonical bytes. A missing parent or mismatched artifact digest prevents treating an event as validated.

`EVENT_SHA256SUMS` and `LESSONS.md` are rebuildable discovery views over the immutable event files. The root TSV is the mandatory append-only capture index. Concurrent later work must add new events with existing parents, never rewrite earlier events; a subsequent reconciliation can join parents. Publication should stage a complete validated set in one Git tree/commit. This packet does not implement a new runtime memory service, transformation engine, concurrent writer or general deployed serializer.

From the crate root, `sha256sum -c memory/EVENT_SHA256SUMS` verifies the retained static event bytes. Event artifact paths are relative to that same root. The source baselines and templates are retained in the existing recipe lanes. `recipe_versions.after.sha256` is explicitly the digest of the exact-byte SDK_INPUTS.tsv closure manifest, not an invented Git commit or a hash of only the recipe name. The manifest binds actual POM/test/contract/receiver/resources/engine inputs. Early unpreserved intermediate recipe streams remain UNKNOWN; known target sources, diffs and failed logs are retained rather than reconstructed and passed off as historical execution.

## Status and reuse limits

VALIDATED means only the declared candidate/source/SDK evidence has executed. At capture, receiving checkout qualification was pending, and no event grants target admission, repository merge authority or phase promotion. REJECTED attempts remain useful negative knowledge. The full-image configure attempt is RECORDED/BLOCKED. Later target results belong in appended events linked to these exact hashes and their actual source/environment pins. Historical 475-test module counts predate newer upstream changes and must not be substituted for current receiving counts.

`PROFILE_VALIDATION.json` records one-off checks of serialization boundaries, closed schema, parent ordering/duplication, source pins, literal observations and static event hashes. It is not a claim that a general memory writer/serializer has shipped. Only declared events are checked, not every possible schema extension. The first-party donor and OpenJDK original fixture copies preserve their original headers and license lineage; see the crate's existing donor provenance and license text. No challenge-platform solution text is copied.

Memory carries evidence and bounded applicability. It cannot override public contracts, donor licenses, receiving policy, exact input seals, or current authorization.
