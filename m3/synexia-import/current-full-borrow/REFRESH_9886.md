# Receive Synexia full-borrow estate refresh #9886

Status: candidate source-custody refresh only; no family or JDK promotion.

## Target baseline

M3JDK21 baseline:
`da6084c4efd4a0128fe36e92ea46d16f67304530`

## Synexia source

Source PR:
`hsoliwal/com.synexia#9886`

Pinned source revision carried by the estate:
`ecfc155eea38b9d4c85e644761a3b2fd2d2f9d1f`

Pinned estate Git blob:
`2ae93e0bd4fd03760df5aa5d99b90aafb36f1ba0`

DAG Git blob (unchanged):
`3e6c56b8555231fb36d0ac620ab112aa5970d45a`

Promotion-phase Git blob (unchanged):
`c4e8582f99caba32d718b2c1e86dae0854cdce3d`

## Delta

The successor estate still contains exactly 32 families.

Only two source identities changed relative to the currently received estate:

- `TEXT_INDEXSTRING`
  - old `3d1bbf3bc944e827b8b82aa6841bc0720d618e93`
  - new `8535757300a93d56df9db5c66e81e2ba81ecde01`
- `OPENREWRITE_RECIPES`
  - old `bb1935e5b0412717b5d9af6a8715993fbfa70a4d`
  - new `460491ea7ce48580ba9ba1f7373e190464666a56`

The other 30 family identities remain unchanged.

## Preserve existing String progress

The target already has an exact source-to-target mapping receipt for
`MINDEX_STRING_RUNTIME`.

That row remains:

- target state: `TARGET_MAPPING_PINNED`;
- proof receipt:
  `m3/synexia-import/current-full-borrow/string-source-target-map.tsv`.

This is custody/mapping progress only and does not mean the String family is accepted.

All other unqualified estate families remain `SOURCE_PIN_ONLY` with `NONE` proof receipt.

## Copyright / license

Qualified first-party Synexia authored implementation/expression keeps Apache-2.0 and
`Copyright 2026 Hitesh Soliwal and contributors` where classified by source provenance.

This receiver does not relicense OpenJDK or third-party donor bodies. Abstract ideas remain
engineering provenance rather than a claim of copyrighted source expression.

## Acceptance

The existing receiver checker must prove exact estate/DAG/phase bytes and refuse any target-state
promotion not explicitly admitted by its per-family rule.

The existing String-phase checker remains independently authoritative for the mapped String owner.

This refresh changes no java.base/HotSpot/JNI runtime source and cannot advance
`STRING -> ARRAYS`.
