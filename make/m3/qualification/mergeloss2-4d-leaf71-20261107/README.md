<!-- SPDX-License-Identifier: Apache-2.0 -->
# Merge-loss leaf 71: compact M3TQ metadata in Matcher and flat queries

Origin 43f50e1f76e (2026-10-25) was dropped by a retain merge. It restores two compactions:

- **Matcher:** the Boolean scan verdict for a Pattern-compiled query reuses the existing Facts stamp slots while no
  Facts are retained, so four per-Matcher fields go away. `usePattern` clears the stamps when no Facts exist.
- **M3TQ:** a flat query (no subqueries) keeps no second ScanPlan copy of its keys. `ScanPlan.testFlat` scans the
  original sorted key plane.

Gates: GATES.tsv. Matcher runtime is NOT_RUN on a stock VM, because it needs M3JDK java.lang.
