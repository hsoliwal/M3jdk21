<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Interop publication adapter: independent source review

**Two source-origin corrections are required before final metadata publication.** This review covers `work-canonical-publication-20261020/prepare_interop_publication.py` at SHA-256 `4f6064e3660ded955026471f64c636fb45964b6929c1e7abac2fe79c12d8ffc9`, the existing helper and unchanged provenance checker/policy, the current canonical crate, and the parent-confirmed forthcoming packet categories. The adapter was not executed.

## Findings

### IPR-1 — P1: classify the Spring donor disassembly explicitly

At lines 95–100, every evidence `.log` becomes `FIRST_PARTY_NO_HEADER` with the canonical repository and new evidence path as origin. The planned `evidence/interop-correction/independent/Spring-MultiValueMapAdapter-javap.log` contains complete donor method bytecode disassembly. That rule misidentifies upstream-derived expression as ordinary first-party metadata, even though Apache-2.0 remains the correct donor license.

Place an exact-path override before the generic evidence rule. Verify log SHA-256 `9a3e94a883baa9c858394822323eaedd1fe9659c36506401c7662de1fb8555aa` and retain Spring 6.2.0 jar SHA-256 `3b82629da38717dd616a70ec1055f9a8e208d58132071a6314a32159f8bce162`, class path and origin. A `GENERATED` row with an explicit donor-bytecode-disassembly disposition accurately records the rendering. Retain/reference the jar's Spring NOTICE and Apache-2.0 rights. The existing checker supports this without changes.

The source inspection pin is `spring-projects/spring-framework` at `5024bb72279188f1d0dcfe19e8abdd4bdb9887c8`, source `spring-core/src/main/java/org/springframework/util/MultiValueMapAdapter.java`, blob `4c7c2f1d133cccef1eab060524a71cd3de4b45a9`. This pin supplements, rather than substitutes for, the exact disassembled jar identity. Its `META-INF/notice.txt` is available with SHA-256 `141d4305646d8e4268468641b3ace0aef3e0b8f73e0319056e12f601079e634e`. If a donor NOTICE is added under `licenses/`, the current single-license allowlist at lines 91–94 must receive an explicit corresponding custody entry.

### IPR-2 — P2: retain target origins for copied M3 owner fixtures

The same generic evidence branch assigns a canonical self-path to the known copies of `M3SpringArrayMultiValueMap.java` and `M3ApacheBag.java`. Their preserved-history/Apache classification is appropriate, but the origin fields should retain `hsoliwal/M3JDK21`, revision `2779aba3f98f31c324be43f731be7914e1a05935`, and the respective `m3/collections/interop/src/main/java/com/m3/collections/interop/` source paths.

Use finite overrides for their final packed paths and verify the already-recorded source seals. Their respective SHA-256 values are `8062247a92ee9dc90d313b715ba9158e47c53b3e7848508a5fdd54ddaaaecb96` and `5ff51272c4fd48ef11a09b9138ac2329392bb2a70e2a4fe43fab2889909d224f`. Newly authored independent probes and factual execution results can retain recorded task-evidence provenance.

## Accepted preservation and coverage

The adapter reuses the existing row helpers and unchanged checker API. Engine and historical host branches verify exact source identities; target payload branches verify manifest seals. Ordinary JSON/TSV/CSV/hash-index metadata receives explicit no-header rows, while new commentable first-party files still face the existing canonical SPDX requirement. Existing task evidence can retain its bytes without stamping historical headers. No additional runtime generation is introduced.

The prior ledger and registry inputs actually match Git blobs `a039aaa32905432b16b938fb342c89d3883f6c5e` and `60a76c5c06e1059a4f7919761b155cf5ef90b8fe`. Source inspection confirms their original byte prefixes are appended to rather than rewritten. The generated frontier consists of the new interop namespace plus the two central successors; unchanged checker/policy copies are supporting projection inputs. No main canonical source write occurs.

An independent read-only hash pass confirmed the main canonical crate still has exactly the published **318 paths**, with **zero missing, extra or changed files**. Separate `CANONICAL_13_TARGET_CUSTODY_20261020.json` verifies all 13 follow-up targets, 26 resource copies, four unchanged engines, unchanged recipe test and unchanged receiver.

## Limits

The final memory/evidence packet was still being assembled. Review the final exact inventory and classifications after those files and notices are frozen. This review does not claim a projection-gate, SDK, Maven, receiver or remote publication pass; none was executed here. Later adapter fixes need a separate follow-up against their new source hash.
