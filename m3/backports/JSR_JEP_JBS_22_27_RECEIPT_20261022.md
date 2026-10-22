# JDK 22–27 JSR/JEP/JBS receiver audit — 2026-10-22

**Kind:** passive, read-only compatibility and provenance receipt for M3JDK21; **not** an implementation recipe, independent catalogue, or promotion decision.

**Source snapshot:** `hsoliwal/M3jdk21@b1cc8ab32eaef5b01582623b100da80946fd9346` (`master`, read 2026-10-22). Java 21 source, public/ABI contracts, and default class-file major version **65** remain the target. The separate Synexia recipe and target JDK image/jtreg/JNI gates remain mandatory.

## Release and platform-specification crosswalk

| JDK donor | Java SE JSR | Delivered JEP entries | GA tag | Programme-reported upstream commits in interval | Donor class-file major |
| --- | --- | ---: | --- | ---: | ---: |
| 22 | [JSR 397](https://jcp.org/en/jsr/detail?id=397) | 12 | `jdk-22+36` | 2,384 | 66 |
| 23 | [JSR 398](https://jcp.org/en/jsr/detail?id=398) | 12 | `jdk-23+37` | 2,355 | 67 |
| 24 | [JSR 399](https://jcp.org/en/jsr/detail?id=399) | 24 | `jdk-24+36` | 2,562 | 68 |
| 25 | [JSR 400](https://jcp.org/en/jsr/detail?id=400) | 18 | `jdk-25+36` | 2,678 | 69 |
| 26 | [JSR 401](https://jcp.org/en/jsr/detail?id=401) | 10 | `jdk-26+35` | 2,611 | 70 |
| 27 | [JSR 402](https://jcp.org/en/jsr/detail?id=402) | 9 | `jdk-27+35` | 2,358 | 71 |
| **22–27 total** | **6 platform JSRs** | **85** | Baseline: `jdk-21+35` | **14,948 commits** | M3JDK21 remains **65** |

Each JSR defines a newer complete Java SE platform; **none is wholesale merged** into a Java 21-conforming SDK. Treat its compatible fixes/API leaves as individually reviewed candidates. The JEP count includes previews and successive revisions of one feature; 14,948 are upstream *commits*, **not** 14,948 distinct JBS issues or accepted backports. The upstream commit interval counts are quoted from the existing [backport programme README](README.md), **not independently rebuilt in this receipt**.

## Exhaustive JEP intake, not exhaustive accepted implementation

| Existing catalogue disposition | JEP rows | Receiver consequence |
| --- | ---: | --- |
| Ordinary candidate | 10 | Source/recipe/contract and runtime proof still required |
| High-risk candidate | 19 | Dependency closure, native/VM/API proof required |
| Java 21 compatibility hold | 5 | Do not switch locked behavior by default |
| JIT/vector hold | 5 | ABI, compiler and platform-specific proof required |
| Preview hold | 3 | Research/opt-in only; not default final Java 21 API |
| Rejected new language grammar | 18 | Cannot silently change Java 21 source grammar |
| Rejected compatibility break | 5 | Preserve Java 21 supported API/behavior |
| Superseded JEP lineage (all types) | 20 | Reconcile with final successor, do not import duplicates |
| **Total** | **85** | **0 JEPs promoted by this audit** |

Authority: [released JEP inventory](RELEASE_JEP_AUTHORITY.tsv) (Git blob `745a26772b8050c28dd93d05c724bfa9c2ad5e18`), [85-row JEP catalogue](JEP_CATALOGUE.tsv) (blob `a0ef9aa95da17a19c3df8ab8aded355fad4f4598`), [JEP residue queue](JEP_RESIDUE_QUEUE.tsv) (blob `82ef31430a0b1f3f85892ce4b00f3d2e06eb96f5`), and existing [all-JEP proof matrix](ALL_JEP_MATRIX.md). The matrix explicitly marks every row `NOT_AUTHORIZED_BY_MATRIX`.

The current priority queue starts with JEP **423** (G1 region pinning), **458** (multi-file launcher), **474** (generational ZGC default, opt-in), **484** (Class-File API extension) and **485** (Stream Gatherers extension). These are *candidate review order*, not claims of Java 21 acceptance or successful native tests. Keep the String → arrays → collections → AST/compiler receiving sequence and product gates intact.

## JBS coverage and actual tool leaf

| Audited JBS seed status | Issues | Evidence |
| --- | ---: | --- |
| Admitted additive tooling | 1 | JDK-8357439, jcmd Bash completion |
| Adapted candidates pending proof | 7 | JDK-8367584, JDK-8364182, JDK-8359706, JDK-8368692, JDK-8374808, JDK-8347112, JDK-8284315 |
| High-risk candidates | 2 | JDK-8378559, JDK-8347938 |
| Dependency hold | 1 | JDK-8374348 |
| Java 21 javac/source-behavior hold | 2 | JDK-8371873, JDK-8371683 |
| **Inspected seed total** | **13** | Only a seed; **not** all post-21 JBS issues |

Authority: [upstream JBS seed file](UPSTREAM_CHANGE_SEEDS.tsv) on the same `master` snapshot (the relevant JDK-8359706 update-line repair is JDK-8380236). The actual jcmd completion postimages for JDK-8357439 are present at `make/modules/jdk.jcmd/Copy.gmk` and `src/jdk.jcmd/share/conf/bash-completion/jcmd` on that snapshot. **The existence of source is not a claim that image/JTReg/shell integration tests passed.**

A complete upstream commit inventory was subsequently executed against the exact GA tags (details below). Its JBS-ID extraction is **not yet canonical**: normalization of the actual upstream subject conventions requires a Synexia recipe correction and verified regeneration. Do not equate subject tokens with independently confirmed JBS issue records, or merge any unverified source.

## Executed upstream JBS census and proof-blocker supplement — 2026-10-22

This supplements the earlier read-only inventory; it grants **zero** additional source/backport acceptance.

| JDK donor | Release-interval commits independently verified | Distinct candidate issue IDs in that interval |
| --- | ---: | ---: |
| 22 | 2,384 | 2,401 |
| 23 | 2,355 | 2,370 |
| 24 | 2,562 | 2,590 |
| 25 | 2,678 | 2,699 |
| 26 | 2,611 | 2,648 |
| 27 | 2,358 | 2,380 |
| **Total commit rows** | **14,948** | **14,726 distinct across all intervals (deduplicated)** |

**Executed, read-only provenance:** complete commit/tree metadata from `openjdk/jdk` GA refs `jdk-21+35` through `jdk-27+35`, run through existing `inventory.py` and `compatibility_queue.py`. The raw inventory has 14,948 rows and SHA-256 `1d4adeb2ba00868d59c944a3d3582652ef1911ba4fb96b39d5950b730cd77151`; its raw 14,948-row compatibility queue has SHA-256 `9552a969b0869f5087b79c182815379427f4115262441b5af9480faa594bacc8`.

**Discovered JBS-ID intake defect:** 14,420 upstream subjects begin with a bare numeric issue marker (e.g. `8305104:`); current `inventory.py` recognizes only explicit `JDK-`-prefixed tokens and therefore supplies issue IDs on just **657** of 14,948 commit rows, spanning **561** distinct IDs. A noncanonical, read-only subject normalization of both forms and additional colon-delimited issue markers produced **14,726 candidate distinct IDs**, with 17 unlinked `Merge`-subject commits. These counts are derived from commit subjects, **not** verified against all JBS database records.

Candidate normalized inventory: SHA-256 `7defa67185f4bb1c695ac3add006ede120153a4a246a00bdfc8c770a12ae74df`; candidate compatibility queue: SHA-256 `869c04d59d153f965809b2654311a032da3f04ed46e7d4dccd2a9de989a3c3fd`. All **14,948** queue rows retain `PENDING_COMPATIBILITY_PROOF`; mechanically comparing raw and candidate queues showed only the JBS-ID field changed on 14,931 rows, with the 17 merge rows unchanged. Both candidate files are **unadmitted local evidence**; they are deliberately not committed as a new public target catalogue.

**Second repair needed:** the raw inventory contains a 191,224-character TSV line. The unmodified compatibility queue failed at Python's 131,072-byte default CSV field limit; a bounded process-local 262,144-byte field limit allowed generation. Canonical input-size handling and a regression fixture must be repaired and executed in Synexia; this run did not edit public programme source.

**Actual focused checks:** `bash m3/backports/recipes/jdk-8357439/test_jcmd_completion.sh` **PASS** (Bash completion stub contract). The 198-test Python backport suite ran: **196 PASS, 2 FAIL**. Both failures were JDK-8364182 source-lineage gates: `src/hotspot/share/classfile/vmSymbols.hpp` is now SHA-256 `7e65d715fb0098213a28ec63e5c1150699062716eecf9e8db32bf8366df3fa08`, not the receipt's accepted target/superseded hashes. Current source still includes the security-properties symbol and added M3 String symbols; this is **not** an equivalence proof. A later [M3 String owner source commit](https://github.com/hsoliwal/M3jdk21/commit/4402dca8d07dc7a98eb70bc6d003a78a1f08803f) touches this file. Hold this JBS packet for independent source-history, canonical recipe and HotSpot/JNI tests.

**Next action:** repair these two ingestion/proof failures through the Synexia-owned source-sealed Maven/OpenRewrite/text recipe with adversarial numeric-prefix/multi-ID, wide-TSV and current-source fixtures, then replay against M3JDK21 and run authoritative product gates. No direct source acceptance or branch merge is authorized by this receipt.

## Acceptance / next serial passes

1. Reconcile generated 14,948-commit source inventory with existing JEP/JBS rows, preserving each distinct upstream commit and JBS relationship; reject gaps, duplicates and stale tags.
2. For each compatible leaf, find or improve the **canonical Synexia** hash-pinned Maven/OpenRewrite recipe; record upstream revision, licence/NOTICE, target preimage/postimage, unchanged Java 21 API/JNI/VM behavior, fixed-point and refusal fixtures.
3. Apply serially per target file with real OpenJDK 21 compile, jtreg, JNI/native, GC/runtime and performance gates appropriate to that leaf; emit passive receiver proof of the executed result.
4. Promote only reviewed results by PR after gates execute; never equate JEP inventory, source hash, GitHub merge ancestry or queued/zero-job CI with passing product proof.

**This receipt changes no OpenJDK source, Java API, class-file version, runtime flag, recipe, Maven build, or existing authority file.** Its receiver snapshot remains the exact original `master` SHA above. Verification: 85-JEP TSV cross-check, all 14,948 upstream GA commits and candidate queue reconciled with exact hashes, and Bash completion smoke as recorded below. Not performed: canonical Synexia recipe execution, Maven/JDK image build, jtreg, JNI/HotSpot runtime or performance testing.
