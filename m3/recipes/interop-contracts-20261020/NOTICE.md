<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Source rights and reuse

First-party recipe configuration, catalogue decisions, independent probes and documentation are Apache-2.0. Exact first-party licence text is retained in `licenses/Apache-2.0.txt`; the main predecessor notice is preserved in `upstream-host/NOTICE.md`. Modified 2026-10-20 for this interop test and metadata successor.

The existing four SDK engine/support sources are pinned in `ENGINE_SOURCES.tsv`. The existing qualification test and receiver are copied byte-for-byte from the main canonical runtime-convergence crate. `SOURCE_PROVENANCE.json` identifies their source commit and Git blobs. Original source notices remain unchanged, including notices in preserved evidence and every target preimage/postimage. Recipe licensing does not relicense receiving payloads or external evidence.

Apache Commons Collections 4.5.0 and Spring Framework 6.2.0 source was inspected to establish API and concrete behavior. Exact release commits, source paths and Git blobs are recorded in the independent donor source pin records. These sources are Apache-2.0. No challenge solution or donor runtime algorithm is copied into the target changes. The donor APIs and binary dependencies remain the receiving module's existing declared dependencies.

The retained Spring `javap` output in the correction evidence is **donor-derived disassembly**, not first-party implementation text. Its exact-path provenance remains attached to that log. `licenses/Spring-6.2.0-LICENSE.txt` and `licenses/Spring-6.2.0-NOTICE.txt` are the exact original `META-INF/license.txt` and `META-INF/notice.txt` from the pinned Spring 6.2.0 JAR, without edits. Other source, class and JAR digests identify observations; binary class/JAR snapshots are not included in this crate.

LeetCode, HackerRank and GeeksforGeeks references in the catalogue are metadata links and category summaries only. No challenge body, editorial, submission, or tutorial source is redistributed. The exact inherited runtime catalogue preserves its original Guava/Eclipse/Boost reference-only and rights dispositions; this follow-up does not broaden their reuse authority.

`provenance/PUBLICATION_SCOPE.md` records the public receiving scope. No private convergence concept or new runtime dependency on Synexia is introduced. Existing public engine copies, already-public interop tests and owner metadata are the receiving source boundary.
