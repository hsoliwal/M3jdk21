<!-- SPDX-License-Identifier: Apache-2.0 -->
# Licence record: SWT viewport sources copied into Apache-2.0 M3 modules

Result: **every copied file is ELECTED_FROM_DUAL or Apache-2.0 at source. No file needs a relicence, no file is
REVIEW_REQUIRED, and no file contains upstream (IBM/Eclipse) content.** The per-file evidence is in
`LICENSE_ELECTION.tsv` (42 rows: 24 main sources and 18 test sources).

## Rule (coordinator, relaying the user, 2026-10-19)

- A file that is dual-licensed `Apache-2.0 OR EPL-2.0` at origin is copied under Apache-2.0 as an **election**, not a
  relicence. This is recorded as ELECTED_FROM_DUAL, and no user confirmation is needed.
- A file that carries EPL-2.0 alone stays REVIEW_REQUIRED for the user.
- A file with upstream (IBM/Eclipse) content cannot be relicensed at all. It stays EPL in a separate module or is
  clean-roomed.

## How it was checked (CPU only)

1. Header at the extraction commit (`553eefd41d`, SWT integration branch round 6): `git cat-file` on the source blob,
   first `SPDX-License-Identifier` line.
2. Header in the attribution change: hsoliwal/eclipse.platform.swt PR #204 ("Synexia attribution", merged 2026-10-17).
   Its head commit `50d0edec601754f8c19654d12e9d71b39625bbf3` publishes these files with
   `SPDX-License-Identifier: Apache-2.0 OR EPL-2.0`. The header there reads "This program and the accompanying
   materials are made available under the terms of the Apache License, Version 2.0, or (at your option) the Eclipse
   Public License 2.0", with "Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others".
   - The merge into master (`0d8193176a`) kept the old EPL-only headers; its blob ids equal the integration blobs.
   - The integration branch never took the new headers either. That is why the extraction blobs still read EPL-2.0.
3. Body identity: for every file whose extraction header is EPL-2.0 alone, the code from the `package` line on was
   compared byte for byte (`cmp`) with the dual-licensed blob of PR #204. All 31 bodies are equal, with line counts
   recorded. The extraction blob therefore differs from the dual-licensed publication only in its header.
4. Upstream content: `grep -i 'IBM Corporation|Copyright (c) 20[01]x'` over every source blob found 0 hits. All of
   these files are new M3 files, and every commit touching them is by Hitesh Soliwal (30 commits, `git log
   --format=%an`).

## Counts

| Decision | Main | Tests |
|---|---:|---:|
| ELECTED_FROM_DUAL, dual at the extraction commit (`PackedHeapGrowth`, several tests) | 1 | 6 |
| ELECTED_FROM_DUAL, same body published dual in PR #204 | 23 | 8 |
| Apache-2.0 at source | 0 | 4 |
| REVIEW_REQUIRED (EPL-2.0 alone, no dual publication) | 0 | 0 |
| Upstream content (must stay EPL) | 0 | 0 |

The target headers ("Copyright 2026 Hitesh Soliwal and contributors", `SPDX-License-Identifier: Apache-2.0`) and the
`provenance-viewport-20261018.json` rows stay as they are. The provenance names the extraction blob; this record
links each one to its dual-licensed twin.

## Later waves

- Nebula Grid sources (wave 3) carry `Apache-2.0 OR EPL-2.0` at source (Synexia header), so they are ELECTED_FROM_DUAL.
- The Swing layout caches (wave 4) are GPLv2+CPE in `src/java.desktop` and need their own record before any Apache
  copy.
