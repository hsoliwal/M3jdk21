<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Restore jdk.m3.logging: module dropped by the fold merge fc917d80ea3c (2026-11-07)

`jdk.m3.logging` (M3StructuredFormatter: one tab-separated line per `java.util.logging` record, plus its JUnit,
JIT-pass and performance tests) landed through #2234 (head `f12245e0b7a8`, "jdk.m3.logging: atom passes after
atomize - JIT, then patternize") and travelled in the receiver #2390 (head `7c23ff3612ef`). The fold merge
`fc917d80ea3c` (2026-11-06, "merge(superset): fold #2390 head 7c23ff3612ef through API_FOLD v1") recorded
`7c23ff3612ef` as its second parent but wrote the first parent's tree (`9bda130301a4`) for the module's three
prefixes and the first parent's `make/conf/module-loader-map.conf`.

## Fold evidence (CPU, reproducible)

| command | result |
|---|---|
| `git log -1 --format=%P fc917d80ea3c` | `9bda130301a4 7c23ff3612ef` |
| `git ls-tree -r --name-only 7c23ff3612ef -- src/jdk.m3.logging make/modules/jdk.m3.logging test/jdk/jdk/m3/logging` | 8 files |
| same at `fc917d80ea3c`, `9bda130301a4`, `origin/master` | 0 files |
| `git diff --stat f12245e0b7a8 7c23ff3612ef -- <3 prefixes>` | empty (the receiver carries #2234's bytes unchanged; no fallback needed) |
| `git diff origin/master 7c23ff3612ef -- make/conf/module-loader-map.conf` | `-jdk.m3.annotations -jdk.m3.graphics +jdk.m3.logging` inside PLATFORM_MODULES (7c23ff3612ef predates master's two jdk.m3 modules) |
| `git diff --name-only 7c23ff3612ef^ 7c23ff3612ef -- make/` | empty (the registration predates the receiver head) |
| `git grep -n jdk.m3.logging 7c23ff3612ef -- make/ ':!make/modules/jdk.m3.logging'` | `make/conf/module-loader-map.conf:85` only |
| `git grep -n jdk.m3.logging origin/master -- make/` | nothing |

## What the recipe does

`apply.py` (RESTORE_JDK_M3_LOGGING v1, hash-pinned, idempotent, `--check` writes nothing):

- RESTORE: the eight files are the blobs of `7c23ff3612ef`, byte for byte (`RESTORE.tsv`: blob sha1 and sha256 per
  file). In this branch they were taken with `git checkout 7c23ff3612ef -- <3 prefixes>` (no hand edits); the recipe
  re-materializes a missing one from `git cat-file blob` and refuses a present one whose bytes differ.
- REGISTER: `make/conf/module-loader-map.conf` gains `    jdk.m3.logging \` after `    jdk.m3.graphics \` in
  PLATFORM_MODULES (sorted position) and one `# //FLLDCIM: MRL` make-comment anchor above the declaration (the
  anchor cannot sit inside the backslash-continued list). Preimage and postimage sha256 are pinned; anything else is
  refused with nothing written.
- The JDK naming fixed point (`make/m3/tooling/jdk-naming/jdk_naming.py plan|apply --only <3 prefixes>`) is
  `moves=0 rewrites=0 refusals=0 absent=0 shadows=0`, so RENAME_APPLIED is empty: the restore bytes are the bytes
  master's law requires.

Chrome rule: not engaged. The content was previously landed (#2234/#2390); this is a merge-loss restore, not new
generation. The only new files are this recipe's own tools and ledgers.

| file | purpose |
|---|---|
| `apply.py` | recipe RESTORE_JDK_M3_LOGGING v1 (8 RESTORE rows + 1 REGISTER row) |
| `EDITS.tsv` | written by the recipe: file, op, before/after sha256, source blob, source commit, dropping merge, rule |
| `RESTORE.tsv` | the restore source per file: commit, fallback, blob sha1, sha256, rename state |
| `DECISION.tsv` | the decision with its evidence |
| `GATES.tsv` | executed gates with numbers |
| `gates.sh` | the compile / JUnit / differential / javap gate |
| `M3StructuredFormatterDifferential.java` | fixed-fixture driver: formatter output and escape/unescape over fixed inputs, run against both builds |

```text
python -I make/m3/qualification/restore-jdk-m3-logging-20261107/apply.py [--check]
bash make/m3/qualification/restore-jdk-m3-logging-20261107/gates.sh <repo root> <out dir on S:> [JDK21] [junit console jar]
```
