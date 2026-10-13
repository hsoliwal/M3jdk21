<!-- SPDX-License-Identifier: Apache-2.0 -->
# Tests to run: m3/ports/swing-viewport

Status per row: RUN (with result), NOT_RUN, or PENDING_LINUX. Memory gate before every JVM: commit headroom at least
8 GB, one JVM at a time, `-Xmx2g` or less (the sized tree run uses `-Xmx256m`).

| # | What | Command | Status |
|---|---|---|---|
| 1 | Residency rename recipe (#826) applied, then re-run for the fixed point | `java ResidencyRename.java RENAME_MANIFEST.tsv <swt@136df9c6> <M3jdk21 root>` | RUN: `CHANGED 21 OF 21`, then `CHANGED 0 OF 21` |
| 2 | Engine compile, `javac --release 21 -Xlint:all` | `run-tests.sh compile` | RUN: clean |
