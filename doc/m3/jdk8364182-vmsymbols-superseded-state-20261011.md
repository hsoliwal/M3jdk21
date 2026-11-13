# JDK-8364182 vmSymbols superseded-state reconciliation — 2026-10-11

Baseline: `882b6975e2f4920bcb13d7c14478ba1c19ee0547`
Branch: `aix/repair-jdk8364182-superseded-vmsymbols-20261011`

## Observed failure

The release workflow's `m3/backports/verify.py` check reports:

- Target: `src/hotspot/share/classfile/vmSymbols.hpp`
- Original JDK-8364182 backport postimage SHA-256:
  `eeffc686c1eef000be5e3be5eb02e13257c744a63ea926762e9d4749fadecc67`
- Exact current M3 runtime integration SHA-256:
  `4f752ab79abf854d144e53118a2cc57a4b3cc2432140e9297079cca86dd2abab`

The current runtime integration recipe independently pins the latter as the exact `vmSymbols.hpp` postimage. The current source still contains
`serializeSecurityPropertiesToByteArray_name`, the JDK-8364182 VM method symbol, and additionally contains the M3 String class symbol entries. This is a later integrated source state, not evidence that the original backport postimage was different.

## Repair contract

- Preserve `baseline_sha256` and original `target_sha256` in
  `m3/backports/recipes/jdk-8364182/adaptation.tsv`.
- Add a separate exact `superseded_target_sha256` field. Only the vmSymbols row
  receives the known current hash; all other rows remain empty.
- Update `m3/backports/verify.py` to accept the original target hash or this
  one exact superseded hash. Validate the latter as lowercase SHA-256 and reject
  equality with either the baseline or original target hash.
- Preserve the original JDK-8364182 donor commit, row count, target path set,
  semantic symbol checks, and candidate-only promotion status.
- Unknown drift must continue to fail closed. Do not replace the original
  target hash or disable the source-integrity verifier.

## Evidence and verification

The original JDK-8364182 backport postimage remains recorded as
`eeffc686...`. The current M3 runtime text manifest independently seals
`vmSymbols.hpp` as `4f752ab7...`; its source contains both the JDK command
symbol and M3 symbols. After the schema change, run `python m3/backports/verify.py`
and the focused backport recipe tests, then the full release workflow. No full
release pass is inferred from this source-state reconciliation.
