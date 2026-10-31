<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Guava RowSortedTable: exact Synexia recipe receiver

Canonical source/recipe authority: hsoliwal/com.synexia PR #12218
https://github.com/hsoliwal/com.synexia/pull/12218
Exact canonical source commit: 137abc1c56eed1d1eb3e705e69ada5bcd442efed.
This qualification directory contains the exact ten Synexia Maven/OpenRewrite source,
fixture, receipt and catalogue files; they are not a second canonical implementation.

The three Java product source/test additions are byte-identical to the canonical
source-sealed templates and were ABSENT on the pinned M3JDK21 master preimage.
The new class implements the actual Guava RowSortedTable interface using the
existing M3TreeMap sorted row index and M3LinkedHashMap / M3TreeMap columns.
Guava Tables.newCustomTable supplies provider API cell and column semantics.
Sub/head/tail rowMap views are backed and preserve removal and comparator behavior.
No new per-cell M3 Entry or Node representation, new native ABI or Synexia runtime
dependency is introduced. Only API projection entries may be materialized.

The original Synexia authoring receipt records Java21 strict compilation and
153,557 real Guava oracle assertions over 3,200 deterministic operations.
Those are historical donor-local proof, not current target-head CI results.
Target CI must execute the real Maven/OpenRewrite recipe, source SHA validation,
strict current M3 core compilation, actual five-provider Maven/JUnit and unchanged
C17 JNI mask parity before promotion.

Guava, Eclipse, Spring, Apache and JDK remain distinct public contracts and are
not asserted to be fully reimplemented. Product collection phase is still gated
by String -> arrays -> collections -> AST/compiler.

Original first-party work Copyright 2026 Hitesh Soliwal and contributors /
Apache-2.0. Guava is used as an unchanged Apache-2.0 provider dependency.
Upstream API source is not copied and LeetCode/HackerRank/GFG entries remain
problem-category reference evidence only.
