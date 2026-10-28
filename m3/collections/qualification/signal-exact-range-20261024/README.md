<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# Original M3JDK21 primitive signal exact-range receiver

This product source receives **only** Java21 output postimages from canonical private Synexia `M3SignalExactRangeIndexRecipe`; no recipe Java/YAML, templates, donor catalogue or improvement memory is packaged in M3JDK21.

- Original owners: `M3SignalColumns`, `M3SignalQualitySchema` and existing freshness owner `M3SignalDependencyEpochs`. Public Java SE API, native/JNI ABI and original array owners unchanged.
- New `M3SignalExactRangeIndex`: retain sorted int physical row IDs and missing-row IDs, not value copies or per-entry objects. Exact integer/long and Double.compare range selection, defensive result arrays, full source SHA+version+epoch stale refusal. Unknown dynamic/native dependencies require upstream conservative invalidation.
- Local Java21 strict compatible owner replay: 38,540 seeded assertions in normal and -Xint. These are not original source-built JDK/JCK/jtreg/JNI or benchmark evidence.
- Required actual original-source strict javac and owning JUnit via this workflow. Original arrays bootstrap and vendored JUnit are retained; no skipped collection tests.
- FULL M3JDK21 String, Regex, AST, M3FS, HotSpot, GC, jmods, JNI, Windows/Linux, memory/throughput and JCK/jtreg admission remain **NOT VERIFIED**. Keep PR draft.

Canonical Synexia resource SHA-256 manifest and Git blob readback are the receiving identity authority; hash metadata is not semantic equality proof.
