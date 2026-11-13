# M3JDK21 backport programme status — 2026-10-03

This is a dated snapshot, not a completion claim.

Regenerate repository-owned counts with:

`python3 m3/backports/program_status.py --root .`

## Released JEP denominator

The JDK 22–27 catalogue contains **82 JEP rows**.

Current disposition grouping:

- **39** — pending proof or implementation
  - 10 candidate
  - 16 candidate-high-risk
  - 5 hold-compat
  - 5 hold-jit
  - 3 hold-preview
- **43** — decided no direct backport
  - 18 reject-language
  - 5 reject-compat
  - 17 superseded
  - 2 superseded-high-risk
  - 1 superseded-jit

A pending row is not necessarily compatible; it means the compatibility/implementation proof is not closed by the catalogue.

## Non-JEP inspected seed

The current seed contains **13 rows**.

- **1 admitted**
- **12 pending proof or implementation**
  - 1 candidate
  - 1 candidate-adapted
  - 6 candidate-high-risk
  - 1 hold-dependency
  - 3 hold-javac

The seed is not the full 14,948-change denominator. The complete inventory workflow remains the denominator authority.

## Materialized packets on master baseline

At the baseline used to generate this snapshot:

- `jdk-8357439` — jcmd bash completion
- `jdk-8347112` — adapted javadoc doc-files behavior
- `jdk-8364182` — adapted VM.security_properties serviceability packet

Open work outside that baseline must not be counted as merged merely because a branch or PR exists.

Merged on 2026-10-03:

- PR #45 — JDK-8364182 security-properties diagnostic command
- PR #46 — M3JDK21 recipe DAG/Maven/Camel-Airflow-Drools control plane
- PR #47 — JDK-8359706 open-file-descriptor diagnostics

Additional merged on 2026-10-03:

- PR #48 — JDK-8374808 KeyStore creation Instant compatibility leaf
- PR #49 — JDK-8368692 password System.in compatibility policy
- PR #50 — scoped recipe-atom packet DAG composition
- PR #51 — community capability / optional module-pack admission model
- PR #52 — Camel, Airflow and Drools projections of the canonical recipe DAG
- PR #53 — current-master recovery of JDK-8359706
- PR #54 — current-master recovery of JDK-8368692

Current review lanes:

- PR #55 — JDK-8367584 JFR help compatibility leaf (draft)
- PR #56 — module-pack tooling and diagnostics-image proof (draft)

Current-tree recovery queue:

- JEP 458 / multi-file source launcher has an implementation branch
  `m3/backport-jep458-multifile-source-launcher-20261003`, but that branch is currently 9 commits
  ahead of its historical base and 54 commits behind current `master`. It is therefore evidence,
  not a promotable current-tree patch. Replay must be recipe-first against current master without
  rebasing or rewriting history.
- Older diverged implementation branches for already merged packets are retained as provenance and
  must not be counted as additional pending implementations.

## Completion boundary

Do not report M3JDK21 compatible-backport completion until:

1. the full 14,948 released-commit denominator is inventoried;
2. every row has an explicit compatibility decision;
3. every proven-compatible row is implemented or proven equivalent;
4. every accepted packet has source-pinned replay and exact verification evidence;
5. Java 21/OpenJDK build, jtreg and relevant runtime gates pass;
6. rerunning inventory/admission finds no compatible residue.

The purpose of this status file is to make remaining work visible, not to reduce the denominator.
