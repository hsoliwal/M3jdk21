<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# JDK-8347112 Javadoc source succession after JDK21u merge

**Historical adaptation preserved, no new feature admission.**

The original JDK-8347112 adapted five-file receiver records a SHA-256 of
`374269a8f8913fd88954c89cb55efbf14cc93ac1ecda8c290092cddb4036ebe7` for `src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/DocFilesHandlerImpl.java`. The other four adaptation target
files still match their original SHA-256 values on M3JDK21 master.

The 2026-10-29 integrated JDK21u merge:
`hsoliwal/M3jdk21@f97e75f20250542e11e068e17036ff373450d60c`
combined product parent `3569c790d5cc3f9bc871ab0ecd025dab0fd5cafc`
with OpenJDK 21u parent `8b241db2b629e3f4c0bd06425f49950b01d6ba18`.
The resulting Java source matches current M3JDK21 master Git blob
`e86016480f297dadc2ce5c729b10e8d2594dd787`, SHA-256
`e6a27731b1d92aaae05e9c64dd231fe3b5791759fb6022a009b104bd7b176d41`.

Exact parent-to-successor source comparisons show the Javadoc-recursion
leaf `if (!configuration.shouldExcludeDocFileDir(srcfile.getName()))`
is preserved. The merged successor also has the upstream 2017–2023 copyright
notice and `isIndexable() { return true; }` override. These are
the **only** two source hunks between the pre-update product parent
and the current file. Do not revert either source line to appease
the historical checksum.

The existing `adaptation.tsv` remains **byte-identical**. Its original
`target_sha256` remains the historical proof, not an overwritten current
hash. The original `verify.py` receives a single exact successor acceptance:
target path, original target SHA, current content SHA, Git blob SHA and
unchanged recursive-copy semantics must all match. Every other historical
backport file remains strict original-hash-only. No fuzzy equivalence
or generic skip is allowed.

Target-side Java21 verification still requires original Javadoc behavior
and jtreg tests, focused JDK image, API/ABI and Maven control evidence;
the source succession check itself does **not** certify those gates.
This is a file-level provenance and proof-harness correction only.
