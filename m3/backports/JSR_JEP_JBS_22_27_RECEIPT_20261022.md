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

To enumerate **all** JBS IDs (including repeat IDs, multiple commits per issue, and untagged changes), execute existing `inventory.py` on a *complete* `openjdk/jdk` tree at the pinned GA tags, then `compatibility_queue.py`; keep commit identity, issue identity, release interval and patch dependency closure separate. This complete generated issue ledger is **not available** in this read-only audit. No claim of all-JBS classification or migration is made.

## Acceptance / next serial passes

1. Reconcile generated 14,948-commit source inventory with existing JEP/JBS rows, preserving each distinct upstream commit and JBS relationship; reject gaps, duplicates and stale tags.
2. For each compatible leaf, find or improve the **canonical Synexia** hash-pinned Maven/OpenRewrite recipe; record upstream revision, licence/NOTICE, target preimage/postimage, unchanged Java 21 API/JNI/VM behavior, fixed-point and refusal fixtures.
3. Apply serially per target file with real OpenJDK 21 compile, jtreg, JNI/native, GC/runtime and performance gates appropriate to that leaf; emit passive receiver proof of the executed result.
4. Promote only reviewed results by PR after gates execute; never equate JEP inventory, source hash, GitHub merge ancestry or queued/zero-job CI with passing product proof.

**This receipt changes no OpenJDK source, Java API, class-file version, runtime flag, recipe, Maven build, or existing authority file.** It is a point-in-time count/crosswalk of the named master snapshot. Verification performed: exact in-memory TSV row count, release grouping and disposition reconciliation. Not performed: upstream Git log replay, recipe execution, Maven/JDK image build, jtreg or native/runtime tests.
