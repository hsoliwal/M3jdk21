<!-- SPDX-License-Identifier: Apache-2.0 -->
# collections-superset track: optimization ledger

`OPTIMIZATION_LEDGER.tsv` has one row per measured leaf of the 2026-10-20 optimization pass (brief rule 23):
`leaf  operation  size  before  after  unit  method  catalogue_id  commit  evidence_path`.

- `M0 baseline` rows compare each owned five-provider class (`after`) with its donor type (`before`) at 10^5 and
  10^6, from `m3/collections/qualification/optimization-pass-20261020/baseline-20261020.tsv`.
- Later leaves compare the owned class before and after one change, measured the same way (one JVM per
  measurement, `-Xms3g -Xmx3g -XX:+UseSerialGC`, JDK 21.0.9); a measured "no" keeps its row.
