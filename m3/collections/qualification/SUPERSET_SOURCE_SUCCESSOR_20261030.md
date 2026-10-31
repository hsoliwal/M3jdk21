<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Superset source successor 2026-10-30

Receipt `qualification/superset-source-successor-20261030.tsv` (SHA-256 `7ab924828e42b85eb14ea1de2f8720bf5c88c5011365af78621f8acbf1785781`)
seals the exact current `m3/collections` source set as one additive transition over the
Tools4j refinement result (composed digest `6283ee8df10e7d2e4b2a0d47635ef57775fb476c1cae9545cfe1692fa59c176e`,
510 rows): 166 refined digests and 262 admitted Java sources, resulting in 772 rows and 765 Java sources under `src`.

Predecessor selector: `qualification/installed-source-enum-set-20261014.tsv` composes to the Tools4j predecessor digest; the sibling
successor `installed-source-successor-20261018.tsv` (com.synexia#12414) stays as history.
Origin: superset merge `b251188755a` of hsoliwal/M3jdk21#1482 kept the sibling selector, so
`verify_donation.py` saw 203 predecessor rows instead of the sealed 113.

Recipe: com.synexia `.m3/task-crates/release-blocker8-tools4j-rows-20261030/tools/fix_donation_ledger.py`
(deterministic, fixed point, fails closed on unknown row shapes).
