<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# csv-spec: the jdk.m3 CSV API as the superset of three CSV libraries

User direction (2026-10-31): "let our API be superset ... JDK superset". M3JDK vendors three CSV implementations
(univocity-parsers 2.9.1, commons-csv 1.12.0, jackson-dataformat-csv 2.16.1). M3JDK owns one implementation in the
JDK whose API covers everything the three offer; the libraries keep their APIs as facades over it. This lane is
the specification. The implementation is HOLD (new production classes go through Chrome local AI, one call per
file; unavailable).

## Read in this order

| File | Content |
|---|---|
| `CSV_CAPABILITY_SYNONYMS.tsv` | CSV-CAPABILITY v1, the reviewed synonym table: canonical capability, group, tier, the donor keys of each of the three donors, semantics and differences (defaults, false friends) |
| `CSV_CAPABILITIES.tsv` | One row per canonical capability with donor coverage (Y/-), key count and every donor method behind it |
| `INTERFACE.md` | The `jdk.m3.csv` API at spec level: types, one home per capability, donor facades |
| `inputs/` | `CSV_API_SUPERSET.v1.tsv` (#2215 API superset v1, 242 keys), donor Javadoc evidence (`DONOR_METHODS.tsv`), enum options (`DONOR_FEATURES.tsv`), keys v1 missed (`DONOR_EXTRA.tsv`) |
| `GATES.tsv`, `RECIPE_MEMORY.tsv`, `FOSS_REUSE_DECISION.tsv` | Gate results, corrections, donor decisions |

## From 8 name matches to 41 shared capabilities

API superset v1 (`inputs/CSV_API_SUPERSET.v1.tsv`) normalized every configuration method to a key by dropping its
verb; only 8 keys matched across donors by name. The review:

1. Widened the universe to 386 (donor, key) pairs: the 250 donor cells of v1, 30 enum-valued options v1 could not
   see (`feature:CsvParser.Feature.TRIM_SPACES`, `feature:QuoteMode.ALL`, ...), 18 keys from univocity's
   `CommonParserSettings` / `CommonWriterSettings` that v1's file regex missed (`gap:`), and 88 record-level
   operation keys (`op:`).
2. Assigned every pair, per donor, to one of 78 canonical capabilities, reading each donor method's Javadoc
   (`inputs/DONOR_METHODS.tsv`).
3. Of the 8 name matches, 5 are true synonyms (`delimiter`, `delimiterString`, `lineSeparator`, `nullValue`,
   `quote`) and 3 are false friends: univocity `escapeChar` is the TSV escape while jackson `escapeChar` is the CSV
   escape; univocity `format` is the dialect object while commons `format(...)` formats a record; commons `header`
   is the column names while jackson `header` is the use-the-first-record flag.

Result: 14 capabilities offered by all three donors, 27 by two, 37 by one; tiers 27 CORE, 44 EXTENDED,
6 INTEGRATION (jackson streaming and feature protocols, univocity processors), 1 INTERNAL. Coverage per donor:
univocity 55, jackson 40, commons-csv 38 of the 78. Defaults differ between donors (whitespace trimming, empty
lines, comments), so the API's own default is RFC 4180 and each donor's defaults are a named preset.

## Re-run

```
OUT=<scratch> bash m3/tooling/jdk-superset/csv-spec/tools/run_gates.sh
```

| Tool | Does |
|---|---|
| `tools/csv_donor_docs.py` | Reads the vendored donor sources: configuration methods with their first Javadoc sentence, enum options, keys outside v1 |
| `tools/csv_capabilities.py` | Projects the synonym table onto the donor-key universe, writes `CSV_CAPABILITIES.tsv`; exit 1 on a missing, duplicate or unknown pair or an empty capability; prints the name-match review |
| `tools/run_gates.sh` | v1 input identity, donor evidence fixed point, capability gate, capability fixed point, interface coverage |

Needs Python 3 and the sparse paths `m3/vendor/third_party/{univocity-parsers,commons-csv,jackson-dataformat-csv}`.

## Next

Per capability, one discriminator input run through each donor and through each facade on the single M3
implementation, outputs equal; then one LLD per `jdk.m3.csv` type and one Chrome generation call per file. The
atoms of the three donors (#2215, `m3/tooling/jdk-superset/csv/`) are the implementation's starting material.
