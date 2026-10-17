<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# M3 bounded filtered and sorted collection views

This receiving packet comes from the private concept convergence PR [com.synexia#12055](https://github.com/hsoliwal/com.synexia/pull/12055), pinned to commit `9a18fe83a821f1814fa0a9e8222759622b445e56`. The runtime adaptation targets `37eba37dcbdf0c0222b494d07ba2fca401ce248f`.

The [recipe](recipe/README.md) contains the complete reusable Maven/OpenRewrite inputs, tests, unchanged SDK owners, category catalogue, donor/license decisions and qualified target payloads. [Qualification](recipe/QUALIFICATION.md) separates passing focused checks from inherited product holds and unrun performance or hardware gates.

[CANONICAL_MIRROR.tsv](CANONICAL_MIRROR.tsv) maps every mirrored source to its exact private commit path, Git blob, SHA-256 and file mode. [SYNEXIA_HANDOFF.json](SYNEXIA_HANDOFF.json) records the source and receiving identities. A fresh offline public-mirror SDK run and zero-change receiving proof are retained under [evidence/public-mirror](evidence/public-mirror/). Historical commands and absolute paths in that evidence describe the recorded run; portable reproduction commands are in the recipe README.

## Review order

1. Read the category and file-review records, then the qualification report.
2. Review the recipe/custody commit and exact pre/postimage manifests.
3. Review the separate application commit containing only the 13 actual SDK outputs.
4. Run the documented Maven SDK and contract profile before further promotion.

This packet preserves the existing public contracts. Receivers validate all admitted targets before writing and replace files serially; application requires a single writer and is not a cross-file filesystem transaction. No merge or product-completion flag is performed by the packet.
