# JDK naming fixed point: residue after step 1 turned green (2026-11-08)

`m3jdk-naming.yml` step 1 had failed since #2829 and hid step 6. After #2833 fixed step 1, step 6 failed:
`jdk_naming.py plan --tree HEAD` moves=36 rewrites=69 (run 42473209207, artifact naming-plan). This leaf sorts the rows.

| Rows | Owner | Action |
|---|---|---|
| 46 files that name Synexia/com.m3 on purpose (26 indexstring and 4 search donor-differential harnesses and stubs, a forward port, 2 receipt gates, 4 compatibility pin checkers, 2 handoff lineage checks, 3 receipts, 3 name-contract files, M3CanonicalApiBoundaryTest) | this lane | `NAMING_KEEP.tsv` rows with reasons (user decision: explicit KEEP list) |
| .github/workflows/m3-regex-plan-amalgamation.yml, m3-regex-required-ngrams.yml | unowned | PROSE_PRODUCT_VERSION applied by the recipe: comment "M3JDK21" -> "M3JDK 21" |
| .github/workflows/m3-jdk-modules.yml -> m3jdk-modules.yml | lane 37 | their #2838 |
| 10 m3/collections rows | a4 | left to the owner |
| 23 make/m3/runtime-integration/regex-rope-affix-effects rows (34 moves) | unowned; restored by b1df0c0d60cb "Restore merge-dropped #1080" | left: restored custody content, needs its owner or the user |

Each KEEP row was read in context before listing. Examples:
- `rejectInternalSurface("com.m3.collections.SlotArray")` proves the legacy surface is refused;
- `LICENSE_POLICY = "SYNEXIA_FIRST_PARTY_APACHE2_V1"` matches the pinned policy TSV, which the recipe keeps;
- `com.synexia.m3.search.M3ZSimilarity.sumJava` is the donor side of a differential;
- `$M2/com/m3/m3-algorithms` is the real Maven groupId.
Renaming any of them would invert the test or break the pin or the harness.
