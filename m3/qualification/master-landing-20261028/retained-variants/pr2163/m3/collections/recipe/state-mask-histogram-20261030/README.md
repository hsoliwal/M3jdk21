<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 state-mask histogram receiver, successor pin (2026-10-30)

Dated, additive successor of
[`state-mask-histogram-20261017`](../state-mask-histogram-20261017/README.md).
The 2026-10-17 receipt stays sealed and unchanged; this directory pins the canonical
Synexia recipe outputs that M3JDK21 received after that seal. Release blocker 13 (a):
`M3 state mask histogram Java21 JNI source proof` failed on master since 2026-10-19
(`generated output/source mirror SHA-256 drift`) because the mirrors moved with the
canonical recipes while the pin stayed at the first seal.

## Canonical lineage (hsoliwal/com.synexia, all contained in develop `ea340cc49a4ef3801a28b59d9c11cf4472cffe79`)

| Order | Canonical PR | Merge commit | Recipe / crate | Output moved | M3JDK21 receiving commit |
|-------|--------------|--------------|----------------|--------------|--------------------------|
| 0 | [#12177](https://github.com/hsoliwal/com.synexia/pull/12177) | `7f70c57ceed2ad24d1ca888db604be74be10e30d` | `com.synexia.m3.StateMaskHistogram20261017` | first seal (Java `965a098c…`, SelfTest `11dbbb46…`, Test `cb985d05…`, C `2a426399…`) | `a27fd892978`, pin `45a6365d6a4` |
| 1 | [#12403](https://github.com/hsoliwal/com.synexia/pull/12403) | `1124f5b72744b306884beedebafd8558d49d4c4c` | `com.synexia.m3.ArrayMaskLookup20261018` (`recipe-crates/m3-array-mask-lookup-20261018`) | SelfTest `11dbbb46…` -> `3386e196…`; C `2a426399…` -> `21ff3a8d…` | `f2688ab6905` (receipt `m3/qualification/string-index-reverse-20261018/collections/SOURCE_SEALS.json`) |
| 2 | [#12702](https://github.com/hsoliwal/com.synexia/pull/12702) | `b0a7ad317c874d02f4ffbb0b50af68f0e3bf1923` | `hash-pinned-java/-text m3jdk21-indexed-histogram-native-20261021` | Java `965a098c…` -> `bdaa38a2…`; C `21ff3a8d…` -> `5d97f190…`; new `M3StateMaskHistogramIndexedNativeContract/Test` | `da5de841528`, `96628417f84`, `5564cf6c950`, `66507ed57f6` |
| 3 | [#12727](https://github.com/hsoliwal/com.synexia/pull/12727) | `f9fb9b3c86e2f4b64a2ade88f4163372c1d5af24` | `recipes/m3-statehist-adaptive-maskcount-20261021` | Java `bdaa38a2…` -> `d1dcab26…`; C kept `5d97f190…`; new `M3StateMaskAdaptiveCountContract` | `b0e2ffcca8f`, `a8b0576f3dc` |

Non-canonical edits found on the mirrors and reverted by this successor (generated files are
regenerated from the canonical output, never hand-edited): licence-header rewrites
`104f2f46ced` (`src/main`) and `9ea34a387da` (`src/test`), whose own rule excluded files
pinned by a hash-pinned manifest.

## Pins

`SOURCE_PINS.tsv` names the canonical repository, the develop commit that contains every
recipe above, the per-recipe merge commits and the Git blob ids of the seven `templates/`.
`manifest.tsv` / `native-manifest.tsv` carry `path`, `preimage`, `postimage`, `template`:
the preimage is the digest the 2026-10-17 receipt sealed (lineage only) or `ABSENT`.

Local reproduction: `bash m3/collections/recipe/state-mask-histogram-20261030/verify.sh`.
This proves a bounded Java21/C17/JNI contract, **not** full patched JDK, OpenRewrite
execution, JCK/jtreg, Maven module or multi-platform parity. The next canonical change
gets the next dated successor directory; this one is not edited afterwards.
