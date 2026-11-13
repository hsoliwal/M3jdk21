# Runtime integration exact superseded-state repair — 2026-10-11

Baseline: `bd56674dad083d9fe554962b933365793395f935`
Branch: `aix/runtime-string-superseded-receipt-20261011`

## Inventory

The runtime integration recipe has 66 hash-pinned source paths. GitHub's forward compare from its recorded target `3c3d66694aaa1b345bbee1c3fe905f1a07ad3906` to the current baseline reports 63 changed repository paths, but only one overlaps the recipe's 66 pinned paths: `src/java.base/share/classes/java/lang/String.java`. This bounds the source-state repair to one file.

## Exact lineage and semantic review

The original recipe postimage remains SHA-256
`c49e422dfe0e6b83a2d51155f0d36b5223719dfefd211a96313705bc3ee253ca`.
At the reviewed source state, the exact file SHA-256 is
`eed88205feda9f64b03515603e5807e3f83961ddeb99dae4ef84bd2034d6f568`.

The forward compare identifies only 21 changed lines in `String.java` (13 additions,
8 deletions). The later evolution relocates the existing
`isConservativeLiteralRegex(String)` helper before the `matches(String)`
Javadoc and replaces the helper's implementation Javadoc with a shorter
comment. The method body and the `matches(String)` consumer remain unchanged.
This is an exact later source state, not a replacement for the original recipe
postimage.

## Repair contract

- Keep the original `before` and `after` SHA-256 values unchanged.
- Add the exact reviewed current SHA-256 as a `superseded` source state.
- Permit a superseded hash on an existing-file transition only when it is
  unique and differs from both the original before and after hashes.
- Keep unknown source drift fail-closed; never broaden to path, prefix, or
  content-similarity matching.
- Preserve patch bytes and `patch_sha256`; no patch hunk is changed.

## Verification

CI must prove the current-tree fixed point, reverse/apply refusal behavior, and
unknown-drift refusal. Then rerun foundation and release CI; new failures must
be inventoried as separate leaves. No overall release pass is claimed by this
one-file source-state reconciliation.
