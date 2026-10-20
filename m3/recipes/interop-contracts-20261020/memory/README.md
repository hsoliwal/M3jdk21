<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Interop recipe correction memory

This is the recipe-owned immutable history for `com.synexia.m3jdk.InteropContracts20261020`, under `synexia-openrewrite-recipes/recipe-crates/m3-interop-contracts-20261020`. It applies the Synexia M3 invariant `SYNEXIA-MOP-RECIPE-MEMORY-1` to the concrete owning-module failures discovered after the main runtime recipe. It introduces no global store, deployed serializer, runtime integration layer, or authority to merge or promote a phase.

The final local candidate has **13 files: 11 existing Java test sources, one overview, and one binding TSV**. No production owner, public API, POM, native implementation or existing strict gate is changed by this follow-up. Its frozen candidate-set SHA256 is `05f397c6181992c354c6113aca7e2f9039bb8c835bef8e5c14a286ec526176eb`. The digest is over sorted UTF-8 records `postimageSha256 + "  " + repositoryRelativePath + LF`. The original baseline is `2779aba3f98f31c324be43f731be7914e1a05935`; this is not a claim that the later actual receiving parent remains that commit.

## Four required facets

**Atomize.** Each source-changing event identifies semantically meaningful per-file atoms, exact before/after artifacts, unchanged contracts and dependencies. The general Spring Outcome helper and its smaller successor are both preserved. A syntax-only intermediate is mechanically reconstructed from its exact preimage and checked against the SHA256 originally captured in INPUTS.json; it is labelled as a reconstruction. No arbitrary atom-count limit is imposed.

**Patternize.** Events include problem categories, exact matching conditions, exclusions and refusal rules. `../catalogue/INTEROP_EVALUATION_20261020.tsv` covers every one of the 13 target operations. `../catalogue/DONOR_EVALUATION_20261020.json` compares pinned donor behavior against the existing M3 contract. `../catalogue/PRIOR_PROBLEMS_20261020.tsv` reuses earlier GitHub/LeetCode/HackerRank/GeeksforGeeks discovery metadata; challenge links do not confer copying rights and no challenge implementation is imported here.

**Annotate.** `../RECIPE_MEMORY.tsv` is the required searchable index. `EVENTS.sha256.tsv` links all 13 events by digest, parents and supersedes. Every event retains its trigger, observed outcome, root-cause certainty, correction, rejected alternatives, source differences, commands where captured, dependency pins and bounded replay conditions. The event's own SHA256 exists only in its filename/index, never in its hashed content.

**Document.** `LESSONS.md` records reusable conclusions and limits. All known compile/runtime failures, the superseded Spring helper, the initial12 review HOLD, the13 successor, stale-class evidence and pre-execution disk refusals are retained. Failures remain failures; compile-only success remains compile-only; later successful observations never rewrite earlier events.

## Verification and admission boundaries

The final **local** Java and required-native owning runs each executed159 tests with zero failures, errors or skips. Both modes separately passed the152-check binding main, and each ran the existing Guava26421-check/6400-step corpus after explicitly verifying expected backend availability. Source/class freshness, dependency seals and packaged TSV equality are recorded under `../evidence/interop-correction/attempt-09-*` and `attempt-10-final13-additional-gates.json`. Every Java class hash is identical to the prior12-target qualification; the13 successor changes a packaged metadata resource.

These events capture their historical scope with canonical SDK, actual receiving and publication admission ungranted. Later SDK and receiving results belong in separate immutable `memory/observations/` receipts authored by those gate owners. Reading a successful local record grants no merge authority or phase promotion. The baseline and final per-file resources are canonical crate-relative references; historical absolute argv/report paths remain unchanged inside original receipts.

## Serialization and append rules

The event bytes use the bounded `MOP-RCM/1.0` canonical JSON profile and the closed schema in `EVENT.schema.json`: UTF-8, no BOM or final LF, ASCII keys sorted recursively, no duplicate/unknown fields, scalar Unicode without normalization, lowercase six-character control escapes, int64-only numbers and sorted unique digest links. `PROFILE_REFERENCE.py.txt` contains the selected existing one-off authoring functions reused for this packet. `PROFILE_VALIDATION.json` reports the concrete positive/negative checks applied to these 13 records. This is static task evidence, not a claim that a general-purpose serializer has been deployed.

Append a new digest-named event for a correction. Link known parents/superseded events and leave their bytes intact. Record exact pins when observed; use UNKNOWN with reasons for uncaptured full SDK closures or precise mutation instants. Append separately captured gate observations instead of changing historical admission fields. The original12 frontier, reports and independent review remain available beside the13 successor.

## Retained evidence and donor custody

All original textual logs, Surefire reports, inspected source fixtures and disassembly are under `../evidence/interop-correction/`. `RETAINED_EVIDENCE_INDEX.json` records the initially staged evidence subset; `PACKET_SHA256SUMS` seals the final packet excluding itself. Compiled class binary bodies are retained in scratch and are not published in this text packet; their exact SHA256/mtime metadata and relevant disassembly are preserved. Two sandbox refusal messages were retained as tool-result observations and recorded retrospectively in `ENVIRONMENT_REFUSALS.json`; original stdout files were not available. The cause of old target files reappearing remains unresolved.

The Spring bytecode disassembly is donor-derived Apache-2.0 evidence from the pinned spring-core6.2 JAR and retains the exact donor LICENSE and NOTICE in the crate. Copied M3 owners remain first-party preserved history with exact source-origin mapping, including the main-recipe-received M3IndexMaskCompactor revision. Newly authored probes are isolated evidence only. No donor production source body was adapted into product code in this follow-up, and no performance improvement is asserted.
